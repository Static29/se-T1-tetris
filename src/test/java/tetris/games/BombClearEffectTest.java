package tetris.games;

import java.util.Random;
import org.junit.jupiter.api.Test;
import tetris.block.*;
import tetris.board.*;
import tetris.score.DeletionScorePolicy;
import static org.junit.jupiter.api.Assertions.*;

class BombClearEffectTest {
    @Test void bombFlashesOnlyRemovedCellsAndPausesWithoutChangingScoreOrBoard() {
        var board = new Board();
        var factory = new ItemBlockFactory(new Random(1));
        var game = new Game(board, () -> factory.create(true, TetrominoType.O, ItemType.BOMB, 0, 0),
                new GameSettings(Difficulty.NORMAL, false, GameMode.ITEM), new DeletionScorePolicy(), new Random(2));
        game.start(); game.hardDrop();
        assertTrue(game.isClearingLines());
        var bright = game.getBoardSnapshot();
        var actual = board.getVisibleCells(); long score = game.getScore();
        assertEquals(0, game.hardDrop());
        game.pause(); game.advanceTime(600);
        assertTrue(game.isClearFlashOn());
        game.resume(); game.advanceTime(60);
        var dark = BattleFrame.decode(BattleFrame.of(game, "PLAYER1").encode()).board();
        int flashed = 0;
        for (int r = 0; r < actual.length; r++) for (int c = 0; c < actual[r].length; c++) {
            if (bright.cells()[r][c] == Cell.WEIGHT) {
                flashed++; assertTrue(dark.cells()[r][c].isEmpty()); assertTrue(actual[r][c].isEmpty());
            } else assertEquals(bright.cells()[r][c], dark.cells()[r][c]);
        }
        assertTrue(flashed > 0); assertTrue(dark.ghostCells().isEmpty());
        game.advanceTime(Game.LINE_CLEAR_MILLIS - 60);
        assertFalse(game.isClearingLines()); assertEquals(score, game.getScore());
        assertArrayEquals(actual, board.getVisibleCells());
    }
}
