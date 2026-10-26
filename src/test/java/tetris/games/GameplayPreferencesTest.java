package tetris.games;

import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.persistence.SettingsStore;
import tetris.score.*;
import tetris.board.*;
import static org.junit.jupiter.api.Assertions.*;
import static tetris.block.TetrominoType.*;

class GameplayPreferencesTest {
    @TempDir Path directory;

    @Test
    void speedBonusIsTenPercentForEachOnePercentAndAddsDifficultyBonus() {
        var policy = new DeletionScorePolicy();
        assertEquals(10, policy.descendedCells(1, Difficulty.NORMAL, 1));
        assertEquals(11, policy.descendedCells(1, Difficulty.NORMAL, 1.01));
        assertEquals(13, policy.descendedCells(1, Difficulty.HARD, 1.01));
        assertEquals(22, policy.descendedCells(1, Difficulty.HARD, 1.10));
        assertEquals(112, policy.descendedCells(1, Difficulty.HARD, 2));
        assertEquals(108, policy.descendedCells(1, Difficulty.EASY, 2));
        assertEquals(2200, policy.clearedRows(1, Difficulty.HARD, 1.10));
        assertEquals(88, policy.removedCells(4, Difficulty.HARD, 1.10));
        assertThrows(IllegalArgumentException.class, () -> policy.descendedCells(1, Difficulty.NORMAL, Double.NaN));
    }

    @Test
    void tenClearedLinesIncreaseRealFallSpeedAndSubsequentDescentPoints() {
        Board board = new Board();
        Game game = new Game(() -> I, board);
        game.start();
        assertEquals(1000, game.getFallIntervalMillis());
        for (int n = 0; n < 10; n++) {
            for (int col = 0; col < 10; col++)
                if (col < 3 || col > 6) board.lock(List.of(new Position(21, col)), Cell.J);
            game.hardDrop();
            game.advanceTime(Game.LINE_CLEAR_MILLIS);
        }
        assertEquals(10, game.getClearedLines());
        assertEquals(909, game.getFallIntervalMillis());
        long before = game.getScore();
        game.softDrop();
        assertEquals(20, game.getScore() - before);
        game.pause();
        game.tick();
        game.hardDrop();
        assertEquals(20, game.getScore() - before);
    }

    @Test
    void failedDownwardMovementAndRotationDoNotAwardDescentPoints() {
        Game game = new Game(() -> T);
        game.start();
        game.rotateClockwise();
        game.moveLeft();
        assertEquals(0, game.getScore());
        while (game.getScore() < 190) game.softDrop();
        long before = game.getScore();
        assertFalse(game.softDrop());
        assertEquals(before, game.getScore());
    }

    @Test
    void screenAndCustomKeysRoundTripWithoutChangingDefaults() throws Exception {
        var keys = KeyBindings.defaults().with(GameAction.LEFT, "A").with(GameAction.PAUSE, "P");
        var settings = new GameSettings(Difficulty.HARD, true, GameMode.ITEM, ScreenSize.FULLSCREEN, keys);
        var store = new SettingsStore(directory.resolve("settings"));
        store.save(settings);
        assertEquals(settings, store.load());
        assertEquals("LEFT", KeyBindings.defaults().key(GameAction.LEFT));
        assertThrows(IllegalArgumentException.class, () -> keys.with(GameAction.RIGHT, "A"));
        assertThrows(IllegalArgumentException.class, () -> keys.with(GameAction.RIGHT, "ENTER"));
        assertThrows(UnsupportedOperationException.class, () -> keys.keys().put(GameAction.LEFT, "B"));
    }

    @Test
    void resetOneModePreservesOtherAndFullResetPersists() throws Exception {
        var store = new LeaderboardStore(directory.resolve("leaderboard"));
        store.submit(new LeaderboardEntry(UUID.randomUUID(), "AAA", Difficulty.NORMAL, 100, GameMode.CLASSIC));
        var item = new LeaderboardEntry(UUID.randomUUID(), "BBB", Difficulty.NORMAL, 200, GameMode.ITEM);
        store.submit(item);
        store.clear(GameMode.CLASSIC);
        assertEquals(List.of(item), store.load());
        store.clearAll();
        assertTrue(new LeaderboardStore(directory.resolve("leaderboard")).load().isEmpty());
    }
}
