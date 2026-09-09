/**
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 * 注意：
 * 本软件为www.joolun.com开发研制，项目使用请保留此说明
 */
package com.joolun.mall.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.shop.request.shipping.ContactBean;
import cn.binarywang.wx.miniapp.bean.shop.request.shipping.OrderKeyBean;
import cn.binarywang.wx.miniapp.bean.shop.request.shipping.PayerBean;
import cn.binarywang.wx.miniapp.bean.shop.request.shipping.ShippingListBean;
import cn.binarywang.wx.miniapp.bean.shop.request.shipping.WxMaOrderShippingInfoUploadRequest;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joolun.mall.config.CommonConstants;
import com.joolun.mall.entity.MallUser;
import com.joolun.mall.entity.OrderInfo;
import com.joolun.mall.entity.OrderItem;
import com.joolun.mall.entity.OrderLogistics;
import com.joolun.weixin.config.WxMaConfiguration;
import com.joolun.weixin.config.WxMaProperties;
import com.joolun.weixin.config.WxRuntimeConfigService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商城发货信息同步到微信小程序交易管理服务。
 */
@Slf4j
@Service
@AllArgsConstructor
public class WxOrderShippingSyncService {
	private static final Map<String, String> WECHAT_EXPRESS_CODES = Map.ofEntries(
			Map.entry("tiantian", "TTKDEX"),
			Map.entry("huitongkuaidi", "BEST"),
			Map.entry("yunda", "YUNDA"),
			Map.entry("yuantong", "YTO"),
			Map.entry("debangwuliu", "DBL"),
			Map.entry("ems", "EMS"),
			Map.entry("shunfeng", "SF"),
			Map.entry("zhongtong", "ZTO"),
			Map.entry("shentong", "STO")
	);

	private final WxRuntimeConfigService wxRuntimeConfigService;
	private final MallUserService mallUserService;
	private final OrderItemService orderItemService;

	public SyncResult sync(OrderInfo orderInfo, OrderLogistics logistics) {
		if (orderInfo == null || logistics == null) {
			return SyncResult.skipped("订单或物流信息不存在");
		}
		if (!CommonConstants.YES.equals(orderInfo.getIsPay())) {
			return SyncResult.skipped("未支付订单无需同步微信发货信息");
		}
		if (StrUtil.isBlank(logistics.getLogistics()) || StrUtil.isBlank(logistics.getLogisticsNo())) {
			return SyncResult.skipped("物流公司或物流单号为空");
		}
		MallUser mallUser = mallUserService.getById(orderInfo.getUserId());
		if (mallUser == null || StrUtil.isBlank(mallUser.getOpenId())) {
			return SyncResult.skipped("订单用户没有小程序 OpenID");
		}

		try {
			WxMaProperties.Config config = wxRuntimeConfigService.getMaConfigs().get(0);
			WxMaService wxMaService = WxMaConfiguration.getMaService(config.getAppId());
			OrderKeyBean orderKey = new OrderKeyBean();
			if (StrUtil.isNotBlank(orderInfo.getTransactionId())) {
				orderKey.setOrderNumberType(2);
				orderKey.setTransactionId(orderInfo.getTransactionId());
			} else {
				orderKey.setOrderNumberType(1);
				orderKey.setMchId(config.getMchId());
				orderKey.setOutTradeNo(orderInfo.getOrderNo());
			}

			ShippingListBean shipping = new ShippingListBean();
			shipping.setExpressCompany(resolveWechatExpressCode(logistics.getLogistics()));
			shipping.setTrackingNo(logistics.getLogisticsNo());
			shipping.setItemDesc(buildItemDescription(orderInfo.getId()));
			if (StrUtil.isNotBlank(logistics.getTelNum())) {
				ContactBean contact = new ContactBean();
				contact.setReceiverContact(StrUtil.trim(logistics.getTelNum()));
				shipping.setContact(contact);
			}

			WxMaOrderShippingInfoUploadRequest request = new WxMaOrderShippingInfoUploadRequest();
			request.setOrderKey(orderKey);
			request.setLogisticsType(1);
			request.setDeliveryMode(1);
			request.setIsAllDelivered(Boolean.TRUE);
			request.setShippingList(List.of(shipping));
			request.setUploadTime(OffsetDateTime.now(ZoneId.of("Asia/Shanghai"))
					.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
			request.setPayer(PayerBean.builder().openid(mallUser.getOpenId()).build());

			var response = wxMaService.getWxMaOrderShippingService().upload(request);
			if (response.getErrCode() != null && response.getErrCode() != 0) {
				return SyncResult.failed(response.getErrMsg());
			}
			return SyncResult.succeeded();
		} catch (Exception e) {
			log.error("同步订单 {} 的微信发货信息失败", orderInfo.getOrderNo(), e);
			return SyncResult.failed(e.getMessage());
		}
	}

	private String buildItemDescription(String orderId) {
		String description = orderItemService.list(Wrappers.<OrderItem>lambdaQuery()
				.eq(OrderItem::getOrderId, orderId)).stream()
				.map(item -> StrUtil.blankToDefault(item.getSpuName(), "商品") + "*" + item.getQuantity())
				.collect(Collectors.joining("，"));
		return StrUtil.maxLength(StrUtil.blankToDefault(description, "商城商品"), 120);
	}

	private String resolveWechatExpressCode(String logisticsCode) {
		return WECHAT_EXPRESS_CODES.getOrDefault(StrUtil.trim(logisticsCode).toLowerCase(), logisticsCode);
	}

	public record SyncResult(boolean success, boolean skipped, String message) {
		public static SyncResult succeeded() {
			return new SyncResult(true, false, "微信发货信息同步成功");
		}

		public static SyncResult skipped(String message) {
			return new SyncResult(false, true, message);
		}

		public static SyncResult failed(String message) {
			return new SyncResult(false, false, StrUtil.blankToDefault(message, "微信发货信息同步失败"));
		}
	}
}
