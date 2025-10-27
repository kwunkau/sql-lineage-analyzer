# 常见问题解答 (FAQ)

## 部署相关

### Q1: 支持哪些操作系统？
**A**: 
- Linux: CentOS 7+, Ubuntu 18.04+, Debian 10+
- Windows: Windows Server 2016+, Windows 10
- macOS: 10.14+（仅开发环境）

推荐生产环境使用 Linux。

### Q2: 最低硬件配置是多少？
**A**:
- CPU: 2核
- 内存: 4GB
- 磁盘: 20GB

生产环境推荐 4核 8GB 内存。

### Q3: 是否支持 H2 数据库生产部署？
**A**: 不推荐。H2仅用于开发测试，生产环境必须使用 MySQL 8.0+。

### Q4: 是否支持其他数据库（PostgreSQL、Oracle）？
**A**: v1.0.0 仅支持 MySQL，其他数据库将在后续版本支持。

## 功能相关

### Q5: 支持哪些数据库方言的SQL？
**A**: 
- Hive
- MySQL
- Spark SQL

更多方言（Oracle、PostgreSQL、SQL Server）将在后续版本支持。

### Q6: 是否支持DDL语句血缘分析？
**A**: v1.0.0 仅支持 DML（SELECT、INSERT）语句，DDL支持将在v1.1.0加入。

### Q7: 能否分析存储过程中的SQL？
**A**: 暂不支持，需要手动提取SQL后分析。

### Q8: Kettle文件最大支持多大？
**A**: 单文件最大 50MB，可在配置文件中调整。

## 性能相关

### Q9: 1000行SQL分析需要多久？
**A**: 
- 简单查询: < 2秒
- 复杂JOIN: 5-10秒
- 超大SQL: 需要性能优化（参考 #44 issue）

### Q10: 批量分析100个Kettle文件需要多久？
**A**: 取决于文件大小和复杂度，通常30-60秒。v1.0.0将支持异步批量处理。

### Q11: 如何优化性能？
**A**:
1. 增加 JVM 内存：`-Xmx4g`
2. 增加数据库连接池：`maximum-pool-size: 50`
3. 启用元数据缓存
4. 使用SSD存储

## 错误排查

### Q12: 应用启动失败，提示端口占用
**A**:
```bash
# 查看占用端口的进程
sudo netstat -tlnp | grep 8080

# 杀死进程
sudo kill -9 <PID>

# 或修改端口
java -jar app.jar --server.port=8081
```

### Q13: 数据库连接失败
**A**:
检查项：
1. MySQL是否运行：`systemctl status mysqld`
2. 用户名密码是否正确
3. 数据库是否存在：`SHOW DATABASES;`
4. 防火墙是否开放3306端口

### Q14: 文件上传报错"Maximum upload size exceeded"
**A**:
```yaml
# 修改 application.yml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 200MB
```

同时修改 Nginx：
```nginx
client_max_body_size 100m;
```

### Q15: 前端页面空白
**A**:
1. 检查浏览器控制台错误（F12）
2. 检查 Nginx 配置是否正确
3. 检查前端文件是否存在
4. 清除浏览器缓存

### Q16: API返回500错误
**A**:
```bash
# 查看后端日志
tail -f /var/log/lineage/application.log

# 查看错误详情
tail -f /var/log/lineage/error.log
```

## 升级相关

### Q17: 如何从 v0.4.0 升级到 v1.0.0？
**A**:
1. 备份数据库
2. 停止服务
3. 更新 JAR 包
4. 执行数据库升级脚本
5. 启动服务

详见 [DEPLOYMENT.md](DEPLOYMENT.md#升级指南)

### Q18: 升级会丢失数据吗？
**A**: 不会。只要执行了数据库备份，可以随时回滚。

### Q19: 是否支持灰度升级？
**A**: v1.0.0 暂不支持，可以使用负载均衡实现。

## 安全相关

### Q20: 是否支持用户认证？
**A**: v1.0.0 暂不支持，将在 v1.1.0 加入。

### Q21: 如何配置HTTPS？
**A**: 
```nginx
server {
    listen 443 ssl;
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    # ...其他配置
}
```

### Q22: 敏感数据如何保护？
**A**:
1. 数据库密码使用环境变量
2. 限制 Actuator 端点访问
3. 配置IP白名单
4. 启用访问日志

## Docker相关

### Q23: Docker镜像多大？
**A**: 约 200MB（使用Alpine JRE）

### Q24: 如何持久化数据？
**A**:
```yaml
volumes:
  - mysql-data:/var/lib/mysql
  - ./uploads:/var/lineage/uploads
```

### Q25: 容器内存不足如何处理？
**A**:
```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          memory: 4G
```

## 开发相关

### Q26: 如何本地开发调试？
**A**:
```bash
# 使用 H2 数据库
cd backend
mvn spring-boot:run -Dspring.profiles.active=dev

# 访问 H2 控制台
http://localhost:8080/h2-console
```

### Q27: 如何贡献代码？
**A**:
1. Fork 项目
2. 创建功能分支
3. 提交 Pull Request
4. 等待审核

### Q28: 如何运行单元测试？
**A**:
```bash
cd backend
mvn test

# 生成覆盖率报告
mvn test jacoco:report
```

## 其他

### Q29: 是否提供技术支持？
**A**: 
- 社区支持：GitHub Issues
- 商业支持：联系开发团队

### Q30: 源码是否开源？
**A**: 是，MIT License。

---

**未找到答案？**
- 提交 Issue: https://github.com/your-org/sql-lineage-analyzer/issues
- 查看文档: [DEPLOYMENT.md](DEPLOYMENT.md)
