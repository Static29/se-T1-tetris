package tetris.block;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BlockTest {
    static Set<String> cells(int[][] coordinates) {
        return Arrays.stream(coordinates).map(Arrays::toString).collect(Collectors.toSet());
    }

    @ParameterizedTest
    @EnumSource(TetrominoType.class)
    void movePreviewDoesNotMutateAndMatchesMove(TetrominoType type) {
        Block block = new Block(type, 3, 5);
        int[][] before = block.getBoardCells();
        int[][] preview = block.previewMove(-2, 4);
        assertEquals(cells(before), cells(block.getBoardCells()));
        assertEquals(3, block.getX());
        assertEquals(5, block.getY());
        for (int i = 0; i < before.length; i++) {
            assertArrayEquals(new int[] {before[i][0] - 2, before[i][1] + 4}, preview[i]);
        }
        block.move(-2, 4);
        assertEquals(cells(preview), cells(block.getBoardCells()));
        assertEquals(1, block.getX());
        assertEquals(9, block.getY());
    }

    @ParameterizedTest
    @EnumSource(TetrominoType.class)
    void fourTurnsRestoreShapeAndKeepOrigin(TetrominoType type) {
        Block block = new Block(type, 3, 5);
        int[][] original = block.getCells();
        int width = type == TetrominoType.I ? 4 : type == TetrominoType.O ? 2 : 3;
        int height = type == TetrominoType.I ? 1 : 2;
        for (int turn = 0; turn < 4; turn++) {
            assertEquals(turn % 2 == 0 ? width : height, block.width());
            assertEquals(turn % 2 == 0 ? height : width, block.height());
            block.rotate();
            assertEquals(type == TetrominoType.O ? 0 : (turn + 1) % 4, block.getRotationState());
            assertEquals(3, block.getX());
            assertEquals(5, block.getY());
        }
        assertTrue(Arrays.deepEquals(original, block.getCells()));
    }

    @ParameterizedTest
    @EnumSource(TetrominoType.class)
    void returnedArraysAndOtherInstancesCannotChangeState(TetrominoType type) {
        Block first = new Block(type);
        Block second = new Block(type);
        int[][] original = first.getCells();
        first.getCells()[0][0] = 999;
        first.getBoardCells()[0][1] = 999;
        first.previewMove(0, 0)[0][0] = 999;
        first.previewRotation()[0][0] = 999;
        first.getRotationOffsets()[0][0] = 999;
        type.getCells()[0][0] = 999;
        assertTrue(Arrays.deepEquals(original, first.getCells()));
        assertArrayEquals(new int[] {0, 0}, first.getRotationOffsets()[0]);
        first.rotate();
        first.move(4, 7);
        assertTrue(Arrays.deepEquals(original, second.getCells()));
        assertTrue(Arrays.deepEquals(original, new Block(type).getCells()));
    }

    @Test
    void normalBlockHasNoItemAndRejectsNullType() {
        Block block = new Block(TetrominoType.T);
        assertEquals(0, block.getX());
        assertEquals(0, block.getY());
        assertFalse(block.hasItem());
        assertNull(block.getItemType());
        assertEquals(0, block.getItemCells().length);
        assertEquals(0, block.getBoardItemCells().length);
        assertEquals('O', block.getCellSymbol(1, 0));
        assertEquals('.', block.getCellSymbol(0, 0));
        assertThrows(NullPointerException.class, () -> new Block(null));
    }

    @ParameterizedTest
    @EnumSource(TetrominoType.class)
    void colorModeSelectsValidHexWithoutChangingShape(TetrominoType type) {
        int[][] before = type.getCells();
        assertTrue(type.getNormalColorHex().matches("#[0-9A-Fa-f]{6}"));
        assertTrue(type.getColorBlindColorHex().matches("#[0-9A-Fa-f]{6}"));
        assertEquals(type.getNormalColorHex(), type.getColorHex(false));
        assertEquals(type.getColorBlindColorHex(), type.getColorHex(true));
        assertTrue(Arrays.deepEquals(before, type.getCells()));
    }
}
