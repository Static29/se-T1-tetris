package tetris.board;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;
import tetris.block.TetrominoType;

/**
 * 가중치에 비례한 확률로 다음 블록 종류를 고른다(Roulette Wheel Selection).
 * 가방(7-bag) 없이 매번 독립적으로 뽑으므로 같은 블록이 연속으로 나올 수 있다.
 * 가중치가 0인 종류는 나오지 않는다.
 */
public final class PieceSelector {
    /** I형을 제외한 블록의 가중치. Req2의 예시 기준값이다. */
    public static final int BASE_WEIGHT = 10;
    /** Req2: easy는 I형이 20% 더 자주, hard는 20% 덜 등장한다. */
    public static final int EASY_I_WEIGHT = 12;
    public static final int NORMAL_I_WEIGHT = 10;
    public static final int HARD_I_WEIGHT = 8;

    private final TetrominoType[] types = TetrominoType.values();
    private final int[] weights = new int[types.length];
    /** cumulativeWeights[i] = 0번부터 i번 종류까지의 가중치 합. 룰렛에서 각 종류가 차지하는 구간의 끝이다. */
    private final int[] cumulativeWeights = new int[types.length];
    private final int totalWeight;
    private final RandomGenerator random;

    /** weights에 없는 종류는 가중치 0으로 본다. 가중치는 0 이상이고 합은 1 이상이어야 한다. */
    public PieceSelector(Map<TetrominoType, Integer> weights, RandomGenerator random) {
        Objects.requireNonNull(weights, "weights");
        this.random = Objects.requireNonNull(random, "random");
        int sum = 0;
        for (int i = 0; i < types.length; i++) {
            Integer weight = weights.getOrDefault(types[i], 0);
            Objects.requireNonNull(weight, "weights의 값");
            if (weight < 0) {
                throw new IllegalArgumentException("가중치는 0 이상이어야 합니다: " + types[i] + "=" + weight);
            }
            this.weights[i] = weight;
            sum = Math.addExact(sum, weight);
            cumulativeWeights[i] = sum;
        }
        if (sum == 0) {
            throw new IllegalArgumentException("가중치의 합이 0이면 블록을 고를 수 없습니다.");
        }
        this.totalWeight = sum;
    }

    /** I형만 iWeight, 나머지 6종은 BASE_WEIGHT인 선택기. 난이도별 값은 EASY/NORMAL/HARD_I_WEIGHT를 쓴다. */
    public static PieceSelector withIWeight(int iWeight, RandomGenerator random) {
        Map<TetrominoType, Integer> weights = new EnumMap<>(TetrominoType.class);
        for (TetrominoType type : TetrominoType.values()) {
            weights.put(type, type == TetrominoType.I ? iWeight : BASE_WEIGHT);
        }
        return new PieceSelector(weights, random);
    }

    /** 0 이상 totalWeight 미만의 번호를 뽑고, 그 번호가 들어 있는 구간의 종류를 반환한다. */
    public TetrominoType next() {
        int ticket = random.nextInt(totalWeight);
        for (int i = 0; i < types.length; i++) {
            if (ticket < cumulativeWeights[i]) {
                return types[i];
            }
        }
        throw new IllegalStateException("룰렛 구간 계산 오류: ticket=" + ticket);
    }

    /** type이 뽑힐 이론상 확률(가중치 / 가중치 합). 테스트와 화면 표시용이다. */
    public double getProbability(TetrominoType type) {
        Objects.requireNonNull(type, "type");
        return (double) weights[type.ordinal()] / totalWeight;
    }
}
