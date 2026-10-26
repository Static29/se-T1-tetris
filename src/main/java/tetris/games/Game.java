package tetris.games;

import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import tetris.block.TetrominoType;
import tetris.block.Block;
import tetris.block.ItemType;
import tetris.block.ItemBlockFactory;
import tetris.board.Board;
import tetris.board.BlockRotation;
import tetris.board.Cell;
import tetris.board.Position;
import tetris.board.FallSpeed;
import tetris.board.BlockSpawner;
import tetris.board.PieceSelector;
import tetris.board.BlockLocker;
import tetris.board.WeightMover;
import tetris.board.LineClearEffect;
import tetris.board.LockDelay;
import tetris.score.ScorePolicy;
import tetris.score.DeletionScorePolicy;

public class Game {
    private final BlockSpawner spawner;
    private final BlockLocker locker;
    private WeightMover weightMover;
    private final LockDelay lockDelay = new LockDelay();
    public static final long LINE_CLEAR_MILLIS = 360;
    private final Deque<LineClearEffect> clearEffects = new ArrayDeque<>();
    private long clearElapsed;
    private long gameTime;
    private long fallElapsed;
    private static final class PieceProgress {
        int paidDepth;
        boolean weightTouched;
    }
    private PieceProgress progress = new PieceProgress();
    private PieceProgress heldProgress;
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
        this(defaultPieceSource(settings), new Board(), settings, new DeletionScorePolicy());
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
        this(board, pieceSource, settings, scorePolicy, RandomGenerator.getDefault());
    }

    public Game(Board board, Supplier<Block> pieceSource, GameSettings settings,
                ScorePolicy scorePolicy, RandomGenerator random) {
        this.board = Objects.requireNonNull(board);
        this.settings = Objects.requireNonNull(settings);
        this.scorePolicy = Objects.requireNonNull(scorePolicy);
        this.spawner = BlockSpawner.fromBlocks(board, Objects.requireNonNull(pieceSource),
                new ItemBlockFactory(random), settings.mode() == GameMode.ITEM);
        this.locker = new BlockLocker(random);
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

    private static Supplier<TetrominoType> defaultPieceSource(GameSettings settings) {
        RandomGenerator random = RandomGenerator.getDefault();
        int iWeight = switch (settings.difficulty()) {
            case EASY -> PieceSelector.EASY_I_WEIGHT;
            case NORMAL -> PieceSelector.NORMAL_I_WEIGHT;
            case HARD -> PieceSelector.HARD_I_WEIGHT;
        };
        return PieceSelector.withIWeight(iWeight, random)::next;
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
        return canAct() && !holdUsed && currentBlock != null
                && currentBlock.getItemType() != ItemType.WEIGHT;
    }

    public GameState getState() {
        return state;
    }

    public void start() {
        if (state == GameState.READY) {
            takeNext();
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
        PieceProgress previousProgress = progress;
        if (weightMover != null) previousProgress.weightTouched = weightMover.isTouched();
        if (heldBlock == null) {
            takeNext();
        } else {
            currentBlock = heldBlock;
            progress = heldProgress;
        }
        heldBlock = previous;
        heldProgress = previousProgress;
        holdUsed = true;
        spawnCurrent();
        return true;
    }

    /**
     * 실제 보드에 현재 블록을 고정한 직후 한 번 호출할 연결 지점.
     * 고정·충돌 검사는 이 메서드가 수행하지 않는다. UI 키에 직접 연결하지 않는다.
     */
    public boolean advanceAfterLock() {
        if (!canAct()) {
            return false;
        }
        takeNext();
        holdUsed = false;
        spawnCurrent();
        return true;
    }

    private void spawnCurrent() {
        currentBlock = currentBlock.spawnCopy((board.getColumns() - currentBlock.getFrameSize()) / 2, 0);
        weightMover = currentBlock.getItemType() == ItemType.WEIGHT
                ? new WeightMover(board, currentBlock, progress.weightTouched) : null;
        lockDelay.clear();
        fallElapsed = 0;
        if (board.isBlockOut(Position.fromXY(currentBlock.getBoardCells()))) {
            state = GameState.GAME_OVER;
        }
    }

    private void takeNext() {
        currentBlock = spawner.spawn();
        nextBlock = spawner.peekNext().copy();
        progress = new PieceProgress();
    }

    private boolean canAct() { return state == GameState.RUNNING && !isClearingLines(); }
    public boolean isClearingLines() { return !clearEffects.isEmpty(); }
    public int getLinesUntilItem() { return spawner.getLinesUntilItem(); }
    public boolean isClearFlashOn() { return (clearElapsed / 60) % 2 == 0; }
    public Set<Integer> getFlashingVisibleRows() {
        if (!isClearingLines()) return Set.of();
        return clearEffects.peek().rows().stream().filter(board::isVisibleRow)
                .map(row -> row - board.getBufferRows()).collect(Collectors.toUnmodifiableSet());
    }

    public int getClearedLines() { return clearedLines; }
    public long getScore() { return score; }
    public GameSettings getSettings() { return settings; }
    public Difficulty getDifficulty() { return settings.difficulty(); }
    public long getFallIntervalMillis() {
        return weightMover == null ? fallSpeed.getIntervalMillis(clearedLines) : fallSpeed.getWeightIntervalMillis(clearedLines);
    }
    public double getSpeedMultiplier() {
        return (double) FallSpeed.BASE_INTERVAL_MILLIS / getFallIntervalMillis();
    }

    /** 아이템 담당이 실제 삭제한 고유 칸 수를 한 번 전달한다. 줄 삭제 칸은 제외한다. */
    public void recordItemCellsRemoved(int count) {
        if (count < 0) throw new IllegalArgumentException("삭제 칸 수는 음수일 수 없습니다.");
        if (canAct()) score += scorePolicy.removedCells(count, getDifficulty(), getSpeedMultiplier());
    }

    /** 화면용 복사본에만 낙하 중 블록을 합성한다. 숨겨진 행은 제외한다. */
    public Cell[][] getVisibleCells() {
        if (isClearingLines()) return clearEffects.peek().visibleCells();
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

    /** 표시 전용. 고정 M과 낙하 중 아이템의 기호를 반환한다. */
    public char[][] getVisibleSymbols() {
        Cell[][] cells = getVisibleCells();
        char[][] symbols = new char[cells.length][board.getColumns()];
        for (int row = 0; row < cells.length; row++) for (int col = 0; col < board.getColumns(); col++)
            symbols[row][col] = cells[row][col].isEmpty() ? '.'
                    : !isClearingLines() && board.isMultiplier(row + board.getBufferRows(), col) ? 'M' : 'O';
        if (!isClearingLines() && currentBlock != null && state != GameState.GAME_OVER) {
            for (int[] cell : currentBlock.getCells()) {
                int row = cell[1] + currentBlock.getY(), col = cell[0] + currentBlock.getX();
                if (board.isVisibleRow(row)) symbols[row - board.getBufferRows()][col] = currentBlock.getCellSymbol(cell[0], cell[1]);
            }
        }
        return symbols;
    }

    /** 현재 블록의 착지 지점. 무게추는 보드 복사본에서만 경로를 계산한다. */
    public List<Position> getGhostCells() {
        if (currentBlock == null || state == GameState.GAME_OVER || isClearingLines()) return List.of();
        Block ghost = currentBlock.copy();
        if (weightMover != null) new WeightMover(board.copy(), ghost).dropToFloor();
        else while (board.canPlace(Position.fromXY(ghost.previewMove(0, 1)))) ghost.move(0, 1);
        return Position.fromXY(ghost.getBoardCells()).stream().filter(p -> board.isVisibleRow(p.row()))
                .map(p -> new Position(p.row() - board.getBufferRows(), p.col())).toList();
    }

    public boolean moveLeft() { return tryMove(-1, 0); }
    public boolean moveRight() { return tryMove(1, 0); }

    private boolean tryMove(int dx, int dy) {
        if (!canAct()) return false;
        if (weightMover != null) {
            if (!(dy > 0 ? weightMover.stepDown() : weightMover.tryMoveSideways(dx))) return false;
        } else {
            if (!board.canPlace(Position.fromXY(currentBlock.previewMove(dx, dy)))) return false;
            currentBlock.move(dx, dy);
        }
        if (dy > 0 && currentBlock.getY() > progress.paidDepth) {
            score += scorePolicy.descendedCells(1, getDifficulty(), getSpeedMultiplier());
            progress.paidDepth = currentBlock.getY();
        }
        updateGrounded(dx != 0);
        return true;
    }

    public boolean rotateClockwise() {
        if (!canAct() || !BlockRotation.tryRotate(board, currentBlock)) return false;
        updateGrounded(true);
        return true;
    }

    /** 자동 하강과 소프트 드롭 공용. 닿으면 500ms 고정 여유를 시작한다. */
    public boolean softDrop() {
        if (!canAct()) return false;
        if (tryMove(0, 1)) return true;
        updateGrounded(false);
        return false;
    }

    public void tick() { softDrop(); }

    private boolean isGrounded() {
        return weightMover != null ? weightMover.isOnFloor()
                : !board.canPlace(Position.fromXY(currentBlock.previewMove(0, 1)));
    }

    private void updateGrounded(boolean movedOrRotated) {
        if (isGrounded()) {
            if (movedOrRotated) lockDelay.onMoveOrRotate(gameTime);
            lockDelay.onGrounded(gameTime);
        } else lockDelay.onAirborne();
    }

    /** 일시정지 시간을 제외한 경과 시간. UI와 테스트가 같은 시계를 사용한다. */
    public void advanceTime(long millis) {
        if (millis < 0) throw new IllegalArgumentException("시간은 음수일 수 없습니다.");
        if (state == GameState.PAUSED || state == GameState.READY) return;
        while (millis > 0) {
            long step = Math.min(16, millis);
            if (isClearingLines()) step = Math.min(step, LINE_CLEAR_MILLIS - clearElapsed);
            millis -= step;
            if (isClearingLines()) {
                clearElapsed += step;
                if (clearElapsed >= LINE_CLEAR_MILLIS) {
                    clearElapsed = 0;
                    clearEffects.removeFirst();
                }
                continue;
            }
            if (state != GameState.RUNNING) return;
            gameTime += step;
            fallElapsed += step;
            updateGrounded(false);
            if (fallElapsed >= getFallIntervalMillis()) {
                fallElapsed -= getFallIntervalMillis();
                softDrop();
            }
            if (lockDelay.isExpired(gameTime)) lockCurrent();
        }
    }

    public int hardDrop() {
        if (!canAct()) return 0;
        int distance = 0;
        while (tryMove(0, 1)) distance++;
        lockCurrent();
        return distance;
    }

    private void lockCurrent() {
        double speed = getSpeedMultiplier();
        var result = locker.lock(board, currentBlock, clearEffects::addLast);
        if (result.lockOut()) {
            state = GameState.GAME_OVER;
            return;
        }
        long gained = scorePolicy.clearedRows(result.fullRowsCleared(), getDifficulty(), speed)
                + scorePolicy.removedCells(result.itemCellsRemoved(), getDifficulty(), speed);
        score += result.multiplierApplied() ? gained * 2 : gained;
        clearedLines += result.totalRowsCleared();
        spawner.onLinesCleared(result.totalRowsCleared());
        takeNext();
        holdUsed = false;
        spawnCurrent();
    }

    private Cell currentCell() {
        return BlockLocker.cellOf(currentBlock);
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
