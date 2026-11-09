package tetris.network;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.concurrent.*;

/** UI 독립 클라이언트. connect는 백그라운드에서, poll은 UI/게임 어댑터에서 사용한다. */
public final class NetworkClient implements AutoCloseable {
    private final BlockingQueue<NetworkMessage> events = new ArrayBlockingQueue<>(256);
    private final PeerConnection connection;
    private volatile String disconnectReason;

    private NetworkClient(Socket socket, ConnectionOptions options) throws IOException {
        connection = new PeerConnection(socket, options, this::received, reason -> disconnectReason = reason);
        connection.start();
    }
    public static NetworkClient connect(String address, int port, ConnectionOptions options) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(address, port), options.connectTimeoutMs());
            return new NetworkClient(socket, options);
        } catch (IOException | RuntimeException e) { socket.close(); throw e; }
    }
    private void received(NetworkMessage event) {
        if (!events.offer(event)) throw new IllegalStateException("Receive queue full");
    }
    /** UI가 이벤트를 소비하지 않아 큐가 가득 차면 연결을 닫는다. 상태를 조용히 유실하지 않는다. */
    public NetworkMessage poll(Duration timeout) throws InterruptedException {
        return events.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
    public boolean send(NetworkMessage message) { return connection.send(message); }
    public boolean isLagging() { return connection.lagging(); }
    public long roundTripMillis() { return connection.rttMs(); }
    public String disconnectReason() { return disconnectReason; }
    @Override public void close() { connection.close(); }
}
