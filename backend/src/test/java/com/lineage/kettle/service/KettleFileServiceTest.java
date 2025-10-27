package com.lineage.kettle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lineage.kettle.dto.BatchUploadResponse;
import com.lineage.kettle.entity.KettleFileRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kettle文件服务测试
 */
@SpringBootTest
@Transactional
class KettleFileServiceTest {
    
    @Resource
    private KettleFileService kettleFileService;
    
    @Test
    void testUploadFile() throws Exception {
        // 加载测试文件
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-transformation.ktr");
        assertNotNull(inputStream);
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.ktr",
                "text/xml",
                inputStream
        );
        
        // 上传文件
        BatchUploadResponse.FileUploadResult result = kettleFileService.uploadFile(file);
        
        // 验证结果
        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertNotNull(result.getFileId());
        assertTrue(result.getSqlCount() > 0);
    }
    
    @Test
    void testBatchUpload() throws Exception {
        // 加载测试文件
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-transformation.ktr");
        assertNotNull(inputStream);
        
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "test1.ktr",
                "text/xml",
                inputStream
        );
        
        // 重新加载流
        InputStream inputStream2 = getClass().getClassLoader()
                .getResourceAsStream("test-transformation.ktr");
        
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "test2.ktr",
                "text/xml",
                inputStream2
        );
        
        List<MultipartFile> files = Arrays.asList(file1, file2);
        
        // 批量上传
        BatchUploadResponse response = kettleFileService.batchUpload(files);
        
        // 验证结果
        assertNotNull(response);
        assertEquals(2, response.getTotalFiles());
        assertTrue(response.getSuccessCount() > 0);
        assertNotNull(response.getTaskId());
        assertNotNull(response.getResults());
    }
    
    @Test
    void testListFileRecords() throws Exception {
        // 上传测试文件
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-transformation.ktr");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.ktr",
                "text/xml",
                inputStream
        );
        
        kettleFileService.uploadFile(file);
        
        // 查询文件记录
        IPage<KettleFileRecord> page = kettleFileService.listFileRecords(1, 10, null);
        
        assertNotNull(page);
        assertTrue(page.getTotal() > 0);
    }
    
    @Test
    void testDeleteFileRecord() throws Exception {
        // 上传测试文件
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-transformation.ktr");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.ktr",
                "text/xml",
                inputStream
        );
        
        BatchUploadResponse.FileUploadResult result = kettleFileService.uploadFile(file);
        Long fileId = result.getFileId();
        
        // 删除文件记录
        kettleFileService.deleteFileRecord(fileId);
        
        // 验证删除
        KettleFileRecord deleted = kettleFileService.getFileRecord(fileId);
        assertNull(deleted);
    }
}
