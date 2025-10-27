package com.lineage.metadata.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lineage.metadata.dto.ColumnMetadataDTO;
import com.lineage.metadata.dto.DataSourceDTO;
import com.lineage.metadata.dto.TableMetadataDTO;
import com.lineage.metadata.entity.ColumnMetadata;
import com.lineage.metadata.entity.DataSource;
import com.lineage.metadata.entity.TableMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 元数据服务测试
 */
@SpringBootTest
@Transactional
class MetadataServiceTest {
    
    @Resource
    private MetadataService metadataService;
    
    @Test
    void testCreateAndGetDataSource() {
        DataSourceDTO dto = new DataSourceDTO();
        dto.setName("测试数据源");
        dto.setType("mysql");
        dto.setUrl("jdbc:mysql://localhost:3306/test");
        dto.setUsername("root");
        
        Long id = metadataService.createDataSource(dto);
        assertNotNull(id);
        
        DataSource dataSource = metadataService.getDataSource(id);
        assertNotNull(dataSource);
        assertEquals("测试数据源", dataSource.getName());
    }
    
    @Test
    void testCreateTableWithColumns() {
        DataSourceDTO dsDto = new DataSourceDTO();
        dsDto.setName("测试数据源");
        dsDto.setType("mysql");
        dsDto.setUrl("jdbc:mysql://localhost:3306/test");
        Long dsId = metadataService.createDataSource(dsDto);
        
        TableMetadataDTO tableDto = new TableMetadataDTO();
        tableDto.setDatasourceId(dsId);
        tableDto.setTableName("user");
        tableDto.setTableComment("用户表");
        
        ColumnMetadataDTO col1 = new ColumnMetadataDTO();
        col1.setColumnName("id");
        col1.setColumnType("BIGINT");
        col1.setOrdinalPosition(1);
        
        tableDto.setColumns(Arrays.asList(col1));
        
        Long tableId = metadataService.createTable(tableDto);
        assertNotNull(tableId);
        
        List<ColumnMetadata> columns = metadataService.listColumnsByTableId(tableId);
        assertEquals(1, columns.size());
    }
}
