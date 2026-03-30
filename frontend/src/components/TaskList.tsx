import type { Task } from '../types/task'
import {
  PRIORITY_LABELS,
  STATUS_LABELS,
} from '../types/task'
import { formatDateTime } from '../utils/date'

type TaskListProps = {
  selectedTaskId: number | null
  tasks: Task[]
  onCreate: () => void
  onSelect: (task: Task) => void
}

export function TaskList({
  selectedTaskId,
  tasks,
  onCreate,
  onSelect,
}: TaskListProps) {
  if (!tasks.length) {
    return (
      <div className="empty-state">
        <p>还没有任务。先创建一条任务，把演示流程跑通。</p>
        <button className="button-primary" type="button" onClick={onCreate}>
          新建第一条任务
        </button>
      </div>
    )
  }

  return (
    <div className="task-list">
      {tasks.map((task) => (
        <button
          key={task.id}
          className={`task-card ${selectedTaskId === task.id ? 'task-card--active' : ''}`}
          type="button"
          onClick={() => onSelect(task)}
        >
          <div className="task-card__top">
            <h3 className="task-card__title">{task.title}</h3>
            <div className="badge-row">
              <span className={`badge badge--status-${task.status}`}>
                {STATUS_LABELS[task.status]}
              </span>
              <span className={`badge badge--priority-${task.priority}`}>
                {PRIORITY_LABELS[task.priority]}
              </span>
            </div>
          </div>

          <p className="task-card__description">
            {task.description || '暂无任务描述'}
          </p>

          <div className="task-card__meta">
            <span>截止：{formatDateTime(task.dueAt)}</span>
            <span>更新：{formatDateTime(task.updatedAt)}</span>
          </div>
        </button>
      ))}
    </div>
  )
}
