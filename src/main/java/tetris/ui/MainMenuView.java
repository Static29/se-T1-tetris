package tetris.ui;

import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/** 버튼 대신 문자와 선택 표시를 사용한다. 메뉴를 늘리려면 항목을 추가한다. */
public class MainMenuView extends VBox {
    private record MenuItem(String title, Runnable action) { }

    private final List<MenuItem> items;
    private final Text content = new Text();
    private int selectedIndex;

    public MainMenuView(Runnable onStart, Runnable onSettings,
                        Runnable onLeaderboard, Runnable onExit) {
        items = List.of(
                new MenuItem("게임 시작", onStart),
                new MenuItem("설정", onSettings),
                new MenuItem("리더보드", onLeaderboard),
                new MenuItem("종료", onExit));
        setAlignment(Pos.CENTER);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: #101010;");
        setFocusTraversable(true);
        content.setFont(Font.font("Monospaced", 22));
        content.setFill(Color.WHITESMOKE);
        getChildren().add(content);
        updateText();

        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                int step = event.getCode() == KeyCode.DOWN ? 1 : -1;
                selectedIndex = Math.floorMod(selectedIndex + step, items.size());
                updateText();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                items.get(selectedIndex).action().run();
                event.consume();
            }
        });
    }

    private void updateText() {
        StringBuilder text = new StringBuilder("===== TETRIS =====\n\n");
        for (int i = 0; i < items.size(); i++) {
            text.append(i == selectedIndex ? "> " : "  ")
                    .append(items.get(i).title()).append("\n\n");
        }
        content.setText(text.append("↑ / ↓: 이동\nEnter: 선택").toString());
    }
}
