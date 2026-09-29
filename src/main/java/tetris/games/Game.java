package tetris.games;

public class Game {
    private GameState state = GameState.READY;

    public GameState getState() {
        return state;
    }

    public void start() {
        if (state == GameState.READY) {
            state = GameState.RUNNING;
        }
    }

    public void pause() {
        if (state == GameState.RUNNING) {
            state = GameState.PAUSED;
        }
    }

    public void resume() {
        if (state == GameState.PAUSED) {
            state = GameState.RUNNING;
        }
    }
}
