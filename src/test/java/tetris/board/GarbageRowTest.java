package tetris.board;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GarbageRowTest {
    @Test
    void withHolesLeavesOnlyGivenColumnsEmpty() {
        GarbageRow row = GarbageRow.withHoles(5, List.of(1, 3));
        assertEquals(5, row.getColumns());
        assertTrue(row.isFilled(0));
        assertFalse(row.isFilled(1));
        assertEquals("G.G.G", row.toString());
    }

    @Test
    void isAValueThatCopiesItsInput() {
        boolean[] filled = {true, false};
        GarbageRow row = new GarbageRow(filled);
        filled[1] = true;
        assertFalse(row.isFilled(1), "만든 뒤 원본 배열을 바꿔도 영향이 없다");
        assertEquals(GarbageRow.withHoles(2, List.of(1)), row);
        assertEquals(GarbageRow.withHoles(2, List.of(1)).hashCode(), row.hashCode());
        assertNotEquals(GarbageRow.withHoles(2, List.of(0)), row);
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(NullPointerException.class, () -> new GarbageRow(null));
        assertThrows(IllegalArgumentException.class, () -> new GarbageRow(new boolean[0]));
        assertThrows(IllegalArgumentException.class, () -> GarbageRow.withHoles(0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> GarbageRow.withHoles(3, List.of(3)));
        assertThrows(IndexOutOfBoundsException.class, () -> GarbageRow.withHoles(3, List.of()).isFilled(-1));
    }
}
