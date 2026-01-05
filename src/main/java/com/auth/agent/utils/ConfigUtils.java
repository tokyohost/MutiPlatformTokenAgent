package com.auth.agent.utils;

import com.auth.agent.config.AgentLogger;
import com.auth.agent.constant.Constant;
import com.auth.agent.domain.AuthData;
import com.auth.agent.redis.LettuceHolder;
import com.sun.org.apache.bcel.internal.Const;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/5 9:24
 * @Content
 */

public class ConfigUtils {
    /**
     * 初始化 Redis 客户端
     */
    public static  <T> T getFromYaml(String yamlPath,String key) {
        if(yamlPath!=null && !yamlPath.startsWith("/")){
            yamlPath="/"+yamlPath;
        }
        try (InputStream in = LettuceHolder.class.getResourceAsStream(yamlPath)) {
            if (in == null) {
                throw new RuntimeException("yaml not found: " + yamlPath);
            }
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(in);

            T value = (T) map.getOrDefault(key, null);
            return value;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get "+key+" from yaml"+yamlPath,e);
        }
    }

    public static String getRedisKeyFromPramas() {
        Map<String, String> paramsMap = ParamsUtils.getParamsMap();
        if(paramsMap.containsKey(Constant.CONFIG_REDIS_KEY)){
            return paramsMap.get(Constant.CONFIG_REDIS_KEY);
        }else{
            throw new RuntimeException("未在参数中找到redisKey配置项");
        }
    }

    public static InputStream getConfigFileAsStream(String configFileName) throws IOException {
        Logger log = AgentLogger.agentLog;
        Path path = Paths.get(System.getProperty("user.dir"), configFileName);
        log.info(" 使用外部配置文件: {}", path.toString());
        InputStream in = Files.newInputStream(path);
        if (in == null) {
            throw new RuntimeException("配置文件未找到: " + configFileName);
        }
        return in;
    }
    public static String getRedisKey() {
        Logger log = AgentLogger.agentLog;
        Map<String, String> paramsMap = ParamsUtils.getParamsMap();
        //判断是否指定外部密码配置文件
        if (paramsMap.containsKey(Constant.PWD_CONFIG_FILE_OUT_SIDE_NAME)) {
            try {
                InputStream configFileAsStream = getConfigFileAsStream(paramsMap.get(Constant.PWD_CONFIG_FILE_OUT_SIDE_NAME));
                Yaml yaml = new Yaml();
                Map<String, Object> map = yaml.load(configFileAsStream);
                String redisKey =(String) map.get(Constant.CONFIG_REDIS_KEY);
                return redisKey;
            } catch (IOException e) {
                throw new RuntimeException("配置文件异常"+e);
            }
        }

        if (paramsMap.containsKey(Constant.PWD_CONFIG_FILE_NAME)) {
            //存在密码配置文件，使用指定的密码配置文件
            String pwdConfigFileName = paramsMap.get(Constant.PWD_CONFIG_FILE_NAME);
            log.info(" 使用指定的密码配置文件: {}", pwdConfigFileName);
            String redisKey = ConfigUtils.getFromYaml(pwdConfigFileName, Constant.CONFIG_REDIS_KEY);
            if(redisKey==null){
                //从参数找
                return getRedisKeyFromPramas();
            }else{
                return redisKey;
            }
        }else{
            return getRedisKeyFromPramas();
        }
    }

    public static Integer getRefushTokenTimeFromPramas() {
        Logger log = AgentLogger.agentLog;
        Map<String, String> paramsMap = ParamsUtils.getParamsMap();
        if(paramsMap.containsKey(Constant.REFUSH_TOKEN_TIME)){
            String time = paramsMap.get(Constant.REFUSH_TOKEN_TIME);
            return Integer.valueOf(time);
        }else{
            log.info("未在参数中找到redisKey配置项");
            return 300;
        }
    }
    public static Integer getRefushTokenTime() {
        Logger log = AgentLogger.agentLog;
        Map<String, String> paramsMap = ParamsUtils.getParamsMap();
        //判断是否指定外部密码配置文件
        if (paramsMap.containsKey(Constant.PWD_CONFIG_FILE_OUT_SIDE_NAME)) {
            try {
                InputStream configFileAsStream = getConfigFileAsStream(paramsMap.get(Constant.PWD_CONFIG_FILE_OUT_SIDE_NAME));
                Yaml yaml = new Yaml();
                Map<String, Object> map = yaml.load(configFileAsStream);
                Integer refushTokenTime =(Integer) map.get(Constant.REFUSH_TOKEN_TIME);
                if (refushTokenTime == null) {
                    return 300;
                }
                return refushTokenTime;
            } catch (IOException e) {
                log.error("配置文件异常"+e);
                return 300;
            }
        }

        if (paramsMap.containsKey(Constant.PWD_CONFIG_FILE_NAME)) {
            //存在密码配置文件，使用指定的内部配置文件
            String pwdConfigFileName = paramsMap.get(Constant.PWD_CONFIG_FILE_NAME);
            log.info(" 使用指定的内部配置文件: {}", pwdConfigFileName);
            Integer refushTokenTime = ConfigUtils.getFromYaml(pwdConfigFileName, Constant.REFUSH_TOKEN_TIME);
            if(refushTokenTime==null){
                //从参数找
                return getRefushTokenTimeFromPramas();
            }else{
                return refushTokenTime;
            }
        }else{
            return getRefushTokenTimeFromPramas();
        }
    }
    public static AuthData getAuthData(String type) {
        Logger log = AgentLogger.agentLog;
        Map<String, String> paramsMap = ParamsUtils.getParamsMap();
        //判断是否指定密码配置文件
        String username;
        String password;
        String tenantAccount = "default";
        String clientId;
        String tenantId = "000000";
        Integer timeout = 10800;
        //判断是否指定外部密码配置文件
        if (paramsMap.containsKey(Constant.PWD_CONFIG_FILE_OUT_SIDE_NAME)) {
            try {
                InputStream configFileAsStream = getConfigFileAsStream(paramsMap.get(Constant.PWD_CONFIG_FILE_OUT_SIDE_NAME));
                Yaml yaml = new Yaml();
                Map<String, Object> map = yaml.load(configFileAsStream);
                username =(String) map.get(Constant.PWD_CONFIG_FILE_USERNAME);
                password =(String) map.get(Constant.PWD_CONFIG_FILE_PASSWORD);
                tenantAccount =(String) map.getOrDefault(Constant.PWD_CONFIG_FILE_TENANTACCOUNT,"default");
                clientId =(String) map.getOrDefault(Constant.PWD_CONFIG_FILE_CLIENT_ID,null);
                tenantId =(String) map.getOrDefault(Constant.PWD_CONFIG_FILE_TENANT_ID,"000000");
                timeout = (Integer)map.getOrDefault(Constant.PWD_CONFIG_FILE_TIMEOUT,10800);
                AuthData authData = new AuthData();
                authData.setUsername(username);
                authData.setPassword(password);
                authData.setTenantAccount(tenantAccount);
                authData.setClientId(clientId);
                authData.setTenantId(tenantId);
                authData.setTimeout(timeout);
                return authData;
            } catch (IOException e) {
                throw new RuntimeException("配置文件异常"+e);
            }
        }


        if (paramsMap.containsKey(Constant.PWD_CONFIG_FILE_NAME)) {
            //存在密码配置文件，使用指定的密码配置文件
            String pwdConfigFileName = paramsMap.get(Constant.PWD_CONFIG_FILE_NAME);
            log.info(" 使用指定的密码配置文件: {}", pwdConfigFileName);
            username = ConfigUtils.getFromYaml(pwdConfigFileName, Constant.PWD_CONFIG_FILE_USERNAME);
            password = ConfigUtils.getFromYaml(pwdConfigFileName, Constant.PWD_CONFIG_FILE_PASSWORD);
            tenantAccount = ConfigUtils.getFromYaml(pwdConfigFileName, Constant.PWD_CONFIG_FILE_TENANTACCOUNT);
            clientId = ConfigUtils.getFromYaml(pwdConfigFileName, Constant.PWD_CONFIG_FILE_CLIENT_ID);
            tenantId = ConfigUtils.getFromYaml(pwdConfigFileName, Constant.PWD_CONFIG_FILE_TENANT_ID);
            timeout = ConfigUtils.getFromYaml(pwdConfigFileName, Constant.PWD_CONFIG_FILE_TIMEOUT);
        }else{
            if(paramsMap.containsKey(Constant.PWD_CONFIG_FILE_USERNAME)){
                log.info(" 使用参数中的用户名: {}", paramsMap.get(Constant.PWD_CONFIG_FILE_USERNAME));
                username = paramsMap.get(Constant.PWD_CONFIG_FILE_USERNAME);
            }else{
                throw new RuntimeException("未指定账号密码配置文件或传入账号密码参数[username]");
            }
            if(paramsMap.containsKey(Constant.PWD_CONFIG_FILE_PASSWORD)){
                log.info(" 使用参数中的用户名: {}", paramsMap.get(Constant.PWD_CONFIG_FILE_PASSWORD));
                password = paramsMap.get(Constant.PWD_CONFIG_FILE_PASSWORD);
            }else{
                throw new RuntimeException("未指定账号密码配置文件或传入账号密码参数[password]");
            }
            if(Constant.ENHANCE_TYPE_RUOYIVUE_PLUS.equalsIgnoreCase(type) && paramsMap.containsKey(Constant.PWD_CONFIG_FILE_CLIENT_ID)){
                log.info(" 使用参数中的客户端ID: {}", paramsMap.get(Constant.PWD_CONFIG_FILE_CLIENT_ID));
                clientId = paramsMap.get(Constant.PWD_CONFIG_FILE_CLIENT_ID);
            }else{
                throw new RuntimeException("未指定账号密码配置文件或传入账号密码参数[clientId]");
            }
            if(Constant.ENHANCE_TYPE_RUOYIVUE_PLUS.equalsIgnoreCase(type) && paramsMap.containsKey(Constant.PWD_CONFIG_FILE_TENANT_ID)){
                log.info(" 使用参数中的tenantId: {}", paramsMap.get(Constant.PWD_CONFIG_FILE_TENANT_ID));
                tenantId = paramsMap.get(Constant.PWD_CONFIG_FILE_TENANT_ID);
            }else{
                throw new RuntimeException("未指定账号密码配置文件或传入账号密码参数[tenantId]");
            }
            if(Constant.ENHANCE_TYPE_CTSI.equalsIgnoreCase(type) && paramsMap.containsKey(Constant.PWD_CONFIG_FILE_TENANTACCOUNT)){
                log.info(" 使用参数中的tenantAccount: {}", paramsMap.get(Constant.PWD_CONFIG_FILE_TENANTACCOUNT));
                tenantAccount = paramsMap.get(Constant.PWD_CONFIG_FILE_TENANTACCOUNT);
            }else{
                throw new RuntimeException("未指定账号密码配置文件或传入账号密码参数[tenantAccount]");
            }
        }
        AuthData authData = new AuthData();
        authData.setUsername(username);
        authData.setPassword(password);
        authData.setTenantAccount(tenantAccount);
        authData.setClientId(clientId);
        authData.setTenantId(tenantId);
        authData.setTimeout(timeout);
        return  authData;
    }
}
