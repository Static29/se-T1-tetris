package tetris.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import tetris.games.Game;
import tetris.games.GameState;

/** 게임 데이터와 상태를 문자로 표시한다. 실제 보드와 자동 하강은 아직 연결하지 않는다. */
public class GameView extends VBox {
    private final Game game;
    private final Text status = createText("", 19);
    private final PiecePreviewView nextPreview = new PiecePreviewView("NEXT");
    private final PiecePreviewView holdPreview = new PiecePreviewView("HOLD");
    private final Text holdHint = createText("", 14);

    public GameView(Game game, Runnable onMainMenu) {
        super(16);
        this.game = game;
        setPadding(new Insets(24));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #101010;");
        setFocusTraversable(true);

        Text board = createText(new BoardView().renderEmptyBoard(), 19);
        VBox holdArea = new VBox(12, holdPreview, holdHint);
        holdArea.setAlignment(Pos.TOP_CENTER);
        VBox nextArea = new VBox(24, nextPreview, status);
        nextArea.setAlignment(Pos.TOP_CENTER);
        HBox playArea = new HBox(24, holdArea, board, nextArea);
        playArea.setAlignment(Pos.CENTER);
        getChildren().addAll(createText("===== TETRIS =====", 22), playArea,
                createText("빈 보드 미리보기 · 게임 로직 연결 예정", 16),
                createText("C: Hold   P: 일시정지 / 재개   Esc: 메뉴로 돌아가기", 16));

        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.P) {
                game.togglePause();
                refresh();
                event.consume();
            } else if (event.getCode() == KeyCode.C) {
                game.hold();
                refresh();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                game.pause();
                onMainMenu.run();
                event.consume();
            }
        });
        refresh();
    }

    /** Game 갱신 후 JavaFX Application Thread에서 호출한다. */
    public void refresh() {
        String state = switch (game.getState()) {
            case READY -> "시작 전";
            case RUNNING -> "진행 중";
            case PAUSED -> "일시정지";
            case GAME_OVER -> "게임 종료";
        };
        String current = game.getCurrentType() == null ? "—" : game.getCurrentType().name();
        status.setText("상태: " + state + "\n\n점수: —\n\n현재: " + current);
        nextPreview.showPiece(game.getNextType());
        holdPreview.showPiece(game.getHeldType());
        holdHint.setText(game.canHold() ? "C: 보관 / 교환" :
                game.getState() == GameState.RUNNING
                        ? "고정 후 사용 가능" : "진행 중에만 사용");
    }

    private static Text createText(String value, int size) {
        Text text = new Text(value);
        text.setFont(Font.font("Monospaced", size));
        text.setFill(Color.WHITESMOKE);
        return text;
    }
}
