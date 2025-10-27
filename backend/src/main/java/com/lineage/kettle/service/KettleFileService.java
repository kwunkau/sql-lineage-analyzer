package com.lineage.kettle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lineage.kettle.dto.BatchUploadResponse;
import com.lineage.kettle.entity.KettleFileRecord;
import com.lineage.kettle.mapper.KettleFileRecordMapper;
import com.lineage.kettle.model.KettleSqlInfo;
import com.lineage.kettle.model.KettleTransformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kettle文件管理服务
 */
@Slf4j
@Service
public class KettleFileService {
    
    @Resource
    private KettleService kettleService;
    
    @Resource
    private KettleFileRecordMapper kettleFileRecordMapper;
    
    @Value("${lineage.kettle.upload-dir:./uploads/kettle}")
    private String uploadDir;
    
    /**
     * 上传单个文件
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchUploadResponse.FileUploadResult uploadFile(MultipartFile file) {
        BatchUploadResponse.FileUploadResult result = new BatchUploadResponse.FileUploadResult();
        result.setFileName(file.getOriginalFilename());
        
        try {
            // 保存文件
            File savedFile = saveFile(file);
            
            // 解析文件
            KettleTransformation transformation = kettleService.parseKettleFile(savedFile);
            List<KettleSqlInfo> sqls = kettleService.extractSqls(transformation);
            
            // 记录到数据库
            KettleFileRecord record = new KettleFileRecord();
            record.setFileName(file.getOriginalFilename());
            record.setFilePath(savedFile.getAbsolutePath());
            record.setFileSize(file.getSize());
            record.setTransformationName(transformation.getName());
            record.setTransformationDesc(transformation.getDescription());
            record.setStepCount(transformation.getSteps().size());
            record.setSqlCount(sqls.size());
            record.setHopCount(transformation.getHops().size());
            record.setParseStatus("success");
            
            kettleFileRecordMapper.insert(record);
            
            result.setFileId(record.getId());
            result.setStatus("success");
            result.setSqlCount(sqls.size());
            
            log.info("文件上传成功: fileName={}, sqlCount={}", file.getOriginalFilename(), sqls.size());
            
        } catch (Exception e) {
            log.error("文件上传失败: fileName={}", file.getOriginalFilename(), e);
            result.setStatus("failed");
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 批量上传文件
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchUploadResponse batchUpload(List<MultipartFile> files) {
        String taskId = UUID.randomUUID().toString();
        
        log.info("批量上传开始: taskId={}, fileCount={}", taskId, files.size());
        
        BatchUploadResponse response = new BatchUploadResponse();
        response.setTaskId(taskId);
        response.setTotalFiles(files.size());
        response.setStatus("completed");
        
        List<BatchUploadResponse.FileUploadResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        
        for (MultipartFile file : files) {
            BatchUploadResponse.FileUploadResult result = uploadFile(file);
            results.add(result);
            
            if ("success".equals(result.getStatus())) {
                successCount++;
            } else {
                failedCount++;
            }
        }
        
        response.setResults(results);
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        
        log.info("批量上传完成: taskId={}, success={}, failed={}", taskId, successCount, failedCount);
        
        return response;
    }
    
    /**
     * 保存上传的文件
     */
    private File saveFile(MultipartFile file) throws IOException {
        // 确保上传目录存在
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        
        // 保存文件
        Path targetPath = uploadPath.resolve(newFilename);
        file.transferTo(targetPath.toFile());
        
        log.info("文件保存成功: {}", targetPath);
        
        return targetPath.toFile();
    }
    
    /**
     * 查询文件记录列表
     */
    public IPage<KettleFileRecord> listFileRecords(int page, int size, String status) {
        Page<KettleFileRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KettleFileRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(KettleFileRecord::getParseStatus, status);
        }
        
        wrapper.orderByDesc(KettleFileRecord::getCreateTime);
        return kettleFileRecordMapper.selectPage(pageParam, wrapper);
    }
    
    /**
     * 获取文件记录详情
     */
    public KettleFileRecord getFileRecord(Long id) {
        return kettleFileRecordMapper.selectById(id);
    }
    
    /**
     * 删除文件记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileRecord(Long id) {
        KettleFileRecord record = kettleFileRecordMapper.selectById(id);
        if (record != null) {
            // 删除物理文件
            try {
                File file = new File(record.getFilePath());
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                log.warn("删除物理文件失败: {}", record.getFilePath(), e);
            }
            
            // 删除数据库记录
            kettleFileRecordMapper.deleteById(id);
            log.info("删除文件记录成功: id={}", id);
        }
    }
}
