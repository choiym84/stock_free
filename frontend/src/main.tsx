import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles.css'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import { DashboardPage } from './pages/DashboardPage'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'

const pages = {
  '/signup': <SignupPage />,
  '/dashboard': <DashboardPage />,
  '/forgot-password': <ForgotPasswordPage />,
  '/reset-password': <ResetPasswordPage />,
} as const
const page = pages[window.location.pathname as keyof typeof pages] ?? <LoginPage />

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {page}
  </StrictMode>,
)
