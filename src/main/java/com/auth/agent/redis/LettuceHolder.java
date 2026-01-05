package com.auth.agent.redis;

import com.auth.agent.constant.Constant;
import com.auth.agent.utils.ConfigUtils;
import com.auth.agent.utils.ParamsUtils;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/4 17:29
 * @Content
 */

public class LettuceHolder {

    private static volatile RedisClient client;
    private static volatile StatefulRedisConnection<String, String> connection;
    private static volatile RedisCommands<String, String> commands;

    private LettuceHolder() {}

    /**
     * 初始化 Redis 客户端
     */
    public static void initConfig(String yamlPath) {
        Map<String, String> paramsMap = ParamsUtils.getParamsMap();
        try{
            //判断是否指定外部密码配置文件
            if (paramsMap.containsKey(Constant.PWD_CONFIG_FILE_OUT_SIDE_NAME)) {
                try {
                    InputStream configFileAsStream = ConfigUtils.getConfigFileAsStream(paramsMap.get(Constant.PWD_CONFIG_FILE_OUT_SIDE_NAME));
                    Yaml yaml = new Yaml();
                    Map<String, Object> map = yaml.load(configFileAsStream);
                    String redisHost =(String) map.get(Constant.REDIS_CONFIG_FILE_HOST);
                    Integer redisPort =(Integer) map.get(Constant.REDIS_CONFIG_FILE_PORT);
                    String redisPassword =(String) map.get(Constant.REDIS_CONFIG_FILE_PASSWORD);
                    Integer redisDatebase =(Integer) map.get(Constant.REDIS_CONFIG_FILE_DATABASE);
                    init(redisHost, redisPort, redisPassword,redisDatebase);
                    return;
                } catch (IOException e) {
                    throw new RuntimeException("配置文件异常"+e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to init Redis from outside Config file", e);
        }




        try (InputStream in = LettuceHolder.class.getResourceAsStream(yamlPath)) {
            if (in == null) {
                throw new RuntimeException("Redis yaml not found: " + yamlPath);
            }
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(in);

            String host;
            if(paramsMap.containsKey("redisHost")){
                host = paramsMap.get("redisHost");
            }else{
                host = (String) map.getOrDefault("host", "127.0.0.1");
            }
            Integer port;
            if(paramsMap.containsKey("redisPort")){
                port = Integer.parseInt(paramsMap.get("redisPort"));
            }else{
                port = (Integer) map.getOrDefault("port", 6379);
            }

            String password;
            if(paramsMap.containsKey("redisPassword")){
                password = paramsMap.get("redisPassword");
            }else{
                password = (String) map.getOrDefault("password", "");
            }
            Integer db;
            if(paramsMap.containsKey("redisDatabase")){
                db = Integer.parseInt(paramsMap.get("redisDatabase"));
            }else{
                db = (Integer) map.getOrDefault("database", 0);
            }

            init(host, port, password,db);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init Redis from yaml", e);
        }
    }

    /**
     * 初始化 Redis 客户端
     */
    public static void init(String host, int port, String password,int db) {
        if (client == null) {
            synchronized (LettuceHolder.class) {
                if (client == null) {
                    String uri = "redis://" + (password != null && !password.isEmpty() ? ":" + password + "@" : "") + host + ":" + port + "/"+db;
                    client = RedisClient.create(uri);
                    connection = client.connect();
                    commands = connection.sync();
                }
            }
        }
    }

    public static RedisCommands<String, String> getCommands() {
        if (commands == null) {
            throw new IllegalStateException("Redis not initialized. Call init() first.");
        }
        return commands;
    }

    public static void shutdown() {
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }
}
