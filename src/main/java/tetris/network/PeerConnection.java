package tetris.network;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** 패키지 내부 전송 계층. 수신/송신/heartbeat를 UI 및 게임 스레드와 분리한다. */
final class PeerConnection implements AutoCloseable {
    private final Socket socket;
    private final ConnectionOptions options;
    private final Consumer<NetworkMessage> receiver;
    private final Consumer<String> disconnected;
    private final BlockingQueue<byte[]> outgoing = new ArrayBlockingQueue<>(128);
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    private volatile long lastReceived = System.nanoTime();
    private volatile long pingSent;
    private volatile long rttMs;
    private volatile boolean lagging;
    private Thread writer;

    PeerConnection(Socket socket, ConnectionOptions options, Consumer<NetworkMessage> receiver,
                   Consumer<String> disconnected) throws IOException {
        this.socket = socket; this.options = options; this.receiver = receiver; this.disconnected = disconnected;
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(options.disconnectMs());
    }
    void start() {
        timer.scheduleAtFixedRate(this::tick, 0, options.heartbeatMs(), TimeUnit.MILLISECONDS);
        writer = Thread.ofVirtual().name("tetris-send").start(() -> {
            try {
                var out = new BufferedOutputStream(socket.getOutputStream());
                while (!closed.get()) { out.write(outgoing.take()); out.flush(); }
            } catch (IOException | InterruptedException e) { stop("WRITE_FAILED"); }
        });
        Thread.ofVirtual().name("tetris-receive").start(() -> {
            try {
                var in = new BufferedInputStream(socket.getInputStream());
                while (!closed.get()) {
                    NetworkMessage message = MessageCodec.read(in);
                    lastReceived = System.nanoTime();
                    if (message.type().equals("PING") && message.fields().size() == 1)
                        send(NetworkMessage.of("PONG", message.fields().get(0)));
                    else if (message.type().equals("PONG") && message.fields().size() == 1) {
                        if (message.fields().get(0).equals(Long.toString(pingSent)) && pingSent != 0) {
                            rttMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - pingSent);
                            pingSent = 0;
                        }
                    } else receiver.accept(message);
                }
            } catch (IOException | RuntimeException e) { stop("READ_FAILED"); }
        });
    }
    private void tick() {
        if (closed.get()) return;
        long now = System.nanoTime();
        long silence = TimeUnit.NANOSECONDS.toMillis(now - lastReceived);
        long unanswered = pingSent == 0 ? 0 : TimeUnit.NANOSECONDS.toMillis(now - pingSent);
        lagging = Math.max(rttMs, Math.max(silence, unanswered)) >= options.lagMs();
        if (silence >= options.disconnectMs() || unanswered >= options.disconnectMs()) { stop("TIMEOUT"); return; }
        if (pingSent == 0) {
            pingSent = now;
            send(NetworkMessage.of("PING", Long.toString(now)));
        }
    }
    boolean send(NetworkMessage message) {
        if (closed.get()) return false;
        try {
            if (outgoing.offer(MessageCodec.encode(message))) return true;
            stop("SEND_QUEUE_FULL");
        } catch (IOException e) { stop("INVALID_OUTGOING_MESSAGE"); }
        return false;
    }
    long rttMs() { return rttMs; }
    boolean lagging() { return lagging; }
    private void stop(String reason) {
        if (!closed.compareAndSet(false, true)) return;
        timer.shutdownNow();
        try { socket.close(); } catch (IOException ignored) { }
        if (writer != null) writer.interrupt();
        disconnected.accept(reason);
    }
    @Override public void close() { stop("CLOSED"); }
}
