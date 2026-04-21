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
      title="Software Engineering Lab"
      description=""
      aside={
        <>
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
