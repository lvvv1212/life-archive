package com.lifearchive.wx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微信账号映射表（独立新表，不修改现有 user 表）。
 * 阶段0仅建立骨架；阶段1由 WxAuthService 填充逻辑。
 */
@Data
@TableName("wx_account")
public class WxAccount implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String openid;

    private String unionid;

    private String sessionKey;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
