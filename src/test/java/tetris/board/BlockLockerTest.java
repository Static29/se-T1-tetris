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
import static tetris.board.BoardFixtures.*;

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
        assertThrows(NullPointerException.class, () -> new LockResult(false, 0, 0, 0, false, null));
    }

    // ---- 대전 모드 공격 줄(Req3) ----
    // T를 원점 (3, 4)에 두면 (4,4), (5,3), (5,4), (5,5)를 차지한다.

    /** row 줄을 emptyCols만 남기고 J로 채운다. */
    private static void fillExcept(Board board, int row, Integer... emptyCols) {
        List<Integer> empty = List.of(emptyCols);
        List<Position> line = new ArrayList<>();
        for (int col = 0; col < board.getColumns(); col++) {
            if (!empty.contains(col)) {
                line.add(new Position(row, col));
            }
        }
        board.lock(line, Cell.J);
    }

    /** holeCols만 빈 10칸짜리 공격 줄. */
    private static GarbageRow holes(Integer... holeCols) {
        return GarbageRow.withHoles(10, List.of(holeCols));
    }

    /** 원점 (3, 4)에 있는 T. item이 null이면 일반 블록. */
    private static Block tBlock(ItemType item, int itemIndex) {
        if (item == null) {
            return new Block(TetrominoType.T, 3, 4);
        }
        return new ItemBlockFactory(fixed(itemIndex)).create(true, TetrominoType.T, item, 3, 4);
    }

    @Test
    void twoFullRowsAreSentWithHolesWhereTheLastBlockWas() {
        Board board = new Board(6, 0, 10);
        fillExcept(board, 4, 4);
        fillExcept(board, 5, 3, 4, 5);
        board.lock(positions(3, 0), Cell.S);
        LockResult result = locker.lock(board, tBlock(null, 0));
        assertEquals(2, result.fullRowsCleared());
        assertEquals(List.of(holes(4), holes(3, 4, 5)), result.attackRows(), "위에서 아래 순서");
        assertEquals(Cell.S, board.getCell(5, 0));
    }

    @Test
    void oneRowIsNotSent() {
        Board board = new Board(6, 0, 10);
        fillExcept(board, 5, 3, 4, 5);
        LockResult result = locker.lock(board, tBlock(null, 0));
        assertEquals(1, result.fullRowsCleared());
        assertEquals(List.of(), result.attackRows());
    }

    @Test
    void lineClearRowIsSentFilledExceptTheBlockCellsAndCountsWithFullRows() {
        // L이 아래 줄(row 5)에 붙은 T를 고른다. row 5는 L로 지워지고, 내려온 row 4는 꽉 차서 지워진다.
        int index = 0;
        while (Position.fromXY(tBlock(ItemType.LINE_CLEAR, index).getBoardItemCells()).get(0).row() != 5) {
            index++;
        }
        Board board = new Board(6, 0, 10);
        fillExcept(board, 4, 4);
        board.lock(positions(5, 0, 5, 1), Cell.J);
        LockResult result = locker.lock(board, tBlock(ItemType.LINE_CLEAR, index));
        assertEquals(1, result.itemRowsCleared());
        assertEquals(1, result.fullRowsCleared());
        // L 줄(row 5)은 원래 비어 있던 칸도 채워서 보내고, T가 있던 3~5칸만 비운다.
        assertEquals(List.of(holes(4), holes(3, 4, 5)), result.attackRows());
    }

    @Test
    void fillRowsAreSentWithFilledCellsAndBlockCellsEmpty() {
        // I가 row 3, col 3~6. F(col 4)가 (4,4), (5,4)를 채워 3줄이 모두 찬다.
        Board board = new Board(6, 0, 10);
        fillExcept(board, 3, 3, 4, 5, 6);
        fillExcept(board, 4, 4);
        fillExcept(board, 5, 4);
        LockResult result = locker.lock(board, iBlock(3, 2, ItemType.FILL, 1));
        assertEquals(3, result.fullRowsCleared());
        assertEquals(List.of(holes(3, 4, 5, 6), holes(4), holes(4)), result.attackRows());
    }

    @Test
    void multiplierBlockAttacksLikeANormalBlockAndOldMarksAreSentFilled() {
        Board board = new Board(6, 0, 10);
        fillExcept(board, 4, 4);
        fillExcept(board, 5, 3, 4, 5);
        board.markMultiplier(new Position(5, 0));
        LockResult result = locker.lock(board, tBlock(ItemType.MULTIPLIER, 0));
        assertTrue(result.multiplierApplied());
        assertEquals(List.of(holes(4), holes(3, 4, 5)), result.attackRows());
    }

    @Test
    void bombBlockNeverAttacks() {
        // 세로 I. 폭탄 주변 칸이 지워져도 폭탄에서 먼 두 줄은 꽉 차서 지워진다.
        Board board = new Board(8, 0, 10);
        Block bomb = new ItemBlockFactory(fixed(0)).create(true, TetrominoType.I, ItemType.BOMB, 3, 4);
        bomb.rotate();
        for (Position cell : Position.fromXY(bomb.getBoardCells())) {
            fillExcept(board, cell.row(), cell.col());
        }
        LockResult result = locker.lock(board, bomb);
        assertTrue(result.fullRowsCleared() >= BlockLocker.MIN_ATTACK_ROWS);
        assertEquals(List.of(), result.attackRows());
    }

    @Test
    void weightNeverAttacks() {
        // 무게추를 원점 (3, 4)에 두면 (4,4), (4,5), (5,3)~(5,6)을 차지한다.
        Board board = new Board(6, 0, 10);
        fillExcept(board, 4, 4, 5);
        fillExcept(board, 5, 3, 4, 5, 6);
        LockResult result = locker.lock(board, new ItemBlockFactory().create(true, null, ItemType.WEIGHT, 3, 4));
        assertEquals(2, result.fullRowsCleared());
        assertEquals(List.of(), result.attackRows());
    }
}
