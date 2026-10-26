package tetris.ui;

import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.paint.Color;
import tetris.games.*;

public final class KeyBindingsView extends VBox {
    private KeyBindings bindings;
    private int selected;
    private boolean capturing;
    private String message = "";
    private final Text content = new Text();
    public KeyBindingsView(KeyBindings initial, Consumer<KeyBindings> done) {
        bindings = initial;
        setAlignment(Pos.CENTER);
        setFocusTraversable(true);
        content.setFont(Font.font("Monospaced", 20));
        content.setFill(Color.WHITESMOKE);
        getChildren().add(content);
        render();
        setOnKeyPressed(event -> {
            if (capturing) {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    capturing = false;
                    message = "";
                } else {
                    try {
                        bindings = bindings.with(GameAction.values()[selected], event.getCode().name());
                        capturing = false;
                        message = "";
                    } catch (IllegalArgumentException e) {
                        message = "이미 사용 중이거나 지원하지 않는 키입니다. 다른 키를 누르세요.";
                    }
                }
            } else {
                switch (event.getCode()) {
                    case UP -> selected = Math.floorMod(selected - 1, 9);
                    case DOWN -> selected = (selected + 1) % 9;
                    case ESCAPE -> done.accept(bindings);
                    case ENTER -> {
                        if (selected < 7) capturing = true;
                        else if (selected == 7) { bindings = KeyBindings.defaults(); message = "기본 키로 복원했습니다."; }
                        else done.accept(bindings);
                    }
                    default -> { }
                }
            }
            render();
            event.consume();
        });
    }
    private void render() {
        StringBuilder text = new StringBuilder("===== 키 설정 =====\n\n");
        for (GameAction action : GameAction.values())
            text.append(selected == action.ordinal() ? "> " : "  ").append(action.getLabel())
                    .append(": ").append(bindings.key(action)).append("\n");
        text.append(selected == 7 ? "> " : "  ").append("기본 키 복원\n")
                .append(selected == 8 ? "> " : "  ").append("설정으로 돌아가기\n\n");
        text.append(capturing ? "새 키를 누르세요 (Esc: 변경 취소)" : "↑/↓: 선택  Enter: 변경  Esc: 돌아가기")
                .append("\n메뉴 조작 키는 변경되지 않습니다.\n설정 화면에서 저장해야 적용됩니다.\n\n").append(message);
        content.setText(text.toString());
    }
}
