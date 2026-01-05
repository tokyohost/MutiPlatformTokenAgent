package com.auth.agent.biyi;

import com.auth.agent.constant.Constant;
import com.auth.agent.domain.AuthData;
import com.auth.agent.redis.LettuceHolder;
import com.auth.agent.utils.ConfigUtils;
import com.auth.agent.utils.ScheduledExecutorUtils;
import io.lettuce.core.api.sync.RedisCommands;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/4 17:52
 * @Content
 */

public class CtsiEnhance {

    public static void enhanceCtsiApp(Object ctx, Logger log) {
        try {
            Method getBeanMethod = ctx.getClass().getMethod("getBean", String.class);
            Object userJwtController = getBeanMethod.invoke(ctx, "userJwtController");

            log.info(" 获取到 Bean: {}", userJwtController);
            //todo 注入用户名密码获取token
            ClassLoader classLoader = ctx.getClass().getClassLoader();
            Class<?> cpUserFormClass = classLoader.loadClass("com.ctsi.ssdc.model.CscpUserForm");
            Object authInstance = cpUserFormClass.getDeclaredConstructor().newInstance();
            log.info(" 创建 CscpUserForm 实例: {}", authInstance);
            AuthData authData = ConfigUtils.getAuthData(Constant.ENHANCE_TYPE_CTSI);


            Method setUsername = cpUserFormClass.getMethod("setUsername", String.class);
            setUsername.invoke(authInstance, authData.getUsername());
            Method setPassword = cpUserFormClass.getMethod("setPassword", String.class);
            setPassword.invoke(authInstance, authData.getPassword());

            Method setTenantAccount = cpUserFormClass.getMethod("setTenantAccount", String.class);
            if (StringUtil.isNullOrEmpty(authData.getTenantAccount())) {
                setTenantAccount.invoke(authInstance, "default");
                authData.setTenantAccount("default");
            } else {
                setTenantAccount.invoke(authInstance, authData.getTenantAccount());
            }
            log.info(" 设置认证信息: {} {} {}", authData.getUsername(), authData.getPassword(), authData.getTenantAccount());
            //拿TenantService
            Object cscpTenantServiceImpl = getBeanMethod.invoke(ctx, "cscpTenantServiceImpl");
            Class<?> cscpTenantServiceImplClass = cscpTenantServiceImpl.getClass();
            Method selectByTenantAccount = cscpTenantServiceImplClass.getMethod("selectByTenantAccount", String.class);
            Object tenant = selectByTenantAccount.invoke(cscpTenantServiceImpl,
                    authData.getTenantAccount());

            /**
             * String principal = cscpTenant.getId() + "," + cscpTenant.getTenantAccount() + "," + userName;
             *             UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(principal, password);
             *             Authentication authentication = null;
             *
             *             try {
             *                 authentication = this.authenticationManager.authenticate(authenticationToken);
             *             } catch (AuthenticationException var19) {
             */

            Field id = tenant.getClass().getDeclaredField("id");
            id.setAccessible(true);
            Object tenantId = id.get(tenant);
            Field tenantAccount = tenant.getClass().getDeclaredField("tenantAccount");
            tenantAccount.setAccessible(true);
            Object tenantAccountValue = tenantAccount.get(tenant);
            String principal = tenantId + "," + tenantAccountValue + "," + authData.getUsername();
            log.info(" 构建 principal: {}", principal);
//            Class<?> UsernamePasswordClass = classLoader.loadClass("org.springframework.security.authentication.UsernamePasswordAuthenticationToken");
            Class<?> authenticationProvideClass = classLoader.loadClass("com.ctsi.ssdc.security.ThirdAuthToken"); //三方登录不需要密码
            classLoader.loadClass("java.lang.String").newInstance();
            Object authenticationToken = authenticationProvideClass
                    .getConstructor(String.class, String.class)
                    .newInstance(principal, null);
            Object authenticationManager = getBeanMethod.invoke(ctx, "authenticationManager");
            Object authenticate;

            Class<?> authenticationClass = classLoader.loadClass("org.springframework.security.core.Authentication");
            try {
                authenticate = authenticationManager.getClass().getMethod("authenticate", authenticationClass)
                        .invoke(authenticationManager, authenticationToken);
            } catch (InvocationTargetException e) {
                Throwable target = e.getTargetException();
                log.error("Target exception:", target);
                throw target;
            }


            log.info(" 认证结果: {}", authenticate);

            Object tokenHxProvider = getBeanMethod.invoke(ctx, "tokenHxProvider");
            Method createToken = tokenHxProvider.getClass().getMethod("createToken", authenticationClass, boolean.class);
            log.info(" 获取 TokenHxProvider: {}", tokenHxProvider);
            Object token = createToken.invoke(tokenHxProvider, authenticate, false);
            String fullToken = "Bearer " + token;
            log.info(" 获取到 Token: {}", fullToken);
            RedisCommands<String, String> redis = LettuceHolder.getCommands();
            redis.set(ConfigUtils.getRedisKey(), fullToken);

            ScheduledExecutorUtils.scheduledExecutorService.schedule(()->{
                try{
                    CtsiEnhance.enhanceCtsiApp(ctx, log);
                }catch (Exception e){
                    log.info(" 定时刷新 Token 失败", e);
                }
            },ConfigUtils.getRefushTokenTime(), TimeUnit.SECONDS);
        } catch (Throwable e) {
            log.error(" 获取 Token 失败", e);
        }
    }
}
