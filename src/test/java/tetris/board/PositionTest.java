package tetris.board;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PositionTest {
    @Test
    void fromXYPutsYIntoRowAndXIntoCol() {
        Position position = Position.fromXY(7, 2);
        assertEquals(2, position.row());
        assertEquals(7, position.col());
        assertEquals(new Position(2, 7), position);
    }

    @Test
    void fromXYConvertsBlockCellArrayInOrder() {
        // T 블록 모양 {x, y}: .#. / ###
        int[][] xyCells = {{1, 0}, {0, 1}, {1, 1}, {2, 1}};
        assertEquals(List.of(new Position(0, 1), new Position(1, 0), new Position(1, 1), new Position(1, 2)),
                Position.fromXY(xyCells));
    }

    @Test
    void fromXYDoesNotKeepReferenceToInputArray() {
        int[][] xyCells = {{3, 4}};
        List<Position> positions = Position.fromXY(xyCells);
        xyCells[0][0] = 9;
        assertEquals(new Position(4, 3), positions.get(0));
        assertThrows(UnsupportedOperationException.class, () -> positions.add(new Position(0, 0)));
    }

    @Test
    void fromXYRejectsNullOrMalformedCells() {
        assertThrows(NullPointerException.class, () -> Position.fromXY(null));
        assertThrows(NullPointerException.class, () -> Position.fromXY(new int[][] {{0, 0}, null}));
        assertThrows(IllegalArgumentException.class, () -> Position.fromXY(new int[][] {{0}}));
        assertThrows(IllegalArgumentException.class, () -> Position.fromXY(new int[][] {{0, 0, 0}}));
        assertTrue(Position.fromXY(new int[0][]).isEmpty());
    }

    @Test
    void offsetReturnsNewPositionWithoutChangingOriginal() {
        Position origin = new Position(5, 5);
        assertEquals(new Position(6, 4), origin.offset(1, -1));
        assertEquals(new Position(5, 5), origin);
    }
}
