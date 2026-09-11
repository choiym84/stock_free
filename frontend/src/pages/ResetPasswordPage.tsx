import { FormEvent, useState } from 'react'
import { resetPassword } from '../lib/authApi'

export function ResetPasswordPage() {
  const token = new URLSearchParams(window.location.search).get('token') ?? ''
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(token ? '' : '유효하지 않은 재설정 링크입니다.')
  const [completed, setCompleted] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (password !== confirmation) {
      setError('비밀번호가 일치하지 않습니다.')
      return
    }
    setBusy(true)
    try {
      await resetPassword(token, password)
      setCompleted(true)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '비밀번호 변경에 실패했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="login-layout">
      <section className="brand-panel" aria-label="Stock Free 소개">
        <div className="brand-mark">SF</div>
        <p className="eyebrow">NEW CREDENTIALS</p>
        <h1>더 안전한<br /><em>비밀번호로.</em></h1>
      </section>
      <section className="form-panel">
        <div className="form-wrap">
          <div className="mobile-logo"><span className="brand-mark small">SF</span> STOCK FREE</div>
          {completed ? (
            <>
              <p className="eyebrow">PASSWORD UPDATED</p>
              <h2>비밀번호가 변경됐어요</h2>
              <p className="form-subtitle">새 비밀번호로 다시 로그인해 주세요.</p>
              <a className="primary-link" href="/">로그인 페이지로 돌아가기 <span aria-hidden="true">→</span></a>
            </>
          ) : (
            <>
              <p className="eyebrow">RESET PASSWORD</p>
              <h2>새 비밀번호를 설정하세요</h2>
              <p className="form-subtitle">8자 이상의 새로운 비밀번호를 입력해 주세요.</p>
              <form onSubmit={handleSubmit} noValidate>
                <label htmlFor="password">새 비밀번호</label>
                <input id="password" type="password" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="8자 이상" minLength={8} maxLength={64} required disabled={!token} />
                <label htmlFor="confirmation">새 비밀번호 확인</label>
                <input id="confirmation" type="password" autoComplete="new-password" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} placeholder="비밀번호를 다시 입력해 주세요" minLength={8} maxLength={64} required disabled={!token} />
                {error && <p className="error" role="alert">{error}</p>}
                <button type="submit" disabled={busy || !token}>{busy ? '변경 중…' : '비밀번호 변경'} <span aria-hidden="true">→</span></button>
              </form>
              <p className="signup"><a href="/">로그인으로 돌아가기</a></p>
            </>
          )}
          <p className="security-note"><span>▣</span> 세션과 CSRF로 안전하게 보호됩니다.</p>
        </div>
      </section>
    </main>
  )
}
