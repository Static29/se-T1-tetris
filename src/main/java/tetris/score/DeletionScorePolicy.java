package tetris.score;

import tetris.games.Difficulty;

public final class DeletionScorePolicy implements ScorePolicy {
    @Override
    public long removedCells(int count, Difficulty difficulty) {
        if (count < 0) throw new IllegalArgumentException("삭제 칸 수는 음수일 수 없습니다.");
        return difficulty.applyScore(count * 10L);
    }

    @Override
    public long clearedRows(int count, Difficulty difficulty) {
        if (count < 0 || count > 4) throw new IllegalArgumentException("한 번의 줄 삭제는 0~4줄입니다.");
        long base = switch (count) {
            case 0 -> 0;
            case 1 -> 100;
            case 2 -> 200;
            case 3 -> 300;
            default -> 600;
        };
        return difficulty.applyScore(base);
    }
}
