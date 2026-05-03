# 民宿综合管理平台（Spring Boot + Spring AI + Vue）

本项目为全功能民宿综合管理平台，集成了 **Spring AI** 实现智能推荐和对话功能。包含用户端和管理端，提供完整的民宿预订、管理、AI 推荐、智能客服等功能。

## ✨ 核心亮点

- 🤖 **AI 智能旅行规划** - 基于 Spring AI + OpenAI，自动生成个性化旅行方案
- 🎯 **AI 智能订单生成** - Function Calling 技术自动查询民宿、检查可用性、生成预订方案
- 💬 **AI 智能客服** - 自然语言对话查询订单、民宿信息，智能回复建议
- 📅 **房间日历管理** - 可视化日历展示房间预订状态，防止冲突
- 📝 **订单评价系统** - 用户可对已完成订单进行评价和评分
- 🖼️ **图片上传管理** - 支持民宿图片上传和展示
- 🏷️ **民宿类型分类** - 按类型管理和筛选民宿

## 主要功能

### 用户端
- ✅ **用户认证**
  - 注册/登录（默认客户角色）
  - Token 验证、刷新、登出
  - 个人信息管理

- ✅ **民宿预订**
  - 民宿列表浏览（支持按类型筛选）
  - 民宿详情查看（含图片）
  - 在线预订（实时房间可用性检查）
  - 房间日历可视化
  - 预订冲突检测

- ✅ **订单管理**
  - 我的订单列表
  - 订单状态跟踪
  - 订单评价和评分

- ✅ **AI 功能**
  - 🤖 AI 旅行规划（智能推荐民宿、路线、活动）
  - 🎯 AI 智能生成订单（自动分析需求，生成多个预订方案）
  - 💬 AI 智能客服对话（查询订单、民宿信息）

- ✅ **旅行资源**
  - 旅游路线查看
  - 游玩项目浏览

- ✅ **客户服务**
  - 发送咨询消息
  - 查看管理员回复

### 管理员端
- ✅ **用户管理**
  - 用户列表（增删改查）
  - 角色分配（客户/管理员）
  - 用户信息修改

- ✅ **民宿管理**
  - 民宿 CRUD 操作
  - 民宿类型管理
  - 图片上传和管理
  - 价格、房间数设置

- ✅ **订单管理**
  - 所有订单查看
  - 订单状态更新（待确认/已确认/已完成/已取消）
  - 订单详情查看
  - 订单评价管理

- ✅ **内容管理**
  - 游玩项目管理（CRUD）
  - 旅游路线管理（CRUD）
  - 标签和分类管理

- ✅ **客服消息**
  - 查看所有客户消息
  - 💬 AI 智能回复建议
  - 消息回复和删除
  - 消息状态管理

- ✅ **系统设置**
  - AI 配置管理
  - 预订窗口设置
  - 系统参数配置

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.2.2
- **AI 集成**: Spring AI 1.0.0-M2
- **构建工具**: Maven 3.8+
- **数据库**: H2（内存数据库，演示用）/ MySQL 8.0+（生产环境）
- **JDK**: Java 17+

### 前端
- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite 5.x
- **UI 组件**: Element Plus
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **Markdown 渲染**: Marked

### AI 功能
- **模型**: OpenAI GPT-4o-mini（可配置）
- **技术**: Function Calling、Chat Completion
- **功能**: 旅行规划、智能订单生成、智能客服对话

## 📁 项目结构
```
minsu_mgr/
├── backend/                              # Spring Boot 后端
│   ├── src/main/java/com/example/minsumgr/
│   │   ├── annotation/                   # 自定义注解（权限控制）
│   │   ├── config/                       # 配置类
│   │   │   ├── AiFunctionConfig.java     # AI Function Calling 配置
│   │   │   ├── DataInitializer.java      # 数据初始化
│   │   │   └── WebMvcConfig.java         # Web 配置（CORS、拦截器）
│   │   ├── controller/                   # REST API 控制器
│   │   │   ├── AuthController.java       # 认证接口
│   │   │   ├── UserController.java       # 用户管理
│   │   │   ├── HomestayController.java   # 民宿管理
│   │   │   ├── BookingController.java    # 订单管理
│   │   │   ├── AiController.java         # AI 推荐
│   │   │   ├── ChatAiController.java     # AI 客服对话
│   │   │   ├── MessageController.java    # 消息管理
│   │   │   ├── RouteController.java      # 路线管理
│   │   │   ├── ActivityController.java   # 活动管理
│   │   │   ├── FileUploadController.java # 文件上传
│   │   │   ├── HomestayTypeController.java # 民宿类型
│   │   │   └── SystemSettingController.java # 系统设置
│   │   ├── domain/                       # 数据实体
│   │   │   ├── User.java                 # 用户实体
│   │   │   ├── Homestay.java             # 民宿实体
│   │   │   ├── Booking.java              # 订单实体
│   │   │   ├── Message.java              # 消息实体
│   │   │   ├── Route.java                # 路线实体
│   │   │   ├── Activity.java             # 活动实体
│   │   │   ├── HomestayType.java         # 民宿类型
│   │   │   └── SystemSetting.java        # 系统设置
│   │   ├── dto/                          # 数据传输对象
│   │   │   ├── LoginRequest.java
│   │   │   ├── BookingDraftRequest.java
│   │   │   └── ...
│   │   ├── interceptor/                  # 拦截器（认证、权限）
│   │   ├── repository/                   # 数据访问层（JPA Repository）
│   │   ├── service/                      # 业务逻辑层
│   │   │   ├── AuthService.java          # 认证服务
│   │   │   ├── AiRecommendationService.java # AI 推荐
│   │   │   ├── AiBookingService.java     # AI 订单生成
│   │   │   ├── ChatAiService.java        # AI 客服对话
│   │   │   ├── QueryToolsService.java    # AI Function 工具
│   │   │   └── ...
│   │   └── util/                         # 工具类
│   ├── src/main/resources/
│   │   └── application.yml               # 应用配置
│   ├── uploads/images/                   # 上传图片存储
│   └── pom.xml                           # Maven 依赖配置
├── frontend/                             # Vue 3 前端
│   ├── src/
│   │   ├── views/                        # 页面视图
│   │   │   ├── LoginView.vue             # 登录页
│   │   │   ├── CustomerView.vue          # 用户端
│   │   │   └── AdminView.vue             # 管理端
│   │   ├── components/                   # 组件
│   │   │   ├── AiRecommendation.vue      # AI 旅行规划
│   │   │   ├── BookingConfirmForm.vue    # 订单确认表单
│   │   │   ├── RoomCalendar.vue          # 房间日历
│   │   │   ├── ChatWindow.vue            # 用户端聊天窗口
│   │   │   ├── AdminChatWindow.vue       # 管理端聊天窗口
│   │   │   ├── UserManagement.vue        # 用户管理
│   │   │   ├── RouteManagement.vue       # 路线管理
│   │   │   ├── ActivityManagement.vue    # 活动管理
│   │   │   └── ...
│   │   ├── api.js                        # API 封装
│   │   ├── router.js                     # 路由配置
│   │   ├── main.js                       # 入口文件
│   │   └── App.vue                       # 根组件
│   ├── index.html
│   ├── package.json                      # npm 依赖配置
│   └── vite.config.js                    # Vite 配置
├── AUTHENTICATION.md                     # 认证说明文档
└── README.md                             # 项目说明
```

## 🚀 安装部署运行

### 环境要求
- **JDK**: 17 或更高版本
- **Node.js**: 16.x 或更高版本
- **Maven**: 3.8 或更高版本
- **数据库**: H2（默认，无需安装）或 MySQL 8.0+（可选）

### 1. 克隆项目
```bash
git clone <repository-url>
cd minsu_mgr
```

### 2. 后端启动

#### 方式一：使用 Maven 直接运行
```bash
cd backend
mvn spring-boot:run
```

#### 方式二：打包后运行
```bash
cd backend
mvn clean package
java -jar target/minsu-mgr-0.0.1-SNAPSHOT.jar
```

**启动成功标志**：
- 控制台输出：`Started MinsuMgrApplication in X seconds`
- 后端服务地址：http://localhost:8080
- H2 数据库控制台：http://localhost:8080/h2-console

### 3. 前端启动

```bash
cd frontend
npm install          # 首次运行需要安装依赖
npm run dev          # 启动开发服务器
```

**启动成功标志**：
- 控制台输出：`Local: http://localhost:5173/`
- 自动打开浏览器访问应用

### 4. 配置 AI 功能（可选）

如需使用 AI 功能，需配置 OpenAI API Key：

#### Linux/Mac:
```bash
export OPENAI_API_KEY="your-api-key-here"
export OPENAI_BASE_URL="https://api.openai.com"  # 可选，使用自定义 API 端点
export OPENAI_MODEL="gpt-4o-mini"                 # 可选，默认 gpt-4o-mini
```

#### Windows (PowerShell):
```powershell
$env:OPENAI_API_KEY="your-api-key-here"
$env:OPENAI_BASE_URL="https://api.openai.com"
$env:OPENAI_MODEL="gpt-4o-mini"
```

#### Windows (CMD):
```cmd
set OPENAI_API_KEY=your-api-key-here
set OPENAI_BASE_URL=https://api.openai.com
set OPENAI_MODEL=gpt-4o-mini
```

**配置后需重启后端服务**

### 5. 生产环境部署

#### 后端打包
```bash
cd backend
mvn clean package -DskipTests
```
生成的 JAR 文件位于 `target/` 目录

#### 前端打包
```bash
cd frontend
npm run build
```
生成的静态文件位于 `dist/` 目录，可部署到 Nginx 等 Web 服务器

#### MySQL 配置（生产环境推荐）
修改 `backend/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/minsu_mgr?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: your-password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update  # 生产环境建议使用 validate
```

创建数据库：
```sql
CREATE DATABASE minsu_mgr CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 📖 使用说明

### 首次访问
1. 打开浏览器访问：http://localhost:5173
2. 点击"去注册"创建新账户（默认为客户角色）
3. 注册成功后自动登录

### 创建管理员账户
系统初始化时会自动创建管理员账户：
- **用户名**: admin
- **密码**: admin123

或手动在数据库中设置用户角色为 `ADMIN`：
```sql
UPDATE user SET role = 'ADMIN' WHERE username = 'your-username';
```

### 用户端功能使用

#### 1. 浏览和预订民宿
- 进入"民宿预订"标签
- 选择心仪的民宿
- 选择入住和退房日期
- 查看房间日历（绿色=可用，红色=已预订）
- 点击"预订"创建订单

#### 2. AI 旅行规划
- 进入"AI 旅行规划"标签
- 设置旅行天数、开始日期、预算偏好
- 输入特殊需求（如：喜欢户外活动、需要安静环境等）
- 点击"✨ 生成旅行计划"
- 查看 AI 生成的推荐方案
- 点击"🎯 智能生成预订订单"自动生成预订

#### 3. AI 智能客服
- 进入"客服咨询"标签
- 在聊天窗口输入问题（如："查询我的订单"、"推荐民宿"）
- AI 会自动查询相关信息并回复

#### 4. 订单管理
- 进入"我的订单"标签
- 查看所有订单状态
- 对已完成订单进行评价

### 管理员功能使用

#### 1. 民宿管理
- 添加/编辑民宿信息
- 上传民宿图片
- 设置房间数量和价格
- 分配民宿类型

#### 2. 订单管理
- 查看所有订单
- 确认/完成/取消订单
- 查看订单评价

#### 3. AI 智能客服
- 查看客户消息
- 点击"AI 建议回复"获取智能回复建议
- 编辑并发送回复

#### 4. 系统管理
- 用户管理（角色分配）
- 路线和活动管理
- AI 配置管理

## 🤖 AI 功能说明

### AI 功能列表

#### 1. AI 旅行规划推荐
- **功能**：根据用户旅行天数、预算、特殊需求，生成个性化旅行方案
- **技术**：基于 Spring AI Chat Completion API
- **输入**：旅行天数、开始日期、预算偏好、自定义需求
- **输出**：民宿推荐、行程规划、游玩建议

#### 2. AI 智能订单生成
- **功能**：自动分析用户需求，查询民宿信息，检查可用性，生成预订方案
- **技术**：Spring AI Function Calling（调用后端工具函数）
- **函数工具**：
  - `queryAllHomestays` - 查询所有民宿
  - `queryHomestayDetail` - 查询民宿详情
  - `queryHomestayAvailability` - 检查房间可用性
  - `createMultipleBookingDrafts` - 生成多个预订方案
- **输出**：多个预订方案供用户选择（含民宿名称、价格、推荐理由）

#### 3. AI 智能客服对话
- **功能**：用户自然语言查询订单、民宿信息，AI 自动调用相关函数返回结果
- **技术**：Spring AI Function Calling + 对话上下文管理
- **函数工具**：
  - `queryUserBookings` - 查询用户订单
  - `queryAllHomestays` - 查询民宿列表
  - `queryHomestayDetail` - 查询民宿详情
  - `queryHomestayAvailability` - 查询房间可用性
  - `queryAllActivities` - 查询活动列表
  - `queryAllRoutes` - 查询路线列表
  - `searchHomestays` - 搜索民宿
- **示例对话**：
  - "查询我的订单"
  - "推荐一下有山景的民宿"
  - "查看民宿 XX 的详细信息"

#### 4. AI 智能回复建议（管理员）
- **功能**：根据客户消息内容，生成智能回复建议
- **技术**：Spring AI Chat Completion
- **使用场景**：管理员回复客户咨询时，点击获取 AI 建议

### AI 配置说明

#### 启用 AI 功能
设置以下环境变量（三种方式任选其一）：

**方式一：直接启动时设置**
```bash
OPENAI_API_KEY=your-api-key mvn spring-boot:run
```

**方式二：系统环境变量**
```bash
# Linux/Mac
export OPENAI_API_KEY="sk-xxxxxxxxxxxxxxxx"
export OPENAI_BASE_URL="https://api.openai.com"  # 可选，使用自定义端点
export OPENAI_MODEL="gpt-4o-mini"                # 可选，默认 gpt-4o-mini

# 重启后端
cd backend && mvn spring-boot:run
```

**方式三：application.yml 配置**
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-4o-mini}
```

#### 使用兼容 API
支持任何 OpenAI 兼容的 API 端点：
```bash
export OPENAI_API_KEY="your-api-key"
export OPENAI_BASE_URL="https://your-api-endpoint.com"
export OPENAI_MODEL="your-model-name"
```

#### 演示模式
如果未配置 API Key，系统将返回友好提示信息，不会影响其他功能的正常使用。

## 📡 API 接口文档

### 认证接口 (AuthController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/register` | 用户注册 | 公开 |
| POST | `/api/auth/login` | 用户登录 | 公开 |
| GET | `/api/auth/verify` | 验证 Token | 需登录 |
| POST | `/api/auth/refresh-token` | 刷新 Token | 需登录 |
| POST | `/api/auth/logout` | 登出 | 需登录 |
| GET | `/api/auth/me` | 获取当前用户信息 | 需登录 |

### 用户管理 (UserController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/users` | 获取所有用户 | 管理员 |
| GET | `/api/users/{id}` | 获取用户详情 | 管理员 |
| POST | `/api/users` | 创建用户 | 管理员 |
| PUT | `/api/users/{id}` | 更新用户 | 管理员 |
| DELETE | `/api/users/{id}` | 删除用户 | 管理员 |

### 民宿管理 (HomestayController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/homestays` | 获取民宿列表 | 公开 |
| GET | `/api/homestays/{id}` | 获取民宿详情 | 公开 |
| POST | `/api/homestays` | 创建民宿 | 管理员 |
| PUT | `/api/homestays/{id}` | 更新民宿 | 管理员 |
| DELETE | `/api/homestays/{id}` | 删除民宿 | 管理员 |
| POST | `/api/homestays/ai-recommend` | AI 推荐民宿 | 需登录 |
| POST | `/api/homestays/ai-booking-draft` | AI 生成订单草稿 | 需登录 |

### 订单管理 (BookingController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/bookings` | 获取所有订单 | 管理员 |
| GET | `/api/bookings/user/{userId}` | 获取用户订单 | 需登录 |
| GET | `/api/bookings/homestay/{homestayId}/confirmed` | 获取民宿已确认订单 | 需登录 |
| POST | `/api/bookings` | 创建订单 | 需登录 |
| PUT | `/api/bookings/{id}` | 更新订单 | 管理员 |
| DELETE | `/api/bookings/{id}` | 删除订单 | 管理员 |
| POST | `/api/bookings/check-conflict` | 检查订单冲突 | 需登录 |
| GET | `/api/bookings/room-calendar/{homestayId}` | 获取房间日历 | 公开 |
| GET | `/api/bookings/user/{userId}/for-review` | 获取待评价订单 | 需登录 |
| POST | `/api/bookings/{id}/review` | 提交订单评价 | 需登录 |
| POST | `/api/bookings/confirm-ai-draft` | 确认 AI 订单草稿 | 需登录 |

### 消息管理 (MessageController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/messages` | 获取所有消息 | 管理员 |
| GET | `/api/messages/user/{userId}` | 获取用户消息 | 需登录 |
| POST | `/api/messages` | 发送消息 | 需登录 |
| PUT | `/api/messages/{id}` | 更新消息 | 管理员 |
| DELETE | `/api/messages/{id}` | 删除消息 | 管理员 |

### 路线管理 (RouteController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/routes` | 获取所有路线 | 公开 |
| POST | `/api/routes` | 创建路线 | 管理员 |
| PUT | `/api/routes/{id}` | 更新路线 | 管理员 |
| DELETE | `/api/routes/{id}` | 删除路线 | 管理员 |

### 活动管理 (ActivityController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/activities` | 获取所有活动 | 公开 |
| POST | `/api/activities` | 创建活动 | 管理员 |
| PUT | `/api/activities/{id}` | 更新活动 | 管理员 |
| DELETE | `/api/activities/{id}` | 删除活动 | 管理员 |

### AI 接口 (AiController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/ai/recommendations` | 获取 AI 旅行推荐 | 需登录 |

### AI 客服 (ChatAiController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/chat-ai/suggest-reply` | 获取 AI 回复建议 | 管理员 |

### 文件上传 (FileUploadController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/upload/image` | 上传图片 | 管理员 |
| GET | `/api/upload/images/{filename}` | 获取图片 | 公开 |

### 民宿类型 (HomestayTypeController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/homestay-types` | 获取所有民宿类型 | 公开 |
| POST | `/api/homestay-types` | 创建民宿类型 | 管理员 |

### 系统设置 (SystemSettingController)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/system-settings/booking-window-days` | 获取预订窗口天数 | 公开 |
| GET | `/api/system-settings/ai-config` | 获取 AI 配置 | 公开 |

## 🗄️ 数据库管理

### H2 内存数据库（默认）
- **控制台地址**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:minsu_mgr`
- **用户名**: `sa`
- **密码**: （留空）
- **特点**: 
  - 无需安装，开箱即用
  - 重启后数据丢失
  - 适合开发和演示

### 初始化数据
系统启动时会自动初始化以下数据：
- 管理员账户（username: admin, password: admin123）
- 示例民宿数据
- 示例路线和活动数据
- 民宿类型数据

### 切换到 MySQL（生产环境）

**1. 创建数据库**
```sql
CREATE DATABASE minsu_mgr CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**2. 修改配置**
编辑 `backend/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/minsu_mgr?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: your-password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  h2:
    console:
      enabled: false  # 关闭 H2 控制台
```

**3. 重启应用**
```bash
mvn spring-boot:run
```

## ⚠️ 注意事项

### 安全相关
1. **默认管理员密码**: 首次部署后请立即修改管理员密码
2. **Token 机制**: 当前使用简单 Base64 编码，生产环境建议替换为 JWT
3. **CORS 配置**: 生产环境需调整 CORS 策略，限制允许的来源
4. **文件上传**: 建议配置文件大小限制和格式验证

### 功能相关
1. **注册用户角色**: 新注册用户默认为"客户"角色
2. **管理员创建**: 
   - 使用默认账户 admin/admin123
   - 或在数据库中手动设置角色为 `ADMIN`
3. **AI 功能**: 需要配置有效的 OpenAI API Key，未配置时返回友好提示
4. **图片存储**: 默认存储在 `backend/uploads/images/` 目录，生产环境建议使用对象存储服务

### 性能优化
1. **数据库连接池**: 可配置 HikariCP 连接池参数
2. **JPA 查询优化**: 建议使用 DTO 投影减少数据传输
3. **AI 调用缓存**: 可考虑缓存常见问题的 AI 回复
4. **静态资源**: 前端静态资源建议使用 CDN

## 🔧 常见问题

### 1. 端口被占用
```bash
# 修改后端端口（application.yml）
server:
  port: 8081

# 修改前端端口（vite.config.js）
export default defineConfig({
  server: { port: 5174 }
})
```

### 2. AI 功能不可用
- 检查环境变量 `OPENAI_API_KEY` 是否设置
- 检查 API 端点是否可访问
- 查看后端日志确认 ChatModel Bean 是否创建成功

### 3. 图片上传失败
- 检查 `backend/uploads/images/` 目录是否存在
- 检查目录写入权限
- 检查文件大小是否超过限制（默认 10MB）

### 4. 数据库连接失败
- 检查数据库服务是否启动
- 检查连接配置是否正确
- 查看后端启动日志错误信息

## 📝 开发计划

### 近期计划
- [ ] 添加订单支付功能
- [ ] 民宿收藏功能
- [ ] 优惠券系统
- [ ] 用户积分系统

### 长期计划
- [ ] 移动端适配
- [ ] 微信小程序版本
- [ ] 数据统计和报表
- [ ] 多语言支持

## 📄 许可证
本项目仅供学习和演示使用。

## 🤝 贡献
欢迎提交 Issue 和 Pull Request！
