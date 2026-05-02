# sp26-SE-Group14

本项目是《软件工程》课程 Lab1 与 Lab2 的迭代开发项目，现已演进为一个支持多用户协作与权限控制的任务管理系统。

- **Lab1 基础版本**：实现了一个前后端分离、基于数据库的最小可运行个人任务管理系统。
- **Lab2 增量版本**：在 Lab1 基础上，引入了团队协作机制、细粒度的角色与权限控制，将个人任务管理升级为面向团队的协作式任务管理平台。

当前版本覆盖功能：

### Lab1 核心功能
- 用户注册与登录
- JWT 登录态维护
- 个人任务创建、查看、修改、删除
- 任务筛选、分页与基础排序
- 后端自动化测试与前端构建检查

### Lab2 增量功能
- 团队创建与成员管理
- 角色与权限控制
- 我的团队
- 个人任务列表扩展
- 个人数据、跨团队数据的隔离
- 前端界面扩展

## 1. 技术栈

- 前端：React + Vite + TypeScript
- 后端：Spring Boot 3 + Spring Security + JPA
- 数据库：H2
- 测试：JUnit 5、Spring Boot Test、MockMvc

## 2. 核心设计说明 (Lab2 增量)

### 2.1 数据模型扩展
- **`Team` 实体**：包含团队 ID、名称、Owner。与 `User` 通过 `TeamMemberRole` 关联。
- **`TeamMemberRole` 关联实体**：记录用户与团队的隶属关系及角色 (`OWNER`， `ADMIN`， `MEMBER`)。
- **`Task` 实体扩展**：增加 `team` 字段

### 2.2 权限校验架构
权限校验遵循“后端强制校验”原则，在 **Service 层** 实现核心业务逻辑的权限控制，确保即使绕过前端也无法进行非法操作。
- **校验点**：在执行任何数据操作（增、删、改、查）前，均会校验当前用户身份、团队角色及对目标资源的操作权限。
- **异常处理**：非法操作将抛出自定义异常（如 `ForbiddenOperationException`），并最终被全局异常处理器捕获，返回标准的 `403 Forbidden` 或 `404 Not Found` 错误响应。

### 2.3 防御性编程与代码质量
1.  **参数校验**：使用 `@Valid` 注解及自定义校验器对 API 输入进行有效性检查。
2.  **空值处理**：返回集合类型的方法保证返回空集合而非 `null`；可能为空的单值使用 `Optional` 封装, 大部分由JPA自动处理。
3.  **注解使用**：关键方法参数使用 `@NotNull` / `@Nullable` 注解明确契约。
4.  **异常处理**：遵循“早抛出，晚捕获”原则，在GlobalExceptionHandler中进行统一异常处理与响应包装。

## 3. 项目目录

```text
.
├── backend/                  # Spring Boot 后端
│   ├── src/main/             # 后端业务代码
│   └── src/test/             # 后端测试代码
├── frontend/                 # React 前端
└── README.md
```

## 4. 本地运行方式

### 4.1 启动后端
```bash
cd backend
./mvnw spring-boot:run
```
默认地址：
- 后端接口：`http://localhost:8080`
- H2 Console：`http://localhost:8080/h2-console`
- API 文档 (Knife4j)：`http://localhost:8080/doc.html`

### 4.2 启动前端
```bash
cd frontend
cp .env.example .env
npm ci
npm run dev
```
默认地址：
- 前端页面：`http://localhost:5173`

## 5. 核心使用流程演示

1.  **注册与登录**：新用户可注册账号并登录。
2.  **创建团队**：
    - 登录后，进入“我的团队”页面，点击“创建团队”。
    - 创建者自动成为该团队的 **Owner**。
3.  **管理团队**：
    - 点击团队卡片，进入对应的团队空间。
    - 在“团队空间”，Owner 可通过用户名将其他用户添加为 **Member**。
    - Owner 可以将 Member 提升为 **Admin**，或将其降级。
4.  **团队协作**：
    - **Admin 或 Owner** 可以在“团队空间”创建任务，并分配给团队成员。
    - **Member** 可以在“任务列表”中查看所有团队任务，但只能修改**分配给自己**的任务的状态。
    - **Admin/Owner** 可以修改或删除任何团队任务。
5.  **任务视图**：
    - “任务列表”页面会合并展示用户创建的个人任务，以及所有分配给该用户的团队任务。
    - 用户可以通过筛选器区分查看“所有任务”、“个人任务”或“团队任务”。

## 6. 测试代码说明 (Lab2 增量)

后端测试代码位于 `backend/src/test/java/com/lab/taskmanager/`。除 Lab1 的测试外，新增了以下关键测试：

- **`team/controller/TeamControllerTest.java`**
    - 验证团队创建、成员添加、角色变更等接口的权限与业务逻辑。
- **`task/controller/TaskControllerPermissionTest.java`** 
    - 专门测试在团队场景下，不同角色（Owner/Admin/Member/非成员）对任务进行增删改查操作时的权限控制是否正确。

测试环境配置文件位于：

- `backend/src/test/resources/application-test.yaml`
  说明：测试时使用 H2 内存数据库，保证自动化测试不受本地已有数据影响。

## 7. 如何运行测试

### 7.1 运行全部后端测试
```bash
cd backend
./mvnw test
```
此命令将运行所有测试，包括 Lab1 和 Lab2 的单元测试、集成测试。

### 7.2 运行前端工程检查
```bash
cd frontend
npm run lint   # 代码风格检查
npm run build  # 生产构建检查
```

## 8. 后续扩展方向

当前版本已满足 Lab1 与 Lab2 的全部需求。系统架构为后续功能扩展留有空间，可能的扩展方向包括：
- 更复杂的权限模型（如基于用户组的权限）。
- 任务评论、附件、子任务与依赖关系。
- 团队内的操作日志与审计。
- 更丰富的任务统计分析与报表功能。