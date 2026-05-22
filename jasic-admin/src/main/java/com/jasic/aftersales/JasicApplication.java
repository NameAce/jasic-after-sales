package com.jasic.aftersales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 佳士售后系统启动类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@SpringBootApplication
public class JasicApplication {

    /**SUCCESS_BANNER_TEMPLATE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String SUCCESS_BANNER_TEMPLATE =
            "\n" +
            "==============================================================\n" +
            "      _           _        _         _    __ _\n" +
            "     | | __ _ ___(_) ___  / \\   __ _| |_ / _| | ___  ___\n" +
            "     | |/ _` / __| |/ __|/ _ \\ / _` | __| |_| |/ _ \\/ __|\n" +
            "     | | (_| \\__ \\ | (__/ ___ \\ (_| | |_|  _| |  __/\\__ \\\n" +
            "     |_|\\__,_|___/_|\\___/_/   \\_\\__,_|\\__|_| |_|\\___||___/\n" +
            "\n" +
            "                 JJJJJ    AAAAA    SSSSS   IIIII   CCCCC\n" +
            "                   J     A     A  S          I    C\n" +
            "                   J     AAAAAAA   SSSSS      I    C\n" +
            "               J   J     A     A       S      I    C\n" +
            "                JJJ      A     A   SSSSS    IIIII   CCCCC\n" +
            "\n" +
            "                  SHENZHEN JASIC TECHNOLOGY CO., LTD.\n" +
            "                    佳士科技 | AFTER-SALES SERVICE\n" +
            "               Welding Equipment and Service Platform\n" +
            "--------------------------------------------------------------\n" +
            "  Application : %s\n" +
            "  Profile     : %s\n" +
            "  API         : %s\n" +
            "  Knife4j     : %s\n" +
            "==============================================================\n";

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(JasicApplication.class, args);
        printSuccessBanner(context.getEnvironment());
    }

    /**
     * 启动完成后输出后端服务成功横幅，便于直接确认当前环境和访问地址。
     *
     * @param environment Spring 环境配置
     */
    private static void printSuccessBanner(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name", "jasic-after-sales");
        String[] activeProfiles = environment.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default";
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String normalizedContextPath = "/".equals(contextPath) ? "" : contextPath;
        String apiUrl = "http://localhost:" + port + normalizedContextPath;
        String docUrl = apiUrl + "/doc.html";
        System.out.printf(SUCCESS_BANNER_TEMPLATE, applicationName, profile, apiUrl, docUrl);
    }
}
