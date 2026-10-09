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
        // 생성자에서는 재정의될 수 있는 clear() 대신 직접 채운다.
        for (int row = 0; row < cells.length; row++) {
            cells[row] = emptyRow();
        }
    }

    public int getVisibleRows() { return visibleRows; }
    public int getBufferRows() { return bufferRows; }
    public int getTotalRows() { return cells.length; }
    public int getColumns() { return columns; }

    public boolean isInside(int row, int col) {
        return row >= 0 && row < cells.length && col >= 0 && col < columns;
    }

    public boolean isInside(Position position) {
        Objects.requireNonNull(position, "position");
        return isInside(position.row(), position.col());
    }

    /** 버퍼가 아닌, 화면에 보이는 줄인지 확인한다. */
    public boolean isVisibleRow(int row) {
        return row >= bufferRows && row < cells.length;
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
        // 아래에서 위로 올라가며 남길 줄만 아래쪽부터 다시 채운다.
        int targetRow = cells.length - 1;
        for (int row = cells.length - 1; row >= 0; row--) {
            if (!isRowFull(row)) {
                cells[targetRow] = cells[row];
                targetRow--;
            }
        }
        for (int row = targetRow; row >= 0; row--) {
            cells[row] = emptyRow();
        }
        return fullRows;
    }

    /** 모든 칸을 비운다. 새 게임을 시작할 때 사용한다. */
    public void clear() {
        for (int row = 0; row < cells.length; row++) {
            cells[row] = emptyRow();
        }
    }

    /** 독립적인 깊은 복사본. 원본과 복사본은 서로 영향을 주지 않는다. AI 시뮬레이션이나 대전 모드에서 사용한다. */
    public Board copy() {
        Board result = new Board(visibleRows, bufferRows, columns);
        for (int row = 0; row < cells.length; row++) {
            result.cells[row] = cells[row].clone();
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

    private void checkInside(int row, int col) {
        if (!isInside(row, col)) {
            throw new IndexOutOfBoundsException("보드 밖 좌표입니다: row=" + row + ", col=" + col);
        }
    }

    private void checkRow(int row) {
        if (row < 0 || row >= cells.length) {
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
