package tetris.games;

import java.util.*;
import java.util.random.RandomGenerator;
import tetris.board.GarbageRow;

/** 단일 게임 스레드가 소유하는 대전 규칙. 로컬 화면은 이 중 2인만 사용한다. */
public final class BattleMatch {
    public static final List<String> PROFILES = List.of("PLAYER1", "PLAYER2", "PLAYER3", "PLAYER4");
    private final List<Game> games;
    private final BattleMode mode;
    private final long durationMillis;
    private final RandomGenerator random;
    private long elapsed;
    private boolean finished;
    private List<Integer> winners = List.of();

    public BattleMatch(int players, BattleMode mode, int seconds, GameSettings settings) {
        this(createGames(players, mode, settings), mode, seconds, RandomGenerator.getDefault());
    }
    public BattleMatch(List<Game> games, BattleMode mode, int seconds, RandomGenerator random) {
        if (games.size() < 2 || games.size() > 4) throw new IllegalArgumentException("2~4명이 필요합니다.");
        if (seconds < 1 || seconds > 86400) throw new IllegalArgumentException("시간은 1~86400초입니다.");
        this.games = List.copyOf(games);
        if (new HashSet<>(games).size() != games.size()) throw new IllegalArgumentException("독립된 게임이 필요합니다.");
        this.mode = Objects.requireNonNull(mode);
        this.random = Objects.requireNonNull(random);
        durationMillis = seconds * 1000L;
        for (int i = 0; i < games.size(); i++) {
            int source = i;
            games.get(i).setAttackListener(rows -> attack(source, rows));
            games.get(i).start();
        }
        resolve();
    }
    private static List<Game> createGames(int count, BattleMode mode, GameSettings settings) {
        if (count < 2 || count > 4) throw new IllegalArgumentException("2~4명이 필요합니다.");
        return java.util.stream.IntStream.range(0, count).mapToObj(i -> new Game(mode.settings(settings))).toList();
    }
    private void attack(int source, List<GarbageRow> rows) {
        var candidates = new ArrayList<Game>();
        for (int i = 0; i < games.size(); i++) {
            Game target = games.get(i);
            if (i != source && target.getState() == GameState.RUNNING && !target.isAttackQueueFull()) candidates.add(target);
        }
        if (!candidates.isEmpty()) candidates.get(random.nextInt(candidates.size())).receiveAttack(rows);
    }
    public void act(int player, GameAction action) {
        if (finished) return;
        apply(games.get(player), action);
        resolve();
    }
    public static void apply(Game game, GameAction action) {
        switch (action) {
            case LEFT -> game.moveLeft(); case RIGHT -> game.moveRight();
            case ROTATE -> game.rotateClockwise(); case SOFT_DROP -> game.softDrop();
            case HARD_DROP -> game.hardDrop(); case HOLD -> game.hold();
            case PAUSE -> { /* 대전 중 개별 일시정지는 지원하지 않는다. */ }
        }
    }
    public void advanceTime(long millis) {
        if (millis < 0) throw new IllegalArgumentException("시간은 음수일 수 없습니다.");
        while (!finished && millis > 0) {
            long step = Math.min(16, millis);
            if (mode == BattleMode.TIMED) step = Math.min(step, durationMillis - elapsed);
            for (Game game : games) game.advanceTime(step);
            elapsed += step;
            millis -= step;
            resolve();
        }
    }
    private void resolve() {
        var alive = java.util.stream.IntStream.range(0, games.size())
                .filter(i -> games.get(i).getState() != GameState.GAME_OVER).boxed().toList();
        if (alive.size() <= 1) { finish(alive); return; }
        if (mode == BattleMode.TIMED && elapsed >= durationMillis) {
            long best = alive.stream().mapToLong(i -> games.get(i).getScore()).max().orElse(0);
            finish(alive.stream().filter(i -> games.get(i).getScore() == best).toList());
        }
    }
    private void finish(List<Integer> winners) {
        this.winners = List.copyOf(winners); finished = true;
        games.forEach(Game::pause);
    }
    public List<Game> games() { return games; }
    public boolean finished() { return finished; }
    public List<Integer> winners() { return winners; }
    public long remainingMillis() { return Math.max(0, durationMillis - elapsed); }
    public BattleMode mode() { return mode; }
    public String result() {
        if (!finished) return "";
        return winners.size() == 1 ? PROFILES.get(winners.get(0)) + " 승리" : "무승부";
    }
}
