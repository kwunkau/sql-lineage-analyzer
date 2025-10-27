package com.lineage.metadata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据源实体
 */
@Data
@TableName("datasource")
public class DataSource {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 数据源名称
     */
    private String name;
    
    /**
     * 数据源类型（mysql/hive/oracle/spark）
     */
    private String type;
    
    /**
     * 连接URL
     */
    private String url;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码（加密存储）
     */
    private String password;
    
    /**
     * 数据库名称
     */
    private String databaseName;
    
    /**
     * 描述
     */
    private String description;
    
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
