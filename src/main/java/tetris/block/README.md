# 아이템 블록 연결 안내

`TetrominoType`은 기존 7종만 유지한다. 난이도별 가중치 및 기본 종류 선택은 Game 담당이다.

## 생성

```java
ItemBlockFactory factory = new ItemBlockFactory();
// Game이 아이템 모드 여부와 10줄 삭제 시점을 판단한 뒤 호출한다.
Block next = factory.create(itemMode, selectedType, spawnX, spawnY);
// 특정 아이템 생성 예시
Block line = factory.create(true, selectedType, ItemType.LINE_CLEAR, spawnX, spawnY);
```

- `itemMode == false`이면 예외를 발생시킨다. 일반 모드는 기존 `new Block(type, x, y)` 사용.
- 무작위 생성은 5종 각각 동일 확률 20%. 부착형의 부착 칸도 동일 확률로 선택한다.
- L/F/B/M은 기본 블록의 한 칸에 붙는다. F/B의 부착 방식은 별도 합의가 없어 L과 같은 방식으로 구현했다.
- 무게추는 위쪽 가운데 2칸, 아래쪽 4칸의 6칸 모양이며 회전해도 그대로 유지한다.
- 무게추의 `getType()`은 `null`이다. 반드시 `getItemType() == ItemType.WEIGHT`로 별도 처리한다.
  무게추를 `TetrominoType`이나 `Cell`로 직접 변환하면 안 된다.

## 좌표 및 화면 표시

- `getCells()`는 상대 좌표, `getBoardCells()`와 미리보기는 보드 절대 좌표다. 모두 `{x, y}` 순서.
- `getItemCells()` / `getBoardItemCells()`는 부착형 아이템 한 칸의 상대 / 절대 좌표 복사본.
  일반 블록 및 무게추는 부착 칸이 없으므로 빈 배열이다. 무게추 전체 좌표는 `getBoardCells()` 사용.
- `getCellSymbol(localX, localY)`는 L/F/B/M, 일반 칸 O, 빈칸 '.'을 반환한다.
  무게추는 O로 원래 모양을 표시한다.
- 이동·회전 시 칸의 인덱스를 유지하므로 부착 아이템이 같은 칸을 따라간다. O 및 무게추는 회전하지 않는다.
- 배열 반환은 복사본이며 호출자가 변경해도 블록 상태가 바뀌지 않는다.

## 보드/게임 담당자가 적용할 효과

| 종류 | 적용 규칙 |
| --- | --- |
| LINE_CLEAR / L | 고정 시 아이템이 있는 행을 삭제하고 기존 점수 규칙 적용 |
| WEIGHT / 모양 표시 | 낙하하며 아래 블록 제거, 최초 접촉 이후 좌우 이동 제한 |
| FILL / F | 고정 즉시 바로 아래부터 바닥까지 같은 열의 빈칸 채우기 |
| BOMB / B | 고정 즉시 중심 3x3 범위의 채워진 칸 약 4개를 무작위 제거 |
| MULTIPLIER / M | 아이템이 있는 행이 삭제될 때 그 행의 점수만 2배 |

`ItemType.MULTIPLIER.getScoreMultiplier()`는 2, 나머지는 1을 반환한다.
효과 적용, 점수 계산, 무게추 접촉 상태와 좌우 제한은 이 패키지에서 처리하지 않는다.
특히 B의 대상이 4칸보다 적을 때의 처리 규칙/담당은 미정이다.

Game의 Next/Hold가 현재 `TetrominoType`만 저장하므로 아이템 종류와 부착 위치를 보존하려면
추가 연결 설계가 필요하다. Game/Board/UI 파일은 이번 작업에서 수정하지 않았다.

## 검증

Java 21로 패키지를 직접 컴파일하고 임시 검증 프로그램으로 다음을 확인했다.
일반 7종의 4회 회전 복원, 일반 모드 생성 거부, 7종 x 5아이템 조합,
무게추 4x2 모양 유지, 이동/회전 미리보기 일치, 부착 칸 추적, 반환 배열 보호,
I의 네 칸 모두에 L 부착 가능, 10,000회 아이템 선택 분포.
선택 결과는 선언 순서대로 2010 / 2029 / 2004 / 1998 / 1959회였다.
이 검증은 저장소의 JUnit/Gradle 전체 테스트 실행 결과가 아니다.
