package com.auth.agent.ruoyi;

import com.auth.agent.biyi.CtsiEnhance;
import com.auth.agent.constant.Constant;
import com.auth.agent.domain.AuthData;
import com.auth.agent.redis.LettuceHolder;
import com.auth.agent.utils.ConfigUtils;
import com.auth.agent.utils.ScheduledExecutorUtils;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;

import java.lang.reflect.*;
import java.util.concurrent.TimeUnit;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/5 14:45
 * @Content
 */

public class RuoyiVuePlusEnhance {
    public static void enhanceRuoyiVuePlusApp(Object ctx, Logger log) {
        Object oldRequestContext = null;
        Class<?> holderClz  = null;
        Class<?> requestAttributes  = null;
        ClassLoader classLoader = ctx.getClass().getClassLoader();
        try {

            Method getBeanMethod = ctx.getClass().getMethod("getBean", String.class);
            Object passwordAuthStrategy = getBeanMethod.invoke(ctx, "passwordAuthStrategy");
            log.info("Invoke passwordAuthStrategy bean: {}", passwordAuthStrategy);
            Object sysLoginService = getBeanMethod.invoke(ctx, "sysLoginService");
            log.info("Invoke sysLoginService bean: {}", sysLoginService);
            Object sysClientServiceImpl = getBeanMethod.invoke(ctx, "sysClientServiceImpl");
            log.info("Invoke sysClientServiceImpl bean: {}", sysClientServiceImpl);
            AuthData authData = ConfigUtils.getAuthData(Constant.ENHANCE_TYPE_RUOYIVUE_PLUS);

            //先查SysClientVo
            Object sysClientVo = sysClientServiceImpl.getClass().getMethod("queryByClientId", String.class).invoke(sysClientServiceImpl, authData.getClientId());
            if(sysClientVo==null){
                log.error("SysClientVo not found for clientId: {}", authData.getClientId());
                return;
            }

            //构造租户Supplier
            Class<?> supplierClz = classLoader.loadClass("java.util.function.Supplier");

            Object supplier = Proxy.newProxyInstance(
                    classLoader,
                    new Class[]{supplierClz},
                    (proxy, method, args) -> {
                        // 等价于 Lambda 里的内容
                        Method loadUserByUsername = passwordAuthStrategy.getClass().getDeclaredMethod("loadUserByUsername", String.class);
                        loadUserByUsername.setAccessible(true);
                        Object sysUserVo = loadUserByUsername.invoke(passwordAuthStrategy, authData.getUsername());
                        if(sysUserVo==null){
                            log.error("SysUserVo not found for username: {}", authData.getUsername());
                            return null;
                        }
                        return sysLoginService.getClass().getMethod("buildLoginUser", sysUserVo.getClass()).invoke(sysLoginService, sysUserVo);
                    }
            );

            Class<?> tenantHelperClass = classLoader.loadClass("org.dromara.common.tenant.helper.TenantHelper");
            Method dynamic = tenantHelperClass.getMethod(
                    "dynamic",
                    String.class,
                    supplierClz
            );
            Object loginUser = dynamic.invoke(null, authData.getTenantId(), supplier);
            if(loginUser==null){
                log.error("LoginUser not found for tenantId: {} username:{}", authData.getTenantId(),authData.getUsername());
                return;
            }
            Field clientKeyField = sysClientVo.getClass().getDeclaredField("clientKey");
            clientKeyField.setAccessible(true);
            String clientKey = (String)clientKeyField.get(sysClientVo);
            Field deviceTypeField = sysClientVo.getClass().getDeclaredField("deviceType");
            deviceTypeField.setAccessible(true);
            String deviceType = (String)deviceTypeField.get(sysClientVo);
            loginUser.getClass().getMethod("setClientKey", String.class).invoke(loginUser, clientKey);
            loginUser.getClass().getMethod("setDeviceType", String.class).invoke(loginUser, deviceType);

            //准备satoken loginParameter
            Class<?> saloginParameterClass = classLoader.loadClass("cn.dev33.satoken.stp.parameter.SaLoginParameter");
            Object loginParameter = saloginParameterClass.newInstance();
            loginParameter.getClass().getMethod("setDeviceType", String.class).invoke(loginParameter, deviceType);
            loginParameter.getClass().getMethod("setTimeout", long.class).invoke(loginParameter, authData.getTimeout());
            loginParameter.getClass().getMethod("setExtra", String.class,Object.class).invoke(loginParameter, "clientid", authData.getClientId());
            //模拟上下文
            // 保存旧上下文（如果有）
            holderClz = classLoader.loadClass(
                    "org.springframework.web.context.request.RequestContextHolder"
            );
            requestAttributes = classLoader.loadClass("org.springframework.web.context.request.RequestAttributes");
            Method getMethod = holderClz.getMethod("getRequestAttributes");
            oldRequestContext = getMethod.invoke(null);
            initRequest(ctx,authData.getClientId());

            //准备 LoginHelper
            Class<?> loginHelperClass = classLoader.loadClass("org.dromara.common.satoken.utils.LoginHelper");
            loginHelperClass.getMethod("login", loginUser.getClass(), saloginParameterClass)
                    .invoke(null, loginUser, loginParameter);
            //获取请求的token值
            Class<?> stpUtilClass = classLoader.loadClass("cn.dev33.satoken.stp.StpUtil");
            Method getTokenValue = stpUtilClass.getMethod("getTokenValue");
            String invokeToken =(String) getTokenValue.invoke(null);
            if(invokeToken!=null){
                log.info(" 获取 Token 成功: {}", invokeToken);
            }else {
                log.error(" 获取 Token 失败");
            }
            RedisCommands<String, String> redis = LettuceHolder.getCommands();
            redis.set(ConfigUtils.getRedisKey(), invokeToken);
            ScheduledExecutorUtils.scheduledExecutorService.schedule(()->{
                try{
                    RuoyiVuePlusEnhance.enhanceRuoyiVuePlusApp(ctx, log);
                }catch (Exception e){
                    log.info(" 定时刷新 Token 失败", e);
                }
            },30, TimeUnit.SECONDS);
        }catch (Throwable e) {
            log.error(" 获取 Token 失败", e);
        }finally {

            try {
                // 恢复原有上下文，如果没有就 reset
                Method setMethod = null;
                setMethod = holderClz.getMethod(
                        "setRequestAttributes",requestAttributes

                );
                if (oldRequestContext != null) {
                    setMethod.invoke(null, oldRequestContext);
                } else {
                    Method resetMethod = holderClz.getMethod("resetRequestAttributes");
                    resetMethod.invoke(null);
                }
                clearContext(ctx);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        }

    }

    private static Object initRequest(Object ctx, String clientId) {
        try {
            ClassLoader appCl = ctx.getClass().getClassLoader();
            //初始化SaToken 上下文
            Class<?> saRequestForMockClass = appCl.loadClass("cn.dev33.satoken.context.mock.SaRequestForMock");
            Class<?> saResponseForMockClass = appCl.loadClass("cn.dev33.satoken.context.mock.SaResponseForMock");
            Class<?> saStorageForMockClass = appCl.loadClass("cn.dev33.satoken.context.mock.SaStorageForMock");
            Object saRequestForMock = saRequestForMockClass.newInstance();
            Object saResponseForMock = saResponseForMockClass.newInstance();
            Object saStorageForMock = saStorageForMockClass.newInstance();

            Class<?> saManager = appCl.loadClass("cn.dev33.satoken.SaManager");
            Object saTokenContext = saManager.getMethod("getSaTokenContext").invoke(null);

            Class<?> saRequestClass = appCl.loadClass("cn.dev33.satoken.context.model.SaRequest");
            Class<?> saResponseClass = appCl.loadClass("cn.dev33.satoken.context.model.SaResponse");
            Class<?> saStorageClass = appCl.loadClass("cn.dev33.satoken.context.model.SaStorage");
            Method setContext = saTokenContext.getClass().getMethod("setContext", saRequestClass, saResponseClass, saStorageClass);
            setContext.invoke(saTokenContext, saRequestForMock, saResponseForMock, saStorageForMock);

            Class<?> sraClz = appCl.loadClass(
                    "org.springframework.web.context.request.ServletRequestAttributes"
            );


            Class<?> reqIf = Class.forName("jakarta.servlet.http.HttpServletRequest", true, appCl);

//            Class<?> reqIf = appClassLoader.loadClass("jakarta.servlet.http.HttpServletRequest");

            Object requestProxy = Proxy.newProxyInstance(
                    appCl,
                    new Class[]{reqIf},
                    (proxy, method, args) -> {
                        String name = method.getName();

                        switch (name) {
                            case "getHeader":
                                if ("User-Agent".equalsIgnoreCase((String) args[0])) {
                                    return "Agent";
                                }
                                if ("clientid".equalsIgnoreCase((String) args[0])) {
                                    return clientId;
                                }
                                return null;

                            case "getRemoteAddr":
                                return "127.0.0.1";

                            case "X-Forwarded-For":
                                return "127.0.0.1";
                            case "X-Real-IP":
                                return "127.0.0.1";
                            case "Proxy-Client-IP":
                                return "127.0.0.1";
                            case "WL-Proxy-Client-IP":
                                return "127.0.0.1";
                            case "clientid":
                                return "127.0.0.1";

                            case "getMethod":
                                return "POST";

                            case "getRequestURI":
                                return "/agent/login";

                            case "getContextPath":
                                return "";

                            default:
                                // 基本类型必须给默认值
                                Class<?> rt = method.getReturnType();
                                if (rt.isPrimitive()) {
                                    if (rt == boolean.class) return false;
                                    if (rt == int.class) return 0;
                                    if (rt == long.class) return 0L;
                                }
                                return null;
                        }
                    }
            );
            Constructor<?> ctor = sraClz.getConstructor(reqIf);
            Object attrs = ctor.newInstance(requestProxy);
            Class<?> holderClz = appCl.loadClass(
                    "org.springframework.web.context.request.RequestContextHolder"
            );

            Method set = holderClz.getMethod(
                    "setRequestAttributes",
                    appCl.loadClass("org.springframework.web.context.request.RequestAttributes")
            );

            set.invoke(null, attrs);
            return attrs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 清除上下文
     * @param ctx
     */
    private static void clearContext(Object ctx){
        try{
            ClassLoader classLoader = ctx.getClass().getClassLoader();
            Class<?> saManager = classLoader.loadClass("cn.dev33.satoken.SaManager");
            Object saTokenContext = saManager.getMethod("getSaTokenContext").invoke(null);
            saTokenContext.getClass().getMethod("clearContext").invoke(saTokenContext);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
