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

    /** 표시·점수는 일반 속도 기준이다. 무게추의 특수 가속은 낙하 간격에만 적용한다. */
    double multiplier(int lines) {
        return fallSpeed.getSpeedMultiplier(lines);
    }
}
