-- ============================================================
-- SQL字段级血缘分析平台 - 初始化测试数据
-- 版本: v1.0
-- 创建时间: 2025-10-23
-- 用途: 开发和测试环境的示例数据
-- ============================================================

-- ============================================================
-- 1. 元数据示例数据
-- ============================================================

-- 1.1 插入示例数据库
INSERT INTO metadata_database (db_name, db_type, description, created_by) VALUES
('production_hive', 'hive', '生产环境Hive数据仓库', 'admin'),
('production_mysql', 'mysql', '生产环境MySQL业务数据库', 'admin'),
('test_spark', 'spark', '测试环境Spark SQL', 'admin');

-- 1.2 插入示例表（Hive）
INSERT INTO metadata_table (database_id, table_name, table_type, description, row_count, created_by) VALUES
(1, 'dw_user_info', 'TABLE', '用户信息事实表', 1000000, 'admin'),
(1, 'dw_order_detail', 'TABLE', '订单明细事实表', 5000000, 'admin'),
(1, 'dim_product', 'TABLE', '产品维度表', 10000, 'admin'),
(1, 'ods_user_raw', 'TABLE', '原始用户数据（ODS层）', 1200000, 'admin'),
(1, 'dws_user_behavior', 'TABLE', '用户行为汇总表（DWS层）', 800000, 'admin');

-- 1.3 插入示例表（MySQL）
INSERT INTO metadata_table (database_id, table_name, table_type, description, row_count, created_by) VALUES
(2, 'users', 'TABLE', '用户基础表', 100000, 'admin'),
(2, 'orders', 'TABLE', '订单表', 500000, 'admin'),
(2, 'products', 'TABLE', '产品表', 5000, 'admin'),
(2, 'order_items', 'TABLE', '订单明细表', 1000000, 'admin');

-- 1.4 插入示例字段（dw_user_info 表）
INSERT INTO metadata_column (table_id, column_name, data_type, is_nullable, is_primary_key, column_comment, ordinal_position) VALUES
(1, 'user_id', 'BIGINT', 0, 1, '用户ID（主键）', 1),
(1, 'user_name', 'STRING', 0, 0, '用户名', 2),
(1, 'mobile', 'STRING', 1, 0, '手机号', 3),
(1, 'email', 'STRING', 1, 0, '邮箱', 4),
(1, 'gender', 'STRING', 1, 0, '性别: M/F', 5),
(1, 'age', 'INT', 1, 0, '年龄', 6),
(1, 'registration_date', 'DATE', 0, 0, '注册日期', 7),
(1, 'user_level', 'STRING', 1, 0, '用户等级: VIP/NORMAL', 8),
(1, 'city', 'STRING', 1, 0, '所在城市', 9),
(1, 'created_at', 'TIMESTAMP', 0, 0, '记录创建时间', 10);

-- 1.5 插入示例字段（dw_order_detail 表）
INSERT INTO metadata_column (table_id, column_name, data_type, is_nullable, is_primary_key, column_comment, ordinal_position) VALUES
(2, 'order_id', 'BIGINT', 0, 1, '订单ID', 1),
(2, 'user_id', 'BIGINT', 0, 0, '用户ID', 2),
(2, 'product_id', 'BIGINT', 0, 0, '产品ID', 3),
(2, 'order_amount', 'DECIMAL', 0, 0, '订单金额', 4),
(2, 'quantity', 'INT', 0, 0, '购买数量', 5),
(2, 'order_status', 'STRING', 0, 0, '订单状态', 6),
(2, 'order_date', 'DATE', 0, 0, '下单日期', 7),
(2, 'payment_method', 'STRING', 1, 0, '支付方式', 8);

-- 1.6 插入示例字段（dim_product 表）
INSERT INTO metadata_column (table_id, column_name, data_type, is_nullable, is_primary_key, column_comment, ordinal_position) VALUES
(3, 'product_id', 'BIGINT', 0, 1, '产品ID', 1),
(3, 'product_name', 'STRING', 0, 0, '产品名称', 2),
(3, 'category', 'STRING', 0, 0, '产品类别', 3),
(3, 'price', 'DECIMAL', 0, 0, '产品价格', 4),
(3, 'brand', 'STRING', 1, 0, '品牌', 5);

-- 1.7 插入MySQL示例字段（users表）
INSERT INTO metadata_column (table_id, column_name, data_type, is_nullable, is_primary_key, column_comment, ordinal_position) VALUES
(6, 'id', 'BIGINT', 0, 1, '用户ID', 1),
(6, 'username', 'VARCHAR', 0, 0, '用户名', 2),
(6, 'email', 'VARCHAR', 1, 0, '邮箱', 3),
(6, 'created_at', 'DATETIME', 0, 0, '创建时间', 4);

-- 1.8 插入MySQL示例字段（orders表）
INSERT INTO metadata_column (table_id, column_name, data_type, is_nullable, is_primary_key, column_comment, ordinal_position) VALUES
(7, 'id', 'BIGINT', 0, 1, '订单ID', 1),
(7, 'user_id', 'BIGINT', 0, 0, '用户ID', 2),
(7, 'amount', 'DECIMAL', 0, 0, '订单金额', 3),
(7, 'status', 'VARCHAR', 0, 0, '订单状态', 4),
(7, 'created_at', 'DATETIME', 0, 0, '创建时间', 5);

-- ============================================================
-- 2. 血缘分析示例数据
-- ============================================================

-- 2.1 插入示例分析任务
INSERT INTO lineage_analysis (task_id, sql_text, sql_hash, db_type, status, result_json, analysis_time_ms, field_count, table_count, created_by) VALUES
('task-demo-001', 
 'SELECT u.user_id, u.user_name, o.order_amount FROM dw_user_info u JOIN dw_order_detail o ON u.user_id = o.user_id WHERE o.order_date >= ''2025-01-01''',
 'a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6',
 'hive',
 'SUCCESS',
 '{"fields":[{"name":"user_id","sources":[{"table":"dw_user_info","column":"user_id"}]},{"name":"user_name","sources":[{"table":"dw_user_info","column":"user_name"}]},{"name":"order_amount","sources":[{"table":"dw_order_detail","column":"order_amount"}]}],"tables":["dw_user_info","dw_order_detail"]}',
 1250,
 3,
 2,
 'admin'),

('task-demo-002',
 'SELECT p.product_name, SUM(o.order_amount) as total_amount FROM dim_product p LEFT JOIN dw_order_detail o ON p.product_id = o.product_id GROUP BY p.product_name',
 'q1w2e3r4t5y6u7i8o9p0a1s2d3f4g5h6',
 'hive',
 'SUCCESS',
 '{"fields":[{"name":"product_name","sources":[{"table":"dim_product","column":"product_name"}]},{"name":"total_amount","sources":[{"table":"dw_order_detail","column":"order_amount"}],"transform":"SUM(order_amount)"}],"tables":["dim_product","dw_order_detail"]}',
 2100,
 2,
 2,
 'admin');

-- 2.2 插入字段血缘关系示例
INSERT INTO lineage_field_relation (analysis_id, target_field, target_alias, source_database, source_table, source_table_alias, source_column, transform_expression, dependency_level) VALUES
(1, 'user_id', 'user_id', 'production_hive', 'dw_user_info', 'u', 'user_id', NULL, 1),
(1, 'user_name', 'user_name', 'production_hive', 'dw_user_info', 'u', 'user_name', NULL, 1),
(1, 'order_amount', 'order_amount', 'production_hive', 'dw_order_detail', 'o', 'order_amount', NULL, 1),
(2, 'product_name', 'product_name', 'production_hive', 'dim_product', 'p', 'product_name', NULL, 1),
(2, 'total_amount', 'total_amount', 'production_hive', 'dw_order_detail', 'o', 'order_amount', 'SUM(order_amount)', 1);

-- 2.3 插入表级血缘关系示例
INSERT INTO lineage_table_relation (analysis_id, source_table, source_table_alias, join_type, join_condition, table_type) VALUES
(1, 'dw_user_info', 'u', 'INNER', 'u.user_id = o.user_id', 'BASE'),
(1, 'dw_order_detail', 'o', 'INNER', 'u.user_id = o.user_id', 'BASE'),
(2, 'dim_product', 'p', 'LEFT', 'p.product_id = o.product_id', 'BASE'),
(2, 'dw_order_detail', 'o', 'LEFT', 'p.product_id = o.product_id', 'BASE');

-- ============================================================
-- 3. Kettle文件示例数据
-- ============================================================

-- 3.1 插入Kettle文件历史
INSERT INTO kettle_file_history (file_name, file_path, file_size, file_hash, file_type, sql_count, status, uploaded_by) VALUES
('etl_user_transform.ktr', '/uploads/kettle/etl_user_transform.ktr', 15360, 'abc123def456ghi789', 'ktr', 3, 'SUCCESS', 'admin'),
('etl_order_aggregation.ktr', '/uploads/kettle/etl_order_aggregation.ktr', 28672, 'xyz987uvw654tsr321', 'ktr', 5, 'SUCCESS', 'admin');

-- 3.2 插入提取的SQL示例
INSERT INTO kettle_extracted_sql (file_id, step_name, step_type, sql_text, sql_hash, analysis_id) VALUES
(1, 'Table Input - User Data', 'TableInput', 'SELECT user_id, user_name, email FROM ods_user_raw WHERE status = ''active''', 'hash001', 1),
(1, 'Table Input - Order Data', 'TableInput', 'SELECT order_id, user_id, order_amount FROM dw_order_detail WHERE order_date >= DATE_SUB(CURRENT_DATE, 30)', 'hash002', NULL),
(2, 'SQL Query - Aggregation', 'TableInput', 'SELECT product_id, COUNT(*) as order_count, SUM(order_amount) as total_sales FROM dw_order_detail GROUP BY product_id', 'hash003', 2);

-- ============================================================
-- 4. 系统配置示例数据（已在schema.sql中插入）
-- ============================================================

-- 额外的业务配置
INSERT INTO system_config (config_key, config_value, config_type, description) VALUES
('ui_theme', 'light', 'STRING', '前端界面主题: light/dark'),
('max_graph_nodes', '500', 'INT', 'DAG图最大节点数限制'),
('export_format', '["excel","json","csv"]', 'JSON', '支持的导出格式列表'),
('enable_sql_cache', 'true', 'BOOLEAN', '是否启用SQL解析缓存'),
('notification_email', 'admin@example.com', 'STRING', '系统通知邮箱');

-- ============================================================
-- 5. 操作日志示例数据
-- ============================================================

INSERT INTO operation_log (user_name, operation_type, operation_module, operation_desc, request_params, response_result, success, execution_time_ms, ip_address) VALUES
('admin', 'ANALYZE', 'LINEAGE', '分析SQL字段血缘', '{"sql":"SELECT...","dbType":"hive"}', '{"status":"success","fieldCount":3}', 1, 1250, '192.168.1.100'),
('admin', 'IMPORT', 'METADATA', '导入元数据', '{"database":"production_hive"}', '{"tablesImported":10,"columnsImported":150}', 1, 3200, '192.168.1.100'),
('user01', 'EXPORT', 'LINEAGE', '导出血缘结果为Excel', '{"taskId":"task-demo-001","format":"excel"}', '{"fileName":"lineage_export.xlsx"}', 1, 850, '192.168.1.101'),
('admin', 'ANALYZE', 'LINEAGE', '批量分析Kettle文件', '{"fileId":1}', '{"sqlCount":3,"successCount":3}', 1, 5200, '192.168.1.100');

-- ============================================================
-- 6. 测试用例SQL（用于验证血缘分析功能）
-- ============================================================

-- 6.1 简单SELECT测试用例
INSERT INTO lineage_analysis (task_id, sql_text, sql_hash, db_type, status, created_by) VALUES
('test-simple-select', 
 'SELECT user_id, user_name FROM dw_user_info',
 'test001',
 'hive',
 'PENDING',
 'test_user');

-- 6.2 多表JOIN测试用例
INSERT INTO lineage_analysis (task_id, sql_text, sql_hash, db_type, status, created_by) VALUES
('test-multi-join',
 'SELECT u.user_id, u.user_name, o.order_amount, p.product_name FROM dw_user_info u JOIN dw_order_detail o ON u.user_id = o.user_id JOIN dim_product p ON o.product_id = p.product_id',
 'test002',
 'hive',
 'PENDING',
 'test_user');

-- 6.3 子查询测试用例
INSERT INTO lineage_analysis (task_id, sql_text, sql_hash, db_type, status, created_by) VALUES
('test-subquery',
 'SELECT user_id, user_name FROM (SELECT user_id, user_name, age FROM dw_user_info WHERE age > 18) t WHERE t.user_name IS NOT NULL',
 'test003',
 'hive',
 'PENDING',
 'test_user');

-- 6.4 UNION测试用例
INSERT INTO lineage_analysis (task_id, sql_text, sql_hash, db_type, status, created_by) VALUES
('test-union',
 'SELECT user_id, user_name FROM dw_user_info WHERE city = ''Beijing'' UNION ALL SELECT user_id, user_name FROM dw_user_info WHERE city = ''Shanghai''',
 'test004',
 'hive',
 'PENDING',
 'test_user');

-- 6.5 窗口函数测试用例
INSERT INTO lineage_analysis (task_id, sql_text, sql_hash, db_type, status, created_by) VALUES
('test-window-function',
 'SELECT user_id, user_name, ROW_NUMBER() OVER (PARTITION BY city ORDER BY age DESC) as rank FROM dw_user_info',
 'test005',
 'hive',
 'PENDING',
 'test_user');

-- ============================================================
-- 7. 数据验证
-- ============================================================

-- 验证元数据数量
SELECT 
    (SELECT COUNT(*) FROM metadata_database) as database_count,
    (SELECT COUNT(*) FROM metadata_table) as table_count,
    (SELECT COUNT(*) FROM metadata_column) as column_count,
    (SELECT COUNT(*) FROM lineage_analysis) as analysis_count,
    (SELECT COUNT(*) FROM lineage_field_relation) as field_relation_count,
    (SELECT COUNT(*) FROM lineage_table_relation) as table_relation_count;

-- 验证配置项
SELECT config_key, config_value, config_type 
FROM system_config 
ORDER BY config_key;

-- 验证示例血缘数据
SELECT 
    la.task_id,
    la.db_type,
    la.status,
    COUNT(DISTINCT lfr.id) as field_relations,
    COUNT(DISTINCT ltr.id) as table_relations
FROM lineage_analysis la
LEFT JOIN lineage_field_relation lfr ON la.id = lfr.analysis_id
LEFT JOIN lineage_table_relation ltr ON la.id = ltr.analysis_id
WHERE la.status = 'SUCCESS'
GROUP BY la.task_id, la.db_type, la.status;

-- ============================================================
-- 初始化数据脚本结束
-- ============================================================

-- 提示信息
SELECT '✅ 初始化数据插入完成！' as message,
       '包含: 3个数据库, 9个表, 30+个字段, 2个示例血缘分析' as summary;
