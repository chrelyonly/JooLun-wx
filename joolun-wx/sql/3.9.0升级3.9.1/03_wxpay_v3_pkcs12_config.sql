/*
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 */

-- 3.9.0 升级 3.9.1：项目支付能力统一使用微信支付 API v3 和 PKCS#12 商户证书。
-- 删除 API v2 配置以及已被 PKCS#12 证书替代的 PEM/序列号配置。

DELETE FROM `sys_config`
WHERE `config_key` IN (
  'wx.ma.mchKey',
  'wx.ma.keyPath',
  'wx.ma.certSerialNo',
  'wx.ma.privateKeyPath',
  'wx.ma.privateCertPath'
);
