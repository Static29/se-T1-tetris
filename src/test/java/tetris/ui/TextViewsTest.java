package tetris.ui;

import java.util.concurrent.atomic.AtomicInteger;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;
import tetris.games.Game;
import tetris.games.GameState;
import tetris.block.TetrominoType;
import static org.junit.jupiter.api.Assertions.*;

/** 창을 열지 않고 문자 메뉴의 키 이벤트와 게임 화면의 콜백을 검증한다. */
class TextViewsTest {
    @Test
    void holdKeyUpdatesBothPreviewBoxesAndRejectsRepeatedHold() {
        var queue = new java.util.ArrayDeque<>(java.util.List.of(
                TetrominoType.I, TetrominoType.T, TetrominoType.O));
        Game game = new Game(queue::removeFirst);
        game.start();
        GameView view = new GameView(game, () -> { });
        assertTrue(allText(view).contains("NEXT"));
        assertTrue(allText(view).contains(PiecePreviewView.format(TetrominoType.T)));
        press(view, KeyCode.C);
        assertEquals(TetrominoType.I, game.getHeldType());
        assertTrue(allText(view).contains(PiecePreviewView.format(TetrominoType.I)));
        assertTrue(allText(view).contains(PiecePreviewView.format(TetrominoType.O)));
        assertTrue(allText(view).contains("고정 후 사용 가능"));
        press(view, KeyCode.C);
        assertEquals(TetrominoType.T, game.getCurrentType());
        assertEquals(TetrominoType.I, game.getHeldType());
    }

    private String allText(Node node) {
        if (node instanceof Text text) {
            return text.getText() + "\n";
        }
        StringBuilder result = new StringBuilder();
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                result.append(allText(child));
            }
        }
        return result.toString();
    }

    @Test
    void arrowsWrapSelectionAndEnterInvokesTheSelectedAction() {
        AtomicInteger selected = new AtomicInteger(-1);
        MainMenuView menu = new MainMenuView(
                () -> selected.set(0), () -> selected.set(1),
                () -> selected.set(2), () -> selected.set(3));
        press(menu, KeyCode.UP);
        assertTrue(((Text) menu.getChildren().get(0)).getText().contains("> 종료"));
        press(menu, KeyCode.ENTER);
        assertEquals(3, selected.get());
        press(menu, KeyCode.DOWN);
        press(menu, KeyCode.ENTER);
        assertEquals(0, selected.get());
        press(menu, KeyCode.DOWN);
        press(menu, KeyCode.ENTER);
        assertEquals(1, selected.get());
        press(menu, KeyCode.DOWN);
        press(menu, KeyCode.ENTER);
        assertEquals(2, selected.get());
    }

    @Test
    void textGameScreenPausesResumesAndReturnsToMenu() {
        Game game = new Game();
        game.start();
        AtomicInteger returns = new AtomicInteger();
        GameView view = new GameView(game, returns::incrementAndGet);
        press(view, KeyCode.P);
        assertEquals(GameState.PAUSED, game.getState());
        press(view, KeyCode.P);
        assertEquals(GameState.RUNNING, game.getState());
        press(view, KeyCode.ESCAPE);
        assertEquals(GameState.PAUSED, game.getState());
        assertEquals(1, returns.get());
    }

    private void press(Node target, KeyCode code) {
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED,
                "", "", code, false, false, false, false));
    }
}
