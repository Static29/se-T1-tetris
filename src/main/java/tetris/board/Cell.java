package tetris.board;


public enum Cell {
    EMPTY,
    I,
    O,
    T,
    S,
    Z,
    J,
    L;

    public boolean isEmpty() {
        return this == EMPTY;
    }
}
