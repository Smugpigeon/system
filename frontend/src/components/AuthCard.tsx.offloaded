import type { PropsWithChildren, ReactNode } from 'react'

type AuthCardProps = PropsWithChildren<{
  eyebrow: string
  title: string
  description: string
  footer?: ReactNode
}>

export function AuthCard({
  eyebrow,
  title,
  description,
  footer,
  children,
}: AuthCardProps) {
  return (
    <section className="auth-card fade-in">
      <header className="auth-card__header">
        <p className="eyebrow">{eyebrow}</p>
        <h2>{title}</h2>
        <p>{description}</p>
      </header>
      {children}
      {footer}
    </section>
  )
}
