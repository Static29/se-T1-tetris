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
    @Test @Timeout(60) void authenticatedRoomUiRegistersLogsInPlaysAndReturns() throws Exception {
        Path store = temporary.resolve("server.p12"), certificate = temporary.resolve("server.cer");
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool.exe").toString();
        runKeytool(keytool, "-genkeypair", "-alias", "server", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2",
                "-dname", "CN=localhost", "-ext", "SAN=DNS:localhost,IP:127.0.0.1", "-keystore", store.toString(), "-storetype", "PKCS12", "-storepass", "test-password");
        runKeytool(keytool, "-exportcert", "-alias", "server", "-keystore", store.toString(), "-storepass", "test-password", "-file", certificate.toString());
        var keys = java.security.KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(store)) { keys.load(input, "test-password".toCharArray()); }
        var managers = javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        managers.init(keys, "test-password".toCharArray());
        var tls = javax.net.ssl.SSLContext.getInstance("TLS"); tls.init(managers.getKeyManagers(), null, null);
        var first = new AtomicReference<NetworkPlayView>(); var second = new AtomicReference<NetworkPlayView>();
        try (var server = tetris.network.DedicatedServer.authenticated(java.net.InetAddress.getLoopbackAddress(), 0,
                tetris.network.ConnectionOptions.DEFAULT, tls, temporary.resolve("accounts.properties"))) {
            for (int i = 0; i < 2; i++) {
                int index = i; var reference = i == 0 ? first : second;
                var client = tetris.network.NetworkClient.connectTls("localhost", server.port(), tetris.network.ConnectionOptions.DEFAULT,
                        tetris.network.ServerCertificateTrust.fromFile(certificate));
                fx(() -> {
                    var view = new NetworkPlayView(GameSettings.DEFAULT, () -> {}); reference.set(view);
                    new Scene(view, 1100, 950); view.connected(client);
                });
                awaitFx(() -> labels(reference.get()).contains("보안 연결 완료"));
                fx(() -> {
                    var view = reference.get(); button(view, "회원가입").fire();
                    input(view, "ID").setText("tester" + index); input(view, "닉네임").setText("플레이어" + index);
                    input(view, "비밀번호").setText("test-pass-123");
                    snapshot(view, 1100, 950, "network-signup"); button(view, "계정 생성").fire();
                });
                awaitFx(() -> labels(reference.get()).contains("계정 생성 완료"));
                Thread.sleep(1100); // 서버 계정 요청 간격 계약(1초).
                fx(() -> { input(reference.get(), "비밀번호").setText("test-pass-123"); button(reference.get(), "로그인").fire(); });
                awaitFx(() -> labels(reference.get()).contains("네트워크 로비"));
            }
            fx(() -> { snapshot(first.get(), 1100, 950, "network-lobby"); button(first.get(), "방 만들기").fire(); });
            awaitFx(() -> labels(first.get()).contains("방 번호 1"));
            fx(() -> { input(second.get(), "참가할 방 번호").setText("1"); button(second.get(), "참가").fire(); });
            awaitFx(() -> labels(first.get()).contains("플레이어1") && labels(second.get()).contains("플레이어0"));
            fx(() -> {
                snapshot(first.get(), 1100, 950, "network-room");
                button(first.get(), "준비 / 준비 취소").fire(); button(second.get(), "준비 / 준비 취소").fire();
            });
            awaitFx(() -> labels(first.get()).contains("플레이 중") && labels(second.get()).contains("플레이 중"));
            fx(() -> {
                press(first.get(), KeyCode.S); snapshot(first.get(), 1100, 950, "authenticated-playing");
                button(second.get(), "대전 포기하고 방 나가기").fire(); button(second.get(), "나가기").fire();
            });
            awaitFx(() -> labels(first.get()).contains("플레이어0 승리"));
            fx(() -> { snapshot(first.get(), 1100, 950, "network-result"); button(first.get(), "확인 · 방으로 돌아가기").fire(); });
            awaitFx(() -> labels(first.get()).contains("1승") && labels(second.get()).contains("네트워크 로비"));
        } finally {
            fx(() -> { if (first.get() != null) first.get().close(); if (second.get() != null) second.get().close(); });
        }
    }
    private static TextField input(Node node, String prompt) {
        return nodes(node, TextField.class).stream().filter(f -> prompt.equals(f.getPromptText())).findFirst().orElseThrow();
    }
    private static String labels(Node node) {
        return nodes(node, Label.class).stream().map(Label::getText).collect(java.util.stream.Collectors.joining("\n"));
    }
    private static void runKeytool(String... command) throws Exception {
        var process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        assertTrue(process.waitFor(20, TimeUnit.SECONDS)); assertEquals(0, process.exitValue());
    }
    @Test void aiChoiceStartsSelectedDifficultyAndConnectionUsesKeyboardFields() throws Exception {
        fx(() -> {
            var started = new AtomicReference<BattleMatch>();
            var selectedAi = new AtomicReference<tetris.ai.AiDifficulty>();
            var setup = new BattleSetupView(GameSettings.DEFAULT, (match, difficulty) -> { started.set(match); selectedAi.set(difficulty); }, () -> {});
            assertTrue(nodes(setup, Button.class).isEmpty());
            var rules = nodes(setup, BattleRulesView.class).getFirst();
            press(rules, KeyCode.DOWN); press(rules, KeyCode.DOWN); press(rules, KeyCode.DOWN); press(rules, KeyCode.RIGHT);
            assertTrue(text(setup).contains("상대: < AI"));
            press(rules, KeyCode.DOWN); press(rules, KeyCode.RIGHT);
            assertTrue(text(setup).contains("AI 난이도: < 어려움 >"));
            press(rules, KeyCode.DOWN); press(rules, KeyCode.ENTER);
            assertNotNull(started.get()); assertEquals(tetris.ai.AiDifficulty.HARD, selectedAi.get());
            snapshot(setup, 800, 720, "local-ai-setup");
            press(rules, KeyCode.UP); press(rules, KeyCode.UP); press(rules, KeyCode.LEFT);
            assertTrue(text(setup).contains("상대: < 사람"));
            press(rules, KeyCode.DOWN); press(rules, KeyCode.ENTER); assertNotNull(started.get()); assertNull(selectedAi.get());
            var network = new NetworkPlayView(GameSettings.DEFAULT, () -> {});
            var fields = nodes(network, TextField.class);
            fields.stream().filter(f -> f.getPromptText().equals("포트")).findFirst().orElseThrow().setText("bad");
            nodes(network, Button.class).stream().filter(b -> b.getText().equals("보안 연결")).findFirst().orElseThrow().fire();
            assertTrue(nodes(network, Label.class).stream().anyMatch(l -> l.getText().contains("65535")));
            snapshot(network, 1100, 950, "network-connect"); network.close();
        });
    }
    @TempDir Path temporary;
    @Test void fallingItemsRemainVisibleAtTheirActualCellAfterMovingAndRotating() throws Exception {
        fx(() -> {
            for (boolean colorBlind : new boolean[]{false, true}) {
                for (var item : List.of(tetris.block.ItemType.LINE_CLEAR, tetris.block.ItemType.FILL,
                        tetris.block.ItemType.BOMB, tetris.block.ItemType.MULTIPLIER)) {
                    var settings = new GameSettings(Difficulty.NORMAL, colorBlind, GameMode.ITEM);
                    var board = new tetris.board.Board();
                    var factory = new tetris.block.ItemBlockFactory(new Random(1));
                    var game = new Game(board, () -> factory.create(true, tetris.block.TetrominoType.T, item, 0, 0),
                            settings, new tetris.score.DeletionScorePolicy());
                    var match = new BattleMatch(List.of(game, new Game(settings)), BattleMode.ITEM, 180, new Random(1));
                    var view = new LocalBattleView(match, settings, () -> {});
                    try {
                        for (int step = 0; step < 4; step++) game.softDrop();
                        for (int rotation = 0; rotation < 4; rotation++) {
                            game.rotateClockwise(); game.softDrop(); press(view, KeyCode.TAB);
                            var cell = game.getCurrentBlock().getBoardItemCells()[0];
                            var rendered = nodes(view, ColoredBoardView.class).getFirst();
                            var row = (HBox) rendered.getChildren().get(cell[1] - board.getBufferRows() + 1);
                            var glyph = (javafx.scene.text.Text) row.getChildren().get(cell[0] + 1);
                            assertEquals(item.getSymbol() + " ", glyph.getText());
                            assertEquals(javafx.scene.paint.Color.WHITE, glyph.getFill());
                            assertFalse(glyph.isUnderline());
                            assertEquals(0, nodes(rendered, javafx.scene.text.Text.class).stream().filter(javafx.scene.text.Text::isUnderline).count());
                        }
                        if (item == tetris.block.ItemType.BOMB && !colorBlind) snapshot(view, 880, 850, "local-falling-item");
                    } finally { view.close(); }
                }
            }
        });
    }
    @Test void localItemCountdownUpdatesPerPlayerAndReportsReadyNext() throws Exception {
        fx(() -> {
            var settings = new GameSettings(Difficulty.NORMAL, false, GameMode.ITEM);
            var board = new tetris.board.Board();
            var game = new Game(board, () -> new tetris.block.Block(tetris.block.TetrominoType.I), settings,
                    new tetris.score.DeletionScorePolicy(), new Random(42));
            var match = new BattleMatch(List.of(game, new Game(settings)), BattleMode.ITEM, 180, new Random(1));
            var view = new LocalBattleView(match, settings, () -> {});
            try {
                var panes = nodes(view, BattleBoardPane.class);
                assertTrue(text(panes.getFirst()).contains("아이템까지 10줄"));
                for (int line = 1; line <= 10; line++) {
                    for (int column = 0; column < 10; column++) if (column < 3 || column > 6)
                        board.lock(List.of(new tetris.board.Position(21, column)), tetris.board.Cell.J);
                    game.hardDrop(); game.advanceTime(Game.LINE_CLEAR_MILLIS); press(view, KeyCode.TAB);
                    assertTrue(text(panes.getFirst()).contains("삭제 " + line + "줄"));
                    assertTrue(text(panes.getFirst()).contains("아이템까지 " + game.getLinesUntilItem() + "줄"));
                    assertTrue(text(panes.get(1)).contains("삭제 0줄"));
                }
                assertTrue(text(panes.getFirst()).contains("NEXT 아이템 준비"));
                snapshot(view, 880, 850, "local-item-countdown");
            } finally { view.close(); }
        });
    }
    @Test void aiBoardUsesTimerAndRejectsPlayerTwoKeys() throws Exception {
        fx(() -> {
            var match = new BattleMatch(2, BattleMode.NORMAL, 180, GameSettings.DEFAULT);
            var view = new LocalBattleView(match, GameSettings.DEFAULT, tetris.ai.AiDifficulty.HARD, () -> {});
            try {
                String before = BattleFrame.of(match.games().get(1), "AI").encode();
                for (var key : List.of(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.NUMPAD1, KeyCode.NUMPAD2)) press(view, key);
                assertEquals(before, BattleFrame.of(match.games().get(1), "AI").encode());
                int x = match.games().get(0).getCurrentBlock().getX();
                press(view, KeyCode.A); assertEquals(x - 1, match.games().get(0).getCurrentBlock().getX());
                view.advanceTime(199);
                assertEquals(before, BattleFrame.of(match.games().get(1), "AI").encode());
                view.advanceTime(1);
                assertNotEquals(before, BattleFrame.of(match.games().get(1), "AI").encode());
                snapshot(view, 880, 850, "local-ai-playing");
                assertTrue(text(view).contains("오른쪽 AI"));
                view.close();
                String stopped = BattleFrame.of(match.games().get(1), "AI").encode();
                view.advanceTime(1000);
                assertEquals(stopped, BattleFrame.of(match.games().get(1), "AI").encode());
            } finally { view.close(); }
            var timed = new BattleMatch(2, BattleMode.TIMED, 1, GameSettings.DEFAULT);
            var timedView = new LocalBattleView(timed, GameSettings.DEFAULT, tetris.ai.AiDifficulty.NORMAL, () -> {});
            timedView.advanceTime(1000); assertTrue(timed.finished());
            String ended = BattleFrame.of(timed.games().get(1), "AI").encode();
            timedView.advanceTime(1000);
            assertEquals(ended, BattleFrame.of(timed.games().get(1), "AI").encode()); timedView.close();
        });
    }
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
            var view = new BattleSetupView(GameSettings.DEFAULT, (match, difficulty) -> started.set(match), () -> {});
            press(view, KeyCode.RIGHT);
            press(view, KeyCode.DOWN); press(view, KeyCode.RIGHT);
            assertTrue(nodes(view, ComboBox.class).isEmpty());
            assertTrue(nodes(view, Spinner.class).isEmpty());
            press(view, KeyCode.DOWN); press(view, KeyCode.DOWN); press(view, KeyCode.DOWN); press(view, KeyCode.ENTER);
            assertEquals(2, started.get().games().size());
            assertEquals(GameMode.ITEM, started.get().games().get(0).getSettings().mode());
            assertTrue(started.get().games().stream().allMatch(g -> g.getDifficulty() == Difficulty.HARD));
            press(view, KeyCode.UP); press(view, KeyCode.UP); press(view, KeyCode.UP); press(view, KeyCode.RIGHT);
            press(view, KeyCode.DOWN);
            for (int i = 0; i < 6; i++) press(view, KeyCode.LEFT);
            assertTrue(text(view).contains("< 1초 >"));
            press(view, KeyCode.RIGHT); assertTrue(text(view).contains("< 30초 >"));
            press(view, KeyCode.RIGHT); assertTrue(text(view).contains("< 60초 >"));
            press(view, KeyCode.LEFT); press(view, KeyCode.LEFT);
            press(view, KeyCode.DOWN); press(view, KeyCode.DOWN); press(view, KeyCode.ENTER);
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
            press(multi, KeyCode.DOWN); press(multi, KeyCode.ENTER); assertEquals(7, selected.get());
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
