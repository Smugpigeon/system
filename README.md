# sp26-SE-Group14

本次提交完成课程项目的第一次基础初始化，当前只包含后端工程骨架。
基于 Spring Boot 3 构建的 RESTful API 服务，支持团队任务分配与进度追踪。
## 当前内容

- 基于 Spring Boot 3 的后端项目初始化
- Maven Wrapper，保证组员本地环境一致
- H2 本地文件数据库配置
- 基础的项目启动类和上下文测试
## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.6 | 核心框架 |
| Java | 17 | 运行环境 |
| Spring Data JPA | - | 数据持久化 |
| H2 Database | - | 开发数据库（文件模式） |
| Spring Validation | - | 参数校验 |
| Spring Security Crypto | - | 密码加密 |
| Lombok | - | 代码简化 |
| Maven | - | 构建工具 |

---
## 后端目录

```
sp26-SE-Group14/
├── backend/              
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/lab/taskmanager/
│   │   │   └── resources/
│   │   └── test/
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── .mvn/
├── LICENSE
└── README.md
```

### 环境要求
- JDK 17+
- Maven 3.8+（或使用项目提供的 Maven Wrapper）

### 本地启动

```bash
cd backend
./mvnw spring-boot:run        # Linux/Mac
# 或
mvnw.cmd spring-boot:run      # Windows
```

### 服务访问

| 端点 | 地址 |
|------|------|
| 应用服务 | `http://localhost:8080` |
| H2 控制台 | `http://localhost:8080/h2-console` |
| JDBC URL | `jdbc:h2:file:./data/task-manager` |

---

## 环境变量配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `SERVER_PORT` | `8080` | 服务端口 |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/task-manager;MODE=MYSQL;AUTO_SERVER=TRUE` | 数据库连接 |
| `SPRING_DATASOURCE_USERNAME` | `sa` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | (空) | 数据库密码 |

---

##  功能模块

-  项目基础骨架搭建
-  H2 文件数据库集成

## 下一步计划

- 增加用户注册与登录模块
- 增加任务实体与基础 CRUD 接口
- 再补前端工程和联调逻辑
