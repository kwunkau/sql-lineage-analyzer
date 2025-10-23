package com.lineage.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * SQL血缘分析请求
 */
@Data
public class AnalyzeRequest {
    
    /**
     * SQL语句
     */
    @NotBlank(message = "SQL cannot be blank")
    private String sql;
    
    /**
     * 数据库类型 (mysql, hive, postgresql等)
     */
    @NotBlank(message = "Database type cannot be blank")
    private String dbType;
}
