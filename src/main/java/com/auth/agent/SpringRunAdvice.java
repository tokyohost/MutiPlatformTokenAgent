package com.auth.agent;

import com.auth.agent.biyi.CheckCtsiUtils;
import com.auth.agent.biyi.CtsiEnhance;
import com.auth.agent.config.AgentLogger;
import com.auth.agent.ruoyi.CheckRuoyiVuePlusUtils;
import com.auth.agent.ruoyi.RuoyiVuePlusEnhance;
import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/4 17:03
 * @Content
 */

public class SpringRunAdvice {
    @Advice.OnMethodExit
    public static void onExit(@Advice.Return Object ctx) {
        Logger log = AgentLogger.agentLog;
        log.warn(" SpringApplication.run() 已完成，ApplicationContext=" + ctx);

        //判断是不是比翼项目
        boolean ctsiApp = CheckCtsiUtils.isCtsiApp(ctx.getClass().getClassLoader());
        if (ctsiApp) {
            log.info(" 当前应用为比翼项目，开始进行比翼相关增强操作");
            CtsiEnhance.enhanceCtsiApp(ctx, log);
        } else {
            log.info(" 当前应用非比翼项目，跳过比翼相关增强操作");
        }
        boolean ruoyiVuePlusApp = CheckRuoyiVuePlusUtils.isRuoyiVuePlusApp(ctx.getClass().getClassLoader());
        if(ruoyiVuePlusApp){
            log.info(" 当前应用为若依VuePlus项目，开始进行若依VuePlus相关增强操作");
            RuoyiVuePlusEnhance.enhanceRuoyiVuePlusApp(ctx, log);
        }else{
            log.info( " 当前应用非若依VuePlus项目，跳过若依VuePlus相关增强操作");
        }

    }
}
