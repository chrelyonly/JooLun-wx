/**
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 */
package com.joolun.web.controller.weixin;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaMessage;
import cn.binarywang.wx.miniapp.message.WxMaOutMessage;
import cn.binarywang.wx.miniapp.util.crypt.WxMaCryptUtils;
import com.joolun.weixin.config.WxMaConfiguration;
import com.joolun.weixin.config.WxRuntimeConfigService;
import com.joolun.weixin.service.WxMaSecurityResultStore;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 单小程序消息推送入口，同时接收异步内容安全检测结果。
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/weixin/portal/api/ma")
public class WxMaPortalController {

	private final WxRuntimeConfigService wxRuntimeConfigService;
	private final WxMaSecurityResultStore securityResultStore;

	@GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
	public String verify(@RequestParam String signature, @RequestParam String timestamp,
			@RequestParam String nonce, @RequestParam String echostr) {
		return service().checkSignature(timestamp, nonce, signature) ? echostr : "非法请求";
	}

	@PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
	public String receive(@RequestBody String requestBody,
			@RequestParam String timestamp,
			@RequestParam String nonce,
			@RequestParam(required = false) String signature,
			@RequestParam(name = "encrypt_type", required = false) String encryptType,
			@RequestParam(name = "msg_signature", required = false) String msgSignature) {
		WxMaService service = service();
		WxMaMessage message;
		if ("aes".equalsIgnoreCase(encryptType)) {
			if (StringUtils.isBlank(msgSignature)) {
				throw new IllegalArgumentException("缺少小程序加密消息签名");
			}
			WxMaMessage encrypted = parseMessage(requestBody);
			String plainText = new WxMaCryptUtils(service.getWxMaConfig())
					.decryptContent(msgSignature, timestamp, nonce, encrypted.getEncrypt());
			message = parseMessage(plainText);
		} else {
			if (StringUtils.isBlank(signature) || !service.checkSignature(timestamp, nonce, signature)) {
				throw new IllegalArgumentException("小程序消息签名校验失败");
			}
			message = parseMessage(requestBody);
		}

		if (StringUtils.isNotBlank(message.getTraceId())) {
			securityResultStore.put(message);
			log.info("收到小程序内容安全检测结果，traceId={}，statusCode={}",
					message.getTraceId(), message.getStatusCode());
			return "success";
		}

		WxMaOutMessage outMessage = WxMaConfiguration.getRouter(appId()).route(message);
		if (outMessage == null) {
			return "success";
		}
		if ("aes".equalsIgnoreCase(encryptType)) {
			return "XML".equalsIgnoreCase(msgDataFormat())
					? outMessage.toEncryptedXml(service.getWxMaConfig())
					: outMessage.toEncryptedJson(service.getWxMaConfig());
		}
		return "XML".equalsIgnoreCase(msgDataFormat()) ? outMessage.toXml() : outMessage.toJson();
	}

	private WxMaMessage parseMessage(String body) {
		return body != null && body.stripLeading().startsWith("<")
				? WxMaMessage.fromXml(body) : WxMaMessage.fromJson(body);
	}

	private WxMaService service() {
		return WxMaConfiguration.getMaService(appId());
	}

	private String appId() {
		return wxRuntimeConfigService.getMaConfigs().get(0).getAppId();
	}

	private String msgDataFormat() {
		return wxRuntimeConfigService.getMaConfigs().get(0).getMsgDataFormat();
	}
}
