package tetris.games;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.block.*;
import tetris.board.Board;
import tetris.score.DeletionScorePolicy;
import static org.junit.jupiter.api.Assertions.*;
import static tetris.block.TetrominoType.*;

class BlockHoldTest {
    @Test
    void weightCannotBeHeldBeforeOrAfterCrushingAndPreservesExistingHold() {
        Block weight = new ItemBlockFactory(new Random(42)).create(true, T, ItemType.WEIGHT, 0, 0);
        var queue = new ArrayDeque<>(List.of(new Block(T), new Block(O), weight, new Block(I), new Block(S)));
        Board board = new Board();
        Game game = new Game(board, queue::removeFirst, GameSettings.DEFAULT, new DeletionScorePolicy());
        game.start();
        assertTrue(game.hold());
        game.hardDrop();
        assertEquals(ItemType.WEIGHT, game.getCurrentBlock().getItemType());
        assertFalse(game.canHold());
        assertFalse(game.hold());
        board.lock(List.of(new tetris.board.Position(5, 4)), tetris.board.Cell.J);
        for (int i = 0; i < 6; i++) assertTrue(game.softDrop());
        assertTrue(board.isEmpty(5, 4));
        long score = game.getScore();
        int y = game.getCurrentBlock().getY();
        String snapshot = board.toString();
        assertFalse(game.hold());
        assertEquals(y, game.getCurrentBlock().getY());
        assertEquals(score, game.getScore());
        assertEquals(snapshot, board.toString());
        assertEquals(T, game.getHeldType());
        assertEquals(I, game.getNextType());
        game.hardDrop();
        assertTrue(game.canHold());
        assertTrue(game.hold());
        assertEquals(T, game.getCurrentType());
    }

    @ParameterizedTest
    @EnumSource(value = ItemType.class, names = "WEIGHT", mode = EnumSource.Mode.EXCLUDE)
    void holdSwapPreservesItemAndResetsOnlyPositionAndOrientation(ItemType item) {
        Block original = new ItemBlockFactory(new Random(42)).create(true, T, item, 8, 12);
        var expectedItemCells = original.spawnCopy(0, 0).getItemCells();
        var queue = new ArrayDeque<>(List.of(original, new Block(O), new Block(I), new Block(S), new Block(J)));
        Game game = new Game(new Board(), queue::removeFirst, GameSettings.DEFAULT, new DeletionScorePolicy());
        game.start();
        game.softDrop();
        game.softDrop();
        game.rotateClockwise();
        game.moveLeft();
        assertTrue(game.hold());
        assertFalse(game.hold());
        Block held = game.getHeldBlock();
        assertEquals(item, held.getItemType());
        assertEquals(0, held.getRotationState());
        assertEquals(0, held.getX());
        assertEquals(0, held.getY());
        assertTrue(Arrays.deepEquals(expectedItemCells, held.getItemCells()));
        assertEquals(I, game.getNextType());
        game.hardDrop();
        assertTrue(game.hold());
        Block restored = game.getCurrentBlock();
        assertEquals(item, restored.getItemType());
        assertEquals(0, restored.getRotationState());
        assertTrue(Arrays.deepEquals(expectedItemCells, restored.getItemCells()));
        assertEquals(S, game.getNextType());
        assertEquals(I, game.getHeldType());
        assertDoesNotThrow(game::getVisibleCells);
        assertDoesNotThrow(game::hardDrop);
    }

    @Test
    void nextRetainsGeneratedItemAndGettersCannotMutateGameOrGenerator() {
        Block next = new ItemBlockFactory(new Random(7)).create(true, I, ItemType.BOMB, 9, 9);
        var queue = new ArrayDeque<>(List.of(new Block(T), next, new Block(O)));
        Game game = new Game(new Board(), queue::removeFirst, GameSettings.DEFAULT, new DeletionScorePolicy());
        game.start();
        assertEquals(ItemType.BOMB, game.getNextBlock().getItemType());
        Block exposed = game.getNextBlock();
        exposed.rotate();
        exposed.move(100, 100);
        assertEquals(0, game.getNextBlock().getRotationState());
        assertEquals(0, game.getNextBlock().getX());
        assertEquals(9, next.getX());
        game.getCurrentBlock().move(-100, -100);
        assertEquals(3, game.getCurrentBlock().getX());
        game.hold();
        assertEquals(ItemType.BOMB, game.getCurrentBlock().getItemType());
        game.getHeldBlock().move(100, 100);
        assertEquals(0, game.getHeldBlock().getX());
    }

    @Test
    void blockCopyPreservesRotationButSpawnCopyRestoresOriginalItemCell() {
        Block block = new ItemBlockFactory(new Random(3)).create(true, L, ItemType.FILL, 3, 5);
        var original = block.getItemCells();
        block.rotate();
        Block copy = block.copy();
        assertEquals(1, copy.getRotationState());
        assertTrue(Arrays.deepEquals(block.getBoardItemCells(), copy.getBoardItemCells()));
        copy.move(100, 100);
        assertEquals(3, block.getX());
        Block spawn = block.spawnCopy(4, 0);
        assertEquals(0, spawn.getRotationState());
        assertTrue(Arrays.deepEquals(original, spawn.getItemCells()));
        assertEquals(1, block.getRotationState());
    }
}
