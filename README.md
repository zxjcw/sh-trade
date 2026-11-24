# 拾光集 · 校园二手交易平台

拾光集是面向校内学生的二手交易平台：学生完成学籍认证后，可以发布、浏览、求购、置换和购买二手物品。平台用三个机制解决二手交易的信任问题：

- **物品履历**：物品编号终身不变，建档、成交、置换、退货由系统写入履历，用户不可修改；
- **资金托管**：买家付款进入平台托管池，确认收货后才放款给卖家；
- **信用与评价分离**：信用记录履约行为，评价只影响展示，不参与信用计算。

> 这份 README 面向参与本项目的开发者。业务流程见 [docs/01-业务流程说明.md](docs/01-业务流程说明.md)，表结构与业务对照见 [docs/02-数据存储清单与业务对照.md](docs/02-数据存储清单与业务对照.md)，前后端接口与分工见 [docs/03-接口文档.md](docs/03-接口文档.md)，数据库表结构、索引与 Flyway 方案见 [docs/04-数据库开发文档.md](docs/04-数据库开发文档.md)，服务器部署与运维见 [docs/05-部署文档.md](docs/05-部署文档.md)。课程报告材料与业务流程图保存在仓库外的 `文档/` 目录。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.3（Java 17）、Spring Security + JWT、MyBatis-Plus、Flyway |
| 数据库 | MySQL 8.x（utf8mb4） |
| 接口文档 | springdoc-openapi（Swagger UI） |
| 前端 | Vue 3 + Vite + TypeScript、Pinia、Vue Router、Element Plus、Axios |
| 工程 | Maven 单模块后端；npm + Vite 前端；Docker Compose 起 MySQL |

## 业务与模块

角色：学生（买家与卖家为同一账号）、运营、学校管理员。

| 编号 | 业务 | 后端模块 |
|---|---|---|
| F1 | 注册与登录 | `modules/auth` |
| F2 | 学籍认证 | `modules/user`（VerificationService） |
| F3 | 物品发布与履历建档 | `modules/item` |
| F4 | 浏览、搜索与筛选 | `modules/item` |
| F5 | 下单与担保支付 | `modules/trade` |
| F6 | 履约交付与交易完成 | `modules/trade` |
| F7 | 评价 | `modules/trade` |
| F8 | 退款与纠纷仲裁 | `modules/trade`（售后）+ `modules/ops`（仲裁） |
| F9 | 求购与悬赏 | `modules/bounty` |
| F10 | 物品接力（再次上架） | `modules/item` |
| F11 | 三方置换 | `modules/exchange` |
| F12 | 内容审核与仲裁处理 | `modules/ops` |
| F13 | 运营报表 | `modules/ops`（report） |
| F14 | 校方管理 | `modules/admin` |

支撑模块：`user`（用户 / 信用 / 钱包）、`catalog`（学籍 / 分类 / 自提点）、`payment`（托管 / 流水 / 平台账户）、`notification`（通知）、`scheduler`（超时任务）。

## 仓库结构

```
.
├─ backend/                  Spring Boot 后端
│  └─ src/main/
│     ├─ java/com/shiguangji/
│     │  ├─ common/          统一响应、异常、安全、配置、文件存储、工具
│     │  ├─ modules/         按业务域分包，模块内为 controller/service/mapper/entity/dto/enums
│     │  └─ scheduler/       定时任务（只做调度，业务逻辑调用各模块 service）
│     └─ resources/
│        ├─ application.yml
│        ├─ application-dev.yml
│        ├─ db/migration/    Flyway 脚本，V{n}__{描述}.sql
│        └─ mapper/          复杂 SQL 的 XML（按需）
├─ frontend/                 Vue 3 前端
│  └─ src/
│     ├─ api/                按后端模块拆分接口封装
│     ├─ router/             路由与守卫
│     ├─ stores/             Pinia（auth、notification）
│     ├─ layouts/            StudentLayout、ConsoleLayout
│     ├─ views/              按业务域分目录；ops/ 为运营端，admin/ 为管理端
│     ├─ components/         通用组件
│     ├─ composables/        useCountdown、useRole 等
│     └─ styles/             全局样式
├─ docs/                     业务流程与数据设计文档
├─ scripts/                  辅助脚本
├─ docker-compose.yml        本地 MySQL
└─ README.md
```

## 快速开始

### 环境要求

- JDK 17
- Maven 3.9+
- Node.js 20+（npm）
- MySQL 8.x，或 Docker Desktop

### 1. 启动数据库

```bash
docker compose up -d mysql
```

默认库 `shiguangji`，账号 `root / root`，端口 3306。没有 Docker 时手动建库（只建空库，表和数据由 Flyway 建）：

```sql
CREATE DATABASE shiguangji
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

### 2. 启动后端

```bash
cd backend
# 本机 MySQL 的 root 密码通过环境变量传入，不要写进仓库
DB_PASSWORD=<你的密码> mvn spring-boot:run
```

Windows PowerShell：

```powershell
$env:DB_PASSWORD = '<你的密码>'
mvn spring-boot:run
```

- 服务地址：http://localhost:8080
- Swagger UI：http://localhost:8080/swagger-ui.html

Flyway 启动时按顺序执行：`db/migration/V1__schema.sql`（21 张表）、`V2__seed.sql`（平台账户 / 分类 / 自提点）、`db/demo/V3__demo_seed.sql`（仅 dev 环境加载的演示数据）。演示账号的密码占位 `{demo}` 会由 dev 环境下的 `DemoDataInitializer` 用 BCrypt 回填为 `123456`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开 http://localhost:5173，开发服务器已把 `/api` 代理到 8080。前端默认启用 mock（`frontend/.env.development` 中 `VITE_USE_MOCK=true`），不启动后端也能完整浏览。要接真实后端，**不用改任何前端文件**，用环境变量覆盖即可：

```bash
# PowerShell
$env:VITE_USE_MOCK = 'false'; npm run dev
# bash
VITE_USE_MOCK=false npm run dev
```

### 4. 数据库变更

新增 Flyway 脚本 `V{n}__{描述}.sql`，只增不改；已执行过的脚本不能再修改。

## 前端演示

前端内置 mock 数据层，覆盖三类角色和主要业务流程。演示数据保存在浏览器 `localStorage`，键名 `shiguangji_mock_db_v2`；删除该键并刷新即可恢复初始数据。

演示账号（密码均为 `123456`）：

| 账号 | 角色 | 用途 |
|---|---|---|
| `student` | 学生（已认证） | 林小满：买家视角，有订单、悬赏、置换方案和通知 |
| `seller` | 学生（已认证） | 陈屿：卖家视角，有在售物品和已完成订单 |
| `operator` | 运营 | 物品审核、仲裁处理、运营报表 |
| `admin` | 管理员 | 学籍、分类、自提点、运营账号 |

已覆盖页面：

- 学生端：登录 / 注册 / 学籍认证、首页、搜索、物品详情、发布、我的物品与再次上架、下单支付与充值、订单列表与详情、评价、售后申请、钱包与流水、通知、个人中心、悬赏大厅 / 发布 / 我的悬赏、置换意向与方案确认
- 运营端：物品审核（通过 / 驳回）、仲裁处理、运营报表
- 管理端：学籍导入与查询、分类管理、自提点管理、运营账号创建

mock 是演示与联调前的占位实现，金额、定时任务和风控规则最终以后端为准。后端 `db/demo/V3__demo_seed.sql` 灌入的是同一套账号与相近的演示数据（7 个账号，密码同为 `123456`），前端用 `VITE_USE_MOCK=false` 启动后可以直接用上表的账号登录。

## 配置

| 位置 | 说明 |
|---|---|
| `backend/src/main/resources/application.yml` | 公共配置：端口、Jackson、上传大小、业务时限、JWT、业务限额 |
| `backend/src/main/resources/application-dev.yml` | 开发环境数据源，支持 `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` 覆盖；Flyway 额外加载 `classpath:db/demo` |
| `backend/src/main/resources/db/migration` | `V1__schema.sql` 建表、`V2__seed.sql` 基础数据（分类 / 自提点 / 平台账户） |
| `backend/src/main/resources/db/demo` | 仅 dev 的演示数据 `V3__demo_seed.sql`（账号、物品、订单、悬赏、置换、仲裁） |
| `app.timeout.*` | 所有业务时限集中配置（未支付 30 秒、自动确认 10 分钟、评价窗口 10 分钟、售后 5 分钟、卖家响应 2 分钟、置换确认 10 分钟），测试时可改为秒级 |
| `app.jwt.*` | `secret`（可用 `APP_JWT_SECRET` 覆盖）与 `expiration`（默认 7 天） |
| `app.limits.*` | 单次充值上限、同时进行中的求购数、求购价格容差 |
| `app.upload-dir` | 上传文件目录，默认 `./uploads`，通过 `/uploads/**` 静态映射访问 |
| `frontend/.env.development` | `VITE_API_BASE_URL`，默认 `/api`；`VITE_USE_MOCK` 可用环境变量覆盖 |

数据库密码、API Key 等敏感信息不写进仓库。

## 开发约定

### 分包与依赖方向

- 按业务域分包，模块内部 `controller → service → mapper`；controller 只做参数校验与编排，不写业务规则。
- 跨模块只调用对方 `service`，不直接访问对方的 mapper / entity。
- 基础数据（学籍、分类、自提点）只在 `catalog` 的 service 中读写。

### 状态与事务

- 物品、订单、售后状态只能由对应模块的 service 变更，非法转换抛 `BizException`。
- 事务边界在 service；下单、支付、放款、退款要把订单、物品、资金、履历、信用、通知放在同一事务内。
- 加锁顺序固定：物品 → 订单 → 用户 → 平台账户，避免死锁。

### 资金

- 只有 `payment` 模块可以写余额和平台账户，其他模块调用 `LedgerService`。
- 每笔资金变动写两条流水（用户 / 平台），带余额快照；依赖 `(订单号, 类型, 账户标识)` 唯一键保证幂等。
- 金额统一 `BigDecimal`，数据库 `DECIMAL(10,2)`。

### 命名与接口

- Java / JSON 用 camelCase，数据库表和字段用 snake_case；`user`、`order` 是 MySQL 保留字，表名用 `app_user`、`trade_order`。
- 接口统一 `/api` 前缀，返回 `Result<T>`，分页返回 `PageResult<T>`。
- 枚举入库存字符串，不存序号。
- 前端接口封装放 `src/api/<模块>.ts`，页面不直接写 URL。

### 其他

- 上传文件写入 `app.upload-dir`（默认 `uploads/`），不入库、不提交。
- 报表实时查询，不落表。
- 业务时限一律读配置，不写死。

## Git 约定

- `main` 保持可启动；功能开发走 `feat/<模块>-<简述>`，修复走 `fix/<简述>`。
- 提交信息用 `feat(item): 物品发布`、`fix(trade): 修复超时取消` 格式。
- 不提交 `target/`、`node_modules/`、`.env`、`uploads/`、IDE 配置。
- 合并前至少跑通 `mvn test`（后端）与 `npm run build`（前端）。

## 当前状态

- [x] 仓库骨架、前后端启动工程
- [x] 前端三端页面与 mock 数据层
- [x] Flyway 21 张表与种子数据（MySQL 8.4.11 实测通过）
- [x] 部署文档、Docker Compose / Nginx / 备份脚本骨架
- [ ] 服务器上线（公网 IP + HTTP；域名/HTTPS 后续）
- [x] 统一响应、异常处理、JWT 鉴权
- [x] F1–F8 核心闭环
- [x] F9–F14
- [x] 定时任务联调
- [x] 前端切换真实接口（`VITE_USE_MOCK=false` 只需启动参数，无需改代码）
