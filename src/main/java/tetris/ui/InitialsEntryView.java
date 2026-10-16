package tetris.ui;

import java.io.IOException;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import tetris.games.Difficulty;

/** 키보드로 세 자리 영문 이니셜을 선택하는 게임 종료 창. */
public final class InitialsEntryView extends VBox {
    @FunctionalInterface
    public interface SubmitAction { void submit(String name) throws IOException; }
    private final char[] letters = {'A', 'A', 'A'};
    private final Text content = new Text();
    private int selected;
    private boolean submitted;
    private String error = "";

    public InitialsEntryView(long score, Difficulty difficulty, SubmitAction submit, Runnable skip) {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setStyle("-fx-background-color: #101010; -fx-padding: 30;");
        setFocusTraversable(true);
        content.setFont(Font.font("Monospaced", 22));
        content.setFill(Color.WHITESMOKE);
        getChildren().add(content);
        render(score, difficulty);
        setOnKeyPressed(event -> {
            if (submitted) { event.consume(); return; }
            switch (event.getCode()) {
                case LEFT -> selected = Math.floorMod(selected - 1, 3);
                case RIGHT -> selected = (selected + 1) % 3;
                case UP -> letters[selected] = (char) ('A' + (letters[selected] - 'A' + 1) % 26);
                case DOWN -> letters[selected] = (char) ('A' + Math.floorMod(letters[selected] - 'A' - 1, 26));
                case ENTER -> {
                    try {
                        submitted = true;
                        submit.submit(new String(letters));
                    } catch (IOException e) {
                        submitted = false;
                        error = "저장 실패. Enter로 재시도 / Esc로 건너뛰기";
                    }
                }
                case ESCAPE -> { submitted = true; skip.run(); }
                default -> { return; }
            }
            render(score, difficulty);
            event.consume();
        });
    }

    private void render(long score, Difficulty difficulty) {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 3; i++) name.append(i == selected ? "[" : " ").append(letters[i]).append(i == selected ? "] " : "  ");
        content.setText("===== GAME OVER =====\n\n난이도: " + difficulty.getLabel()
                + "\n점수: " + score + "\n\n이름을 선택하세요\n\n" + name
                + "\n\n←/→: 자리 선택  ↑/↓: A~Z 선택\nEnter: 기록 저장  Esc: 건너뛰기\n\n" + error);
    }
}
