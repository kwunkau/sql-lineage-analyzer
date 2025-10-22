# SQL字段级血缘分析平台 - 开发规范

> 文档版本: v1.0  
> 创建时间: 2025-10-23  
> 适用范围: 后端Java代码 + 前端JavaScript代码  
> 基准: 阿里巴巴Java开发手册 + Google JavaScript Style Guide

---

## 1. 通用规范

### 1.1 文件编码
- ✅ 统一使用 **UTF-8** 编码
- ✅ 行尾符：LF（\n）
- ✅ 文件末尾保留一个空行

### 1.2 缩进和空格
- ✅ Java: 4个空格缩进（不使用Tab）
- ✅ JavaScript: 2个空格缩进
- ✅ YAML/JSON: 2个空格缩进
- ✅ 运算符前后加空格：`a + b`，不是 `a+b`
- ✅ 逗号后加空格：`func(a, b, c)`

---

## 2. Java 代码规范

### 2.1 命名规范

| 类型 | 规范 | 示例 |
|-----|------|------|
| **类名** | 大驼峰（UpperCamelCase） | `LineageAnalyzer`, `DruidParserService` |
| **方法名** | 小驼峰（lowerCamelCase） | `analyzeSQL()`, `parseExpression()` |
| **变量名** | 小驼峰 | `fieldName`, `targetTable` |
| **常量名** | 全大写，下划线分隔 | `MAX_RETRY_COUNT`, `DEFAULT_DB_TYPE` |
| **包名** | 全小写，单数形式 | `com.lineage.core`, `com.lineage.service` |
| **枚举类** | 大驼峰 | `DbType`, `TaskStatus` |
| **枚举值** | 全大写 | `DbType.MYSQL`, `TaskStatus.COMPLETED` |

**特殊前缀约定**：
- ❌ 不使用匈牙利命名法（如 `strName`, `intCount`）
- ✅ 布尔变量以 `is/has/can` 开头：`isValid`, `hasError`, `canParse`
- ✅ 集合变量以复数结尾：`tableList`, `fieldNames`

### 2.2 类和方法设计

#### 类设计原则
```java
/**
 * 血缘分析核心引擎
 * 
 * @author AI Task Engine
 * @version 1.0
 * @since 2025-10-23
 */
@Service
@Slf4j
public class LineageAnalyzer {
    
    // 1. 常量定义
    private static final int MAX_RECURSION_DEPTH = 100;
    
    // 2. 依赖注入
    @Autowired
    private DruidParserService parserService;
    
    // 3. 公共方法
    public LineageResult analyze(String sql, String dbType) {
        // 实现...
    }
    
    // 4. 私有方法
    private List<SourceField> resolveExpression(SQLExpr expr) {
        // 实现...
    }
}
```

#### 方法设计原则
- ✅ 单一职责：一个方法只做一件事
- ✅ 参数不超过5个，超过则封装为对象
- ✅ 方法长度不超过80行
- ✅ 圈复杂度不超过10

```java
// ❌ 错误示例：参数过多
public void process(String sql, String dbType, String tableName, 
                   String columnName, boolean isNullable, String dataType) {
}

// ✅ 正确示例：封装为对象
public void process(AnalyzeRequest request) {
}
```

### 2.3 注释规范

#### 类注释
```java
/**
 * Druid SQL解析服务
 * <p>
 * 支持多种数据库方言的SQL解析，包括：
 * <ul>
 *   <li>MySQL / MariaDB</li>
 *   <li>Apache Hive</li>
 *   <li>Apache Spark SQL</li>
 * </ul>
 * 
 * @author AI Task Engine
 * @version 1.0
 * @since 2025-10-23
 */
public class DruidParserService {
}
```

#### 方法注释
```java
/**
 * 解析SQL语句并返回AST
 * 
 * @param sql SQL语句，不能为空
 * @param dbType 数据库类型，支持 mysql/hive/spark
 * @return SQL抽象语法树
 * @throws SQLParseException 当SQL语法错误时
 * @throws IllegalArgumentException 当dbType不支持时
 */
public SQLStatement parseSQL(String sql, String dbType) {
    // 实现...
}
```

#### 行内注释
```java
// ✅ 好的注释：解释"为什么"
// 使用ConcurrentHashMap避免并发修改异常
private Map<String, FieldNode> fieldCache = new ConcurrentHashMap<>();

// ❌ 坏的注释：重复代码逻辑
// 创建一个新的ArrayList
List<String> list = new ArrayList<>();
```

### 2.4 异常处理

```java
// ✅ 正确的异常处理
public LineageResult analyze(String sql, String dbType) {
    try {
        SQLStatement stmt = parserService.parseSQL(sql, dbType);
        return doAnalyze(stmt);
    } catch (SQLParseException e) {
        log.error("SQL解析失败: {}", sql, e);
        throw new BusinessException("SQL语法错误: " + e.getMessage());
    } catch (Exception e) {
        log.error("血缘分析异常", e);
        throw new SystemException("系统错误，请联系管理员");
    }
}

// ❌ 错误示例：吞掉异常
try {
    // ...
} catch (Exception e) {
    // 什么都不做
}

// ❌ 错误示例：打印后继续
try {
    // ...
} catch (Exception e) {
    e.printStackTrace();
}
```

### 2.5 日志规范

```java
@Slf4j
public class LineageAnalyzer {
    
    public LineageResult analyze(String sql, String dbType) {
        // ✅ INFO: 业务流程关键节点
        log.info("开始分析SQL血缘, dbType={}, sqlLength={}", dbType, sql.length());
        
        // ✅ DEBUG: 详细调试信息
        log.debug("解析后的AST: {}", stmt);
        
        // ✅ WARN: 可恢复的异常情况
        if (fields.isEmpty()) {
            log.warn("未识别到任何字段, sql={}", sql);
        }
        
        // ✅ ERROR: 错误和异常
        if (error) {
            log.error("血缘分析失败, sql={}", sql, exception);
        }
        
        // ❌ 错误：不要使用System.out.println
        // System.out.println("调试信息");
    }
}
```

### 2.6 代码格式

#### 括号风格（K&R风格）
```java
// ✅ 正确
public void method() {
    if (condition) {
        // ...
    } else {
        // ...
    }
}

// ❌ 错误
public void method()
{
    if (condition)
    {
        // ...
    }
}
```

#### 空行规则
```java
public class Example {
    // 字段后空一行
    private int count;
    
    // 方法间空一行
    public void method1() {
        // ...
    }
    
    public void method2() {
        // 逻辑块间空一行
        int x = 1;
        
        if (x > 0) {
            // ...
        }
        
        return result;
    }
}
```

---

## 3. JavaScript 代码规范

### 3.1 命名规范

| 类型 | 规范 | 示例 |
|-----|------|------|
| **变量名** | 小驼峰 | `fieldName`, `lineageData` |
| **常量名** | 全大写，下划线分隔 | `API_BASE_URL`, `MAX_NODES` |
| **函数名** | 小驼峰 | `analyzeSQL()`, `renderGraph()` |
| **类名** | 大驼峰 | `LineageVisualizer`, `ApiClient` |
| **文件名** | 小写，连字符分隔 | `lineage-visualizer.js`, `api-client.js` |

### 3.2 变量声明

```javascript
// ✅ 使用 const/let，不使用 var
const API_URL = '/api/lineage/analyze';
let currentGraph = null;

// ✅ 一行一个声明
const name = 'test';
const age = 18;

// ❌ 避免多个声明在一行
const a = 1, b = 2, c = 3;
```

### 3.3 函数定义

```javascript
// ✅ 使用箭头函数（简短函数）
const sum = (a, b) => a + b;

// ✅ 使用普通函数（需要 this 绑定）
function LineageVisualizer() {
  this.graph = null;
}

// ✅ 函数注释
/**
 * 分析SQL并渲染血缘图
 * @param {string} sql - SQL语句
 * @param {string} dbType - 数据库类型
 * @returns {Promise<Object>} 血缘分析结果
 */
async function analyzeAndRender(sql, dbType) {
  const result = await apiClient.analyze(sql, dbType);
  visualizer.render(result);
  return result;
}
```

### 3.4 代码格式

```javascript
// ✅ 对象和数组
const config = {
  width: 800,
  height: 600,
  layout: {
    type: 'dagre',
    rankdir: 'LR'
  }
};

const items = [
  'item1',
  'item2',
  'item3'
];

// ✅ 条件语句
if (condition) {
  // ...
} else if (otherCondition) {
  // ...
} else {
  // ...
}

// ✅ 循环
for (const item of items) {
  console.log(item);
}

// ✅ 异步处理
try {
  const result = await fetchData();
  processResult(result);
} catch (error) {
  console.error('Error:', error);
}
```

---

## 4. 数据库规范

### 4.1 表名和字段命名

```sql
-- ✅ 表名：小写，下划线分隔，复数形式
CREATE TABLE metadata_tables (
    id BIGINT PRIMARY KEY,
    table_name VARCHAR(100),
    created_at TIMESTAMP
);

-- ✅ 字段名：小写，下划线分隔
CREATE TABLE lineage_field_relations (
    source_table_name VARCHAR(100),
    source_column_name VARCHAR(100),
    target_column_name VARCHAR(100)
);
```

### 4.2 SQL语句规范

```sql
-- ✅ 关键字大写，表名/字段小写
SELECT 
    t.table_name,
    c.column_name,
    c.data_type
FROM metadata_tables t
LEFT JOIN metadata_columns c ON t.id = c.table_id
WHERE t.db_type = 'mysql'
ORDER BY t.table_name;

-- ✅ 复杂查询分行，对齐
SELECT 
    COUNT(*) AS total_count,
    AVG(analysis_time_ms) AS avg_time
FROM lineage_analysis
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(created_at)
HAVING total_count > 10;
```

---

## 5. Git 提交规范

### 5.1 提交消息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型**：
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式（不影响代码运行）
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**：
```
feat(lineage): 实现JOIN语句的字段血缘分析

- 添加SQLJoinVisitor类
- 支持INNER/LEFT/RIGHT JOIN
- 单元测试覆盖率85%

Closes #123
```

### 5.2 分支命名

```
feature/lineage-join-support
bugfix/sql-parse-error
hotfix/memory-leak
refactor/service-layer
```

---

## 6. 测试规范

### 6.1 单元测试

```java
@SpringBootTest
class LineageAnalyzerTest {
    
    @Autowired
    private LineageAnalyzer analyzer;
    
    @Test
    @DisplayName("应该正确解析简单SELECT语句")
    void shouldParseSimpleSelect() {
        // Given
        String sql = "SELECT id, name FROM users";
        String dbType = "mysql";
        
        // When
        LineageResult result = analyzer.analyze(sql, dbType);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.getFields().size());
        assertTrue(result.getTables().contains("users"));
    }
    
    @Test
    @DisplayName("应该抛出异常当SQL语法错误时")
    void shouldThrowExceptionWhenSQLInvalid() {
        // Given
        String invalidSQL = "SELECT FROM";
        
        // When & Then
        assertThrows(SQLParseException.class, () -> {
            analyzer.analyze(invalidSQL, "mysql");
        });
    }
}
```

### 6.2 测试覆盖率要求

- ✅ 核心业务逻辑: **≥ 80%**
- ✅ 工具类: **≥ 70%**
- ✅ 控制器: **≥ 60%**

---

## 7. 性能规范

### 7.1 避免常见性能问题

```java
// ❌ 错误：字符串拼接使用 +
String sql = "";
for (String part : parts) {
    sql = sql + part;  // 每次创建新对象
}

// ✅ 正确：使用 StringBuilder
StringBuilder sb = new StringBuilder();
for (String part : parts) {
    sb.append(part);
}
String sql = sb.toString();

// ❌ 错误：在循环中查询数据库
for (String tableName : tables) {
    Table table = tableMapper.selectByName(tableName);  // N+1问题
}

// ✅ 正确：批量查询
List<Table> tables = tableMapper.selectByNames(tableNames);
```

### 7.2 缓存使用

```java
@Service
public class MetadataService {
    
    // ✅ 使用缓存减少数据库查询
    @Cacheable(value = "tables", key = "#tableName")
    public Table getTableByName(String tableName) {
        return tableMapper.selectByName(tableName);
    }
    
    @CacheEvict(value = "tables", key = "#table.tableName")
    public void updateTable(Table table) {
        tableMapper.update(table);
    }
}
```

---

## 8. 安全规范

### 8.1 SQL注入防护

```java
// ❌ 危险：字符串拼接SQL
String sql = "SELECT * FROM users WHERE name = '" + userName + "'";

// ✅ 安全：使用参数化查询
@Select("SELECT * FROM users WHERE name = #{userName}")
User selectByName(@Param("userName") String userName);
```

### 8.2 敏感信息处理

```yaml
# ✅ 配置文件不包含明文密码
spring:
  datasource:
    password: ${DB_PASSWORD}  # 从环境变量读取

# ❌ 不要提交到Git
spring:
  datasource:
    password: admin123
```

```java
// ✅ 日志脱敏
log.info("用户登录: username={}", maskSensitive(username));

private String maskSensitive(String value) {
    if (value.length() <= 4) return "****";
    return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
}
```

---

## 9. 文档规范

### 9.1 README.md 结构

```markdown
# 项目名称

简要描述（一句话）

## 功能特性
- 特性1
- 特性2

## 快速开始
### 环境要求
### 安装步骤
### 运行

## API文档
链接或说明

## 技术栈

## 贡献指南

## 许可证
```

### 9.2 API文档

```java
/**
 * 血缘分析API
 */
@RestController
@RequestMapping("/api/lineage")
@Api(tags = "血缘分析")
public class LineageController {
    
    @PostMapping("/analyze")
    @ApiOperation(value = "分析SQL字段血缘", notes = "支持Hive/MySQL/Spark SQL")
    @ApiResponses({
        @ApiResponse(code = 200, message = "分析成功"),
        @ApiResponse(code = 400, message = "SQL语法错误"),
        @ApiResponse(code = 500, message = "服务器错误")
    })
    public Result<LineageResult> analyze(
        @ApiParam(value = "SQL语句", required = true) @RequestBody String sql,
        @ApiParam(value = "数据库类型", required = true) @RequestParam String dbType
    ) {
        // 实现...
    }
}
```

---

## 10. 代码审查检查清单

### 提交前自检

- [ ] 代码符合命名规范
- [ ] 注释完整且准确
- [ ] 无 System.out.println / console.log（改用日志）
- [ ] 异常处理完善
- [ ] 单元测试已编写且通过
- [ ] 无编译警告
- [ ] 代码格式化（Ctrl+Alt+L / Prettier）
- [ ] 无敏感信息（密码、密钥）
- [ ] Git提交消息符合规范

---

**文档状态**: ✅ 已完成  
**下一步**: T0.2.1.1 - 设计元数据表结构
