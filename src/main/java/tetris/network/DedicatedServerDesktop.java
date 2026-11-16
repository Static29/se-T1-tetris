package tetris.network;

import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.nio.file.*;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.*;
import javax.swing.*;

/** Windows server control window. The listening socket follows the window lifecycle. */
public final class DedicatedServerDesktop {
    private final JFrame frame = new JFrame("Tetris Dedicated Server");
    private final JTextField bind = new JTextField("127.0.0.1", 24);
    private final JTextField port = new JTextField("7777", 24);
    private final JTextField certificate = new JTextField(24);
    private final JPasswordField password = new JPasswordField(24);
    private final JButton browse = new JButton("찾아보기");
    private final JButton start = new JButton("서버 시작");
    private final JButton stop = new JButton("서버 종료");
    private final JLabel status = new JLabel("서버가 꺼져 있습니다.");
    private final Path accounts = Path.of(System.getProperty("user.home"), ".tetris", "server", "accounts.properties");
    private DedicatedServer server;
    private boolean starting;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DedicatedServerDesktop().show());
    }

    private void show() {
        Path demoKey = Path.of(System.getProperty("java.io.tmpdir"), "tetris-account-demo", "server.p12");
        if (Files.isRegularFile(demoKey)) certificate.setText(demoKey.toString());

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("바인드 주소"));
        form.add(bind);
        form.add(new JLabel("포트"));
        form.add(port);
        form.add(new JLabel("서버 인증서 (.p12)"));
        JPanel certField = new JPanel(new BorderLayout(6, 0));
        certField.add(certificate, BorderLayout.CENTER);
        certField.add(browse, BorderLayout.EAST);
        form.add(certField);
        form.add(new JLabel("인증서 파일 암호"));
        form.add(password);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(start);
        buttons.add(stop);
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        body.add(status, BorderLayout.NORTH);
        body.add(form, BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);

        browse.addActionListener(event -> {
            var chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PKCS12", "p12", "pfx"));
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                certificate.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        start.addActionListener(event -> start());
        stop.addActionListener(event -> stop());
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                if (starting) return;
                stop();
                frame.dispose();
            }
        });
        frame.setContentPane(body);
        frame.getRootPane().setDefaultButton(start);
        updateControls();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void start() {
        if (starting || server != null) return;
        String address = bind.getText().strip();
        String keyFile = certificate.getText().strip();
        int selectedPort;
        try {
            selectedPort = Integer.parseInt(port.getText().strip());
            if (selectedPort < 1 || selectedPort > 65535) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            status.setText("포트는 1~65535 사이의 숫자로 입력하세요.");
            return;
        }
        if (address.isEmpty() || keyFile.isEmpty()) {
            status.setText("바인드 주소와 서버 인증서 파일을 지정하세요.");
            return;
        }

        char[] secret = password.getPassword();
        password.setText("");
        starting = true;
        updateControls();
        status.setText("서버를 시작하는 중입니다.");
        new SwingWorker<DedicatedServer, Void>() {
            @Override protected DedicatedServer doInBackground() throws Exception {
                try {
                    SSLContext tls = loadCertificate(Path.of(keyFile), secret);
                    return DedicatedServer.authenticated(InetAddress.getByName(address), selectedPort,
                            ConnectionOptions.DEFAULT, tls, accounts);
                } finally {
                    Arrays.fill(secret, '\0');
                }
            }

            @Override protected void done() {
                try {
                    server = get();
                    status.setText("계정 서버 실행 중 · " + address + ":" + server.port());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    status.setText("서버 시작이 중단됐습니다.");
                } catch (ExecutionException error) {
                    status.setText("서버 시작에 실패했습니다.");
                    JOptionPane.showMessageDialog(frame, error.getCause().getMessage(),
                            "서버 시작 실패", JOptionPane.ERROR_MESSAGE);
                } finally {
                    starting = false;
                    updateControls();
                }
            }
        }.execute();
    }

    static SSLContext loadCertificate(Path path, char[] password)
            throws java.io.IOException, java.security.GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(path)) {
            keyStore.load(input, password);
        }
        boolean hasPrivateKey = false;
        for (var aliases = keyStore.aliases(); aliases.hasMoreElements();) {
            if (keyStore.entryInstanceOf(aliases.nextElement(), KeyStore.PrivateKeyEntry.class)) {
                hasPrivateKey = true;
                break;
            }
        }
        if (!hasPrivateKey) throw new IllegalArgumentException("개인 키가 들어 있는 서버 인증서 파일이 필요합니다.");
        var managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        managers.init(keyStore, password);
        var tls = SSLContext.getInstance("TLS");
        tls.init(managers.getKeyManagers(), null, null);
        return tls;
    }

    private void stop() {
        if (server != null) {
            server.close();
            server = null;
        }
        status.setText("서버가 꺼져 있습니다.");
        updateControls();
    }

    private void updateControls() {
        boolean editable = !starting && server == null;
        for (JComponent control : new JComponent[]{bind, port, certificate, password, browse, start}) {
            control.setEnabled(editable);
        }
        stop.setEnabled(server != null);
    }
}
