package com.lineage;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * SQL字段级血缘分析平台 - 启动类
 * 
 * <p>
 * 基于Spring Boot框架，提供SQL字段血缘分析服务
 * 支持Alibaba Druid多数据库方言解析（Hive/MySQL/Spark）
 * </p>
 * 
 * @author AI Task Engine
 * @version 1.0.0
 * @since 2025-10-23
 */
@Slf4j
@SpringBootApplication
@EnableAsync
@MapperScan("com.lineage.mapper")
public class Application {

    /**
     * 应用程序入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        try {
            ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
            Environment env = context.getEnvironment();
            printStartupInfo(env);
        } catch (Exception e) {
            log.error("应用启动失败", e);
            System.exit(1);
        }
    }

    /**
     * 打印应用启动信息
     * 
     * @param env Spring环境对象
     */
    private static void printStartupInfo(Environment env) {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "/");
        String hostAddress = "localhost";
        
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("无法获取本机IP地址，使用localhost");
        }

        String banner = "\n" +
            "========================================================================\n" +
            "   _____ ____    __       __    _                             \n" +
            "  / ___// __ \\  / /      / /   (_)___  ___  _____ _____ ____ \n" +
            "  \\__ \\/ / / / / /      / /   / / __ \\/ _ \\/ __ `/ __ `/ _ \\\n" +
            " ___/ / /_/ / / /___   / /___/ / / / /  __/ /_/ / /_/ /  __/\n" +
            "/____/\\___\\_\\/_____/  /_____/_/_/ /_/\\___/\\__,_/\\__, /\\___/ \n" +
            "                                                /____/        \n" +
            "========================================================================\n" +
            "应用名称:   " + env.getProperty("spring.application.name", "SQL Lineage Analyzer") + "\n" +
            "应用版本:   1.0.0-SNAPSHOT\n" +
            "运行环境:   " + env.getProperty("spring.profiles.active", "default") + "\n" +
            "数据库类型: " + getDatabaseType(env) + "\n" +
            "========================================================================\n" +
            "访问地址:\n" +
            "  本地:     " + protocol + "://localhost:" + serverPort + contextPath + "\n" +
            "  外部:     " + protocol + "://" + hostAddress + ":" + serverPort + contextPath + "\n" +
            "  H2控制台: " + protocol + "://localhost:" + serverPort + "/h2-console\n" +
            "========================================================================\n" +
            "API文档:    " + protocol + "://localhost:" + serverPort + "/swagger-ui.html\n" +
            "健康检查:   " + protocol + "://localhost:" + serverPort + "/actuator/health\n" +
            "========================================================================\n" +
            "启动成功！";

        log.info(banner);
    }

    /**
     * 获取数据库类型
     * 
     * @param env Spring环境对象
     * @return 数据库类型描述
     */
    private static String getDatabaseType(Environment env) {
        String url = env.getProperty("spring.datasource.url", "");
        if (url.contains("h2")) {
            return "H2 (内嵌数据库)";
        } else if (url.contains("mysql")) {
            return "MySQL";
        } else if (url.contains("postgresql")) {
            return "PostgreSQL";
        } else {
            return "Unknown";
        }
    }
}
