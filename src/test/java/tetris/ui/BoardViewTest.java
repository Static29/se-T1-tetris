package tetris.ui;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import tetris.board.Cell;
import static org.junit.jupiter.api.Assertions.*;

class BoardViewTest {
    @Test
    void emptyBoardHasTwentyRowsOfTenCellsInsideTextBorders() {
        String[] lines = new BoardView().renderEmptyBoard().split("\n");
        assertEquals(22, lines.length);
        assertEquals("X".repeat(23), lines[0]);
        assertEquals(lines[0], lines[21]);
        for (int row = 1; row <= 20; row++) {
            assertEquals("X " + ". ".repeat(10) + "X", lines[row]);
        }
    }

    @Test
    void blockSymbolsKeepRowAndColumnOrderWithoutChangingInput() {
        Cell[][] cells = emptyCells();
        cells[0][9] = Cell.T;
        cells[19][0] = Cell.I;
        String[] lines = new BoardView().render(cells).split("\n");
        assertEquals("X " + ". ".repeat(9) + "T X", lines[1]);
        assertEquals("X I " + ". ".repeat(9) + "X", lines[20]);
        assertEquals(Cell.T, cells[0][9]);
        assertEquals(Cell.I, cells[19][0]);
        assertEquals(Cell.EMPTY, cells[0][0]);
    }

    @Test
    void rejectsMalformedSnapshotsInsteadOfDrawingMisalignedRows() {
        BoardView view = new BoardView();
        assertThrows(IllegalArgumentException.class, () -> view.render(null));
        assertThrows(IllegalArgumentException.class, () -> view.render(new Cell[19][10]));
        Cell[][] cells = emptyCells();
        cells[0] = new Cell[9];
        assertThrows(IllegalArgumentException.class, () -> view.render(cells));
        cells[0] = null;
        assertThrows(IllegalArgumentException.class, () -> view.render(cells));
    }

    private Cell[][] emptyCells() {
        Cell[][] cells = new Cell[20][10];
        for (Cell[] row : cells) {
            Arrays.fill(row, Cell.EMPTY);
        }
        return cells;
    }
}
