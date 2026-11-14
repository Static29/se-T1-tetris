package tetris.games;

public enum GameAction {
    LEFT("왼쪽 이동", "A", true), RIGHT("오른쪽 이동", "D", true),
    ROTATE("회전", "W", false), SOFT_DROP("하강", "S", true),
    HARD_DROP("즉시 낙하", "F", false), HOLD("Hold", "G", false),
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
