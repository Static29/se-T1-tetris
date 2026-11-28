package tetris.ai;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import tetris.block.Block;
import tetris.block.ItemBlockFactory;
import tetris.block.ItemType;
import tetris.block.TetrominoType;
import tetris.board.Board;
import tetris.board.BlockSpawner;
import tetris.board.Cell;
import tetris.board.Position;
import tetris.games.BattleMatch;
import tetris.games.Game;
import tetris.games.GameAction;
import tetris.games.GameSettings;
import tetris.score.DeletionScorePolicy;
import static org.junit.jupiter.api.Assertions.*;

class ComputerPlayerTest {
    private final ComputerPlayer player = new ComputerPlayer();

    /** 맨 아래 rows줄을 wellColumn만 비우고 채운다. */
    private static Board wellBoard(int rows, int wellColumn) {
        Board board = new Board();
        for (int r = board.getTotalRows() - rows; r < board.getTotalRows(); r++) {
            for (int c = 0; c < board.getColumns(); c++) {
                if (c != wellColumn) board.lock(List.of(new Position(r, c)), Cell.J);
            }
        }
        return board;
    }

    private static Game game(Board board, TetrominoType... types) {
        var queue = new ArrayDeque<>(List.of(types));
        return new Game(board, () -> new Block(queue.isEmpty() ? TetrominoType.O : queue.poll()),
                GameSettings.DEFAULT, new DeletionScorePolicy(), new Random(1));
    }

    /** AI가 누르는 키를 블록 하나가 고정될 때까지 그대로 적용한다. */
    private void playOnePiece(Game game) {
        int before = game.getClearedLines();
        Block first = game.getCurrentBlock();
        for (int i = 0; i < 40; i++) {
            GameAction action = player.nextAction(AiInput.of(game)).orElseThrow();
            BattleMatch.apply(game, action);
            if (action == GameAction.HARD_DROP) return;
        }
        fail("40번 안에 블록을 놓지 않음: " + first.getType() + ", 지운 줄 " + before);
    }

    @Test void putsVerticalIIntoTheWellAndClearsFourLines() {
        Game game = game(wellBoard(4, 9), TetrominoType.I);
        game.start();
        playOnePiece(game);
        assertEquals(4, game.getClearedLines());
    }

    @Test void fillsTheWellOnTheLeftSideToo() {
        Game game = game(wellBoard(1, 0), TetrominoType.I);
        game.start();
        playOnePiece(game);
        assertEquals(1, game.getClearedLines(), "세로 I로 왼쪽 끝 한 줄을 지움");
    }

    @Test void firstKeyFollowsRotateThenMoveThenDropOrder() {
        Game game = game(wellBoard(4, 9), TetrominoType.I);
        game.start();
        assertEquals(Optional.of(GameAction.ROTATE), player.nextAction(AiInput.of(game)));
    }

    @Test void holdsWhenTheHeldPieceIsMuchBetter() {
        Board board = wellBoard(4, 0);
        Block o = new Block(TetrominoType.O);
        o.move((board.getColumns() - o.getFrameSize()) / 2, BlockSpawner.SPAWN_Y);
        var input = new AiInput(board, o, new Block(TetrominoType.I), new Block(TetrominoType.O), true);
        assertEquals(Optional.of(GameAction.HOLD), player.nextAction(input));
    }

    @Test void doesNotHoldWhenHoldIsNotAllowed() {
        Board board = wellBoard(4, 0);
        Block o = new Block(TetrominoType.O);
        o.move((board.getColumns() - o.getFrameSize()) / 2, BlockSpawner.SPAWN_Y);
        var input = new AiInput(board, o, new Block(TetrominoType.I), null, false);
        assertNotEquals(Optional.of(GameAction.HOLD), player.nextAction(input));
    }

    @Test void usesNextWhenHoldIsEmpty() {
        Board board = wellBoard(4, 0);
        Block o = new Block(TetrominoType.O);
        o.move((board.getColumns() - o.getFrameSize()) / 2, BlockSpawner.SPAWN_Y);
        var input = new AiInput(board, o, null, new Block(TetrominoType.I), true);
        assertEquals(Optional.of(GameAction.HOLD), player.nextAction(input));
    }

    @Test void doesNothingWhenThereIsNoFallingBlock() {
        Game notStarted = game(new Board(), TetrominoType.T);
        assertEquals(Optional.empty(), player.nextAction(AiInput.of(notStarted)));
        assertThrows(NullPointerException.class, () -> player.nextAction(null));
    }

    @Test void dropsTheWeightRightAway() {
        Block weight = new ItemBlockFactory(new Random(1)).create(true, TetrominoType.T, ItemType.WEIGHT, 3, 0);
        var input = new AiInput(new Board(), weight, null, null, false);
        assertEquals(Optional.of(GameAction.HARD_DROP), player.nextAction(input));
    }

    @Test void inputExcludesTheFallingBlockAndKeepsLockedCells() {
        Game game = game(wellBoard(1, 4), TetrominoType.T);
        game.start();
        Board seen = AiInput.of(game).board();
        assertEquals(9, java.util.stream.IntStream.range(0, 10).filter(c -> !seen.isEmpty(21, c)).count());
        for (int r = 0; r < 21; r++) for (int c = 0; c < 10; c++) assertTrue(seen.isEmpty(r, c), "떨어지는 블록은 빠짐");
    }

    @Test void inputIsADefensiveCopy() {
        Board board = new Board();
        var input = new AiInput(board, new Block(TetrominoType.T), null, null, false);
        board.lock(List.of(new Position(21, 0)), Cell.J);
        input.board().lock(List.of(new Position(21, 1)), Cell.J);
        assertTrue(input.board().isEmpty(21, 0));
        assertTrue(input.board().isEmpty(21, 1));
        input.current().move(1, 0);
        assertEquals(0, input.current().getX());
    }

    @Test void evaluatorPrefersFewerHolesAndLowerStacks() {
        Board flat = wellBoard(1, 9);
        Board holey = wellBoard(1, 9);
        holey.lock(List.of(new Position(20, 3)), Cell.J);
        holey.removeCells(List.of(new Position(21, 3)));
        assertEquals(0, BoardEvaluator.holes(flat));
        assertEquals(1, BoardEvaluator.holes(holey));
        assertTrue(BoardEvaluator.score(flat, 0) > BoardEvaluator.score(holey, 0));
        assertEquals(1, BoardEvaluator.heights(flat)[0]);
        assertEquals(0, BoardEvaluator.heights(flat)[9]);
    }

    @Test void blockInTheBufferRowsIsTheWorstChoice() {
        Board low = new Board();
        Board top = new Board();
        top.lock(List.of(new Position(1, 0)), Cell.J);
        assertTrue(BoardEvaluator.score(top, 0) < BoardEvaluator.score(low, 0) + BoardEvaluator.TOP_OUT_PENALTY / 2);
    }
}
