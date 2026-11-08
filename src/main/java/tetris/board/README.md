# 보드 패키지 안내

작성: 박제상(보드). Req1·Req2의 보드 기능은 `Game`(정승우)에 연결되어 있다.
이 문서는 board가 제공하는 API, Game이 실제로 쓰는 방식, 아직 남은 차이를 정리한다.

## 의존 방향

- `games → board → block` 단방향. board는 `tetris.games`를 import하지 않는다.
- `Board`·`Cell`·`Position`·`FallSpeed`·`LockDelay`는 `tetris.block`도 모른다(절대 좌표 `Position`만 받는다).
- Block과 Board를 잇는 코드는 `BlockRotation`, `BlockSpawner`, `BlockLocker`, `WeightMover`에만 둔다.
- Block의 `{x, y}` → `Position(row, col)` 변환은 `Position.fromXY`만 사용한다.

## 클래스 요약

| 클래스 | 역할 |
| --- | --- |
| `Board` | 고정된 칸만 저장(보이는 20줄 + 버퍼 2줄 × 10칸). 충돌·고정·줄 삭제·Block out/Lock out·M 표시 |
| `Cell` / `Position` | 칸 종류(EMPTY, 7종, WEIGHT) / 보드 절대 좌표와 `{x, y}` 변환 |
| `BlockRotation` | Block이 주는 SRS 회전 후보를 0번부터 `canPlace`로 검사해 첫 성공 후보 적용 |
| `PieceSelector` | Roulette Wheel Selection으로 다음 종류 선택. 가방(7-bag) 없이 매번 독립 추첨 |
| `BlockSpawner` | Next를 미리 정해 두고 생성 위치(`SPAWN_Y` = 0)에 놓아 내보냄. 대기 아이템이 있으면 Next를 아이템으로 교체 |
| `ItemCounter` (내부) | 아이템 등장 주기: 10줄 카운트·이월·대기 아이템 수 |
| `FallSpeed` | 지운 줄 수 → 속도 배율·자동 하강 간격(ms). 무게추 1.5배 간격 |
| `LockDelay` | 바닥에 닿은 뒤 0.5초 고정 여유. 이동·회전 성공 시 다시 셈(블록당 최대 15회) |
| `BlockLocker` / `LockResult` | 고정 → Lock out 검사 → 아이템 효과 → 꽉 찬 줄 삭제 순서를 지키고 점수 재료를 합쳐 반환 |
| `ItemEffect` (내부) | 아이템 한 종류의 효과. `LineClearItemEffect`·`FillItemEffect`·`BombItemEffect`·`MultiplierItemEffect` |
| `LineClearEffect` | 줄 삭제 직전 화면과 지울 줄 번호(삭제 연출용). `BlockLocker.lock(board, block, effects)`가 넘김 |
| `WeightMover` | 무게추 전용 이동(닿기 전까지만 좌우, 지나가며 칸 삭제, 바닥 착지) |

## 난이도별 값

| 난이도 | I형 가중치 (나머지 10) | 레벨당 속도 배율 증가 | 레벨 1(10줄) 배율 / 간격 |
| --- | --- | --- | --- |
| Easy | `PieceSelector.EASY_I_WEIGHT` = 12 | `FallSpeed.EASY_INCREASE_PERCENT` = 8 (+0.08) | x1.08 / 925ms |
| Normal | `NORMAL_I_WEIGHT` = 10 | `NORMAL_INCREASE_PERCENT` = 10 (+0.1) | x1.1 / 909ms |
| Hard | `HARD_I_WEIGHT` = 8 | `HARD_INCREASE_PERCENT` = 12 (+0.12) | x1.12 / 892ms |

- 속도 배율 = 1 + 레벨 × 증가 비율 / 100, 레벨 = 지운 줄 ÷ 10. 간격 = 1000ms / 배율(소수점 버림), 최소 50ms.
- 표시·점수에는 `getSpeedMultiplier(clearedLines)`를 쓴다(간격에서 거꾸로 계산하는 오차 없음).
- 속도는 줄 삭제 기준만 사용한다. Req1의 "일정 수 이상 블록 생성 시 증가"는 쓰지 않는다(팀 결정).
- 현재 Game이 `Difficulty`별 switch로 위 상수를 고른다. 값을 `Difficulty`로 옮길지는 게임 담당과 협의 중이다.

## Game 연결 현황

| 흐름 | board API | 비고 |
| --- | --- | --- |
| 생성·Next | `BlockSpawner.fromBlocks(...)`, `spawn()`, `peekNext()` | 원점 가로 가운데·`SPAWN_Y` = 0(버퍼 맨 위). I는 row 1, 나머지는 row 0부터 |
| Hold 복귀 | `spawner.resetToSpawn(block)` | Game.spawnCurrent가 매번 호출. 회전 0·생성 위치, 부착 아이템 유지 |
| 생성 충돌 | `board.isBlockOut(Position.fromXY(block.getBoardCells()))` | |
| 회전 | `BlockRotation.tryRotate(board, block)` | |
| 하강·고정 여유 | `FallSpeed`, `LockDelay` | Game이 일시정지를 뺀 게임 시간을 누적해 넘긴다 |
| 고정 | `BlockLocker.lock(board, block, effects)` | 점수 = (줄 점수 + 칸 점수) × (M이면 2) |
| 속도·아이템 카운트 | `LockResult.totalRowsCleared()` → `spawner.onLinesCleared(...)` | L로 지운 줄도 포함 |
| 무게추 | `WeightMover`, `FallSpeed.getWeightIntervalMillis` | 무게추 Hold 금지(게임 결정) |
| 화면 | `board.getVisibleCells()`, `board.isMultiplier(row, col)`, `BlockLocker.cellOf(block)` | 무게추는 `getType()`이 null이라 `cellOf`로 `Cell.WEIGHT` 변환 |

생성 위치는 `BlockSpawner` 한 곳에서만 정한다(2026-11-07 팀 결정). 이전에는 `spawn()`이 블록 맨 윗줄을
화면 첫 줄(row 2)에 놓고 Game이 `spawnCopy(가운데, 0)`로 다시 놓아 두 곳에서 위치를 계산했다.

## 아이템 효과 (팀 결정 2026-10-26)

| 아이템 | 효과 | 점수 재료 (`LockResult`) |
| --- | --- | --- |
| L 줄 삭제 | L이 있는 줄을 꽉 차지 않아도 1줄 삭제(위 줄이 내려옴) | `itemCellsRemoved` = 그 줄의 찬 칸 수, `itemRowsCleared` = 1 |
| F 빈칸 채우기 | F 바로 아래 ~ 바닥까지 같은 열의 빈칸을 그 블록 종류로 채움 | 없음(채워서 줄이 차면 `fullRowsCleared`) |
| B 폭탄 | B 칸은 항상 사라지고, 주변 8칸 중 찬 칸에서 최대 4개 무작위 삭제(구멍은 그대로) | `itemCellsRemoved` = B 포함 지운 칸 수 |
| M 점수 배율 | 고정된 칸에 배율 표시. 줄 삭제·L·B로 사라지면 그 고정의 점수 전체 2배(중복 없음) | `multiplierApplied` |
| 무게추 | 회전 불가, 닿기 전까지만 좌우 이동, 지나가며 아래 칸 삭제, 바닥 착지 후 고정 여유, 1.5배 속도 | 지운 칸 0점. 그때 사라진 M도 배율 없음. 착지로 줄이 차면 일반 줄 점수 |

새 부착형 아이템을 추가하는 순서:
1. `ItemType`(블록 담당)에 종류와 문자를 추가한다.
2. board에 `ItemEffect`를 구현한 클래스를 하나 만든다(효과 적용 + `Outcome`으로 점수 재료 반환).
3. `BlockLocker.effectOf`의 switch에 case를 하나 추가한다. 빠뜨리면 컴파일 오류로 알려 준다.

"(내부)" 표시 클래스는 package-private이라 board 밖에서는 보이지 않는다. Game은 `BlockLocker`·`BlockSpawner`만 쓴다.

## 검증

board 테스트는 `src/test/java/tetris/board`에 있다. 여러 테스트가 같이 쓰는 좌표·채우기 도우미는 `BoardFixtures`에 둔다.

- 기능: `BoardTest`, `PositionTest`, `BlockRotationTest`, `BlockSpawnerTest`, `FallSpeedTest`, `LockDelayTest`,
  `BlockLockerTest`, `WeightMoverTest`, `ItemCounterTest`
- 확률(Req2 5쪽): `PieceSelectorTest`·`BlockSpawnerTest` — 100,000회 추첨, 기대 횟수 대비 ±5%, 시드 고정
- 비기능(성능): `BoardPerformanceTest` — 블록 1,000개 처리, 4줄 삭제·복사 1,000회 각각 1초 이내
