package com.lineage.kettle.model;

import lombok.Data;

/**
 * Kettle SQL信息
 */
@Data
public class KettleSqlInfo {
    
    /**
     * 步骤名称
     */
    private String stepName;
    
    /**
     * 步骤类型
     */
    private String stepType;
    
    /**
     * SQL语句
     */
    private String sql;
    
    /**
     * 源表（TableInput）
     */
    private String sourceTable;
    
    /**
     * 目标表（TableOutput）
     */
    private String targetTable;
    
    /**
     * Schema名称
     */
    private String schemaName;
    
    /**
     * 数据库连接名称
     */
    private String connectionName;
}
