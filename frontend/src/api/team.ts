import { http } from './http'
import type { ApiResponse } from '../types/api'
import type {
  TeamCreateRequest,
  TeamDetail,
  TeamMember,
  TeamMemberAddRequest,
  TeamRoleUpdateRequest,
  TeamSummary,
} from '../types/team'

export async function fetchMyTeams() {
  const response = await http.get<ApiResponse<TeamSummary[]>>('/teams')
  return response.data.data
}

export async function fetchTeamDetail(teamId: number) {
  const response = await http.get<ApiResponse<TeamDetail>>(`/teams/${teamId}`)
  return response.data.data
}

export async function createTeam(payload: TeamCreateRequest) {
  const response = await http.post<ApiResponse<TeamSummary>>('/teams', payload)
  return response.data.data
}

export async function addTeamMember(teamId: number, payload: TeamMemberAddRequest) {
  const response = await http.post<ApiResponse<TeamMember>>(
    `/teams/${teamId}/members`,
    payload,
  )
  return response.data.data
}

export async function updateTeamMemberRole(
  teamId: number,
  userId: number,
  payload: TeamRoleUpdateRequest,
) {
  const response = await http.put<ApiResponse<TeamMember>>(
    `/teams/${teamId}/members/${userId}/role`,
    payload,
  )
  return response.data.data
}