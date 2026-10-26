package tetris.games;

import java.nio.file.*;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.persistence.SettingsStore;
import tetris.score.*;
import static org.junit.jupiter.api.Assertions.*;

class SettingsAndLeaderboardTest {
    @TempDir Path directory;
    @Test
    void settingsPersistAcrossInstances() throws IOException {
        Path file = directory.resolve("settings.properties");
        SettingsStore store = new SettingsStore(file);
        assertEquals(GameSettings.DEFAULT, store.load());
        var settings = new GameSettings(Difficulty.HARD, true);
        store.save(settings);
        assertEquals(settings, new SettingsStore(file).load());
    }

    @Test
    void leaderboardPersistsOnlyTopTwentyAndDoesNotDuplicateResult() throws IOException {
        Path file = directory.resolve("scores.properties");
        LeaderboardStore store = new LeaderboardStore(file);
        for (int i = 0; i < 25; i++) store.submit(new LeaderboardEntry(UUID.randomUUID(), "ABC", Difficulty.NORMAL, i * 100));
        var loaded = new LeaderboardStore(file).load();
        assertEquals(20, loaded.size());
        assertEquals(2400, loaded.getFirst().score());
        assertEquals(500, loaded.getLast().score());
        assertEquals(loaded, store.submit(loaded.getFirst()));
    }

    @Test
    void tiesKeepEarlierRecordFirstAndNamesNeedExactlyThreeLetters() throws IOException {
        LeaderboardStore store = new LeaderboardStore(directory.resolve("scores.properties"));
        var first = new LeaderboardEntry(UUID.randomUUID(), "AAA", Difficulty.EASY, 100);
        var second = new LeaderboardEntry(UUID.randomUUID(), "ZZZ", Difficulty.HARD, 100);
        store.submit(first);
        var entries = store.submit(second);
        assertEquals(first, entries.get(0));
        assertEquals(second, entries.get(1));
        for (String name : new String[]{"AB", "ABCD", "abc", "A1B", "가나다"}) {
            assertThrows(IllegalArgumentException.class, () -> new LeaderboardEntry(UUID.randomUUID(), name, Difficulty.NORMAL, 0));
        }
    }

    @Test
    void corruptRecordsAreReportedAndNotOverwritten() throws IOException {
        Path file = directory.resolve("scores.properties");
        Files.writeString(file, "count=1\n0.score=oops\n");
        String before = Files.readString(file);
        LeaderboardStore store = new LeaderboardStore(file);
        assertThrows(IOException.class, store::load);
        assertThrows(IOException.class, () -> store.submit(new LeaderboardEntry(UUID.randomUUID(), "ABC", Difficulty.NORMAL, 100)));
        assertEquals(before, Files.readString(file));
    }

    @Test
    void writeFailureIsReported() throws IOException {
        Path file = directory.resolve("directory-instead-of-file");
        Files.createDirectory(file);
        assertThrows(IOException.class, () -> new SettingsStore(file).save(GameSettings.DEFAULT));
    }
}
