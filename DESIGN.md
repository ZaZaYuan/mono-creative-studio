# 实现设计与验收映射

## 架构

- `web`：Next.js App Router、React、TypeScript，负责模板浏览、上传、任务状态和历史展示。
- `server`：Spring Boot、MyBatis，提供模板、任务、素材和结果接口，执行后台三阶段模拟任务。
- MySQL：保存模板、素材、任务及阶段状态。图片保存在 Java 服务可访问的本地目录。
- 前端通过 `/api/backend/*` 重写请求到 Java 服务，避免浏览器跨域配置。

## 业务契约

1. `GET /api/templates?category=` 返回模板及分类。模板由数据库预置数据提供。
2. `GET /api/templates/{id}` 返回模板详情和素材限制。
3. `POST /api/tasks` 使用 multipart/form-data，字段为 `templateId` 和 `images`。验证模板、数量、MIME 和文件大小，保存素材与任务后立即返回 `202 {id,status}`。
4. `GET /api/tasks/{id}` 只读取持久状态，不推进任务。
5. `GET /api/tasks` 返回当前身份的历史任务，按创建时间倒序。
6. `GET /api/files/{id}` 返回已保存的素材或结果预览。

任务整体状态：`PENDING → PROCESSING → SUCCEEDED/FAILED`；阶段：`MATERIAL_CHECK → GENERATION → FINALIZATION`。每一步进入和结束都更新数据库；失败后记录阶段与原因，不执行后续步骤。结果整理后存储结果文件路径。

## 实现边界

- 队列使用 Java 有界线程池。任务记录提交后投递，查询不触发运行。
- 多任务独立记录、独立执行状态。应用重启后自动恢复未完成任务不在题目范围内，README 将明示。
- 失败注入使用仅在开发配置启用的后端参数，按指定阶段稳定失败。
- 无真实 AI、支付、对象存储或外部身份服务。

## 验收映射

| 条款 | 实现位置 |
|---|---|
| R-01 | web、server、MySQL |
| R-02 | 模板 API、预置数据、列表分类 |
| R-03 | 详情、上传、前后端双重验证、服务端文件目录 |
| R-04 | 任务创建与后台执行器 |
| R-05 | 三阶段状态持久化与只读查询 |
| R-06 | 开发配置的指定阶段失败 |
| R-07 | 独立的上传、提交、任务、查询异常状态 |
| R-08 | 独立任务与未完成任务轮询 |
| R-09 | MySQL 历史与可预览结果 |
| R-10 | 匿名 `atelier_id` Cookie 关联个人任务；任务和文件接口按身份隔离，README 说明演示与限制 |
| R-11 | 桌面和窄屏界面、可访问性标签 |
| R-12 | GitHub Actions 与关键行为测试 |
| R-13 | README、锁文件、初始化数据、来源与 AI 工作流 |
| R-14 | Dockerfiles、Compose 和启动说明 |
