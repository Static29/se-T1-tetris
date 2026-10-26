package tetris.games;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import tetris.block.TetrominoType;
import tetris.block.Block;
import tetris.board.Board;
import tetris.board.BlockRotation;
import tetris.board.Cell;
import tetris.board.Position;
import tetris.board.FallSpeed;
import tetris.score.ScorePolicy;
import tetris.score.DeletionScorePolicy;

public class Game {
    private final Supplier<Block> pieceSource;
    private GameState state = GameState.READY;
    private Block nextBlock;
    private Block heldBlock;
    private boolean holdUsed;
    private final Board board;
    private Block currentBlock;
    private int clearedLines;
    private final GameSettings settings;
    private final ScorePolicy scorePolicy;
    private long score;
    private final FallSpeed fallSpeed;

    public Game() {
        this(GameSettings.DEFAULT);
    }

    public Game(GameSettings settings) {
        this(defaultPieceSource(), new Board(), settings, new DeletionScorePolicy());
    }

    /** 생성기를 주입하여 테스트와 추후 팀원의 생성 로직 연결에 사용한다. */
    public Game(Supplier<TetrominoType> pieceSource) {
        this(pieceSource, new Board());
    }

    public Game(Supplier<TetrominoType> pieceSource, Board board) {
        this(pieceSource, board, GameSettings.DEFAULT, new DeletionScorePolicy());
    }

    public Game(Supplier<TetrominoType> pieceSource, Board board,
                GameSettings settings, ScorePolicy scorePolicy) {
        this(board, adaptTypes(pieceSource), settings, scorePolicy);
    }

    /** 아이템을 포함한 완성된 Block 생성기를 연결한다. 전달된 객체는 복사해 소유한다. */
    public Game(Board board, Supplier<Block> pieceSource,
                GameSettings settings, ScorePolicy scorePolicy) {
        this.pieceSource = Objects.requireNonNull(pieceSource);
        this.board = Objects.requireNonNull(board);
        this.settings = Objects.requireNonNull(settings);
        this.scorePolicy = Objects.requireNonNull(scorePolicy);
        this.fallSpeed = new FallSpeed(switch (settings.difficulty()) {
            case EASY -> FallSpeed.EASY_INCREASE_PERCENT;
            case NORMAL -> FallSpeed.NORMAL_INCREASE_PERCENT;
            case HARD -> FallSpeed.HARD_INCREASE_PERCENT;
        });
    }

    private static Supplier<Block> adaptTypes(Supplier<TetrominoType> source) {
        Objects.requireNonNull(source);
        return () -> new Block(Objects.requireNonNull(source.get(), "블록 종류는 null일 수 없습니다."));
    }

    private static Supplier<TetrominoType> defaultPieceSource() {
        RandomGenerator random = RandomGenerator.getDefault();
        TetrominoType[] types = TetrominoType.values();
        // Requirements 1: 매번 7종 중 동일한 확률로 선택한다.
        return () -> types[random.nextInt(types.length)];
    }

    public TetrominoType getCurrentType() {
        return currentBlock == null ? null : currentBlock.getType();
    }

    public TetrominoType getNextType() {
        return nextBlock == null ? null : nextBlock.getType();
    }

    public TetrominoType getHeldType() {
        return heldBlock == null ? null : heldBlock.getType();
    }

    public Block getCurrentBlock() { return currentBlock == null ? null : currentBlock.copy(); }
    public Block getNextBlock() { return nextBlock == null ? null : nextBlock.copy(); }
    public Block getHeldBlock() { return heldBlock == null ? null : heldBlock.copy(); }

    public boolean canHold() {
        return state == GameState.RUNNING && !holdUsed;
    }

    public GameState getState() {
        return state;
    }

    public void start() {
        if (state == GameState.READY) {
            Block first = drawPiece();
            Block second = drawPiece();
            currentBlock = first;
            nextBlock = second;
            state = GameState.RUNNING;
            spawnCurrent();
        }
    }

    /** 아이템 정보는 유지하면서 교환한 블록의 위치·회전만 초기화한다. */
    public boolean hold() {
        if (!canHold()) {
            return false;
        }
        Block previous = currentBlock.spawnCopy(0, 0);
        if (heldBlock == null) {
            Block upcoming = drawPiece();
            currentBlock = nextBlock;
            nextBlock = upcoming;
        } else {
            currentBlock = heldBlock;
        }
        heldBlock = previous;
        holdUsed = true;
        spawnCurrent();
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
        Block upcoming = drawPiece();
        currentBlock = nextBlock;
        nextBlock = upcoming;
        holdUsed = false;
        spawnCurrent();
        return true;
    }

    private void spawnCurrent() {
        currentBlock = currentBlock.spawnCopy((board.getColumns() - currentBlock.getFrameSize()) / 2, 0);
        if (board.isBlockOut(Position.fromXY(currentBlock.getBoardCells()))) {
            state = GameState.GAME_OVER;
        }
    }

    public int getClearedLines() { return clearedLines; }
    public long getScore() { return score; }
    public GameSettings getSettings() { return settings; }
    public Difficulty getDifficulty() { return settings.difficulty(); }
    public long getFallIntervalMillis() { return fallSpeed.getIntervalMillis(clearedLines); }
    public double getSpeedMultiplier() {
        return (double) FallSpeed.BASE_INTERVAL_MILLIS / getFallIntervalMillis();
    }

    /** 아이템 담당이 실제 삭제한 고유 칸 수를 한 번 전달한다. 줄 삭제 칸은 제외한다. */
    public void recordItemCellsRemoved(int count) {
        if (count < 0) throw new IllegalArgumentException("삭제 칸 수는 음수일 수 없습니다.");
        if (state == GameState.RUNNING) score += scorePolicy.removedCells(count, getDifficulty(), getSpeedMultiplier());
    }

    /** 화면용 복사본에만 낙하 중 블록을 합성한다. 숨겨진 행은 제외한다. */
    public Cell[][] getVisibleCells() {
        Cell[][] cells = board.getVisibleCells();
        if (currentBlock != null && state != GameState.GAME_OVER) {
            for (Position p : Position.fromXY(currentBlock.getBoardCells())) {
                if (board.isVisibleRow(p.row())) {
                    cells[p.row() - board.getBufferRows()][p.col()] = currentCell();
                }
            }
        }
        return cells;
    }

    public boolean moveLeft() { return tryMove(-1, 0); }
    public boolean moveRight() { return tryMove(1, 0); }

    private boolean tryMove(int dx, int dy) {
        if (state != GameState.RUNNING
                || !board.canPlace(Position.fromXY(currentBlock.previewMove(dx, dy)))) {
            return false;
        }
        currentBlock.move(dx, dy);
        if (dy > 0) score += scorePolicy.descendedCells(dy, getDifficulty(), getSpeedMultiplier());
        return true;
    }

    public boolean rotateClockwise() {
        return state == GameState.RUNNING && BlockRotation.tryRotate(board, currentBlock);
    }

    /** 자동 하강과 소프트 드롭 공용. 아래가 막히면 즉시 고정한다. */
    public boolean softDrop() {
        if (state != GameState.RUNNING) return false;
        if (tryMove(0, 1)) return true;
        lockCurrent();
        return false;
    }

    public void tick() { softDrop(); }

    public int hardDrop() {
        if (state != GameState.RUNNING) return 0;
        int distance = 0;
        while (tryMove(0, 1)) distance++;
        lockCurrent();
        return distance;
    }

    private void lockCurrent() {
        var positions = Position.fromXY(currentBlock.getBoardCells());
        board.lock(positions, currentCell());
        if (board.isLockOut(positions)) {
            state = GameState.GAME_OVER;
            return;
        }
        int rows = board.clearFullRows().size();
        score += scorePolicy.clearedRows(rows, getDifficulty(), getSpeedMultiplier());
        clearedLines += rows;
        advanceAfterLock();
    }

    private Block drawPiece() {
        return Objects.requireNonNull(pieceSource.get(), "블록 생성 결과는 null일 수 없습니다.").spawnCopy(0, 0);
    }

    private Cell currentCell() {
        // 무게추에는 TetrominoType이 없다. 보드 효과 연결 전에는 일반 점유 칸으로 표시·고정한다.
        return currentBlock.getType() == null ? Cell.O : Cell.valueOf(currentBlock.getType().name());
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
