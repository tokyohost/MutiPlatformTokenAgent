package com.auth.agent;

import com.auth.agent.config.AgentLogbackInitializer;
import com.auth.agent.config.AgentLogger;
import com.auth.agent.redis.LettuceHolder;
import com.auth.agent.utils.ParamsUtils;
import io.lettuce.core.api.sync.RedisCommands;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/4 10:10
 * @Content
 */
public class AgentStarter {

    // JVM 启动时加载
    public static void premain(String agentArgs, Instrumentation inst) {

        AgentLogbackInitializer.init();
        AgentLogger.init();  // 初始化 Agent 日志
        Logger log = AgentLogger.agentLog;
        log.info(" premain start");
        ConcurrentHashMap<String, String> parse = parse(agentArgs);
        ParamsUtils.setParamsMap(parse);
        // 初始化
        LettuceHolder.initConfig("/redis.yml");

        // 获取 Redis commands
        RedisCommands<String, String> redis = LettuceHolder.getCommands();
        redis.set("agent:test", "hello lettuce!");
        String value = redis.get("agent:test");
        log.info("Redis value: " + value);
        log.info("App start Invoke SpringRunAdvice");
        hookSpringApplicationRun(inst, SpringRunAdvice.class);
        log.info(" premain, args=" + agentArgs);



    }

    // JVM 运行时 attach
    public static void agentmain(String agentArgs, Instrumentation inst) {
        Logger log = AgentLogger.agentLog;
        log.info(" agentmain, args=" + agentArgs);
    }
    public static ConcurrentHashMap<String, String> parse(String agentArgs) {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        if (agentArgs == null || agentArgs.isEmpty()) {
            return map;
        }
        for (String arg : agentArgs.split(",")) {
            String[] kv = arg.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    private static void hookSpringApplicationRun(Instrumentation inst,Class interceptorClass) {
        AgentBuilder.Transformer transformer = (builder, typeDescription, classLoader, module,protectionDomain) ->
                builder.visit(Advice.to(interceptorClass).on(ElementMatchers.named("run")));

        new AgentBuilder.Default()
                .type(ElementMatchers.named("org.springframework.boot.SpringApplication"))
                .transform(transformer)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .installOn(inst);

    }
}
