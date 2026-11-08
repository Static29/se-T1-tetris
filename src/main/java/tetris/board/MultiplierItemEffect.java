package tetris.board;

import java.util.function.Consumer;

/**
 * M(점수 배율): 고정된 M 칸에 배율 표시를 남긴다. 이 칸이 나중에 줄 삭제·L·B로 사라지면
 * 그 고정의 점수 전체가 2배가 된다(판정은 각 삭제 경로가 board.isMultiplier로 한다).
 */
final class MultiplierItemEffect implements ItemEffect {
    @Override
    public Outcome apply(Board board, Position item, Cell cell, Consumer<LineClearEffect> effects) {
        board.markMultiplier(item);
        return Outcome.NONE;
    }
}
