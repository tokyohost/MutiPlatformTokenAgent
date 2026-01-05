package com.auth.agent.ruoyi;

import com.auth.agent.config.AgentLogger;
import org.slf4j.Logger;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/5 14:43
 * @Content
 */

public class CheckRuoyiVuePlusUtils {


    public static boolean isRuoyiVuePlusApp(ClassLoader  classLoader) {
        Logger log = AgentLogger.agentLog;
        try {
            // 这里尝试加载 RuoyiVuePlus 的某个类
            classLoader.loadClass("org.dromara.DromaraApplication");
            log.info("目标jvm 是 RuoyiVuePlus(若依Plus) 项目");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
