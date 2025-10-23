package com.lineage.core.dialect;

import com.alibaba.druid.DbType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库方言解析器
 * 
 * 根据数据库类型字符串解析为 Druid DbType
 */
@Component
public class DbTypeResolver {

    private static final Map<String, DbType> DB_TYPE_MAP = new HashMap<>();

    static {
        DB_TYPE_MAP.put("mysql", DbType.mysql);
        DB_TYPE_MAP.put("hive", DbType.hive);
        DB_TYPE_MAP.put("postgresql", DbType.postgresql);
        DB_TYPE_MAP.put("oracle", DbType.oracle);
        DB_TYPE_MAP.put("sqlserver", DbType.sqlserver);
    }

    /**
     * 解析数据库类型字符串
     *
     * @param dbTypeStr 数据库类型字符串（不区分大小写）
     * @return Druid DbType
     * @throws IllegalArgumentException 不支持的数据库类型
     */
    public DbType resolve(String dbTypeStr) {
        if (StringUtils.isBlank(dbTypeStr)) {
            throw new IllegalArgumentException("Database type cannot be blank");
        }

        String normalizedType = dbTypeStr.trim().toLowerCase();
        DbType dbType = DB_TYPE_MAP.get(normalizedType);

        if (dbType == null) {
            throw new IllegalArgumentException(
                String.format("Unsupported database type: %s. Supported types: %s", 
                    dbTypeStr, DB_TYPE_MAP.keySet())
            );
        }

        return dbType;
    }

    /**
     * 检查是否支持指定的数据库类型
     *
     * @param dbTypeStr 数据库类型字符串
     * @return true if supported
     */
    public boolean isSupported(String dbTypeStr) {
        if (StringUtils.isBlank(dbTypeStr)) {
            return false;
        }
        return DB_TYPE_MAP.containsKey(dbTypeStr.trim().toLowerCase());
    }

    /**
     * 获取所有支持的数据库类型
     *
     * @return 支持的数据库类型列表
     */
    public Map<String, DbType> getSupportedTypes() {
        return new HashMap<>(DB_TYPE_MAP);
    }
}
