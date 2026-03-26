# Collaborative Task Manager Lab1

一个面向课程 Lab1 的前后端分离任务管理系统基础框架。当前版本已经覆盖：

- 用户注册
- 用户登录
- JWT 登录态保持
- 个人任务的创建、查看、修改、删除
- 不同用户之间的任务数据隔离
- 适合多人并行开发的目录结构与模块边界

这个仓库的目标不是一次性做完后续所有 Lab，而是先把 Lab1 的最小可运行版本和可扩展骨架搭稳，让组员可以继续在此基础上拆分功能。

## 技术栈

- 前端：React 19 + TypeScript + Vite + React Router + Axios
- 后端：Spring Boot 3 + Spring Security + Spring Data JPA + Validation
- 数据库：H2 文件数据库（默认本地开箱即用）
- 认证方式：JWT

## 目录结构

```text
software_engineering/
├── backend/                  # Spring Boot 后端
│   └── src/main/java/com/lab/taskmanager
│       ├── auth/             # 注册、登录、JWT、安全过滤器
│       ├── common/           # 通用响应、异常、审计基类
│       ├── config/           # 安全与配置绑定
│       ├── task/             # 任务实体、DTO、Service、Controller
│       └── user/             # 用户实体与仓库
├── frontend/                 # React 前端
│   └── src/
│       ├── api/              # 接口请求封装
│       ├── app/              # 顶层路由
│       ├── components/       # 通用组件
│       ├── context/          # 登录态上下文
│       ├── layout/           # 页面布局
│       ├── pages/            # 登录、注册、任务页
│       ├── types/            # 类型定义与选项常量
│       └── utils/            # 日期与本地存储工具
└── docs/                     # 分工与扩展说明
```

## 快速启动

### 1. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

默认地址：`http://localhost:8080`

后端默认使用本地 H2 文件数据库：

- 数据文件目录：`backend/data/`
- H2 Console：`http://localhost:8080/h2-console`

如果想改成 MySQL，可以覆盖这些环境变量：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/task_manager'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='your_password'
export APP_JWT_SECRET='your_base64_secret'
```

### 2. 启动前端

```bash
cd frontend
cp .env.example .env
npm ci # 不修改package-lock.json
npm run dev
```

使用的npm版本为 10.9.2

使用的node版本为v22.15.0

默认地址：`http://localhost:5173`

### 3. 前后端联调

- 前端默认请求：`http://localhost:8080/api`
- 登录成功后，JWT 会保存在浏览器本地存储中
- 页面刷新后会继续保持登录状态

## 当前已实现的接口

### 认证

- `POST /api/auth/register`
- `POST /api/auth/login`

### 个人任务

- `GET /api/tasks`
- `POST /api/tasks`
- `GET /api/tasks/{id}`
- `PUT /api/tasks/{id}`
- `DELETE /api/tasks/{id}`

统一返回格式：

```json
{
  "success": true,
  "message": "任务创建成功",
  "data": {}
}
```

## 设计说明

### 为什么先用 H2

Lab1 的重点是把完整业务链路做通，而不是在数据库运维上消耗时间。默认用 H2 文件库可以保证：

- 仓库拉下来后无需额外装数据库就能跑
- 数据仍然是“真实落库”，不是内存临时变量
- 后续需要切 MySQL 时，只要替换数据源配置即可

### 为什么选 JWT

实验要求登录状态在页面刷新后仍然有效。JWT 的好处是：

- 前后端分离更自然
- 前端只需要保存令牌即可
- 后续做权限细分、接口鉴权时扩展成本低

### 为什么模块拆成 auth / task / user / common

这样做的目的是避免后面所有代码都堆进 Controller 或单个 Service 中，方便组员并行开发：

- 一位同学继续扩展认证、权限、审计
- 一位同学完善任务列表、筛选、分页、交互
- 一位同学补充测试、文档和异常处理
- 一位同学准备 Lab2 的团队空间、成员关系、角色设计

## 推荐分工

查看 [docs/team-split.md](docs/team-split.md)

## GitFlow 建议

建议按课程要求使用：

1. `main`：稳定可演示版本
2. `develop`：日常集成分支
3. `feature/xxx`：个人功能开发分支
4. `hotfix/xxx`：主分支缺陷修复

建议提交粒度：

- `feat: scaffold backend auth module`
- `feat: implement task crud api`
- `feat: build login and register pages`
- `docs: add setup and collaboration guide`
- `fix: handle invalid token response`

## 当前适合作为下一步扩展的内容

- 任务筛选、搜索、分页
- 更完善的表单校验与用户提示
- 接口测试与前端组件测试
- 团队空间、任务指派、角色权限
- 操作日志、评论、任务依赖关系
