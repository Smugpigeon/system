import type { Task } from '../types/task'
import {
  PRIORITY_LABELS,
  SCOPE_LABELS,
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
        <p>当前没有可展示的任务。你可以先创建个人任务，或去团队空间领取协作任务。</p>
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
              {task.blockedByDependencies ? (
                <span className="badge badge--blocked">
                  阻塞 {task.unfinishedPredecessorCount}
                </span>
              ) : null}
            </div>
          </div>

          <p className="task-card__description">
            {task.description || '暂无任务描述'}
          </p>

          <div className="task-card__meta">
            <span>负责人：{task.assigneeUsername}</span>
            <span>创建者：{task.ownerUsername}</span>
            <span>截止：{formatDateTime(task.dueAt)}</span>
            <span>更新：{formatDateTime(task.updatedAt)}</span>
            <span>前置：{task.predecessorCount}</span>
            <span>后继：{task.successorCount}</span>
          </div>
        </button>
      ))}
    </div>
  )
}
