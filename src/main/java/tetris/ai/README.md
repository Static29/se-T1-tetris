# 컴퓨터 대전 (Req3 도전 과제)

대전 모드의 상대를 컴퓨터가 플레이한다. 사람의 키보드 입력을 알고리즘이 대신 누르는 방식이다.
`AI-opponent` 브랜치 전용이며 main에는 머지하지 않는다(요구사항).

## 구성

| 클래스 | 역할 |
|---|---|
| `AiInput` | 컴퓨터가 보는 현재 상태: 고정된 보드, 현재·Hold·Next 블록, Hold 가능 여부. `AiInput.of(game)` |
| `ComputerPlayer` | 판단 함수. `nextAction(AiInput)` → 다음에 누를 키 하나(`GameAction`) |
| `BoardEvaluator` | 놓은 뒤 보드 점수: 높이 합·지운 줄·구멍·울퉁불퉁함 |
| `AiDifficulty` | 키 간격: 쉬움 1초, 보통 0.5초, 어려움 0.2초 |
| `ComputerOpponent` | 간격마다 키를 물어 `BattleMatch.act(player, key)`로 누름 |

## 판단 방법

1. 현재 블록을 회전 0~3번 × 좌우 끝까지 옮긴 모든 자리에 떨어뜨려 본다.
2. 각 결과를 `BoardEvaluator`로 점수 매기고 가장 좋은 자리를 고른다. 같으면 키를 덜 누르는 자리.
3. Hold 블록(비어 있으면 Next)을 생성 위치에서 같은 방법으로 계산해 0.5점 이상 더 좋으면 Hold.
4. 고른 자리로 가는 첫 키만 반환한다: 회전 → 좌우 이동 → 즉시 낙하.
5. 다음 호출 때 지금 상태로 다시 계산한다. 블록이 내려오거나 공격 줄이 올라와도 어긋나지 않는다.

- 아이템 효과는 계산하지 않고 일반 블록처럼 놓는다. 무게추는 바로 떨어뜨린다.
- 게임 내부는 바꾸지 않는다. `Game`의 공개 메서드만 읽고, 키는 사람과 같은 `BattleMatch.act`로 누른다.

## 화면 연결 (games/UI 담당)

```java
// 대전 시작 시: 오른쪽 2P를 컴퓨터로
var ai = new ComputerOpponent(match, 1, AiDifficulty.NORMAL);

// LocalBattleView 타이머: 프레임마다
match.advanceTime(dt);
ai.advanceTime(dt);

// 키 입력: 컴퓨터 자리(1)는 키보드 입력을 무시
```
