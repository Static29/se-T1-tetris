package tetris.board;


public record Position(int row, int col) {

    /** 이 좌표에서 (dRow, dCol)만큼 이동한 새 좌표를 반환한다. */
    public Position offset(int dRow, int dCol) {
        return new Position(row + dRow, col + dCol);
    }
}
