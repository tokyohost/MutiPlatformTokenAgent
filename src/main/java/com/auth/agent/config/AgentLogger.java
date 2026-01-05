package com.auth.agent.config;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/4 16:53
 * @Content
 */

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentLogger {

    public static Logger agentLog;

    public static void init() {
        try {
            LoggerContext context = new LoggerContext();
            context.setName("AgentLoggerContext");

            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);

            // 用 Agent 自己的 classloader 加载 XML
            configurator.doConfigure(
                    AgentLogger.class.getClassLoader().getResourceAsStream("agent-logback.xml")
            );

            StatusPrinter.printInCaseOfErrorsOrWarnings(context);

            agentLog = context.getLogger("Agent");
            agentLog.info(" Logger initialized");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

