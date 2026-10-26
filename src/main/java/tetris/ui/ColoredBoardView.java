package tetris.ui;

import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import tetris.block.TetrominoType;
import tetris.board.Cell;

/** 각 칸의 문자와 색을 분리해서 표시한다. 내부 Cell 종류는 그대로 유지한다. */
public final class ColoredBoardView extends VBox {
    private final Text[][] cells = new Text[20][10];
    public ColoredBoardView() {
        getChildren().add(label("X".repeat(23)));
        for (int row = 0; row < 20; row++) {
            HBox line = new HBox(label("X "));
            for (int col = 0; col < 10; col++) {
                cells[row][col] = label(". ");
                line.getChildren().add(cells[row][col]);
            }
            line.getChildren().add(label("X"));
            getChildren().add(line);
        }
        getChildren().add(label("X".repeat(23)));
    }
    public void render(Cell[][] snapshot, boolean colorBlindMode) {
        new BoardView().render(snapshot); // 기존 크기·null 검증 계약 유지
        for (int row = 0; row < 20; row++) for (int col = 0; col < 10; col++) {
            Cell cell = snapshot[row][col];
            cells[row][col].setText(cell.isEmpty() ? ". " : "O ");
            cells[row][col].setFill(cell.isEmpty() ? Color.DIMGRAY
                    : Color.web(TetrominoType.valueOf(cell.name()).getColorHex(colorBlindMode)));
        }
    }
    private Text label(String value) {
        Text text = new Text(value);
        text.setFont(Font.font("Monospaced", FontWeight.BOLD, 19));
        text.setFill(Color.WHITESMOKE);
        return text;
    }
}
