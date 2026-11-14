package tetris.ui;

import java.util.function.Consumer;
import java.util.function.BiConsumer;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.paint.Color;
import tetris.games.*;

public final class KeyBindingsView extends VBox {
    private KeyBindings bindings;
    private KeyBindings second;
    private int player;
    private int selected;
    private boolean capturing;
    private String message = "";
    private final Text content = new Text();
    public KeyBindingsView(KeyBindings initial, Consumer<KeyBindings> done) {
        this(initial, KeyBindings.compatiblePlayerTwo(initial), (first, second) -> done.accept(first));
    }
    public KeyBindingsView(KeyBindings initial, KeyBindings second, BiConsumer<KeyBindings, KeyBindings> done) {
        bindings = initial;
        this.second = second;
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #101010;");
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
                        KeyBindings updated = (player == 0 ? bindings : this.second).with(GameAction.values()[selected], event.getCode().name());
                        KeyBindings.validatePlayers(player == 0 ? updated : bindings, player == 1 ? updated : this.second);
                        if (player == 0) bindings = updated; else this.second = updated;
                        capturing = false;
                        message = "";
                    } catch (IllegalArgumentException e) {
                        message = "이미 사용 중인 1P·2P 키이거나 지원하지 않는 키입니다. 다른 키를 누르세요.";
                    }
                }
            } else {
                switch (event.getCode()) {
                    case UP -> selected = Math.floorMod(selected - 1, 9);
                    case DOWN -> selected = (selected + 1) % 9;
                    case LEFT, RIGHT, TAB -> { player = 1 - player; message = ""; }
                    case ESCAPE -> done.accept(bindings, this.second);
                    case ENTER -> {
                        if (selected < 7) {
                            if (player == 1 && selected == GameAction.PAUSE.ordinal()) message = "로컬 대전의 Esc 나가기는 공통 고정 키입니다.";
                            else capturing = true;
                        }
                        else if (selected == 7) { bindings = KeyBindings.defaults(); this.second = KeyBindings.playerTwoDefaults(); message = "두 플레이어의 기본 키로 복원했습니다."; }
                        else done.accept(bindings, this.second);
                    }
                    default -> { }
                }
            }
            render();
            event.consume();
        });
    }
    private void render() {
        StringBuilder text = new StringBuilder("===== " + (player == 0 ? "1P · 왼쪽" : "2P · 오른쪽") + " 키 설정 =====\n\n");
        for (GameAction action : GameAction.values())
            text.append(selected == action.ordinal() ? "> " : "  ").append(action.getLabel())
                    .append(": ").append(player == 1 && action == GameAction.PAUSE ? "ESCAPE (나가기 · 고정)"
                            : (player == 0 ? bindings : second).key(action)).append("\n");
        text.append(selected == 7 ? "> " : "  ").append("기본 키 복원\n")
                .append(selected == 8 ? "> " : "  ").append("설정으로 돌아가기\n\n");
        text.append(capturing ? "새 키를 누르세요 (Esc: 변경 취소)" : "↑/↓: 선택  ←/→ 또는 Tab: 1P/2P 전환\nEnter: 변경  Esc: 돌아가기")
                .append("\n1P: 솔로·네트워크·로컬 왼쪽 / 2P: 로컬 오른쪽\nNUMPAD1/2는 숫자패드 키입니다.\n설정 화면에서 저장해야 적용됩니다.\n\n").append(message);
        content.setText(text.toString());
    }
}
