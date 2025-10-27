package com.lineage.kettle.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Kettle转换文件模型
 */
@Data
public class KettleTransformation {
    
    /**
     * 转换名称
     */
    private String name;
    
    /**
     * 转换描述
     */
    private String description;
    
    /**
     * 步骤列表
     */
    private List<KettleStep> steps = new ArrayList<>();
    
    /**
     * 连接列表
     */
    private List<KettleHop> hops = new ArrayList<>();
    
    /**
     * 添加步骤
     */
    public void addStep(KettleStep step) {
        this.steps.add(step);
    }
    
    /**
     * 添加连接
     */
    public void addHop(KettleHop hop) {
        this.hops.add(hop);
    }
}
