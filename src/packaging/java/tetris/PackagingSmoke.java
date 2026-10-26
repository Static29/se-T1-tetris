package tetris;

import tetris.games.*;

/** JUnit/JDK 설치에 의존하지 않고 축소된 배포 런타임에서 실행한다. */
public final class PackagingSmoke {
    public static void main(String[] args) {
        for (GameMode mode : GameMode.values()) {
            for (Difficulty difficulty : Difficulty.values()) {
                Game game = new Game(new GameSettings(difficulty, false, mode));
                game.start();
                if (game.getState() != GameState.RUNNING || game.getCurrentBlock() == null)
                    throw new AssertionError("Game did not start");
                game.advanceTime(1000);
                if (game.getScore() <= 0) throw new AssertionError("Gravity did not advance");
                game.pause();
                long pausedScore = game.getScore();
                game.advanceTime(1000);
                if (game.getScore() != pausedScore) throw new AssertionError("Pause failed");
                game.resume();
                if (!game.hold() || game.hardDrop() <= 0) throw new AssertionError("Input actions failed");
            }
        }
        System.out.println("PACKAGED_GAME_OK: all modes and difficulties started and advanced");
    }
}
