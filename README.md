# LifeArchive 运行指南

LifeArchive —— 基于 AI 的个人数字记忆管理与人生档案平台。

技术栈：Vue 3 + TypeScript + Vite（前端） / Spring Boot 3 + MyBatis-Plus + MySQL + Redis（后端） / DeepSeek 大模型 + RAG。

---

## 一、环境要求

### Docker 方式

只需要安装 **Docker Desktop**（Windows / macOS），不需要安装 Java、MySQL、Redis、Maven。

- Docker Desktop 下载：https://www.docker.com/products/docker-desktop/
- 安装后启动 Docker Desktop，确认托盘图标显示运行中。

> 后端采用 Docker 多阶段构建，Maven 编译和 JDK 22 运行环境都在容器内，本机无需任何开发环境。

### 不安装 Docker 的方式

需要 **JDK 22**（见下文"不安装 Docker 的启动方式"）；前端无论哪种方式都需要 **Node.js 18+**。

---

## 二、快速启动（推荐）

### 1. 启动后端

双击项目根目录的 `start.bat`，脚本会自动：

1. 拉取 MySQL 8.0 / Redis 7 镜像；
2. 在容器内编译并打包后端；
3. 启动 mysql、redis、backend 三个容器，并自动初始化数据库表。

等待约 10~30 秒（首次启动需下载依赖，可能更久），浏览器访问：

```
http://localhost:8080/api/health
```

返回 `{"code":200,...}` 即表示后端启动成功。

### 2. 启动前端

打开一个新终端，执行：

```bash
cd frontend
npm install
npm run dev
```

浏览器访问：

```
http://localhost:5173
```

> 前端启动需要 Node.js 18+，首次执行 `npm install` 会联网下载依赖。

### 3. 停止服务

双击项目根目录的 `stop.bat`，或执行：

```bash
docker compose down
```

停止容器不会删除数据，下次启动数据仍在（保存在 Docker 数据卷中）。

---

## 三、手动启动（可选）

不用 `start.bat` 时，也可以手动执行：

```bash
docker compose build backend
docker compose up -d
```

---

## 四、不安装 Docker 的启动方式（本地开发）

### 方式一：零依赖（推荐，只需 JDK 22）

后端 `dev` 模式使用内置的 H2 内存数据库，**不需要 MySQL 和 Redis**：

1. 安装 JDK 22（如 Temurin，https://adoptium.net/ ），并将 `JAVA_HOME` 环境变量指向 JDK 22 的安装目录；
2. 打开终端进入 `backend` 目录：
   - Windows（PowerShell / cmd）：
     ```bash
     mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
     ```
   - macOS / Linux：
     ```bash
     ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
     ```
3. 首次运行会自动下载 Maven 发行版和项目依赖（需联网，耗时较长属正常）；
4. 验证后端：浏览器访问 `http://localhost:8080/api/health`，返回 `{"code":200,...}` 即成功；
5. 前端启动方式不变：`cd frontend` → `npm install` → `npm run dev`，访问 `http://localhost:5173`。

注意：

- dev 模式数据库在内存中，**重启后端后数据清空**，适合开发和演示；
- 上传的文件保存在 `backend/upload/`；
- 不设置 `DEEPSEEK_API_KEY` 时，AI 功能自动降级为规则引擎，其余功能不受影响。需要完整 AI 能力时设置环境变量后重启后端：
  - PowerShell：`$env:DEEPSEEK_API_KEY="你的密钥"`
  - cmd：`set DEEPSEEK_API_KEY=你的密钥`
- 本机已安装 Maven 3.9+ 的话，可以直接用 `mvn spring-boot:run -Dspring-boot.run.profiles=dev` 替代 `mvnw`。

### 方式二：本地 MySQL + Redis（与 Docker 环境一致）

1. 安装并启动 MySQL 8 和 Redis（Windows 可用原生安装包）；
2. 创建数据库并初始化表（任选其一）：
   - 在 MySQL 客户端执行：
     ```sql
     CREATE DATABASE life_archive DEFAULT CHARACTER SET utf8mb4;
     ```
   - 再执行 `sql/init.sql` 中的建表语句；
3. 确认 `backend/src/main/resources/application.yml` 中的默认配置与本机一致：MySQL 账号 `root/root`、地址 `localhost:3306`、Redis `localhost:6379`，不一致则修改该文件；
4. 进入 `backend` 目录运行 `mvnw.cmd spring-boot:run`（**不指定** `dev` profile，使用默认配置）；
5. 验证 `http://localhost:8080/api/health`，再按上文方式启动前端。

---

## 五、配置文件说明

| 文件 | 作用 |
|---|---|
| `docker-compose.yml` | 定义 mysql / redis / backend 三个服务 |
| `docker-compose.override.yml` | 注入微信、AI 密钥等环境变量 |
| `.env` | 存放密钥（微信 AppID/Secret、DeepSeek API Key） |
| `sql/init.sql` | 数据库初始化脚本（容器首次启动自动执行） |

> `.env` 已加入 `.gitignore`，如果通过 git 或压缩包交付且没有该文件，AI 功能会自动降级为规则引擎，其余功能不受影响。需要完整 AI 能力时，在项目根目录创建 `.env` 并填入：
>
> ```bash
> WX_APPID=你的微信小程序AppID
> WX_SECRET=你的微信小程序Secret
> DEEPSEEK_API_KEY=你的DeepSeek密钥
> ```

---

## 六、常见问题

**1. 端口被占用**

容器占用 3307（MySQL）、6379（Redis）、8080（后端）。若启动失败，先关闭占用这些端口的程序，或在 `docker-compose.yml` 中修改端口映射。

**2. 上传的图片无法显示**

确认后端容器正常运行，且浏览器通过 `http://localhost:8080` 访问（前端 Vite 已配置 `/upload` 代理）。

**3. 首次启动很慢**

首次构建需要联网下载 Maven 依赖和基础镜像，属于正常现象，后续启动会很快。

**4. 忘记管理员账号**

本项目没有预置管理员账号，直接使用首页「注册」功能创建账号即可。

---

## 七、目录结构

```text
life/
├── backend/          Spring Boot 后端（含 Dockerfile）
├── frontend/         Vue 3 前端
├── sql/              数据库初始化脚本
├── docker-compose.yml
├── start.bat         一键启动后端
└── stop.bat          停止后端
```
