import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// 正式前端工程构建配置（B0 工程化）
// - base 与线上部署路径一致：/bs-ball/（部署到 webapps/bs-ball/）
// - 开发服务器（npm run dev，端口 3000）代理 /bsball-server → 本地后端 8080，与线上 nginx 行为一致
// - 旧编译版对照预览见 vite.legacy-preview.config.js（npm run preview:legacy，端口 3001）
export default defineConfig({
  base: '/bs-ball/',
  plugins: [vue()],
  build: {
    outDir: 'dist',
    emptyOutDir: true
  },
  server: {
    port: 3000,
    open: false,
    proxy: {
      '/bsball-server': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
