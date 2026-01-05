package com.auth.agent.redis;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/4 17:26
 * @Content
 */

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisAgentClient {

    private static RedisCommands<String, String> commands;

    public static void init(String redisUri) {
        RedisClient client = RedisClient.create(redisUri); // e.g. "redis://:password@127.0.0.1:6379/0"
        commands = client.connect().sync();
    }

    public static String get(String key) {
        return commands.get(key);
    }

    public static void set(String key, String value) {
        commands.set(key, value);
    }
}
