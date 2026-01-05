package com.auth.agent.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/5 8:40
 * @Content
 */

public class ParamsUtils {
    static volatile ConcurrentHashMap<String, String> paramsMap = new ConcurrentHashMap<>();

    public synchronized static Map<String, String> getParamsMap() {
        return ParamsUtils.paramsMap;
    }

    public synchronized static void setParamsMap(ConcurrentHashMap<String, String> paramsMap) {
        ParamsUtils.paramsMap = paramsMap;
    }

}
