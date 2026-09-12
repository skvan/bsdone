# AGENTS.md

## 專案概述

這是一個棒球賽事管理與數據展示網站，功能類似 GameChanger App（賽事記錄、數據統計、團隊管理等）。

- **主要技術**：Spring Boot 4.1.0 + Java 17（後端）、Vue/React（前端）
- **套件管理器**：Maven 3.6.3（後端）、pnpm（前端）
- **資料庫**：PostgreSQL
- **部署環境**：阿里雲服務器

## 專案結構

```
bsdone/                      # 專案根目錄
├── bsball-backend/          # Java 後端源碼
│   ├── src/main/java/com/bsball/
│   │   ├── api/             # REST API 控制器
│   │   ├── service/         # 業務邏輯層
│   │   ├── repository/      # 數據訪問層
│   │   ├── model/           # 實體與 DTO
│   │   ├── config/          # 配置類
│   │   └── core/            # 核心組件
│   └── pom.xml
├── bsball_project/          # 從服務器下載的原始項目文件
└── bsball_backup.sql        # 數據庫備份文件
```

## 開始工作前

1. 修改前先閱讀相關程式碼與測試
2. 保留既有架構和命名慣例
3. 不要修改與目前任務無關的檔案
4. 如果需求有歧義，先從阿里雲的代碼庫裡找資訊，盡可能地以阿里雲上的版本與架構為準，不做多餘的改動和自創
5. 優先沿用現有元件、函式與工具
6. 函式保持單一職責，避免不必要的抽象

## 分支策略（重要！）

本專案採用**版本驅動**的分支策略，分支與環境一一對應：

| 分支 | 環境 | 說明 |
|------|------|------|
| `main` | 生產環境 | 阿里雲線上版本，受保護 |
| `v{版本號}/dev` | 測試環境 | 每個版本從 main fork，集成所有人的功能 |
| `feature/*` | 本地開發 | 每個人從對應 dev 分支 fork 的功能分支 |

### 工作流

1. 技術負責人從 `main` 創建版本分支（如 `v1.0/dev`）
2. 開發者從版本分支拉功能分支：`git checkout -b feature/我的功能`
3. 開發完成後，創建 Pull Request 合併回對應的 `v{版本號}/dev`
4. 測試通過後，由技術負責人將 `v{版本號}/dev` 合併回 `main` 並打 Tag 發版

### Git 規範

- **每個功能必須開 feature 分支**，不要在 main 或 dev 分支上直接開發
- commit message 必須以類型前綴開頭：`feat:`, `fix:`, `style:`, `refactor:`, `chore:`
- 不要提交 `node_modules/`、`*.class`、`*.jar`、`.env` 等檔案
- 不要修改 `application-prod.yml` 中的敏感配置
- 不要刪除或重構現有檔案，除非任務明確要求
- 開始工作前先 `git pull origin <對應dev分支>` 確保最新代碼
- 不要使用 `git push --force`

## 編碼規範（重要！所有 AI 生成代碼必須遵守）

### 命名約定

| 類型 | 風格 | 示例 |
|------|------|------|
| Java 變量 / 方法 | camelCase | `getPlayerStats`, `teamName` |
| Java 類名 | PascalCase | `GameService`, `PlayerApi` |
| Java 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| 數據庫表名 / 欄位 | snake_case | `player_stats`, `created_at` |
| 前端變量 / 函數 | camelCase | `fetchGameData`, `isActive` |
| CSS 類名 | kebab-case | `game-card`, `player-avatar` |
| API 路由 | kebab-case | `/hit-spray/record`, `/sys-user/list` |
| Flyway 遷移腳本 | `V{版本}__{描述}.sql` | `V12__add_player_stats.sql` |

### 檔案組織

- 新增 API 控制器 → `api/` 目錄，命名 `XxxApi.java`
- 新增業務邏輯 → `service/` 目錄，命名 `XxxService.java`
- 新增資料存取 → `repository/` 目錄，命名 `XxxRepository.java`
- 新增實體類 → `model/entity/` 目錄
- 新增 DTO → `model/dto/` 目錄
- 數據庫變更 → 必須通過 Flyway migration 腳本，放 `resources/db/migration/postgresql/`
- **禁止在已有目錄外創建新頂層目錄**，除非明確討論過

### 代碼風格

- 每個方法不超過 50 行，超過就拆分成更小的私有方法
- 不要重複寫相同邏輯，抽成工具方法放 `utils/`
- API 返回值統一用 `Result<T>` 包裝（成功 `Result.ok(...)`, 失敗 `Result.fail(...)`）
- 列表接口統一返回 `PageResult<T>`，支持 `page`/`pageSize`/`keyword` 分頁參數
- 需要登入的接口在方法入口讀取 `CurrentUserHolder.get()`，為空返回 401
- CRUD 路由遵循 `/sys/{resource}/list|all|create|update/{id}|delete/{id}` 命名
- 使用建構器注入（`@RequiredArgsConstructor`），不用 `@Autowired` 字段注入
- 不要留下註解掉的程式碼或 `System.out.println` 除錯輸出
- 新增公開方法時補上 Javadoc 註釋

### 禁止事項

- ❌ 不要自己發明新的架構模式，沿用現有代碼風格
- ❌ 不要直接修改已編譯的前端產物（`bsball_project/webapps/assets/`）
- ❌ 不要在代碼中硬編碼密碼、密鑰、服務器地址
- ❌ 不要引入新的第三方依賴，除非明確討論過
- ❌ 不要創建與現有目錄結構不一致的新目錄
- ❌ 不要修改 `.gitignore`、`pom.xml`、`application*.yml` 等配置文件，除非任務明確要求
- ❌ 不要使用 `any`（前端 TypeScript），除非有明確理由並加註說明
- ❌ 不要手動修改自動產生的檔案

## 測試與驗證

- 修正 bug 時，盡可能新增能重現問題的測試
- 新功能應涵蓋正常流程與重要邊界情況
- 完成修改後，至少執行：
  ```bash
  # 後端
  mvn compile
  mvn test
  
  # 前端
  pnpm lint
  pnpm typecheck
  pnpm test
  ```
- 若無法執行某項檢查，交付時說明原因

## 依賴與安全

- 未經要求，不要新增正式環境依賴
- **不要把 API key、密碼、token 或其他機密寫入程式碼**
- 不要修改 `.env` 或提交機密資料
- 不要執行破壞性資料庫操作
- 不要為了讓測試通過而移除安全檢查或降低驗證標準

## Git 與變更範圍

- 保留使用者現有且與任務無關的修改
- 不要使用會丟失變更的 Git 指令
- 每次修改應集中處理目前任務
- 不要自行提交、推送或建立 Pull Request，除非明確要求

## 完成交付時

簡要說明：
1. 修改了什麼
2. 影響哪些主要檔案
3. 執行了哪些驗證
4. 是否還有已知限制或後續工作

## 常用指令

```bash
# 後端編譯（在專案根目錄執行）
cd bsball-backend
mvn compile

# 後端運行
mvn spring-boot:run

# 後端打包
mvn package -DskipTests
```

## 注意事項

- 阿里雲服務器上的檔案為生產版本，**不對阿里雲的檔案做任何變動**
- 本地需自行安裝 JDK 17 與 Maven 3.6+，確保 `java`、`mvn` 已加入 PATH
- 每位開發者使用自己的本地環境路徑，**不要將個人路徑寫入倉庫任何檔案**
