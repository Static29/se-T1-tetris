package tetris.ai;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import tetris.block.Block;
import tetris.block.TetrominoType;
import tetris.board.Board;
import tetris.games.BattleMatch;
import tetris.games.BattleMode;
import tetris.games.Game;
import tetris.games.GameAction;
import tetris.games.GameSettings;
import tetris.games.GameState;
import tetris.score.DeletionScorePolicy;
import static org.junit.jupiter.api.Assertions.*;

class ComputerOpponentTest {
    /** 시드를 고정한 무작위 블록 게임. */
    private static Game seededGame(long seed) {
        Random random = new Random(seed);
        TetrominoType[] types = TetrominoType.values();
        return new Game(new Board(), () -> new Block(types[random.nextInt(types.length)]),
                GameSettings.DEFAULT, new DeletionScorePolicy(), new Random(seed));
    }

    private static BattleMatch match() {
        return new BattleMatch(List.of(seededGame(11), seededGame(22)), BattleMode.NORMAL, 180, new Random(1));
    }

    @Test void pressesOneKeyOnlyAfterTheInterval() {
        BattleMatch match = match();
        var ai = new ComputerOpponent(match, 1, AiDifficulty.NORMAL);
        assertEquals(Optional.empty(), ai.advanceTime(499));
        Optional<GameAction> pressed = ai.advanceTime(1);
        assertTrue(pressed.isPresent());
        assertEquals(Optional.empty(), ai.advanceTime(100), "누른 뒤에는 다시 간격을 기다림");
        assertEquals(1, ai.player());
        assertEquals(AiDifficulty.NORMAL, ai.difficulty());
    }

    @Test void difficultySetsTheKeyInterval() {
        assertEquals(1000, AiDifficulty.EASY.intervalMillis());
        assertEquals(500, AiDifficulty.NORMAL.intervalMillis());
        assertEquals(200, AiDifficulty.HARD.intervalMillis());
        assertEquals("보통", AiDifficulty.NORMAL.toString());
        assertEquals("어려움", AiDifficulty.HARD.getLabel());
    }

    @Test void rejectsBadArguments() {
        BattleMatch match = match();
        assertThrows(IllegalArgumentException.class, () -> new ComputerOpponent(match, 2, AiDifficulty.EASY));
        assertThrows(IllegalArgumentException.class, () -> new ComputerOpponent(match, -1, AiDifficulty.EASY));
        assertThrows(NullPointerException.class, () -> new ComputerOpponent(match, 0, null));
        var ai = new ComputerOpponent(match, 0, AiDifficulty.EASY);
        assertThrows(IllegalArgumentException.class, () -> ai.advanceTime(-1));
    }

    @Test void computerBeatsAnIdlePlayer() {
        BattleMatch match = match();
        var ai = new ComputerOpponent(match, 1, AiDifficulty.HARD);
        for (long t = 0; t < 600_000 && !match.finished(); t += 16) {
            match.advanceTime(16);
            ai.advanceTime(16);
        }
        assertTrue(match.finished(), "가만히 있는 1P가 먼저 쌓여 끝나야 함");
        assertEquals(List.of(1), match.winners());
        assertTrue(match.games().get(1).getClearedLines() > 0, "컴퓨터는 줄을 지움");
        assertEquals(Optional.empty(), ai.advanceTime(1000), "끝난 대전에서는 누르지 않음");
    }

    @Test void computerSurvivesLongWithoutAttacks() {
        Game game = seededGame(33);
        BattleMatch match = new BattleMatch(List.of(game, seededGame(44)), BattleMode.TIMED, 120, new Random(1));
        var ai = new ComputerOpponent(match, 0, AiDifficulty.HARD);
        var other = new ComputerOpponent(match, 1, AiDifficulty.HARD);
        for (long t = 0; t < 120_000 && !match.finished(); t += 16) {
            match.advanceTime(16);
            ai.advanceTime(16);
            other.advanceTime(16);
        }
        assertTrue(match.finished());
        assertTrue(game.getClearedLines() >= 10, "2분 동안 10줄 이상 지움: " + game.getClearedLines());
        assertNotEquals(GameState.GAME_OVER, game.getState());
    }
}
