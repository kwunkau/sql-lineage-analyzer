package com.lineage.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import lombok.Data;

/**
 * 血缘分析结果 Excel 行数据
 */
@Data
@HeadStyle(fillForegroundColor = 22)
@HeadFontStyle(bold = true, fontHeightInPoints = 11)
public class LineageExcelRow {
    
    @ExcelProperty(value = "序号", index = 0)
    @ColumnWidth(8)
    private Integer index;
    
    @ExcelProperty(value = "目标字段", index = 1)
    @ColumnWidth(20)
    private String targetField;
    
    @ExcelProperty(value = "源表", index = 2)
    @ColumnWidth(25)
    private String sourceTable;
    
    @ExcelProperty(value = "源字段", index = 3)
    @ColumnWidth(30)
    private String sourceFields;
    
    @ExcelProperty(value = "转换逻辑", index = 4)
    @ColumnWidth(40)
    private String transformation;
    
    @ExcelProperty(value = "依赖层级", index = 5)
    @ColumnWidth(12)
    private Integer dependencyLevel;
}
