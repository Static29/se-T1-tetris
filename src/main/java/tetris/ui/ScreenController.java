package tetris.ui;

import java.util.EnumSet;
import java.util.Set;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.Group;
import javafx.scene.input.KeyCombination;
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
    private final Stage stage;
    private final StackPane viewport = new StackPane();
    private final Group scaledContent = new Group();
    private Parent activeScreen;
    private final SettingsStore settingsStore;
    private final LeaderboardStore leaderboardStore;
    private GameSettings settings = GameSettings.DEFAULT;
    private String settingsMessage = "";

    public ScreenController(Stage stage) {
        this.stage = stage;
        Path dataDirectory = Path.of(System.getProperty("user.home"), ".tetris");
        settingsStore = new SettingsStore(dataDirectory.resolve("settings.properties"));
        leaderboardStore = new LeaderboardStore(dataDirectory.resolve("leaderboard.properties"));
        try { settings = settingsStore.load(); }
        catch (IOException e) { settingsMessage = "설정을 읽지 못해 기본값을 사용 중입니다."; }
        viewport.getChildren().add(scaledContent);
        viewport.setMinSize(0, 0);
        viewport.setStyle("-fx-background-color: #101010;");
        scene = new Scene(viewport, 800, 720);
        viewport.widthProperty().addListener((o, before, after) -> scaleContent());
        viewport.heightProperty().addListener((o, before, after) -> scaleContent());
        // 메뉴 선택/일시정지는 키를 길게 눌러도 한 번만 처리한다.
        // 방향키 반복 입력은 그대로 각 화면에 전달한다.
        Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            boolean repeated = !held.add(code);
            boolean suppress = code == KeyCode.ENTER || code == KeyCode.ESCAPE
                    || (activeScreen instanceof GameView game && game.suppressRepeat(code));
            if (suppress && repeated) {
                event.consume();
            }
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> held.remove(event.getCode()));
        stage.focusedProperty().addListener((observable, previous, focused) -> {
            if (!focused) {
                held.clear();
            }
        });
        stage.setTitle("T1 TETRIS");
        stage.setMinWidth(480);
        stage.setMinHeight(440);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint("");
        stage.setScene(scene);
        applyScreenSize();
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
            showScreen(leaderboardPage(entries, resultId, message, game.getSettings().mode()));
        }, () -> showLeaderboard(null, "기록 등록을 건너뛰었습니다.", game.getSettings().mode()), this::showSettings));
    }

    private void showSettings() {
        Parent previous = activeScreen;
        showScreen(new SettingsView(settings, settingsMessage, selected -> {
            settingsStore.save(selected);
            settings = selected;
            settingsMessage = "";
            applyScreenSize();
        }, () -> showScreen(previous)));
    }

    private void showLeaderboard(UUID recentId, String message) {
        showLeaderboard(recentId, message, settings.mode());
    }

    private void showLeaderboard(UUID recentId, String message, GameMode mode) {
        try { showScreen(leaderboardPage(leaderboardStore.load(), recentId, message, mode)); }
        catch (IOException e) {
            showScreen(leaderboardPage(List.of(), null,
                    "기록을 읽지 못했습니다. 기존 저장 파일은 유지됩니다.", mode));
        }
    }

    private LeaderboardView leaderboardPage(List<LeaderboardEntry> entries, UUID recentId, String message, GameMode mode) {
        return new LeaderboardView(entries, recentId, message, mode, this::showMainMenu, (selectedMode, all) -> {
            if (all) leaderboardStore.clearAll();
            else leaderboardStore.clear(selectedMode);
            return leaderboardStore.load();
        });
    }

    private void showScreen(Parent screen) {
        if (screen instanceof MainMenuView menu) {
            menu.setDifficulty(settings.difficulty());
            menu.setMode(settings.mode());
        }
        if (screen instanceof GameView game) game.applyPreferences(settings);
        activeScreen = screen;
        if (screen instanceof Region region) {
            region.setMinSize(800, 720);
            region.setPrefSize(800, 720);
            region.setMaxSize(800, 720);
        }
        scaledContent.getChildren().setAll(screen);
        scaleContent();
        Platform.runLater(() -> {
            if (activeScreen == screen) {
                screen.requestFocus();
            }
        });
    }

    private void scaleContent() {
        double scale = Math.min(viewport.getWidth() / 800.0, viewport.getHeight() / 720.0);
        if (scale > 0) { scaledContent.setScaleX(scale); scaledContent.setScaleY(scale); }
    }

    private void applyScreenSize() {
        boolean fullScreen = settings.screenSize() == tetris.games.ScreenSize.FULLSCREEN;
        stage.setFullScreen(fullScreen);
        if (!fullScreen) {
            stage.setMaximized(false);
            boolean small = settings.screenSize() == tetris.games.ScreenSize.SMALL;
            stage.setWidth(small ? 656 : 816);
            stage.setHeight(small ? 615 : 759);
            stage.centerOnScreen();
        }
    }
}
