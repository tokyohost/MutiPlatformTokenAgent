package com.auth.agent.biyi;

import com.auth.agent.config.AgentLogger;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/4 17:40
 * @Content
 */

public class CheckCtsiUtils {

    public static boolean isCtsiApp(ClassLoader  classLoader) {
        Logger log = AgentLogger.agentLog;
        try {
            //判断是否存在包名 com.ctsi开头的
            // 这里尝试加载 CTSI 的某个类
            classLoader.loadClass("com.ctsi.ssdc.WebApplication");
            log.info("目标jvm 是 CTSI(比翼) 项目");
            return true;
        } catch (ClassNotFoundException e) {
           return false;
        }
    }
}
