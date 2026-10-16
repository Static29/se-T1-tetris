package tetris.games;

public enum Difficulty {
    EASY("Easy", 80), NORMAL("Normal", 100), HARD("Hard", 120);
    private final String label;
    private final int scorePercent;
    Difficulty(String label, int scorePercent) {
        this.label = label;
        this.scorePercent = scorePercent;
    }
    public String getLabel() { return label; }
    public long applyScore(long baseScore) { return baseScore * scorePercent / 100; }
}
