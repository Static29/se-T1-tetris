package tetris.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * B(폭탄): B 칸은 항상 사라지고, 주변 8칸 중 찬 칸에서 최대 REMOVE_COUNT개를 무작위로 지운다.
 * 위 칸은 내려오지 않아 구멍이 남는다. 점수 재료는 B 포함 지운 칸 수.
 */
final class BombItemEffect implements ItemEffect {
    /** 폭탄이 주변에서 지우는 최대 칸 수. */
    static final int REMOVE_COUNT = 4;

    private final RandomGenerator random;

    /** random은 지울 칸을 고를 때 쓴다. 테스트에서는 시드를 고정한다. */
    BombItemEffect(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public Outcome apply(Board board, Position item, Cell cell, Consumer<LineClearEffect> effects) {
        List<Position> targets = targets(board, item);
        boolean multiplierApplied = targets.stream().anyMatch(board::isMultiplier);
        int cellsRemoved = board.removeCells(targets);
        return new Outcome(0, cellsRemoved, multiplierApplied);
    }

    /** 폭탄 칸 자신과, 주변 8칸 중 찬 칸에서 무작위로 고른 최대 REMOVE_COUNT개. */
    private List<Position> targets(Board board, Position bomb) {
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
        int pickCount = Math.min(REMOVE_COUNT, neighbors.size());
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
}
