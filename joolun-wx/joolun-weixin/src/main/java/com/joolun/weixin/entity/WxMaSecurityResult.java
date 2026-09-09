/**
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 */
package com.joolun.weixin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小程序异步内容安全检测结果。
 */
@Data
@TableName("wx_ma_security_result")
public class WxMaSecurityResult {

	@TableId(type = IdType.INPUT)
	private String traceId;
	private String statusCode;
	private String suggest;
	private String label;
	private String detailJson;
	private LocalDateTime receivedTime;
}
