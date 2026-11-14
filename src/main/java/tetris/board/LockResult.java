package tetris.board;

import java.util.List;
import java.util.Objects;

/**
 * 블록 하나를 고정하고 아이템 효과까지 적용한 결과. 점수 계산과 아이템 등장·속도 카운트, 대전 모드 공격에 쓴다.
 *
 * @param lockOut           Lock out으로 게임 오버. true면 효과·줄 삭제를 하지 않았다
 * @param fullRowsCleared   가득 차서 지운 줄 수(기존 줄 점수 대상)
 * @param itemRowsCleared   줄 삭제(L) 아이템으로 지운 줄 수
 * @param itemCellsRemoved  아이템(L·B)으로 지운 칸 수(칸 점수 대상). L은 지운 줄의 찬 칸, B는 폭탄 칸 포함
 * @param multiplierApplied 이번 고정에서 점수 배율(M) 칸이 사라져 이번 점수 전체를 2배로 해야 하는지
 * @param attackRows        대전 모드에서 상대에게 보낼 공격 줄(위에서 아래 순서). 보내지 않으면 빈 목록
 */
public record LockResult(boolean lockOut, int fullRowsCleared, int itemRowsCleared,
                         int itemCellsRemoved, boolean multiplierApplied, List<GarbageRow> attackRows) {

    /** Lock out일 때의 결과. */
    public static final LockResult LOCK_OUT = new LockResult(true, 0, 0, 0, false);

    public LockResult {
        if (fullRowsCleared < 0 || itemRowsCleared < 0 || itemCellsRemoved < 0) {
            throw new IllegalArgumentException("고정 결과의 줄·칸 수는 음수일 수 없습니다.");
        }
        attackRows = List.copyOf(Objects.requireNonNull(attackRows, "attackRows"));
    }

    /** 공격 줄이 없는 결과. 대전 모드가 아니거나 공격 조건을 채우지 못한 경우와 같다. */
    public LockResult(boolean lockOut, int fullRowsCleared, int itemRowsCleared,
                      int itemCellsRemoved, boolean multiplierApplied) {
        this(lockOut, fullRowsCleared, itemRowsCleared, itemCellsRemoved, multiplierApplied, List.of());
    }

    /** 아이템 등장 카운트와 속도 레벨에 더할 줄 수. 꽉 찬 줄과 줄 삭제 아이템으로 지운 줄을 합친다. */
    public int totalRowsCleared() { return fullRowsCleared + itemRowsCleared; }
}
