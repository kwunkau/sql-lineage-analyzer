package com.lineage.controller;

import com.lineage.core.tracker.LineageResult;
import com.lineage.service.ExcelExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 导出控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/export")
@Validated
public class ExportController {
    
    @Autowired
    private ExcelExportService excelExportService;
    
    /**
     * 导出血缘分析结果为 Excel
     *
     * @param lineageResult 血缘分析结果
     * @return Excel 文件流
     */
    @PostMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(@Valid @RequestBody LineageResult lineageResult) {
        log.info("Received excel export request: {} dependencies", 
                 lineageResult.getFieldDependencies().size());
        
        try {
            // 生成 Excel 文件
            ByteArrayOutputStream outputStream = excelExportService.exportToExcel(lineageResult);
            byte[] bytes = outputStream.toByteArray();
            
            // 生成文件名
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = "lineage_analysis_" + timestamp + ".xlsx";
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(bytes.length);
            
            log.info("Excel export successful: {} bytes, filename={}", bytes.length, filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);
            
        } catch (Exception e) {
            log.error("Failed to export excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
