import { defineConfig } from 'vite'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// 编译好的前端静态文件目录（从阿里云服务器下载）
const webappsRoot = path.resolve(__dirname, '../bsball_project/webapps')

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject',
  '.mp4': 'video/mp4',
  '.webp': 'image/webp',
  '.txt': 'text/plain; charset=utf-8',
  '.gz': 'application/gzip'
}

/**
 * 自定义插件：提供编译后的静态文件（SPA 模式）
 * - /bs-ball/*  -> webapps/bs-ball/*
 * - /static/*   -> webapps/static/*
 * - /home/*     -> webapps/home/*
 * - 非文件请求回退到对应 index.html（SPA 路由）
 */
function serveCompiledStatic() {
  return {
    name: 'serve-compiled-static',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = (req.url || '').split('?')[0]

        // 只处理前端路径
        if (!url.startsWith('/bs-ball') && !url.startsWith('/static') && !url.startsWith('/home') && !url.startsWith('/portal') && url !== '/' && url !== '/index.html') {
          return next()
        }

        // 根路径 -> 入口页（webapps/portal.html，QUEST 生成）
        // 入口页里的「系统介绍」按钮链接到 /index.html 官网营销首页
        // 「进入数据库」按钮链接到 /bs-ball/bs-ball 数据系统
        // /portal -> 入口页（保留兼容）
        if (url === '/' || url === '/portal') {
          const portalPage = path.join(webappsRoot, 'portal.html')
          res.writeHead(200, { 'Content-Type': MIME_TYPES['.html'] })
          fs.createReadStream(portalPage).pipe(res)
          return
        }
        if (url === '/index.html') {
          const homePage = path.join(webappsRoot, 'index.html')
          res.writeHead(200, { 'Content-Type': MIME_TYPES['.html'] })
          fs.createReadStream(homePage).pipe(res)
          return
        }

        // 计算文件路径
        let filePath = path.join(webappsRoot, decodeURIComponent(url))

        // 安全检查：防止路径穿越
        if (!filePath.startsWith(webappsRoot)) {
          res.writeHead(403)
          res.end('Forbidden')
          return
        }

        // 如果是目录或不存在，尝试 SPA 回退
        let isDirectory = false
        try {
          const stat = fs.statSync(filePath)
          isDirectory = stat.isDirectory()
        } catch {
          // 文件不存在
        }

        if (isDirectory || !fs.existsSync(filePath)) {
          // SPA 回退：返回对应应用的 index.html
          const appPrefix = url.startsWith('/bs-ball') ? 'bs-ball' : url.startsWith('/home') ? 'home' : ''
          const indexFile = appPrefix ? path.join(webappsRoot, appPrefix, 'index.html') : path.join(webappsRoot, 'index.html')
          if (fs.existsSync(indexFile)) {
            res.writeHead(200, { 'Content-Type': MIME_TYPES['.html'] })
            fs.createReadStream(indexFile).pipe(res)
            return
          }
          res.writeHead(404)
          res.end('Not Found')
          return
        }

        // 提供静态文件
        const ext = path.extname(filePath).toLowerCase()
        const contentType = MIME_TYPES[ext] || 'application/octet-stream'
        res.writeHead(200, {
          'Content-Type': contentType,
          'Cache-Control': 'no-cache'
        })
        fs.createReadStream(filePath).pipe(res)
      })
    }
  }
}

export default defineConfig({
  server: {
    port: 3000,
    open: false,
    proxy: {
      // 后端 API 代理到 Spring Boot
      '/bsball-server': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  plugins: [serveCompiledStatic()]
})
