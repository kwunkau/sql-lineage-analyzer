package com.lineage.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lineage.metadata.entity.ColumnMetadata;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字段元数据 Mapper
 */
@Mapper
public interface ColumnMetadataMapper extends BaseMapper<ColumnMetadata> {
}
