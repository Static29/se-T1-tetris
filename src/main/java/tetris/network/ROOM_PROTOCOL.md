# 방 서버 연동 규약

독립 서버 진입점에 바인드 주소와 포트를 지정하고 마지막 인자로 `--rooms`를 전달한다.
기존 게임 화면과 내장 서버는 기존 1대1 규약을 사용한다. 아래 규약을 처리하는 클라이언트 화면이 필요하다.

| 방향 | 메시지 | 필드 순서 |
|---|---|---|
| 서버 → 클라이언트 | ROOM_WELCOME | playerId |
| 클라이언트 → 서버 | CREATE_ROOM | 목표 인원, DUEL/TIMED, 제한 초(DUEL은 0), 아이템 true/false, EASY/NORMAL/HARD |
| 클라이언트 → 서버 | JOIN_ROOM | 방 번호 |
| 서버 → 클라이언트 | ROOM_JOINED | 방 번호, 목표 인원, 모드, 제한 초, 아이템, 난이도 |
| 서버 → 클라이언트 | ROOM_LOBBY | 방 번호, 방장 playerId, 현재 인원, 목표 인원 |
| 클라이언트 → 서버 | READY | true/false |
| 서버 → 클라이언트 | READY | playerId, true/false |
| 서버 → 클라이언트 | ROOM_START | matchId, 모드, 제한 초, 아이템, 난이도 |
| 양방향 | STATE / ATTACK | matchId, 순번, payload. 서버는 matchId 뒤에 발신자 ID 추가 |
| 클라이언트 → 서버 | QUEUE_SPACE | matchId, true/false |
| 클라이언트 → 서버 | GAME_OVER | matchId |
| 서버 → 클라이언트 | ELIMINATED | matchId, playerId |
| 서버 → 클라이언트 | TIME_UP | matchId |
| 클라이언트 → 서버 | FINAL_SCORE | matchId, 음이 아닌 최종 점수 |
| 서버 → 클라이언트 | RESULT | matchId, 승자 playerId 또는 DRAW |
| 클라이언트 → 서버 | LEAVE_ROOM | 없음 |
| 서버 → 클라이언트 | ROOM_LEFT / ROOM_PLAYER_LEFT | 본인에게는 필드 없음, 남은 인원에게는 나간 playerId |
| 서버 → 클라이언트 | ERROR | 오류 코드 |

- 정원 2~4명이 모두 준비하면 자동으로 시작한다. 방 규칙은 생성 시 설정하고 재경기에도 유지한다.
- ATTACK 행은 기존과 같은 10칸 0/1 표현이며 2~10개 행을 `/`로 구분한다. Board는 한 블록 고정에서 F·L 효과와 일반 완성으로 삭제된 줄을 합산한다. 다른 아이템 효과로 삭제된 줄은 공격 수에 포함하지 않는다.
- 서버는 공격 하나를 임의의 대상 한 명에게 보내며, 대상이 없으면 폐기한다. 큐는 시작 시 비어 있다고 가정한다. 클라이언트는 10줄이 되거나 다시 여유가 생기는 순간 QUEUE_SPACE를 보낸다. 네트워크 지연으로 더 들어온 공격의 최종 용량 검사는 Board의 책임이다.
- 연결된 탈락자는 상태와 결과를 계속 받는다. 연결이 끊긴 참가자는 방에서 제거하며, 방장은 남은 참가자 중 최초 입장자에게 이전한다. 재경기에는 다시 정원을 채워야 한다.
- 시간제는 서버가 TIME_UP을 보낸 뒤 생존자의 점수 보고를 기다린다. 초기 정책은 최대 5초 대기 후 보고된 점수끼리 비교하는 것이다. 경기 중 한 명만 남으면 즉시 결과를 발표한다.
- 참가자 ID는 연결 단위다. 계정 모드에서는 서버가 인증된 표시 이름을 `PLAYER_NAME(playerId, displayName)`으로 방에 알린다. 화면은 이 이름을 사용하고 플레이어 추적에는 ID를 사용한다.

## 계정 모드

독립 서버에 `--accounts <accountFile>`을 지정하면 TLS와 로그인 검사가 적용된 방 서버가 열린다. 계정 파일은 서버 프로세스 하나만 사용하며 저장소 밖에 둔다.

```powershell
java "-Djavax.net.ssl.keyStore=<서버 PKCS12 파일>" "-Djavax.net.ssl.keyStoreType=PKCS12" "-Djavax.net.ssl.keyStorePassword=<키 저장소 암호>" -cp <클래스 경로> tetris.network.DedicatedServerMain <바인드 주소> 7777 --accounts <계정 파일 경로>
```

클라이언트는 `NetworkClient.connectTls(address, port, options, sslContext)`를 백그라운드에서 호출한다. 공인 인증서의 경우 `SSLContext.getDefault()`를 사용한다. 자체 인증서의 경우 해당 인증서가 등록된 truststore로 SSLContext를 구성한다. 입력 주소는 인증서 SAN의 DNS 이름 또는 IP와 일치해야 한다. keystore는 서버에만 두고 클라이언트에는 공개 인증서만 배포한다.

| 방향 | 메시지 | 필드 순서 |
|---|---|---|
| 서버 → 클라이언트 | AUTH_REQUIRED | 없음 |
| 클라이언트 → 서버 | REGISTER | 로그인 ID, 표시 이름, 비밀번호 |
| 서버 → 클라이언트 | REGISTERED | 없음. 이어서 LOGIN 요청 |
| 클라이언트 → 서버 | LOGIN | 로그인 ID, 비밀번호 |
| 서버 → 클라이언트 | LOGGED_IN | playerId, 표시 이름. 이어서 ROOM_WELCOME |
| 클라이언트 → 서버 | CHANGE_PASSWORD | 현재 비밀번호, 새 비밀번호 |
| 서버 → 클라이언트 | PASSWORD_CHANGED | 없음. 현재 로그인은 유지 |
| 서버 → 클라이언트 | ERROR | 오류 코드 |

- 로그인 ID는 영문·숫자·밑줄 4~20자이며 대소문자를 구분하지 않는다. 표시 이름은 앞뒤 공백 제거 및 NFKC 정규화 후 문자·숫자 2~12자이며 대소문자를 무시하고 중복을 검사한다.
- 새 비밀번호는 Java 문자열 길이 8~128자이고 공백만으로 구성할 수 없다. 서버는 무작위 salt와 PBKDF2 해시를 저장한다.
- 가입 후 별도로 로그인한다. 같은 계정으로 새 접속이 로그인하면 `ALREADY_LOGGED_IN`을 반환한다. 로그아웃은 연결을 닫는 방식이다.
- 계정 작업은 연결당 1초 간격으로 요청한다. `AUTH_RETRY_LATER`를 받으면 잠시 기다렸다가 재시도한다. 로그인 전 방 명령은 `LOGIN_REQUIRED`로 거부된다.
- 회원가입·로그인과 방·대전 화면은 NetworkPlayView에서 연결한다. 분실 복구는 별도 구현이 필요하다.

## 기본 서버 자동 접속 (2026-11-23)

- 멀티플레이의 네트워크 메뉴를 열면 `/tetris/dedicated-server.properties`의 주소·포트로 자동 TLS 접속한다.
- 현재 기본 주소는 `203.229.63.118:7777`이다. 이 주소는 실제 외부 접속이 보장되었다는 의미가 아니다.
- `/tetris/dedicated-server.cer` 공개 인증서를 게임에 포함하므로 사용자에게 파일 선택이나 별도 인증서 설치를 요구하지 않는다.
- 서버에는 반드시 이 공개 인증서와 짝인 새 `server.p12`를 적용해야 한다. 이전 데모 인증서로 실행한 서버는 거부된다.
- 개인 키와 암호는 운영자의 `%LOCALAPPDATA%\T1-Tetris\server\cert-<생성시각>`에만 보관한다.
  해당 폴더의 `server.p12`와 `certificate-password.txt`를 서버 창에서 사용하고 Git/배포 ZIP에는 넣지 않는다.
- 서버 바인드는 실제 로컬 주소 또는 `0.0.0.0`, 포트는 7777을 사용한다. 공유기 뒤라면 TCP 7777 포워딩과 방화벽 허용이 별도로 필요하다.
  공인 주소가 로컬 인터페이스에 없는 경우 그 공인 주소를 서버 바인드에 직접 입력하면 안 된다.
- 팀원은 공개 인증서가 포함된 최신 소스를 pull하거나 새로 빌드한 배포본을 받아야 한다.
  GitHub 연결 여부 자체를 인증하거나 GitHub 팀원만 접속시키는 기능은 아니다. 서버 로그인은 기존 게임 계정을 사용한다.
- 새 인증서는 생성일로부터 365일 유효하다. 주소/인증서 변경 또는 만료 전에는 서버 인증서와 게임 리소스를 함께 갱신한다.
- 오류 시 다시 연결 버튼을 제공한다. 서버가 항상 켜져 있어도 일시적인 네트워크 실패나 인증서 오류가 성공으로 처리되지는 않는다.
