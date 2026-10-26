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
    private final ItemType itemType;
    private final int itemCellIndex;
    private int rotationState;

    public Block(TetrominoType type) {
        this(type, 0, 0);
    }

    public Block(TetrominoType type, int x, int y) {
        this(Objects.requireNonNull(type, "type"), x, y, spawnCells(type), null, -1);
    }

    private Block(TetrominoType type, int x, int y, int[][] cells,
                  ItemType itemType, int itemCellIndex) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.cells = translated(cells, 0, 0);
        this.itemType = itemType;
        this.itemCellIndex = itemCellIndex;
    }

    static Block withItem(TetrominoType type, int x, int y, ItemType item, int cellIndex) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(item, "item");
        if (item == ItemType.WEIGHT) throw new IllegalArgumentException("무게추는 별도 모양입니다.");
        int[][] cells = spawnCells(type);
        if (cellIndex < 0 || cellIndex >= cells.length) throw new IllegalArgumentException("잘못된 아이템 칸");
        return new Block(type, x, y, cells, item, cellIndex);
    }

    static Block weight(int x, int y) {
        return new Block(null, x, y,
                new int[][] {{1, 0}, {2, 0}, {0, 1}, {1, 1}, {2, 1}, {3, 1}},
                ItemType.WEIGHT, -1);
    }

    /** 일반/부착형 블록의 종류. 별도 모양인 무게추는 null이며 hasItem/getItemType으로 구분한다. */
    public TetrominoType getType() { return type; }
    public boolean hasItem() { return itemType != null; }
    /** 일반 블록은 null을 반환한다. */
    public ItemType getItemType() { return itemType; }

    /** 부착된 아이템 한 칸의 상대 좌표. 일반 블록과 무게추는 빈 배열을 반환한다. */
    public int[][] getItemCells() {
        if (itemCellIndex < 0) return new int[0][2];
        return new int[][] {cells[itemCellIndex].clone()};
    }

    /** 고정 시 보드/점수 담당자가 효과를 적용할 아이템 칸의 절대 좌표. */
    public int[][] getBoardItemCells() {
        return translated(getItemCells(), x, y);
    }

    /** 상대 위치의 표시 문자. 빈칸은 '.', 일반 칸은 'O', 아이템 칸은 고유 문자. */
    public char getCellSymbol(int localX, int localY) {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i][0] == localX && cells[i][1] == localY) {
                return i == itemCellIndex ? itemType.getSymbol() : 'O';
            }
        }
        return '.';
    }
    public int getX() { return x; }
    public int getY() { return y; }

    /** 위치·회전·아이템 정보를 보존하는 독립 복사본. */
    public Block copy() {
        Block copy = new Block(type, x, y, cells, itemType, itemCellIndex);
        copy.rotationState = rotationState;
        return copy;
    }

    /** 아이템 종류와 부착 칸은 유지하고 생성 위치·방향으로 초기화한 복사본. */
    public Block spawnCopy(int x, int y) {
        if (itemType == ItemType.WEIGHT) return weight(x, y);
        return new Block(type, x, y, spawnCells(type), itemType, itemCellIndex);
    }

    /** 0=생성 방향, 1=R, 2=180도, 3=L. O와 무게추는 0을 유지한다. */
    public int getRotationState() { return rotationState; }

    /** 빈칸을 포함한 고정 회전 공간 크기. x/y는 이 공간의 왼쪽 위다. */
    public int getFrameSize() {
        if (type == TetrominoType.I || itemType == ItemType.WEIGHT) return 4;
        return type == TetrominoType.O ? 2 : 3;
    }

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
        return previewRotation(0);
    }

    /**
     * 시계 방향 SRS 보정 후보의 {dx, dy} 복사본. y는 아래쪽이 양수다.
     * 보드는 순서대로 충돌 검사하고 첫 번째 가능한 후보만 적용해야 한다.
     * O와 무게추는 이동 없는 후보 하나만 제공한다.
     */
    public int[][] getRotationOffsets() {
        if (!canChangeOrientation()) return new int[][] {{0, 0}};
        return SrsRotation.clockwiseOffsets(type, rotationState);
    }

    /** 상태 변경 없이 지정 후보의 회전 후 보드 절대 좌표를 반환한다. */
    public int[][] previewRotation(int candidateIndex) {
        int[] offset = rotationOffset(candidateIndex);
        return translated(rotatedCells(), x + offset[0], y + offset[1]);
    }

    /** 고정 중심으로 시계 방향 90도 회전. 충돌 검사/벽 차기 선택은 수행하지 않는다. */
    public void rotate() {
        rotate(0);
    }

    /** 보드가 검증한 후보를 적용한다. 모든 후보가 막혔다면 호출하지 않는다. */
    public void rotate(int candidateIndex) {
        int[] offset = rotationOffset(candidateIndex);
        cells = rotatedCells();
        x += offset[0];
        y += offset[1];
        if (canChangeOrientation()) rotationState = (rotationState + 1) % 4;
    }

    /** 점유 칸만 감싼 너비/높이. 상대 좌표에는 앞쪽 빈 행/열이 남을 수 있다. */
    public int width() { return extent(0); }
    public int height() { return extent(1); }

    private int extent(int axis) {
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int[] cell : cells) {
            max = Math.max(max, cell[axis]);
            min = Math.min(min, cell[axis]);
        }
        return max - min + 1;
    }

    private int[][] rotatedCells() {
        if (!canChangeOrientation()) {
            return getCells();
        }
        int size = getFrameSize();
        int[][] result = new int[cells.length][2];
        for (int i = 0; i < cells.length; i++) {
            result[i][0] = size - 1 - cells[i][1];
            result[i][1] = cells[i][0];
        }
        return result;
    }

    private boolean canChangeOrientation() {
        return type != TetrominoType.O && itemType != ItemType.WEIGHT;
    }

    private int[] rotationOffset(int candidateIndex) {
        int[][] offsets = getRotationOffsets();
        if (candidateIndex < 0 || candidateIndex >= offsets.length) {
            throw new IllegalArgumentException("회전 후보 범위 밖: " + candidateIndex);
        }
        return offsets[candidateIndex];
    }

    private static int[][] spawnCells(TetrominoType type) {
        // I는 4x4 공간의 두 번째 행에 놓아 중심 (1.5, 1.5)를 유지한다.
        return translated(type.getCells(), 0, type == TetrominoType.I ? 1 : 0);
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
