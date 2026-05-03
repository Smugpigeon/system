import { useEffect } from 'react'

type ToastProps = {
  message: string
  type: 'success' | 'error' | 'info'
  onClose: () => void
}

export function Toast({ message, type, onClose }: ToastProps) {
  useEffect(() => {
    const timer = window.setTimeout(onClose, 2600)
    return () => window.clearTimeout(timer)
  }, [onClose])

  return (
    <div className={`toast toast--${type}`}>
      <span>{message}</span>
      <button type="button" onClick={onClose}>
        关闭
      </button>
    </div>
  )
}
