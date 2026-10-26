package tetris.board;

/**
 * 지운 줄 수에 따른 자동 하강 속도. 처음에는 1초에 1칸(Req1, 배율 x1.0)이고 10줄마다 레벨이 오른다.
 * 속도 배율 = 1 + 레벨 × increasePercent / 100. normal은 레벨마다 0.1씩 고정으로 늘어난다(x1.0, x1.1, x1.2, ...).
 * Req2: easy는 증가량이 20% 적고(0.08), hard는 20% 많다(0.12). 하강 간격 = 1000ms / 속도 배율.
 */
public final class FallSpeed {
    public static final long BASE_INTERVAL_MILLIS = 1_000;
    public static final int LINES_PER_LEVEL = 10;
    /** 너무 빨라져 조작할 수 없게 되지 않도록 둔 하한(배율 x20). */
    public static final long MIN_INTERVAL_MILLIS = 50;
    /** 레벨당 속도 배율 증가량(%). 10이면 레벨마다 +0.1. */
    public static final int EASY_INCREASE_PERCENT = 8;
    public static final int NORMAL_INCREASE_PERCENT = 10;
    public static final int HARD_INCREASE_PERCENT = 12;
    /** 무게추는 무거우므로 같은 레벨의 일반 블록보다 1.5배 빠르게 떨어진다(팀 결정). */
    public static final int WEIGHT_SPEED_PERCENT = 150;

    private final int increasePercent;

    /** increasePercent: 레벨당 속도 배율 증가량을 %로 나타낸 값(normal 10 = +0.1). 0이면 빨라지지 않는다. */
    public FallSpeed(int increasePercent) {
        if (increasePercent < 0) {
            throw new IllegalArgumentException("속도 증가 비율은 0 이상이어야 합니다: " + increasePercent);
        }
        this.increasePercent = increasePercent;
    }

    public int getIncreasePercent() { return increasePercent; }

    /** 0줄부터 레벨 0이고, LINES_PER_LEVEL줄마다 1씩 오른다. */
    public int getLevel(int clearedLines) {
        if (clearedLines < 0) {
            throw new IllegalArgumentException("지운 줄 수는 음수일 수 없습니다: " + clearedLines);
        }
        return clearedLines / LINES_PER_LEVEL;
    }

    /** 속도 배율을 100배 한 정수. 레벨 0은 100(x1.0), normal 레벨 1은 110(x1.1). 소수 오차 없이 계산하려고 쓴다. */
    public long getSpeedPercent(int clearedLines) {
        long level = getLevel(clearedLines);
        return 100 + level * increasePercent;
    }

    /** 화면에 보여 줄 속도 배율(x1.0, x1.1, ...). 하한 간격에 막혀도 배율 값 자체는 계속 오른다. */
    public double getSpeedMultiplier(int clearedLines) {
        return getSpeedPercent(clearedLines) / 100.0;
    }

    /** 자동 하강 한 칸 사이의 간격(ms) = 1000 / 속도 배율. 소수점 아래는 버린다(x1.1 → 909ms). */
    public long getIntervalMillis(int clearedLines) {
        long interval = BASE_INTERVAL_MILLIS * 100 / getSpeedPercent(clearedLines);
        return Math.max(MIN_INTERVAL_MILLIS, interval);
    }

    /** 무게추의 하강 간격. 일반 간격을 WEIGHT_SPEED_PERCENT / 100배 빠르게 한다(하한은 같음). */
    public long getWeightIntervalMillis(int clearedLines) {
        return Math.max(MIN_INTERVAL_MILLIS, getIntervalMillis(clearedLines) * 100 / WEIGHT_SPEED_PERCENT);
    }
}
