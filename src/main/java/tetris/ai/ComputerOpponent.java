package tetris.ai;

import java.util.Objects;
import java.util.Optional;
import tetris.games.BattleMatch;
import tetris.games.GameAction;

/**
 * 대전의 한 자리를 컴퓨터가 맡는다. 난이도 간격마다 ComputerPlayer에게 키를 물어 BattleMatch에 누른다.
 * 사람이 키보드로 누르는 것과 같은 경로(BattleMatch.act)를 쓰므로 게임 규칙은 그대로 적용된다.
 *
 * <p>화면 연결: 대전 화면의 타이머가 프레임마다 지난 시간을 advanceTime으로 넘기면 된다.
 * 그 자리의 키보드 입력은 화면에서 막는다.
 */
public final class ComputerOpponent {
    private final BattleMatch match;
    private final int player;
    private final AiDifficulty difficulty;
    private final ComputerPlayer brain = new ComputerPlayer();
    private long waited;

    /** player: 컴퓨터가 맡을 자리 번호(0부터). */
    public ComputerOpponent(BattleMatch match, int player, AiDifficulty difficulty) {
        this.match = Objects.requireNonNull(match, "match");
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
        if (player < 0 || player >= match.games().size()) {
            throw new IllegalArgumentException("없는 자리입니다: " + player);
        }
        this.player = player;
    }

    /**
     * millis만큼 시간이 지났다. 간격이 다 찼으면 키 하나를 누르고 그 키를 돌려준다.
     * 조작할 수 없는 순간(줄 삭제 연출 등)에는 기다렸다가 가능해지면 바로 누른다.
     */
    public Optional<GameAction> advanceTime(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("시간은 음수일 수 없습니다.");
        }
        if (match.finished()) {
            return Optional.empty();
        }
        waited += millis;
        if (waited < difficulty.intervalMillis()) {
            return Optional.empty();
        }
        Optional<GameAction> action = brain.nextAction(AiInput.of(match.games().get(player)));
        action.ifPresent(key -> {
            match.act(player, key);
            waited = 0;
        });
        return action;
    }

    public int player() { return player; }

    public AiDifficulty difficulty() { return difficulty; }
}
