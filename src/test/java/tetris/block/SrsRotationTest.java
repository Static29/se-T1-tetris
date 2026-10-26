package tetris.block;

import static org.junit.jupiter.api.Assertions.*;
import static tetris.block.BlockTest.cells;

import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SrsRotationTest {
    // 문헌의 y-up 표를 테스트 기준으로 사용하고 API의 y-down 좌표와 비교한다.
    private static final int[][][] JLSTZ_UP = {
        {{0,0},{-1,0},{-1,1},{0,-2},{-1,-2}},
        {{0,0},{1,0},{1,-1},{0,2},{1,2}},
        {{0,0},{1,0},{1,1},{0,-2},{1,-2}},
        {{0,0},{-1,0},{-1,-1},{0,2},{-1,2}}
    };
    private static final int[][][] I_UP = {
        {{0,0},{-2,0},{1,0},{-2,-1},{1,2}},
        {{0,0},{-1,0},{2,0},{-1,2},{2,-1}},
        {{0,0},{2,0},{-1,0},{2,1},{-1,-2}},
        {{0,0},{1,0},{-2,0},{1,-2},{-2,1}}
    };

    @ParameterizedTest
    @EnumSource(value = TetrominoType.class, names = "O", mode = EnumSource.Mode.EXCLUDE)
    void clockwiseShapeUsesFixedPivot(TetrominoType type) {
        Map<TetrominoType, int[][]> expected = Map.of(
            TetrominoType.I, new int[][] {{2,0},{2,1},{2,2},{2,3}},
            TetrominoType.T, new int[][] {{1,0},{1,1},{2,1},{1,2}},
            TetrominoType.J, new int[][] {{1,0},{2,0},{1,1},{1,2}},
            TetrominoType.L, new int[][] {{1,0},{1,1},{1,2},{2,2}},
            TetrominoType.S, new int[][] {{1,0},{1,1},{2,1},{2,2}},
            TetrominoType.Z, new int[][] {{2,0},{1,1},{2,1},{1,2}});
        Block block = new Block(type);
        block.rotate();
        assertEquals(cells(expected.get(type)), cells(block.getCells()));
        assertEquals(type == TetrominoType.I ? 4 : 3, block.getFrameSize());
    }

    @ParameterizedTest
    @EnumSource(value = TetrominoType.class, names = "O", mode = EnumSource.Mode.EXCLUDE)
    void everyTransitionProvidesOrderedCandidatesWithoutMutation(TetrominoType type) {
        for (int state = 0; state < 4; state++) {
            for (int candidate = 0; candidate < 5; candidate++) {
                Block block = new Block(type, 3, 5);
                for (int turn = 0; turn < state; turn++) block.rotate();
                int[] reference = (type == TetrominoType.I ? I_UP : JLSTZ_UP)[state][candidate];
                assertArrayEquals(new int[] {reference[0], -reference[1]},
                        block.getRotationOffsets()[candidate], type + " state=" + state);
                int[][] before = block.getBoardCells();
                int[][] base = block.previewRotation();
                int[][] preview = block.previewRotation(candidate);
                for (int i = 0; i < base.length; i++) {
                    assertArrayEquals(new int[] {base[i][0] + reference[0], base[i][1] - reference[1]}, preview[i]);
                }
                assertTrue(Arrays.deepEquals(before, block.getBoardCells()));
                assertEquals(state, block.getRotationState());
                block.rotate(candidate);
                assertTrue(Arrays.deepEquals(preview, block.getBoardCells()));
                assertEquals(3 + reference[0], block.getX());
                assertEquals(5 - reference[1], block.getY());
                assertEquals((state + 1) % 4, block.getRotationState());
            }
        }
    }

    @Test
    void knownWallAndFloorCasesHaveUsableKickCandidates() {
        Block wall = new Block(TetrominoType.T, -1, 5);
        wall.rotate(); // R -> 2では右1マスの候補が左壁を回避する。
        assertFalse(fits(wall.previewRotation(0)));
        assertTrue(fits(wall.previewRotation(1)));
        wall.rotate(1);
        assertEquals(0, wall.getX());

        Block floor = new Block(TetrominoType.T, 3, 18);
        assertFalse(fits(floor.previewRotation(0)));
        assertFalse(fits(floor.previewRotation(1)));
        assertTrue(fits(floor.previewRotation(2)));
        floor.rotate(2);
        assertEquals(17, floor.getY());
    }

    // 검증 전용 20x10 경계. 실제 Board의 구현/정책을 정의하지 않는다.
    private static boolean fits(int[][] cells) {
        return Arrays.stream(cells).allMatch(c -> c[0] >= 0 && c[0] < 10 && c[1] >= 0 && c[1] < 20);
    }

    @Test
    void rejectedOrInvalidCandidatesPreserveAllState() {
        Block block = Block.withItem(TetrominoType.T, 3, 5, ItemType.LINE_CLEAR, 0);
        int[][] before = block.getBoardCells();
        int[][] marker = block.getBoardItemCells();
        // 모든 후보를 살펴봐도 적용하지 않으면 상태가 바뀌지 않는다.
        for (int i = 0; i < 5; i++) block.previewRotation(i);
        for (int invalid : new int[] {-1, 5}) {
            assertThrows(IllegalArgumentException.class, () -> block.previewRotation(invalid));
            assertThrows(IllegalArgumentException.class, () -> block.rotate(invalid));
        }
        assertTrue(Arrays.deepEquals(before, block.getBoardCells()));
        assertTrue(Arrays.deepEquals(marker, block.getBoardItemCells()));
        assertEquals(0, block.getRotationState());
        assertEquals(3, block.getX());
        assertEquals(5, block.getY());
    }

    @Test
    void iSpawnsOnSecondRowAndODoesNotKick() {
        assertEquals(cells(new int[][] {{0,1},{1,1},{2,1},{3,1}}), cells(new Block(TetrominoType.I).getCells()));
        Block o = new Block(TetrominoType.O);
        int[][] original = o.getCells();
        assertEquals(1, o.getRotationOffsets().length);
        assertEquals(2, o.getFrameSize());
        o.rotate();
        assertTrue(Arrays.deepEquals(original, o.getCells()));
        assertThrows(IllegalArgumentException.class, () -> o.rotate(1));
    }
}
