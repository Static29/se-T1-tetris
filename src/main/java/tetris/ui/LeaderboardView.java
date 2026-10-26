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
import java.io.IOException;

public final class LeaderboardView extends VBox {
    @FunctionalInterface
    public interface ResetAction {
        List<LeaderboardEntry> reset(GameMode mode, boolean all) throws IOException;
    }
    private GameMode mode;
    private List<LeaderboardEntry> currentEntries;
    private String notice;
    private boolean confirming;
    private boolean resetAll;
    public LeaderboardView(List<LeaderboardEntry> entries, UUID recentId, String message, Runnable back) {
        this(entries, recentId, message, GameMode.CLASSIC, back);
    }
    public LeaderboardView(List<LeaderboardEntry> entries, UUID recentId, String message, GameMode initialMode, Runnable back) {
        this(entries, recentId, message, initialMode, back, null);
    }
    public LeaderboardView(List<LeaderboardEntry> entries, UUID recentId, String message, GameMode initialMode,
                           Runnable back, ResetAction reset) {
        mode = initialMode;
        currentEntries = List.copyOf(entries);
        notice = message;
        Text content = new Text();
        Runnable render = () -> content.setText(confirming
                ? "===== 기록 초기화 =====\n\n" + (resetAll ? "Classic + Item 전체" : mode.getLabel())
                    + " 기록을 삭제할까요?\n이 작업은 되돌릴 수 없습니다.\n\nEnter: 삭제  Esc: 취소"
                : format(currentEntries, recentId, mode) + "\n" + notice
                    + "\n← / →: Classic / Item 전환\nEnter / Esc: 메뉴로 돌아가기"
                    + (reset == null ? "" : "\nDelete: 현재 모드 초기화  R: 전체 초기화"));
        render.run();
        content.setFont(Font.font("Monospaced", 18));
        content.setFill(Color.WHITESMOKE);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #101010;");
        setFocusTraversable(true);
        getChildren().add(content);
        setOnKeyPressed(event -> {
            if (confirming) {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) confirming = false;
                else if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    try { currentEntries = reset.reset(mode, resetAll); notice = "기록을 초기화했습니다."; }
                    catch (IOException e) { notice = "초기화하지 못했습니다. 다시 시도해 주세요."; }
                    confirming = false;
                }
                render.run();
                event.consume();
                return;
            }
            switch (event.getCode()) {
                case DELETE, R -> {
                    if (reset != null) {
                        resetAll = event.getCode() == javafx.scene.input.KeyCode.R;
                        confirming = true;
                        render.run();
                    }
                    event.consume();
                }
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
