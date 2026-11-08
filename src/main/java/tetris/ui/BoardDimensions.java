package tetris.ui;

import tetris.board.Board;
import tetris.board.Cell;

/** 문자·색상 보드가 공유하는 표시 크기와 스냅샷 검증. */
record BoardDimensions(int rows, int columns) {
    static final BoardDimensions DEFAULT = new BoardDimensions(Board.DEFAULT_VISIBLE_ROWS, Board.DEFAULT_COLUMNS);

    BoardDimensions {
        if (rows <= 0 || columns <= 0) throw new IllegalArgumentException("보드 크기는 양수여야 합니다.");
    }

    String border() { return "X".repeat(columns * 2 + 3); }

    void validate(Cell[][] cells) {
        if (cells == null || cells.length != rows) throw new IllegalArgumentException("보드 행 수 불일치");
        for (Cell[] row : cells) {
            if (row == null || row.length != columns) throw new IllegalArgumentException("보드 열 수 불일치");
            for (Cell cell : row) if (cell == null)
                throw new IllegalArgumentException("빈칸은 Cell.EMPTY로 전달해야 합니다.");
        }
    }

    void validate(char[][] symbols) {
        if (symbols == null) return;
        if (symbols.length != rows) throw new IllegalArgumentException("문자 행 수 불일치");
        for (char[] row : symbols) if (row == null || row.length != columns)
            throw new IllegalArgumentException("문자 열 수 불일치");
    }
}
