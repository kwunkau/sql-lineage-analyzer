package com.lineage.metadata.importer;

import com.lineage.metadata.dto.ColumnMetadataDTO;
import com.lineage.metadata.dto.TableMetadataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL元数据导入器
 */
@Slf4j
@Component
public class MySQLMetadataImporter implements DatabaseMetadataImporter {
    
    @Override
    public List<TableMetadataDTO> importMetadata(String url, String username, String password, List<String> tableNames) throws Exception {
        log.info("开始从MySQL导入元数据: url={}, tableCount={}", url, 
                CollectionUtils.isEmpty(tableNames) ? "ALL" : tableNames.size());
        
        List<TableMetadataDTO> tables = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String databaseName = conn.getCatalog();
            
            log.info("连接成功: database={}", databaseName);
            
            // 获取所有表或指定表
            ResultSet tableRs;
            if (CollectionUtils.isEmpty(tableNames)) {
                tableRs = metaData.getTables(databaseName, null, "%", new String[]{"TABLE"});
            } else {
                // 逐个查询指定的表
                for (String tableName : tableNames) {
                    ResultSet rs = metaData.getTables(databaseName, null, tableName, new String[]{"TABLE"});
                    TableMetadataDTO table = extractTableMetadata(conn, metaData, databaseName, rs);
                    if (table != null) {
                        tables.add(table);
                    }
                    rs.close();
                }
                
                log.info("导入完成: 共{}个表", tables.size());
                return tables;
            }
            
            // 遍历所有表
            while (tableRs.next()) {
                String tableName = tableRs.getString("TABLE_NAME");
                String tableComment = tableRs.getString("REMARKS");
                
                TableMetadataDTO table = new TableMetadataDTO();
                table.setTableName(tableName);
                table.setSchemaName(databaseName);
                table.setTableComment(tableComment);
                table.setTableType("TABLE");
                
                // 获取字段信息
                List<ColumnMetadataDTO> columns = importColumns(metaData, databaseName, tableName);
                table.setColumns(columns);
                
                tables.add(table);
                
                log.debug("导入表: {} ({} 个字段)", tableName, columns.size());
            }
            
            tableRs.close();
            
            log.info("导入完成: 共{}个表", tables.size());
        }
        
        return tables;
    }
    
    /**
     * 提取表元数据
     */
    private TableMetadataDTO extractTableMetadata(Connection conn, DatabaseMetaData metaData, 
                                                   String databaseName, ResultSet tableRs) throws SQLException {
        if (!tableRs.next()) {
            return null;
        }
        
        String tableName = tableRs.getString("TABLE_NAME");
        String tableComment = tableRs.getString("REMARKS");
        
        TableMetadataDTO table = new TableMetadataDTO();
        table.setTableName(tableName);
        table.setSchemaName(databaseName);
        table.setTableComment(tableComment);
        table.setTableType("TABLE");
        
        // 获取字段信息
        List<ColumnMetadataDTO> columns = importColumns(metaData, databaseName, tableName);
        table.setColumns(columns);
        
        return table;
    }
    
    /**
     * 导入字段信息
     */
    private List<ColumnMetadataDTO> importColumns(DatabaseMetaData metaData, String databaseName, String tableName) throws SQLException {
        List<ColumnMetadataDTO> columns = new ArrayList<>();
        
        ResultSet columnRs = metaData.getColumns(databaseName, null, tableName, "%");
        
        while (columnRs.next()) {
            ColumnMetadataDTO column = new ColumnMetadataDTO();
            
            column.setColumnName(columnRs.getString("COLUMN_NAME"));
            column.setColumnType(columnRs.getString("TYPE_NAME"));
            column.setColumnLength(columnRs.getInt("COLUMN_SIZE"));
            column.setNullable(columnRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? 1 : 0);
            column.setDefaultValue(columnRs.getString("COLUMN_DEF"));
            column.setColumnComment(columnRs.getString("REMARKS"));
            column.setOrdinalPosition(columnRs.getInt("ORDINAL_POSITION"));
            
            columns.add(column);
        }
        
        columnRs.close();
        
        return columns;
    }
    
    @Override
    public boolean testConnection(String url, String username, String password) {
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            log.error("连接测试失败: {}", e.getMessage());
            return false;
        }
    }
}
