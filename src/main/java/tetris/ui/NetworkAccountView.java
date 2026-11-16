package tetris.ui;

import java.nio.file.Path;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import tetris.network.ConnectionOptions;
import tetris.network.NetworkClient;
import tetris.network.NetworkMessage;
import tetris.network.ServerCertificateTrust;

/** Account entry screen for the dedicated TLS server. */
final class NetworkAccountView extends VBox {
    private final Properties endpointSettings = loadEndpoint();
    private final TextField loginId = new TextField();
    private final TextField displayName = new TextField();
    private final PasswordField password = new PasswordField();
    private final PasswordField confirmation = new PasswordField();
    private final Label status = new Label("계정 서버에 연결하는 중…");
    private final Button connect = new Button("다시 연결");
    private final Button chooseCertificate = new Button("공개 인증서 선택");
    private final Button clearCertificate = new Button("해제");
    private final Label certificateLabel = new Label("기본 인증서 사용");
    private final Button login = new Button("로그인");
    private final Button register = new Button("회원가입");
    private final Button disconnect = new Button("연결 종료");
    private final VBox credentials = new VBox(8);
    private final AnimationTimer timer = new AnimationTimer() {
        @Override public void handle(long now) { update(); }
    };

    private NetworkClient client;
    private Path certificatePath;
    private boolean closed;
    private boolean connecting;
    private boolean authReady;
    private boolean signedIn;
    private String expected = "";
    private long deadline;

    NetworkAccountView(Runnable back) {
        super(14);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #101010;");
        setFocusTraversable(true);
        loginId.setPromptText("로그인 ID");
        displayName.setPromptText("표시 이름 (가입 시)");
        password.setPromptText("비밀번호");
        confirmation.setPromptText("비밀번호 확인 (가입 시)");
        status.setStyle("-fx-text-fill: white;");
        status.setWrapText(true);
        status.setMaxWidth(520);

        certificateLabel.setStyle("-fx-text-fill: white;");
        certificateLabel.setMaxWidth(260);
        var certificate = new HBox(8, chooseCertificate, certificateLabel, clearCertificate);
        certificate.setAlignment(Pos.CENTER);
        var actions = new HBox(8, login, register);
        actions.setAlignment(Pos.CENTER);
        credentials.setMaxWidth(320);
        credentials.getChildren().addAll(loginId, displayName, password, confirmation, actions);
        var goBack = new Button("돌아가기");
        getChildren().addAll(BattleBoardPane.text("네트워크 계정", 26),
                certificate, credentials, status, connect, disconnect, goBack);

        connect.setOnAction(event -> connect());
        chooseCertificate.setOnAction(event -> {
            var chooser = new FileChooser();
            chooser.setTitle("서버에서 받은 공개 인증서 선택");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "공개 인증서", "*.cer", "*.crt", "*.pem", "*.der"));
            var file = chooser.showOpenDialog(getScene().getWindow());
            if (file != null) {
                certificatePath = file.toPath();
                certificateLabel.setText(file.getName());
                connect();
            }
        });
        clearCertificate.setOnAction(event -> {
            certificatePath = null;
            certificateLabel.setText("기본 인증서 사용");
            connect();
        });
        login.setOnAction(event -> submit(false));
        register.setOnAction(event -> submit(true));
        password.setOnAction(event -> submit(false));
        confirmation.setOnAction(event -> submit(true));
        disconnect.setOnAction(event -> reset("연결을 종료했습니다."));
        goBack.setOnAction(event -> { close(); back.run(); });
        sceneProperty().addListener((observable, previous, current) -> {
            if (current == null) close();
            else {
                timer.start();
                connect();
            }
        });
        refresh();
    }

    private static Properties loadEndpoint() {
        var properties = new Properties();
        try (var input = NetworkAccountView.class.getResourceAsStream("/tetris/dedicated-server.properties")) {
            if (input != null) properties.load(input);
        } catch (IOException | IllegalArgumentException e) {
            properties.clear();
        }
        return properties;
    }

    private void connect() {
        if (closed || connecting || client != null) return;
        String host = endpointSettings.getProperty("host", "").strip();
        int serverPort;
        try {
            serverPort = Integer.parseInt(endpointSettings.getProperty("port", "7777").strip());
            if (serverPort < 1 || serverPort > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            status.setText("배포 설정의 서버 포트가 올바르지 않습니다.");
            return;
        }
        if (host.isEmpty() || host.length() > 253 || host.chars().anyMatch(Character::isWhitespace)) {
            status.setText("배포 설정에 서버 주소가 없습니다.");
            return;
        }
        connecting = true;
        Path selectedCertificate = certificatePath;
        status.setText("보안 연결 중…");
        refresh();
        Thread.ofVirtual().name("account-connect").start(() -> {
            SSLContext tls;
            try {
                tls = selectedCertificate == null ? SSLContext.getDefault()
                        : ServerCertificateTrust.fromFile(selectedCertificate);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (!closed) reset("공개 인증서를 읽을 수 없습니다. 유효한 서버 인증서 파일을 선택하세요.");
                });
                return;
            }
            try {
                var connection = NetworkClient.connectTls(host, serverPort, ConnectionOptions.DEFAULT, tls);
                Platform.runLater(() -> {
                    if (closed) {
                        connection.close();
                        return;
                    }
                    client = connection;
                    connecting = false;
                    expect("AUTH_REQUIRED");
                    status.setText("서버 응답을 기다리는 중…");
                    refresh();
                });
            } catch (Exception e) {
                String message = e instanceof SSLHandshakeException
                        ? "서버에서 받은 공개 인증서와 인증서에 등록된 접속 주소를 확인하세요."
                        : "서버에 연결할 수 없습니다. 서버 실행 상태를 확인한 뒤 다시 연결하세요.";
                Platform.runLater(() -> { if (!closed) reset(message); });
            }
        });
    }

    private void submit(boolean signingUp) {
        if (!authReady || signedIn || !expected.isEmpty()) return;
        String id = loginId.getText().strip();
        String secret = password.getText();
        String name = displayName.getText().strip();
        if (id.isEmpty() || secret.isEmpty() || signingUp && name.isEmpty()) {
            status.setText(signingUp ? "로그인 ID, 표시 이름, 비밀번호를 입력하세요." : "로그인 ID와 비밀번호를 입력하세요.");
            return;
        }
        if (signingUp && !secret.equals(confirmation.getText())) {
            status.setText("비밀번호 확인이 일치하지 않습니다.");
            return;
        }
        NetworkMessage message = signingUp ? NetworkMessage.of("REGISTER", id, name, secret)
                : NetworkMessage.of("LOGIN", id, secret);
        password.clear();
        confirmation.clear();
        if (!client.send(message)) {
            reset("계정 요청을 전송할 수 없습니다.");
            return;
        }
        expect(signingUp ? "REGISTERED" : "LOGGED_IN");
        status.setText(signingUp ? "회원가입 요청 중…" : "로그인 요청 중…");
        refresh();
    }

    private void expect(String type) {
        expected = type;
        deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
    }

    private void update() {
        if (client == null) return;
        if (client.disconnectReason() != null) {
            reset("서버 연결이 종료됐습니다.");
            return;
        }
        try {
            for (int i = 0; i < 32 && client != null; i++) {
                var event = client.poll(Duration.ZERO);
                if (event == null) break;
                receive(event);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            reset("서버 응답을 읽지 못했습니다.");
        }
        if (!expected.isEmpty() && System.nanoTime() - deadline >= 0) {
            reset("서버 응답 시간이 초과됐습니다.");
        }
    }

    private void receive(NetworkMessage message) {
        var fields = message.fields();
        if (message.type().equals("ERROR") && authReady && !signedIn) {
            expected = "";
            status.setText(fields.size() == 1 ? errorText(fields.getFirst()) : "계정 요청에 실패했습니다.");
        } else if (message.type().equals(expected)) {
            expected = "";
            switch (message.type()) {
                case "AUTH_REQUIRED" -> {
                    authReady = true;
                    status.setText("연결됐습니다. 로그인하거나 새 계정을 만드세요.");
                    loginId.requestFocus();
                }
                case "REGISTERED" -> {
                    displayName.clear();
                    status.setText("가입했습니다. 비밀번호를 다시 입력하고 로그인하세요.");
                    password.requestFocus();
                }
                case "LOGGED_IN" -> {
                    if (fields.size() != 2) {
                        reset("로그인 응답 형식이 올바르지 않습니다.");
                        return;
                    }
                    signedIn = true;
                    status.setText(fields.get(1) + " 님, 로그인했습니다.");
                }
                default -> { }
            }
        } else if (expected.equals("AUTH_REQUIRED")) {
            reset("이 주소에서 계정 서버 응답을 받을 수 없습니다.");
        }
        refresh();
    }

    private static String errorText(String code) {
        return switch (code) {
            case "INVALID_LOGIN_ID" -> "로그인 ID는 영문·숫자·밑줄로 4~20자를 입력하세요.";
            case "INVALID_DISPLAY_NAME" -> "표시 이름은 문자와 숫자로 2~12자를 입력하세요.";
            case "INVALID_PASSWORD_LENGTH" -> "비밀번호는 8~128자로 입력하세요.";
            case "LOGIN_ID_TAKEN" -> "이미 사용 중인 로그인 ID입니다.";
            case "DISPLAY_NAME_TAKEN" -> "이미 사용 중인 표시 이름입니다.";
            case "INVALID_CREDENTIALS" -> "로그인 ID 또는 비밀번호가 일치하지 않습니다.";
            case "ALREADY_LOGGED_IN" -> "이 계정이 이미 접속 중입니다.";
            case "AUTH_RETRY_LATER" -> "잠시 후 다시 시도하세요.";
            case "ACCOUNT_LIMIT" -> "서버의 계정 한도에 도달했습니다.";
            case "ACCOUNT_STORAGE_ERROR" -> "서버가 계정 정보를 저장하지 못했습니다.";
            default -> "계정 요청에 실패했습니다.";
        };
    }

    private void refresh() {
        boolean canConnect = !connecting && client == null;
        connect.setDisable(!canConnect);
        connect.setVisible(canConnect);
        connect.setManaged(canConnect);
        chooseCertificate.setDisable(!canConnect);
        clearCertificate.setDisable(!canConnect || certificatePath == null);
        disconnect.setDisable(client == null);
        credentials.setVisible(!signedIn);
        credentials.setManaged(!signedIn);
        credentials.setDisable(!authReady || !expected.isEmpty());
    }

    private void reset(String message) {
        if (client != null) client.close();
        client = null;
        connecting = authReady = signedIn = false;
        expected = "";
        password.clear();
        confirmation.clear();
        status.setText(message);
        refresh();
    }

    void close() {
        if (closed) return;
        closed = true;
        timer.stop();
        reset("");
    }
}
