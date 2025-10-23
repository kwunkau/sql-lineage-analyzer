package com.lineage.core.tracker;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 血缘分析结果
 */
@Data
public class LineageResult {
    
    /**
     * 原始SQL
     */
    private String sql;
    
    /**
     * 数据库类型
     */
    private String dbType;
    
    /**
     * 查询涉及的表列表
     */
    private List<String> tables;
    
    /**
     * 字段依赖关系列表
     */
    private List<FieldDependency> fieldDependencies;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    public LineageResult() {
        this.tables = new ArrayList<>();
        this.fieldDependencies = new ArrayList<>();
        this.success = true;
    }
    
    /**
     * 添加表
     */
    public void addTable(String table) {
        if (table != null && !tables.contains(table)) {
            tables.add(table);
        }
    }
    
    /**
     * 添加字段依赖
     */
    public void addFieldDependency(FieldDependency dependency) {
        if (dependency != null) {
            fieldDependencies.add(dependency);
        }
    }
    
    /**
     * 设置错误
     */
    public void setError(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
    }
}
