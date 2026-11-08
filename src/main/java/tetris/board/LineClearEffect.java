package tetris.board;

import java.util.List;

/**
 * 줄을 지우기 직전의 화면과 지울 줄 번호. BlockLocker가 줄 삭제 연출(깜빡임)용으로 넘긴다.
 *
 * @param visibleCells 지우기 직전 Board.getVisibleCells()의 복사본. 실제 보드와 배열을 공유하지 않는다
 * @param rows         지울 줄의 보드 절대 행 번호(버퍼 포함). 화면 행은 row - bufferRows
 */
public record LineClearEffect(Cell[][] visibleCells, List<Integer> rows) {
    public LineClearEffect {
        visibleCells = copy(visibleCells);
        rows = List.copyOf(rows);
    }

    /** 받은 쪽이 배열을 바꿔도 이 기록이 바뀌지 않도록 매번 복사본을 준다. */
    @Override
    public Cell[][] visibleCells() {
        return copy(visibleCells);
    }

    private static Cell[][] copy(Cell[][] cells) {
        Cell[][] result = new Cell[cells.length][];
        for (int row = 0; row < cells.length; row++) {
            result[row] = cells[row].clone();
        }
        return result;
    }
}
