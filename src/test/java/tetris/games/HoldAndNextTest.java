package tetris.games;

import java.util.ArrayDeque;
import java.util.List;
import org.junit.jupiter.api.Test;
import tetris.block.TetrominoType;
import static org.junit.jupiter.api.Assertions.*;
import static tetris.block.TetrominoType.*;

class HoldAndNextTest {
    private Game gameWith(TetrominoType... types) {
        var queue = new ArrayDeque<>(List.of(types));
        return new Game(queue::removeFirst);
    }

    @Test
    void startPreparesCurrentAndNextWithEmptyHold() {
        Game game = gameWith(I, T);
        assertFalse(game.canHold());
        assertFalse(game.hold());
        assertFalse(game.advanceAfterLock());
        game.start();
        assertEquals(I, game.getCurrentType());
        assertEquals(T, game.getNextType());
        assertNull(game.getHeldType());
        assertTrue(game.canHold());
    }

    @Test
    void firstHoldStoresCurrentAndConsumesNextExactlyOnce() {
        Game game = gameWith(I, T, O);
        game.start();
        assertTrue(game.hold());
        assertEquals(I, game.getHeldType());
        assertEquals(T, game.getCurrentType());
        assertEquals(O, game.getNextType());
        assertFalse(game.hold());
        assertEquals(I, game.getHeldType());
        assertEquals(T, game.getCurrentType());
        assertEquals(O, game.getNextType());
    }

    @Test
    void lockAllowsOneSwapWithoutConsumingNext() {
        Game game = gameWith(I, T, O, S);
        game.start();
        game.hold();
        assertTrue(game.advanceAfterLock());
        assertEquals(O, game.getCurrentType());
        assertEquals(S, game.getNextType());
        assertTrue(game.canHold());
        // 생성기는 소진된 상태다. 교환할 때 새 블록을 뽑으면 이 테스트가 실패한다.
        assertTrue(game.hold());
        assertEquals(I, game.getCurrentType());
        assertEquals(O, game.getHeldType());
        assertEquals(S, game.getNextType());
        assertFalse(game.hold());
        assertEquals(I, game.getCurrentType());
    }

    @Test
    void pauseAndResumeDoNotResetHoldLimit() {
        Game game = gameWith(I, T, O);
        game.start();
        game.pause();
        assertFalse(game.hold());
        assertFalse(game.advanceAfterLock());
        assertEquals(I, game.getCurrentType());
        assertEquals(T, game.getNextType());
        game.resume();
        assertTrue(game.hold());
        game.pause();
        game.resume();
        game.start();
        assertFalse(game.hold());
        assertEquals(T, game.getCurrentType());
    }

    @Test
    void nextAdvancesAfterEveryLockEvenWithoutHolding() {
        Game game = gameWith(J, L, Z, S);
        game.start();
        game.advanceAfterLock();
        assertEquals(L, game.getCurrentType());
        assertEquals(Z, game.getNextType());
        game.advanceAfterLock();
        assertEquals(Z, game.getCurrentType());
        assertEquals(S, game.getNextType());
        assertNull(game.getHeldType());
    }

    @Test
    void separateGameStartsWithFreshHoldState() {
        Game oldGame = gameWith(I, T, O);
        oldGame.start();
        oldGame.hold();
        Game newGame = gameWith(Z, S);
        newGame.start();
        assertNull(newGame.getHeldType());
        assertTrue(newGame.canHold());
    }
}
