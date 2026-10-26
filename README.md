# se-T1-tetris
소프트웨어공학 팀프로젝트입니다

## 협업 기록

실제 커밋·병합 날짜를 기준으로 3명의 담당 작업과 5개 개발 단계의 결과를 정리합니다.
확인되지 않은 회의 일정이나 스프린트 기간은 추정하지 않습니다.

- [날짜별 개발 진행 기록](docs/agile/README.md)
- [팀 역할과 협업 경계](docs/agile/team.md)
- [전체 작업 목록](docs/agile/backlog.md)
- [결정 및 문제 해결 기록](docs/agile/decisions.md)
- [작업 결과 양식](docs/agile/templates/iteration.md)
- [기록 근거와 용어](docs/agile/evidence.md)

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

## 테스트와 라인 커버리지

```powershell
.\gradlew.bat check
```

JUnit 테스트, JaCoCo 보고서 생성, 전체 LINE 커버리지 70% 기준 검증을 함께 실행합니다.
기준 미달 시 `check`가 실패합니다. `test`만 실행해도 보고서는 생성되지만 70% 기준 검증은 `check`에서 수행합니다.
모든 테스트를 강제로 다시 실행하려면 `check --rerun-tasks`를 사용합니다.

- 커버리지 HTML: `build/reports/jacoco/test/html/index.html`
- 커버리지 XML/CSV: `build/reports/jacoco/test/jacocoTestReport.xml`, `jacocoTestReport.csv`
- 테스트 결과: `build/reports/tests/test/index.html`
- 측정 범위: `src/main/java` 전체(UI·실행 진입점 포함), 임의 제외 없음
- [측정 결과 및 제출 증거 안내](docs/testing-and-coverage.md)

## Windows 실행파일 빌드·배포

Windows JDK 21 환경에서 다음 명령을 실행하면 테스트 후 Java 런타임과 JavaFX를 포함한 배포본을 만듭니다.
ZIP 생성 전에는 포함된 런타임으로 모든 모드·난이도의 게임 시작, 자동 하강, 일시정지, Hold·하드 드롭을 검증합니다.
기본 난수 생성기에 필요한 `jdk.random` 모듈도 배포 런타임에 포함합니다.

```powershell
.\gradlew.bat packageWindowsZip
```

- 실행: `build/jpackage/T1 TETRIS/T1 TETRIS.exe` 더블클릭
- 전달할 파일: `build/distributions/T1-TETRIS-windows.zip`
- 받는 사람은 ZIP 전체를 압축 해제한 뒤 `T1 TETRIS.exe`를 실행합니다. Java/Gradle 설치가 필요 없습니다.
- exe 하나만 옮기면 실행되지 않습니다. 옆의 `app`, `runtime` 폴더를 함께 유지해야 합니다.
- 설치 마법사가 없는 휴대용 배포본이며, 빌드한 Windows/JDK와 같은 CPU 아키텍처를 대상으로 합니다.
- 설정·리더보드는 기존처럼 사용자 홈의 `.tetris`에 저장되므로 배포 폴더를 교체해도 유지됩니다.
- 빌드를 다시 하면 `build/jpackage/T1 TETRIS`의 이전 배포본만 교체합니다.
- 빨간 블록 T1 아이콘은 `src/main/resources/tetris/icon.ico`(exe), `icon.png`(게임 창)를 사용합니다.
  디자인 재생성 스크립트는 `scripts/create-icon.ps1`입니다.

## 현재 구현 범위

- 시작 메뉴: T1 TETRIS, 게임 시작, 설정, 리더보드, 종료
- JavaFX 창 안에서 모든 메뉴와 보드를 문자로 표시 (터미널 실행 방식은 아님)
- 메뉴 조작: 위/아래 방향키로 `>` 선택 표시 이동, Enter로 선택
- 게임 화면: 20×10 보드, SRS 회전·이동·낙하·고정·줄 삭제, 점수·난이도 표시
- Esc로 일시정지 메뉴 표시: 게임으로 복귀, 설정, 메인 메뉴로 복귀, 게임 종료
- 일시정지 메뉴에서 방향키로 선택하고 Enter로 실행, Esc로 현재 게임 재개
- 설정: Easy/Normal/Hard 선택, 색맹모드 토글, 저장/취소 (진행 중 게임은 정지 상태 유지)
- 메인 메뉴로 복귀하면 현재 게임을 떠나고, 게임 종료를 선택하면 프로그램 종료
- NEXT: 다음 블록 1개를 문자와 종류별 색상으로 표시
- HOLD: C로 현재 블록 보관/교환, 블록 고정 전에는 1회만 허용
- 현재/Next/Hold는 Block 객체로 아이템 종류·부착 칸을 보존하며 미리보기에 아이템 문자를 표시
- 보드·블록·Next/Hold 모양은 굵은 고정폭 글꼴 사용
- Enter/Esc/C를 누르고 있을 때 동작이 반복되지 않도록 처리
- 게임 종료 시 영문 3자리 이름 선택, 등수·이름·난이도·점수 Top 20 리더보드
- Classic / Item 모드 설정과 메인·게임 화면 표시, 모드별 Top 20 리더보드(좌우 키 전환)
- 기존 모드 정보 없는 기록은 Classic으로 유지하며 Item에서는 10줄마다 아이템 등장
- 줄 삭제 깜빡임, 회색 착지 미리보기, Hold 재하강 점수 중복 방지 지원
- 설정·기록은 사용자 홈의 `.tetris/`에 저장하여 재실행 후 유지
- 기본/작은/전체화면 설정, 게임 조작 키 변경과 기본 키 복원
- 보드·Next·Hold 모두 일반 칸을 O로 표시하며 종류별 색상과 색맹 팔레트 적용
- 리더보드 Delete: 현재 모드 초기화 / R: 전체 초기화 (확인 후 삭제)

기본점수는 하강 1칸당 10점, 개별 칸 삭제당 10점, 1/2/3/4줄 동시 삭제 총 1000/2000/3000/6000점입니다.
난이도 증가분(Easy -20%, Normal 0%, Hard +20%)과 속도 보너스를 합산합니다.
속도 +1%마다 기본점수 +10%이며 최종식은 기본점수 × (1 + 난이도 증가분 + 속도 증가분 × 10)입니다.
기본 하강은 1초에 1칸, 10줄마다 빨라집니다. 난이도별 등장 가중치와 L/F/B/M·무게추 효과가 연결되어 있습니다.
자세한 연결 계약은 [게임 연결 안내](src/main/java/tetris/games/README.md)를 참고하세요.

## 파일 역할

- `tetris.Main`: JavaFX 실행과 첫 화면 준비
- `tetris.ui.ScreenController`: 창 안의 화면 전환, 새 게임 생성
- `tetris.ui.MainMenuView`: 메뉴와 선택 이벤트
- `tetris.ui.GameView`: 전달받은 Game의 상태 표시와 조작
- `tetris.ui.PauseMenuView`: 문자 일시정지 메뉴, 게임 입력 차단
- `tetris.ui.SettingsView`, `KeyBindingsView`: 난이도·색맹모드·화면·키 설정
- `tetris.ui.InitialsEntryView`, `LeaderboardView`: 이름 선택과 기록 표시
- `tetris.score`: 하강·삭제 점수 정책, 모드별 상위 20개 기록 저장
- `tetris.persistence`: 설정 저장과 파일 교체
- `tetris.ui.BoardView`: 보드 스냅샷을 문자로 변환 (JavaFX 의존성·게임 데이터 소유 없음)
- `tetris.ui.ColoredBoardView`: 보드 문자와 셀별 팔레트 표시
- `tetris.ui.PiecePreviewView`: 팀원의 모양 데이터를 읽어 Next/Hold 문자 상자로 표시
- `tetris.games.Game`: JavaFX에 의존하지 않는 게임 상태 관리

보드 담당 브랜치에서 `FallSpeed` 파일을 가져와 낙하 간격과 점수 속도 배율에 연결했습니다.

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
