# LifeArchive 微信小程序翻版迁移计划

> 目标：将现有 Web 版 LifeArchive（Vue3 + Spring Boot 3 + MySQL/Redis）翻版为微信小程序。
> 原则：后端尽量复用，前端换皮但复用业务逻辑；用 uni-app (Vue3) 最大化复用现有 Vue 经验与原 API 封装。
> 适用读者：Lv（已做过微信小程序，熟悉登录与审核流程）

---

## 0. 现状盘点（已核对真实代码）

### Web 前端（frontend/src）
| 页面 | 文件 | 职责 |
|---|---|---|
| 登录 | `views/Login.vue` | 用户名密码登录 |
| 注册 | `views/Register.vue` | 注册账号 |
| 首页 | `views/Home.vue` | 概览 |
| 上传 | `views/Upload.vue` | 照片/视频/日记上传 |
| 记忆列表 | `views/Memories.vue` | 卡片列表 + 详情/编辑弹窗（含 eventTime 时间字段） |
| 时间轴 | `views/Timeline.vue` | 按时间线展示 |
| 智能助手 | `views/Assistant.vue` | RAG 对话 |
| 故事生成 | `views/Story.vue` | 选主题 → marked 渲染文章（已重写修复） |
| 数据统计 | `views/Stats.vue` | ECharts 可视化 |

API 封装：`src/api/` 下 `request.ts`(Axios 封装) + `user/ai/timeline/story/stats/assistant/memory.ts`。

### 后端（Spring Boot，docker 部署）
- Controller：`UserController`(register/login) / `MemoryController`(CRUD+上传+更新eventTime) / `StoryController` / `AIController` / `TimelineController` / `AssistantController` / `StatsController` / `HealthController`
- 鉴权：JWT 拦截器从 `request.getAttribute("userId")` 取用户；登录返回 `data.token`
- AI：`LLMClient`(OpenAI兼容) + `RuleBasedAnalyzer` 降级；RAG 自研内存 `VectorStore`（bigram+余弦）
- 部署：docker-compose（mysql:3307 / redis:6379 / backend:8080，多阶段构建 Java22）

**结论**：后端是纯 API 服务，与客户端无关，约 80% 可复用；唯一算新功能的是**微信登录对接**。

---

## 1. 技术选型

| 层 | 选型 | 理由 |
|---|---|---|
| 小程序框架 | **uni-app (Vue3 版)** | 复用你的 Vue 功底与现有 API 层；一套代码可同时出 H5/小程序/App |
| 组件库 | `uni-ui` / `uView` | 替代 Element Plus |
| Markdown 渲染 | `mp-html` | 替代 marked（故事/AI 分析富文本） |
| 图表 | `ec-canvas`(ECharts) 或 `ucharts` | 替代 Web 的 ECharts |
| 后端 | **不变**（Spring Boot） | 仅新增微信登录模块 |
| 请求封装 | `uni.request` 自封装 `request.js` | 替代 Axios，逻辑一致 |

---

## 2. 后端改造清单（文件级）

> 改完需 `docker compose up -d --build` 重建镜像（项目已验证的多阶段构建）。

### 2.1 微信登录（核心新功能）
| 文件 | 改动 |
|---|---|
| 新增 `controller/WxLoginController.java` | `POST /api/wx/login`：接收 `{code}` → 调微信 `jscode2session` → 拿 openid → 查/建用户 → 发 JWT 返回 `data.token` |
| 新增 `service/WxAuthService.java` | 封装 code2Session 调用、openid→用户映射、自动注册逻辑 |
| `entity/User.java` | 新增 `private String openid;` 字段（+ `@TableField`） |
| `mapper/UserMapper.java` | 对应字段（MyBatis Plus 自动映射，通常无需手写） |
| `sql/init.sql` | `user` 表加 `openid VARCHAR(64) UNIQUE` 列 |
| `application.yml` | 新增 `wx.appid` / `wx.secret`（**用环境变量注入，勿硬编码进 git**） |
| 依赖 `pom.xml` | 推荐引入 `weixin-java-miniapp`(WxJava) SDK，简化 code2Session 与后续能力 |

### 2.2 文件上传对接
- `MemoryController` 的 `MultipartFile` 参数**不用改**；小程序端用 `wx.uploadFile`（`filePath`+`name`+`formData` 带 token）。
- 确认上传字段名与 Web 端一致（如 `file`），避免小程序侧对不上。

### 2.3 跨域与域名
- 生产：微信后台配置 **HTTPS 合法域名**（后端需有公网 HTTPS 地址）。
- 本地开发：微信开发者工具「不校验合法域名」勾选即可，后端 `restart: unless-stopped` 照常。

### 2.4 （可选）RAG 持久化
- 现有内存 `VectorStore` 重启即丢。建议顺手换成 pgvector / RedisSearch，小程序与 Web 端同时受益。非 MVP 必需。

---

## 3. 小程序前端结构（uni-app）

```
miniapp/
├── pages/
│   ├── auth/auth.vue            # 微信授权登录（替代 Web 的 Login/Register）
│   ├── home/home.vue            # 首页概览
│   ├── upload/upload.vue        # wx.chooseMedia 上传
│   ├── memories/memories.vue    # 记忆列表 + 详情/编辑（复用 Web 的弹窗逻辑）
│   ├── timeline/timeline.vue    # 时间轴
│   ├── assistant/assistant.vue  # RAG 对话
│   ├── story/story.vue          # 主题选择 + mp-html 文章
│   └── stats/stats.vue          # ec-canvas 图表
├── components/
│   ├── memory-card.vue          # 记忆卡片
│   ├── edit-dialog.vue          # 编辑弹窗（含 eventTime 时间字段）
│   ├── markdown-view.vue        # 封装 mp-html
│   └── chart-box.vue            # 封装 ec-canvas
├── utils/
│   ├── request.js               # uni.request 封装（token、res.data 提取，对齐 Web request.ts）
│   └── format.js                # 时间格式化 + parseTags（对齐 Web 工具）
├── store/ 或 composables/
│   └── user.js                  # token/登录态，uni.setStorageSync 持久化
├── static/
├── App.vue / main.js / pages.json (tabBar + 分包)
└── manifest.json                # 配 AppID
```

### API 层迁移
将 Web 的 `src/api/*.ts` **逐函数改写为 `uni.request` 版**，保持函数名、入参、返回结构一致，业务页面逻辑可直接套用：
- `user.ts` → 删登录/注册，新增 `wxLogin(code)`；保留 token 存储逻辑
- `memory.ts` → `listMemoriesApi` / `getMemoryDetailApi` / `updateMemoryApi`（eventTime 已支持）/ `uploadImageApi`(wx.uploadFile)
- `story.ts` / `ai.ts` / `timeline.ts` / `stats.ts` / `assistant.ts` → 原样平移

---

## 4. 分阶段实施路线

| 阶段 | 内容 | 关键文件 | 工作量(人天) | 验收标准 |
|---|---|---|---|---|
| **0 准备** | 注册小程序、拿 AppID、配微信后台、后端加微信配置项 | `application.yml`, `pom.xml` | 0.5 | 本地能跑通微信开发者工具 |
| **1 微信登录** | 后端 code2Session + openid + JWT；curl 验证拿 token | `WxLoginController`, `WxAuthService`, `User.java`, `sql/init.sql` | 1.5 | `POST /api/wx/login` 返回 200+token |
| **2 小程序骨架** | uni-app 工程、tabBar、request 封装、登录态 | `miniapp/` 脚手架, `utils/request.js`, `store/user.js` | 1.5 | 授权→拿到 token→首页能显示 |
| **3 记忆闭环** | 列表 + 详情/编辑（eventTime）+ 上传 | `memories.vue`, `edit-dialog.vue`, `upload.vue` | 2.5 | 上传图片→列表可见→编辑时间生效（对齐 Web 已修的 bug） |
| **4 时间轴+故事** | 时间线布局 + mp-html 渲染文章 | `timeline.vue`, `story.vue`, `markdown-view.vue` | 2 | 故事文章正常换行显示（Web 曾因 grid 布局空白，小程序无此问题） |
| **5 智能助手** | 聊天 UI + 接口对接（流式可选） | `assistant.vue` | 1.5 | 能对话返回结果 |
| **6 数据统计** | ec-canvas 图表 | `stats.vue`, `chart-box.vue` | 1.5 | 图表正常渲染 |
| **7 优化** | RAG 持久化(可选)、OSS 上传、分包、包体积 | `VectorStore`, 上传服务 | 2（可选） | 主包<2MB、图片走 OSS |
| **8 上线** | 真机调试 + 提交审核 | — | 1 | 审核通过 |

**MVP（阶段 0–3）≈ 6 人天**；完整版（0–8）≈ 14 人天。一人推进 2–4 周可出完整版。

---

## 5. 主要风险与对策

| 风险 | 对策 |
|---|---|
| 包体积（主包 2MB） | 图片/视频走 OSS/COS（项目已预留 `fileUrl` 字段）；使用分包加载 |
| 流式输出 | Assistant 用 SSE；小程序用 `uni.request` 的 `onChunkReceived`（需 2.19+ 基础库）或轮询 |
| Markdown 渲染 | `mp-html` 组件，对齐 Web 已修的换行逻辑 |
| 图表性能 | `ec-canvas` 按需引入，避免全量 ECharts |
| 微信审核 | AI 生成内容需标注"由AI生成"；内容/AI 类目需资质 |
| 登录态过期 | token 存 `uni.setStorageSync`，过期后重新 `wx.login` 静默刷新 |
| 后端未重建 | 每次改 Java 后务必 `docker compose up -d --build`（经验教训：曾因未重建镜像导致修复无效） |

---

## 6. 验证方式

- **后端**：`curl -X POST /api/wx/login -d '{"code":"...模拟"}'` 拿 token → 带 token 调 `/api/memory?userId=...` 验证列表。
- **小程序**：微信开发者工具 + 真机预览；重点回归 Web 已修的两类问题——① 编辑保存不再 500（eventTime 解析）；② 预览弹窗时间与信息完整显示。

---

## 7. 建议的下��步

1. 你确认框架用 **uni-app (Vue3)**（默认推荐）。
2. 我可先落地**阶段 1（后端微信登录）**，给出具体代码改动。
3. 或先搭 **miniapp 脚手架 + request 封装**，把登录态跑通。
4. 需要的话，我也可以把本计划拆成 `TaskCreate` 任务清单逐项跟踪。

> 备注：Web 端已修复的 Story.vue 空白（布局问题）、Memories 编辑保存 500（eventTime/Map 解析）、预览信息不全等问题，小程序端因 UI 重写不会继承这些 bug，但业务逻辑（eventTime 字段、API 结构）请保持对齐。
