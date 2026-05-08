import { http } from './http'
import type { ApiResponse, PageResponse } from '../types/api'
import type {
  Task,
  TaskDependencyResponse,
  TaskPayload,
  TeamTaskPayload,
  TaskStatus,
} from '../types/task'

export type TaskQueryParams = {
  page?: number
  size?: number
  status?: string
  priority?: string
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

export async function fetchTaskDependencies(taskId: number) {
  const response = await http.get<ApiResponse<TaskDependencyResponse>>(`/tasks/${taskId}/dependencies`)
  return response.data.data
}

export async function addTaskDependency(taskId: number, predecessorTaskId: number) {
  const response = await http.post<ApiResponse<TaskDependencyResponse>>(
    `/tasks/${taskId}/dependencies`,
    { predecessorTaskId },
  )
  return response.data.data
}

export async function removeTaskDependency(taskId: number, predecessorTaskId: number) {
  const response = await http.delete<ApiResponse<TaskDependencyResponse>>(
    `/tasks/${taskId}/dependencies/${predecessorTaskId}`,
  )
  return response.data.data
}

export async function fetchTeamTaskDependencies(teamId: number, taskId: number) {
  const response = await http.get<ApiResponse<TaskDependencyResponse>>(
    `/teams/${teamId}/tasks/${taskId}/dependencies`,
  )
  return response.data.data
}

export async function addTeamTaskDependency(teamId: number, taskId: number, predecessorTaskId: number) {
  const response = await http.post<ApiResponse<TaskDependencyResponse>>(
    `/teams/${teamId}/tasks/${taskId}/dependencies`,
    { predecessorTaskId },
  )
  return response.data.data
}

export async function removeTeamTaskDependency(teamId: number, taskId: number, predecessorTaskId: number) {
  const response = await http.delete<ApiResponse<TaskDependencyResponse>>(
    `/teams/${teamId}/tasks/${taskId}/dependencies/${predecessorTaskId}`,
  )
  return response.data.data
}
