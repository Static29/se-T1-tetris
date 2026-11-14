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
        return key != null && (key.matches("[A-Z]") || key.matches("DIGIT[0-9]") || key.matches("NUMPAD[0-9]")
                || Set.of("LEFT", "RIGHT", "UP", "DOWN", "SPACE", "ESCAPE",
                        "TAB", "BACK_SPACE", "DELETE", "HOME", "END", "PAGE_UP", "PAGE_DOWN").contains(key));
    }
    public static KeyBindings defaults() {
        Map<GameAction, String> keys = new EnumMap<>(GameAction.class);
        for (GameAction action : GameAction.values()) keys.put(action, action.getDefaultKey());
        return new KeyBindings(keys);
    }
    public static KeyBindings playerTwoDefaults() {
        var keys = new EnumMap<GameAction, String>(GameAction.class);
        String[] values = {"LEFT", "RIGHT", "UP", "DOWN", "NUMPAD1", "NUMPAD2", "ESCAPE"};
        for (GameAction action : GameAction.values()) keys.put(action, values[action.ordinal()]);
        return new KeyBindings(keys);
    }
    /** 기존 1P 사용자 설정을 보존하고, 겹치는 2P 초기 키만 사용하지 않는 키로 배정한다. */
    public static KeyBindings compatiblePlayerTwo(KeyBindings first) {
        var used = new HashSet<String>(); used.add("ESCAPE");
        for (GameAction action : GameAction.values()) if (action != GameAction.PAUSE) used.add(first.key(action));
        var keys = new EnumMap<GameAction, String>(GameAction.class);
        var defaults = playerTwoDefaults();
        for (GameAction action : GameAction.values()) {
            if (action == GameAction.PAUSE) { keys.put(action, "ESCAPE"); continue; }
            String key = defaults.key(action);
            if (used.contains(key)) {
                for (char candidate = 'A'; candidate <= 'Z'; candidate++)
                    if (!used.contains(String.valueOf(candidate))) { key = String.valueOf(candidate); break; }
            }
            keys.put(action, key); used.add(key);
        }
        return new KeyBindings(keys);
    }
    public static void validatePlayers(KeyBindings first, KeyBindings second) {
        var used = new HashSet<String>();
        for (KeyBindings bindings : List.of(first, second)) for (GameAction action : GameAction.values())
            if (action != GameAction.PAUSE && (bindings.key(action).equals("ESCAPE") || !used.add(bindings.key(action))))
                throw new IllegalArgumentException("1P·2P 조작 키가 겹치거나 Esc를 사용합니다.");
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
