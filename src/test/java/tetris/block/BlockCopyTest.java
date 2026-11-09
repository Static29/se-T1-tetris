package tetris.block;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Next/Hold 등에서 사용하는 복사 계약을 기존 구현과 리팩토링 모두에 적용한다. */
class BlockCopyTest {
    static Stream<Arguments> blockCases() {
        Stream<Arguments> ordinary = Arrays.stream(TetrominoType.values())
                .map(type -> Arguments.of(type, null, -1));
        Stream<Arguments> attached = Arrays.stream(TetrominoType.values()).flatMap(type ->
                Arrays.stream(ItemType.values()).filter(item -> item != ItemType.WEIGHT)
                        .flatMap(item -> IntStream.range(0, 4)
                                .mapToObj(index -> Arguments.of(type, item, index))));
        return Stream.concat(Stream.concat(ordinary, attached),
                Stream.of(Arguments.of(null, ItemType.WEIGHT, -1)));
    }

    private static Block create(TetrominoType type, ItemType item, int index) {
        if (item == ItemType.WEIGHT) return Block.weight(3, 5);
        return item == null ? new Block(type, 3, 5) : Block.withItem(type, 3, 5, item, index);
    }

    @ParameterizedTest(name = "copy: {0}, {1}, cell={2}")
    @MethodSource("blockCases")
    void copyPreservesStateAndIsIndependent(TetrominoType type, ItemType item, int index) {
        Block original = create(type, item, index);
        for (int state = 0; state < 4; state++) {
            Block copy = original.copy();
            assertNotSame(original, copy);
            assertEquals(original.getType(), copy.getType());
            assertEquals(original.getItemType(), copy.getItemType());
            assertEquals(original.getX(), copy.getX());
            assertEquals(original.getY(), copy.getY());
            assertEquals(original.getRotationState(), copy.getRotationState());
            assertTrue(Arrays.deepEquals(original.getCells(), copy.getCells()));
            assertTrue(Arrays.deepEquals(original.getBoardItemCells(), copy.getBoardItemCells()));
            assertTrue(Arrays.deepEquals(original.getRotationOffsets(), copy.getRotationOffsets()));

            int[][] sourceCells = original.getBoardCells();
            copy.rotate();
            copy.move(10, -3);
            copy.getCells()[0][0] = 999;
            assertTrue(Arrays.deepEquals(sourceCells, original.getBoardCells()));
            int[][] copiedCells = copy.getBoardCells();
            original.rotate();
            original.move(-1, 2);
            assertTrue(Arrays.deepEquals(copiedCells, copy.getBoardCells()));
        }
    }

    @ParameterizedTest(name = "spawnCopy: {0}, {1}, cell={2}")
    @MethodSource("blockCases")
    void spawnCopyResetsPoseButPreservesAttachedCell(TetrominoType type, ItemType item, int index) {
        Block original = create(type, item, index);
        int[][] spawnShape = original.getCells();
        int[][] spawnMarker = original.getItemCells();
        for (int state = 0; state < 4; state++) {
            int[][] sourceCells = original.getBoardCells();
            int[][] sourceMarker = original.getBoardItemCells();
            int sourceState = original.getRotationState();
            Block reset = original.spawnCopy(-2, 7);
            assertNotSame(original, reset);
            assertEquals(type, reset.getType());
            assertEquals(item, reset.getItemType());
            assertEquals(-2, reset.getX());
            assertEquals(7, reset.getY());
            assertEquals(0, reset.getRotationState());
            assertTrue(Arrays.deepEquals(spawnShape, reset.getCells()));
            assertTrue(Arrays.deepEquals(spawnMarker, reset.getItemCells()));
            reset.rotate();
            reset.move(1, 1);
            assertTrue(Arrays.deepEquals(sourceCells, original.getBoardCells()));
            assertTrue(Arrays.deepEquals(sourceMarker, original.getBoardItemCells()));
            assertEquals(sourceState, original.getRotationState());
            original.rotate();
            original.move(2, 1);
        }
    }
}
