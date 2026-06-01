import { useAutoAnimate } from '@formkit/auto-animate/react'
import type { Task } from '../types/task'
import {
  PRIORITY_LABELS,
  SCOPE_LABELS,
  STATUS_LABELS,
} from '../types/task'
import { formatDateTime } from '../utils/date'

type DepCount = { deps: number; dependents: number }

type TaskListProps = {
  selectedTaskId: number | null
  tasks: Task[]
  onCreate: () => void
  onSelect: (task: Task) => void
  /** Map of taskId → dependency counts, used to show the 🔗 badge */
  depCounts?: Record<number, DepCount>
}

export function TaskList({
  selectedTaskId,
  tasks,
  onCreate,
  onSelect,
  depCounts = {},
}: TaskListProps) {
  // 任务卡片增删改时自动平滑过渡——比 Framer Motion 配置更省，零业务逻辑改动
  const [listRef] = useAutoAnimate<HTMLDivElement>()

  if (!tasks.length) {
    return (
      <div className="empty-state">
        <p>当前没有可展示的任务。你可以先创建个人任务，或去团队空间领取协作任务。</p>
        <button className="button-primary" type="button" onClick={onCreate}>
          新建第一条任务
        </button>
      </div>
    )
  }

  return (
    <div className="task-list" ref={listRef}>
      {tasks.map((task) => {
        const counts = depCounts[task.id]
        const hasDeps = counts !== undefined && (counts.deps > 0 || counts.dependents > 0)

        return (
          <button
            key={task.id}
            className={`task-card ${selectedTaskId === task.id ? 'task-card--active' : ''}`}
            type="button"
            onClick={() => onSelect(task)}
          >
            <div className="task-card__top">
              <div className="task-card__headings">
                <p className="task-card__eyebrow">
                  {SCOPE_LABELS[task.scope]}
                  {task.teamName ? ` / ${task.teamName}` : ''}
                </p>
                <h3 className="task-card__title">{task.title}</h3>
              </div>
              <div className="badge-row">
                <span className={`badge badge--scope-${task.scope.toLowerCase()}`}>
                  {SCOPE_LABELS[task.scope]}
                </span>
                <span className={`badge badge--status-${task.status}`}>
                  {STATUS_LABELS[task.status]}
                </span>
                <span className={`badge badge--priority-${task.priority}`}>
                  {PRIORITY_LABELS[task.priority]}
                </span>
                {hasDeps && (
                  <span
                    className="badge badge--deps"
                    title={`${counts.deps} 个前置任务 · ${counts.dependents} 个后继任务`}
                  >
                    🔗 {counts.deps}/{counts.dependents}
                  </span>
                )}
              </div>
            </div>

            <p className="task-card__description">
              {task.description || '暂无任务描述'}
            </p>

            <div className="task-card__meta">
              <span>ID: {task.id}</span> 
              <span>负责人：{task.assigneeUsername ?? '未分配'}</span>
              <span>创建者：{task.ownerUsername}</span>
              <span>截止：{formatDateTime(task.dueAt)}</span>
              <span>更新：{formatDateTime(task.updatedAt)}</span>
            </div>
          </button>
        )
      })}
    </div>
  )
}
