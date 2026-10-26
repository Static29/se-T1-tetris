package tetris.board;

import java.util.List;

/** 삭제 직전의 화면과 보드 절대 행 번호. 실제 보드와 배열을 공유하지 않는다. */
public record LineClearEffect(Cell[][] visibleCells, List<Integer> rows) {
    public LineClearEffect {
        visibleCells = copy(visibleCells);
        rows = List.copyOf(rows);
    }
    @Override public Cell[][] visibleCells() { return copy(visibleCells); }
    private static Cell[][] copy(Cell[][] cells) {
        Cell[][] result = new Cell[cells.length][];
        for (int i = 0; i < cells.length; i++) result[i] = cells[i].clone();
        return result;
    }
}
