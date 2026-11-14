package tetris.ui;

import java.util.*;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import tetris.games.BattleFrame;

/** 내 화면은 왼쪽 50%, 상대 1~3명은 오른쪽 50%를 공유한다. 통신 인원 제한과 독립적이다. */
final class NetworkBattleBoards extends GridPane {
    private final BoardSlot local = new BoardSlot();
    private final GridPane opponents = new GridPane();
    private final List<BoardSlot> slots = new ArrayList<>();

    NetworkBattleBoards() {
        setStyle("-fx-background-color: #101010;");
        for (int i = 0; i < 2; i++) {
            var column = new ColumnConstraints(); column.setPercentWidth(50); getColumnConstraints().add(column);
        }
        var row = new RowConstraints(); row.setPercentHeight(100); getRowConstraints().add(row);
        add(local, 0, 0); add(opponents, 1, 0);
        setMinSize(0, 540); setPrefSize(1000, 540); setMaxHeight(540);
        arrange(1);
    }

    void render(BattleFrame own, List<BattleFrame> others, boolean colorBlind) {
        if (others.size() > 3) throw new IllegalArgumentException("상대는 최대 3명입니다.");
        if (slots.size() != Math.max(1, others.size())) arrange(Math.max(1, others.size()));
        local.board.render(own, colorBlind);
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            slot.board.setVisible(i < others.size());
            if (i < others.size()) slot.board.render(others.get(i), colorBlind);
        }
    }

    private void arrange(int count) {
        opponents.getChildren().clear(); opponents.getColumnConstraints().clear(); opponents.getRowConstraints().clear(); slots.clear();
        int columns = count == 1 ? 1 : 2;
        int rows = count == 3 ? 2 : 1;
        for (int i = 0; i < columns; i++) {
            var column = new ColumnConstraints(); column.setPercentWidth(100.0 / columns); opponents.getColumnConstraints().add(column);
        }
        for (int i = 0; i < rows; i++) {
            var row = new RowConstraints(); row.setPercentHeight(100.0 / rows); opponents.getRowConstraints().add(row);
        }
        for (int i = 0; i < count; i++) {
            var slot = new BoardSlot(); slots.add(slot); opponents.add(slot, i % columns, i / columns);
        }
    }

    private static final class BoardSlot extends Pane {
        final BattleBoardPane board = new BattleBoardPane();
        final Scale scale = new Scale(1, 1, 0, 0);
        BoardSlot() {
            setMinSize(0, 0); board.setManaged(false); board.getTransforms().add(scale); getChildren().add(board);
        }
        @Override protected void layoutChildren() {
            double width = 400, height = 540;
            double factor = Math.max(0, Math.min(getWidth() / width, getHeight() / height));
            board.resize(width, height); scale.setX(factor); scale.setY(factor);
            board.relocate((getWidth() - width * factor) / 2, (getHeight() - height * factor) / 2);
        }
    }
}
