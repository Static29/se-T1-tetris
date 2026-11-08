package tetris.ui;

import java.util.Arrays;
import tetris.board.Cell;

/** 보드의 읽기 전용 데이터를 문자로 변환한다. JavaFX나 게임 규칙에 의존하지 않는다. */
public class BoardView {
    private final BoardDimensions dimensions;

    public BoardView() { dimensions = BoardDimensions.DEFAULT; }
    public BoardView(int rows, int columns) { dimensions = new BoardDimensions(rows, columns); }

    public String renderEmptyBoard() {
        Cell[][] cells = new Cell[dimensions.rows()][dimensions.columns()];
        for (Cell[] row : cells) {
            Arrays.fill(row, Cell.EMPTY);
        }
        return render(cells);
    }

    /** 지정한 크기의 스냅샷을 전달한다. 입력 배열은 수정하지 않는다. */
    public String render(Cell[][] cells) {
        dimensions.validate(cells);
        String border = dimensions.border();
        StringBuilder text = new StringBuilder(border).append('\n');
        for (Cell[] row : cells) {
            text.append("X ");
            for (Cell cell : row) {
                text.append(cell.isEmpty() ? '.' : 'O').append(' ');
            }
            text.append("X\n");
        }
        return text.append(border).toString();
    }
}
