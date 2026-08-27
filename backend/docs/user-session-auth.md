# User session authentication

## Endpoints

| Method | Path | Authentication | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/auth/csrf` | Public | Issue the CSRF cookie and response token |
| `POST` | `/api/v1/auth/register` | Public | Register a user |
| `POST` | `/api/v1/auth/login` | Public | Create an authenticated session |
| `POST` | `/api/v1/auth/logout` | Required | Invalidate the current session |
| `GET` | `/api/v1/users/me` | Required | Get the current user profile |

All responses except the `204 No Content` logout response use the common `ApiResponse` envelope.
Internal numeric user IDs and password hashes are not returned by the API.

## Browser flow

The browser client must include credentials on every authentication request. Before the first
`POST`, fetch a CSRF token:

```javascript
const csrfResponse = await fetch("http://localhost:8080/api/v1/auth/csrf", {
  credentials: "include",
});
const { data: csrf } = await csrfResponse.json();
```

Send the returned token using its `headerName` on each state-changing request. The browser sends
the matching `XSRF-TOKEN` cookie automatically:

```javascript
await fetch("http://localhost:8080/api/v1/auth/login", {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/json",
    [csrf.headerName]: csrf.token,
  },
  body: JSON.stringify({
    email: "user@example.com",
    password: "password123",
  }),
});
```

Keep the CSRF token in memory rather than browser storage. Fetch a new token after logout because
logout clears the existing CSRF cookie.

## Environment settings

- `CORS_ALLOWED_ORIGINS` is a comma-separated allowlist. It defaults to
  `http://localhost:5173` for local development.
- Set `SESSION_COOKIE_SECURE=true` when HTTPS is used in production.
- The session timeout is 30 minutes.

Production should serve the frontend and API from the same site. If they must be cross-site, the
cookie `SameSite` policy also needs a deliberate HTTPS-only configuration change.
