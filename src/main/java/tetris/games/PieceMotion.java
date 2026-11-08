package tetris.games;

import tetris.block.Block;
import tetris.block.ItemType;
import tetris.board.Board;
import tetris.board.Position;
import tetris.board.WeightMover;

/** 낙하·접지·착지 예측에 같은 이동 규칙을 적용한다. Board API를 재사용한다. */
final class PieceMotion {
    private final Board board;
    private final Block block;
    private final WeightMover weight;

    PieceMotion(Board board, Block block) {
        this.board = board;
        this.block = block;
        weight = block.getItemType() == ItemType.WEIGHT ? new WeightMover(board, block) : null;
    }

    boolean isWeight() { return weight != null; }

    private boolean canDescend() {
        return weight != null ? !weight.isOnFloor()
                : board.canPlace(Position.fromXY(block.previewMove(0, 1)));
    }

    boolean isGrounded() { return !canDescend(); }

    boolean stepDown() {
        if (weight != null) return weight.stepDown();
        if (!canDescend()) return false;
        block.move(0, 1);
        return true;
    }

    boolean moveSideways(int dx) {
        if (weight != null) return weight.tryMoveSideways(dx);
        if (!board.canPlace(Position.fromXY(block.previewMove(dx, 0)))) return false;
        block.move(dx, 0);
        return true;
    }

    Block landing() {
        Block ghost = block.copy();
        // 무게추 예측은 고정 칸을 지우므로 보드까지 복사한다. 일반 블록은 읽기만 한다.
        PieceMotion preview = new PieceMotion(isWeight() ? board.copy() : board, ghost);
        while (preview.stepDown()) { }
        return ghost;
    }
}
