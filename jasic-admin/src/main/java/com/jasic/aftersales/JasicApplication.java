package com.jasic.aftersales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 佳士售后系统启动类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@SpringBootApplication
public class JasicApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(JasicApplication.class, args);
        System.out.println("========== 佳士售后系统启动成功 ==========");
    }
}
