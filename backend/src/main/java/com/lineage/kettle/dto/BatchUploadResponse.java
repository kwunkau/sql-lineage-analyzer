package com.lineage.kettle.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量上传响应
 */
@Data
public class BatchUploadResponse {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 总文件数
     */
    private int totalFiles;
    
    /**
     * 成功数量
     */
    private int successCount;
    
    /**
     * 失败数量
     */
    private int failedCount;
    
    /**
     * 处理状态
     */
    private String status;
    
    /**
     * 文件记录列表
     */
    private List<FileUploadResult> results;
    
    @Data
    public static class FileUploadResult {
        private String fileName;
        private Long fileId;
        private String status;
        private String errorMessage;
        private Integer sqlCount;
    }
}
