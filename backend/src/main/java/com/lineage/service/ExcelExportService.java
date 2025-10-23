package com.lineage.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.lineage.core.tracker.FieldDependency;
import com.lineage.core.tracker.LineageResult;
import com.lineage.dto.excel.LineageExcelRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导出服务
 */
@Slf4j
@Service
public class ExcelExportService {
    
    /**
     * 将血缘分析结果导出为 Excel
     *
     * @param lineageResult 血缘分析结果
     * @return Excel 文件流
     */
    public ByteArrayOutputStream exportToExcel(LineageResult lineageResult) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            // 转换数据
            List<LineageExcelRow> rows = convertToExcelRows(lineageResult);
            
            // 使用 EasyExcel 生成 Excel
            EasyExcel.write(outputStream, LineageExcelRow.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("血缘分析结果")
                    .doWrite(rows);
            
            log.info("Generated Excel with {} rows", rows.size());
            
        } catch (Exception e) {
            log.error("Failed to generate Excel", e);
            throw new RuntimeException("Excel generation failed", e);
        }
        
        return outputStream;
    }
    
    /**
     * 转换 LineageResult 为 Excel 行数据
     *
     * @param lineageResult 血缘分析结果
     * @return Excel 行数据列表
     */
    private List<LineageExcelRow> convertToExcelRows(LineageResult lineageResult) {
        List<LineageExcelRow> rows = new ArrayList<>();
        
        if (lineageResult == null || lineageResult.getFieldDependencies() == null) {
            return rows;
        }
        
        int index = 1;
        for (FieldDependency dep : lineageResult.getFieldDependencies()) {
            LineageExcelRow row = new LineageExcelRow();
            
            // 序号
            row.setIndex(index++);
            
            // 目标字段
            String targetField = dep.getTargetAlias() != null && !dep.getTargetAlias().isEmpty() 
                    ? dep.getTargetAlias() 
                    : dep.getTargetField();
            row.setTargetField(targetField);
            
            // 源表
            String sourceTable = formatTableName(dep.getSourceTable(), dep.getSourceTableAlias());
            row.setSourceTable(sourceTable);
            
            // 源字段
            String sourceFields = dep.getSourceFields() != null && !dep.getSourceFields().isEmpty()
                    ? String.join(", ", dep.getSourceFields())
                    : "直接引用";
            row.setSourceFields(sourceFields);
            
            // 转换逻辑
            String transformation;
            if (dep.getExpression() != null && !dep.getExpression().isEmpty()) {
                transformation = dep.getExpression();
            } else if (dep.isAggregation()) {
                transformation = "聚合函数";
            } else {
                transformation = "直接映射";
            }
            row.setTransformation(transformation);
            
            // 依赖层级（简化计算）
            int dependencyLevel = calculateDependencyLevel(dep);
            row.setDependencyLevel(dependencyLevel);
            
            rows.add(row);
        }
        
        return rows;
    }
    
    /**
     * 格式化表名
     *
     * @param table 表名
     * @param alias 表别名
     * @return 格式化后的表名
     */
    private String formatTableName(String table, String alias) {
        if (table == null || table.isEmpty()) {
            return "未知表";
        }
        if (alias != null && !alias.isEmpty() && !alias.equals(table)) {
            return table + " (" + alias + ")";
        }
        return table;
    }
    
    /**
     * 计算依赖层级（简化版）
     *
     * @param dep 字段依赖
     * @return 依赖层级
     */
    private int calculateDependencyLevel(FieldDependency dep) {
        // 简化实现：有表达式或聚合为2层，否则为1层
        if (dep.getExpression() != null || dep.isAggregation()) {
            return 2;
        }
        return 1;
    }
}
