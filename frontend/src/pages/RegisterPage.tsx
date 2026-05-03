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
      setUsernameError('用户名只能包含字母、数字、下划线，长度4-20位')
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
    const hasLetter = /[A-Za-z]/.test(value)
    const hasNumber = /\d/.test(value)
    if (!hasLetter || !hasNumber) {
      setPasswordError('密码必须同时包含字母和数字')
      return false
    }
    setPasswordError('')
    return true
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const isUsernameValid = validateUsername(username)
    const isPasswordValid = validatePassword(password)

    if (!isUsernameValid || !isPasswordValid) {
      setError('请正确填写所有字段')
      return
    }

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
      title="Create clean accounts first, then build collaboration on top of them."
      description="注册时先把用户名、密码约束和落库安全性做好，这是后续团队角色权限控制的基础。"
      aside={(
        <>
          <div className="aside-card">
            <h2>注册规则</h2>
            <p>用户名限定 4-20 位字母/数字/下划线，密码至少 6 位，且必须同时包含字母和数字。</p>
          </div>
          <div className="aside-card">
            <h2>密码安全</h2>
            <p>密码不会明文存储，后端只保存哈希值。注册完成后会自动登录，方便直接演示系统功能。</p>
          </div>
        </>
      )}
    >
      <div className="auth-wrap">
        <AuthCard
          eyebrow="Register"
          title="创建新账号"
          description="注册成功后自动进入工作台，方便继续验证个人任务与团队空间能力。"
          footer={(
            <div className="auth-links">
              <span>已经有账号？</span>
              <Link to="/login">返回登录</Link>
            </div>
          )}
        >
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="register-username">用户名</label>
              <input
                id="register-username"
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
              <label htmlFor="register-password">密码</label>
              <input
                id="register-password"
                autoComplete="new-password"
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
              {isSubmitting ? '注册中...' : '注册并进入工作台'}
            </button>
          </form>
        </AuthCard>
      </div>
    </AppShell>
  )
}
