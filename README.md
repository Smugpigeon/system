# sp26-SE-Group14

本项目是《软件工程》课程的协作式任务管理系统，当前版本对应 **Lab2**。系统在 Lab1 的注册登录和个人任务管理基础上，进一步引入了团队、成员角色与后端强制权限校验，目标是交付一个易于维护、功能齐全、能解释且具备防御性编程特征的前后端分离 Web 应用。

当前版本已经覆盖：

- 用户注册、登录、JWT 登录态保持
- 个人任务创建、查看、修改、删除
- 团队创建、团队列表、团队空间
- Owner / Admin / Member 三层角色权限
- 团队成员添加与角色调整
- 团队任务创建、分配、更新、删除、状态流转
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
│   │   ├── task/             # 个人任务、团队任务、排序、分页
│   │   ├── team/             # 团队、成员、角色与权限
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

## 5. Lab2 功能概览

### 5.1 认证与个人工作台

- 注册成功后自动进入工作台
- 登录后通过 JWT 访问受保护接口
- 刷新页面后仍保持登录态
- `/tasks` 页面同时展示：
  - 当前用户创建的个人任务
  - 分配给当前用户的团队任务

### 5.2 团队与角色权限

- `/teams`
  - 展示“我的团队”
  - 创建团队
  - 显示当前用户在团队中的角色

- `/teams/:teamId`
  - 展示团队成员及角色
  - Owner 可添加成员并调整 Member / Admin
  - Admin / Owner 可创建、编辑、删除和分配团队任务
  - Member 可浏览团队所有任务，但只能修改分配给自己的任务状态

### 5.3 后端强校验

后端不会依赖前端按钮隐藏来做权限控制：

- 非团队成员不能访问团队数据
- Member 不能越权修改他人任务
- 团队任务不能通过个人任务接口非法删除或修改
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

- `backend/src/test/java/com/lab/taskmanager/task/algorithm/TaskRankingServiceTest.java`
  - 排序算法单元测试，验证 ranking 逻辑仍可用

测试配置文件：

- `backend/src/test/resources/application-test.yaml`
  - 使用 H2 内存数据库，避免污染本地文件库

## 7. 如何运行验证

也可以直接在仓库根目录执行完整评测链：

```bash
./scripts/verify_lab2.sh
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

### 7.3 只跑关键 Lab2 测试

```bash
cd backend
./mvnw -Dtest=Lab2RequirementIntegrationTest test
./mvnw -Dtest=TeamServiceTest test
./mvnw -Dtest=TeamControllerTest test
```

## 8. 建议的验收演示顺序

1. 注册或登录一个用户
2. 进入个人工作台，演示个人任务 CRUD
3. 进入“我的团队”，创建团队
4. 将另一名用户加入团队
5. 将成员提升为 Admin 或降回 Member
6. 在团队空间创建团队任务并分配给成员
7. 使用被分配的成员账号登录，验证只能修改自己的任务状态
8. 切换 outsider 账号，验证无法访问该团队数据

## 9. 设计原则

本项目当前版本遵循这些原则：

- 个人任务与团队任务共用任务域模型，但用 `TaskScope` 明确区分
- 权限判断集中在后端 Service 层，而不是散落在 Controller 或前端
- 保留 Lab1 接口兼容性，避免后续实验在已有能力上返工
- 用统一异常包装和 HTTP 状态码表达错误原因
- 用自动化测试守住增量开发中的回归风险
