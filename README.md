# SQL字段级血缘分析平台

> 基于 Alibaba Druid 的SQL字段级血缘分析工具  
> 支持 Hive、MySQL、Spark SQL 多种数据库方言

[![版本](https://img.shields.io/badge/version-1.0.0--SNAPSHOT-blue)](https://github.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen)](https://spring.io/projects/spring-boot)
[![Druid](https://img.shields.io/badge/Druid-1.2.20-orange)](https://github.com/alibaba/druid)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

---

## 📖 项目简介

SQL字段级血缘分析平台是一款专业的数据血缘分析工具，帮助数据工程师和开发者快速理解复杂SQL的数据流向，支持字段级别的依赖追踪和影响分析。

### 核心功能

- ✅ **SQL解析**：支持单表SELECT、多表JOIN、子查询、UNION、窗口函数
- ✅ **多数据库支持**：Hive、MySQL、Spark SQL
- ✅ **Kettle文件解析**：批量提取Kettle转换中的SQL
- ✅ **元数据管理**：维护库、表、字段元数据
- ✅ **多种可视化**：表格、树形、DAG流程图
- ✅ **结果导出**：支持Excel格式导出

---

## 🏗️ 项目结构

```
sql-lineage/
├── .agents/                  # AI任务管理系统
│   ├── tasks/                # 任务配置文件
│   │   └── sql-lineage.yaml
│   ├── docs/                 # 开发文档
│   │   ├── task-format.md
│   │   ├── task-engine.md
│   │   ├── architecture.md
│   │   └── coding-standards.md
│   └── rules/                # 开发规范
│       ├── base.md
│       └── dev.md
│
├── backend/                  # 后端项目（Spring Boot）
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/lineage/
│   │   │   │   ├── Application.java      # 启动类
│   │   │   │   ├── controller/           # 控制器层
│   │   │   │   ├── service/              # 服务层
│   │   │   │   ├── mapper/               # 数据访问层
│   │   │   │   ├── entity/               # 实体类
│   │   │   │   ├── dto/                  # 数据传输对象
│   │   │   │   │   ├── request/
│   │   │   │   │   └── response/
│   │   │   │   ├── core/                 # 核心引擎
│   │   │   │   │   ├── visitor/          # AST访问者
│   │   │   │   │   ├── tracker/          # 字段依赖追踪
│   │   │   │   │   ├── dialect/          # 数据库方言
│   │   │   │   │   └── graph/            # 血缘图结构
│   │   │   │   └── config/               # 配置类
│   │   │   └── resources/
│   │   │       ├── application.yml        # 应用配置
│   │   │       ├── mapper/                # MyBatis映射
│   │   │       └── sql/                   # SQL脚本
│   │   └── test/                          # 测试代码
│   └── pom.xml                            # Maven配置
│
├── frontend/                 # 前端项目（HTML/CSS/JS）
│   ├── index.html            # 主页面
│   ├── css/
│   │   ├── style.css         # 全局样式
│   │   ├── layout.css        # 布局样式
│   │   └── components.css    # 组件样式
│   ├── js/
│   │   ├── main.js           # 主应用脚本
│   │   ├── api.js            # API调用封装
│   │   ├── table-view.js     # 表格视图
│   │   ├── tree-view.js      # 树形视图
│   │   ├── visualizer.js     # DAG可视化
│   │   └── utils.js          # 工具函数
│   ├── lib/                  # 第三方库
│   └── images/               # 图片资源
│
├── docs/                     # 项目文档
│   └── database/             # 数据库设计
│       ├── schema.sql        # 建表脚本
│       ├── er-diagram.md     # ER图说明
│       └── init-data.sql     # 初始化数据
│
└── README.md                 # 项目说明
```

---

## 🚀 快速开始

### 环境要求

- **JDK**: 8+
- **Maven**: 3.6+
- **Node.js**: (可选，仅开发时用于前端工具)
- **浏览器**: Chrome 90+ / Firefox 88+ / Edge 90+

### 安装步骤

#### 1. 克隆项目

```bash
git clone https://github.com/your-repo/sql-lineage.git
cd sql-lineage
```

#### 2. 后端启动

```bash
cd backend

# 编译打包
mvn clean package -DskipTests

# 运行（开发环境）
java -jar target/sql-lineage-analyzer.jar --spring.profiles.active=dev

# 或直接使用Maven运行
mvn spring-boot:run
```

#### 3. 访问应用

- **前端界面**: http://localhost:8080/
- **H2控制台**: http://localhost:8080/h2-console
- **健康检查**: http://localhost:8080/actuator/health

#### 4. 初始化数据库（首次运行）

```bash
# H2数据库会自动创建，也可以手动执行SQL
java -jar target/sql-lineage-analyzer.jar --spring.profiles.active=dev

# 执行初始化脚本（可选）
# 在H2控制台中执行 docs/database/schema.sql 和 init-data.sql
```

---

## 📚 使用指南

### 示例：分析简单SQL

1. 打开浏览器访问 http://localhost:8080/
2. 选择数据库类型（如 Hive）
3. 输入SQL语句：
```sql
SELECT 
    u.user_id,
    u.user_name,
    o.order_amount
FROM dw_user_info u
JOIN dw_order_detail o ON u.user_id = o.user_id
WHERE o.order_date >= '2025-01-01'
```
4. 点击"开始分析"
5. 查看结果（表格/树形/DAG视图）
6. 可选：导出Excel

### API调用示例

```javascript
// 使用Axios调用分析API
axios.post('http://localhost:8080/api/lineage/analyze', {
  sql: "SELECT * FROM users",
  dbType: "mysql"
})
.then(response => {
  console.log('血缘分析结果:', response.data);
})
.catch(error => {
  console.error('分析失败:', error);
});
```

---

## 🛠️ 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | Web框架 |
| Alibaba Druid | 1.2.20 | SQL解析器 |
| MyBatis Plus | 3.5.3 | ORM框架 |
| H2 Database | 2.1.x | 开发数据库 |
| MySQL | 8.0+ | 生产数据库 |
| Hutool | 5.8.16 | Java工具库 |
| EasyExcel | 3.3.2 | Excel导出 |
| DOM4J | 2.1.4 | XML解析（Kettle） |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| HTML5/CSS3/ES6+ | - | 基础技术 |
| AntV G6 | 4.8.x | 图形可视化 |
| DataTables.js | 1.13.x | 表格组件 |
| jsTree | 3.3.x | 树形组件 |
| Axios | 1.6.x | HTTP客户端 |

---

## 📊 数据库设计

### 核心表

- `metadata_database` - 数据库元数据
- `metadata_table` - 表元数据
- `metadata_column` - 字段元数据
- `lineage_analysis` - 血缘分析任务
- `lineage_field_relation` - 字段血缘关系
- `lineage_table_relation` - 表级血缘关系
- `kettle_file_history` - Kettle文件历史
- `kettle_extracted_sql` - 提取的SQL

详细设计见 [docs/database/er-diagram.md](docs/database/er-diagram.md)

---

## 🧪 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=LineageAnalyzerTest

# 生成测试覆盖率报告
mvn test jacoco:report
# 报告位置: target/site/jacoco/index.html
```

---

## 📦 部署

### 生产环境部署

```bash
# 1. 打包
mvn clean package -DskipTests

# 2. 配置数据库（MySQL）
# 修改 application.yml 中的数据库连接信息

# 3. 运行
java -jar target/sql-lineage-analyzer.jar \
  --spring.profiles.active=prod \
  --server.port=8080 \
  --spring.datasource.url=jdbc:mysql://your-mysql-host:3306/lineage_db \
  --spring.datasource.username=your_username \
  --spring.datasource.password=your_password
```

### Docker部署（待实现）

```bash
# 构建镜像
docker build -t sql-lineage-analyzer:1.0.0 .

# 运行容器
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=your-mysql-host \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  sql-lineage-analyzer:1.0.0
```

---

## 📝 开发指南

### 代码规范

- 遵循 [.agents/docs/coding-standards.md](.agents/docs/coding-standards.md)
- Java代码：阿里巴巴Java开发手册
- JavaScript代码：Google JavaScript Style Guide

### Git提交规范

```
feat(lineage): 实现JOIN语句的字段血缘分析
fix(parser): 修复子查询解析错误
docs(readme): 更新安装文档
```

### 任务管理

项目使用AI任务管理系统，详见 [.agents/docs/task-engine.md](.agents/docs/task-engine.md)

---

## 🗺️ 开发路线图

### ✅ M0 - 需求与设计（Day 1）
- [x] 需求访谈
- [x] 技术架构设计
- [x] 数据库设计
- [x] 项目骨架搭建

### 🔄 M1 - 核心引擎原型（Day 2-3）
- [ ] Druid解析器集成
- [ ] 基础字段血缘分析
- [ ] REST API开发

### ⏸️ M2 - 完整解析能力（Day 4-7）
- [ ] 支持JOIN、子查询
- [ ] 支持UNION、窗口函数
- [ ] 单元测试覆盖

### ⏸️ M3 - Kettle+元数据（Day 8-10）
- [ ] Kettle文件解析
- [ ] 元数据管理CRUD

### ⏸️ M4 - 前端可视化（Day 11-13）
- [ ] 表格/树形视图
- [ ] DAG血缘图
- [ ] Excel导出

### ⏸️ M5 - 性能优化与部署（Day 14-15）
- [ ] 性能测试（1000+行SQL）
- [ ] 内网部署
- [ ] 使用文档

---

## 🤝 贡献

欢迎提交Issue和Pull Request！

---

## 📄 许可证

MIT License

---

## 👥 联系方式

- 项目负责人: 数据工程团队
- 邮箱: admin@example.com

---

**最后更新**: 2025-10-23  
**当前版本**: 1.0.0-SNAPSHOT  
**里程碑进度**: M0 ✅ 已完成
