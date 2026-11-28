package tetris.ui;

import java.io.IOException;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import tetris.games.GameSettings;
import tetris.games.ScreenSize;
import tetris.games.KeyBindings;

public final class SettingsView extends VBox {
    @FunctionalInterface
    public interface SaveAction { void save(GameSettings settings) throws IOException; }
    private final Text content = new Text();
    private boolean colorBlindMode;
    private ScreenSize screenSize;
    private KeyBindings keyBindings;
    private KeyBindings playerTwoBindings;
    private KeyBindingsView keysView;
    private int selected;
    private String message;
    private GameSettings savedSettings;
    private boolean graphical;

    public SettingsView(GameSettings initial, String message, SaveAction save, Runnable back) {
        savedSettings = initial;
        graphical = initial.graphical();
        colorBlindMode = initial.colorBlindMode();
        screenSize = initial.screenSize();
        keyBindings = initial.keyBindings();
        playerTwoBindings = initial.playerTwoBindings();
        this.message = message;
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #101010;");
        setFocusTraversable(true);
        content.setFont(Font.font("Monospaced", 21));
        content.setFill(Color.WHITESMOKE);
        getChildren().add(content);
        render();
        setOnKeyPressed(event -> {
            if (keysView != null) {
                keysView.getOnKeyPressed().handle(event);
                return;
            }
            switch (event.getCode()) {
                case UP -> selected = Math.floorMod(selected - 1, 6);
                case DOWN -> selected = (selected + 1) % 6;
                case LEFT, RIGHT -> {
                    int step = event.getCode() == javafx.scene.input.KeyCode.LEFT ? -1 : 1;
                    if (selected == 0) colorBlindMode = !colorBlindMode;
                    if (selected == 4) graphical = !graphical;
                    if (selected == 1) screenSize = ScreenSize.values()[Math.floorMod(screenSize.ordinal() + step, 3)];
                }
                case ENTER -> {
                    if (selected == 0) colorBlindMode = !colorBlindMode;
                    else if (selected == 4) graphical = !graphical;
                    else if (selected == 1) screenSize = ScreenSize.values()[(screenSize.ordinal() + 1) % 3];
                    else if (selected == 2) {
                        keysView = new KeyBindingsView(keyBindings, playerTwoBindings, (keys, second) -> {
                            keyBindings = keys;
                            playerTwoBindings = second;
                            keysView = null;
                            getChildren().setAll(content);
                            render();
                            requestFocus();
                        }, (keys, second) -> {
                            var updated = new GameSettings(savedSettings.difficulty(), savedSettings.colorBlindMode(), savedSettings.mode(), savedSettings.screenSize(), keys, second, savedSettings.graphical());
                            save.save(updated);
                            savedSettings = updated;
                            keyBindings = keys; playerTwoBindings = second;
                        });
                        getChildren().setAll(keysView);
                        keysView.requestFocus();
                    }
                    else if (selected == 5) {
                        try {
                            save.save(GameSettings.DEFAULT);
                            savedSettings = GameSettings.DEFAULT;
                            graphical = savedSettings.graphical();
                            colorBlindMode = savedSettings.colorBlindMode(); screenSize = savedSettings.screenSize();
                            keyBindings = savedSettings.keyBindings(); playerTwoBindings = savedSettings.playerTwoBindings();
                            this.message = "전체 설정을 기본값으로 초기화하고 저장했습니다.";
                        } catch (IOException e) { this.message = "초기화 값을 저장하지 못했습니다. 다시 시도하세요."; }
                    } else {
                        try { save.save(new GameSettings(savedSettings.difficulty(), colorBlindMode, savedSettings.mode(), screenSize, keyBindings, playerTwoBindings, graphical)); back.run(); }
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
        String[] items = {"색맹모드: " + (colorBlindMode ? "ON" : "OFF"),
                "화면 크기: " + screenSize.getLabel(), "키 설정 (1P / 2P)", "저장하고 돌아가기", "표시 방식: < " + (graphical ? "GUI 블록" : "텍스트") + " >", "전체 설정 기본값으로 초기화 · 저장"};
        StringBuilder text = new StringBuilder("===== 설정 =====\n\n");
        for (int i = 0; i < items.length; i++) text.append(i == selected ? "> " : "  ").append(items[i]).append("\n\n");
        content.setText(text + "↑/↓: 이동  ←/→ 또는 Enter: 변경\nEsc: 취소\n\n" + message);
    }
}
