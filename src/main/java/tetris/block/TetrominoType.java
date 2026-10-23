package tetris.block;

/**
 * 테트로미노 7종과 회전 전 기본 모양.
 * 좌표는 블록 왼쪽 위를 기준으로 하며, x는 오른쪽, y는 아래쪽으로 증가한다.
 * 보드 위의 위치 및 회전 중심은 이 좌표와 별도로 관리한다.
 */
public enum TetrominoType {
    // ####
    I(new int[][] {{0, 0}, {1, 0}, {2, 0}, {3, 0}}, "#00FFFF", "#56B4E9"),

    // ##
    // ##
    O(new int[][] {{0, 0}, {1, 0}, {0, 1}, {1, 1}}, "#FFFF00", "#F0E442"),

    // .#.
    // ###
    T(new int[][] {{1, 0}, {0, 1}, {1, 1}, {2, 1}}, "#EE82EE", "#CC79A7"),

    // .##
    // ##.
    S(new int[][] {{1, 0}, {2, 0}, {0, 1}, {1, 1}}, "#00FF00", "#009E73"),

    // ##.
    // .##
    Z(new int[][] {{0, 0}, {1, 0}, {1, 1}, {2, 1}}, "#FA8072", "#D55E00"),

    // #..
    // ###
    J(new int[][] {{0, 0}, {0, 1}, {1, 1}, {2, 1}}, "#6495ED", "#0072B2"),

    // ..#
    // ###
    L(new int[][] {{2, 0}, {0, 1}, {1, 1}, {2, 1}}, "#FFA500", "#E69F00");

    private final int[][] cells;
    private final String normalColorHex;
    private final String colorBlindColorHex;

    TetrominoType(int[][] cells, String normalColorHex, String colorBlindColorHex) {
        this.cells = cells;
        this.normalColorHex = normalColorHex;
        this.colorBlindColorHex = colorBlindColorHex;
    }

    /** 기존 JavaFX 미리보기와 같은 일반 색상. UI에서 Color.web으로 변환한다. */
    public String getNormalColorHex() { return normalColorHex; }

    /** Okabe-Ito 기반 보조 팔레트. 모든 색각 유형의 구분을 보장하는 것은 아니다. */
    public String getColorBlindColorHex() { return colorBlindColorHex; }

    /** 모드 상태는 저장하지 않고 호출 시 전달받아 색상 값만 선택한다. */
    public String getColorHex(boolean colorBlindMode) {
        return colorBlindMode ? colorBlindColorHex : normalColorHex;
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
