package tetris.games;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.block.*;
import tetris.board.*;
import tetris.score.DeletionScorePolicy;
import static org.junit.jupiter.api.Assertions.*;

class BattleIntegrationTest {
    private static final GarbageRow ROW = GarbageRow.withHoles(10, List.of(0));
    private Game game(Board board) { return new Game(() -> TetrominoType.T, board); }
    private Board twoLines() {
        Board board = new Board();
        for (int c = 0; c < 10; c++) {
            if (c != 4 && c != 5) {
                board.lock(List.of(new Position(20, c)), Cell.J);
                board.lock(List.of(new Position(21, c)), Cell.J);
            }
        }
        return board;
    }
    @Test void incomingWaitsUntilLockThenPushesBeforeSpawn() {
        Board board = new Board(); Game game = game(board); game.start();
        assertEquals(1, game.receiveAttack(List.of(ROW)));
        game.softDrop(); game.hold();
        assertEquals(1, game.getPendingRows().size());
        assertEquals(Cell.EMPTY, board.getCell(21, 1));
        game.hardDrop();
        assertTrue(game.getPendingRows().isEmpty());
        assertEquals(Cell.GARBAGE, board.getCell(21, 1));
        assertEquals(Cell.EMPTY, board.getCell(21, 0));
        assertEquals(0, game.getCurrentBlock().getY());
    }
    @Test void overflowLosesWithoutTakingNextAndQueueCapsAtTen() {
        Board board = new Board(); board.lock(List.of(new Position(0, 0)), Cell.J);
        AtomicInteger created = new AtomicInteger();
        Game game = new Game(() -> { created.incrementAndGet(); return TetrominoType.T; }, board);
        game.start(); int before = created.get();
        assertEquals(10, game.receiveAttack(Collections.nCopies(12, ROW)));
        assertTrue(game.isAttackQueueFull()); assertEquals(0, game.receiveAttack(List.of(ROW)));
        game.hardDrop();
        assertEquals(GameState.GAME_OVER, game.getState()); assertEquals(before, created.get());
        assertEquals(0, game.receiveAttack(List.of(ROW)));
    }
    @Test void randomAttackExcludesSelfFullQueueAndEliminatedPlayers() {
        Game attacker = new Game(() -> TetrominoType.O, twoLines()), full = game(new Board()), target = game(new Board());
        Board blocked = new Board(); blocked.lock(List.of(new Position(0, 4)), Cell.J);
        Game lost = game(blocked);
        var match = new BattleMatch(List.of(attacker, full, target, lost), BattleMode.NORMAL, 180, new Random(1));
        full.receiveAttack(Collections.nCopies(10, ROW));
        match.act(0, GameAction.HARD_DROP);
        assertEquals(2, target.getPendingRows().size());
        assertEquals(10, full.getPendingRows().size());
        assertTrue(attacker.getPendingRows().isEmpty()); assertTrue(lost.getPendingRows().isEmpty());
    }
    @Test void noEligibleTargetDropsAttackWithoutFailure() {
        Game attacker = new Game(() -> TetrominoType.O, twoLines()), target = game(new Board());
        var match = new BattleMatch(List.of(attacker, target), BattleMode.NORMAL, 180, new Random(1));
        target.receiveAttack(Collections.nCopies(10, ROW));
        assertDoesNotThrow(() -> match.act(0, GameAction.HARD_DROP));
        assertEquals(10, target.getPendingRows().size());
    }
    @Test void multiplierItemPreservesLineAttackAndDoublesDeletionScore() {
        Game game = new Game(twoLines(), () -> new ItemBlockFactory(new Random(3))
                .create(true, TetrominoType.O, ItemType.MULTIPLIER, 0, 0), GameSettings.DEFAULT, new DeletionScorePolicy());
        var sent = new ArrayList<GarbageRow>(); game.setAttackListener(sent::addAll); game.start(); int distance = game.hardDrop();
        assertEquals(2, game.getClearedLines());
        assertEquals(List.of(GarbageRow.withHoles(10, List.of(4, 5)), GarbageRow.withHoles(10, List.of(4, 5))), sent);
        assertEquals(4000 + distance * 10L, game.getScore());
    }
    @Test void timedWinnerUsesScoresAndStopsAllGames() {
        var match = new BattleMatch(2, BattleMode.TIMED, 1, GameSettings.DEFAULT);
        match.act(0, GameAction.SOFT_DROP); match.advanceTime(2000);
        assertTrue(match.finished()); assertEquals(List.of(0), match.winners());
        assertEquals("PLAYER1 승리", match.result()); assertEquals(0, match.remainingMillis());
        long score = match.games().get(0).getScore();
        match.act(0, GameAction.HARD_DROP); match.advanceTime(10000);
        assertEquals(score, match.games().get(0).getScore());
    }
    @Test void timedTieAndEliminationAreDistinctOutcomes() {
        var tied = new BattleMatch(2, BattleMode.TIMED, 1, GameSettings.DEFAULT);
        tied.advanceTime(1000); assertEquals("무승부", tied.result());
        Board board = new Board(); board.lock(List.of(new Position(0, 4)), Cell.J);
        var won = new BattleMatch(List.of(game(board), game(new Board())), BattleMode.NORMAL, 180, new Random(1));
        assertTrue(won.finished()); assertEquals(List.of(1), won.winners());
        assertThrows(IllegalArgumentException.class, () -> won.advanceTime(-1));
        assertThrows(IllegalArgumentException.class, () -> new BattleMatch(1, BattleMode.NORMAL, 180, GameSettings.DEFAULT));
    }
    @ParameterizedTest @EnumSource(BattleMode.class)
    void frameRoundTripPreservesPreviewGarbageScoreAndBoard(BattleMode mode) {
        Game game = new Game(mode.settings(GameSettings.DEFAULT)); game.start();
        game.receiveAttack(List.of(ROW)); game.hold(); game.softDrop();
        var frame = BattleFrame.of(game, "PLAYER3"); var copy = BattleFrame.decode(frame.encode());
        assertEquals(frame.name(), copy.name()); assertEquals(frame.score(), copy.score());
        assertEquals(frame.next(), copy.next()); assertEquals(frame.hold(), copy.hold());
        assertEquals(frame.pending(), copy.pending()); assertEquals(frame.board().ghostCells(), copy.board().ghostCells());
        assertTrue(Arrays.deepEquals(frame.board().cells(), copy.board().cells()));
        assertTrue(Arrays.deepEquals(frame.board().symbols(), copy.board().symbols()));
        assertThrows(IllegalArgumentException.class, () -> BattleFrame.decode("invalid"));
        assertThrows(IllegalArgumentException.class, () -> BattleFrame.decode("A".repeat(16001)));
        assertThrows(IllegalArgumentException.class, () -> BattleFrame.decodeRows("111"));
        assertEquals(List.of(), BattleFrame.decodeRows(""));
    }
}
