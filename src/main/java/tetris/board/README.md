# 보드 패키지 연결 안내 (Req2: 생성·확률·속도·고정 여유·아이템 등장)

작성: 박제상(보드). 이 패키지의 새 클래스는 Game/UI 파일을 수정하지 않았다.
아래는 Game·GameView에 연결할 때의 안내이며, 실제 연결은 게임 코어 담당과 협의 후 진행한다.

## 클래스 요약

| 클래스 | 역할 |
| --- | --- |
| `PieceSelector` | Roulette Wheel Selection으로 다음 블록 종류 선택. 가방(7-bag) 없이 매번 독립 추첨 |
| `BlockSpawner` | Next 블록을 미리 정하고 생성 위치에 놓아 내보냄. 아이템 모드의 10줄 카운트와 Next 아이템 교체 |
| `FallSpeed` | 지운 줄 수 → 자동 하강 간격(ms). 1초에 1칸에서 시작, 10줄마다 레벨 상승 |
| `LockDelay` | 바닥에 닿은 뒤 0.5초 고정 여유. 이동·회전 성공 시 다시 셈(블록당 최대 15회) |
| `BlockRotation` | SRS 회전 후보 검사(기존) |

## 난이도별 값

| 난이도 | I형 가중치 (나머지 10) | 속도 증가 비율 | 레벨 1(10줄) 간격 |
| --- | --- | --- | --- |
| Easy | `PieceSelector.EASY_I_WEIGHT` = 12 | `FallSpeed.EASY_INCREASE_PERCENT` = 80 | 555ms |
| Normal | `NORMAL_I_WEIGHT` = 10 | `NORMAL_INCREASE_PERCENT` = 100 | 500ms |
| Hard | `HARD_I_WEIGHT` = 8 | `HARD_INCREASE_PERCENT` = 120 | 454ms |

보드 패키지는 `tetris.games`를 import하지 않는다(games → board 단방향 유지).
`Difficulty`에 위 두 값을 필드로 추가하거나 Game에서 switch로 변환하는 방식을 제안한다.

속도 공식: 속도(칸/초) = 1 + 레벨 × 증가 비율 / 100, 간격 = 1000ms / 속도, 최소 50ms.
Req1의 "일정 수 이상 블록 생성 시 속도 증가"는 사용하지 않고 줄 삭제 기준만 사용한다(팀 결정).

## 연결 순서 (Game 기준)

### 1) 생성과 Next

```java
PieceSelector selector = PieceSelector.withIWeight(iWeightOf(difficulty), RandomGenerator.getDefault());
BlockSpawner spawner = new BlockSpawner(board, selector, new ItemBlockFactory(), mode == GameMode.ITEM);

// 새 블록이 필요할 때 (게임 시작, 고정 후)
currentBlock = spawner.spawn();
lockDelay.clear();
if (board.isBlockOut(Position.fromXY(currentBlock.getBoardCells()))) state = GameState.GAME_OVER;

// 미리보기: 종류가 아니라 Block을 넘긴다 (아이템 문자 표시 필요)
Block next = spawner.peekNext();
```

- 확률만 먼저 적용하려면 현재 생성자 그대로 `new Game(selector::next, board, settings, policy)`로도 된다.
  다만 아이템 등장과 생성 위치까지 쓰려면 BlockSpawner로 바꿔야 한다.
- **생성 위치가 현재 Game과 다르다.** 현재 Game은 회전 공간 원점 y=0(블록이 숨겨진 2줄 안에서 시작)이고,
  BlockSpawner는 블록의 맨 윗줄이 화면에 보이는 첫 줄(row 2)에 오게 놓는다(보드 설계 결정).
  가로는 둘 다 `(columns - frameSize) / 2`.
- `peekNext()`가 돌려주는 Block은 미리보기에만 쓰고 이동·회전하지 않는다.
- 미리보기는 `getFrameSize()` 범위에서 `getCellSymbol(x, y)`로 그리면 아이템 문자(L/F/B/M)까지 표시된다.
  무게추는 `getType()`이 null이므로 `TetrominoType` 기준 표시(`PiecePreviewView.showPiece`)로는 그릴 수 없다.

### 2) Hold (아이템 블록도 보관 가능 — 팀 결정)

Hold는 종류가 아니라 `Block`을 저장한다. 꺼낼 때 생성 상태로 되돌린다.

```java
Block previous = currentBlock;
currentBlock = (heldBlock == null) ? spawner.spawn() : heldBlock;
spawner.resetToSpawn(currentBlock);   // 회전 상태 0, 생성 위치. 부착 아이템은 유지
heldBlock = previous;
lockDelay.clear();
```

### 3) 고정 후 줄 수 전달

```java
int rows = board.clearFullRows().size();
spawner.onLinesCleared(rows);   // 아이템 모드에서 10줄째에 Next가 즉시 아이템으로 바뀜
```

- 남은 줄은 이월한다(9줄 + 2줄 → 아이템 1개, 다음 아이템까지 9줄).
- 한 번에 20줄 이상이 쌓이면 지금 Next와 그다음 Next가 차례로 아이템이 된다.
- 아이템 효과(L 줄 삭제 등)로 지운 줄을 카운트에 포함할지는 미정. 포함하려면 같은 메서드로 전달.

### 4) 자동 하강과 고정 여유

700ms Timeline 대신 짧은 주기(AnimationTimer 또는 16ms Timeline)로 Game에 경과 시간을 넘긴다.
일시정지 중 시간이 흐르지 않도록 **실행 중에만 누적한 게임 시간**을 쓴다.

```java
// 매 프레임: deltaMillis = 지난 프레임과의 차이 (RUNNING일 때만 호출)
gameTimeMillis += deltaMillis;
if (gameTimeMillis - lastFallMillis >= fallSpeed.getIntervalMillis(clearedLines)) {
    lastFallMillis = gameTimeMillis;
    if (!tryMove(0, 1)) lockDelay.onGrounded(gameTimeMillis);
}
if (lockDelay.isExpired(gameTimeMillis)) lockCurrent();

// 좌우 이동·회전에 성공했을 때
if (isGrounded()) lockDelay.onMoveOrRotate(gameTimeMillis); else lockDelay.onAirborne();

// 소프트 드롭(↓): 내려가면 그대로, 못 내려가면 고정하지 않고 여유 시작
if (!tryMove(0, 1)) lockDelay.onGrounded(gameTimeMillis);

// 하드 드롭(Space): 여유 없이 즉시 고정 (기존과 동일)
```

`isGrounded()`는 `!board.canPlace(Position.fromXY(currentBlock.previewMove(0, 1)))`.

## 검증

`./gradlew.bat test` — `PieceSelectorTest`(100,000회 추첨, 기대 횟수 대비 ±5%),
`BlockSpawnerTest`(실제 생성 코드 `spawn()`의 확률 검증 포함), `FallSpeedTest`, `LockDelayTest`.
시드를 고정해 실행마다 같은 결과가 나온다.
