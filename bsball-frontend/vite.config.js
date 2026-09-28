import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';
import fs from 'node:fs';
import path from 'node:path';

// 发布版本注入（Issue #123/#113）：版本优先环境变量 APP_VERSION（部署流水线传入），否则 package.json；
// 构建时间优先环境变量 APP_BUILD_TIME（与 version.json 保持一致），否则取当前时刻。
// 源码中以 __APP_VERSION__ / __APP_BUILD_TIME__ 引用（构建时静态替换）。
const pkg = JSON.parse(fs.readFileSync(path.join(path.dirname(fileURLToPath(import.meta.url)), 'package.json'), 'utf8'));
const appVersion = (process.env.APP_VERSION || pkg.version || '1.0.0').trim();
const appBuildTime = (process.env.APP_BUILD_TIME || new Date().toISOString()).trim();

// 正式前端工程构建配置（B0 工程化）
// - base 与线上部署路径一致：/bs-ball/（部署到 webapps/bs-ball/）
// - 开发服务器（npm run dev，端口 3000）代理 /bsball-server → 本地后端 8080，与线上 nginx 行为一致
// - 旧编译版对照预览见 vite.legacy-preview.config.js（npm run preview:legacy，端口 3001）
export default defineConfig({
  base: '/bs-ball/',
  define: {
    __APP_VERSION__: JSON.stringify(appVersion),
    __APP_BUILD_TIME__: JSON.stringify(appBuildTime)
  },
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
