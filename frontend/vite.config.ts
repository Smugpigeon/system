import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // Semi 的整包 CSS 放在 dist/css 下，但其 package.json 的 exports 字段未白名单该路径，
      // Vite 8 的严格解析会拒绝。这里显式 alias 到真实文件，保留 main.tsx 中的标准引用写法。
      '@douyinfe/semi-ui/dist/css/semi.min.css': fileURLToPath(
        new URL('./node_modules/@douyinfe/semi-ui/dist/css/semi.min.css', import.meta.url),
      ),
    },
  },
})
