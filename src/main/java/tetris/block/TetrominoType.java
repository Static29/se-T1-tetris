package tetris.block;

/**
 * 테트로미노 7종과 회전 전 기본 모양.
 * 좌표는 블록 왼쪽 위를 기준으로 하며, x는 오른쪽, y는 아래쪽으로 증가한다.
 * 보드 위의 위치 및 회전 중심은 이 좌표와 별도로 관리한다.
 */
public enum TetrominoType {
    // ####
    I(new int[][] {{0, 0}, {1, 0}, {2, 0}, {3, 0}}),

    // ##
    // ##
    O(new int[][] {{0, 0}, {1, 0}, {0, 1}, {1, 1}}),

    // .#.
    // ###
    T(new int[][] {{1, 0}, {0, 1}, {1, 1}, {2, 1}}),

    // .##
    // ##.
    S(new int[][] {{1, 0}, {2, 0}, {0, 1}, {1, 1}}),

    // ##.
    // .##
    Z(new int[][] {{0, 0}, {1, 0}, {1, 1}, {2, 1}}),

    // #..
    // ###
    J(new int[][] {{0, 0}, {0, 1}, {1, 1}, {2, 1}}),

    // ..#
    // ###
    L(new int[][] {{2, 0}, {0, 1}, {1, 1}, {2, 1}});

    private final int[][] cells;

    TetrominoType(int[][] cells) {
        this.cells = cells;
    }

    /**
     * 각 칸의 {x, y} 상대 좌표를 반환한다.
     * 반환한 배열을 수정해도 원래 블록 정의는 바뀌지 않는다.
     */
    public int[][] getCells() {
        int[][] copy = new int[cells.length][];
        for (int i = 0; i < cells.length; i++) {
            copy[i] = cells[i].clone();
        }
        return copy;
    }
}
