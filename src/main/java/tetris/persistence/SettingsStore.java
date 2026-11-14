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
            // 예전 기본 방향키 설정만 새 1P 기본값으로 이동한다. 사용자 지정 키는 보존한다.
            String[] legacy = {"LEFT", "RIGHT", "UP", "DOWN", "SPACE", "C", "ESCAPE"};
            boolean oldDefaults = !values.containsKey("keyBindingsVersion");
            for (GameAction action : GameAction.values()) oldDefaults &= legacy[action.ordinal()].equals(keys.get(action));
            KeyBindings first = oldDefaults ? KeyBindings.defaults() : new KeyBindings(keys);
            KeyBindings fallback = KeyBindings.compatiblePlayerTwo(first);
            var second = new EnumMap<GameAction, String>(GameAction.class);
            for (GameAction action : GameAction.values())
                second.put(action, values.getProperty("key2." + action.name(), fallback.key(action)));
            return new GameSettings(Difficulty.valueOf(values.getProperty("difficulty", "NORMAL")),
                    Boolean.parseBoolean(mode), GameMode.valueOf(values.getProperty("mode", "CLASSIC")),
                    ScreenSize.valueOf(values.getProperty("screenSize", "DEFAULT")), first, new KeyBindings(second));
        } catch (IllegalArgumentException e) { throw new IOException("설정 파일 형식 오류", e); }
    }
    public void save(GameSettings settings) throws IOException {
        Properties values = new Properties();
        values.setProperty("difficulty", settings.difficulty().name());
        values.setProperty("mode", settings.mode().name());
        values.setProperty("colorBlindMode", Boolean.toString(settings.colorBlindMode()));
        values.setProperty("screenSize", settings.screenSize().name());
        values.setProperty("keyBindingsVersion", "2");
        for (GameAction action : GameAction.values()) {
            values.setProperty("key." + action.name(), settings.keyBindings().key(action));
            values.setProperty("key2." + action.name(), settings.playerTwoBindings().key(action));
        }
        PropertyFiles.write(path, values);
    }
}
