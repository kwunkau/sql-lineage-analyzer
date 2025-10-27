package com.lineage.metadata.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 元数据导入请求
 */
@Data
public class MetadataImportRequest {
    
    /**
     * 数据源ID
     */
    @NotNull(message = "数据源ID不能为空")
    private Long dataSourceId;
    
    /**
     * 要导入的表名列表（为空则导入所有表）
     */
    private List<String> tableNames;
}
