# Zeabur 部署配置指南

本文档记录了将 ssyx-monolith 项目部署到 Zeabur 并配置微信小程序登录的完整过程。

---

## 一、准备工作

### 1.1 项目结构

```
ssyx-monolith/
├── ssyx-monolith/          # Spring Boot 主应用
├── youxuan_wechat/         # 微信小程序前端
├── doc/sql/                # 数据库初始化脚本
│   ├── shequ-user.sql
│   ├── shequ-product.sql
│   ├── shequ-activity.sql
│   ├── shequ-order.sql
│   ├── shequ-acl.sql
│   └── shequ-sys.sql
└── ...
```

### 1.2 需要准备的信息

- 微信小程序 AppID 和 AppSecret（从微信公众平台获取）
- GitHub 仓库地址（用于从外部导入SQL）

---

## 二、Zeabur 环境变量配置

在 Zeabur 项目中配置以下环境变量：

### 2.1 微信配置

| 环境变量名 | 说明 | 示例值 |
|-----------|------|--------|
| `WX_OPEN_APP_ID` | 微信小程序 AppID | `wxf103a66e025a65e4` |
| `WX_OPEN_APP_SECRET` | 微信小程序 AppSecret | `` |

### 2.2 数据库配置

| 环境变量名 | 说明 | 值 |
|-----------|------|-----|
| `MYSQL_DATABASE` | 数据库名称 | `shequ-user` |
| `MYSQL_HOST` | 数据库主机 | (Zeabur自动配置) |
| `MYSQL_PORT` | 数据库端口 | (Zeabur自动配置) |
| `MYSQL_USERNAME` | 数据库用户名 | (Zeabur自动配置) |
| `MYSQL_PASSWORD` | 数据库密码 | (Zeabur自动配置) |

> **重要**：`MYSQL_DATABASE` 必须设置为 `shequ-user`，与导入的SQL数据库名一致！

---

## 三、数据库初始化

### 3.1 连接到应用容器

在 Zeabur 控制台，点击应用服务 → Console，进入 bash 终端。

### 3.2 创建数据库

```bash
mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USERNAME -p$MYSQL_PASSWORD -e "CREATE DATABASE IF NOT EXISTS \`shequ-user\` DEFAULT CHARACTER SET utf8mb4;"
```

### 3.3 从 GitHub 导入所有SQL表

依次执行以下命令（用 `sed` 去掉 `CREATE DATABASE` 和 `USE` 语句，将所有表导入同一个数据库）：

```bash
# 1. 导入 user 相关表
curl -sSL "https://raw.githubusercontent.com/1030933569/ssyx-monolith/main/doc/sql/shequ-user.sql" | sed -e '/^CREATE DATABASE/d' -e '/^USE /d' | mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USERNAME -p$MYSQL_PASSWORD shequ-user

# 2. 导入 product 相关表
curl -sSL "https://raw.githubusercontent.com/1030933569/ssyx-monolith/main/doc/sql/shequ-product.sql" | sed -e '/^CREATE DATABASE/d' -e '/^USE /d' | mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USERNAME -p$MYSQL_PASSWORD shequ-user

# 3. 导入 activity 相关表
curl -sSL "https://raw.githubusercontent.com/1030933569/ssyx-monolith/main/doc/sql/shequ-activity.sql" | sed -e '/^CREATE DATABASE/d' -e '/^USE /d' | mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USERNAME -p$MYSQL_PASSWORD shequ-user

# 4. 导入 order 相关表
curl -sSL "https://raw.githubusercontent.com/1030933569/ssyx-monolith/main/doc/sql/shequ-order.sql" | sed -e '/^CREATE DATABASE/d' -e '/^USE /d' | mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USERNAME -p$MYSQL_PASSWORD shequ-user
```

### 3.4 修复表结构问题

如果遇到 `Unknown column 'sku_type'` 错误，执行：

```bash
mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USERNAME -p$MYSQL_PASSWORD shequ-user -e "ALTER TABLE sku_info CHANGE sku_types sku_type tinyint(1) NOT NULL DEFAULT 0;"
```

### 3.5 验证表导入成功

```bash
mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USERNAME -p$MYSQL_PASSWORD shequ-user -e "SHOW TABLES;"
```

应该能看到 `user`、`category`、`sku_info` 等表。

---

## 四、小程序配置

### 4.1 修改 AppID

编辑 `youxuan_wechat/project.config.json`，确保 `appid` 与微信公众平台一致：

```json
{
  "appid": "wxf103a66e025a65e4",
  ...
}
```

### 4.2 配置后端API地址

编辑 `youxuan_wechat/utils/http.js`，确保 `BASE_URL` 指向 Zeabur 部署的后端：

```javascript
const BASE_URL = 'https://ssysmono.zeabur.app/api'
```

---

## 五、常见问题排查

### 5.1 错误：`获取accessToken失败` (code: 218)

**原因**：微信 AppID 或 AppSecret 配置错误

**解决**：
1. 检查 Zeabur 环境变量 `WX_OPEN_APP_ID` 和 `WX_OPEN_APP_SECRET`
2. 确保与微信公众平台后台的值完全一致
3. 确保小程序 `project.config.json` 中的 `appid` 也一致

### 5.2 错误：`invalid code` (errcode: 40029)

**原因**：小程序 AppID 与后端配置的 AppID 不匹配

**解决**：统一 `project.config.json` 和 Zeabur 环境变量中的 AppID

### 5.3 错误：`Table 'xxx' doesn't exist`

**原因**：数据库表未创建

**解决**：按照第三节步骤导入SQL

### 5.4 错误：`Unknown column 'xxx'`

**原因**：表结构不完整或列名不匹配

**解决**：
1. 检查表结构：`mysql ... -e "DESC 表名;"`
2. 根据需要添加或重命名列

### 5.5 错误：502 Bad Gateway

**原因**：应用启动失败，通常是数据库连接问题

**解决**：
1. 检查 Zeabur 应用日志
2. 确认 `MYSQL_DATABASE` 环境变量值正确
3. 确认数据库已创建且可访问

---

## 六、快速重置指南

如果需要完全重置数据库：

```bash
# 1. 删除并重建数据库
mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USERNAME -p$MYSQL_PASSWORD -e "DROP DATABASE IF EXISTS \`shequ-user\`; CREATE DATABASE \`shequ-user\` DEFAULT CHARACTER SET utf8mb4;"

# 2. 重新导入所有SQL（参考3.3节）

# 3. 修复表结构问题（参考3.4节）
```

---

## 七、相关文件路径

| 文件 | 说明 |
|------|------|
| `ssyx-monolith/src/main/resources/application.yml` | Spring Boot 配置 |
| `youxuan_wechat/project.config.json` | 小程序项目配置 |
| `youxuan_wechat/utils/http.js` | API请求配置 |
| `doc/sql/*.sql` | 数据库初始化脚本 |

---

*文档创建时间：2026-01-08*
