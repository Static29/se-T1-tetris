package tetris.games;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.block.*;
import tetris.board.*;
import tetris.score.DeletionScorePolicy;
import static org.junit.jupiter.api.Assertions.*;
import static tetris.block.TetrominoType.*;

class ItemGameplayIntegrationTest {
    @Test void crossingTenWithTwoRowsShowsItemInNextAndCarriesRemainder() {
        Board board = new Board();
        Game game = new Game(board, () -> new Block(I),
                new GameSettings(Difficulty.NORMAL, false, GameMode.ITEM), new DeletionScorePolicy(), new Random(42));
        game.start();
        for (int i = 0; i < 9; i++) {
            fillGap(board);
            game.hardDrop();
            game.advanceTime(Game.LINE_CLEAR_MILLIS);
        }
        assertFalse(game.getNextBlock().hasItem());
        assertEquals(1, game.getLinesUntilItem());
        for (int row = 20; row < 22; row++) for (int col = 0; col < 10; col++)
            if (col != 5) board.lock(List.of(new Position(row, col)), Cell.J);
        game.rotateClockwise();
        game.hardDrop();
        assertEquals(11, game.getClearedLines());
        assertEquals(9, game.getLinesUntilItem());
        assertFalse(game.getCurrentBlock().hasItem());
        assertTrue(game.getNextBlock().hasItem());
        game.advanceTime(Game.LINE_CLEAR_MILLIS);
        var preview = game.getNextBlock();
        assertTrue(game.hold());
        assertEquals(preview.getItemType(), game.getCurrentBlock().getItemType());
        assertFalse(game.getNextBlock().hasItem());
        assertEquals(9, game.getLinesUntilItem());
    }

    private void fillGap(Board board) {
        for (int col = 0; col < 10; col++)
            if (col < 3 || col > 6) board.lock(List.of(new Position(21, col)), Cell.J);
    }

    @Test void holdDoesNotPayAgainForPreviouslyReachedDepth() {
        Game game = new Game(() -> T);
        game.start();
        for (int i = 0; i < 6; i++) game.softDrop();
        assertEquals(60, game.getScore());
        game.hold();
        game.hardDrop();
        game.hold();
        long before = game.getScore();
        for (int i = 0; i < 6; i++) assertTrue(game.softDrop());
        assertEquals(before, game.getScore());
        assertTrue(game.softDrop());
        assertEquals(before + 10, game.getScore());
    }

    @Test void ghostPredictsLandingWithoutChangingBoardOrScore() {
        Board board = new Board();
        board.lock(List.of(new Position(20, 4)), Cell.J);
        Game game = new Game(() -> T, board);
        game.start();
        game.rotateClockwise();
        String before = board.toString();
        var ghost = game.getGhostCells();
        assertEquals(4, ghost.size());
        assertEquals(before, board.toString());
        assertEquals(0, game.getScore());
        game.hardDrop();
        for (Position p : ghost) assertEquals(Cell.T, board.getCell(p.row() + 2, p.col()));
    }

    @Test void clearFlashFreezesActionsAndPauseFreezesItsClock() {
        Board board = new Board();
        fillGap(board);
        Game game = new Game(() -> I, board);
        game.start();
        game.hardDrop();
        long score = game.getScore();
        assertEquals(Set.of(19), game.getFlashingVisibleRows());
        assertEquals(Cell.I, game.getVisibleCells()[19][3]);
        assertTrue(board.isEmpty(21, 3));
        assertFalse(game.moveLeft());
        assertFalse(game.hold());
        assertEquals(0, game.hardDrop());
        game.advanceTime(60);
        assertFalse(game.isClearFlashOn());
        game.pause();
        game.advanceTime(10000);
        assertTrue(game.isClearingLines());
        game.resume();
        game.advanceTime(299);
        assertTrue(game.isClearingLines());
        game.advanceTime(1);
        assertFalse(game.isClearingLines());
        assertEquals(score, game.getScore());
        assertTrue(game.canHold());
    }

    @ParameterizedTest @EnumSource(GameMode.class)
    void itemsAppearAfterTenLinesOnlyInItemMode(GameMode mode) {
        Board board = new Board();
        Game game = new Game(board, () -> new Block(I),
                new GameSettings(Difficulty.NORMAL, false, mode), new DeletionScorePolicy(), new Random(42));
        game.start();
        for (int i = 0; i < 10; i++) {
            assertFalse(game.getCurrentBlock().hasItem());
            fillGap(board);
            game.hardDrop();
            game.advanceTime(Game.LINE_CLEAR_MILLIS);
        }
        assertEquals(10, game.getClearedLines());
        assertFalse(game.getCurrentBlock().hasItem());
        assertEquals(mode == GameMode.ITEM, game.getNextBlock().hasItem());
        var preview = game.getNextBlock();
        game.hardDrop();
        assertEquals(preview.getType(), game.getCurrentBlock().getType());
        assertEquals(preview.getItemType(), game.getCurrentBlock().getItemType());
        assertTrue(Arrays.deepEquals(preview.getCells(), game.getCurrentBlock().getCells()));
    }

    @ParameterizedTest @EnumSource(ItemType.class)
    void gameAppliesSameItemResultAndScoreAsBoardLocker(ItemType item) {
        Block block = new ItemBlockFactory(new Random(9)).create(true, T, item, 0, 0);
        Board board = new Board();
        Game game = new Game(board, () -> block, GameSettings.DEFAULT,
                new DeletionScorePolicy(), new Random(42));
        game.start();
        Board expectedBoard = board.copy();
        Block expectedBlock = game.getCurrentBlock();
        int distance = 0;
        if (item == ItemType.WEIGHT) {
            WeightMover mover = new WeightMover(expectedBoard, expectedBlock);
            while (mover.stepDown()) distance++;
        } else {
            while (expectedBoard.canPlace(Position.fromXY(expectedBlock.previewMove(0, 1)))) {
                expectedBlock.move(0, 1); distance++;
            }
        }
        var result = new BlockLocker(new Random(42)).lock(expectedBoard, expectedBlock);
        var policy = new DeletionScorePolicy();
        double speed = game.getSpeedMultiplier();
        long deletion = policy.clearedRows(result.fullRowsCleared(), Difficulty.NORMAL, speed)
                + (item == ItemType.LINE_CLEAR
                    ? policy.lineItemRemovedCells(result.itemCellsRemoved(), Difficulty.NORMAL, speed)
                    : policy.removedCells(result.itemCellsRemoved(), Difficulty.NORMAL, speed));
        assertEquals(distance, game.hardDrop());
        assertEquals(expectedBoard.toString(), board.toString());
        assertEquals(result.totalRowsCleared(), game.getClearedLines());
        assertEquals(distance * policy.descendedCells(1, Difficulty.NORMAL, speed)
                + deletion * (result.multiplierApplied() ? 2 : 1), game.getScore());
    }

    @Test void groundedPieceWaitsFiveHundredMillisecondsAndPauseDoesNotCount() {
        Game game = new Game(() -> T);
        game.start();
        while (game.softDrop()) { }
        int y = game.getCurrentBlock().getY();
        game.advanceTime(499);
        assertEquals(y, game.getCurrentBlock().getY());
        game.pause();
        game.advanceTime(10000);
        game.resume();
        game.advanceTime(1);
        assertEquals(0, game.getCurrentBlock().getY());
    }

    @Test void multiplierDoublesDeletionButNotDescent() {
        Board board = new Board();
        fillGap(board);
        board.markMultiplier(new Position(21, 0));
        Game game = new Game(() -> I, board);
        game.start();
        int distance = game.hardDrop();
        assertEquals(2000 + distance * 10, game.getScore());
    }

    @Test void fillCanClearMoreThanFourRows() {
        Block fill = new ItemBlockFactory(new Random(9)).create(true, T, ItemType.FILL, 0, 0);
        Board board = new Board();
        int column = 3 + fill.getItemCells()[0][0];
        for (int row = 17; row < 22; row++) for (int col = 0; col < 10; col++)
            if (col != column) board.lock(List.of(new Position(row, col)), Cell.J);
        Game game = new Game(board, () -> fill, GameSettings.DEFAULT, new DeletionScorePolicy());
        game.start();
        int distance = game.hardDrop();
        assertEquals(5, game.getClearedLines());
        assertEquals(7000 + distance * 10, game.getScore());
    }

    @Test void weightGhostDoesNotEraseRealBoard() {
        Block weight = new ItemBlockFactory(new Random(9)).create(true, T, ItemType.WEIGHT, 0, 0);
        Board board = new Board();
        board.lock(List.of(new Position(10, 4)), Cell.J);
        Game game = new Game(board, () -> weight, GameSettings.DEFAULT, new DeletionScorePolicy());
        game.start();
        String before = board.toString();
        var ghost = game.getGhostCells();
        assertEquals(6, ghost.size());
        assertEquals(before, board.toString());
        assertEquals(0, game.getScore());
        game.hardDrop();
        assertTrue(board.isEmpty(10, 4));
        for (Position p : ghost) assertEquals(Cell.WEIGHT, board.getCell(p.row() + 2, p.col()));
    }
}
