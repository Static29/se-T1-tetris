package tetris.board;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

/**
 * 블록 하나를 고정하는 동안 지운 줄을 대전 모드 공격 줄로 바꾼다(Req3).
 * 비워서 보내는 칸은 방금 놓은 블록의 칸과 F가 채운 칸이고, 나머지는 원래 비어 있었어도 채워서 보낸다(L 줄, 팀 결정).
 * L로 줄을 지우면 위 줄이 한 칸씩 내려오므로, 지금 줄이 고정 직후 몇 번 줄이었는지 따라가
 * 공격 줄을 보드에 있던 위아래 순서 그대로 돌려준다.
 */
final class AttackRowBuilder {
    private final int columns;
    /** 비워서 보낼 칸. 고정 직후의 좌표다. */
    private final Set<Position> holes;
    /** originRows[지금 줄] = 고정 직후의 줄 번호. L로 위에 새로 생긴 빈 줄은 -1. */
    private final int[] originRows;
    /** 고정 직후 줄 번호 순으로 정렬한 공격 줄. */
    private final SortedMap<Integer, GarbageRow> cleared = new TreeMap<>();

    /** blockCells: 방금 고정한 블록의 칸. 아이템 효과를 적용하기 전에 만든다. */
    AttackRowBuilder(Board board, Collection<Position> blockCells) {
        this.columns = board.getColumns();
        this.holes = new HashSet<>(blockCells);
        this.originRows = IntStream.range(0, board.getTotalRows()).toArray();
    }

    /** 아이템이 채운 칸(F)도 비워서 보낸다. */
    void addHoles(Collection<Position> cells) {
        for (Position cell : cells) {
            holes.add(new Position(originRows[cell.row()], cell.col()));
        }
    }

    /** row 한 줄을 지워 위 줄이 한 칸씩 내려왔다(L). */
    void rowCleared(int row) {
        record(row);
        System.arraycopy(originRows, 0, originRows, 1, row);
        originRows[0] = -1;
    }

    /** 꽉 찬 줄을 지웠다. 이번 고정의 마지막 삭제라서 이후 줄 번호는 따라가지 않는다. */
    void fullRowsCleared(List<Integer> rows) {
        rows.forEach(this::record);
    }

    /** 지운 줄 수. */
    int size() {
        return cleared.size();
    }

    /** 공격 줄을 위에서 아래 순서로 반환한다. */
    List<GarbageRow> rows() {
        return List.copyOf(cleared.values());
    }

    private void record(int row) {
        int origin = originRows[row];
        List<Integer> holeColumns = holes.stream()
                .filter(cell -> cell.row() == origin)
                .map(Position::col)
                .toList();
        cleared.put(origin, GarbageRow.withHoles(columns, holeColumns));
    }
}
