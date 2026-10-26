package tetris.games;

public enum GameAction {
    LEFT("왼쪽 이동", "LEFT", true), RIGHT("오른쪽 이동", "RIGHT", true),
    ROTATE("회전", "UP", false), SOFT_DROP("하강", "DOWN", true),
    HARD_DROP("즉시 낙하", "SPACE", false), HOLD("Hold", "C", false),
    PAUSE("일시정지", "ESCAPE", false);
    private final String label;
    private final String defaultKey;
    private final boolean repeatable;
    GameAction(String label, String defaultKey, boolean repeatable) {
        this.label = label; this.defaultKey = defaultKey; this.repeatable = repeatable;
    }
    public String getLabel() { return label; }
    public String getDefaultKey() { return defaultKey; }
    public boolean isRepeatable() { return repeatable; }
}
