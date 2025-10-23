package com.lineage.core;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import com.alibaba.druid.util.JdbcConstants;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 探索Druid如何表示窗口函数的AST结构
 */
class WindowFunctionExplorationTest {

    @Test
    void exploreRowNumberWindowFunction() {
        String sql = "SELECT ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary DESC) AS rn, name FROM employees";
        
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        SQLSelectStatement stmt = (SQLSelectStatement) stmtList.get(0);
        SQLSelectQueryBlock query = stmt.getSelect().getQueryBlock();
        
        System.out.println("=== ROW_NUMBER Window Function ===");
        for (SQLSelectItem item : query.getSelectList()) {
            SQLExpr expr = item.getExpr();
            System.out.println("Item: " + item);
            System.out.println("Expr class: " + expr.getClass().getName());
            System.out.println("Expr toString: " + expr);
            
            // 探索AST结构
            expr.accept(new MySqlASTVisitorAdapter() {
                @Override
                public boolean visit(SQLAggregateExpr x) {
                    System.out.println("  -> SQLAggregateExpr detected");
                    System.out.println("     Method: " + x.getMethodName());
                    System.out.println("     Has OVER: " + (x.getOver() != null));
                    if (x.getOver() != null) {
                        System.out.println("     OVER: " + x.getOver());
                        System.out.println("     PARTITION BY: " + x.getOver().getPartitionBy());
                        System.out.println("     ORDER BY: " + x.getOver().getOrderBy());
                    }
                    return true;
                }
                
                @Override
                public boolean visit(SQLMethodInvokeExpr x) {
                    System.out.println("  -> SQLMethodInvokeExpr detected");
                    System.out.println("     Method: " + x.getMethodName());
                    return true;
                }
            });
            System.out.println();
        }
    }
    
    @Test
    void exploreLagWindowFunction() {
        String sql = "SELECT LAG(amount, 1) OVER (ORDER BY order_date) AS prev_amount FROM orders";
        
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        SQLSelectStatement stmt = (SQLSelectStatement) stmtList.get(0);
        SQLSelectQueryBlock query = stmt.getSelect().getQueryBlock();
        
        System.out.println("=== LAG Window Function ===");
        for (SQLSelectItem item : query.getSelectList()) {
            SQLExpr expr = item.getExpr();
            System.out.println("Item: " + item);
            System.out.println("Expr class: " + expr.getClass().getName());
            System.out.println("Expr toString: " + expr);
            
            expr.accept(new MySqlASTVisitorAdapter() {
                @Override
                public boolean visit(SQLAggregateExpr x) {
                    System.out.println("  -> SQLAggregateExpr detected");
                    System.out.println("     Method: " + x.getMethodName());
                    System.out.println("     Arguments: " + x.getArguments());
                    System.out.println("     Has OVER: " + (x.getOver() != null));
                    if (x.getOver() != null) {
                        System.out.println("     OVER: " + x.getOver());
                        System.out.println("     PARTITION BY: " + x.getOver().getPartitionBy());
                        System.out.println("     ORDER BY: " + x.getOver().getOrderBy());
                    }
                    return true;
                }
            });
            System.out.println();
        }
    }
    
    @Test
    void exploreRankWindowFunction() {
        String sql = "SELECT RANK() OVER (PARTITION BY category ORDER BY price DESC) AS price_rank, product_name FROM products";
        
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        SQLSelectStatement stmt = (SQLSelectStatement) stmtList.get(0);
        SQLSelectQueryBlock query = stmt.getSelect().getQueryBlock();
        
        System.out.println("=== RANK Window Function ===");
        for (SQLSelectItem item : query.getSelectList()) {
            SQLExpr expr = item.getExpr();
            System.out.println("Item: " + item);
            System.out.println("Expr class: " + expr.getClass().getName());
            
            expr.accept(new MySqlASTVisitorAdapter() {
                @Override
                public boolean visit(SQLAggregateExpr x) {
                    System.out.println("  -> SQLAggregateExpr detected");
                    System.out.println("     Method: " + x.getMethodName());
                    System.out.println("     Has OVER: " + (x.getOver() != null));
                    if (x.getOver() != null) {
                        System.out.println("     OVER: " + x.getOver());
                        System.out.println("     PARTITION BY: " + x.getOver().getPartitionBy());
                        System.out.println("     ORDER BY: " + x.getOver().getOrderBy());
                    }
                    return true;
                }
            });
            System.out.println();
        }
    }
}
