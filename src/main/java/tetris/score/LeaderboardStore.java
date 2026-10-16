package tetris.score;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import tetris.games.Difficulty;
import tetris.games.GameMode;
import tetris.persistence.PropertyFiles;

public final class LeaderboardStore {
    public static final int LIMIT = 20;
    private final Path path;
    public LeaderboardStore(Path path) { this.path = path; }
    public List<LeaderboardEntry> load() throws IOException {
        Properties values = PropertyFiles.read(path);
        List<LeaderboardEntry> entries = new ArrayList<>();
        try {
            int count = Integer.parseInt(values.getProperty("count", "0"));
            if (count < 0 || count > LIMIT * GameMode.values().length) throw new IllegalArgumentException();
            Set<UUID> ids = new HashSet<>();
            for (int i = 0; i < count; i++) {
                String key = i + ".";
                var entry = new LeaderboardEntry(UUID.fromString(values.getProperty(key + "id")),
                        values.getProperty(key + "name"), Difficulty.valueOf(values.getProperty(key + "difficulty")),
                        Long.parseLong(values.getProperty(key + "score")),
                        GameMode.valueOf(values.getProperty(key + "mode", "CLASSIC")));
                if (!ids.add(entry.id())) throw new IllegalArgumentException();
                entries.add(entry);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IOException("리더보드 파일 형식 오류", e);
        }
        entries.sort(Comparator.comparingLong(LeaderboardEntry::score).reversed());
        return List.copyOf(entries);
    }
    /** 동점은 먼저 등록된 기록 우선. 같은 플레이 ID 재전송은 중복 저장하지 않는다. */
    public List<LeaderboardEntry> submit(LeaderboardEntry entry) throws IOException {
        List<LeaderboardEntry> entries = new ArrayList<>(load());
        if (entries.stream().anyMatch(e -> e.id().equals(entry.id()))) return List.copyOf(entries);
        entries.add(entry);
        entries.sort(Comparator.comparingLong(LeaderboardEntry::score).reversed());
        List<LeaderboardEntry> retained = new ArrayList<>();
        for (GameMode mode : GameMode.values()) {
            entries.stream().filter(e -> e.mode() == mode).limit(LIMIT).forEach(retained::add);
        }
        entries = retained;
        Properties values = new Properties();
        values.setProperty("count", Integer.toString(entries.size()));
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            String key = i + ".";
            values.setProperty(key + "id", e.id().toString());
            values.setProperty(key + "name", e.name());
            values.setProperty(key + "difficulty", e.difficulty().name());
            values.setProperty(key + "score", Long.toString(e.score()));
            values.setProperty(key + "mode", e.mode().name());
        }
        PropertyFiles.write(path, values);
        return List.copyOf(entries);
    }
}
