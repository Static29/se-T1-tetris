package tetris.ui;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tetris.games.*;
import tetris.network.RecentServerStore;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class BattleViewsTest {
    @TempDir Path temporary;
    @BeforeAll static void startFx() throws Exception {
        var ready = new CountDownLatch(1);
        try { Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); }); }
        catch (IllegalStateException e) { ready.countDown(); }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
    }
    private static void fx(Runnable work) throws Exception {
        var future = new FutureTask<Void>(() -> { work.run(); return null; });
        Platform.runLater(future); future.get(10, TimeUnit.SECONDS);
    }
    @Test void localPlayersHaveIndependentControlsAndLayouts() throws Exception {
        fx(() -> {
            for (int count : new int[]{2}) {
                var match = new BattleMatch(count, BattleMode.NORMAL, 180, GameSettings.DEFAULT);
                var back = new AtomicBoolean();
                var view = new LocalBattleView(match, GameSettings.DEFAULT, () -> back.set(true));
                int x0 = match.games().get(0).getCurrentBlock().getX();
                int x1 = match.games().get(1).getCurrentBlock().getX();
                press(view, KeyCode.LEFT); assertEquals(x1 - 1, match.games().get(1).getCurrentBlock().getX());
                assertEquals(x0, match.games().get(0).getCurrentBlock().getX());
                press(view, KeyCode.G); press(view, KeyCode.G);
                assertNotNull(match.games().get(0).getHeldBlock());
                snapshot(view, (int) view.contentWidth(), 850, "local-" + count);
                press(view, KeyCode.ESCAPE); assertTrue(back.get()); view.close();
            }
            assertThrows(IllegalArgumentException.class, () -> new LocalBattleView(
                    new BattleMatch(3, BattleMode.NORMAL, 180, GameSettings.DEFAULT), GameSettings.DEFAULT, () -> {}));
        });
    }
    @Test void localSetupAlwaysStartsTwoPlayersAndSelectsTimedOrItemRules() throws Exception {
        fx(() -> {
            var started = new AtomicReference<BattleMatch>();
            var view = new BattleSetupView(GameSettings.DEFAULT, started::set, () -> {});
            press(view, KeyCode.RIGHT);
            press(view, KeyCode.DOWN); press(view, KeyCode.RIGHT);
            assertTrue(nodes(view, ComboBox.class).isEmpty());
            assertTrue(nodes(view, Spinner.class).isEmpty());
            press(view, KeyCode.DOWN); press(view, KeyCode.DOWN); press(view, KeyCode.ENTER);
            assertEquals(2, started.get().games().size());
            assertEquals(GameMode.ITEM, started.get().games().get(0).getSettings().mode());
            assertTrue(started.get().games().stream().allMatch(g -> g.getDifficulty() == Difficulty.HARD));
            press(view, KeyCode.UP); press(view, KeyCode.UP); press(view, KeyCode.RIGHT);
            press(view, KeyCode.DOWN);
            for (int i = 0; i < 6; i++) press(view, KeyCode.LEFT);
            assertTrue(text(view).contains("< 1초 >"));
            press(view, KeyCode.RIGHT); assertTrue(text(view).contains("< 30초 >"));
            press(view, KeyCode.RIGHT); assertTrue(text(view).contains("< 60초 >"));
            press(view, KeyCode.LEFT); press(view, KeyCode.LEFT);
            press(view, KeyCode.DOWN); press(view, KeyCode.ENTER);
            assertEquals(1000, started.get().remainingMillis());
            assertEquals(BattleMode.TIMED, started.get().mode());
            snapshot(view, 800, 720, "local-setup");
        });
    }
    @Test void mainSeparatesSoloAndMultiplayerAndSoloSetupPassesChosenRules() throws Exception {
        fx(() -> {
            var selected = new AtomicInteger(-1);
            var main = new MainMenuView(() -> selected.set(0), () -> selected.set(1),
                    () -> selected.set(2), () -> selected.set(3), () -> selected.set(4));
            assertTrue(text(main).contains("일반 게임")); assertTrue(text(main).contains("멀티플레이"));
            assertFalse(text(main).contains("난이도")); assertFalse(text(main).contains("게임 모드"));
            press(main, KeyCode.ENTER); assertEquals(0, selected.get());
            press(main, KeyCode.DOWN); press(main, KeyCode.ENTER); assertEquals(1, selected.get());
            snapshot(main, 800, 720, "main-menu");
            var multi = new MultiplayerMenuView(() -> selected.set(5), () -> selected.set(6), () -> selected.set(7));
            press(multi, KeyCode.ENTER); assertEquals(5, selected.get());
            press(multi, KeyCode.DOWN); press(multi, KeyCode.ENTER); assertEquals(6, selected.get());
            press(multi, KeyCode.ESCAPE); assertEquals(7, selected.get());
            snapshot(multi, 800, 720, "multiplayer-menu");
            var chosen = new AtomicReference<GameSettings>();
            var base = new GameSettings(Difficulty.NORMAL, true, GameMode.CLASSIC, ScreenSize.SMALL,
                    KeyBindings.defaults().with(GameAction.HOLD, "H"), KeyBindings.playerTwoDefaults());
            var solo = new SoloSetupView(base, chosen::set, () -> selected.set(8));
            press(solo, KeyCode.RIGHT); press(solo, KeyCode.DOWN); press(solo, KeyCode.RIGHT);
            assertNull(chosen.get());
            press(solo, KeyCode.DOWN); press(solo, KeyCode.ENTER);
            assertEquals(Difficulty.HARD, chosen.get().difficulty()); assertEquals(GameMode.ITEM, chosen.get().mode());
            assertEquals(base.keyBindings(), chosen.get().keyBindings());
            assertEquals(base.playerTwoBindings(), chosen.get().playerTwoBindings());
            assertTrue(chosen.get().colorBlindMode()); assertEquals(ScreenSize.SMALL, chosen.get().screenSize());
            press(solo, KeyCode.ESCAPE); assertEquals(8, selected.get());
            snapshot(solo, 800, 720, "solo-setup");
        });
    }
    @Test void localKeyboardAttackPreviewPreservesHolesUntilReceiverLocks() throws Exception {
        fx(() -> {
            for (int lineCount : new int[] {1, 2}) {
                var board = new tetris.board.Board();
                for (int row = 22 - lineCount; row < 22; row++) for (int c = 0; c < 10; c++)
                    if (c != 4 && c != 5) board.lock(List.of(new tetris.board.Position(row, c)), tetris.board.Cell.J);
                var attacker = new Game(() -> tetris.block.TetrominoType.O, board);
                var receiver = new Game(() -> tetris.block.TetrominoType.O);
                var match = new BattleMatch(List.of(attacker, receiver), BattleMode.NORMAL, 180, new Random(1));
                var view = new LocalBattleView(match, GameSettings.DEFAULT, () -> {});
                try {
                    assertTrue(text(view).contains("+----------+"));
                    press(view, KeyCode.F);
                    assertEquals(lineCount, attacker.getClearedLines());
                    assertEquals(lineCount == 1 ? 0 : 2, receiver.getPendingRows().size());
                    if (lineCount == 2) {
                        assertTrue(text(view).contains("받을 공격 2/10"));
                        assertTrue(text(view).contains("|OOOO..OOOO|\n|OOOO..OOOO|"));
                        snapshot(view, 880, 850, "local-attack-preview");
                        press(view, KeyCode.NUMPAD1);
                        assertTrue(receiver.getPendingRows().isEmpty());
                        var cells = receiver.getVisibleCells();
                        assertEquals(tetris.board.Cell.GARBAGE, cells[19][0]);
                        assertEquals(tetris.board.Cell.EMPTY, cells[19][4]);
                    }
                } finally { view.close(); }
            }
        });
    }
    @Test void durationStepsStayOnThirtySecondBoundaries() {
        assertEquals(30, BattleRulesView.stepSeconds(1, 1));
        assertEquals(1, BattleRulesView.stepSeconds(30, -1));
        assertEquals(60, BattleRulesView.stepSeconds(30, 1));
        assertEquals(30, BattleRulesView.stepSeconds(31, -1));
        assertEquals(86400, BattleRulesView.stepSeconds(86400, 1));
        assertEquals(1, BattleRulesView.stepSeconds(1, -1));
    }
    @Test void playerKeyEditorRejectsCrossPlayerConflictsAndLocalUsesSavedKeys() throws Exception {
        fx(() -> {
            var saved = new AtomicReference<GameSettings>();
            var editor = new KeyBindingsView(KeyBindings.defaults(), KeyBindings.playerTwoDefaults(),
                    (first, second) -> saved.set(new GameSettings(Difficulty.NORMAL, false, GameMode.CLASSIC, ScreenSize.DEFAULT, first, second)));
            press(editor, KeyCode.RIGHT); // 오른쪽 2P
            press(editor, KeyCode.ENTER); press(editor, KeyCode.A);
            assertTrue(text(editor).contains("이미 사용 중"));
            press(editor, KeyCode.H); press(editor, KeyCode.ESCAPE);
            assertEquals("H", saved.get().playerTwoBindings().key(GameAction.LEFT));
            assertEquals("A", saved.get().keyBindings().key(GameAction.LEFT));
            snapshot(editor, 800, 720, "player-keys");
            var match = new BattleMatch(2, BattleMode.NORMAL, 180, saved.get());
            var local = new LocalBattleView(match, saved.get(), () -> {});
            try {
                int left = match.games().get(0).getCurrentBlock().getX();
                int right = match.games().get(1).getCurrentBlock().getX();
                press(local, KeyCode.LEFT); assertEquals(right, match.games().get(1).getCurrentBlock().getX());
                press(local, KeyCode.H); assertEquals(right - 1, match.games().get(1).getCurrentBlock().getX());
                assertEquals(left, match.games().get(0).getCurrentBlock().getX());
                press(local, KeyCode.A); assertEquals(left - 1, match.games().get(0).getCurrentBlock().getX());
                press(local, KeyCode.NUMPAD2); assertNotNull(match.games().get(1).getHeldBlock());
            } finally { local.close(); }
        });
    }
    @Test void networkLobbyLoadsRecentAddressAndRejectsBlankAddress() throws Exception {
        var store = new RecentServerStore(temporary.resolve("recent.txt")); store.save("192.0.2.10");
        fx(() -> {
            var view = new NetworkBattleView(GameSettings.DEFAULT, () -> {}, store);
            var address = nodes(view, TextField.class).get(0);
            assertEquals("192.0.2.10", address.getText());
            address.setText("");
            nodes(view, Button.class).stream().filter(b -> b.getText().equals("접속")).findFirst().orElseThrow().fire();
            snapshot(view, 1100, 950, "network-lobby");
            assertTrue(nodes(view, Button.class).stream().filter(b -> b.getText().contains("준비")).findFirst().orElseThrow().isDisabled());
            view.close(); view.close();
        });
    }
    @Test void battleKeysDoNotOverlapAcrossPlayers() {
        for (KeyCode code : KeyCode.values()) {
            int mapped = 0;
            for (int player = 0; player < 2; player++) if (BattleKeys.action(GameSettings.DEFAULT, player, code).isPresent()) mapped++;
            assertTrue(mapped <= 1, code.name());
        }
    }
    @Test void networkOpponentsShareRightHalfWithoutShrinkingLocalBoard() throws Exception {
        fx(() -> {
            var frames = new ArrayList<BattleFrame>();
            for (String profile : BattleMatch.PROFILES) {
                var game = new Game(); game.start(); game.softDrop(); game.softDrop();
                frames.add(BattleFrame.of(game, profile));
            }
            var view = new NetworkBattleBoards();
            for (int count : new int[] {2, 3, 4, 2}) {
                view.render(frames.get(0), frames.subList(1, count), false);
                snapshot(view, 1000, 540, "network-layout-" + count);
                var own = (Pane) view.getChildren().get(0);
                var opponents = (GridPane) view.getChildren().get(1);
                assertEquals(500, own.getWidth(), 0.1);
                assertEquals(500, opponents.getWidth(), 0.1);
                assertEquals(500, opponents.getLayoutX(), 0.1);
                assertEquals(count - 1, opponents.getChildren().size());
                for (Node slot : opponents.getChildren()) {
                    assertTrue(slot.getBoundsInParent().getMaxX() <= 500.1);
                    assertTrue(slot.getBoundsInParent().getMaxY() <= 540.1);
                }
            }
        });
    }
    @Test void networkScreensConnectStartRenderAndReturnAfterDisconnect() throws Exception {
        int freePort;
        try (var socket = new java.net.ServerSocket(0)) { freePort = socket.getLocalPort(); }
        var first = new AtomicReference<NetworkBattleView>();
        var second = new AtomicReference<NetworkBattleView>();
        try {
            fx(() -> {
                var host = new NetworkBattleView(GameSettings.DEFAULT, () -> {}, new RecentServerStore(temporary.resolve("host.txt")));
                var guest = new NetworkBattleView(GameSettings.DEFAULT, () -> {}, new RecentServerStore(temporary.resolve("guest.txt")));
                first.set(host); second.set(guest);
                setSpinner(host, 0, freePort); setSpinner(guest, 0, freePort);
                setCombo(guest, 0, "PLAYER2");
                new Scene(host, 1100, 950); new Scene(guest, 1100, 950);
                button(host, "서버 열고 참가").fire();
            });
            awaitFx(() -> text(first.get()).contains("접속 1/2"));
            fx(() -> button(second.get(), "접속").fire());
            awaitFx(() -> text(first.get()).contains("접속 2/2") && text(second.get()).contains("접속 2/2"));
            fx(() -> { button(first.get(), "게임 시작 / 준비").fire(); button(second.get(), "게임 시작 / 준비").fire(); });
            awaitFx(() -> text(first.get()).contains("PLAYER2 ·") && text(second.get()).contains("PLAYER1 ·"));
            fx(() -> {
                press(first.get(), KeyCode.S);
                snapshot(first.get(), 1100, 950, "network-playing");
                second.get().close();
            });
            awaitFx(() -> text(first.get()).contains("연결이 종료"));
            fx(() -> assertFalse(button(first.get(), "접속").isDisabled()));
            assertTrue(Files.exists(temporary.resolve("guest.txt")));
        } finally {
            fx(() -> { if (first.get() != null) first.get().close(); if (second.get() != null) second.get().close(); });
        }
    }
    private static void awaitFx(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            var done = new AtomicBoolean(); fx(() -> done.set(condition.getAsBoolean()));
            if (done.get()) return;
            Thread.sleep(20);
        }
        fail("화면 상태 대기 시간 초과");
    }
    private static String text(Node view) {
        return nodes(view, javafx.scene.text.Text.class).stream().map(javafx.scene.text.Text::getText).collect(java.util.stream.Collectors.joining("\n"));
    }
    private static Button button(Node view, String title) {
        return nodes(view, Button.class).stream().filter(b -> b.getText().equals(title)).findFirst().orElseThrow();
    }
    @SuppressWarnings("unchecked")
    private static void setSpinner(Node view, int index, int value) {
        ((Spinner<Integer>) nodes(view, Spinner.class).get(index)).getValueFactory().setValue(value);
    }
    @SuppressWarnings("unchecked")
    private static void setCombo(Node view, int index, Object value) {
        ((ComboBox<Object>) nodes(view, ComboBox.class).get(index)).setValue(value);
    }
    private static void press(Node view, KeyCode key) {
        Event.fireEvent(view, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false));
    }
    private static <T extends Node> List<T> nodes(Node root, Class<T> type) {
        var result = new ArrayList<T>();
        if (type.isInstance(root)) result.add(type.cast(root));
        if (root instanceof Parent parent) for (Node child : parent.getChildrenUnmodifiable()) result.addAll(nodes(child, type));
        return result;
    }
    private static void snapshot(Region view, int width, int height, String name) {
        if (view.getScene() == null) new Scene(view, width, height);
        view.setMinSize(width, height); view.setPrefSize(width, height); view.setMaxSize(width, height);
        view.resize(width, height); view.applyCss(); view.layout();
        WritableImage image = view.snapshot(null, null);
        BufferedImage png = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < png.getHeight(); y++) for (int x = 0; x < png.getWidth(); x++) png.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        try {
            Path directory = Path.of("build", "reports", "battle-ui"); Files.createDirectories(directory);
            ImageIO.write(png, "png", directory.resolve(name + ".png").toFile());
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }
}
