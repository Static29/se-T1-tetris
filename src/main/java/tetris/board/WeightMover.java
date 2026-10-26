package tetris.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import tetris.block.Block;
import tetris.block.ItemType;

/**
 * 무게추 아이템 하나의 이동을 맡는다(Req2 9쪽).
 * 처음에는 좌우로 움직일 수 있지만, 바닥이나 다른 블록에 한 번 닿으면 더 이상 좌우로 움직이지 못한다.
 * 아래로 내려갈 때는 자기 칸과 겹치는 고정 칸을 지우며 보드 바닥까지 내려간다.
 * 지운 칸은 점수를 주지 않고, 그 칸에 점수 배율(M) 표시가 있었어도 배율 없이 사라진다(팀 결정).
 * 회전은 Block이 무게추 모양을 유지하므로 따로 막지 않는다. 바닥에서의 고정 여유는 LockDelay로 처리한다.
 */
public final class WeightMover {
    private final Board board;
    private final Block weight;
    /** 바닥이나 다른 블록에 한 번이라도 닿았는지. 한 번 true가 되면 다시 false가 되지 않는다. */
    private boolean touched;
    private int erasedCellCount;

    public WeightMover(Board board, Block weight) {
        this.board = Objects.requireNonNull(board, "board");
        this.weight = Objects.requireNonNull(weight, "weight");
        if (weight.getItemType() != ItemType.WEIGHT) {
            throw new IllegalArgumentException("무게추 블록만 WeightMover로 움직일 수 있습니다.");
        }
        updateTouched();
    }

    public boolean isTouched() { return touched; }

    /** 내려오며 지운 칸 수. 점수에는 쓰지 않으며 확인·표시용이다. */
    public int getErasedCellCount() { return erasedCellCount; }

    /** 아직 닿지 않았을 때만 dx(-1 또는 1)칸 옆으로 움직인다. 벽이나 고정 블록과 겹치면 움직이지 않는다. */
    public boolean tryMoveSideways(int dx) {
        if (dx != -1 && dx != 1) {
            throw new IllegalArgumentException("좌우 이동은 -1 또는 1칸이어야 합니다: " + dx);
        }
        if (touched || !board.canPlace(Position.fromXY(weight.previewMove(dx, 0)))) {
            return false;
        }
        weight.move(dx, 0);
        updateTouched();
        return true;
    }

    /** 바닥에 닿아 더 내려갈 수 없으면 true. 고정 블록은 지우고 지나가므로 바닥만 기준이다. */
    public boolean isOnFloor() {
        for (Position position : Position.fromXY(weight.previewMove(0, 1))) {
            if (position.row() >= board.getTotalRows()) {
                return true;
            }
        }
        return false;
    }

    /** 한 칸 내려간다. 내려간 자리에 있던 고정 칸은 지운다. 이미 바닥이면 움직이지 않고 false. */
    public boolean stepDown() {
        if (isOnFloor()) {
            touched = true;
            return false;
        }
        List<Position> filled = new ArrayList<>();
        for (Position position : Position.fromXY(weight.previewMove(0, 1))) {
            if (!board.isEmpty(position)) {
                filled.add(position);
            }
        }
        if (!filled.isEmpty()) {
            erasedCellCount += board.removeCells(filled);
        }
        weight.move(0, 1);
        updateTouched();
        return true;
    }

    /** 하드 드롭: 바닥까지 내려가며 칸을 지운다. 내려간 칸 수를 반환한다. */
    public int dropToFloor() {
        int distance = 0;
        while (stepDown()) {
            distance++;
        }
        return distance;
    }

    /** 바로 아래가 바닥이거나 고정 칸이면 닿은 것으로 기록한다. */
    private void updateTouched() {
        if (touched) {
            return;
        }
        for (Position below : Position.fromXY(weight.previewMove(0, 1))) {
            if (below.row() >= board.getTotalRows() || !board.isEmpty(below)) {
                touched = true;
                return;
            }
        }
    }
}
