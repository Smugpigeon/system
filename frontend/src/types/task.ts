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
  assigneeId: number
  assigneeUsername: string
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
    assigneeId: String(task.assigneeId),
  }
}

// ─── Task Dependency Types ────────────────────────────────────────────────────

/**
 * A single dependency record returned by the API.
 * `dependencyId` is the PK of the dependency row itself (used for deletion).
 * `taskId`       is the related task's id.
 * `title`        is the related task's title.
 * `status`       is the related task's current status.
 * `isOutgoing`   true  → this entry is a task that the selected task DEPENDS ON
 *                false → this entry is a task that DEPENDS ON the selected task
 */
export type TaskDependencyItem = {
  dependencyId: number
  taskId: number
  title: string
  status: TaskStatus
  isOutgoing: boolean
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
