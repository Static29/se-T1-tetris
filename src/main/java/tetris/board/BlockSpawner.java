package tetris.board;

import java.util.Objects;
import java.util.function.Supplier;
import tetris.block.Block;
import tetris.block.ItemBlockFactory;

/**
 * 다음 블록(Next)을 미리 정해 두고, 요청하면 생성 위치에 놓아 내보낸다.
 * 종류는 PieceSelector가 확률로 정하고, 아이템 모드에서는 지운 줄이 LINES_PER_ITEM줄 쌓일 때마다
 * 화면에 보이는 Next를 즉시 아이템 블록으로 바꾼다. 보드의 칸은 바꾸지 않는다.
 */
public final class BlockSpawner {
    /** Req2: 아이템 모드에서 10줄을 지울 때마다 아이템이 등장한다. */
    public static final int LINES_PER_ITEM = 10;

    private final Board board;
    private final Supplier<Block> source;
    private final ItemBlockFactory itemFactory;
    private final boolean itemMode;
    private Block next;
    /** 다음 아이템까지 쌓인 줄 수(0 ~ LINES_PER_ITEM-1). 넘친 줄은 다음 아이템으로 이월한다. */
    private int linesTowardItem;
    /** 한 번에 많은 줄을 지워 아직 Next에 반영하지 못한 아이템 수. 이후 Next부터 차례로 반영한다. */
    private int pendingItems;

    public BlockSpawner(Board board, PieceSelector selector, ItemBlockFactory itemFactory, boolean itemMode) {
        this(board, itemFactory, itemMode, adaptSelector(selector));
    }

    /** 테스트·외부 생성기가 전달한 아이템 정보도 보존한다. */
    private BlockSpawner(Board board, ItemBlockFactory itemFactory, boolean itemMode, Supplier<Block> source) {
        this.board = Objects.requireNonNull(board, "board");
        this.source = Objects.requireNonNull(source, "source");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory");
        this.itemMode = itemMode;
        this.next = drawNext();
    }

    public static BlockSpawner fromBlocks(Board board, Supplier<Block> source, ItemBlockFactory factory, boolean itemMode) {
        return new BlockSpawner(board, factory, itemMode, source);
    }

    private static Supplier<Block> adaptSelector(PieceSelector selector) {
        Objects.requireNonNull(selector, "selector");
        return () -> new Block(selector.next());
    }

    /** 다음에 생성될 블록. 미리보기 표시에만 사용하고 이동·회전하지 않는다. */
    public Block peekNext() { return next; }

    public boolean isItemMode() { return itemMode; }

    /** 다음 아이템까지 남은 줄 수. 아이템 모드가 아니어도 계산은 한다. */
    public int getLinesUntilItem() { return LINES_PER_ITEM - linesTowardItem; }

    public int getPendingItemCount() { return pendingItems; }

    /**
     * Next 블록을 생성 위치에 놓아 반환하고, 새 Next를 정한다.
     * 생성 위치에 놓을 수 있는지(Block out)는 호출하는 쪽이 board.isBlockOut으로 검사한다.
     */
    public Block spawn() {
        Block spawned = next;
        placeAtSpawn(spawned);
        next = drawNext();
        return spawned;
    }

    /** Hold에서 꺼낸 블록처럼 이미 쓰던 블록을 생성 모양(회전 상태 0)과 생성 위치로 되돌린다. 아이템은 유지된다. */
    public void resetToSpawn(Block block) {
        Objects.requireNonNull(block, "block");
        // 후보 0번은 항상 (0, 0) 보정이라 회전만 바뀌고 원점은 그대로다. 네 번 돌면 처음 모양으로 돌아온다.
        while (block.getRotationState() != 0) {
            block.rotate();
        }
        placeAtSpawn(block);
    }

    /**
     * 줄을 지운 뒤 지운 줄 수를 한 번 전달한다. 아이템 모드에서 LINES_PER_ITEM줄이 쌓이면
     * 지금 보이는 Next를 바로 아이템으로 바꾸고, 그 이상 쌓인 아이템은 이후 Next에 차례로 반영한다.
     */
    public void onLinesCleared(int lineCount) {
        if (lineCount < 0) {
            throw new IllegalArgumentException("지운 줄 수는 음수일 수 없습니다: " + lineCount);
        }
        if (!itemMode || lineCount == 0) {
            return;
        }
        linesTowardItem += lineCount;
        pendingItems += linesTowardItem / LINES_PER_ITEM;
        linesTowardItem %= LINES_PER_ITEM;
        if (pendingItems > 0 && !next.hasItem()) {
            next = itemFactory.create(true, next.getType(), 0, 0);
            pendingItems--;
        }
    }

    private Block drawNext() {
        Block block = Objects.requireNonNull(source.get(), "source result").spawnCopy(0, 0);
        if (pendingItems > 0 && !block.hasItem()) {
            pendingItems--;
            return itemFactory.create(true, block.getType(), 0, 0);
        }
        return block;
    }

    /** 회전 공간을 가로 가운데에 두고, 블록의 맨 윗줄이 화면에 보이는 첫 줄(버퍼 바로 아래)에 오게 놓는다. */
    private void placeAtSpawn(Block block) {
        int topLocalRow = Integer.MAX_VALUE;
        for (int[] cell : block.getCells()) {
            topLocalRow = Math.min(topLocalRow, cell[1]);
        }
        int spawnX = (board.getColumns() - block.getFrameSize()) / 2;
        int spawnY = board.getBufferRows() - topLocalRow;
        block.move(spawnX - block.getX(), spawnY - block.getY());
    }
}
