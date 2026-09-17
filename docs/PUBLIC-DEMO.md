# 합성 데이터 공개 데모

이 구성은 별도 빈 DB에 합성 경기 하나를 적재한 뒤 읽기 전용으로 실행합니다. 기존 연구 DB와 실제 발화 파일을 사용하지 않습니다. 이 문서는 배포 절차이며, 외부 서버 배포 완료를 뜻하지 않습니다.

## 준비와 빌드

Java 17, Node.js 24, MySQL이 필요합니다. 프런트엔드는 `frontend/package-lock.json`과 `npm ci`를 기준으로 설치합니다. 저장소에 남아 있는 pnpm lock이나 루트 node_modules를 사용하지 않습니다.

```sh
./gradlew bootJar
cd frontend
npm ci
NEXT_PUBLIC_DEMO_MODE=true NEXT_PUBLIC_API_URL= npm run build
```

위 프런트엔드 빌드는 같은 origin의 `/api`로 요청합니다. 두 NEXT_PUBLIC 변수는 빌드 시 결정되므로 변경 시 다시 빌드합니다. Linux 운영용 의존성은 Linux에서 설치합니다.

## 별도 DB와 초기 적재

새로운 빈 DB와 그 DB에만 권한이 있는 전용 사용자를 만듭니다. 아래 환경변수를 권한이 제한된 서비스 환경 파일에 저장합니다. 실제 비밀번호와 환경 파일은 Git에 추가하지 않습니다.

```dotenv
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=8081
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/lol_portfolio_demo?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
SPRING_DATASOURCE_USERNAME=lol_portfolio_demo
SPRING_DATASOURCE_PASSWORD=REPLACE_WITH_A_NEW_LOCAL_SECRET
SPRING_JPA_HIBERNATE_DDL_AUTO=update
CORS_ALLOWED_ORIGIN=http://localhost
APP_DEMO_READ_ONLY=false
```

`CORS_ALLOWED_ORIGIN`은 실제 화면 origin으로 바꿉니다. 초기 적재 중에는 backend를 loopback에만 바인딩하고 외부 reverse proxy를 연결하지 않습니다. 환경변수를 적용한 뒤 JAR를 실행하고 로컬에서 한 번만 적재합니다.

```sh
java -Xms64m -Xmx256m -jar build/libs/backend-0.0.1-SNAPSHOT.jar
# 별도 터미널에서 실행. 중복 적재하지 않습니다.
curl --fail --form zipFile=@examples/portfolio-demo.zip \
  --form matchCode=SYNTHETIC-DEMO --form myTeam=BLUE \
  http://127.0.0.1:8081/api/matches/import
```

응답의 경기 ID가 1인지 확인합니다. 데모 홈은 `/matches/1`로 연결되므로 기존 DB를 재사용하거나 중복 import하지 않습니다. 선수 10명, 발화 24개, 이벤트 4개, 경기 길이 120000ms를 확인합니다. fixture는 `python examples/build-portfolio-demo.py`로 재생성할 수 있습니다.

적재 후 `APP_DEMO_READ_ONLY=true`로 바꾸고 backend를 재시작합니다. 스키마 초기화가 끝났다면 `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`로 바꿉니다. 이 필터가 POST/PUT/PATCH/DELETE 등을 403으로 막습니다. 화면에서 버튼을 숨기는 설정만으로는 API가 보호되지 않습니다.

## 프런트엔드와 공개 경로

```sh
cd frontend
npm run start -- --hostname 127.0.0.1 --port 3001
```

reverse proxy는 `/api/`를 `127.0.0.1:8081`로, 나머지를 `127.0.0.1:3001`로 연결합니다. 프록시에서도 GET/HEAD/OPTIONS 이외의 API 메서드를 차단합니다. backend와 Next.js 포트를 직접 외부에 개방하지 않습니다. 원래 서비스와 DB를 보존하고 새 서비스 이름·배포 경로를 사용합니다.

공개 전 확인 항목:

- 합성 데이터 안내, 시간 범위 조절, 7개 대화 패턴, 네트워크·로그 화면.
- 경기 목록이 합성 경기만 포함하며 실제 연구 데이터가 없음.
- GET 조회 성공, POST import와 DELETE 요청은 403, 거부 후 경기 수 유지.
- 새 관리 키로 접속 확인 후 이전 키 폐기. 기존 다른 관리 키는 보존.
- 실제 공개 URL을 비로그인 브라우저에서 확인한 뒤 포트폴리오에 연결.

## 검증 범위

2026-09-18 Windows 로컬에서 프런트엔드 빌드와 TypeScript 검사를 수행했습니다. backend는 JDK 24로 Java 17 bytecode를 생성했고, 읽기 전용 필터 테스트 8개와 H2 통합 테스트 2개를 통과했습니다. 통합 테스트는 실제 ZIP importer, 조회 API, 7개 패턴, 범위 분석 응답 및 변경 요청 차단을 확인합니다. MySQL 운영 환경과 지표의 통계적 정확성까지 검증한 결과는 아닙니다.
