package com.lineage.kettle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lineage.kettle.entity.KettleFileRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * Kettle文件记录 Mapper
 */
@Mapper
public interface KettleFileRecordMapper extends BaseMapper<KettleFileRecord> {
}
