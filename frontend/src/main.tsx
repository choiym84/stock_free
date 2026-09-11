import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles.css'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'

const page = window.location.pathname === '/signup' ? <SignupPage /> : <LoginPage />

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {page}
  </StrictMode>,
)
