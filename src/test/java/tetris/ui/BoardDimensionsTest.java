package tetris.ui;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import tetris.board.Board;
import tetris.board.Cell;
import tetris.games.Game;
import tetris.block.TetrominoType;
import static org.junit.jupiter.api.Assertions.*;

class BoardDimensionsTest {
    @Test void customBoardSizeIsSharedByTextColorAndGameViews() {
        Board board = new Board(12, 4, 8);
        Cell[][] snapshot = board.getVisibleCells();
        var text = new BoardView(12, 8).render(snapshot).split("\n");
        assertEquals(14, text.length);
        assertEquals("X".repeat(19), text[0]);
        var color = new ColoredBoardView(12, 8);
        assertDoesNotThrow(() -> color.render(snapshot, false));
        assertEquals(14, color.getChildren().size());
        Game game = new Game(() -> TetrominoType.T, board);
        game.start();
        assertDoesNotThrow(() -> new GameView(game, () -> {}, () -> {}).refresh());
    }

    @Test void malformedSnapshotsAreRejectedBeforeRendering() {
        assertThrows(IllegalArgumentException.class, () -> new BoardView(0, 10));
        var color = new ColoredBoardView(2, 3);
        var cells = new Cell[2][3];
        for (var row : cells) Arrays.fill(row, Cell.EMPTY);
        assertThrows(IllegalArgumentException.class, () -> color.render(new Cell[1][3], false));
        assertThrows(IllegalArgumentException.class, () -> color.render(cells, new char[1][3],
                java.util.List.of(), java.util.Set.of(), false, false));
        cells[1][1] = null;
        assertThrows(IllegalArgumentException.class, () -> color.render(cells, false));
    }
}
