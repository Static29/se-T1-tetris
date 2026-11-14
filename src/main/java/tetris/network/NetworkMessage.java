package tetris.network;

import java.util.List;
import java.util.Objects;

/** 게임 객체와 분리된 불변 전송 메시지. 필드 순서는 README의 protocol v1을 따른다. */
public record NetworkMessage(String type, List<String> fields) {
    public NetworkMessage {
        Objects.requireNonNull(type);
        if (!type.matches("[A-Z_]{1,32}")) throw new IllegalArgumentException("Invalid type");
        fields = List.copyOf(fields);
        if (fields.size() > 16) throw new IllegalArgumentException("Too many fields");
    }
    public static NetworkMessage of(String type, String... fields) {
        return new NetworkMessage(type, List.of(fields));
    }
}
