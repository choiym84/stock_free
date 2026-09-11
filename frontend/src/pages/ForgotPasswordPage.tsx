import { FormEvent, useState } from 'react'
import { requestPasswordReset } from '../lib/authApi'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [completed, setCompleted] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      await requestPasswordReset(email.trim())
      setCompleted(true)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '요청에 실패했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="login-layout">
      <section className="brand-panel" aria-label="Stock Free 소개">
        <div className="brand-mark">SF</div>
        <p className="eyebrow">ACCOUNT RECOVERY</p>
        <h1>다시<br /><em>돌아오세요.</em></h1>
      </section>
      <section className="form-panel">
        <div className="form-wrap">
          <div className="mobile-logo"><span className="brand-mark small">SF</span> STOCK FREE</div>
          {completed ? (
            <>
              <p className="eyebrow">CHECK YOUR EMAIL</p>
              <h2>요청을 접수했어요</h2>
              <p className="form-subtitle">계정이 있다면 비밀번호 재설정 안내를 확인해 주세요.</p>
              <a className="primary-link" href="/">로그인 페이지로 돌아가기 <span aria-hidden="true">→</span></a>
            </>
          ) : (
            <>
              <p className="eyebrow">RESET PASSWORD</p>
              <h2>비밀번호를 잊으셨나요?</h2>
              <p className="form-subtitle">가입한 이메일을 입력하면 재설정 안내를 보내드려요.</p>
              <form onSubmit={handleSubmit} noValidate>
                <label htmlFor="email">이메일</label>
                <input id="email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" maxLength={255} required />
                {error && <p className="error" role="alert">{error}</p>}
                <button type="submit" disabled={busy}>{busy ? '전송 중…' : '재설정 안내 받기'} <span aria-hidden="true">→</span></button>
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
