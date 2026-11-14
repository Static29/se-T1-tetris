package tetris.ui;

import java.util.*;
import javafx.animation.AnimationTimer;
import javafx.geometry.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import tetris.games.*;

public final class LocalBattleView extends VBox implements AutoCloseable {
    private final BattleMatch match;
    private final List<BattleBoardPane> boards = new ArrayList<>();
    private final Text status = BattleBoardPane.text("", 22);
    private final AnimationTimer timer;
    private final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
    public LocalBattleView(BattleMatch match, GameSettings settings, Runnable onBack) {
        super(16); this.match = match;
        if (match.games().size() != 2) throw new IllegalArgumentException("로컬 대전은 1:1만 지원합니다.");
        setAlignment(Pos.TOP_CENTER); setPadding(new Insets(24)); setStyle("-fx-background-color: #101010;"); setFocusTraversable(true);
        HBox row = new HBox(24); row.setAlignment(Pos.CENTER);
        for (int i = 0; i < match.games().size(); i++) { var pane = new BattleBoardPane(i == 0 ? "왼쪽 1P" : "오른쪽 2P"); boards.add(pane); row.getChildren().add(pane); }
        Text keys = BattleBoardPane.text(BattleKeys.help(settings)
                + "\n2줄 이상 동시 삭제 → 공격 / 받은 공격은 현재 블록 고정 후 반영\nEsc: 대전 나가기", 16);
        keys.setWrappingWidth(800);
        getChildren().addAll(status, row, keys);
        timer = new AnimationTimer() {
            long previous;
            @Override public void handle(long now) {
                if (previous != 0) match.advanceTime(Math.min(250, (now - previous) / 1_000_000));
                previous = now; refresh(settings.colorBlindMode());
            }
        };
        sceneProperty().addListener((o, before, after) -> { if (after == null) timer.stop(); else timer.start(); });
        focusedProperty().addListener((o, before, after) -> { if (!after) held.clear(); });
        setOnKeyReleased(e -> held.remove(e.getCode()));
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) { close(); onBack.run(); e.consume(); return; }
            boolean repeated = !held.add(e.getCode());
            for (int i = 0; i < boards.size(); i++) {
                var action = BattleKeys.action(settings, i, e.getCode());
                if (action.isPresent() && (!repeated || action.get().isRepeatable())) match.act(i, action.get());
            }
            refresh(settings.colorBlindMode()); e.consume();
        });
        refresh(settings.colorBlindMode());
    }
    private void refresh(boolean colorBlind) {
        status.setText(match.finished() ? match.result() + " · Esc: 돌아가기"
                : match.mode() + (match.mode() == BattleMode.TIMED ? " · 남은 시간 " + (match.remainingMillis() + 999) / 1000 + "초" : ""));
        for (int i = 0; i < boards.size(); i++) boards.get(i).render(BattleFrame.of(match.games().get(i), BattleMatch.PROFILES.get(i)), colorBlind);
    }
    public double contentWidth() { return 440 * boards.size(); }
    @Override public void close() { timer.stop(); }
}
