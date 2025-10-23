package com.lineage.core;

import com.lineage.core.tracker.FieldDependency;
import com.lineage.core.tracker.LineageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LineageAnalyzerTest {

    @Autowired
    private LineageAnalyzer analyzer;

    @Test
    void testAnalyzeSimpleSelect() {
        String sql = "SELECT id, name FROM users";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTables().size());
        assertEquals("users", result.getTables().get(0));
        assertEquals(2, result.getFieldDependencies().size());
    }

    @Test
    void testAnalyzeSelectWithAlias() {
        String sql = "SELECT id AS user_id, name AS user_name FROM users";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTables().size());
        assertEquals(2, result.getFieldDependencies().size());
        
        FieldDependency dep1 = result.getFieldDependencies().get(0);
        assertEquals("user_id", dep1.getTargetAlias());
        assertTrue(dep1.getSourceFields().contains("id"));
    }

    @Test
    void testAnalyzeSelectWithTableAlias() {
        String sql = "SELECT u.id, u.name FROM users u";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTables().size());
        assertEquals("users", result.getTables().get(0));
        assertEquals(2, result.getFieldDependencies().size());
        
        FieldDependency dep1 = result.getFieldDependencies().get(0);
        assertEquals("users", dep1.getSourceTable());
        assertEquals("u", dep1.getSourceTableAlias());
        assertTrue(dep1.getSourceFields().contains("id"));
    }

    @Test
    void testAnalyzeSelectWithWildcard() {
        String sql = "SELECT * FROM users";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTables().size());
        assertEquals(1, result.getFieldDependencies().size());
        
        FieldDependency dep = result.getFieldDependencies().get(0);
        assertEquals("*", dep.getTargetField());
        assertTrue(dep.getSourceFields().contains("*"));
    }

    @Test
    void testAnalyzeSelectWithAggregation() {
        String sql = "SELECT COUNT(*) AS total FROM users";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTables().size());
        assertEquals(1, result.getFieldDependencies().size());
        
        FieldDependency dep = result.getFieldDependencies().get(0);
        assertEquals("total", dep.getTargetAlias());
        assertTrue(dep.isAggregation());
        assertNotNull(dep.getExpression());
    }

    @Test
    void testAnalyzeSelectWithMultipleAggregations() {
        String sql = "SELECT COUNT(*) AS cnt, SUM(amount) AS total, AVG(age) AS avg_age FROM users";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(3, result.getFieldDependencies().size());
        
        for (FieldDependency dep : result.getFieldDependencies()) {
            assertTrue(dep.isAggregation());
        }
    }

    @Test
    void testAnalyzeWithDifferentDbTypes() {
        String sql = "SELECT id, name FROM users";
        
        LineageResult mysqlResult = analyzer.analyze(sql, "mysql");
        LineageResult hiveResult = analyzer.analyze(sql, "hive");
        LineageResult pgResult = analyzer.analyze(sql, "postgresql");
        
        assertTrue(mysqlResult.isSuccess());
        assertTrue(hiveResult.isSuccess());
        assertTrue(pgResult.isSuccess());
        
        assertEquals(2, mysqlResult.getFieldDependencies().size());
        assertEquals(2, hiveResult.getFieldDependencies().size());
        assertEquals(2, pgResult.getFieldDependencies().size());
    }

    @Test
    void testAnalyzeNonSelectStatement() {
        String sql = "INSERT INTO users (id, name) VALUES (1, 'test')";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("SELECT"));
    }

    @Test
    void testAnalyzeInvalidSQL() {
        String sql = "SELEC * FROM users"; // 拼写错误
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void testAnalyzeBatch() {
        List<String> sqlList = Arrays.asList(
            "SELECT id, name FROM users",
            "SELECT * FROM orders",
            "SELECT COUNT(*) FROM products"
        );
        
        List<LineageResult> results = analyzer.analyzeBatch(sqlList, "mysql");
        
        assertEquals(3, results.size());
        
        assertTrue(results.get(0).isSuccess());
        assertTrue(results.get(1).isSuccess());
        assertTrue(results.get(2).isSuccess());
        
        assertEquals(2, results.get(0).getFieldDependencies().size());
        assertEquals(1, results.get(1).getFieldDependencies().size());
        assertEquals(1, results.get(2).getFieldDependencies().size());
    }

    @Test
    void testAnalyzeWithQualifiedNames() {
        String sql = "SELECT users.id, users.name FROM users";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getFieldDependencies().size());
        
        for (FieldDependency dep : result.getFieldDependencies()) {
            assertEquals("users", dep.getSourceTable());
        }
    }

    @Test
    void testAnalyzeResultContainsSql() {
        String sql = "SELECT id FROM users";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertEquals(sql, result.getSql());
        assertEquals("mysql", result.getDbType());
    }

    @Test
    void testAnalyzeEmptyResult() {
        String sql = "SELECT id, name FROM users";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertNotNull(result.getTables());
        assertNotNull(result.getFieldDependencies());
        assertFalse(result.getTables().isEmpty());
        assertFalse(result.getFieldDependencies().isEmpty());
    }
}
