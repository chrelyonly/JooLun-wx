/*
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 */

-- 3.9.0 升级 3.9.1：单公众号、单小程序官方能力接入。
-- 每项业务能力使用独立菜单和页面；默认授权给超级管理员角色（role_id=2）。

CREATE TABLE IF NOT EXISTS `wx_ma_security_result` (
  `trace_id` varchar(64) NOT NULL COMMENT '微信异步检测任务ID',
  `status_code` varchar(32) DEFAULT NULL COMMENT '检测状态码',
  `suggest` varchar(32) DEFAULT NULL COMMENT '综合建议：pass/review/risky',
  `label` varchar(32) DEFAULT NULL COMMENT '风险标签',
  `detail_json` text COMMENT '详细检测结果JSON',
  `received_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结果接收时间',
  PRIMARY KEY (`trace_id`),
  KEY `idx_wx_ma_security_received_time` (`received_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序异步内容安全检测结果';

-- 公众号独立功能页面。
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2077, '模板消息', 4, 80, 'template-message', 'wxmp/template-message/index', NULL, 'WxMpTemplateMessage', 1, 0, 'C', '0', '0', 'wxmp:operations:query', 'message', 'admin', NOW(), '', NULL, '公众号模板消息管理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2077);

UPDATE `sys_menu` SET `menu_name` = '模板消息', `parent_id` = 4, `order_num` = 80,
  `path` = 'template-message', `component` = 'wxmp/template-message/index', `route_name` = 'WxMpTemplateMessage',
  `menu_type` = 'C', `perms` = 'wxmp:operations:query', `icon` = 'message', `remark` = '公众号模板消息管理'
WHERE `menu_id` = 2077;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(2085, '参数二维码', 4, 90, 'qrcode', 'wxmp/qrcode/index', NULL, 'WxMpQrcode', 1, 0, 'C', '0', '0', 'wxmp:operations:edit', 'code', 'admin', NOW(), '', NULL, '公众号参数二维码'),
(2086, '运营数据分析', 4, 100, 'data-analysis', 'wxmp/data-analysis/index', NULL, 'WxMpDataAnalysis', 1, 0, 'C', '0', '0', 'wxmp:operations:query', 'chart', 'admin', NOW(), '', NULL, '公众号官方数据分析'),
(2087, '客服工作台', 4, 110, 'customer-service', 'wxmp/customer-service/index', NULL, 'WxMpCustomerService', 1, 0, 'C', '0', '0', 'wxmp:operations:query', 'peoples', 'admin', NOW(), '', NULL, '公众号客服账号、会话及消息'),
(2088, '群发管理', 4, 120, 'mass-message', 'wxmp/mass-message/index', NULL, 'WxMpMassMessage', 1, 0, 'C', '0', '0', 'wxmp:operations:query', 'clipboard', 'admin', NOW(), '', NULL, '公众号群发及任务管理'),
(2089, '评论管理', 4, 130, 'comments', 'wxmp/comments/index', NULL, 'WxMpComments', 1, 0, 'C', '0', '0', 'wxmp:operations:query', 'edit', 'admin', NOW(), '', NULL, '公众号图文评论管理')
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `parent_id` = VALUES(`parent_id`), `order_num` = VALUES(`order_num`),
  `path` = VALUES(`path`), `component` = VALUES(`component`), `route_name` = VALUES(`route_name`),
  `menu_type` = VALUES(`menu_type`), `perms` = VALUES(`perms`), `icon` = VALUES(`icon`), `remark` = VALUES(`remark`);

-- 小程序独立功能页面。
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2078, '订阅消息', 2049, 20, 'subscribe-message', 'wxma/subscribe-message/index', NULL, 'WxMaSubscribeMessage', 1, 0, 'C', '0', '0', 'wxma:operations:query', 'message', 'admin', NOW(), '', NULL, '小程序订阅消息管理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2078);

UPDATE `sys_menu` SET `menu_name` = '订阅消息', `parent_id` = 2049, `order_num` = 20,
  `path` = 'subscribe-message', `component` = 'wxma/subscribe-message/index', `route_name` = 'WxMaSubscribeMessage',
  `menu_type` = 'C', `perms` = 'wxma:operations:query', `icon` = 'message', `remark` = '小程序订阅消息管理'
WHERE `menu_id` = 2078;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(2090, '小程序码', 2049, 30, 'qrcode', 'wxma/qrcode/index', NULL, 'WxMaQrcode', 1, 0, 'C', '0', '0', 'wxma:operations:edit', 'code', 'admin', NOW(), '', NULL, '小程序码及二维码'),
(2091, '内容安全', 2049, 40, 'content-security', 'wxma/content-security/index', NULL, 'WxMaContentSecurity', 1, 0, 'C', '0', '0', 'wxma:operations:query', 'lock', 'admin', NOW(), '', NULL, '小程序内容安全检测'),
(2092, '数据分析', 2049, 50, 'data-analysis', 'wxma/data-analysis/index', NULL, 'WxMaDataAnalysis', 1, 0, 'C', '0', '0', 'wxma:operations:query', 'chart', 'admin', NOW(), '', NULL, '小程序官方数据分析'),
(2093, '发货及物流', 2049, 60, 'shipping', 'wxma/shipping/index', NULL, 'WxMaShipping', 1, 0, 'C', '0', '0', 'wxma:operations:query', 'list', 'admin', NOW(), '', NULL, '小程序发货信息管理')
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `parent_id` = VALUES(`parent_id`), `order_num` = VALUES(`order_num`),
  `path` = VALUES(`path`), `component` = VALUES(`component`), `route_name` = VALUES(`route_name`),
  `menu_type` = VALUES(`menu_type`), `perms` = VALUES(`perms`), `icon` = VALUES(`icon`), `remark` = VALUES(`remark`);

-- 原有三类公共操作权限，继续作为模板/订阅消息页按钮权限。
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(2079, '公众号运营查询', 2077, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:query', '#', 'admin', NOW(), '', NULL, ''),
(2080, '公众号运营维护', 2077, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:edit', '#', 'admin', NOW(), '', NULL, ''),
(2081, '公众号消息发送', 2077, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:send', '#', 'admin', NOW(), '', NULL, ''),
(2082, '小程序运营查询', 2078, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxma:operations:query', '#', 'admin', NOW(), '', NULL, ''),
(2083, '小程序运营维护', 2078, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxma:operations:edit', '#', 'admin', NOW(), '', NULL, ''),
(2084, '小程序消息发送', 2078, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxma:operations:send', '#', 'admin', NOW(), '', NULL, '')
ON DUPLICATE KEY UPDATE `parent_id` = VALUES(`parent_id`), `perms` = VALUES(`perms`);

-- 各独立页面的权限按钮，便于角色按功能授权。
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(2094, '生成公众号二维码', 2085, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:edit', '#', 'admin', NOW(), '', NULL, ''),
(2095, '查询公众号数据', 2086, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:query', '#', 'admin', NOW(), '', NULL, ''),
(2096, '查询客服数据', 2087, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:query', '#', 'admin', NOW(), '', NULL, ''),
(2097, '维护客服账号和会话', 2087, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:edit', '#', 'admin', NOW(), '', NULL, ''),
(2098, '发送客服消息', 2087, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:send', '#', 'admin', NOW(), '', NULL, ''),
(2099, '查询群发任务', 2088, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:query', '#', 'admin', NOW(), '', NULL, ''),
(2100, '维护群发任务', 2088, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:edit', '#', 'admin', NOW(), '', NULL, ''),
(2101, '发送群发消息', 2088, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:send', '#', 'admin', NOW(), '', NULL, ''),
(2102, '查询公众号评论', 2089, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:query', '#', 'admin', NOW(), '', NULL, ''),
(2103, '维护公众号评论', 2089, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxmp:operations:edit', '#', 'admin', NOW(), '', NULL, ''),
(2104, '生成小程序码', 2090, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxma:operations:edit', '#', 'admin', NOW(), '', NULL, ''),
(2105, '查询安全检测结果', 2091, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxma:operations:query', '#', 'admin', NOW(), '', NULL, ''),
(2106, '提交内容安全检测', 2091, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxma:operations:edit', '#', 'admin', NOW(), '', NULL, ''),
(2107, '查询小程序数据', 2092, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxma:operations:query', '#', 'admin', NOW(), '', NULL, ''),
(2108, '查询发货信息', 2093, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxma:operations:query', '#', 'admin', NOW(), '', NULL, ''),
(2109, '维护发货信息', 2093, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'wxma:operations:edit', '#', 'admin', NOW(), '', NULL, '')
ON DUPLICATE KEY UPDATE `parent_id` = VALUES(`parent_id`), `perms` = VALUES(`perms`);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, m.`menu_id` FROM `sys_menu` m
WHERE m.`menu_id` BETWEEN 2077 AND 2109
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` role_menu
    WHERE role_menu.`role_id` = 2 AND role_menu.`menu_id` = m.`menu_id`
  );
