package tetris.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import tetris.block.Block;
import tetris.block.ItemBlockFactory;
import tetris.block.ItemType;
import tetris.block.TetrominoType;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 무게추 모양(로컬): .OO. / OOOO. 원점 (x, y)에 두면 위 두 칸은 (y, x+1~x+2), 아래 네 칸은 (y+1, x~x+3).
 * 보드는 6줄(버퍼 없음) × 10칸을 쓴다.
 */
class WeightMoverTest {
    private static Block weight(int x, int y) {
        return new ItemBlockFactory().create(true, null, ItemType.WEIGHT, x, y);
    }

    private static void fill(Board board, int row, int fromCol, int toCol, Cell cell) {
        List<Position> line = new ArrayList<>();
        for (int col = fromCol; col <= toCol; col++) {
            line.add(new Position(row, col));
        }
        board.lock(line, cell);
    }

    @Test
    void movesSidewaysFreelyBeforeTouching() {
        Board board = new Board(6, 0, 10);
        Block block = weight(3, 0);
        WeightMover mover = new WeightMover(board, block);
        assertFalse(mover.isTouched());
        assertTrue(mover.tryMoveSideways(-1));
        assertTrue(mover.tryMoveSideways(1));
        assertTrue(mover.tryMoveSideways(1));
        assertEquals(4, block.getX());
    }

    @Test
    void wallsStillBlockSidewaysMove() {
        Board board = new Board(6, 0, 10);
        Block block = weight(0, 0);
        WeightMover mover = new WeightMover(board, block);
        assertFalse(mover.tryMoveSideways(-1));
        assertEquals(0, block.getX());
    }

    @Test
    void cannotMoveSidewaysAfterTouchingBlock() {
        Board board = new Board(6, 0, 10);
        board.lock(List.of(new Position(3, 3)), Cell.J);
        Block block = weight(3, 0);
        WeightMover mover = new WeightMover(board, block);
        assertTrue(mover.stepDown());   // 아래 네 칸이 row 2 → 바로 아래 (3,3)이 찬 칸이라 닿음
        assertTrue(mover.isTouched());
        assertFalse(mover.tryMoveSideways(1));
        assertEquals(3, block.getX());
    }

    @Test
    void touchingStaysTrueEvenAfterErasingThrough() {
        Board board = new Board(6, 0, 10);
        board.lock(List.of(new Position(3, 3)), Cell.J);
        WeightMover mover = new WeightMover(board, weight(3, 0));
        mover.stepDown();
        mover.stepDown();   // (3,3)을 지우며 내려감
        assertTrue(mover.isTouched());
        assertFalse(mover.tryMoveSideways(-1));
    }

    @Test
    void dropErasesEveryCellInItsPathAndLandsOnFloor() {
        Board board = new Board(6, 0, 10);
        fill(board, 4, 2, 7, Cell.J);
        fill(board, 5, 2, 7, Cell.J);
        Block block = weight(3, 0);
        WeightMover mover = new WeightMover(board, block);
        assertEquals(4, mover.dropToFloor());
        assertTrue(mover.isOnFloor());
        assertEquals(8, mover.getErasedCellCount(), "row 4·5의 col 3~6");
        for (int col = 3; col <= 6; col++) {
            assertTrue(board.isEmpty(4, col));
            assertTrue(board.isEmpty(5, col));
        }
        assertEquals(Cell.J, board.getCell(5, 2), "경로 밖은 남는다");
        assertEquals(Cell.J, board.getCell(5, 7));
        assertFalse(mover.stepDown(), "바닥에서는 더 내려가지 않는다");
    }

    @Test
    void erasedMultiplierGivesNoBonusAndLandingLocksAsWeight() {
        Board board = new Board(6, 0, 10);
        board.lock(List.of(new Position(3, 4)), Cell.T);
        board.markMultiplier(new Position(3, 4));
        Block block = weight(3, 0);
        new WeightMover(board, block).dropToFloor();
        LockResult result = new BlockLocker(new Random(1)).lock(board, block);
        assertEquals(new LockResult(false, 0, 0, 0, false), result);
        assertEquals(Cell.WEIGHT, board.getCell(5, 3));
        assertFalse(board.isMultiplier(3, 4));
    }

    @Test
    void landingThatCompletesBottomRowClearsItNormally() {
        Board board = new Board(6, 0, 10);
        fill(board, 5, 0, 2, Cell.J);
        fill(board, 5, 7, 9, Cell.J);
        Block block = weight(3, 0);
        new WeightMover(board, block).dropToFloor();
        LockResult result = new BlockLocker(new Random(1)).lock(board, block);
        assertEquals(1, result.fullRowsCleared());
        assertEquals(Cell.WEIGHT, board.getCell(5, 4), "위 두 칸이 한 줄 내려온다");
        assertEquals(Cell.WEIGHT, board.getCell(5, 5));
    }

    @Test
    void rejectsNonWeightBlocksAndInvalidMoves() {
        Board board = new Board(6, 0, 10);
        assertThrows(IllegalArgumentException.class, () -> new WeightMover(board, new Block(TetrominoType.T)));
        assertThrows(NullPointerException.class, () -> new WeightMover(null, weight(3, 0)));
        assertThrows(NullPointerException.class, () -> new WeightMover(board, null));
        WeightMover mover = new WeightMover(board, weight(3, 0));
        assertThrows(IllegalArgumentException.class, () -> mover.tryMoveSideways(2));
        assertThrows(IllegalArgumentException.class, () -> mover.tryMoveSideways(0));
    }
}
