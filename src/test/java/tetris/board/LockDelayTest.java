package tetris.board;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 시간은 밀리초 숫자로 직접 넘긴다. 기본 여유 500ms, 리셋 최대 15회. */
class LockDelayTest {

    @Test
    void doesNotLockBeforeTouchingGround() {
        LockDelay delay = new LockDelay();
        assertFalse(delay.isWaiting());
        assertFalse(delay.isExpired(10_000));
    }

    @Test
    void locksOnlyAfterFullDelay() {
        LockDelay delay = new LockDelay();
        delay.onGrounded(1_000);
        assertTrue(delay.isWaiting());
        assertFalse(delay.isExpired(1_499));
        assertTrue(delay.isExpired(1_500));
    }

    @Test
    void groundedAgainWhileWaitingDoesNotRestartTimer() {
        // 자동 하강 신호가 바닥에서 여러 번 와도 여유가 늘어나지 않는다.
        LockDelay delay = new LockDelay();
        delay.onGrounded(1_000);
        delay.onGrounded(1_400);
        assertTrue(delay.isExpired(1_500));
    }

    @Test
    void moveOrRotateRestartsTimer() {
        LockDelay delay = new LockDelay();
        delay.onGrounded(1_000);
        delay.onMoveOrRotate(1_400);
        assertFalse(delay.isExpired(1_500));
        assertTrue(delay.isExpired(1_900));
        assertEquals(1, delay.getResetCount());
    }

    @Test
    void moveWhileFallingDoesNotUseReset() {
        LockDelay delay = new LockDelay();
        delay.onMoveOrRotate(1_000);
        assertEquals(0, delay.getResetCount());
        assertFalse(delay.isWaiting());
    }

    @Test
    void resetsStopAfterMaximum() {
        LockDelay delay = new LockDelay(500, 15);
        delay.onGrounded(0);
        for (int i = 1; i <= 15; i++) {
            delay.onMoveOrRotate(i * 100L);
        }
        assertEquals(15, delay.getResetCount());
        // 16번째 이동은 시간을 다시 세지 않는다. 마지막 리셋(1,500ms)에서 500ms 뒤 고정.
        delay.onMoveOrRotate(1_900);
        assertEquals(15, delay.getResetCount());
        assertTrue(delay.isExpired(2_000));
    }

    @Test
    void walkingOffLedgeCancelsWaiting() {
        LockDelay delay = new LockDelay();
        delay.onGrounded(1_000);
        delay.onAirborne();
        assertFalse(delay.isWaiting());
        assertFalse(delay.isExpired(5_000));
        delay.onGrounded(5_000);
        assertFalse(delay.isExpired(5_499));
        assertTrue(delay.isExpired(5_500));
    }

    @Test
    void clearForNewPieceResetsEverything() {
        LockDelay delay = new LockDelay();
        delay.onGrounded(0);
        delay.onMoveOrRotate(100);
        delay.clear();
        assertFalse(delay.isWaiting());
        assertEquals(0, delay.getResetCount());
    }

    @Test
    void zeroDelayLocksImmediatelyAfterTouching() {
        LockDelay delay = new LockDelay(0, 0);
        delay.onGrounded(700);
        assertTrue(delay.isExpired(700));
    }

    @Test
    void rejectsNegativeSettings() {
        assertThrows(IllegalArgumentException.class, () -> new LockDelay(-1, 15));
        assertThrows(IllegalArgumentException.class, () -> new LockDelay(500, -1));
    }
}
