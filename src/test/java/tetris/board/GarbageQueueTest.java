package tetris.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static tetris.board.BoardFixtures.*;

/** 공격 줄 대기열(Req3 6쪽): 최대 10줄, 가득 차면 무시, 넘치면 새 공격의 아래쪽을 잘라낸다. */
class GarbageQueueTest {
    /** hole 칸만 빈 10칸짜리 같은 줄 count개. */
    private static List<GarbageRow> rows(int count, int hole) {
        return Collections.nCopies(count, GarbageRow.withHoles(10, List.of(hole)));
    }

    /** i번째 줄은 i번 칸이 빈, 서로 구분되는 줄 count개. */
    private static List<GarbageRow> numbered(int count) {
        List<GarbageRow> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(GarbageRow.withHoles(10, List.of(i)));
        }
        return result;
    }

    @Test
    void newAttackIsAddedBelowPendingRows() {
        GarbageQueue queue = new GarbageQueue();
        assertEquals(2, queue.receive(rows(2, 0)));
        assertEquals(3, queue.receive(rows(3, 9)));
        assertEquals(5, queue.size());
        List<GarbageRow> pending = queue.getPendingRows();
        assertEquals(rows(2, 0), pending.subList(0, 2));
        assertEquals(rows(3, 9), pending.subList(2, 5));
        assertFalse(queue.isFull());
    }

    @Test
    void overflowCutsTheBottomOfTheNewAttack() {
        GarbageQueue queue = new GarbageQueue();
        queue.receive(rows(8, 9));
        List<GarbageRow> attack = numbered(3);
        assertEquals(2, queue.receive(attack));
        assertEquals(attack.subList(0, 2), queue.getPendingRows().subList(8, 10), "위쪽 2줄만 남는다");
        assertTrue(queue.isFull());
    }

    @Test
    void fullQueueIgnoresNewAttacks() {
        GarbageQueue queue = new GarbageQueue();
        queue.receive(rows(GarbageQueue.MAX_ROWS, 0));
        assertTrue(queue.isFull());
        assertEquals(0, queue.receive(rows(2, 5)));
        assertEquals(rows(GarbageQueue.MAX_ROWS, 0), queue.getPendingRows());
    }

    @Test
    void insertIntoPushesAllRowsUpFromTheBottomAndEmptiesTheQueue() {
        Board board = new Board(6, 0, 10);
        board.lock(positions(5, 2), Cell.T);
        GarbageQueue queue = new GarbageQueue();
        queue.receive(numbered(2));
        assertFalse(queue.insertInto(board));
        assertEquals(0, queue.size());
        assertEquals(Cell.T, board.getCell(3, 2), "기존 칸은 2줄 위로");
        assertTrue(board.isEmpty(4, 0), "위 공격 줄의 빈칸");
        assertEquals(Cell.GARBAGE, board.getCell(4, 1));
        assertTrue(board.isEmpty(5, 1), "아래 공격 줄의 빈칸");
        assertEquals(Cell.GARBAGE, board.getCell(5, 0));
    }

    @Test
    void insertIntoReportsOverflowWhenCellsArePushedOffTheTop() {
        Board board = new Board(4, 2, 10);
        board.lock(positions(0, 0), Cell.T);
        GarbageQueue queue = new GarbageQueue();
        queue.receive(rows(1, 0));
        assertTrue(queue.insertInto(board));
        assertEquals(0, queue.size());
    }

    @Test
    void clearAndNullChecks() {
        GarbageQueue queue = new GarbageQueue();
        queue.receive(rows(3, 0));
        queue.clear();
        assertEquals(0, queue.size());
        assertThrows(NullPointerException.class, () -> queue.receive(null));
        assertThrows(NullPointerException.class, () -> queue.receive(Arrays.asList((GarbageRow) null)));
        assertThrows(NullPointerException.class, () -> queue.insertInto(null));
    }
}
