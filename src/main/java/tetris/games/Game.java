package tetris.games;

import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.Deque;
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
import tetris.board.BlockSpawner;
import tetris.board.PieceSelector;
import tetris.board.BlockLocker;
import tetris.board.LineClearEffect;
import tetris.board.LockDelay;
import tetris.score.ScorePolicy;
import tetris.score.DeletionScorePolicy;

public class Game {
    private final BlockSpawner spawner;
    private final BlockLocker locker;
    private PieceMotion motion;
    private final BoardProjection projection;
    private final LockDelay lockDelay = new LockDelay();
    public static final long LINE_CLEAR_MILLIS = 360;
    private final Deque<LineClearEffect> clearEffects = new ArrayDeque<>();
    private long clearElapsed;
    private long gameTime;
    private long fallElapsed;
    private static final class PieceProgress {
        int paidDepth;
        PieceProgress(int spawnDepth) { paidDepth = spawnDepth; }
    }
    private PieceProgress progress;
    private PieceProgress heldProgress;
    private GameState state = GameState.READY;
    private Block heldBlock;
    private boolean holdUsed;
    private final Board board;
    private Block currentBlock;
    private int clearedLines;
    private final GameSettings settings;
    private final ScorePolicy scorePolicy;
    private long score;
    private final GameSpeed speed;

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
        this.projection = new BoardProjection(board);
        this.settings = Objects.requireNonNull(settings);
        this.scorePolicy = Objects.requireNonNull(scorePolicy);
        this.spawner = BlockSpawner.fromBlocks(board, Objects.requireNonNull(pieceSource),
                new ItemBlockFactory(random), settings.mode() == GameMode.ITEM);
        this.locker = new BlockLocker(random);
        this.speed = new GameSpeed(settings.difficulty());
    }

    private static Supplier<Block> adaptTypes(Supplier<TetrominoType> source) {
        Objects.requireNonNull(source);
        return () -> new Block(Objects.requireNonNull(source.get(), "블록 종류는 null일 수 없습니다."));
    }

    private static Supplier<TetrominoType> defaultPieceSource(GameSettings settings) {
        RandomGenerator random = RandomGenerator.getDefault();
        return PieceSelector.withIWeight(settings.difficulty().getIWeight(), random)::next;
    }

    public TetrominoType getCurrentType() {
        return currentBlock == null ? null : currentBlock.getType();
    }

    public TetrominoType getNextType() {
        return state == GameState.READY ? null : spawner.peekNext().getType();
    }

    public TetrominoType getHeldType() {
        return heldBlock == null ? null : heldBlock.getType();
    }

    public Block getCurrentBlock() { return currentBlock == null ? null : currentBlock.copy(); }
    public Block getNextBlock() { return state == GameState.READY ? null : spawner.peekNext().copy(); }
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
            initializeCurrentPieceState();
        }
    }

    /** 아이템 정보는 유지하면서 교환한 블록의 위치·회전만 초기화한다. */
    public boolean hold() {
        if (!canHold()) {
            return false;
        }
        // Hold 보관·미리보기용 로컬 원점이며 실제 보드 생성 좌표는 Spawner가 결정한다.
        Block previous = currentBlock.spawnCopy(0, 0);
        PieceProgress previousProgress = progress;
        if (heldBlock == null) {
            takeNext();
        } else {
            currentBlock = heldBlock;
            progress = heldProgress;
            spawner.resetToSpawn(currentBlock);
        }
        heldBlock = previous;
        heldProgress = previousProgress;
        holdUsed = true;
        initializeCurrentPieceState();
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
        initializeCurrentPieceState();
        return true;
    }

    private void initializeCurrentPieceState() {
        motion = new PieceMotion(board, currentBlock);
        lockDelay.clear();
        fallElapsed = 0;
        if (board.isBlockOut(Position.fromXY(currentBlock.getBoardCells()))) {
            state = GameState.GAME_OVER;
        }
    }

    private void takeNext() {
        currentBlock = spawner.spawn();
        progress = new PieceProgress(currentBlock.getY());
    }

    private boolean canAct() { return state == GameState.RUNNING && !isClearingLines(); }
    public boolean isClearingLines() { return !clearEffects.isEmpty(); }
    public int getLinesUntilItem() { return spawner.getLinesUntilItem(); }
    public boolean isClearFlashOn() { return (clearElapsed / 60) % 2 == 0; }
    public Set<Integer> getFlashingVisibleRows() {
        return projection.flashingRows(clearEffects.peek());
    }

    public int getClearedLines() { return clearedLines; }
    public int getVisibleRows() { return board.getVisibleRows(); }
    public int getColumns() { return board.getColumns(); }
    public long getScore() { return score; }
    public GameSettings getSettings() { return settings; }
    public Difficulty getDifficulty() { return settings.difficulty(); }
    public long getFallIntervalMillis() {
        return speed.interval(clearedLines, motion != null && motion.isWeight());
    }
    public double getSpeedMultiplier() {
        return speed.multiplier(clearedLines, motion != null && motion.isWeight());
    }

    /** 아이템 담당이 실제 삭제한 고유 칸 수를 한 번 전달한다. 줄 삭제 칸은 제외한다. */
    public void recordItemCellsRemoved(int count) {
        if (count < 0) throw new IllegalArgumentException("삭제 칸 수는 음수일 수 없습니다.");
        if (canAct()) score += scorePolicy.removedCells(count, getDifficulty(), getSpeedMultiplier());
    }

    /** 화면용 복사본에만 낙하 중 블록을 합성한다. 숨겨진 행은 제외한다. */
    public Cell[][] getVisibleCells() {
        return projection.snapshot(visibleActiveBlock(), null, clearEffects.peek(), isClearFlashOn()).cells();
    }

    /** 표시 전용. 고정 M과 낙하 중 아이템의 기호를 반환한다. */
    public char[][] getVisibleSymbols() {
        return projection.snapshot(visibleActiveBlock(), null, clearEffects.peek(), isClearFlashOn()).symbols();
    }

    /** 현재 블록의 착지 지점. 무게추는 보드 복사본에서만 경로를 계산한다. */
    public List<Position> getGhostCells() {
        return projection.ghostCells(landingBlock());
    }

    /** UI는 프레임당 이 메서드 한 번으로 셀·문자·착지·깜빡임 데이터를 얻는다. */
    public GameBoardSnapshot getBoardSnapshot() {
        return projection.snapshot(visibleActiveBlock(), landingBlock(), clearEffects.peek(), isClearFlashOn());
    }

    private Block visibleActiveBlock() {
        return state == GameState.GAME_OVER || isClearingLines() ? null : currentBlock;
    }

    private Block landingBlock() {
        return visibleActiveBlock() == null ? null : motion.landing();
    }

    public boolean moveLeft() { return tryMove(-1, 0); }
    public boolean moveRight() { return tryMove(1, 0); }

    private boolean tryMove(int dx, int dy) {
        if (!canAct()) return false;
        if (!(dy > 0 ? motion.stepDown() : motion.moveSideways(dx))) return false;
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

    private void updateGrounded(boolean movedOrRotated) {
        if (motion.isGrounded()) {
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
        score += scorePolicy.lockedBlock(result, currentBlock.getItemType(), getDifficulty(), speed);
        clearedLines += result.totalRowsCleared();
        takeNext();
        // 기존 Next는 그대로 Current로 넘긴 뒤 새 Next에 아이템을 반영한다.
        spawner.onLinesCleared(result.totalRowsCleared());
        holdUsed = false;
        initializeCurrentPieceState();
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
