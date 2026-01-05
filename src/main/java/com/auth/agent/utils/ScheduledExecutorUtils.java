package com.auth.agent.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/5 14:45
 * @Content
 */

public class ScheduledExecutorUtils {
   public static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
}
