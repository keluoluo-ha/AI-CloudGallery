# AI-CloudGallery

> 基于 Spring Boot + Vue3 的智能化云图库平台，支持多用户空间管理、图片协同编辑、AI 智能扩图等能力。

## 项目简介

AI-CloudGallery 是一个功能完善的云图库后端服务，采用主流 Java 技术栈构建，面向图片存储、管理与协作场景。项目支持多用户分空间管理图片资源，集成 WebSocket 实现图片实时协同编辑，并引入 Disruptor 高性能异步处理架构。同时对接阿里云 AI 能力，支持图片智能扩图等 AI 场景。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.6 | Web 应用框架 |
| MyBatis Plus | 3.5.3.1 | ORM 持久层框架 |
| MySQL | 8.x | 关系型数据库 |
| Redis | 5.x+ | 缓存与会话存储 |
| Sa-Token | 1.39.0 | 权限认证框架 |
| ShardingSphere | 5.2.0 | 分库分表中间件 |
| WebSocket | - | 实时通信协议 |
| Disruptor | 3.4.2 | 高性能异步队列 |
| 腾讯云 COS | 5.6.227 | 对象存储服务 |
| 阿里云 AI | - | AI 扩图 API |
| Knife4j | 4.4.0 | API 文档生成 |
| Hutool | 5.8.26 | Java 工具类库 |
| Caffeine | 3.1.8 | 本地缓存 |
| Lombok | 1.18.34 | 代码简化 |

## 项目结构

```
com.hhk.picturebacked
├── annotation          # 自定义注解（权限校验等）
├── aop                 # AOP 切面（权限拦截）
├── api                 # 第三方 API 对接
│   ├── Aliyunimage     # 阿里云 AI 扩图
│   └── imageSearch     # 搜狗图片搜索
├── auth                # Sa-Token 权限认证模块
├── commom              # 公共封装（统一响应、分页、工具）
├── config              # 项目配置（MyBatisPlus、COS、JSON）
├── constant            # 常量定义
├── controller          # 控制层（REST API 接口）
├── exception           # 全局异常处理
├── manager             # 业务管理器
│   ├── sharding        # 动态分片管理
│   ├── upload          # 图片上传策略（文件/URL）
│   └── websocket       # WebSocket + Disruptor 实时协作
├── mapper              # 数据访问层（MyBatis Plus）
├── model               # 数据模型
│   ├── dto             # 数据传输对象
│   ├── entity          # 数据库实体
│   ├── enums           # 枚举定义
│   └── vo              # 视图对象
├── service             # 业务逻辑层
│   └── impl            # 业务实现
└── utils               # 工具类
```

## 核心功能

### 用户模块
- 用户注册 / 登录 / 注销
- 用户信息管理与权限控制

### 图片模块
- 图片上传（支持文件上传与 URL 抓取）
- 图片查询、编辑、删除
- 图片审核流程
- 以图搜图（搜狗图片搜索）
- 颜色相似图片搜索
- AI 智能扩图（阿里云 AI）

### 空间模块
- 空间创建与管理（公有/私有）
- 空间级别与容量控制
- 空间成员管理与权限分配
- 空间数据分析（分类/标签/大小/使用率/用户/排名）

### 实时协作模块
- WebSocket 长连接通信
- Disruptor 高性能异步事件处理
- 图片实时协同编辑

## 快速启动

### 环境准备

- JDK 11+
- Maven 3.6+
- MySQL 8.0+
- Redis 5.0+

### 数据库初始化

1. 创建数据库：
```sql
CREATE DATABASE yu_picture DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改 `application.yml` 中的数据库配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/yu_picture
    username: root
    password: 你的密码
```

### 配置 Redis

确保 Redis 服务已启动，并修改配置：
```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
```

### 配置腾讯云 COS（可选）

在 `application.yml` 中配置对象存储：
```yaml
cos:
  client:
    host: https://你的bucket.myqcloud.com
    secretId: ${SECRETID}
    secretKey: ${SECRETKEY}
    region: ${REGION}
    bucket: 你的bucket名
```

### 配置阿里云 AI（可选）

```yaml
aliYunAi:
  apiKey: ${SECRETKEY}
```

### 运行项目

```bash
# 克隆项目
git clone https://github.com/keluoluo-ha/AI-CloudGallery.git

# 进入项目目录
cd AI-CloudGallery

# 编译运行
mvn spring-boot:run
```

项目启动后访问：
- API 接口文档：`http://localhost:8123/api/doc.html`
- 基础路径：`http://localhost:8123/api`

## 接口文档

项目集成 Knife4j 自动生成 API 文档，启动后访问：

```
http://localhost:8123/api/doc.html
```

## 分节提交记录

项目按模块分 12 个节点提交，清晰呈现开发演进过程：

| 序号 | Commit 内容 |
|------|-------------|
| 1 | 初始化项目 - SpringBoot 脚手架、pom 依赖及配置文件 |
| 2 | 新增公共基础模块 - 统一响应、异常处理、常量定义 |
| 3 | 新增配置层 - SpringBoot 配置、COS 客户端、AOP 权限拦截注解 |
| 4 | 新增数据模型层 - 实体类及枚举定义 |
| 5 | 新增用户 DTO 层 - 用户注册/登录/查询/更新请求对象 |
| 6 | 新增图片 DTO 层 - 图片上传/查询/编辑/审核及 AI 扩图请求对象 |
| 7 | 新增空间 DTO 层 - 空间管理/分析/成员请求对象 |
| 8 | 新增 VO 视图层 - 展示对象及空间分析响应 |
| 9 | 新增 Mapper 数据访问层及 Sa-Token 权限认证模块 |
| 10 | 新增第三方 API 对接 - 阿里云 AI 扩图及搜狗图片搜索接口 |
| 11 | 新增 Manager 管理层 - COS 存储、图片上传、WebSocket 实时协作(Disruptor 异步)及工具类 |
| 12 | 新增 Service 业务层及 Controller 接口层 - 完成用户/图片/空间/空间成员/分析完整 CRUD 及业务逻辑 |

## 项目亮点

- **分库分表**：基于 ShardingSphere 实现图片表按空间 ID 动态分片，支撑海量数据存储
- **高性能异步处理**：WebSocket 协同编辑场景引入 Disruptor 无锁队列，提升并发处理能力
- **Sa-Token 权限体系**：细粒度空间权限控制，支持角色级别的访问管理
- **AI 能力集成**：对接阿里云 AI 实现图片智能扩图
- **多级存储策略**：支持本地文件与 URL 两种图片上传方式

## 开源协议

[MIT License](LICENSE)

---

> 项目作者：[@keluoluo-ha](https://github.com/keluoluo-ha)
