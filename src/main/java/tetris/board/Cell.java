package tetris.board;

/**
 * 보드 한 칸의 상태. 빈칸이거나, 어떤 종류의 블록이 고정되어 있다.
 * tetris.block에 의존하지 않도록 보드 전용으로 정의하며, 블록 종류와 이름을 맞춘다(Cell.valueOf(type.name())).
 * 무게추는 테트로미노 종류가 없으므로(getType() == null) WEIGHT로 고정한다.
 */
public enum Cell {
    EMPTY,
    I,
    O,
    T,
    S,
    Z,
    J,
    L,
    WEIGHT;

    public boolean isEmpty() {
        return this == EMPTY;
    }
}
