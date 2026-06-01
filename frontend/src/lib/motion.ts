/**
 * 全局动效设计令牌（参考 Material 3 缓动曲线 + Linear/Vercel 的克制风格）。
 *
 * 原则：
 * - 只动 transform / opacity（GPU 合成，不触发重排）。
 * - 进场用 ease-out（减速到位，显得响应快），退场用 ease-in（加速离开，别挡路）。
 * - 退场比进场更快。
 * - 时长保持在 100~280ms，超过就像加载而不是反馈。
 */

export const ease = {
  /** 标准曲线：屏内移动 / 颜色变化 */
  standard: [0.4, 0, 0.2, 1],
  /** 减速（ease-out）：元素进场 */
  out: [0, 0, 0.2, 1],
  /** 加速（ease-in）：元素退场 */
  in: [0.4, 0, 1, 1],
  /** 强调曲线：弹窗 / 抽屉进场 */
  emphasized: [0.2, 0, 0, 1],
} as const

export const dur = {
  micro: 0.12,
  enter: 0.2,
  exit: 0.15,
  move: 0.24,
  overlay: 0.28,
} as const

/** 触感元素（弹窗、数字滚动）用的弹簧，干脆、几乎不过冲 */
export const snappy = { type: 'spring', stiffness: 400, damping: 30 } as const

/** 列表交错进场的每项间隔 */
export const STAGGER = 0.04
