package tetris.network;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Serializes room changes and timer events independently of socket I/O. */
final class BattleRooms implements AutoCloseable {
    private final Map<String, Player> players = new HashMap<>();
    private final Map<String, Room> rooms = new HashMap<>();
    private final ScheduledExecutorService clock = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("battle-clock").factory());
    private long nextRoom = 1;
    private long nextMatch = 1;

    synchronized void connect(String id, Consumer<NetworkMessage> output) {
        connect(id, output, id);
    }

    synchronized void connect(String id, Consumer<NetworkMessage> output, String displayName) {
        players.put(id, new Player(id, output, displayName));
        send(players.get(id), "ROOM_WELCOME", id);
    }

    synchronized void receive(String id, NetworkMessage message) {
        Player player = players.get(id);
        if (player == null) return;
        var fields = message.fields();
        try {
            switch (message.type()) {
                case "CREATE_ROOM" -> {
                    require(fields.size() == 5 && player.room == null, "INVALID_CREATE");
                    int size = Integer.parseInt(fields.get(0));
                    require(size >= 2 && size <= 4, "INVALID_SIZE");
                    String mode = fields.get(1);
                    require(Set.of("DUEL", "TIMED").contains(mode), "INVALID_MODE");
                    int seconds = Integer.parseInt(fields.get(2));
                    require(mode.equals("TIMED") ? seconds >= 1 && seconds <= 86400 : seconds == 0,
                            "INVALID_DURATION");
                    boolean items = flag(fields.get(3));
                    String difficulty = fields.get(4);
                    require(Set.of("EASY", "NORMAL", "HARD").contains(difficulty), "INVALID_DIFFICULTY");
                    Room room = new Room(Long.toString(nextRoom++), size, mode, seconds, items, difficulty);
                    rooms.put(room.id, room);
                    join(player, room);
                }
                case "JOIN_ROOM" -> {
                    require(fields.size() == 1 && player.room == null, "INVALID_JOIN");
                    Room room = rooms.get(fields.getFirst());
                    require(room != null && room.match == null && room.members.size() < room.size,
                            "ROOM_UNAVAILABLE");
                    join(player, room);
                }
                case "LEAVE_ROOM" -> {
                    require(fields.isEmpty() && player.room != null, "INVALID_LEAVE");
                    leave(player);
                    send(player, "ROOM_LEFT");
                }
                default -> {
                    require(player.room != null, "NOT_IN_ROOM");
                    inRoom(player, message);
                }
            }
        } catch (IllegalArgumentException e) {
            send(player, "ERROR", e instanceof NumberFormatException ? "INVALID_NUMBER" : e.getMessage());
        }
    }

    private void join(Player player, Room room) {
        player.room = room;
        player.ready = false;
        room.members.put(player.id, player);
        send(player, "ROOM_JOINED", room.id, Integer.toString(room.size), room.mode,
                Integer.toString(room.seconds), Boolean.toString(room.items), room.difficulty);
        roster(room);
    }

    private void inRoom(Player player, NetworkMessage message) {
        Room room = player.room;
        var fields = message.fields();
        if (message.type().equals("READY")) {
            require(room.match == null && fields.size() == 1, "INVALID_READY");
            player.ready = flag(fields.getFirst());
            roster(room);
            if (room.members.size() == room.size && room.members.values().stream().allMatch(p -> p.ready)) {
                start(room);
            }
            return;
        }
        int expected = switch (message.type()) {
            case "STATE", "ATTACK" -> 3;
            case "QUEUE_SPACE", "FINAL_SCORE" -> 2;
            case "GAME_OVER" -> 1;
            default -> throw new IllegalArgumentException("UNSUPPORTED_COMMAND");
        };
        require(fields.size() == expected, "FIELD_COUNT");
        Match match = room.match;
        require(match != null && match.id.equals(fields.getFirst()), "STALE_MATCH");
        require(match.alive.contains(player.id), "PLAYER_ELIMINATED");
        if (match.finishing && !Set.of("FINAL_SCORE", "GAME_OVER").contains(message.type())) return;
        switch (message.type()) {
            case "STATE" -> {
                long sequence = sequence(fields.get(1));
                require(fields.get(2).length() <= 16000, "STATE_TOO_LARGE");
                if (sequence <= player.lastState) return;
                player.lastState = sequence;
                var update = NetworkMessage.of("STATE", match.id, player.id, fields.get(1), fields.get(2));
                for (Player other : room.members.values()) {
                    if (other != player) other.output.accept(update);
                }
            }
            case "ATTACK" -> attack(room, match, player, fields);
            case "QUEUE_SPACE" -> player.queueHasSpace = flag(fields.get(1));
            case "GAME_OVER" -> eliminate(room, player.id);
            case "FINAL_SCORE" -> {
                require(match.finishing, "TIME_NOT_UP");
                match.scores.putIfAbsent(player.id, sequence(fields.get(1)));
                if (match.scores.keySet().containsAll(match.alive)) finishTimed(room);
            }
        }
    }

    private void start(Room room) {
        Match match = new Match("m" + nextMatch++, room.members.keySet());
        room.match = match;
        for (Player player : room.members.values()) {
            player.queueHasSpace = true;
            player.lastAttack = -1;
            player.lastState = -1;
        }
        broadcast(room, "ROOM_START", match.id, room.mode, Integer.toString(room.seconds),
                Boolean.toString(room.items), room.difficulty);
        if (room.mode.equals("TIMED")) {
            match.timer = clock.schedule(() -> timeUp(room, match), room.seconds, TimeUnit.SECONDS);
        }
    }

    private void attack(Room room, Match match, Player player, List<String> fields) {
        long sequence = sequence(fields.get(1));
        String rows = fields.get(2);
        require(rows.length() <= 109 && rows.matches("[01]{10}(/[01]{10}){1,9}"), "INVALID_ATTACK_ROWS");
        if (sequence <= player.lastAttack) return;
        player.lastAttack = sequence;
        var candidates = room.members.values().stream()
                .filter(p -> p != player && match.alive.contains(p.id) && p.queueHasSpace).toList();
        if (candidates.isEmpty()) return;
        Player target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        send(target, "ATTACK", match.id, player.id, fields.get(1), rows);
    }

    private synchronized void timeUp(Room room, Match match) {
        if (room.match != match) return;
        match.finishing = true;
        broadcast(room, "TIME_UP", match.id);
        match.timer = clock.schedule(() -> {
            synchronized (this) {
                if (room.match == match) finishTimed(room);
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void finishTimed(Room room) {
        Match match = room.match;
        long best = -1;
        String winner = "DRAW";
        for (String id : match.alive) {
            Long score = match.scores.get(id);
            if (score == null) continue;
            if (score > best) {
                best = score;
                winner = id;
            } else if (score == best) {
                winner = "DRAW";
            }
        }
        finish(room, winner);
    }

    private void eliminate(Room room, String id) {
        Match match = room.match;
        if (match == null || !match.alive.remove(id)) return;
        broadcast(room, "ELIMINATED", match.id, id);
        if (match.alive.size() <= 1) {
            finish(room, match.alive.stream().findFirst().orElse("DRAW"));
        } else if (match.finishing && match.scores.keySet().containsAll(match.alive)) {
            finishTimed(room);
        }
    }

    private void finish(Room room, String winner) {
        Match match = room.match;
        if (match.timer != null) match.timer.cancel(false);
        room.match = null;
        room.members.values().forEach(player -> player.ready = false);
        broadcast(room, "RESULT", match.id, winner);
        roster(room);
    }

    synchronized void disconnect(String id) {
        Player player = players.remove(id);
        if (player != null && player.room != null) leave(player);
    }

    private void leave(Player player) {
        Room room = player.room;
        player.room = null;
        room.members.remove(player.id);
        room.members.values().forEach(member -> member.ready = false);
        broadcast(room, "ROOM_PLAYER_LEFT", player.id);
        eliminate(room, player.id);
        if (room.members.isEmpty()) rooms.remove(room.id);
        else roster(room);
    }

    private void roster(Room room) {
        if (room.members.isEmpty()) return;
        broadcast(room, "ROOM_LOBBY", room.id, room.members.keySet().iterator().next(),
                Integer.toString(room.members.size()), Integer.toString(room.size));
        for (Player player : room.members.values()) {
            broadcast(room, "PLAYER_NAME", player.id, player.displayName);
            broadcast(room, "READY", player.id, Boolean.toString(player.ready));
        }
    }

    private static void broadcast(Room room, String type, String... fields) {
        NetworkMessage message = NetworkMessage.of(type, fields);
        room.members.values().forEach(player -> player.output.accept(message));
    }

    private static void send(Player player, String type, String... fields) {
        player.output.accept(NetworkMessage.of(type, fields));
    }

    private static boolean flag(String text) {
        require(Set.of("true", "false").contains(text), "INVALID_FLAG");
        return Boolean.parseBoolean(text);
    }

    private static long sequence(String text) {
        long sequence = Long.parseLong(text);
        require(sequence >= 0, "INVALID_NUMBER");
        return sequence;
    }

    private static void require(boolean condition, String error) {
        if (!condition) throw new IllegalArgumentException(error);
    }

    @Override public synchronized void close() {
        clock.shutdownNow();
        players.clear();
        rooms.values().forEach(room -> room.match = null);
        rooms.clear();
    }

    private static final class Player {
        final String id;
        final Consumer<NetworkMessage> output;
        final String displayName;
        Room room;
        boolean ready;
        boolean queueHasSpace;
        long lastAttack;
        long lastState;

        Player(String id, Consumer<NetworkMessage> output, String displayName) {
            this.id = id;
            this.output = output;
            this.displayName = displayName;
        }
    }

    private static final class Room {
        final String id;
        final int size;
        final String mode;
        final int seconds;
        final boolean items;
        final String difficulty;
        final Map<String, Player> members = new LinkedHashMap<>();
        Match match;

        Room(String id, int size, String mode, int seconds, boolean items, String difficulty) {
            this.id = id;
            this.size = size;
            this.mode = mode;
            this.seconds = seconds;
            this.items = items;
            this.difficulty = difficulty;
        }
    }

    private static final class Match {
        final String id;
        final Set<String> alive;
        final Map<String, Long> scores = new HashMap<>();
        boolean finishing;
        ScheduledFuture<?> timer;

        Match(String id, Set<String> members) {
            this.id = id;
            alive = new HashSet<>(members);
        }
    }
}
