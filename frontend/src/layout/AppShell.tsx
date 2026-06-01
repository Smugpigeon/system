import type { PropsWithChildren, ReactNode } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

type AppShellProps = PropsWithChildren<{
  title?: string
  description?: string
  aside?: ReactNode
}>

/**
 * 全局外壳：顶部固定导航栏（品牌 + 主导航 + 用户区）+ 居中收窄的内容区。
 * 导航栏在所有页面统一呈现，未登录页（登录/注册）不显示用户区。
 */
export function AppShell({ children }: AppShellProps) {
  const { auth, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const navItems = [
    { label: '工作台', to: '/tasks' },
    { label: '我的团队', to: '/teams' },
  ]

  return (
    <div className="page">
      <header className="topbar">
        <div className="topbar__inner">
          <Link to={isAuthenticated ? '/tasks' : '/login'} className="brand">
            <span className="brand__spark" aria-hidden="true">✳</span>
            <span className="brand__name">任务台</span>
          </Link>

          {isAuthenticated && (
            <nav className="topbar__nav">
              {navItems.map((item) => {
                const active = location.pathname.startsWith(item.to)
                return (
                  <Link
                    key={item.to}
                    to={item.to}
                    className={`topbar__link ${active ? 'topbar__link--active' : ''}`}
                  >
                    {item.label}
                  </Link>
                )
              })}
            </nav>
          )}

          {isAuthenticated && (
            <div className="topbar__user">
              <span className="topbar__username">{auth?.username}</span>
              <button type="button" className="button-ghost button-sm" onClick={handleLogout}>
                退出登录
              </button>
            </div>
          )}
        </div>
      </header>

      <div className="app-shell">
        <main className="app-shell__main">
          <div className="main-stack">{children}</div>
        </main>
      </div>
    </div>
  )
}
