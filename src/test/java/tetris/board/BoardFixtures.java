package tetris.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import tetris.block.Block;

/**
 * 여러 board 테스트가 같은 내용으로 따로 쓰던 도우미를 모은 곳.
 * 테스트 하나에서만 쓰는 도우미(fillRow, fillAllExcept 등)는 각 테스트에 그대로 둔다.
 */
final class BoardFixtures {
    private BoardFixtures() {
    }

    /** (row, col) 쌍을 순서대로 받아 좌표 목록을 만든다. 예: positions(0, 0, 0, 1) */
    static List<Position> positions(int... rowCols) {
        List<Position> result = new ArrayList<>();
        for (int i = 0; i < rowCols.length; i += 2) {
            result.add(new Position(rowCols[i], rowCols[i + 1]));
        }
        return result;
    }

    /** row 줄에서 fromCol ~ toCol 칸을 cell로 채운다. */
    static void fill(Board board, int row, int fromCol, int toCol, Cell cell) {
        List<Position> line = new ArrayList<>();
        for (int col = fromCol; col <= toCol; col++) {
            line.add(new Position(row, col));
        }
        board.lock(line, cell);
    }

    /** 칸 순서와 관계없이 비교하기 위해 블록의 점유 칸을 집합으로 만든다. */
    static Set<Position> occupied(Block block) {
        return Set.copyOf(Position.fromXY(block.getBoardCells()));
    }
}
