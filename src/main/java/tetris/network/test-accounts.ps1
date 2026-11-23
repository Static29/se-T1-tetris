$ErrorActionPreference = 'Stop'
$work = Join-Path ([IO.Path]::GetTempPath()) ('tetris-account-test-' + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $work | Out-Null
try {
    $key = Join-Path $work 'local-test.p12'
    & keytool -genkeypair -alias local-test -keyalg RSA -dname CN=localhost -ext SAN=dns:localhost -validity 1 -storetype PKCS12 -keystore $key -storepass localtestonly
    if ($LASTEXITCODE -ne 0) { throw 'Test certificate generation failed.' }
    $certificate = Join-Path $work 'local-test.cer'
    & keytool -exportcert -alias local-test -keystore $key -storepass localtestonly -file $certificate
    if ($LASTEXITCODE -ne 0) { throw 'Test public certificate export failed.' }
    $resourceDirectory = Join-Path $work 'tetris'
    New-Item -ItemType Directory -Path $resourceDirectory | Out-Null
    Copy-Item -LiteralPath $certificate -Destination (Join-Path $resourceDirectory 'dedicated-server.cer')
    $source = Join-Path $work 'AccountSmoke.java'
    $javaSource = @'
import java.net.InetAddress;
import java.nio.file.*;
import java.security.KeyStore;
import java.time.Duration;
import javax.net.ssl.*;
import tetris.network.*;

public class AccountSmoke {
    private static final ConnectionOptions OPTIONS = ConnectionOptions.DEFAULT;
    private static final String OLD_PASSWORD = "Test8pwd";
    private static final String NEW_PASSWORD = "Next8pwd";

    public static void main(String[] args) throws Exception {
        var keys = KeyStore.getInstance("PKCS12");
        var password = "localtestonly".toCharArray();
        try (var input = Files.newInputStream(Path.of(args[0]))) {
            keys.load(input, password);
        }
        var keyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManager.init(keys, password);
        var serverTls = SSLContext.getInstance("TLS");
        serverTls.init(keyManager.getKeyManagers(), null, null);

        var clientTls = ServerCertificateTrust.bundled();
        ServerCertificateTrust.fromFile(Path.of(args[2]));
        try {
            ServerCertificateTrust.fromFile(Path.of(args[0]));
            throw new AssertionError("Server key store accepted as a public certificate");
        } catch (java.security.cert.CertificateException expected) { }
        Path accounts = Path.of(args[1]);
        var loopback = InetAddress.getByName("127.0.0.1");

        try (var server = DedicatedServer.authenticated(loopback, 0, OPTIONS, serverTls, accounts)) {
            rejectHandshake("localhost", server.port(), SSLContext.getDefault());
            rejectHandshake("127.0.0.1", server.port(), clientTls);
            System.out.println("PASS: untrusted certificate and mismatched address rejected");
        }

        try (var server = DedicatedServer.authenticated(loopback, 0, OPTIONS, serverTls, accounts);
             var client = NetworkClient.connectTls("localhost", server.port(), OPTIONS, clientTls);
             var guest = NetworkClient.connectTls("localhost", server.port(), OPTIONS, clientTls)) {
            expect(client, "AUTH_REQUIRED");
            expect(guest, "AUTH_REQUIRED");
            send(guest, "REGISTER", "smoke_guest", "SmokeGuest", OLD_PASSWORD);
            expect(guest, "REGISTERED");
            send(client, "CREATE_ROOM", "2", "DUEL", "0", "false", "NORMAL");
            expect(client, "ERROR", "LOGIN_REQUIRED");
            send(client, "REGISTER", "smoke_player", "SmokePlayer", "Short7x");
            expect(client, "ERROR", "INVALID_PASSWORD_LENGTH");
            Thread.sleep(1100);
            send(client, "REGISTER", "smoke_player", "SmokePlayer", OLD_PASSWORD);
            expect(client, "REGISTERED");
            System.out.println("PASS: registration and login gate");
            Thread.sleep(1100);
            send(client, "LOGIN", "smoke_player", OLD_PASSWORD);
            loginAccepted(client);
            send(client, "CREATE_ROOM", "2", "DUEL", "0", "false", "NORMAL");
            String roomId = expect(client, "ROOM_JOINED").fields().getFirst();
            expect(client, "ROOM_LOBBY");
            expect(client, "PLAYER_NAME");
            expect(client, "READY");
            System.out.println("PASS: login and room creation");
            send(guest, "LOGIN", "smoke_guest", OLD_PASSWORD);
            loginAccepted(guest);
            send(guest, "JOIN_ROOM", roomId);
            expect(guest, "ROOM_JOINED");
            send(client, "READY", "true");
            send(guest, "READY", "true");
            String matchId = await(client, "ROOM_START").fields().getFirst();
            if (!await(guest, "ROOM_START").fields().getFirst().equals(matchId)) {
                throw new AssertionError("Clients received different match IDs");
            }
            System.out.println("PASS: room join, readiness and shared match start");
            Thread.sleep(1100);
            send(client, "CHANGE_PASSWORD", OLD_PASSWORD, NEW_PASSWORD);
            expect(client, "PASSWORD_CHANGED");
        }
        try (var server = DedicatedServer.authenticated(loopback, 0, OPTIONS, serverTls, accounts);
             var client = NetworkClient.connectTls("localhost", server.port(), OPTIONS, clientTls)) {
            expect(client, "AUTH_REQUIRED");
            send(client, "LOGIN", "smoke_player", OLD_PASSWORD);
            expect(client, "ERROR", "INVALID_CREDENTIALS");
            Thread.sleep(1100);
            send(client, "LOGIN", "smoke_player", NEW_PASSWORD);
            loginAccepted(client);
            System.out.println("PASS: changed password persisted across server restart");
        }
    }

    static void rejectHandshake(String host, int port, SSLContext tls) throws Exception {
        try (var client = NetworkClient.connectTls(host, port, OPTIONS, tls)) {
            throw new AssertionError("Unexpected TLS connection: " + host);
        } catch (SSLHandshakeException expected) { }
    }

    static void loginAccepted(NetworkClient client) throws Exception {
        expect(client, "LOGGED_IN");
        expect(client, "ROOM_WELCOME");
    }

    static void send(NetworkClient client, String type, String... fields) {
        if (!client.send(NetworkMessage.of(type, fields))) throw new AssertionError("Send failed: " + type);
    }

    static NetworkMessage await(NetworkClient client, String type) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (System.nanoTime() - deadline < 0) {
            var event = client.poll(Duration.ofMillis(100));
            if (event == null) continue;
            if (event.type().equals(type)) return event;
            if (event.type().equals("ERROR")) throw new AssertionError(event);
        }
        throw new AssertionError("Missing " + type);
    }

    static NetworkMessage expect(NetworkClient client, String type, String... fields) throws Exception {
        var event = client.poll(Duration.ofSeconds(8));
        if (event == null || !event.type().equals(type)
                || (fields.length > 0 && !event.fields().equals(java.util.List.of(fields)))) {
            throw new AssertionError("Expected " + type + ", received " + event);
        }
        return event;
    }
}
'@
    [IO.File]::WriteAllText($source, $javaSource, [Text.UTF8Encoding]::new($false))
    $networkSources = @(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.java' | ForEach-Object FullName)
    & javac --release 21 -encoding UTF-8 -d $work @networkSources $source
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
    & java -cp $work AccountSmoke $key (Join-Path $work 'accounts.properties') $certificate
    if ($LASTEXITCODE -ne 0) { throw 'Account smoke test failed.' }
} finally {
    $resolved = (Resolve-Path -LiteralPath $work).Path
    $parent = (Resolve-Path -LiteralPath ([IO.Path]::GetTempPath())).Path
    if ((Split-Path -Parent $resolved) -eq $parent -and (Split-Path -Leaf $resolved) -match '^tetris-account-test-[0-9a-f-]{36}$') {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
