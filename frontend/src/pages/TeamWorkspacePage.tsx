import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  addTeamMember,
  dissolveTeam,
  fetchTeamDetail,
  leaveTeam,
  ownerLeaveTeam,
  removeTeamMember,
  updateTeamMemberRole,
} from '../api/teams'
import {
  addTeamTaskDependency,
  createTeamTask,
  deleteTeamTask,
  fetchTeamTaskDependencies,
  fetchTeamTasks,
  removeTeamTaskDependency,
  updateTeamTask,
  updateTeamTaskStatus,
  type TaskQueryParams,
} from '../api/tasks'
import { getErrorMessage } from '../api/http'
import { TaskFilters, type FilterOptions } from '../components/TaskFilters'
import { TaskForm } from '../components/TaskForm'
import { TaskList } from '../components/TaskList'
import { Toast } from '../components/Toast'
import { useAuth } from '../context/useAuth'
import { AppShell } from '../layout/AppShell'
import type { Task, TaskDependencyItem, TaskFormValues, TeamTaskPayload } from '../types/task'
import { taskToFormValues, STATUS_LABELS } from '../types/task'
import { TEAM_ROLE_LABELS, type TeamDetail, type TeamRole } from '../types/team'

export function TeamWorkspacePage() {
  const navigate = useNavigate()
  const { teamId } = useParams()
  const { auth, logout } = useAuth()
  const parsedTeamId = Number(teamId)
  const [team, setTeam] = useState<TeamDetail | null>(null)
  const [tasks, setTasks] = useState<Task[]>([])
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null)
  const [formMode, setFormMode] = useState<'create' | 'edit'>('edit')
  const [loading, setLoading] = useState(true)
  const [loadingError, setLoadingError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const [memberError, setMemberError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isUpdatingMembers, setIsUpdatingMembers] = useState(false)
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize] = useState(10)
  const [totalPages, setTotalPages] = useState(0)
  const [totalRecords, setTotalRecords] = useState(0)
  const [memberUsername, setMemberUsername] = useState('')
  const [filters, setFilters] = useState<FilterOptions>({
    status: 'ALL',
    priority: 'ALL',
    keyword: '',
  })
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null)

  const [dependencies, setDependencies] = useState<TaskDependencyItem[]>([])
  const [dependents, setDependents] = useState<TaskDependencyItem[]>([])
  const [depLoading, setDepLoading] = useState(false)
  const [dependencyError, setDependencyError] = useState('')
  const [dependencyInput, setDependencyInput] = useState('')

  const loadWorkspace = useCallback(async () => {
    if (!Number.isFinite(parsedTeamId) || parsedTeamId <= 0) {
      setLoadingError('团队编号不合法')
      setLoading(false)
      return
    }

    try {
      setLoading(true)
      setLoadingError('')
      const params: TaskQueryParams = {
        page: currentPage,
        size: pageSize,
        sortBy: 'updatedAt',
      }

      if (filters.status !== 'ALL') params.status = filters.status
      if (filters.priority !== 'ALL') params.priority = filters.priority
      if (filters.keyword.trim()) params.keyword = filters.keyword.trim()

      const [detail, taskPage] = await Promise.all([
        fetchTeamDetail(parsedTeamId),
        fetchTeamTasks(parsedTeamId, params),
      ])

      setTeam(detail)
      setTasks(taskPage.records)
      setTotalPages(taskPage.totalPages)
      setTotalRecords(taskPage.totalRecords)

      if (!taskPage.records.length) {
        setSelectedTaskId(null)
        setFormMode(detail.currentUserRole === 'MEMBER' ? 'edit' : 'create')
        return
      }

      if (formMode === 'create') return

      const stillExists = taskPage.records.some((task) => task.id === selectedTaskId)
      if (!stillExists || selectedTaskId === null) {
        setSelectedTaskId(taskPage.records[0].id)
      }
    } catch (error) {
      setLoadingError(getErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }, [currentPage, filters, pageSize, parsedTeamId, selectedTaskId, formMode])

  useEffect(() => {
    loadWorkspace()
  }, [loadWorkspace])

  const loadDependencies = useCallback(async (taskId: number) => {
    if (!team) return
    setDepLoading(true)
    try {
      const data = await fetchTeamTaskDependencies(team.id, taskId)
      setDependencies(data.dependencies ?? [])
      setDependents(data.dependents ?? [])
      setDependencyError('')
    } catch (error) {
      setDependencies([])
      setDependents([])
      setDependencyError(getErrorMessage(error))
    } finally {
      setDepLoading(false)
    }
  }, [team])

  useEffect(() => {
    const selected = tasks.find((t) => t.id === selectedTaskId)
    if (selectedTaskId && selected && formMode === 'edit') {
      loadDependencies(selectedTaskId)
    } else {
      setDependencies([])
      setDependents([])
    }
  }, [selectedTaskId, tasks, formMode, loadDependencies])

  const handleAddDependency = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!team || !selectedTaskId) return
    const predecessorTaskId = Number(dependencyInput)
    if (!Number.isFinite(predecessorTaskId) || predecessorTaskId <= 0) {
      setDependencyError('请输入有效的前置任务 ID')
      return
    }

    try {
      await addTeamTaskDependency(team.id, selectedTaskId, predecessorTaskId)
      setToast({ message: '前置任务已添加', type: 'success' })
      setDependencyInput('')
      await loadDependencies(selectedTaskId)
      await loadWorkspace()
    } catch (error) {
      const message = getErrorMessage(error)
      setDependencyError(message)
      setToast({ message, type: 'error' })
    }
  }

  const handleRemoveDependency = async (predecessorTaskId: number) => {
    if (!team || !selectedTaskId) return
    try {
      await removeTeamTaskDependency(team.id, selectedTaskId, predecessorTaskId)
      setToast({ message: '依赖已移除', type: 'success' })
      await loadDependencies(selectedTaskId)
      await loadWorkspace()
    } catch (error) {
      const message = getErrorMessage(error)
      setDependencyError(message)
      setToast({ message, type: 'error' })
    }
  }

  const unfinishedPredecessors = useMemo(
    () => dependencies.filter((d) => d.status !== 'DONE'),
    [dependencies],
  )
  const isBlocked = unfinishedPredecessors.length > 0

  const handleFilterChange = (newFilters: FilterOptions) => {
    setFilters(newFilters)
    setCurrentPage(1)
  }

  const handleRefresh = () => loadWorkspace()

  const handleOpenCreate = () => {
    setSelectedTaskId(null)
    setFormMode('create')
    setSubmitError('')
  }

  const handleSelectTask = (task: Task) => {
    setSelectedTaskId(task.id)
    setFormMode('edit')
    setSubmitError('')
  }

  const handleSubmit = async (values: TaskFormValues) => {
    if (!team) return

    const payload: TeamTaskPayload = {
      title: values.title.trim(),
      description: values.description.trim(),
      status: values.status,
      priority: values.priority,
      dueAt: values.dueAt || null,
      assigneeId: Number(values.assigneeId),
    }

    try {
      setIsSubmitting(true)
      setSubmitError('')

      if (formMode === 'create') {
        await createTeamTask(team.id, payload)
        setToast({ message: '团队任务创建成功', type: 'success' })
        await loadWorkspace()
        return
      }

      const selectedTask = tasks.find((task) => task.id === selectedTaskId)
      if (!selectedTask) {
        setSubmitError('未选中任务，无法保存')
        return
      }

      if (isBlocked && values.status === 'DONE') {
        setSubmitError(`存在 ${unfinishedPredecessors.length} 个未完成前置任务，无法标记为已完成。`)
        return
      }

      if (selectedTask.canEditDetails) {
        await updateTeamTask(team.id, selectedTask.id, payload)
        setToast({ message: '团队任务更新成功', type: 'success' })
      } else if (selectedTask.canEditStatus) {
        await updateTeamTaskStatus(team.id, selectedTask.id, values.status)
        setToast({ message: '团队任务状态已更新', type: 'success' })
      } else {
        setSubmitError('当前角色无权修改该团队任务')
        return
      }

      await loadWorkspace()
    } catch (error) {
      const message = getErrorMessage(error)
      setSubmitError(message)
      setToast({ message, type: 'error' })
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDelete = async () => {
    const selectedTask = tasks.find((task) => task.id === selectedTaskId)
    if (!selectedTask || !selectedTask.canDelete || !team) {
      setSubmitError('当前角色无权删除团队任务')
      return
    }

    if (!window.confirm('确认删除当前团队任务吗？该操作不可撤销。')) return

    try {
      setIsSubmitting(true)
      setSubmitError('')
      await deleteTeamTask(team.id, selectedTask.id)
      setToast({ message: '团队任务已删除', type: 'success' })
      await loadWorkspace()
    } catch (error) {
      const message = getErrorMessage(error)
      setSubmitError(message)
      setToast({ message, type: 'error' })
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleAddMember = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!team) return
    if (!memberUsername.trim()) {
      setMemberError('请输入要添加的用户名')
      return
    }

    try {
      setIsUpdatingMembers(true)
      setMemberError('')
      await addTeamMember(team.id, memberUsername.trim())
      setMemberUsername('')
      setToast({ message: '团队成员添加成功', type: 'success' })
      await loadWorkspace()
    } catch (error) {
      const message = getErrorMessage(error)
      setMemberError(message)
      setToast({ message, type: 'error' })
    } finally {
      setIsUpdatingMembers(false)
    }
  }

  const handleRoleChange = async (userId: number, role: TeamRole) => {
    if (!team) return
    try {
      setIsUpdatingMembers(true)
      setMemberError('')
      await updateTeamMemberRole(team.id, userId, role)
      setToast({ message: '团队角色更新成功', type: 'success' })
      await loadWorkspace()
    } catch (error) {
      const message = getErrorMessage(error)
      setMemberError(message)
      setToast({ message, type: 'error' })
    } finally {
      setIsUpdatingMembers(false)
    }
  }

  const handleRemoveMember = async (userId: number) => {
    if (!team) return
    if (!window.confirm('确认移除该成员吗？其负责的团队任务会自动转交给 Owner。')) return

    try {
      setIsUpdatingMembers(true)
      await removeTeamMember(team.id, userId)
      setToast({ message: '团队成员已移除', type: 'success' })
      await loadWorkspace()
    } catch (error) {
      const message = getErrorMessage(error)
      setMemberError(message)
      setToast({ message, type: 'error' })
    } finally {
      setIsUpdatingMembers(false)
    }
  }

  const handleLeaveTeam = async () => {
    if (!team || !auth) return

    const isOwner = team.currentUserRole === 'OWNER'

    if (isOwner) {
      const candidates = team.members.filter(m => m.role !== 'OWNER')
      if (candidates.length === 0) {
        if (window.confirm('你是团队唯一的成员，离开团队将自动解散团队。确认吗？')) {
          await dissolveTeam(team.id)
          setToast({ message: '团队已解散', type: 'success' })
          navigate('/teams')
        }
        return
      }
      const newOwnerUsername = prompt('请选择新 Owner 的用户名', candidates[0]?.username)
      if (!newOwnerUsername) return
      const newOwner = team.members.find(m => m.username === newOwnerUsername)
      if (!newOwner) {
        setMemberError('未找到该用户')
        return
      }
      await ownerLeaveTeam(team.id, newOwner.userId)
      setToast({ message: '已离开团队，新 Owner 已指定', type: 'success' })
      navigate('/teams')
    } else {
      if (!window.confirm('确认离开团队吗？你负责的任务将自动转交给 Owner。')) return
      await leaveTeam(team.id, auth.userId)
      setToast({ message: '已离开团队', type: 'success' })
      navigate('/teams')
    }
  }

  const handleDissolveTeam = async () => {
    if (!team) return
    if (!window.confirm('确认解散团队吗？解散后成员不能继续访问团队空间。')) return

    try {
      await dissolveTeam(team.id)
      setToast({ message: '团队已解散', type: 'success' })
      navigate('/teams')
    } catch (error) {
      const message = getErrorMessage(error)
      setMemberError(message)
      setToast({ message, type: 'error' })
    }
  }

  const selectedTask = tasks.find((task) => task.id === selectedTaskId) ?? null
  const canManageTeamTasks = team?.currentUserRole === 'OWNER' || team?.currentUserRole === 'ADMIN'
  const canManageMembers = team?.currentUserRole === 'OWNER'

  const assigneeOptions = useMemo(
    () => (team?.members ?? []).map((member) => ({
      value: String(member.userId),
      label: `${member.username} (${TEAM_ROLE_LABELS[member.role]})`,
    })),
    [team],
  )

  const selectedTaskHint = useMemo(() => {
    if (!selectedTask) return ''
    if (isBlocked) {
      return `⚠️ 该任务还有 ${unfinishedPredecessors.length} 个未完成前置任务，不能标记为 DONE。`
    }
    if (selectedTask.canEditDetails) return ''
    if (selectedTask.canEditStatus) {
      return '当前角色仅能修改这条任务的状态，不能修改标题、描述、优先级或负责人。'
    }
    return '当前角色只能查看这条任务，无法直接修改。'
  }, [selectedTask, isBlocked, unfinishedPredecessors.length])

  return (
    <AppShell
      title="Run each team as an isolated workspace with explicit member roles."
      description="团队空间负责展示团队成员、角色和团队任务。所有团队数据都在后端按成员关系与角色做强制校验。"
      aside={(
        <>
          <div className="aside-card">
            <h2>权限矩阵</h2>
            <p>Member 浏览全部团队任务，只能修改自己被分配任务的状态；Admin 能管理团队任务；Owner 还能管理成员与角色。</p>
          </div>
          <div className="aside-card">
            <h2>Lab3 新增</h2>
            <p>成员可主动离开团队；Owner 可移除成员、解散团队；Admin/Owner 可管理任务依赖（前置任务）。</p>
          </div>
        </>
      )}
    >
      <header className="main-header">
        <div>
          <p className="eyebrow">Team Workspace / Lab3</p>
          <h1>{team ? team.name : '团队空间'}</h1>
          <p>
            当前登录角色：{team ? TEAM_ROLE_LABELS[team.currentUserRole] : '加载中'}
          </p>
        </div>
        <div className="toolbar">
          <button className="button-ghost" onClick={() => navigate('/teams')}>返回我的团队</button>
          <button className="button-ghost" onClick={() => navigate('/tasks')}>返回工作台</button>
          <button className="button-ghost" onClick={handleLeaveTeam}>离开团队</button>
          {canManageMembers && (
            <button className="button-danger" onClick={handleDissolveTeam}>解散团队</button>
          )}
          <button className="button-ghost" onClick={logout}>退出登录</button>
        </div>
      </header>

      {loadingError && <div className="message message--error">{loadingError}</div>}

      <section className="workspace-grid">
        {/* 左侧：成员列表 */}
        <div className="panel panel-stack">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Members</p>
              <h2>团队成员与角色</h2>
            </div>
          </div>

          {canManageMembers && (
            <form className="inline-form" onSubmit={handleAddMember}>
              <input
                value={memberUsername}
                maxLength={20}
                placeholder="输入要添加的用户名"
                onChange={(e) => setMemberUsername(e.target.value)}
              />
              <button className="button-primary" type="submit" disabled={isUpdatingMembers}>
                {isUpdatingMembers ? '处理中...' : '添加成员'}
              </button>
            </form>
          )}

          {memberError && <div className="message message--error">{memberError}</div>}

          <div className="member-list">
            {(team?.members ?? []).map((member) => (
              <article key={member.userId} className="member-card">
                <div>
                  <h3>{member.username}</h3>
                  <p>{TEAM_ROLE_LABELS[member.role]}</p>
                </div>
                <div className="member-card__actions">
                  <span className={`role-badge role-badge--${member.role.toLowerCase()}`}>
                    {TEAM_ROLE_LABELS[member.role]}
                  </span>
                  {canManageMembers && member.role !== 'OWNER' && (
                    <>
                      {member.role === 'MEMBER' ? (
                        <button className="button-ghost" onClick={() => handleRoleChange(member.userId, 'ADMIN')}>
                          设为 Admin
                        </button>
                      ) : (
                        <button className="button-ghost" onClick={() => handleRoleChange(member.userId, 'MEMBER')}>
                          降为 Member
                        </button>
                      )}
                      <button className="button-danger" onClick={() => handleRemoveMember(member.userId)}>
                        移除
                      </button>
                    </>
                  )}
                </div>
              </article>
            ))}
          </div>
        </div>

        {/* 右侧：任务列表 */}
        <div className="panel panel-stack">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Team Tasks</p>
              <h2>团队任务列表</h2>
            </div>
            <div className="toolbar">
              {canManageTeamTasks && (
                <button className="button-primary" onClick={handleOpenCreate}>新建团队任务</button>
              )}
              <button className="button-ghost" onClick={handleRefresh}>刷新任务</button>
            </div>
          </div>

          <TaskFilters onFilterChange={handleFilterChange} totalCount={totalRecords} filteredCount={tasks.length} />

          <div className="pagination">
            <button
              className="button-ghost"
              disabled={currentPage <= 1 || loading}
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
            >上一页</button>
            <span>第 {currentPage} / {Math.max(totalPages, 1)} 页</span>
            <button
              className="button-ghost"
              disabled={currentPage >= totalPages || totalPages === 0 || loading}
              onClick={() => setCurrentPage((p) => p + 1)}
            >下一页</button>
          </div>

          {loading ? (
            <div className="empty-state"><p>团队任务加载中...</p></div>
          ) : (
            <TaskList
              selectedTaskId={selectedTaskId}
              tasks={tasks}
              onCreate={canManageTeamTasks ? handleOpenCreate : () => undefined}
              onSelect={handleSelectTask}
            />
          )}
        </div>

        {/* 下方：任务详情 + 依赖管理 */}
        <div className="panel panel-stack workspace-grid__full">
          <div className="panel-header">
            <div>
              <p className="eyebrow">{formMode === 'create' ? 'Create Team Task' : 'Task Detail'}</p>
              <h2>{formMode === 'create' ? '新建团队任务' : '团队任务详情'}</h2>
            </div>
          </div>

          <TaskForm
            mode={formMode}
            initialValues={taskToFormValues(selectedTask)}
            error={submitError}
            isSubmitting={isSubmitting}
            onCancelCreate={() => {
              setFormMode('edit')
              setSelectedTaskId(tasks[0]?.id ?? null)
            }}
            onDelete={selectedTask?.canDelete ? handleDelete : undefined}
            onSubmit={handleSubmit}
            allowDetailEditing={formMode === 'create' ? canManageTeamTasks : Boolean(selectedTask?.canEditDetails)}
            allowStatusEditing={formMode === 'create' ? canManageTeamTasks : Boolean(selectedTask?.canEditStatus)}
            allowDelete={Boolean(selectedTask?.canDelete)}
            showAssignee
            assigneeOptions={assigneeOptions}
            readOnlyHint={selectedTaskHint}
            submitLabel={
              formMode === 'create'
                ? '创建团队任务'
                : selectedTask?.canEditDetails
                  ? '保存团队任务'
                  : '更新任务状态'
            }
          />

          {/* ── 任务依赖管理区域 (Lab3) ── */}
          {selectedTask && formMode === 'edit' && (
            <section className="dependency-panel" style={{ marginTop: '1.5rem', borderTop: '1px solid var(--line)', paddingTop: '1rem' }}>
              <div className="panel-header">
                <div>
                  <p className="eyebrow">Dependencies</p>
                  <h2>任务依赖</h2>
                </div>
              </div>

              {isBlocked && (
                <div className="message message--warning">
                  ⚠️ 该任务有 {unfinishedPredecessors.length} 个未完成前置任务，无法标记为 DONE。
                </div>
              )}

              {canManageTeamTasks ? (
                <form className="inline-form" onSubmit={handleAddDependency}>
                  <input
                    value={dependencyInput}
                    placeholder="输入同团队前置任务 ID"
                    onChange={(e) => setDependencyInput(e.target.value)}
                  />
                  <button className="button-primary" type="submit">新增依赖</button>
                </form>
              ) : (
                <div className="message message--info">
                  只有 Admin 或 Owner 可以新增、移除依赖。
                </div>
              )}

              {dependencyError && <div className="message message--error">{dependencyError}</div>}

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginTop: '1rem' }}>
                {/* 前置任务 */}
                <div>
                  <h3>前置任务（当前任务依赖）</h3>
                  {depLoading ? (
                    <p>加载中...</p>
                  ) : dependencies.length === 0 ? (
                    <p className="muted-text">暂无前置任务</p>
                  ) : (
                    <ul className="dependency-list">
                      {dependencies.map((dep) => (
                        <li key={dep.dependencyId} className="dependency-item">
                          <span className={`status-badge status-badge--${dep.status.toLowerCase()}`}>
                            {STATUS_LABELS[dep.status]}
                          </span>
                          <span>#{dep.taskId} {dep.title}</span>
                          {canManageTeamTasks && (
                            <button className="button-ghost" onClick={() => handleRemoveDependency(dep.taskId)}>
                              移除
                            </button>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>

                {/* 后继任务 */}
                <div>
                  <h3>后继任务（依赖当前任务）</h3>
                  {depLoading ? (
                    <p>加载中...</p>
                  ) : dependents.length === 0 ? (
                    <p className="muted-text">暂无后继任务</p>
                  ) : (
                    <ul className="dependency-list">
                      {dependents.map((dep) => (
                        <li key={dep.dependencyId} className="dependency-item">
                          <span className={`status-badge status-badge--${dep.status.toLowerCase()}`}>
                            {STATUS_LABELS[dep.status]}
                          </span>
                          <span>#{dep.taskId} {dep.title}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            </section>
          )}
        </div>
      </section>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </AppShell>
  )
}