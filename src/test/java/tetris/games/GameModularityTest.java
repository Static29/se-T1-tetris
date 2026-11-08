package tetris.games;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.block.*;
import tetris.board.*;
import tetris.score.DeletionScorePolicy;
import static org.junit.jupiter.api.Assertions.*;

class GameModularityTest {
    @ParameterizedTest @EnumSource(TetrominoType.class)
    void startAndHoldUseSpawnerCoordinatesAndPreservePaidDepth(TetrominoType type) {
        Board board = new Board();
        var spawner = BlockSpawner.fromBlocks(board, () -> new Block(type),
                new ItemBlockFactory(new Random(1)), false);
        Block expected = spawner.spawn();
        Game game = new Game(() -> type, board);
        game.start();
        assertTrue(Arrays.deepEquals(expected.getBoardCells(), game.getCurrentBlock().getBoardCells()));
        for (int i = 0; i < 5; i++) assertTrue(game.softDrop());
        game.rotateClockwise();
        assertTrue(game.hold());
        game.hardDrop();
        assertTrue(game.hold());
        assertTrue(Arrays.deepEquals(expected.getBoardCells(), game.getCurrentBlock().getBoardCells()));
        long before = game.getScore();
        for (int i = 0; i < 5; i++) assertTrue(game.softDrop());
        assertEquals(before, game.getScore());
    }

    @Test void frameUsesCustomBufferAndNeverExposesMutableGameData() {
        Board board = new Board(12, 4, 8);
        board.lock(List.of(new Position(15, 0)), Cell.T);
        board.markMultiplier(new Position(15, 0));
        Game game = new Game(() -> TetrominoType.T, board);
        game.start();
        for (int i = 0; i < 4; i++) game.softDrop();
        var frame = game.getBoardSnapshot();
        assertEquals(12, frame.cells().length);
        assertEquals(8, frame.cells()[0].length);
        assertEquals('M', frame.symbols()[11][0]);
        assertEquals(Cell.T, frame.cells()[0][3]);
        frame.cells()[0][3] = Cell.EMPTY;
        frame.symbols()[11][0] = '.';
        assertEquals(Cell.T, frame.cells()[0][3]);
        assertEquals('M', frame.symbols()[11][0]);
        assertThrows(UnsupportedOperationException.class, () -> frame.ghostCells().clear());
        var landing = frame.ghostCells();
        game.hardDrop();
        for (Position p : landing) assertEquals(Cell.T, board.getCell(p.row() + 4, p.col()));
        assertEquals(Cell.T, frame.cells()[0][3]);
    }

    @Test void flashingRowsUseActualBufferAndHideActiveAndGhost() {
        Board board = new Board(12, 4, 8);
        for (int col = 0; col < 8; col++) if (col < 2 || col > 5)
            board.lock(List.of(new Position(15, col)), Cell.J);
        Game game = new Game(() -> TetrominoType.I, board);
        game.start();
        game.hardDrop();
        var frame = game.getBoardSnapshot();
        assertEquals(Set.of(11), frame.flashingRows());
        assertTrue(frame.ghostCells().isEmpty());
        assertEquals(Cell.I, frame.cells()[11][2]);
        game.advanceTime(Game.LINE_CLEAR_MILLIS);
        assertTrue(game.getBoardSnapshot().flashingRows().isEmpty());
        assertFalse(game.getBoardSnapshot().ghostCells().isEmpty());
    }

    @ParameterizedTest @EnumSource(Difficulty.class)
    void speedKeepsExactMultiplierAndExistingIntervalCap(Difficulty difficulty) {
        var speed = new GameSpeed(difficulty);
        var boardSpeed = new FallSpeed(difficulty.getSpeedIncreasePercent());
        for (int lines : new int[] {0, 10, 30, 10000}) {
            assertEquals(boardSpeed.getIntervalMillis(lines), speed.interval(lines, false));
            assertEquals(boardSpeed.getWeightIntervalMillis(lines), speed.interval(lines, true));
            assertEquals(boardSpeed.getSpeedMultiplier(lines), speed.multiplier(lines, false));
            assertEquals(boardSpeed.getSpeedMultiplier(lines) * 1.5, speed.multiplier(lines, true), 1e-10);
        }
        assertEquals(50, speed.interval(10000, true));
        assertTrue(speed.multiplier(10000, true) > 20);
    }

    @Test void snapshotOfReadyAndGameOverDoesNotPredictLanding() {
        Board board = new Board();
        board.lock(List.of(new Position(0, 4)), Cell.J);
        Game game = new Game(() -> TetrominoType.T, board);
        assertTrue(game.getBoardSnapshot().ghostCells().isEmpty());
        game.start();
        assertEquals(GameState.GAME_OVER, game.getState());
        assertTrue(game.getBoardSnapshot().ghostCells().isEmpty());
    }
}
