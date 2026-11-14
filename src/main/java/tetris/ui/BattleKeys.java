package tetris.ui;

import java.util.*;
import javafx.scene.input.KeyCode;
import tetris.games.*;

/** 저장된 1P·2P 설정을 로컬 입력과 화면 안내에 동일하게 사용한다. */
final class BattleKeys {
    static Optional<GameAction> action(GameSettings settings, int player, KeyCode key) {
        return bindings(settings, player).actionFor(key.name()).filter(a -> a != GameAction.PAUSE);
    }
    private static KeyBindings bindings(GameSettings settings, int player) {
        if (player < 0 || player > 1) throw new IllegalArgumentException("로컬 플레이어 번호 오류");
        return player == 0 ? settings.keyBindings() : settings.playerTwoBindings();
    }
    static String help(GameSettings settings) {
        var text = new StringBuilder();
        for (int player = 0; player < 2; player++) {
            var keys = bindings(settings, player);
            text.append(player == 0 ? "왼쪽 1P: " : "오른쪽 2P: ");
            text.append(label(keys, GameAction.LEFT)).append('/').append(label(keys, GameAction.RIGHT)).append(" 이동  ")
                    .append(label(keys, GameAction.ROTATE)).append(" 회전  ")
                    .append(label(keys, GameAction.SOFT_DROP)).append(" 하강  ")
                    .append(label(keys, GameAction.HARD_DROP)).append(" 즉시 낙하  ")
                    .append(label(keys, GameAction.HOLD)).append(" Hold");
            if (player == 0) text.append('\n');
        }
        return text.toString();
    }
    private static String label(KeyBindings keys, GameAction action) {
        return switch (keys.key(action)) {
            case "LEFT" -> "←"; case "RIGHT" -> "→"; case "UP" -> "↑"; case "DOWN" -> "↓";
            default -> KeyCode.valueOf(keys.key(action)).getName().replace("Numpad ", "Num");
        };
    }
}
