package tetris.games;

import java.nio.file.*;
import java.util.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.persistence.SettingsStore;
import tetris.score.*;
import tetris.ui.*;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.text.Text;
import javafx.scene.input.*;
import static org.junit.jupiter.api.Assertions.*;

class GameModeTest {
    @TempDir Path directory;

    @Test
    void legacySettingsAndRecordsDefaultToClassic() throws IOException {
        Path settings = directory.resolve("settings");
        Files.writeString(settings, "difficulty=HARD\ncolorBlindMode=true\n");
        assertEquals(GameMode.CLASSIC, new SettingsStore(settings).load().mode());
        Path scores = directory.resolve("scores");
        Files.writeString(scores, "count=1\n0.id=" + UUID.randomUUID()
                + "\n0.name=ABC\n0.difficulty=NORMAL\n0.score=600\n");
        var store = new LeaderboardStore(scores);
        assertEquals(GameMode.CLASSIC, store.load().getFirst().mode());
        store.submit(new LeaderboardEntry(UUID.randomUUID(), "XYZ", Difficulty.HARD, 720, GameMode.ITEM));
        assertEquals(2, store.load().size());
    }

    @Test
    void bothModesKeepTwentyIndependentlyAfterReload() throws IOException {
        Path file = directory.resolve("scores");
        var store = new LeaderboardStore(file);
        for (GameMode mode : GameMode.values()) {
            for (int i = 0; i < 25; i++) store.submit(new LeaderboardEntry(
                    UUID.randomUUID(), "ABC", Difficulty.NORMAL, i + (mode == GameMode.ITEM ? 1000 : 0), mode));
        }
        var entries = new LeaderboardStore(file).load();
        assertEquals(40, entries.size());
        for (GameMode mode : GameMode.values()) {
            assertEquals(20, entries.stream().filter(e -> e.mode() == mode).count());
        }
        assertEquals(5, entries.stream().filter(e -> e.mode() == GameMode.CLASSIC).mapToLong(LeaderboardEntry::score).min().orElseThrow());
    }

    @Test
    void settingModePersistsAndGameKeepsItsStartingMode() throws IOException {
        var store = new SettingsStore(directory.resolve("settings"));
        var item = new GameSettings(Difficulty.HARD, true, GameMode.ITEM);
        store.save(item);
        Game game = new Game(store.load());
        game.start();
        store.save(GameSettings.DEFAULT);
        assertEquals(GameMode.ITEM, game.getSettings().mode());
        assertEquals(GameMode.CLASSIC, store.load().mode());
    }

    @Test
    void settingsSelectItemAndMainMenuShowsIt() {
        var saved = new java.util.concurrent.atomic.AtomicReference<GameSettings>();
        var view = new SettingsView(GameSettings.DEFAULT, "", saved::set, () -> { });
        press(view, KeyCode.DOWN);
        press(view, KeyCode.DOWN);
        press(view, KeyCode.RIGHT);
        press(view, KeyCode.DOWN);
        press(view, KeyCode.ENTER);
        assertEquals(GameMode.ITEM, saved.get().mode());
        var menu = new MainMenuView(() -> { }, () -> { }, () -> { }, () -> { });
        menu.setMode(saved.get().mode());
        assertTrue(text(menu).contains("게임 모드: Item"));
    }

    @Test
    void leaderboardTabsNeverMixModes() {
        var entries = List.of(
                new LeaderboardEntry(UUID.randomUUID(), "AAA", Difficulty.NORMAL, 100, GameMode.CLASSIC),
                new LeaderboardEntry(UUID.randomUUID(), "BBB", Difficulty.HARD, 720, GameMode.ITEM));
        var view = new LeaderboardView(entries, null, "", GameMode.CLASSIC, () -> { });
        assertTrue(text(view).contains("AAA"));
        assertFalse(text(view).contains("BBB"));
        press(view, KeyCode.RIGHT);
        assertTrue(text(view).contains("Item"));
        assertTrue(text(view).contains("BBB"));
        assertFalse(text(view).contains("AAA"));
        press(view, KeyCode.LEFT);
        assertTrue(text(view).contains("AAA"));
    }

    private void press(Node node, KeyCode code) {
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }
    private String text(Node node) {
        if (node instanceof Text t) return t.getText();
        if (node instanceof Parent p) return p.getChildrenUnmodifiable().stream().map(this::text).reduce("", (a,b) -> a + b);
        return "";
    }
}
