package com.lineage.kettle.model;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Kettle步骤模型
 */
@Data
public class KettleStep {
    
    /**
     * 步骤名称
     */
    private String name;
    
    /**
     * 步骤类型（TableInput, TableOutput, SelectValues等）
     */
    private String type;
    
    /**
     * 步骤属性（SQL、表名、字段等）
     */
    private Map<String, String> attributes = new HashMap<>();
    
    /**
     * 添加属性
     */
    public void addAttribute(String key, String value) {
        this.attributes.put(key, value);
    }
    
    /**
     * 获取属性
     */
    public String getAttribute(String key) {
        return this.attributes.get(key);
    }
}
