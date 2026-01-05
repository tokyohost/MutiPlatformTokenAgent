package com.auth.agent.config;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/4 16:45
 * @Content
 */

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;

public class AgentLogbackInitializer {

    public static void init() {
        try {
            LoggerContext context =
                    (LoggerContext) LoggerFactory.getILoggerFactory();

            context.reset();

            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);

            configurator.doConfigure(
                    AgentLogbackInitializer.class
                            .getClassLoader()
                            .getResource("agent-logback.xml")
            );

            StatusPrinter.printInCaseOfErrorsOrWarnings(context);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
