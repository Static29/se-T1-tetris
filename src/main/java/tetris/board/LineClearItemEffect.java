package tetris.board;

import java.util.List;
import java.util.function.Consumer;

/** L(줄 삭제): L이 있는 줄을 꽉 차지 않았어도 지우고 위 줄을 내린다. 점수 재료는 그 줄의 찬 칸 수와 1줄. */
final class LineClearItemEffect implements ItemEffect {
    @Override
    public Outcome apply(Board board, Position item, Cell cell, Consumer<LineClearEffect> effects) {
        int row = item.row();
        boolean multiplierApplied = board.hasMultiplierInRow(row);
        effects.accept(new LineClearEffect(board.getVisibleCells(), List.of(row)));
        int cellsRemoved = board.clearRow(row);
        return new Outcome(1, cellsRemoved, multiplierApplied);
    }
}
