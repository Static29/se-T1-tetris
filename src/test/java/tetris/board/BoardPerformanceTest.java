package tetris.board;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 비기능 요구사항 검증: 반복 키 입력에도 즉시 반응해야 한다(Requirements 1).
 * 화면 한 프레임(60fps 기준 약 16ms) 안에 보드 판정이 충분히 끝나는지 넉넉한 기준으로 확인한다.
 * 느린 PC나 CI에서도 흔들리지 않도록, 기준은 실제 측정값보다 훨씬 크게 잡는다.
 */
class BoardPerformanceTest {
    private static final int PIECE_COUNT = 1_000;
    private static final int MOVES_PER_PIECE = 10;
    private static final int REPEAT_COUNT = 1_000;
    private static final Duration WHOLE_GAME_LIMIT = Duration.ofSeconds(1);
    private static final Duration ONE_FRAME = Duration.ofMillis(16);

    /** O 블록(2×2)을 왼쪽 위 (row, col) 기준으로 만든다. */
    private static List<Position> square(int row, int col) {
        return List.of(new Position(row, col), new Position(row, col + 1),
                new Position(row + 1, col), new Position(row + 1, col + 1));
    }

    private static List<Position> moved(List<Position> positions, int dRow, int dCol) {
        List<Position> result = new ArrayList<>(positions.size());
        for (Position position : positions) {
            result.add(position.offset(dRow, dCol));
        }
        return result;
    }

    /**
     * 블록 1,000개 분량의 한 게임을 흉내 낸다. 블록마다 생성 검사, 좌우 이동 판정 10번,
     * 바닥까지 한 칸씩 낙하 판정, 고정, Lock out 검사, 줄 삭제를 한다.
     * 1초 안에 끝나면 블록 하나당 평균 1ms 미만이며, 한 프레임(16ms)보다 훨씬 짧다.
     */
    @Test
    void thousandPiecesOfMovesLocksAndClearsFinishWithinOneSecond() {
        Board board = new Board();
        int[] clearedLines = new int[1];
        assertTimeout(WHOLE_GAME_LIMIT, () -> {
            for (int piece = 0; piece < PIECE_COUNT; piece++) {
                // Q3 결정: 화면에 보이는 첫 줄(버퍼 바로 아래)에서 생성한다.
                List<Position> current = square(board.getBufferRows(), 4);
                assertFalse(board.isBlockOut(current));
                for (int move = 0; move < MOVES_PER_PIECE; move++) {
                    List<Position> next = moved(current, 0, move % 2 == 0 ? -1 : 1);
                    if (board.canPlace(next)) {
                        current = next;
                    }
                }
                // 열 0, 2, 4, 6, 8을 돌아가며 채워서 5개마다 2줄이 지워지게 한다.
                current = moved(current, 0, (piece % 5) * 2 - current.get(0).col());
                List<Position> below = moved(current, 1, 0);
                while (board.canPlace(below)) {
                    current = below;
                    below = moved(current, 1, 0);
                }
                board.lock(current, Cell.O);
                assertFalse(board.isLockOut(current));
                clearedLines[0] += board.clearFullRows().size();
            }
        });
        // 작업이 실제로 수행됐는지 확인한다(1,000개 / 5개 × 2줄).
        assertEquals(PIECE_COUNT / 5 * 2, clearedLines[0]);
    }

    /**
     * 가장 무거운 판정인 4줄 동시 삭제와 보드 복사를 1,000번 반복해도 1초 안에 끝나야 한다.
     * 한 번에 평균 1ms 미만이므로 한 프레임(16ms)보다 훨씬 짧다.
     */
    @Test
    void thousandFourLineClearsAndCopiesFinishWithinOneSecond() {
        assertTrue(WHOLE_GAME_LIMIT.dividedBy(REPEAT_COUNT).compareTo(ONE_FRAME) < 0);
        assertTimeout(WHOLE_GAME_LIMIT, () -> {
            for (int i = 0; i < REPEAT_COUNT; i++) {
                Board board = new Board();
                for (int row = 18; row <= 21; row++) {
                    for (int col = 0; col < board.getColumns(); col += 2) {
                        board.lock(square(row, col).subList(0, 2), Cell.I);
                    }
                }
                Board copy = board.copy();
                assertEquals(4, board.clearFullRows().size());
                assertEquals(4, copy.findFullRows().size());
            }
        });
    }
}
