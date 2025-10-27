package com.lineage.kettle.parser;

import com.lineage.kettle.model.KettleHop;
import com.lineage.kettle.model.KettleStep;
import com.lineage.kettle.model.KettleTransformation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kettle解析器测试
 */
@SpringBootTest
class KettleParserTest {
    
    @Resource
    private KettleParser kettleParser;
    
    @Test
    void testParseKettleFile() throws Exception {
        // 加载测试文件
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-transformation.ktr");
        assertNotNull(inputStream, "测试文件不存在");
        
        // 解析
        KettleTransformation transformation = kettleParser.parse(inputStream);
        
        // 验证基本信息
        assertNotNull(transformation);
        assertEquals("test-transformation", transformation.getName());
        assertEquals("测试用Kettle转换文件", transformation.getDescription());
        
        // 验证步骤
        assertEquals(3, transformation.getSteps().size());
        
        // 验证第一个步骤（TableInput）
        KettleStep step1 = transformation.getSteps().get(0);
        assertEquals("表输入_用户表", step1.getName());
        assertEquals("TableInput", step1.getType());
        assertNotNull(step1.getAttribute("sql"));
        assertTrue(step1.getAttribute("sql").contains("SELECT"));
        assertTrue(step1.getAttribute("sql").contains("users"));
        
        // 验证第二个步骤（TableOutput）
        KettleStep step2 = transformation.getSteps().get(1);
        assertEquals("表输出_目标表", step2.getName());
        assertEquals("TableOutput", step2.getType());
        assertEquals("target_users", step2.getAttribute("table"));
        assertEquals("test_db", step2.getAttribute("schema"));
        
        // 验证连接
        assertEquals(2, transformation.getHops().size());
        
        KettleHop hop1 = transformation.getHops().get(0);
        assertEquals("表输入_用户表", hop1.getFromStep());
        assertEquals("表输出_目标表", hop1.getToStep());
        assertTrue(hop1.isEnabled());
    }
    
    @Test
    void testParseStepWithSql() throws Exception {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-transformation.ktr");
        
        KettleTransformation transformation = kettleParser.parse(inputStream);
        
        // 查找包含SQL的步骤
        KettleStep sqlStep = transformation.getSteps().stream()
                .filter(s -> s.getAttribute("sql") != null)
                .findFirst()
                .orElse(null);
        
        assertNotNull(sqlStep);
        String sql = sqlStep.getAttribute("sql");
        assertNotNull(sql);
        assertFalse(sql.isEmpty());
        assertTrue(sql.toUpperCase().contains("SELECT"));
    }
    
    @Test
    void testParseStepWithTableOutput() throws Exception {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-transformation.ktr");
        
        KettleTransformation transformation = kettleParser.parse(inputStream);
        
        // 查找TableOutput步骤
        KettleStep outputStep = transformation.getSteps().stream()
                .filter(s -> "TableOutput".equals(s.getType()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(outputStep);
        assertNotNull(outputStep.getAttribute("table"));
        assertEquals("target_users", outputStep.getAttribute("table"));
    }
}
