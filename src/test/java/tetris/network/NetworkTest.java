package tetris.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(15)
class NetworkTest {
    private static final ConnectionOptions OPTIONS = new ConnectionOptions(2000, 100, 500, 3000);
    @TempDir Path temporary;

    private DedicatedServer server() throws IOException {
        return new DedicatedServer(InetAddress.getLoopbackAddress(), 0, OPTIONS);
    }
    private NetworkClient client(DedicatedServer server) throws IOException {
        return NetworkClient.connect(InetAddress.getLoopbackAddress().getHostAddress(), server.port(), OPTIONS);
    }
    private NetworkMessage await(NetworkClient client, String type) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (System.nanoTime() < deadline) {
            NetworkMessage event = client.poll(Duration.ofMillis(100));
            if (event != null && event.type().equals(type)) return event;
        }
        fail("Missing " + type + ", disconnected=" + client.disconnectReason());
        return null;
    }
    private String start(NetworkClient a, NetworkClient b) throws Exception {
        assertTrue(a.send(NetworkMessage.of("READY", "true")));
        assertTrue(b.send(NetworkMessage.of("READY", "true")));
        String id = await(a, "START").fields().get(0);
        assertEquals(id, await(b, "START").fields().get(0));
        return id;
    }

    @Test void codecHandlesFragmentedAndConcatenatedFrames() throws Exception {
        var first = NetworkMessage.of("STATE", "m1", "1", "한글\n아이템|L");
        var second = NetworkMessage.of("READY", "true");
        var bytes = new ByteArrayOutputStream();
        bytes.write(MessageCodec.encode(first)); bytes.write(MessageCodec.encode(second));
        InputStream fragmented = new FilterInputStream(new ByteArrayInputStream(bytes.toByteArray())) {
            @Override public int read(byte[] b, int off, int len) throws IOException {
                return super.read(b, off, Math.min(len, 1));
            }
        };
        assertEquals(first, MessageCodec.read(fragmented));
        assertEquals(second, MessageCodec.read(fragmented));
        assertThrows(EOFException.class, () -> MessageCodec.read(fragmented));
    }
    @Test void codecRejectsOversizedTruncatedAndWrongVersionFrames() throws Exception {
        assertThrows(IOException.class, () -> MessageCodec.encode(NetworkMessage.of("STATE", "a".repeat(65536))));
        assertThrows(IOException.class, () -> MessageCodec.read(new ByteArrayInputStream(new byte[]{127, -1, -1, -1})));
        byte[] valid = MessageCodec.encode(NetworkMessage.of("READY", "true"));
        assertThrows(IOException.class, () -> MessageCodec.read(new ByteArrayInputStream(java.util.Arrays.copyOf(valid, valid.length - 1))));
        valid[7] = 2;
        assertThrows(IOException.class, () -> MessageCodec.read(new ByteArrayInputStream(valid)));
    }
    @Test void twoPlayersConfigureStartRelayAndReplayWithoutReconnect() throws Exception {
        try (var server = server(); var a = client(server)) {
            String host = await(a, "WELCOME").fields().get(0);
            await(a, "CONFIG");
            try (var b = client(server)) {
                assertEquals(host, await(b, "WELCOME").fields().get(1));
                await(b, "CONFIG");
                a.send(NetworkMessage.of("CONFIG", "TIMED", "60"));
                assertEquals(java.util.List.of("TIMED", "60"), await(b, "CONFIG").fields());
                String match = start(a, b);
                a.send(NetworkMessage.of("STATE", match, "0", "snapshot"));
                assertEquals(java.util.List.of(match, host, "0", "snapshot"), await(b, "STATE").fields());
                b.send(NetworkMessage.of("GAME_OVER", match));
                assertEquals(match, await(a, "GAME_OVER").fields().get(0));
                a.send(NetworkMessage.of("RESULT", match, host));
                assertEquals(java.util.List.of(match, host), await(b, "RESULT").fields());
                await(a, "RESULT");
                String next = start(a, b);
                assertNotEquals(match, next);
                a.send(NetworkMessage.of("STATE", match, "1", "old"));
                assertEquals("STALE_MATCH", await(a, "ERROR").fields().get(0));
            }
        }
    }
    @Test void nonHostCannotConfigureOrPublishResultAndChatIsUnsupported() throws Exception {
        try (var server = server(); var a = client(server)) {
            await(a, "WELCOME");
            try (var b = client(server)) {
                await(b, "WELCOME");
                b.send(NetworkMessage.of("CONFIG", "ITEM", "0"));
                assertEquals("HOST_ONLY", await(b, "ERROR").fields().get(0));
                b.send(NetworkMessage.of("CHAT", "not part of requirements"));
                assertEquals("UNSUPPORTED_COMMAND", await(b, "ERROR").fields().get(0));
                String match = start(a, b);
                b.send(NetworkMessage.of("RESULT", match, "DRAW"));
                assertEquals("HOST_ONLY", await(b, "ERROR").fields().get(0));
            }
        }
    }
    @Test void duplicateAttackIsNotRelayedTwiceAndMalformedRowsAreRejected() throws Exception {
        try (var server = server(); var a = client(server)) {
            await(a, "WELCOME");
            try (var b = client(server)) {
                await(b, "WELCOME"); String match = start(a, b);
                var attack = NetworkMessage.of("ATTACK", match, "0", "1111001111/1110011111");
                a.send(attack); a.send(attack);
                a.send(NetworkMessage.of("STATE", match, "0", "barrier"));
                assertEquals("1111001111/1110011111", await(b, "ATTACK").fields().get(3));
                // 같은 TCP 경로의 다음 메시지가 STATE이면 중복 공격이 전송되지 않은 것이다.
                assertEquals("STATE", b.poll(Duration.ofSeconds(2)).type());
                a.send(NetworkMessage.of("ATTACK", match, "1", "111"));
                assertEquals("INVALID_ATTACK_ROWS", await(a, "ERROR").fields().get(0));
            }
        }
    }
    @Test void thirdPlayerIsRejected() throws Exception {
        try (var server = server(); var a = client(server)) {
            await(a, "WELCOME");
            try (var b = client(server)) {
                await(b, "WELCOME");
                try (var third = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
                    third.setSoTimeout(2000);
                    assertEquals(-1, third.getInputStream().read());
                }
            }
        }
    }
    @Test void missingHeartbeatClosesConnectionAndServerCanAcceptAgain() throws Exception {
        var fast = new ConnectionOptions(1000, 50, 150, 400);
        try (var server = new DedicatedServer(InetAddress.getLoopbackAddress(), 0, fast);
             var raw = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
            raw.setSoTimeout(2000);
            while (raw.getInputStream().read() != -1) { /* ignore PING, wait for EOF */ }
            try (var next = client(server)) { assertNotNull(await(next, "WELCOME")); }
        }
    }
    @Test void abruptDisconnectClosesOpponentSession() throws Exception {
        try (var server = server(); var a = client(server)) {
            await(a, "WELCOME");
            try (var b = client(server)) {
                await(b, "WELCOME"); a.close();
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (b.disconnectReason() == null && System.nanoTime() < deadline) b.poll(Duration.ofMillis(20));
                assertNotNull(b.disconnectReason());
            }
        }
    }
    @Test void heartbeatKeepsIdleLobbyConnected() throws Exception {
        try (var server = new DedicatedServer(InetAddress.getLoopbackAddress(), 0,
                new ConnectionOptions(1000, 50, 200, 600)); var a = client(server)) {
            await(a, "WELCOME");
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(900);
            while (System.nanoTime() < deadline) a.poll(Duration.ofMillis(50));
            assertNull(a.disconnectReason());
            a.send(NetworkMessage.of("CONFIG", "ITEM", "0"));
            NetworkMessage config;
            do { config = await(a, "CONFIG"); } while (!config.fields().get(0).equals("ITEM"));
        }
    }
    @Test void recentAddressPersistsAndInvalidValuesDoNotOverwrite() throws Exception {
        var store = new RecentServerStore(temporary.resolve("config/recent-ip.txt"));
        assertTrue(store.load().isEmpty());
        store.save("192.168.0.12");
        assertEquals("192.168.0.12", store.load().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> store.save("bad\naddress"));
        assertEquals("192.168.0.12", store.load().orElseThrow());
    }
    @Test void invalidConnectionOptionsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ConnectionOptions(1, 100, 50, 200));
        assertThrows(IllegalArgumentException.class, () -> new ConnectionOptions(0, 100, 200, 300));
    }

    @Test void clientReportsLagThenDisconnectForUnresponsiveServer() throws Exception {
        var fast = new ConnectionOptions(1000, 50, 150, 800);
        try (var rawServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             var client = NetworkClient.connect(InetAddress.getLoopbackAddress().getHostAddress(), rawServer.getLocalPort(), fast);
             var ignoredPeer = rawServer.accept()) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            boolean observedLag = false;
            while (client.disconnectReason() == null && System.nanoTime() < deadline) {
                observedLag |= client.isLagging();
                client.poll(Duration.ofMillis(10));
            }
            assertTrue(observedLag, "Lag should be visible before disconnect");
            assertNotNull(client.disconnectReason());
        }
    }

    @Test void oneReadyPlayerDoesNotStartAndConfigResetsReadiness() throws Exception {
        try (var server = server(); var a = client(server)) {
            await(a, "WELCOME");
            try (var b = client(server)) {
                await(b, "WELCOME"); await(b, "CONFIG");
                a.send(NetworkMessage.of("READY", "true"));
                a.send(NetworkMessage.of("CONFIG", "ITEM", "0"));
                assertEquals("ITEM", await(b, "CONFIG").fields().get(0));
                b.send(NetworkMessage.of("READY", "true"));
                // READY와 같은 스트림에 보낸 잘못된 STATE에 대한 응답으로 처리 완료를 확인한다.
                b.send(NetworkMessage.of("STATE", "m1", "0", "not started"));
                assertEquals("STALE_MATCH", await(b, "ERROR").fields().get(0));
                a.send(NetworkMessage.of("READY", "true"));
                assertEquals("ITEM", await(b, "START").fields().get(1));
            }
        }
    }
}
