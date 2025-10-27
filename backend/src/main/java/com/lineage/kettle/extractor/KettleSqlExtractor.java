package com.lineage.kettle.extractor;

import com.lineage.kettle.model.KettleSqlInfo;
import com.lineage.kettle.model.KettleStep;
import com.lineage.kettle.model.KettleTransformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Kettle SQL提取器
 */
@Slf4j
@Component
public class KettleSqlExtractor {
    
    /**
     * 支持的包含SQL的步骤类型
     */
    private static final String TYPE_TABLE_INPUT = "TableInput";
    private static final String TYPE_TABLE_OUTPUT = "TableOutput";
    private static final String TYPE_INSERT_UPDATE = "InsertUpdate";
    private static final String TYPE_UPDATE = "Update";
    private static final String TYPE_DELETE = "Delete";
    private static final String TYPE_EXECUTE_SQL = "ExecSQL";
    
    /**
     * 从Kettle转换中提取SQL
     */
    public List<KettleSqlInfo> extractSqls(KettleTransformation transformation) {
        List<KettleSqlInfo> sqlInfos = new ArrayList<>();
        
        log.info("开始提取SQL: transformation={}", transformation.getName());
        
        for (KettleStep step : transformation.getSteps()) {
            KettleSqlInfo sqlInfo = extractSqlFromStep(step);
            if (sqlInfo != null) {
                sqlInfos.add(sqlInfo);
            }
        }
        
        log.info("SQL提取完成: count={}", sqlInfos.size());
        return sqlInfos;
    }
    
    /**
     * 从步骤中提取SQL
     */
    private KettleSqlInfo extractSqlFromStep(KettleStep step) {
        String stepType = step.getType();
        
        if (TYPE_TABLE_INPUT.equals(stepType)) {
            return extractFromTableInput(step);
        } else if (TYPE_TABLE_OUTPUT.equals(stepType)) {
            return extractFromTableOutput(step);
        } else if (TYPE_INSERT_UPDATE.equals(stepType) || 
                   TYPE_UPDATE.equals(stepType) || 
                   TYPE_DELETE.equals(stepType)) {
            return extractFromDmlStep(step);
        } else if (TYPE_EXECUTE_SQL.equals(stepType)) {
            return extractFromExecuteSql(step);
        }
        
        return null;
    }
    
    /**
     * 从TableInput步骤提取SQL
     */
    private KettleSqlInfo extractFromTableInput(KettleStep step) {
        String sql = step.getAttribute("sql");
        if (sql == null || sql.isEmpty()) {
            return null;
        }
        
        KettleSqlInfo info = new KettleSqlInfo();
        info.setStepName(step.getName());
        info.setStepType(step.getType());
        info.setSql(cleanSql(sql));
        info.setConnectionName(step.getAttribute("connection"));
        
        // 尝试从SQL中提取表名
        info.setSourceTable(extractTableNameFromSql(sql));
        
        return info;
    }
    
    /**
     * 从TableOutput步骤提取信息
     */
    private KettleSqlInfo extractFromTableOutput(KettleStep step) {
        String tableName = step.getAttribute("table");
        if (tableName == null || tableName.isEmpty()) {
            return null;
        }
        
        KettleSqlInfo info = new KettleSqlInfo();
        info.setStepName(step.getName());
        info.setStepType(step.getType());
        info.setTargetTable(tableName);
        info.setSchemaName(step.getAttribute("schema"));
        info.setConnectionName(step.getAttribute("connection"));
        
        // TableOutput没有显式SQL，生成描述性说明
        String schema = step.getAttribute("schema");
        String fullTableName = schema != null && !schema.isEmpty() 
                ? schema + "." + tableName 
                : tableName;
        info.setSql("INSERT INTO " + fullTableName);
        
        return info;
    }
    
    /**
     * 从DML步骤（InsertUpdate/Update/Delete）提取信息
     */
    private KettleSqlInfo extractFromDmlStep(KettleStep step) {
        String tableName = step.getAttribute("table");
        if (tableName == null || tableName.isEmpty()) {
            return null;
        }
        
        KettleSqlInfo info = new KettleSqlInfo();
        info.setStepName(step.getName());
        info.setStepType(step.getType());
        info.setTargetTable(tableName);
        info.setSchemaName(step.getAttribute("schema"));
        info.setConnectionName(step.getAttribute("connection"));
        
        // 生成描述性SQL
        String schema = step.getAttribute("schema");
        String fullTableName = schema != null && !schema.isEmpty() 
                ? schema + "." + tableName 
                : tableName;
        
        String operation = step.getType().replace("Update", " UPDATE ").replace("Delete", " DELETE ");
        info.setSql(operation + fullTableName);
        
        return info;
    }
    
    /**
     * 从ExecuteSQL步骤提取SQL
     */
    private KettleSqlInfo extractFromExecuteSql(KettleStep step) {
        String sql = step.getAttribute("sql");
        if (sql == null || sql.isEmpty()) {
            return null;
        }
        
        KettleSqlInfo info = new KettleSqlInfo();
        info.setStepName(step.getName());
        info.setStepType(step.getType());
        info.setSql(cleanSql(sql));
        info.setConnectionName(step.getAttribute("connection"));
        
        return info;
    }
    
    /**
     * 清理SQL（去除多余空白）
     */
    private String cleanSql(String sql) {
        if (sql == null) {
            return null;
        }
        
        return sql.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("\\n+", " ");
    }
    
    /**
     * 从SQL中提取表名（简单实现）
     */
    private String extractTableNameFromSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return null;
        }
        
        String upperSql = sql.toUpperCase();
        
        // 尝试提取 FROM 后面的表名
        int fromIndex = upperSql.indexOf(" FROM ");
        if (fromIndex != -1) {
            String afterFrom = sql.substring(fromIndex + 6).trim();
            String[] parts = afterFrom.split("\\s+");
            if (parts.length > 0) {
                return parts[0].replaceAll("[,;]", "");
            }
        }
        
        return null;
    }
}
