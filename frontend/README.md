# Stock Free Frontend

백엔드와 독립적으로 실행·빌드되는 Vite + React + TypeScript 프로젝트입니다. 시세 화면을
확장할 수 있도록 `lightweight-charts` 기반의 재사용 가능한 `MarketChart`도 포함합니다.

## 실행

```bash
npm install
npm run dev
```

개발 중 `/api` 요청은 `http://localhost:8080` 백엔드로 프록시됩니다. 배포 시에는
`.env`의 `VITE_API_BASE_URL`에 백엔드 origin을 지정하세요.

## 인증 계약

- `GET /api/auth/csrf`: 세션 쿠키와 CSRF 토큰 발급
- `POST /api/auth/login`: `{ "email": string, "password": string }`를 JSON으로 전송
- 두 요청 모두 세션 유지를 위해 `credentials: include`를 사용
- 로그인 요청에는 `X-XSRF-TOKEN` 헤더를 전송

로그인 성공 후 `/dashboard`로 이동합니다. 백엔드의 실제 엔드포인트가 다르면
`src/lib/authApi.ts`의 두 경로만 변경하면 됩니다.

## 차트 데이터 연결

`MarketChart`는 백엔드의 분 단위 OHLC 응답을 `CandlestickData[]`로 변환해 전달하는 구조입니다.
차트 라이브러리는 백엔드 모듈과 분리된 프론트엔드 의존성이므로 백엔드 Gradle 빌드에는 영향을 주지 않습니다.
