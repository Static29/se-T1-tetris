package tetris.block;

import java.util.Objects;

/**
 * 한 블록의 위치와 현재 모양을 관리한다. x는 오른쪽, y는 아래쪽으로 증가한다.
 * 경계 및 충돌 검사와 이동·회전 허용 여부는 보드에서 처리한다.
 */
public class Block {
    private final TetrominoType type;
    private int x;
    private int y;
    private int[][] cells;

    public Block(TetrominoType type) {
        this(type, 0, 0);
    }

    public Block(TetrominoType type, int x, int y) {
        this.type = Objects.requireNonNull(type, "type");
        this.x = x;
        this.y = y;
        this.cells = type.getCells();
    }

    public TetrominoType getType() { return type; }
    public int getX() { return x; }
    public int getY() { return y; }

    /** 현재 모양의 {x, y} 상대 좌표 복사본을 반환한다. */
    public int[][] getCells() {
        return translated(cells, 0, 0);
    }

    /** 보드상의 {x, y} 절대 좌표를 반환한다. Position으로 변환할 때는 (y, x) 순서다. */
    public int[][] getBoardCells() {
        return translated(cells, x, y);
    }

    /** 상태를 바꾸지 않고 이동 후 보드상의 절대 좌표를 반환한다. */
    public int[][] previewMove(int dx, int dy) {
        return translated(cells, x + dx, y + dy);
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    /** 상태를 바꾸지 않고 회전 후 보드상의 절대 좌표를 반환한다. */
    public int[][] previewRotation() {
        return translated(rotatedCells(), x, y);
    }

    /**
     * 감싸는 사각형의 왼쪽 위 위치를 유지하며 시계 방향으로 90도 회전한다.
     * O는 그대로 유지한다. 고정 회전 중심과 wall kick은 적용하지 않는다.
     */
    public void rotate() {
        cells = rotatedCells();
    }

    public int width() { return extent(0); }
    public int height() { return extent(1); }

    private int extent(int axis) {
        int max = 0;
        for (int[] cell : cells) {
            max = Math.max(max, cell[axis]);
        }
        return max + 1;
    }

    private int[][] rotatedCells() {
        if (type == TetrominoType.O) {
            return getCells();
        }
        int height = height();
        int[][] result = new int[cells.length][2];
        for (int i = 0; i < cells.length; i++) {
            result[i][0] = height - 1 - cells[i][1];
            result[i][1] = cells[i][0];
        }
        return result;
    }

    private static int[][] translated(int[][] source, int dx, int dy) {
        int[][] result = new int[source.length][2];
        for (int i = 0; i < source.length; i++) {
            result[i][0] = source[i][0] + dx;
            result[i][1] = source[i][1] + dy;
        }
        return result;
    }
}
