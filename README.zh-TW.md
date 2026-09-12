# BSD棒壘球數據平台

> [简体中文](README.md) | **繁體中文**  
> 線上環境：<https://www.bsdone.com>

賽事實時錄入、球員與球隊管理、比賽數據統計、多租戶架構的棒球 / 壘球數據管理平台。

---

## 團隊協作入口

| 你想做什麼 | 去哪裡 |
|-----------|--------|
| 了解流程規則（新成員先看） | [專案管理流程文件](docs/PROJECT_MANAGEMENT.md) |
| 查看任務進度 | [專案看板](https://github.com/users/skvan/projects/1) |
| 提需求 / 報 Bug / 建任務 | [新建 Issue](https://github.com/skvan/bsdone/issues/new/choose) |
| 討論某個任務 | 在對應 Issue 下評論 |

> 非技術成員只需要用到 Issues 和看板，無需安裝任何工具、無需寫代碼。

---

## 開發者入口

- 技術架構 / 部署配置 / 伺服器資訊：[繁體版](PROJECT_README.zh-TW.md) / [简体版](PROJECT_README.md)
- 深度技術參考（API 介面 / 資料模型，簡體）：[docs/tech](docs/tech/README.md)
- AI 協作規範：[AGENTS.md](AGENTS.md)

### 分支策略（版本驅動）

| 分支 | 環境 | 說明 |
|------|------|------|
| `main` | 生產環境 | 受保護，只接受版本合併 |
| `v{版本號}/dev` | 測試環境 | 版本功能整合，測試通過後合併回 main |
| `feature/*` | 本地開發 | 從對應版本分支拉出 |

所有代碼變更必須通過 PR 合併，**不允許直接推送到 `main` 或 `v1.0/dev`**。
