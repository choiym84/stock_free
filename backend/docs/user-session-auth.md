# User session authentication

## Endpoints

| Method | Path | Authentication | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/auth/csrf` | Public | Issue the CSRF cookie and response token |
| `POST` | `/api/v1/auth/register` | Public | Register a user |
| `POST` | `/api/v1/auth/login` | Public | Create an authenticated session |
| `POST` | `/api/v1/auth/logout` | Required | Invalidate the current session |
| `POST` | `/api/v1/auth/email-verifications` | Public | Resend an email verification token |
| `POST` | `/api/v1/auth/email-verifications/confirm` | Public | Verify an email with a one-time token |
| `POST` | `/api/v1/auth/password-resets` | Public | Request a password reset; always returns 202 |
| `POST` | `/api/v1/auth/password-resets/confirm` | Public | Set a password with a one-time token |
| `GET` | `/api/v1/users/me` | Required | Get the current user profile |
| `PATCH` | `/api/v1/users/me/nickname` | Required | Change nickname |
| `PUT` | `/api/v1/users/me/password` | Required | Change password after current-password check |
| `DELETE` | `/api/v1/users/me` | Required | Withdraw after current-password check |
| `GET` | `/api/v1/users/me/authentication-events` | Required | Page through personal login attempts |
| `GET` | `/api/v1/admin/users` | Admin | Page through users |
| `PATCH` | `/api/v1/admin/users/{id}/role` | Admin | Change another user's role |
| `PATCH` | `/api/v1/admin/users/{id}/status` | Admin | Change another user's status |
| `GET` | `/api/v1/admin/authentication-events` | Admin | Page through all login attempts |

Responses with bodies use the common `ApiResponse` envelope. Password hashes and raw account tokens
are never returned. Numeric IDs are exposed only to administrators for user-management operations.

New registrations have `emailVerified=false`. With the default
`REQUIRE_EMAIL_VERIFICATION=true`, login remains disabled until the email token is confirmed.
Verification and reset tokens expire, can be used once, and are stored in PostgreSQL only as SHA-256
hashes. Password reset requests deliberately return the same `202 Accepted` for known and unknown
email addresses.

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
- Five failed attempts per email or 20 per IP in 15 minutes produce `429 LOGIN_RATE_LIMITED` and a
  `Retry-After` header. Success, failure, and rate-limit events are audited.
- `ACCOUNT_NOTIFICATION_DELIVERY=console` is local-only. Use `smtp` with `MAIL_*` settings in
  production.
- The default profile stores sessions locally. Use `SPRING_PROFILES_ACTIVE=redis-session` for a
  shared indexed Redis session store in a multi-instance deployment.

Production should serve the frontend and API from the same site. If they must be cross-site, the
cookie `SameSite` policy also needs a deliberate HTTPS-only configuration change.

Changing a role, status, or password, resetting a password, or withdrawing expires all existing
sessions. With local sessions this takes effect on the next request to the same instance. With the
`redis-session` profile the indexed sessions are deleted across all instances.
