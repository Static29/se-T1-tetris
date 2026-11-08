package tetris.board;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tetris.block.Block;
import tetris.block.ItemBlockFactory;
import tetris.block.TetrominoType;
import static org.junit.jupiter.api.Assertions.*;
import static tetris.board.BoardFixtures.*;

/** 기본 보드는 0~1번 줄이 버퍼, 2번 줄이 화면에 보이는 첫 줄이다. 생성 원점은 y = 0(버퍼 맨 위)이다. */
class BlockSpawnerTest {
    private static final long SEED = 20261024L;

    /** type만 나오는 선택기. */
    private static PieceSelector only(TetrominoType type) {
        return new PieceSelector(Map.of(type, 1), new Random(SEED));
    }

    private static BlockSpawner spawner(PieceSelector selector, boolean itemMode) {
        return new BlockSpawner(new Board(), selector, new ItemBlockFactory(new Random(SEED)), itemMode);
    }

    @Test
    void everyPieceSpawnsCenteredWithOriginAtTopOfBuffer() {
        for (TetrominoType type : TetrominoType.values()) {
            Board board = new Board();
            Block block = new BlockSpawner(board, only(type), new ItemBlockFactory(new Random(SEED)), false).spawn();
            assertEquals(BlockSpawner.SPAWN_Y, block.getY(), type.name());
            assertEquals((board.getColumns() - block.getFrameSize()) / 2, block.getX(), type.name());
            assertFalse(board.isBlockOut(Position.fromXY(block.getBoardCells())), type.name());
        }
    }

    @Test
    void iPieceSpawnsInBufferRowOneColumnsThreeToSix() {
        Block block = spawner(only(TetrominoType.I), false).spawn();
        assertEquals(Set.of(new Position(1, 3), new Position(1, 4), new Position(1, 5), new Position(1, 6)),
                occupied(block));
    }

    @Test
    void tPieceSpawnsInBothBufferRows() {
        Block block = spawner(only(TetrominoType.T), false).spawn();
        assertEquals(Set.of(new Position(0, 4), new Position(1, 3), new Position(1, 4), new Position(1, 5)),
                occupied(block));
    }

    @Test
    void resetToSpawnPlacesBlockStoredAtOriginLikeSpawn() {
        // Game의 Hold는 블록을 spawnCopy(0, 0)으로 보관했다가 꺼낼 때 resetToSpawn으로 생성 위치에 놓는다.
        BlockSpawner spawner = spawner(only(TetrominoType.L), false);
        Block spawned = spawner.spawn();
        Block held = spawned.spawnCopy(0, 0);
        spawner.resetToSpawn(held);
        assertEquals(occupied(spawned), occupied(held));
    }

    @Test
    void spawnReturnsThePreviewedNextAndPreparesANewOne() {
        BlockSpawner spawner = spawner(PieceSelector.withIWeight(10, new Random(SEED)), false);
        Block previewed = spawner.peekNext();
        Block spawned = spawner.spawn();
        assertSame(previewed, spawned);
        assertNotSame(spawned, spawner.peekNext());
    }

    @Test
    void realSpawnCodeFollowsDifficultyProbabilityWithinFivePercent() {
        // Req2: 실제 게임에서 블록을 생성하는 코드가 확률 테스트를 통과해야 한다.
        PieceSelector selector = PieceSelector.withIWeight(PieceSelector.EASY_I_WEIGHT, new Random(SEED));
        BlockSpawner spawner = spawner(selector, false);
        int drawCount = 100_000;
        Map<TetrominoType, Integer> counts = new EnumMap<>(TetrominoType.class);
        for (int i = 0; i < drawCount; i++) {
            counts.merge(spawner.spawn().getType(), 1, Integer::sum);
        }
        for (TetrominoType type : TetrominoType.values()) {
            double expected = selector.getProbability(type) * drawCount;
            assertEquals(expected, counts.get(type), expected * 0.05, type.name());
        }
    }

    @Test
    void classicModeNeverMakesItems() {
        BlockSpawner spawner = spawner(PieceSelector.withIWeight(10, new Random(SEED)), false);
        spawner.onLinesCleared(30);
        assertFalse(spawner.peekNext().hasItem());
        for (int i = 0; i < 10; i++) {
            assertFalse(spawner.spawn().hasItem());
        }
    }

    @Test
    void tenthLineReplacesVisibleNextWithItemImmediately() {
        BlockSpawner spawner = spawner(only(TetrominoType.T), true);
        spawner.onLinesCleared(9);
        assertFalse(spawner.peekNext().hasItem());
        assertEquals(1, spawner.getLinesUntilItem());
        spawner.onLinesCleared(1);
        assertTrue(spawner.peekNext().hasItem());
        assertTrue(spawner.spawn().hasItem());
        assertFalse(spawner.peekNext().hasItem(), "아이템은 한 번만 나온다");
    }

    @Test
    void extraLinesCarryOverToNextItem() {
        BlockSpawner spawner = spawner(only(TetrominoType.T), true);
        spawner.onLinesCleared(9);
        spawner.onLinesCleared(2);
        assertTrue(spawner.peekNext().hasItem());
        assertEquals(9, spawner.getLinesUntilItem());
    }

    @Test
    void twentyLinesAtOnceQueueTheSecondItemForTheFollowingNext() {
        BlockSpawner spawner = spawner(only(TetrominoType.T), true);
        spawner.onLinesCleared(20);
        assertTrue(spawner.peekNext().hasItem());
        assertEquals(1, spawner.getPendingItemCount());
        spawner.spawn();
        assertTrue(spawner.peekNext().hasItem());
        assertEquals(0, spawner.getPendingItemCount());
        spawner.spawn();
        assertFalse(spawner.peekNext().hasItem());
    }

    @Test
    void resetToSpawnRestoresRotationAndPosition() {
        BlockSpawner spawner = spawner(only(TetrominoType.T), false);
        Block block = spawner.spawn();
        Set<Position> atSpawn = occupied(block);
        block.rotate();
        block.rotate();
        block.move(-2, 7);
        spawner.resetToSpawn(block);
        assertEquals(0, block.getRotationState());
        assertEquals(atSpawn, occupied(block));
    }

    @Test
    void heldItemBlockKeepsItsItemAfterReset() {
        BlockSpawner spawner = spawner(only(TetrominoType.L), true);
        spawner.onLinesCleared(10);
        Block item = spawner.spawn();
        int itemCellsBefore = item.getItemCells().length;
        item.rotate();
        item.move(3, 5);
        spawner.resetToSpawn(item);
        assertTrue(item.hasItem());
        assertEquals(0, item.getRotationState());
        assertEquals(itemCellsBefore, item.getItemCells().length);
    }

    @Test
    void rejectsNegativeLinesAndNulls() {
        BlockSpawner spawner = spawner(only(TetrominoType.T), true);
        assertThrows(IllegalArgumentException.class, () -> spawner.onLinesCleared(-1));
        assertThrows(NullPointerException.class, () -> spawner.resetToSpawn(null));
        PieceSelector selector = only(TetrominoType.T);
        ItemBlockFactory factory = new ItemBlockFactory();
        assertThrows(NullPointerException.class, () -> new BlockSpawner(null, selector, factory, true));
        assertThrows(NullPointerException.class, () -> new BlockSpawner(new Board(), null, factory, true));
        assertThrows(NullPointerException.class, () -> new BlockSpawner(new Board(), selector, null, true));
    }
}
