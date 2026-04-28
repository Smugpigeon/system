# sp26-SE-Group14

本项目是《软件工程》Lab1 的课程项目，目标是完成一个前后端分离、基于数据库的最小可运行版本任务管理系统。当前版本已经覆盖：

- 用户注册与登录
- JWT 登录态维护
- 个人任务创建、查看、修改、删除
- 任务筛选、分页与基础排序
- 后端自动化测试与前端构建检查

## 1. 技术栈

- 前端：React + Vite + TypeScript
- 后端：Spring Boot 3 + Spring Security + JPA
- 数据库：H2
- 测试：JUnit 5、Spring Boot Test、MockMvc

## 2. 项目目录

```text
.
├── backend/                  # Spring Boot 后端
│   ├── src/main/             # 后端业务代码
│   └── src/test/             # 后端测试代码
├── frontend/                 # React 前端
└── README.md
```

## 3. 本地运行方式

### 3.1 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

默认地址：

- 后端接口：`http://localhost:8080`
- H2 Console：`http://localhost:8080/h2-console`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- Knife4j：`http://localhost:8080/doc.html`

### 3.2 启动前端

```bash
cd frontend
cp .env.example .env
npm ci
npm run dev
```

默认地址：

- 前端页面：`http://localhost:5173`

## 4. 运行时说明

### 4.1 登录与任务功能

启动前后端后，可直接通过以下流程验证系统主链路：

1. 打开注册页，创建账号
2. 注册成功后自动进入任务页
3. 刷新页面后登录态仍然保留
4. 在任务页中创建、查看、修改、删除个人任务
5. 使用筛选条件查看不同状态和优先级的任务

### 4.2 数据库说明

项目默认使用 H2，本地运行时会使用文件数据库，便于重复启动后保留数据。  
如果只是运行测试，则会切换到测试配置，不污染本地业务数据。

## 5. 测试代码说明

后端测试代码位于：

```text
backend/src/test/java/com/lab/taskmanager/
```

当前和 Lab1 验收最相关的测试代码主要有：

- `backend/src/test/java/com/lab/taskmanager/BackendApplicationTests.java`
  说明：验证 Spring Boot 测试上下文能正常启动。

- `backend/src/test/java/com/lab/taskmanager/auth/controller/AuthControllerTest.java`
  说明：验证注册、登录接口在控制器层的返回结构和错误响应。

- `backend/src/test/java/com/lab/taskmanager/task/controller/TaskControllerTest.java`
  说明：验证任务列表、创建、删除和参数校验等控制器行为。

- `backend/src/test/java/com/lab/taskmanager/acceptance/Lab1RequirementIntegrationTest.java`
  说明：面向 Lab1 验收要求的集成测试，覆盖注册、登录、鉴权、任务 CRUD、数据隔离、分页边界等核心流程。

- `backend/src/test/java/com/lab/taskmanager/task/algorithm/TaskRankingServiceTest.java`
  说明：验证任务排序算法中“逾期优先”“进行中临期优先”“已完成低紧急度”等规则。

测试环境配置文件位于：

- `backend/src/test/resources/application-test.yaml`
  说明：测试时使用 H2 内存数据库，保证自动化测试不受本地已有数据影响。

## 6. 如何运行测试

### 6.1 运行全部后端测试

```bash
cd backend
./mvnw test
```

该命令会运行当前全部后端测试，包括：

- 启动测试
- 认证控制器测试
- 任务控制器测试
- 排序算法测试
- Lab1 验收集成测试

### 6.2 只运行单个测试类

如果只想验证某一部分，可以执行：

```bash
cd backend
./mvnw -Dtest=Lab1RequirementIntegrationTest test
./mvnw -Dtest=AuthControllerTest test
./mvnw -Dtest=TaskControllerTest test
./mvnw -Dtest=TaskRankingServiceTest test
```

### 6.3 运行前端工程检查

```bash
cd frontend
npm run lint
npm run build
```

说明：

- `npm run lint` 用于检查前端代码风格和静态问题
- `npm run build` 用于确认前端项目可以正常打包

## 7. 当前版本建议的验收验证方式

如果要做演示或验收，建议按下面顺序进行：

1. 先运行后端 `./mvnw test`，证明主要功能已有自动化测试覆盖
2. 启动后端与前端服务
3. 现场演示注册与登录
4. 演示个人任务的创建、修改、删除和筛选
5. 如有需要，可展示测试代码说明当前系统已经覆盖的边界情况

## 8. 后续扩展方向

当前版本主要针对 Lab1。后续如果继续做 Lab2 或更复杂的协作功能，可以在现有基础上扩展：

- 团队与成员管理
- 更细的权限控制
- 任务评论、依赖关系、操作日志
- 更完整的统计和推荐能力
