import { http } from './http'
import type { ApiResponse, PageResponse } from '../types/api'
import type {
  Task,
  TaskPayload,
  TeamTaskPayload,
  TaskStatus,
  TaskDependency,
  TaskDependencyItem,
  TaskDependenciesResponse,
} from '../types/task'

export type TaskQueryParams = {
  page?: number
  size?: number
  status?: string
  priority?: string
  keyword?: string
  sortBy?: string
}

export async function fetchTasks(params?: TaskQueryParams) {
  const response = await http.get<ApiResponse<PageResponse<Task>>>('/tasks', {
    params,
  })
  return response.data.data
}

export async function createTask(payload: TaskPayload) {
  const response = await http.post<ApiResponse<Task>>('/tasks', payload)
  return response.data.data
}

export async function updateTask(taskId: number, payload: TaskPayload) {
  const response = await http.put<ApiResponse<Task>>(`/tasks/${taskId}`, payload)
  return response.data.data
}

export async function deleteTask(taskId: number) {
  await http.delete(`/tasks/${taskId}`)
}

export async function fetchTeamTasks(teamId: number, params?: TaskQueryParams) {
  const response = await http.get<ApiResponse<PageResponse<Task>>>(`/teams/${teamId}/tasks`, {
    params,
  })
  return response.data.data
}

export async function fetchTeamTask(teamId: number, taskId: number) {
  const response = await http.get<ApiResponse<Task>>(`/teams/${teamId}/tasks/${taskId}`)
  return response.data.data
}

export async function createTeamTask(teamId: number, payload: TeamTaskPayload) {
  const response = await http.post<ApiResponse<Task>>(`/teams/${teamId}/tasks`, payload)
  return response.data.data
}

export async function updateTeamTask(teamId: number, taskId: number, payload: TeamTaskPayload) {
  const response = await http.put<ApiResponse<Task>>(`/teams/${teamId}/tasks/${taskId}`, payload)
  return response.data.data
}

export async function updateTeamTaskStatus(teamId: number, taskId: number, status: TaskStatus) {
  const response = await http.patch<ApiResponse<Task>>(`/teams/${teamId}/tasks/${taskId}/status`, { status })
  return response.data.data
}

export async function deleteTeamTask(teamId: number, taskId: number) {
  await http.delete(`/teams/${teamId}/tasks/${taskId}`)
}

// ─── Task Dependency API ──────────────────────────────────────────────────────

/**
 * Fetch all dependency relationships for a personal task.
 * Returns both prerequisites (dependencies) and successors (dependents).
 */
export async function fetchTaskDependencies(taskId: number): Promise<TaskDependenciesResponse> {
  const response = await http.get<ApiResponse<TaskDependenciesResponse>>(
    `/tasks/${taskId}/dependencies`,
  )
  return response.data.data
}

/**
 * Create a dependency: `fromTaskId` depends on `toTaskId`.
 * i.e. `toTaskId` must be DONE before `fromTaskId` can be marked DONE.
 */
export async function addTaskDependency(
  fromTaskId: number,
  toTaskId: number,
): Promise<TaskDependency> {
  const response = await http.post<ApiResponse<TaskDependency>>('/tasks/dependencies', {
    fromTaskId,
    toTaskId,
  })
  return response.data.data
}

/**
 * Remove a dependency record by its own id.
 */
export async function removeTaskDependency(dependencyId: number): Promise<void> {
  await http.delete(`/tasks/dependencies/${dependencyId}`)
}

/**
 * Fetch personal tasks that can be added as a dependency for `taskId`.
 * The backend should exclude the task itself and any that would form a cycle.
 */
export async function fetchAvailableDependencies(
  taskId: number,
  keyword?: string,
): Promise<Task[]> {
  const response = await http.get<ApiResponse<Task[]>>('/tasks/available-for-dependency', {
    params: { taskId, keyword },
  })
  return response.data.data
}
