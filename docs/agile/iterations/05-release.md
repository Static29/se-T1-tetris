# 5단계 — 아이템·속도 통합 및 배포·검증

기록 범위: **2026-10-25~2026-10-26**.
원격 통합 결과와 10/26 로컬 미커밋 산출물을 분리한다.

## 목표와 요구사항

Req2의 난이도·아이템 효과·행 삭제 애니메이션을 통합하고,
Req1의 점수·설정·배포 및 코드 검증 체계를 보완한다.

## 실제 커밋 및 통합

| 날짜 | 담당 | 확인된 작업 | 근거 |
|---|---|---|---|
| 10/25 | 박제상 | PieceSelector·BlockSpawner·FallSpeed·LockDelay 및 테스트 | [f6a2ea5](https://github.com/Static29/se-T1-tetris/commit/f6a2ea5) |
| 10/26 | 박서상 작업 통합 | 블록·SRS·아이템 테스트를 포함한 tetromino 병합 | [PR #12](https://github.com/Static29/se-T1-tetris/pull/12); 작성 커밋은 10/23 |
| 10/26 | 박제상 | BlockLocker·LockResult·WeightMover, L/F/B/M·무게추 보드 효과 | [da4c66d](https://github.com/Static29/se-T1-tetris/commit/da4c66d), [PR #15](https://github.com/Static29/se-T1-tetris/pull/15) |
| 10/26 | 정승우 | 색상·색각 이상 모드 연결, 하강 점수·화면 크기·키 설정·기록 초기화 | [c703bec](https://github.com/Static29/se-T1-tetris/commit/c703bec), [PR #13](https://github.com/Static29/se-T1-tetris/pull/13) |
| 10/26 | 정승우 | Next/Hold를 Block 객체로 연결, 굵은 문자 표시 | [a1a7e22](https://github.com/Static29/se-T1-tetris/commit/a1a7e22), [PR #14](https://github.com/Static29/se-T1-tetris/pull/14) |
| 10/26 | 정승우 | 아이템 통합·착지 미리보기·행 삭제 이펙트, Hold 중복 하강 점수 방지·무게추 Hold 금지 | [96f352f](https://github.com/Static29/se-T1-tetris/commit/96f352f), [PR #16](https://github.com/Static29/se-T1-tetris/pull/16) |
| 10/26 | 정승우 | L·폭탄 칸당 점수 개선, 아이템을 새 Next에서 먼저 표시 | [1328910](https://github.com/Static29/se-T1-tetris/commit/1328910), [PR #19](https://github.com/Static29/se-T1-tetris/pull/19) |
| 10/26 | 박제상 | 10줄당 Easy +0.08 / Normal +0.10 / Hard +0.12 속도 배율 | [c02eb65](https://github.com/Static29/se-T1-tetris/commit/c02eb65), [PR #17](https://github.com/Static29/se-T1-tetris/pull/17) |
| 10/26 | 정승우 | 정확한 속도 배율을 점수에 연결, 난이도·레벨별 회귀 검증 | [c560e76](https://github.com/Static29/se-T1-tetris/commit/c560e76), [PR #19](https://github.com/Static29/se-T1-tetris/pull/19) |

PR #19 병합 커밋은 [acab7f5](https://github.com/Static29/se-T1-tetris/commit/acab7f5)다.
당시 96f352f에 board 패키지의 통합 지원 변경도 포함되었음을 숨기지 않는다.
이후 속도·점수 연결은 board의 제공 API를 사용해 담당 간 충돌을 줄였다.

## 10/26 로컬 구현·검증 결과 — 아직 커밋·PR 없음

담당: 정승우.

| 작업 | 산출물 | 검증·상태 |
|---|---|---|
| Windows 실행파일·ZIP | jpackage Gradle 작업, Launcher, README 실행 안내 | 런타임·JavaFX 포함 배포본 생성 |
| 빨간 블록 T1 아이콘 | PNG·ICO와 생성 스크립트 | 실행파일·게임 창에 적용, 패키징 성공 |
| 배포 실행 오류 수정 | 런타임에 jdk.random 추가 | 메뉴 후 게임 시작 실패 원인 재현 및 수정 |
| 배포 전용 검증 | src/packaging/java/tetris/PackagingSmoke.java | 포함된 런타임으로 모든 모드·난이도 시작·하강·일시정지·Hold·하드 드롭 검증 |
| JaCoCo·품질 기준 | build.gradle, 테스트·커버리지 문서 | 365개 통과, LINE 1,362/1,540 = 88.44%; check에 최소 70% 연결 |

배포 파일: `build/distributions/T1-TETRIS-windows.zip`.
보고서: `build/reports/jacoco/test/html/index.html`.
build 산출물은 Git 제외 대상이며 제출 시 별도 보관해야 한다.

## 결과·한계·후속 작업

- 아이템 생성 데이터 → 보드 효과 → Game 점수·Next → UI 표시를 연결했다.
- 일반 단위 테스트 통과만으로 축소된 배포 런타임의 의존성 누락을 검출하지 못했다.
  실제 배포 런타임 검증을 추가해 재발을 방지했다.
- 현재 커버리지 측정에는 UI·진입점을 포함한다. 수치 통과와 실제 GUI 인수시험은 별개다.
- 박제상 노트의 362개와 현재 365개는 시점이 다르며 합산하지 않는다.
- 배포·커버리지 변경을 커밋·PR로 연결하고, 최종 커밋에서 재측정해 제출 자료를 확정해야 한다.
- 외부 작업 보드 운영 증거와 최소 사양 PC 검증은 별도 후속 작업이다.

[진행 목록](../README.md) · [검증 상세](../../testing-and-coverage.md)
