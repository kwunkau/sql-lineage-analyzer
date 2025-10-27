package com.lineage.metadata.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 数据源DTO
 */
@Data
public class DataSourceDTO {
    
    private Long id;
    
    @NotBlank(message = "数据源名称不能为空")
    private String name;
    
    @NotBlank(message = "数据源类型不能为空")
    @Pattern(regexp = "mysql|hive|oracle|spark", message = "数据源类型必须是mysql/hive/oracle/spark之一")
    private String type;
    
    @NotBlank(message = "连接URL不能为空")
    private String url;
    
    private String username;
    
    private String password;
    
    private String databaseName;
    
    private String description;
}
