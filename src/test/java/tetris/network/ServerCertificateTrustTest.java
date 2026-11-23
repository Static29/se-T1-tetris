package tetris.network;

import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerCertificateTrustTest {
    @Test void bundledCertificateCoversConfiguredServerWithoutChangingGlobalTrust() throws Exception {
        SSLContext original = SSLContext.getDefault();
        var properties = new Properties();
        try (var input = getClass().getResourceAsStream("/tetris/dedicated-server.properties")) {
            assertNotNull(input);
            properties.load(input);
        }
        try (var input = getClass().getResourceAsStream("/tetris/dedicated-server.cer")) {
            assertNotNull(input);
            var certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
            certificate.checkValidity();
            assertTrue(certificate.getSubjectAlternativeNames().stream()
                    .anyMatch(name -> properties.getProperty("host").equals(name.get(1))));
        }
        assertNotNull(ServerCertificateTrust.bundled().getSocketFactory());
        assertSame(original, SSLContext.getDefault());
    }

    @Test void missingCertificateIsNotSilentlyTrusted() {
        assertThrows(java.io.IOException.class,
                () -> ServerCertificateTrust.fromFile(Path.of("nonexistent-tetris-certificate.cer")));
    }
}
