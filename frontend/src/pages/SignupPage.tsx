import { FormEvent, useState } from 'react'
import { register } from '../lib/authApi'

export function SignupPage() {
  const [email, setEmail] = useState('')
  const [nickname, setNickname] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirmation, setPasswordConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [completed, setCompleted] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (password !== passwordConfirmation) {
      setError('비밀번호가 일치하지 않습니다.')
      return
    }

    setBusy(true)
    try {
      await register(email.trim(), nickname.trim(), password)
      setCompleted(true)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '회원가입에 실패했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="login-layout">
      <section className="brand-panel" aria-label="Stock Free 소개">
        <div className="brand-mark">SF</div>
        <p className="eyebrow">START YOUR JOURNEY</p>
        <h1>투자의 흐름을<br /><em>더 자유롭게.</em></h1>
        <p className="brand-copy">간단한 가입으로 시장의 흐름을 확인하고,<br />나만의 투자 리듬을 만들어 보세요.</p>
        <div className="market-note"><span className="pulse" /> MARKET IS OPEN <strong>09:00—15:30 KST</strong></div>
      </section>

      <section className="form-panel">
        <div className="form-wrap">
          <div className="mobile-logo"><span className="brand-mark small">SF</span> STOCK FREE</div>
          {completed ? (
            <>
              <p className="eyebrow">WELCOME TO STOCK FREE</p>
              <h2>가입이 완료됐어요</h2>
              <p className="form-subtitle">이메일로 전송된 인증 링크를 확인한 뒤 로그인해 주세요.</p>
              <a className="primary-link" href="/">로그인 페이지로 돌아가기 <span aria-hidden="true">→</span></a>
            </>
          ) : (
            <>
              <p className="eyebrow">CREATE ACCOUNT</p>
              <h2>새롭게 시작해 보세요</h2>
              <p className="form-subtitle">계정을 만들고 오늘의 시장을 확인해 보세요.</p>
              <form onSubmit={handleSubmit} noValidate>
                <label htmlFor="email">이메일</label>
                <input id="email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" maxLength={255} required />
                <label htmlFor="nickname">닉네임</label>
                <input id="nickname" type="text" autoComplete="nickname" value={nickname} onChange={(event) => setNickname(event.target.value)} placeholder="stock_free" minLength={2} maxLength={20} pattern="[A-Za-z0-9가-힣_]+" required />
                <label htmlFor="password">비밀번호</label>
                <input id="password" type="password" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="8자 이상" minLength={8} maxLength={64} required />
                <label htmlFor="password-confirmation">비밀번호 확인</label>
                <input id="password-confirmation" type="password" autoComplete="new-password" value={passwordConfirmation} onChange={(event) => setPasswordConfirmation(event.target.value)} placeholder="비밀번호를 다시 입력해 주세요" minLength={8} maxLength={64} required />
                {error && <p className="error" role="alert">{error}</p>}
                <button type="submit" disabled={busy}>{busy ? '가입 중…' : '회원가입'} <span aria-hidden="true">→</span></button>
              </form>
              <p className="signup">이미 계정이 있으신가요? <a href="/">로그인</a></p>
            </>
          )}
          <p className="security-note"><span>▣</span> 세션과 CSRF로 안전하게 보호됩니다.</p>
        </div>
      </section>
    </main>
  )
}
