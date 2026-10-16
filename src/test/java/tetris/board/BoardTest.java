package tetris.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 기본 보드는 0~1번 줄이 버퍼, 2~21번 줄이 보이는 영역, 0~9번 칸이다. */
class BoardTest {
    /** (row, col) 쌍을 순서대로 받아 좌표 목록을 만든다. 예: positions(0, 0, 0, 1) */
    private static List<Position> positions(int... rowCols) {
        List<Position> result = new ArrayList<>();
        for (int i = 0; i < rowCols.length; i += 2) {
            result.add(new Position(rowCols[i], rowCols[i + 1]));
        }
        return result;
    }

    /** row 줄을 emptyCol 칸만 남기고 채운다. emptyCol이 -1이면 줄 전체를 채운다. */
    private static void fillRow(Board board, int row, Cell cell, int emptyCol) {
        List<Position> line = new ArrayList<>();
        for (int col = 0; col < board.getColumns(); col++) {
            if (col != emptyCol) {
                line.add(new Position(row, col));
            }
        }
        board.lock(line, cell);
    }

    @Test
    void defaultBoardHasTwentyVisibleRowsTwoBufferRowsAndTenColumns() {
        Board board = new Board();
        assertEquals(20, board.getVisibleRows());
        assertEquals(2, board.getBufferRows());
        assertEquals(22, board.getTotalRows());
        assertEquals(10, board.getColumns());
    }

    @Test
    void newBoardIsEmptyEverywhere() {
        Board board = new Board();
        for (int row = 0; row < board.getTotalRows(); row++) {
            for (int col = 0; col < board.getColumns(); col++) {
                assertEquals(Cell.EMPTY, board.getCell(row, col));
            }
        }
        assertTrue(board.findFullRows().isEmpty());
    }

    @Test
    void rejectsInvalidSizes() {
        assertThrows(IllegalArgumentException.class, () -> new Board(0, 2, 10));
        assertThrows(IllegalArgumentException.class, () -> new Board(20, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> new Board(20, 2, 0));
        assertDoesNotThrow(() -> new Board(1, 0, 1));
    }

    @Test
    void visibleRowsStartAfterBuffer() {
        Board board = new Board();
        assertFalse(board.isVisibleRow(-1));
        assertFalse(board.isVisibleRow(1));
        assertTrue(board.isVisibleRow(2));
        assertTrue(board.isVisibleRow(21));
        assertFalse(board.isVisibleRow(22));
    }

    @Test
    void outsideCoordinatesAreTreatedAsWallsAndFloor() {
        Board board = new Board();
        assertTrue(board.isInside(0, 0));
        assertTrue(board.isInside(21, 9));
        assertFalse(board.isInside(-1, 0));
        assertFalse(board.isInside(22, 0));
        assertFalse(board.isInside(0, -1));
        assertFalse(board.isInside(0, 10));
        assertFalse(board.isEmpty(new Position(5, -1)));
        assertFalse(board.isEmpty(new Position(5, 10)));
        assertFalse(board.isEmpty(new Position(22, 5)));
        assertTrue(board.isEmpty(new Position(21, 5)));
    }

    @Test
    void getCellOutsideBoardThrowsInsteadOfReturningEmpty() {
        Board board = new Board();
        assertThrows(IndexOutOfBoundsException.class, () -> board.getCell(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.getCell(new Position(0, 10)));
        assertThrows(NullPointerException.class, () -> board.getCell(null));
    }

    @Test
    void canPlaceDetectsWallsFloorAndLockedCells() {
        Board board = new Board();
        assertTrue(board.canPlace(positions(20, 4, 21, 4, 21, 5, 21, 6)));
        assertFalse(board.canPlace(positions(21, 9, 21, 10)));
        assertFalse(board.canPlace(positions(21, 4, 22, 4)));
        board.lock(positions(21, 5), Cell.O);
        assertFalse(board.canPlace(positions(20, 4, 21, 4, 21, 5, 21, 6)));
        assertTrue(board.canPlace(positions(20, 4, 20, 5, 20, 6, 19, 5)));
    }

    @Test
    void positionListMustNotBeEmptyContainNullOrRepeatAPosition() {
        Board board = new Board();
        assertThrows(NullPointerException.class, () -> board.canPlace(null));
        assertThrows(IllegalArgumentException.class, () -> board.canPlace(List.of()));
        assertThrows(NullPointerException.class, () -> board.canPlace(Arrays.asList(new Position(0, 0), null)));
        assertThrows(IllegalArgumentException.class, () -> board.canPlace(positions(3, 3, 3, 3)));
        assertThrows(IllegalArgumentException.class, () -> board.lock(positions(3, 3, 3, 3), Cell.T));
        assertEquals(Cell.EMPTY, board.getCell(3, 3));
    }

    @Test
    void lockStoresGivenCellAtEveryPosition() {
        Board board = new Board();
        board.lock(positions(20, 4, 21, 3, 21, 4, 21, 5), Cell.T);
        assertEquals(Cell.T, board.getCell(20, 4));
        assertEquals(Cell.T, board.getCell(21, 3));
        assertEquals(Cell.T, board.getCell(21, 4));
        assertEquals(Cell.T, board.getCell(21, 5));
        assertEquals(Cell.EMPTY, board.getCell(20, 3));
    }

    @Test
    void failedLockLeavesBoardUnchanged() {
        Board board = new Board();
        board.lock(positions(21, 0), Cell.I);
        String before = board.toString();
        // 앞의 두 칸은 놓을 수 있지만 마지막 칸 때문에 전체 고정이 실패해야 한다.
        assertThrows(IllegalArgumentException.class, () -> board.lock(positions(20, 0, 20, 1, 21, 0), Cell.Z));
        assertThrows(IllegalArgumentException.class, () -> board.lock(positions(20, 8, 20, 9, 20, 10), Cell.Z));
        assertEquals(before, board.toString());
    }

    @Test
    void lockRejectsEmptyOrNullCell() {
        Board board = new Board();
        assertThrows(IllegalArgumentException.class, () -> board.lock(positions(21, 0), Cell.EMPTY));
        assertThrows(NullPointerException.class, () -> board.lock(positions(21, 0), null));
        assertEquals(Cell.EMPTY, board.getCell(21, 0));
    }

    @Test
    void blockOutWhenSpawnPositionsAreBlocked() {
        Board board = new Board();
        List<Position> spawn = positions(0, 3, 0, 4, 0, 5, 0, 6);
        assertFalse(board.isBlockOut(spawn));
        board.lock(positions(0, 5), Cell.L);
        assertTrue(board.isBlockOut(spawn));
    }

    @Test
    void lockOutOnlyWhenEveryLockedCellIsInsideBuffer() {
        Board board = new Board();
        assertTrue(board.isLockOut(positions(0, 4, 1, 4, 1, 5, 1, 6)));
        assertFalse(board.isLockOut(positions(1, 4, 2, 4, 2, 5, 2, 6)));
        assertFalse(board.isLockOut(positions(20, 4, 21, 4)));
    }

    @Test
    void boardWithoutBufferNeverLocksOut() {
        Board board = new Board(4, 0, 4);
        assertFalse(board.isLockOut(positions(0, 0, 0, 1)));
        assertTrue(board.isVisibleRow(0));
    }

    @Test
    void findFullRowsReturnsRowsTopToBottomWithoutChangingBoard() {
        Board board = new Board();
        fillRow(board, 21, Cell.I, -1);
        fillRow(board, 19, Cell.J, -1);
        fillRow(board, 20, Cell.S, 3);
        String before = board.toString();
        assertEquals(List.of(19, 21), board.findFullRows());
        assertTrue(board.isRowFull(19));
        assertFalse(board.isRowFull(20));
        assertEquals(before, board.toString());
    }

    @Test
    void isRowFullOutsideBoardThrows() {
        Board board = new Board();
        assertThrows(IndexOutOfBoundsException.class, () -> board.isRowFull(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isRowFull(22));
    }

    @Test
    void clearFullRowsReturnsEmptyListWhenNothingIsFull() {
        Board board = new Board();
        fillRow(board, 21, Cell.I, 0);
        String before = board.toString();
        assertTrue(board.clearFullRows().isEmpty());
        assertEquals(before, board.toString());
    }

    @Test
    void clearingOneRowMovesUpperCellsDownByOne() {
        Board board = new Board();
        fillRow(board, 21, Cell.I, -1);
        board.lock(positions(20, 2, 19, 7), Cell.T);
        assertEquals(List.of(21), board.clearFullRows());
        assertEquals(Cell.T, board.getCell(21, 2));
        assertEquals(Cell.T, board.getCell(20, 7));
        assertEquals(Cell.EMPTY, board.getCell(20, 2));
        assertEquals(Cell.EMPTY, board.getCell(19, 7));
    }

    @Test
    void clearingSeparatedRowsKeepsRemainingRowsInOrder() {
        Board board = new Board();
        fillRow(board, 21, Cell.I, -1);
        fillRow(board, 20, Cell.S, 1);
        fillRow(board, 19, Cell.O, -1);
        fillRow(board, 18, Cell.Z, 8);
        assertEquals(List.of(19, 21), board.clearFullRows());
        // 남은 18번 줄과 20번 줄이 순서를 유지한 채 바닥으로 내려온다.
        assertEquals(Cell.S, board.getCell(21, 0));
        assertEquals(Cell.EMPTY, board.getCell(21, 1));
        assertEquals(Cell.Z, board.getCell(20, 0));
        assertEquals(Cell.EMPTY, board.getCell(20, 8));
        assertFalse(board.isRowFull(21));
        assertTrue(board.findFullRows().isEmpty());
        for (int col = 0; col < 10; col++) {
            assertEquals(Cell.EMPTY, board.getCell(19, col));
        }
    }

    @Test
    void clearingFourRowsAtOnceEmptiesTheBoard() {
        Board board = new Board();
        for (int row = 18; row <= 21; row++) {
            fillRow(board, row, Cell.I, -1);
        }
        assertEquals(List.of(18, 19, 20, 21), board.clearFullRows());
        assertEquals(new Board().toString(), board.toString());
    }

    @Test
    void bufferCellsMoveDownIntoVisibleAreaAfterClearing() {
        Board board = new Board();
        board.lock(positions(1, 4), Cell.J);
        fillRow(board, 21, Cell.I, -1);
        board.clearFullRows();
        assertEquals(Cell.J, board.getCell(2, 4));
        assertEquals(Cell.EMPTY, board.getCell(1, 4));
        assertEquals(Cell.EMPTY, board.getCell(0, 4));
    }

    @Test
    void refilledTopRowsDoNotShareTheSameArray() {
        Board board = new Board();
        fillRow(board, 21, Cell.I, -1);
        fillRow(board, 20, Cell.I, -1);
        board.clearFullRows();
        // 새로 채운 빈 줄들이 같은 배열을 공유하면 한 칸만 고정해도 여러 줄이 바뀐다.
        board.lock(positions(0, 0), Cell.T);
        assertEquals(Cell.EMPTY, board.getCell(1, 0));
        assertEquals(Cell.EMPTY, board.getCell(21, 0));
    }

    @Test
    void clearEmptiesEveryCell() {
        Board board = new Board();
        fillRow(board, 21, Cell.I, 5);
        board.lock(positions(0, 0), Cell.T);
        board.clear();
        assertEquals(new Board().toString(), board.toString());
    }

    @Test
    void copyIsIndependentOfOriginal() {
        Board original = new Board(5, 1, 4);
        original.lock(positions(5, 0), Cell.O);
        Board copy = original.copy();
        assertEquals(original.toString(), copy.toString());
        assertEquals(5, copy.getVisibleRows());
        assertEquals(1, copy.getBufferRows());
        assertEquals(4, copy.getColumns());
        copy.lock(positions(5, 1), Cell.S);
        original.lock(positions(4, 3), Cell.Z);
        assertEquals(Cell.EMPTY, original.getCell(5, 1));
        assertEquals(Cell.EMPTY, copy.getCell(4, 3));
    }

    @Test
    void visibleCellsExcludeBufferAndAreCopies() {
        Board board = new Board();
        board.lock(positions(1, 0), Cell.J);
        board.lock(positions(2, 0, 21, 9), Cell.T);
        Cell[][] visible = board.getVisibleCells();
        assertEquals(20, visible.length);
        for (Cell[] row : visible) {
            assertEquals(10, row.length);
        }
        assertEquals(Cell.T, visible[0][0]);
        assertEquals(Cell.T, visible[19][9]);
        visible[5][5] = Cell.Z;
        assertEquals(Cell.EMPTY, board.getCell(7, 5));
    }

    @Test
    void toStringShowsBufferSeparatorAndBlockLetters() {
        Board board = new Board(2, 1, 3);
        board.lock(positions(2, 0), Cell.L);
        assertEquals("...\n---\n...\nL..\n", board.toString());
    }
}
