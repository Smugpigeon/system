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

  return (
    <AppShell
      title="Sign in before touching any personal or team task data."
      description="认证链路是整个系统的第一层边界。只有登录态稳定了，团队角色和任务隔离才能成立。"
      aside={(
        <>
          <div className="aside-card">
            <h2>Lab3 登录后</h2>
            <p>进入个人工作台后，你可以查看个人任务、团队任务和任务依赖，也可以跳转到我的团队和团队空间。</p>
          </div>
          <div className="aside-card">
            <h2>登录态保持</h2>
            <p>前端会保留 JWT，页面刷新后仍维持有效登录状态；后端统一校验令牌和角色权限。</p>
          </div>
        </>
      )}
    >
      <div className="auth-wrap">
        <AuthCard
          eyebrow="Login"
          title="进入任务系统"
          description="输入用户名和密码后进入工作台。未登录用户不能访问个人任务与团队空间。"
          footer={(
            <div className="auth-links">
              <span>还没有账号？</span>
              <Link to="/register">去注册</Link>
            </div>
          )}
        >
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="login-username">用户名</label>
              <input
                id="login-username"
                autoComplete="username"
                maxLength={20}
                minLength={4}
                value={username}
                onChange={(event) => {
                  setUsername(event.target.value)
                  validateUsername(event.target.value)
                }}
              />
              {usernameError ? <span className="field-error">{usernameError}</span> : null}
            </div>

            <div className="field">
              <label htmlFor="login-password">密码</label>
              <input
                id="login-password"
                autoComplete="current-password"
                type="password"
                minLength={6}
                value={password}
                onChange={(event) => {
                  setPassword(event.target.value)
                  validatePassword(event.target.value)
                }}
              />
              {passwordError ? <span className="field-error">{passwordError}</span> : null}
            </div>

            {error ? <div className="message message--error">{error}</div> : null}

            <button className="button-primary" type="submit" disabled={isSubmitting}>
              {isSubmitting ? '登录中...' : '登录并进入工作台'}
            </button>
          </form>
        </AuthCard>
      </div>
    </AppShell>
  )
}
