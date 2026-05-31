import type { PropsWithChildren, ReactNode } from 'react'

type AppShellProps = PropsWithChildren<{
  title?: string
  description?: string
  aside?: ReactNode
}>

export function AppShell({ children }: AppShellProps) {
  return (
    <div className="page">
      <div className="app-shell">
        <main className="app-shell__main">
          <div className="main-stack">{children}</div>
        </main>
      </div>
    </div>
  )
}
