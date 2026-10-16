package tetris.score;

import java.util.Objects;
import java.util.UUID;
import tetris.games.Difficulty;
import tetris.games.GameMode;

/** 로그인 계정과 분리된 플레이 기록 ID를 유지한다. */
public record LeaderboardEntry(UUID id, String name, Difficulty difficulty, long score, GameMode mode) {
    public LeaderboardEntry(UUID id, String name, Difficulty difficulty, long score) {
        this(id, name, difficulty, score, GameMode.CLASSIC);
    }
    public LeaderboardEntry {
        Objects.requireNonNull(id);
        Objects.requireNonNull(difficulty);
        Objects.requireNonNull(mode);
        if (name == null || !name.matches("[A-Z]{3}")) throw new IllegalArgumentException("이름은 영문 대문자 3글자입니다.");
        if (score < 0) throw new IllegalArgumentException("점수는 음수일 수 없습니다.");
    }
}
