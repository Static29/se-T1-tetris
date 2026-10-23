package tetris.ui;

import java.io.IOException;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import tetris.games.Difficulty;
import tetris.games.GameSettings;
import tetris.games.GameMode;

public final class SettingsView extends VBox {
    @FunctionalInterface
    public interface SaveAction { void save(GameSettings settings) throws IOException; }
    private final Text content = new Text();
    private Difficulty difficulty;
    private GameMode mode;
    private boolean colorBlindMode;
    private int selected;
    private String message;

    public SettingsView(GameSettings initial, String message, SaveAction save, Runnable back) {
        difficulty = initial.difficulty();
        mode = initial.mode();
        colorBlindMode = initial.colorBlindMode();
        this.message = message;
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #101010;");
        setFocusTraversable(true);
        content.setFont(Font.font("Monospaced", 21));
        content.setFill(Color.WHITESMOKE);
        getChildren().add(content);
        render();
        setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case UP -> selected = Math.floorMod(selected - 1, 4);
                case DOWN -> selected = (selected + 1) % 4;
                case LEFT, RIGHT -> {
                    int step = event.getCode() == javafx.scene.input.KeyCode.LEFT ? -1 : 1;
                    if (selected == 0) difficulty = Difficulty.values()[Math.floorMod(difficulty.ordinal() + step, 3)];
                    if (selected == 1) colorBlindMode = !colorBlindMode;
                    if (selected == 2) mode = mode == GameMode.CLASSIC ? GameMode.ITEM : GameMode.CLASSIC;
                }
                case ENTER -> {
                    if (selected == 0) difficulty = Difficulty.values()[(difficulty.ordinal() + 1) % 3];
                    else if (selected == 1) colorBlindMode = !colorBlindMode;
                    else if (selected == 2) mode = mode == GameMode.CLASSIC ? GameMode.ITEM : GameMode.CLASSIC;
                    else {
                        try { save.save(new GameSettings(difficulty, colorBlindMode, mode)); back.run(); }
                        catch (IOException e) { this.message = "설정을 저장하지 못했습니다. 다시 시도해 주세요."; }
                    }
                }
                case ESCAPE -> back.run();
                default -> { return; }
            }
            render();
            event.consume();
        });
    }

    private void render() {
        String[] items = {"난이도: " + difficulty.getLabel(),
                "색맹모드: " + (colorBlindMode ? "ON" : "OFF"), "게임 모드: " + mode.getLabel(), "저장하고 돌아가기"};
        StringBuilder text = new StringBuilder("===== 설정 =====\n\n");
        for (int i = 0; i < items.length; i++) text.append(i == selected ? "> " : "  ").append(items[i]).append("\n\n");
        content.setText(text + "↑/↓: 이동  ←/→ 또는 Enter: 변경\nEsc: 취소\n\n"
                + "난이도·모드는 다음 게임부터 적용됩니다.\nItem 모드의 아이템 등장은 준비 중입니다.\n색맹모드 색상 적용은 준비 중입니다.\n\n" + message);
    }
}
