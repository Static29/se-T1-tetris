package tetris.board;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import tetris.block.Block;

/**
 * 떨어지는 블록의 시계 방향 SRS 회전을 보드에 맞춰 적용한다.
 * 회전 모양과 보정 후보(벽 차기)는 Block이 제공하고, 여기서는 후보를 0번부터 보드에 놓아 보며 처음 가능한 후보만 적용한다.
 * Board가 tetris.block에 의존하지 않도록 Block과 Board를 잇는 코드는 이 클래스에만 둔다.
 */
public final class BlockRotation {

    private BlockRotation() {
    }

    /**
     * 보드에 놓을 수 있는 첫 회전 후보 번호를 반환한다. 모두 막혔으면 빈 OptionalInt.
     * 블록과 보드는 바꾸지 않는다. 블록의 실제 점유 칸만 검사하고 회전 공간의 빈칸은 검사하지 않는다.
     */
    public static OptionalInt findRotation(Board board, Block block) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(block, "block");
        int candidateCount = block.getRotationOffsets().length;
        for (int candidate = 0; candidate < candidateCount; candidate++) {
            List<Position> rotated = Position.fromXY(block.previewRotation(candidate));
            if (board.canPlace(rotated)) {
                return OptionalInt.of(candidate);
            }
        }
        return OptionalInt.empty();
    }

    /** 회전에 성공하면 true. 모든 후보가 막혔으면 블록을 그대로 두고 false를 반환한다. */
    public static boolean tryRotate(Board board, Block block) {
        OptionalInt candidate = findRotation(board, block);
        if (candidate.isEmpty()) {
            return false;
        }
        block.rotate(candidate.getAsInt());
        return true;
    }
}
