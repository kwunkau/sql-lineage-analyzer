package com.lineage.metadata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 表元数据实体
 */
@Data
@TableName("table_metadata")
public class TableMetadata {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 数据源ID
     */
    private Long datasourceId;
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * Schema名称
     */
    private String schemaName;
    
    /**
     * 表描述
     */
    private String tableComment;
    
    /**
     * 表类型（TABLE/VIEW）
     */
    private String tableType;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDeleted;
}
