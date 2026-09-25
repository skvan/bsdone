# BSD棒垒球数据平台

> **简体中文** | [繁體中文](README.zh-TW.md)  
> 线上环境：<https://www.bsdone.com>

赛事实时录入、球员与球队管理、比赛数据统计、多租户架构的棒球 / 垒球数据管理平台。

---

## 团队协作入口

| 你想做什么 | 去哪里 |
|-----------|--------|
| 了解流程规则（新成员先看） | [项目管理流程文档](docs/PROJECT_MANAGEMENT.md) |
| 查看任务进度 | [项目看板](https://github.com/users/skvan/projects/1) |
| 提需求 / 报 Bug / 建任务 | [新建 Issue](https://github.com/skvan/bsdone/issues/new/choose) |
| 讨论某个任务 | 在对应 Issue 下评论 |

> 非技术成员只需要用到 Issues 和看板，无需安装任何工具、无需写代码。

---

## 开发者入口

- 技术架构 / 部署配置 / 服务器信息：[简体版](PROJECT_README.md) / [繁體版](PROJECT_README.zh-TW.md)
- 深度技术参考（API 接口 / 数据模型）：[docs/tech](docs/tech/README.md)
- AI 协作规范：[AGENTS.md](AGENTS.md)

### 分支策略（版本驱动）

| 分支 | 环境 | 说明 |
|------|------|------|
| `main` | 生产环境 | 受保护，只接受版本合并 |
| `v{版本号}/dev` | 测试环境 | 版本功能集成，测试通过后合并回 main |
| `feature/*` | 本地开发 | 从对应版本分支拉出 |

所有代码变更必须通过 PR 合并，**不允许直接推送到 `main` 或 `v1.1/dev`**。
