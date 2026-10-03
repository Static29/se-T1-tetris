package tetris.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import tetris.block.TetrominoType;

/** Next와 Hold가 함께 사용하는 문자 미리보기. 블록 데이터는 읽기만 한다. */
public class PiecePreviewView extends VBox {
    private final Text shape = new Text();

    public PiecePreviewView(String title) {
        super(8);
        setAlignment(Pos.TOP_CENTER);
        Text heading = new Text(title);
        heading.setFont(Font.font("Monospaced", 18));
        heading.setFill(Color.WHITESMOKE);
        shape.setFont(Font.font("Monospaced", 18));
        getChildren().addAll(heading, shape);
        showPiece(null);
    }

    public void showPiece(TetrominoType type) {
        shape.setText(format(type));
        shape.setFill(type == null ? Color.GRAY : switch (type) {
            case I -> Color.CYAN;
            case O -> Color.YELLOW;
            case T -> Color.VIOLET;
            case S -> Color.LIME;
            case Z -> Color.SALMON;
            case J -> Color.CORNFLOWERBLUE;
            case L -> Color.ORANGE;
        });
    }

    /** 종류별 글자로 구분하며, {x,y}를 [row=y][col=x]로 변환한다. */
    public static String format(TetrominoType type) {
        char[][] grid = new char[4][4];
        for (char[] row : grid) {
            java.util.Arrays.fill(row, '.');
        }
        if (type != null) {
            for (int[] cell : type.getCells()) {
                grid[cell[1]][cell[0]] = type.name().charAt(0);
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
