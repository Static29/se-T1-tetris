# 1단계 — 개발 기반 및 모듈 골격

기록 범위: **2026-09-19~2026-09-29**. 커밋 날짜 기준의 사후 정리이며 공식 스프린트 일정이 아니다.

## 목표와 요구사항

보드·테트로미노·게임 시스템의 기본 구조를 분리하고 동일한 빌드·테스트 환경을 마련한다.
Req1의 텍스트 기반 화면·7종 테트로미노·게임 상태·개발 환경·단위 테스트 기반에 해당한다.

## 실제 작업과 산출물

| 날짜 | 담당 | 확인된 작업 | 근거 |
|---|---|---|---|
| 09/19 | 정승우 | 저장소 소개·Git 제외 설정 | [659f106](https://github.com/Static29/se-T1-tetris/commit/659f106), [6a51f7b](https://github.com/Static29/se-T1-tetris/commit/6a51f7b) |
| 09/27 | 박제상 | Cell·Position 기반 타입 추가 | [17d6def](https://github.com/Static29/se-T1-tetris/commit/17d6def) |
| 09/28 | 박서상 | 테트로미노 기본 데이터 구현 | [6e53486](https://github.com/Static29/se-T1-tetris/commit/6e53486), [PR #1](https://github.com/Static29/se-T1-tetris/pull/1) |
| 09/28 | 정승우 | Game·GameState와 패키지 구조 정리 | [0c930b6](https://github.com/Static29/se-T1-tetris/commit/0c930b6), [2518fe6](https://github.com/Static29/se-T1-tetris/commit/2518fe6), [PR #6](https://github.com/Static29/se-T1-tetris/pull/6) |
| 09/29 | 정승우 | Java 21·Gradle Wrapper·JavaFX·JUnit 구성, 문자 메뉴·화면 전환, Next/Hold 골격 | [3b01249](https://github.com/Static29/se-T1-tetris/commit/3b01249) |

보드 기반 타입은 [PR #5](https://github.com/Static29/se-T1-tetris/pull/5)에서 09/28 병합했다.
17d6def의 제목과 달리 실제 변경은 Cell·Position 두 파일이다. 완성된 Board·게임 전체 구현으로 확대해서 기록하지 않는다.

## 검증·문제 해결

- 3b01249 메시지 및 당시 문서: JUnit 25개 통과. 이번 정리에서 과거 커밋을 다시 실행하지는 않았다.
- 당시 기록에 Windows 한글 경로의 Gradle 테스트 프로세스 인코딩 문제와 IDE 의존성 인식 문제 해결이 남아 있다.
- `Game` 패키지 선언과 브랜치 간 구현 차이의 병합 충돌을 정리했다는 당시 기록이 있다.
- IDE 분석 캐시 문제와 실제 Gradle 컴파일 오류를 구분했다.

## 결과와 다음 단계

게임 화면과 상태·Next/Hold의 기반을 마련했다. 실제 낙하·충돌·고정·행 삭제 연결은 아직 후속 작업이었다.
초기 화면이 표시된 사실을 플레이 기능 전체 완료로 기록하지 않는다.

[진행 목록](../README.md) · [백로그](../backlog.md)
