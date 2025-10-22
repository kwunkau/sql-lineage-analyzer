# SQL字段级血缘分析平台 - 数据库ER图

> 文档版本: v1.0  
> 创建时间: 2025-10-23  
> 数据库: MySQL 8.0+ / H2 2.1+

---

## 1. ER图概览

```
┌─────────────────────────────────────────────────────────────────────┐
│                          元数据管理域                                 │
└─────────────────────────────────────────────────────────────────────┘

   ┌──────────────────┐
   │metadata_database │ 数据库元数据
   │─────────────────│
   │ PK id            │
   │    db_name       │
   │    db_type       │
   │    description   │
   └────────┬─────────┘
            │ 1
            │
            │ N
   ┌────────┴─────────┐
   │ metadata_table   │ 表元数据
   │──────────────────│
   │ PK id            │
   │ FK database_id   │
   │    table_name    │
   │    table_type    │
   │    description   │
   └────────┬─────────┘
            │ 1
            │
            │ N
   ┌────────┴──────────┐
   │ metadata_column   │ 字段元数据
   │───────────────────│
   │ PK id             │
   │ FK table_id       │
   │    column_name    │
   │    data_type      │
   │    is_nullable    │
   │    is_primary_key │
   └───────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                        血缘分析域                                     │
└─────────────────────────────────────────────────────────────────────┘

   ┌───────────────────┐
   │lineage_analysis   │ 血缘分析任务
   │───────────────────│
   │ PK id             │
   │    task_id        │
   │    sql_text       │
   │    sql_hash       │
   │    db_type        │
   │    status         │
   │    result_json    │
   └────────┬──────────┘
            │ 1
            │
            ├────────────────────────────┬────────────────────────┐
            │ N                          │ N                      │
   ┌────────┴─────────────┐    ┌────────┴─────────────┐  ┌───────┴────────┐
   │lineage_field_relation│    │lineage_table_relation│  │kettle_extracted│
   │──────────────────────│    │──────────────────────│  │_sql            │
   │ PK id                │    │ PK id                │  │────────────────│
   │ FK analysis_id       │    │ FK analysis_id       │  │ PK id          │
   │    target_field      │    │    source_table      │  │ FK analysis_id │
   │    source_table      │    │    target_table      │  │    sql_text    │
   │    source_column     │    │    join_type         │  └────────────────┘
   │    transform_expr    │    │    join_condition    │
   └──────────────────────┘    └──────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                         Kettle管理域                                  │
└─────────────────────────────────────────────────────────────────────┘

   ┌───────────────────┐
   │kettle_file_history│ Kettle文件历史
   │───────────────────│
   │ PK id             │
   │    file_name      │
   │    file_path      │
   │    file_hash      │
   │    sql_count      │
   │    status         │
   └────────┬──────────┘
            │ 1
            │
            │ N
   ┌────────┴──────────┐
   │kettle_extracted   │
   │_sql               │
   │───────────────────│
   │ PK id             │
   │ FK file_id        │
   │ FK analysis_id    │
   │    step_name      │
   │    sql_text       │
   └───────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                         系统管理域                                    │
└─────────────────────────────────────────────────────────────────────┘

   ┌───────────────┐          ┌─────────────────┐
   │system_config  │          │operation_log    │
   │───────────────│          │─────────────────│
   │ PK id         │          │ PK id           │
   │    config_key │          │    user_name    │
   │    config_val │          │    operation    │
   │    config_type│          │    module       │
   └───────────────┘          │    success      │
                              └─────────────────┘
```

---

## 2. 详细表关系说明

### 2.1 元数据管理域

#### 关系链
```
metadata_database (1) ──→ (N) metadata_table
metadata_table (1) ──→ (N) metadata_column
```

**业务含义**：
- 一个数据库包含多个表
- 一个表包含多个字段
- 支持多数据库类型（Hive、MySQL、Spark）

**典型查询场景**：
```sql
-- 查询某数据库下所有表和字段
SELECT 
    d.db_name,
    t.table_name,
    c.column_name,
    c.data_type
FROM metadata_database d
JOIN metadata_table t ON d.id = t.database_id
JOIN metadata_column c ON t.id = c.table_id
WHERE d.db_name = 'production' AND d.db_type = 'hive';
```

### 2.2 血缘分析域

#### 核心关系
```
lineage_analysis (1) ──→ (N) lineage_field_relation
lineage_analysis (1) ──→ (N) lineage_table_relation
```

**业务含义**：
- 一次SQL分析任务生成多条字段级血缘关系
- 一次SQL分析任务涉及多个表级关系（JOIN）

**数据流转**：
```
用户提交SQL
    ↓
创建 lineage_analysis 记录（status=PENDING）
    ↓
解析SQL，提取血缘关系
    ↓
插入 lineage_field_relation（字段依赖）
插入 lineage_table_relation（表关联）
    ↓
更新 lineage_analysis (status=SUCCESS, result_json=...)
```

**典型查询场景**：
```sql
-- 查询某字段的所有源字段
SELECT 
    lfr.target_field,
    lfr.source_table,
    lfr.source_column,
    lfr.transform_expression
FROM lineage_analysis la
JOIN lineage_field_relation lfr ON la.id = lfr.analysis_id
WHERE la.task_id = 'task-20251023-001'
  AND lfr.target_field = 'user_name';
```

### 2.3 Kettle管理域

#### 关系链
```
kettle_file_history (1) ──→ (N) kettle_extracted_sql
kettle_extracted_sql (N) ──→ (1) lineage_analysis
```

**业务含义**：
- 一个Kettle文件可以提取多条SQL
- 每条SQL可以进行独立的血缘分析

**数据流转**：
```
上传Kettle文件
    ↓
创建 kettle_file_history 记录
    ↓
解析XML，提取SQL
    ↓
插入 kettle_extracted_sql (N条记录)
    ↓
对每条SQL进行血缘分析
    ↓
关联 analysis_id
```

---

## 3. 核心表字段说明

### 3.1 lineage_analysis（血缘分析任务表）

| 字段 | 类型 | 说明 | 示例 |
|-----|------|------|------|
| id | BIGINT | 主键 | 1001 |
| task_id | VARCHAR(50) | 任务唯一标识 | task-20251023-001 |
| sql_text | TEXT | SQL语句 | SELECT t1.id, t2.name... |
| sql_hash | VARCHAR(64) | SQL的MD5哈希 | a1b2c3d4e5f6... |
| db_type | VARCHAR(20) | 数据库类型 | hive / mysql / spark |
| status | VARCHAR(20) | 分析状态 | PENDING/RUNNING/SUCCESS/FAILED |
| result_json | LONGTEXT | 血缘结果（JSON） | {"fields":[...], "tables":[...]} |
| error_message | TEXT | 错误信息 | SQL语法错误: line 5... |
| analysis_time_ms | INT | 分析耗时（毫秒） | 1250 |
| field_count | INT | 输出字段数 | 15 |
| table_count | INT | 涉及表数 | 5 |

**status 状态机**：
```
PENDING → RUNNING → SUCCESS
                  ↘ FAILED
```

### 3.2 lineage_field_relation（字段血缘关系表）

| 字段 | 类型 | 说明 | 示例 |
|-----|------|------|------|
| analysis_id | BIGINT | 关联任务ID | 1001 |
| target_field | VARCHAR(200) | 目标字段 | user_id |
| target_alias | VARCHAR(200) | 目标别名 | uid |
| source_table | VARCHAR(100) | 源表名 | users |
| source_table_alias | VARCHAR(100) | 源表别名 | u |
| source_column | VARCHAR(100) | 源字段名 | id |
| transform_expression | TEXT | 转换表达式 | CAST(id AS STRING) |
| dependency_level | INT | 依赖层级 | 1（直接）, 2（间接） |

**示例数据**：
```
分析SQL: SELECT u.id AS uid, o.amount * 1.1 AS total FROM users u JOIN orders o ON u.id = o.user_id

生成记录：
| target_field | source_table | source_column | transform_expression | dependency_level |
|--------------|--------------|---------------|---------------------|------------------|
| uid          | users        | id            | NULL                | 1                |
| total        | orders       | amount        | amount * 1.1        | 1                |
```

### 3.3 lineage_table_relation（表级血缘关系表）

| 字段 | 类型 | 说明 | 示例 |
|-----|------|------|------|
| analysis_id | BIGINT | 关联任务ID | 1001 |
| source_table | VARCHAR(100) | 源表名 | users |
| source_table_alias | VARCHAR(100) | 源表别名 | u |
| target_table | VARCHAR(100) | 目标表名 | result_table（INSERT场景） |
| join_type | VARCHAR(20) | JOIN类型 | INNER/LEFT/RIGHT/FULL |
| join_condition | TEXT | JOIN条件 | u.id = o.user_id |
| table_type | VARCHAR(20) | 表类型 | BASE/SUBQUERY/CTE/VIEW |

---

## 4. 索引策略

### 4.1 查询性能优化索引

```sql
-- 1. 按SQL哈希查询重复分析
CREATE INDEX idx_sql_hash ON lineage_analysis(sql_hash);

-- 2. 按状态和时间范围查询任务
CREATE INDEX idx_status_time ON lineage_analysis(status, created_at);

-- 3. 按目标字段查询血缘（高频查询）
CREATE INDEX idx_target_field ON lineage_field_relation(target_field, analysis_id);

-- 4. 按源表查询影响分析（重要）
CREATE INDEX idx_source_table ON lineage_field_relation(source_table, source_column);

-- 5. 表关联查询
CREATE INDEX idx_table_relation ON lineage_table_relation(source_table, target_table);
```

### 4.2 索引使用场景

| 查询场景 | 使用的索引 | 查询示例 |
|---------|-----------|---------|
| 查找某字段的所有引用 | idx_source_table | WHERE source_table='users' AND source_column='id' |
| 查找某字段的所有来源 | idx_target_field | WHERE target_field='user_name' |
| 去重分析（避免重复） | idx_sql_hash | WHERE sql_hash='a1b2c3...' |
| 查询失败任务 | idx_status_time | WHERE status='FAILED' AND created_at > '2025-10-01' |

---

## 5. 数据量估算

### 5.1 容量规划（按1年使用周期）

| 表名 | 单条记录大小 | 日增长量 | 年数据量 | 存储空间 |
|-----|-------------|---------|---------|---------|
| metadata_database | 0.5KB | 5条 | 1,825条 | 1MB |
| metadata_table | 0.5KB | 50条 | 18,250条 | 10MB |
| metadata_column | 0.5KB | 500条 | 182,500条 | 100MB |
| lineage_analysis | 10KB | 500条 | 182,500条 | **1.8GB** |
| lineage_field_relation | 0.5KB | 5,000条 | 1,825,000条 | **1GB** |
| lineage_table_relation | 0.5KB | 2,000条 | 730,000条 | 400MB |
| operation_log | 1KB | 1,000条 | 365,000条 | 400MB |

**总计**: 约 **3.7GB/年** （不含Kettle文件存储）

### 5.2 分区建议

```sql
-- 对大表进行分区（MySQL 8.0+）
ALTER TABLE lineage_analysis 
PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202510 VALUES LESS THAN (202511),
    PARTITION p202511 VALUES LESS THAN (202512),
    PARTITION p202512 VALUES LESS THAN (202601),
    PARTITION p202601 VALUES LESS THAN MAXVALUE
);
```

---

## 6. 视图设计

### 6.1 完整血缘视图（常用查询简化）

```sql
CREATE VIEW v_lineage_full AS
SELECT 
    la.task_id,
    la.sql_text,
    la.db_type,
    la.status,
    lfr.target_field,
    lfr.target_alias,
    lfr.source_table,
    lfr.source_column,
    lfr.transform_expression,
    lfr.dependency_level,
    ltr.join_type,
    ltr.join_condition
FROM lineage_analysis la
LEFT JOIN lineage_field_relation lfr ON la.id = lfr.analysis_id
LEFT JOIN lineage_table_relation ltr ON la.id = ltr.analysis_id;
```

### 6.2 血缘统计仪表盘视图

```sql
CREATE VIEW v_lineage_dashboard AS
SELECT 
    DATE(created_at) AS stat_date,
    db_type,
    COUNT(*) AS total_tasks,
    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
    AVG(analysis_time_ms) AS avg_time_ms,
    MAX(analysis_time_ms) AS max_time_ms,
    AVG(field_count) AS avg_fields,
    AVG(table_count) AS avg_tables
FROM lineage_analysis
WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(created_at), db_type;
```

---

## 7. 数据完整性约束

### 7.1 外键约束
```
metadata_table.database_id → metadata_database.id
metadata_column.table_id → metadata_table.id
lineage_field_relation.analysis_id → lineage_analysis.id
lineage_table_relation.analysis_id → lineage_analysis.id
kettle_extracted_sql.file_id → kettle_file_history.id
kettle_extracted_sql.analysis_id → lineage_analysis.id
```

### 7.2 唯一性约束
```
metadata_database: (db_name, db_type, is_deleted)
metadata_table: (database_id, table_name, is_deleted)
metadata_column: (table_id, column_name, is_deleted)
lineage_analysis: (task_id)
system_config: (config_key)
```

### 7.3 逻辑删除约束
- 所有元数据表使用 `is_deleted` 字段
- 删除时设置 `is_deleted=1`，不物理删除
- 唯一约束包含 `is_deleted` 字段

---

## 8. 备份和恢复策略

### 8.1 备份策略
```bash
# 全量备份（每日）
mysqldump -u root -p lineage_db > backup_$(date +%Y%m%d).sql

# 增量备份（每小时）
mysqlbinlog --start-datetime="$(date -d '1 hour ago' '+%Y-%m-%d %H:%M:%S')" \
  /var/lib/mysql/mysql-bin.000001 > incremental_backup.sql
```

### 8.2 数据归档
```sql
-- 归档180天前的分析记录到历史表
INSERT INTO lineage_analysis_archive 
SELECT * FROM lineage_analysis 
WHERE created_at < DATE_SUB(CURDATE(), INTERVAL 180 DAY);

DELETE FROM lineage_analysis 
WHERE created_at < DATE_SUB(CURDATE(), INTERVAL 180 DAY);
```

---

**文档状态**: ✅ 已完成  
**相关文件**: schema.sql  
**下一步**: T0.2.1.2 - 创建初始化数据SQL
