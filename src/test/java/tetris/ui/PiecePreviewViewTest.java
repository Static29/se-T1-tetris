package tetris.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.block.TetrominoType;
import static org.junit.jupiter.api.Assertions.*;

class PiecePreviewViewTest {
    @ParameterizedTest
    @EnumSource(TetrominoType.class)
    void everyTypeFitsInBoxWithExactlyFourCells(TetrominoType type) {
        String preview = PiecePreviewView.format(type);
        assertEquals(4, preview.chars().filter(c -> c == type.name().charAt(0)).count());
        String[] lines = preview.split("\n");
        assertEquals(6, lines.length);
        for (String line : lines) {
            assertEquals(11, line.length());
        }
    }

    @Test
    void emptyHoldRendersEmptyBox() {
        String preview = PiecePreviewView.format(null);
        assertEquals(16, preview.chars().filter(c -> c == '.').count());
    }

    @Test
    void tShapeUsesXYCoordinatesWithoutTransposing() {
        String[] lines = PiecePreviewView.format(TetrominoType.T).split("\n");
        assertEquals("| . T . . |", lines[1]);
        assertEquals("| T T T . |", lines[2]);
    }
}
