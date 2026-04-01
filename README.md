# sp26-SE-Group14

本次提交完成课程项目的第一次基础初始化，当前只包含后端工程骨架。

## 当前内容

- 基于 Spring Boot 3 的后端项目初始化
- Maven Wrapper，保证组员本地环境一致
- H2 本地文件数据库配置
- 基础的项目启动类和上下文测试

## 后端目录

```text
backend/
├── .mvn/
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/lab/taskmanager/BackendApplication.java
    │   └── resources/application.properties
    └── test/
        └── java/com/lab/taskmanager/BackendApplicationTests.java
```
## API文档
集成Knife4j+Swagger生成API文档，方便前后端联调，访问以下地址查看：
- `http://localhost:8080/doc.html`

## 本地启动

### 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

默认端口：

- http://localhost:8080

H2 Console：

- http://localhost:8080/h2-console

Swagger：

- http://localhost:8080/swagger-ui/index.html

### 启动前端

```bash
cd frontend
cp .env.example .env
npm ci # 不修改package-lock.json
npm run dev
```

默认地址：http://localhost:5173

## 下一步计划

- 增加用户注册与登录模块
- 增加任务实体与基础 CRUD 接口
- 再补前端工程和联调逻辑
