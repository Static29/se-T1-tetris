package tetris.ui;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import tetris.games.*;
import tetris.network.*;

/** 인증된 서버의 로그인 → 로비 → 방 → 대전 화면. 서버/계정 파일은 변경하지 않는다. */
final class NetworkPlayView extends StackPane implements AutoCloseable {
    private enum Page { CONNECT, LOGIN, REGISTER, LOBBY, ROOM, GAME, RESULT }
    private final GameSettings settings;
    private final Runnable back;
    private final Label status = label("");
    private final TextField host = field("서버 주소"), port = field("포트"), id = field("ID"), nickname = field("닉네임");
    private final PasswordField password = new PasswordField();
    private final VBox body = new VBox(16);
    private final Label roster = label(""), heading = label(""), gameStatus = label("");
    private final NetworkBattleBoards boards = new NetworkBattleBoards();
    private final AnimationTimer timer;
    private NetworkClient client;
    private RoomBattleSession session;
    private Path certificate;
    private Page page = Page.CONNECT;
    private boolean closed, connecting, authenticated;
    private int generation;
    private long previous, deadline;
    private String expected = "";

    NetworkPlayView(GameSettings settings, Runnable back) {
        this.settings = settings; this.back = back;
        getStylesheets().add(NetworkPlayView.class.getResource("/tetris/network-play.css").toExternalForm());
        getStyleClass().add("network-play");
        body.setAlignment(Pos.CENTER); body.setPadding(new Insets(24));
        status.setMaxWidth(800); status.setWrapText(true);
        status.setAlignment(Pos.CENTER); status.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        password.setPromptText("비밀번호"); password.setMaxWidth(360);
        loadEndpoint(); show(Page.CONNECT);
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                long delta = previous == 0 ? 0 : Math.min(250, (now - previous) / 1_000_000);
                previous = now; tick(delta);
            }
        };
        sceneProperty().addListener((o, before, after) -> { if (after == null) close(); else timer.start(); });
        setOnKeyPressed(event -> {
            if (page == Page.GAME && session != null && getChildren().size() == 1 && !(event.getTarget() instanceof Button)) {
                BattleKeys.action(settings, 0, event.getCode()).ifPresent(session::act);
                event.consume();
            }
        });
    }
    private void loadEndpoint() {
        var properties = new Properties();
        try (var input = getClass().getResourceAsStream("/tetris/dedicated-server.properties")) {
            if (input != null) properties.load(input);
        } catch (java.io.IOException e) { status.setText("기본 서버 설정을 읽지 못했습니다. 직접 입력하세요."); }
        host.setText(properties.getProperty("host", "")); port.setText(properties.getProperty("port", "7777"));
    }
    private static Label label(String text) { var value = new Label(text); value.setWrapText(true); return value; }
    private static TextField field(String prompt) { var field = new TextField(); field.setPromptText(prompt); field.setMaxWidth(360); return field; }
    private static Button button(String title, Runnable action) {
        var button = new Button(title); button.setOnAction(e -> action.run()); return button;
    }
    private HBox row(Node... nodes) { var row = new HBox(12, nodes); row.setAlignment(Pos.CENTER); return row; }
    private void show(Page next) {
        page = next; body.setVisible(page != Page.RESULT); body.getChildren().clear(); getChildren().setAll(body);
        heading.setText(switch (page) {
            case CONNECT -> "네트워크 연결"; case LOGIN -> "로그인"; case REGISTER -> "회원가입";
            case LOBBY -> "네트워크 로비"; case ROOM -> "대기실"; case GAME -> "T1 TETRIS · 네트워크 대전"; case RESULT -> "대전 결과";
        });
        heading.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;"); body.getChildren().add(heading);
        switch (page) {
            case CONNECT -> {
                var cert = label(certificate == null ? "기본 신뢰 인증서 사용" : certificate.getFileName().toString());
                body.getChildren().addAll(host, port, cert, row(button("서버 공개 인증서 선택", () -> {
                    var chooser = new FileChooser(); chooser.setTitle("서버 운영자에게 받은 공개 인증서");
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("공개 인증서", "*.cer", "*.crt", "*.pem", "*.der"));
                    var file = chooser.showOpenDialog(getScene().getWindow());
                    if (file != null) { certificate = file.toPath(); cert.setText(file.getName()); }
                }), button("기본 인증서", () -> { certificate = null; cert.setText("기본 신뢰 인증서 사용"); })),
                        button("보안 연결", this::connect), label("서버에서 받은 공개 인증서를 선택하세요.\n공인 인증서 서버는 기본 인증서로 연결할 수 있습니다."),
                        button("돌아가기", () -> { close(); back.run(); }));
            }
            case LOGIN, REGISTER -> {
                boolean registration = page == Page.REGISTER;
                body.getChildren().addAll(label("ID"), id);
                if (registration) body.getChildren().addAll(label("닉네임"), nickname);
                body.getChildren().addAll(label("비밀번호"), password, button(registration ? "계정 생성" : "로그인", this::submit));
                password.setOnAction(e -> submit());
                body.getChildren().add(button(registration ? "로그인으로" : "회원가입", () -> {
                    if (!expected.isEmpty()) return;
                    password.clear(); show(registration ? Page.LOGIN : Page.REGISTER);
                }));
                body.getChildren().addAll(label("ID: 영문·숫자·밑줄 4~20자 / 비밀번호: 8~128자\n닉네임: 문자·숫자 2~12자"), button("연결 종료", this::disconnect));
            }
            case LOBBY -> lobby();
            case ROOM -> {
                body.getChildren().addAll(label("방 번호 " + session.roomId() + " · " + session.rulesLabel()), roster,
                        button("준비 / 준비 취소", session::toggleReady),
                        label("설정 인원이 모두 준비하면 시작합니다.\n규칙 변경은 현재 서버에서 지원하지 않습니다. 새 방을 만들어주세요."),
                        label("채팅 · 서버 연동 대기"));
                var chat = field("채팅 준비 중"); chat.setDisable(true);
                body.getChildren().addAll(chat, button("방 나가기", session::leave));
            }
            case GAME -> {
                body.getChildren().addAll(gameStatus, boards, label("1인 플레이 키 설정 사용 · 개별 일시정지 없음"),
                        button("대전 포기하고 방 나가기", this::confirmLeave));
                setFocusTraversable(true); Platform.runLater(this::requestFocus);
            }
            case RESULT -> {
                String winner = session.winner().equals("DRAW") ? "무승부" : session.name(session.winner()) + " 승리";
                var content = new StringBuilder(winner + "\n\n순위    닉네임    게임 점수\n");
                for (var entry : session.standings()) content.append(entry.rank() == 0 ? "미확정" : entry.rank() + "위")
                        .append("    ").append(entry.name()).append("    ").append(entry.score() == null ? "미수신" : entry.score()).append('\n');
                var popup = new VBox(24, label("대전 결과"), label(content.toString()),
                        label("상대 점수는 마지막 수신값입니다.\n시간전의 승자 외 순위·랭크 점수는 서버가 제공하지 않습니다."),
                        button("확인 · 방으로 돌아가기", () -> { session.dismissResult(); show(session.inRoom() ? Page.ROOM : Page.LOBBY); }));
                popup.getStyleClass().add("result-card"); popup.setAlignment(Pos.CENTER); popup.setMaxSize(680, 540);
                getChildren().add(popup);
            }
        }
        body.getChildren().add(status);
    }
    private void lobby() {
        var count = new ComboBox<Integer>(); count.getItems().addAll(2, 3, 4); count.setValue(2);
        var difficulty = new ComboBox<Difficulty>(); difficulty.getItems().setAll(Difficulty.values()); difficulty.setValue(settings.difficulty());
        var mode = new ComboBox<BattleMode>(); mode.getItems().setAll(BattleMode.values()); mode.setValue(BattleMode.NORMAL);
        var duration = field("제한 시간 (초)"); duration.setText("180"); duration.setDisable(true);
        mode.valueProperty().addListener((o, old, value) -> duration.setDisable(value != BattleMode.TIMED));
        var number = field("참가할 방 번호");
        body.getChildren().addAll(label("개인전 방 만들기"), row(label("인원"), count, label("난이도"), difficulty, mode), duration,
                button("방 만들기", () -> {
                    try { session.create(count.getValue(), mode.getValue(), difficulty.getValue(), mode.getValue() == BattleMode.TIMED ? Integer.parseInt(duration.getText().strip()) : 180); }
                    catch (IllegalArgumentException e) { status.setText("인원은 2~4명, 시간은 1~86400초로 입력하세요."); }
                }), label("방 번호로 참가"), number, button("참가", () -> {
                    try { session.join(number.getText().strip()); }
                    catch (IllegalArgumentException e) { status.setText(e.getMessage()); }
                }), label("방 목록 조회는 서버 연동 대기 중입니다. 방 번호를 공유해 참가하세요."), button("로그아웃", this::disconnect));
    }
    private void connect() {
        if (connecting || client != null || closed) return;
        final int selectedPort;
        try { selectedPort = Integer.parseInt(port.getText().strip()); if (selectedPort < 1 || selectedPort > 65535) throw new NumberFormatException(); }
        catch (NumberFormatException e) { status.setText("포트는 1~65535 사이의 숫자입니다."); return; }
        String address = host.getText().strip();
        if (address.isBlank() || address.length() > 253 || address.chars().anyMatch(Character::isWhitespace)) { status.setText("서버 주소를 입력하세요."); return; }
        int attempt = ++generation; Path selectedCertificate = certificate;
        connecting = true; status.setText("TLS 보안 연결 중…");
        Thread.ofVirtual().name("network-play-connect").start(() -> {
            try {
                SSLContext tls = selectedCertificate == null ? SSLContext.getDefault() : ServerCertificateTrust.fromFile(selectedCertificate);
                var connection = NetworkClient.connectTls(address, selectedPort, ConnectionOptions.DEFAULT, tls);
                Platform.runLater(() -> {
                    if (closed || attempt != generation) { connection.close(); return; }
                    connected(connection);
                });
            } catch (Exception e) {
                Platform.runLater(() -> { if (!closed && attempt == generation) {
                    connecting = false; status.setText("연결 실패. 서버 주소·포트·공개 인증서 및 인증서에 등록된 주소를 확인하세요.");
                }});
            }
        });
    }
    /** 연결 성공 시 소켓 소유권을 이 화면에 넘긴다. */
    void connected(NetworkClient connection) {
        if (closed) { connection.close(); return; }
        connecting = false; client = connection; expect("AUTH_REQUIRED");
        status.setText("서버 인증 응답 대기 중…");
    }
    private void submit() {
        if (client == null || !expected.isEmpty()) return;
        String login = id.getText().strip(), secret = password.getText();
        if (!login.matches("[A-Za-z0-9_]{4,20}") || secret.isBlank() || secret.length() < 8 || secret.length() > 128) {
            status.setText("ID와 비밀번호의 형식을 확인하세요."); return;
        }
        try {
        if (page == Page.REGISTER) {
            String name = java.text.Normalizer.normalize(nickname.getText().strip(), java.text.Normalizer.Form.NFKC);
            if (!name.matches("[\\p{L}\\p{N}]{2,12}")) { status.setText("닉네임은 문자와 숫자 2~12자입니다."); return; }
            send(NetworkMessage.of("REGISTER", login, name, secret)); expect("REGISTERED");
        } else { send(NetworkMessage.of("LOGIN", login, secret)); expect("LOGGED_IN"); }
        password.clear(); status.setText("요청 처리 중…");
        } catch (IllegalStateException e) { disconnect(); status.setText("요청을 전송하지 못했습니다. 다시 연결하세요."); }
    }
    private void expect(String response) { expected = response; deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos(); }
    private void send(NetworkMessage message) {
        if (client == null || !client.send(message)) throw new IllegalStateException("전송 실패");
    }
    private void tick(long millis) {
        if (client == null || closed) return;
        try {
            if (client.disconnectReason() != null) { disconnect(); status.setText("서버 연결이 종료됐습니다. 다시 연결하세요."); return; }
            for (int i = 0; i < 256; i++) {
                var message = client.poll(Duration.ZERO); if (message == null) break; receive(message);
            }
            if (!expected.isEmpty() && System.nanoTime() >= deadline) { disconnect(); status.setText("서버 응답 시간이 초과됐습니다."); return; }
            if (session == null) return;
            session.update(millis);
            if (!session.winner().isEmpty() && page != Page.RESULT) show(Page.RESULT);
            else if (session.running() && page != Page.GAME && page != Page.RESULT) show(Page.GAME);
            else if (!session.running() && page != Page.RESULT) {
                Page next = session.inRoom() ? Page.ROOM : Page.LOBBY;
                if (session.welcomed() && page != next) show(next);
            }
            if (page == Page.ROOM) {
                var text = new StringBuilder("참가자 · 현재 접속 중 승수\n\n");
                for (var member : session.members()) text.append(member.id().equals(session.hostId()) ? "[방장] " : "")
                        .append(member.name()).append("    ").append(member.sessionWins()).append("승    ")
                        .append(member.ready() ? "준비 완료" : "대기 중").append('\n');
                roster.setText(text.toString());
            } else if (page == Page.GAME) {
                var visible = session.visibleFrames();
                if (!visible.isEmpty()) boards.render(visible.getFirst(), visible.subList(1, visible.size()), settings.colorBlindMode());
                gameStatus.setText((session.playing() ? "플레이 중" : "관전 / 결과 대기") + " · " + session.rulesLabel()
                        + (session.timed() ? " · 남은 시간 " + session.remainingSeconds() + "초" : ""));
            }
            if (!session.error().isEmpty()) status.setText("서버 응답: " + session.error());
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); disconnect(); }
        catch (RuntimeException e) { disconnect(); status.setText("서버 응답을 처리하지 못했습니다. 다시 연결하세요."); }
    }
    private void receive(NetworkMessage message) {
        if (authenticated) {
            session.receive(message);
            if (message.type().equals("ROOM_WELCOME")) expected = "";
            return;
        }
        if (message.type().equals("ERROR")) {
            expected = ""; status.setText("요청 실패: " + String.join(", ", message.fields()) + " · 입력을 확인하고 잠시 후 다시 시도하세요."); return;
        }
        if (!message.type().equals(expected)) throw new IllegalArgumentException("인증 응답 오류");
        expected = "";
        switch (message.type()) {
            case "AUTH_REQUIRED" -> { show(Page.LOGIN); status.setText("보안 연결 완료"); }
            case "REGISTERED" -> { show(Page.LOGIN); status.setText("계정 생성 완료. 잠시 후 비밀번호를 입력하여 로그인하세요."); }
            case "LOGGED_IN" -> {
                if (message.fields().size() != 2) throw new IllegalArgumentException("로그인 응답 오류");
                authenticated = true; session = new RoomBattleSession(settings, this::send);
                expect("ROOM_WELCOME");
                status.setText(message.fields().get(1) + " 님, 환영합니다.");
            }
            default -> throw new IllegalArgumentException("인증 응답 오류");
        }
    }
    private void confirmLeave() {
        var prompt = new VBox(18, label("대전을 포기하고 방을 나가시겠습니까?"));
        prompt.setAlignment(Pos.CENTER); prompt.setMaxSize(600, 220); prompt.getStyleClass().add("result-card");
        prompt.getChildren().add(row(button("나가기", () -> { session.leave(); getChildren().remove(prompt); }), button("계속하기", () -> { getChildren().remove(prompt); requestFocus(); })));
        getChildren().add(prompt);
    }
    private void disconnect() {
        generation++; connecting = authenticated = false; expected = "";
        if (client != null) client.close(); client = null;
        if (session != null) session.disconnected(); session = null;
        password.clear(); status.setText(""); show(Page.CONNECT);
    }
    @Override public void close() { if (closed) return; closed = true; timer.stop(); disconnect(); }
}
