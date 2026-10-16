package tetris.block;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Game이 모드와 10줄 카운트를 판단한 뒤 호출할 아이템 생성기.
 * 일반 모드에서는 호출을 거부한다. 기본 테트로미노 종류는 Game 생성기로부터 전달받는다.
 */
public final class ItemBlockFactory {
    private final RandomGenerator random;

    public ItemBlockFactory() { this(RandomGenerator.getDefault()); }
    public ItemBlockFactory(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    /** 아이템 5종을 동일 확률로 선택하고, 부착형은 무작위 한 칸에 붙인다. */
    public Block create(boolean itemMode, TetrominoType baseType, int x, int y) {
        requireItemMode(itemMode);
        Objects.requireNonNull(baseType, "baseType");
        ItemType[] items = ItemType.values();
        return create(itemMode, baseType, items[random.nextInt(items.length)], x, y);
    }

    /** 특정 아이템 생성용. 무게추는 baseType을 사용하지 않는다. */
    public Block create(boolean itemMode, TetrominoType baseType, ItemType item, int x, int y) {
        requireItemMode(itemMode);
        Objects.requireNonNull(item, "item");
        if (item == ItemType.WEIGHT) return Block.weight(x, y);
        Objects.requireNonNull(baseType, "baseType");
        return Block.withItem(baseType, x, y, item, random.nextInt(baseType.getCells().length));
    }

    private static void requireItemMode(boolean itemMode) {
        if (!itemMode) throw new IllegalStateException("아이템은 아이템 모드에서만 생성할 수 있습니다.");
    }
}
