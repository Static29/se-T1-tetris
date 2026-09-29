# se-T1-tetris
소프트웨어공학 팀프로젝트입니다

## 협업 기록

매주 계획 → 구현·테스트 → 리뷰 → 회고 순서로 진행하고 기록합니다.

- [주차별 Agile 운영 안내](docs/agile/README.md)
- [팀 역할과 협업 경계](docs/agile/team.md)
- [전체 작업 목록](docs/agile/backlog.md)
- [주차별 기록 양식](docs/agile/week-template.md)
- [2026-09-29 기준 진행 기록](docs/agile/weeks/2026-09-29.md)

## 개발 환경

- JDK 21
- Gradle Wrapper 8.14.3 (Gradle 별도 설치 불필요)
- JavaFX 21.0.11: 문자 화면을 담는 창, 키 입력과 화면 전환
- JUnit 5: GUI 없이 게임 상태 전환 테스트

첫 실행 시 Gradle과 의존성을 다운로드하므로 인터넷 연결이 필요합니다.

Windows의 한글 경로에서도 테스트 프로세스를 실행할 수 있도록
`gradle.properties`에 Gradle JVM의 인코딩 호환 옵션을 설정했습니다.
Java 소스 파일은 UTF-8로 저장하고 컴파일합니다.

## Windows 개발용 실행

프로젝트 루트에서 PowerShell로 실행합니다.

```powershell
.\gradlew.bat run
```

테스트:

```powershell
.\gradlew.bat test
```

이 명령은 개발용입니다. 최종 사용자용 실행 파일 패키징은 이후 진행합니다.

## 현재 구현 범위

- 시작 메뉴: 게임 시작, 설정, 리더보드, 종료
- JavaFX 창 안에서 모든 메뉴와 보드를 문자로 표시 (터미널 실행 방식은 아님)
- 메뉴 조작: 위/아래 방향키로 `>` 선택 표시 이동, Enter로 선택
- 게임 화면: `X` 테두리와 `.` 빈칸으로 된 20×10 보드 미리보기, 문자 상태 표시
- P로 일시정지/재개, Esc로 메뉴 복귀
- NEXT: 다음 블록 1개를 문자와 종류별 색상으로 표시
- HOLD: C로 현재 블록 보관/교환, 블록 고정 전에는 1회만 허용
- Enter/P/Esc/C를 누르고 있을 때 동작이 반복되지 않도록 처리
- 설정과 리더보드는 준비 중 화면만 제공

실제 Board와 움직이는 Tetromino는 아직 연결하지 않았습니다.
점수, 자동 하강, 기록 저장은 아직 구현하지 않았습니다.
블록 색상은 Next/Hold 미리보기에 적용했으며, 실제 게임판 색상과 색각 이상 모드는 미구현입니다.
현재 화면에서는 첫 Hold 이후 고정 기능이 없어 Hold가 잠긴 상태로 유지됩니다.

## 파일 역할

- `tetris.Main`: JavaFX 실행과 첫 화면 준비
- `tetris.ui.ScreenController`: 창 안의 화면 전환, 새 게임 생성
- `tetris.ui.MainMenuView`: 메뉴와 선택 이벤트
- `tetris.ui.GameView`: 전달받은 Game의 상태 표시와 조작
- `tetris.ui.BoardView`: 보드 스냅샷을 문자로 변환 (JavaFX 의존성·게임 데이터 소유 없음)
- `tetris.ui.PiecePreviewView`: 팀원의 모양 데이터를 읽어 Next/Hold 문자 상자로 표시
- `tetris.games.Game`: JavaFX에 의존하지 않는 게임 상태 관리

보드·블록 담당자의 `tetris.board`, `tetris.block`은 이번 UI 작업에서 변경하지 않습니다.

## 이후 화면 확장

`Game`과 보드·블록 로직에는 JavaFX 코드를 넣지 않습니다.
현재 `BoardView.render(Cell[][])`는 전달받은 데이터를 수정하지 않고 문자만 반환합니다.
팀원의 Board API가 확정되면 UI에서 읽어온 스냅샷을 전달하도록 연결합니다.
추후 도형 기반 GUI나 터미널 UI로 바꾸더라도 게임 로직은 유지하고 표시·입력 부분을 교체합니다.

## Next / Hold 연결 규칙

- 새 게임은 현재 블록과 다음 블록을 7종 중 각각 동일한 확률로 선택합니다.
- Hold가 비어 있으면 현재 블록을 저장하고 Next를 현재 블록으로 가져옵니다.
- Hold가 차 있으면 현재 블록과 교환하며 Next는 소비하지 않습니다.
- Hold로 꺼내거나 교체된 블록이 보드에 고정되기 전에는 다시 Hold할 수 없습니다.
- 실제 보드 고정 처리 완료 후 `Game.advanceAfterLock()`을 한 번 호출합니다.
  이 메서드는 고정을 수행하지 않으며, Next를 진행하고 Hold 제한만 해제합니다.
- 팀원 코드 연결 시 시작/Hold/고정 이후 `getCurrentType()`의 블록을 초기 위치·회전으로 생성하고
  생성 위치 충돌에 대한 Game Over 검사도 연결해야 합니다.
- 상태 변경 후 JavaFX 스레드에서 `GameView.refresh()`를 호출하여 미리보기를 갱신합니다.
- 생성 로직은 `Game(Supplier<TetrominoType>)` 생성자로 팀원의 구현을 연결할 수 있습니다.
