# BSD棒垒球数据平台 — 项目文档

> **简体中文** | [繁體中文](PROJECT_README.zh-TW.md)  
> **上线网址**: [www.bsdone.com](https://www.bsdone.com)  
> **文档版本**: 2026-09-09  
> **项目状态**: 线上运行中（阿里云）

---

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 技术架构](#2-技术架构)
- [3. 目录结构](#3-目录结构)
- [4. 后端 (bsball-backend)](#4-后端-bsball-backend)
- [5. 前端 (bsball-frontend)](#5-前端-bsball-frontend)
- [6. 部署包 (bsball_project)](#6-部署包-bsball_project)
- [7. 部署环境](#7-部署环境)
- [8. 实时比赛球场 SVG](#8-实时比赛球场-svg)
- [9. Demo 账号功能](#9-demo-账号功能)
- [10. 自动守护与维护](#10-自动守护与维护)
- [11. 已知问题与解决方案](#11-已知问题与解决方案)

---

## 1. 项目概览

BsBall 是一套**棒球 / 垒球赛事数据管理平台**，支持：

- 赛事实时录入（Live Scoring）
- 球员 / 球队管理
- 比赛数据统计（打击、投球、防守）
- 多租户架构（每支球队 / 联盟独立租户）
- 棒球 / 垒球模式切换（共用一套球场几何）
- 门户首页 + 后台管理
- Demo 账号（只读 + 内存暂存录入）

---

## 2. 技术架构

```
┌─────────────┐       ┌──────────────────┐       ┌─────────────┐
│  Nginx      │──────▶│  Spring Boot     │──────▶│ PostgreSQL  │
│  (前端静态 + │       │  (Java, 8080)    │       │  (数据库)    │
│   gzip_static)│      │                  │       │             │
└──────┬──────┘       └──────────────────┘       └─────────────┘
       │
       ▼
┌─────────────┐
│  Vite 编译   │
│  前端产物    │
│  (assets/)  │
└─────────────┘
```

| 层级 | 技术 | 说明 |
|------|------|------|
| **后端** | Java + Spring Boot 4.1.0 | `com.bsball:bsball-server:1.0.0` |
| **前端** | Vite 6 + Vue 3 + Element Plus | 源码重建中（`bsball-frontend/`，分批次实施）；生产仍为编译产物 |
| **数据库** | PostgreSQL | Flyway migration 管理 schema |
| **反向代理** | Nginx | gzip_static on（优先返回 .gz） |
| **部署** | 阿里云 ECS | systemd 管理 Java 服务 |

---

## 3. 目录结构

```
Bsdone-project-backup/
├── bsball-backend/           # 后端 Java 源码（Spring Boot）
│   ├── src/main/java/com/bsball/
│   │   ├── api/              # REST API 控制器（55 个文件）
│   │   ├── common/           # 通用工具（13 个文件）
│   │   ├── config/           # 配置类（22 个文件）
│   │   ├── core/             # 核心逻辑（22 个文件）
│   │   ├── data/             # 数据初始化（1 个文件）
│   │   ├── exception/        # 异常处理（2 个文件）
│   │   ├── mapper/           # MyBatis Mapper（2 个文件）
│   │   ├── model/            # 数据模型 / Entity（93 个文件）
│   │   │   ├── dto/          # Data Transfer Objects
│   │   │   ├── entity/       # JPA Entity
│   │   │   ├── enums/        # 枚举（比赛状态等）
│   │   │   └── vo/           # View Objects
│   │   ├── mybatis/          # MyBatis 配置（1 个文件）
│   │   ├── repository/       # 数据访问层（45 个文件）
│   │   ├── service/          # 业务逻辑层（72 个文件）
│   │   ├── stats/            # 统计相关（3 个文件）
│   │   ├── util/             # 工具类（1 个文件）
│   │   ├── utils/            # 工具类（9 个文件）
│   │   └── BsBallApplication.java  # 主入口
│   ├── src/main/resources/
│   │   ├── application-prod.yml    # 生产环境配置
│   │   ├── application-dev.yml     # 开发环境配置
│   │   ├── logback-spring.xml      # 日志配置
│   │   ├── mapper/                 # MyBatis XML
│   │   └── db/migration/postgresql/  # Flyway 迁移脚本
│   ├── target/                     # Maven 编译产物
│   └── pom.xml                     # Maven 配置
│
├── bsball-frontend/          # 前端源码工程（Vite + Vue 3 + Element Plus）
│   ├── index.html            # SPA 入口（base=/bs-ball/）
│   ├── src/                  # 源码（router/views/... 随批次建设）
│   ├── scripts/              # 打包脚本（package-webapps.mjs）
│   ├── vite.config.js        # 正式构建配置（npm run dev/build/package）
│   ├── vite.legacy-preview.config.js  # 旧编译版对照预览（npm run preview:legacy，3001）
│   └── node_modules/         # npm 依赖（可重建）
│
├── bsball-source-full/       # 反编译完整源码（JAR 解压）
│   ├── com/bsball/           # 同后端包结构
│   └── org/springframework/  # Spring Framework loader
│
├── bsball_project/           # 完整部署包
│   ├── java-server/
│   │   ├── bsball-server.jar   # 可执行 JAR
│   │   ├── logs/               # 运行期日志
│   │   ├── uploads/            # 上传文件
│   │   └── data/               # 运行期数据
│   └── webapps/
│       ├── bs-ball/            # 前端静态资源
│       │   ├── assets/         # Vite 编译产物（JS/CSS）
│       │   ├── team-logos/     # 球队 Logo 图片
│       │   ├── index.html      # 后台入口
│       │   └── *.svg           # 球场 SVG
│       ├── home/               # 门户首页
│       ├── static/             # 共用静态资源（fonts/logo）
│       ├── index.html          # 根页面
│       └── portal.html         # 门户页面
│
└── bsball_backup.dump        # 数据库备份
```

---

## 4. 后端 (bsball-backend)

### 4.1 Maven 配置

```xml
<groupId>com.bsball</groupId>
<artifactId>bsball-server</artifactId>
<version>1.0.0</version>
<!-- parent: spring-boot-starter-parent 4.1.0 -->
```

### 4.2 包说明

| 包 | 文件数 | 职责 |
|------|--------|------|
| `api` | 55 | REST 控制器（Auth、Game、Player、Team、HitSpray、Portal 等） |
| `service` | 72 | 业务逻辑（比赛、录入、统计、租户管理） |
| `model` | 93 | Entity / DTO / VO / Enum |
| `repository` | 45 | JPA Repository + 自定义查询 |
| `config` | 22 | Spring 配置（安全、缓存、跨域、Flyway） |
| `core` | 22 | 核心机制（权限过滤器、租户解析、Token 管理） |
| `common` | 13 | 通用响应、常量、工具 |
| `utils` | 9 | 字符串、日期、加密等工具 |
| `stats` | 3 | 统计计算 |
| `mapper` | 2 | MyBatis XML Mapper |
| `exception` | 2 | 全局异常处理 |

### 4.3 核心 API

| 路径 | 说明 |
|------|------|
| `POST /bsball-server/auth/login` | 登录（无 /api 前缀） |
| `GET /bsball-server/portal/settings` | 门户设置 |
| `/game/**` | 比赛 CRUD |
| `/hit-spray/**` | 落点数据录入 |
| `/actuator/health` | 健康检查（需鉴权） |

### 4.4 多租户架构

- 租户码（`tenantCode`）由前端 URL 路径提取
- `AuthApi` 对 `demo` 账号特殊处理：直接返回 `tenantId=2`，不受 URL 租户码影响
- Token 按租户隔离存储于 `localStorage` 的 `admin_token::<租户码>`

### 4.5 验证码配置

| 配置键 | 说明 | 默认 |
|--------|------|------|
| `app.auth.captcha.enabled` | 是否启用 | `true` |
| `app.auth.captcha.type` | 类型：input/click/drag/random | `random` |
| `app.auth.captcha.random-types` | 随机模式候选池 | `input,click,drag` |

可通过环境变量 `APP_AUTH_CAPTCHA_TYPE` / `APP_AUTH_CAPTCHA_RANDOM_TYPES` 覆盖。

---

## 5. 前端 (bsball-frontend)

### 5.1 现状（源码重建中，2026-09 起）

原始 `.vue` 源码缺失（历史仓库与上游均无、无 source map），现以 **编译产物为规格** 分批次重建源码工程：

- 工程：`bsball-frontend/`（Vite 6 + Vue 3 + Element Plus + vue-router，`base=/bs-ball/`）
- 批次：B0 工程化 → B1 骨架与契约底座 → B2 认证族 → B3 标准 CRUD 族 → B4 领域族 → B5 LiveGame 家族
- 生产环境在切换前仍为原编译产物（`bsball_project/webapps/`，只读对照基线）

### 5.2 开发与对照

```bash
cd bsball-frontend
npm run dev              # 新工程开发服务器（3000，/bsball-server 代理到本地后端 8080）
npm run preview:legacy   # 旧编译版对照预览（3001）
npm run package          # 构建 + 打包 webapps-dev.tar.gz（部署包约定：顶层含 webapps/）
```

### 5.3 过渡期（生产仍为编译版时）的应急修改方式

在重建完成、生产切换之前，如必须快速修复线上旧版，仍沿用「直接修改编译产物」流程（过渡手段）：

1. `grep` 定位特征字符串
2. Python 脚本 `assert` 精确替换（搭配上下文打印核对）
3. `gzip -9 -c file > file.gz` 重新生成压缩文件（Nginx gzip_static 必需）
4. `curl --compressed` 验证线上
5. 浏览器 `Ctrl+Shift+R` 强制刷新

---

## 6. 部署包 (bsball_project)

### 6.1 java-server

```
java-server/
├── bsball-server.jar    # Spring Boot 可执行 JAR（prod profile）
├── logs/                # bs-ball.log, bs-ball-error.log
├── uploads/             # 用户上传（按 yyyy/MM/dd 目录）
└── data/                # 运行期数据
```

### 6.2 webapps

```
webapps/
├── index.html           # 根页面
├── portal.html          # 门户首页
├── static/              # 共用资源（fonts, logo）
├── home/                # 门户子页面
└── bs-ball/             # 后台 + 实时比赛前端
    ├── assets/          # JS + CSS（Vite 编译）
    ├── team-logos/      # 球队 Logo
    ├── index.html       # 后台 SPA 入口
    └── *.svg            # 球场、头像等 SVG
```

---

## 7. 部署环境

### 7.1 阿里云服务器

| 项目 | 值 |
|------|------|
| IP | `8.138.99.113` |
| OS | Linux |
| 远程 | SSH port 22 |
| Java 服务路径 | `/usr/local/java-server/` |
| 前端静态路径 | `/usr/local/webapps/` |
| 备份路径 | `/usr/local/bsball-login-fix/` |
| Nginx 配置 | `/etc/nginx/nginx.conf` |

### 7.2 systemd 服务

```ini
# java-server.service
[Service]
ExecStart=java -jar bsball-server.jar --spring.profiles.active=prod
WorkingDirectory=/usr/local/java-server
# 开机自启 (enabled)、自动重启 (auto-restart)
# 监听 8080, context-path=/bsball-server
```

**重启命令**: `systemctl restart java-server`（启动约 34 秒）

> **⚠️ 严禁** `nohup java -jar &` 手动启动——会与 systemd 进程冲突，导致 8080 端口崩溃循环。

### 7.3 Nginx 配置

```nginx
root /usr/local/webapps/;
gzip_static on;    # 优先返回 .gz 预压缩文件
```

**关键**：改任何静态文件（JS/CSS/SVG）后**必须**同步重新生成 `.gz`：

```bash
gzip -9 -c file.js > file.js.gz
```

否则 Nginx 继续发旧的 `.gz`，改了不生效。

---

## 8. 实时比赛球场 SVG

### 8.1 概览

球场 SVG 渲染于 `LiveGame-Bjdd5Cir.js`，核心参数：

| 参数 | 值 |
|------|------|
| viewBox | `-300 -150 1080 780` |
| 本垒坐标 | `(240, 555)` |
| 草地外野弧 | `M 240 590 L -179.5 165.8 A 572.2 572.2 0 0 1 659.5 165.8 Z` |
| 白线（右） | 起点 y545 → 终点 (659.5, 165.8) |
| 白线（左） | 起点 y545 → 终点 (-179.5, 165.8) |

### 8.2 棒球 / 垒球模式

**共用一套几何**。`gameMode` 为运行时变量（`"BASEBALL"` / `"SOFTBALL"`），同一文件切换：

- 球场几何（防守队员坐标、circle、白线、草地）**只有一套**，改一次即两模式生效
- 仅 `balls` / `strikes` 起始数分模式（垒球起手 1 球）

### 8.3 防守队员坐标 (Zo)

| 位置 | x | y |
|------|---|---|
| 1B | 375 | 405 |
| 2B | 320 | 330 |
| SS | 160 | 330 |
| 3B | 115 | 405 |
| P | 240 | 425 |
| **C** | **240** | **600** |
| LF | 75 | 230 |
| CF | 240 | 150 |
| RF | 405 | 230 |

### 8.4 本垒区元素

| 元素 | 参数 |
|------|------|
| 内野土区 circle | `cx=240, cy=559, r=38.9`（下缘 597.9） |
| 投手丘 circle | `cx=240, cy=448, r=16` |
| 打击区白框（右） | `x=252, y=545, w=10, h=22` |
| 打击区白框（左） | `x=218, y=545, w=10, h=22` |
| 本垒板 polygon | `233,548 247,548 247,555 240,562 233,555` |

### 8.5 击球按钮 (field-quick-pitch)

CSS 绝对定位覆盖（规则 9）：

```css
.field-quick-pitch {
  bottom: 20px !important;
  left: 59% !important;
  transform: scale(.8) !important;
  transform-origin: left bottom !important;
}
```

防守队员字体已放大 ×1.3（defender-text 31.2px、dual 23.4px、sub 16.9px）。

---

## 9. Demo 账号功能

### 9.1 登录

- **账号**: `demo` / **密码**: `demo`
- 自动绑定 `tenantId=2`（demo 租户），不受 URL tenantCode 影响

### 9.2 只读保护

采用**后端拦截写入请求**方案：

- `ApiPermissionFilter` 拦截 demo 用户的所有写入 API
- 前端操作看起来正常（不报错），但数据不写入数据库
- 刷新页面后改动消失

### 9.3 实时录入（内存暂存）

允许 demo 账号创建比赛和录入落点，但**只存内存**：

| 组件 | 说明 |
|------|------|
| `DemoGameStore` | `ConcurrentHashMap<Long, DemoGame>` 内存缓存 |
| `GameService` | demo 用户创建比赛生成负数 ID（`-demo-{timestamp}`） |
| `HitSprayService` | 落点数据存内存 |

> **注意**：演示数据随服务重启而消失。

### 9.4 DevTools 反调试守卫

配置键 `portalDevtoolsGuard`（存 `sys_config` 表）：

- 检测 DevTools 开启时显示全屏遮罩锁死
- demo / 非 admin 用户易误报
- 解决：设置 `portalDevtoolsGuard=false`（需 `systemctl restart java-server` 清缓存）

---

## 10. 自动守护与维护

### 10.1 门户登录修复守护

```ini
# bsball-login-guard.timer
OnCalendar=*-*-* 03:30    # 每日 03:30 执行
Persistent=true           # 错过开机补跑
```

脚本 `/usr/local/bsball-login-fix/bsball_login_guard.sh`：

- 检查 assets 是否含登录修复标记
- 缺失时自动调用 `apply_login_fix.sh` 重打补丁
- 日志写入 `/var/log/bsball-login-guard.log`（>1MB 自动截断）

### 10.2 健康检查

```bash
# 用 demo 登录判断服务存活（actuator/health 需鉴权返回 403）
curl -s -o /dev/null -w "%{http_code}" \
  -X POST https://www.bsdone.com/bsball-server/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo"}'
# 期望: 200
```

### 10.3 日志查看

```bash
# Spring Boot 日志
tail -f /usr/local/java-server/logs/bs-ball.log

# systemd journal
journalctl -u java-server -f

# 登录守护日志
tail -20 /var/log/bsball-login-guard.log
```

---

## 11. 已知问题与解决方案

### 11.1 前端修改不生效

**原因**：Nginx `gzip_static on` 优先返回 `.gz` 文件，改了 `.js` 没更新 `.gz`。

**解决**：

```bash
gzip -9 -c file.js > file.js.gz
# 验证：zcat file.js.gz | grep '特征字符串'
```

### 11.2 浏览器看到旧版

**原因**：hash 文件名不变，浏览器 / Safari 缓存未更新。

**解决**：`Ctrl+Shift+R`（Mac `Cmd+Shift+R`）强制刷新；Safari 建议移除特定域名数据。

### 11.3 登录后停在门户进不去后台

**原因**：前端 token 按租户隔离（`admin_token::<租户码>`），门户登录 redirect 回门户根。

**解决**：已打补丁——redirect 为门户根时改写为 `/${tenant}/admin/dashboard`。补丁由每日 timer 自动守护。

### 11.4 Demo 前台按钮点不了

**原因**：`portalDevtoolsGuard` 反调试守卫误报，全屏 `not-allowed` 遮罩锁死。

**解决**：

```sql
-- sys_config 表（demo=租户 2）
UPDATE sys_config SET config_value='false'
WHERE tenant_id=2 AND config_key='portalDevtoolsGuard';
```

然后 `systemctl restart java-server`（清 `@Cacheable` 缓存）。

### 11.5 Spring Boot 重启进程冲突

**原因**：手动 `nohup java -jar &` 导致多个进程争抢 8080 端口。

**解决**：

```bash
# 查看进程
ps -ef | grep java-server
# 只保留 systemd 管理的单一进程
systemctl restart java-server
```

> **排查时 `grep` 要含 `java-server`**，不能只筛 `ball` / `bs` 关键字。

### 11.6 basebal-field.svg 改了没效果

**原因**：`baseball-field.svg` 是孤立文件，前端无引用。球场 SVG 在 `LiveGame-Bjdd5Cir.js` 内联渲染。

**解决**：改 `LiveGame-Bjdd5Cir.js` 中的 SVG 参数，不要动 `baseball-field.svg`。

---

## 附录：快速参考

### 常用命令

```bash
# 重启服务
systemctl restart java-server

# 查看服务状态
systemctl status java-server

# 查看日志
journalctl -u java-server --since "10 min ago"

# 手动触发登录守护
systemctl start bsball-login-guard.service

# 校验线上 JS 内容
curl --compressed https://www.bsdone.com/bs-ball/assets/LiveGame-Bjdd5Cir.js | grep '特征'

# 重新生成 gzip
gzip -9 -c file.js > file.js.gz
```

### 验证码配置

```yaml
# application-prod.yml 或环境变量
app:
  auth:
    captcha:
      enabled: true
      type: random          # input / click / drag / random
      random-types: input,click,drag
```
