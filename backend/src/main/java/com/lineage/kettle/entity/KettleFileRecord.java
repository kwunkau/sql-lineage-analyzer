package com.lineage.kettle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Kettle文件记录实体
 */
@Data
@TableName("kettle_file_record")
public class KettleFileRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 转换名称
     */
    private String transformationName;
    
    /**
     * 转换描述
     */
    private String transformationDesc;
    
    /**
     * 步骤数量
     */
    private Integer stepCount;
    
    /**
     * SQL数量
     */
    private Integer sqlCount;
    
    /**
     * 连接数量
     */
    private Integer hopCount;
    
    /**
     * 解析状态（pending/success/failed）
     */
    private String parseStatus;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
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
