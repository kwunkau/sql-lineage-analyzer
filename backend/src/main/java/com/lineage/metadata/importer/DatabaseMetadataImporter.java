package com.lineage.metadata.importer;

import com.lineage.metadata.dto.ColumnMetadataDTO;
import com.lineage.metadata.dto.TableMetadataDTO;

import java.util.List;

/**
 * 数据库元数据导入器接口
 */
public interface DatabaseMetadataImporter {
    
    /**
     * 从数据库导入表和字段元数据
     * 
     * @param url 数据库连接URL
     * @param username 用户名
     * @param password 密码
     * @param tableNames 要导入的表名列表（为空则导入所有表）
     * @return 表元数据列表
     */
    List<TableMetadataDTO> importMetadata(String url, String username, String password, List<String> tableNames) throws Exception;
    
    /**
     * 测试数据库连接
     */
    boolean testConnection(String url, String username, String password);
}
