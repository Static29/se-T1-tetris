package tetris.board;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import tetris.block.TetrominoType;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Req2: 1,000번 이상 뽑아 설정한 확률과 오차 ±5% 이내로 나오는지 확인한다.
 * 결과가 실행마다 달라지지 않도록 시드를 고정하고, 오차가 우연히 커지지 않도록 100,000번 뽑는다.
 * 오차는 기대 횟수 대비 비율(상대 오차)로 계산한다. 400번 기대면 380~420번이다.
 */
class PieceSelectorTest {
    private static final int DRAW_COUNT = 100_000;
    private static final double TOLERANCE = 0.05;
    private static final long SEED = 20261024L;

    private static Map<TetrominoType, Integer> countDraws(PieceSelector selector, int drawCount) {
        Map<TetrominoType, Integer> counts = new EnumMap<>(TetrominoType.class);
        for (int i = 0; i < drawCount; i++) {
            counts.merge(selector.next(), 1, Integer::sum);
        }
        return counts;
    }

    /** 모든 종류가 기대 횟수(확률 × 뽑은 횟수)와 상대 오차 TOLERANCE 이내인지 확인한다. */
    private static void assertDistribution(PieceSelector selector, Map<TetrominoType, Integer> counts, int drawCount) {
        for (TetrominoType type : TetrominoType.values()) {
            double expected = selector.getProbability(type) * drawCount;
            int actual = counts.getOrDefault(type, 0);
            assertEquals(expected, actual, expected * TOLERANCE, type + " 기대 " + expected + "번, 실제 " + actual + "번");
        }
    }

    @Test
    void normalDifficultyChoosesEveryPieceEquallyWithinFivePercent() {
        PieceSelector selector = PieceSelector.withIWeight(PieceSelector.NORMAL_I_WEIGHT, new Random(SEED));
        assertEquals(1.0 / 7, selector.getProbability(TetrominoType.I), 1e-12);
        assertDistribution(selector, countDraws(selector, DRAW_COUNT), DRAW_COUNT);
    }

    @Test
    void easyDifficultyChoosesIPieceTwentyPercentMoreOften() {
        PieceSelector selector = PieceSelector.withIWeight(PieceSelector.EASY_I_WEIGHT, new Random(SEED));
        // I 12, 나머지 6종 10씩: 합 72
        assertEquals(12.0 / 72, selector.getProbability(TetrominoType.I), 1e-12);
        assertEquals(10.0 / 72, selector.getProbability(TetrominoType.T), 1e-12);
        assertDistribution(selector, countDraws(selector, DRAW_COUNT), DRAW_COUNT);
    }

    @Test
    void hardDifficultyChoosesIPieceTwentyPercentLessOften() {
        PieceSelector selector = PieceSelector.withIWeight(PieceSelector.HARD_I_WEIGHT, new Random(SEED));
        assertEquals(8.0 / 68, selector.getProbability(TetrominoType.I), 1e-12);
        assertDistribution(selector, countDraws(selector, DRAW_COUNT), DRAW_COUNT);
    }

    @Test
    void requirementExampleDoubleWeightGivesFortyAndTwentyPercent() {
        // Req2 예시: 블록 4개 중 B의 확률이 다른 것의 2배면 B 40%, 나머지 20%. 1,000번이면 약 400번 / 200번.
        Map<TetrominoType, Integer> weights = new EnumMap<>(TetrominoType.class);
        weights.put(TetrominoType.I, 2);
        weights.put(TetrominoType.O, 1);
        weights.put(TetrominoType.T, 1);
        weights.put(TetrominoType.S, 1);
        PieceSelector selector = new PieceSelector(weights, new Random(SEED));
        assertEquals(0.4, selector.getProbability(TetrominoType.I), 1e-12);
        assertEquals(0.2, selector.getProbability(TetrominoType.O), 1e-12);
        Map<TetrominoType, Integer> counts = countDraws(selector, DRAW_COUNT);
        assertDistribution(selector, counts, DRAW_COUNT);
        assertFalse(counts.containsKey(TetrominoType.Z), "가중치가 없는 종류는 나오지 않는다");
    }

    @Test
    void sameSeedGivesSameSequence() {
        PieceSelector first = PieceSelector.withIWeight(PieceSelector.EASY_I_WEIGHT, new Random(SEED));
        PieceSelector second = PieceSelector.withIWeight(PieceSelector.EASY_I_WEIGHT, new Random(SEED));
        for (int i = 0; i < 1_000; i++) {
            assertEquals(first.next(), second.next());
        }
    }

    @Test
    void rejectsInvalidWeightsAndNulls() {
        Random random = new Random(SEED);
        Map<TetrominoType, Integer> negative = new EnumMap<>(TetrominoType.class);
        negative.put(TetrominoType.I, -1);
        negative.put(TetrominoType.O, 5);
        assertThrows(IllegalArgumentException.class, () -> new PieceSelector(negative, random));
        assertThrows(IllegalArgumentException.class, () -> new PieceSelector(Map.of(), random));
        assertThrows(IllegalArgumentException.class, () -> PieceSelector.withIWeight(-1, random));
        assertThrows(NullPointerException.class, () -> new PieceSelector(null, random));
        assertThrows(NullPointerException.class, () -> PieceSelector.withIWeight(10, null));
        assertThrows(NullPointerException.class, () -> PieceSelector.withIWeight(10, random).getProbability(null));
    }
}
