package tetris.persistence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import tetris.games.Difficulty;
import tetris.games.GameSettings;
import tetris.games.GameMode;

public final class SettingsStore {
    private final Path path;
    public SettingsStore(Path path) { this.path = path; }
    public GameSettings load() throws IOException {
        Properties values = PropertyFiles.read(path);
        try {
            String mode = values.getProperty("colorBlindMode", "false");
            if (!mode.equals("true") && !mode.equals("false")) throw new IllegalArgumentException();
            return new GameSettings(Difficulty.valueOf(values.getProperty("difficulty", "NORMAL")),
                    Boolean.parseBoolean(mode), GameMode.valueOf(values.getProperty("mode", "CLASSIC")));
        } catch (IllegalArgumentException e) { throw new IOException("설정 파일 형식 오류", e); }
    }
    public void save(GameSettings settings) throws IOException {
        Properties values = new Properties();
        values.setProperty("difficulty", settings.difficulty().name());
        values.setProperty("mode", settings.mode().name());
        values.setProperty("colorBlindMode", Boolean.toString(settings.colorBlindMode()));
        PropertyFiles.write(path, values);
    }
}
