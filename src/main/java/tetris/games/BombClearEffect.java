package tetris.games;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import tetris.board.Cell;
import tetris.board.Position;

/** 폭발 전후 차이로 실제 삭제 칸만 표시한다. 실제 보드와 점수에는 영향을 주지 않는다. */
final class BombClearEffect {
    private final GameBoardSnapshot before;
    private final List<Position> removed = new ArrayList<>();

    BombClearEffect(GameBoardSnapshot before, Cell[][] after) {
        this.before = before;
        var cells = before.cells();
        for (int r = 0; r < cells.length; r++) for (int c = 0; c < cells[r].length; c++)
            if (!cells[r][c].isEmpty() && after[r][c].isEmpty()) removed.add(new Position(r, c));
    }
    boolean isEmpty() { return removed.isEmpty(); }
    GameBoardSnapshot snapshot(boolean flashOn) {
        var cells = before.cells(); var symbols = before.symbols();
        for (var p : removed) {
            // 기존 전송 포맷과 UI를 그대로 사용하면서 밝은 칸 / 빈칸으로 교대한다.
            cells[p.row()][p.col()] = flashOn ? Cell.WEIGHT : Cell.EMPTY;
            if (!flashOn) symbols[p.row()][p.col()] = '.';
        }
        return new GameBoardSnapshot(cells, symbols, List.of(), Set.of(), flashOn);
    }
}
