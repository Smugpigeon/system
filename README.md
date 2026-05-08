# sp26-SE-Group14

本项目是《软件工程》课程的协作式任务管理系统，当前版本对应 **Lab3**。系统在 Lab1 的注册登录和个人任务管理、Lab2 的团队协作与角色权限基础上，继续加入任务依赖、成员生命周期和团队解散等复杂业务规则，目标是交付一个易于维护、功能齐全、能解释且具备防御性编程特征的前后端分离 Web 应用。

当前版本已经覆盖：

- 用户注册、登录、JWT 登录态保持
- 个人任务创建、查看、修改、删除
- 团队创建、团队列表、团队空间
- Owner / Admin / Member 三层角色权限
- 团队成员添加与角色调整
- 团队任务创建、分配、更新、删除、状态流转
- 个人任务与团队任务依赖关系管理
- 未完成前置任务阻止后继任务 DONE
- 被其他任务依赖的任务禁止删除
- Member / Admin 主动离开团队，Owner 移除成员
- 离队或被移除成员的团队任务自动转交给 Owner
- Owner 解散团队，解散后普通团队空间不可继续访问
- 个人数据、跨团队数据、未登录访问的后端强校验
- 后端自动化测试、前端静态检查与生产构建验证
- 一键式本地评测链脚本

## 1. 技术栈

- 前端：React 19 + Vite 8 + TypeScript
- 后端：Spring Boot 3 + Spring Security + JPA
- 数据库：H2
- API 文档：SpringDoc OpenAPI + Knife4j
- 测试：JUnit 5、Spring Boot Test、MockMvc、Mockito

## 2. 目录结构

```text
.
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/com/lab/taskmanager/
│   │   ├── auth/             # 认证、JWT、安全链路
│   │   ├── task/             # 个人任务、团队任务、依赖、排序、分页
│   │   ├── team/             # 团队、成员、角色、生命周期与权限
│   │   ├── user/             # 用户实体与查询
│   │   └── common/           # 通用异常、统一返回体、审计基类
│   └── src/test/             # 单元测试、控制器测试、集成测试
├── frontend/                 # React 前端
│   └── src/
│       ├── api/              # 前端 API 层
│       ├── components/       # 可复用组件
│       ├── context/          # 认证上下文
│       ├── pages/            # 登录、注册、工作台、团队页、团队空间
│       └── types/            # 前后端数据契约类型
├── .nvmrc                    # 推荐 Node 版本
└── README.md
```

## 3. 环境要求

- Java 17
- Node.js 22.22.2
- npm 10+

推荐先切换到仓库根目录定义的 Node 版本：

```bash
nvm use
```

如果本机没有安装对应 Node 版本，可先执行：

```bash
nvm install 22.22.2
```

如果你不用 nvm，也请确保前端命令实际运行在 Node 22 上。当前依赖没有按 Node 25 做兼容验证。

## 4. 本地启动

### 4.1 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

默认地址：

- 后端接口：`http://localhost:8080`
- H2 Console：`http://localhost:8080/h2-console`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- Knife4j：`http://localhost:8080/doc.html`

### 4.2 启动前端

```bash
cd frontend
cp .env.example .env
npm ci
npm run dev
```

默认地址：

- 前端页面：`http://localhost:5173`

## 5. Lab3 功能概览

### 5.1 认证与个人工作台

- 注册成功后自动进入工作台
- 登录后通过 JWT 访问受保护接口
- 刷新页面后仍保持登录态
- `/tasks` 页面同时展示：
  - 当前用户创建的个人任务
  - 分配给当前用户的团队任务

### 5.2 团队、角色权限与生命周期

- `/teams`
  - 展示“我的团队”
  - 创建团队
  - 显示当前用户在团队中的角色

- `/teams/:teamId`
  - 展示团队成员及角色
  - Owner 可添加成员并调整 Member / Admin
  - Owner 可移除 Member / Admin
  - Member / Admin 可主动离开团队
  - Owner 可解散团队
  - Admin / Owner 可创建、编辑、删除和分配团队任务
  - Member 可浏览团队所有任务，但只能修改分配给自己的任务状态

### 5.3 任务依赖关系

- 个人任务只能依赖当前用户自己的其他个人任务
- 团队任务只能依赖同一团队中的其他任务
- 系统同时展示某个任务的前置任务和后继任务
- Admin / Owner 可在团队空间管理团队任务依赖
- 用户可在个人工作台管理自己的个人任务依赖
- 任务不能依赖自己，不能重复依赖，不能形成循环依赖
- 如果存在未完成前置任务，后继任务不能被标记为 `DONE`

### 5.4 开放策略选择

我们在 Lab3 中采用以下策略，便于保持数据一致性和演示可解释性：

- 删除任务：如果任务仍被其他任务依赖，则禁止删除；如果任务只依赖别人，则删除任务前自动移除它自己的前置依赖边。
- 成员离开或被移除：不删除历史任务，所有仍分配给该成员的团队任务自动转交给 Owner。
- Owner 离开：不允许 Owner 直接离开团队，Owner 只能先解散团队。
- 团队解散：采用软解散，团队状态变为 `DISSOLVED`，历史任务和依赖保留，但普通团队空间访问和修改被后端阻止。

### 5.5 后端强校验

后端不会依赖前端按钮隐藏来做权限控制：

- 非团队成员不能访问团队数据
- Member 不能越权修改他人任务
- 团队任务不能通过个人任务接口非法删除或修改
- 非 Admin / Owner 不能修改团队任务依赖
- 已离队成员不能继续访问或操作原团队任务
- 已解散团队不能继续通过普通团队空间访问
- 非法输入会返回明确错误原因和合理 HTTP 状态码

## 6. 测试与评测链

后端测试代码位于：

```text
backend/src/test/java/com/lab/taskmanager/
```

当前最重要的测试包括：

- `backend/src/test/java/com/lab/taskmanager/BackendApplicationTests.java`
  - 验证 Spring Boot 测试上下文可以在测试配置下启动

- `backend/src/test/java/com/lab/taskmanager/auth/controller/AuthControllerTest.java`
  - 控制器层测试，覆盖注册、登录和参数校验

- `backend/src/test/java/com/lab/taskmanager/task/controller/TaskControllerTest.java`
  - 控制器层测试，覆盖个人任务查询、创建、删除和参数校验

- `backend/src/test/java/com/lab/taskmanager/team/controller/TeamControllerTest.java`
  - 控制器层测试，覆盖团队创建、成员添加请求校验、角色调整接口

- `backend/src/test/java/com/lab/taskmanager/team/service/TeamServiceTest.java`
  - 业务层单元测试，使用 Mock 验证团队创建、重复成员添加、Owner 角色保护等逻辑

- `backend/src/test/java/com/lab/taskmanager/acceptance/Lab1RequirementIntegrationTest.java`
  - Lab1 回归集成测试，保证 Lab2 增量没有破坏注册登录和个人任务主链路

- `backend/src/test/java/com/lab/taskmanager/acceptance/Lab2RequirementIntegrationTest.java`
  - Lab2 集成测试，覆盖团队创建、添加成员、角色晋升、团队任务创建与状态修改、跨团队隔离

- `backend/src/test/java/com/lab/taskmanager/acceptance/Lab3RequirementIntegrationTest.java`
  - Lab3 集成测试，覆盖团队任务依赖、未完成前置任务阻止 DONE、依赖删除策略、成员离开和团队解散

- `backend/src/test/java/com/lab/taskmanager/task/service/TaskDependencyServiceTest.java`
  - Lab3 业务层单元测试，使用 Mock 验证依赖状态流转和被依赖任务删除限制

- `backend/src/test/java/com/lab/taskmanager/task/algorithm/TaskRankingServiceTest.java`
  - 排序算法单元测试，验证 ranking 逻辑仍可用

测试配置文件：

- `backend/src/test/resources/application-test.yaml`
  - 使用 H2 内存数据库，避免污染本地文件库

## 7. 如何运行验证

也可以直接在仓库根目录执行完整评测链：

```bash
./scripts/verify_lab3.sh
```

这条脚本会顺序执行：

- 后端 `./mvnw clean test`
- 前端 `npm ci`
- 前端 `npm run lint`
- 前端 `npm run build`

### 7.1 运行全部后端测试

```bash
cd backend
./mvnw clean test
```

### 7.2 运行前端检查

```bash
cd frontend
npm run lint
npm run build
```

### 7.3 只跑关键 Lab3 测试

```bash
cd backend
./mvnw -Dtest=Lab3RequirementIntegrationTest test
./mvnw -Dtest=TaskDependencyServiceTest test
./mvnw -Dtest=Lab1RequirementIntegrationTest,Lab2RequirementIntegrationTest test
```

## 8. 建议的验收演示顺序

1. 注册或登录一个用户
2. 进入个人工作台，演示个人任务 CRUD
3. 进入“我的团队”，创建团队
4. 将另一名用户加入团队
5. 将成员提升为 Admin 或降回 Member
6. 在团队空间创建团队任务并分配给成员
7. 为团队任务添加前置依赖，验证前置未完成时后继不能 DONE
8. 尝试删除被依赖任务，验证后端拒绝并返回明确错误
9. 使用被分配的成员账号登录，验证只能修改自己的任务状态
10. 演示成员主动离开或 Owner 移除成员，确认任务转交 Owner
11. 演示 Owner 解散团队，确认团队空间不能继续普通访问
12. 切换 outsider 账号，验证无法访问该团队数据

## 9. 设计原则

本项目当前版本遵循这些原则：

- 个人任务与团队任务共用任务域模型，但用 `TaskScope` 明确区分
- 权限判断集中在后端 Service 层，而不是散落在 Controller 或前端
- 任务依赖使用独立 `task_dependencies` 表建模，避免在任务表中保存多值字段
- 团队和成员关系采用状态字段软关闭，保留历史数据用于追溯和报告说明
- 保留 Lab1 接口兼容性，避免后续实验在已有能力上返工
- 用统一异常包装和 HTTP 状态码表达错误原因
- 用自动化测试守住增量开发中的回归风险
