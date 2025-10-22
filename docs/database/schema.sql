-- ============================================================
-- SQL字段级血缘分析平台 - 数据库建表脚本
-- 版本: v1.0
-- 创建时间: 2025-10-23
-- 数据库: MySQL 8.0+ / H2 2.1+
-- ============================================================

-- ============================================================
-- 1. 元数据管理表
-- ============================================================

-- 1.1 数据库元数据表
CREATE TABLE metadata_database (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    db_name VARCHAR(100) NOT NULL COMMENT '数据库名称',
    db_type VARCHAR(20) NOT NULL COMMENT '数据库类型: mysql/hive/spark',
    description TEXT COMMENT '数据库描述',
    connection_url VARCHAR(500) COMMENT '连接URL（可选）',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_db_name_type (db_name, db_type, is_deleted)
) COMMENT '数据库元数据表';

-- 1.2 表元数据表
CREATE TABLE metadata_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    database_id BIGINT NOT NULL COMMENT '所属数据库ID',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    table_type VARCHAR(20) DEFAULT 'TABLE' COMMENT '表类型: TABLE/VIEW',
    description TEXT COMMENT '表描述',
    row_count BIGINT DEFAULT 0 COMMENT '表行数（估算值）',
    storage_size_mb DECIMAL(10,2) COMMENT '存储大小(MB)',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (database_id) REFERENCES metadata_database(id),
    UNIQUE KEY uk_db_table (database_id, table_name, is_deleted),
    INDEX idx_table_name (table_name)
) COMMENT '表元数据表';

-- 1.3 字段元数据表
CREATE TABLE metadata_column (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    table_id BIGINT NOT NULL COMMENT '所属表ID',
    column_name VARCHAR(100) NOT NULL COMMENT '字段名',
    data_type VARCHAR(50) NOT NULL COMMENT '数据类型: VARCHAR/INT/BIGINT等',
    column_length INT COMMENT '字段长度',
    column_precision INT COMMENT '数字精度',
    column_scale INT COMMENT '数字标度',
    is_nullable TINYINT DEFAULT 1 COMMENT '是否可为空: 0-NOT NULL, 1-NULL',
    is_primary_key TINYINT DEFAULT 0 COMMENT '是否主键: 0-否, 1-是',
    default_value VARCHAR(500) COMMENT '默认值',
    column_comment TEXT COMMENT '字段注释',
    ordinal_position INT NOT NULL COMMENT '字段顺序位置',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (table_id) REFERENCES metadata_table(id),
    UNIQUE KEY uk_table_column (table_id, column_name, is_deleted),
    INDEX idx_column_name (column_name)
) COMMENT '字段元数据表';

-- ============================================================
-- 2. 血缘分析结果表
-- ============================================================

-- 2.1 血缘分析任务表
CREATE TABLE lineage_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id VARCHAR(50) UNIQUE NOT NULL COMMENT '任务唯一标识',
    sql_text TEXT NOT NULL COMMENT 'SQL语句',
    sql_hash VARCHAR(64) COMMENT 'SQL的MD5哈希值（用于去重）',
    db_type VARCHAR(20) NOT NULL COMMENT '数据库类型',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '分析状态: PENDING/RUNNING/SUCCESS/FAILED',
    result_json LONGTEXT COMMENT '血缘分析结果（JSON格式）',
    error_message TEXT COMMENT '错误信息',
    analysis_time_ms INT COMMENT '分析耗时（毫秒）',
    field_count INT COMMENT '输出字段数量',
    table_count INT COMMENT '涉及表数量',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    completed_at TIMESTAMP COMMENT '完成时间',
    INDEX idx_status (status),
    INDEX idx_sql_hash (sql_hash),
    INDEX idx_created_at (created_at)
) COMMENT '血缘分析任务表';

-- 2.2 字段血缘关系表（详细记录）
CREATE TABLE lineage_field_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    analysis_id BIGINT NOT NULL COMMENT '分析任务ID',
    target_field VARCHAR(200) NOT NULL COMMENT '目标字段（输出字段）',
    target_alias VARCHAR(200) COMMENT '目标字段别名',
    source_database VARCHAR(100) COMMENT '源数据库名',
    source_table VARCHAR(100) COMMENT '源表名',
    source_table_alias VARCHAR(100) COMMENT '源表别名',
    source_column VARCHAR(100) COMMENT '源字段名',
    transform_expression TEXT COMMENT '转换表达式（如果有函数/运算）',
    dependency_level INT DEFAULT 1 COMMENT '依赖层级：1-直接依赖，2-二级依赖...',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (analysis_id) REFERENCES lineage_analysis(id),
    INDEX idx_target_field (target_field),
    INDEX idx_source_table (source_table),
    INDEX idx_source_column (source_column)
) COMMENT '字段血缘关系明细表';

-- 2.3 表级血缘关系表
CREATE TABLE lineage_table_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    analysis_id BIGINT NOT NULL COMMENT '分析任务ID',
    source_table VARCHAR(100) NOT NULL COMMENT '源表名',
    source_table_alias VARCHAR(100) COMMENT '源表别名',
    target_table VARCHAR(100) COMMENT '目标表名（如果是INSERT/CREATE）',
    join_type VARCHAR(20) COMMENT 'JOIN类型: INNER/LEFT/RIGHT/FULL',
    join_condition TEXT COMMENT 'JOIN条件',
    table_type VARCHAR(20) DEFAULT 'BASE' COMMENT '表类型: BASE/SUBQUERY/CTE/VIEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (analysis_id) REFERENCES lineage_analysis(id),
    INDEX idx_source_table (source_table),
    INDEX idx_target_table (target_table)
) COMMENT '表级血缘关系表';

-- ============================================================
-- 3. Kettle文件管理表
-- ============================================================

-- 3.1 Kettle文件上传历史表
CREATE TABLE kettle_file_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) COMMENT '文件路径',
    file_size BIGINT COMMENT '文件大小（字节）',
    file_hash VARCHAR(64) COMMENT '文件MD5哈希',
    file_type VARCHAR(10) COMMENT '文件类型: ktr/kjb',
    sql_count INT DEFAULT 0 COMMENT '提取的SQL数量',
    status VARCHAR(20) DEFAULT 'UPLOADED' COMMENT '处理状态: UPLOADED/PARSING/SUCCESS/FAILED',
    error_message TEXT COMMENT '错误信息',
    uploaded_by VARCHAR(50) COMMENT '上传人',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    processed_at TIMESTAMP COMMENT '处理完成时间',
    INDEX idx_file_name (file_name),
    INDEX idx_file_hash (file_hash)
) COMMENT 'Kettle文件上传历史表';

-- 3.2 Kettle提取的SQL语句表
CREATE TABLE kettle_extracted_sql (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    file_id BIGINT NOT NULL COMMENT '关联的Kettle文件ID',
    step_name VARCHAR(255) COMMENT 'Kettle步骤名称',
    step_type VARCHAR(50) COMMENT '步骤类型: TableInput/SQLFileOutput等',
    sql_text TEXT NOT NULL COMMENT '提取的SQL语句',
    sql_hash VARCHAR(64) COMMENT 'SQL哈希值',
    analysis_id BIGINT COMMENT '关联的血缘分析ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (file_id) REFERENCES kettle_file_history(id),
    FOREIGN KEY (analysis_id) REFERENCES lineage_analysis(id),
    INDEX idx_file_id (file_id),
    INDEX idx_sql_hash (sql_hash)
) COMMENT 'Kettle提取的SQL语句表';

-- ============================================================
-- 4. 系统配置和日志表
-- ============================================================

-- 4.1 系统配置表
CREATE TABLE system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_key VARCHAR(100) UNIQUE NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(20) DEFAULT 'STRING' COMMENT '配置类型: STRING/INT/BOOLEAN/JSON',
    description VARCHAR(500) COMMENT '配置说明',
    is_encrypted TINYINT DEFAULT 0 COMMENT '是否加密: 0-否, 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '系统配置表';

-- 4.2 操作日志表
CREATE TABLE operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_name VARCHAR(50) COMMENT '操作用户',
    operation_type VARCHAR(50) COMMENT '操作类型: ANALYZE/IMPORT/EXPORT/DELETE',
    operation_module VARCHAR(50) COMMENT '操作模块: LINEAGE/METADATA/KETTLE',
    operation_desc TEXT COMMENT '操作描述',
    request_params TEXT COMMENT '请求参数（JSON）',
    response_result TEXT COMMENT '响应结果（JSON）',
    success TINYINT COMMENT '是否成功: 0-失败, 1-成功',
    error_message TEXT COMMENT '错误信息',
    execution_time_ms INT COMMENT '执行耗时（毫秒）',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_name (user_name),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
) COMMENT '操作日志表';

-- ============================================================
-- 5. 初始化配置数据
-- ============================================================

INSERT INTO system_config (config_key, config_value, config_type, description) VALUES
('max_sql_length', '100000', 'INT', 'SQL语句最大长度限制'),
('analysis_timeout_seconds', '600', 'INT', '血缘分析超时时间（秒）'),
('enable_cache', 'true', 'BOOLEAN', '是否启用结果缓存'),
('cache_expire_hours', '24', 'INT', '缓存过期时间（小时）'),
('batch_size', '100', 'INT', '批量处理批次大小'),
('supported_db_types', '["mysql","hive","spark"]', 'JSON', '支持的数据库类型列表'),
('max_recursion_depth', '50', 'INT', '字段依赖追踪最大递归深度'),
('enable_performance_log', 'true', 'BOOLEAN', '是否启用性能日志');

-- ============================================================
-- 6. 创建索引（性能优化）
-- ============================================================

-- 血缘分析任务表索引
CREATE INDEX idx_lineage_analysis_compound ON lineage_analysis(status, created_at, db_type);

-- 字段血缘关系表索引
CREATE INDEX idx_field_relation_compound ON lineage_field_relation(analysis_id, target_field, source_table);

-- 表级血缘关系表索引
CREATE INDEX idx_table_relation_compound ON lineage_table_relation(analysis_id, source_table, target_table);

-- ============================================================
-- 7. 创建视图（便于查询）
-- ============================================================

-- 7.1 完整元数据视图
CREATE VIEW v_metadata_full AS
SELECT 
    d.id AS database_id,
    d.db_name,
    d.db_type,
    t.id AS table_id,
    t.table_name,
    t.table_type,
    c.id AS column_id,
    c.column_name,
    c.data_type,
    c.is_nullable,
    c.is_primary_key,
    c.column_comment
FROM metadata_database d
LEFT JOIN metadata_table t ON d.id = t.database_id AND t.is_deleted = 0
LEFT JOIN metadata_column c ON t.id = c.table_id AND c.is_deleted = 0
WHERE d.is_deleted = 0;

-- 7.2 血缘分析统计视图
CREATE VIEW v_lineage_stats AS
SELECT 
    DATE(created_at) AS analysis_date,
    db_type,
    status,
    COUNT(*) AS task_count,
    AVG(analysis_time_ms) AS avg_time_ms,
    SUM(field_count) AS total_fields,
    SUM(table_count) AS total_tables
FROM lineage_analysis
GROUP BY DATE(created_at), db_type, status;

-- ============================================================
-- 8. 数据库性能优化建议
-- ============================================================

/*
生产环境建议：

1. 分区表（针对大数据量）：
   - lineage_analysis 按月分区（created_at）
   - operation_log 按月分区（created_at）

2. 定期清理：
   - 清理30天前的operation_log
   - 归档180天前的lineage_analysis

3. 索引监控：
   - 监控慢查询日志
   - 定期执行 ANALYZE TABLE 更新统计信息

4. 连接池配置：
   - 最小连接数: 5
   - 最大连接数: 50
   - 连接超时: 30秒
*/

-- ============================================================
-- 脚本结束
-- ============================================================
