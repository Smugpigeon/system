import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { Task, TaskPayload } from '../types/task'

export async function fetchTasks() {
  const response = await http.get<ApiResponse<Task[]>>('/tasks')
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
