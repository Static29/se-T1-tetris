package tetris.score;

import org.junit.jupiter.api.Test;
import tetris.block.ItemType;
import tetris.board.LockResult;
import tetris.games.Difficulty;
import static org.junit.jupiter.api.Assertions.*;

class LockScorePolicyTest {
    @Test void multiplierAppliesOnceToBothDeletionSources() {
        ScorePolicy policy = new DeletionScorePolicy();
        var result = new LockResult(false, 4, 1, 3, true);
        assertEquals(12600, policy.lockedBlock(result, ItemType.LINE_CLEAR, Difficulty.NORMAL, 1));
        assertEquals(12600, policy.lockedBlock(result, ItemType.BOMB, Difficulty.NORMAL, 1));
        assertEquals(0, policy.lockedBlock(LockResult.LOCK_OUT, null, Difficulty.NORMAL, 1));
        assertEquals(6000, policy.lockedBlock(new LockResult(false, 4, 0, 0, false), null, Difficulty.NORMAL, 1));
    }

    @Test void customLineItemPolicyStillReceivesLineItemDeletes() {
        ScorePolicy policy = new ScorePolicy() {
            public long removedCells(int count, Difficulty difficulty) { return count * 7; }
            public long clearedRows(int count, Difficulty difficulty) { return count * 11; }
            public long lineItemRemovedCells(int count, Difficulty difficulty, double speed) { return count * 13; }
        };
        var result = new LockResult(false, 1, 1, 2, true);
        assertEquals(74, policy.lockedBlock(result, ItemType.LINE_CLEAR, Difficulty.NORMAL, 1));
        assertEquals(50, policy.lockedBlock(result, ItemType.BOMB, Difficulty.NORMAL, 1));
    }
}
