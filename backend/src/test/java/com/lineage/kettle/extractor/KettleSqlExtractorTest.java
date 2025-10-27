package com.lineage.kettle.extractor;

import com.lineage.kettle.model.KettleSqlInfo;
import com.lineage.kettle.model.KettleTransformation;
import com.lineage.kettle.parser.KettleParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kettle SQL提取器测试
 */
@SpringBootTest
class KettleSqlExtractorTest {
    
    @Resource
    private KettleParser kettleParser;
    
    @Resource
    private KettleSqlExtractor kettleSqlExtractor;
    
    @Test
    void testExtractSqls() throws Exception {
        // 加载并解析测试文件
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-transformation.ktr");
        KettleTransformation transformation = kettleParser.parse(inputStream);
        
        // 提取SQL
        List<KettleSqlInfo> sqls = kettleSqlExtractor.extractSqls(transformation);
        
        // 验证结果
        assertNotNull(sqls);
        assertEquals(3, sqls.size()); // TableInput + TableOutput + ExecSQL
        
        // 验证TableInput的SQL
        KettleSqlInfo tableInputSql = sqls.stream()
                .filter(s -> "TableInput".equals(s.getStepType()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(tableInputSql);
        assertEquals("表输入_用户表", tableInputSql.getStepName());
        assertNotNull(tableInputSql.getSql());
        assertTrue(tableInputSql.getSql().contains("SELECT"));
        assertTrue(tableInputSql.getSql().contains("users"));
        
        // 验证TableOutput的信息
        KettleSqlInfo tableOutputSql = sqls.stream()
                .filter(s -> "TableOutput".equals(s.getStepType()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(tableOutputSql);
        assertEquals("表输出_目标表", tableOutputSql.getStepName());
        assertEquals("target_users", tableOutputSql.getTargetTable());
        assertEquals("test_db", tableOutputSql.getSchemaName());
        
        // 验证ExecSQL的SQL
        KettleSqlInfo execSql = sqls.stream()
                .filter(s -> "ExecSQL".equals(s.getStepType()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(execSql);
        assertNotNull(execSql.getSql());
        assertTrue(execSql.getSql().contains("DELETE"));
    }
    
    @Test
    void testExtractTableNameFromSql() throws Exception {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-transformation.ktr");
        KettleTransformation transformation = kettleParser.parse(inputStream);
        
        List<KettleSqlInfo> sqls = kettleSqlExtractor.extractSqls(transformation);
        
        // 验证从SELECT语句中提取表名
        KettleSqlInfo tableInputSql = sqls.stream()
                .filter(s -> "TableInput".equals(s.getStepType()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(tableInputSql);
        assertNotNull(tableInputSql.getSourceTable());
        assertTrue(tableInputSql.getSourceTable().contains("users"));
    }
}
