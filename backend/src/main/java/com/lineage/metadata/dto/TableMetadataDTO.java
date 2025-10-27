package com.lineage.metadata.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 表元数据DTO
 */
@Data
public class TableMetadataDTO {
    
    private Long id;
    
    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;
    
    @NotBlank(message = "表名不能为空")
    private String tableName;
    
    private String schemaName;
    
    private String tableComment;
    
    private String tableType;
    
    /**
     * 字段列表（用于批量创建）
     */
    private List<ColumnMetadataDTO> columns;
}
