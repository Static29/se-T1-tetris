package tetris.games;

public enum GameMode {
    CLASSIC("Classic"), ITEM("Item");
    private final String label;
    GameMode(String label) { this.label = label; }
    public String getLabel() { return label; }
}
