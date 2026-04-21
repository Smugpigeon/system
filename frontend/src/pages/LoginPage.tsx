import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { getErrorMessage } from '../api/http'
import { AuthCard } from '../components/AuthCard'
import { useAuth } from '../context/useAuth'
import { AppShell } from '../layout/AppShell'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, setAuthSession } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [usernameError, setUsernameError] = useState('')
  const [passwordError, setPasswordError] = useState('')

  if (isAuthenticated) {
    return <Navigate to="/tasks" replace />
  }

  const validateUsername = (value: string) => {
    if (!value) {
      setUsernameError('')
      return false
    }
    const usernameRegex = /^[A-Za-z0-9_]{4,20}$/
    if (!usernameRegex.test(value)) {
      setUsernameError('用户名格式不正确（4-20位字母、数字、下划线）')
      return false
    }
    setUsernameError('')
    return true
  }

  const validatePassword = (value: string) => {
    if (!value) {
      setPasswordError('')
      return false
    }
    if (value.length < 6) {
      setPasswordError('密码长度至少6位')
      return false
    }
    setPasswordError('')
    return true
  }

  const handleUsernameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value
    setUsername(value)
    validateUsername(value)
  }

  const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value
    setPassword(value)
    validatePassword(value)
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!validateUsername(username) || !validatePassword(password)) {
      setError('请正确填写用户名和密码')
      return
    }

    try {
      setIsSubmitting(true)
      setError('')
      const payload = await login({ username, password })
      setAuthSession(payload)
      const nextPath = (location.state as { from?: string } | null)?.from ?? '/tasks'
      navigate(nextPath, { replace: true })
    } catch (submitError) {
      setError(getErrorMessage(submitError))
    } finally {
      setIsSubmitting(false)
    }
  }

  const isFormValid = 
    username && 
    password && 
    !usernameError && 
    !passwordError
  
  return (
    <AppShell
      title="Ship the minimum viable collaboration system with a solid base."
      description=""
      aside={
        <>
          <div className="aside-card">
            <h2></h2>
            <p>
              
            </p>
          </div>
          <div className="aside-card">
            <h2></h2>
            <p>
              
            </p>
          </div>
        </>
      }
    >
      <div className="auth-wrap">
        <AuthCard
          eyebrow="Login"
          title="进入任务管理台"
          description="使用用户名和密码登录。未登录用户不能访问任务页面。"
          footer={
            <div className="auth-links">
              <span>还没有账号？</span>
              <Link to="/register">去注册</Link>
            </div>
          }
        >
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="login-username">用户名</label>
              <input
                id="login-username"
                autoComplete="username"
                maxLength={20}
                minLength={4}
                pattern="[A-Za-z0-9_]{4,20}"
                placeholder="4-20 位字母、数字或下划线"
                required
                value={username}
                onChange={handleUsernameChange}
                className={usernameError ? 'input-error' : ''}
              />
              {usernameError && <span className="field-error">{usernameError}</span>}
            </div>

            <div className="field">
              <label htmlFor="login-password">密码</label>
              <input
                id="login-password"
                autoComplete="current-password"
                minLength={6}
                placeholder="至少 6 位，且包含字母与数字"
                required
                type="password"
                value={password}
                onChange={handlePasswordChange}
                className={passwordError ? 'input-error' : ''}
              />
              {passwordError && <span className="field-error">{passwordError}</span>}
            </div>

            {error ? <div className="message message--error">{error}</div> : null}

            <button 
              className="button-primary" 
              disabled={isSubmitting || !isFormValid} 
              type="submit"
            >
              {isSubmitting ? '登录中...' : '登录并进入任务台'}
            </button>
          </form>
        </AuthCard>
      </div>
    </AppShell>
  )
}
