export type TeamRole = 'OWNER' | 'ADMIN' | 'MEMBER'

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

export const TEAM_ROLE_LABELS: Record<TeamRole, string> = {
  OWNER: 'Owner',
  ADMIN: 'Admin',
  MEMBER: 'Member',
}
