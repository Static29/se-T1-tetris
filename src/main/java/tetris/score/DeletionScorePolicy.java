package tetris.score;

import tetris.games.Difficulty;

public final class DeletionScorePolicy implements ScorePolicy {
    @Override
    public long removedCells(int count, Difficulty difficulty) {
        return removedCells(count, difficulty, 1);
    }
    @Override
    public long removedCells(int count, Difficulty difficulty, double speedMultiplier) {
        if (count < 0) throw new IllegalArgumentException("삭제 칸 수는 음수일 수 없습니다.");
        return adjusted(count * 10L, difficulty, speedMultiplier);
    }

    @Override
    public long clearedRows(int count, Difficulty difficulty) {
        return clearedRows(count, difficulty, 1);
    }
    @Override
    public long clearedRows(int count, Difficulty difficulty, double speedMultiplier) {
        if (count < 0 || count > 4) throw new IllegalArgumentException("한 번의 줄 삭제는 0~4줄입니다.");
        long base = switch (count) {
            case 0 -> 0;
            case 1 -> 100;
            case 2 -> 200;
            case 3 -> 300;
            default -> 600;
        };
        return adjusted(base * 10, difficulty, speedMultiplier);
    }

    @Override
    public long descendedCells(int count, Difficulty difficulty, double speedMultiplier) {
        if (count < 0) throw new IllegalArgumentException("하강 칸 수는 음수일 수 없습니다.");
        return adjusted(count * 10L, difficulty, speedMultiplier);
    }

    /** 속도 +1%마다 기본점수 +10%. 난이도 증가분과 합산하고 소수점 이하는 버린다. */
    private long adjusted(long base, Difficulty difficulty, double speedMultiplier) {
        if (!Double.isFinite(speedMultiplier) || speedMultiplier < 1)
            throw new IllegalArgumentException("속도 배율은 1 이상이어야 합니다.");
        double multiplier = 1 + (difficulty.getScorePercent() - 100) / 100.0
                + (speedMultiplier - 1) * 10;
        return (long) Math.floor(base * multiplier + 1e-8);
    }
}
