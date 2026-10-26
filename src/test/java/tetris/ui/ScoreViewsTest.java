package tetris.ui;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.*;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.*;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;
import tetris.games.*;
import tetris.board.*;
import tetris.score.LeaderboardEntry;
import static org.junit.jupiter.api.Assertions.*;
import static tetris.block.TetrominoType.T;

class ScoreViewsTest {
    @Test
    void initialsWrapAndSubmitExactlyOnce() {
        AtomicReference<String> name = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        var view = new InitialsEntryView(600, Difficulty.NORMAL, value -> {
            name.set(value); calls.incrementAndGet();
        }, () -> { });
        press(view, KeyCode.DOWN); // A -> Z
        press(view, KeyCode.RIGHT);
        press(view, KeyCode.UP); // A -> B
        press(view, KeyCode.RIGHT);
        press(view, KeyCode.UP);
        press(view, KeyCode.UP); // A -> C
        press(view, KeyCode.ENTER);
        press(view, KeyCode.ENTER);
        assertEquals("ZBC", name.get());
        assertEquals(1, calls.get());
    }

    @Test
    void saveFailureAllowsRetryAndSkipDoesNotSubmit() {
        AtomicInteger attempts = new AtomicInteger();
        var view = new InitialsEntryView(0, Difficulty.EASY, name -> {
            if (attempts.incrementAndGet() == 1) throw new IOException();
        }, () -> { });
        press(view, KeyCode.ENTER);
        assertTrue(text(view).contains("저장 실패"));
        press(view, KeyCode.ENTER);
        assertEquals(2, attempts.get());
        AtomicInteger skipped = new AtomicInteger();
        var skip = new InitialsEntryView(0, Difficulty.EASY, name -> fail(), skipped::incrementAndGet);
        press(skip, KeyCode.ESCAPE);
        assertEquals(1, skipped.get());
    }

    @Test
    void settingsChangeDraftThenSaveOrCancel() {
        AtomicReference<GameSettings> saved = new AtomicReference<>();
        AtomicInteger backs = new AtomicInteger();
        var view = new SettingsView(GameSettings.DEFAULT, "", saved::set, backs::incrementAndGet);
        press(view, KeyCode.RIGHT); // hard
        press(view, KeyCode.DOWN);
        press(view, KeyCode.ENTER); // color blind on
        assertNull(saved.get());
        press(view, KeyCode.DOWN);
        press(view, KeyCode.DOWN);
        press(view, KeyCode.DOWN);
        press(view, KeyCode.DOWN);
        press(view, KeyCode.ENTER);
        assertEquals(new GameSettings(Difficulty.HARD, true), saved.get());
        assertEquals(1, backs.get());
        saved.set(null);
        var cancel = new SettingsView(GameSettings.DEFAULT, "", saved::set, backs::incrementAndGet);
        press(cancel, KeyCode.RIGHT);
        press(cancel, KeyCode.ESCAPE);
        assertNull(saved.get());
    }

    @Test
    void gameOverOpensOneNameWindowAndShowsDifficultyAndScore() {
        Board board = new Board();
        board.lock(List.of(new Position(0, 4)), Cell.J);
        Game game = new Game(() -> T, board, new GameSettings(Difficulty.HARD, false), new tetris.score.DeletionScorePolicy());
        game.start();
        AtomicInteger saved = new AtomicInteger();
        GameView view = new GameView(game, () -> { }, () -> { }, name -> saved.incrementAndGet(), () -> { }, null);
        view.refresh();
        view.refresh();
        assertEquals(1, view.getChildren().stream().filter(n -> n instanceof InitialsEntryView).count());
        assertTrue(text(view).contains("난이도: Hard"));
        assertTrue(text(view).contains("점수: 0"));
        press(view, KeyCode.ENTER);
        assertEquals(1, saved.get());
    }

    @Test
    void leaderboardShowsColumnsAndHighlightsNewResult() {
        var entry = new LeaderboardEntry(UUID.randomUUID(), "ABC", Difficulty.HARD, 720);
        String result = LeaderboardView.format(List.of(entry), entry.id());
        assertTrue(result.contains("등수  이름  난이도"));
        assertTrue(result.contains(">  1    ABC"));
        assertTrue(result.contains("Hard"));
        assertTrue(result.contains("720"));
    }

    private void press(Node node, KeyCode code) {
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }
    private String text(Node node) {
        if (node instanceof Text t) return t.getText();
        if (node instanceof Parent p) return p.getChildrenUnmodifiable().stream().map(this::text).reduce("", (a, b) -> a + "\n" + b);
        return "";
    }
}
