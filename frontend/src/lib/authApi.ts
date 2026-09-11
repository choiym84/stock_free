const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
const authBasePath = '/api/v1/auth'

type CsrfResponse = { headerName: string; token: string }
type ApiEnvelope<T> = { data?: T; error?: { message?: string; details?: string[] } }
export type CurrentUser = {
  email: string
  nickname: string
  role: string
  status: string
  emailVerified: boolean
  createdAt: string
}

function endpoint(path: string) {
  return `${apiBaseUrl}${path}`
}

async function readError(response: Response, fallback: string) {
  try {
    const body = await response.json() as ApiEnvelope<unknown> & { message?: string; detail?: string }
    return body.error?.message ?? body.error?.details?.join(', ') ?? body.message ?? body.detail ?? fallback
  } catch {
    return fallback
  }
}

async function csrfToken() {
  const response = await fetch(endpoint(`${authBasePath}/csrf`), {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) throw new Error(await readError(response, '보안 토큰을 가져오지 못했습니다.'))

  const body = await response.json() as ApiEnvelope<CsrfResponse>
  if (!body.data?.token) throw new Error('보안 토큰이 없습니다. 잠시 후 다시 시도해 주세요.')
  return body.data
}

async function postWithCsrf(path: string, payload: unknown) {
  const csrf = await csrfToken()
  const response = await fetch(endpoint(`${authBasePath}${path}`), {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  })
  if (!response.ok) throw new Error(await readError(response, '요청을 처리하지 못했습니다.'))
}

export async function login(email: string, password: string) {
  await postWithCsrf('/login', { email, password })
}

export async function register(email: string, nickname: string, password: string) {
  await postWithCsrf('/register', { email, nickname, password })
}

export async function getCurrentUser() {
  const response = await fetch(endpoint('/api/v1/users/me'), {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) throw new Error(await readError(response, '로그인 상태를 확인하지 못했습니다.'))
  const body = await response.json() as ApiEnvelope<CurrentUser>
  if (!body.data) throw new Error('사용자 정보를 가져오지 못했습니다.')
  return body.data
}

export async function logout() {
  await postWithCsrf('/logout', undefined)
}

export async function requestPasswordReset(email: string) {
  await postWithCsrf('/password-resets', { email })
}

export async function resetPassword(token: string, newPassword: string) {
  await postWithCsrf('/password-resets/confirm', { token, newPassword })
}
