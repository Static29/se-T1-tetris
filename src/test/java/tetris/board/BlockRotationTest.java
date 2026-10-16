package tetris.board;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tetris.block.Block;
import tetris.block.ItemBlockFactory;
import tetris.block.ItemType;
import tetris.block.TetrominoType;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Block의 x, y는 회전 공간(J·L·S·T·Z 3×3, I 4×4)의 왼쪽 위이며 row = y, col = x다.
 * 회전 상태: 0 = 생성, 1 = R, 2 = 180도, 3 = L. 시계 방향만 회전한다.
 */
class BlockRotationTest {
    /** (row, col) 쌍을 순서대로 받아 좌표 목록을 만든다. 예: positions(0, 0, 0, 1) */
    private static List<Position> positions(int... rowCols) {
        List<Position> result = new ArrayList<>();
        for (int i = 0; i < rowCols.length; i += 2) {
            result.add(new Position(rowCols[i], rowCols[i + 1]));
        }
        return result;
    }

    /** 칸 순서와 관계없이 비교하기 위해 블록의 점유 칸을 집합으로 만든다. */
    private static Set<Position> occupied(Block block) {
        return Set.copyOf(Position.fromXY(block.getBoardCells()));
    }

    /** 회전 검사 없이 times번 회전한 블록. 시험할 회전 상태를 만들 때 쓴다. */
    private static Block rotatedBlock(TetrominoType type, int x, int y, int times) {
        Block block = new Block(type, x, y);
        for (int i = 0; i < times; i++) {
            block.rotate();
        }
        return block;
    }

    /** emptyPositions를 제외한 모든 칸을 채운다. */
    private static void fillAllExcept(Board board, Collection<Position> emptyPositions) {
        List<Position> filled = new ArrayList<>();
        for (int row = 0; row < board.getTotalRows(); row++) {
            for (int col = 0; col < board.getColumns(); col++) {
                Position position = new Position(row, col);
                if (!emptyPositions.contains(position)) {
                    filled.add(position);
                }
            }
        }
        board.lock(filled, Cell.Z);
    }

    @Test
    void rotatesInPlaceWhenBasicRotationFits() {
        Board board = new Board();
        Block block = new Block(TetrominoType.T, 3, 10);
        assertEquals(OptionalInt.of(0), BlockRotation.findRotation(board, block));
        assertTrue(BlockRotation.tryRotate(board, block));
        assertEquals(1, block.getRotationState());
        assertEquals(3, block.getX());
        assertEquals(10, block.getY());
        assertEquals(Set.copyOf(positions(10, 4, 11, 4, 11, 5, 12, 4)), occupied(block));
    }

    @Test
    void findRotationDoesNotChangeBlockOrBoard() {
        Board board = new Board();
        Block block = rotatedBlock(TetrominoType.T, -1, 10, 1);
        Set<Position> before = occupied(block);
        String boardBefore = board.toString();
        BlockRotation.findRotation(board, block);
        assertEquals(before, occupied(block));
        assertEquals(1, block.getRotationState());
        assertEquals(boardBefore, board.toString());
    }

    @Test
    void tPieceAgainstLeftWallIsPushedRight() {
        // R 상태 T가 왼쪽 벽 col 0에 붙어 있음(회전 공간 x = -1). R -> 2 기본 회전은 col -1로 벽을 뚫는다.
        Board board = new Board();
        Block block = rotatedBlock(TetrominoType.T, -1, 10, 1);
        assertEquals(OptionalInt.of(1), BlockRotation.findRotation(board, block));
        assertTrue(BlockRotation.tryRotate(board, block));
        assertEquals(2, block.getRotationState());
        assertEquals(0, block.getX());
        assertEquals(Set.copyOf(positions(11, 0, 11, 1, 11, 2, 12, 1)), occupied(block));
    }

    @Test
    void iPieceAgainstRightWallIsPushedLeft() {
        // R 상태 세로 I가 오른쪽 벽 col 9에 붙어 있음(회전 공간 x = 7). R -> 2 기본 회전은 col 7~10이다.
        Board board = new Board();
        Block block = rotatedBlock(TetrominoType.I, 7, 10, 1);
        assertTrue(BlockRotation.tryRotate(board, block));
        assertEquals(6, block.getX());
        assertEquals(Set.copyOf(positions(12, 6, 12, 7, 12, 8, 12, 9)), occupied(block));
    }

    @Test
    void blockedFirstKickFallsThroughToLaterCandidate() {
        // L 상태 세로 I가 오른쪽 벽 col 9에 붙어 있음(회전 공간 x = 8). L -> 0 후보 1 (+1, 0)도 벽 밖이라
        // 후보 2 (-2, 0)이 선택된다.
        Board board = new Board();
        Block block = rotatedBlock(TetrominoType.I, 8, 10, 3);
        assertEquals(OptionalInt.of(2), BlockRotation.findRotation(board, block));
        assertTrue(BlockRotation.tryRotate(board, block));
        assertEquals(0, block.getRotationState());
        assertEquals(Set.copyOf(positions(11, 6, 11, 7, 11, 8, 11, 9)), occupied(block));
    }

    @Test
    void pieceSqueezesTwoRowsDownWhenOnlyThatCandidateFits() {
        // T 0 -> R 후보 0~2 자리는 막혀 있고 후보 3 (아래로 2칸) 자리만 비어 있다.
        Board board = new Board(6, 0, 10);
        fillAllExcept(board, positions(3, 4, 4, 4, 4, 5, 5, 4));
        Block block = new Block(TetrominoType.T, 3, 1);
        assertEquals(OptionalInt.of(3), BlockRotation.findRotation(board, block));
        assertTrue(BlockRotation.tryRotate(board, block));
        assertEquals(3, block.getY());
        assertEquals(Set.copyOf(positions(3, 4, 4, 4, 4, 5, 5, 4)), occupied(block));
    }

    @Test
    void tSpinDoubleSlotAcceptsRotationUnderOverhang() {
        // row 3: ...X......   (col 3이 지붕처럼 덮고 있음)
        // row 4: XXX...XXXX
        // row 5: XXXX.XXXXX
        Board board = new Board(6, 0, 10);
        board.lock(positions(3, 3), Cell.J);
        for (int col = 0; col < 10; col++) {
            if (col < 3 || col > 5) {
                board.lock(positions(4, col), Cell.J);
            }
            if (col != 4) {
                board.lock(positions(5, col), Cell.J);
            }
        }
        // 아래를 향한 T(2 상태)는 지붕 때문에 위에서 그대로 떨어뜨려 넣을 수 없다.
        assertFalse(board.canPlace(positions(3, 3, 3, 4, 3, 5, 4, 4)));
        // R 상태 T(회전 공간 x = 3, y = 3)는 들어갈 수 있고, 여기서 R -> 2로 돌리면 구멍에 끼워진다.
        Block block = rotatedBlock(TetrominoType.T, 3, 3, 1);
        assertTrue(board.canPlace(Position.fromXY(block.getBoardCells())));
        assertTrue(BlockRotation.tryRotate(board, block));
        board.lock(Position.fromXY(block.getBoardCells()), Cell.valueOf(block.getType().name()));
        assertEquals(List.of(4, 5), board.findFullRows());
    }

    @Test
    void everyCandidateBlockedLeavesBlockUnchanged() {
        Board board = new Board(6, 0, 10);
        Block block = new Block(TetrominoType.T, 3, 1);
        fillAllExcept(board, Position.fromXY(block.getBoardCells()));
        Set<Position> before = occupied(block);
        assertEquals(OptionalInt.empty(), BlockRotation.findRotation(board, block));
        assertFalse(BlockRotation.tryRotate(board, block));
        assertEquals(0, block.getRotationState());
        assertEquals(3, block.getX());
        assertEquals(1, block.getY());
        assertEquals(before, occupied(block));
    }

    @Test
    void oPieceKeepsShapeAndPosition() {
        Board board = new Board();
        Block block = new Block(TetrominoType.O, 4, 10);
        Set<Position> before = occupied(block);
        assertTrue(BlockRotation.tryRotate(board, block));
        assertEquals(0, block.getRotationState());
        assertEquals(before, occupied(block));
    }

    @Test
    void weightItemWithoutTetrominoTypeCanBeChecked() {
        // 무게추는 getType()이 null이다. 회전 판정은 종류를 쓰지 않으므로 예외 없이 모양을 유지한다.
        Board board = new Board();
        Block weight = new ItemBlockFactory().create(true, null, ItemType.WEIGHT, 3, 10);
        Set<Position> before = occupied(weight);
        assertTrue(BlockRotation.tryRotate(board, weight));
        assertEquals(before, occupied(weight));
    }

    @Test
    void rejectsNullBoardOrBlock() {
        Block block = new Block(TetrominoType.T, 3, 10);
        assertThrows(NullPointerException.class, () -> BlockRotation.findRotation(null, block));
        assertThrows(NullPointerException.class, () -> BlockRotation.findRotation(new Board(), null));
        assertThrows(NullPointerException.class, () -> BlockRotation.tryRotate(null, block));
    }
}
