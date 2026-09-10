const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

type CsrfResponse = { token?: string }

function endpoint(path: string) {
  return `${apiBaseUrl}${path}`
}

async function readError(response: Response, fallback: string) {
  try {
    const body = await response.json() as { message?: string; detail?: string }
    return body.message ?? body.detail ?? fallback
  } catch {
    return fallback
  }
}

export async function login(email: string, password: string) {
  const csrfResponse = await fetch(endpoint('/api/auth/csrf'), {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })
  if (!csrfResponse.ok) throw new Error(await readError(csrfResponse, '보안 토큰을 가져오지 못했습니다.'))

  const csrf = await csrfResponse.json() as CsrfResponse
  const token = csrf.token ?? getCookie('XSRF-TOKEN')
  if (!token) throw new Error('보안 토큰이 없습니다. 잠시 후 다시 시도해 주세요.')

  const response = await fetch(endpoint('/api/auth/login'), {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': decodeURIComponent(token),
    },
    body: JSON.stringify({ email, password }),
  })
  if (!response.ok) throw new Error(await readError(response, '이메일 또는 비밀번호를 확인해 주세요.'))
}

function getCookie(name: string) {
  return document.cookie.split('; ').find((part) => part.startsWith(`${name}=`))?.split('=').slice(1).join('=')
}
