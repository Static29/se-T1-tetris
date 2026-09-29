package tetris.ui;

import java.util.Arrays;
import tetris.board.Cell;

/** 보드의 읽기 전용 데이터를 문자로 변환한다. JavaFX나 게임 규칙에 의존하지 않는다. */
public class BoardView {
    private static final int ROWS = 20;
    private static final int COLUMNS = 10;

    public String renderEmptyBoard() {
        Cell[][] cells = new Cell[ROWS][COLUMNS];
        for (Cell[] row : cells) {
            Arrays.fill(row, Cell.EMPTY);
        }
        return render(cells);
    }

    /** 추후 Board에서 읽어온 20×10 스냅샷을 전달한다. 입력 배열은 수정하지 않는다. */
    public String render(Cell[][] cells) {
        if (cells == null || cells.length != ROWS) {
            throw new IllegalArgumentException("보드는 20행이어야 합니다.");
        }
        String border = "X".repeat(COLUMNS * 2 + 3);
        StringBuilder text = new StringBuilder(border).append('\n');
        for (Cell[] row : cells) {
            if (row == null || row.length != COLUMNS) {
                throw new IllegalArgumentException("보드는 10열이어야 합니다.");
            }
            text.append("X ");
            for (Cell cell : row) {
                if (cell == null) {
                    throw new IllegalArgumentException("빈칸은 Cell.EMPTY로 전달해야 합니다.");
                }
                text.append(cell.isEmpty() ? '.' : cell.name().charAt(0)).append(' ');
            }
            text.append("X\n");
        }
        return text.append(border).toString();
    }
}
