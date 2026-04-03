import {
  startTransition,
  useCallback,
  useEffect,
  useState,
} from 'react'
import { createTask, deleteTask, fetchTasks, updateTask, type TaskQueryParams } from '../api/tasks'
import { getErrorMessage } from '../api/http'
import { TaskForm } from '../components/TaskForm'
import { TaskList } from '../components/TaskList'
import { TaskFilters, type FilterOptions } from '../components/TaskFilters'
import { Toast } from '../components/Toast'
import { useAuth } from '../context/useAuth'
import { AppShell } from '../layout/AppShell'
import type { Task, TaskFormValues, TaskPayload } from '../types/task'
import { emptyTaskFormValues, taskToFormValues } from '../types/task'

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
      
      const pageData = await fetchTasks(params)
      
      startTransition(() => { 
        setTotalPages(pageData.totalPages)
        setTotalRecords(pageData.totalRecords)
        
        let filteredRecords = pageData.records
        if (filters.keyword.trim()) {
          const keyword = filters.keyword.toLowerCase()
          filteredRecords = pageData.records.filter(task => 
            task.title.toLowerCase().includes(keyword) ||
            (task.description && task.description.toLowerCase().includes(keyword))
          )
        }
        
        setTasks(filteredRecords)
        
        if (!filteredRecords.length) {
          setSelectedTaskId(null)
          setFormMode('create')
        } else {
          if (formMode === 'create' && selectedTaskId === null) {
            return
          }
          const stillExists = filteredRecords.some(t => t.id === selectedTaskId)
          if (!stillExists || selectedTaskId === null) {
            setSelectedTaskId(filteredRecords[0].id)
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

  useEffect(() => {
    loadTasks()
  }, [currentPage, filters.status, filters.priority, loadTasks])

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

  return (
    <AppShell
      title="Forge personal work into a clean, explainable Lab1 demo."
      description="这个版本只做个人任务与认证，但已经把后续团队协作、权限控制和任务依赖的扩展位置留出来。"
      aside={
        <>
          <div className="aside-card">
            <h2>当前验收重点</h2>
            <ul className="checkpoint-list">
              <li className="checkpoint-item">
                <span className="checkpoint-title">认证闭环</span>
                <span className="checkpoint-copy">
                  注册、登录、令牌保持、未登录访问拦截。
                </span>
              </li>
              <li className="checkpoint-item">
                <span className="checkpoint-title">任务闭环</span>
                <span className="checkpoint-copy">
                  个人任务创建、修改、删除、详情与数据隔离。
                </span>
              </li>
              <li className="checkpoint-item">
                <span className="checkpoint-title">可扩展结构</span>
                <span className="checkpoint-copy">
                  前后端都按模块拆分，后续同学不必重构基础层。
                </span>
              </li>
            </ul>
          </div>

          <div className="aside-card">
            <h2>后续可继续接入</h2>
            <ul className="roadmap-list">
              <li className="roadmap-item">
                <span className="roadmap-title">团队与项目空间</span>
                <span className="roadmap-copy">
                  在 `backend/task` 基础上继续扩展 project、member、role。
                </span>
              </li>
              <li className="roadmap-item">
                <span className="roadmap-title">权限与审计日志</span>
                <span className="roadmap-copy">
                  复用现有安全链路，增加角色判断和操作记录表。
                </span>
              </li>
              <li className="roadmap-item">
                <span className="roadmap-title">任务依赖与评论</span>
                <span className="roadmap-copy">
                  前端列表和详情面板已经预留出继续加组件的空间。
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
            用这个页面直接演示 Lab1 的核心流程：登录成功后进入任务管理界面，
            任务只归当前用户所有，刷新后登录状态仍然有效。
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
          <p className="summary-note">覆盖新增、编辑、详情与删除的完整流程。</p>
        </article>
        <article className="summary-card">
          <h3>待处理</h3>
          <div className="summary-number">{summary.pending}</div>
          <p className="summary-note">用于演示任务状态流转与优先级区分。</p>
        </article>
        <article className="summary-card">
          <h3>已完成</h3>
          <div className="summary-number">{summary.done}</div>
          <p className="summary-note">展示个人任务的完成情况与更新时间。</p>
        </article>
      </section>

      <section className="filters-section">
        <TaskFilters
          onFilterChange={handleFilterChange}
          totalCount={totalRecords}
          filteredCount={tasks.length}
        />
      </section>
      
      <section className="content-grid">
        <article className="panel">
          <header className="panel-header">
            <p className="eyebrow">Task list</p>
            <h2 className="panel-title">我的任务</h2>
            <p className="panel-subtitle">
              每条任务都来自当前登录用户，天然满足数据隔离要求。
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
                ? '先把 Lab1 的核心 CRUD 跑通，后面再接入指派、评论、依赖关系。'
                : '右侧表单直接承担查看详情与修改任务的职责，演示时路径更短。'}
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
            <h3>给队友的扩展入口</h3>
            <ul className="detail-list">
              <li className="detail-item">
                <span className="detail-title">前端可继续拆分列表筛选与分页</span>
                <span className="detail-copy">
                  当前页面已按 `pages / components / api / context / types` 拆开。
                </span>
              </li>
              <li className="detail-item">
                <span className="detail-title">后端可继续补 service、test 与更多 DTO</span>
                <span className="detail-copy">
                  `auth` 和 `task` 模块已经分包，不会挤成单个 God class。
                </span>
              </li>
            </ul>
          </section>
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
