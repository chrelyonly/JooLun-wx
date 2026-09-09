/**
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 * 注意：
 * 本软件为www.joolun.com开发研制，项目使用请保留此说明
 */
package com.joolun.mall.config;

import com.joolun.common.utils.StringUtils;
import com.joolun.mall.constant.MallApplicationConfigKeyConstants;
import com.joolun.system.service.ISysConfigService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 商城运行配置服务。
 * 系统参数优先，application.yml 作为默认值。
 *
 * @author www.joolun.com
 */
@Service
@AllArgsConstructor
public class MallRuntimeConfigService {

	private final ISysConfigService sysConfigService;
	private final MallConfigProperties mallConfigProperties;

	public String getNotifyHost() {
		String value = sysConfigService.selectConfigByKey(MallApplicationConfigKeyConstants.NOTIFY_HOST);
		return StringUtils.isNotEmpty(value) ? value : mallConfigProperties.getNotifyHost();
	}
}
