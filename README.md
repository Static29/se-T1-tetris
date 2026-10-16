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
- 게임 화면: 20×10 보드, SRS 회전·이동·낙하·고정·줄 삭제, 점수·난이도 표시
- Esc로 일시정지 메뉴 표시: 게임으로 복귀, 설정, 메인 메뉴로 복귀, 게임 종료
- 일시정지 메뉴에서 방향키로 선택하고 Enter로 실행, Esc로 현재 게임 재개
- 설정: Easy/Normal/Hard 선택, 색맹모드 토글, 저장/취소 (진행 중 게임은 정지 상태 유지)
- 메인 메뉴로 복귀하면 현재 게임을 떠나고, 게임 종료를 선택하면 프로그램 종료
- NEXT: 다음 블록 1개를 문자와 종류별 색상으로 표시
- HOLD: C로 현재 블록 보관/교환, 블록 고정 전에는 1회만 허용
- Enter/Esc/C를 누르고 있을 때 동작이 반복되지 않도록 처리
- 게임 종료 시 영문 3자리 이름 선택, 등수·이름·난이도·점수 Top 20 리더보드
- Classic / Item 모드 설정과 메인·게임 화면 표시, 모드별 Top 20 리더보드(좌우 키 전환)
- 기존 모드 정보 없는 기록은 Classic으로 유지하며 Item의 실제 아이템 등장은 후속 연결 예정
- 설정·기록은 사용자 홈의 `.tetris/`에 저장하여 재실행 후 유지

점수는 개별 칸 삭제당 10점, 1/2/3/4줄 동시 삭제 총 100/200/300/600점이며
Easy/Normal/Hard 배율은 0.8/1/1.2입니다. 드롭 자체에는 점수가 없습니다.
블록 색상은 Next/Hold 미리보기에 적용했으며 실제 게임판 색상은 후속 작업입니다.
색맹모드는 설정 저장·전달까지만 구현했습니다. 난이도별 속도·등장 가중치와
아이템 효과의 실제 실행은 후속 연결 대상입니다.
자세한 연결 계약은 [게임 연결 안내](src/main/java/tetris/games/README.md)를 참고하세요.

## 파일 역할

- `tetris.Main`: JavaFX 실행과 첫 화면 준비
- `tetris.ui.ScreenController`: 창 안의 화면 전환, 새 게임 생성
- `tetris.ui.MainMenuView`: 메뉴와 선택 이벤트
- `tetris.ui.GameView`: 전달받은 Game의 상태 표시와 조작
- `tetris.ui.PauseMenuView`: 문자 일시정지 메뉴, 게임 입력 차단
- `tetris.ui.SettingsView`: 난이도·색맹모드 설정
- `tetris.ui.InitialsEntryView`, `LeaderboardView`: 이름 선택과 기록 표시
- `tetris.score`: 삭제 기반 점수 정책, 상위 20개 기록 저장
- `tetris.persistence`: 설정 저장과 파일 교체
- `tetris.ui.BoardView`: 보드 스냅샷을 문자로 변환 (JavaFX 의존성·게임 데이터 소유 없음)
- `tetris.ui.PiecePreviewView`: 팀원의 모양 데이터를 읽어 Next/Hold 문자 상자로 표시
- `tetris.games.Game`: JavaFX에 의존하지 않는 게임 상태 관리

보드·블록 담당자의 `tetris.board`, `tetris.block`은 이번 UI 작업에서 변경하지 않습니다.

## 이후 화면 확장

`Game`과 보드·블록 로직에는 JavaFX 코드를 넣지 않습니다.
현재 `BoardView.render(Cell[][])`는 전달받은 데이터를 수정하지 않고 문자만 반환합니다.
Game에서 실제 Board와 낙하 중 블록을 합성한 스냅샷을 전달합니다.
추후 도형 기반 GUI나 터미널 UI로 바꾸더라도 게임 로직은 유지하고 표시·입력 부분을 교체합니다.

## Next / Hold 연결 규칙

- 새 게임은 현재 블록과 다음 블록을 7종 중 각각 동일한 확률로 선택합니다.
- Hold가 비어 있으면 현재 블록을 저장하고 Next를 현재 블록으로 가져옵니다.
- Hold가 차 있으면 현재 블록과 교환하며 Next는 소비하지 않습니다.
- Hold로 꺼내거나 교체된 블록이 보드에 고정되기 전에는 다시 Hold할 수 없습니다.
- 실제 보드 고정 처리 완료 후 `Game.advanceAfterLock()`을 한 번 호출합니다.
  이 메서드는 고정을 수행하지 않으며 Next 진행, Hold 제한 해제, 생성 충돌 검사를 수행합니다.
- 시작/Hold/고정 이후 현재 블록을 초기 위치·회전으로 생성하고 Game Over를 검사합니다.
- 상태 변경 후 JavaFX 스레드에서 `GameView.refresh()`를 호출하여 미리보기를 갱신합니다.
- 생성 로직은 `Game(Supplier<TetrominoType>)` 생성자로 팀원의 구현을 연결할 수 있습니다.
