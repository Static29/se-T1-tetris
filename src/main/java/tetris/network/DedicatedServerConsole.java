package tetris.network;

import java.io.Console;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/** Starts the account server from a Windows console without shell commands. */
public final class DedicatedServerConsole {
    private DedicatedServerConsole() {}

    public static void main(String[] args) throws InterruptedException {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("A console is required");
        try {
            String bind = read(console, "Bind address", "127.0.0.1");
            int port = Integer.parseInt(read(console, "Port", "7777"));
            if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be 1-65535");
            Path demo = Path.of(System.getProperty("java.io.tmpdir"), "tetris-account-demo", "server.p12");
            String keyFile = read(console, "Server PKCS12 file", Files.isRegularFile(demo) ? demo.toString() : "");
            char[] secret = console.readPassword("Certificate password: ");
            if (secret == null) return;
            javax.net.ssl.SSLContext tls;
            try {
                tls = DedicatedServerDesktop.loadCertificate(Path.of(keyFile), secret);
            } finally {
                Arrays.fill(secret, '\0');
            }
            Path accounts = Path.of(System.getProperty("user.home"), ".tetris", "server", "accounts.properties");
            try (var server = DedicatedServer.authenticated(InetAddress.getByName(bind), port,
                    ConnectionOptions.DEFAULT, tls, accounts)) {
                Runtime.getRuntime().addShutdownHook(new Thread(server::close));
                console.printf("Tetris TLS account server listening on %s:%d%n", bind, server.port());
                console.printf("Press Ctrl+C to stop the server.%n");
                new CountDownLatch(1).await();
            }
        } catch (java.io.IOException | java.security.GeneralSecurityException | IllegalArgumentException error) {
            console.printf("Server startup failed: %s%n", error.getMessage());
            console.readLine("Press Enter to close.");
        }
    }

    private static String read(Console console, String label, String fallback) {
        String value = console.readLine("%s%s: ", label, fallback.isEmpty() ? "" : " [" + fallback + "]");
        if (value == null) throw new IllegalArgumentException("Input closed");
        return value.isBlank() ? fallback : value.strip();
    }
}
