import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchMyTeams } from '../api/team'
import { getErrorMessage } from '../api/http'
import { Toast } from '../components/Toast'
import { useAuth } from '../context/useAuth'
import { AppShell } from '../layout/AppShell'
import { TEAM_ROLE_LABELS, type TeamRole, type TeamSummary } from '../types/team'

type RoleFilter = 'ALL' | TeamRole

const ROLE_FILTERS: { value: RoleFilter; label: string }[] = [
  { value: 'ALL', label: '全部' },
  { value: 'OWNER', label: '拥有者' },
  { value: 'ADMIN', label: '管理员' },
  { value: 'MEMBER', label: '成员' },
]

export function MyTeamsPage() {
  const { auth, logout } = useAuth()
  const [teams, setTeams] = useState<TeamSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingError, setLoadingError] = useState('')
  const [keyword, setKeyword] = useState('')
  const [roleFilter, setRoleFilter] = useState<RoleFilter>('ALL')
  const [toast, setToast] = useState<{
    message: string
    type: 'success' | 'error' | 'info'
  } | null>(null)

  const loadTeams = useCallback(async () => {
    try {
      setLoading(true)
      setLoadingError('')
      const data = await fetchMyTeams()
      setTeams(data)
    } catch (error) {
      const msg = getErrorMessage(error)
      setLoadingError(msg)
      setToast({ message: msg, type: 'error' })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadTeams()
  }, [loadTeams])

  const filteredTeams = useMemo(() => {
    const kw = keyword.trim().toLowerCase()
    return teams.filter((team) => {
      if (roleFilter !== 'ALL' && team.currentUserRole !== roleFilter) {
        return false
      }
      if (kw && !team.name.toLowerCase().includes(kw)) {
        return false
      }
      return true
    })
  }, [teams, keyword, roleFilter])

  const summary = useMemo(() => {
    return {
      total: teams.length,
      owned: teams.filter((t) => t.currentUserRole === 'OWNER').length,
      admin: teams.filter((t) => t.currentUserRole === 'ADMIN').length,
    }
  }, [teams])

  return (
    <AppShell
      title="My Teams"
      description="查看你所在的团队、当前角色与协作概况"
      aside={
        <>
          <div className="aside-card">
            <h2>快捷导航</h2>
            <ul className="checkpoint-list">
              <li className="checkpoint-item">
                <Link to="/tasks" className="checkpoint-title">
                  返回任务工作台
                </Link>
                <span className="checkpoint-copy">查看个人与团队任务</span>
              </li>
              <li className="checkpoint-item">
                <span className="checkpoint-title">我的团队</span>
                <span className="checkpoint-copy">当前页面</span>
              </li>
            </ul>
          </div>

          <div className="aside-card">
            <h2>提示</h2>
            <ul className="roadmap-list">
              <li className="roadmap-item">
                <span className="roadmap-title">角色说明</span>
                <span className="roadmap-copy">
                  拥有者可管理成员；管理员可分配任务；成员负责执行。
                </span>
              </li>
            </ul>
          </div>
        </>
      }
    >
      <header className="main-header">
        <div>
          <p className="eyebrow">Authenticated workspace</p>
          <h1>{auth?.username} 的团队列表</h1>
          <p>共 {summary.total} 个团队，其中拥有者 {summary.owned} 个，管理员 {summary.admin} 个。</p>
        </div>
        <div className="toolbar">
          <button
            className="button-ghost"
            type="button"
            onClick={loadTeams}
          >
            刷新列表
          </button>
          <button className="button-secondary" type="button" onClick={logout}>
            退出登录
          </button>
        </div>
      </header>

      <section className="filters-section">
        <div className="filters-row" style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <input
            className="filter-input"
            type="text"
            placeholder="按团队名称搜索"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
          <select
            className="filter-input"
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value as RoleFilter)}
          >
            {ROLE_FILTERS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                角色：{opt.label}
              </option>
            ))}
          </select>
          <span className="filter-summary">
            显示 {filteredTeams.length} / {teams.length}
          </span>
        </div>
      </section>

      <section className="content-grid">
        <article className="panel">
          <header className="panel-header">
            <p className="eyebrow">Team list</p>
            <h2 className="panel-title">我的团队</h2>
            <p className="panel-subtitle">点击团队卡片进入工作台查看相关任务</p>
          </header>

          {loading ? (
            <div className="message message--note">正在加载团队列表...</div>
          ) : loadingError ? (
            <div className="message message--error">{loadingError}</div>
          ) : filteredTeams.length === 0 ? (
            <div className="message message--note">
              {teams.length === 0
                ? '你还没有加入任何团队。'
                : '当前筛选条件下没有团队，试试调整筛选项。'}
            </div>
          ) : (
            <ul className="detail-list">
              {filteredTeams.map((team) => (
                <li key={team.id} className="detail-item">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
                    <div>
                      <span className="detail-title">{team.name}</span>
                      <span className="detail-copy">
                        {team.memberCount} 位成员 · {team.teamTaskCount} 个团队任务
                      </span>
                    </div>
                    <span className={`role-badge role-badge--${team.currentUserRole.toLowerCase()}`}>
                      {TEAM_ROLE_LABELS[team.currentUserRole]}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </article>
      </section>

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </AppShell>
  )
}