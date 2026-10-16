package tetris.ui;

import java.util.List;
import java.util.UUID;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import tetris.score.LeaderboardEntry;
import tetris.games.GameMode;

public final class LeaderboardView extends VBox {
    private GameMode mode;
    public LeaderboardView(List<LeaderboardEntry> entries, UUID recentId, String message, Runnable back) {
        this(entries, recentId, message, GameMode.CLASSIC, back);
    }
    public LeaderboardView(List<LeaderboardEntry> entries, UUID recentId, String message, GameMode initialMode, Runnable back) {
        mode = initialMode;
        Text content = new Text();
        Runnable render = () -> content.setText(format(entries, recentId, mode) + "\n" + message
                + "\n← / →: Classic / Item 전환\nEnter / Esc: 메뉴로 돌아가기");
        render.run();
        content.setFont(Font.font("Monospaced", 18));
        content.setFill(Color.WHITESMOKE);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #101010;");
        setFocusTraversable(true);
        getChildren().add(content);
        setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case LEFT, RIGHT -> {
                    mode = mode == GameMode.CLASSIC ? GameMode.ITEM : GameMode.CLASSIC;
                    render.run();
                    event.consume();
                }
                case ENTER, ESCAPE -> { back.run(); event.consume(); }
                default -> { }
            }
        });
    }
    public static String format(List<LeaderboardEntry> entries, UUID recentId) {
        return format(entries, recentId, GameMode.CLASSIC);
    }
    public static String format(List<LeaderboardEntry> entries, UUID recentId, GameMode mode) {
        entries = entries.stream().filter(e -> e.mode() == mode)
                .sorted(java.util.Comparator.comparingLong(LeaderboardEntry::score).reversed()).limit(20).toList();
        StringBuilder text = new StringBuilder("===== " + mode.getLabel() + " · TOP 20 =====\n\n  등수  이름  난이도      점수\n");
        if (entries.isEmpty()) text.append("\n아직 등록된 기록이 없습니다.\n");
        for (int i = 0; i < Math.min(20, entries.size()); i++) {
            var entry = entries.get(i);
            text.append(String.format("%s %2d    %s   %-6s  %8d%n",
                    entry.id().equals(recentId) ? ">" : " ", i + 1, entry.name(), entry.difficulty().getLabel(), entry.score()));
        }
        return text.toString();
    }
}
