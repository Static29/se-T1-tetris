package tetris.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

final class MultiplayerMenuView extends VBox {
    private final Text content = BattleBoardPane.text("", 22);
    private int selected;
    MultiplayerMenuView(Runnable local, Runnable network, Runnable back) {
        setAlignment(Pos.CENTER); setFocusTraversable(true); setStyle("-fx-background-color: #101010;");
        getChildren().add(content); render();
        Runnable[] actions = {local, network, back};
        setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP -> selected = Math.floorMod(selected - 1, 3);
                case DOWN -> selected = (selected + 1) % 3;
                case ENTER -> actions[selected].run();
                case ESCAPE -> back.run();
                default -> { return; }
            }
            render(); e.consume();
        });
    }
    private void render() {
        var text = new StringBuilder("===== 멀티플레이 =====\n\n");
        String[] rows = {"로컬 1:1 · 한 PC에서 플레이", "네트워크 · 서버에 연결", "돌아가기"};
        for (int i = 0; i < rows.length; i++) text.append(i == selected ? "> " : "  ").append(rows[i]).append("\n\n");
        content.setText(text + "↑/↓: 선택  Enter: 실행  Esc: 메인 메뉴");
    }
}
