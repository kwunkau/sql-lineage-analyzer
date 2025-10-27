package com.lineage.kettle.service;

import com.lineage.kettle.extractor.KettleSqlExtractor;
import com.lineage.kettle.model.KettleSqlInfo;
import com.lineage.kettle.model.KettleTransformation;
import com.lineage.kettle.parser.KettleParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

/**
 * Kettle服务
 */
@Slf4j
@Service
public class KettleService {
    
    @Resource
    private KettleParser kettleParser;
    
    @Resource
    private KettleSqlExtractor kettleSqlExtractor;
    
    /**
     * 解析Kettle文件
     */
    public KettleTransformation parseKettleFile(File file) throws Exception {
        log.info("解析Kettle文件: {}", file.getName());
        return kettleParser.parse(file);
    }
    
    /**
     * 解析上传的Kettle文件
     */
    public KettleTransformation parseKettleFile(MultipartFile file) throws Exception {
        log.info("解析上传的Kettle文件: {}", file.getOriginalFilename());
        return kettleParser.parse(file.getInputStream());
    }
    
    /**
     * 提取SQL语句
     */
    public List<KettleSqlInfo> extractSqls(KettleTransformation transformation) {
        return kettleSqlExtractor.extractSqls(transformation);
    }
    
    /**
     * 解析并提取SQL（一步到位）
     */
    public List<KettleSqlInfo> parseAndExtractSqls(MultipartFile file) throws Exception {
        log.info("解析并提取SQL: {}", file.getOriginalFilename());
        
        KettleTransformation transformation = parseKettleFile(file);
        return extractSqls(transformation);
    }
}
