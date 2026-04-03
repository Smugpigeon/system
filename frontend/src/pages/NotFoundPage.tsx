import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="not-found-wrap">
      <section className="not-found-card fade-in">
        <p className="eyebrow">404</p>
        <h1>页面不存在</h1>
        <p>这个路由没有对应页面，可以返回登录页或任务页继续操作。</p>
        <div className="toolbar">
          <Link className="button-primary" to="/tasks">
            回到任务页
          </Link>
          <Link className="button-ghost" to="/login">
            回到登录页
          </Link>
        </div>
      </section>
    </div>
  )
}
