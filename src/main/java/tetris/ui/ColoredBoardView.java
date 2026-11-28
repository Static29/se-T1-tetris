package tetris.ui;

import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import tetris.block.TetrominoType;
import tetris.board.Cell;
import tetris.board.Position;
import java.util.List;
import java.util.Set;

/** 각 칸의 문자와 색을 분리해서 표시한다. 내부 Cell 종류는 그대로 유지한다. */
public final class ColoredBoardView extends VBox {
    private final Text[][] cells;
    private final BoardDimensions dimensions;
    private final javafx.scene.canvas.Canvas graphics;
    private final List<javafx.scene.Node> textRows;
    private boolean graphical;
    public void setGraphical(boolean enabled) {
        if (graphical == enabled) return;
        graphical = enabled;
        if (enabled) getChildren().setAll(graphics); else getChildren().setAll(textRows);
    }
    public ColoredBoardView() {
        this(BoardDimensions.DEFAULT.rows(), BoardDimensions.DEFAULT.columns());
    }
    public ColoredBoardView(int rows, int columns) {
        dimensions = new BoardDimensions(rows, columns);
        cells = new Text[rows][columns];
        getChildren().add(label(dimensions.border()));
        for (int row = 0; row < rows; row++) {
            HBox line = new HBox(label("X "));
            for (int col = 0; col < columns; col++) {
                cells[row][col] = label(". ");
                line.getChildren().add(cells[row][col]);
            }
            line.getChildren().add(label("X"));
            getChildren().add(line);
        }
        getChildren().add(label(dimensions.border()));
        graphics = new javafx.scene.canvas.Canvas(columns * 23 + 4, rows * 23 + 4);
        textRows = List.copyOf(getChildren());
    }
    public void render(Cell[][] snapshot, boolean colorBlindMode) {
        render(snapshot, null, List.of(), Set.of(), false, colorBlindMode);
    }

    public void render(Cell[][] snapshot, char[][] symbols, List<Position> ghost,
                       Set<Integer> flashingRows, boolean flashOn, boolean colorBlindMode) {
        dimensions.validate(snapshot);
        dimensions.validate(symbols);
        Set<Position> ghostPositions = Set.copyOf(ghost);
        var g = graphics.getGraphicsContext2D();
        if (graphical) {
            g.setFill(Color.web("#0b101a")); g.fillRect(0, 0, graphics.getWidth(), graphics.getHeight());
            g.setStroke(Color.web("#62718a")); g.setLineWidth(1);
            g.strokeRect(.5, .5, graphics.getWidth() - 1, graphics.getHeight() - 1);
        }
        for (int row = 0; row < dimensions.rows(); row++) for (int col = 0; col < dimensions.columns(); col++) {
            Cell cell = snapshot[row][col];
            boolean silhouette = cell.isEmpty() && ghostPositions.contains(new Position(row, col));
            char symbol = symbols == null ? (cell.isEmpty() ? '.' : 'O') : symbols[row][col];
            boolean item = !cell.isEmpty() && "LFBM".indexOf(symbol) >= 0;
            cells[row][col].setText((silhouette && ".O".indexOf(symbol) >= 0 ? 'O' : symbol) + " ");
            Color color = silhouette ? Color.GRAY : cell.isEmpty() ? Color.DIMGRAY
                    : item ? Color.WHITE
                    : cell == Cell.WEIGHT ? Color.WHITESMOKE
                    : cell == Cell.GARBAGE ? Color.GRAY
                    : Color.web(TetrominoType.valueOf(cell.name()).getColorHex(colorBlindMode));
            cells[row][col].setFill(flashingRows.contains(row)
                    ? (flashOn ? Color.WHITE : Color.TRANSPARENT) : color);
            if (graphical) {
                double x = col * 23 + 2, y = row * 23 + 2;
                g.setStroke(Color.web("#172030")); g.setLineWidth(.5); g.strokeRect(x, y, 23, 23);
                if (flashingRows.contains(row) && !flashOn) continue;
                if (!cell.isEmpty() || silhouette) {
                    Color blockColor = flashingRows.contains(row) ? Color.WHITE
                            : silhouette ? Color.GRAY : cell == Cell.WEIGHT ? Color.WHITESMOKE
                            : cell == Cell.GARBAGE ? Color.GRAY : Color.web(TetrominoType.valueOf(cell.name()).getColorHex(colorBlindMode));
                    BlockGraphics.tile(g, x, y, 23, blockColor, symbol, silhouette);
                }
            }
        }
    }
    private Text label(String value) {
        Text text = new Text(value);
        text.setFont(Font.font("Monospaced", FontWeight.BOLD, 19));
        text.setFill(Color.WHITESMOKE);
        return text;
    }
}
