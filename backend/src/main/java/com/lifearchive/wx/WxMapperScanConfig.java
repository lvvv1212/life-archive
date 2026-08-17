package com.lifearchive.wx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 微信模块 Mapper 扫描配置。
 *
 * 现有 MyBatisPlusConfig 仅扫描 com.lifearchive.mapper，本配置在不修改
 * 任何现有文件的前提下，将 com.lifearchive.wx.mapper 纳入扫描，使
 * WxAccountMapper 能被注册为 Spring Bean。两个 @MapperScan 包路径互不重叠，
 * 互不影响。
 */
@Configuration
@MapperScan("com.lifearchive.wx.mapper")
public class WxMapperScanConfig {
}
