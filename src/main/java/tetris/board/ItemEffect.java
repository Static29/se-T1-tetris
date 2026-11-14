package tetris.board;

import java.util.List;
import java.util.function.Consumer;

/**
 * 블록에 붙은 아이템 한 종류가 고정 직후 보드에 주는 효과.
 * BlockLocker가 고정 → Lock out 검사를 마친 뒤, 꽉 찬 줄 삭제 전에 한 번 호출한다.
 * 새 아이템은 이 인터페이스를 구현한 클래스를 만들고 BlockLocker.effectOf·canAttack에 case를 하나씩 추가한다.
 */
interface ItemEffect {
    /**
     * @param board   블록이 이미 고정된 보드
     * @param item    아이템 칸의 보드 좌표
     * @param cell    아이템이 붙은 블록의 칸 종류(채우기 등에 쓴다)
     * @param effects 줄을 지우기 직전의 화면을 받을 곳(줄 삭제 연출)
     * @return 점수·공격 재료. 효과가 없으면 Outcome.NONE
     */
    Outcome apply(Board board, Position item, Cell cell, Consumer<LineClearEffect> effects);

    /**
     * 아이템 효과 한 번의 결과. 점수 재료는 LockResult의 아이템 부분과 같은 뜻이고,
     * clearedRows·filledCells는 대전 모드 공격 줄을 만들 때 쓴다.
     *
     * @param clearedRows       아이템으로 지운 줄 번호(지운 순서, 지우던 순간의 좌표)
     * @param cellsRemoved      아이템으로 지운 칸 수
     * @param multiplierApplied 지운 칸 중 점수 배율(M) 칸이 있었는지
     * @param filledCells       아이템이 새로 채운 칸(F). 공격 줄에서는 빈칸으로 보낸다
     */
    record Outcome(List<Integer> clearedRows, int cellsRemoved, boolean multiplierApplied,
                   List<Position> filledCells) {
        static final Outcome NONE = new Outcome(List.of(), 0, false, List.of());

        public Outcome {
            clearedRows = List.copyOf(clearedRows);
            filledCells = List.copyOf(filledCells);
        }

        /** row 한 줄을 지운 결과(L). */
        static Outcome rowCleared(int row, int cellsRemoved, boolean multiplierApplied) {
            return new Outcome(List.of(row), cellsRemoved, multiplierApplied, List.of());
        }

        /** 줄은 지우지 않고 칸만 지운 결과(B). */
        static Outcome cellsRemoved(int cellsRemoved, boolean multiplierApplied) {
            return new Outcome(List.of(), cellsRemoved, multiplierApplied, List.of());
        }

        /** 빈칸을 채운 결과(F). 점수 재료는 없다. */
        static Outcome filled(List<Position> filledCells) {
            return new Outcome(List.of(), 0, false, filledCells);
        }

        /** 아이템으로 지운 줄 수. */
        int rowsCleared() {
            return clearedRows.size();
        }
    }
}
