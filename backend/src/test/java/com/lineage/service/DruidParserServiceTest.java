package com.lineage.service;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.lineage.core.dialect.DbTypeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DruidParserServiceTest {

    @Autowired
    private DruidParserService parserService;

    @Autowired
    private DbTypeResolver dbTypeResolver;

    @Test
    void testParseSimpleMySQLSelect() {
        String sql = "SELECT id, name FROM users WHERE age > 18";
        List<SQLStatement> statements = parserService.parseSQL(sql, "mysql");

        assertNotNull(statements);
        assertEquals(1, statements.size());
        assertTrue(statements.get(0) instanceof SQLSelectStatement);
    }

    @Test
    void testParseSimpleHiveSelect() {
        String sql = "SELECT * FROM hive_table WHERE dt = '2024-01-01'";
        List<SQLStatement> statements = parserService.parseSQL(sql, "hive");

        assertNotNull(statements);
        assertEquals(1, statements.size());
        assertTrue(statements.get(0) instanceof SQLSelectStatement);
    }

    @Test
    void testParseSimplePostgreSQLSelect() {
        String sql = "SELECT user_id, COUNT(*) as cnt FROM events GROUP BY user_id";
        List<SQLStatement> statements = parserService.parseSQL(sql, "postgresql");

        assertNotNull(statements);
        assertEquals(1, statements.size());
        assertTrue(statements.get(0) instanceof SQLSelectStatement);
    }

    @ParameterizedTest
    @CsvSource({
        "mysql, SELECT 1",
        "hive, SELECT * FROM table1",
        "postgresql, SELECT id FROM pg_table"
    })
    void testParseWithDifferentDialects(String dbType, String sql) {
        List<SQLStatement> statements = parserService.parseSQL(sql, dbType);
        
        assertNotNull(statements);
        assertFalse(statements.isEmpty());
        assertTrue(statements.get(0) instanceof SQLSelectStatement);
    }

    @Test
    void testParseMultipleStatements() {
        String sql = "SELECT * FROM t1; SELECT * FROM t2; SELECT * FROM t3";
        List<SQLStatement> statements = parserService.parseSQL(sql, "mysql");

        assertNotNull(statements);
        assertEquals(3, statements.size());
        statements.forEach(stmt -> assertTrue(stmt instanceof SQLSelectStatement));
    }

    @Test
    void testParseSingleSQL() {
        String sql = "SELECT id, name FROM users";
        SQLStatement statement = parserService.parseSingleSQL(sql, "mysql");

        assertNotNull(statement);
        assertTrue(statement instanceof SQLSelectStatement);
    }

    @Test
    void testParseSingleSQLWithMultipleStatements() {
        String sql = "SELECT * FROM t1; SELECT * FROM t2";
        SQLStatement statement = parserService.parseSingleSQL(sql, "mysql");

        assertNotNull(statement);
        assertTrue(statement instanceof SQLSelectStatement);
    }

    @Test
    void testParseSingleSQLWithNoStatement() {
        String sql = "";
        
        assertThrows(IllegalArgumentException.class, () -> {
            parserService.parseSingleSQL(sql, "mysql");
        });
    }

    @Test
    void testParseComplexSQL() {
        String sql = "SELECT u.id, u.name, o.order_id " +
                    "FROM users u " +
                    "LEFT JOIN orders o ON u.id = o.user_id " +
                    "WHERE u.age > 18 " +
                    "ORDER BY u.name";
        
        List<SQLStatement> statements = parserService.parseSQL(sql, "mysql");
        
        assertNotNull(statements);
        assertEquals(1, statements.size());
    }

    @Test
    void testParseWithSubquery() {
        String sql = "SELECT * FROM (SELECT id, name FROM users WHERE age > 18) t";
        List<SQLStatement> statements = parserService.parseSQL(sql, "mysql");

        assertNotNull(statements);
        assertEquals(1, statements.size());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void testParseWithBlankSQL(String sql) {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> parserService.parseSQL(sql, "mysql")
        );
        assertEquals("SQL cannot be blank", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void testParseWithBlankDbType(String dbType) {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> parserService.parseSQL("SELECT 1", dbType)
        );
        assertEquals("Database type cannot be blank", exception.getMessage());
    }

    @Test
    void testParseWithUnsupportedDbType() {
        assertThrows(IllegalArgumentException.class, () -> {
            parserService.parseSQL("SELECT 1", "unsupported_db");
        });
    }

    @Test
    void testParseWithInvalidSQL() {
        String invalidSql = "SELEC * FORM users"; // 拼写错误
        
        assertThrows(IllegalArgumentException.class, () -> {
            parserService.parseSQL(invalidSql, "mysql");
        });
    }

    @Test
    void testFormatSQL() {
        String uglySql = "select id,name from users where age>18";
        String formatted = parserService.formatSQL(uglySql, "mysql");

        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.contains("SELECT"));
        assertTrue(formatted.contains("FROM"));
        assertTrue(formatted.contains("WHERE"));
    }

    @Test
    void testFormatSQLWithDifferentDialects() {
        String sql = "select * from table1";
        
        String mysqlFormatted = parserService.formatSQL(sql, "mysql");
        String hiveFormatted = parserService.formatSQL(sql, "hive");
        String postgresFormatted = parserService.formatSQL(sql, "postgresql");
        
        assertNotNull(mysqlFormatted);
        assertNotNull(hiveFormatted);
        assertNotNull(postgresFormatted);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testFormatWithBlankSQL(String sql) {
        assertThrows(IllegalArgumentException.class, () -> {
            parserService.formatSQL(sql, "mysql");
        });
    }

    @Test
    void testFormatWithInvalidSQL() {
        String invalidSql = "SELEC * FORM users"; // 拼写错误
        assertThrows(IllegalArgumentException.class, () -> {
            parserService.formatSQL(invalidSql, "mysql");
        });
    }

    @Test
    void testParseWithCaseInsensitiveDbType() {
        String sql = "SELECT 1";
        
        assertDoesNotThrow(() -> parserService.parseSQL(sql, "MySQL"));
        assertDoesNotThrow(() -> parserService.parseSQL(sql, "HIVE"));
        assertDoesNotThrow(() -> parserService.parseSQL(sql, "PostgreSQL"));
    }
}
