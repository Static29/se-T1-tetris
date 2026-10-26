# 4단계 — SRS 및 게임 화면·점수 시스템 통합

기록 범위: **2026-10-16~2026-10-23**.

## 목표와 요구사항

Req1의 실제 하강·충돌·고정·행 삭제·점수 표시·스코어보드를 연결하고,
Req2의 난이도·모드 설정 기반을 마련한다. SRS는 조작 편의성을 위한 구현 방식이다.

## 실제 작업과 산출물

| 날짜 | 담당 | 확인된 작업 | 근거 |
|---|---|---|---|
| 10/16 | 박서상 | SRS 회전 후보·보정량 및 미리보기 API | [1241b7d](https://github.com/Static29/se-T1-tetris/commit/1241b7d) |
| 10/16 | 박제상 | BlockRotation으로 회전 후보와 Board 충돌 검사 연결, 리팩터링 | [c0c6d5c](https://github.com/Static29/se-T1-tetris/commit/c0c6d5c), [772fad7](https://github.com/Static29/se-T1-tetris/commit/772fad7), [PR #10](https://github.com/Static29/se-T1-tetris/pull/10) |
| 10/16 | 정승우 | 실제 블록 하강·행 삭제 UI 연결, 난이도·점수·스코어보드·설정 저장 | [c9d9c76](https://github.com/Static29/se-T1-tetris/commit/c9d9c76) |
| 10/23 | 정승우 | 문서·디렉터리 보조 파일 정리, games 통합 | [b8f050d](https://github.com/Static29/se-T1-tetris/commit/b8f050d), [PR #11](https://github.com/Static29/se-T1-tetris/pull/11) |
| 10/23 | 박서상 | 블록·SRS·아이템 JUnit 테스트 추가 | [f53d844](https://github.com/Static29/se-T1-tetris/commit/f53d844) |

c9d9c76은 **10/16 작성**, PR #11은 **10/23 병합**이다. 구현일과 통합일을 혼동하지 않는다.
f53d844를 포함한 tetromino PR #12의 병합은 다음 단계인 10/26이다.

## 설계·검증

박제상 개발노트 요약에 따르면 별도 SRS 표 구현 후 기존 Block의 표를 확인해 중복을 제거하고
충돌 연결 클래스로 역할을 정리했다. Git의 Block·BlockRotation 변경이 연결 구조의 근거다.

c9d9c76에는 `GameBoardIntegrationTest`, `GameModeTest`, `ScoreIntegrationTest`,
`SettingsAndLeaderboardTest`, `ScoreViewsTest`가 포함된다.
테스트 파일 존재와 해당 시점의 전체 실행 성공은 구분하며, 확인하지 않은 통과 개수는 적지 않는다.

## 결과와 다음 단계

실제 게임 화면에서 생성·조작·고정·행 삭제를 연결했고 점수·스코어보드 기반도 구현했다.
따라서 점수 시스템이 5단계에 처음 등장했다고 기록하지 않는다.
다음 단계에서 아이템의 실제 보드 효과, 속도·점수 정책과 배포 검증을 통합했다.

[진행 목록](../README.md) · [백로그](../backlog.md)
