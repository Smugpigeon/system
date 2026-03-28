import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { register } from '../api/auth'
import { getErrorMessage } from '../api/http'
import { AuthCard } from '../components/AuthCard'
import { useAuth } from '../context/useAuth'
import { AppShell } from '../layout/AppShell'

export function RegisterPage() {
  const navigate = useNavigate()
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
      const payload = await register({ username, password })
      setAuthSession(payload)
      navigate('/tasks', { replace: true })
    } catch (submitError) {
      setError(getErrorMessage(submitError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AppShell
      title="Start with clean accounts, then grow toward team collaboration."
      description="注册阶段先把用户名、密码约束和数据库落库做好，这是后续权限模型的基础。"
      aside={
        <>
          <div className="aside-card">
            <h2>字段约束</h2>
            <p>用户名限定 4-20 位且唯一；密码至少 6 位，并强制同时包含字母和数字。</p>
          </div>
          <div className="aside-card">
            <h2>安全要求</h2>
            <p>密码只会以哈希形式存储，不会在数据库中明文保存。</p>
          </div>
        </>
      }
    >
      <div className="auth-wrap">
        <AuthCard
          eyebrow="Register"
          title="先创建一个可登录的账号"
          description="注册成功后自动登录，方便你直接进入任务管理界面演示。"
          footer={
            <div className="auth-links">
              <span>已经有账号？</span>
              <Link to="/login">返回登录</Link>
            </div>
          }
        >
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="register-username">用户名</label>
              <input
                id="register-username"
                autoComplete="username"
                maxLength={20}
                minLength={4}
                pattern="[A-Za-z0-9_]{4,20}"
                placeholder="4-20 位字母、数字或下划线,例如：lab1_team01"
                required
                value={username}
                onChange={(event) => setUsername(event.target.value)}
              />
            </div>

            <div className="field">
              <label htmlFor="register-password">密码</label>
              <input
                id="register-password"
                autoComplete="new-password"
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
              {isSubmitting ? '注册中...' : '注册并进入任务台'}
            </button>
          </form>
        </AuthCard>
      </div>
    </AppShell>
  )
}
