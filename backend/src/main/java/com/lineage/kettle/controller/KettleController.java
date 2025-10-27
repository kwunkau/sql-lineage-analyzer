package com.lineage.kettle.controller;

import com.lineage.dto.response.ApiResponse;
import com.lineage.kettle.dto.KettleParseResponse;
import com.lineage.kettle.model.KettleSqlInfo;
import com.lineage.kettle.model.KettleTransformation;
import com.lineage.kettle.service.KettleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

/**
 * Kettle控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/kettle")
public class KettleController {
    
    @Resource
    private KettleService kettleService;
    
    /**
     * 解析Kettle文件
     */
    @PostMapping("/parse")
    public ApiResponse<KettleTransformation> parseKettleFile(@RequestParam("file") MultipartFile file) {
        try {
            log.info("接收Kettle文件: {}", file.getOriginalFilename());
            
            // 验证文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".ktr")) {
                return ApiResponse.error(400, "文件格式错误，仅支持.ktr文件");
            }
            
            KettleTransformation transformation = kettleService.parseKettleFile(file);
            return ApiResponse.success("解析成功", transformation);
            
        } catch (Exception e) {
            log.error("解析Kettle文件失败", e);
            return ApiResponse.error(500, "解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析并提取SQL
     */
    @PostMapping("/parse-sql")
    public ApiResponse<KettleParseResponse> parseAndExtractSql(@RequestParam("file") MultipartFile file) {
        try {
            log.info("解析并提取SQL: {}", file.getOriginalFilename());
            
            // 验证文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".ktr")) {
                return ApiResponse.error(400, "文件格式错误，仅支持.ktr文件");
            }
            
            // 解析转换
            KettleTransformation transformation = kettleService.parseKettleFile(file);
            
            // 提取SQL
            List<KettleSqlInfo> sqls = kettleService.extractSqls(transformation);
            
            KettleParseResponse response = KettleParseResponse.of(transformation, sqls);
            return ApiResponse.success("解析成功", response);
            
        } catch (Exception e) {
            log.error("解析并提取SQL失败", e);
            return ApiResponse.error(500, "解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 仅提取SQL（不返回完整转换信息）
     */
    @PostMapping("/extract-sql")
    public ApiResponse<List<KettleSqlInfo>> extractSql(@RequestParam("file") MultipartFile file) {
        try {
            log.info("提取SQL: {}", file.getOriginalFilename());
            
            // 验证文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".ktr")) {
                return ApiResponse.error(400, "文件格式错误，仅支持.ktr文件");
            }
            
            List<KettleSqlInfo> sqls = kettleService.parseAndExtractSqls(file);
            return ApiResponse.success("提取成功，共" + sqls.size() + "条SQL", sqls);
            
        } catch (Exception e) {
            log.error("提取SQL失败", e);
            return ApiResponse.error(500, "提取失败: " + e.getMessage());
        }
    }
}
