import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { TeamDetail, TeamMember, TeamRole, TeamSummary } from '../types/team'

export async function fetchMyTeams() {
  const response = await http.get<ApiResponse<TeamSummary[]>>('/teams')
  return response.data.data
}

export async function createTeam(name: string) {
  const response = await http.post<ApiResponse<TeamSummary>>('/teams', { name })
  return response.data.data
}

export async function fetchTeamDetail(teamId: number) {
  const response = await http.get<ApiResponse<TeamDetail>>(`/teams/${teamId}`)
  return response.data.data
}

export async function addTeamMember(teamId: number, username: string) {
  const response = await http.post<ApiResponse<TeamMember>>(`/teams/${teamId}/members`, { username })
  return response.data.data
}

export async function updateTeamMemberRole(teamId: number, userId: number, role: TeamRole) {
  const response = await http.put<ApiResponse<TeamMember>>(`/teams/${teamId}/members/${userId}/role`, { role })
  return response.data.data
}
