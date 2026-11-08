package tetris.board;

import java.util.Objects;
import java.util.function.Supplier;
import tetris.block.Block;
import tetris.block.ItemBlockFactory;

/**
 * 다음 블록(Next)을 미리 정해 두고, 요청하면 생성 위치에 놓아 내보낸다.
 * 종류는 PieceSelector(또는 주입한 생성기)가 정하고, 아이템 모드에서는 ItemCounter가 LINES_PER_ITEM줄마다
 * 아이템을 대기시키면 화면에 보이는 Next를 즉시 아이템 블록으로 바꾼다. 보드의 칸은 바꾸지 않는다.
 */
public final class BlockSpawner {
    /** Req2: 아이템 모드에서 10줄을 지울 때마다 아이템이 등장한다. */
    public static final int LINES_PER_ITEM = 10;
    /**
     * 생성 위치의 회전 공간 원점 y(팀 결정 2026-11-07). 0은 숨겨진 버퍼 맨 위 줄이므로
     * I는 row 1, 나머지는 row 0부터 차지하고 버퍼에서 내려와 화면에 나타난다.
     */
    public static final int SPAWN_Y = 0;

    private final Board board;
    private final Supplier<Block> source;
    private final ItemBlockFactory itemFactory;
    private final boolean itemMode;
    /** 아이템 등장 주기(10줄 카운트·이월·대기 수). 아이템 모드가 아니면 줄이 더해지지 않는다. */
    private final ItemCounter itemCounter = new ItemCounter();
    private Block next;

    public BlockSpawner(Board board, PieceSelector selector, ItemBlockFactory itemFactory, boolean itemMode) {
        this(board, itemFactory, itemMode, adaptSelector(selector));
    }

    private BlockSpawner(Board board, ItemBlockFactory itemFactory, boolean itemMode, Supplier<Block> source) {
        this.board = Objects.requireNonNull(board, "board");
        this.source = Objects.requireNonNull(source, "source");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory");
        this.itemMode = itemMode;
        this.next = drawNext();
    }

    /** 종류 대신 완성된 Block을 주는 생성기를 쓴다. 생성기가 붙인 아이템 정보도 보존한다(Game·테스트용). */
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

    /** 다음 아이템까지 남은 줄 수. 아이템 모드가 아니면 줄이 쌓이지 않아 항상 LINES_PER_ITEM이다. */
    public int getLinesUntilItem() { return itemCounter.getLinesUntilItem(); }

    /** 한 번에 많은 줄을 지워 아직 Next에 반영하지 못한 아이템 수. 이후 Next부터 차례로 반영한다. */
    public int getPendingItemCount() { return itemCounter.getPendingItemCount(); }

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
        itemCounter.addClearedLines(lineCount);
        next = applyPendingItem(next);
    }

    private Block drawNext() {
        return applyPendingItem(Objects.requireNonNull(source.get(), "source result").spawnCopy(0, 0));
    }

    /**
     * 대기 중인 아이템이 있고 block이 아직 아이템이 아니면, 같은 종류의 아이템 블록으로 바꾸고 대기 수를 하나 줄인다.
     * 지금 보이는 Next를 바꿀 때(onLinesCleared)와 새 Next를 뽑을 때(drawNext)가 같은 규칙을 쓰게 한다.
     */
    private Block applyPendingItem(Block block) {
        if (block.hasItem() || !itemCounter.takePendingItem()) {
            return block;
        }
        return itemFactory.create(true, block.getType(), 0, 0);
    }

    /** 회전 공간의 원점을 가로 가운데, 세로 SPAWN_Y(버퍼 맨 위)에 놓는다. */
    private void placeAtSpawn(Block block) {
        int spawnX = (board.getColumns() - block.getFrameSize()) / 2;
        block.move(spawnX - block.getX(), SPAWN_Y - block.getY());
    }
}
