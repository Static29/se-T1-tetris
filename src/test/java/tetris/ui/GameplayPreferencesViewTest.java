package tetris.ui;

import java.util.*;
import java.util.concurrent.atomic.*;
import javafx.event.Event;
import javafx.scene.*;
import javafx.scene.input.*;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import tetris.games.*;
import tetris.board.Cell;
import tetris.block.TetrominoType;
import tetris.score.LeaderboardEntry;
import static org.junit.jupiter.api.Assertions.*;

class GameplayPreferencesViewTest {
    @Test void graphicalPreferenceSavesAndOldFilesDefaultToText(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        var store = new tetris.persistence.SettingsStore(directory.resolve("settings.properties"));
        assertFalse(store.load().graphical());
        var saved = new AtomicReference<GameSettings>();
        var settings = new SettingsView(GameSettings.DEFAULT, "", saved::set, () -> {});
        for (int i = 0; i < 4; i++) press(settings, KeyCode.DOWN);
        press(settings, KeyCode.RIGHT); press(settings, KeyCode.UP); press(settings, KeyCode.ENTER);
        assertTrue(saved.get().graphical()); store.save(saved.get()); assertEquals(saved.get(), store.load());
        assertTrue(BattleMode.ITEM.settings(saved.get()).graphical());
        var reset = new SettingsView(saved.get(), "", store::save, () -> {});
        press(reset, KeyCode.UP); press(reset, KeyCode.ENTER);
        assertEquals(GameSettings.DEFAULT, store.load());
    }
    @Test void resetsAllSettingsAndSavesKeysWithoutLeavingKeyPage() {
        var initial = new GameSettings(Difficulty.HARD, true, GameMode.ITEM, ScreenSize.SMALL,
                KeyBindings.defaults().with(GameAction.HOLD, "H"));
        var saved = new AtomicReference<GameSettings>();
        var view = new SettingsView(initial, "", saved::set, () -> {});
        press(view, KeyCode.UP); press(view, KeyCode.ENTER);
        assertEquals(GameSettings.DEFAULT, saved.get());
        press(view, KeyCode.UP); press(view, KeyCode.UP); press(view, KeyCode.UP); press(view, KeyCode.ENTER);
        press(view, KeyCode.ENTER); press(view, KeyCode.Q);
        press(view, KeyCode.UP); press(view, KeyCode.ENTER);
        assertEquals("Q", saved.get().keyBindings().key(GameAction.LEFT));
        assertTrue(view.getChildren().getFirst() instanceof KeyBindingsView);
        assertTrue(text(view).contains("키 설정을 저장했습니다"));
    }

    @Test void landingPreviewKeepsItemSymbolsGrayThroughFrameEncoding() {
        var factory = new tetris.block.ItemBlockFactory(new Random(1));
        var game = new Game(new tetris.board.Board(),
                () -> factory.create(true, TetrominoType.T, tetris.block.ItemType.BOMB, 0, 0),
                new GameSettings(Difficulty.NORMAL, false, GameMode.ITEM), new tetris.score.DeletionScorePolicy());
        game.start(); game.softDrop(); game.rotateClockwise();
        var snapshot = BattleFrame.decode(BattleFrame.of(game, "PLAYER1").encode()).board();
        var item = snapshot.ghostCells().stream().filter(p -> snapshot.symbols()[p.row()][p.col()] == 'B').findFirst().orElseThrow();
        var view = new ColoredBoardView();
        view.render(snapshot.cells(), snapshot.symbols(), snapshot.ghostCells(), Set.of(), false, false);
        var row = (javafx.scene.layout.HBox) view.getChildren().get(item.row() + 1);
        var glyph = (Text) row.getChildren().get(item.col() + 1);
        assertEquals("B ", glyph.getText()); assertEquals(Color.GRAY, glyph.getFill());
        assertTrue(snapshot.cells()[item.row()][item.col()].isEmpty());
    }
    @Test
    void boardAndBothPreviewPalettesUseBlockApiAndOnlyOSymbols() {
        Cell[][] cells = new Cell[20][10];
        for (Cell[] row : cells) Arrays.fill(row, Cell.EMPTY);
        var view = new ColoredBoardView();
        for (TetrominoType type : TetrominoType.values()) {
            cells[0][0] = Cell.valueOf(type.name());
            for (boolean mode : new boolean[]{false, true}) {
                view.render(cells, mode);
                Text occupied = texts(view).stream().filter(t -> t.getText().equals("O ")).findFirst().orElseThrow();
                assertEquals(Color.web(type.getColorHex(mode)), occupied.getFill());
                var preview = new PiecePreviewView("NEXT");
                preview.showPiece(type, mode);
                Text shape = (Text) preview.getChildren().get(1);
                assertEquals(Color.web(type.getColorHex(mode)), shape.getFill());
                assertEquals(4, shape.getText().chars().filter(c -> c == 'O').count());
            }
        }
    }

    @Test
    void customKeysReplaceDefaultsAndSettingsApplyWhilePaused() {
        Game game = new Game(() -> TetrominoType.T);
        game.start();
        var view = new GameView(game, () -> { }, () -> { });
        var keys = KeyBindings.defaults().with(GameAction.HOLD, "H").with(GameAction.PAUSE, "P");
        view.applyPreferences(new GameSettings(Difficulty.HARD, true, GameMode.ITEM, ScreenSize.SMALL, keys));
        press(view, KeyCode.C);
        assertNull(game.getHeldType());
        press(view, KeyCode.H);
        assertNotNull(game.getHeldType());
        assertTrue(view.suppressRepeat(KeyCode.H));
        assertFalse(view.suppressRepeat(KeyCode.DOWN));
        press(view, KeyCode.P);
        assertEquals(GameState.PAUSED, game.getState());
        assertEquals(Difficulty.NORMAL, game.getDifficulty());
        assertEquals(GameMode.CLASSIC, game.getSettings().mode());
        press(view, KeyCode.ESCAPE);
        assertEquals(GameState.RUNNING, game.getState());
    }

    @Test
    void bindingCaptureRejectsDuplicateThenAcceptsNewKeyAndRestoresDefaults() {
        AtomicReference<KeyBindings> saved = new AtomicReference<>();
        var view = new KeyBindingsView(KeyBindings.defaults(), saved::set);
        press(view, KeyCode.ENTER);
        press(view, KeyCode.RIGHT);
        assertTrue(text(view).contains("이미 사용"));
        press(view, KeyCode.A);
        press(view, KeyCode.ESCAPE);
        assertEquals("A", saved.get().key(GameAction.LEFT));
        for (int i = 0; i < 7; i++) press(view, KeyCode.DOWN);
        press(view, KeyCode.ENTER);
        press(view, KeyCode.ESCAPE);
        assertEquals(KeyBindings.defaults(), saved.get());
    }

    @Test
    void resetRequiresConfirmationAndEscapeCancels() {
        var entry = new LeaderboardEntry(UUID.randomUUID(), "AAA", Difficulty.NORMAL, 100);
        AtomicInteger calls = new AtomicInteger();
        AtomicBoolean all = new AtomicBoolean();
        var view = new LeaderboardView(List.of(entry), null, "", GameMode.CLASSIC, () -> { }, (mode, resetAll) -> {
            calls.incrementAndGet(); all.set(resetAll); return List.of();
        });
        press(view, KeyCode.DELETE);
        assertEquals(0, calls.get());
        press(view, KeyCode.ESCAPE);
        assertEquals(0, calls.get());
        assertTrue(text(view).contains("AAA"));
        press(view, KeyCode.R);
        press(view, KeyCode.ENTER);
        assertEquals(1, calls.get());
        assertTrue(all.get());
        assertFalse(text(view).contains("AAA"));
    }

    @Test
    void settingsScreenCyclesAllWindowModesWithoutSavingDraft() {
        AtomicReference<GameSettings> saved = new AtomicReference<>();
        var view = new SettingsView(GameSettings.DEFAULT, "", saved::set, () -> { });
        press(view, KeyCode.DOWN);
        press(view, KeyCode.RIGHT);
        assertTrue(text(view).contains("작은 화면"));
        press(view, KeyCode.RIGHT);
        assertTrue(text(view).contains("전체 화면"));
        assertNull(saved.get());
        press(view, KeyCode.DOWN);
        press(view, KeyCode.DOWN);
        press(view, KeyCode.ENTER);
        assertEquals(ScreenSize.FULLSCREEN, saved.get().screenSize());
    }

    private void press(Node node, KeyCode code) {
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }
    private List<Text> texts(Node node) {
        if (node instanceof Text text) return List.of(text);
        if (node instanceof Parent p) return p.getChildrenUnmodifiable().stream().flatMap(n -> texts(n).stream()).toList();
        return List.of();
    }
    private String text(Node node) { return texts(node).stream().map(Text::getText).reduce("", (a,b) -> a + b); }
}
