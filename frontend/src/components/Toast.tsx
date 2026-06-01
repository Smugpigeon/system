import { useEffect } from 'react'
import { motion } from 'motion/react'
import { dur, ease } from '../lib/motion'

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
    <motion.div
      className={`toast toast--${type}`}
      initial={{ opacity: 0, x: 24, scale: 0.98 }}
      animate={{ opacity: 1, x: 0, scale: 1 }}
      transition={{ duration: dur.enter, ease: ease.out }}
    >
      <span>{message}</span>
      <button type="button" onClick={onClose}>
        关闭
      </button>
    </motion.div>
  )
}
