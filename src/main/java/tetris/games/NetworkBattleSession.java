package tetris.games;

import java.time.Duration;
import java.util.*;
import tetris.network.*;

/** 기존 2인 DedicatedServer 프로토콜을 게임에 연결한다. 모든 메서드는 게임 스레드에서 호출한다. */
public final class NetworkBattleSession implements AutoCloseable {
    private final NetworkClient client;
    private final GameSettings settings;
    private final String profile;
    private String playerId = "", hostId = "", matchId = "", opponentId = "";
    private BattleMode mode = BattleMode.NORMAL;
    private int seconds = 180, players;
    private final Map<String, Boolean> ready = new HashMap<>();
    private Game game;
    private BattleFrame opponent;
    private long stateSequence, attackSequence, elapsed, sinceState;
    private boolean sentGameOver, opponentOver, resultSent, connected = true;
    private String result = "", error = "";

    public NetworkBattleSession(NetworkClient client, GameSettings settings, String profile) {
        if (!BattleMatch.PROFILES.contains(profile)) throw new IllegalArgumentException("프로필 오류");
        this.client = Objects.requireNonNull(client); this.settings = Objects.requireNonNull(settings); this.profile = profile;
    }
    public void configure(BattleMode mode, int seconds) {
        if (seconds < 1 || seconds > 86400) throw new IllegalArgumentException("시간 범위 오류");
        send("CONFIG", mode.name(), Integer.toString(mode == BattleMode.TIMED ? seconds : 0));
    }
    public void ready() { send("READY", Boolean.toString(!ready.getOrDefault(playerId, false))); }
    private void send(String type, String... fields) {
        if (!client.send(NetworkMessage.of(type, fields))) error = "메시지를 전송하지 못했습니다.";
    }
    public void update(long millis) {
        if (millis < 0) throw new IllegalArgumentException("시간 범위 오류");
        if (!connected) return;
        try {
            NetworkMessage message;
            for (int i = 0; i < 256 && (message = client.poll(Duration.ZERO)) != null; i++) receive(message);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); close(); }
        catch (RuntimeException e) { error = "잘못된 서버 메시지: " + e.getMessage(); close(); }
        if (client.disconnectReason() != null) {
            connected = false;
            error = "연결이 종료되었습니다: " + client.disconnectReason()
                    + (error.isEmpty() ? "" : " (" + error + ")");
            return;
        }
        if (!running()) return;
        long step = mode == BattleMode.TIMED ? Math.min(millis, Math.max(0, seconds * 1000L - elapsed)) : millis;
        game.advanceTime(step); elapsed += step; sinceState += millis;
        if (mode == BattleMode.TIMED && elapsed >= seconds * 1000L) game.pause();
        if (game.getState() == GameState.GAME_OVER && !sentGameOver) {
            sendState(); send("GAME_OVER", matchId); sentGameOver = true;
        }
        if (sinceState >= 50) { sendState(); sinceState = 0; }
        if (isHost() && opponent != null && !resultSent) {
            String winner = null;
            boolean lost = game.getState() == GameState.GAME_OVER;
            if (lost || opponentOver) winner = lost && opponentOver ? "DRAW" : lost ? opponentId : playerId;
            else if (mode == BattleMode.TIMED && game.getState() == GameState.PAUSED && opponent.state() == GameState.PAUSED)
                winner = game.getScore() == opponent.score() ? "DRAW" : game.getScore() > opponent.score() ? playerId : opponentId;
            if (winner != null) { resultSent = true; send("RESULT", matchId, winner); }
        }
    }
    private void receive(NetworkMessage message) {
        var f = message.fields();
        switch (message.type()) {
            case "WELCOME" -> { playerId = f.get(0); hostId = f.get(1); }
            case "CONFIG" -> { mode = BattleMode.valueOf(f.get(0)); seconds = mode == BattleMode.TIMED ? Integer.parseInt(f.get(1)) : 180; }
            case "LOBBY" -> { players = Integer.parseInt(f.get(0)); hostId = f.get(1); }
            case "READY" -> ready.put(f.get(0), Boolean.parseBoolean(f.get(1)));
            case "START" -> {
                matchId = f.get(0); mode = BattleMode.valueOf(f.get(1));
                seconds = mode == BattleMode.TIMED ? Integer.parseInt(f.get(2)) : 180;
                if (seconds < 1 || seconds > 86400) throw new IllegalArgumentException("제한 시간 오류");
                elapsed = sinceState = stateSequence = attackSequence = 0;
                sentGameOver = opponentOver = resultSent = false; result = ""; error = ""; opponent = null;
                game = new Game(mode.settings(settings));
                game.setAttackListener(rows -> send("ATTACK", matchId, Long.toString(attackSequence++), BattleFrame.encodeRows(rows)));
                game.start(); sendState();
            }
            case "STATE" -> {
                if (!running() || !matchId.equals(f.get(0))) return;
                opponentId = f.get(1); opponent = BattleFrame.decode(f.get(3));
                opponentOver |= opponent.state() == GameState.GAME_OVER;
            }
            case "ATTACK" -> {
                if (running() && matchId.equals(f.get(0))) game.receiveAttack(BattleFrame.decodeRows(f.get(3)));
            }
            case "GAME_OVER" -> {
                if (running() && matchId.equals(f.get(0)) && !playerId.equals(f.get(1))) opponentOver = true;
            }
            case "RESULT" -> {
                if (!running() || !matchId.equals(f.get(0))) return;
                String winner = f.get(1);
                result = winner.equals("DRAW") ? "무승부" : winner.equals(playerId) ? profile + " 승리" : "상대 승리";
                game.pause(); matchId = ""; ready.clear();
            }
            case "ERROR" -> error = f.get(0);
            default -> { }
        }
    }
    public void act(GameAction action) {
        if (!running()) return;
        BattleMatch.apply(game, action); sendState();
    }
    private void sendState() {
        if (running()) send("STATE", matchId, Long.toString(stateSequence++), BattleFrame.of(game, profile).encode());
    }
    public boolean running() { return connected && !matchId.isEmpty(); }
    public boolean connected() { return connected; }
    public boolean welcomed() { return !playerId.isEmpty(); }
    public boolean isHost() { return !playerId.isEmpty() && playerId.equals(hostId); }
    public BattleMode mode() { return mode; }
    public int seconds() { return seconds; }
    public int players() { return players; }
    public boolean isReady() { return ready.getOrDefault(playerId, false); }
    public String lobbyStatus() { return "접속 " + players + "/2 · 준비 " + ready.values().stream().filter(Boolean::booleanValue).count() + "/2"; }
    public BattleFrame local() { return game == null ? null : BattleFrame.of(game, profile); }
    public BattleFrame opponent() { return opponent; }
    public String result() { return result; }
    public String error() { return error; }
    public long remainingMillis() { return Math.max(0, seconds * 1000L - elapsed); }
    public boolean lagging() { return client.isLagging(); }
    public long rtt() { return client.roundTripMillis(); }
    @Override public void close() { connected = false; client.close(); }
}
