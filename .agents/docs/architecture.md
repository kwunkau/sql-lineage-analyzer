# SQL字段级血缘分析平台 - 技术架构设计

> 文档版本: v1.0  
> 创建时间: 2025-10-23  
> 作者: AI Task Engine  
> 状态: ✅ 已完成

---

## 1. 技术栈选型

### 1.1 后端技术栈

| 组件 | 技术选型 | 版本 | 选型理由 |
|-----|---------|------|---------|
| **核心框架** | Spring Boot | 2.7.x | 快速开发、生态成熟、易于部署 |
| **SQL解析器** | Alibaba Druid | 1.2.20 | ✅ 支持多数据库方言（Hive/MySQL/Spark）<br>✅ AST访问者模式<br>✅ 国产、文档完善 |
| **ORM框架** | MyBatis Plus | 3.5.3 | 代码生成、CRUD简化、性能优良 |
| **数据库** | H2 (开发) / MySQL (生产) | H2: 2.1.x<br>MySQL: 8.0+ | H2内嵌部署简单，MySQL生产稳定 |
| **XML解析** | DOM4J | 2.1.3 | Kettle文件（XML）解析 |
| **Excel导出** | EasyExcel | 3.3.2 | 大数据量导出、低内存占用 |
| **工具库** | Hutool | 5.8.16 | 中文友好的Java工具集 |
| **日志** | SLF4J + Logback | 1.7.36 | 标准日志门面 |

### 1.2 前端技术栈

| 组件 | 技术选型 | 版本 | 选型理由 |
|-----|---------|------|---------|
| **基础** | HTML5 + CSS3 + ES6+ | - | 原生开发，无框架依赖 |
| **图形引擎** | AntV G6 | 4.8.x | ✅ 国产图形库<br>✅ 专业DAG/血缘图<br>✅ 交互能力强 |
| **表格组件** | DataTables.js | 1.13.x | 功能丰富（排序、搜索、分页） |
| **树形组件** | jsTree | 3.3.x | 层级数据展示 |
| **HTTP客户端** | Axios | 1.6.x | Promise风格，拦截器支持 |
| **UI框架** | 自定义CSS + Flexbox | - | 轻量级，避免框架臃肿 |

### 1.3 数据库方言支持

通过Druid的 `DbType` 枚举支持：

```java
// 支持的数据库类型
DbType.hive    → Apache Hive SQL
DbType.mysql   → MySQL / MariaDB
DbType.spark   → Apache Spark SQL
```

---

## 2. 系统架构图

### 2.1 总体架构（前后端分离）

```
┌─────────────────────────────────────────────────────────────┐
│                      用户层 (Browser)                        │
│                                                              │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐           │
│  │ SQL输入界面 │  │ 血缘可视化 │  │ 元数据管理 │           │
│  └────────────┘  └────────────┘  └────────────┘           │
│         │               │               │                   │
└─────────┼───────────────┼───────────────┼───────────────────┘
          │               │               │
          └───────────────┴───────────────┘
                      HTTP REST API
                      (JSON)
          ┌───────────────┬───────────────┐
          │               │               │
┌─────────┼───────────────┼───────────────┼───────────────────┐
│         ↓               ↓               ↓                    │
│  ┌──────────────────────────────────────────────┐           │
│  │          控制器层 (Controller)                │           │
│  │  /lineage/analyze   /metadata/*   /export/*  │           │
│  └──────────────────────────────────────────────┘           │
│                         ↓                                    │
│  ┌──────────────────────────────────────────────┐           │
│  │              服务层 (Service)                 │           │
│  │  • DruidParserService   (SQL解析)            │           │
│  │  • LineageAnalyzer      (血缘分析引擎)       │           │
│  │  • KettleParserService  (Kettle解析)         │           │
│  │  • MetadataService      (元数据CRUD)         │           │
│  │  • ExportService        (导出)               │           │
│  │  • BatchProcessor       (批量异步)           │           │
│  └──────────────────────────────────────────────┘           │
│                         ↓                                    │
│  ┌──────────────────────────────────────────────┐           │
│  │          数据访问层 (Mapper/DAO)              │           │
│  │  MyBatis Plus                                 │           │
│  └──────────────────────────────────────────────┘           │
│                         ↓                                    │
│  ┌──────────────────────────────────────────────┐           │
│  │           数据库 (H2/MySQL)                   │           │
│  │  • metadata_*  (元数据表)                    │           │
│  │  • lineage_*   (血缘结果表)                  │           │
│  └──────────────────────────────────────────────┘           │
│                                                              │
│                  Spring Boot Application                     │
└──────────────────────────────────────────────────────────────┘
```

### 2.2 核心分析流程

```
┌──────────┐
│ 用户输入SQL │
└─────┬────┘
      │
      ↓
┌─────────────────┐
│ 1. SQL预处理    │  (去注释、格式化)
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ 2. Druid解析    │ → SQLStatement (AST)
└────────┬────────┘
         │
         ↓
┌─────────────────────────┐
│ 3. AST遍历 (访问者模式) │
│  • 提取SELECT字段       │
│  • 提取FROM表           │
│  • 提取JOIN条件         │
│  • 解析WHERE/GROUP BY   │
└──────────┬──────────────┘
           │
           ↓
┌──────────────────────┐
│ 4. 字段依赖追踪       │
│  • 递归解析表达式     │
│  • 别名映射           │
│  • 子查询字段传播     │
└──────────┬───────────┘
           │
           ↓
┌──────────────────────┐
│ 5. 构建血缘图        │  (DAG结构)
└──────────┬───────────┘
           │
           ↓
┌──────────────────────┐
│ 6. 结果序列化        │ → JSON
└──────────┬───────────┘
           │
           ↓
┌──────────────────────┐
│ 7. 前端可视化        │  (表格/树/图)
└──────────────────────┘
```

---

## 3. 模块划分

### 3.1 后端模块结构

```
backend/
└── src/main/java/com/lineage/
    ├── Application.java                   # Spring Boot启动类
    │
    ├── controller/                        # 控制器层
    │   ├── LineageController.java         # 血缘分析API
    │   ├── MetadataController.java        # 元数据管理API
    │   ├── KettleController.java          # Kettle解析API
    │   └── ExportController.java          # 导出API
    │
    ├── service/                           # 服务层
    │   ├── DruidParserService.java        # Druid SQL解析服务
    │   ├── MetadataService.java           # 元数据CRUD服务
    │   ├── KettleParserService.java       # Kettle XML解析服务
    │   ├── ExportService.java             # Excel导出服务
    │   └── BatchService.java              # 批量处理服务
    │
    ├── core/                              # 核心引擎
    │   ├── LineageAnalyzer.java           # 血缘分析主类
    │   ├── visitor/
    │   │   └── LineageVisitor.java        # AST访问者
    │   ├── tracker/
    │   │   └── FieldDependencyTracker.java # 字段依赖追踪器
    │   ├── dialect/
    │   │   └── DbTypeResolver.java        # 数据库方言解析
    │   └── graph/
    │       └── LineageGraph.java          # 血缘图数据结构
    │
    ├── entity/                            # 实体类
    │   ├── MetadataDatabase.java
    │   ├── MetadataTable.java
    │   ├── MetadataColumn.java
    │   └── LineageAnalysis.java
    │
    ├── dto/                               # 数据传输对象
    │   ├── request/
    │   │   ├── AnalyzeRequest.java        # 分析请求
    │   │   └── MetadataImportRequest.java
    │   └── response/
    │       ├── LineageResult.java         # 血缘结果
    │       └── CommonResponse.java        # 统一响应格式
    │
    ├── mapper/                            # MyBatis Mapper
    │   ├── MetadataMapper.java
    │   └── LineageMapper.java
    │
    └── config/                            # 配置类
        ├── DruidConfig.java
        ├── WebConfig.java
        └── AsyncConfig.java
```

### 3.2 前端模块结构

```
frontend/
├── index.html                    # 主页面
├── css/
│   ├── style.css                 # 全局样式
│   ├── layout.css                # 布局样式
│   └── components.css            # 组件样式
├── js/
│   ├── main.js                   # 应用入口
│   ├── api.js                    # API封装
│   ├── visualizer.js             # 血缘图可视化 (G6)
│   ├── table-view.js             # 表格视图
│   ├── tree-view.js              # 树形视图
│   └── utils.js                  # 工具函数
└── lib/                          # 第三方库
    ├── g6.min.js
    ├── axios.min.js
    ├── datatables.min.js
    └── jstree.min.js
```

---

## 4. 核心算法设计

### 4.1 字段血缘分析算法

**输入**: SQL字符串、数据库类型  
**输出**: 字段血缘关系图 (JSON)

**算法步骤**:

```java
public LineageResult analyze(String sql, String dbType) {
    // 步骤1: 解析SQL为AST
    SQLStatement stmt = DruidParser.parse(sql, DbType.of(dbType));
    
    // 步骤2: 创建访问者
    LineageVisitor visitor = new LineageVisitor();
    stmt.accept(visitor);
    
    // 步骤3: 构建血缘图
    LineageGraph graph = new LineageGraph();
    
    for (SelectItem item : visitor.getSelectItems()) {
        String targetField = item.getAlias();
        List<SourceField> sources = resolveFieldSources(item.getExpr());
        graph.addEdge(sources, targetField);
    }
    
    // 步骤4: 返回结果
    return graph.toResult();
}

// 递归解析表达式
private List<SourceField> resolveFieldSources(SQLExpr expr) {
    if (expr instanceof SQLIdentifierExpr) {
        // 简单字段: col1
        return resolveSingleColumn((SQLIdentifierExpr) expr);
    } else if (expr instanceof SQLPropertyExpr) {
        // 限定字段: t1.col1
        return resolveQualifiedColumn((SQLPropertyExpr) expr);
    } else if (expr instanceof SQLMethodInvokeExpr) {
        // 函数: sum(col1)
        List<SourceField> result = new ArrayList<>();
        for (SQLExpr arg : ((SQLMethodInvokeExpr) expr).getArguments()) {
            result.addAll(resolveFieldSources(arg));  // 递归
        }
        return result;
    } else if (expr instanceof SQLBinaryOpExpr) {
        // 运算: col1 + col2
        SQLBinaryOpExpr binExpr = (SQLBinaryOpExpr) expr;
        List<SourceField> result = new ArrayList<>();
        result.addAll(resolveFieldSources(binExpr.getLeft()));   // 递归
        result.addAll(resolveFieldSources(binExpr.getRight()));  // 递归
        return result;
    }
    // ... 其他表达式类型
    return Collections.emptyList();
}
```

### 4.2 Kettle文件解析算法

```java
public List<String> extractSQLFromKTR(File ktrFile) {
    Document doc = SAXReader.read(ktrFile);
    Element root = doc.getRootElement();
    
    List<String> sqlList = new ArrayList<>();
    List<Element> steps = root.selectNodes("//step");
    
    for (Element step : steps) {
        String stepType = step.elementText("type");
        
        // TableInput步骤包含SQL
        if ("TableInput".equals(stepType)) {
            String sql = step.elementText("sql");
            if (StringUtils.isNotBlank(sql)) {
                sqlList.add(sql);
            }
        }
        // 其他包含SQL的步骤类型...
    }
    
    return sqlList;
}
```

---

## 5. 技术风险评估

| 风险 | 等级 | 影响 | 缓解策略 |
|------|------|------|---------|
| **Druid对Spark SQL支持不完整** | 🟡 中 | 部分Spark特有语法无法解析 | MVP阶段重点支持Hive/MySQL，Spark作为二期扩展 |
| **超大SQL性能问题** | 🔴 高 | 1000+行SQL可能解析超时 | 实施异步处理、AST缓存、分段解析 |
| **Kettle版本兼容性** | 🟡 中 | 不同版本XML结构差异 | 支持主流版本（8.x/9.x），提供兼容性检查 |
| **复杂SQL边界情况** | 🟡 中 | 特殊SQL语法遗漏 | 建立测试用例库，迭代补充 |
| **多数据库方言差异** | 🟡 中 | 同一语法不同数据库语义不同 | 方言隔离，分别处理 |
| **前端大图渲染性能** | 🟢 低 | 20+表的DAG图可能卡顿 | G6虚拟渲染、节点折叠 |

---

## 6. 非功能性需求

### 6.1 性能指标

| 指标 | 目标 | 测试方法 |
|------|------|---------|
| 简单SQL解析 | < 100ms | 单表SELECT，10个字段 |
| 复杂SQL解析 | < 600s | 1000行SQL，20+表 |
| 批量处理 | 100条SQL/10min | 异步队列处理 |
| 并发支持 | 10用户同时使用 | JMeter压测 |
| 内存占用 | < 1GB | 大SQL解析 |

### 6.2 可靠性

- **数据准确性**: 字段血缘准确率 > 95%
- **容错性**: SQL语法错误时返回友好提示，不崩溃
- **日志完整性**: 所有操作记录日志，便于排查

### 6.3 可维护性

- **代码规范**: 遵循阿里巴巴Java开发手册
- **注释覆盖**: 核心算法类 > 80%
- **单元测试**: 覆盖率 > 80%

---

## 7. 部署架构

### 7.1 内网部署方案

```
┌────────────────────────────────────────┐
│          内网服务器 (Linux/Windows)     │
│                                         │
│  ┌──────────────────────────────────┐ │
│  │  Nginx (80/443)                  │ │
│  │  ├─ /        → 静态前端资源     │ │
│  │  └─ /api/*   → 后端Spring Boot  │ │
│  └──────────────────────────────────┘ │
│                  ↓                     │
│  ┌──────────────────────────────────┐ │
│  │  Spring Boot (8080)              │ │
│  │  - JVM参数: -Xms512m -Xmx2g     │ │
│  └──────────────────────────────────┘ │
│                  ↓                     │
│  ┌──────────────────────────────────┐ │
│  │  MySQL (3306)                    │ │
│  │  - 元数据库: lineage_metadata    │ │
│  └──────────────────────────────────┘ │
└────────────────────────────────────────┘
```

### 7.2 打包部署

```bash
# 后端打包
mvn clean package -DskipTests

# 生成: backend/target/sql-lineage-analyzer-1.0.0.jar

# 运行
java -jar sql-lineage-analyzer-1.0.0.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

---

## 8. 开发环境要求

| 环境 | 版本要求 |
|------|---------|
| JDK | 8+ |
| Maven | 3.6+ |
| IDE | IntelliJ IDEA 2021+ |
| 数据库 | MySQL 8.0+ (生产) / H2 (开发) |
| 浏览器 | Chrome 90+ / Firefox 88+ / Edge 90+ |

---

## 9. 后续优化方向

### 第二期计划

1. **更多SQL类型支持**
   - CREATE VIEW 血缘分析
   - INSERT INTO SELECT 数据写入血缘
   - MERGE/UPSERT 语句

2. **性能优化**
   - SQL解析结果缓存（Redis）
   - 增量分析（只分析变化部分）
   - 分布式处理（多节点并行）

3. **功能增强**
   - 血缘影响分析（字段修改影响评估）
   - 血缘对比（版本差异）
   - 血缘搜索（找出某字段的所有引用）

4. **企业级特性**
   - 多租户隔离
   - 权限管理
   - 审计日志

---

## 附录: 技术选型对比

### A1. SQL解析器对比

| 解析器 | 优点 | 缺点 | 结论 |
|-------|------|------|------|
| **Alibaba Druid** | ✅ 多方言支持<br>✅ 国产文档友好<br>✅ AST访问者模式 | ❌ Spark SQL支持有限 | ✅ **选择** |
| Apache Calcite | ✅ 功能强大<br>✅ 扩展性好 | ❌ 学习曲线陡峭<br>❌ 文档复杂 | ❌ 不选 |
| JSqlParser | ✅ 纯Java<br>✅ 轻量级 | ❌ 方言支持少<br>❌ 复杂SQL解析弱 | ❌ 不选 |

### A2. 前端图形库对比

| 图形库 | 优点 | 缺点 | 结论 |
|-------|------|------|------|
| **AntV G6** | ✅ 专业图形引擎<br>✅ 国产支持好<br>✅ DAG布局优秀 | ❌ 体积较大(~200KB) | ✅ **选择** |
| D3.js | ✅ 灵活强大<br>✅ 生态丰富 | ❌ 学习成本高<br>❌ 需要自己实现布局 | ❌ 不选 |
| Cytoscape.js | ✅ 图算法丰富 | ❌ 样式定制复杂 | ❌ 不选 |

---

**文档状态**: ✅ 已完成  
**下一步**: 进入数据库设计阶段 (T0.2.1.1)
