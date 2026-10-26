package tetris.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.function.Consumer;
import tetris.block.Block;
import tetris.block.ItemType;

/**
 * 블록을 보드에 고정하고, 붙어 있는 아이템의 효과와 줄 삭제까지 한 번에 처리한다.
 * 점수는 계산하지 않고 LockResult로 재료(줄 수·칸 수·배율 여부)만 돌려준다.
 *
 * <ul>
 * <li>L(줄 삭제): L이 있는 줄을 꽉 차지 않았어도 지운다. 점수는 그 줄의 찬 칸 수.</li>
 * <li>F(빈칸 채우기): F 바로 아래부터 바닥까지 같은 열의 빈칸을 그 블록 종류로 채운다.</li>
 * <li>B(폭탄): B 칸은 항상 사라지고, 주변 8칸 중 찬 칸에서 최대 4개를 무작위로 지운다. 점수는 B 포함 지운 칸 수.</li>
 * <li>M(점수 배율): 고정된 칸에 배율 표시를 남긴다. 이 칸이 줄 삭제·L·B로 사라지는 고정의 점수 전체가 2배.</li>
 * <li>무게추: 효과 없이 고정한다. 내려오며 지운 칸은 WeightMover가 처리하며 점수·배율이 없다.</li>
 * </ul>
 * 처리 순서: 고정 → Lock out 검사 → 아이템 효과 → 꽉 찬 줄 삭제.
 */
public final class BlockLocker {
    /** 폭탄이 주변에서 지우는 최대 칸 수. */
    public static final int BOMB_REMOVE_COUNT = 4;

    private final RandomGenerator random;

    /** random은 폭탄이 지울 칸을 고를 때 쓴다. 테스트에서는 시드를 고정한다. */
    public BlockLocker(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    /** 블록을 보드에 저장할 칸 종류. 무게추는 종류가 없으므로 WEIGHT, 나머지는 테트로미노 종류와 같은 이름. */
    public static Cell cellOf(Block block) {
        Objects.requireNonNull(block, "block");
        if (block.getItemType() == ItemType.WEIGHT) {
            return Cell.WEIGHT;
        }
        return Cell.valueOf(block.getType().name());
    }

    /** 블록의 현재 위치에 고정하고 아이템 효과·줄 삭제를 적용한다. 놓을 수 없는 위치면 아무것도 바꾸지 않고 예외를 던진다. */
    public LockResult lock(Board board, Block block) {
        return lock(board, block, effect -> { });
    }

    public LockResult lock(Board board, Block block, Consumer<LineClearEffect> effects) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(effects, "effects");
        Cell cell = cellOf(block);
        List<Position> positions = Position.fromXY(block.getBoardCells());
        board.lock(positions, cell);
        if (board.isLockOut(positions)) {
            return LockResult.LOCK_OUT;
        }

        int itemRowsCleared = 0;
        int itemCellsRemoved = 0;
        boolean multiplierApplied = false;
        List<Position> itemPositions = Position.fromXY(block.getBoardItemCells());
        if (!itemPositions.isEmpty()) {
            Position item = itemPositions.get(0);
            switch (block.getItemType()) {
                case LINE_CLEAR -> {
                    multiplierApplied = board.hasMultiplierInRow(item.row());
                    effects.accept(new LineClearEffect(board.getVisibleCells(), List.of(item.row())));
                    itemCellsRemoved = board.clearRow(item.row());
                    itemRowsCleared = 1;
                }
                case FILL -> fillBelow(board, item, cell);
                case BOMB -> {
                    List<Position> targets = bombTargets(board, item);
                    multiplierApplied = containsMultiplier(board, targets);
                    itemCellsRemoved = board.removeCells(targets);
                }
                case MULTIPLIER -> board.markMultiplier(item);
                case WEIGHT -> { }   // 무게추는 부착 칸이 없어 여기 오지 않는다.
            }
        }

        List<Integer> fullRows = board.findFullRows();
        for (int row : fullRows) {
            if (board.hasMultiplierInRow(row)) {
                multiplierApplied = true;
            }
        }
        if (!fullRows.isEmpty()) effects.accept(new LineClearEffect(board.getVisibleCells(), fullRows));
        int fullRowsCleared = board.clearFullRows().size();
        return new LockResult(false, fullRowsCleared, itemRowsCleared, itemCellsRemoved, multiplierApplied);
    }

    /** F 바로 아래부터 바닥까지 같은 열의 빈칸을 cell로 채운다. 위쪽은 채우지 않는다. */
    private static void fillBelow(Board board, Position item, Cell cell) {
        List<Position> emptyBelow = new ArrayList<>();
        for (int row = item.row() + 1; row < board.getTotalRows(); row++) {
            if (board.isEmpty(row, item.col())) {
                emptyBelow.add(new Position(row, item.col()));
            }
        }
        if (!emptyBelow.isEmpty()) {
            board.lock(emptyBelow, cell);
        }
    }

    /** 폭탄 칸 자신과, 주변 8칸 중 찬 칸에서 무작위로 고른 최대 BOMB_REMOVE_COUNT개. */
    private List<Position> bombTargets(Board board, Position bomb) {
        List<Position> neighbors = new ArrayList<>();
        for (int dRow = -1; dRow <= 1; dRow++) {
            for (int dCol = -1; dCol <= 1; dCol++) {
                Position neighbor = bomb.offset(dRow, dCol);
                if ((dRow != 0 || dCol != 0) && board.isInside(neighbor) && !board.isEmpty(neighbor)) {
                    neighbors.add(neighbor);
                }
            }
        }
        // 앞쪽부터 무작위 칸과 자리를 바꿔 처음 pickCount개를 고른다(부분 Fisher-Yates 섞기).
        int pickCount = Math.min(BOMB_REMOVE_COUNT, neighbors.size());
        for (int i = 0; i < pickCount; i++) {
            int j = i + random.nextInt(neighbors.size() - i);
            Position swap = neighbors.get(i);
            neighbors.set(i, neighbors.get(j));
            neighbors.set(j, swap);
        }
        List<Position> targets = new ArrayList<>(neighbors.subList(0, pickCount));
        targets.add(bomb);
        return targets;
    }

    private static boolean containsMultiplier(Board board, List<Position> positions) {
        for (Position position : positions) {
            if (board.isMultiplier(position)) {
                return true;
            }
        }
        return false;
    }
}
