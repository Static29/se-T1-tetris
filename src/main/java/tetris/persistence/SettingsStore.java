package tetris.persistence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import tetris.games.Difficulty;
import tetris.games.GameSettings;
import tetris.games.GameMode;
import tetris.games.ScreenSize;
import tetris.games.KeyBindings;
import tetris.games.GameAction;
import java.util.EnumMap;

public final class SettingsStore {
    private final Path path;
    public SettingsStore(Path path) { this.path = path; }
    public GameSettings load() throws IOException {
        Properties values = PropertyFiles.read(path);
        try {
            String mode = values.getProperty("colorBlindMode", "false");
            if (!mode.equals("true") && !mode.equals("false")) throw new IllegalArgumentException();
            var keys = new EnumMap<GameAction, String>(GameAction.class);
            for (GameAction action : GameAction.values())
                keys.put(action, values.getProperty("key." + action.name(), action.getDefaultKey()));
            return new GameSettings(Difficulty.valueOf(values.getProperty("difficulty", "NORMAL")),
                    Boolean.parseBoolean(mode), GameMode.valueOf(values.getProperty("mode", "CLASSIC")),
                    ScreenSize.valueOf(values.getProperty("screenSize", "DEFAULT")), new KeyBindings(keys));
        } catch (IllegalArgumentException e) { throw new IOException("설정 파일 형식 오류", e); }
    }
    public void save(GameSettings settings) throws IOException {
        Properties values = new Properties();
        values.setProperty("difficulty", settings.difficulty().name());
        values.setProperty("mode", settings.mode().name());
        values.setProperty("colorBlindMode", Boolean.toString(settings.colorBlindMode()));
        values.setProperty("screenSize", settings.screenSize().name());
        for (GameAction action : GameAction.values())
            values.setProperty("key." + action.name(), settings.keyBindings().key(action));
        PropertyFiles.write(path, values);
    }
}
