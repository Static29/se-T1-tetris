package tetris;

import javafx.application.Application;
import javafx.stage.Stage;
import tetris.ui.ScreenController;

/** JavaFX 창을 준비한다. 실제 화면은 문자로 표현하며 게임 규칙은 Game에 둔다. */
public class Main extends Application {
    @Override
    public void start(Stage stage) {
        ScreenController controller = new ScreenController(stage);
        controller.showMainMenu();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
