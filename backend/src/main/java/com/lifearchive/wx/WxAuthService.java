package com.lifearchive.wx;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifearchive.common.JwtUtils;
import com.lifearchive.entity.User;
import com.lifearchive.mapper.UserMapper;
import com.lifearchive.wx.entity.WxAccount;
import com.lifearchive.wx.mapper.WxAccountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 微信登录业务：code2Session -> 查/建 user -> 关联 wx_account -> 签发 JWT。
 * 直接 HTTP 调用微信接口，不引入 WxJava SDK（零侵入，不改 pom.xml）。
 */
@Service
public class WxAuthService {

    private final WxProperties wxProperties;
    private final WxAccountMapper wxAccountMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public WxAuthService(WxProperties wxProperties,
                         WxAccountMapper wxAccountMapper,
                         UserMapper userMapper,
                         JwtUtils jwtUtils) {
        this.wxProperties = wxProperties;
        this.wxAccountMapper = wxAccountMapper;
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 微信登录主流程。
     *
     * @param code wx.login() 换取的一次性 code
     * @return 含 JWT token 与用户信息的登录结果
     */
    public WxLoginResult login(String code) {
        if (code == null || code.isBlank()) {
            throw new RuntimeException("缺少微信登录 code");
        }

        WxSession session = fetchWxSession(code);
        if (session.getOpenid() == null || session.getOpenid().isBlank()) {
            throw new RuntimeException("微信登录失败：未获取到 openid");
        }

        // 1. 按 openid 查找是否已绑定账号
        LambdaQueryWrapper<WxAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WxAccount::getOpenid, session.getOpenid());
        WxAccount account = wxAccountMapper.selectOne(wrapper);

        Long userId;
        if (account == null) {
            // 2. 新用户：创建 user + wx_account 绑定
            User user = new User();
            user.setUsername("wx_" + session.getOpenid());
            // 微信用户无密码登录，写入随机密码哈希作为占位
            user.setPassword(DigestUtils.md5DigestAsHex(
                    UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
            user.setNickname("微信用户");
            userMapper.insert(user);
            userId = user.getId();

            account = new WxAccount();
            account.setUserId(userId);
            account.setOpenid(session.getOpenid());
            account.setUnionid(session.getUnionid());
            account.setSessionKey(session.getSessionKey());
            account.setCreatedAt(LocalDateTime.now());
            account.setUpdatedAt(LocalDateTime.now());
            wxAccountMapper.insert(account);
        } else {
            // 3. 老用户：刷新 session_key
            userId = account.getUserId();
            account.setSessionKey(session.getSessionKey());
            account.setUpdatedAt(LocalDateTime.now());
            wxAccountMapper.updateById(account);
        }

        // 4. 签发 JWT（复用现有 JwtUtils）
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        return new WxLoginResult(token, user);
    }

    /**
     * 调用微信 jscode2session 接口。
     * 未配置 appid/secret 时走开发态 mock（本地联调无需真实小程序账号）。
     */
    private WxSession fetchWxSession(String code) {
        String appId = wxProperties.getAppId();
        String secret = wxProperties.getSecret();
        if (appId == null || appId.isBlank() || secret == null || secret.isBlank()) {
            WxSession mock = new WxSession();
            mock.setOpenid("mock_" + code);
            mock.setSessionKey("mock_session_" + code);
            return mock;
        }

        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + appId
                + "&secret=" + secret
                + "&js_code=" + code
                + "&grant_type=authorization_code";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            WxSession session = objectMapper.readValue(response.body(), WxSession.class);
            if (session.getErrcode() != null && session.getErrcode() != 0) {
                throw new RuntimeException("微信 code2session 失败: " + session.getErrmsg());
            }
            return session;
        } catch (java.io.IOException | InterruptedException e) {
            throw new RuntimeException("调用微信接口异常: " + e.getMessage(), e);
        }
    }

    // ---------- 内部 DTO ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class WxSession {
        private String openid;
        @JsonProperty("session_key")
        private String sessionKey;
        private String unionid;
        private Integer errcode;
        private String errmsg;

        public String getOpenid() { return openid; }
        public void setOpenid(String openid) { this.openid = openid; }
        public String getSessionKey() { return sessionKey; }
        public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
        public String getUnionid() { return unionid; }
        public void setUnionid(String unionid) { this.unionid = unionid; }
        public Integer getErrcode() { return errcode; }
        public void setErrcode(Integer errcode) { this.errcode = errcode; }
        public String getErrmsg() { return errmsg; }
        public void setErrmsg(String errmsg) { this.errmsg = errmsg; }
    }

    static class WxLoginResult {
        private final String token;
        private final User user;

        WxLoginResult(String token, User user) {
            this.token = token;
            this.user = user;
        }

        public String getToken() { return token; }
        public User getUser() { return user; }
    }
}
