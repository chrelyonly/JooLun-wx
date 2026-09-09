/**
 * Copyright (C) 2026
 * All rights reserved, Designed By www.joolun.com
 */
import request from "@/utils/request";

const baseUrl = "/wxmp/operations";

export const getUserOptions = (keyword = "") => request({ url: `${baseUrl}/users`, method: "get", params: { keyword } });
export const getTemplates = () => request({ url: `${baseUrl}/templates`, method: "get" });
export const addTemplate = (data) => request({ url: `${baseUrl}/templates`, method: "post", data });
export const deleteTemplate = (templateId) => request({ url: `${baseUrl}/templates/${encodeURIComponent(templateId)}`, method: "delete" });
export const sendTemplate = (data) => request({ url: `${baseUrl}/templates/send`, method: "post", data });
export const createQrcode = (data) => request({ url: `${baseUrl}/qrcode`, method: "post", data });
export const getAnalytics = (params) => request({ url: `${baseUrl}/analytics`, method: "get", params });

export const getCustomerAccounts = () => request({ url: `${baseUrl}/customer/accounts`, method: "get" });
export const getOnlineAccounts = () => request({ url: `${baseUrl}/customer/online`, method: "get" });
export const addCustomerAccount = (data) => request({ url: `${baseUrl}/customer/accounts`, method: "post", data });
export const updateCustomerAccount = (data) => request({ url: `${baseUrl}/customer/accounts`, method: "put", data });
export const deleteCustomerAccount = (account) => request({ url: `${baseUrl}/customer/accounts/${encodeURIComponent(account)}`, method: "delete" });
export const getCustomerSessions = (account) => request({ url: `${baseUrl}/customer/sessions`, method: "get", params: { account } });
export const getWaitingSessions = () => request({ url: `${baseUrl}/customer/waiting`, method: "get" });
export const createCustomerSession = (data) => request({ url: `${baseUrl}/customer/sessions`, method: "post", data });
export const closeCustomerSession = (params) => request({ url: `${baseUrl}/customer/sessions`, method: "delete", params });
export const getCustomerRecords = (params) => request({ url: `${baseUrl}/customer/records`, method: "get", params });
export const sendCustomerMessage = (data) => request({ url: `${baseUrl}/customer/messages`, method: "post", data });

export const sendMassByTag = (data) => request({ url: `${baseUrl}/mass/tag`, method: "post", data });
export const sendMassByOpenIds = (data) => request({ url: `${baseUrl}/mass/openids`, method: "post", data });
export const previewMass = (data) => request({ url: `${baseUrl}/mass/preview`, method: "post", data });
export const getMassStatus = (msgId) => request({ url: `${baseUrl}/mass/${msgId}`, method: "get" });
export const deleteMass = (msgId, articleIndex) => request({ url: `${baseUrl}/mass/${msgId}`, method: "delete", params: { articleIndex } });
export const getMassSpeed = () => request({ url: `${baseUrl}/mass-speed`, method: "get" });
export const updateMassSpeed = (speed) => request({ url: `${baseUrl}/mass-speed/${speed}`, method: "put" });

export const getComments = (params) => request({ url: `${baseUrl}/comments`, method: "get", params });
export const operateComment = (action, data) => request({ url: `${baseUrl}/comments/${action}`, method: "post", data });
