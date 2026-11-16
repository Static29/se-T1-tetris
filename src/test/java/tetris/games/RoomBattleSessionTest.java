package tetris.games;

import java.net.InetAddress;
import java.time.Duration;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import tetris.network.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(15)
class RoomBattleSessionTest {
    @Test void fourRealClientsStartEliminateResizeFinishAndRematch() throws Exception {
        try (var server = new DedicatedServer(InetAddress.getLoopbackAddress(), 0, ConnectionOptions.DEFAULT, true)) {
            var clients = new ArrayList<NetworkClient>(); var sessions = new ArrayList<RoomBattleSession>();
            try {
                for (int i = 0; i < 4; i++) {
                    var client = NetworkClient.connect("localhost", server.port(), ConnectionOptions.DEFAULT); clients.add(client);
                    sessions.add(new RoomBattleSession(GameSettings.DEFAULT, m -> assertTrue(client.send(m))));
                }
                await(clients, sessions, () -> sessions.stream().allMatch(RoomBattleSession::welcomed));
                var host = sessions.getFirst(); host.create(4, BattleMode.ITEM, Difficulty.HARD, 180);
                await(clients, sessions, host::inRoom);
                for (var session : sessions.subList(1, 4)) session.join(host.roomId());
                await(clients, sessions, () -> sessions.stream().allMatch(s -> s.members().size() == 4));
                sessions.forEach(RoomBattleSession::toggleReady);
                await(clients, sessions, () -> sessions.stream().allMatch(s -> s.visibleFrames().size() == 4));
                assertTrue(host.rulesLabel().contains("아이템"));
                for (int index = 3; index >= 1; index--) {
                    var loser = sessions.get(index);
                    for (int i = 0; i < 100 && loser.playing(); i++) loser.act(GameAction.HARD_DROP);
                    int count = index;
                    await(clients, sessions, () -> host.visibleFrames().size() == count);
                    assertFalse(loser.playing());
                }
                await(clients, sessions, () -> sessions.stream().allMatch(s -> !s.winner().isEmpty()));
                assertEquals(host.playerId(), host.winner());
                assertEquals(List.of(1, 2, 3, 4), host.standings().stream().map(RoomBattleSession.Standing::rank).toList());
                assertEquals(1, host.members().stream().filter(m -> m.id().equals(host.playerId())).findFirst().orElseThrow().sessionWins());
                sessions.forEach(RoomBattleSession::dismissResult); sessions.forEach(RoomBattleSession::toggleReady);
                await(clients, sessions, () -> sessions.stream().allMatch(s -> s.visibleFrames().size() == 4 && s.playing()));
                assertEquals(0, host.local().score());
                sessions.get(3).leave();
                await(clients, sessions, () -> !sessions.get(3).inRoom() && host.visibleFrames().size() == 3);
            } finally { clients.forEach(NetworkClient::close); }
        }
    }
    @Test void timedServerRequestsFinalScoresAndReturnsToRoom() throws Exception {
        try (var server = new DedicatedServer(InetAddress.getLoopbackAddress(), 0, ConnectionOptions.DEFAULT, true);
             var a = NetworkClient.connect("localhost", server.port(), ConnectionOptions.DEFAULT);
             var b = NetworkClient.connect("localhost", server.port(), ConnectionOptions.DEFAULT)) {
            var sa = new RoomBattleSession(GameSettings.DEFAULT, a::send); var sb = new RoomBattleSession(GameSettings.DEFAULT, b::send);
            var clients = List.of(a, b); var sessions = List.of(sa, sb);
            await(clients, sessions, () -> sa.welcomed() && sb.welcomed());
            sa.create(2, BattleMode.TIMED, Difficulty.EASY, 1); await(clients, sessions, sa::inRoom);
            sb.join(sa.roomId()); await(clients, sessions, () -> sa.members().size() == 2 && sb.members().size() == 2);
            sessions.forEach(RoomBattleSession::toggleReady); await(clients, sessions, () -> sa.playing() && sb.playing());
            sa.act(GameAction.HARD_DROP);
            await(clients, sessions, () -> !sa.winner().isEmpty() && !sb.winner().isEmpty());
            assertEquals(sa.playerId(), sa.winner()); assertTrue(sa.inRoom()); assertFalse(sa.running());
            sa.dismissResult(); assertTrue(sa.winner().isEmpty());
        }
    }
    @Test void validatesRulesAndNicknamesAndIgnoresDuplicateOrStaleAttack() {
        var sent = new ArrayList<NetworkMessage>(); var session = started(sent);
        assertThrows(IllegalArgumentException.class, () -> session.create(5, BattleMode.NORMAL, Difficulty.NORMAL, 1));
        assertThrows(IllegalArgumentException.class, () -> session.join("bad"));
        var attack = NetworkMessage.of("ATTACK", "m1", "p2", "0", "1111111110/1111111110");
        session.receive(attack); session.receive(attack);
        assertEquals(2, session.local().pending().size());
        session.receive(NetworkMessage.of("ATTACK", "old", "p2", "1", "1111111110/1111111110"));
        assertEquals(2, session.local().pending().size());
        for (int i = 1; i < 5; i++) session.receive(NetworkMessage.of("ATTACK", "m1", "p2", "" + i, "1111111110/1111111110"));
        assertEquals(10, session.local().pending().size());
        assertTrue(sent.stream().anyMatch(m -> m.type().equals("QUEUE_SPACE") && m.fields().get(1).equals("false")));
        session.act(GameAction.HARD_DROP);
        assertTrue(sent.stream().anyMatch(m -> m.type().equals("QUEUE_SPACE") && m.fields().get(1).equals("true")));
        var game = new Game(); game.start();
        var frame = BattleFrame.of(game, "가짜닉네임");
        session.receive(NetworkMessage.of("STATE", "m1", "p2", "1", frame.encode()));
        assertEquals("상대닉네임", session.visibleFrames().get(1).name());
        assertEquals("닉네임", BattleFrame.decode(BattleFrame.of(game, "닉네임").encode()).name());
        assertThrows(IllegalArgumentException.class, () -> BattleFrame.of(game, "\n"));
        session.receive(NetworkMessage.of("ROOM_PLAYER_LEFT", "p2"));
        session.receive(NetworkMessage.of("ELIMINATED", "m1", "p2"));
        assertEquals("상대닉네임", session.name("p2"));
        session.receive(NetworkMessage.of("RESULT", "m1", "p1"));
        assertEquals(2, session.standings().size());
        session.receive(NetworkMessage.of("ERROR", "STALE_MATCH"));
        assertEquals("", session.error());
    }
    private static RoomBattleSession started(List<NetworkMessage> sent) {
        var session = new RoomBattleSession(GameSettings.DEFAULT, sent::add);
        session.receive(NetworkMessage.of("ROOM_WELCOME", "p1"));
        session.receive(NetworkMessage.of("ROOM_JOINED", "1", "2", "DUEL", "0", "false", "NORMAL"));
        session.receive(NetworkMessage.of("PLAYER_NAME", "p1", "내닉네임"));
        session.receive(NetworkMessage.of("PLAYER_NAME", "p2", "상대닉네임"));
        session.receive(NetworkMessage.of("ROOM_START", "m1", "DUEL", "0", "false", "NORMAL"));
        return session;
    }
    private static void await(List<NetworkClient> clients, List<RoomBattleSession> sessions, BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(7).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            for (int i = 0; i < clients.size(); i++) {
                NetworkMessage message;
                while ((message = clients.get(i).poll(Duration.ZERO)) != null) sessions.get(i).receive(message);
                assertEquals("", sessions.get(i).error());
            }
            Thread.sleep(5);
        }
        assertTrue(condition.getAsBoolean(), "서버 상태 전환 시간 초과");
    }
}
