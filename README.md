# Stock Free

실시간 국내 주식 시세를 바탕으로 매수·매도를 경험할 수 있는 모의투자 서비스입니다. 단순 CRUD를 넘어 주문 정합성, 동시성 제어, 인증 보안, 시세 데이터 신뢰성을 검증하는 백엔드 포트폴리오를 목표로 합니다.

> 실제 자산이 오가지 않는 교육·개발용 모의투자 프로젝트입니다.

## 프로젝트 소개

운영자가 지정한 국내 주식 종목의 시세를 수집하고, 사용자는 서버가 보유한 유효한 호가를 기준으로 시장가 모의주문을 실행합니다. 체결 결과는 계좌 잔액·보유 수량·거래 원장에 기록해 사후 검증할 수 있도록 설계합니다.

- 외부 시세 API보다 주문 도메인의 정합성과 동시성 검증을 먼저 완성
- 주문 상태 변경을 하나의 트랜잭션으로 처리
- 중복 요청은 client_order_id로 멱등 처리
- 현재 상태와 변경 원장을 함께 저장해 데이터 추적성 확보
- 단일 서버에서 시작하되 Redis·WebSocket 확장 고려

## MVP 범위

| 영역 | 계획된 기능 |
| --- | --- |
| 사용자 | 회원가입, 로그인, 계정 복구, 프로필 및 권한 관리 |
| 시세 | KIS WebSocket 실시간 체결·호가 수집 |
| 차트 | 1분 OHLCV 집계 및 과거 커서 조회 |
| 주문 | 최우선 상대 호가 기반 시장가 매수·매도 |
| 계좌 | 잔액, 보유 수량, 평균 매입가, 거래 이력 |
| 운영 | 종목 수집 활성화, 재연결, 누락 분봉 보정 |

지정가·부분 체결·주문 대기/취소·공매도·신용거래·수수료/세금·정규장 외 거래는 MVP에서 제외합니다. 자세한 정책은 [MVP 요구사항](backend/docs/mvp-market-data-trading-requirements.md)을 참고하세요.

## 전체 아키텍처

```text
┌─────────────┐  REST / WebSocket  ┌─────────────────────┐
│ React Client│ ◀────────────────▶ │ Spring Boot Backend │
└─────────────┘                    │ 인증·주문·계좌       │
                                    │ 시세·분봉 집계       │
       KIS WebSocket / REST         └──────────┬──────────┘
                    │                          │ JPA / Flyway
                    ▼                          ▼
             ┌─────────────┐            ┌─────────────┐
             │ KIS Adapter │            │ PostgreSQL  │
             └─────────────┘            └─────────────┘
                                    Redis 세션·공유 캐시(확장)
```

프론트엔드는 KIS에 직접 접속하지 않습니다. 인증 정보와 주문 체결 가격은 백엔드가 관리하며 클라이언트 표시 가격은 체결 결정에 사용하지 않습니다.

## 도메인 및 데이터 모델

```text
User ── 1:1 ── Account ── 1:N ── Order ── 1:N ── AccountTransaction
                    │                 │
                    └── 1:N ── Holding ── 1:N ── HoldingTransaction
Stock ── 1:N ── Holding / Order / StockMinuteCandle
```

- Account: 가상 현금 잔액과 동시성 제어용 버전
- Stock: 종목 마스터와 거래/수집 활성 상태
- Order: 요청·체결·거절 결과와 사용한 호가 시각
- Holding: 계좌별 보유 수량과 평균단가
- AccountTransaction, HoldingTransaction: 잔액·수량 변경 원장
- StockMinuteCandle: 종목·분봉 시각 복합 키의 확정 OHLCV

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1, Spring MVC, Spring Security |
| Persistence | Spring Data JPA, Hibernate, PostgreSQL 17, Flyway |
| Realtime | Spring WebSocket, KIS Open Trading API |
| Cache/Scale-out | Redis 세션 및 공유 캐시 확장 계획 |
| Frontend | React, TypeScript, Vite, lightweight-charts |
| Test | JUnit 5, Spring Test, Testcontainers, REST Docs |
| Local | Docker Compose, Gradle Wrapper |

## CS 관점의 설계 포인트

### 트랜잭션과 원자성

주문 체결, 잔액 변경, 보유 수량 변경, 원장 기록을 하나의 DB 트랜잭션으로 묶습니다. 예외가 발생하면 전체를 롤백해 “잔액만 차감되고 주식은 늘지 않는” 상태를 방지합니다.

### 동시성 제어

잔액 1,000,000원에 800,000원 매수 요청 2개가 동시에 들어오면 하나만 성공해야 합니다. 계좌와 보유 행의 잠금/버전 전략, 서비스 검증, PostgreSQL 제약조건으로 다음 불변식을 이중 보장합니다.

- 현금 잔액과 보유 수량은 음수가 될 수 없음
- 계좌·종목 조합의 Holding은 하나만 존재
- 동일 계좌의 동일 client_order_id는 한 번만 체결

### 멱등성과 데이터 신뢰성

네트워크 재시도와 중복 클릭을 고려해 계좌 범위의 멱등 키와 DB 유일 제약조건을 사용합니다. 호가 수신 후 5초가 지나면 주문을 거절하고, WebSocket 단절 중 추정 가격으로 체결하지 않습니다. 짧은 누락은 KIS 당일 분봉 REST API로 보정합니다.

### 인증 보안

서버 측 세션, BCrypt 비밀번호 해시, CSRF 방어, 세션 자격 증명 제거, 로그인 실패 제한과 감사 로그를 적용하는 방향으로 설계했습니다. 권한·상태·비밀번호 변경 시 세션을 폐기하고 다중 인스턴스에서는 Redis 세션으로 확장합니다.

## 트러블슈팅 및 해결 기록

문제의 증상뿐 아니라 원인과 지켜야 할 불변식을 함께 기록했습니다.

| 문제 | 원인 | 대응 |
| --- | --- | --- |
| Flyway checksum mismatch | 적용된 migration 파일 수정 또는 과거 개발 볼륨 잔존 | 적용 파일은 보존하고 새 버전 추가. 로컬 데이터 폐기가 가능한 경우에만 볼륨 재생성 |
| PostgreSQL 접속 실패 | 컨테이너 미기동, health check 미완료, 5432 충돌 | docker compose ps/logs, docker info, lsof, compose config로 원인 분리 |
| 오래된 호가 체결 위험 | WebSocket 단절 후 마지막 호가 재사용 | 수신 시각 검증, 5초 초과 호가는 주문에 사용하지 않음 |
| 중복 주문 | 클라이언트 재시도·중복 클릭 | 멱등 키와 DB 유일 제약조건으로 1회 처리 |
| 잔액/보유 불일치 | 주문 중 일부 단계만 반영 | 주문·상태 변경·원장 기록을 하나의 트랜잭션으로 처리 |
| 테스트의 개발 DB 오염 | 실행 환경 미분리 | bootRun은 영속 DB, test는 Testcontainers 임시 DB 사용 |

## 실행

### 백엔드

```bash
cd backend
docker compose up -d
./gradlew test
./gradlew bootRun
```

### 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

자세한 내용은 [backend README](backend/README.md)와 [frontend README](frontend/README.md)를 참고하세요.

## 현재 상태와 로드맵

현재 저장소에는 PostgreSQL 개발 환경, Flyway 초기 스키마, Spring Boot 기반, Vite·React 프론트엔드와 차트/로그인 화면 골격이 반영되어 있습니다. 인증·주문·시세 수집은 도메인 규칙과 통합 테스트를 우선 검증한 뒤 단계적으로 연결합니다.

1. 인증·계정 생명주기와 프론트엔드 세션 연결
2. Mock 가격 공급자로 주문 코어 및 동시성 테스트 완성
3. 주문/계좌/보유/원장 REST API 연결
4. KIS WebSocket 어댑터와 실시간 클라이언트 전송
5. 1분봉 저장·커서 조회·누락 보정
6. Redis 기반 세션/시세 공유와 운영 지표 확장

## 문서

- [MVP 시장 데이터 및 모의투자 요구사항](backend/docs/mvp-market-data-trading-requirements.md)
- [백엔드 실행 및 데이터베이스 안내](backend/README.md)
- [프론트엔드 실행 안내](frontend/README.md)

## 라이선스

현재 라이선스 정책을 정하는 중입니다.
