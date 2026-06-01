import {
  startTransition,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { useNavigate } from 'react-router-dom'
import {
  createTask,
  deleteTask,
  fetchTasks,
  updateTask,
  updateTeamTaskStatus,
  fetchTaskDependencies,
  addTaskDependency,
  removeTaskDependency,
  fetchAvailableDependencies,
  type TaskQueryParams,
} from '../api/tasks'
import { getErrorMessage } from '../api/http'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { TaskForm } from '../components/TaskForm'
import { TaskList } from '../components/TaskList'
import { TaskFilters, type FilterOptions } from '../components/TaskFilters'
import { Toast } from '../components/Toast'
import { useAuth } from '../context/useAuth'
import { AppShell } from '../layout/AppShell'
import type {
  Task,
  TaskFormValues,
  TaskPayload,
  TaskDependencyItem,
} from '../types/task'
import { taskToFormValues, STATUS_LABELS } from '../types/task'
import './DashboardPage.css'

export function DashboardPage() {
  const navigate = useNavigate()
  const { auth } = useAuth()

  // ── Task list state ────────────────────────────────────────────────────────
  const [tasks, setTasks] = useState<Task[]>([])
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null)
  const [formMode, setFormMode] = useState<'create' | 'edit'>('create')
  // 新建表单默认折叠，点击「新建个人任务」后以下拉动画展开；编辑模式恒展开
  const [createOpen, setCreateOpen] = useState(false)
  // 指向折叠表单的标题行，展开后平滑滚动过去，避免用户以为按钮没反应
  const createToggleRef = useRef<HTMLButtonElement>(null)

  const scrollToForm = () => {
    // 等展开动画推进后，把新建卡滚到页面底部，确保整栏可见
    window.setTimeout(() => {
      createToggleRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 220)
  }
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
  const [toast, setToast] = useState<{
    message: string
    type: 'success' | 'error' | 'info'
  } | null>(null)

  // ── Dependency state ───────────────────────────────────────────────────────
  /** Tasks that the selected task depends on (prerequisites) */
  const [dependencies, setDependencies] = useState<TaskDependencyItem[]>([])
  /** Tasks that depend on the selected task (successors) */
  const [dependents, setDependents] = useState<TaskDependencyItem[]>([])
  /** Tasks available to be added as a new dependency */
  const [availableDeps, setAvailableDeps] = useState<Task[]>([])
  const [showDepModal, setShowDepModal] = useState(false)
  const [depLoading, setDepLoading] = useState(false)
  const [dependencyError, setDependencyError] = useState('')

  /**
   * Build a quick lookup: taskId → { deps, dependents }
   * Used by TaskList to show the 🔗 badge on the currently-selected task.
   */
  const depCounts = useMemo<Record<number, { deps: number; dependents: number }>>(() => {
    if (selectedTaskId === null) return {}
    return {
      [selectedTaskId]: {
        deps: dependencies.length,
        dependents: dependents.length,
      },
    }
  }, [dependencies, dependents, selectedTaskId])

  // ── Load tasks ─────────────────────────────────────────────────────────────
  const loadTasks = useCallback(async () => {
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

        if (formMode === 'create' && selectedTaskId === null) return

        const stillExists = pageData.records.some((t) => t.id === selectedTaskId)
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

  // 用 ref 跟踪最新的依赖请求 id；快速切任务时丢弃过期响应，防止旧请求覆盖新数据
  const depRequestIdRef = useRef(0)

  // ── Load dependencies whenever selected personal task changes ──────────────
  const loadDependencies = useCallback(async (taskId: number) => {
    const requestId = ++depRequestIdRef.current
    setDepLoading(true)
    try {
      const data = await fetchTaskDependencies(taskId)
      if (depRequestIdRef.current !== requestId) return
      setDependencies(data.predecessors ?? [])
      setDependents(data.successors ?? [])
      setDependencyError('')
    } catch (error) {
      if (depRequestIdRef.current !== requestId) return
      const message = getErrorMessage(error)
      setDependencies([])
      setDependents([])
      setDependencyError(message)
      setToast({ message, type: 'error' })
    } finally {
      if (depRequestIdRef.current === requestId) setDepLoading(false)
    }
  }, [])

  useEffect(() => {
    const selected = tasks.find((t) => t.id === selectedTaskId)
    if (formMode === 'edit' && selected?.scope === 'PERSONAL' && selectedTaskId !== null) {
      loadDependencies(selectedTaskId)
    } else {
      setDependencies([])
      setDependents([])
    }
  }, [selectedTaskId, formMode, tasks, loadDependencies])

  // ── Dependency handlers ────────────────────────────────────────────────────
  const loadAvailableDeps = useCallback(async (taskId: number) => {
    try {
      const data = await fetchAvailableDependencies(taskId)
      setAvailableDeps(data)
      setDependencyError('')
    } catch (error) {
      const message = getErrorMessage(error)
      setAvailableDeps([])
      setDependencyError(message)
      setToast({ message, type: 'error' })
    }
  }, [])

  const handleOpenDepModal = () => {
    if (!selectedTaskId) return
    loadAvailableDeps(selectedTaskId)
    setShowDepModal(true)
  }

  const handleAddDependency = async (toTaskId: number) => {
    if (!selectedTaskId) return
    try {
      await addTaskDependency(selectedTaskId, toTaskId)
      await loadDependencies(selectedTaskId)
      setToast({ message: '前置任务已添加', type: 'success' })
    } catch (error) {
      setToast({ message: getErrorMessage(error), type: 'error' })
    }
  }

  const handleRemoveDependency = async (predecessorTaskId: number) => {
    if (!selectedTaskId) return
    try {
      await removeTaskDependency(selectedTaskId, predecessorTaskId)
      await loadDependencies(selectedTaskId)
      setToast({ message: '依赖关系已移除', type: 'success' })
    } catch (error) {
      setToast({ message: getErrorMessage(error), type: 'error' })
    }
  }

  // ── Task CRUD handlers ─────────────────────────────────────────────────────
  const handleRefresh = () => loadTasks()

  const handleFilterChange = (newFilters: FilterOptions) => {
    setFilters(newFilters)
    setCurrentPage(1)
  }

  const handleOpenCreate = () => {
    setSelectedTaskId(null)
    setSubmitError('')
    setFormMode('create')
    setCreateOpen(true)
    scrollToForm()
  }

  const handleSelectTask = (task: Task) => {
    setSelectedTaskId(task.id)
    setSubmitError('')
    setFormMode('edit')
    setCreateOpen(true)
    scrollToForm()
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

      const selected = tasks.find((t) => t.id === selectedTaskId)
      if (!selected) {
        setSubmitError('未选中任务，无法保存')
        return
      }

      if (selected.scope === 'PERSONAL') {
        await updateTask(selected.id, payload)
        setToast({ message: '个人任务更新成功', type: 'success' })
      } else if (selected.teamId && selected.canEditStatus) {
        await updateTeamTaskStatus(selected.teamId, selected.id, values.status)
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

  const [confirm, setConfirm] = useState<{
    title: string
    message: string
    tone?: 'default' | 'danger'
    confirmLabel?: string
    onConfirm: () => Promise<void> | void
  } | null>(null)

  const handleDelete = () => {
    const selected = tasks.find((t) => t.id === selectedTaskId)
    if (!selected || selected.scope !== 'PERSONAL') {
      setSubmitError('团队任务请在团队空间中删除')
      return
    }

    setConfirm({
      title: '删除个人任务',
      message: '确认删除当前个人任务吗？该操作不可撤销。',
      tone: 'danger',
      confirmLabel: '删除',
      onConfirm: async () => {
        try {
          setIsSubmitting(true)
          setSubmitError('')
          await deleteTask(selected.id)
          setToast({ message: '个人任务已删除', type: 'success' })
          await loadTasks()
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

  // ── Derived values ─────────────────────────────────────────────────────────
  const selectedTask = tasks.find((t) => t.id === selectedTaskId) ?? null

  const selectedTaskHint = useMemo(() => {
    if (!selectedTask) return ''
    if (selectedTask.scope === 'TEAM') {
      return selectedTask.canEditStatus
        ? '这是团队任务。在个人工作台中你只能更新状态；如需调整标题、描述、负责人，请进入团队空间。'
        : '这是团队任务。当前用户在个人工作台中仅可查看详情，请进入团队空间或联系管理员处理。'
    }
    return ''
  }, [selectedTask])

  /** Unfinished prerequisites blocking this task from being marked DONE */
  const blockedByUnfinished = useMemo(
    () => dependencies.filter((d) => d.status !== 'DONE'),
    [dependencies],
  )

  const summary = {
    total: totalRecords,
    personal: tasks.filter((t) => t.scope === 'PERSONAL').length,
    assignedTeam: tasks.filter((t) => t.scope === 'TEAM').length,
    done: tasks.filter((t) => t.status === 'DONE').length,
  }

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <AppShell
      title="Keep personal work and team work visible in one dependable dashboard."
      description="Lab3 的个人工作台同时展示自己创建的个人任务，以及分配给自己的团队任务。个人任务可完整管理（含依赖关系），团队任务会按权限限制操作。"
      aside={(
        <>
          <div className="aside-card">
            <h2>当前工作台范围</h2>
            <ul className="checkpoint-list">
              <li className="checkpoint-item">
                <span className="checkpoint-title">个人任务</span>
                <span className="checkpoint-copy">
                  当前用户自己创建的任务，支持完整增删改查及依赖管理。
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
      {/* ── Header ── */}
      <header className="main-header">
        <div>
          <p className="eyebrow">Personal Dashboard / Lab3</p>
          <h1>{auth?.username} 的工作台</h1>
        </div>
        <div className="toolbar">
          <button className="button-ghost" type="button" onClick={handleRefresh}>
            刷新列表
          </button>
          <button className="button-primary" type="button" onClick={handleOpenCreate}>
            新建个人任务
          </button>
        </div>
      </header>

      {/* ── Summary cards ── */}
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

      {loadingError ? <div className="message message--error">{loadingError}</div> : null}

      {/* ── Workspace ── */}
      <section className="workspace-grid">
        {/* Task list panel */}
        <div className="panel panel-stack">
          <div className="panel-header">
            <div>
              <p className="eyebrow">My Tasks</p>
              <h2>我的任务</h2>
            </div>
            <div className="pagination">
              <button
                className="button-ghost"
                type="button"
                disabled={currentPage <= 1 || loading}
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              >
                上一页
              </button>
              <span>第 {currentPage} / {Math.max(totalPages, 1)} 页</span>
              <button
                className="button-ghost"
                type="button"
                disabled={currentPage >= totalPages || totalPages === 0 || loading}
                onClick={() => setCurrentPage((p) => p + 1)}
              >
                下一页
              </button>
            </div>
          </div>

          {/* 筛选条件就放在清单上方，选完即时筛出符合的任务清单 */}
          <TaskFilters
            onFilterChange={handleFilterChange}
            totalCount={totalRecords}
            filteredCount={tasks.length}
          />

          {loading ? (
            <div className="empty-state"><p>任务列表加载中...</p></div>
          ) : (
            <TaskList
              selectedTaskId={selectedTaskId}
              tasks={tasks}
              depCounts={depCounts}
              onCreate={handleOpenCreate}
              onSelect={handleSelectTask}
            />
          )}

        </div>

        {/* 新建 / 编辑任务独立成一张卡；点新建后滚动到这里并展开 */}
        <div className="panel panel-stack">
          <button
            ref={createToggleRef}
            type="button"
            className="panel-header panel-header--toggle"
            onClick={() => {
              if (formMode === 'create') {
                setCreateOpen((open) => !open)
              } else {
                handleOpenCreate()
              }
            }}
            aria-expanded={createOpen || formMode === 'edit'}
          >
            <div>
              <p className="eyebrow">{formMode === 'create' ? 'Create Personal Task' : 'Task Detail'}</p>
              <h2>{formMode === 'create' ? '新建个人任务' : '任务详情'}</h2>
            </div>
            <motion.span
              className="panel-chevron"
              animate={{ rotate: (createOpen || formMode === 'edit') ? 180 : 0 }}
              transition={{ duration: 0.2, ease: [0, 0, 0.2, 1] }}
              aria-hidden="true"
            >
              ⌄
            </motion.span>
          </button>

          <AnimatePresence initial={false}>
            {(createOpen || formMode === 'edit') && (
              <motion.div
                key="create-form"
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                transition={{ duration: 0.28, ease: [0.2, 0, 0, 1] }}
                style={{ overflow: 'hidden' }}
              >
                <div className="panel-stack" style={{ paddingTop: '1.25rem' }}>
                  {selectedTask?.scope === 'TEAM' && selectedTask.teamId ? (
                    <button
                      className="button-ghost"
                      type="button"
                      onClick={() => navigate(`/teams/${selectedTask.teamId}`)}
                    >
                      前往团队空间
                    </button>
                  ) : null}

                  {/* Blocked-by hint — shown when trying to DONE a task with unfinished prerequisites */}
                  {formMode === 'edit'
                    && selectedTask?.scope === 'PERSONAL'
                    && blockedByUnfinished.length > 0 && (
                    <div className="message message--warning">
                      ⚠️ 该任务有 {blockedByUnfinished.length} 个前置任务尚未完成（
                      {blockedByUnfinished.map((d) => d.title).join('、')}
                      ），无法标记为已完成。
                    </div>
                  )}

                  <TaskForm
                    mode={formMode}
                    initialValues={taskToFormValues(selectedTask)}
                    error={submitError}
                    isSubmitting={isSubmitting}
                    onCancelCreate={() => {
                      setFormMode('create')
                      setSelectedTaskId(null)
                      setCreateOpen(false)
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
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Dependency panel — only for personal tasks in edit mode */}
        {formMode === 'edit' && selectedTask?.scope === 'PERSONAL' && (
          <div className="panel panel-stack">
            <div className="panel-header">
              <div>
                <p className="eyebrow">Dependencies</p>
                <h2>任务依赖</h2>
              </div>
              <button
                className="button-primary"
                type="button"
                onClick={handleOpenDepModal}
                disabled={depLoading}
              >
                + 添加前置任务
              </button>
            </div>

            {/* Prerequisites */}
            <div className="dependency-section">
              <h4>前置任务（此任务依赖的任务）</h4>
              {dependencyError ? <div className="message message--error">{dependencyError}</div> : null}
              {depLoading ? (
                <p>加载中...</p>
              ) : dependencies.length === 0 ? (
                <p className="empty-text">暂无前置任务</p>
              ) : (
                <ul className="dependency-list">
                  {dependencies.map((dep) => (
                    <li key={dep.id} className="dependency-item">
                      <span className={`status-badge status-badge--${dep.status.toLowerCase()}`}>
                        {STATUS_LABELS[dep.status]}
                      </span>
                      <span className="dep-title">{dep.title}</span>
                      <button
                        className="button-ghost button-sm"
                        type="button"
                        onClick={() => handleRemoveDependency(dep.id)}
                        title="移除此依赖"
                      >
                        ×
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {/* Successors */}
            <div className="dependency-section">
              <h4>后继任务（依赖此任务的任务）</h4>
              {depLoading ? (
                <p>加载中...</p>
              ) : dependents.length === 0 ? (
                <p className="empty-text">暂无后继任务</p>
              ) : (
                <ul className="dependency-list">
                  {dependents.map((dep) => (
                    <li key={dep.id} className="dependency-item">
                      <span className={`status-badge status-badge--${dep.status.toLowerCase()}`}>
                        {STATUS_LABELS[dep.status]}
                      </span>
                      <span className="dep-title">{dep.title}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}
      </section>

      {/* ── Add-dependency modal ── */}
      {showDepModal && (
        <div
          className="modal-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="选择前置任务"
          onClick={() => setShowDepModal(false)}
        >
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>选择前置任务</h3>
              <button
                className="button-ghost"
                type="button"
                aria-label="关闭"
                onClick={() => setShowDepModal(false)}
              >
                ×
              </button>
            </div>
            <div className="modal-body">
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
                        <span className="dep-title">{task.title}</span>
                        <span className="dep-priority">{task.priority}</span>
                      </div>
                      <button
                        className="button-primary button-sm"
                        type="button"
                        onClick={() => {
                          handleAddDependency(task.id)
                          setShowDepModal(false)
                        }}
                      >
                        选择
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── Toast ── */}
      {toast ? (
        <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />
      ) : null}
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
