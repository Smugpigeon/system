export type TeamRole = 'OWNER' | 'ADMIN' | 'MEMBER'
export type TeamStatus = 'ACTIVE' | 'DISSOLVED'
export type TeamMembershipStatus = 'ACTIVE' | 'LEFT' | 'REMOVED'

export type TeamSummary = {
  id: number
  name: string
  status: TeamStatus
  currentUserRole: TeamRole
  memberCount: number
  teamTaskCount: number
}

export type TeamMember = {
  userId: number
  username: string
  role: TeamRole
  status: TeamMembershipStatus
}

export type TeamDetail = {
  id: number
  name: string
  status: TeamStatus
  currentUserRole: TeamRole
  members: TeamMember[]
}

export const TEAM_ROLE_LABELS: Record<TeamRole, string> = {
  OWNER: 'Owner',
  ADMIN: 'Admin',
  MEMBER: 'Member',
}

export const TEAM_STATUS_LABELS: Record<TeamStatus, string> = {
  ACTIVE: '正常',
  DISSOLVED: '已解散',
}

export const TEAM_MEMBERSHIP_STATUS_LABELS: Record<TeamMembershipStatus, string> = {
  ACTIVE: '有效',
  LEFT: '已离开',
  REMOVED: '已移除',
}
