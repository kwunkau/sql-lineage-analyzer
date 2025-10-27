package com.lineage.kettle.dto;

import com.lineage.kettle.model.KettleSqlInfo;
import com.lineage.kettle.model.KettleTransformation;
import lombok.Data;

import java.util.List;

/**
 * Kettle解析响应
 */
@Data
public class KettleParseResponse {
    
    /**
     * 转换信息
     */
    private KettleTransformation transformation;
    
    /**
     * 提取的SQL列表
     */
    private List<KettleSqlInfo> sqls;
    
    /**
     * 步骤数量
     */
    private int stepCount;
    
    /**
     * 连接数量
     */
    private int hopCount;
    
    /**
     * SQL数量
     */
    private int sqlCount;
    
    public static KettleParseResponse of(KettleTransformation transformation, List<KettleSqlInfo> sqls) {
        KettleParseResponse response = new KettleParseResponse();
        response.setTransformation(transformation);
        response.setSqls(sqls);
        response.setStepCount(transformation.getSteps().size());
        response.setHopCount(transformation.getHops().size());
        response.setSqlCount(sqls.size());
        return response;
    }
}
