import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  addTeamMember,
  dissolveTeam,
  fetchTeamDetail,
  leaveTeam,
  removeTeamMember,
  transferTeamOwnership,
  updateTeamMemberRole,
} from '../api/teams'
import {
  addTeamTaskDependency,
  createTeamTask,
  deleteTeamTask,
  fetchAvailableTeamDependencies,
  fetchTeamTaskDependencies,
  fetchTeamTasks,
  removeTeamTaskDependency,
  updateTeamTask,
  updateTeamTaskStatus,
  type TaskQueryParams,
} from '../api/tasks'
import { getErrorMessage } from '../api/http'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Dialog } from '../components/Dialog'
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
  const [availableDeps, setAvailableDeps] = useState<Task[]>([])
  const [showDepModal, setShowDepModal] = useState(false)
  const [showTransferDialog, setShowTransferDialog] = useState(false)
  const [pendingNewOwnerId, setPendingNewOwnerId] = useState<string>('')

  type ConfirmConfig = {
    title: string
    message: string
    tone?: 'default' | 'danger'
    confirmLabel?: string
    onConfirm: () => Promise<void> | void
  }
  const [confirm, setConfirm] = useState<ConfirmConfig | null>(null)

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
      const message = getErrorMessage(error)
      setLoadingError(message)
      setToast({ message, type: 'error' })
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
      setDependencies(data.predecessors ?? [])
      setDependents(data.successors ?? [])
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

  const handleAddDependency = async (predecessorTaskId: number) => {
    if (!team || !selectedTaskId) return
    try {
      await addTeamTaskDependency(team.id, selectedTaskId, predecessorTaskId)
      setToast({ message: '前置任务已添加', type: 'success' })
      setShowDepModal(false)
      await loadDependencies(selectedTaskId)
      await loadWorkspace()
    } catch (error) {
      const message = getErrorMessage(error)
      setDependencyError(message)
      setToast({ message, type: 'error' })
    }
  }

  const handleOpenDepModal = async () => {
    if (!team || !selectedTaskId) return
    try {
      const data = await fetchAvailableTeamDependencies(team.id, selectedTaskId)
      setAvailableDeps(data)
      setDependencyError('')
      setShowDepModal(true)
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

  const handleDelete = () => {
    const selectedTask = tasks.find((task) => task.id === selectedTaskId)
    if (!selectedTask || !selectedTask.canDelete || !team) {
      setSubmitError('当前角色无权删除团队任务')
      return
    }

    setConfirm({
      title: '删除团队任务',
      message: '确认删除当前团队任务吗？该操作不可撤销。',
      tone: 'danger',
      confirmLabel: '删除',
      onConfirm: async () => {
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
      },
    })
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

  const handleRemoveMember = (userId: number) => {
    if (!team) return
    setConfirm({
      title: '移除团队成员',
      message: '确认移除该成员吗？其未完成的团队任务会被清空负责人但保留原状态，已完成任务保留历史记录。',
      tone: 'danger',
      confirmLabel: '移除',
      onConfirm: async () => {
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
      },
    })
  }

  // Member / Admin 离开团队。Owner 不能调用此方法（UI 上隐藏按钮），必须先转让所有权或解散团队。
  const handleLeaveTeam = () => {
    if (!team || !auth) return
    setConfirm({
      title: '离开团队',
      message: '确认离开团队吗？离开后你将失去访问该团队的权限，未完成的任务会清空负责人但保留原状态。',
      tone: 'danger',
      confirmLabel: '离开',
      onConfirm: async () => {
        try {
          await leaveTeam(team.id, auth.userId)
          setToast({ message: '已离开团队', type: 'success' })
          navigate('/teams')
        } catch (error) {
          const message = getErrorMessage(error)
          setMemberError(message)
          setToast({ message, type: 'error' })
        }
      },
    })
  }

  const handleOpenTransferDialog = () => {
    if (!team) return
    const firstCandidate = team.members.find((m) => m.role !== 'OWNER')
    setPendingNewOwnerId(firstCandidate ? String(firstCandidate.userId) : '')
    setShowTransferDialog(true)
  }

  const handleConfirmTransfer = async () => {
    if (!team) return
    const newOwnerId = Number(pendingNewOwnerId)
    if (!Number.isInteger(newOwnerId) || newOwnerId <= 0) {
      setMemberError('请选择有效的新 Owner')
      return
    }
    try {
      setIsUpdatingMembers(true)
      await transferTeamOwnership(team.id, newOwnerId)
      setShowTransferDialog(false)
      setToast({ message: '团队所有权已转让，你已变为 Member', type: 'success' })
      await loadWorkspace()
    } catch (error) {
      const message = getErrorMessage(error)
      setMemberError(message)
      setToast({ message, type: 'error' })
    } finally {
      setIsUpdatingMembers(false)
    }
  }

  const handleDissolveTeam = () => {
    if (!team) return
    setConfirm({
      title: '解散团队',
      message: '确认解散团队吗？解散后成员不能继续访问团队空间，团队任务和依赖关系将被归档。',
      tone: 'danger',
      confirmLabel: '解散',
      onConfirm: async () => {
        try {
          await dissolveTeam(team.id)
          setToast({ message: '团队已解散', type: 'success' })
          navigate('/teams')
        } catch (error) {
          const message = getErrorMessage(error)
          setMemberError(message)
          setToast({ message, type: 'error' })
        }
      },
    })
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
          {canManageMembers ? (
            <>
              <button className="button-ghost" onClick={handleOpenTransferDialog}>转让所有权</button>
              <button className="button-danger" onClick={handleDissolveTeam}>解散团队</button>
            </>
          ) : (
            <button className="button-ghost" onClick={handleLeaveTeam}>离开团队</button>
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
                <button className="button-primary" type="button" onClick={handleOpenDepModal}>
                  + 添加前置任务
                </button>
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
                        <li key={dep.id} className="dependency-item">
                          <span className={`status-badge status-badge--${dep.status.toLowerCase()}`}>
                            {STATUS_LABELS[dep.status]}
                          </span>
                          <span>#{dep.id} {dep.title}</span>
                          {canManageTeamTasks && (
                            <button className="button-ghost" onClick={() => handleRemoveDependency(dep.id)}>
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
                        <li key={dep.id} className="dependency-item">
                          <span className={`status-badge status-badge--${dep.status.toLowerCase()}`}>
                            {STATUS_LABELS[dep.status]}
                          </span>
                          <span>#{dep.id} {dep.title}</span>
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

      <Dialog
        open={showTransferDialog}
        onClose={() => setShowTransferDialog(false)}
        title="转让团队所有权"
        description="选择一个团队成员接任 Owner。转让后你将变为 Member，仍可在团队中继续工作；如需离开团队，请在转让后从成员入口主动离开。"
        footer={(
          <>
            <button type="button" className="button-ghost" onClick={() => setShowTransferDialog(false)}>
              取消
            </button>
            <button
              type="button"
              className="button-primary"
              onClick={handleConfirmTransfer}
              disabled={!pendingNewOwnerId || isUpdatingMembers}
            >
              确认转让
            </button>
          </>
        )}
      >
        {team && team.members.filter((m) => m.role !== 'OWNER').length === 0 ? (
          <p className="empty-text">团队当前没有其他成员，无法转让所有权。请使用"解散团队"。</p>
        ) : (
          <label className="form-field">
            <span className="form-label">选择新 Owner</span>
            <select
              value={pendingNewOwnerId}
              onChange={(e) => setPendingNewOwnerId(e.target.value)}
            >
              {team?.members
                .filter((m) => m.role !== 'OWNER')
                .map((m) => (
                  <option key={m.userId} value={String(m.userId)}>
                    {m.username}（{TEAM_ROLE_LABELS[m.role]}）
                  </option>
                ))}
            </select>
          </label>
        )}
      </Dialog>

      <Dialog
        open={showDepModal}
        onClose={() => setShowDepModal(false)}
        title="选择前置任务"
        description="选择同团队内的一个任务作为当前任务的前置依赖。"
      >
        {availableDeps.length === 0 ? (
          <p className="empty-text">没有可用的任务可以添加为前置任务</p>
        ) : (
          <ul className="available-dep-list">
            {availableDeps.map((task) => (
              <li key={task.id} className="available-dep-item">
                <div className="dep-info">
                  <span className={`status-badge status-badge--${task.status.toLowerCase()}`}>
                    {STATUS_LABELS[task.status]}
                  </span>
                  <span className="dep-title">#{task.id} {task.title}</span>
                </div>
                <button
                  className="button-primary button-sm"
                  type="button"
                  onClick={() => handleAddDependency(task.id)}
                >
                  选择
                </button>
              </li>
            ))}
          </ul>
        )}
      </Dialog>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
      {confirm && (
        <ConfirmDialog
          open
          title={confirm.title}
          message={confirm.message}
          tone={confirm.tone}
          confirmLabel={confirm.confirmLabel}
          onCancel={() => setConfirm(null)}
          onConfirm={() => {
            const action = confirm.onConfirm
            setConfirm(null)
            void action()
          }}
        />
      )}
    </AppShell>
  )
}
