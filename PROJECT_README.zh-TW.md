# BSD棒壘球數據平台 — 專案文件

> [简体中文](PROJECT_README.md) | **繁體中文**  
> **上線網址**: [www.bsdone.com](https://www.bsdone.com)  
> **文件版本**: 2026-09-09  
> **專案狀態**: 線上運行中（阿里雲）

---

## 目錄

- [1. 專案概覽](#1-專案概覽)
- [2. 技術架構](#2-技術架構)
- [3. 目錄結構](#3-目錄結構)
- [4. 後端 (bsball-backend)](#4-後端-bsball-backend)
- [5. 前端 (bsball-frontend)](#5-前端-bsball-frontend)
- [6. 部署包 (bsball_project)](#6-部署包-bsball_project)
- [7. 部署環境](#7-部署環境)
- [8. 即時比賽球場 SVG](#8-即時比賽球場-svg)
- [9. Demo 帳號功能](#9-demo-帳號功能)
- [10. 自動守護與維護](#10-自動守護與維護)
- [11. 已知問題與解決方案](#11-已知問題與解決方案)

---

## 1. 專案概覽

BsBall 是一套**棒球 / 壘球賽事數據管理平台**，支援：

- 賽事即時錄入（Live Scoring）
- 球員 / 球隊管理
- 比賽數據統計（打擊、投球、守備）
- 多租戶架構（每支隊伍 / 聯盟獨立租戶）
- 棒球 / 壘球模式切換（共用一套球場幾何）
- 門戶首頁 + 後台管理
- Demo 帳號（唯讀 + 記憶體暫存錄入）

---

## 2. 技術架構

```
┌─────────────┐       ┌──────────────────┐       ┌─────────────┐
│  Nginx      │──────▶│  Spring Boot     │──────▶│ PostgreSQL  │
│  (前端靜態 + │       │  (Java, 8080)    │       │  (資料庫)    │
│   gzip_static)│      │                  │       │             │
└──────┬──────┘       └──────────────────┘       └─────────────┘
       │
       ▼
┌─────────────┐
│  Vite 編譯   │
│  前端產物    │
│  (assets/)  │
└─────────────┘
```

| 層級 | 技術 | 說明 |
|------|------|------|
| **後端** | Java + Spring Boot 4.1.0 | `com.bsball:bsball-server:1.0.0` |
| **前端** | Vite 6.0 編譯 | 編譯後產物，無原始 .vue 碼 |
| **資料庫** | PostgreSQL | Flyway migration 管理 schema |
| **反向代理** | Nginx | gzip_static on（優先返回 .gz） |
| **部署** | 阿里雲 ECS | systemd 管理 Java 服務 |

---

## 3. 目錄結構

```
Bsdone-project-backup/
├── bsball-backend/           # 後端 Java 源碼（Spring Boot）
│   ├── src/main/java/com/bsball/
│   │   ├── api/              # REST API 控制器（55 檔）
│   │   ├── common/           # 通用工具（13 檔）
│   │   ├── config/           # 配置類（22 檔）
│   │   ├── core/             # 核心邏輯（22 檔）
│   │   ├── data/             # 資料初始化（1 檔）
│   │   ├── exception/        # 例外處理（2 檔）
│   │   ├── mapper/           # MyBatis Mapper（2 檔）
│   │   ├── model/            # 資料模型 / Entity（93 檔）
│   │   │   ├── dto/          # Data Transfer Objects
│   │   │   ├── entity/       # JPA Entity
│   │   │   ├── enums/        # 枚舉（比賽狀態等）
│   │   │   └── vo/           # View Objects
│   │   ├── mybatis/          # MyBatis 配置（1 檔）
│   │   ├── repository/       # 資料存取層（45 檔）
│   │   ├── service/          # 業務邏輯層（72 檔）
│   │   ├── stats/            # 統計相關（3 檔）
│   │   ├── util/             # 工具類（1 檔）
│   │   ├── utils/            # 工具類（9 檔）
│   │   └── BsBallApplication.java  # 主入口
│   ├── src/main/resources/
│   │   ├── application-prod.yml    # 生產環境配置
│   │   ├── application-dev.yml     # 開發環境配置
│   │   ├── logback-spring.xml      # 日誌配置
│   │   ├── mapper/                 # MyBatis XML
│   │   └── db/migration/postgresql/  # Flyway 遷移腳本
│   ├── target/                     # Maven 編譯產物
│   └── pom.xml                     # Maven 配置
│
├── bsball-frontend/          # 前端工程
│   ├── package.json          # 依賴定義
│   ├── vite.config.js        # Vite 配置
│   └── node_modules/         # npm 依賴（可重建）
│
├── bsball-source-full/       # 反編譯完整源碼（JAR 解壓）
│   ├── com/bsball/           # 同後端套件結構
│   └── org/springframework/  # Spring Framework loader
│
├── bsball_project/           # 完整部署包
│   ├── java-server/
│   │   ├── bsball-server.jar   # 可執行 JAR
│   │   ├── logs/               # 執行期日誌
│   │   ├── uploads/            # 上傳檔案
│   │   └── data/               # 執行期資料
│   └── webapps/
│       ├── bs-ball/            # 前端靜態資源
│       │   ├── assets/         # Vite 編譯產物（JS/CSS）
│       │   ├── team-logos/     # 球隊 Logo 圖片
│       │   ├── index.html      # 後台入口
│       │   └── *.svg           # 球場 SVG
│       ├── home/               # 門戶首頁
│       ├── static/             # 共用靜態（fonts/logo）
│       ├── index.html          # 根頁面
│       └── portal.html         # 門戶頁面
│
└── bsball_backup.dump        # 資料庫備份
```

---

## 4. 後端 (bsball-backend)

### 4.1 Maven 配置

```xml
<groupId>com.bsball</groupId>
<artifactId>bsball-server</artifactId>
<version>1.0.0</version>
<!-- parent: spring-boot-starter-parent 4.1.0 -->
```

### 4.2 套件說明

| 套件 | 檔案數 | 職責 |
|------|--------|------|
| `api` | 55 | REST 控制器（Auth、Game、Player、Team、HitSpray、Portal 等） |
| `service` | 72 | 業務邏輯（比賽、錄入、統計、租戶管理） |
| `model` | 93 | Entity / DTO / VO / Enum |
| `repository` | 45 | JPA Repository + 自訂查詢 |
| `config` | 22 | Spring 配置（安全、快取、跨域、Flyway） |
| `core` | 22 | 核心機制（權限過濾器、租戶解析、Token 管理） |
| `common` | 13 | 通用回應、常數、工具 |
| `utils` | 9 | 字串、日期、加密等工具 |
| `stats` | 3 | 統計計算 |
| `mapper` | 2 | MyBatis XML Mapper |
| `exception` | 2 | 全域例外處理 |

### 4.3 核心 API

| 路徑 | 說明 |
|------|------|
| `POST /bsball-server/auth/login` | 登入（無 /api 前綴） |
| `GET /bsball-server/portal/settings` | 門戶設定 |
| `/game/**` | 比賽 CRUD |
| `/hit-spray/**` | 落點數據錄入 |
| `/actuator/health` | 健康檢查（需鑑權） |

### 4.4 多租戶架構

- 租戶碼（`tenantCode`）由前端 URL 路徑提取
- `AuthApi` 對 `demo` 帳號特殊處理：直接返回 `tenantId=2`，不受 URL 租戶碼影響
- Token 按租戶隔離存於 `localStorage` 的 `admin_token::<租戶碼>`

### 4.5 驗證碼配置

| 配置鍵 | 說明 | 預設 |
|--------|------|------|
| `app.auth.captcha.enabled` | 是否啟用 | `true` |
| `app.auth.captcha.type` | 類型：input/click/drag/random | `random` |
| `app.auth.captcha.random-types` | 隨機模式候選池 | `input,click,drag` |

可透過環境變數 `APP_AUTH_CAPTCHA_TYPE` / `APP_AUTH_CAPTCHA_RANDOM_TYPES` 覆蓋。

---

## 5. 前端 (bsball-frontend)

### 5.1 現狀

前端為 **Vite 6.0 編譯後的靜態產物**，原始 `.vue` 源碼不在備份中。

- `package.json` 僅含 `vite` 開發依賴
- 編譯產物位於 `bsball_project/webapps/bs-ball/assets/`
- 主要 JS 入口：`LiveGame-Bjdd5Cir.js`（即時比賽頁面）
- 主要 CSS：`LiveGame-Opn_XCmh.css`

### 5.2 前端修改方式

因無源碼，所有修改直接在 **minified 編譯產物** 上操作：

1. `grep` 定位特徵字串
2. Python 腳本 `assert` 精確替換（搭配上下文印出核對）
3. `gzip -9 -c file > file.gz` 重新生成壓縮檔（Nginx gzip_static 必要）
4. `curl --compressed` 驗證線上
5. 瀏覽器 `Ctrl+Shift+R` 強制刷新

---

## 6. 部署包 (bsball_project)

### 6.1 java-server

```
java-server/
├── bsball-server.jar    # Spring Boot 可執行 JAR（prod profile）
├── logs/                # bs-ball.log, bs-ball-error.log
├── uploads/             # 用戶上傳（按 yyyy/MM/dd 目錄）
└── data/                # 執行期資料
```

### 6.2 webapps

```
webapps/
├── index.html           # 根頁面
├── portal.html          # 門戶首頁
├── static/              # 共用資源（fonts, logo）
├── home/                # 門戶子頁面
└── bs-ball/             # 後台 + 即時比賽前端
    ├── assets/          # JS + CSS（Vite 編譯）
    ├── team-logos/      # 球隊 Logo
    ├── index.html       # 後台 SPA 入口
    └── *.svg            # 球場、頭像等 SVG
```

---

## 7. 部署環境

### 7.1 阿里雲伺服器

| 項目 | 值 |
|------|------|
| IP | `8.138.99.113` |
| OS | Linux |
| 遠程 | SSH port 22 |
| Java 服務路徑 | `/usr/local/java-server/` |
| 前端靜態路徑 | `/usr/local/webapps/` |
| 備份路徑 | `/usr/local/bsball-login-fix/` |
| Nginx 配置 | `/etc/nginx/nginx.conf` |

### 7.2 systemd 服務

```ini
# java-server.service
[Service]
ExecStart=java -jar bsball-server.jar --spring.profiles.active=prod
WorkingDirectory=/usr/local/java-server
# 開機自啟 (enabled)、自動重啟 (auto-restart)
# 監聽 8080, context-path=/bsball-server
```

**重啟命令**: `systemctl restart java-server`（啟動約 34 秒）

> **⚠️ 嚴禁** `nohup java -jar &` 手動啟動——會與 systemd 進程衝突，導致 8080 端口崩潰循環。

### 7.3 Nginx 配置

```nginx
root /usr/local/webapps/;
gzip_static on;    # 優先返回 .gz 預壓縮檔
```

**關鍵**：改任何靜態檔（JS/CSS/SVG）後**必須**同步重生成 `.gz`：

```bash
gzip -9 -c file.js > file.js.gz
```

否則 Nginx 繼續發舊 `.gz`，改了不生效。

---

## 8. 即時比賽球場 SVG

### 8.1 概覽

球場 SVG 渲染於 `LiveGame-Bjdd5Cir.js`，核心參數：

| 參數 | 值 |
|------|------|
| viewBox | `-300 -150 1080 780` |
| 本壘座標 | `(240, 555)` |
| 草地外野弧 | `M 240 590 L -179.5 165.8 A 572.2 572.2 0 0 1 659.5 165.8 Z` |
| 白線（右） | 起點 y545 → 終點 (659.5, 165.8) |
| 白線（左） | 起點 y545 → 終點 (-179.5, 165.8) |

### 8.2 棒球 / 壘球模式

**共用一套幾何**。`gameMode` 為運行時變量（`"BASEBALL"` / `"SOFTBALL"`），同一檔案切換：

- 球場幾何（守備員座標、circle、白線、草地）**只有一套**，改一次即兩模式生效
- 僅 `balls` / `strikes` 起始數分模式（壘球起手 1 球）

### 8.3 守備員座標 (Zo)

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

### 8.4 本壘區元素

| 元素 | 參數 |
|------|------|
| 內野土區 circle | `cx=240, cy=559, r=38.9`（下緣 597.9） |
| 投手丘 circle | `cx=240, cy=448, r=16` |
| 打擊區白框（右） | `x=252, y=545, w=10, h=22` |
| 打擊區白框（左） | `x=218, y=545, w=10, h=22` |
| 本壘板 polygon | `233,548 247,548 247,555 240,562 233,555` |

### 8.5 擊球按鈕 (field-quick-pitch)

CSS 絕對定位覆蓋（規則 9）：

```css
.field-quick-pitch {
  bottom: 20px !important;
  left: 59% !important;
  transform: scale(.8) !important;
  transform-origin: left bottom !important;
}
```

守備員字體已放大 ×1.3（defender-text 31.2px、dual 23.4px、sub 16.9px）。

---

## 9. Demo 帳號功能

### 9.1 登入

- **帳號**: `demo` / **密碼**: `demo`
- 自動綁定 `tenantId=2`（demo 租戶），不受 URL tenantCode 影響

### 9.2 唯讀保護

採用**後端攔截寫入請求**方案：

- `ApiPermissionFilter` 攔截 demo 用戶的所有寫入 API
- 前端操作看起來正常（不報錯），但資料不寫入資料庫
- 刷新頁面後改動消失

### 9.3 實時錄入（記憶體暫存）

允許 demo 帳號創建比賽和錄入落點，但**只存記憶體**：

| 元件 | 說明 |
|------|------|
| `DemoGameStore` | `ConcurrentHashMap<Long, DemoGame>` 記憶體緩存 |
| `GameService` | demo 用戶創建比賽生成負數 ID（`-demo-{timestamp}`） |
| `HitSprayService` | 落點數據存記憶體 |

> **注意**：演示數據隨服務重啟而消失。

### 9.4 DevTools 反調試守衛

配置鍵 `portalDevtoolsGuard`（存 `sys_config` 表）：

- 偵測 DevTools 開啟時顯示全屏遮罩鎖死
- demo / 非 admin 用戶易誤報
- 解法：設 `portalDevtoolsGuard=false`（需 `systemctl restart java-server` 清快取）

---

## 10. 自動守護與維護

### 10.1 門戶登入修復守護

```ini
# bsball-login-guard.timer
OnCalendar=*-*-* 03:30    # 每日 03:30 執行
Persistent=true           # 錯過開機補跑
```

腳本 `/usr/local/bsball-login-fix/bsball_login_guard.sh`：

- 檢查 assets 是否含登入修復標記
- 缺失時自動調用 `apply_login_fix.sh` 重打補丁
- 日誌寫 `/var/log/bsball-login-guard.log`（>1MB 自動截尾）

### 10.2 健康檢查

```bash
# 用 demo 登入判斷服務存活（actuator/health 需鑑權返回 403）
curl -s -o /dev/null -w "%{http_code}" \
  -X POST https://www.bsdone.com/bsball-server/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo"}'
# 期望: 200
```

### 10.3 日誌查看

```bash
# Spring Boot 日誌
tail -f /usr/local/java-server/logs/bs-ball.log

# systemd journal
journalctl -u java-server -f

# 登入守護日誌
tail -20 /var/log/bsball-login-guard.log
```

---

## 11. 已知問題與解決方案

### 11.1 前端修改不生效

**原因**：Nginx `gzip_static on` 優先返回 `.gz` 檔，改了 `.js` 沒更新 `.gz`。

**解法**：

```bash
gzip -9 -c file.js > file.js.gz
# 驗證：zcat file.js.gz | grep '特徵字串'
```

### 11.2 瀏覽器看到舊版

**原因**：hash 檔名不變，瀏覽器 / Safari 快取未更新。

**解法**：`Ctrl+Shift+R`（Mac `Cmd+Shift+R`）強制刷新；Safari 建議移除特定域名數據。

### 11.3 登入後停在門戶進不去後台

**原因**：前端 token 按租戶隔離（`admin_token::<租戶碼>`），門戶登入 redirect 回門戶根。

**解法**：已打補丁——redirect 為門戶根時改寫為 `/${tenant}/admin/dashboard`。補丁由每日 timer 自動守護。

### 11.4 Demo 前台按鈕點不了

**原因**：`portalDevtoolsGuard` 反調試守衛誤報，全屏 `not-allowed` 遮罩鎖死。

**解法**：

```sql
-- sys_config 表（demo=租戶 2）
UPDATE sys_config SET config_value='false'
WHERE tenant_id=2 AND config_key='portalDevtoolsGuard';
```

然後 `systemctl restart java-server`（清 `@Cacheable` 快取）。

### 11.5 Spring Boot 重啟進程衝突

**原因**：手動 `nohup java -jar &` 導致多個進程爭 8080 端口。

**解法**：

```bash
# 查看進程
ps -ef | grep java-server
# 只保留 systemd 管理的單一進程
systemctl restart java-server
```

> **排查時 `grep` 要含 `java-server`**，不能只篩 `ball` / `bs` 關鍵字。

### 11.6 basebal-field.svg 改了沒效

**原因**：`baseball-field.svg` 是孤立檔案，前端無引用。球場 SVG 在 `LiveGame-Bjdd5Cir.js` 內聯渲染。

**解法**：改 `LiveGame-Bjdd5Cir.js` 中的 SVG 參數，不要動 `baseball-field.svg`。

---

## 附錄：快速參考

### 常用命令

```bash
# 重啟服務
systemctl restart java-server

# 查看服務狀態
systemctl status java-server

# 查看日誌
journalctl -u java-server --since "10 min ago"

# 手動觸發登入守護
systemctl start bsball-login-guard.service

# 驗證線上 JS 內容
curl --compressed https://www.bsdone.com/bs-ball/assets/LiveGame-Bjdd5Cir.js | grep '特徵'

# 重新生成 gzip
gzip -9 -c file.js > file.js.gz
```

### 驗證碼配置

```yaml
# application-prod.yml 或環境變數
app:
  auth:
    captcha:
      enabled: true
      type: random          # input / click / drag / random
      random-types: input,click,drag
```
