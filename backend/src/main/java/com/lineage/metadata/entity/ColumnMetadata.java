package com.lineage.metadata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 字段元数据实体
 */
@Data
@TableName("column_metadata")
public class ColumnMetadata {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 表元数据ID
     */
    private Long tableId;
    
    /**
     * 字段名
     */
    private String columnName;
    
    /**
     * 字段类型
     */
    private String columnType;
    
    /**
     * 字段长度
     */
    private Integer columnLength;
    
    /**
     * 是否可空（0-不可空，1-可空）
     */
    private Integer nullable;
    
    /**
     * 默认值
     */
    private String defaultValue;
    
    /**
     * 字段描述
     */
    private String columnComment;
    
    /**
     * 字段顺序
     */
    private Integer ordinalPosition;
    
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
