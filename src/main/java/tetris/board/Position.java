package tetris.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 보드 위의 절대 좌표. row는 아래쪽, col은 오른쪽으로 증가한다(row 0이 버퍼 맨 위 줄).
 * Block의 {x, y} 좌표는 row = y, col = x로 변환한다. 변환은 fromXY만 사용해서 순서 실수를 막는다.
 */
public record Position(int row, int col) {

    /** {x, y} 좌표를 Position(row = y, col = x)으로 바꾼다. */
    public static Position fromXY(int x, int y) {
        return new Position(y, x);
    }

    /**
     * Block.getBoardCells() 같은 {x, y} 배열 목록을 Position 목록으로 바꾼다.
     * 각 원소는 길이가 2여야 한다. 반환 목록은 수정할 수 없다.
     */
    public static List<Position> fromXY(int[][] xyCells) {
        Objects.requireNonNull(xyCells, "xyCells");
        List<Position> result = new ArrayList<>(xyCells.length);
        for (int[] cell : xyCells) {
            Objects.requireNonNull(cell, "xyCells의 원소");
            if (cell.length != 2) {
                throw new IllegalArgumentException("좌표는 {x, y} 두 값이어야 합니다: 길이 " + cell.length);
            }
            result.add(fromXY(cell[0], cell[1]));
        }
        return List.copyOf(result);
    }

    /** 이 좌표에서 (dRow, dCol)만큼 이동한 새 좌표를 반환한다. */
    public Position offset(int dRow, int dCol) {
        return new Position(row + dRow, col + dCol);
    }
}
