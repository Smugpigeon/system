import {
  startTransition,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react'
import { useNavigate } from 'react-router-dom'
import {
  createTask,
  deleteTask,
  fetchTasks,
  updateTask,
  updateTeamTaskStatus,
  type TaskQueryParams,
} from '../api/tasks'
import { getErrorMessage } from '../api/http'
import { TaskForm } from '../components/TaskForm'
import { TaskList } from '../components/TaskList'
import { TaskFilters, type FilterOptions } from '../components/TaskFilters'
import { Toast } from '../components/Toast'
import { useAuth } from '../context/useAuth'
import { AppShell } from '../layout/AppShell'
import type { Task, TaskFormValues, TaskPayload } from '../types/task'
import { taskToFormValues } from '../types/task'

export function DashboardPage() {
  const navigate = useNavigate()
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
    keyword: '',
  })
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null)

  const loadTasks = useCallback(async () => {
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
          return
        }

        if (formMode === 'create' && selectedTaskId === null) {
          return
        }

        const stillExists = pageData.records.some((task) => task.id === selectedTaskId)
        if (!stillExists || selectedTaskId === null) {
          setSelectedTaskId(pageData.records[0].id)
          setFormMode('edit')
        }
      })
    } catch (error) {
      const message = getErrorMessage(error)
      setLoadingError(message)
      setToast({ message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }, [currentPage, filters.keyword, filters.priority, filters.status, pageSize, selectedTaskId, formMode])

  useEffect(() => {
    loadTasks()
  }, [loadTasks])

  const handleRefresh = () => {
    loadTasks()
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
        setToast({ message: '个人任务创建成功', type: 'success' })
        setCurrentPage(1)
        await loadTasks()
        return
      }

      const selectedTask = tasks.find((task) => task.id === selectedTaskId)
      if (!selectedTask) {
        setSubmitError('未选中任务，无法保存')
        return
      }

      if (selectedTask.scope === 'PERSONAL') {
        await updateTask(selectedTask.id, payload)
        setToast({ message: '个人任务更新成功', type: 'success' })
      } else if (selectedTask.teamId && selectedTask.canEditStatus) {
        await updateTeamTaskStatus(selectedTask.teamId, selectedTask.id, values.status)
        setToast({ message: '团队任务状态已更新', type: 'success' })
      } else {
        setSubmitError('当前任务不允许在个人工作台直接修改')
        return
      }

      await loadTasks()
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
    if (!selectedTask || selectedTask.scope !== 'PERSONAL') {
      setSubmitError('团队任务请在团队空间中删除')
      return
    }

    const confirmed = window.confirm('确认删除当前个人任务吗？该操作不可撤销。')
    if (!confirmed) {
      return
    }

    try {
      setIsSubmitting(true)
      setSubmitError('')
      await deleteTask(selectedTask.id)
      setToast({ message: '个人任务已删除', type: 'success' })
      await loadTasks()
    } catch (error) {
      const message = getErrorMessage(error)
      setSubmitError(message)
      setToast({ message, type: 'error' })
    } finally {
      setIsSubmitting(false)
    }
  }

  const selectedTask = tasks.find((task) => task.id === selectedTaskId) ?? null
  const selectedTaskHint = useMemo(() => {
    if (!selectedTask) {
      return ''
    }
    if (selectedTask.scope === 'TEAM') {
      if (selectedTask.canEditStatus) {
        return '这是团队任务。在个人工作台中你只能更新状态；如需调整标题、描述、负责人，请进入团队空间。'
      }
      return '这是团队任务。当前用户在个人工作台中仅可查看详情，请进入团队空间或联系管理员处理。'
    }
    return ''
  }, [selectedTask])

  const summary = {
    total: totalRecords,
    personal: tasks.filter((task) => task.scope === 'PERSONAL').length,
    assignedTeam: tasks.filter((task) => task.scope === 'TEAM').length,
    done: tasks.filter((task) => task.status === 'DONE').length,
  }

  return (
    <AppShell
      title="Keep personal work and team work visible in one dependable dashboard."
      description="Lab2 的个人工作台同时展示自己创建的个人任务，以及分配给自己的团队任务。个人任务可完整管理，团队任务会按权限限制操作。"
      aside={(
        <>
          <div className="aside-card">
            <h2>当前工作台范围</h2>
            <ul className="checkpoint-list">
              <li className="checkpoint-item">
                <span className="checkpoint-title">个人任务</span>
                <span className="checkpoint-copy">
                  当前用户自己创建的任务，支持完整增删改查。
                </span>
              </li>
              <li className="checkpoint-item">
                <span className="checkpoint-title">团队任务</span>
                <span className="checkpoint-copy">
                  当前用户被分配到的团队任务，按角色控制可编辑范围。
                </span>
              </li>
            </ul>
          </div>
          <div className="aside-card">
            <h2>切到团队空间时</h2>
            <p>
              Owner 可管理成员与角色，Admin 可创建、编辑、删除和重新分配团队任务，Member 只能浏览任务并修改分配给自己的任务状态。
            </p>
          </div>
        </>
      )}
    >
      <header className="main-header">
        <div>
          <p className="eyebrow">Personal Dashboard / Lab2</p>
          <h1>{auth?.username} 的工作台</h1>
          <p>
            在这里统一查看个人任务和分配给你的团队任务。团队协作管理入口放在独立团队空间，避免权限判断散落在同一页面里。
          </p>
        </div>
        <div className="toolbar">
          <button className="button-ghost" type="button" onClick={() => navigate('/teams')}>
            我的团队
          </button>
          <button className="button-primary" type="button" onClick={handleOpenCreate}>
            新建个人任务
          </button>
          <button className="button-ghost" type="button" onClick={handleRefresh}>
            刷新列表
          </button>
          <button className="button-ghost" type="button" onClick={logout}>
            退出登录
          </button>
        </div>
      </header>

      <section className="summary-grid">
        <article className="summary-card">
          <h3>总任务数</h3>
          <strong>{summary.total}</strong>
          <span>当前页已加载的总任务记录</span>
        </article>
        <article className="summary-card">
          <h3>个人任务</h3>
          <strong>{summary.personal}</strong>
          <span>由当前用户创建</span>
        </article>
        <article className="summary-card">
          <h3>团队任务</h3>
          <strong>{summary.assignedTeam}</strong>
          <span>当前用户被分配到的团队任务</span>
        </article>
        <article className="summary-card">
          <h3>已完成</h3>
          <strong>{summary.done}</strong>
          <span>状态为 DONE 的任务</span>
        </article>
      </section>

      <TaskFilters
        onFilterChange={handleFilterChange}
        totalCount={totalRecords}
        filteredCount={tasks.length}
      />

      {loadingError ? <div className="message message--error">{loadingError}</div> : null}

      <section className="workspace-grid">
        <div className="panel panel-stack">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Task Stream</p>
              <h2>当前任务</h2>
            </div>
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
          </div>
          {loading ? (
            <div className="empty-state">
              <p>任务列表加载中...</p>
            </div>
          ) : (
            <TaskList
              selectedTaskId={selectedTaskId}
              tasks={tasks}
              onCreate={handleOpenCreate}
              onSelect={handleSelectTask}
            />
          )}
        </div>

        <div className="panel panel-stack">
          <div className="panel-header">
            <div>
              <p className="eyebrow">{formMode === 'create' ? 'Create Personal Task' : 'Task Detail'}</p>
              <h2>{formMode === 'create' ? '新建个人任务' : '任务详情'}</h2>
            </div>
            {selectedTask?.scope === 'TEAM' && selectedTask.teamId ? (
              <button
                className="button-ghost"
                type="button"
                onClick={() => navigate(`/teams/${selectedTask.teamId}`)}
              >
                前往团队空间
              </button>
            ) : null}
          </div>
          <TaskForm
            mode={formMode}
            initialValues={taskToFormValues(selectedTask)}
            error={submitError}
            isSubmitting={isSubmitting}
            onCancelCreate={() => {
              setFormMode('create')
              setSelectedTaskId(null)
            }}
            onDelete={selectedTask?.scope === 'PERSONAL' ? handleDelete : undefined}
            onSubmit={handleSubmit}
            allowDetailEditing={formMode === 'create' || selectedTask?.scope === 'PERSONAL'}
            allowStatusEditing={formMode === 'create' || Boolean(selectedTask?.canEditStatus)}
            allowDelete={selectedTask?.scope === 'PERSONAL'}
            readOnlyHint={selectedTaskHint}
            submitLabel={
              formMode === 'create'
                ? '创建个人任务'
                : selectedTask?.scope === 'TEAM'
                  ? '更新任务状态'
                  : '保存修改'
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
