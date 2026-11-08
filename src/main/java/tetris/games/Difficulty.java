package tetris.games;

import tetris.board.FallSpeed;
import tetris.board.PieceSelector;

public enum Difficulty {
    EASY("Easy", 80, PieceSelector.EASY_I_WEIGHT, FallSpeed.EASY_INCREASE_PERCENT),
    NORMAL("Normal", 100, PieceSelector.NORMAL_I_WEIGHT, FallSpeed.NORMAL_INCREASE_PERCENT),
    HARD("Hard", 120, PieceSelector.HARD_I_WEIGHT, FallSpeed.HARD_INCREASE_PERCENT);
    private final String label;
    private final int scorePercent;
    private final int iWeight;
    private final int speedIncreasePercent;
    Difficulty(String label, int scorePercent, int iWeight, int speedIncreasePercent) {
        this.label = label;
        this.scorePercent = scorePercent;
        this.iWeight = iWeight;
        this.speedIncreasePercent = speedIncreasePercent;
    }
    public String getLabel() { return label; }
    public int getScorePercent() { return scorePercent; }
    public int getIWeight() { return iWeight; }
    public int getSpeedIncreasePercent() { return speedIncreasePercent; }
    public long applyScore(long baseScore) { return baseScore * scorePercent / 100; }
}
