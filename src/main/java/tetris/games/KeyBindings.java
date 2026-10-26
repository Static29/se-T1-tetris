package tetris.games;

import java.util.*;

/** JavaFX에 의존하지 않는 키 이름 매핑. 수정 시 새 스냅샷을 만든다. */
public record KeyBindings(Map<GameAction, String> keys) {
    public KeyBindings {
        keys = Map.copyOf(keys);
        if (keys.size() != GameAction.values().length) throw new IllegalArgumentException("모든 조작 키가 필요합니다.");
        Set<String> seen = new HashSet<>();
        for (GameAction action : GameAction.values()) {
            String key = keys.get(action);
            if (key == null || !isSupported(key) || !seen.add(key))
                throw new IllegalArgumentException("지원하지 않거나 중복된 키입니다.");
        }
    }
    public static boolean isSupported(String key) {
        return key != null && (key.matches("[A-Z]") || key.matches("DIGIT[0-9]")
                || Set.of("LEFT", "RIGHT", "UP", "DOWN", "SPACE", "ESCAPE",
                        "TAB", "BACK_SPACE", "DELETE", "HOME", "END", "PAGE_UP", "PAGE_DOWN").contains(key));
    }
    public static KeyBindings defaults() {
        Map<GameAction, String> keys = new EnumMap<>(GameAction.class);
        for (GameAction action : GameAction.values()) keys.put(action, action.getDefaultKey());
        return new KeyBindings(keys);
    }
    public String key(GameAction action) { return keys.get(action); }
    public Optional<GameAction> actionFor(String key) {
        return keys.entrySet().stream().filter(e -> e.getValue().equals(key)).map(Map.Entry::getKey).findFirst();
    }
    public KeyBindings with(GameAction action, String key) {
        Map<GameAction, String> copy = new EnumMap<>(keys);
        copy.put(action, key);
        return new KeyBindings(copy);
    }
}
