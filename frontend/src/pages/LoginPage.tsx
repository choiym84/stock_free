import { FormEvent, useState } from 'react'
import { login } from '../lib/authApi'

export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      await login(email.trim(), password)
      window.location.assign('/dashboard')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '로그인에 실패했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="login-layout">
      <section className="brand-panel" aria-label="Stock Free 소개">
        <div className="brand-mark">SF</div>
        <p className="eyebrow">SMART INVESTING, MADE SIMPLE</p>
        <h1>투자의 흐름을<br /><em>더 자유롭게.</em></h1>
        <p className="brand-copy">복잡한 숫자 속에서 중요한 순간을 발견하고,<br />나만의 투자 리듬을 만들어 보세요.</p>
        <div className="market-note"><span className="pulse" /> MARKET IS OPEN <strong>09:00—15:30 KST</strong></div>
      </section>

      <section className="form-panel">
        <div className="form-wrap">
          <div className="mobile-logo"><span className="brand-mark small">SF</span> STOCK FREE</div>
          <p className="eyebrow">WELCOME BACK</p>
          <h2>다시 만나서 반가워요</h2>
          <p className="form-subtitle">계정에 로그인하고 오늘의 시장을 확인해 보세요.</p>
          <form onSubmit={handleSubmit} noValidate>
            <label htmlFor="email">이메일</label>
            <input id="email" type="email" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" required />
            <div className="label-row"><label htmlFor="password">비밀번호</label><a href="/forgot-password">비밀번호 찾기</a></div>
            <input id="password" type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="••••••••" minLength={8} required />
            {error && <p className="error" role="alert">{error}</p>}
            <button type="submit" disabled={busy}>{busy ? '로그인 중…' : '로그인'} <span aria-hidden="true">→</span></button>
          </form>
          <p className="signup">아직 계정이 없으신가요? <a href="/signup">회원가입</a></p>
          <p className="security-note"><span>▣</span> 세션과 CSRF로 안전하게 보호됩니다.</p>
        </div>
      </section>
    </main>
  )
}
