package tetris.ui;

import javafx.animation.AnimationTimer;
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
import tetris.games.GameSettings;
import tetris.games.GameAction;

/** Game에 조작과 하강 신호를 전달하고 보드 스냅샷을 문자로 표시한다. */
public class GameView extends StackPane {
    private final Game game;
    private final Text status = createText("", 19);
    private final PiecePreviewView nextPreview = new PiecePreviewView("NEXT");
    private final PiecePreviewView holdPreview = new PiecePreviewView("HOLD");
    private final Text holdHint = createText("", 14);
    private PauseMenuView pauseMenu;
    private final ColoredBoardView boardView = new ColoredBoardView();
    private GameSettings controls;
    private final Text movementHint = createText("", 14);
    private final Text actionHint = createText("", 14);
    private final AnimationTimer gravity;
    private long lastFrame;
    private boolean clockRunning;
    private final InitialsEntryView.SubmitAction submitScore;
    private final Runnable skipScore;
    private InitialsEntryView initialsEntry;

    public GameView(Game game, Runnable onMainMenu, Runnable onExit) {
        this(game, onMainMenu, onExit, name -> onMainMenu.run(), onMainMenu, null);
    }

    public GameView(Game game, Runnable onMainMenu, Runnable onExit,
                    InitialsEntryView.SubmitAction submitScore, Runnable skipScore, Runnable onSettings) {
        this.game = game;
        this.controls = game.getSettings();
        this.submitScore = submitScore;
        this.skipScore = skipScore;
        gravity = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastFrame != 0) {
                    long elapsed = (now - lastFrame) / 1_000_000;
                    lastFrame += elapsed * 1_000_000;
                    game.advanceTime(elapsed);
                    refresh();
                } else lastFrame = now;
            }
        };
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) stopClock();
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
        HBox playArea = new HBox(24, holdArea, boardView, nextArea);
        playArea.setAlignment(Pos.CENTER);
        VBox gameContent = new VBox(16, createText("===== T1 TETRIS =====", 22), playArea,
                movementHint, actionHint);
        gameContent.setAlignment(Pos.CENTER);
        getChildren().add(gameContent);

        setOnKeyPressed(event -> {
            if (initialsEntry != null) {
                initialsEntry.getOnKeyPressed().handle(event);
            } else if (pauseMenu != null) {
                // 포커스가 게임 화면에 남아 있어도 메뉴만 입력을 처리한다.
                pauseMenu.getOnKeyPressed().handle(event);
            } else if (controls.keyBindings().actionFor(event.getCode().name()).orElse(null) == GameAction.PAUSE
                    && game.getState() == GameState.RUNNING) {
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
                GameAction action = controls.keyBindings().actionFor(event.getCode().name()).orElse(null);
                if (action == null) return;
                switch (action) {
                    case LEFT -> game.moveLeft();
                    case RIGHT -> game.moveRight();
                    case ROTATE -> game.rotateClockwise();
                    case SOFT_DROP -> game.softDrop();
                    case HARD_DROP -> game.hardDrop();
                    case HOLD -> game.hold();
                    default -> { return; }
                }
                refresh();
                event.consume();
            }
        });
        refresh();
    }

    /** 진행 중 난이도·모드는 유지하고 표시·입력 설정만 즉시 반영한다. */
    public void applyPreferences(GameSettings settings) {
        controls = settings;
        refresh();
    }

    public boolean suppressRepeat(KeyCode code) {
        if (game.getState() != GameState.RUNNING || pauseMenu != null || initialsEntry != null) return false;
        return controls.keyBindings().actionFor(code.name()).map(a -> !a.isRepeatable()).orElse(false);
    }

    private String key(GameAction action) {
        return KeyCode.valueOf(controls.keyBindings().key(action)).getName();
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
        status.setText("상태: " + state + "\n\n모드: " + game.getSettings().mode().getLabel()
                + "\n\n난이도: " + game.getDifficulty().getLabel()
                + "\n\n점수: " + game.getScore() + "\n\n삭제 줄: " + game.getClearedLines()
                + "\n\n속도: " + String.format(java.util.Locale.ROOT, "%.2fx", game.getSpeedMultiplier()));
        boardView.render(game.getVisibleCells(), game.getVisibleSymbols(), game.getGhostCells(),
                game.getFlashingVisibleRows(), game.isClearFlashOn(), controls.colorBlindMode());
        if (getScene() != null && (game.getState() == GameState.RUNNING
                || game.getState() == GameState.GAME_OVER && game.isClearingLines())) {
            if (!clockRunning) { clockRunning = true; gravity.start(); }
        } else stopClock();
        nextPreview.showBlock(game.getNextBlock(), controls.colorBlindMode());
        holdPreview.showBlock(game.getHeldBlock(), controls.colorBlindMode());
        movementHint.setText(key(GameAction.LEFT) + "/" + key(GameAction.RIGHT) + ": 이동   "
                + key(GameAction.ROTATE) + ": 회전   " + key(GameAction.SOFT_DROP) + ": 하강");
        actionHint.setText(key(GameAction.HARD_DROP) + ": 즉시 낙하   " + key(GameAction.HOLD)
                + ": Hold   " + key(GameAction.PAUSE) + ": 일시정지");
        holdHint.setText(game.canHold() ? key(GameAction.HOLD) + ": 보관 / 교환" :
                game.getCurrentBlock() != null && game.getCurrentBlock().getItemType() == tetris.block.ItemType.WEIGHT
                        ? "무게추는 Hold 불가" :
                game.getState() == GameState.RUNNING
                        ? "고정 후 사용 가능" : "진행 중에만 사용");
        if (game.getState() == GameState.GAME_OVER && !game.isClearingLines() && initialsEntry == null) {
            initialsEntry = new InitialsEntryView(game.getScore(), game.getDifficulty(), submitScore, skipScore);
            getChildren().add(initialsEntry);
            initialsEntry.requestFocus();
        }
    }

    private void stopClock() {
        gravity.stop();
        clockRunning = false;
        lastFrame = 0;
    }

    private static Text createText(String value, int size) {
        Text text = new Text(value);
        text.setFont(Font.font("Monospaced", size));
        text.setFill(Color.WHITESMOKE);
        return text;
    }
}
