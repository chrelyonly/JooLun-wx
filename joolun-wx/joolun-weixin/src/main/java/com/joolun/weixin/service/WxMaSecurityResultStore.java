/**
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 */
package com.joolun.weixin.service;

import cn.binarywang.wx.miniapp.bean.WxMaMessage;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joolun.weixin.entity.WxMaSecurityResult;
import com.joolun.weixin.mapper.WxMaSecurityResultMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 保存最近的小程序异步内容安全结果，便于后台按 traceId 查询。
 */
@Service
@AllArgsConstructor
public class WxMaSecurityResultStore {

	private final WxMaSecurityResultMapper resultMapper;

	public synchronized void put(WxMaMessage message) {
		if (message == null || message.getTraceId() == null) {
			return;
		}
		WxMaSecurityResult result = new WxMaSecurityResult();
		result.setTraceId(message.getTraceId());
		result.setStatusCode(message.getStatusCode());
		if (message.getResult() != null) {
			result.setSuggest(message.getResult().getSuggest());
			result.setLabel(message.getResult().getLabel());
		}
		result.setDetailJson(JSONUtil.toJsonStr(message.getDetail()));
		result.setReceivedTime(LocalDateTime.now());
		if (resultMapper.updateById(result) == 0) {
			resultMapper.insert(result);
		}
	}

	public WxMaSecurityResult get(String traceId) {
		return resultMapper.selectById(traceId);
	}

	public List<WxMaSecurityResult> recent(int limit) {
		int safeLimit = Math.max(1, Math.min(limit, 100));
		return resultMapper.selectList(Wrappers.<WxMaSecurityResult>lambdaQuery()
				.orderByDesc(WxMaSecurityResult::getReceivedTime)
				.last("LIMIT " + safeLimit));
	}
}
