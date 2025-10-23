package com.lineage.core.visitor;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;
import com.lineage.core.tracker.FieldDependency;
import com.lineage.core.tracker.FieldDependencyTracker;
import com.lineage.core.tracker.LineageResult;
import lombok.extern.slf4j.Slf4j;

/**
 * SQL AST 访问者
 * 
 * 遍历 SQL AST 提取字段血缘关系
 */
@Slf4j
public class LineageVisitor extends SQLASTVisitorAdapter {
    
    private final LineageResult result;
    private final FieldDependencyTracker tracker;
    
    private FieldDependency currentDependency;
    private String currentTableName;
    private String currentTableAlias;
    
    public LineageVisitor(LineageResult result, FieldDependencyTracker tracker) {
        this.result = result;
        this.tracker = tracker;
    }
    
    /**
     * 访问 SELECT 语句
     */
    @Override
    public boolean visit(SQLSelectStatement x) {
        log.debug("Visiting SELECT statement");
        return true;
    }
    
    /**
     * 访问 SELECT 查询
     */
    @Override
    public boolean visit(SQLSelectQueryBlock x) {
        log.debug("Visiting SELECT query block");
        
        // 1. 先处理 FROM 子句（表信息）
        SQLTableSource from = x.getFrom();
        if (from != null) {
            from.accept(this);
        }
        
        // 2. 再处理 SELECT 列表（字段依赖）
        for (SQLSelectItem item : x.getSelectList()) {
            item.accept(this);
        }
        
        return false; // 不再递归访问子节点
    }
    
    /**
     * 访问表引用
     */
    @Override
    public boolean visit(SQLExprTableSource x) {
        String tableName = x.getTableName();
        String alias = x.getAlias();
        
        if (tableName != null) {
            result.addTable(tableName);
            currentTableName = tableName;
            
            if (alias != null) {
                currentTableAlias = alias;
                tracker.registerTableAlias(alias, tableName);
                log.debug("Found table: {} AS {}", tableName, alias);
            } else {
                currentTableAlias = tableName;
                log.debug("Found table: {}", tableName);
            }
        }
        
        return false;
    }
    
    /**
     * 访问 JOIN 表引用
     */
    @Override
    public boolean visit(SQLJoinTableSource x) {
        log.debug("Visiting JOIN table source: {}", x.getJoinType());
        
        // 访问左表
        SQLTableSource left = x.getLeft();
        if (left != null) {
            left.accept(this);
        }
        
        // 访问右表
        SQLTableSource right = x.getRight();
        if (right != null) {
            right.accept(this);
        }
        
        // ON 条件不影响字段血缘，只是连接条件
        // 如果需要分析 ON 条件中的字段关系，可以在这里处理
        
        return false;
    }
    
    /**
     * 访问子查询表（派生表）
     * 例如: FROM (SELECT id, name FROM users) t
     */
    @Override
    public boolean visit(SQLSubqueryTableSource x) {
        String alias = x.getAlias();
        log.debug("Visiting subquery table source with alias: {}", alias);
        
        // 保存当前字段依赖列表的大小
        int previousDependencyCount = result.getFieldDependencies().size();
        
        // 递归访问子查询，只提取表信息
        SQLSelect select = x.getSelect();
        if (select != null) {
            SQLSelectQuery query = select.getQuery();
            if (query != null) {
                // 递归处理子查询
                query.accept(this);
            }
        }
        
        // 移除子查询中添加的字段依赖（子查询不应该影响外层的字段依赖）
        int currentDependencyCount = result.getFieldDependencies().size();
        if (currentDependencyCount > previousDependencyCount) {
            // 移除多余的依赖
            for (int i = currentDependencyCount - 1; i >= previousDependencyCount; i--) {
                result.getFieldDependencies().remove(i);
            }
            log.debug("Removed {} subquery dependencies", currentDependencyCount - previousDependencyCount);
        }
        
        // 注册子查询别名
        if (alias != null) {
            // 子查询作为一个虚拟表，使用别名作为表名
            tracker.registerTableAlias(alias, alias);
            currentTableName = alias;
            currentTableAlias = alias;
            log.debug("Registered subquery alias: {}", alias);
        }
        
        return false;
    }
    
    /**
     * 访问 SELECT 列表项
     */
    @Override
    public boolean visit(SQLSelectItem x) {
        SQLExpr expr = x.getExpr();
        String alias = x.getAlias();
        
        currentDependency = new FieldDependency();
        
        // 设置目标字段名和别名
        if (alias != null) {
            currentDependency.setTargetAlias(alias);
            currentDependency.setTargetField(alias);
            tracker.registerFieldAlias(alias, expr.toString());
        } else if (expr instanceof SQLIdentifierExpr) {
            String fieldName = ((SQLIdentifierExpr) expr).getName();
            currentDependency.setTargetField(fieldName);
        } else if (expr instanceof SQLPropertyExpr) {
            SQLPropertyExpr propExpr = (SQLPropertyExpr) expr;
            currentDependency.setTargetField(propExpr.getName());
        } else if (expr instanceof SQLAllColumnExpr) {
            currentDependency.setTargetField("*");
        } else {
            currentDependency.setTargetField(expr.toString());
        }
        
        // 访问表达式提取来源字段
        if (expr != null) {
            expr.accept(this);
        }
        
        // 设置表信息
        if (currentDependency.getSourceTable() == null) {
            currentDependency.setSourceTable(currentTableName);
            currentDependency.setSourceTableAlias(currentTableAlias);
        }
        
        result.addFieldDependency(currentDependency);
        log.debug("Added field dependency: {}", currentDependency.getTargetField());
        
        return false;
    }
    
    /**
     * 访问标识符（字段名）
     */
    @Override
    public boolean visit(SQLIdentifierExpr x) {
        String fieldName = x.getName();
        if (currentDependency != null) {
            currentDependency.addSourceField(fieldName);
            log.debug("Found source field: {}", fieldName);
        }
        return false;
    }
    
    /**
     * 访问属性表达式（table.column）
     */
    @Override
    public boolean visit(SQLPropertyExpr x) {
        String tableName = x.getOwnernName();
        String fieldName = x.getName();
        
        if (currentDependency != null) {
            currentDependency.addSourceField(fieldName);
            
            // 解析表别名
            String resolvedTable = tracker.resolveTableAlias(tableName);
            currentDependency.setSourceTable(resolvedTable);
            currentDependency.setSourceTableAlias(tableName);
            
            log.debug("Found qualified field: {}.{}", tableName, fieldName);
        }
        
        return false;
    }
    
    /**
     * 访问聚合函数
     */
    @Override
    public boolean visit(SQLAggregateExpr x) {
        if (currentDependency != null) {
            currentDependency.setAggregation(true);
            currentDependency.setExpression(x.toString());
            log.debug("Found aggregation: {}", x.getMethodName());
        }
        
        // 递归访问参数
        for (SQLExpr arg : x.getArguments()) {
            arg.accept(this);
        }
        
        return false;
    }
    
    /**
     * 访问 * 表达式
     */
    @Override
    public boolean visit(SQLAllColumnExpr x) {
        if (currentDependency != null) {
            currentDependency.addSourceField("*");
            log.debug("Found wildcard column");
        }
        return false;
    }
}
