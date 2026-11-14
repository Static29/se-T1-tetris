package tetris.games;

public enum BattleMode {
    NORMAL("일반 대전", GameMode.CLASSIC), ITEM("아이템 대전", GameMode.ITEM),
    TIMED("시간제한 대전", GameMode.CLASSIC);
    private final String label;
    private final GameMode gameMode;
    BattleMode(String label, GameMode gameMode) { this.label = label; this.gameMode = gameMode; }
    public GameSettings settings(GameSettings base) {
        return new GameSettings(base.difficulty(), base.colorBlindMode(), gameMode,
                base.screenSize(), base.keyBindings(), base.playerTwoBindings());
    }
    @Override public String toString() { return label; }
}
