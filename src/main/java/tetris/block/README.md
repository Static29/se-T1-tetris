# 아이템 블록 연결 안내

`TetrominoType`은 기존 7종만 유지한다. 난이도별 가중치 및 기본 종류 선택은 Game 담당이다.

## 일반 / 색각 보조 색상

`TetrominoType`은 JavaFX/AWT 의존성 없이 `#RRGGBB` 문자열을 제공한다.
일반 값은 기존 `PiecePreviewView`의 JavaFX 색상 상수와 같다.
보조 값은 [Okabe-Ito Color Universal Design](https://jfly.uni-koeln.de/color/) 팔레트에서 선택했다.

| 블록 | 일반 | 색각 보조 |
| --- | --- | --- |
| I | #00FFFF | #56B4E9 |
| O | #FFFF00 | #F0E442 |
| T | #EE82EE | #CC79A7 |
| S | #00FF00 | #009E73 |
| Z | #FA8072 | #D55E00 |
| J | #6495ED | #0072B2 |
| L | #FFA500 | #E69F00 |

`getNormalColorHex()` / `getColorBlindColorHex()`로 각각 조회하거나
`getColorHex(boolean colorBlindMode)`로 현재 모드에 맞는 값을 조회한다.
모드 설정의 저장/전환은 Game/UI 담당이며 enum 상태를 변경하지 않는다.

```java
// UI 담당자의 연결 예시. 이 작업에서는 UI 파일을 수정하지 않았다.
Color color = Color.web(type.getColorHex(colorBlindMode));
```

현재 UI의 switch 색상 코드를 위 조회로 바꿔야 실제 화면에 적용된다.
보드/Next/Hold에도 같은 모드 값을 전달해야 한다.
L/F/B/M은 기본 테트로미노 색상을 사용하고 기존 아이템 문자를 유지한다.
무게추는 `getType()==null`인 별도 모양이므로 이 API를 직접 호출하지 말고 UI에서 별도 처리해야 한다.
무게추 전용 색상은 이번에 정의하지 않았다.
보조 팔레트만으로 모든 색각 유형에서 구분을 보장하지 않는다.
문자·패턴 등 색상 외 식별 수단과 배경 대비는 UI 연결 후 함께 확인해야 한다.

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
- SRS 적용 후 `getX()/getY()`는 점유 칸의 최소 좌표가 아닌 **고정 회전 공간의 원점**이다.
  J/L/S/T/Z는 3x3, I는 4x4 공간이며 앞쪽 빈 행/열을 제거하지 않는다.
  I는 처음에 4x4의 두 번째 행(y=1)을 차지한다. `TetrominoType.getCells()`의 기존 기본 모양은 유지했다.
- `width()/height()`는 점유 칸만 감싼 크기다. 상대 좌표의 최댓값+1을 의미하지 않는다.
  표시는 `getCells()` 좌표를 직접 순회하거나 `getFrameSize()` 범위에서 `getCellSymbol()`을 조회한다.
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

Game의 현재/Next/Hold는 `Block`을 저장하며 아이템 종류와 부착 칸을 보존한다.
`copy()`는 현재 회전·위치를 보존하고 `spawnCopy(x, y)`는 아이템을 유지한 채 생성 방향으로 초기화한다.
UI는 Block의 실제 칸·표시 문자를 사용한다. 자동 아이템 생성 시점과 보드 효과는 추가 연결 대상이다.

## 시계 방향 SRS 회전 연결

`rotate()`와 `previewRotation()`은 기존처럼 시계 방향 90도이며, 이제 고정 회전축을 사용한다.
J/L/S/T/Z의 중심은 (1,1), I의 중심은 (1.5,1.5)다.
`getRotationState()`는 0/R/2/L에 대응하는 0/1/2/3을 반환한다.
O와 무게추는 모양과 아이템 위치를 유지하며 회전 상태 0, 보정 후보 한 개만 제공한다.
반시계 방향 회전 및 180도 단일 회전은 이번 API에 포함하지 않았다.

`getRotationOffsets()`는 현재 상태에서 다음 상태로 회전할 때의 후보를 SRS 순서대로 반환한다.
I는 별도 표를 사용하며, 원래 위치 (0,0)를 포함해 총 5후보다.
문헌 표의 y는 위쪽 양수이므로 이 코드에서는 부호를 반대로 변환했다.
보정량은 모두 회전 전 원점 기준이며 후보끼리 누적하지 않는다.

보드 담당자가 연결할 예시(이 문서의 예시일 뿐 Board 파일은 변경하지 않았다):

```java
for (int i = 0; i < block.getRotationOffsets().length; i++) {
    int[][] candidate = block.previewRotation(i); // 상태 불변, 절대 {x,y}
    if (board.canPlace(candidate)) {             // 실제 Board API에 맞춰 연결
        block.rotate(i);                        // 검증한 회전 및 보정량 함께 반영
        break;
    }
}
// 모두 실패하면 rotate를 호출하지 않는다. 위치/회전/아이템 상태가 모두 유지된다.
```

기존 `previewRotation()` → 검사 → `rotate()` 연결도 후보 0에 대해서는 가능하지만,
벽 차기까지 사용하려면 보드가 위 후보 순회를 연결해야 한다.
빈 프레임 부분이 벽/바닥 밖에 있어도 실제 점유 칸만 충돌 검사해야 한다.
보드 위쪽의 음수 y를 허용할지는 보드/게임 담당자의 생성·충돌 정책을 따른다.

회전/보정표 참고: [Tetris with Few Piece Types, 2절과 표 2·3](https://erikdemaine.org/papers/TetrisPieces_FUN2024/paper.pdf).

## 검증 (아이템 구현 당시)

Java 21로 패키지를 직접 컴파일하고 임시 검증 프로그램으로 다음을 확인했다.
일반 7종의 4회 회전 복원, 일반 모드 생성 거부, 7종 x 5아이템 조합,
무게추 4x2 모양 유지, 이동/회전 미리보기 일치, 부착 칸 추적, 반환 배열 보호,
I의 네 칸 모두에 L 부착 가능, 10,000회 아이템 선택 분포.
선택 결과는 선언 순서대로 2010 / 2029 / 2004 / 1998 / 1959회였다.
이 검증은 저장소의 JUnit/Gradle 전체 테스트 실행 결과가 아니다.

## SRS 재현 가능한 검증

프로젝트 루트 PowerShell에서 실행한다. JDK 21 이상이 필요하며 Java 21 대상으로 컴파일한다.
소스/클래스 임시 파일은 시스템 임시 디렉터리에만 생성하고 저장소 외부 파일을 변경하지 않는다.

```powershell
.\src\main\java\tetris\block\verify-srs.ps1 -JdkHome $env:JAVA_HOME
```

스크립트는 독립적으로 정의한 예상 모양과 문헌의 y-up 보정표를 사용해 다음을 검사한다.

- 7종의 4회 회전 복원, 고정 원점, I 생성 행 및 시계 방향 예상 모양.
- 두 보정표의 모든 시계 방향 전이 8행, 각 후보의 미리보기와 반영 결과.
- 부착형 4종 x 기본 블록 7종 x 부착 위치 4곳의 회전 및 이동 추적.
- 무게추 유지, 배열 복사 보호, 잘못된 후보 거부 시 상태 불변.
- 검증용 경계 판정으로 벽 차기/바닥 차기 성공, 모든 후보 실패 시 상태 불변.
- 일반 모드 아이템 생성 거부.

JDK 25에서 `--release 21`로 컴파일하고 위 검증을 통과했다.
Gradle 전체 테스트나 실제 Board/UI 통합 실행을 확인한 것은 아니다.
