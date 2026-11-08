package tetris.board;

import java.util.function.Consumer;

/**
 * 블록에 붙은 아이템 한 종류가 고정 직후 보드에 주는 효과.
 * BlockLocker가 고정 → Lock out 검사를 마친 뒤, 꽉 찬 줄 삭제 전에 한 번 호출한다.
 * 새 아이템은 이 인터페이스를 구현한 클래스를 만들고 BlockLocker.effectOf에 case를 하나 추가한다.
 */
interface ItemEffect {
    /**
     * @param board   블록이 이미 고정된 보드
     * @param item    아이템 칸의 보드 좌표
     * @param cell    아이템이 붙은 블록의 칸 종류(채우기 등에 쓴다)
     * @param effects 줄을 지우기 직전의 화면을 받을 곳(줄 삭제 연출)
     * @return 점수 재료. 효과가 없으면 Outcome.NONE
     */
    Outcome apply(Board board, Position item, Cell cell, Consumer<LineClearEffect> effects);

    /**
     * 아이템 효과 한 번의 점수 재료. LockResult의 아이템 부분과 같은 뜻이다.
     *
     * @param rowsCleared       아이템으로 지운 줄 수
     * @param cellsRemoved      아이템으로 지운 칸 수
     * @param multiplierApplied 지운 칸 중 점수 배율(M) 칸이 있었는지
     */
    record Outcome(int rowsCleared, int cellsRemoved, boolean multiplierApplied) {
        static final Outcome NONE = new Outcome(0, 0, false);
    }
}
