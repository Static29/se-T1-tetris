package tetris.score;

import tetris.games.Difficulty;

/** 같은 삭제 칸은 아이템 삭제 또는 완성 줄 삭제 중 한 경로로만 전달한다. */
public interface ScorePolicy {
    long removedCells(int count, Difficulty difficulty);
    long clearedRows(int count, Difficulty difficulty);
    default long lineItemRemovedCells(int count, Difficulty difficulty, double speedMultiplier) {
        return removedCells(count, difficulty, speedMultiplier);
    }
    default long removedCells(int count, Difficulty difficulty, double speedMultiplier) {
        return removedCells(count, difficulty);
    }
    default long clearedRows(int count, Difficulty difficulty, double speedMultiplier) {
        return clearedRows(count, difficulty);
    }
    default long descendedCells(int count, Difficulty difficulty, double speedMultiplier) {
        return 0;
    }
}
