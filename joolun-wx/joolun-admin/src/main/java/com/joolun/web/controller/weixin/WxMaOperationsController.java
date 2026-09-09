/**
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 * 注意：
 * 本软件为www.joolun.com开发研制，项目使用请保留此说明
 */
package com.joolun.web.controller.weixin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.binarywang.wx.miniapp.api.WxMaAnalysisService;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import cn.binarywang.wx.miniapp.bean.security.WxMaMediaSecCheckCheckRequest;
import cn.binarywang.wx.miniapp.bean.security.WxMaMsgSecCheckCheckRequest;
import cn.binarywang.wx.miniapp.bean.shop.request.shipping.WxMaOrderShippingInfoGetListRequest;
import cn.binarywang.wx.miniapp.bean.shop.request.shipping.WxMaOrderShippingInfoGetRequest;
import cn.binarywang.wx.miniapp.bean.shop.request.shipping.WxMaOrderShippingInfoNotifyConfirmRequest;
import cn.binarywang.wx.miniapp.bean.shop.request.shipping.WxMaOrderShippingInfoUploadRequest;
import com.joolun.common.core.controller.BaseController;
import com.joolun.common.core.domain.AjaxResult;
import com.joolun.weixin.config.WxMaConfiguration;
import com.joolun.weixin.config.WxRuntimeConfigService;
import com.joolun.weixin.entity.WxMaSecurityResult;
import com.joolun.weixin.entity.WxUser;
import com.joolun.weixin.service.WxMaSecurityResultStore;
import com.joolun.weixin.service.WxUserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * 单小程序运营能力：订阅消息、小程序码、内容安全、数据分析和发货信息管理。
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/wxma/operations")
public class WxMaOperationsController extends BaseController {

	private final WxRuntimeConfigService wxRuntimeConfigService;
	private final WxMaSecurityResultStore securityResultStore;
	private final WxUserService wxUserService;

	@GetMapping("/users")
	@PreAuthorize("@ss.hasPermi('wxma:operations:query')")
	public AjaxResult userOptions(@RequestParam(defaultValue = "") String keyword) {
		String search = keyword.trim();
		List<WxUserOption> users = wxUserService.list(Wrappers.<WxUser>lambdaQuery()
				.select(WxUser::getOpenId, WxUser::getNickName, WxUser::getHeadimgUrl)
				.eq(WxUser::getAppType, "1")
				.isNotNull(WxUser::getOpenId)
				.ne(WxUser::getOpenId, "")
				.and(!search.isEmpty(), query -> query.like(WxUser::getNickName, search)
						.or().like(WxUser::getOpenId, search))
				.orderByDesc(WxUser::getUpdateTime)
				.last("LIMIT 50"))
				.stream()
				.map(user -> new WxUserOption(user.getOpenId(), user.getNickName(), user.getHeadimgUrl()))
				.toList();
		return success(users);
	}

	@GetMapping("/templates/categories")
	@PreAuthorize("@ss.hasPermi('wxma:operations:query')")
	public AjaxResult templateCategories() {
		return invoke(() -> service().getSubscribeService().getCategory());
	}

	@GetMapping("/templates/public")
	@PreAuthorize("@ss.hasPermi('wxma:operations:query')")
	public AjaxResult publicTemplates(@RequestParam String ids,
			@RequestParam(defaultValue = "0") int start,
			@RequestParam(defaultValue = "30") int limit) {
		return invoke(() -> service().getSubscribeService()
				.getPubTemplateTitleList(ids.split(","), start, limit));
	}

	@GetMapping("/templates/public/{templateTitleId}/keywords")
	@PreAuthorize("@ss.hasPermi('wxma:operations:query')")
	public AjaxResult publicTemplateKeywords(@PathVariable String templateTitleId) {
		return invoke(() -> service().getSubscribeService().getPubTemplateKeyWordsById(templateTitleId));
	}

	@GetMapping("/templates")
	@PreAuthorize("@ss.hasPermi('wxma:operations:query')")
	public AjaxResult templates() {
		return invoke(() -> service().getSubscribeService().getTemplateList());
	}

	@PostMapping("/templates")
	@PreAuthorize("@ss.hasPermi('wxma:operations:edit')")
	public AjaxResult addTemplate(@RequestBody TemplateAddRequest request) {
		return invoke(() -> service().getSubscribeService()
				.addTemplate(request.templateTitleId(), request.keywordIds(), request.sceneDesc()));
	}

	@DeleteMapping("/templates/{templateId}")
	@PreAuthorize("@ss.hasPermi('wxma:operations:edit')")
	public AjaxResult deleteTemplate(@PathVariable String templateId) {
		return invoke(() -> service().getSubscribeService().delTemplate(templateId));
	}

	@PostMapping("/templates/send")
	@PreAuthorize("@ss.hasPermi('wxma:operations:send')")
	public AjaxResult sendSubscribeMessage(@RequestBody WxMaSubscribeMessage message) {
		return invoke(() -> {
			service().getSubscribeService().sendSubscribeMsg(message);
			return Boolean.TRUE;
		});
	}

	@PostMapping(value = "/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
	@PreAuthorize("@ss.hasPermi('wxma:operations:edit')")
	public ResponseEntity<byte[]> createQrcode(@RequestBody QrcodeRequest request) throws WxErrorException {
		int width = request.width() == null ? 430 : request.width();
		String envVersion = request.envVersion() == null ? "release" : request.envVersion();
		boolean checkPath = request.checkPath() == null || request.checkPath();
		boolean hyaline = Boolean.TRUE.equals(request.hyaline());
		byte[] image = switch (request.type() == null ? "UNLIMIT" : request.type().toUpperCase()) {
			case "QR" -> service().getQrcodeService().createQrcodeBytes(request.path(), width);
			case "WXA" -> service().getQrcodeService()
					.createWxaCodeBytes(request.path(), envVersion, width, true, null, hyaline);
			case "UNLIMIT" -> service().getQrcodeService()
					.createWxaCodeUnlimitBytes(request.scene(), request.path(), checkPath, envVersion, width, true, null, hyaline);
			default -> throw new IllegalArgumentException("不支持的小程序码类型：" + request.type());
		};
		return ResponseEntity.ok()
				.cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
				.contentType(MediaType.IMAGE_PNG)
				.body(image);
	}

	@PostMapping("/security/text")
	@PreAuthorize("@ss.hasPermi('wxma:operations:edit')")
	public AjaxResult checkText(@RequestBody SecurityTextRequest request) {
		return invoke(() -> service().getSecurityService().checkMessage(
				WxMaMsgSecCheckCheckRequest.builder()
						.version("2")
						.openid(request.openId())
						.scene(request.scene() == null ? 2 : request.scene())
						.content(request.content())
						.nickname(request.nickname())
						.title(request.title())
						.signature(request.signature())
						.build()));
	}

	@PostMapping("/security/media")
	@PreAuthorize("@ss.hasPermi('wxma:operations:edit')")
	public AjaxResult checkMedia(@RequestBody SecurityMediaRequest request) {
		return invoke(() -> service().getSecurityService()
				.mediaCheckAsync(WxMaMediaSecCheckCheckRequest.builder()
						.mediaUrl(request.mediaUrl())
						.mediaType(request.mediaType() == null ? 2 : request.mediaType())
						.version(2)
						.openid(request.openId())
						.scene(request.scene() == null ? 2 : request.scene())
						.build()));
	}

	@GetMapping("/security/media-results")
	@PreAuthorize("@ss.hasPermi('wxma:operations:query')")
	public AjaxResult recentMediaResults(@RequestParam(defaultValue = "20") int limit) {
		return success(securityResultStore.recent(limit));
	}

	@GetMapping("/security/media-results/{traceId}")
	@PreAuthorize("@ss.hasPermi('wxma:operations:query')")
	public AjaxResult mediaResult(@PathVariable String traceId) {
		WxMaSecurityResult result = securityResultStore.get(traceId);
		return result == null ? error("尚未收到该 traceId 的检测结果") : success(result);
	}

	@GetMapping("/analytics")
	@PreAuthorize("@ss.hasPermi('wxma:operations:query')")
	public AjaxResult analytics(@RequestParam String metric,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
		return invoke(() -> queryAnalytics(metric, startDate, endDate));
	}

	@GetMapping("/shipping/enabled")
	@PreAuthorize("@ss.hasPermi('wxma:operations:query')")
	public AjaxResult shippingEnabled() {
		return invoke(() -> service().getWxMaOrderShippingService().isTradeManaged(appId()));
	}

	@PostMapping("/shipping/upload")
	@PreAuthorize("@ss.hasPermi('wxma:operations:edit')")
	public AjaxResult uploadShipping(@RequestBody WxMaOrderShippingInfoUploadRequest request) {
		return invoke(() -> service().getWxMaOrderShippingService().upload(request));
	}

	@PostMapping("/shipping/query")
	@PreAuthorize("@ss.hasPermi('wxma:operations:query')")
	public AjaxResult queryShipping(@RequestBody WxMaOrderShippingInfoGetRequest request) {
		return invoke(() -> service().getWxMaOrderShippingService().get(request));
	}

	@PostMapping("/shipping/list")
	@PreAuthorize("@ss.hasPermi('wxma:operations:query')")
	public AjaxResult shippingList(@RequestBody WxMaOrderShippingInfoGetListRequest request) {
		return invoke(() -> service().getWxMaOrderShippingService().getList(request));
	}

	@PostMapping("/shipping/notify-confirm")
	@PreAuthorize("@ss.hasPermi('wxma:operations:edit')")
	public AjaxResult notifyConfirm(@RequestBody WxMaOrderShippingInfoNotifyConfirmRequest request) {
		return invoke(() -> service().getWxMaOrderShippingService().notifyConfirmReceive(request));
	}

	@PutMapping("/shipping/jump-path")
	@PreAuthorize("@ss.hasPermi('wxma:operations:edit')")
	public AjaxResult setShippingJumpPath(@RequestBody JumpPathRequest request) {
		return invoke(() -> service().getWxMaOrderShippingService().setMsgJumpPath(request.path()));
	}

	private Object queryAnalytics(String metric, Date startDate, Date endDate) throws WxErrorException {
		validateAnalyticsRange(metric, startDate, endDate);
		WxMaAnalysisService analysis = service().getAnalysisService();
		return switch (metric) {
			case "dailySummary" -> analysis.getDailySummaryTrend(startDate, endDate);
			case "dailyVisit" -> analysis.getDailyVisitTrend(startDate, endDate);
			case "weeklyVisit" -> analysis.getWeeklyVisitTrend(startDate, endDate);
			case "monthlyVisit" -> analysis.getMonthlyVisitTrend(startDate, endDate);
			case "visitDistribution" -> analysis.getVisitDistribution(startDate, endDate);
			case "dailyRetain" -> analysis.getDailyRetainInfo(startDate, endDate);
			case "weeklyRetain" -> analysis.getWeeklyRetainInfo(startDate, endDate);
			case "monthlyRetain" -> analysis.getMonthlyRetainInfo(startDate, endDate);
			case "visitPage" -> analysis.getVisitPage(startDate, endDate);
			case "userPortrait" -> analysis.getUserPortrait(startDate, endDate);
			default -> throw new IllegalArgumentException("不支持的数据指标：" + metric);
		};
	}

	private void validateAnalyticsRange(String metric, Date startDate, Date endDate) {
		ZoneId zoneId = ZoneId.systemDefault();
		LocalDate start = startDate.toInstant().atZone(zoneId).toLocalDate();
		LocalDate end = endDate.toInstant().atZone(zoneId).toLocalDate();
		if (start.isAfter(end)) {
			throw new IllegalArgumentException("开始日期不能晚于结束日期");
		}
		if (end.isAfter(LocalDate.now(zoneId).minusDays(1))) {
			throw new IllegalArgumentException("数据分析最晚只能查询到昨天");
		}

		switch (metric) {
			case "dailySummary", "dailyVisit", "visitDistribution", "dailyRetain", "visitPage" -> {
				if (!start.equals(end)) {
					throw new IllegalArgumentException("当前指标仅支持查询单日数据，开始日期和结束日期必须相同");
				}
			}
			case "weeklyVisit", "weeklyRetain" -> {
				if (start.getDayOfWeek() != DayOfWeek.MONDAY || end.getDayOfWeek() != DayOfWeek.SUNDAY
						|| ChronoUnit.DAYS.between(start, end) != 6) {
					throw new IllegalArgumentException("周指标必须查询一个完整自然周（周一至周日）");
				}
			}
			case "monthlyVisit", "monthlyRetain" -> {
				if (start.getDayOfMonth() != 1 || !end.equals(start.withDayOfMonth(start.lengthOfMonth()))) {
					throw new IllegalArgumentException("月指标必须查询一个完整自然月");
				}
			}
			case "userPortrait" -> {
				long days = ChronoUnit.DAYS.between(start, end);
				if (days != 0 && days != 6 && days != 29) {
					throw new IllegalArgumentException("用户画像只支持查询单日、最近 7 天或最近 30 天");
				}
			}
			default -> throw new IllegalArgumentException("不支持的数据指标：" + metric);
		}
	}

	private WxMaService service() {
		return WxMaConfiguration.getMaService(appId());
	}

	private String appId() {
		return wxRuntimeConfigService.getMaConfigs().get(0).getAppId();
	}

	private AjaxResult invoke(WxSupplier supplier) {
		try {
			return success(supplier.get());
		} catch (Exception e) {
			log.error("调用微信小程序接口失败", e);
			if (e instanceof WxErrorException wxError && wxError.getError() != null) {
				return error(wxError.getError().getErrorMsg());
			}
			return error(e.getMessage());
		}
	}

	@FunctionalInterface
	private interface WxSupplier {
		Object get() throws Exception;
	}

	public record TemplateAddRequest(String templateTitleId, List<Integer> keywordIds, String sceneDesc) {
	}

	public record WxUserOption(String openId, String nickName, String headimgUrl) {
	}

	public record QrcodeRequest(String type, String path, String scene, Integer width, String envVersion,
			Boolean checkPath, Boolean hyaline) {
	}

	public record SecurityTextRequest(String openId, Integer scene, String content, String nickname,
			String title, String signature) {
	}

	public record SecurityMediaRequest(String mediaUrl, Integer mediaType, String openId, Integer scene) {
	}

	public record JumpPathRequest(String path) {
	}
}
