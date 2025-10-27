package com.lineage.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lineage.metadata.entity.TableMetadata;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表元数据 Mapper
 */
@Mapper
public interface TableMetadataMapper extends BaseMapper<TableMetadata> {
}
