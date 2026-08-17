package com.lifearchive.wx;

import com.lifearchive.common.Result;
import com.lifearchive.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序登录接口。
 *
 * 路径 /api/user/login/wechat 故意落在 JwtInterceptor 已排除的前缀
 * /api/user/login 之下，从而在【不修改任何现有文件】的前提下被放行。
 * 若后续允许改动拦截器，可将其改为更规范的 /api/wx/login 并同步补充白名单。
 */
@RestController
@RequestMapping("/api/user/login")
public class WxLoginController {

    @Autowired
    private WxAuthService wxAuthService;

    /**
     * 微信小程序登录：传入 wx.login() 得到的 code。
     * 请求体: {"code": "..."}
     * 返回:   {"code":200,"message":"登录成功","data":{"token":"...","user":{...}}}
     */
    @PostMapping("/wechat")
    public Result<Map<String, Object>> wechatLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        WxAuthService.WxLoginResult result = wxAuthService.login(code);

        Map<String, Object> data = new HashMap<>();
        data.put("token", result.getToken());
        data.put("user", result.getUser());
        return Result.success("登录成功", data);
    }
}
