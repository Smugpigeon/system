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
    <section className="auth-card">
      <p className="auth-card__eyebrow">{eyebrow}</p>
      <h2 className="auth-card__title">{title}</h2>
      <p className="auth-card__description">{description}</p>
      {children}
      {footer ? <div className="auth-card__footer">{footer}</div> : null}
    </section>
  )
}
