# LifeArchive API接口文档


## 基础信息


接口前缀：

/api


数据格式：

JSON



---

# 用户模块


## 用户注册


接口：

POST

/api/user/register



请求：

```json
{
 "username":"test",
 "password":"123456",
 "email":"xxx@qq.com"
}

返回：

{
 "code":200,
 "message":"success"
}
用户登录

接口：

POST

/api/user/login

请求：

{
 "username":"test",
 "password":"123456"
}

返回：

{
 "token":"xxxxx",
 "userId":1
}
记忆管理模块
上传记忆

接口：

POST

/api/memory/upload

参数：

multipart/form-data

字段：

file:

上传文件

title:

标题

description:

描述

返回：

{
"id":100,
"url":"/upload/a.jpg"
}
获取记忆列表

GET

/api/memory/list

返回：

[
 {
  "id":1,
  "title":"大学旅行",
  "date":"2023-05-20",
  "type":"photo"
 }
]
AI分析模块
分析图片

POST

/api/ai/analyze/image

请求：

{
"memoryId":1
}

返回：

{
"location":"杭州西湖",
"emotion":"开心",
"tags":[
"旅行",
"朋友"
]
}
时间轴模块
获取时间轴

GET

/api/timeline/list

返回：

[
{
"year":2023,
"title":"大学旅行",
"description":"第一次旅行经历"
}
]
AI助手模块
AI问答

POST

/api/assistant/chat

请求：

{
"question":
"我第一次旅行是什么时候"
}

返回：

{
"answer":
"你的第一次旅行是在2023年5月"
}
回忆生成模块
生成故事

POST

/api/story/generate

请求：

{
"theme":
"我的大学生活"
}

返回：

{
"content":
"你的大学故事..."
}
接口规范

所有接口必须：

使用RESTful设计
返回统一格式：
{
"code":200,
"message":"",
"data":{}
}
错误必须返回明确提示。

---

## 三个文件配合关系

最终你的项目结构建议：


LifeArchive
│
├── CLAUDE.md
│
├── PROJECT_PLAN.md
│
├── API_DOC.md
│
├── backend
│
├── frontend
│
├── sql
│
└── README.md


开发流程：


Claude Code读取 CLAUDE.md
↓
查看 PROJECT_PLAN.md
↓
确认当前阶段
↓
查看 API_DOC.md
↓
开发代码
↓
更新 PROJECT_PLAN.md
↓
等待你的确认