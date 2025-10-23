package com.lineage.core.tracker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 字段依赖追踪器
 * 
 * 负责管理字段别名和依赖关系
 */
@Slf4j
@Component
public class FieldDependencyTracker {
    
    /**
     * 字段别名映射 (alias -> original field)
     */
    private Map<String, String> fieldAliasMap;
    
    /**
     * 表别名映射 (alias -> original table)
     */
    private Map<String, String> tableAliasMap;
    
    public FieldDependencyTracker() {
        this.fieldAliasMap = new HashMap<>();
        this.tableAliasMap = new HashMap<>();
    }
    
    /**
     * 注册字段别名
     */
    public void registerFieldAlias(String alias, String originalField) {
        if (alias != null && originalField != null) {
            fieldAliasMap.put(alias.toLowerCase(), originalField);
            log.debug("Registered field alias: {} -> {}", alias, originalField);
        }
    }
    
    /**
     * 注册表别名
     */
    public void registerTableAlias(String alias, String originalTable) {
        if (alias != null && originalTable != null) {
            tableAliasMap.put(alias.toLowerCase(), originalTable);
            log.debug("Registered table alias: {} -> {}", alias, originalTable);
        }
    }
    
    /**
     * 解析字段别名
     */
    public String resolveFieldAlias(String alias) {
        if (alias == null) {
            return null;
        }
        return fieldAliasMap.getOrDefault(alias.toLowerCase(), alias);
    }
    
    /**
     * 解析表别名
     */
    public String resolveTableAlias(String alias) {
        if (alias == null) {
            return null;
        }
        return tableAliasMap.getOrDefault(alias.toLowerCase(), alias);
    }
    
    /**
     * 清空追踪器
     */
    public void clear() {
        fieldAliasMap.clear();
        tableAliasMap.clear();
        log.debug("Tracker cleared");
    }
    
    /**
     * 获取所有表（包括别名解析后的）
     */
    public Map<String, String> getAllTables() {
        return new HashMap<>(tableAliasMap);
    }
}
