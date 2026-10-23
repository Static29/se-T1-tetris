package tetris.persistence;

import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

public final class PropertyFiles {
    private PropertyFiles() { }
    public static Properties read(Path path) throws IOException {
        Properties values = new Properties();
        if (Files.notExists(path)) return values;
        try (var reader = Files.newBufferedReader(path)) { values.load(reader); }
        catch (IllegalArgumentException e) { throw new IOException("저장 파일 형식 오류", e); }
        return values;
    }
    public static void write(Path path, Properties values) throws IOException {
        Path target = path.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), "tetris-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temp)) { values.store(writer, "Tetris"); }
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }
}
