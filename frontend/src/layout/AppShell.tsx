import type { PropsWithChildren, ReactNode } from 'react'

type AppShellProps = PropsWithChildren<{
  title: string
  description: string
  aside: ReactNode
}>

export function AppShell({
  title,
  description,
  aside,
  children,
}: AppShellProps) {
  return (
    <div className="page">
      <div className="app-shell">
        <aside className="app-shell__aside">
          <div className="aside-stack fade-in">
            <div className="brand-mark">
              <span className="brand-dot" />
              task foundry
            </div>
            <p className="eyebrow">Collaborative Task Management</p>
            <h1 className="brand-title">{title}</h1>
            <p className="brand-copy">{description}</p>
            {aside}
          </div>
        </aside>

        <main className="app-shell__main">
          <div className="main-stack fade-in">{children}</div>
        </main>
      </div>
    </div>
  )
}
