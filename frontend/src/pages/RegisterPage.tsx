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

  const isFormValid = 
    username && 
    password && 
    !usernameError && 
    !passwordError
  
  return (
    <AppShell
      title="Start with clean accounts, then grow toward team collaboration."
      description=""
      aside={
        <>
          <div className="aside-card">
            <h2></h2>
            <p></p>
          </div>
          <div className="aside-card">
            <h2></h2>
            <p></p>
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
                onChange={handleUsernameChange}
                className={usernameError ? 'input-error' : ''}
              />
              {usernameError && <span className="field-error">{usernameError}</span>}
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
              {isSubmitting ? '注册中...' : '注册并进入任务台'}
            </button>
          </form>
        </AuthCard>
      </div>
    </AppShell>
  )
}
