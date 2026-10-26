package tetris.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 고정된 블록만 저장하는 게임판. row는 아래쪽, col은 오른쪽으로 증가한다.
 * 맨 위 bufferRows개 줄은 화면에 보이지 않는 생성 공간이고, 그 아래 visibleRows개 줄이 화면에 보인다.
 * 테트로미노의 모양·회전·움직이는 블록은 알지 않으며, 절대 좌표 목록(Position)만 받아서 판단한다.
 * 게임 루프의 한 스레드에서만 사용한다고 가정하며 동기화하지 않는다.
 */
public class Board {
    public static final int DEFAULT_VISIBLE_ROWS = 20;
    public static final int DEFAULT_BUFFER_ROWS = 2;
    public static final int DEFAULT_COLUMNS = 10;

    private final int visibleRows;
    private final int bufferRows;
    private final int columns;
    private final Cell[][] cells;
    /**
     * 점수 배율(M) 아이템이 붙은 칸 표시. cells와 같은 [row][col] 모양이며,
     * 줄이 지워져 내려갈 때 cells와 같은 줄 배열을 함께 옮겨 칸과 표시가 어긋나지 않게 한다.
     */
    private final boolean[][] multiplierMarks;

    /** 보이는 20줄, 숨겨진 버퍼 2줄, 10칸짜리 기본 보드를 만든다. */
    public Board() {
        this(DEFAULT_VISIBLE_ROWS, DEFAULT_BUFFER_ROWS, DEFAULT_COLUMNS);
    }

    /** 테스트나 다른 모드를 위해 크기를 지정한다. visibleRows·columns는 1 이상, bufferRows는 0 이상이어야 한다. */
    public Board(int visibleRows, int bufferRows, int columns) {
        if (visibleRows < 1 || bufferRows < 0 || columns < 1) {
            throw new IllegalArgumentException("보드 크기가 올바르지 않습니다: visibleRows=" + visibleRows
                    + ", bufferRows=" + bufferRows + ", columns=" + columns);
        }
        this.visibleRows = visibleRows;
        this.bufferRows = bufferRows;
        this.columns = columns;
        this.cells = new Cell[bufferRows + visibleRows][];
        this.multiplierMarks = new boolean[bufferRows + visibleRows][];
        // 생성자에서는 재정의될 수 있는 clear() 대신 private 메서드로 채운다.
        fillEmptyRows(0, cells.length);
    }

    public int getVisibleRows() { return visibleRows; }
    public int getBufferRows() { return bufferRows; }
    public int getTotalRows() { return cells.length; }
    public int getColumns() { return columns; }

    public boolean isInside(int row, int col) {
        return isRowInside(row) && col >= 0 && col < columns;
    }

    public boolean isInside(Position position) {
        Objects.requireNonNull(position, "position");
        return isInside(position.row(), position.col());
    }

    /** 버퍼가 아닌, 화면에 보이는 줄인지 확인한다. */
    public boolean isVisibleRow(int row) {
        return isRowInside(row) && row >= bufferRows;
    }

    /** 보드 밖 좌표는 IndexOutOfBoundsException을 던진다. 벽·바닥 판정에는 isEmpty를 사용한다. */
    public Cell getCell(int row, int col) {
        checkInside(row, col);
        return cells[row][col];
    }

    public Cell getCell(Position position) {
        Objects.requireNonNull(position, "position");
        return getCell(position.row(), position.col());
    }

    /** 보드 밖 좌표는 벽·바닥으로 보고 false를 반환한다. */
    public boolean isEmpty(int row, int col) {
        return isInside(row, col) && cells[row][col].isEmpty();
    }

    public boolean isEmpty(Position position) {
        Objects.requireNonNull(position, "position");
        return isEmpty(position.row(), position.col());
    }

    /** 모든 좌표가 보드 안의 빈칸이면 true. 이동·회전·생성 전에 미리 계산한 좌표로 호출한다. */
    public boolean canPlace(Collection<Position> positions) {
        checkPositions(positions);
        for (Position position : positions) {
            if (!isEmpty(position)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 블록을 보드에 고정한다. 놓을 수 없는 좌표가 하나라도 있으면 아무 칸도 바꾸지 않고 예외를 던진다.
     * 줄 삭제는 하지 않으므로 고정 후 clearFullRows를 따로 호출한다.
     */
    public void lock(Collection<Position> positions, Cell cell) {
        Objects.requireNonNull(cell, "cell");
        if (cell.isEmpty()) {
            throw new IllegalArgumentException("Cell.EMPTY는 고정할 수 없습니다.");
        }
        if (!canPlace(positions)) {
            throw new IllegalArgumentException("보드 밖이거나 이미 찬 칸에는 고정할 수 없습니다: " + positions);
        }
        for (Position position : positions) {
            cells[position.row()][position.col()] = cell;
        }
    }

    /** Block out: 새 블록을 생성 위치에 놓을 수 없으면 게임 오버. */
    public boolean isBlockOut(Collection<Position> spawnPositions) {
        return !canPlace(spawnPositions);
    }

    /** Lock out: 고정된 블록의 모든 칸이 숨겨진 버퍼 안에 있으면 게임 오버. 고정 직후, 줄 삭제 전에 호출한다. */
    public boolean isLockOut(Collection<Position> lockedPositions) {
        checkPositions(lockedPositions);
        for (Position position : lockedPositions) {
            if (position.row() >= bufferRows) {
                return false;
            }
        }
        return true;
    }

    public boolean isRowFull(int row) {
        checkRow(row);
        for (Cell cell : cells[row]) {
            if (cell.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 가득 찬 줄 번호를 위에서 아래 순서로 반환한다. 보드는 바꾸지 않는다. */
    public List<Integer> findFullRows() {
        List<Integer> fullRows = new ArrayList<>();
        for (int row = 0; row < cells.length; row++) {
            if (isRowFull(row)) {
                fullRows.add(row);
            }
        }
        return List.copyOf(fullRows);
    }

    /**
     * 가득 찬 줄을 모두 지우고 위쪽 줄을 지운 줄 수만큼 내린다(naive gravity).
     * 지운 줄의 원래 번호를 위에서 아래 순서로 반환한다. 반환 크기가 이번에 지운 줄 수다.
     */
    public List<Integer> clearFullRows() {
        List<Integer> fullRows = findFullRows();
        if (fullRows.isEmpty()) {
            return fullRows;
        }
        // 아래에서 위로 올라가며 남길 줄만 아래쪽부터 다시 채운다. 가득 찬 줄은 위에서 이미 찾았다.
        int targetRow = cells.length - 1;
        for (int row = cells.length - 1; row >= 0; row--) {
            if (!fullRows.contains(row)) {
                moveRow(row, targetRow);
                targetRow--;
            }
        }
        // 내려오고 남은 맨 위 0 ~ targetRow번 줄을 빈 줄로 채운다.
        fillEmptyRows(0, targetRow + 1);
        return fullRows;
    }

    /**
     * 가득 차지 않았어도 row 한 줄을 지우고 그 위 줄들을 한 칸씩 내린다(줄 삭제 아이템용).
     * 지운 줄에 있던 찬 칸 수를 반환한다.
     */
    public int clearRow(int row) {
        checkRow(row);
        int removedCells = countFilledCells(row);
        for (int r = row; r > 0; r--) {
            moveRow(r - 1, r);
        }
        fillEmptyRows(0, 1);
        return removedCells;
    }

    /**
     * 주어진 칸들을 빈칸으로 만든다. 위 칸을 내리지 않는다(폭탄·무게추용).
     * 이미 빈칸인 좌표는 건너뛰며, 실제로 지운 칸 수를 반환한다. 보드 밖 좌표가 있으면 아무것도 지우지 않고 예외를 던진다.
     */
    public int removeCells(Collection<Position> positions) {
        checkPositions(positions);
        for (Position position : positions) {
            if (!isInside(position)) {
                throw new IllegalArgumentException("보드 밖 좌표는 지울 수 없습니다: " + position);
            }
        }
        int removed = 0;
        for (Position position : positions) {
            if (!cells[position.row()][position.col()].isEmpty()) {
                cells[position.row()][position.col()] = Cell.EMPTY;
                multiplierMarks[position.row()][position.col()] = false;
                removed++;
            }
        }
        return removed;
    }

    /** 이미 고정된 칸에 점수 배율(M) 표시를 붙인다. 빈칸이나 보드 밖이면 예외를 던진다. */
    public void markMultiplier(Position position) {
        Objects.requireNonNull(position, "position");
        // isEmpty는 보드 밖을 벽(false)으로 보므로 보드 밖 검사를 따로 한다.
        if (!isInside(position) || isEmpty(position)) {
            throw new IllegalArgumentException("빈칸이나 보드 밖에는 배율 표시를 붙일 수 없습니다: " + position);
        }
        multiplierMarks[position.row()][position.col()] = true;
    }

    /** 점수 배율(M) 표시가 있는 칸인지. 보드 밖은 false. */
    public boolean isMultiplier(int row, int col) {
        return isInside(row, col) && multiplierMarks[row][col];
    }

    public boolean isMultiplier(Position position) {
        Objects.requireNonNull(position, "position");
        return isMultiplier(position.row(), position.col());
    }

    /** row 줄에 점수 배율(M) 표시가 하나라도 있는지. */
    public boolean hasMultiplierInRow(int row) {
        checkRow(row);
        for (boolean mark : multiplierMarks[row]) {
            if (mark) {
                return true;
            }
        }
        return false;
    }

    /** 모든 칸을 비운다. 새 게임을 시작할 때 사용한다. */
    public void clear() {
        fillEmptyRows(0, cells.length);
    }

    /** 독립적인 깊은 복사본. 원본과 복사본은 서로 영향을 주지 않는다. AI 시뮬레이션이나 대전 모드에서 사용한다. */
    public Board copy() {
        Board result = new Board(visibleRows, bufferRows, columns);
        for (int row = 0; row < cells.length; row++) {
            result.cells[row] = cells[row].clone();
            result.multiplierMarks[row] = multiplierMarks[row].clone();
        }
        return result;
    }

    /**
     * 화면에 보이는 줄만 [row][col] 복사본으로 반환한다. 반환 배열의 0번 줄은 보드의 bufferRows번 줄이다.
     * 반환한 배열을 수정해도 보드는 바뀌지 않는다. BoardView.render에 전달할 수 있다.
     */
    public Cell[][] getVisibleCells() {
        Cell[][] result = new Cell[visibleRows][];
        for (int row = 0; row < visibleRows; row++) {
            result[row] = cells[bufferRows + row].clone();
        }
        return result;
    }

    /** 디버깅용. 버퍼를 포함한 모든 줄을 빈칸 '.'과 블록 글자로 표시하고, 버퍼와 보이는 영역 사이에 '-' 줄을 넣는다. */
    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < cells.length; row++) {
            if (row == bufferRows && bufferRows > 0) {
                text.append("-".repeat(columns)).append('\n');
            }
            for (Cell cell : cells[row]) {
                text.append(cell.isEmpty() ? '.' : cell.name().charAt(0));
            }
            text.append('\n');
        }
        return text.toString();
    }

    private Cell[] emptyRow() {
        Cell[] row = new Cell[columns];
        Arrays.fill(row, Cell.EMPTY);
        return row;
    }

    /** fromRow 이상 toRowExclusive 미만 줄을 각각 새 빈 줄로 바꾼다. 줄마다 다른 배열을 쓴다. */
    private void fillEmptyRows(int fromRow, int toRowExclusive) {
        for (int row = fromRow; row < toRowExclusive; row++) {
            cells[row] = emptyRow();
            multiplierMarks[row] = new boolean[columns];
        }
    }

    /** fromRow 줄을 toRow 자리로 옮긴다. 칸과 배율 표시를 항상 함께 옮긴다. */
    private void moveRow(int fromRow, int toRow) {
        cells[toRow] = cells[fromRow];
        multiplierMarks[toRow] = multiplierMarks[fromRow];
    }

    private int countFilledCells(int row) {
        int count = 0;
        for (Cell cell : cells[row]) {
            if (!cell.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** 버퍼를 포함한 보드 줄 범위 안인지. isInside·isVisibleRow·checkRow가 같은 기준을 쓰게 한다. */
    private boolean isRowInside(int row) {
        return row >= 0 && row < cells.length;
    }

    /** isInside의 예외 버전. 배열 예외 대신 row·col을 함께 담은 메시지를 준다. */
    private void checkInside(int row, int col) {
        if (!isInside(row, col)) {
            throw new IndexOutOfBoundsException("보드 밖 좌표입니다: row=" + row + ", col=" + col);
        }
    }

    /** isRowInside의 예외 버전. 줄 번호만 받는 메서드(isRowFull)에서 사용한다. */
    private void checkRow(int row) {
        if (!isRowInside(row)) {
            throw new IndexOutOfBoundsException("보드 밖 줄입니다: row=" + row);
        }
    }

    /** 비어 있거나, null 좌표가 있거나, 같은 좌표가 두 번 들어 있으면 블록 좌표로 볼 수 없다. */
    private static void checkPositions(Collection<Position> positions) {
        Objects.requireNonNull(positions, "positions");
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("좌표 목록이 비어 있습니다.");
        }
        Set<Position> seen = new HashSet<>();
        for (Position position : positions) {
            Objects.requireNonNull(position, "positions의 원소");
            if (!seen.add(position)) {
                throw new IllegalArgumentException("같은 좌표가 중복되었습니다: " + position);
            }
        }
    }
}
