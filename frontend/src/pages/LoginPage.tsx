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

  if (isAuthenticated) {
    return <Navigate to="/tasks" replace />
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
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
      title="Ship the minimum viable collaboration system with a solid base."
      description="先完成最小可运行版本，再给组内同学留下明确的接口、模块和协作空间。"
      aside={
        <>
          <div className="aside-card">
            <h2>为什么先做登录</h2>
            <p>
              认证是 Lab1 的第一层边界。没有用户身份，任务隔离与权限控制都没法成立。
            </p>
          </div>
          <div className="aside-card">
            <h2>本次实现策略</h2>
            <p>
              用 JWT 保持前端刷新后的登录态，页面直接进入任务台，避免演示链路过长。
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
                onChange={(event) => setUsername(event.target.value)}
              />
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
                onChange={(event) => setPassword(event.target.value)}
              />
            </div>

            {error ? <div className="message message--error">{error}</div> : null}

            <button className="button-primary" disabled={isSubmitting} type="submit">
              {isSubmitting ? '登录中...' : '登录并进入任务台'}
            </button>
          </form>
        </AuthCard>
      </div>
    </AppShell>
  )
}
