import { useEffect, useRef, type PropsWithChildren, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

type DialogProps = PropsWithChildren<{
  open: boolean
  onClose: () => void
  title?: string
  description?: string
  footer?: ReactNode
  /** 点击遮罩或按 ESC 是否可关闭，默认 true。需要强制用户做出选择时设为 false。 */
  dismissible?: boolean
}>

export function Dialog({
  open,
  onClose,
  title,
  description,
  footer,
  dismissible = true,
  children,
}: DialogProps) {
  const contentRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && dismissible) {
        onClose()
      }
    }
    document.addEventListener('keydown', handleKey)
    document.body.style.overflow = 'hidden'
    contentRef.current?.focus()
    return () => {
      document.removeEventListener('keydown', handleKey)
      document.body.style.overflow = ''
    }
  }, [open, dismissible, onClose])

  if (!open) return null

  return createPortal(
    <div
      className="dialog-overlay"
      onClick={() => dismissible && onClose()}
      role="presentation"
    >
      <div
        ref={contentRef}
        className="dialog-content"
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? 'dialog-title' : undefined}
        tabIndex={-1}
        onClick={(event) => event.stopPropagation()}
      >
        {title && (
          <header className="dialog-header">
            <h2 id="dialog-title">{title}</h2>
            {description && <p className="dialog-description">{description}</p>}
          </header>
        )}
        <div className="dialog-body">{children}</div>
        {footer && <footer className="dialog-footer">{footer}</footer>}
      </div>
    </div>,
    document.body,
  )
}
