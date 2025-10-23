package com.lineage.core;

import com.lineage.core.tracker.LineageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PerformanceTest {

    @Autowired
    private LineageAnalyzer analyzer;

    private Runtime runtime;

    @BeforeEach
    void setUp() {
        runtime = Runtime.getRuntime();
        System.gc();
    }

    @Test
    void testLargeComplexSQL() {
        String sql = generateLargeSQL(1000);
        
        long start = System.currentTimeMillis();
        LineageResult result = analyzer.analyze(sql, "mysql");
        long duration = System.currentTimeMillis() - start;
        
        System.out.println("=== Large SQL Test ===");
        System.out.println("Lines: " + countLines(sql));
        System.out.println("Time: " + duration + " ms");
        System.out.println("Tables: " + result.getTables().size());
        
        assertTrue(result.isSuccess());
        assertTrue(duration < 600_000, "Should complete in < 600s");
        printMemory();
    }

    @Test
    void testMultiTableJoin() {
        String sql = generateJoinSQL(25);
        
        long start = System.currentTimeMillis();
        LineageResult result = analyzer.analyze(sql, "mysql");
        long duration = System.currentTimeMillis() - start;
        
        System.out.println("=== Multi-Table JOIN ===");
        System.out.println("Tables: " + result.getTables().size());
        System.out.println("Time: " + duration + " ms");
        
        assertTrue(result.isSuccess());
        assertTrue(result.getTables().size() >= 20);
        
        double accuracy = (double) result.getTables().size() / 25 * 100;
        System.out.println("Accuracy: " + accuracy + "%");
        assertTrue(accuracy > 95);
        printMemory();
    }

    private String generateLargeSQL(int lines) {
        StringBuilder sql = new StringBuilder("SELECT\n");
        
        for (int i = 1; i <= 50; i++) {
            sql.append("    t").append(i % 10 + 1).append(".col").append(i).append(",\n");
        }
        sql.setLength(sql.length() - 2);
        sql.append("\nFROM table1 t1\n");
        
        for (int i = 2; i <= 10; i++) {
            sql.append("JOIN table").append(i).append(" t").append(i)
               .append(" ON t1.id = t").append(i).append(".id\n");
        }
        
        int unions = (lines - countLines(sql.toString())) / 10;
        for (int u = 0; u < unions; u++) {
            sql.append("UNION ALL\n");
            sql.append("SELECT ");
            for (int i = 1; i <= 50; i++) {
                sql.append("NULL");
                if (i < 50) sql.append(", ");
            }
            sql.append(" FROM table").append(u + 11).append("\n");
        }
        
        return sql.toString();
    }

    private String generateJoinSQL(int tables) {
        StringBuilder sql = new StringBuilder("SELECT\n");
        for (int i = 1; i <= tables; i++) {
            sql.append("    t").append(i).append(".id");
            if (i < tables) sql.append(",\n");
        }
        sql.append("\nFROM table1 t1\n");
        
        for (int i = 2; i <= tables; i++) {
            sql.append("JOIN table").append(i).append(" t").append(i)
               .append(" ON t1.id = t").append(i).append(".id\n");
        }
        
        return sql.toString();
    }

    private int countLines(String s) {
        return s.split("\n").length;
    }

    private void printMemory() {
        long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        System.out.println("Memory: " + used + " MB");
        assertTrue(used < 512, "Memory < 512MB");
    }
}
