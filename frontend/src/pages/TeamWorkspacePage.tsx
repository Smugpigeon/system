import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { addTeamMember, fetchTeamDetail, updateTeamMemberRole } from '../api/teams'
import {
  createTeamTask,
  deleteTeamTask,
  fetchTeamTasks,
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
import type { Task, TaskFormValues, TeamTaskPayload } from '../types/task'
import { taskToFormValues } from '../types/task'
import { TEAM_ROLE_LABELS, type TeamDetail, type TeamRole } from '../types/team'

export function TeamWorkspacePage() {
  const navigate = useNavigate()
  const { teamId } = useParams()
  const { logout } = useAuth()
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

      if (filters.status !== 'ALL') {
        params.status = filters.status
      }
      if (filters.priority !== 'ALL') {
        params.priority = filters.priority
      }

      const [detail, taskPage] = await Promise.all([
        fetchTeamDetail(parsedTeamId),
        fetchTeamTasks(parsedTeamId, params),
      ])

      let filteredRecords = taskPage.records
      if (filters.keyword.trim()) {
        const keyword = filters.keyword.toLowerCase()
        filteredRecords = taskPage.records.filter((task) =>
          task.title.toLowerCase().includes(keyword)
          || task.description.toLowerCase().includes(keyword)
          || task.assigneeUsername.toLowerCase().includes(keyword)
          || task.ownerUsername.toLowerCase().includes(keyword),
        )
      }

      setTeam(detail)
      setTasks(filteredRecords)
      setTotalPages(taskPage.totalPages)
      setTotalRecords(taskPage.totalRecords)

      if (!filteredRecords.length) {
        setSelectedTaskId(null)
        setFormMode(detail.currentUserRole === 'MEMBER' ? 'edit' : 'create')
        return
      }

      if (formMode === 'create') {
        return
      }

      const stillExists = filteredRecords.some((task) => task.id === selectedTaskId)
      if (!stillExists || selectedTaskId === null) {
        setSelectedTaskId(filteredRecords[0].id)
      }
    } catch (error) {
      setLoadingError(getErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }, [currentPage, filters.keyword, filters.priority, filters.status, pageSize, parsedTeamId, selectedTaskId, formMode])

  useEffect(() => {
    loadWorkspace()
  }, [loadWorkspace])

  const handleFilterChange = (newFilters: FilterOptions) => {
    setFilters(newFilters)
    setCurrentPage(1)
  }

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
    if (!team) {
      return
    }

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

    const confirmed = window.confirm('确认删除当前团队任务吗？该操作不可撤销。')
    if (!confirmed) {
      return
    }

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
    if (!team) {
      return
    }
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
    if (!team) {
      return
    }

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
    if (!selectedTask) {
      return ''
    }
    if (selectedTask.canEditDetails) {
      return ''
    }
    if (selectedTask.canEditStatus) {
      return '当前角色仅能修改这条任务的状态，不能修改标题、描述、优先级或负责人。'
    }
    return '当前角色只能查看这条任务，无法直接修改。'
  }, [selectedTask])

  return (
    <AppShell
      title="Run each team as an isolated workspace with explicit member roles."
      description="团队空间负责展示团队成员、角色和团队任务。所有团队数据都在后端按成员关系与角色做强制校验，不能仅靠前端按钮隐藏。"
      aside={(
        <>
          <div className="aside-card">
            <h2>权限矩阵</h2>
            <p>
              Member 浏览全部团队任务，只能修改自己被分配任务的状态；Admin 能管理团队任务；Owner 还能管理成员与角色。
            </p>
          </div>
          <div className="aside-card">
            <h2>后端强校验</h2>
            <p>
              即使绕过前端直接调接口，非团队成员也拿不到团队数据，Member 也不能修改不属于自己的任务内容。
            </p>
          </div>
        </>
      )}
    >
      <header className="main-header">
        <div>
          <p className="eyebrow">Team Workspace</p>
          <h1>{team ? team.name : '团队空间'}</h1>
          <p>
            这里展示团队成员和角色，并提供团队任务的浏览、创建、分配、更新与删除能力。当前登录角色：
            {team ? ` ${TEAM_ROLE_LABELS[team.currentUserRole]}` : ' 加载中'}
          </p>
        </div>
        <div className="toolbar">
          <button className="button-ghost" type="button" onClick={() => navigate('/teams')}>
            返回我的团队
          </button>
          <button className="button-ghost" type="button" onClick={() => navigate('/tasks')}>
            返回工作台
          </button>
          <button className="button-ghost" type="button" onClick={logout}>
            退出登录
          </button>
        </div>
      </header>

      {loadingError ? <div className="message message--error">{loadingError}</div> : null}

      <section className="workspace-grid">
        <div className="panel panel-stack">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Members</p>
              <h2>团队成员与角色</h2>
            </div>
          </div>

          {canManageMembers ? (
            <form className="inline-form" onSubmit={handleAddMember}>
              <input
                value={memberUsername}
                maxLength={20}
                placeholder="输入要添加的用户名"
                onChange={(event) => setMemberUsername(event.target.value)}
              />
              <button className="button-primary" type="submit" disabled={isUpdatingMembers}>
                {isUpdatingMembers ? '处理中...' : '添加成员'}
              </button>
            </form>
          ) : null}

          {memberError ? <div className="message message--error">{memberError}</div> : null}

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
                  {canManageMembers && member.role !== 'OWNER' ? (
                    member.role === 'MEMBER' ? (
                      <button
                        className="button-ghost"
                        type="button"
                        disabled={isUpdatingMembers}
                        onClick={() => handleRoleChange(member.userId, 'ADMIN')}
                      >
                        设为 Admin
                      </button>
                    ) : (
                      <button
                        className="button-ghost"
                        type="button"
                        disabled={isUpdatingMembers}
                        onClick={() => handleRoleChange(member.userId, 'MEMBER')}
                      >
                        降为 Member
                      </button>
                    )
                  ) : null}
                </div>
              </article>
            ))}
          </div>
        </div>

        <div className="panel panel-stack">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Team Tasks</p>
              <h2>团队任务列表</h2>
            </div>
            <div className="toolbar">
              {canManageTeamTasks ? (
                <button className="button-primary" type="button" onClick={handleOpenCreate}>
                  新建团队任务
                </button>
              ) : null}
              <button className="button-ghost" type="button" onClick={loadWorkspace}>
                刷新任务
              </button>
            </div>
          </div>

          <TaskFilters
            onFilterChange={handleFilterChange}
            totalCount={totalRecords}
            filteredCount={tasks.length}
          />

          <div className="pagination">
            <button
              className="button-ghost"
              type="button"
              disabled={currentPage <= 1 || loading}
              onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
            >
              上一页
            </button>
            <span>
              第 {currentPage} / {Math.max(totalPages, 1)} 页
            </span>
            <button
              className="button-ghost"
              type="button"
              disabled={currentPage >= totalPages || totalPages === 0 || loading}
              onClick={() => setCurrentPage((page) => page + 1)}
            >
              下一页
            </button>
          </div>

          {loading ? (
            <div className="empty-state">
              <p>团队任务加载中...</p>
            </div>
          ) : (
            <TaskList
              selectedTaskId={selectedTaskId}
              tasks={tasks}
              onCreate={canManageTeamTasks ? handleOpenCreate : () => undefined}
              onSelect={handleSelectTask}
            />
          )}
        </div>

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
            allowDetailEditing={formMode === 'create' ? Boolean(canManageTeamTasks) : Boolean(selectedTask?.canEditDetails)}
            allowStatusEditing={formMode === 'create' ? Boolean(canManageTeamTasks) : Boolean(selectedTask?.canEditStatus)}
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
