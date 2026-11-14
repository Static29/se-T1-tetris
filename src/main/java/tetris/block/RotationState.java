package tetris.block;

/** 블록 내부의 시계 방향 회전 상태. 외부 API는 기존 정수 코드를 유지한다. */
enum RotationState {
    SPAWN(0), RIGHT(1), REVERSE(2), LEFT(3);

    private final int code;

    RotationState(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }

    RotationState clockwise() {
        return switch (this) {
            case SPAWN -> RIGHT;
            case RIGHT -> REVERSE;
            case REVERSE -> LEFT;
            case LEFT -> SPAWN;
        };
    }
}
