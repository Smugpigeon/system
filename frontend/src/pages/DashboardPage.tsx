import {
  startTransition,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react'
import { createTask, deleteTask, fetchTasks, fetchTeamTasks, updateTask, type TaskQueryParams } from '../api/tasks'
import { fetchMyTeams } from '../api/team'
import { getErrorMessage } from '../api/http'
import { TaskForm } from '../components/TaskForm'
import { TaskList } from '../components/TaskList'
import { TaskFilters, type FilterOptions } from '../components/TaskFilters'
import { Toast } from '../components/Toast'
import { useAuth } from '../context/useAuth'
import { AppShell } from '../layout/AppShell'
import type { Task, TaskFormValues, TaskPayload } from '../types/task'
import { emptyTaskFormValues, taskToFormValues } from '../types/task'
import type { TeamSummary } from '../types/team'

type AssignedTeamTask = Task & { teamId: number; teamName: string }
type ScopeFilter = 'ALL' | 'PERSONAL' | 'TEAM'

export function DashboardPage() {
  const { auth, logout } = useAuth()
  const [tasks, setTasks] = useState<Task[]>([])
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null)
  const [formMode, setFormMode] = useState<'create' | 'edit'>('create')
  const [loading, setLoading] = useState(true)
  const [loadingError, setLoadingError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize] = useState(10)
  const [totalPages, setTotalPages] = useState(0)
  const [totalRecords, setTotalRecords] = useState(0)

  const [filters, setFilters] = useState<FilterOptions>({
    status: 'ALL',
    priority: 'ALL',
    keyword: ''
  })

  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null)

  // === 团队任务相关状态 ===
  const [teams, setTeams] = useState<TeamSummary[]>([])
  const [assignedTeamTasks, setAssignedTeamTasks] = useState<AssignedTeamTask[]>([])
  const [teamTasksLoading, setTeamTasksLoading] = useState(false)
  const [teamTasksError, setTeamTasksError] = useState('')
  const [scopeFilter, setScopeFilter] = useState<ScopeFilter>('ALL')

  const loadTasks = useCallback(async () => {
    try {
      setLoading(true)
      setLoadingError('')

      const params: TaskQueryParams = {
        page: currentPage,
        size: pageSize,
        sortBy: 'updatedAt'
      }

      if (filters.status !== 'ALL') {
        params.status = filters.status
      }
      if (filters.priority !== 'ALL') {
        params.priority = filters.priority
      }

      if (filters.keyword.trim()) {
        params.keyword = filters.keyword.trim()
      }
      const pageData = await fetchTasks(params)

      startTransition(() => {
        setTotalPages(pageData.totalPages)
        setTotalRecords(pageData.totalRecords)

        setTasks(pageData.records)

        if (!pageData.records.length) {
        setSelectedTaskId(null)
        setFormMode('create')
      } else {
        const stillExists = pageData.records.some(t => t.id === selectedTaskId)
        if (stillExists && selectedTaskId) {
        } else {
          setSelectedTaskId(pageData.records[0].id)
          setFormMode('edit')
        }
      }
      })
    } catch (error) {
      setLoadingError(getErrorMessage(error))
      setToast({ message: getErrorMessage(error), type: 'error' })
    } finally {
      setLoading(false)
    }
  }, [currentPage, filters.status, filters.priority, filters.keyword, pageSize, selectedTaskId, formMode])

  const loadAssignedTeamTasks = useCallback(async () => {
    if (!auth?.username) return
    try {
      setTeamTasksLoading(true)
      setTeamTasksError('')

      const myTeams = await fetchMyTeams()
      setTeams(myTeams)

      if (myTeams.length === 0) {
        setAssignedTeamTasks([])
        return
      }

      // 并发拉取每个团队的任务，再按当前用户名过滤
      const perTeamPages = await Promise.all(
        myTeams.map((team) =>
          fetchTeamTasks(team.id, { page: 1, size: 100, sortBy: 'updatedAt' })
            .then((page) => ({ team, page }))
            .catch(() => ({ team, page: null as null })),
        ),
      )

      const merged: AssignedTeamTask[] = []
      for (const { team, page } of perTeamPages) {
        if (!page) continue
        for (const task of page.records) {
          // 后端 Task 若含 assigneeUsername 则严格按它过滤；
          // 字段缺失时降级为展示该团队全部任务（保证不空白）
          const assignee =
            (task as unknown as { assigneeUsername?: string | null }).assigneeUsername
          if (assignee === undefined || assignee === null || assignee === auth.username) {
            merged.push({ ...task, teamId: team.id, teamName: team.name })
          }
        }
      }
      setAssignedTeamTasks(merged)
    } catch (error) {
      setTeamTasksError(getErrorMessage(error))
    } finally {
      setTeamTasksLoading(false)
    }
  }, [auth?.username])

  useEffect(() => {
    loadTasks()
  }, [currentPage, filters.status, filters.priority, loadTasks])

  useEffect(() => {
    loadAssignedTeamTasks()
  }, [loadAssignedTeamTasks])

  const handleRefresh = () => {
    loadTasks()
    loadAssignedTeamTasks()
  }

  const handleFilterChange = (newFilters: FilterOptions) => {
    setFilters(newFilters)
    setCurrentPage(1)
  }

  const handleOpenCreate = () => {
    setSelectedTaskId(null)
    setSubmitError('')
    setFormMode('create')
  }

  const handleSelectTask = (task: Task) => {
    setSelectedTaskId(task.id)
    setSubmitError('')
    setFormMode('edit')
  }

  const handleSubmit = async (values: TaskFormValues) => {
    const payload: TaskPayload = {
      title: values.title.trim(),
      description: values.description.trim(),
      status: values.status,
      priority: values.priority,
      dueAt: values.dueAt || null,
    }

    try {
      setIsSubmitting(true)
      setSubmitError('')
      if (formMode === 'create') {
        await createTask(payload)
        setToast({ message: '任务创建成功！', type: 'success' })
        setCurrentPage(1)
        await loadTasks()
        return
      }

      if (!selectedTaskId) {
        setSubmitError('未选中任务，无法保存')
        return
      }

      await updateTask(selectedTaskId, payload)
      setToast({ message: '任务更新成功！', type: 'success' })
      await loadTasks()
    } catch (error) {
      setSubmitError(getErrorMessage(error))
      setToast({ message: getErrorMessage(error), type: 'error' })
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDelete = async () => {
    if (!selectedTaskId) return

    const confirmed = window.confirm('确认删除当前任务吗？该操作不可撤销。')
    if (!confirmed) return

    try {
      setIsSubmitting(true)
      setSubmitError('')
      await deleteTask(selectedTaskId)
      setToast({ message: '任务已删除', type: 'success' })
      await loadTasks()
    } catch (error) {
      setSubmitError(getErrorMessage(error))
      setToast({ message: getErrorMessage(error), type: 'error' })
    } finally {
      setIsSubmitting(false)
    }
  }

  const selectedTask = tasks.find((task) => task.id === selectedTaskId) ?? null

  const summary = {
    total: totalRecords,
    pending: tasks.filter((task) => task.status !== 'DONE').length,
    done: tasks.filter((task) => task.status === 'DONE').length,
  }

  const visibleTeamTasks = useMemo(() => {
    if (scopeFilter === 'PERSONAL') return []
    return assignedTeamTasks
  }, [assignedTeamTasks, scopeFilter])

  const showPersonalSection = scopeFilter !== 'TEAM'
  const showTeamSection = scopeFilter !== 'PERSONAL'

  return (
    <AppShell
      title="Software Engineering Lab"
      description=""
      aside={
        <>
          <div className="aside-card">
            <h2>快捷导航</h2>
            <ul className="nav-list">
              <li className="nav-item">
                <a href="/teams" className="nav-link">我的团队</a>
              </li>
            </ul>
          </div>

          <div className="aside-card">
            <h2></h2>
            <ul className="checkpoint-list">
              <li className="checkpoint-item">
                <span className="checkpoint-title"></span>
                <span className="checkpoint-copy">
                  
                </span>
              </li>
              <li className="checkpoint-item">
                <span className="checkpoint-title"></span>
                <span className="checkpoint-copy">

                </span>
              </li>
              <li className="checkpoint-item">
                <span className="checkpoint-title"></span>
                <span className="checkpoint-copy">
                  
                </span>
              </li>
            </ul>
          </div>

          <div className="aside-card">
            <h2></h2>
            <ul className="roadmap-list">
              <li className="roadmap-item">
                <span className="roadmap-title"></span>
                <span className="roadmap-copy">
                  
                </span>
              </li>
              <li className="roadmap-item">
                <span className="roadmap-title"></span>
                <span className="roadmap-copy">
                  
                </span>
              </li>
              <li className="roadmap-item">
                <span className="roadmap-title"></span>
                <span className="roadmap-copy">
                
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
          <h1>{auth?.username} 的个人任务台</h1>
          <p>
            
          </p>
        </div>
        <div className="toolbar">
          <button className="button-primary" type="button" onClick={handleOpenCreate}>
            新建任务
          </button>
          <button
            className="button-ghost"
            type="button"
            onClick={handleRefresh}
          >
            刷新列表
          </button>
          <button className="button-secondary" type="button" onClick={logout}>
            退出登录
          </button>
        </div>
      </header>

      <section className="summary-strip">
        <article className="summary-card">
          <h3>全部任务</h3>
          <div className="summary-number">{summary.total}</div>
          <p className="summary-note"></p>
        </article>
        <article className="summary-card">
          <h3>待处理</h3>
          <div className="summary-number">{summary.pending}</div>
          <p className="summary-note"></p>
        </article>
        <article className="summary-card">
          <h3>已完成</h3>
          <div className="summary-number">{summary.done}</div>
          <p className="summary-note"></p>
        </article>
        <article className="summary-card">
          <h3>分配给我的团队任务</h3>
          <div className="summary-number">{assignedTeamTasks.length}</div>
          <p className="summary-note">来自 {teams.length} 个团队</p>
        </article>
      </section>

      <section className="filters-section">
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap', marginBottom: '0.75rem' }}>
          <span className="eyebrow">范围</span>
          {(['ALL', 'PERSONAL', 'TEAM'] as ScopeFilter[]).map((value) => (
            <button
              key={value}
              type="button"
              className={scopeFilter === value ? 'button-primary' : 'button-ghost'}
              onClick={() => setScopeFilter(value)}
            >
              {value === 'ALL' ? '全部' : value === 'PERSONAL' ? '个人任务' : '团队任务'}
            </button>
          ))}
        </div>
        <TaskFilters
          onFilterChange={handleFilterChange}
          totalCount={totalRecords}
          filteredCount={tasks.length}
        />
      </section>

      {showPersonalSection && (
      <section className="content-grid">
        <article className="panel">
          <header className="panel-header">
            <p className="eyebrow">Task list</p>
            <h2 className="panel-title">我的任务</h2>
            <p className="panel-subtitle">
              
            </p>
          </header>

          {loading ? (
            <div className="message message--note">正在加载任务列表...</div>
          ) : loadingError ? (
            <div className="message message--error">{loadingError}</div>
          ) : (
            <>
              <TaskList
                selectedTaskId={selectedTaskId}
                tasks={tasks}
                onCreate={handleOpenCreate}
                onSelect={handleSelectTask}
              />
              
              {/* 分页组件 */}
              {totalPages > 1 && (
                <div style={{ 
                  marginTop: '1rem', 
                  display: 'flex', 
                  gap: '0.5rem', 
                  justifyContent: 'center',
                  alignItems: 'center'
                }}>
                  <button 
                    className="button-ghost"
                    disabled={currentPage === 1}
                    onClick={() => setCurrentPage(p => p - 1)}
                  >
                    上一页
                  </button>
                  <span style={{ padding: '0 1rem' }}>
                    第 {currentPage} / {totalPages} 页
                  </span>
                  <button 
                    className="button-ghost"
                    disabled={currentPage >= totalPages}
                    onClick={() => setCurrentPage(p => p + 1)}
                  >
                    下一页
                  </button>
                </div>
              )}
            </>
          )}
        </article>

        <article className="panel panel-stack">
          <header className="panel-header">
            <p className="eyebrow">
              {formMode === 'create' ? 'Create mode' : 'Task detail'}
            </p>
            <h2 className="panel-title">
              {formMode === 'create' ? '新建任务' : selectedTask?.title ?? '任务详情'}
            </h2>
            <p className="panel-subtitle">
              {formMode === 'create'
                ? ''
                : ''}
            </p>
          </header>

          <TaskForm
            key={formMode === 'create' ? 'create' : selectedTaskId}
            mode={formMode}
            initialValues={
              formMode === 'create'
                ? { ...emptyTaskFormValues }
                : taskToFormValues(selectedTask)
            }
            error={submitError}
            isSubmitting={isSubmitting}
            onCancelCreate={() => setSubmitError('')}
            onDelete={formMode === 'edit' ? handleDelete : undefined}
            onSubmit={handleSubmit}
          />

          <section className="panel-note">
            <h3></h3>
            <ul className="detail-list">
              <li className="detail-item">
                <span className="detail-title"></span>
                <span className="detail-copy">
                  
                </span>
              </li>
              <li className="detail-item">
                <span className="detail-title"></span>
                <span className="detail-copy">
                  
                </span>
              </li>
            </ul>
          </section>
        </article>
      </section>
      )}

      {showTeamSection && (
      <section className="content-grid" style={{ marginTop: '1.5rem' }}>
        <article className="panel">
          <header className="panel-header">
            <p className="eyebrow">Team workload</p>
            <h2 className="panel-title">分配给我的团队任务</h2>
            <p className="panel-subtitle">
              来自所有团队、当前指派给你的任务
            </p>
          </header>

          {teamTasksLoading ? (
            <div className="message message--note">正在加载团队任务...</div>
          ) : teamTasksError ? (
            <div className="message message--error">{teamTasksError}</div>
          ) : visibleTeamTasks.length === 0 ? (
            <div className="message message--note">暂无分配给你的团队任务。</div>
          ) : (
            <ul className="detail-list">
              {visibleTeamTasks.map((task) => (
                <li key={`${task.teamId}-${task.id}`} className="detail-item">
                  <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center', gap: '1rem' }}>
                    <div style={{ minWidth: 0 }}>
                      <span className="detail-title">{task.title}</span>
                      <span className="detail-copy">
                        来自团队「{task.teamName}」 · 状态 {task.status} · 优先级 {task.priority}
                      </span>
                    </div>
                    {task.dueAt && (
                      <span className="detail-copy" style={{ fontSize: '0.75rem', whiteSpace: 'nowrap' }}>
                        截止 {new Date(task.dueAt).toLocaleString()}
                      </span>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </article>
      </section>
      )}

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
