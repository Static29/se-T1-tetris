package tetris.network;

import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;

/** 독립 실행 진입점. JavaFX/Gradle 플러그인/외부 서버 라이브러리가 필요 없다. */
public final class DedicatedServerMain {
    private DedicatedServerMain() { }
    public static void main(String[] args) throws Exception {
        String bind = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 7777;
        try (var server = new DedicatedServer(InetAddress.getByName(bind), port, ConnectionOptions.DEFAULT)) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            System.out.println("1v1 Tetris server listening on " + bind + ":" + server.port());
            System.out.println("Ctrl+C to stop. For LAN use your LAN address or 0.0.0.0.");
            new CountDownLatch(1).await();
        }
    }
}
