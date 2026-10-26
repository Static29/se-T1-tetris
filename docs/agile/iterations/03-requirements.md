# 3단계 — 2차 요구사항 분석 및 확장 준비

기록 범위: **2026-10-08~2026-10-14**.

## 목표와 요구사항

난이도·아이템 모드 등 Req2의 확장 항목을 정리하고, 실제 플레이를 위한 Board API 및 아이템 데이터를 준비한다.
색각 이상 모드·색상 구분은 Req1에도 존재하므로 모두 Req2에서 처음 제시됐다고 표현하지 않는다.

## 실제 작업과 산출물

| 날짜 | 담당 | 확인된 작업 | 근거 |
|---|---|---|---|
| 10/08 | 정승우, 팀 논의 기록 | 신규 요구사항·아이템 종류·담당 영역·미결 규칙 문서화 | [be522d1](https://github.com/Static29/se-T1-tetris/commit/be522d1), [de556ac](https://github.com/Static29/se-T1-tetris/commit/de556ac) |
| 10/10 | 박제상 | Board 경계·배치·고정·행 삭제·게임 종료 판정, 좌표 변환·성능 테스트 | [e88a5ee](https://github.com/Static29/se-T1-tetris/commit/e88a5ee) |
| 10/14 | 박서상 | ItemType·ItemBlockFactory, L/F/B/M 부착 칸과 무게추 특수 모양 | [a85d45d](https://github.com/Static29/se-T1-tetris/commit/a85d45d) |

10/08은 회의 내용을 전달·문서화한 날짜다. 참석자·정확한 시각·전체 발언록은 확인되지 않았다.
업무의 최종 구현 담당은 [팀 역할](../team.md)과 실제 커밋에 따라 기술한다.

## 설계·검증

- Board는 고정 셀만 관리하고 이동 중 Block은 Game이 별도로 관리한다.
- `Position.fromXY()`로 블록의 x/y와 보드의 row/col 좌표를 연결한다.
- `BoardTest`, `PositionTest`, `BoardPerformanceTest`가 e88a5ee에 포함됐다.
- 아이템 생성과 효과 적용을 구분한다. 10/14의 팩토리 구현만으로 보드 효과·점수 연결까지 완료됐다고 하지 않는다.
- 박제상 개발노트 요약에는 Board 재작성·좌표 변환·성능 검증 과정이 추가 설명되어 있다. 커밋에 드러나지 않는 세부 작업 날짜는 별도로 만들어 넣지 않는다.

## 결과와 다음 단계

충돌·고정·행 삭제 API와 아이템 데이터 표현을 마련했다. UI·SRS·실제 게임 흐름 통합과
아이템 효과·점수 계약 확정이 후속 작업으로 남았다.

[진행 목록](../README.md) · [근거와 용어](../evidence.md)
