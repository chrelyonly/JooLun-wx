/**
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 * 注意：
 * 本软件为www.joolun.com开发研制，项目使用请保留此说明
 */
package com.joolun.weixin.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * @author www.joolun.com
 */
@Data
@ConfigurationProperties(prefix = "wx.ma")
public class WxMaProperties {

    private List<Config> configs;

    @Data
    public static class Config {
        /**
         * 设置微信小程序的appid
         */
        private String appId;

        /**
         * 设置微信小程序的Secret
         */
        private String secret;

        /**
         * 设置微信小程序消息服务器配置的token
         */
        private String token;

        /**
         * 设置微信小程序消息服务器配置的EncodingAESKey
         */
        private String aesKey;

        /**
         * 消息格式，XML或者JSON
         */
        private String msgDataFormat;
        /**
         * 微信支付商户号
         */
        private String mchId;

        /**
         * 微信支付 API v3 密钥。
         */
        private String apiV3Key;

        /**
         * 商户 API PKCS#12 证书路径，证书密码为商户号。
         */
        private String pkcs12Path;

        /**
         * 微信支付公钥 ID（公钥模式）。
         */
        private String publicKeyId;

        /**
         * 微信支付公钥 pub_key.pem 路径（公钥模式）。
         */
        private String publicKeyPath;
    }

}
