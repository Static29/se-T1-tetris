package tetris.network;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** 단일 1대1 세션 서버. 방/매칭/채팅/다인전 기능은 포함하지 않는다. */
public final class DedicatedServer implements AutoCloseable {
    private final ServerSocket listener;
    private final ConnectionOptions options;
    private final Map<String, Player> players = new LinkedHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private String hostId;
    private String mode = "NORMAL";
    private int durationSeconds;
    private String matchId = "";
    private long nextPlayer = 1;
    private long nextMatch = 1;

    public DedicatedServer(InetAddress address, int port, ConnectionOptions options) throws IOException {
        this.options = Objects.requireNonNull(options);
        listener = new ServerSocket();
        try { listener.bind(new InetSocketAddress(address, port)); }
        catch (IOException e) { listener.close(); throw e; }
        Thread.ofVirtual().name("tetris-accept").start(this::acceptLoop);
    }
    public int port() { return listener.getLocalPort(); }
    private void acceptLoop() {
        while (!closed.get()) {
            Socket socket = null;
            try {
                socket = listener.accept();
                synchronized (this) {
                    if (closed.get() || players.size() == 2) { socket.close(); continue; }
                    String id = "p" + nextPlayer++;
                    Player player = new Player(id);
                    player.connection = new PeerConnection(socket, options,
                            message -> receive(id, message), reason -> disconnected(id));
                    players.put(id, player);
                    if (hostId == null) hostId = id;
                    player.connection.start();
                    send(player, "WELCOME", id, hostId);
                    send(player, "CONFIG", mode, Integer.toString(durationSeconds));
                    lobby();
                }
            } catch (IOException e) {
                if (socket != null) try { socket.close(); } catch (IOException ignored) { }
                if (!closed.get()) System.err.println("Network accept failed: " + e.getMessage());
            }
        }
    }
    private synchronized void receive(String id, NetworkMessage message) {
        Player player = players.get(id);
        if (player == null) return;
        var f = message.fields();
        try {
            switch (message.type()) {
                case "CONFIG" -> {
                    count(f, 2); host(player); idle();
                    String selected = f.get(0);
                    if (!Set.of("NORMAL", "ITEM", "TIMED").contains(selected)) fail("INVALID_MODE");
                    int seconds = Integer.parseInt(f.get(1));
                    if ((selected.equals("TIMED") && (seconds < 1 || seconds > 86400))
                            || (!selected.equals("TIMED") && seconds != 0)) fail("INVALID_DURATION");
                    mode = selected; durationSeconds = seconds;
                    players.values().forEach(p -> p.ready = false);
                    broadcast("CONFIG", mode, Integer.toString(seconds)); lobby();
                }
                case "READY" -> {
                    count(f, 1); idle();
                    if (!Set.of("true", "false").contains(f.get(0))) fail("INVALID_READY");
                    player.ready = Boolean.parseBoolean(f.get(0)); lobby();
                    if (players.size() == 2 && players.values().stream().allMatch(p -> p.ready)) {
                        matchId = "m" + nextMatch++;
                        players.values().forEach(p -> { p.lastAttack = -1; p.lastState = -1; });
                        broadcast("START", matchId, mode, Integer.toString(durationSeconds));
                    }
                }
                case "STATE" -> {
                    count(f, 3); active(f.get(0)); long sequence = nonnegative(f.get(1));
                    if (sequence <= player.lastState) return;
                    player.lastState = sequence;
                    relay(player, "STATE", matchId, id, f.get(1), f.get(2));
                }
                case "ATTACK" -> {
                    count(f, 3); active(f.get(0)); long sequence = nonnegative(f.get(1));
                    if (sequence <= player.lastAttack) return;
                    validateRows(f.get(2)); player.lastAttack = sequence;
                    relay(player, "ATTACK", matchId, id, f.get(1), f.get(2));
                }
                case "GAME_OVER" -> {
                    count(f, 1); active(f.get(0));
                    broadcast("GAME_OVER", matchId, id);
                    // 승패는 Game 담당이 결정한다. 네트워크 도착 순서로 승자를 정하지 않는다.
                }
                case "RESULT" -> {
                    count(f, 2); host(player); active(f.get(0));
                    if (!players.containsKey(f.get(1)) && !f.get(1).equals("DRAW")) fail("INVALID_WINNER");
                    broadcast("RESULT", matchId, f.get(1));
                    matchId = ""; players.values().forEach(p -> p.ready = false); lobby();
                }
                default -> fail("UNSUPPORTED_COMMAND");
            }
        } catch (IllegalArgumentException e) { send(player, "ERROR", e.getMessage()); }
    }
    private static void validateRows(String payload) {
        String[] rows = payload.split("/", -1);
        if (rows.length < 1 || rows.length > 10) fail("INVALID_ATTACK_ROWS");
        for (String row : rows) if (!row.matches("[01]{10}")) fail("INVALID_ATTACK_ROWS");
    }
    private static long nonnegative(String text) {
        long value = Long.parseLong(text);
        if (value < 0) fail("INVALID_SEQUENCE");
        return value;
    }
    private void lobby() {
        broadcast("LOBBY", Integer.toString(players.size()), hostId == null ? "" : hostId);
        for (Player p : List.copyOf(players.values())) broadcast("READY", p.id, Boolean.toString(p.ready));
    }
    private void relay(Player sender, String type, String... fields) {
        for (Player p : List.copyOf(players.values())) if (p != sender) send(p, type, fields);
    }
    private void broadcast(String type, String... fields) {
        for (Player p : List.copyOf(players.values())) send(p, type, fields);
    }
    private void send(Player p, String type, String... fields) { p.connection.send(NetworkMessage.of(type, fields)); }
    private void idle() { if (!matchId.isEmpty()) fail("MATCH_RUNNING"); }
    private void active(String id) { if (matchId.isEmpty() || !matchId.equals(id)) fail("STALE_MATCH"); }
    private void host(Player p) { if (!p.id.equals(hostId)) fail("HOST_ONLY"); }
    private static void count(List<String> fields, int count) { if (fields.size() != count) fail("FIELD_COUNT"); }
    private static void fail(String reason) { throw new IllegalArgumentException(reason); }

    /** 한쪽 연결이 끊기면 세션 종료. 양쪽 UI는 접속 초기 화면으로 돌아가야 한다. */
    private synchronized void disconnected(String id) {
        if (!players.containsKey(id)) return;
        var previous = List.copyOf(players.values());
        players.clear(); hostId = null; matchId = ""; mode = "NORMAL"; durationSeconds = 0;
        for (Player p : previous) p.connection.close();
    }
    @Override public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { listener.close(); } catch (IOException ignored) { }
        if (!players.isEmpty()) disconnected(players.keySet().iterator().next());
    }
    private static final class Player {
        final String id;
        PeerConnection connection;
        boolean ready;
        long lastState = -1, lastAttack = -1;
        Player(String id) { this.id = id; }
    }
}
