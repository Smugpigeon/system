import { http } from './http'
import type { ApiResponse, PageResponse } from '../types/api'
import type { Task, TaskPayload, TeamTaskPayload, TaskStatus } from '../types/task'

export type TaskQueryParams = {
  page?: number      
  size?: number      
  status?: string    
  priority?: string  
  sortBy?: string  
  keyword?: string  
}

export async function fetchTasks(params?: TaskQueryParams) {
  const response = await http.get<ApiResponse<PageResponse<Task>>>('/tasks', {
    params
  })
  return response.data.data  // 返回 PageResponse<Task>
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

export type TeamTaskQueryParams = {
  page?: number
  size?: number
  status?: string
  priority?: string
  keyword?: string
  sortBy?: string
}

export async function fetchTeamTasks(
  teamId: number,
  params?: TeamTaskQueryParams,
) {
  const response = await http.get<ApiResponse<PageResponse<Task>>>(
    `/teams/${teamId}/tasks`,
    { params },
  )
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

export async function updateTeamTask(taskId: number, payload: TeamTaskPayload) {
  const response = await http.put<ApiResponse<Task>>(`/tasks/${taskId}`, payload)
  return response.data.data
}

export async function updateTeamTaskStatus(taskId: number, payload: TaskPayload) {
  const response = await http.put<ApiResponse<Task>>(`/tasks/${taskId}`, payload)
  return response.data.data
}

export async function deleteTeamTask(taskId: number) {
  await http.delete(`/tasks/${taskId}`)
}

export async function assignTeamTask(teamId: number, taskId: number, assigneeId: number) {
  const response = await http.put<ApiResponse<Task>>(
    `/teams/${teamId}/tasks/${taskId}/assign`,
    { assigneeId }
  )
  return response.data.data
}