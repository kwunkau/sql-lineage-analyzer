package com.lineage.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lineage.metadata.dto.ColumnMetadataDTO;
import com.lineage.metadata.dto.DataSourceDTO;
import com.lineage.metadata.dto.TableMetadataDTO;
import com.lineage.metadata.entity.ColumnMetadata;
import com.lineage.metadata.entity.DataSource;
import com.lineage.metadata.entity.TableMetadata;
import com.lineage.metadata.mapper.ColumnMetadataMapper;
import com.lineage.metadata.mapper.DataSourceMapper;
import com.lineage.metadata.mapper.TableMetadataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 元数据服务
 */
@Slf4j
@Service
public class MetadataService {
    
    @Resource
    private DataSourceMapper dataSourceMapper;
    
    @Resource
    private TableMetadataMapper tableMetadataMapper;
    
    @Resource
    private ColumnMetadataMapper columnMetadataMapper;
    
    // ==================== 数据源管理 ====================
    
    /**
     * 创建数据源
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDataSource(DataSourceDTO dto) {
        DataSource entity = new DataSource();
        BeanUtils.copyProperties(dto, entity);
        dataSourceMapper.insert(entity);
        log.info("创建数据源成功, id={}, name={}", entity.getId(), entity.getName());
        return entity.getId();
    }
    
    /**
     * 查询数据源
     */
    public DataSource getDataSource(Long id) {
        return dataSourceMapper.selectById(id);
    }
    
    /**
     * 列表查询数据源（分页）
     */
    public IPage<DataSource> listDataSources(int page, int size, String type) {
        Page<DataSource> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq(DataSource::getType, type);
        }
        wrapper.orderByDesc(DataSource::getCreateTime);
        return dataSourceMapper.selectPage(pageParam, wrapper);
    }
    
    /**
     * 更新数据源
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDataSource(Long id, DataSourceDTO dto) {
        DataSource entity = dataSourceMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("数据源不存在: " + id);
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        dataSourceMapper.updateById(entity);
        log.info("更新数据源成功, id={}", id);
    }
    
    /**
     * 删除数据源（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDataSource(Long id) {
        dataSourceMapper.deleteById(id);
        log.info("删除数据源成功, id={}", id);
    }
    
    // ==================== 表元数据管理 ====================
    
    /**
     * 创建表元数据
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createTable(TableMetadataDTO dto) {
        TableMetadata entity = new TableMetadata();
        BeanUtils.copyProperties(dto, entity);
        tableMetadataMapper.insert(entity);
        log.info("创建表元数据成功, id={}, tableName={}", entity.getId(), entity.getTableName());
        
        // 批量创建字段
        if (!CollectionUtils.isEmpty(dto.getColumns())) {
            for (ColumnMetadataDTO columnDTO : dto.getColumns()) {
                columnDTO.setTableId(entity.getId());
                createColumn(columnDTO);
            }
        }
        
        return entity.getId();
    }
    
    /**
     * 查询表元数据
     */
    public TableMetadata getTable(Long id) {
        return tableMetadataMapper.selectById(id);
    }
    
    /**
     * 列表查询表元数据（分页）
     */
    public IPage<TableMetadata> listTables(int page, int size, Long datasourceId) {
        Page<TableMetadata> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TableMetadata> wrapper = new LambdaQueryWrapper<>();
        if (datasourceId != null) {
            wrapper.eq(TableMetadata::getDatasourceId, datasourceId);
        }
        wrapper.orderByDesc(TableMetadata::getCreateTime);
        return tableMetadataMapper.selectPage(pageParam, wrapper);
    }
    
    /**
     * 更新表元数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTable(Long id, TableMetadataDTO dto) {
        TableMetadata entity = tableMetadataMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("表元数据不存在: " + id);
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        tableMetadataMapper.updateById(entity);
        log.info("更新表元数据成功, id={}", id);
    }
    
    /**
     * 删除表元数据（级联删除字段）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(Long id) {
        // 删除表
        tableMetadataMapper.deleteById(id);
        
        // 级联删除字段
        LambdaQueryWrapper<ColumnMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ColumnMetadata::getTableId, id);
        columnMetadataMapper.delete(wrapper);
        
        log.info("删除表元数据成功（级联删除字段）, id={}", id);
    }
    
    // ==================== 字段元数据管理 ====================
    
    /**
     * 创建字段元数据
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createColumn(ColumnMetadataDTO dto) {
        ColumnMetadata entity = new ColumnMetadata();
        BeanUtils.copyProperties(dto, entity);
        columnMetadataMapper.insert(entity);
        log.debug("创建字段元数据成功, id={}, columnName={}", entity.getId(), entity.getColumnName());
        return entity.getId();
    }
    
    /**
     * 按表ID查询字段列表
     */
    public List<ColumnMetadata> listColumnsByTableId(Long tableId) {
        LambdaQueryWrapper<ColumnMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ColumnMetadata::getTableId, tableId)
                .orderByAsc(ColumnMetadata::getOrdinalPosition);
        return columnMetadataMapper.selectList(wrapper);
    }
    
    /**
     * 更新字段元数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateColumn(Long id, ColumnMetadataDTO dto) {
        ColumnMetadata entity = columnMetadataMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("字段元数据不存在: " + id);
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        columnMetadataMapper.updateById(entity);
        log.debug("更新字段元数据成功, id={}", id);
    }
    
    /**
     * 删除字段元数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteColumn(Long id) {
        columnMetadataMapper.deleteById(id);
        log.debug("删除字段元数据成功, id={}", id);
    }
}
