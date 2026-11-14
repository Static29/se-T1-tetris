package tetris.network;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;

/** 저장 위치는 호출자가 지정한다. 기존 설정 파일과 섞지 않는다. 접속 성공 후 save를 호출한다. */
public final class RecentServerStore {
    private final Path file;
    public RecentServerStore(Path file) { this.file = file.toAbsolutePath(); }
    public Optional<String> load() throws IOException {
        if (!Files.exists(file)) return Optional.empty();
        String address = Files.readString(file, StandardCharsets.UTF_8).strip();
        return address.isEmpty() ? Optional.empty() : Optional.of(address);
    }
    public void save(String address) throws IOException {
        if (address == null || address.isBlank() || address.length() > 253
                || address.chars().anyMatch(Character::isWhitespace)) throw new IllegalArgumentException("Invalid address");
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "recent-server-", ".tmp");
        try {
            Files.writeString(temporary, address, StandardCharsets.UTF_8);
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
