package com.lifearchive.wx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置（从环境变量注入，不修改 application.yml）。
 * 通过 docker-compose.override.yml + .env 注入 WX_APPID / WX_SECRET。
 */
@Component
public class WxProperties {

    @Value("${WX_APPID:}")
    private String appId;

    @Value("${WX_SECRET:}")
    private String secret;

    public String getAppId() {
        return appId;
    }

    public String getSecret() {
        return secret;
    }
}
