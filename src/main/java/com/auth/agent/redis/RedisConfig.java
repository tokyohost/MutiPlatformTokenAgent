package com.auth.agent.redis;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/4 10:18
 * @Content
 */

public class RedisConfig {
    String host;
    int port;
    String password;
    int database;

    static RedisConfig defaultConfig() {
        RedisConfig cfg = new RedisConfig();
        cfg.host = "127.0.0.1";
        cfg.port = 6379;
        cfg.database = 0;
        return cfg;
    }
}