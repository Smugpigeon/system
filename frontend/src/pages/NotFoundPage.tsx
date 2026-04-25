import { Link } from 'react-router-dom'
import { AppShell } from '../layout/AppShell'

export function NotFoundPage() {
  return (
    <AppShell
      title="The route exists nowhere in this workspace."
      description="这不是有效页面。你可以回到个人工作台或我的团队入口继续操作。"
      aside={(
        <div className="aside-card">
          <h2>可访问入口</h2>
          <p>`/tasks` 用于个人工作台，`/teams` 用于团队入口，未登录用户会被自动重定向到登录页。</p>
        </div>
      )}
    >
      <div className="panel panel-stack">
        <p className="eyebrow">404</p>
        <h2>页面不存在</h2>
        <p>请检查链接是否正确，或从下面的入口返回系统。</p>
        <div className="toolbar">
          <Link className="button-primary" to="/tasks">
            返回工作台
          </Link>
          <Link className="button-ghost" to="/teams">
            返回我的团队
          </Link>
        </div>
      </div>
    </AppShell>
  )
}
