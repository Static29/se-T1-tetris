package tetris.games;

import java.io.*;
import java.util.*;
import tetris.block.Block;
import tetris.board.*;

/** 전송 전용 화면 데이터. Java 객체 역직렬화 없이 크기를 제한하여 읽는다. */
public record BattleFrame(String name, GameBoardSnapshot board, long score, GameState state,
                          List<GarbageRow> pending, String next, String hold) {
    public BattleFrame {
        if (!BattleMatch.PROFILES.contains(name)) throw new IllegalArgumentException("알 수 없는 프로필");
        Objects.requireNonNull(board); Objects.requireNonNull(state);
        pending = List.copyOf(pending);
        if (score < 0 || pending.size() > 10 || next.length() != 16 || hold.length() != 16)
            throw new IllegalArgumentException("잘못된 화면 데이터");
    }
    public static BattleFrame of(Game game, String name) {
        return new BattleFrame(name, game.getBoardSnapshot(), game.getScore(), game.getState(),
                game.getPendingRows(), preview(game.getNextBlock()), preview(game.getHeldBlock()));
    }
    private static String preview(Block block) {
        char[] cells = new char[16]; Arrays.fill(cells, '.');
        if (block != null) for (int[] cell : block.getCells())
            cells[cell[1] * 4 + cell[0]] = block.getCellSymbol(cell[0], cell[1]);
        return new String(cells);
    }
    public String encode() {
        try {
            var bytes = new ByteArrayOutputStream(); var out = new DataOutputStream(bytes);
            out.writeInt(1); out.writeUTF(name); out.writeLong(score); out.writeUTF(state.name());
            var cells = board.cells(); var symbols = board.symbols();
            out.writeInt(cells.length); out.writeInt(cells[0].length);
            for (int r = 0; r < cells.length; r++) for (int c = 0; c < cells[r].length; c++) {
                out.writeUTF(cells[r][c].name()); out.writeChar(symbols[r][c]);
            }
            out.writeInt(board.ghostCells().size());
            for (Position p : board.ghostCells()) { out.writeInt(p.row()); out.writeInt(p.col()); }
            out.writeInt(board.flashingRows().size());
            for (int row : board.flashingRows()) out.writeInt(row);
            out.writeBoolean(board.flashOn()); out.writeUTF(encodeRows(pending));
            out.writeUTF(next); out.writeUTF(hold);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    public static BattleFrame decode(String payload) {
        if (payload.length() > 16000) throw new IllegalArgumentException("화면 데이터가 너무 큽니다.");
        try {
            var in = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(payload)));
            if (in.readInt() != 1) throw new IOException("Unknown frame version");
            String name = in.readUTF(); long score = in.readLong(); GameState state = GameState.valueOf(in.readUTF());
            int rows = in.readInt(), columns = in.readInt();
            if (rows != Board.DEFAULT_VISIBLE_ROWS || columns != Board.DEFAULT_COLUMNS) throw new IOException("Board size");
            Cell[][] cells = new Cell[rows][columns]; char[][] symbols = new char[rows][columns];
            for (int r = 0; r < rows; r++) for (int c = 0; c < columns; c++) {
                cells[r][c] = Cell.valueOf(in.readUTF()); symbols[r][c] = in.readChar();
                if (".OLFBM".indexOf(symbols[r][c]) < 0) throw new IOException("Cell symbol");
            }
            int count = bounded(in.readInt(), 16); var ghost = new ArrayList<Position>();
            for (int i = 0; i < count; i++) ghost.add(new Position(bounded(in.readInt(), rows - 1), bounded(in.readInt(), columns - 1)));
            count = bounded(in.readInt(), rows); var flashing = new HashSet<Integer>();
            for (int i = 0; i < count; i++) flashing.add(bounded(in.readInt(), rows - 1));
            boolean flashOn = in.readBoolean(); var pending = decodeRows(in.readUTF());
            String next = in.readUTF(), hold = in.readUTF();
            if (in.available() != 0) throw new IOException("Trailing data");
            return new BattleFrame(name, new GameBoardSnapshot(cells, symbols, ghost, flashing, flashOn),
                    score, state, pending, next, hold);
        } catch (IOException | IllegalArgumentException e) { throw new IllegalArgumentException("잘못된 상대 화면 데이터", e); }
    }
    private static int bounded(int value, int maximum) throws IOException {
        if (value < 0 || value > maximum) throw new IOException("Out of range");
        return value;
    }
    public static String encodeRows(List<GarbageRow> rows) {
        return String.join("/", rows.stream().map(row -> {
            var s = new StringBuilder();
            for (int c = 0; c < row.getColumns(); c++) s.append(row.isFilled(c) ? '1' : '0');
            return s.toString();
        }).toList());
    }
    public static List<GarbageRow> decodeRows(String text) {
        if (text.isEmpty()) return List.of();
        if (!text.matches("[01]{10}(/[01]{10}){0,9}")) throw new IllegalArgumentException("잘못된 공격 줄");
        return Arrays.stream(text.split("/")).map(row -> {
            boolean[] filled = new boolean[10];
            for (int c = 0; c < 10; c++) filled[c] = row.charAt(c) == '1';
            return new GarbageRow(filled);
        }).toList();
    }
}
