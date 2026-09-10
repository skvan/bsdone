# AGENTS.md

## 專案概述

這是一個棒球賽事管理與數據展示網站，功能類似 GameChanger App（賽事記錄、數據統計、團隊管理等）。

- **主要技術**：Spring Boot 4.1.0 + Java 17（後端）、Vue/React（前端）
- **套件管理器**：Maven 3.6.3（後端）、pnpm（前端）
- **資料庫**：PostgreSQL
- **部署環境**：阿里雲服務器

## 專案結構

```
C:\Users\Clay\Qoder\
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

## 代碼規範

- 使用 TypeScript（前端）；避免使用 `any`，除非有明確理由
- 不要留下註解掉的程式碼或除錯輸出
- 新增公開函式或重要邏輯時，補上必要文件
- 不要手動修改自動產生的檔案

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
# 設置環境變量（Windows PowerShell）
$env:JAVA_HOME = "C:\Users\Clay\java\jdk-17"
$env:Path = "C:\Users\Clay\java\jdk-17\bin;C:\Users\Clay\Downloads\apache-maven-3.6.3-bin\apache-maven-3.6.3\bin;$env:Path"

# 後端編譯
cd C:\Users\Clay\Qoder\bsball-backend
mvn compile

# 後端運行
mvn spring-boot:run

# 後端打包
mvn package -DskipTests
```

## 注意事項

- 阿里雲服務器上的檔案為生產版本，**不對阿里雲的檔案做任何變動**
- 本地開發環境為 Windows，JDK 17 位於 `C:\Users\Clay\java\jdk-17`
- Maven 3.6.3 位於 `C:\Users\Clay\Downloads\apache-maven-3.6.3-bin\apache-maven-3.6.3`
