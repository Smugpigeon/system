# sp26-SE-Group14

基于 Spring Boot 3 + Vue 3 构建的全栈任务管理系统，支持用户注册登录、任务CRUD及智能优先级排序。

## 技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 后端 | Spring Boot | 3.x | 核心框架 |
| 后端 | Java | 17 | 运行环境 |
| 后端 | Spring Security + JWT | - | 认证授权 |
| 后端 | Spring Data JPA | - | 数据持久化 |
| 后端 | H2 Database | - | 开发数据库 |
| 前端 | Vue | 3.4 | 前端框架 |
| 前端 | Vite | 5.x | 构建工具 |
| 前端 | Element Plus | 2.6 | UI组件库 |
| 前端 | Axios | 1.6 | HTTP客户端 |

## 项目结构

```
sp26-SE-Group14/
├── backend/                    # Spring Boot后端
│   ├── src/main/java/
│   │   └── com/lab/taskmanager/
│   │       ├── auth/          # 认证模块（登录/注册/JWT）
│   │       ├── task/          # 任务模块（实体/服务/存储）
│   │       ├── user/          # 用户模块
│   │       ├── common/        # 公共配置（CORS/安全/工具）
│   │       └── controller/    # 控制器（TaskController等）
│   └── src/main/resources/
│       └── application.properties
├── frontend/                   # Vue3前端
│   ├── src/
│   │   ├── api/http.js        # Axios封装
│   │   ├── router/index.js    # 路由配置
│   │   ├── views/             # 页面组件
│   │   │   ├── LoginView.vue  # 登录/注册
│   │   │   └── TaskList.vue   # 任务管理
│   │   └── App.vue
│   └── package.json
└── README.md
```

## 快速启动

### 环境要求
- JDK 17+
- Node.js 18+
- Maven 3.8+（或使用 `./mvnw`）

### 1. 启动后端（端口8080）

```bash
cd backend

# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

### 2. 启动前端（端口5173）

```bash
cd frontend
npm install
npm run dev
```

### 3. 访问应用

| 端点 | 地址 |
|------|------|
| 前端页面 | http://localhost:5173 |
| 后端API | http://localhost:8080 |
| H2控制台 | http://localhost:8080/h2-console |
| H2 JDBC URL | `jdbc:h2:file:./data/task-manager` |

## API接口

### 认证接口
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |

### 任务接口（需JWT）
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/tasks` | 获取当前用户所有任务 |
| GET | `/api/tasks/{id}` | 获取单个任务 |
| POST | `/api/tasks` | 创建任务 |
| PUT | `/api/tasks/{id}` | 更新任务 |
| DELETE | `/api/tasks/{id}` | 删除任务 |

## 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `SERVER_PORT` | `8080` | 后端端口 |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/task-manager` | 数据库连接 |
| `SPRING_DATASOURCE_USERNAME` | `sa` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | (空) | 数据库密码 |

## 测试

```bash
cd backend
mvnw.cmd test        # 运行后端测试
```

## 许可

[LICENSE](LICENSE)