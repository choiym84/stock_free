# Stock Free 백엔드

Spring Boot 기반의 Stock Free 백엔드입니다. PostgreSQL을 개발 데이터베이스로 사용하며 Flyway로 스키마를 관리하고 Testcontainers로 테스트 전용 데이터베이스를 구성합니다.

## 로컬 PostgreSQL 실행

로컬 데이터베이스는 compose.yaml에 정의되어 있습니다. Docker Compose를 사용하면 PostgreSQL을 직접 설치하지 않아도 동일한 버전과 설정으로 개발할 수 있습니다.

### 구성 요소

- 이미지: Docker Hub의 PostgreSQL 17 템플릿
- 컨테이너: 이미지로부터 실행되는 PostgreSQL 프로세스
- 포트: macOS의 127.0.0.1:5432를 컨테이너의 5432에 연결
- 볼륨: stock-free_postgres-data에 컨테이너와 독립적으로 데이터 저장
- Compose: 이미지, 컨테이너, 포트, 볼륨, 헬스 체크를 함께 관리

포트는 localhost에만 바인딩되므로 개발 데이터베이스가 로컬 네트워크에 노출되지 않습니다.

### 시작 및 상태 확인

backend 디렉터리에서 실행합니다.

```bash
docker compose up -d
docker compose ps
docker compose logs -f postgres
```

docker compose ps에서 healthy가 될 때까지 기다립니다. 로그 추적은 Ctrl+C로 종료할 수 있으며 PostgreSQL은 계속 실행됩니다.

기본 로컬 접속 정보:

```text
호스트:       127.0.0.1
포트:         5432
데이터베이스: stock_free
사용자:       stock_free
비밀번호:     stock_free_local
```

로컬 접속 정보를 변경하려면 .env.example을 복사해 .env를 수정합니다.

```bash
cp .env.example .env
```

환경 변수는 데이터 볼륨이 비어 있을 때만 사용자와 데이터베이스를 초기화합니다. 기존 볼륨이 생성된 뒤 .env를 바꿔도 저장된 인증 정보는 자동으로 변경되지 않습니다.

## Spring 애플리케이션 실행

```bash
./gradlew bootRun
```

Spring Boot가 compose.yaml을 감지해 필요한 경우 PostgreSQL을 시작하고 준비될 때까지 기다린 뒤 JDBC 연결 정보를 가져옵니다. start-and-stop 라이프사이클은 애플리케이션이 종료될 때 Spring Boot가 시작한 컨테이너를 중지하지만 이름 있는 볼륨의 데이터는 보존합니다.

## 컨테이너 내부에서 psql 접속

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

```text
\l                 데이터베이스 목록
\dt                테이블 목록
\d table_name      테이블 구조 확인
\q                 종료
```

## 중지·재시작·삭제

```bash
docker compose stop       # PostgreSQL 중지, 컨테이너와 데이터 보존
docker compose start      # 중지된 컨테이너 시작
docker compose restart    # PostgreSQL 재시작
docker compose down       # 컨테이너/네트워크 삭제, 데이터 볼륨 보존
docker compose up -d      # 보존된 볼륨으로 컨테이너 재생성
```

로컬 데이터를 모두 초기화하려면 다음 명령을 사용합니다. down -v는 이름 있는 데이터 볼륨까지 삭제하는 파괴적 작업입니다.

```bash
docker compose down -v
```

## Flyway 스키마 관리

마이그레이션 파일은 src/main/resources/db/migration에 있습니다. 초기 스키마는 V1__create_stock_free_schema.sql이 생성합니다. 이미 적용된 마이그레이션은 수정하지 말고 V2__add_market_calendar.sql과 같은 새 버전을 추가합니다. 적용된 파일을 수정하면 Flyway checksum 오류가 발생할 수 있습니다.

## 개발 DB와 테스트 DB

- ./gradlew bootRun: compose.yaml의 영속 PostgreSQL 사용
- ./gradlew test: Testcontainers가 별도의 임시 PostgreSQL 17 컨테이너 생성

테스트는 개발 데이터를 변경하지 않으며, 테스트 종료 후 임시 컨테이너를 정리합니다.

### PostgreSQL을 사용하는 이유

이 프로젝트의 핵심은 주문 처리 중 계좌 잔액과 보유 수량이 깨지지 않는 것입니다. PostgreSQL은
트랜잭션, 외래 키, 유일 제약조건, CHECK 제약조건을 제공하므로 애플리케이션 로직뿐 아니라
데이터베이스에서도 정합성 규칙을 검증할 수 있습니다. 또한 금액에는 NUMERIC, 시각에는
TIMESTAMPTZ를 사용할 수 있고, PostgreSQL 17을 Docker와 Testcontainers에서 동일하게 실행해
개발 환경과 테스트 환경의 차이를 줄일 수 있습니다.

## 문제 해결

```bash
docker info
docker compose ps
docker compose logs postgres
lsof -nP -iTCP:5432 -sTCP:LISTEN
docker compose config
```

### Flyway checksum 오류

공유 데이터베이스에서는 마이그레이션을 임의로 수정하거나 볼륨을 삭제하지 않습니다. 로컬 데이터를 버려도 되는 경우에만 다음 순서로 개발 환경을 초기화합니다.

```bash
docker compose down -v
docker compose up -d
./gradlew clean test
```

### 포트 충돌

5432 포트를 다른 프로세스가 사용 중이면 lsof로 확인합니다. 종료하기 어렵다면 .env의 POSTGRES_PORT를 다른 로컬 포트로 바꾼 뒤 docker compose config로 실제 바인딩을 확인합니다.

### 테스트 실행 실패

Testcontainers는 Docker 데몬이 필요합니다. Docker Desktop을 시작한 뒤 다음 명령으로 확인합니다.

```bash
docker info
./gradlew clean test
```
