# Sanctuary Coffee 在线咖啡店系统

前后端分离的在线咖啡店电商与运营管理平台，覆盖商品、购物车、下单支付、会员积分、优惠券促销、配送履约、售后评价、财务统计与 RBAC 权限管理。

## 功能概览

### 账号与权限

- 用户注册、登录、密码初始化
- Spring Security + JWT 无状态鉴权
- 角色与权限（RBAC）：用户、管理员、配送员及细粒度权限码
- 前端路由 / 菜单按角色与权限码收敛
- 管理端权限管理台

### 商品与库存

- 商品分类管理
- 商品信息、SKU / 规格
- 库存流水与库存预警
- C 端商品浏览与选购

### 购物车与订单

- 购物车增删改查
- 下单确认与订单列表
- 订单状态流转
- 管理端订单管理

### 支付与退款

- 支付下单页与支付记录
- 退款申请与退款管理
- 售后申请、处理与进度日志

### 会员与营销

- 会员中心、会员等级
- 会员权益与权益发放/核销记录
- 积分流水
- 优惠券中心与优惠券管理
- 促销活动及促销商品关联

### 配送履约

- 配送方式、配送区域
- 配送员与配送任务
- 配送轨迹跟踪
- 配送异常处理
- 管理端配送看板

### 评价

- 商品评价、我的评价
- 评价回复
- 管理端评价管理

### 用户资产

- 个人中心
- 收货地址管理

### 数据与财务

- 经营数据看板（ECharts）
- 销售 / 用户行为等统计分析
- 财务报表、发票管理
- 管理端工作台

### 系统能力

- 文件上传
- Knife4j（OpenAPI）接口文档
- Druid 数据源监控
- Redis 缓存支撑
- MySQL 完整建库脚本与增量迁移 SQL（V2–V7）

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端语言 | Java 17 |
| 后端框架 | Spring Boot 3.1.6 |
| 安全 | Spring Security、JWT（jjwt 0.12.x） |
| 持久化 | MyBatis-Plus 3.5.x、MySQL、Druid |
| 缓存 | Spring Data Redis / Lettuce |
| 接口文档 | Knife4j 4.x |
| 工具 | Hutool、Lombok、Validation、AOP |
| 前端框架 | Vue 3 |
| 构建 | Vite 5 |
| UI | Element Plus、Element Plus Icons |
| 状态 / 路由 | Pinia、Vue Router |
| 请求 | Axios |
| 图表 | ECharts |
| 地图 | Leaflet |
| 样式 | Sass |

### 后端主要模块（`com.coffee`）

- `controller`：REST 接口（认证、商品、订单、支付、会员、配送、财务、统计、RBAC 等）
- `service` / `service.impl`：业务逻辑
- `entity` / `mapper`：约 40+ 实体与数据访问
- `security`：JWT 与安全配置
- `dto` / `vo`：入参与出参模型
- `common`：统一响应、异常、工具

### 前端主要目录（`frontend/src`）

- `views/`：认证、商品、购物车、订单、支付、会员、优惠券、配送、评价、财务、统计、管理端等页面（约 40 个 Vue 页面）
- `api/`：接口封装
- `stores/`：Pinia 状态
- `router/`：路由与权限守卫
- `utils/permission`：角色与权限码工具

## 工程结构

```text
demo/
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/com/coffee/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-dev.yml
│   └── pom.xml
├── frontend/                # Vue 3 前端
│   ├── src/
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── docs/
│   └── database/            # 建库与增量 SQL
│       ├── coffee_shop_complete.sql
│       └── V2__*.sql … V7__*.sql
└── README.md
```

## 数据库

1. 安装 MySQL 8.x，创建库或直接执行完整脚本：

```powershell
mysql -u root -p < docs/database/coffee_shop_complete.sql
```

2. 如需增量变更，按序执行 `docs/database/V2__` 至 `V7__` 脚本。

3. 默认库名：`coffee_shop`（见 `application.yml`）。

## 配置说明

通过环境变量覆盖敏感项（推荐），避免把真实密码写入仓库：

| 变量 | 含义 | 默认参考 |
|------|------|----------|
| `DB_USERNAME` | 数据库用户 | `root` |
| `DB_PASSWORD` | 数据库密码 | 空 |
| `REDIS_HOST` / `REDIS_PORT` | Redis | `localhost:6379` |
| `JWT_SECRET` | JWT 密钥 | 开发默认值（上线务必更换） |
| `FILE_UPLOAD_PATH` | 上传目录 | `/uploads` |

后端默认端口：`8080`，上下文路径：`/api`。

## 本地运行

### 环境要求

- JDK 17+
- Maven 3.8+（或使用项目 `mvnw`）
- Node.js 18+
- MySQL 8.x
- Redis（按功能启用）

### 启动后端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

接口文档（Knife4j）一般在：

```text
http://localhost:8080/api/doc.html
```

### 启动前端

```powershell
cd frontend
npm install
npm run dev
```

按 Vite 控制台提示访问本地地址（通常为 `http://127.0.0.1:5173`）。

### 打包

```powershell
# 后端
cd backend
.\mvnw.cmd -DskipTests package

# 前端
cd frontend
npm run build
```

## 作者

- GitHub：[@AreamSaber](https://github.com/AreamSaber)
- Email：whk1085403136@gmail.com

## 许可证

未单独声明许可证时，默认仅供学习与交流使用。如需商用或再分发，请先与作者确认。
