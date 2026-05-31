import { toDateTimeLocalInput } from '../utils/date'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export type TaskScope = 'PERSONAL' | 'TEAM'

export type Task = {
  id: number
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueAt: string | null
  createdAt: string
  updatedAt: string
  scope: TaskScope
  teamId: number | null
  teamName: string | null
  ownerId: number
  ownerUsername: string
  // 后端允许 null：成员离队 / 被移除后任务的 assignee 会被清空，但状态保留（见 fix 014d201）
  assigneeId: number | null
  assigneeUsername: string | null
  canEditDetails: boolean
  canEditStatus: boolean
  canDelete: boolean
}

export type TaskPayload = {
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueAt: string | null
}

export type TeamTaskPayload = TaskPayload & {
  assigneeId: number
}

export type TaskFormValues = {
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueAt: string
  assigneeId: string
}

export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: '待开始',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
}

export const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: '低优先',
  MEDIUM: '中优先',
  HIGH: '高优先',
}

export const SCOPE_LABELS: Record<TaskScope, string> = {
  PERSONAL: '个人',
  TEAM: '团队',
}

export const STATUS_OPTIONS = [
  { value: 'TODO', label: STATUS_LABELS.TODO },
  { value: 'IN_PROGRESS', label: STATUS_LABELS.IN_PROGRESS },
  { value: 'DONE', label: STATUS_LABELS.DONE },
] as const

export const PRIORITY_OPTIONS = [
  { value: 'LOW', label: PRIORITY_LABELS.LOW },
  { value: 'MEDIUM', label: PRIORITY_LABELS.MEDIUM },
  { value: 'HIGH', label: PRIORITY_LABELS.HIGH },
] as const

export const emptyTaskFormValues: TaskFormValues = {
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  dueAt: '',
  assigneeId: '',
}

export function taskToFormValues(task: Task | null): TaskFormValues {
  if (!task) {
    return emptyTaskFormValues
  }

  return {
    title: task.title,
    description: task.description,
    status: task.status,
    priority: task.priority,
    dueAt: toDateTimeLocalInput(task.dueAt),
    // assigneeId 可能为 null（成员离队 / 被移除后任务变未分配），表单用空串表示
    assigneeId: task.assigneeId == null ? '' : String(task.assigneeId),
  }
}

// ─── Task Dependency Types ────────────────────────────────────────────────────

/**
 * 字段对齐后端 DependencyTaskInfo DTO。
 * `id` 是关联任务的 id；删除依赖时也用它当 predecessorId。
 * 列表方向（前置 / 后继）由调用方按数组归属判断，不需要 isOutgoing 标志。
 */
export type TaskDependencyItem = {
  id: number
  title: string
  status: TaskStatus
  ownerUsername: string
  assigneeUsername: string | null
}

export type TaskDependency = {
  id: number
  fromTaskId: number
  toTaskId: number
  createdAt: string
}

/**
 * The shape returned by GET /tasks/:id/dependencies
 */
export type TaskDependenciesResponse = {
  predecessors: TaskDependencyItem[]   
  successors: TaskDependencyItem[]     
}

export type TaskWithDeps = Task & {
  dependencies: TaskDependencyItem[]
  dependents: TaskDependencyItem[]
}
