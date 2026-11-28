package tetris.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import tetris.block.TetrominoType;
import tetris.block.Block;

/** Next와 Hold가 함께 사용하는 문자 미리보기. 블록 데이터는 읽기만 한다. */
public class PiecePreviewView extends VBox {
    private final Text shape = new Text();
    private final javafx.scene.canvas.Canvas graphics = new javafx.scene.canvas.Canvas(92, 92);
    private boolean graphical;
    public void setGraphical(boolean enabled) {
        graphical = enabled;
        shape.setVisible(!enabled); shape.setManaged(!enabled);
        graphics.setVisible(enabled); graphics.setManaged(enabled);
    }

    public PiecePreviewView(String title) {
        super(8);
        setAlignment(Pos.TOP_CENTER);
        Text heading = new Text(title);
        heading.setFont(Font.font("Monospaced", 18));
        heading.setFill(Color.WHITESMOKE);
        shape.setFont(Font.font("Monospaced", FontWeight.BOLD, 18));
        getChildren().addAll(heading, shape, graphics); setGraphical(false);
        showPiece(null);
    }

    public void showPiece(TetrominoType type) {
        showPiece(type, false);
    }

    public void showPiece(TetrominoType type, boolean colorBlindMode) {
        showBlock(type == null ? null : new Block(type), colorBlindMode);
    }

    public void showBlock(Block block, boolean colorBlindMode) {
        shape.setText(formatBlock(block));
        shape.setFill(block == null ? Color.GRAY : block.getType() == null ? Color.WHITESMOKE
                : Color.web(block.getType().getColorHex(colorBlindMode)));
        if (graphical) {
            char[] symbols = new char[16]; java.util.Arrays.fill(symbols, '.');
            if (block != null) for (int[] cell : block.getCells()) symbols[cell[1] * 4 + cell[0]] = block.getCellSymbol(cell[0], cell[1]);
            BlockGraphics.preview(graphics, new String(symbols), (Color) shape.getFill());
        }
    }

    /** 일반 칸은 O로 통일하며, {x,y}를 [row=y][col=x]로 변환한다. */
    public static String format(TetrominoType type) {
        return formatBlock(type == null ? null : new Block(type));
    }

    public static String formatBlock(Block block) {
        char[][] grid = new char[4][4];
        for (char[] row : grid) {
            java.util.Arrays.fill(row, '.');
        }
        if (block != null) {
            for (int[] cell : block.getCells()) {
                grid[cell[1]][cell[0]] = block.getCellSymbol(cell[0], cell[1]);
            }
        }
        StringBuilder text = new StringBuilder("+---------+\n");
        for (char[] row : grid) {
            text.append("| ");
            for (char cell : row) {
                text.append(cell).append(' ');
            }
            text.append("|\n");
        }
        return text.append("+---------+").toString();
    }
}
