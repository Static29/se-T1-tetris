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
| `BlockLocker` / `LockResult` | 고정 + 아이템 효과(L·F·B·M) + 줄 삭제를 한 번에 처리하고 점수 재료를 반환 |
| `WeightMover` | 무게추 전용 이동(닿기 전까지만 좌우, 지나가며 칸 삭제, 바닥 착지) |

## 난이도별 값

| 난이도 | I형 가중치 (나머지 10) | 레벨당 속도 배율 증가 | 레벨 1(10줄) 배율 / 간격 |
| --- | --- | --- | --- |
| Easy | `PieceSelector.EASY_I_WEIGHT` = 12 | `FallSpeed.EASY_INCREASE_PERCENT` = 8 (+0.08) | x1.08 / 925ms |
| Normal | `NORMAL_I_WEIGHT` = 10 | `NORMAL_INCREASE_PERCENT` = 10 (+0.1) | x1.1 / 909ms |
| Hard | `HARD_I_WEIGHT` = 8 | `HARD_INCREASE_PERCENT` = 12 (+0.12) | x1.12 / 892ms |

보드 패키지는 `tetris.games`를 import하지 않는다(games → board 단방향 유지).
`Difficulty`에 위 두 값을 필드로 추가하거나 Game에서 switch로 변환하는 방식을 제안한다.

속도 공식(2026-10-26 변경): 속도 배율 = 1 + 레벨 × 증가 비율 / 100 (normal: x1.0 → x1.1 → x1.2 …),
간격 = 1000ms / 배율(소수점 버림), 최소 50ms. 화면 표시·점수에는 `fallSpeed.getSpeedMultiplier(clearedLines)`를 쓰면
간격에서 거꾸로 계산할 때 생기는 오차(1000/909 = 1.1001) 없이 정확한 배율이 나온다.
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

## 아이템 효과 (팀 결정 2026-10-26)

| 아이템 | 효과 | 점수 재료 (`LockResult`) |
| --- | --- | --- |
| L 줄 삭제 | L이 있는 줄을 꽉 차지 않아도 1줄 삭제(위 줄이 내려옴) | `itemCellsRemoved` = 그 줄의 찬 칸 수, `itemRowsCleared` = 1 |
| F 빈칸 채우기 | F 바로 아래 ~ 바닥까지 같은 열의 빈칸을 그 블록 종류로 채움 | 없음(채워서 줄이 차면 `fullRowsCleared`) |
| B 폭탄 | B 칸은 항상 사라지고, 주변 8칸 중 찬 칸에서 최대 4개 무작위 삭제(구멍은 그대로) | `itemCellsRemoved` = B 포함 지운 칸 수 |
| M 점수 배율 | 고정된 칸에 배율 표시. 줄 삭제·L·B로 사라지면 그 고정의 점수 전체 2배(중복 없음) | `multiplierApplied` |
| 무게추 | 회전 불가, 닿기 전까지만 좌우 이동, 지나가며 아래 칸 삭제, 바닥 착지 후 고정 여유 적용, 1.5배 속도 | 지운 칸 0점. 그때 사라진 M도 배율 없음. 착지로 줄이 차면 일반 줄 점수 |

L로 지운 줄도 아이템 등장 카운트와 속도 레벨에 합산한다: `LockResult.totalRowsCleared()`.

### 고정 처리 (일반 블록·아이템 공통)

```java
BlockLocker locker = new BlockLocker(RandomGenerator.getDefault());

LockResult result = locker.lock(board, currentBlock);   // 고정 → Lock out 검사 → 아이템 효과 → 꽉 찬 줄 삭제
if (result.lockOut()) { state = GameState.GAME_OVER; return; }

long gained = scorePolicy.clearedRows(result.fullRowsCleared(), difficulty)
            + scorePolicy.removedCells(result.itemCellsRemoved(), difficulty);
if (result.multiplierApplied()) gained *= 2;
score += gained;

clearedLines += result.totalRowsCleared();   // 속도 레벨
spawner.onLinesCleared(result.totalRowsCleared());   // 아이템 등장 카운트
```

- 기존 `lockCurrent()`의 `board.lock` + `clearFullRows`를 위 한 줄로 바꾸면 된다.
- 화면 표시용 칸 종류는 `BlockLocker.cellOf(block)`를 쓴다. 무게추는 `getType()`이 null이라
  `Cell.valueOf(type.name())`이 실패하므로 `Cell.WEIGHT`가 필요하다(`Game.getVisibleCells` 포함).
- M 칸 표시는 `board.isMultiplier(row, col)`로 조회한다(JavaFX 화면에서 표시).

### 무게추 이동

```java
if (currentBlock.getItemType() == ItemType.WEIGHT) weightMover = new WeightMover(board, currentBlock);

// 좌우: 닿은 뒤에는 false
weightMover.tryMoveSideways(dx);
// 자동 하강 간격: fallSpeed.getWeightIntervalMillis(clearedLines)
if (!weightMover.stepDown()) lockDelay.onGrounded(gameTimeMillis);   // 바닥이면 고정 여유 시작
// 바닥 판정(고정 여유용): weightMover.isOnFloor()
// 하드 드롭: weightMover.dropToFloor(); 이후 locker.lock(board, currentBlock)
```

무게추는 일반 `tryMove(0, 1)`(canPlace) 대신 `stepDown()`을 써야 블록을 지우며 내려간다.

## 검증

`./gradlew.bat test` — `PieceSelectorTest`(100,000회 추첨, 기대 횟수 대비 ±5%),
`BlockSpawnerTest`(실제 생성 코드 `spawn()`의 확률 검증 포함), `FallSpeedTest`, `LockDelayTest`.
시드를 고정해 실행마다 같은 결과가 나온다.
