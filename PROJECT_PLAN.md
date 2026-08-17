# LifeArchive 项目开发计划


## 一、项目基本信息


项目名称：

LifeArchive


项目定位：

基于AI的个人数字记忆管理与人生档案平台。


项目目标：

通过AI技术分析用户照片、视频、文字等个人数据，自动构建个人数字档案，实现：

- 记忆管理
- 智能检索
- 时间轴生成
- AI故事生成


---

# 二、技术架构


## 前端

Vue3

TypeScript

Vite

Element Plus

ECharts



## 后端

Spring Boot 3

Java 17

MyBatis Plus

MySQL

Redis



## AI服务

大语言模型API

RAG

向量数据库



---

# 三、开发进度


## Phase 0：需求分析与架构设计

状态：

✅ 已完成


完成内容：

- 系统功能分析
- 技术架构设计
- 数据库设计
- 项目目录规划



---

## Phase 1：基础环境搭建


状态：

✅ 已完成


目标：

完成前后端基础环境。


任务：

后端：

- ✅ Spring Boot 3.3.0 初始化
- ✅ MySQL 数据库连接配置
- ✅ MyBatis Plus 3.5.7 配置
- ✅ Redis 配置
- ✅ JWT 依赖引入
- ✅ 统一响应结果封装 (Result)
- ✅ 跨域配置 (CORS)
- ✅ 文件上传配置
- ✅ Maven Wrapper 配置
- ✅ 健康检查接口 (/api/health)


前端：

- ✅ Vue 3 + Vite 初始化
- ✅ TypeScript 配置
- ✅ 路由配置 (Vue Router 4)
- ✅ Element Plus UI 框架配置
- ✅ Axios 请求封装与拦截器
- ✅ ECharts 依赖引入
- ✅ 登录页面 (Login.vue)
- ✅ 首页仪表盘 (Home.vue)
- ✅ 开发代理配置 (Proxy → Backend)


完成标准：

✅ 前后端均可正常构建。



---

## Phase 2：用户认证模块


状态：

✅ 已完成


任务：

后端：

- ✅ 用户表设计（user 表，含用户名/密码/邮箱/昵称）
- ✅ 注册接口 (POST /api/user/register)
- ✅ 登录接口 (POST /api/user/login)
- ✅ JWT Token 认证
- ✅ 认证拦截器 (JwtInterceptor)
- ✅ 全局异常处理器
- ✅ 用户信息接口 (GET /api/user/info)


前端：

- ✅ 登录页面 (Login.vue) - 对接真实 API
- ✅ 注册页面 (Register.vue) - 表单验证
- ✅ 路由守卫 - 未登录拦截
- ✅ Token 存储与认证工具



---

## Phase 3：数字记忆管理


状态：

✅ 已完成


功能：

后端：

- ✅ 文件上传（图片/视频）
- ✅ 文本记忆创建
- ✅ 记忆列表（分页 + 类型筛选）
- ✅ 记忆详情
- ✅ 记忆删除
- ✅ 记忆统计接口
- ✅ 文件本地存储


前端：

- ✅ 侧边栏导航布局 (MainLayout.vue)
- ✅ 记忆列表页 (Memories.vue) - 卡片展示 + 筛选 + 分页
- ✅ 上传记忆页 (Upload.vue) - 文件上传 + 写日记
- ✅ 记忆详情弹窗
- ✅ 首页统计卡片对接 API


---

## Phase 4：AI记忆分析


状态：

✅ 已完成


功能：

后端：

- ✅ LLM 客户端 (LLMClient) - 支持 OpenAI 兼容接口
- ✅ AI 分析服务 (AIService) - 图片分析 / 文本分析 / 自动分析
- ✅ 规则引擎回退 (RuleBasedAnalyzer) - LLM不可用时自动切换
- ✅ 图片分析：地点识别、情绪分析、事件类型、标签提取、摘要生成
- ✅ 文本分析：情绪识别、关键词提取、标签分类、内容摘要
- ✅ AIController - 3个API端点


前端：

- ✅ 记忆卡片「AI分析」按钮（悬浮显示）
- ✅ 分析加载状态
- ✅ 详情弹窗 AI 分析面板（绿色面板展示结果）
- ✅ 未分析记忆提示分析按钮
- ✅ 已分析记忆左上角「AI已分析」标签
- ✅ 分析结果实时更新到卡片和详情


---

## Phase 5：人生时间轴


状态：

✅ 已完成


功能：

后端：

- ✅ TimelineService - 按年份分组组织记忆数据
- ✅ 全量时间轴 + 按年份筛选
- ✅ 事件节点字段：time/title/description/image/tags/emotion/location
- ✅ TimelineController - 2个REST端点


前端：

- ✅ Timeline.vue - 垂直时间轴页面
- ✅ 年份筛选器（全部/具体年份）
- ✅ 时间轴节点 + 事件卡片（图片/描述/标签/地点/情绪）
- ✅ 点击查看事件详情弹窗
- ✅ 路由守卫 + 侧边栏时间轴入口


根据用户数据生成：

- ✅ 年份节点（按年份分组，倒序排列）
- ✅ 事件分类（按类型标签：照片/视频/日记/旅行/学习）
- ✅ 记忆详情（弹窗展示地点/情绪/AI摘要/标签）



---

## Phase 6：RAG智能助手


状态：

✅ 已完成


功能：

后端：

- ✅ VectorStore - 内存向量存储（bigram分词 + 词频向量 + 余弦相似度）
- ✅ KnowledgeBaseService - 知识库索引构建（记忆→向量文档）
- ✅ RAGService - 完整 RAG 流水线（检索→增强→生成）
- ✅ LLM 模式：调用大模型生成自然语言回答
- ✅ 模板模式（无LLM）：关键词检索 + 格式化回答 + 来源标注
- ✅ AssistantController - 2个API端点


前端：

- ✅ Assistant.vue - 对话式聊天界面
- ✅ 欢迎页 + 引导问题建议
- ✅ 对话历史 localStorage 持久化
- ✅ 消息气泡（用户/AI 双色区分）
- ✅ 思考中加载动画
- ✅ 来源记忆标注（参考记忆标签）
- ✅ 侧边栏「AI助手」入口


RAG流程：

用户提问 → bigram分词 → 向量检索（余弦相似度 top5）
→ LLM生成回答 / 模板生成回答
→ 返回 { answer, sources }


---

## Phase 7：AI回忆生成


状态：

✅ 已完成


功能：

后端：

- ✅ StoryService - 主题选择 + 向量检索相关记忆 + 文章生成
- ✅ LLM 模式：大模型撰写自然叙事文章（Markdown）
- ✅ 模板模式（无LLM）：按年份分章 + 记忆条目 + 情绪统计 + 数据总览
- ✅ 8个预设主题 + 自定义主题
- ✅ StoryController - API端点


前端：

- ✅ Story.vue - 主题选择面板（6个预设+自定义）
- ✅ 生成结果 Markdown 渲染展示
- ✅ 文章元信息（字数/参考记忆数）
- ✅ 复制文章功能


生成：

- ✅ 人生故事（按年份分章节、第一人称叙事）
- ✅ 阶段总结（时间跨度、情绪统计、地点统计）
- ✅ 回忆文章（Markdown格式、带章节标题）



---

## Phase 8：数据可视化


状态：

✅ 已完成


展示：

后端：

- ✅ StatsService - 6维度数据聚合（年度/月度/地点/情绪/类型/最近）
- ✅ StatsController - 数据面板API


前端 ECharts 可视化：

- ✅ 年度记忆数量趋势（柱状图 + 折线混合图）
- ✅ 月度活跃度分析（渐变面积图）
- ✅ 地点分布 Top15（横向柱状图）
- ✅ 情绪分布分析（玫瑰图，7种情绪色）
- ✅ 记忆类型分布（环形图）
- ✅ 概览卡片（记忆总数/涉及地点/跨越年度/活跃月份）
- ✅ 最近记忆列表


全项目完成：

- ✅ 七大核心模块全部实现
- ✅ 后端 39 个 Java 源文件，BUILD SUCCESS
- ✅ 前端 9 页面 + 1 布局，production build 通过
- ✅ 项目可展示、可部署



---

# 四、开发记录


## 日期：2026-07-16

## 完成内容：Phase 1 + Phase 2 基础环境搭建与用户认证

## 修改文件：
**Phase 1 - 基础环境：**
- backend/pom.xml - Spring Boot 3.3.0 项目配置
- backend/src/main/java/com/lifearchive/LifeArchiveApplication.java - 启动类
- backend/src/main/java/com/lifearchive/common/Result.java - 统一响应封装
- backend/src/main/java/com/lifearchive/config/ - 四大配置类
- backend/src/main/java/com/lifearchive/controller/HealthController.java - 健康检查
- backend/src/main/resources/application.yml - 主配置
- frontend/ - Vue 3 + Vite 完整前端项目

**Phase 2 - 用户认证：**
- sql/init.sql - 数据库初始化（user + memory 表）
- backend/.../entity/User.java + Memory.java - 实体类
- backend/.../common/JwtUtils.java - JWT 工具
- backend/.../common/GlobalExceptionHandler.java - 全局异常处理
- backend/.../common/MetaObjectHandlerConfig.java - 自动填充
- backend/.../interceptor/JwtInterceptor.java - 认证拦截器
- backend/.../mapper/UserMapper.java - 用户 Mapper
- backend/.../service/UserService.java + impl - 用户服务
- backend/.../controller/UserController.java - 用户接口
- frontend/src/api/user.ts - 用户 API 封装
- frontend/src/utils/auth.ts - 前端认证工具
- frontend/src/views/Login.vue - 登录页（对接API）
- frontend/src/views/Register.vue - 注册页
- frontend/src/router/index.ts - 路由守卫

**Phase 3 - 数字记忆管理：** (略)
**Phase 4 - AI记忆分析：**
- backend/.../service/ai/LLMClient.java - LLM API客户端
- backend/.../service/ai/AIService.java + impl - AI分析服务
- backend/.../service/ai/rules/RuleBasedAnalyzer.java - 规则引擎回退
- backend/.../controller/AIController.java - AI分析端点（3个API）
- frontend/src/api/ai.ts + frontend/.../Memories.vue - 分析按钮+结果面板

**Phase 5 - 人生时间轴：**
- backend/.../service/TimelineService.java + impl - 时间轴服务
- backend/.../controller/TimelineController.java - 时间轴端点
- frontend/src/api/timeline.ts + views/Timeline.vue - 垂直时间轴页面
- frontend/src/router/index.ts - 新增 /timeline 路由

## 遇到问题：
- Spring Boot 3 + JDK 22 兼容性 → 验证通过
- TypeScript 6.x baseUrl deprecated → ignoreDeprecations 兼容
- JWT 密钥需256+bits → HS256 兼容

**Phase 6 - RAG智能助手：** (略)
**Phase 7 - AI回忆生成：**
- backend/.../service/StoryService.java + impl - 故事生成服务
- backend/.../controller/StoryController.java - 故事生成API
- frontend/src/views/Story.vue - 主题选择 + Markdown文章展示
- frontend/src/layout/MainLayout.vue - 侧边栏+回忆生成入口

**Phase 8 - 数据可视化：**
- backend/.../service/StatsService.java + impl - 6维度聚合统计
- backend/.../controller/StatsController.java - 数据面板API
- frontend/src/views/Stats.vue - ECharts可视化（5个图表+4个卡片）

## 🎉 全部 8 个 Phase 开发完成！




---

# 五、开发注意事项


1. 不允许跳过阶段。


2. 修改已有功能必须说明原因。


3. 新功能开发前必须检查已有代码。


4. 保持数据库设计稳定。


5. 每完成一个阶段，需要等待负责人确认。
