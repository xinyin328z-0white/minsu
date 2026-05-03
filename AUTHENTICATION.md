# 民宿管理系统 - 鉴权功能说明

## 🔐 认证系统架构

本项目实现了完整的JWT（JSON Web Token）认证和授权系统。

### 核心组件

#### 1. **JwtTokenProvider** (`util/JwtTokenProvider.java`)
- 负责生成和验证JWT Token
- 使用HS256算法签名
- Token有效期：24小时
- Refresh Token有效期：7天

**主要方法：**
- `generateToken(userId, username, role)` - 生成访问Token
- `generateRefreshToken(userId, username)` - 生成刷新Token
- `validateToken(token)` - 验证Token有效性
- `isTokenExpired(token)` - 检查Token是否过期
- `getUserIdFromToken(token)` - 从Token中提取用户ID
- `getRoleFromToken(token)` - 从Token中提取用户角色

#### 2. **RequireRole 注解** (`annotation/RequireRole.java`)
- 用于标记需要权限验证的API端点
- 支持多个角色的权限定义

**使用示例：**
```java
@PostMapping("/settings")
@RequireRole(roles = "ADMIN")  // 仅限管理员
public void updateSettings() { }

@GetMapping("/profile")
@RequireRole(roles = {"ADMIN", "CUSTOMER"})  // 管理员和用户都可访问
public void getProfile() { }
```

#### 3. **JwtInterceptor** (`interceptor/JwtInterceptor.java`)
- 拦截所有API请求
- 验证Authorization header中的Token
- 检查Token有效性和权限
- 自动注入用户信息到request属性中

**流程：**
1. 检查Authorization头是否存在
2. 提取Bearer Token
3. 验证Token签名和有效期
4. 检查用户角色是否符合@RequireRole要求
5. 若验证失败返回401/403错误

#### 4. **WebMvcConfig** (`config/WebMvcConfig.java`)
- 注册JWT拦截器
- 配置CORS策略
- 排除不需要认证的端点

**排除的端点：**
- `/api/auth/login` - 登录
- `/api/auth/register` - 注册
- `/api/auth/verify` - Token验证（可选）
- `/api/h2-console/**` - H2数据库控制台

### 认证流程

#### 登录流程
```
用户输入用户名密码
  ↓
POST /api/auth/login
  ↓
验证用户名和密码
  ↓
生成JWT Token和Refresh Token
  ↓
返回Token到前端
```

**请求示例：**
```json
POST /api/auth/login
{
  "username": "fjr",
  "password": "fjr"
}
```

**响应示例：**
```json
{
  "userId": 1,
  "username": "fjr",
  "role": "ADMIN",
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}
```

#### API调用流程
```
前端保存Token到localStorage
  ↓
每次API请求在Authorization header中携带Token
  ↓
Authorization: Bearer <token>
  ↓
拦截器验证Token
  ↓
验证通过 → 继续处理请求
验证失败 → 返回401/403错误
```

#### Token刷新流程
```
Token即将过期
  ↓
POST /api/auth/refresh-token
{
  "refreshToken": "..."
}
  ↓
验证Refresh Token
  ↓
生成新的访问Token
  ↓
返回新Token
```

### 权限控制

#### 角色定义
- **ADMIN** - 管理员
  - 可管理系统设置、AI配置
  - 可查看和回复客服消息
  - 可管理用户和数据
  
- **CUSTOMER** - 普通用户
  - 可查看个人资料
  - 可浏览民宿和预订
  - 可发送客服消息

#### 权限应用的API端点

**需要ADMIN权限：**
- `GET /api/users` - 获取所有用户列表
- `POST /api/users` - 创建用户
- `PUT /api/users/{id}` - 更新用户信息
- `GET/PUT /api/settings/**` - 系统设置
- `GET /api/messages` - 获取所有消息（仅管理员可见）

**需要ADMIN或CUSTOMER权限：**
- `GET /api/users/{id}` - 获取用户信息
- `PUT /api/users/{id}/profile` - 更新个人资料
- `POST /api/auth/logout` - 登出

**无需权限验证：**
- `POST /api/auth/login` - 登录
- `POST /api/auth/register` - 注册
- `GET /api/auth/verify` - 验证Token

### 前端集成

#### 存储Token
```javascript
// 登录成功后
localStorage.setItem('token', loginResponse.token)
localStorage.setItem('refreshToken', loginResponse.refreshToken)
localStorage.setItem('userId', loginResponse.userId)
localStorage.setItem('role', loginResponse.role)
```

#### 发送认证请求
```javascript
// 在Axios拦截器中添加Token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

#### 处理Token过期
```javascript
// 当收到401错误时
if (error.response.status === 401) {
  // 使用Refresh Token获取新Token
  const refreshToken = localStorage.getItem('refreshToken')
  const newResponse = await api.post('/auth/refresh-token', 
    { refreshToken }
  )
  // 更新Token后重试原请求
}
```

### 安全特性

1. **JWT签名** - 使用HS256算法，防止Token被篡改
2. **Token过期** - 自动过期机制，降低泄露风险
3. **Refresh Token** - 访问Token失效后可用Refresh Token更新
4. **角色验证** - API端点级别的权限控制
5. **CORS保护** - 跨域请求受限

### 错误处理

| HTTP状态码 | 含义 | 处理方式 |
|-----------|------|---------|
| 200 | 成功 | 正常处理 |
| 400 | 请求错误 | 显示错误信息 |
| 401 | 未授权 | 重定向到登录页 |
| 403 | 禁止访问 | 显示权限不足提示 |
| 500 | 服务器错误 | 显示系统错误 |

### 测试方式

#### 1. 使用curl测试登录
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"fjr","password":"fjr"}'
```

#### 2. 使用Token访问受保护的API
```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer <你的token>"
```

#### 3. 测试权限检查
```bash
# 使用CUSTOMER的token访问ADMIN端点
curl -X GET http://localhost:8080/api/settings/ai-config \
  -H "Authorization: Bearer <customer-token>"
# 应该返回403 Forbidden
```

### 配置信息

**JWT密钥：** 使用256位密钥自动生成（生产环境建议配置到环境变量）

**Token有效期配置位置：** `JwtTokenProvider.java`
```java
private static final long TOKEN_VALIDITY = 24 * 60 * 60 * 1000;  // 24小时
private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000;  // 7天
```

### 依赖库

```xml
<!-- JWT支持 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
```

---

## 总结

本项目实现了生产级别的JWT认证系统，包括：
✅ Token生成和验证
✅ 角色基权限控制
✅ Token刷新机制
✅ 自动拦截和权限检查
✅ 完整的错误处理
✅ 前后端集成指南

系统已准备就绪，可直接用于实现完整的用户认证和授权功能。
