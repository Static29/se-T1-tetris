package tetris.network;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import javax.net.ssl.SSLContext;

/** 독립 실행 진입점. JavaFX/Gradle 플러그인/외부 서버 라이브러리가 필요 없다. */
public final class DedicatedServerMain {
    private DedicatedServerMain() { }
    public static void main(String[] args) throws Exception {
        String bind = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 7777;
        boolean roomMode = args.length > 2 && args[2].equals("--rooms");
        boolean auth = args.length > 2 && args[2].equals("--accounts");
        if (args.length > 2 && !roomMode && !auth || auth && args.length != 4
                || !auth && args.length > 3) {
            throw new IllegalArgumentException("Usage: [bind] [port] [--rooms | --accounts <accountFile>]");
        }
        if (auth && System.getProperty("javax.net.ssl.keyStore", "").isBlank()) {
            throw new IllegalArgumentException("Set javax.net.ssl.keyStore for account mode");
        }
        var address = InetAddress.getByName(bind);
        try (var server = auth
                ? DedicatedServer.authenticated(address, port, ConnectionOptions.DEFAULT,
                        SSLContext.getDefault(), Path.of(args[3]))
                : new DedicatedServer(address, port, ConnectionOptions.DEFAULT, roomMode)) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            String mode = auth ? "Tetris TLS account server" : roomMode ? "Tetris room server" : "1v1 Tetris server";
            System.out.println(mode + " listening on " + bind + ":" + server.port());
            System.out.println("Ctrl+C to stop. For LAN use your LAN address or 0.0.0.0.");
            new CountDownLatch(1).await();
        }
    }
}
