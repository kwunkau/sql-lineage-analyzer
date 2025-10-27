package com.lineage.kettle.model;

import lombok.Data;

/**
 * Kettle步骤连接模型
 */
@Data
public class KettleHop {
    
    /**
     * 源步骤名称
     */
    private String fromStep;
    
    /**
     * 目标步骤名称
     */
    private String toStep;
    
    /**
     * 是否启用
     */
    private boolean enabled = true;
}
