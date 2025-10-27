package com.lineage.metadata.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lineage.dto.response.ApiResponse;
import com.lineage.metadata.dto.ColumnMetadataDTO;
import com.lineage.metadata.dto.DataSourceDTO;
import com.lineage.metadata.dto.MetadataImportRequest;
import com.lineage.metadata.dto.TableMetadataDTO;
import com.lineage.metadata.entity.ColumnMetadata;
import com.lineage.metadata.entity.DataSource;
import com.lineage.metadata.entity.TableMetadata;
import com.lineage.metadata.service.MetadataImportService;
import com.lineage.metadata.service.MetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 元数据管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/metadata")
@Validated
public class MetadataController {
    
    @Resource
    private MetadataService metadataService;
    
    @Resource
    private MetadataImportService metadataImportService;
    
    // ==================== 数据源管理 ====================
    
    /**
     * 创建数据源
     */
    @PostMapping("/datasource")
    public ApiResponse<Long> createDataSource(@Valid @RequestBody DataSourceDTO dto) {
        log.info("创建数据源: name={}, type={}", dto.getName(), dto.getType());
        Long id = metadataService.createDataSource(dto);
        return ApiResponse.success("创建数据源成功", id);
    }
    
    /**
     * 查询单个数据源
     */
    @GetMapping("/datasource/{id}")
    public ApiResponse<DataSource> getDataSource(@PathVariable Long id) {
        DataSource dataSource = metadataService.getDataSource(id);
        if (dataSource == null) {
            return ApiResponse.error(404, "数据源不存在");
        }
        return ApiResponse.success(dataSource);
    }
    
    /**
     * 列表查询数据源（分页）
     */
    @GetMapping("/datasource")
    public ApiResponse<IPage<DataSource>> listDataSources(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type) {
        IPage<DataSource> result = metadataService.listDataSources(page, size, type);
        return ApiResponse.success(result);
    }
    
    /**
     * 更新数据源
     */
    @PutMapping("/datasource/{id}")
    public ApiResponse<Void> updateDataSource(
            @PathVariable Long id,
            @Valid @RequestBody DataSourceDTO dto) {
        log.info("更新数据源: id={}", id);
        metadataService.updateDataSource(id, dto);
        return ApiResponse.success("更新数据源成功", null);
    }
    
    /**
     * 删除数据源
     */
    @DeleteMapping("/datasource/{id}")
    public ApiResponse<Void> deleteDataSource(@PathVariable Long id) {
        log.info("删除数据源: id={}", id);
        metadataService.deleteDataSource(id);
        return ApiResponse.success("删除数据源成功", null);
    }
    
    // ==================== 表元数据管理 ====================
    
    /**
     * 创建表元数据
     */
    @PostMapping("/table")
    public ApiResponse<Long> createTable(@Valid @RequestBody TableMetadataDTO dto) {
        log.info("创建表元数据: tableName={}", dto.getTableName());
        Long id = metadataService.createTable(dto);
        return ApiResponse.success("创建表元数据成功", id);
    }
    
    /**
     * 查询单个表元数据
     */
    @GetMapping("/table/{id}")
    public ApiResponse<TableMetadata> getTable(@PathVariable Long id) {
        TableMetadata table = metadataService.getTable(id);
        if (table == null) {
            return ApiResponse.error(404, "表元数据不存在");
        }
        return ApiResponse.success(table);
    }
    
    /**
     * 列表查询表元数据（分页）
     */
    @GetMapping("/table")
    public ApiResponse<IPage<TableMetadata>> listTables(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long datasourceId) {
        IPage<TableMetadata> result = metadataService.listTables(page, size, datasourceId);
        return ApiResponse.success(result);
    }
    
    /**
     * 更新表元数据
     */
    @PutMapping("/table/{id}")
    public ApiResponse<Void> updateTable(
            @PathVariable Long id,
            @Valid @RequestBody TableMetadataDTO dto) {
        log.info("更新表元数据: id={}", id);
        metadataService.updateTable(id, dto);
        return ApiResponse.success("更新表元数据成功", null);
    }
    
    /**
     * 删除表元数据（级联删除字段）
     */
    @DeleteMapping("/table/{id}")
    public ApiResponse<Void> deleteTable(@PathVariable Long id) {
        log.info("删除表元数据: id={}", id);
        metadataService.deleteTable(id);
        return ApiResponse.success("删除表元数据成功", null);
    }
    
    // ==================== 字段元数据管理 ====================
    
    /**
     * 创建字段元数据
     */
    @PostMapping("/column")
    public ApiResponse<Long> createColumn(@Valid @RequestBody ColumnMetadataDTO dto) {
        log.info("创建字段元数据: columnName={}", dto.getColumnName());
        Long id = metadataService.createColumn(dto);
        return ApiResponse.success("创建字段元数据成功", id);
    }
    
    /**
     * 按表ID查询字段列表
     */
    @GetMapping("/column")
    public ApiResponse<List<ColumnMetadata>> listColumns(@RequestParam Long tableId) {
        List<ColumnMetadata> columns = metadataService.listColumnsByTableId(tableId);
        return ApiResponse.success(columns);
    }
    
    /**
     * 更新字段元数据
     */
    @PutMapping("/column/{id}")
    public ApiResponse<Void> updateColumn(
            @PathVariable Long id,
            @Valid @RequestBody ColumnMetadataDTO dto) {
        log.info("更新字段元数据: id={}", id);
        metadataService.updateColumn(id, dto);
        return ApiResponse.success("更新字段元数据成功", null);
    }
    
    /**
     * 删除字段元数据
     */
    @DeleteMapping("/column/{id}")
    public ApiResponse<Void> deleteColumn(@PathVariable Long id) {
        log.info("删除字段元数据: id={}", id);
        metadataService.deleteColumn(id);
        return ApiResponse.success("删除字段元数据成功", null);
    }
    
    // ==================== 元数据导入 ====================
    
    /**
     * 从数据源导入元数据
     */
    @PostMapping("/import")
    public ApiResponse<Integer> importMetadata(@Valid @RequestBody MetadataImportRequest request) {
        try {
            log.info("导入元数据: dataSourceId={}", request.getDataSourceId());
            
            int count = metadataImportService.importFromDataSource(
                    request.getDataSourceId(), 
                    request.getTableNames()
            );
            
            return ApiResponse.success("导入成功，共" + count + "个表", count);
            
        } catch (Exception e) {
            log.error("导入元数据失败", e);
            return ApiResponse.error(500, "导入失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试数据源连接
     */
    @GetMapping("/test-connection/{dataSourceId}")
    public ApiResponse<Boolean> testConnection(@PathVariable Long dataSourceId) {
        log.info("测试数据源连接: dataSourceId={}", dataSourceId);
        
        boolean success = metadataImportService.testConnection(dataSourceId);
        
        if (success) {
            return ApiResponse.success("连接成功", true);
        } else {
            return ApiResponse.error(500, "连接失败");
        }
    }
}
