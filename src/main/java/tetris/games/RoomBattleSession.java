package tetris.games;

import java.util.*;
import java.util.function.Consumer;
import tetris.network.NetworkMessage;

/** 방 프로토콜과 게임을 연결한다. UI 스레드에서 갱신하며 소켓과 인증은 호출자가 관리한다. */
public final class RoomBattleSession {
    public record Member(String id, String name, boolean ready, int sessionWins) {}
    public record Standing(int rank, String name, Long score) {}
    private final GameSettings preferences;
    private final Consumer<NetworkMessage> output;
    private final Map<String, String> names = new LinkedHashMap<>();
    private final Map<String, String> matchNames = new HashMap<>();
    private final Map<String, Boolean> ready = new HashMap<>();
    private final Map<String, Integer> wins = new HashMap<>(), ranks = new HashMap<>();
    private final Map<String, BattleFrame> frames = new LinkedHashMap<>();
    private final Map<String, Long> sequences = new HashMap<>(), attacks = new HashMap<>();
    private final Set<String> alive = new LinkedHashSet<>();
    private String playerId = "", roomId = "", hostId = "", matchId = "", winner = "", error = "";
    private int target = 2, seconds;
    private boolean timed, items, stopped, sentOver, queueSpace = true;
    private Difficulty difficulty = Difficulty.NORMAL;
    private Game game;
    private long stateSequence, attackSequence, elapsed, sinceState;
    private List<Standing> standings = List.of();

    public RoomBattleSession(GameSettings preferences, Consumer<NetworkMessage> output) {
        this.preferences = Objects.requireNonNull(preferences); this.output = Objects.requireNonNull(output);
    }
    private void send(String type, String... fields) { output.accept(NetworkMessage.of(type, fields)); }
    public void create(int count, BattleMode mode, Difficulty level, int duration) {
        if (!welcomed() || inRoom() || count < 2 || count > 4 || duration < 1 || duration > 86400)
            throw new IllegalArgumentException("방 설정 오류");
        send("CREATE_ROOM", Integer.toString(count), mode == BattleMode.TIMED ? "TIMED" : "DUEL",
                mode == BattleMode.TIMED ? Integer.toString(duration) : "0", Boolean.toString(mode == BattleMode.ITEM), level.name());
    }
    public void join(String number) {
        if (!welcomed() || inRoom() || !number.matches("[0-9]{1,18}")) throw new IllegalArgumentException("방 번호를 입력하세요.");
        send("JOIN_ROOM", number);
    }
    public void toggleReady() { if (inRoom() && !running() && winner.isEmpty()) send("READY", Boolean.toString(!ready.getOrDefault(playerId, false))); }
    public void leave() { if (inRoom()) send("LEAVE_ROOM"); }
    public void dismissResult() { winner = ""; standings = List.of(); }
    public void disconnected() { stopped = true; if (game != null) game.pause(); }

    public void receive(NetworkMessage message) {
        var f = message.fields();
        switch (message.type()) {
            case "ROOM_WELCOME" -> playerId = f.get(0);
            case "ROOM_JOINED" -> {
                roomId = f.get(0); target = Integer.parseInt(f.get(1));
                rules(f, 2); names.clear(); ready.clear(); frames.clear(); winner = ""; error = "";
            }
            case "ROOM_LOBBY" -> hostId = f.get(1);
            case "PLAYER_NAME" -> names.put(f.get(0), f.get(1));
            case "READY" -> ready.put(f.get(0), Boolean.parseBoolean(f.get(1)));
            case "ROOM_PLAYER_LEFT" -> { names.remove(f.get(0)); ready.remove(f.get(0)); }
            case "ROOM_LEFT" -> {
                disconnected(); matchId = roomId = winner = ""; names.clear(); ready.clear(); frames.clear(); game = null;
            }
            case "ROOM_START" -> {
                matchId = f.get(0); rules(f, 1); winner = error = ""; standings = List.of();
                alive.clear(); alive.addAll(names.keySet()); ranks.clear(); frames.clear(); sequences.clear(); attacks.clear();
                matchNames.clear(); matchNames.putAll(names);
                stopped = sentOver = false; queueSpace = true;
                elapsed = sinceState = stateSequence = attackSequence = 0;
                game = new Game(new GameSettings(difficulty, preferences.colorBlindMode(), items ? GameMode.ITEM : GameMode.CLASSIC,
                        preferences.screenSize(), preferences.keyBindings(), preferences.playerTwoBindings(), preferences.graphical()));
                game.setAttackListener(rows -> { if (playing()) send("ATTACK", matchId, Long.toString(attackSequence++), BattleFrame.encodeRows(rows)); });
                game.start(); sendState(); send("QUEUE_SPACE", matchId, "true");
            }
            case "STATE" -> {
                if (!current(f) || !alive.contains(f.get(1)) || f.get(1).equals(playerId)) return;
                long sequence = Long.parseLong(f.get(2));
                if (sequence <= sequences.getOrDefault(f.get(1), -1L)) return;
                BattleFrame frame = BattleFrame.decode(f.get(3));
                // 화면 payload의 이름 대신 서버가 인증한 닉네임을 사용한다.
                frames.put(f.get(1), new BattleFrame(name(f.get(1)), frame.board(), frame.score(), frame.state(), frame.pending(), frame.next(), frame.hold()));
                sequences.put(f.get(1), sequence);
            }
            case "ATTACK" -> {
                if (!current(f) || !playing() || !alive.contains(f.get(1)) || f.get(1).equals(playerId)) return;
                long sequence = Long.parseLong(f.get(2));
                if (sequence <= attacks.getOrDefault(f.get(1), -1L)) return;
                game.receiveAttack(BattleFrame.decodeRows(f.get(3))); attacks.put(f.get(1), sequence); publishQueue();
            }
            case "ELIMINATED" -> {
                if (!current(f)) return;
                String id = f.get(1);
                if (alive.contains(id)) { ranks.put(id, alive.size()); alive.remove(id); }
                if (id.equals(playerId)) { game.pause(); stopped = true; }
            }
            case "TIME_UP" -> {
                if (!current(f)) return;
                if (playing()) { sendState(); send("FINAL_SCORE", matchId, Long.toString(game.getScore())); }
                stopped = true; game.pause();
            }
            case "RESULT" -> {
                if (!current(f)) return;
                winner = f.get(1); stopped = true; game.pause();
                frames.put(playerId, BattleFrame.of(game, name(playerId)));
                if (!winner.equals("DRAW")) wins.merge(winner, 1, Integer::sum);
                var rows = new ArrayList<Standing>();
                var ids = new LinkedHashSet<>(alive); ids.addAll(ranks.keySet());
                for (String id : ids) {
                    // 서버는 승자만 확정한다. 시간전의 나머지 순위는 단정하지 않는다(0).
                    int rank = id.equals(winner) ? 1 : ranks.getOrDefault(id, 0);
                    rows.add(new Standing(rank, name(id), frames.containsKey(id) ? frames.get(id).score() : null));
                }
                rows.sort(Comparator.comparingInt(s -> s.rank() == 0 ? Integer.MAX_VALUE : s.rank()));
                standings = List.copyOf(rows); matchId = ""; ready.replaceAll((id, value) -> false);
            }
            case "ERROR" -> {
                String code = f.isEmpty() ? "서버 요청 실패" : f.get(0);
                // 종료 통보와 이미 전송 중인 마지막 STATE가 교차한 응답은 정상 종료를 덮지 않는다.
                if (code.equals("STALE_MATCH") && !running() || code.equals("PLAYER_ELIMINATED") && !playing()) return;
                error = code;
            }
            default -> { }
        }
    }
    private void rules(List<String> fields, int offset) {
        timed = fields.get(offset).equals("TIMED"); seconds = Integer.parseInt(fields.get(offset + 1));
        items = Boolean.parseBoolean(fields.get(offset + 2)); difficulty = Difficulty.valueOf(fields.get(offset + 3));
        if (seconds < 0 || seconds > 86400 || timed && seconds == 0) throw new IllegalArgumentException("제한 시간 오류");
    }
    private boolean current(List<String> fields) { return running() && !fields.isEmpty() && matchId.equals(fields.get(0)); }
    public void update(long millis) {
        if (millis < 0) throw new IllegalArgumentException("시간 오류");
        if (!playing()) return;
        elapsed += millis; sinceState += millis; game.advanceTime(millis); publishQueue();
        if (game.getState() == GameState.GAME_OVER && !sentOver) {
            sendState(); send("GAME_OVER", matchId); sentOver = true; stopped = true;
        } else if (sinceState >= 50) { sendState(); sinceState = 0; }
    }
    public void act(GameAction action) { if (playing()) { BattleMatch.apply(game, action); update(0); if (playing()) sendState(); } }
    private void publishQueue() {
        boolean space = !game.isAttackQueueFull();
        if (queueSpace != space) { queueSpace = space; send("QUEUE_SPACE", matchId, Boolean.toString(space)); }
    }
    private void sendState() { send("STATE", matchId, Long.toString(stateSequence++), BattleFrame.of(game, name(playerId)).encode()); }
    public boolean welcomed() { return !playerId.isEmpty(); }
    public boolean inRoom() { return !roomId.isEmpty(); }
    public boolean running() { return !matchId.isEmpty(); }
    public boolean playing() { return running() && !stopped && alive.contains(playerId); }
    public String roomId() { return roomId; }
    public String name(String id) { return names.getOrDefault(id, matchNames.getOrDefault(id, id)); }
    public String hostId() { return hostId; }
    public String playerId() { return playerId; }
    public String error() { return error; }
    public String winner() { return winner; }
    public List<Standing> standings() { return standings; }
    public List<Member> members() { return names.entrySet().stream().map(e -> new Member(e.getKey(), e.getValue(), ready.getOrDefault(e.getKey(), false), wins.getOrDefault(e.getKey(), 0))).toList(); }
    public String rulesLabel() { return "개인전 · " + target + "인 · " + (timed ? "시간 " + seconds + "초" : items ? "아이템" : "일반") + " · " + difficulty.getLabel(); }
    public long remainingSeconds() { return Math.max(0, seconds - elapsed / 1000); }
    public boolean timed() { return timed; }
    public BattleFrame local() { return game == null ? null : BattleFrame.of(game, name(playerId)); }
    /** 탈락 후에는 생존자 중 한 명을 왼쪽에 배치하여 관전한다. */
    public List<BattleFrame> visibleFrames() {
        var result = new ArrayList<BattleFrame>();
        if (alive.contains(playerId) && game != null) result.add(local());
        for (String id : alive) if (!id.equals(playerId) && frames.containsKey(id)) result.add(frames.get(id));
        return List.copyOf(result);
    }
}
