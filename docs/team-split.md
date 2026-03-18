# 推荐小组分工

下面是一种适合这个仓库当前结构的拆分方式，优点是写入面相对独立，减少互相覆盖代码的概率。

## 同学 A：认证与安全

负责目录：

- `backend/src/main/java/com/lab/taskmanager/auth/`
- `backend/src/main/java/com/lab/taskmanager/config/`

可以继续做的内容：

- 增加 `/api/auth/me`
- 细化 JWT 过期处理
- 增加角色字段与权限注解
- 增加认证相关测试

## 同学 B：任务后端

负责目录：

- `backend/src/main/java/com/lab/taskmanager/task/`

可以继续做的内容：

- 增加筛选、排序、分页
- 丰富字段约束
- 增加任务详情相关测试
- 为后续 Lab 预留项目、成员、依赖关系字段

## 同学 C：前端页面与交互

负责目录：

- `frontend/src/pages/`
- `frontend/src/components/`
- `frontend/src/layout/`

可以继续做的内容：

- 优化表单交互
- 增加空状态与错误提示
- 增加任务筛选和搜索
- 优化移动端展示

## 同学 D：文档、测试与工程化

负责目录：

- `README.md`
- `docs/`
- `backend/src/test/`
- `frontend` 下的 lint、构建与环境说明

可以继续做的内容：

- 补实验报告中的设计说明
- 整理每位成员的 commit 截图
- 增加接口测试或集成测试
- 完善运行说明与 FAQ

## 合并建议

- 每个人从 `develop` 切自己的 `feature/xxx`
- 一次只做一个明确目标，保持“小步提交”
- 提交前先拉取最新 `develop`
- 合并前走一次 MR，至少让另一位同学审查
