package tetris.board;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 속도(칸/초) = 1 + 레벨 × 증가 비율 / 100, 간격 = 1000ms / 속도. 10줄마다 레벨 1. */
class FallSpeedTest {
    private final FallSpeed easy = new FallSpeed(FallSpeed.EASY_INCREASE_PERCENT);
    private final FallSpeed normal = new FallSpeed(FallSpeed.NORMAL_INCREASE_PERCENT);
    private final FallSpeed hard = new FallSpeed(FallSpeed.HARD_INCREASE_PERCENT);

    @Test
    void startsAtOneCellPerSecond() {
        assertEquals(1_000, easy.getIntervalMillis(0));
        assertEquals(1_000, normal.getIntervalMillis(0));
        assertEquals(1_000, hard.getIntervalMillis(0));
    }

    @Test
    void levelRisesEveryTenLines() {
        assertEquals(0, normal.getLevel(9));
        assertEquals(1, normal.getLevel(10));
        assertEquals(2, normal.getLevel(25));
        assertEquals(1_000, normal.getIntervalMillis(9));
        assertEquals(500, normal.getIntervalMillis(10));
        assertEquals(333, normal.getIntervalMillis(20));
    }

    @Test
    void easyIncreasesTwentyPercentLessAndHardTwentyPercentMore() {
        // 레벨 1에서 속도: easy 1.8, normal 2.0, hard 2.2칸/초
        assertEquals(555, easy.getIntervalMillis(10));
        assertEquals(500, normal.getIntervalMillis(10));
        assertEquals(454, hard.getIntervalMillis(10));
        for (int lines = 10; lines <= 200; lines += 10) {
            assertTrue(easy.getIntervalMillis(lines) > normal.getIntervalMillis(lines), "easy가 더 느려야 함: " + lines);
            assertTrue(hard.getIntervalMillis(lines) < normal.getIntervalMillis(lines)
                    || hard.getIntervalMillis(lines) == FallSpeed.MIN_INTERVAL_MILLIS, "hard가 더 빨라야 함: " + lines);
        }
    }

    @Test
    void intervalNeverGoesBelowMinimum() {
        assertEquals(FallSpeed.MIN_INTERVAL_MILLIS, hard.getIntervalMillis(10_000));
        assertEquals(FallSpeed.MIN_INTERVAL_MILLIS, normal.getIntervalMillis(Integer.MAX_VALUE));
    }

    @Test
    void weightFallsOneAndHalfTimesFaster() {
        assertEquals(666, normal.getWeightIntervalMillis(0));
        assertEquals(333, normal.getWeightIntervalMillis(10));
        assertEquals(FallSpeed.MIN_INTERVAL_MILLIS, hard.getWeightIntervalMillis(10_000));
    }

    @Test
    void zeroPercentNeverSpeedsUp() {
        assertEquals(1_000, new FallSpeed(0).getIntervalMillis(500));
    }

    @Test
    void rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new FallSpeed(-1));
        assertThrows(IllegalArgumentException.class, () -> normal.getLevel(-1));
        assertThrows(IllegalArgumentException.class, () -> normal.getIntervalMillis(-1));
    }
}
