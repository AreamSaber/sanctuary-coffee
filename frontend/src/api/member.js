import request from '@/utils/request'

/**
 * 获取会员信息
 */
export function getMemberInfo() {
  return request({
    url: '/member/info',
    method: 'get'
  })
}

/**
 * 获取积分余额
 */
export function getPointsBalance() {
  return request({
    url: '/member/points/balance',
    method: 'get'
  })
}

/**
 * 分页查询积分记录
 */
export function getPointsRecords(params) {
  return request({
    url: '/member/points/records',
    method: 'get',
    params
  })
}

/**
 * 分页查询可领取优惠券
 */
export function getAvailableCoupons(params) {
  return request({
    url: '/coupon/available',
    method: 'get',
    params
  })
}

/**
 * 领取优惠券
 */
export function receiveCoupon(id) {
  return request({
    url: `/coupon/${id}/receive`,
    method: 'post'
  })
}

/**
 * 获取我的优惠券
 */
export function getMyCoupons(status) {
  return request({
    url: '/coupon/my',
    method: 'get',
    params: { status }
  })
}

/**
 * 分页查询优惠券（管理员）
 */
export function getCouponPage(params) {
  return request({
    url: '/coupon/page',
    method: 'get',
    params
  })
}

/**
 * 创建优惠券
 */
export function createCoupon(data) {
  return request({
    url: '/coupon',
    method: 'post',
    data
  })
}

/**
 * 更新优惠券
 */
export function updateCoupon(id, data) {
  return request({
    url: `/coupon/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除优惠券
 */
export function deleteCoupon(id) {
  return request({
    url: `/coupon/${id}`,
    method: 'delete'
  })
}

/**
 * 更新优惠券状态
 */
export function updateCouponStatus(id, status) {
  return request({
    url: `/coupon/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 获取优惠券核销统计
 */
export function getCouponStats(params) {
  return request({
    url: '/coupon/stats',
    method: 'get',
    params
  })
}

/**
 * 分页查询会员等级
 */
export function getMemberLevelPage(params) {
  return request({
    url: '/member-level/page',
    method: 'get',
    params
  })
}

/**
 * 获取所有启用的会员等级
 */
export function getActiveLevels() {
  return request({
    url: '/member-level/list',
    method: 'get'
  })
}

/**
 * 创建会员等级
 */
export function createLevel(data) {
  return request({
    url: '/member-level',
    method: 'post',
    data
  })
}

/**
 * 更新会员等级
 */
export function updateLevel(data) {
  return request({
    url: '/member-level',
    method: 'put',
    data
  })
}

/**
 * 删除会员等级
 */
export function deleteLevel(id) {
  return request({
    url: `/member-level/${id}`,
    method: 'delete'
  })
}

/**
 * 更新会员等级状态
 */
export function updateLevelStatus(id, status) {
  return request({
    url: `/member-level/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 分页查询会员权益（管理员）
 */
export function getMemberBenefitPage(params) {
  return request({
    url: '/member-benefit/page',
    method: 'get',
    params
  })
}

/**
 * 查询启用会员权益
 */
export function getActiveMemberBenefits() {
  return request({
    url: '/member-benefit/active',
    method: 'get'
  })
}

/**
 * 查询当前用户会员权益
 */
export function getMyMemberBenefits() {
  return request({
    url: '/member-benefit/my',
    method: 'get'
  })
}

/**
 * 查询当前用户权益使用记录
 */
export function getMyBenefitUsage(params) {
  return request({
    url: '/member-benefit/usage/my',
    method: 'get',
    params
  })
}

/**
 * 分页查询会员权益使用记录（管理员）
 */
export function getBenefitUsagePage(params) {
  return request({
    url: '/member-benefit/usage/page',
    method: 'get',
    params
  })
}

/**
 * 创建会员权益
 */
export function createMemberBenefit(data) {
  return request({
    url: '/member-benefit',
    method: 'post',
    data
  })
}

/**
 * 更新会员权益
 */
export function updateMemberBenefit(id, data) {
  return request({
    url: `/member-benefit/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除会员权益
 */
export function deleteMemberBenefit(id) {
  return request({
    url: `/member-benefit/${id}`,
    method: 'delete'
  })
}

/**
 * 更新会员权益状态
 */
export function updateMemberBenefitStatus(id, status) {
  return request({
    url: `/member-benefit/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 查询会员等级权益矩阵
 */
export function getLevelBenefitMatrix() {
  return request({
    url: '/member-benefit/level-matrix',
    method: 'get'
  })
}

/**
 * 保存会员等级权益绑定
 */
export function saveLevelBenefitBindings(levelId, benefitIds) {
  return request({
    url: `/member-benefit/level/${levelId}`,
    method: 'put',
    data: { benefitIds }
  })
}

/**
 * 获取会员列表（管理员）
 */
export function getMemberList(params) {
  return request({
    url: '/member/list',
    method: 'get',
    params
  })
}

/**
 * 编辑会员资料（管理员）
 */
export function updateMember(userId, data) {
  return request({
    url: `/member/${userId}`,
    method: 'put',
    data
  })
}

/**
 * 调整会员积分（管理员）
 */
export function adjustMemberPoints(params) {
  return request({
    url: '/member/points/adjust',
    method: 'post',
    params
  })
}

/**
 * 查询当前用户权益发放记录
 */
export function getMyBenefitGrantLogs(params) {
  return request({
    url: '/member-benefit/grant-logs',
    method: 'get',
    params
  })
}
