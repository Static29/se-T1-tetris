# 테스트 및 라인 커버리지

## 실행과 기준

JDK 21 환경의 프로젝트 루트에서 실행한다.

```powershell
.\gradlew.bat check --no-daemon --console=plain
```

`test` → `jacocoTestReport` → `jacocoTestCoverageVerification`을 실행한다.
JaCoCo 0.8.13을 사용하고 전체 BUNDLE의 LINE/COVEREDRATIO가 0.70 미만이면 실패한다.
보고서만 필요하면 `test jacocoTestReport`를 실행한다. `test`에도 보고서 생성이 연결되어 있다.
캐시된 결과 대신 모든 작업을 다시 실행할 때는 `--rerun-tasks`를 붙인다.
`clean`은 기존 배포본을 포함해 build 디렉터리를 지우므로 커버리지 측정에 필수는 아니다.

대상은 `src/main/java` 전체이며 UI와 Main/Launcher도 포함한다. 직접 작성한 제품 코드의
패키지나 클래스를 제외하지 않는다. 외부 라이브러리, 테스트 소스, 배포 검증용
`src/packaging/java`는 제품 코드 분모에 포함하지 않는다. 실행 가능한 줄을 기준으로 하며
주석과 빈 줄을 세는 지표가 아니다. 전체 비율은 패키지 비율의 단순 평균이 아니다.

## 2026-10-26 측정 결과

기준: HEAD `acab7f5`에 당시 미커밋된 배포·아이콘·JaCoCo 변경을 포함한 작업 트리.
최종 제출 시 변경을 커밋하고 해당 커밋에서 다시 측정해 커밋 번호와 보고서를 함께 남긴다.

- JUnit: 365개, 실패 0개, 오류 0개
- 전체 실행 줄: 1,362 / 1,540 (미실행 178줄)
- **전체 라인 커버리지: 88.44% — 70% 기준 통과**
- 실행 명령: `./gradlew.bat check --no-daemon --console=plain`

| 패키지 | 실행 줄 | 미실행 줄 | Line Coverage |
|---|---:|---:|---:|
| tetris.games | 266 | 3 | 98.88% |
| tetris.score | 76 | 3 | 96.20% |
| tetris.block | 135 | 0 | 100.00% |
| tetris.ui | 422 | 154 | 73.26% |
| tetris.persistence | 32 | 3 | 91.43% |
| tetris.board | 431 | 4 | 99.08% |
| tetris (실행 진입점) | 0 | 11 | 0.00% |

미실행 줄은 주로 UI와 실행 진입점에 남아 있다. 커버리지 통과는 실제 화면·키보드 조작이나
모든 분기 검증을 보장하지 않는다. 배포 런타임 검증은 `packageWindowsZip`에서 별도로 수행하며
그 결과를 이 JUnit 커버리지 수치에 합산하지 않는다.

## 비기능 요구사항 근거

`src/test/java/tetris/board/BoardPerformanceTest.java`는 다음 두 테스트를 포함하며 이번 실행에서 통과했다.

- 1,000개 블록의 이동 판정·낙하·고정·줄 삭제가 총 1초 이내 완료
- 4줄 삭제·보드 복사 1,000회가 총 1초 이내 완료

이 결과는 현재 실행 환경의 보드 연산 성능 근거다. 최소 사양 PC에서의 GUI 응답시간이나
실제 프레임 속도를 검증한 것으로 확대 해석하지 않는다.

## 증거 및 변경 관리

- 테스트 코드와 build.gradle 설정은 기능 코드와 함께 Git으로 관리한다.
- PR 전에 `check`를 실행하고 결과를 PR 설명에 기록한다.
- 보고서: `build/reports/jacoco/test/html/index.html` 및 같은 디렉터리의 XML/CSV.
- JUnit 보고서: `build/reports/tests/test/index.html` 및 `build/test-results/test`의 XML.
- 제출 시 HTML 폴더 전체와 XML, 측정 커밋·날짜·명령·환경을 함께 보관한다.
- build는 Git 제외 대상이다. 보고서는 별도 제출 ZIP 또는 향후 CI 산출물로 보관한다.
- GitHub Actions 자동 실행과 업무 배정 보드 연동은 이번 설정에 포함하지 않았다.
