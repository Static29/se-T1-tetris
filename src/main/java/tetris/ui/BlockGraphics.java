package tetris.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;

/** 보드와 미리보기에서 공유하는 블록 표현. 게임 상태는 변경하지 않는다. */
final class BlockGraphics {
    private BlockGraphics() { }
    static void tile(GraphicsContext g, double x, double y, double size, Color color, char symbol, boolean ghost) {
        if (ghost) {
            g.setFill(Color.rgb(170, 180, 200, .13)); g.fillRect(x + 1, y + 1, size - 2, size - 2);
            g.setStroke(Color.GRAY); g.setLineWidth(1.5); g.strokeRect(x + 2, y + 2, size - 4, size - 4);
        } else {
            g.setFill(color.darker()); g.fillRect(x + 1, y + 1, size - 2, size - 2);
            g.setFill(color); g.fillRect(x + 3, y + 3, size - 6, size - 6);
            g.setFill(Color.rgb(255, 255, 255, .35)); g.fillRect(x + 2, y + 1, size - 4, 2);
        }
        if ("LFBM".indexOf(symbol) >= 0) {
            g.setFill(ghost ? Color.LIGHTGRAY : Color.rgb(10, 15, 25, .9));
            if (!ghost) g.fillRoundRect(x + 3, y + 3, size - 6, size - 6, 5, 5);
            g.setFill(ghost ? Color.LIGHTGRAY : Color.WHITE);
            g.setFont(Font.font("Monospaced", FontWeight.BOLD, size * .66));
            g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.CENTER);
            g.fillText(String.valueOf(symbol), x + size / 2, y + size / 2);
        }
    }
    static void preview(Canvas canvas, String symbols, Color color) {
        var g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        double size = canvas.getWidth() / 4;
        for (int i = 0; i < 16; i++) if (symbols.charAt(i) != '.')
            tile(g, (i % 4) * size, (i / 4) * size, size, color, symbols.charAt(i), false);
    }
}
