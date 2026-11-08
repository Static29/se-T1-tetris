package tetris.board;

/**
 * 아이템 모드의 등장 주기: 지운 줄을 세어 BlockSpawner.LINES_PER_ITEM줄마다 아이템 하나를 대기시킨다.
 * 넘친 줄은 다음 아이템으로 이월하고, 한 번에 여러 개가 쌓이면 대기 수가 늘어난다.
 * 블록을 만들거나 보드를 바꾸지 않는다. 어느 블록을 아이템으로 바꿀지는 BlockSpawner가 정한다.
 */
final class ItemCounter {
    /** 다음 아이템까지 쌓인 줄 수(0 ~ LINES_PER_ITEM-1). */
    private int linesTowardItem;
    /** 아직 Next에 반영하지 못한 아이템 수. */
    private int pendingItems;

    /** 지운 줄 수를 더한다. 음수 검사는 호출하는 BlockSpawner가 한다. */
    void addClearedLines(int lineCount) {
        linesTowardItem += lineCount;
        pendingItems += linesTowardItem / BlockSpawner.LINES_PER_ITEM;
        linesTowardItem %= BlockSpawner.LINES_PER_ITEM;
    }

    /** 대기 중인 아이템이 있으면 하나 꺼내고 true. 없으면 false. */
    boolean takePendingItem() {
        if (pendingItems == 0) {
            return false;
        }
        pendingItems--;
        return true;
    }

    int getLinesUntilItem() { return BlockSpawner.LINES_PER_ITEM - linesTowardItem; }

    int getPendingItemCount() { return pendingItems; }
}
