package tetris.network;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Creates connection-local trust from one explicitly selected public certificate. */
public final class ServerCertificateTrust {
    private ServerCertificateTrust() { }

    public static SSLContext fromFile(Path path) throws IOException, GeneralSecurityException {
        var factory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate;
        try (var input = Files.newInputStream(path)) {
            var certificates = factory.generateCertificates(input);
            if (certificates.size() != 1
                    || !(certificates.iterator().next() instanceof X509Certificate selected)) {
                throw new CertificateException("Expected one public X.509 certificate");
            }
            certificate = selected;
        }
        certificate.checkValidity();
        var store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setCertificateEntry("selected-server", certificate);
        var managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        managers.init(store);
        var context = SSLContext.getInstance("TLS");
        context.init(null, managers.getTrustManagers(), null);
        return context;
    }
}
