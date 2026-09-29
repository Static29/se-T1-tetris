package tetris.games;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import tetris.block.TetrominoType;

public class Game {
    private final Supplier<TetrominoType> pieceSource;
    private GameState state = GameState.READY;
    private TetrominoType currentType;
    private TetrominoType nextType;
    private TetrominoType heldType;
    private boolean holdUsed;

    public Game() {
        this(defaultPieceSource());
    }

    /** 생성기를 주입하여 테스트와 추후 팀원의 생성 로직 연결에 사용한다. */
    public Game(Supplier<TetrominoType> pieceSource) {
        this.pieceSource = Objects.requireNonNull(pieceSource);
    }

    private static Supplier<TetrominoType> defaultPieceSource() {
        RandomGenerator random = RandomGenerator.getDefault();
        TetrominoType[] types = TetrominoType.values();
        // Requirements 1: 매번 7종 중 동일한 확률로 선택한다.
        return () -> types[random.nextInt(types.length)];
    }

    public TetrominoType getCurrentType() {
        return currentType;
    }

    public TetrominoType getNextType() {
        return nextType;
    }

    public TetrominoType getHeldType() {
        return heldType;
    }

    public boolean canHold() {
        return state == GameState.RUNNING && !holdUsed;
    }

    public GameState getState() {
        return state;
    }

    public void start() {
        if (state == GameState.READY) {
            TetrominoType first = drawPiece();
            TetrominoType second = drawPiece();
            currentType = first;
            nextType = second;
            state = GameState.RUNNING;
        }
    }

    /** 성공하면 새 currentType의 블록을 초기 위치·회전으로 생성해야 한다. */
    public boolean hold() {
        if (!canHold()) {
            return false;
        }
        TetrominoType previous = currentType;
        if (heldType == null) {
            TetrominoType upcoming = drawPiece();
            currentType = nextType;
            nextType = upcoming;
        } else {
            currentType = heldType;
        }
        heldType = previous;
        holdUsed = true;
        return true;
    }

    /**
     * 실제 보드에 현재 블록을 고정한 직후 한 번 호출할 연결 지점.
     * 고정·충돌 검사는 이 메서드가 수행하지 않는다. UI 키에 직접 연결하지 않는다.
     */
    public boolean advanceAfterLock() {
        if (state != GameState.RUNNING) {
            return false;
        }
        TetrominoType upcoming = drawPiece();
        currentType = nextType;
        nextType = upcoming;
        holdUsed = false;
        return true;
    }

    private TetrominoType drawPiece() {
        return Objects.requireNonNull(pieceSource.get(), "블록 생성 결과는 null일 수 없습니다.");
    }

    public void pause() {
        if (state == GameState.RUNNING) {
            state = GameState.PAUSED;
        }
    }

    public void resume() {
        if (state == GameState.PAUSED) {
            state = GameState.RUNNING;
        }
    }

    /** 키 입력이나 화면 기술과 관계없이 일시정지 상태를 전환한다. */
    public void togglePause() {
        if (state == GameState.RUNNING) {
            pause();
        } else if (state == GameState.PAUSED) {
            resume();
        }
    }
}
