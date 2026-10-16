package tetris.games;

import java.util.List;
import org.junit.jupiter.api.Test;
import tetris.board.Board;
import tetris.board.Cell;
import tetris.board.Position;
import static org.junit.jupiter.api.Assertions.*;
import static tetris.block.TetrominoType.*;

class GameBoardIntegrationTest {
    @Test
    void blockedRotationLeavesBoardAndActivePieceUnchanged() {
        Board board = new Board();
        Game game = new Game(() -> T, board);
        game.start();
        game.softDrop();
        game.softDrop();
        var occupied = java.util.Set.of(new Position(2, 4), new Position(3, 3),
                new Position(3, 4), new Position(3, 5));
        for (int row = 0; row < board.getTotalRows(); row++) {
            for (int col = 0; col < board.getColumns(); col++) {
                Position p = new Position(row, col);
                if (!occupied.contains(p)) board.lock(List.of(p), Cell.J);
            }
        }
        var before = game.getVisibleCells();
        String fixedBefore = board.toString();
        assertFalse(game.rotateClockwise());
        assertTrue(java.util.Arrays.deepEquals(before, game.getVisibleCells()));
        assertEquals(fixedBefore, board.toString());
    }

    @Test
    void holdChecksSpawnCollision() {
        Board board = new Board();
        Game game = new Game(() -> T, board);
        game.start();
        game.softDrop();
        game.softDrop();
        board.lock(List.of(new Position(0, 4)), Cell.J);
        assertTrue(game.hold());
        assertEquals(GameState.GAME_OVER, game.getState());
    }

    @Test
    void fallingPieceIsOnlyInSnapshotAndCannotCrossWall() {
        Board board = new Board();
        Game game = new Game(() -> T, board);
        game.start();
        game.softDrop();
        game.softDrop();
        for (int i = 0; i < 3; i++) assertTrue(game.moveLeft());
        assertFalse(game.moveLeft());
        assertEquals(Cell.T, game.getVisibleCells()[0][1]);
        assertEquals(Cell.EMPTY, board.getCell(2, 1));
        var snapshot = game.getVisibleCells();
        snapshot[0][1] = Cell.EMPTY;
        assertEquals(Cell.T, game.getVisibleCells()[0][1]);
    }

    @Test
    void gameUsesSrsFloorKick() {
        Game game = new Game(() -> T);
        game.start();
        for (int i = 0; i < 20; i++) assertTrue(game.softDrop());
        assertTrue(game.rotateClockwise());
        var cells = game.getVisibleCells();
        assertEquals(Cell.T, cells[17][3]);
        assertEquals(Cell.T, cells[18][3]);
        assertEquals(Cell.T, cells[18][4]);
        assertEquals(Cell.T, cells[19][3]);
    }

    @Test
    void gameUsesSrsWallKick() {
        Game game = new Game(() -> T);
        game.start();
        game.softDrop();
        game.softDrop();
        assertTrue(game.rotateClockwise());
        for (int i = 0; i < 4; i++) assertTrue(game.moveLeft());
        assertFalse(game.moveLeft());
        assertTrue(game.rotateClockwise());
        var cells = game.getVisibleCells();
        assertEquals(Cell.T, cells[1][0]);
        assertEquals(Cell.T, cells[1][1]);
        assertEquals(Cell.T, cells[1][2]);
        assertEquals(Cell.T, cells[2][1]);
    }

    @Test
    void hardDropLocksClearsAndAdvances() {
        Board board = new Board();
        for (int col = 0; col < 10; col++) {
            if (col < 3 || col > 6) board.lock(List.of(new Position(21, col)), Cell.J);
        }
        Game game = new Game(() -> I, board);
        game.start();
        assertEquals(20, game.hardDrop());
        assertEquals(1, game.getClearedLines());
        assertTrue(game.canHold());
        for (Cell[] row : board.getVisibleCells()) {
            for (Cell cell : row) assertEquals(Cell.EMPTY, cell);
        }
    }

    @Test
    void pauseRejectsAllGameplayActions() {
        Game game = new Game(() -> T);
        game.start();
        game.softDrop();
        game.softDrop();
        var before = game.getVisibleCells();
        game.pause();
        assertFalse(game.moveLeft());
        assertFalse(game.moveRight());
        assertFalse(game.rotateClockwise());
        assertFalse(game.softDrop());
        assertEquals(0, game.hardDrop());
        assertFalse(game.hold());
        game.tick();
        assertTrue(java.util.Arrays.deepEquals(before, game.getVisibleCells()));
        game.resume();
        assertTrue(game.moveLeft());
    }

    @Test
    void spawnCollisionEndsGame() {
        Board board = new Board();
        board.lock(List.of(new Position(0, 4)), Cell.J);
        Game game = new Game(() -> T, board);
        game.start();
        assertEquals(GameState.GAME_OVER, game.getState());
        assertFalse(game.rotateClockwise());
        assertFalse(game.hold());
    }

    @Test
    void lockingEntirelyInBufferEndsGameBeforeClearingRows() {
        Board board = new Board();
        for (int col = 0; col < 10; col++) board.lock(List.of(new Position(2, col)), Cell.J);
        Game game = new Game(() -> T, board);
        game.start();
        assertFalse(game.softDrop());
        assertEquals(GameState.GAME_OVER, game.getState());
        assertTrue(board.isRowFull(2));
        assertEquals(0, game.getClearedLines());
    }

    @Test
    void holdResetsPositionAndRotationAndUnlocksAfterLanding() {
        Game game = new Game(() -> T);
        game.start();
        game.softDrop();
        game.softDrop();
        game.rotateClockwise();
        game.moveLeft();
        assertTrue(game.hold());
        assertFalse(game.hold());
        game.softDrop();
        game.softDrop();
        assertEquals(Cell.T, game.getVisibleCells()[0][4]);
        assertEquals(Cell.T, game.getVisibleCells()[1][3]);
        game.hardDrop();
        assertTrue(game.canHold());
    }
}
