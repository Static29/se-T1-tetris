package tetris.ui;

import java.util.EnumSet;
import java.util.Set;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tetris.games.Game;
import tetris.games.GameSettings;
import tetris.games.GameMode;
import tetris.persistence.SettingsStore;
import tetris.score.LeaderboardStore;
import tetris.score.LeaderboardEntry;
import java.nio.file.Path;
import java.io.IOException;
import java.util.UUID;
import java.util.List;

/** 창과 키 입력은 JavaFX로 처리하고, 화면 내용은 문자로 표현한다. */
public class ScreenController {
    private final Scene scene;
    private final SettingsStore settingsStore;
    private final LeaderboardStore leaderboardStore;
    private GameSettings settings = GameSettings.DEFAULT;
    private String settingsMessage = "";

    public ScreenController(Stage stage) {
        Path dataDirectory = Path.of(System.getProperty("user.home"), ".tetris");
        settingsStore = new SettingsStore(dataDirectory.resolve("settings.properties"));
        leaderboardStore = new LeaderboardStore(dataDirectory.resolve("leaderboard.properties"));
        try { settings = settingsStore.load(); }
        catch (IOException e) { settingsMessage = "설정을 읽지 못해 기본값을 사용 중입니다."; }
        scene = new Scene(new VBox(), 800, 720);
        // 메뉴 선택/일시정지는 키를 길게 눌러도 한 번만 처리한다.
        // 방향키 반복 입력은 그대로 각 화면에 전달한다.
        Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if ((code == KeyCode.ENTER || code == KeyCode.ESCAPE
                    || code == KeyCode.C || code == KeyCode.SPACE || code == KeyCode.UP)
                    && !held.add(code)) {
                event.consume();
            }
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> held.remove(event.getCode()));
        stage.focusedProperty().addListener((observable, previous, focused) -> {
            if (!focused) {
                held.clear();
            }
        });
        stage.setTitle("Text Tetris");
        stage.setMinWidth(640);
        stage.setMinHeight(680);
        stage.setScene(scene);
    }

    public void showMainMenu() {
        MainMenuView menu = new MainMenuView(
                this::startGame,
                this::showSettings,
                () -> showLeaderboard(null, ""),
                Platform::exit);
        showScreen(menu);
    }

    public void startGame() {
        Game game = new Game(settings);
        game.start();
        UUID resultId = UUID.randomUUID();
        showScreen(new GameView(game, this::showMainMenu, Platform::exit, name -> {
            var entries = leaderboardStore.submit(new LeaderboardEntry(resultId, name, game.getDifficulty(),
                    game.getScore(), game.getSettings().mode()));
            String message = entries.stream().anyMatch(e -> e.id().equals(resultId))
                    ? "> 이번 기록" : "이번 기록은 상위 20위 밖입니다.";
            showScreen(new LeaderboardView(entries, resultId, message, game.getSettings().mode(), this::showMainMenu));
        }, () -> showLeaderboard(null, "기록 등록을 건너뛰었습니다.", game.getSettings().mode()), this::showSettings));
    }

    private void showSettings() {
        Parent previous = scene.getRoot();
        showScreen(new SettingsView(settings, settingsMessage, selected -> {
            settingsStore.save(selected);
            settings = selected;
            settingsMessage = "";
        }, () -> showScreen(previous)));
    }

    private void showLeaderboard(UUID recentId, String message) {
        showLeaderboard(recentId, message, settings.mode());
    }

    private void showLeaderboard(UUID recentId, String message, GameMode mode) {
        try { showScreen(new LeaderboardView(leaderboardStore.load(), recentId, message, mode, this::showMainMenu)); }
        catch (IOException e) {
            showScreen(new LeaderboardView(List.of(), null,
                    "기록을 읽지 못했습니다. 기존 저장 파일은 유지됩니다.", mode, this::showMainMenu));
        }
    }

    private void showScreen(Parent screen) {
        if (screen instanceof MainMenuView menu) {
            menu.setDifficulty(settings.difficulty());
            menu.setMode(settings.mode());
        }
        scene.setRoot(screen);
        Platform.runLater(() -> {
            if (scene.getRoot() == screen) {
                screen.requestFocus();
            }
        });
    }
}
