/**
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 * 注意：
 * 本软件为www.joolun.com开发研制，项目使用请保留此说明
 */
package com.joolun.web.controller.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商城 application.yml 配置。
 *
 * @author www.joolun.com
 */
@Data
public class MallApplicationConfigDTO {

	@NotBlank(message = "商城回调域名不能为空")
	@Size(max = 500, message = "商城回调域名不能超过500个字符")
	@Pattern(regexp = "^https?://\\S+$", message = "商城回调域名必须是以 http:// 或 https:// 开头的地址")
	private String notifyHost;
}
