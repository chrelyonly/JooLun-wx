/*
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 * 注意：
 * 本软件为www.joolun.com开发研制，项目使用请保留此说明
 */

-- 3.9.0 升级 3.9.1：系统管理下增加微信账号配置、商城配置菜单及查询/修改权限。
-- 配置值首次在页面保存时写入 sys_config；未保存时继续使用 application.yml 默认值。

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2071, '微信账号配置', 1, 10, 'wxconfig', 'system/wxconfig/index', NULL, '', 1, 0, 'C', '0', '0', 'system:wxconfig:query', 'wechat', 'admin', NOW(), '', NULL, '微信账号配置菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2071);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2072, '商城配置', 1, 11, 'mallconfig', 'system/mallconfig/index', NULL, '', 1, 0, 'C', '0', '0', 'system:mallconfig:query', 'shopping', 'admin', NOW(), '', NULL, '商城配置菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2072);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2073, '微信配置查询', 2071, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:wxconfig:query', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2073);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2074, '微信配置修改', 2071, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:wxconfig:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2074);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2075, '商城配置查询', 2072, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:mallconfig:query', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2075);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2076, '商城配置修改', 2072, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:mallconfig:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2076);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, m.`menu_id` FROM `sys_menu` m
WHERE m.`menu_id` BETWEEN 2071 AND 2076
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` role_menu
    WHERE role_menu.`role_id` = 2 AND role_menu.`menu_id` = m.`menu_id`
  );
