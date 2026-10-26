package tetris.games;

import java.util.List;
import org.junit.jupiter.api.Test;
import tetris.board.*;
import tetris.score.*;
import static tetris.block.TetrominoType.*;
import static org.junit.jupiter.api.Assertions.*;

class ScoreIntegrationTest {
    @Test
    void deletionTableAndDifficultyMultipliers() {
        ScorePolicy policy = new DeletionScorePolicy();
        long[][] expected = {{0, 800, 1600, 2400, 4800}, {0, 1000, 2000, 3000, 6000}, {0, 1200, 2400, 3600, 7200}};
        for (Difficulty d : Difficulty.values()) {
            for (int lines = 0; lines <= 4; lines++) assertEquals(expected[d.ordinal()][lines], policy.clearedRows(lines, d));
        }
        assertEquals(32, policy.removedCells(4, Difficulty.EASY));
        assertEquals(40, policy.removedCells(4, Difficulty.NORMAL));
        assertEquals(48, policy.removedCells(4, Difficulty.HARD));
        assertThrows(IllegalArgumentException.class, () -> policy.removedCells(-1, Difficulty.NORMAL));
        assertThrows(IllegalArgumentException.class, () -> policy.clearedRows(5, Difficulty.NORMAL));
    }

    @Test
    void allDropMethodsRewardOnlyActualDownwardDistance() {
        Game game = new Game(() -> T);
        game.start();
        game.tick();
        game.softDrop();
        int distance = game.hardDrop();
        assertEquals((2 + distance) * 10, game.getScore());
    }

    @Test
    void fourRowsGiveTotalNotPerCellPlusLinePoints() {
        for (Difficulty d : Difficulty.values()) {
            Board board = new Board();
            for (int row = 18; row < 22; row++) {
                for (int col = 0; col < 10; col++) {
                    if (col != 5) board.lock(List.of(new Position(row, col)), Cell.J);
                }
            }
            Game game = new Game(() -> I, board, new GameSettings(d, false), new DeletionScorePolicy());
            game.start();
            assertTrue(game.rotateClockwise());
            int distance = game.hardDrop();
            assertEquals(4, game.getClearedLines());
            assertEquals(d.applyScore(6000 + distance * 10), game.getScore());
        }
    }

    @Test
    void itemDeletionHookUsesSameDifficultyAndIgnoresPausedEvents() {
        Game game = new Game(new GameSettings(Difficulty.HARD, true));
        game.recordItemCellsRemoved(4);
        assertEquals(0, game.getScore());
        game.start();
        game.recordItemCellsRemoved(4);
        assertEquals(48, game.getScore());
        game.pause();
        game.recordItemCellsRemoved(4);
        assertEquals(48, game.getScore());
        assertTrue(game.getSettings().colorBlindMode());
    }

    @Test
    void scorePolicyCanBeReplacedWithoutChangingGame() {
        ScorePolicy policy = new ScorePolicy() {
            public long removedCells(int count, Difficulty difficulty) { return 7; }
            public long clearedRows(int count, Difficulty difficulty) { return 0; }
        };
        Game game = new Game(() -> T, new Board(), GameSettings.DEFAULT, policy);
        game.start();
        game.recordItemCellsRemoved(3);
        assertEquals(7, game.getScore());
    }
}
