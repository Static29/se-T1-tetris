package tetris.games;

import java.util.List;
import java.util.Set;
import tetris.board.Cell;
import tetris.board.Position;

/** 한 프레임의 화면 데이터. 배열은 Game/Board와 공유하지 않고 접근 시에도 복사한다. */
public record GameBoardSnapshot(Cell[][] cells, char[][] symbols, List<Position> ghostCells,
                                Set<Integer> flashingRows, boolean flashOn) {
    public GameBoardSnapshot {
        cells = copy(cells);
        symbols = copy(symbols);
        ghostCells = List.copyOf(ghostCells);
        flashingRows = Set.copyOf(flashingRows);
    }

    @Override public Cell[][] cells() { return copy(cells); }
    @Override public char[][] symbols() { return copy(symbols); }

    private static Cell[][] copy(Cell[][] source) {
        Cell[][] result = source.clone();
        for (int i = 0; i < result.length; i++) result[i] = result[i].clone();
        return result;
    }
    private static char[][] copy(char[][] source) {
        char[][] result = source.clone();
        for (int i = 0; i < result.length; i++) result[i] = result[i].clone();
        return result;
    }
}
