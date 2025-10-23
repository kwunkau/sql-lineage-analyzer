package com.lineage.controller;

import com.lineage.core.LineageAnalyzer;
import com.lineage.core.tracker.LineageResult;
import com.lineage.dto.request.AnalyzeRequest;
import com.lineage.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * SQL血缘分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/lineage")
@Validated
public class LineageController {
    
    @Autowired
    private LineageAnalyzer analyzer;
    
    /**
     * 分析SQL血缘关系
     *
     * @param request 分析请求
     * @return 血缘分析结果
     */
    @PostMapping("/analyze")
    public ApiResponse<LineageResult> analyze(@Valid @RequestBody AnalyzeRequest request) {
        log.info("Received lineage analyze request: dbType={}, sql={}", 
                 request.getDbType(), request.getSql());
        
        try {
            LineageResult result = analyzer.analyze(request.getSql(), request.getDbType());
            
            if (result.isSuccess()) {
                log.info("Analysis successful: {} tables, {} dependencies", 
                         result.getTables().size(), result.getFieldDependencies().size());
                return ApiResponse.success(result);
            } else {
                log.warn("Analysis failed: {}", result.getErrorMessage());
                return ApiResponse.error(400, result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("Failed to analyze SQL", e);
            return ApiResponse.error("Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("SQL Lineage Analyzer is running");
    }
}
