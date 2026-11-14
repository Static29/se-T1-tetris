package tetris.games;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.persistence.SettingsStore;
import static org.junit.jupiter.api.Assertions.*;

class PlayerKeySettingsTest {
    @TempDir Path directory;
    @Test void separateBindingsPersistAndBattleModesPreserveThem() throws Exception {
        var first = KeyBindings.defaults().with(GameAction.LEFT, "H");
        var second = KeyBindings.playerTwoDefaults().with(GameAction.HOLD, "NUMPAD3");
        var settings = new GameSettings(Difficulty.NORMAL, false, GameMode.CLASSIC, ScreenSize.DEFAULT, first, second);
        var store = new SettingsStore(directory.resolve("settings.properties")); store.save(settings);
        assertEquals(settings, store.load());
        for (var mode : BattleMode.values()) {
            assertEquals(first, mode.settings(settings).keyBindings());
            assertEquals(second, mode.settings(settings).playerTwoBindings());
        }
        assertThrows(IllegalArgumentException.class, () -> new GameSettings(Difficulty.NORMAL, false,
                GameMode.CLASSIC, ScreenSize.DEFAULT, first, second.with(GameAction.LEFT, "H")));
    }
    @Test void legacyDefaultsMigrateButCustomFirstPlayerKeysRemain() throws Exception {
        Path path = directory.resolve("legacy.properties");
        String original = "key.LEFT=LEFT\nkey.RIGHT=RIGHT\nkey.ROTATE=UP\nkey.SOFT_DROP=DOWN\nkey.HARD_DROP=SPACE\nkey.HOLD=C\nkey.PAUSE=ESCAPE\n";
        Files.writeString(path, original);
        var store = new SettingsStore(path);
        assertEquals(KeyBindings.defaults(), store.load().keyBindings());
        assertEquals(KeyBindings.playerTwoDefaults(), store.load().playerTwoBindings());
        Files.writeString(path, original.replace("key.HOLD=C", "key.HOLD=H"));
        var custom = store.load();
        assertEquals("LEFT", custom.keyBindings().key(GameAction.LEFT));
        assertEquals("H", custom.keyBindings().key(GameAction.HOLD));
        assertDoesNotThrow(() -> KeyBindings.validatePlayers(custom.keyBindings(), custom.playerTwoBindings()));
        store.save(custom); assertEquals(custom, store.load());
    }
}
