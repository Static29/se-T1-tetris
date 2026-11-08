package tetris.games;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import tetris.block.Block;
import tetris.board.*;

/** 버퍼를 포함한 보드 좌표를 화면 좌표로 변환하는 단일 경계. */
final class BoardProjection {
    private final Board board;

    BoardProjection(Board board) { this.board = board; }
    private int screenRow(int boardRow) { return boardRow - board.getBufferRows(); }
    private int boardRow(int screenRow) { return screenRow + board.getBufferRows(); }

    Set<Integer> flashingRows(LineClearEffect effect) {
        return effect == null ? Set.of() : effect.rows().stream().filter(board::isVisibleRow)
                .map(this::screenRow).collect(Collectors.toUnmodifiableSet());
    }

    List<Position> ghostCells(Block landing) {
        return landing == null ? List.of() : Position.fromXY(landing.getBoardCells()).stream()
                .filter(p -> board.isVisibleRow(p.row()))
                .map(p -> new Position(screenRow(p.row()), p.col())).toList();
    }

    GameBoardSnapshot snapshot(Block active, Block landing, LineClearEffect effect, boolean flashOn) {
        Cell[][] cells = effect == null ? board.getVisibleCells() : effect.visibleCells();
        char[][] symbols = new char[cells.length][board.getColumns()];
        for (int row = 0; row < cells.length; row++) for (int col = 0; col < cells[row].length; col++) {
            symbols[row][col] = cells[row][col].isEmpty() ? '.'
                    : effect == null && board.isMultiplier(boardRow(row), col) ? 'M' : 'O';
        }
        if (active != null && effect == null) {
            Cell type = BlockLocker.cellOf(active);
            for (int[] local : active.getCells()) {
                int row = active.getY() + local[1], col = active.getX() + local[0];
                if (board.isVisibleRow(row)) {
                    cells[screenRow(row)][col] = type;
                    symbols[screenRow(row)][col] = active.getCellSymbol(local[0], local[1]);
                }
            }
        }
        return new GameBoardSnapshot(cells, symbols, effect == null ? ghostCells(landing) : List.of(),
                flashingRows(effect), flashOn);
    }
}
