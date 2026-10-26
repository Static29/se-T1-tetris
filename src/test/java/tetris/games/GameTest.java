package tetris.games;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameTest {
    @Test
    void togglePauseWorksOnlyAfterStarting() {
        Game game = new Game();
        game.togglePause();
        assertEquals(GameState.READY, game.getState());
        game.start();
        game.togglePause();
        assertEquals(GameState.PAUSED, game.getState());
        game.togglePause();
        assertEquals(GameState.RUNNING, game.getState());
    }

    @Test
    void menuSessionCanStartPauseAndResume() {
        Game game = new Game();
        assertEquals(GameState.READY, game.getState());
        game.start();
        assertEquals(GameState.RUNNING, game.getState());
        game.pause();
        assertEquals(GameState.PAUSED, game.getState());
        game.resume();
        assertEquals(GameState.RUNNING, game.getState());
    }

    @Test
    void pauseAndResumeCannotStartAnUnstartedGame() {
        Game game = new Game();
        game.pause();
        game.resume();
        assertEquals(GameState.READY, game.getState());
    }

    @Test
    void startingAgainDoesNotResumePausedGame() {
        Game game = new Game();
        game.start();
        game.pause();
        game.start();
        assertEquals(GameState.PAUSED, game.getState());
    }
}
