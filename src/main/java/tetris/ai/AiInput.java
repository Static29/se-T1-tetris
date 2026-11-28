package tetris.ai;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import tetris.block.Block;
import tetris.board.Board;
import tetris.board.Cell;
import tetris.board.Position;
import tetris.games.Game;
import tetris.games.GameState;

/**
 * 컴퓨터 플레이어가 보는 "현재 보드 상태". 사람이 화면에서 보는 정보(보드·현재·Hold·Next 블록)만 담는다.
 *
 * @param board   고정된 칸만 있는 보드(떨어지는 블록 제외). 버퍼 줄은 비어 있다.
 * @param current 떨어지는 블록. 지금 조작할 수 없으면(게임 전·끝·줄 삭제 연출 중) null
 * @param held    Hold에 있는 블록. 없으면 null
 * @param next    다음 블록. 없으면 null
 * @param canHold 지금 Hold를 쓸 수 있는지
 */
public record AiInput(Board board, Block current, Block held, Block next, boolean canHold) {

    public AiInput {
        board = Objects.requireNonNull(board, "board").copy();
        current = current == null ? null : current.copy();
        held = held == null ? null : held.copy();
        next = next == null ? null : next.copy();
    }

    /** 보드는 매번 복사본을 돌려준다. 시뮬레이션이 원래 입력을 바꾸지 않게 하기 위해서다. */
    @Override
    public Board board() { return board.copy(); }

    @Override
    public Block current() { return current == null ? null : current.copy(); }

    @Override
    public Block held() { return held == null ? null : held.copy(); }

    @Override
    public Block next() { return next == null ? null : next.copy(); }

    /**
     * 게임의 공개 정보로 입력을 만든다. 게임 내부는 바꾸지 않는다.
     * 화면에 보이는 칸에서 떨어지는 블록 칸을 빼서 고정된 보드를 다시 만든다.
     */
    public static AiInput of(Game game) {
        Objects.requireNonNull(game, "game");
        Cell[][] visible = game.getVisibleCells();
        boolean playable = game.getState() == GameState.RUNNING && !game.isClearingLines();
        Block current = playable ? game.getCurrentBlock() : null;
        Board board = new Board(visible.length, Board.DEFAULT_BUFFER_ROWS, visible[0].length);
        Set<Position> active = new HashSet<>();
        if (current != null) {
            active.addAll(Position.fromXY(current.getBoardCells()));
        }
        for (int row = 0; row < visible.length; row++) {
            for (int col = 0; col < visible[row].length; col++) {
                Position position = new Position(row + board.getBufferRows(), col);
                if (!visible[row][col].isEmpty() && !active.contains(position)) {
                    board.lock(java.util.List.of(position), visible[row][col]);
                }
            }
        }
        return new AiInput(board, current, game.getHeldBlock(), game.getNextBlock(), playable && game.canHold());
    }
}
