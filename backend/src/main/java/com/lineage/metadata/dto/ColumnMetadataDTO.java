package com.lineage.metadata.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 字段元数据DTO
 */
@Data
public class ColumnMetadataDTO {
    
    private Long id;
    
    @NotNull(message = "表ID不能为空")
    private Long tableId;
    
    @NotBlank(message = "字段名不能为空")
    private String columnName;
    
    @NotBlank(message = "字段类型不能为空")
    private String columnType;
    
    private Integer columnLength;
    
    private Integer nullable;
    
    private String defaultValue;
    
    private String columnComment;
    
    private Integer ordinalPosition;
}
