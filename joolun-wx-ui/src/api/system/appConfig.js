import request from '@/utils/request'

// 查询微信账号配置
export function getWxAccountConfig() {
  return request({
    url: '/system/app-config/wx',
    method: 'get'
  })
}

// 保存微信账号配置
export function updateWxAccountConfig(data) {
  return request({
    url: '/system/app-config/wx',
    method: 'put',
    data
  })
}

// 查询商城配置
export function getMallConfig() {
  return request({
    url: '/system/app-config/mall',
    method: 'get'
  })
}

// 保存商城配置
export function updateMallConfig(data) {
  return request({
    url: '/system/app-config/mall',
    method: 'put',
    data
  })
}
