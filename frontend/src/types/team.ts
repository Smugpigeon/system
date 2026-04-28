export type TeamRole = 'OWNER' | 'ADMIN' | 'MEMBER'

export const TEAM_ROLE_LABELS: Record<TeamRole, string> = {
  OWNER: '拥有者',
  ADMIN: '管理员',
  MEMBER: '成员',
}

export type TeamSummary = {
  id: number
  name: string
  currentUserRole: TeamRole
  memberCount: number
  teamTaskCount: number
}

export type TeamMember = {
  userId: number
  username: string
  role: TeamRole
}

export type TeamDetail = {
  id: number
  name: string
  currentUserRole: TeamRole
  members: TeamMember[]
}

export type TeamCreateRequest = {
  name: string
}

export type TeamMemberAddRequest = {
  username: string
  role: TeamRole
}

export type TeamRoleUpdateRequest = {
  role: TeamRole
}