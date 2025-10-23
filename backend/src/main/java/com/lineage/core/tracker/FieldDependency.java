package com.lineage.core.tracker;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 字段依赖关系
 */
@Data
public class FieldDependency {
    
    /**
     * 目标字段名
     */
    private String targetField;
    
    /**
     * 目标字段别名
     */
    private String targetAlias;
    
    /**
     * 来源表名
     */
    private String sourceTable;
    
    /**
     * 来源表别名
     */
    private String sourceTableAlias;
    
    /**
     * 来源字段列表
     */
    private List<String> sourceFields;
    
    /**
     * 表达式（如果是计算字段）
     */
    private String expression;
    
    /**
     * 是否为聚合函数
     */
    private boolean isAggregation;
    
    public FieldDependency() {
        this.sourceFields = new ArrayList<>();
    }
    
    public FieldDependency(String targetField) {
        this();
        this.targetField = targetField;
    }
    
    /**
     * 添加来源字段
     */
    public void addSourceField(String field) {
        if (field != null && !sourceFields.contains(field)) {
            sourceFields.add(field);
        }
    }
}
