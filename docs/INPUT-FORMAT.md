# 경기 입력 형식과 합성 예제

`MatchController`와 `MatchService.importMatch`에서 확인한 입력 계약입니다. [합성 ZIP](../examples/synthetic-match.zip)은 이 문서를 위해 새로 작성한 가상 경기입니다. 실제 선수·참가자·발화 데이터가 포함되지 않습니다. 분류 코드는 설명용으로 직접 지정했으며 모델 예측 결과가 아닙니다.

## 업로드

`POST /api/matches/import`, multipart 필드:

| 필드 | 값 |
| --- | --- |
| zipFile | ZIP 파일 |
| matchCode | 표시할 경기 코드. 예: SYNTHETIC-DEMO |
| myTeam | BLUE 또는 RED. 제공한 예제는 BLUE 선택 |

```powershell
curl.exe -X POST http://localhost:8080/api/matches/import -F "zipFile=@examples/synthetic-match.zip" -F "matchCode=SYNTHETIC-DEMO" -F "myTeam=BLUE"
```

위 호출은 실행 중인 로컬 서버의 DB에 새 경기 데이터를 저장합니다. 응답은 경기 ID이며 `GET /api/matches/{id}`로 조회합니다. 서버·DB 환경이 준비되어 있어야 합니다. 이번 문서 정리에서는 업로드를 실행하지 않았습니다.

## ZIP 내부 파일

모든 CSV는 UTF-8이며 첫 줄은 건너뜁니다. 헤더 이름이 아닌 **열 순서**로 읽습니다.

| 파일명 조건 | 실제 소비하는 열, 0부터 시작 | 합성 예제 |
| --- | --- | --- |
| 이름에 match_ 포함 + .txt | 첫 줄을 읽어 메타 파일 존재 여부 확인 | match_demo.txt |
| info.csv 포함 | 0 팀, 1 숫자 ID, 2 닉네임, 3 포지션 | 10명의 가상 플레이어 |
| da_result.csv 포함 | 0 화자, 1 문장, 2 시작, 3 끝, 5 act code | 가상 발화 6행 |
| event.csv 포함 | 0 이벤트, 1 시간, 2 killer, 3 victim | 가상 이벤트 2행 |

시간은 현재 코드 흐름에 맞춰 ms로 제공합니다. 화자는 `1-demo`처럼 앞부분에 숫자 ID를 둡니다. RED를 선택하면 화자 ID에 5를 더해 플레이어를 찾습니다. DA 코드는 `0=I`, `1=Q`, `2=D`, `3=C`, 범위를 벗어나면 UNK입니다. 예제의 4번 열은 사용하지 않는 자리입니다.

`game_time.csv`는 읽지만 현재 import의 최종 duration 계산에는 사용하지 않습니다. duration은 **마지막 발화 시작 시각과 마지막 이벤트 시각 중 큰 값**으로 계산합니다. 이 예제는 마지막 발화가 20,000ms에 끝나지만 duration은 19,000ms가 됩니다. 실제 경기 전체 길이나 마지막 발화 끝과 동일하다고 해석하면 안 됩니다.

## 해석·오류 처리의 한계

- 메타 파일이 없으면 import가 실패합니다. 나머지 행의 파싱 오류는 로그를 남기고 건너뛰므로 HTTP 성공만으로 모든 행의 정상 적재를 입증할 수 없습니다.
- CSV 파서는 줄 단위로 읽습니다. 인용된 문장 안의 줄바꿈 등 완전한 CSV 규격 지원을 가정하지 않습니다.
- 현재 네트워크 지표는 방향 있는 연결을 세면서 분모 10을 사용하고 범위 계산에만 상한을 적용합니다. 이 샘플은 표준 밀도 정확성을 입증하는 벤치마크가 아닙니다.
- 검증 범위는 파일명·열 수·ID 연결·시간 형식과 소스 계약의 대조입니다. DB 적재·분석 API·UI의 종단 실행 검증은 수행하지 않았습니다.

소스: [MatchService](../src/main/java/com/lolcoaching/backend/service/MatchService.java), [MatchController](../src/main/java/com/lolcoaching/backend/controller/MatchController.java).
