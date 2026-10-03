package tetris.ui;

import java.util.EnumSet;
import java.util.Set;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tetris.games.Game;

/** 창과 키 입력은 JavaFX로 처리하고, 화면 내용은 문자로 표현한다. */
public class ScreenController {
    private final Scene scene;

    public ScreenController(Stage stage) {
        scene = new Scene(new VBox(), 800, 720);
        // 메뉴 선택/일시정지는 키를 길게 눌러도 한 번만 처리한다.
        // 방향키 반복 입력은 그대로 각 화면에 전달한다.
        Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if ((code == KeyCode.ENTER || code == KeyCode.ESCAPE
                    || code == KeyCode.C)
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
                () -> showComingSoon("설정"),
                () -> showComingSoon("리더보드"),
                Platform::exit);
        showScreen(menu);
    }

    public void startGame() {
        Game game = new Game();
        game.start();
        showScreen(new GameView(game, this::showMainMenu, Platform::exit));
    }

    private void showComingSoon(String title) {
        Text content = new Text("===== " + title + " =====\n\n"
                + "아직 준비 중인 기능입니다.\n\n"
                + "> 메뉴로 돌아가기\n\nEnter / Esc: 메뉴로 돌아가기");
        content.setFont(Font.font("Monospaced", 22));
        content.setFill(Color.WHITESMOKE);
        VBox page = new VBox(content);
        page.setAlignment(Pos.CENTER);
        page.setStyle("-fx-background-color: #101010;");
        page.setFocusTraversable(true);
        page.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.ENTER) {
                showMainMenu();
                event.consume();
            }
        });
        showScreen(page);
    }

    private void showScreen(Parent screen) {
        scene.setRoot(screen);
        Platform.runLater(() -> {
            if (scene.getRoot() == screen) {
                screen.requestFocus();
            }
        });
    }
}
