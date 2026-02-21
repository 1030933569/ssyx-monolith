# 环境部署指南：（基于 Docker）

> 适用对象：完全没有环境配置经验、甚至电脑上还没有安装 Docker 的同学。  
> 项目目标：一键把 `MySQL` + `Redis` + `ssyx-monolith (后端服务)` 跑起来。

---

## 第一步：安装 Docker 环境 (如果已安装请跳过)

### 1.1 下载与安装 Docker Desktop (Windows/Mac)
Docker 就像是一个虚拟的“集装箱环境”，可以把运行电商系统所需要的所有杂乱软件打包运行。
1. 前往 Docker 官网下载：[https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)
2. 下载安装包后，一路点击 “Next” 傻瓜式安装。
3. **重要提示（Windows）**：安装完成后，Docker 可能会提示需要安装 **WSL2** (Windows Subsystem for Linux)。请按照它弹出的弹窗指引，在终端（CMD）中运行 `wsl --update` 或者下载对应补丁包。
4. 安装完成后，在开始菜单中搜索并打开 **Docker Desktop**，看到左下角有个绿色的鲸鱼图标，说明 Docker 成功启动了！

### 1.2 验证 Docker 是否可用
1. 打开终端（CMD 或 PowerShell，如果是 Mac 就打开 Terminal）。
2. 输入命令：`docker -v`。如果输出了类似 `Docker version 20.10.x` 的字样，说明环境配置成功。
3. 输入命令：`docker-compose -v`，检查批量运行工具是否也安装完毕。

---

## 第二步：准备项目配置

在运行之前，我们需要确保 Docker 会把初始化数据导入到 MySQL 中，请按照以下步骤检查：

### 2.1 确认 SQL 文件就位
确保你的项目中有存放数据库表结构脚本的文件夹，通常在这个路径：
`你的项目路径/doc/sql/init-db.sh` 以及其同级目录下的各大 `.sql` 文件（如 `shequ-order.sql`, `shequ-user.sql` 等）。这些文件会在数据库首次启动时自动建表。

### 2.2 了解 docker-compose.yml
在项目根目录下有一个 `docker-compose.yml`。这个文件是核心，它告诉了 Docker 应该启动三个容器服务（Container）：
1. **mysql** (包含 MySQL 5.7 引擎和你的默认账号 root/root)
2. **redis** (无密码版 Redis 6 高速缓存)
3. **app** (就是你这个基于 Java Spring Boot 开发的 ssyx-monolith 业务应用程序)

---

## 第三步：一键构建与部署

现在，我们要通过一行命令魔法般地跑起前后端需要的所有服务。

1. 打开一个自带命令行的终端（比如在 VS Code 中按下 `Ctrl + ~` 打开集成终端，或者在项目根目录下右键打开 PowerShell/Bash）。
2. 确保终端的当前路径是**你的项目根目录**。
3. 输入以下核心命令（**注意，期间需要下载镜像包，请保持网络通畅，大约需要5~15分钟**）：

```bash
docker-compose up -d --build
```
*   `up`：启动所有在 compose 编排文件里定义的服务。
*   `-d`：在后台（Daemon）静默运行，不会霸占你的黑框框终端。
*   `--build`：在启动前，强制利用项目下的 `Dockerfile` 把你最新的 Java 后端代码打包编译成新的镜像。

---

## 第四步：检查是否成功运行

当你看到类似如下三行绿色的输出时，说明全部组件都已挂载：
```text
Creating ssyx-mysql ... done
Creating ssyx-redis ... done
Creating ssyx-app   ... done
```

**如何验证真的成功了？**
1. **测试后端接口**：在浏览器里输入 `http://localhost:8080/doc.html`。如果能打开 Swagger 也就是 Knife4j 的接口 API 页面，说明后端的 Spring Boot 已经和 MySQL、Redis 牵手成功并健壮运行了！
2. **连接数据库检查**：你可以用 Navicat 或者 IDEA 自带的 Database 数据库连线工具，连上 `localhost:3306` (账号/密码均是 `root`)，里面应该有一个名叫 `ssyx` 的库，以及自动创建好的近十张表。

### 如果碰到报错怎么办（排雷指南）
*   **端口被占用（Port is already allocated）**：说明你电脑上可能本来就开着其他的 MySQL（3306 端口）或 Tomcat/Java 程序（8080 端口）。解决方案：把本地自带的关掉，再去跑 Docker。
*   **如何查看后端报错日志？**：输入这行命令，能实时像看黑客电影一样看到后端 Spring Boot 的运行报错日志：
    ```bash
    docker logs -f ssyx-app
    ```
*   **我想停止一切服务或者重来怎么办？**
    ```bash
    docker-compose down
    ```
    这个命令不仅会优雅地关机，还会顺便清理掉所有附着的“网络尸体”，方便你修复 Bug 后再次 `docker-compose up -d --build` 重新来过。
