package tetris.ai;

import tetris.board.Board;

/**
 * 블록을 놓은 뒤의 보드가 얼마나 좋은지 점수로 매긴다. 클수록 좋다.
 * 널리 쓰이는 4가지 특징(높이 합, 지운 줄, 구멍, 울퉁불퉁함)에 가중치를 곱해 더한다.
 * 가중치는 유전 알고리즘으로 찾은 공개된 값(Yiyuan Lee, 2013)을 그대로 쓴다.
 */
final class BoardEvaluator {
    static final double HEIGHT_WEIGHT = -0.510066;
    static final double LINES_WEIGHT = 0.760666;
    static final double HOLES_WEIGHT = -0.35663;
    static final double BUMPINESS_WEIGHT = -0.184483;
    /** 버퍼 줄(화면 위)에 블록이 걸리면 곧 게임 오버이므로 다른 어떤 자리보다 나쁘게 만든다. */
    static final double TOP_OUT_PENALTY = -1000;

    private BoardEvaluator() {
    }

    /** linesCleared: 이번에 지운 줄 수. board: 줄을 지운 뒤의 보드. */
    static double score(Board board, int linesCleared) {
        int[] heights = heights(board);
        int aggregateHeight = 0;
        int bumpiness = 0;
        for (int col = 0; col < heights.length; col++) {
            aggregateHeight += heights[col];
            if (col > 0) {
                bumpiness += Math.abs(heights[col] - heights[col - 1]);
            }
        }
        double score = HEIGHT_WEIGHT * aggregateHeight + LINES_WEIGHT * linesCleared
                + HOLES_WEIGHT * holes(board) + BUMPINESS_WEIGHT * bumpiness;
        return reachesBuffer(board) ? score + TOP_OUT_PENALTY : score;
    }

    /** 열마다 가장 위 블록의 높이(바닥 = 0). */
    static int[] heights(Board board) {
        int[] heights = new int[board.getColumns()];
        for (int col = 0; col < board.getColumns(); col++) {
            for (int row = 0; row < board.getTotalRows(); row++) {
                if (!board.isEmpty(row, col)) {
                    heights[col] = board.getTotalRows() - row;
                    break;
                }
            }
        }
        return heights;
    }

    /** 위가 막힌 빈칸 수. */
    static int holes(Board board) {
        int holes = 0;
        for (int col = 0; col < board.getColumns(); col++) {
            boolean covered = false;
            for (int row = 0; row < board.getTotalRows(); row++) {
                if (!board.isEmpty(row, col)) {
                    covered = true;
                } else if (covered) {
                    holes++;
                }
            }
        }
        return holes;
    }

    private static boolean reachesBuffer(Board board) {
        for (int row = 0; row < board.getBufferRows(); row++) {
            for (int col = 0; col < board.getColumns(); col++) {
                if (!board.isEmpty(row, col)) {
                    return true;
                }
            }
        }
        return false;
    }
}
