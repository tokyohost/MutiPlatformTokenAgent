package com.auth.agent.domain;

import lombok.Data;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/5 9:43
 * @Content
 */

@Data
public class AuthData {
    private String username;
    private String password;

    /**
     * 比翼默认租户 default
     */
    private String tenantAccount;

    /**
     * 若以plus 方式接入，则需要传入clientId
     */
    private String clientId;

    /**
     * 授权类型，默认 password
     */
    private String grantType;

    /**
     * 若以PLUS 方式接入，则需要传入tenantId
     * 未开启租户使用默认 000000
     */
    private String tenantId;

    /**
     * 若以plus token 失效时间 秒
     */
    private Integer timeout;
}
