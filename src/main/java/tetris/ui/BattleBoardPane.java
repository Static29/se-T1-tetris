package tetris.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import tetris.games.*;

/** 로컬·네트워크에서 공통으로 사용하는 한 플레이어의 화면. */
final class BattleBoardPane extends VBox {
    private final Text heading = text("연결 대기", 19);
    private final String label;
    private final ColoredBoardView board = new ColoredBoardView();
    private final Text next = text("", 14), hold = text("", 14), pendingTitle = text("받을 공격 0/10", 12), pending = text("", 12);
    BattleBoardPane() {
        this(null);
    }
    BattleBoardPane(String label) {
        super(10); this.label = label; setAlignment(Pos.TOP_CENTER);
        VBox side = new VBox(6, next, hold, pendingTitle, pending);
        HBox content = new HBox(10, board, side); content.setAlignment(Pos.TOP_CENTER);
        getChildren().addAll(heading, content);
    }
    void render(BattleFrame frame, boolean colorBlind) {
        if (frame == null) return;
        heading.setText((label == null ? frame.name() : label) + " · " + frame.score() + "점"
                + (frame.state() == GameState.GAME_OVER ? " · 탈락" : ""));
        var snapshot = frame.board();
        board.render(snapshot.cells(), snapshot.symbols(), snapshot.ghostCells(), snapshot.flashingRows(), snapshot.flashOn(), colorBlind);
        next.setText("NEXT\n" + preview(frame.next())); hold.setText("HOLD\n" + preview(frame.hold()));
        pendingTitle.setText("받을 공격 " + frame.pending().size() + "/10");
        var s = new StringBuilder("+----------+\n");
        for (int i = 0; i < 10; i++) {
            int index = i - (10 - frame.pending().size());
            s.append('|');
            for (int c = 0; c < 10; c++) s.append(index < 0 ? ' ' : frame.pending().get(index).isFilled(c) ? 'O' : '.');
            s.append("|\n");
        }
        pending.setText(s.append("+----------+").toString()); pending.setFill(Color.LIGHTGRAY);
    }
    private static String preview(String value) {
        var s = new StringBuilder();
        for (int r = 0; r < 4; r++) { for (int c = 0; c < 4; c++) s.append(value.charAt(r * 4 + c)).append(' '); s.append('\n'); }
        return s.toString();
    }
    static Text text(String value, int size) {
        Text t = new Text(value); t.setFill(Color.WHITESMOKE); t.setFont(Font.font("Monospaced", FontWeight.BOLD, size)); return t;
    }
}
