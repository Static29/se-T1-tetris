package tetris.block;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ItemBlockFactoryTest {
    static Stream<Arguments> attachments() {
        return Arrays.stream(TetrominoType.values()).flatMap(type ->
            Arrays.stream(ItemType.values()).filter(item -> item != ItemType.WEIGHT).flatMap(item ->
                java.util.stream.IntStream.range(0, 4).mapToObj(cell -> Arguments.of(type, item, cell))));
    }

    @ParameterizedTest
    @MethodSource("attachments")
    void attachedItemFollowsItsCellThroughRotationAndMovement(TetrominoType type, ItemType item, int index) {
        Block block = Block.withItem(type, 3, 5, item, index);
        assertEquals(type, block.getType());
        assertEquals(item, block.getItemType());
        assertTrue(block.hasItem());
        assertEquals(1, block.getItemCells().length);
        for (int turn = 0; turn < 4; turn++) {
            int[] old = block.getItemCells()[0];
            int candidate = type == TetrominoType.O ? 0 : 2;
            int[] kick = block.getRotationOffsets()[candidate];
            int x = block.getX(), y = block.getY();
            block.rotate(candidate);
            int[] expected = type == TetrominoType.O ? old : new int[] {block.getFrameSize()-1-old[1], old[0]};
            assertArrayEquals(expected, block.getItemCells()[0]);
            assertArrayEquals(new int[] {expected[0]+x+kick[0], expected[1]+y+kick[1]}, block.getBoardItemCells()[0]);
            long markedCells = Arrays.stream(block.getCells())
                .filter(c -> block.getCellSymbol(c[0], c[1]) == item.getSymbol()).count();
            assertEquals(1, markedCells);
        }
        int[] before = block.getBoardItemCells()[0];
        block.move(-2, 3);
        assertArrayEquals(new int[] {before[0]-2, before[1]+3}, block.getBoardItemCells()[0]);
        int[] local = block.getItemCells()[0];
        block.getItemCells()[0][0] = 999;
        block.getBoardItemCells()[0][0] = 999;
        assertArrayEquals(local, block.getItemCells()[0]);
    }

    @Test
    void weightHasIndependentShapeAndNoAttachedCell() {
        Block weight = new ItemBlockFactory(new Random(42)).create(true, null, ItemType.WEIGHT, 3, 5);
        assertNull(weight.getType());
        assertEquals(ItemType.WEIGHT, weight.getItemType());
        assertEquals(BlockTest.cells(new int[][] {{1,0},{2,0},{0,1},{1,1},{2,1},{3,1}}), BlockTest.cells(weight.getCells()));
        assertEquals(4, weight.width());
        assertEquals(2, weight.height());
        assertEquals(0, weight.getItemCells().length);
        assertEquals(1, weight.getRotationOffsets().length);
        int[][] before = weight.getBoardCells();
        weight.rotate();
        assertTrue(Arrays.deepEquals(before, weight.getBoardCells()));
        assertEquals(0, weight.getRotationState());
    }

    @Test
    void normalModeRejectsBothFactoryOverloads() {
        var factory = new ItemBlockFactory(new Random(42));
        assertThrows(IllegalStateException.class, () -> factory.create(false, TetrominoType.T, 0, 0));
        for (ItemType item : ItemType.values()) {
            assertThrows(IllegalStateException.class, () -> factory.create(false, TetrominoType.T, item, 0, 0));
        }
    }

    @Test
    void seededItemAndAttachmentDistributionsAreApproximatelyUniform() {
        // 고정 seed로 재현 가능. 10만 회 선택 후 기대 횟수 대비 ±5% 상대 오차 검사.
        var factory = new ItemBlockFactory(new Random(20261023));
        int[] items = new int[5];
        int[] attached = new int[4];
        int samples = 100_000;
        for (int i = 0; i < samples; i++) {
            Block block = factory.create(true, TetrominoType.T, 0, 0);
            items[block.getItemType().ordinal()]++;
            Block line = factory.create(true, TetrominoType.I, ItemType.LINE_CLEAR, 0, 0);
            attached[line.getItemCells()[0][0]]++;
        }
        for (int count : items) assertEquals(samples / 5.0, count, samples / 5.0 * .05);
        for (int count : attached) assertEquals(samples / 4.0, count, samples / 4.0 * .05);
    }

    @Test
    void invalidArgumentsAreRejectedAndMultiplierRemainsTwo() {
        var factory = new ItemBlockFactory(new Random(42));
        assertThrows(NullPointerException.class, () -> new ItemBlockFactory(null));
        assertThrows(NullPointerException.class, () -> factory.create(true, null, 0, 0));
        assertThrows(NullPointerException.class, () -> factory.create(true, TetrominoType.T, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Block.withItem(TetrominoType.T, 0, 0, ItemType.WEIGHT, 0));
        for (int index : new int[] {-1, 4})
            assertThrows(IllegalArgumentException.class, () -> Block.withItem(TetrominoType.T, 0, 0, ItemType.FILL, index));
        assertEquals(2, ItemType.MULTIPLIER.getScoreMultiplier());
        assertEquals('L', ItemType.LINE_CLEAR.getSymbol());
        assertEquals('F', ItemType.FILL.getSymbol());
        assertEquals('B', ItemType.BOMB.getSymbol());
        assertEquals('M', ItemType.MULTIPLIER.getSymbol());
    }
}
