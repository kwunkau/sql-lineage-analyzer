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
    
    // ==================== JOIN Tests ====================
    
    @Test
    void testAnalyzeInnerJoin() {
        String sql = "SELECT u.id, u.name, o.amount FROM users u INNER JOIN orders o ON u.id = o.user_id";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTables().size());
        assertTrue(result.getTables().contains("users"));
        assertTrue(result.getTables().contains("orders"));
        assertEquals(3, result.getFieldDependencies().size());
    }
    
    @Test
    void testAnalyzeLeftJoin() {
        String sql = "SELECT u.name, o.status FROM users u LEFT JOIN orders o ON u.id = o.user_id";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTables().size());
        assertEquals(2, result.getFieldDependencies().size());
        
        FieldDependency dep1 = result.getFieldDependencies().get(0);
        assertEquals("users", dep1.getSourceTable());
        assertTrue(dep1.getSourceFields().contains("name"));
        
        FieldDependency dep2 = result.getFieldDependencies().get(1);
        assertEquals("orders", dep2.getSourceTable());
        assertTrue(dep2.getSourceFields().contains("status"));
    }
    
    @Test
    void testAnalyzeRightJoin() {
        String sql = "SELECT u.id, o.order_id FROM users u RIGHT JOIN orders o ON u.id = o.user_id";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTables().size());
        assertEquals(2, result.getFieldDependencies().size());
    }
    
    @Test
    void testAnalyzeMultipleJoins() {
        String sql = "SELECT u.name, o.amount, p.product_name " +
                    "FROM users u " +
                    "INNER JOIN orders o ON u.id = o.user_id " +
                    "INNER JOIN products p ON o.product_id = p.id";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(3, result.getTables().size());
        assertTrue(result.getTables().contains("users"));
        assertTrue(result.getTables().contains("orders"));
        assertTrue(result.getTables().contains("products"));
        assertEquals(3, result.getFieldDependencies().size());
    }
    
    @Test
    void testAnalyzeJoinWithoutAlias() {
        String sql = "SELECT users.id, orders.amount FROM users INNER JOIN orders ON users.id = orders.user_id";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTables().size());
        assertEquals(2, result.getFieldDependencies().size());
    }
    
    @Test
    void testAnalyzeJoinWithAggregation() {
        String sql = "SELECT u.dept, COUNT(o.id) AS order_count " +
                    "FROM users u " +
                    "LEFT JOIN orders o ON u.id = o.user_id " +
                    "GROUP BY u.dept";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTables().size());
        assertEquals(2, result.getFieldDependencies().size());
        
        // 验证聚合函数
        boolean hasAggregation = result.getFieldDependencies().stream()
                .anyMatch(FieldDependency::isAggregation);
        assertTrue(hasAggregation);
    }
    
    // ==================== Subquery Tests ====================
    
    @Test
    void testAnalyzeSimpleDerivedTable() {
        String sql = "SELECT t.id, t.name FROM (SELECT id, name FROM users WHERE age > 18) t";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTables().size());
        assertTrue(result.getTables().contains("users"));
        assertEquals(2, result.getFieldDependencies().size());
    }
    
    @Test
    void testAnalyzeDerivedTableWithAlias() {
        String sql = "SELECT sub.user_id, sub.total FROM " +
                    "(SELECT id AS user_id, COUNT(*) AS total FROM users GROUP BY id) sub";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTables().size());
        assertEquals(2, result.getFieldDependencies().size());
    }
    
    @Test
    void testAnalyzeNestedSubquery() {
        String sql = "SELECT t1.id FROM " +
                    "(SELECT t2.id FROM (SELECT id FROM users) t2) t1";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTables().size());
        assertTrue(result.getTables().contains("users"));
        assertEquals(1, result.getFieldDependencies().size());
    }
    
    @Test
    void testAnalyzeDerivedTableWithJoin() {
        String sql = "SELECT t.user_id, o.amount FROM " +
                    "(SELECT id AS user_id, name FROM users) t " +
                    "INNER JOIN orders o ON t.user_id = o.user_id";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTables().size());
        assertTrue(result.getTables().contains("users"));
        assertTrue(result.getTables().contains("orders"));
        assertEquals(2, result.getFieldDependencies().size());
    }
    
    @Test
    void testAnalyzeSubqueryInWhere() {
        String sql = "SELECT id, name FROM users WHERE id IN (SELECT user_id FROM orders WHERE amount > 100)";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        // WHERE 中的子查询会被访问，所以包含两个表
        assertTrue(result.getTables().size() >= 1);
        assertTrue(result.getTables().contains("users"));
        assertEquals(2, result.getFieldDependencies().size());
    }
    
    @Test
    void testAnalyzeComplexDerivedTable() {
        String sql = "SELECT t.dept, t.avg_salary FROM " +
                    "(SELECT dept, AVG(salary) AS avg_salary FROM users GROUP BY dept) t " +
                    "WHERE t.avg_salary > 5000";
        LineageResult result = analyzer.analyze(sql, "mysql");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTables().size());
        assertTrue(result.getTables().contains("users"));
        assertEquals(2, result.getFieldDependencies().size());
        
        // 外层查询的字段来自子查询，不会被标记为聚合
        // 子查询的聚合函数依赖已被正确移除
        boolean hasAggregation = result.getFieldDependencies().stream()
                .anyMatch(FieldDependency::isAggregation);
        assertFalse(hasAggregation); // 外层不应该有聚合标记
    }
}
