package tetris.board;

/**
 * 지운 줄 수에 따른 자동 하강 간격. 처음에는 1초에 1칸(Req1)이고 10줄마다 레벨이 오른다.
 * 레벨이 하나 오를 때마다 속도가 1칸/초 × increasePercent/100 만큼 빨라진다.
 * Req2: easy는 증가량이 20% 적고(80), hard는 20% 많다(120).
 */
public final class FallSpeed {
    public static final long BASE_INTERVAL_MILLIS = 1_000;
    public static final int LINES_PER_LEVEL = 10;
    /** 너무 빨라져 조작할 수 없게 되지 않도록 둔 하한(초당 20칸). */
    public static final long MIN_INTERVAL_MILLIS = 50;
    public static final int EASY_INCREASE_PERCENT = 80;
    public static final int NORMAL_INCREASE_PERCENT = 100;
    public static final int HARD_INCREASE_PERCENT = 120;

    private final int increasePercent;

    /** increasePercent: 레벨당 속도 증가량을 normal(100) 대비 백분율로 나타낸 값. 0이면 빨라지지 않는다. */
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

    /** 자동 하강 한 칸 사이의 간격(ms). 속도(칸/초) = 1 + 레벨 × increasePercent / 100, 간격 = 1000 / 속도. */
    public long getIntervalMillis(int clearedLines) {
        long level = getLevel(clearedLines);
        // 속도를 100배 한 정수로 계산해 소수 오차 없이 나눈다.
        long speedPercent = 100 + level * increasePercent;
        long interval = BASE_INTERVAL_MILLIS * 100 / speedPercent;
        return Math.max(MIN_INTERVAL_MILLIS, interval);
    }
}
