package tetris.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import tetris.games.BattleMode;
import tetris.games.Difficulty;
import tetris.games.GameSettings;
import tetris.games.GameMode;

/** 기존 문자 메뉴와 같은 방식으로 조작하는 대전 규칙 선택기. */
final class BattleRulesView extends VBox {
    private final Text content = BattleBoardPane.text("", 20);
    private final String confirmLabel;
    private final Runnable confirm, back;
    private BattleMode mode = BattleMode.NORMAL;
    private int seconds = 180, selected;
    private Difficulty difficulty;
    private boolean opponentSelection, aiOpponent;
    BattleRulesView(GameSettings initial, boolean opponentSelection, String confirmLabel, Runnable confirm, Runnable back) {
        this(initial, confirmLabel, confirm, back);
        this.opponentSelection = opponentSelection;
        render();
    }
    BattleRulesView(GameSettings initial, String confirmLabel, Runnable confirm, Runnable back) {
        this(confirmLabel, confirm, back);
        difficulty = initial.difficulty();
        mode = initial.mode() == GameMode.ITEM ? BattleMode.ITEM : BattleMode.NORMAL;
        render();
    }
    BattleRulesView(String confirmLabel, Runnable confirm, Runnable back) {
        this.confirmLabel = confirmLabel; this.confirm = confirm; this.back = back;
        setAlignment(Pos.CENTER); setFocusTraversable(true); setStyle("-fx-background-color: #101010;");
        getChildren().add(content); render();
        setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP -> selected = Math.floorMod(selected - 1, rowCount());
                case DOWN -> selected = (selected + 1) % rowCount();
                case LEFT, RIGHT -> change(e.getCode() == javafx.scene.input.KeyCode.LEFT ? -1 : 1);
                case ENTER -> {
                    if (selected < confirmIndex()) change(1);
                    else if (selected == confirmIndex()) { if (!aiOpponent) confirm.run(); }
                    else back.run();
                }
                case ESCAPE -> { if (back != null) back.run(); }
                default -> { return; }
            }
            render(); e.consume();
        });
        content.setOnMouseClicked(e -> requestFocus());
    }
    private void change(int direction) {
        if (opponentSelection && selected == 2 + offset()) aiOpponent = !aiOpponent;
        if (difficulty != null && selected == 0) difficulty = Difficulty.values()[Math.floorMod(difficulty.ordinal() + direction, Difficulty.values().length)];
        if (selected == offset()) mode = BattleMode.values()[Math.floorMod(mode.ordinal() + direction, BattleMode.values().length)];
        if (selected == 1 + offset() && mode == BattleMode.TIMED) seconds = stepSeconds(seconds, direction);
    }
    static int stepSeconds(int seconds, int direction) {
        if (direction > 0) return Math.min(86400, (seconds / 30 + 1) * 30);
        return Math.max(1, ((seconds - 1) / 30) * 30);
    }
    private void render() {
        var rows = new java.util.ArrayList<String>();
        if (difficulty != null) rows.add("난이도: < " + difficulty.getLabel() + " >");
        rows.add("대전 유형: < " + mode + " >");
        rows.add("제한 시간: " + (mode == BattleMode.TIMED ? "< " + seconds + "초 >" : "시간제한 모드에서 설정"));
        if (opponentSelection) rows.add("상대: < " + (aiOpponent ? "AI (연동 준비 중)" : "사람 (오른쪽 2P)") + " >");
        rows.add(confirmLabel);
        if (back != null) rows.add("돌아가기");
        var text = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) text.append(i == selected ? "> " : "  ").append(rows.get(i)).append("\n\n");
        content.setText(text + "↑/↓: 선택  ←/→: 변경  Enter: 실행"
                + (aiOpponent ? "\n\nAI 구현 후 연결 예정입니다. 현재 AI 대전은 시작할 수 없습니다." : ""));
    }
    BattleMode mode() { return mode; }
    int seconds() { return seconds; }
    Difficulty difficulty() { return difficulty; }
    private int offset() { return difficulty == null ? 0 : 1; }
    private int confirmIndex() { return 2 + offset() + (opponentSelection ? 1 : 0); }
    private int rowCount() { return confirmIndex() + (back == null ? 1 : 2); }
}
