-- SQL字段级血缘分析平台 - 数据库表结构
-- 版本: v0.4.0 - M3
-- 创建时间: 2025-10-27

-- 数据源表
CREATE TABLE IF NOT EXISTS datasource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    type VARCHAR(20) NOT NULL COMMENT '数据源类型（mysql/hive/oracle/spark）',
    url VARCHAR(500) NOT NULL COMMENT '连接URL',
    username VARCHAR(100) COMMENT '用户名',
    password VARCHAR(200) COMMENT '密码（加密存储）',
    database_name VARCHAR(100) COMMENT '数据库名称',
    description VARCHAR(500) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    UNIQUE KEY uk_name (name, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';

-- 表元数据表
CREATE TABLE IF NOT EXISTS table_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    datasource_id BIGINT NOT NULL COMMENT '数据源ID',
    table_name VARCHAR(200) NOT NULL COMMENT '表名',
    schema_name VARCHAR(100) COMMENT 'Schema名称',
    table_comment VARCHAR(500) COMMENT '表描述',
    table_type VARCHAR(20) DEFAULT 'TABLE' COMMENT '表类型（TABLE/VIEW）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    KEY idx_datasource_id (datasource_id),
    UNIQUE KEY uk_table (datasource_id, schema_name, table_name, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表元数据表';

-- 字段元数据表
CREATE TABLE IF NOT EXISTS column_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    table_id BIGINT NOT NULL COMMENT '表元数据ID',
    column_name VARCHAR(200) NOT NULL COMMENT '字段名',
    column_type VARCHAR(100) NOT NULL COMMENT '字段类型',
    column_length INT COMMENT '字段长度',
    nullable INT DEFAULT 1 COMMENT '是否可空（0-不可空，1-可空）',
    default_value VARCHAR(500) COMMENT '默认值',
    column_comment VARCHAR(500) COMMENT '字段描述',
    ordinal_position INT COMMENT '字段顺序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    KEY idx_table_id (table_id),
    KEY idx_column_name (column_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段元数据表';
