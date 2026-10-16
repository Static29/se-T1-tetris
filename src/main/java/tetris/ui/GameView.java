package tetris.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import tetris.games.Game;
import tetris.games.GameState;

/** Game에 조작과 하강 신호를 전달하고 보드 스냅샷을 문자로 표시한다. */
public class GameView extends StackPane {
    private final Game game;
    private final Text status = createText("", 19);
    private final PiecePreviewView nextPreview = new PiecePreviewView("NEXT");
    private final PiecePreviewView holdPreview = new PiecePreviewView("HOLD");
    private final Text holdHint = createText("", 14);
    private PauseMenuView pauseMenu;
    private final Text boardText = createText("", 19);
    private final BoardView boardView = new BoardView();
    private final Timeline gravity;
    private final InitialsEntryView.SubmitAction submitScore;
    private final Runnable skipScore;
    private InitialsEntryView initialsEntry;

    public GameView(Game game, Runnable onMainMenu, Runnable onExit) {
        this(game, onMainMenu, onExit, name -> onMainMenu.run(), onMainMenu, null);
    }

    public GameView(Game game, Runnable onMainMenu, Runnable onExit,
                    InitialsEntryView.SubmitAction submitScore, Runnable skipScore, Runnable onSettings) {
        this.game = game;
        this.submitScore = submitScore;
        this.skipScore = skipScore;
        gravity = new Timeline(new KeyFrame(Duration.millis(700), event -> {
            game.tick();
            refresh();
        }));
        gravity.setCycleCount(Timeline.INDEFINITE);
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) gravity.stop();
            else refresh();
        });
        setPadding(new Insets(24));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #101010;");
        setFocusTraversable(true);

        VBox holdArea = new VBox(12, holdPreview, holdHint);
        holdArea.setAlignment(Pos.TOP_CENTER);
        VBox nextArea = new VBox(24, nextPreview, status);
        nextArea.setAlignment(Pos.TOP_CENTER);
        HBox playArea = new HBox(24, holdArea, boardText, nextArea);
        playArea.setAlignment(Pos.CENTER);
        VBox gameContent = new VBox(16, createText("===== TETRIS =====", 22), playArea,
                createText("← →: 이동   ↑: 회전   ↓: 하강   Space: 즉시 낙하", 16),
                createText("C: Hold   Esc: 일시정지", 16));
        gameContent.setAlignment(Pos.CENTER);
        getChildren().add(gameContent);

        setOnKeyPressed(event -> {
            if (initialsEntry != null) {
                initialsEntry.getOnKeyPressed().handle(event);
            } else if (pauseMenu != null) {
                // 포커스가 게임 화면에 남아 있어도 메뉴만 입력을 처리한다.
                pauseMenu.getOnKeyPressed().handle(event);
            } else if (event.getCode() == KeyCode.ESCAPE && game.getState() == GameState.RUNNING) {
                game.pause();
                refresh();
                pauseMenu = new PauseMenuView(this::resumeGame, onMainMenu, onExit, onSettings);
                getChildren().add(pauseMenu);
                pauseMenu.requestFocus();
                event.consume();
            } else if (game.getState() == GameState.GAME_OVER
                    && event.getCode() == KeyCode.ENTER) {
                onMainMenu.run();
                event.consume();
            } else if (game.getState() == GameState.RUNNING) {
                switch (event.getCode()) {
                    case LEFT -> game.moveLeft();
                    case RIGHT -> game.moveRight();
                    case UP -> game.rotateClockwise();
                    case DOWN -> game.softDrop();
                    case SPACE -> game.hardDrop();
                    case C -> game.hold();
                    default -> { return; }
                }
                refresh();
                event.consume();
            }
        });
        refresh();
    }

    private void resumeGame() {
        getChildren().remove(pauseMenu);
        pauseMenu = null;
        game.resume();
        refresh();
        requestFocus();
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
        status.setText("상태: " + state + "\n\n모드: " + game.getSettings().mode().getLabel()
                + "\n\n난이도: " + game.getDifficulty().getLabel()
                + "\n\n점수: " + game.getScore() + "\n\n삭제 줄: " + game.getClearedLines()
                + "\n\n현재: " + current);
        boardText.setText(boardView.render(game.getVisibleCells()));
        if (game.getState() == GameState.RUNNING && getScene() != null) gravity.play();
        else if (game.getState() == GameState.PAUSED) gravity.pause();
        else gravity.stop();
        nextPreview.showPiece(game.getNextType());
        holdPreview.showPiece(game.getHeldType());
        holdHint.setText(game.canHold() ? "C: 보관 / 교환" :
                game.getState() == GameState.RUNNING
                        ? "고정 후 사용 가능" : "진행 중에만 사용");
        if (game.getState() == GameState.GAME_OVER && initialsEntry == null) {
            initialsEntry = new InitialsEntryView(game.getScore(), game.getDifficulty(), submitScore, skipScore);
            getChildren().add(initialsEntry);
            initialsEntry.requestFocus();
        }
    }

    private static Text createText(String value, int size) {
        Text text = new Text(value);
        text.setFont(Font.font("Monospaced", size));
        text.setFill(Color.WHITESMOKE);
        return text;
    }
}
