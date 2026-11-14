package tetris.ui;

import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import tetris.games.*;
import tetris.network.*;

/** 네트워크 연결은 백그라운드, 게임 갱신과 화면 변경은 JavaFX 스레드에서 수행한다. */
public final class NetworkBattleView extends VBox implements AutoCloseable {
    private final GameSettings settings;
    private final Text status = BattleBoardPane.text("서버를 열거나 서버 IP로 접속하세요.", 17);
    private final Text result = BattleBoardPane.text("", 22);
    private final TextField address = new TextField("127.0.0.1");
    private final Spinner<Integer> port = new Spinner<>(1024, 65535, 47654);
    private final ComboBox<String> profiles = new ComboBox<>();
    private final BattleRulesView rules = new BattleRulesView("규칙 적용", this::configureSelected, null);
    private final Button host = new Button("서버 열고 참가"), connect = new Button("접속"), ready = new Button("게임 시작 / 준비");
    private final VBox ruleArea = new VBox(12, rules, ready);
    private final NetworkBattleBoards boards = new NetworkBattleBoards();
    private final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
    private final RecentServerStore recent;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AnimationTimer timer;
    private NetworkBattleSession session;
    private DedicatedServer server;
    private boolean connecting, saved;
    private String endpoint = "", serverInfo = "", notice = "";

    public NetworkBattleView(GameSettings settings, Runnable back) {
        this(settings, back, new RecentServerStore(Path.of(System.getProperty("user.home"), ".tetris", "recent-server.txt")));
    }
    NetworkBattleView(GameSettings settings, Runnable back, RecentServerStore recent) {
        super(10); this.settings = settings; this.recent = recent;
        setAlignment(Pos.TOP_CENTER); setPadding(new Insets(18)); setStyle("-fx-background-color: #101010;"); setFocusTraversable(true);
        status.setWrappingWidth(1000);
        profiles.getItems().setAll(BattleMatch.PROFILES); profiles.setValue(BattleMatch.PROFILES.get(0));
        try { recent.load().ifPresent(address::setText); } catch (java.io.IOException e) { notice = "최근 IP를 읽지 못했습니다."; }
        address.setPrefColumnCount(14); port.setPrefWidth(100);
        HBox connection = new HBox(8, profiles, address, port, host, connect); connection.setAlignment(Pos.CENTER);
        ruleArea.setAlignment(Pos.CENTER);
        ready.setStyle("-fx-background-color: #101010; -fx-text-fill: white; -fx-border-color: white; -fx-font-size: 18;");
        Button exit = new Button("나가기"); exit.setOnAction(e -> { close(); back.run(); });
        host.setOnAction(e -> begin(true)); connect.setOnAction(e -> begin(false));
        ready.setOnAction(e -> { if (session != null) session.ready(); });
        boards.setVisible(false); boards.setManaged(false);
        getChildren().addAll(BattleBoardPane.text("네트워크 대전 · 1:1", 26), connection, ruleArea, status, result, boards,
                BattleBoardPane.text("조작: 저장된 키 설정 사용 · 대전 중 개별 일시정지 없음\n현재 네트워크 대전은 2명까지 지원합니다.", 15), exit);
        timer = new AnimationTimer() {
            long previous;
            @Override public void handle(long now) {
                long millis = previous == 0 ? 0 : Math.min(250, (now - previous) / 1_000_000);
                previous = now; update(millis);
            }
        };
        sceneProperty().addListener((o, before, after) -> { if (after == null) timer.stop(); else timer.start(); });
        focusedProperty().addListener((o, before, after) -> { if (!after) held.clear(); });
        addEventFilter(KeyEvent.KEY_RELEASED, e -> held.remove(e.getCode()));
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (session == null || !session.running()) return;
            boolean repeated = !held.add(e.getCode());
            settings.keyBindings().actionFor(e.getCode().name()).ifPresent(action -> {
                if (!repeated || action.isRepeatable()) session.act(action);
            });
            e.consume();
        });
        update(0);
    }
    private void configureSelected() {
        if (session != null && session.isHost() && !session.running()) session.configure(rules.mode(), rules.seconds());
    }
    private void begin(boolean hosting) {
        if (connecting || session != null) return;
        String target = address.getText().strip(); int selectedPort = port.getValue(); String profile = profiles.getValue();
        if (!hosting && (target.isBlank() || target.length() > 253 || target.chars().anyMatch(Character::isWhitespace))) {
            notice = "올바른 서버 IP 또는 호스트 이름을 입력하세요."; return;
        }
        connecting = true; notice = hosting ? "서버를 여는 중..." : "접속 중...";
        Thread.ofVirtual().name("battle-connect").start(() -> {
            DedicatedServer created = null; NetworkClient client = null;
            try {
                if (hosting) created = new DedicatedServer(InetAddress.getByName("0.0.0.0"), selectedPort, ConnectionOptions.DEFAULT);
                client = NetworkClient.connect(hosting ? "127.0.0.1" : target, selectedPort, ConnectionOptions.DEFAULT);
                var connectedClient = client; var createdServer = created;
                String ips = hosting ? addresses() : "";
                Platform.runLater(() -> {
                    if (closed.get()) { connectedClient.close(); if (createdServer != null) createdServer.close(); return; }
                    server = createdServer; session = new NetworkBattleSession(connectedClient, settings, profile);
                    connecting = false; saved = false; endpoint = hosting ? "127.0.0.1" : target;
                    serverInfo = hosting ? "서버 IP: " + ips + " / 포트 " + selectedPort + "\n" : "";
                    notice = "서버 응답을 기다립니다.";
                });
            } catch (Exception e) {
                if (client != null) client.close(); if (created != null) created.close();
                Platform.runLater(() -> { if (!closed.get()) { connecting = false; notice = "접속 실패: " + e.getMessage(); } });
            }
        });
    }
    private static String addresses() throws SocketException {
        var addresses = new ArrayList<String>();
        for (var adapter : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!adapter.isUp() || adapter.isLoopback()) continue;
            for (var ip : Collections.list(adapter.getInetAddresses()))
                if (ip instanceof Inet4Address) addresses.add(ip.getHostAddress());
        }
        return addresses.isEmpty() ? "127.0.0.1 (같은 PC)" : String.join(", ", addresses);
    }
    private void update(long millis) {
        if (closed.get()) return;
        if (session != null) {
            boolean wasRunning = session.running(); session.update(millis);
            if (!session.connected()) {
                notice = session.error(); session.close(); session = null;
                if (server != null) { server.close(); server = null; }
                serverInfo = ""; boards.setVisible(false); boards.setManaged(false); result.setText("");
            } else {
                if (session.welcomed() && !saved) {
                    saved = true;
                    try { recent.save(endpoint); } catch (java.io.IOException e) { notice = "접속 완료. 최근 IP 저장 실패"; }
                }
                if (wasRunning != session.running()) held.clear();
                boolean running = session.running();
                if (!wasRunning && running) requestFocus();
                status.setText(serverInfo + (running ? session.mode() + (session.mode() == BattleMode.TIMED
                        ? " · " + (session.remainingMillis() + 999) / 1000 + "초" : "") : session.lobbyStatus() + " · " + session.mode())
                        + " · " + (session.isHost() ? "방장" : "참가자")
                        + (session.lagging() ? " · 네트워크 지연! " : " · RTT ") + session.rtt() + "ms"
                        + (session.error().isEmpty() ? "" : "\n" + session.error()));
                result.setText(session.result());
                var localFrame = session.local();
                boards.setVisible(localFrame != null); boards.setManaged(boards.isVisible());
                var opponent = session.opponent();
                boards.render(localFrame, opponent == null ? List.of() : List.of(opponent), settings.colorBlindMode());
                ready.setText(session.isReady() ? "준비 취소" : "게임 시작 / 준비");
            }
        }
        boolean idle = session == null;
        if (idle) status.setText(notice.isEmpty() ? "서버를 열거나 서버 IP로 접속하세요." : notice);
        host.setDisable(connecting || !idle); connect.setDisable(connecting || !idle);
        profiles.setDisable(connecting || !idle); address.setDisable(connecting || !idle); port.setDisable(connecting || !idle);
        boolean canConfigure = !idle && session.welcomed() && session.isHost() && !session.running();
        rules.setDisable(!canConfigure); rules.setVisible(canConfigure); rules.setManaged(canConfigure);
        ruleArea.setVisible(idle || !session.running()); ruleArea.setManaged(ruleArea.isVisible());
        ready.setDisable(idle || !session.welcomed() || session.running());
    }
    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        timer.stop(); if (session != null) session.close(); if (server != null) server.close();
    }
}
