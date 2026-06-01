import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { MotionConfig } from 'motion/react'
import { LocaleProvider } from '@douyinfe/semi-ui'
import zh_CN from '@douyinfe/semi-ui/lib/es/locale/source/zh_CN'
import '@douyinfe/semi-ui/dist/css/semi.min.css'
import App from './App'
import { AuthProvider } from './context/AuthContext'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    {/* reducedMotion="user" 让 Motion 对开启“减少动态效果”的用户自动去掉位移/缩放，仅保留淡入淡出 */}
    <MotionConfig reducedMotion="user">
      <LocaleProvider locale={zh_CN}>
        <BrowserRouter>
          <AuthProvider>
            <App />
          </AuthProvider>
        </BrowserRouter>
      </LocaleProvider>
    </MotionConfig>
  </React.StrictMode>,
)
