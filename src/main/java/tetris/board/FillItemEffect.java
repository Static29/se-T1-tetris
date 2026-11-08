package tetris.board;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * F(빈칸 채우기): F 바로 아래부터 바닥까지 같은 열의 빈칸을 그 블록 종류로 채운다. 위쪽은 채우지 않는다.
 * 자체 점수는 없고, 채워서 줄이 차면 BlockLocker의 꽉 찬 줄 삭제에서 일반 줄 점수가 된다.
 */
final class FillItemEffect implements ItemEffect {
    @Override
    public Outcome apply(Board board, Position item, Cell cell, Consumer<LineClearEffect> effects) {
        List<Position> emptyBelow = new ArrayList<>();
        for (int row = item.row() + 1; row < board.getTotalRows(); row++) {
            if (board.isEmpty(row, item.col())) {
                emptyBelow.add(new Position(row, item.col()));
            }
        }
        if (!emptyBelow.isEmpty()) {
            board.lock(emptyBelow, cell);
        }
        return Outcome.NONE;
    }
}
