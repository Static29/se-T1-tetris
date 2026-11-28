package tetris.ai;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import tetris.block.Block;
import tetris.block.ItemType;
import tetris.board.Board;
import tetris.board.BlockLocker;
import tetris.board.BlockRotation;
import tetris.board.BlockSpawner;
import tetris.board.Position;
import tetris.games.GameAction;

/**
 * 컴퓨터 플레이어의 판단 함수(Req3 도전 과제): 현재 보드 상태를 받아 다음에 누를 키 하나를 돌려준다.
 *
 * <p>방법: 현재 블록을 회전 0~3번, 왼쪽·오른쪽 끝까지 옮겨 놓을 수 있는 모든 자리에 떨어뜨려 보고,
 * BoardEvaluator 점수가 가장 높은 자리를 고른다. Hold 블록(없으면 Next)이 더 좋으면 Hold를 누른다.
 * 그 자리로 가는 첫 키(회전 → 좌우 이동 → 즉시 낙하 순)만 돌려주고, 다음 호출에서 다시 계산한다.
 * 블록이 내려오거나 공격 줄이 올라와도 매번 지금 상태로 다시 고르므로 계획이 어긋나지 않는다.
 *
 * <p>아이템 효과는 계산하지 않고 일반 블록처럼 놓는다. 무게추는 옮기지 않고 바로 떨어뜨린다.
 */
public final class ComputerPlayer {
    /** Hold로 바꾸려면 이만큼 더 좋아야 한다. 점수가 거의 같을 때 Hold를 반복해 시간을 버리지 않게 한다. */
    static final double HOLD_MARGIN = 0.5;
    private static final int MAX_ROTATIONS = 4;

    /** 현재 보드 상태를 보고 다음에 누를 키. 지금 조작할 수 없으면 빈 값. */
    public Optional<GameAction> nextAction(AiInput input) {
        Objects.requireNonNull(input, "input");
        Block current = input.current();
        if (current == null) {
            return Optional.empty();
        }
        if (current.getItemType() == ItemType.WEIGHT) {
            return Optional.of(GameAction.HARD_DROP);
        }
        Board board = input.board();
        Plan best = bestPlan(board, current);
        if (input.canHold() && shouldHold(board, input, best)) {
            return Optional.of(GameAction.HOLD);
        }
        return Optional.of(best == null ? GameAction.HARD_DROP : best.firstAction());
    }

    /** Hold하면 나올 블록(Hold가 비었으면 Next)을 생성 위치에 놓고 계산해 더 좋으면 true. */
    private boolean shouldHold(Board board, AiInput input, Plan best) {
        Block other = input.held() != null ? input.held() : input.next();
        if (other == null || other.getItemType() == ItemType.WEIGHT) {
            return false;
        }
        Block spawned = other.spawnCopy((board.getColumns() - other.getFrameSize()) / 2, BlockSpawner.SPAWN_Y);
        if (!board.canPlace(cells(spawned))) {
            return false;
        }
        Plan alternative = bestPlan(board, spawned);
        if (alternative == null) {
            return false;
        }
        return best == null || alternative.score() > best.score() + HOLD_MARGIN;
    }

    /** start에서 갈 수 있는 모든 자리 중 가장 좋은 계획. 놓을 자리가 없으면 null. */
    Plan bestPlan(Board board, Block start) {
        Plan best = null;
        Set<Integer> seenStates = new HashSet<>();
        Block rotated = start.copy();
        for (int rotations = 0; rotations < MAX_ROTATIONS; rotations++) {
            if (rotations > 0 && !BlockRotation.tryRotate(board, rotated)) {
                break;
            }
            if (!seenStates.add(rotated.getRotationState())) {
                continue; // O처럼 회전해도 모양이 같으면 다시 계산하지 않는다.
            }
            best = better(best, evaluate(board, rotated, rotations, 0));
            for (int direction : new int[] {-1, 1}) {
                Block moved = rotated.copy();
                int steps = 0;
                while (board.canPlace(Position.fromXY(moved.previewMove(direction, 0)))) {
                    moved.move(direction, 0);
                    steps++;
                    best = better(best, evaluate(board, moved, rotations, direction * steps));
                }
            }
        }
        return best;
    }

    /** block을 지금 칸에서 바로 떨어뜨려 고정한 결과의 점수. */
    private static Plan evaluate(Board board, Block block, int rotations, int shift) {
        Block dropped = block.copy();
        while (board.canPlace(Position.fromXY(dropped.previewMove(0, 1)))) {
            dropped.move(0, 1);
        }
        Board simulated = board.copy();
        simulated.lock(cells(dropped), BlockLocker.cellOf(dropped));
        int lines = simulated.clearFullRows().size();
        return new Plan(rotations, shift, BoardEvaluator.score(simulated, lines));
    }

    /** 점수가 높은 쪽. 같으면 키를 덜 누르는 쪽. */
    private static Plan better(Plan current, Plan candidate) {
        if (current == null || candidate.score() > current.score()) {
            return candidate;
        }
        if (candidate.score() == current.score() && candidate.keyCount() < current.keyCount()) {
            return candidate;
        }
        return current;
    }

    private static List<Position> cells(Block block) {
        return Position.fromXY(block.getBoardCells());
    }

    /** 목표 자리: 회전 횟수, 좌우 이동 칸 수(음수 = 왼쪽), 그 자리의 점수. */
    record Plan(int rotations, int shift, double score) {
        GameAction firstAction() {
            if (rotations > 0) return GameAction.ROTATE;
            if (shift < 0) return GameAction.LEFT;
            if (shift > 0) return GameAction.RIGHT;
            return GameAction.HARD_DROP;
        }

        int keyCount() { return rotations + Math.abs(shift); }
    }
}
