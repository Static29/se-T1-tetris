package tetris.block;

/** 시계 방향 전이 0->R, R->2, 2->L, L->0 순서. 화면 좌표(y 아래쪽 양수). */
final class SrsRotation {
    private SrsRotation() { }

    // 원래 회전 위치를 포함해 각 전이당 5후보. 각 후보는 원래 위치 기준이며 누적하지 않는다.
    private static final int[][][] JLSTZ = {
        {{0, 0}, {-1, 0}, {-1, -1}, {0, 2}, {-1, 2}},
        {{0, 0}, {1, 0}, {1, 1}, {0, -2}, {1, -2}},
        {{0, 0}, {1, 0}, {1, -1}, {0, 2}, {1, 2}},
        {{0, 0}, {-1, 0}, {-1, 1}, {0, -2}, {-1, -2}}
    };

    private static final int[][][] I = {
        {{0, 0}, {-2, 0}, {1, 0}, {-2, 1}, {1, -2}},
        {{0, 0}, {-1, 0}, {2, 0}, {-1, -2}, {2, 1}},
        {{0, 0}, {2, 0}, {-1, 0}, {2, -1}, {-1, 2}},
        {{0, 0}, {1, 0}, {-2, 0}, {1, 2}, {-2, -1}}
    };

    static int[][] clockwiseOffsets(TetrominoType type, int state) {
        int[][] source = (type == TetrominoType.I ? I : JLSTZ)[state];
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) copy[i] = source[i].clone();
        return copy;
    }
}
