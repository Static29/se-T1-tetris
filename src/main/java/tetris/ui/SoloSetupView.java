package tetris.ui;

import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import tetris.games.*;

/** 일반 게임 진입 후 이번 게임의 규칙을 선택한다. 공통 환경·키 설정은 보존한다. */
final class SoloSetupView extends VBox {
    private final Text content = BattleBoardPane.text("", 22);
    private Difficulty difficulty;
    private GameMode mode;
    private int selected;
    SoloSetupView(GameSettings base, Consumer<GameSettings> start, Runnable back) {
        difficulty = base.difficulty(); mode = base.mode();
        setAlignment(Pos.CENTER); setFocusTraversable(true); setStyle("-fx-background-color: #101010;");
        getChildren().add(content); render();
        setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP -> selected = Math.floorMod(selected - 1, 4);
                case DOWN -> selected = (selected + 1) % 4;
                case LEFT, RIGHT -> change(e.getCode() == javafx.scene.input.KeyCode.LEFT ? -1 : 1);
                case ENTER -> {
                    if (selected < 2) change(1);
                    else if (selected == 2) start.accept(new GameSettings(difficulty, base.colorBlindMode(), mode,
                            base.screenSize(), base.keyBindings(), base.playerTwoBindings()));
                    else back.run();
                }
                case ESCAPE -> back.run();
                default -> { return; }
            }
            render(); e.consume();
        });
    }
    private void change(int direction) {
        if (selected == 0) difficulty = Difficulty.values()[Math.floorMod(difficulty.ordinal() + direction, Difficulty.values().length)];
        if (selected == 1) mode = GameMode.values()[Math.floorMod(mode.ordinal() + direction, GameMode.values().length)];
    }
    private void render() {
        var text = new StringBuilder("===== 일반 게임 =====\n\n");
        String[] rows = {"난이도: < " + difficulty.getLabel() + " >", "게임 모드: < " + mode.getLabel() + " >", "게임 시작", "돌아가기"};
        for (int i = 0; i < rows.length; i++) text.append(i == selected ? "> " : "  ").append(rows[i]).append("\n\n");
        content.setText(text + "↑/↓: 선택  ←/→: 변경  Enter: 실행\nEsc: 메인 메뉴\n\n조작은 1P 키 설정을 사용합니다.");
    }
}
