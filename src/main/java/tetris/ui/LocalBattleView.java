package tetris.ui;

import java.util.*;
import javafx.animation.AnimationTimer;
import javafx.geometry.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import tetris.games.*;
import tetris.ai.AiDifficulty;
import tetris.ai.ComputerOpponent;

public final class LocalBattleView extends VBox implements AutoCloseable {
    private final BattleMatch match;
    private final ComputerOpponent computer;
    private boolean closed;
    private final List<BattleBoardPane> boards = new ArrayList<>();
    private final Text status = BattleBoardPane.text("", 22);
    private final AnimationTimer timer;
    private final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
    public LocalBattleView(BattleMatch match, GameSettings settings, Runnable onBack) {
        this(match, settings, null, onBack);
    }
    public LocalBattleView(BattleMatch match, GameSettings settings, AiDifficulty aiDifficulty, Runnable onBack) {
        super(16); this.match = match;
        if (match.games().size() != 2) throw new IllegalArgumentException("로컬 대전은 1:1만 지원합니다.");
        computer = aiDifficulty == null ? null : new ComputerOpponent(match, 1, aiDifficulty);
        setAlignment(Pos.TOP_CENTER); setPadding(new Insets(24)); setStyle("-fx-background-color: #101010;"); setFocusTraversable(true);
        HBox row = new HBox(24); row.setAlignment(Pos.CENTER);
        for (int i = 0; i < match.games().size(); i++) { var pane = new BattleBoardPane(i == 0 ? "왼쪽 1P" : computer == null ? "오른쪽 2P" : "오른쪽 AI · " + aiDifficulty.getLabel()); boards.add(pane); row.getChildren().add(pane); }
        String help = computer == null ? BattleKeys.help(settings)
                : BattleKeys.help(settings).split("\n", 2)[0] + "\n오른쪽 AI: 자동 조작 · 키보드 입력 없음";
        Text keys = BattleBoardPane.text(help
                + "\n2줄 이상 동시 삭제 → 공격 / 받은 공격은 현재 블록 고정 후 반영\nEsc: 대전 나가기", 16);
        keys.setWrappingWidth(800);
        getChildren().addAll(status, row, keys);
        timer = new AnimationTimer() {
            long previous;
            @Override public void handle(long now) {
                if (previous != 0) advanceTime(Math.min(250, (now - previous) / 1_000_000));
                previous = now; refresh(settings.colorBlindMode());
            }
        };
        sceneProperty().addListener((o, before, after) -> { if (after == null) timer.stop(); else timer.start(); });
        focusedProperty().addListener((o, before, after) -> { if (!after) held.clear(); });
        setOnKeyReleased(e -> held.remove(e.getCode()));
        setOnKeyPressed(e -> {
            if (closed) return;
            if (e.getCode() == KeyCode.ESCAPE) { close(); onBack.run(); e.consume(); return; }
            boolean repeated = !held.add(e.getCode());
            for (int i = 0; i < boards.size(); i++) {
                if (computer != null && i == computer.player()) continue;
                var action = BattleKeys.action(settings, i, e.getCode());
                if (action.isPresent() && (!repeated || action.get().isRepeatable())) match.act(i, action.get());
            }
            refresh(settings.colorBlindMode()); e.consume();
        });
        refresh(settings.colorBlindMode());
        boards.forEach(pane -> pane.setGraphical(settings.graphical()));
        refresh(settings.colorBlindMode());
    }
    void advanceTime(long millis) {
        if (closed) return;
        match.advanceTime(millis);
        if (computer != null) computer.advanceTime(millis);
    }
    private void refresh(boolean colorBlind) {
        String result = computer == null || match.winners().size() != 1 ? match.result()
                : match.winners().getFirst() == computer.player() ? "AI 승리" : "왼쪽 1P 승리";
        status.setText(match.finished() ? result + " · Esc: 돌아가기"
                : match.mode() + (match.mode() == BattleMode.TIMED ? " · 남은 시간 " + (match.remainingMillis() + 999) / 1000 + "초" : ""));
        for (int i = 0; i < boards.size(); i++) {
            var game = match.games().get(i);
            boards.get(i).render(BattleFrame.of(game, computer != null && i == computer.player() ? "AI" : BattleMatch.PROFILES.get(i)), colorBlind);
            boards.get(i).renderProgress(game);
        }
    }
    public double contentWidth() { return 440 * boards.size(); }
    @Override public void close() { closed = true; timer.stop(); }
}
