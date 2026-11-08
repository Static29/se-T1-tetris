package tetris.games;

import tetris.board.FallSpeed;

/** Board의 시간 간격과 게임의 표시·점수용 배율을 연결한다. */
final class GameSpeed {
    private final FallSpeed fallSpeed;

    GameSpeed(Difficulty difficulty) {
        fallSpeed = new FallSpeed(difficulty.getSpeedIncreasePercent());
    }

    long interval(int lines, boolean weight) {
        return weight ? fallSpeed.getWeightIntervalMillis(lines) : fallSpeed.getIntervalMillis(lines);
    }

    double multiplier(int lines, boolean weight) {
        double normal = fallSpeed.getSpeedMultiplier(lines);
        // Board에 정확한 무게추 배율 API가 없어 상수로 연결한다. 정수 ms 역산은 오차·상한 정책을 바꾼다.
        return weight ? normal * FallSpeed.WEIGHT_SPEED_PERCENT / 100.0 : normal;
    }
}
