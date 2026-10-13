package tetris.block;

/** 아이템 효과 적용은 보드/게임 담당자가 수행한다. */
public enum ItemType {
    /** 고정 시 아이템이 있는 행 삭제. */
    LINE_CLEAR('L'),
    /** 너비 4칸의 별도 모양. 아래 칸 제거 및 최초 접촉 후 좌우 제한은 보드에서 처리. */
    WEIGHT('O'),
    /** 고정 시 바로 아래부터 바닥까지 같은 열의 빈칸 채우기. */
    FILL('F'),
    /** 고정 시 중심 3x3 범위에서 채워진 칸 약 4개를 무작위 제거. 부족할 때 규칙은 미정. */
    BOMB('B'),
    /** 해당 행이 삭제될 때 그 행 점수만 2배. */
    MULTIPLIER('M');

    private final char symbol;

    ItemType(char symbol) { this.symbol = symbol; }
    public char getSymbol() { return symbol; }
    public int getScoreMultiplier() { return this == MULTIPLIER ? 2 : 1; }
}
