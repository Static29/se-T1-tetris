package tetris.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 대전 모드에서 한 플레이어가 받은 공격 줄을 보드에 넣기 전까지 모아 두는 곳(Req3 6쪽).
 * 화면의 "넘어온 블럭 표시 부분"이 이 내용을 위에서 아래 순서로 그린다.
 * <ul>
 * <li>최대 MAX_ROWS줄. 이미 가득 차 있으면 새 공격은 무시한다.</li>
 * <li>새 공격은 기존 줄의 아래쪽에 붙고, 합쳐서 MAX_ROWS줄을 넘으면 새 공격의 아래쪽을 잘라낸다.</li>
 * <li>블록을 고정한 뒤, 다음 블록을 생성하기 전에 insertInto로 모두 보드에 넣고 비운다.</li>
 * </ul>
 * 다인 모드에서는 공격하는 쪽이 isFull로 대상의 대기 줄이 가득 찼는지 확인하고 다른 대상을 고를 수 있다.
 */
public final class GarbageQueue {
    /** 대기할 수 있는 최대 공격 줄 수(Req3). */
    public static final int MAX_ROWS = 10;

    private final List<GarbageRow> rows = new ArrayList<>();

    /**
     * 공격 줄을 받는다. 가득 차 있으면 아무것도 받지 않고, 넘치는 만큼은 attack의 아래쪽부터 버린다.
     *
     * @param attack 위에서 아래 순서의 공격 줄(LockResult.attackRows())
     * @return 실제로 받은 줄 수. 가득 차 있었으면 0
     */
    public int receive(List<GarbageRow> attack) {
        Objects.requireNonNull(attack, "attack");
        attack.forEach(row -> Objects.requireNonNull(row, "attack의 원소"));
        int accepted = Math.min(attack.size(), MAX_ROWS - rows.size());
        rows.addAll(attack.subList(0, accepted));
        return accepted;
    }

    /** 대기 줄이 MAX_ROWS줄로 가득 차서 더 받을 수 없으면 true. */
    public boolean isFull() {
        return rows.size() >= MAX_ROWS;
    }

    /** 대기 중인 줄 수. */
    public int size() {
        return rows.size();
    }

    /** 대기 중인 공격 줄을 위에서 아래 순서로 반환한다. 화면 표시용 복사본이다. */
    public List<GarbageRow> getPendingRows() {
        return List.copyOf(rows);
    }

    /**
     * 대기 줄을 모두 board 맨 아래에 밀어 넣고 비운다. 블록 고정 후, 다음 블록 생성 전에 호출한다.
     *
     * @return 밀어올린 칸이 보드 위로 넘쳐 게임 오버면 true(Board.pushUpGarbage)
     */
    public boolean insertInto(Board board) {
        Objects.requireNonNull(board, "board");
        boolean overflow = board.pushUpGarbage(rows);
        rows.clear();
        return overflow;
    }

    /** 대기 줄을 모두 버린다. 새 게임을 시작할 때 사용한다. */
    public void clear() {
        rows.clear();
    }
}
