package tetris.board;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.function.Consumer;
import tetris.block.Block;
import tetris.block.ItemType;

/**
 * 블록을 보드에 고정하고, 붙어 있는 아이템의 효과와 줄 삭제까지 한 번에 처리한다.
 * 점수는 계산하지 않고 LockResult로 재료(줄 수·칸 수·배율 여부)와 대전 모드 공격 줄만 돌려준다.
 * 처리 순서: 고정 → Lock out 검사 → 아이템 효과 → 꽉 찬 줄 삭제 → 공격 줄 결정.
 *
 * <p>대전 모드 공격(Req3): 이 블록으로 지운 줄(L 줄 + 꽉 찬 줄)이 MIN_ATTACK_ROWS줄 이상이면 그 줄들을 공격 줄로 돌려준다.
 * 방금 놓은 블록 칸과 F가 채운 칸은 비워서 보내고, L 줄은 그 밖의 칸을 모두 채워 보낸다.
 * B·무게추로 고정한 경우는 줄을 지워도 보내지 않는다(팀 결정).
 *
 * <p>아이템별 효과는 ItemEffect 구현 클래스가 맡고, 이 클래스는 순서와 결과 합치기만 한다.
 * <ul>
 * <li>L: LineClearItemEffect / F: FillItemEffect / B: BombItemEffect / M: MultiplierItemEffect</li>
 * <li>무게추: 부착 칸이 없어 효과 없이 고정한다. 내려오며 지운 칸은 WeightMover가 처리하며 점수·배율이 없다.</li>
 * </ul>
 */
public final class BlockLocker {
    /** 상대에게 공격 줄을 보내려면 한 번에 지워야 하는 최소 줄 수(Req3). */
    public static final int MIN_ATTACK_ROWS = 2;

    private static final ItemEffect LINE_CLEAR = new LineClearItemEffect();
    private static final ItemEffect FILL = new FillItemEffect();
    private static final ItemEffect MULTIPLIER = new MultiplierItemEffect();

    /** 폭탄만 무작위 선택을 하므로 난수를 가진 인스턴스를 BlockLocker마다 둔다. */
    private final ItemEffect bomb;

    /** random은 폭탄이 지울 칸을 고를 때 쓴다. 테스트에서는 시드를 고정한다. */
    public BlockLocker(RandomGenerator random) {
        this.bomb = new BombItemEffect(random);
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

    /**
     * lock(board, block)과 같고, 줄을 지우기 직전마다 그 순간의 화면과 지울 줄 번호를 effects에 넘긴다(줄 삭제 연출용).
     * L 아이템 줄과 꽉 찬 줄은 지우는 순서대로 따로 넘긴다.
     */
    public LockResult lock(Board board, Block block, Consumer<LineClearEffect> effects) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(effects, "effects");
        Cell cell = cellOf(block);
        List<Position> positions = Position.fromXY(block.getBoardCells());
        board.lock(positions, cell);
        if (board.isLockOut(positions)) {
            return LockResult.LOCK_OUT;
        }

        AttackRowBuilder attack = new AttackRowBuilder(board, positions);
        ItemEffect.Outcome item = applyItem(board, block, cell, effects);
        attack.addHoles(item.filledCells());
        item.clearedRows().forEach(attack::rowCleared);
        boolean multiplierApplied = item.multiplierApplied();
        List<Integer> fullRows = board.findFullRows();
        if (!fullRows.isEmpty()) {
            multiplierApplied |= fullRows.stream().anyMatch(board::hasMultiplierInRow);
            effects.accept(new LineClearEffect(board.getVisibleCells(), fullRows));
        }
        attack.fullRowsCleared(fullRows);
        int fullRowsCleared = board.clearFullRows().size();
        List<GarbageRow> attackRows = canAttack(block) && attack.size() >= MIN_ATTACK_ROWS
                ? attack.rows() : List.of();
        return new LockResult(false, fullRowsCleared, item.rowsCleared(), item.cellsRemoved(),
                multiplierApplied, attackRows);
    }

    /** 이 블록으로 지운 줄을 상대에게 보낼 수 있는지. 아이템이 없는 블록은 보낸다. B·무게추는 보내지 않는다(팀 결정). */
    private static boolean canAttack(Block block) {
        if (!block.hasItem()) {
            return true;
        }
        return switch (block.getItemType()) {
            case LINE_CLEAR, FILL, MULTIPLIER -> true;
            case BOMB, WEIGHT -> false;
        };
    }

    /** 부착 아이템이 있으면 그 칸에 효과를 적용한다. 일반 블록과 무게추는 부착 칸이 없어 NONE. */
    private ItemEffect.Outcome applyItem(Board board, Block block, Cell cell, Consumer<LineClearEffect> effects) {
        List<Position> itemPositions = Position.fromXY(block.getBoardItemCells());
        if (itemPositions.isEmpty()) {
            return ItemEffect.Outcome.NONE;
        }
        return effectOf(block.getItemType()).apply(board, itemPositions.get(0), cell, effects);
    }

    /** 아이템 종류 → 효과. switch라서 ItemType에 종류가 추가되면 여기서 컴파일 오류로 알려 준다. */
    private ItemEffect effectOf(ItemType type) {
        return switch (type) {
            case LINE_CLEAR -> LINE_CLEAR;
            case FILL -> FILL;
            case BOMB -> bomb;
            case MULTIPLIER -> MULTIPLIER;
            case WEIGHT -> throw new IllegalStateException("무게추는 부착 칸이 없어 아이템 효과를 적용하지 않습니다.");
        };
    }
}
