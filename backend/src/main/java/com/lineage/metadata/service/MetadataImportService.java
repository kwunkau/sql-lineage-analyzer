package com.lineage.metadata.service;

import com.lineage.metadata.dto.TableMetadataDTO;
import com.lineage.metadata.entity.DataSource;
import com.lineage.metadata.importer.DatabaseMetadataImporter;
import com.lineage.metadata.importer.MySQLMetadataImporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 元数据导入服务
 */
@Slf4j
@Service
public class MetadataImportService {
    
    @Resource
    private MetadataService metadataService;
    
    @Resource
    private MySQLMetadataImporter mysqlMetadataImporter;
    
    /**
     * 从数据源导入元数据
     */
    @Transactional(rollbackFor = Exception.class)
    public int importFromDataSource(Long dataSourceId, List<String> tableNames) throws Exception {
        log.info("开始导入元数据: dataSourceId={}, tableNames={}", dataSourceId, tableNames);
        
        // 获取数据源信息
        DataSource dataSource = metadataService.getDataSource(dataSourceId);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在: " + dataSourceId);
        }
        
        // 根据数据源类型选择导入器
        DatabaseMetadataImporter importer = getImporter(dataSource.getType());
        
        // 导入元数据
        List<TableMetadataDTO> tables = importer.importMetadata(
                dataSource.getUrl(), 
                dataSource.getUsername(), 
                dataSource.getPassword(), 
                tableNames
        );
        
        // 保存到数据库
        int importedCount = 0;
        for (TableMetadataDTO table : tables) {
            table.setDatasourceId(dataSourceId);
            
            try {
                // 尝试创建（如果已存在会抛异常，后续可以改为更新）
                metadataService.createTable(table);
                importedCount++;
            } catch (Exception e) {
                log.warn("导入表失败: table={}, error={}", table.getTableName(), e.getMessage());
            }
        }
        
        log.info("导入完成: 总数={}, 成功={}", tables.size(), importedCount);
        return importedCount;
    }
    
    /**
     * 测试数据源连接
     */
    public boolean testConnection(Long dataSourceId) {
        DataSource dataSource = metadataService.getDataSource(dataSourceId);
        if (dataSource == null) {
            return false;
        }
        
        DatabaseMetadataImporter importer = getImporter(dataSource.getType());
        return importer.testConnection(
                dataSource.getUrl(), 
                dataSource.getUsername(), 
                dataSource.getPassword()
        );
    }
    
    /**
     * 根据数据源类型获取导入器
     */
    private DatabaseMetadataImporter getImporter(String type) {
        if ("mysql".equalsIgnoreCase(type)) {
            return mysqlMetadataImporter;
        }
        // TODO: 支持其他数据库类型
        throw new RuntimeException("不支持的数据源类型: " + type);
    }
}
