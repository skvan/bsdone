import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

// 正式前端工程构建配置（B0 工程化）
// - base 与线上部署路径一致：/bs-ball/（部署到 webapps/bs-ball/）
// - 开发服务器（npm run dev，端口 3000）代理 /bsball-server → 本地后端 8080，与线上 nginx 行为一致
// - 旧编译版对照预览见 vite.legacy-preview.config.js（npm run preview:legacy，端口 3001）
export default defineConfig({
  base: '/bs-ball/',
  plugins: [vue()],
  resolve: {
    alias: {
      // wangeditor 的 ESM 版对 vue 使用默认导入（仅 CJS interop 可用），构建/开发统一改指其 CJS 版
      '@wangeditor/editor-for-vue': fileURLToPath(new URL('./node_modules/@wangeditor/editor-for-vue/dist/index.js', import.meta.url))
    }
  },
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
