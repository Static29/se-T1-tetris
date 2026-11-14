package tetris.board;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * 대전 모드에서 상대에게 보내는 공격 줄 한 줄. 칸마다 찼는지만 기억하며 블록 종류·아이템은 담지 않는다.
 * 상대 보드에 들어가면 찬 칸은 모두 Cell.GARBAGE가 된다. 값 객체라서 만든 뒤에는 바뀌지 않는다.
 */
public final class GarbageRow {
    private final boolean[] filled;

    /** filled[col]이 true인 칸이 찬 칸. 배열은 복사해서 보관한다. */
    public GarbageRow(boolean[] filled) {
        Objects.requireNonNull(filled, "filled");
        if (filled.length < 1) {
            throw new IllegalArgumentException("공격 줄은 1칸 이상이어야 합니다.");
        }
        this.filled = filled.clone();
    }

    /** holeColumns 칸만 비고 나머지는 모두 찬 columns칸짜리 줄. */
    public static GarbageRow withHoles(int columns, Collection<Integer> holeColumns) {
        Objects.requireNonNull(holeColumns, "holeColumns");
        if (columns < 1) {
            throw new IllegalArgumentException("공격 줄은 1칸 이상이어야 합니다: " + columns);
        }
        boolean[] filled = new boolean[columns];
        Arrays.fill(filled, true);
        for (int col : holeColumns) {
            if (col < 0 || col >= columns) {
                throw new IllegalArgumentException("줄 밖의 빈칸입니다: " + col);
            }
            filled[col] = false;
        }
        return new GarbageRow(filled);
    }

    public int getColumns() { return filled.length; }

    public boolean isFilled(int col) {
        if (col < 0 || col >= filled.length) {
            throw new IndexOutOfBoundsException("줄 밖의 칸입니다: " + col);
        }
        return filled[col];
    }

    /** 보드에 넣을 칸 배열. 찬 칸은 GARBAGE, 나머지는 EMPTY. */
    Cell[] toCells() {
        Cell[] cells = new Cell[filled.length];
        for (int col = 0; col < filled.length; col++) {
            cells[col] = filled[col] ? Cell.GARBAGE : Cell.EMPTY;
        }
        return cells;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GarbageRow row && Arrays.equals(filled, row.filled);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(filled);
    }

    /** 찬 칸은 'G', 빈칸은 '.'. Board.toString과 같은 표기다. */
    @Override
    public String toString() {
        StringBuilder text = new StringBuilder(filled.length);
        for (boolean cell : filled) {
            text.append(cell ? 'G' : '.');
        }
        return text.toString();
    }
}
