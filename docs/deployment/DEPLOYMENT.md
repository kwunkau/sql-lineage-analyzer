# SQL字段级血缘分析平台 - 部署文档

> 版本：v1.0.0  
> 更新日期：2025-10-27

---

## 📋 目录

- [系统要求](#系统要求)
- [环境准备](#环境准备)
- [部署步骤](#部署步骤)
  - [方式1：传统部署（推荐生产环境）](#方式1传统部署推荐生产环境)
  - [方式2：Docker部署（推荐开发测试）](#方式2docker部署推荐开发测试)
- [配置说明](#配置说明)
- [健康检查](#健康检查)
- [常见问题](#常见问题)

---

## 系统要求

### 最低配置

| 组件 | 要求 |
|------|------|
| CPU | 2核 |
| 内存 | 4GB |
| 磁盘 | 20GB |
| 操作系统 | Linux / Windows Server |

### 推荐配置（生产环境）

| 组件 | 要求 |
|------|------|
| CPU | 4核+ |
| 内存 | 8GB+ |
| 磁盘 | 50GB+ (SSD) |
| 操作系统 | CentOS 7+ / Ubuntu 20.04+ |

### 软件依赖

| 软件 | 版本 | 用途 |
|------|------|------|
| **JDK** | 8+ | 运行后端应用 |
| **MySQL** | 8.0+ | 生产数据库 |
| **Nginx** | 1.18+ | 前端服务和反向代理 |
| **Maven** | 3.6+ | 编译打包（可选） |

---

## 环境准备

### 1. 安装 JDK

#### CentOS/RHEL
```bash
# 安装 OpenJDK 8
sudo yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel

# 验证安装
java -version
```

#### Ubuntu/Debian
```bash
# 安装 OpenJDK 8
sudo apt update
sudo apt install -y openjdk-8-jdk

# 验证安装
java -version
```

#### 配置 JAVA_HOME
```bash
# 编辑 /etc/profile 或 ~/.bashrc
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# 使配置生效
source /etc/profile
```

### 2. 安装 MySQL

#### 安装 MySQL 8.0
```bash
# CentOS 7
sudo yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm
sudo yum install -y mysql-community-server

# Ubuntu
sudo apt update
sudo apt install -y mysql-server-8.0

# 启动 MySQL
sudo systemctl start mysqld
sudo systemctl enable mysqld
```

#### 初始化数据库
```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE lineage_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户（生产环境建议单独用户）
CREATE USER 'lineage_user'@'%' IDENTIFIED BY 'your_strong_password';
GRANT ALL PRIVILEGES ON lineage_db.* TO 'lineage_user'@'%';
FLUSH PRIVILEGES;

# 退出
EXIT;
```

#### 导入数据库结构
```bash
# 执行初始化脚本
mysql -u lineage_user -p lineage_db < backend/src/main/resources/sql/schema.sql
```

### 3. 安装 Nginx

```bash
# CentOS
sudo yum install -y nginx

# Ubuntu
sudo apt install -y nginx

# 启动 Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

---

## 部署步骤

### 方式1：传统部署（推荐生产环境）

#### 第1步：获取应用包

##### 选项A：使用发布版本（推荐）
```bash
# 下载最新发布版本
wget https://github.com/your-org/sql-lineage-analyzer/releases/download/v1.0.0/sql-lineage-analyzer-1.0.0.jar
```

##### 选项B：源码编译
```bash
# 克隆项目
git clone https://github.com/your-org/sql-lineage-analyzer.git
cd sql-lineage-analyzer

# 编译打包
cd backend
mvn clean package -DskipTests

# JAR 包位置
ls target/sql-lineage-analyzer-1.0.0.jar
```

#### 第2步：创建部署目录
```bash
# 创建应用目录
sudo mkdir -p /opt/lineage
sudo mkdir -p /var/log/lineage
sudo mkdir -p /var/lineage/uploads

# 复制应用文件
sudo cp backend/target/sql-lineage-analyzer-1.0.0.jar /opt/lineage/
sudo cp -r frontend /opt/lineage/

# 设置权限
sudo chown -R lineage:lineage /opt/lineage
sudo chown -R lineage:lineage /var/log/lineage
sudo chown -R lineage:lineage /var/lineage
```

#### 第3步：配置应用

创建配置文件 `/opt/lineage/application-prod.yml`：

```yaml
server:
  port: 8080

spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://localhost:3306/lineage_db?useSSL=true&serverTimezone=Asia/Shanghai
    username: lineage_user
    password: your_strong_password
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  # 文件上传配置
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB

# 日志配置
logging:
  level:
    root: INFO
    com.lineage: DEBUG
  file:
    name: /var/log/lineage/application.log
    max-size: 100MB
    max-history: 30

# 业务配置
lineage:
  kettle:
    upload-dir: /var/lineage/uploads

# Actuator 监控
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: when-authorized
```

#### 第4步：配置 Nginx

创建 Nginx 配置 `/etc/nginx/conf.d/lineage.conf`：

```nginx
upstream lineage_backend {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name your-domain.com;  # 修改为实际域名

    # 前端静态资源
    location / {
        root /opt/lineage/frontend;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://lineage_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时配置
        proxy_connect_timeout 30s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Actuator 监控端点（可选，生产环境建议内网访问）
    location /actuator/ {
        proxy_pass http://lineage_backend;
        # allow 192.168.0.0/16;  # 仅内网访问
        # deny all;
    }

    # 日志
    access_log /var/log/nginx/lineage_access.log;
    error_log /var/log/nginx/lineage_error.log;
}
```

重启 Nginx：
```bash
# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

#### 第5步：创建 Systemd 服务

创建服务文件 `/etc/systemd/system/lineage.service`：

```ini
[Unit]
Description=SQL Lineage Analyzer Service
After=network.target mysql.service

[Service]
Type=simple
User=lineage
Group=lineage
WorkingDirectory=/opt/lineage

# JVM 参数
Environment="JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 启动命令
ExecStart=/usr/bin/java $JAVA_OPTS \
    -jar /opt/lineage/sql-lineage-analyzer-1.0.0.jar \
    --spring.config.location=/opt/lineage/application-prod.yml \
    --spring.profiles.active=prod

# 重启策略
Restart=on-failure
RestartSec=10s

# 日志
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

#### 第6步：启动服务

```bash
# 创建用户
sudo useradd -r -s /bin/false lineage

# 重新加载 systemd
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start lineage

# 设置开机自启
sudo systemctl enable lineage

# 查看状态
sudo systemctl status lineage

# 查看日志
sudo journalctl -u lineage -f
```

#### 第7步：验证部署

```bash
# 检查应用健康
curl http://localhost:8080/actuator/health

# 访问前端
curl http://localhost/

# 查看日志
tail -f /var/log/lineage/application.log
```

---

### 方式2：Docker部署（推荐开发测试）

详见 [DOCKER.md](DOCKER.md)

快速启动：
```bash
# 克隆项目
git clone https://github.com/your-org/sql-lineage-analyzer.git
cd sql-lineage-analyzer

# 使用 docker-compose 启动
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

访问：
- 前端：http://localhost
- 后端：http://localhost:8080
- 健康检查：http://localhost:8080/actuator/health

---

## 配置说明

### 数据库配置

#### 连接池优化
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # 最大连接数
      minimum-idle: 5            # 最小空闲连接
      connection-timeout: 30000  # 连接超时（毫秒）
      idle-timeout: 600000       # 空闲超时（毫秒）
      max-lifetime: 1800000      # 连接最大生存时间（毫秒）
```

#### 生产环境建议
- 独立数据库用户，最小权限原则
- 启用 SSL 连接：`useSSL=true`
- 定期备份数据库
- 监控慢查询

### JVM 参数优化

#### 堆内存配置
```bash
# 小规模（4GB内存）
-Xms1g -Xmx2g

# 中规模（8GB内存）
-Xms2g -Xmx4g

# 大规模（16GB内存）
-Xms4g -Xmx8g
```

#### 垃圾回收优化
```bash
# 使用 G1GC（推荐）
-XX:+UseG1GC 
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m

# GC 日志（用于调优）
-Xlog:gc*:file=/var/log/lineage/gc.log:time,uptime:filecount=5,filesize=100m
```

### 文件上传配置

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB        # 单文件最大大小
      max-request-size: 100MB    # 请求最大大小

lineage:
  kettle:
    upload-dir: /var/lineage/uploads  # 上传目录
```

确保目录存在且有写权限：
```bash
sudo mkdir -p /var/lineage/uploads
sudo chown lineage:lineage /var/lineage/uploads
sudo chmod 755 /var/lineage/uploads
```

---

## 健康检查

### 应用健康检查

```bash
# 健康状态
curl http://localhost:8080/actuator/health

# 响应示例（正常）
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 性能指标

```bash
# JVM 内存
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP 请求统计
curl http://localhost:8080/actuator/metrics/http.server.requests

# 数据库连接池
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### 监控脚本

创建 `scripts/health-check.sh`：
```bash
#!/bin/bash

HEALTH_URL="http://localhost:8080/actuator/health"

# 检查健康状态
RESPONSE=$(curl -s $HEALTH_URL)
STATUS=$(echo $RESPONSE | jq -r '.status')

if [ "$STATUS" == "UP" ]; then
    echo "✅ 服务运行正常"
    exit 0
else
    echo "❌ 服务异常: $RESPONSE"
    exit 1
fi
```

配置定时任务：
```bash
# 每5分钟检查一次
*/5 * * * * /opt/lineage/scripts/health-check.sh >> /var/log/lineage/health-check.log 2>&1
```

---

## 常见问题

### 1. 应用无法启动

**症状**：`systemctl start lineage` 失败

**排查步骤**：
```bash
# 查看详细错误
sudo journalctl -u lineage -n 50 --no-pager

# 检查端口占用
sudo netstat -tlnp | grep 8080

# 检查 Java 版本
java -version

# 检查配置文件
cat /opt/lineage/application-prod.yml
```

**常见原因**：
- 端口 8080 被占用
- 数据库连接失败
- 配置文件路径错误
- 权限不足

### 2. 数据库连接失败

**错误信息**：`Communications link failure`

**解决方法**：
```bash
# 检查 MySQL 是否运行
sudo systemctl status mysqld

# 测试连接
mysql -h localhost -u lineage_user -p -e "SELECT 1"

# 检查防火墙
sudo firewall-cmd --list-ports

# 开放 MySQL 端口
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload
```

### 3. 文件上传失败

**错误信息**：`Maximum upload size exceeded`

**解决方法**：
```yaml
# 修改 application-prod.yml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 200MB
```

同时修改 Nginx 配置：
```nginx
# /etc/nginx/nginx.conf
http {
    client_max_body_size 100m;
}
```

### 4. 前端页面404

**症状**：访问 http://localhost 显示 404

**解决方法**：
```bash
# 检查 Nginx 配置
sudo nginx -t

# 检查前端文件
ls -la /opt/lineage/frontend/

# 检查权限
sudo chown -R nginx:nginx /opt/lineage/frontend/

# 重启 Nginx
sudo systemctl restart nginx
```

### 5. 内存不足（OOM）

**症状**：`java.lang.OutOfMemoryError`

**解决方法**：
```bash
# 增加堆内存
# 编辑 /etc/systemd/system/lineage.service
Environment="JAVA_OPTS=-Xms4g -Xmx8g"

# 重启服务
sudo systemctl daemon-reload
sudo systemctl restart lineage

# 监控内存使用
jstat -gcutil <pid> 1000
```

### 6. 性能慢

**症状**：SQL分析耗时超过10秒

**优化措施**：
1. 增加数据库连接池：`maximum-pool-size: 50`
2. 启用缓存：参考性能优化文档
3. 增加 JVM 内存
4. 检查数据库索引
5. 查看慢查询日志

---

## 日志管理

### 日志位置
- 应用日志：`/var/log/lineage/application.log`
- 错误日志：`/var/log/lineage/error.log`
- GC日志：`/var/log/lineage/gc.log`
- Nginx日志：`/var/log/nginx/lineage_*.log`

### 日志清理
```bash
# 清理30天前的日志
find /var/log/lineage -name "*.log.*" -mtime +30 -delete

# 配置 logrotate
sudo vi /etc/logrotate.d/lineage

/var/log/lineage/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 lineage lineage
}
```

---

## 升级指南

### 1. 备份数据
```bash
# 备份数据库
mysqldump -u lineage_user -p lineage_db > lineage_db_backup_$(date +%Y%m%d).sql

# 备份上传文件
tar -czf uploads_backup_$(date +%Y%m%d).tar.gz /var/lineage/uploads/
```

### 2. 停止服务
```bash
sudo systemctl stop lineage
```

### 3. 更新应用
```bash
# 备份旧版本
sudo mv /opt/lineage/sql-lineage-analyzer-1.0.0.jar \
       /opt/lineage/sql-lineage-analyzer-1.0.0.jar.bak

# 部署新版本
sudo cp sql-lineage-analyzer-1.1.0.jar /opt/lineage/
```

### 4. 更新数据库
```bash
# 执行升级脚本
mysql -u lineage_user -p lineage_db < upgrade-1.0-to-1.1.sql
```

### 5. 启动服务
```bash
sudo systemctl start lineage

# 检查日志
sudo journalctl -u lineage -f
```

---

## 卸载

```bash
# 停止服务
sudo systemctl stop lineage
sudo systemctl disable lineage

# 删除服务文件
sudo rm /etc/systemd/system/lineage.service
sudo systemctl daemon-reload

# 删除应用文件
sudo rm -rf /opt/lineage

# 删除日志和数据
sudo rm -rf /var/log/lineage
sudo rm -rf /var/lineage

# 删除数据库（可选）
mysql -u root -p -e "DROP DATABASE lineage_db; DROP USER 'lineage_user'@'%';"

# 删除用户
sudo userdel lineage
```

---

## 安全建议

1. **数据库安全**
   - 使用强密码
   - 限制远程访问
   - 定期备份

2. **应用安全**
   - 配置 HTTPS（SSL证书）
   - 限制 Actuator 端点访问
   - 定期更新依赖

3. **系统安全**
   - 配置防火墙
   - 最小权限原则
   - 定期安全审计

4. **网络安全**
   - 使用 VPN 或内网访问
   - 配置 IP 白名单
   - 启用访问日志

---

## 更多资源

- [Docker部署文档](DOCKER.md)
- [配置参考文档](CONFIG.md)
- [常见问题解答](FAQ.md)
- [API文档](../api/API.md)
- [GitHub Issues](https://github.com/your-org/sql-lineage-analyzer/issues)

---

**文档维护**：开发团队  
**最后更新**：2025-10-27  
**适用版本**：v1.0.0+
