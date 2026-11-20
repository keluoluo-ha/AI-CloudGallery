# AI-CloudGallery

> 基于 Spring Boot 的跨境电商素材中台，面向多平台出海业务，提供商品素材资产管理、四平台规格校验、AI 素材异步渲染、素材覆盖率与缺口分析等能力。

## 项目简介

AI-CloudGallery 是一个面向跨境电商场景的素材中台后端服务，采用主流 Java 技术栈构建。跨境店铺（亚马逊 / Shopee / TikTok Shop / Temu）在商品上架与运营中需要大量重复处理商品图片，本项目以「商品 + SKU + 素材」为核心数据模型，将图片作为可复用的素材资产统一管理：

- **素材资产化**：素材按类型（主图 / 场景图 / 白底图等）与商品 SKU 绑定，多空间隔离管理，支持审核、协同编辑
- **平台合规**：内置四平台素材规格校验引擎，按目标平台的尺寸/比例/格式要求校验素材
- **AI 生产线**：智能抠图、白底合规图、AI 场景图、规格适配等渲染能力以异步任务形式提供
- **经营洞察**：按平台计算素材覆盖率与缺口清单，帮助运营快速补齐上架素材

底座能力延续图片中台通用架构：多空间权限体系（Sa-Token）、WebSocket + Disruptor 实时协同编辑、ShardingSphere 分库分表、腾讯云 COS 对象存储、Redis + Caffeine 多级缓存。

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
| 阿里云 AI | - | AI 图像处理（扩图/抠图/场景图） |
| Knife4j | 4.4.0 | API 文档生成 |
| Hutool | 5.8.26 | Java 工具类库 |
| Caffeine | 3.1.8 | 本地缓存（二级缓存） |
| JMeter | 5.x | 缓存与接口压测 |
| Lombok | 1.18.34 | 代码简化 |

## 项目结构

```
com.hhk.picturebacked
├── annotation          # 自定义注解（权限校验等）
├── aop                 # AOP 切面（权限拦截）
├── api                 # 第三方 API 对接
│   ├── Aliyunimage     # 阿里云 AI（扩图/抠图/场景图渲染）
│   └── imageSearch     # 搜狗图片搜索
├── auth                # Sa-Token 权限认证模块
├── commom              # 公共封装（统一响应、分页、工具）
├── config              # 项目配置（MyBatisPlus、COS、JSON）
├── constant            # 常量定义
├── consumer            # 异步任务消费者（AI 素材渲染）
├── controller          # 控制层（REST API 接口）
├── exception           # 全局异常处理
├── manager             # 业务管理器
│   ├── sharding        # 动态分片管理
│   ├── upload          # 素材上传策略（文件/URL）
│   └── websocket       # WebSocket + Disruptor 实时协作
├── mapper              # 数据访问层（MyBatis Plus）
├── model               # 数据模型
│   ├── dto             # 数据传输对象
│   ├── entity          # 数据库实体（User/Picture/Space/Product/AiTask 等）
│   ├── enums           # 枚举定义（素材类型/平台/规格/AI 任务等）
│   └── vo              # 视图对象
├── service             # 业务逻辑层
│   └── impl            # 业务实现
└── utils               # 工具类
```

## 核心功能

### 用户模块
- 用户注册 / 登录 / 注销
- 用户信息管理与权限控制

### 商品与 SKU 模块
- 商品管理（创建 / 编辑 / 查询 / 状态流转）
- 商品 SKU 管理与素材绑定，图片可挂载到具体商品

### 素材模块
- 素材上传（支持文件上传与 URL 抓取），按素材类型（主图 / 场景图 / 白底图等）归类
- 素材查询、编辑、删除与审核流程
- 以图搜图（搜狗图片搜索）、颜色相似素材搜索
- AI 智能扩图（阿里云 AI）
- 素材浏览量异步削峰计数

### 平台规格校验
- 内置亚马逊 / Shopee / TikTok Shop / Temu 四平台素材规格库（尺寸 / 比例 / 格式）
- 规格校验引擎：对指定素材按目标平台输出校验结果

### AI 素材渲染
- 异步渲染任务：智能抠图（透明底）、白底合规图、AI 场景图、规格适配（本地尺寸处理）
- 任务全生命周期管理：创建、排队、执行、状态查询，配合消息消费者异步处理

### 空间模块
- 空间创建与管理（公有/私有）
- 空间级别与容量控制
- 空间成员管理与权限分配
- 空间数据分析（分类/标签/大小/使用率/用户/排名）

### 平台素材分析
- 平台素材覆盖率分析：按四平台齐备规则计算各商品素材齐备率
- 素材缺口分析：输出缺失素材类型清单，指导补齐上架素材

### 实时协作模块
- WebSocket 长连接通信
- Disruptor 高性能异步事件处理
- 素材实时协同编辑

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

2. 执行初始化脚本：
- `src/main/resources/db/01_ecommerce_schema.sql`（电商化基础设施：商品/SKU 数据层）
- `src/main/resources/db/02_ai_task_schema.sql`（AI 渲染任务表）

3. 修改 `application.yml` 中的数据库配置：
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

项目按模块分节点提交，清晰呈现开发演进过程：

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
| 13 | 电商化基础设施与商品数据层 - 商品实体、Product 数据层及电商库表 |
| 14 | 商品 SKU 管理与图片绑定商品 - SKU 素材挂载、枚举接口与查询能力 |
| 15 | 四平台素材规格校验引擎 - 平台规格库、规格校验接口与结果视图 |
| 16 | AI 素材异步渲染任务 - 抠图/白底/场景图/规格适配任务与异步消费者 |
| 17 | 图片浏览量异步削峰计数模块 |
| 18 | 素材平台覆盖率与缺口分析复用 - 覆盖率/缺口分析接口与平台齐备规则 |
| 19 | 二级缓存压测 - JMeter 缓存基准测试脚本 |

## 项目亮点

- **电商化素材模型**：以「商品 + SKU + 素材类型」组织图片资产，贴合跨境电商上架与运营流程
- **四平台规格校验**：内置亚马逊 / Shopee / TikTok Shop / Temu 规格库，上架前即完成素材合规校验
- **AI 素材生产线**：抠图、白底合规、场景图、规格适配全部走异步任务，削峰填谷不阻塞主流程
- **覆盖率与缺口分析**：按平台齐备规则计算素材齐备率并输出缺口清单，运营补料有据可依
- **分库分表**：基于 ShardingSphere 实现素材表按空间 ID 动态分片，支撑海量数据存储
- **高性能异步处理**：WebSocket 协同编辑场景引入 Disruptor 无锁队列，浏览量计数异步削峰
- **多级缓存与压测**：Caffeine + Redis 二级缓存，配套 JMeter 基准压测脚本
- **Sa-Token 权限体系**：细粒度空间权限控制，支持角色级别的访问管理
- **多级存储策略**：支持本地文件与 URL 两种素材上传方式

## 开源协议

[MIT License](LICENSE)

---

> 项目作者：[@keluoluo-ha](https://github.com/keluoluo-ha)
