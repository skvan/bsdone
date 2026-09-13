# AGENTS.md

## 项目概述

这是一个棒球赛事管理与数据展示网站，功能类似 GameChanger App（赛事记录、数据统计、团队管理等）。

- **主要技术**：Spring Boot 4.1.0 + Java 17（后端）、Vue/React（前端）
- **包管理器**：Maven 3.6.3（后端）、pnpm（前端）
- **数据库**：PostgreSQL
- **部署环境**：阿里云服务器

## 项目结构

```
bsdone/                      # 项目根目录
├── bsball-backend/          # Java 后端源码
│   ├── src/main/java/com/bsball/
│   │   ├── api/             # REST API 控制器
│   │   ├── service/         # 业务逻辑层
│   │   ├── repository/      # 数据访问层
│   │   ├── model/           # 实体与 DTO
│   │   ├── config/          # 配置类
│   │   └── core/            # 核心组件
│   ├── src/test/java/com/bsball/        # 单元测试（与被测类同包）
│   └── pom.xml
├── bsball_project/          # 从服务器下载的原始项目文件
└── bsball_backup.sql        # 数据库备份文件
```

## 开始工作前

1. 修改前先阅读相关代码与测试
2. 保留既有架构和命名惯例
3. 不要修改与当前任务无关的文件
4. 如果需求有歧义，先从阿里云的代码库里找信息，尽可能地以阿里云上的版本与架构为准，不做多余的改动和自创
5. 优先沿用现有组件、函数与工具
6. 函数保持单一职责，避免不必要的抽象

## 分支策略（重要！）

本项目采用**版本驱动**的分支策略，分支与环境一一对应：

| 分支 | 环境 | 说明 |
|------|------|------|
| `main` | 生产环境 | 阿里云线上版本，受保护 |
| `v{版本号}/dev` | 测试环境 | 每个版本从 main fork，集成所有人的功能 |
| `feature/*` | 本地开发 | 每个人从对应 dev 分支 fork 的功能分支 |

### 工作流

1. 技术负责人从 `main` 创建版本分支（如 `v1.0/dev`）
2. 开发者从版本分支拉功能分支：`git checkout -b feature/我的功能`
3. 开发完成后，创建 Pull Request 合并回对应的 `v{版本号}/dev`
4. 测试通过后，由技术负责人将 `v{版本号}/dev` 合并回 `main` 并打 Tag 发版

### Git 规范

- **每个功能必须开 feature 分支**，不要在 main 或 dev 分支上直接开发
- commit message 必须以类型前缀开头：`feat:`, `fix:`, `style:`, `refactor:`, `docs:`, `test:`, `chore:`
- 不要提交 `node_modules/`、`*.class`、`*.jar`、`.env` 等文件
- 不要修改 `application-prod.yml` 中的敏感配置
- 不要删除或重构现有文件，除非任务明确要求
- 开始工作前先 `git pull origin <对应dev分支>` 确保最新代码
- 不要使用 `git push --force`

## 编码规范（重要！所有 AI 生成代码必须遵守）

### 命名约定

| 类型 | 风格 | 示例 |
|------|------|------|
| Java 变量 / 方法 | camelCase | `getPlayerStats`, `teamName` |
| Java 类名 | PascalCase | `GameService`, `PlayerApi` |
| Java 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| 数据库表名 / 字段 | snake_case | `player_stats`, `created_at` |
| 前端变量 / 函数 | camelCase | `fetchGameData`, `isActive` |
| CSS 类名 | kebab-case | `game-card`, `player-avatar` |
| API 路由 | kebab-case | `/hit-spray/record`, `/sys-user/list` |
| Flyway 迁移脚本 | `V{版本}__{描述}.sql` | `V12__add_player_stats.sql` |

### 文件组织

- 新增 API 控制器 → `api/` 目录，命名 `XxxApi.java`
- 新增业务逻辑 → `service/` 目录，命名 `XxxService.java`
- 新增数据访问 → `repository/` 目录，命名 `XxxRepository.java`
- 新增实体类 → `model/entity/` 目录
- 新增 DTO → `model/dto/` 目录
- 数据库变更 → 必须通过 Flyway migration 脚本，放 `resources/db/migration/postgresql/`
- **禁止在已有目录外创建新顶层目录**，除非明确讨论过

### 代码风格

- 每个方法不超过 50 行，超过就拆分成更小的私有方法
- 不要重复写相同逻辑，抽成工具方法放 `utils/`
- API 返回值统一用 `Result<T>` 包装（成功 `Result.ok(...)`, 失败 `Result.fail(...)`）
- 列表接口统一返回 `PageResult<T>`，支持 `page`/`pageSize`/`keyword` 分页参数
- 需要登录的接口在方法入口读取 `CurrentUserHolder.get()`，为空返回 401
- CRUD 路由遵循 `/sys/{resource}/list|all|create|update/{id}|delete/{id}` 命名
- 使用构造器注入（`@RequiredArgsConstructor`），不用 `@Autowired` 字段注入
- 不要留下注释掉的代码或 `System.out.println` 调试输出
- 新增公开方法时补上 Javadoc 注释

### 禁止事项

- ❌ 不要自己发明新的架构模式，沿用现有代码风格
- ❌ 不要直接修改已编译的前端产物（`bsball_project/webapps/assets/`）
- ❌ 不要在代码中硬编码密码、密钥、服务器地址
- ❌ 不要引入新的第三方依赖，除非明确讨论过
- ❌ 不要创建与现有目录结构不一致的新目录
- ❌ 不要修改 `.gitignore`、`pom.xml`、`application*.yml` 等配置文件，除非任务明确要求
- ❌ 不要使用 `any`（前端 TypeScript），除非有明确理由并加注说明
- ❌ 不要手动修改自动生成的文件

## 单元测试（重要！所有 AI 生成代码必须遵守）

### 触发条件

改动 `service/`、`repository/`、`utils/`、`core/` 等含逻辑的代码时，**必须在同一个 PR 内新增或更新对应的单元测试**，不允许留到「下次再说」。

### 技术选型与位置

- 框架：JUnit 5 + Mockito（`spring-boot-starter-test` 已自带，不要引入新依赖）
- 测试类型：**纯单元测试**——不启动 Spring 容器、不连接 PostgreSQL/Redis、不访问网络、不依赖真实时间
- 有依赖的类一律用 Mockito mock（`@ExtendWith(MockitoExtension.class)` + `@Mock`），不要用 `@SpringBootTest`
- 位置与命名：`bsball-backend/src/test/java/com/bsball/<与被测类相同的包>/<被测类名>Test.java`
- 参考样例：`bsball-backend/src/test/java/com/bsball/service/DataScopeServiceTest.java`

### 内容要求

- 覆盖正常流程与重要边界：`null`/空值、空集合、异常路径、权限分支、边界数值
- 修正 bug 时，**先写一个能复现该 bug 的失败测试**，再改代码
- 测试方法用 `@DisplayName` 以中文说明意图

### 禁止事项

- ❌ 为了让测试通过而删除测试、`@Disabled` 跳过或注释掉已有测试
- ❌ 空断言（如 `assertTrue(true)`）、只验证「不抛异常」而不断言结果
- ❌ 断言依赖真实时间、随机数、网络、外部服务或本地环境差异
- ❌ 为了让测试通过而放宽断言标准或降低验证强度

### 交付要求

- 交付前**必须实际运行**，并给出结果：
  ```bash
  cd bsball-backend
  mvn test
  ```
- 测试未通过不允许交付；确实无法运行时，必须在交付说明中写明原因
- 前端（`bsball-frontend`）目前无源码与测试基建，**暂不纳入单元测试要求**

## 文档同步（重要！改代码必须同步改文档）

### 触发条件

以下任一改动，必须在**同一个 PR 内**同步更新对应文档：

| 代码改动 | 必须同步更新的文档 |
|----------|--------------------|
| 新增/修改接口（`api/`、路由、请求/响应字段） | `docs/tech/API接口参考/` 对应文件 |
| 修改实体、表结构、Flyway 迁移 | `docs/tech/数据模型设计/` 对应文件 |
| 架构、模块、目录结构、技术栈变化 | `PROJECT_README.md` + `PROJECT_README.zh-TW.md` |
| 开发流程、分支策略、协作方式变化 | `docs/PROJECT_MANAGEMENT.md` |
| 首页入口、导航变化 | `README.md` + `README.zh-TW.md` |

### 文档语言

- 简体文档（`README.md`、`PROJECT_README.md`、`AGENTS.md`、`docs/**`）：简体中文 + 大陆用语习惯（资料→信息、函式→函数、檔案→文件）
- 繁体文档（`*.zh-TW.md`）：保留繁体字与**台湾用语**，不要替换成大陆用语
- 成对文件（`README`、`PROJECT_README`）必须**两版同时更新**，并保留顶部 `**简体中文** | [繁體中文](...)` 切换链接

### 要求

- 文档更新与代码改动必须在同一个 PR 内完成
- `docs/tech/` 标注「以代码为准」：只要改动影响了其中已记载的内容，就必须同步修正
- 确实不需要更新文档时，必须在 PR 描述中说明原因，不能默默跳过
- ❌ 禁止写空泛、不准确或与代码不符的文档来充数

## 依赖与安全

- 未经要求，不要新增生产环境依赖
- **不要把 API key、密码、token 或其他机密写入代码**
- 不要修改 `.env` 或提交机密资料
- 不要执行破坏性数据库操作
- 不要为了让测试通过而移除安全检查或降低验证标准

## Git 与变更范围

- 保留用户现有且与任务无关的修改
- 不要使用会丢失变更的 Git 指令
- 每次修改应集中处理当前任务
- 不要自行提交、推送或创建 Pull Request，除非明确要求

## 完成交付时

简要说明：
1. 修改了什么
2. 影响哪些主要文件
3. 执行了哪些验证
4. 是否还有已知限制或后续工作

## 常用指令

```bash
# 后端编译（在项目根目录执行）
cd bsball-backend
mvn compile

# 后端单元测试（提交前必须通过）
mvn test

# 后端运行
mvn spring-boot:run

# 后端打包
mvn package -DskipTests
```

## 给非技术成员的照抄提示词

用 AI 改代码时，把下面这段**一起发给 AI**，它就会按本文件的规则写出测试并更新文档：

```text
请先阅读项目根目录的 AGENTS.md，并严格遵循其中的「单元测试」与「文档同步」规则：
1. 为本次改动新增/更新单元测试（JUnit 5 + Mockito，纯单元测试，不启动 Spring、不连数据库），
   参考 bsball-backend/src/test/java/com/bsball/service/DataScopeServiceTest.java 的写法；
2. 按「文档同步」清单更新对应文档；README / PROJECT_README 要同时更新 .zh-TW.md 繁体版，
   繁体版保留台湾用语；
3. 完成后运行 mvn test 并贴出结果；如果失败，修到通过或说明原因；
4. 不要修改与本次任务无关的文件。
```

## 注意事项

- 阿里云服务器上的文件为生产版本，**不对阿里云的文件做任何变动**
- 本地需自行安装 JDK 17 与 Maven 3.6+，确保 `java`、`mvn` 已加入 PATH
- 每位开发者使用自己的本地环境路径，**不要将个人路径写入仓库任何文件**
