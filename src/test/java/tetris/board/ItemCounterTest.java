package tetris.board;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 아이템 등장 주기만 따로 검사한다. Next 교체까지 합친 동작은 BlockSpawnerTest가 검사한다. */
class ItemCounterTest {
    @Test
    void startsWithFullCycleAndNoPendingItem() {
        ItemCounter counter = new ItemCounter();
        assertEquals(BlockSpawner.LINES_PER_ITEM, counter.getLinesUntilItem());
        assertEquals(0, counter.getPendingItemCount());
        assertFalse(counter.takePendingItem());
    }

    @Test
    void tenLinesQueueOneItemAndExtraLinesCarryOver() {
        ItemCounter counter = new ItemCounter();
        counter.addClearedLines(9);
        assertEquals(1, counter.getLinesUntilItem());
        counter.addClearedLines(2);
        assertEquals(1, counter.getPendingItemCount());
        assertEquals(9, counter.getLinesUntilItem());
    }

    @Test
    void manyLinesAtOnceQueueSeveralItemsTakenOneByOne() {
        ItemCounter counter = new ItemCounter();
        counter.addClearedLines(23);
        assertEquals(2, counter.getPendingItemCount());
        assertEquals(7, counter.getLinesUntilItem());
        assertTrue(counter.takePendingItem());
        assertTrue(counter.takePendingItem());
        assertFalse(counter.takePendingItem());
        assertEquals(0, counter.getPendingItemCount());
    }
}
