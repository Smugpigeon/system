import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createTeam, fetchMyTeams } from '../api/teams'
import { getErrorMessage } from '../api/http'
import { Toast } from '../components/Toast'
import { useAuth } from '../context/useAuth'
import { AppShell } from '../layout/AppShell'
import type { TeamSummary } from '../types/team'
import { TEAM_ROLE_LABELS, TEAM_STATUS_LABELS } from '../types/team'

export function TeamsPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [teams, setTeams] = useState<TeamSummary[]>([])
  const [teamName, setTeamName] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null)

  const loadTeams = useCallback(async () => {
    try {
      setLoading(true)
      setError('')
      setTeams(await fetchMyTeams())
    } catch (loadError) {
      setError(getErrorMessage(loadError))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadTeams()
  }, [loadTeams])

  const handleCreateTeam = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!teamName.trim()) {
      setToast({ message: '团队名称不能为空', type: 'error' })
      return
    }

    try {
      setSubmitting(true)
      const createdTeam = await createTeam(teamName.trim())
      setToast({ message: '团队创建成功', type: 'success' })
      setTeamName('')
      await loadTeams()
      navigate(`/teams/${createdTeam.id}`)
    } catch (submitError) {
      setToast({ message: getErrorMessage(submitError), type: 'error' })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AppShell
      title="Organize collaboration through explicit teams, roles and workspaces."
      description="这里是 Lab3 的团队入口。你可以查看自己加入的有效团队，创建新团队，并进入团队空间管理成员、任务与依赖。"
      aside={(
        <>
          <div className="aside-card">
            <h2>角色分层</h2>
            <p>Owner 管成员和角色，Admin 管团队任务，Member 浏览团队任务并只改自己任务的状态。</p>
          </div>
          <div className="aside-card">
            <h2>页面职责</h2>
            <p>“我的团队”只做团队入口与概览；成员管理和团队任务管理集中在团队空间页面。</p>
          </div>
        </>
      )}
    >
      <header className="main-header">
        <div>
          <p className="eyebrow">My Teams</p>
          <h1>团队协作入口</h1>
          <p>先进入团队，再在团队空间中做成员、角色和团队任务管理，避免把团队权限逻辑散落到个人工作台。</p>
        </div>
        <div className="toolbar">
          <button className="button-ghost" type="button" onClick={() => navigate('/tasks')}>
            返回工作台
          </button>
          <button className="button-ghost" type="button" onClick={logout}>
            退出登录
          </button>
        </div>
      </header>

      <section className="workspace-grid workspace-grid--narrow">
        <div className="panel panel-stack">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Create Team</p>
              <h2>创建新团队</h2>
            </div>
          </div>
          <form className="inline-form" onSubmit={handleCreateTeam}>
            <input
              value={teamName}
              maxLength={80}
              placeholder="例如：Group14-Lab3 后端联调组"
              onChange={(event) => setTeamName(event.target.value)}
            />
            <button className="button-primary" type="submit" disabled={submitting}>
              {submitting ? '创建中...' : '创建团队'}
            </button>
          </form>
        </div>

        <div className="panel panel-stack">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Team Directory</p>
              <h2>我的团队</h2>
            </div>
            <button className="button-ghost" type="button" onClick={loadTeams}>
              刷新列表
            </button>
          </div>

          {error ? <div className="message message--error">{error}</div> : null}

          {loading ? (
            <div className="empty-state">
              <p>团队列表加载中...</p>
            </div>
          ) : teams.length ? (
            <div className="team-grid">
              {teams.map((team) => (
                <article key={team.id} className="team-card">
                  <div className="team-card__top">
                    <div>
                      <p className="team-card__eyebrow">Role / {TEAM_ROLE_LABELS[team.currentUserRole]}</p>
                      <h3>{team.name}</h3>
                    </div>
                    <span className={`role-badge role-badge--${team.currentUserRole.toLowerCase()}`}>
                      {TEAM_ROLE_LABELS[team.currentUserRole]}
                    </span>
                  </div>
                  <div className="team-card__meta">
                    <span>状态：{TEAM_STATUS_LABELS[team.status]}</span>
                    <span>成员数：{team.memberCount}</span>
                    <span>团队任务：{team.teamTaskCount}</span>
                  </div>
                  <button
                    className="button-primary"
                    type="button"
                    onClick={() => navigate(`/teams/${team.id}`)}
                  >
                    进入团队空间
                  </button>
                </article>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <p>你还没有加入任何团队。可以先创建团队，再把其他同学加进来。</p>
            </div>
          )}
        </div>
      </section>

      {toast ? (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      ) : null}
    </AppShell>
  )
}
