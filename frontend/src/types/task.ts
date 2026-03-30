import { toDateTimeLocalInput } from '../utils/date'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export type Task = {
  id: number
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueAt: string | null
  createdAt: string
  updatedAt: string
}

export type TaskPayload = {
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueAt: string | null
}

export type TaskFormValues = {
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueAt: string
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
  }
}
