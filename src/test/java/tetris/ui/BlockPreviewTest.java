package tetris.ui;

import java.util.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.block.*;
import tetris.board.Board;
import tetris.games.*;
import tetris.score.DeletionScorePolicy;
import static org.junit.jupiter.api.Assertions.*;

class BlockPreviewTest {
    @ParameterizedTest
    @EnumSource(ItemType.class)
    void previewUsesActualItemCellsWithoutMutatingBlock(ItemType item) {
        Block block = new ItemBlockFactory(new Random(42)).create(true, TetrominoType.T, item, 8, 9);
        block.rotate();
        var before = block.getCells();
        var view = new PiecePreviewView("NEXT");
        view.showBlock(block, true);
        String formatted = PiecePreviewView.formatBlock(block);
        if (item == ItemType.WEIGHT) assertEquals(6, formatted.chars().filter(c -> c == 'O').count());
        else {
            assertEquals(1, formatted.chars().filter(c -> c == item.getSymbol()).count());
            assertEquals(3, formatted.chars().filter(c -> c == 'O').count());
        }
        assertEquals(formatted, ((Text)view.getChildren().get(1)).getText());
        assertTrue(Arrays.deepEquals(before, block.getCells()));
        assertEquals(8, block.getX());
    }

    @Test
    void gameViewShowsItemsInNextThenHold() {
        Block bomb = new ItemBlockFactory(new Random(1)).create(true, TetrominoType.I, ItemType.BOMB, 0, 0);
        Block fill = new ItemBlockFactory(new Random(2)).create(true, TetrominoType.T, ItemType.FILL, 0, 0);
        var queue = new ArrayDeque<>(List.of(bomb, fill, new Block(TetrominoType.S)));
        Game game = new Game(new Board(), queue::removeFirst, GameSettings.DEFAULT, new DeletionScorePolicy());
        game.start();
        var view = new GameView(game, () -> {}, () -> {});
        assertTrue(texts(view).stream().anyMatch(t -> t.getText().equals(PiecePreviewView.formatBlock(fill))));
        game.hold();
        view.refresh();
        assertEquals(ItemType.BOMB, game.getHeldBlock().getItemType());
        assertTrue(texts(view).stream().anyMatch(t -> t.getText().equals(PiecePreviewView.formatBlock(bomb))));
    }

    @Test
    void boardAndPreviewCharactersUseBoldFonts() {
        assertTrue(texts(new ColoredBoardView()).stream().allMatch(t -> t.getFont().getStyle().toLowerCase().contains("bold")));
        var preview = new PiecePreviewView("HOLD");
        assertTrue(((Text)preview.getChildren().get(1)).getFont().getStyle().toLowerCase().contains("bold"));
    }

    private List<Text> texts(Node node) {
        if (node instanceof Text t) return List.of(t);
        if (node instanceof Parent p) return p.getChildrenUnmodifiable().stream().flatMap(n -> texts(n).stream()).toList();
        return List.of();
    }

    @Test void boardRendersGrayGhostItemSymbolWeightAndFlash() {
        var cells = new Board().getVisibleCells();
        cells[19][1] = tetris.board.Cell.WEIGHT;
        cells[19][2] = tetris.board.Cell.T;
        char[][] symbols = new char[20][10];
        for (char[] row : symbols) Arrays.fill(row, '.');
        symbols[19][1] = 'O';
        symbols[19][2] = 'M';
        var view = new ColoredBoardView();
        view.render(cells, symbols, List.of(new tetris.board.Position(19, 0)), Set.of(), false, true);
        var row = (javafx.scene.layout.HBox) view.getChildren().get(20);
        assertEquals(javafx.scene.paint.Color.GRAY, ((Text)row.getChildren().get(1)).getFill());
        assertEquals("O ", ((Text)row.getChildren().get(1)).getText());
        assertEquals(javafx.scene.paint.Color.WHITESMOKE, ((Text)row.getChildren().get(2)).getFill());
        assertEquals("M ", ((Text)row.getChildren().get(3)).getText());
        view.render(cells, symbols, List.of(), Set.of(19), false, true);
        assertEquals(javafx.scene.paint.Color.TRANSPARENT, ((Text)row.getChildren().get(3)).getFill());
        view.render(cells, symbols, List.of(), Set.of(19), true, true);
        assertEquals(javafx.scene.paint.Color.WHITE, ((Text)row.getChildren().get(3)).getFill());
    }
}
