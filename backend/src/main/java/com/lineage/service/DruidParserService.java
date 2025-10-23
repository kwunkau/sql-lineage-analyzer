package com.lineage.service;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.lineage.core.dialect.DbTypeResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Druid SQL 解析服务
 * 
 * 负责将 SQL 字符串解析为 Druid AST（抽象语法树）
 */
@Slf4j
@Service
public class DruidParserService {

    @Autowired
    private DbTypeResolver dbTypeResolver;

    /**
     * 解析 SQL 为 AST（抽象语法树）
     *
     * @param sql    SQL 语句
     * @param dbType 数据库类型字符串
     * @return SQLStatement 列表
     * @throws IllegalArgumentException SQL 为空或解析失败
     */
    public List<SQLStatement> parseSQL(String sql, String dbType) {
        validateInput(sql, dbType);

        DbType resolvedDbType = dbTypeResolver.resolve(dbType);
        log.debug("Parsing SQL with database type: {}", resolvedDbType);

        try {
            List<SQLStatement> statements = SQLUtils.parseStatements(sql, resolvedDbType);
            log.info("Successfully parsed {} SQL statement(s)", statements.size());
            return statements;
        } catch (Exception e) {
            log.error("Failed to parse SQL: {}", sql, e);
            throw new IllegalArgumentException("SQL parsing failed: " + e.getMessage(), e);
        }
    }

    /**
     * 解析单条 SQL 语句
     *
     * @param sql    SQL 语句
     * @param dbType 数据库类型字符串
     * @return 第一个 SQLStatement
     * @throws IllegalArgumentException SQL 为空或解析失败
     */
    public SQLStatement parseSingleSQL(String sql, String dbType) {
        List<SQLStatement> statements = parseSQL(sql, dbType);
        
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("No SQL statement found");
        }

        if (statements.size() > 1) {
            log.warn("Multiple SQL statements found, returning first one");
        }

        return statements.get(0);
    }

    /**
     * 格式化 SQL（美化输出）
     *
     * @param sql    原始 SQL
     * @param dbType 数据库类型字符串
     * @return 格式化后的 SQL
     */
    public String formatSQL(String sql, String dbType) {
        validateInput(sql, dbType);

        // 先验证SQL是否可解析
        parseSQL(sql, dbType);

        DbType resolvedDbType = dbTypeResolver.resolve(dbType);
        
        try {
            return SQLUtils.format(sql, resolvedDbType);
        } catch (Exception e) {
            log.error("Failed to format SQL: {}", sql, e);
            throw new IllegalArgumentException("SQL formatting failed: " + e.getMessage(), e);
        }
    }

    /**
     * 验证输入参数
     */
    private void validateInput(String sql, String dbType) {
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("SQL cannot be blank");
        }
        if (StringUtils.isBlank(dbType)) {
            throw new IllegalArgumentException("Database type cannot be blank");
        }
    }
}
