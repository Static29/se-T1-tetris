package tetris.ui;

import javafx.geometry.*;
import javafx.scene.layout.VBox;
import tetris.games.*;
import java.util.function.Consumer;

final class BattleSetupView extends VBox {
    BattleSetupView(GameSettings settings, Consumer<BattleMatch> start, Runnable back) {
        super(24); setAlignment(Pos.CENTER); setPadding(new Insets(24)); setStyle("-fx-background-color: #101010;");
        BattleRulesView rules = new BattleRulesView(settings, "대전 시작", () -> startMatch(settings, start), back);
        getChildren().addAll(BattleBoardPane.text("===== 로컬 1:1 대전 =====", 26), rules,
                BattleBoardPane.text("한 PC에서 좌우 화면으로 플레이\n한 번에 2줄 이상 삭제하면 상대를 공격합니다.\n받은 공격은 현재 블록 고정 후 아래에 추가됩니다.", 16));
        setFocusTraversable(true);
        setOnKeyPressed(e -> { if (!e.isConsumed()) rules.getOnKeyPressed().handle(e); });
    }
    private void startMatch(GameSettings settings, Consumer<BattleMatch> start) {
        BattleRulesView rules = (BattleRulesView) getChildren().get(1);
        var selected = new GameSettings(rules.difficulty(), settings.colorBlindMode(), settings.mode(),
                settings.screenSize(), settings.keyBindings(), settings.playerTwoBindings());
        start.accept(new BattleMatch(2, rules.mode(), rules.seconds(), selected));
    }
}
