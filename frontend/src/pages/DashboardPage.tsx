import { useEffect, useState } from 'react'
import { CurrentUser, getCurrentUser, logout } from '../lib/authApi'

export function DashboardPage() {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    getCurrentUser()
      .then(setUser)
      .catch(() => window.location.assign('/'))
  }, [])

  async function handleLogout() {
    setBusy(true)
    try {
      await logout()
      window.location.assign('/')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '로그아웃에 실패했습니다.')
      setBusy(false)
    }
  }

  if (!user) {
    return <main className="dashboard-page"><p className="dashboard-loading">대시보드를 불러오는 중…</p></main>
  }

  return (
    <main className="dashboard-page">
      <header className="dashboard-header">
        <a className="dashboard-logo" href="/">SF <span>STOCK FREE</span></a>
        <div className="dashboard-account">
          <span>{user.nickname}</span>
          <button className="logout-button" type="button" onClick={handleLogout} disabled={busy}>로그아웃</button>
        </div>
      </header>
      <section className="dashboard-content">
        <p className="eyebrow">YOUR MARKET DESK</p>
        <h1>{user.nickname}님, 반가워요.</h1>
        <p className="dashboard-subtitle">오늘의 투자 흐름을 한눈에 확인해 보세요.</p>
        <div className="dashboard-grid">
          <article className="dashboard-card dashboard-card-primary">
            <p className="card-label">ACCOUNT</p>
            <strong>{user.email}</strong>
            <span>안전하게 로그인되어 있습니다.</span>
          </article>
          <article className="dashboard-card">
            <p className="card-label">MARKET STATUS</p>
            <strong>MARKET IS OPEN</strong>
            <span>09:00—15:30 KST</span>
          </article>
          <article className="dashboard-card dashboard-card-wide">
            <p className="card-label">WATCHLIST</p>
            <strong>관심 종목을 준비 중입니다.</strong>
            <span>시세 데이터가 연결되면 이곳에서 확인할 수 있어요.</span>
          </article>
        </div>
        {error && <p className="error" role="alert">{error}</p>}
      </section>
    </main>
  )
}
