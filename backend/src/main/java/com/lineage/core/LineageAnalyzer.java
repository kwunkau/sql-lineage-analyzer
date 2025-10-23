package com.lineage.core;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.lineage.core.tracker.FieldDependencyTracker;
import com.lineage.core.tracker.LineageResult;
import com.lineage.core.visitor.LineageVisitor;
import com.lineage.service.DruidParserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SQL 血缘分析器
 * 
 * 核心功能：分析 SQL 语句，提取字段级血缘关系
 */
@Slf4j
@Component
public class LineageAnalyzer {
    
    @Autowired
    private DruidParserService parserService;
    
    @Autowired
    private FieldDependencyTracker tracker;
    
    /**
     * 分析 SQL 血缘关系
     *
     * @param sql    SQL 语句
     * @param dbType 数据库类型
     * @return 血缘分析结果
     */
    public LineageResult analyze(String sql, String dbType) {
        LineageResult result = new LineageResult();
        result.setSql(sql);
        result.setDbType(dbType);
        
        try {
            // 1. 解析 SQL
            log.info("Analyzing SQL: {}", sql);
            SQLStatement statement = parserService.parseSingleSQL(sql, dbType);
            
            // 2. 检查是否为 SELECT 语句
            if (!(statement instanceof SQLSelectStatement)) {
                result.setError("Only SELECT statements are supported");
                log.warn("Non-SELECT statement provided: {}", statement.getClass().getSimpleName());
                return result;
            }
            
            // 3. 清空追踪器
            tracker.clear();
            
            // 4. 创建访问者并遍历 AST
            LineageVisitor visitor = new LineageVisitor(result, tracker);
            statement.accept(visitor);
            
            log.info("Successfully analyzed SQL with {} field dependencies", 
                     result.getFieldDependencies().size());
            
        } catch (Exception e) {
            log.error("Failed to analyze SQL: {}", sql, e);
            result.setError("Analysis failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 批量分析 SQL 列表
     *
     * @param sqlList SQL 语句列表
     * @param dbType  数据库类型
     * @return 血缘分析结果列表
     */
    public java.util.List<LineageResult> analyzeBatch(java.util.List<String> sqlList, String dbType) {
        java.util.List<LineageResult> results = new java.util.ArrayList<>();
        
        for (String sql : sqlList) {
            LineageResult result = analyze(sql, dbType);
            results.add(result);
        }
        
        return results;
    }
}
