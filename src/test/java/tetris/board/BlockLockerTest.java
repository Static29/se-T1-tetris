package tetris.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import tetris.block.Block;
import tetris.block.ItemBlockFactory;
import tetris.block.ItemType;
import tetris.block.TetrominoType;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 가로 I 블록은 회전 공간 둘째 줄(로컬 y = 1)에 있다. 원점 (x, y)에 두면 row = y + 1, col = x ~ x+3.
 * 아이템 칸 번호 k는 I의 왼쪽에서 k번째 칸(col = x + k)이다.
 */
class BlockLockerTest {
    private static final long SEED = 20261026L;
    private final BlockLocker locker = new BlockLocker(new Random(SEED));

    /** nextInt가 항상 value를 돌려주는 난수. 아이템이 붙는 칸 번호를 고정할 때 쓴다. */
    private static RandomGenerator fixed(int value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() { return 0; }

            @Override
            public int nextInt(int bound) { return value; }
        };
    }

    /** 원점 (x, y)에 있는 가로 I 블록. item이 null이면 일반 블록. */
    private static Block iBlock(int x, int y, ItemType item, int itemIndex) {
        if (item == null) {
            return new Block(TetrominoType.I, x, y);
        }
        return new ItemBlockFactory(fixed(itemIndex)).create(true, TetrominoType.I, item, x, y);
    }

    private static List<Position> positions(int... rowCols) {
        List<Position> result = new ArrayList<>();
        for (int i = 0; i < rowCols.length; i += 2) {
            result.add(new Position(rowCols[i], rowCols[i + 1]));
        }
        return result;
    }

    /** row 줄에서 fromCol ~ toCol 칸을 cell로 채운다. */
    private static void fill(Board board, int row, int fromCol, int toCol, Cell cell) {
        List<Position> line = new ArrayList<>();
        for (int col = fromCol; col <= toCol; col++) {
            line.add(new Position(row, col));
        }
        board.lock(line, cell);
    }

    private static int countFilled(Board board) {
        int count = 0;
        for (int row = 0; row < board.getTotalRows(); row++) {
            for (int col = 0; col < board.getColumns(); col++) {
                if (!board.isEmpty(row, col)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void normalBlockLocksAndClearsFullRows() {
        Board board = new Board(6, 0, 10);
        fill(board, 5, 0, 2, Cell.J);
        fill(board, 5, 7, 9, Cell.J);
        LockResult result = locker.lock(board, iBlock(3, 4, null, 0));
        assertEquals(new LockResult(false, 1, 0, 0, false), result);
        assertEquals(0, countFilled(board));
    }

    @Test
    void lockOutStopsBeforeEffectsAndRowClears() {
        Board board = new Board();
        // 원점 y = -1이면 I가 row 0(버퍼)에만 놓인다.
        LockResult result = locker.lock(board, iBlock(3, -1, ItemType.LINE_CLEAR, 0));
        assertTrue(result.lockOut());
        assertEquals(LockResult.LOCK_OUT, result);
        assertEquals(Cell.I, board.getCell(0, 3), "Lock out이어도 블록은 고정된 상태로 남는다");
    }

    @Test
    void lineClearItemRemovesItsRowEvenIfNotFull() {
        Board board = new Board(6, 0, 10);
        fill(board, 5, 0, 1, Cell.J);
        board.lock(positions(4, 0), Cell.T);
        LockResult result = locker.lock(board, iBlock(3, 4, ItemType.LINE_CLEAR, 2));
        // row 5: J 2칸 + I 4칸 = 6칸 삭제. 위 줄의 T가 한 칸 내려온다.
        assertEquals(new LockResult(false, 0, 1, 6, false), result);
        assertEquals(1, result.totalRowsCleared());
        assertEquals(Cell.T, board.getCell(5, 0));
        assertEquals(1, countFilled(board));
    }

    @Test
    void lineClearOnRowWithMultiplierDoublesScore() {
        Board board = new Board(6, 0, 10);
        board.lock(positions(5, 0), Cell.J);
        board.markMultiplier(new Position(5, 0));
        LockResult result = locker.lock(board, iBlock(3, 4, ItemType.LINE_CLEAR, 0));
        assertTrue(result.multiplierApplied());
        assertFalse(board.isMultiplier(5, 0), "배율 표시도 줄과 함께 사라진다");
    }

    @Test
    void fillItemFillsEmptyCellsBelowItToTheFloorOnly() {
        Board board = new Board(6, 0, 10);
        board.lock(positions(4, 4), Cell.J);
        // I가 row 2, col 3~6. F는 col 4.
        LockResult result = locker.lock(board, iBlock(3, 1, ItemType.FILL, 1));
        assertEquals(new LockResult(false, 0, 0, 0, false), result);
        assertEquals(Cell.I, board.getCell(3, 4));
        assertEquals(Cell.J, board.getCell(4, 4), "이미 찬 칸은 그대로");
        assertEquals(Cell.I, board.getCell(5, 4));
        assertTrue(board.isEmpty(1, 4), "F 위쪽은 채우지 않는다");
        assertTrue(board.isEmpty(3, 3), "다른 열은 채우지 않는다");
    }

    @Test
    void fillThatCompletesRowsClearsThemNormally() {
        Board board = new Board(6, 0, 10);
        fill(board, 5, 0, 3, Cell.J);
        fill(board, 5, 5, 9, Cell.J);
        LockResult result = locker.lock(board, iBlock(3, 2, ItemType.FILL, 1));
        // F가 (4,4)~(5,4)를 채워 row 5가 가득 찬다.
        assertEquals(1, result.fullRowsCleared());
    }

    @Test
    void bombRemovesItselfAndFourOfItsFilledNeighbors() {
        Board board = new Board(6, 0, 10);
        fill(board, 2, 3, 5, Cell.J);
        fill(board, 4, 3, 5, Cell.J);
        // I가 row 3, col 3~6. B는 (3,4). 주변 8칸이 모두 찬 상태.
        LockResult result = locker.lock(board, iBlock(3, 2, ItemType.BOMB, 1));
        assertEquals(5, result.itemCellsRemoved(), "B 1칸 + 주변 4칸");
        assertTrue(board.isEmpty(3, 4));
        int remainingInSquare = 0;
        for (int row = 2; row <= 4; row++) {
            for (int col = 3; col <= 5; col++) {
                if (!board.isEmpty(row, col)) {
                    remainingInSquare++;
                }
            }
        }
        assertEquals(4, remainingInSquare);
        assertEquals(Cell.I, board.getCell(3, 6), "3×3 밖은 지우지 않는다");
    }

    @Test
    void bombWithFewerThanFourNeighborsRemovesOnlyThose() {
        Board board = new Board(6, 0, 10);
        // I가 row 5, col 0~3. B는 왼쪽 끝 (5,0). 찬 주변 칸은 (5,1) 하나.
        LockResult result = locker.lock(board, iBlock(0, 4, ItemType.BOMB, 0));
        assertEquals(2, result.itemCellsRemoved());
        assertTrue(board.isEmpty(5, 0));
        assertTrue(board.isEmpty(5, 1));
        assertEquals(2, countFilled(board));
    }

    @Test
    void bombRemovingMultiplierCellDoublesScore() {
        Board board = new Board(6, 0, 10);
        board.lock(positions(4, 0), Cell.J);
        board.markMultiplier(new Position(4, 0));
        LockResult result = locker.lock(board, iBlock(0, 4, ItemType.BOMB, 0));
        assertTrue(result.multiplierApplied());
        assertEquals(3, result.itemCellsRemoved());
    }

    @Test
    void multiplierItemLeavesMarkUntilItsRowIsCleared() {
        Board board = new Board(6, 0, 10);
        LockResult first = locker.lock(board, iBlock(0, 4, ItemType.MULTIPLIER, 1));
        assertFalse(first.multiplierApplied());
        assertTrue(board.isMultiplier(5, 1));
        // 같은 줄을 일반 블록으로 채워 지우면 그때 2배.
        fill(board, 5, 4, 5, Cell.J);
        LockResult second = locker.lock(board, iBlock(6, 4, null, 0));
        assertEquals(new LockResult(false, 1, 0, 0, true), second);
        assertFalse(board.isMultiplier(5, 1));
    }

    @Test
    void multiplierBlockThatCompletesItsOwnRowAppliesImmediately() {
        Board board = new Board(6, 0, 10);
        fill(board, 5, 0, 2, Cell.J);
        fill(board, 5, 7, 9, Cell.J);
        LockResult result = locker.lock(board, iBlock(3, 4, ItemType.MULTIPLIER, 2));
        assertEquals(new LockResult(false, 1, 0, 0, true), result);
    }

    @Test
    void weightIsLockedAsWeightCell() {
        Board board = new Board(6, 0, 10);
        Block weight = new ItemBlockFactory().create(true, null, ItemType.WEIGHT, 3, 4);
        assertEquals(Cell.WEIGHT, BlockLocker.cellOf(weight));
        LockResult result = locker.lock(board, weight);
        assertEquals(new LockResult(false, 0, 0, 0, false), result);
        assertEquals(Cell.WEIGHT, board.getCell(5, 3));
        assertEquals(Cell.WEIGHT, board.getCell(4, 4));
    }

    @Test
    void failedLockChangesNothing() {
        Board board = new Board(6, 0, 10);
        board.lock(positions(5, 4), Cell.J);
        String before = board.toString();
        assertThrows(IllegalArgumentException.class, () -> locker.lock(board, iBlock(3, 4, ItemType.BOMB, 0)));
        assertEquals(before, board.toString());
    }

    @Test
    void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> new BlockLocker(null));
        assertThrows(NullPointerException.class, () -> locker.lock(null, iBlock(3, 4, null, 0)));
        assertThrows(NullPointerException.class, () -> locker.lock(new Board(), null));
        assertThrows(NullPointerException.class, () -> BlockLocker.cellOf(null));
        assertThrows(IllegalArgumentException.class, () -> new LockResult(false, -1, 0, 0, false));
    }
}
