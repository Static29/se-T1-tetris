package tetris.score;

import tetris.games.Difficulty;
import tetris.block.ItemType;
import tetris.board.LockResult;

/** 같은 삭제 칸은 아이템 삭제 또는 완성 줄 삭제 중 한 경로로만 전달한다. */
public interface ScorePolicy {
    /** 한 고정 작업의 삭제 점수. 하강 점수는 M 배율 대상이 아니다. */
    default long lockedBlock(LockResult result, ItemType item, Difficulty difficulty, double speedMultiplier) {
        if (result.lockOut()) return 0;
        long cells = item == ItemType.LINE_CLEAR
                ? lineItemRemovedCells(result.itemCellsRemoved(), difficulty, speedMultiplier)
                : removedCells(result.itemCellsRemoved(), difficulty, speedMultiplier);
        long deletion = clearedRows(result.fullRowsCleared(), difficulty, speedMultiplier) + cells;
        return result.multiplierApplied() ? deletion * ItemType.MULTIPLIER.getScoreMultiplier() : deletion;
    }
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
