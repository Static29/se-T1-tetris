package tetris.games;

import java.net.InetAddress;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import tetris.network.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class NetworkBattleSessionTest {
    private static final ConnectionOptions OPTIONS = new ConnectionOptions(2000, 100, 500, 3000);
    private void await(BooleanSupplier condition, NetworkBattleSession a, NetworkBattleSession b) throws Exception {
        long deadline = System.nanoTime() + 4_000_000_000L;
        do {
            a.update(0); b.update(0);
            if (condition.getAsBoolean()) return;
            Thread.sleep(5);
        } while (System.nanoTime() < deadline);
        fail("상태 대기 실패: " + a.error() + " / " + b.error());
    }
    @Test void realServerStartStateResultReplayAndDisconnect() throws Exception {
        try (var server = new DedicatedServer(InetAddress.getLoopbackAddress(), 0, OPTIONS);
             var a = new NetworkBattleSession(NetworkClient.connect("127.0.0.1", server.port(), OPTIONS), GameSettings.DEFAULT, "PLAYER1");
             var b = new NetworkBattleSession(NetworkClient.connect("127.0.0.1", server.port(), OPTIONS), GameSettings.DEFAULT, "PLAYER2")) {
            await(() -> a.players() == 2 && b.players() == 2 && a.welcomed() && b.welcomed(), a, b);
            var host = a.isHost() ? a : b;
            host.configure(BattleMode.TIMED, 1);
            await(() -> a.mode() == BattleMode.TIMED && b.mode() == BattleMode.TIMED, a, b);
            a.ready(); b.ready();
            await(() -> a.running() && b.running() && a.opponent() != null && b.opponent() != null, a, b);
            long before = System.nanoTime(); a.act(GameAction.SOFT_DROP);
            await(() -> b.opponent().score() == 10, a, b);
            assertTrue(System.nanoTime() - before < 200_000_000L, "루프백 입력→상대 상태 200ms 이내");
            a.update(1000); b.update(1000);
            await(() -> !a.result().isEmpty() && !b.result().isEmpty(), a, b);
            assertEquals("PLAYER1 승리", a.result()); assertEquals("상대 승리", b.result());
            assertFalse(a.running()); assertFalse(b.running());
            host.configure(BattleMode.ITEM, 180);
            await(() -> a.mode() == BattleMode.ITEM && b.mode() == BattleMode.ITEM, a, b);
            a.ready(); b.ready();
            await(() -> a.running() && b.running() && a.opponent() != null, a, b);
            assertEquals(0, a.local().score()); assertEquals("", a.result());
            b.close(); await(() -> !a.connected(), a, b);
            assertFalse(a.error().isEmpty());
        }
    }
}
