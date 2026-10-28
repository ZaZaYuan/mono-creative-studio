# MONO 创作实验室

一个可独立运行的素材制作网站：浏览模板、上传图片、提交后台任务、查看三阶段处理状态，以及恢复个人历史和预览结果。模拟生成会把第一张上传图片排版成作品图，用于演示完整业务闭环；不调用真实 AI 服务。

## 技术与目录

- `web/`：Next.js 16.4.3、React 19.1、TypeScript，App Router。浏览器调用同源 `/api/backend/*`，Next Route Handler 转发到 Java。
- `server/`：Java 21、Spring Boot 3.4.5、MyBatis 3.0.4、Flyway、MySQL 8.4。
- `compose.yaml`：MySQL、Java、Next.js 及持久卷。
- `DESIGN.md`：接口、状态流转与 R-01 至 R-14 的映射。

## 启动

推荐安装 Docker Compose 后在仓库根目录运行：

```bash
docker compose up --build
```

打开 `http://localhost:3000`。MySQL 初始化由 Flyway 自动执行，预置 6 个模板。第一次构建需要下载 Maven 和 npm 依赖。

Java Docker 构建使用 `server/docker-settings.xml` 中的阿里云公开 Maven 镜像，以适配 Docker 容器无法直连 Maven Central 的网络环境；本地 Maven 与 GitHub Actions 仍使用各自默认配置。

本地分开运行：先启动 MySQL，并创建 `atelier` 数据库和 `atelier` 用户（密码 `atelier`）；在 `server` 执行 `mvn spring-boot:run`，在 `web` 执行 `npm ci && npm run dev`。环境变量可覆盖 `DB_URL`、`DB_USER`、`DB_PASSWORD`、`STORAGE_DIR`、`STAGE_DELAY_MS`、`DEMO_FAILURE_ENABLED` 和 `API_INTERNAL_URL`。后端默认端口 8080，前端默认 3000。

## 测试与 CI

```bash
cd server && mvn test package
cd web && npm ci && npm run lint && npm run typecheck && npm run build
```

`.github/workflows/ci.yml` 在 push 和 pull request 时执行上述检查。验收时以提交链接中的 commit SHA 为准。

另有 `DatabaseFlowIT`，在有独立 MySQL 测试库时设置 `RUN_DB_IT=true`、数据库及素材目录环境变量，再执行 `mvn -Dtest=DatabaseFlowIT test`。该测试实际验证 Flyway、模板读取、成功和失败任务、结果文件、历史及身份隔离；默认 CI 不运行它，因为 CI 不提供 MySQL 服务。

## 主要接口

| 方法 | 路径 | 功能 |
|---|---|---|
| GET | `/api/templates?category=` | 模板及分类筛选 |
| GET | `/api/templates/{id}` | 模板详情 |
| POST | `/api/tasks` | multipart：`templateId`、重复的 `images`、可选 `failAtStage`；返回 202 与任务对象 |
| GET | `/api/tasks` | 当前身份历史 |
| GET | `/api/tasks/{id}` | 当前身份的任务状态，只读 |
| GET | `/api/files/{id}` | 受身份限制的图片预览 |

任务状态 `PENDING`、`PROCESSING`、`SUCCEEDED`、`FAILED`；阶段 `MATERIAL_CHECK`、`GENERATION`、`FINALIZATION`。失败原因保存在任务记录中。提交请求完成校验、素材保存和任务记录创建后返回，后续步骤由 Java 有界线程池执行。前端只轮询未完成任务，刷新后从 Java/MySQL 重新读取。

## 演示方法

1. 打开“探索模板”，按分类筛选，进入模板详情。
2. 上传模板要求数量的 JPG/PNG 图片，单张不超过 5 MB。选择“正常完成”并提交，立即切到“我的创作”，观察三个阶段，最终点击“预览作品”。
3. 再提交一个任务，把“演示失败阶段”设为“模拟生成后失败”，观察任务停在该阶段并保留失败原因。
4. 连续提交多个任务，刷新页面，确认历史互不干扰并继续更新。调整 `STAGE_DELAY_MS` 可放慢或加快演示。

匿名用户由 Java 设置一年有效期的 HttpOnly、SameSite=Lax Cookie `atelier_id`。清除 Cookie 会获得新的匿名身份，旧任务不会显示；这是演示身份，不提供账号合并或找回。结果和上传文件保存在 Java 服务本地目录，Compose 使用持久卷。

## 已知边界与回滚

后台线程池和文件目录适用于单实例演示。服务进程在任务运行中被强制终止时，该任务不会自动恢复；题目将重启恢复列为非必需项。队列满时任务会以明确原因标记失败。没有真实 AI、视频处理、对象存储、支付或外部账号服务。

部署回滚：先备份 MySQL 数据卷和 `media_data`；使用先前的源码 commit 重新构建启动。数据库迁移为追加式，回滚跨迁移版本前应先从备份恢复数据库，不应直接删除生产数据卷。

## AI 工作流与素材来源

本项目使用 AI 编程工具：先从题目拆出 R-01 至 R-14，制定状态机和接口；再生成后端、前端、测试与交付配置；通过 Maven、lint、类型检查和构建结果修正问题。人工应复核业务边界、演示路径和最终运行效果。项目内的模板名称、说明、抽象图形均为本项目原创示例；没有使用题目截图中的品牌图片或付费素材。依赖为公开的开源 npm/Maven 包，具体版本见锁文件和 `pom.xml`。
