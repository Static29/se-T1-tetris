# 2단계 — 기본 조작 및 실행 환경 점검

기록 날짜: **2026-10-03**.

## 목표와 요구사항

Req1의 이동·회전·일시정지·재개·종료 조작과 팀원 환경에서의 빌드 가능성을 확보한다.

## 실제 작업과 산출물

| 날짜 | 담당 | 확인된 작업 | 구현·통합 근거 |
|---|---|---|---|
| 10/03 | 정승우 | Esc 일시정지 메뉴, 복귀·메인 메뉴·종료 흐름 | [bb65f92](https://github.com/Static29/se-T1-tetris/commit/bb65f92), [PR #8](https://github.com/Static29/se-T1-tetris/pull/8) |
| 10/03 | 박서상 | Block 이동·회전, 절대 좌표·회전 미리보기 API | [1e412a0](https://github.com/Static29/se-T1-tetris/commit/1e412a0), [PR #7](https://github.com/Static29/se-T1-tetris/pull/7) |
| 10/03 | 정승우 | 통합 확인 및 환경 문제 기록 | [3282962](https://github.com/Static29/se-T1-tetris/commit/3282962) |
| 10/03 | 박제상에게 전달 | Board 초기화·충돌·고정·행 삭제·가시 영역 데이터 제공 업무 | 3282962의 당시 문서. 이 날짜의 구현 완료를 의미하지 않음 |

## 검증·환경 문제

당시 기록은 `games=28c2d64`와 `develope=8669fec`의 파일 내용이 같고,
`gradlew.bat clean test` 결과 **26개 통과, 실패·오류 0개**였다고 명시한다.
이는 보존된 당시 실행 기록이며 이번에 재실행한 결과가 아니다.

다른 개발 환경에서 Java 버전 오류가 발생했고 해결됐다는 사용자 보고가 있다.
기준 환경은 JDK 21이다. 정확한 오류 메시지·변경 전 버전·해결 명령은 기록되지 않아 추정하지 않는다.
향후 동일 문제에서는 `java -version`, Gradle JVM, `JAVA_HOME`, IDE JDK를 함께 기록한다.

## 결과와 다음 단계

블록의 이동·회전 API와 일시정지 메뉴가 마련됐다. 실제 보드 충돌·행 삭제까지의 연결은 이후 단계에서 수행했다.
다음 단계는 Board 판정 구현과 신규 요구사항의 기능 분해였다.

[진행 목록](../README.md) · [결정·문제 해결](../decisions.md)
