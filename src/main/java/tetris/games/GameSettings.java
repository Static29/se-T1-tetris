package tetris.games;

import java.util.Objects;

/** 새 게임에 전달하는 설정 스냅샷. 색상 적용은 렌더링 담당이 연결한다. */
public record GameSettings(Difficulty difficulty, boolean colorBlindMode, GameMode mode) {
    public static final GameSettings DEFAULT = new GameSettings(Difficulty.NORMAL, false);
    public GameSettings(Difficulty difficulty, boolean colorBlindMode) {
        this(difficulty, colorBlindMode, GameMode.CLASSIC);
    }
    public GameSettings {
        Objects.requireNonNull(difficulty);
        Objects.requireNonNull(mode);
    }
}
