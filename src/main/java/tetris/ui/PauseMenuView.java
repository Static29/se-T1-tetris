package tetris.ui;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/** 게임 상태를 유지한 채 표시하는 문자 일시정지 메뉴. */
public class PauseMenuView extends VBox {
    private final Text content = new Text();
    private final List<String> titles = List.of("게임으로 복귀", "설정", "메인 메뉴로 복귀", "게임 종료");
    private int selectedIndex;
    private boolean settingsOpen;

    public PauseMenuView(Runnable onResume, Runnable onMainMenu, Runnable onExit) {
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #101010; -fx-border-color: white; -fx-padding: 24;");
        setMaxSize(440, 460);
        setFocusTraversable(true);
        content.setFont(Font.font("Monospaced", 22));
        content.setFill(Color.WHITESMOKE);
        getChildren().add(content);
        updateText();
        setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (settingsOpen) {
                if (code == KeyCode.ESCAPE || code == KeyCode.ENTER) {
                    settingsOpen = false;
                    updateText();
                }
            } else if (code == KeyCode.ESCAPE) {
                onResume.run();
            } else if (code == KeyCode.UP || code == KeyCode.DOWN) {
                selectedIndex = Math.floorMod(selectedIndex + (code == KeyCode.DOWN ? 1 : -1), titles.size());
                updateText();
            } else if (code == KeyCode.ENTER) {
                switch (selectedIndex) {
                    case 0 -> onResume.run();
                    case 1 -> {
                        settingsOpen = true;
                        content.setText("===== 설정 =====\n\n아직 준비 중인 기능입니다.\n게임은 일시정지 상태입니다.\n\nEnter / Esc: 이전 메뉴");
                    }
                    case 2 -> onMainMenu.run();
                    case 3 -> onExit.run();
                    default -> throw new IllegalStateException("잘못된 메뉴 선택");
                }
            }
            // 일시정지 중에는 Hold 등 게임 입력이 아래 화면으로 전달되지 않는다.
            event.consume();
        });
    }

    private void updateText() {
        StringBuilder text = new StringBuilder("===== 일시정지 =====\n\n");
        for (int i = 0; i < titles.size(); i++) {
            text.append(i == selectedIndex ? "> " : "  ").append(titles.get(i)).append("\n\n");
        }
        content.setText(text.append("↑ / ↓: 이동   Enter: 선택\nEsc: 게임으로 복귀").toString());
    }
}
