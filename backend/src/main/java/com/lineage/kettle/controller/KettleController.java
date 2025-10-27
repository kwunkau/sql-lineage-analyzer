package com.lineage.kettle.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lineage.dto.response.ApiResponse;
import com.lineage.kettle.dto.BatchUploadResponse;
import com.lineage.kettle.dto.KettleParseResponse;
import com.lineage.kettle.entity.KettleFileRecord;
import com.lineage.kettle.model.KettleSqlInfo;
import com.lineage.kettle.model.KettleTransformation;
import com.lineage.kettle.service.KettleFileService;
import com.lineage.kettle.service.KettleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Arrays;
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
    
    @Resource
    private KettleFileService kettleFileService;
    
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
    
    // ==================== 批量上传管理 ====================
    
    /**
     * 上传单个Kettle文件（持久化）
     */
    @PostMapping("/upload")
    public ApiResponse<BatchUploadResponse.FileUploadResult> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            log.info("上传Kettle文件: {}", file.getOriginalFilename());
            
            // 验证文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".ktr")) {
                return ApiResponse.error(400, "文件格式错误，仅支持.ktr文件");
            }
            
            // 验证文件大小
            if (file.getSize() > 50 * 1024 * 1024) {
                return ApiResponse.error(400, "文件大小超过50MB限制");
            }
            
            BatchUploadResponse.FileUploadResult result = kettleFileService.uploadFile(file);
            
            if ("success".equals(result.getStatus())) {
                return ApiResponse.success("上传成功", result);
            } else {
                return ApiResponse.error(500, "上传失败: " + result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return ApiResponse.error(500, "上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量上传Kettle文件
     */
    @PostMapping("/batch-upload")
    public ApiResponse<BatchUploadResponse> batchUpload(@RequestParam("files") MultipartFile[] files) {
        try {
            log.info("批量上传Kettle文件: count={}", files.length);
            
            if (files.length == 0) {
                return ApiResponse.error(400, "未选择文件");
            }
            
            // 验证文件类型
            for (MultipartFile file : files) {
                String filename = file.getOriginalFilename();
                if (filename == null || !filename.endsWith(".ktr")) {
                    return ApiResponse.error(400, "文件格式错误: " + filename);
                }
            }
            
            BatchUploadResponse response = kettleFileService.batchUpload(Arrays.asList(files));
            return ApiResponse.success("批量上传完成", response);
            
        } catch (Exception e) {
            log.error("批量上传失败", e);
            return ApiResponse.error(500, "批量上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询文件记录列表
     */
    @GetMapping("/files")
    public ApiResponse<IPage<KettleFileRecord>> listFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        IPage<KettleFileRecord> result = kettleFileService.listFileRecords(page, size, status);
        return ApiResponse.success(result);
    }
    
    /**
     * 查询文件记录详情
     */
    @GetMapping("/files/{id}")
    public ApiResponse<KettleFileRecord> getFileRecord(@PathVariable Long id) {
        KettleFileRecord record = kettleFileService.getFileRecord(id);
        if (record == null) {
            return ApiResponse.error(404, "文件记录不存在");
        }
        return ApiResponse.success(record);
    }
    
    /**
     * 删除文件记录
     */
    @DeleteMapping("/files/{id}")
    public ApiResponse<Void> deleteFileRecord(@PathVariable Long id) {
        log.info("删除文件记录: id={}", id);
        kettleFileService.deleteFileRecord(id);
        return ApiResponse.success("删除成功", null);
    }
}
