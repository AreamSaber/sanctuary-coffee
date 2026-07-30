<template>
  <div class="member-center-container">
    <el-row :gutter="20">
      <!-- 会员信息卡片 -->
      <el-col :span="24">
        <el-card class="member-card">
          <div class="member-header">
            <div class="member-info">
              <el-avatar :size="80" :src="avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="info-text">
                <h2>{{ memberInfo.levelName || '普通会员' }}</h2>
                <p class="points-balance">实时积分余额：{{ pointsBalance }}</p>
                <p>成长值: {{ memberInfo.growthValue }} / {{ memberInfo.nextLevelGrowth }}</p>
                <el-progress 
                  :percentage="getGrowthPercentage()" 
                  :stroke-width="8"
                  :show-text="false"
                />
              </div>
            </div>
            <div class="member-stats">
              <div class="stat-item">
                <div class="stat-value">{{ pointsBalance }}</div>
                <div class="stat-label">积分余额</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">¥{{ memberInfo.totalConsumption || 0 }}</div>
                <div class="stat-label">累计消费</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ memberInfo.totalOrders || 0 }}</div>
                <div class="stat-label">订单数</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 生日礼包 -->
      <el-col :span="24">
        <el-card class="birthday-card">
          <div class="birthday-banner">
            <div class="birthday-banner__icon">
              <el-icon :size="48"><Present /></el-icon>
            </div>
            <div class="birthday-banner__content">
              <h3>生日礼包</h3>
              <p v-if="isBirthdayToday">今天是您的生日，生日快乐！系统已自动为您发放生日礼券，请查看权益发放记录。</p>
              <p v-else-if="daysUntilBirthday > 0">距离您的生日还有 {{ daysUntilBirthday }} 天，届时系统将自动发放生日礼券。</p>
              <p v-else>请在个人资料中完善生日信息，生日当天系统将自动发放礼券。</p>
            </div>
            <el-tag v-if="isBirthdayToday" type="danger" effect="dark" size="large">生日快乐</el-tag>
            <el-tag v-else-if="memberInfo.birthday" type="info" effect="plain">
              生日：{{ memberInfo.birthday }}
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- 我的会员权益 -->
      <el-col :span="24">
        <el-card class="benefits-card">
          <template #header>
            <div class="card-header">
              <span>我的会员权益</span>
              <el-tag type="warning" effect="plain">{{ memberInfo.levelName || '普通会员' }}</el-tag>
            </div>
          </template>
          <el-empty v-if="memberBenefits.length === 0" description="当前等级暂无专属权益" />
          <div v-else class="benefit-grid">
            <div v-for="benefit in memberBenefits" :key="benefit.id" class="benefit-item">
              <div class="benefit-badge">{{ benefit.benefitTypeText }}</div>
              <h3>{{ benefit.benefitName }}</h3>
              <strong>{{ benefit.valueText }}</strong>
              <p>{{ benefit.description || '该权益已随当前会员等级自动生效' }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 权益使用记录 -->
      <el-col :span="24">
        <el-card class="usage-card">
          <template #header>
            <div class="card-header">
              <span>权益使用记录</span>
              <el-tag type="info" effect="plain">自动记录抵扣与积分倍率</el-tag>
            </div>
          </template>
          <el-table :data="benefitUsageRecords" style="width: 100%" max-height="320">
            <el-table-column prop="benefitName" label="权益" min-width="150">
              <template #default="{ row }">
                <div class="usage-benefit">
                  <strong>{{ row.benefitName }}</strong>
                  <small>{{ row.benefitTypeText }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="businessTypeText" label="场景" width="120" />
            <el-table-column label="权益效果" width="160">
              <template #default="{ row }">
                <span :class="getUsageEffectClass(row)">
                  {{ formatUsageEffect(row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" min-width="220" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="170" />
          </el-table>
          <el-pagination
            v-model:current-page="usagePage.pageNum"
            v-model:page-size="usagePage.pageSize"
            :total="usagePage.total"
            layout="prev, pager, next"
            @current-change="loadBenefitUsage"
            style="margin-top: 10px; justify-content: center"
          />
        </el-card>
      </el-col>
      
      <!-- 权益发放记录 -->
      <el-col :span="24">
        <el-card class="grant-card">
          <template #header>
            <div class="card-header">
              <span>权益发放记录</span>
              <el-tag type="success" effect="plain">系统发放与手动发放</el-tag>
            </div>
          </template>
          <el-empty v-if="grantRecords.length === 0" description="暂无权益发放记录" />
          <template v-else>
            <el-table :data="grantRecords" style="width: 100%" max-height="320">
              <el-table-column prop="benefitType" label="权益类型" width="120">
                <template #default="{ row }">
                  <el-tag>{{ formatBenefitType(row.benefitType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="grantReason" label="发放原因" min-width="200" />
              <el-table-column label="发放值" width="120">
                <template #default="{ row }">
                  <span v-if="row.grantValue" class="text-success">¥{{ row.grantValue }}</span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'">
                    {{ row.status === 1 ? '已发放' : '已撤销' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="grantTime" label="发放时间" width="170" />
              <el-table-column prop="expireTime" label="过期时间" width="170">
                <template #default="{ row }">
                  {{ row.expireTime || '永久' }}
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="grantPage.pageNum"
              v-model:page-size="grantPage.pageSize"
              :total="grantPage.total"
              layout="prev, pager, next"
              @current-change="loadGrantRecords"
              style="margin-top: 10px; justify-content: center"
            />
          </template>
        </el-card>
      </el-col>

      <!-- 积分记录 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>积分记录</span>
          </template>
          <el-table :data="pointsRecords" style="width: 100%" max-height="400">
            <el-table-column prop="description" label="说明" />
            <el-table-column label="积分" width="100">
              <template #default="{ row }">
                <span :class="row.type === 1 ? 'text-success' : 'text-danger'">
                  {{ row.type === 1 ? '+' : '-' }}{{ row.points }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="afterBalance" label="余额" width="80" />
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
          <el-pagination
            v-model:current-page="pointsPage.pageNum"
            v-model:page-size="pointsPage.pageSize"
            :total="pointsPage.total"
            layout="prev, pager, next"
            @current-change="loadPointsRecords"
            style="margin-top: 10px; justify-content: center"
          />
        </el-card>
      </el-col>
      
      <!-- 我的优惠券 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的优惠券</span>
              <el-button type="primary" size="small" @click="goCouponCenter">
                领券中心
              </el-button>
            </div>
          </template>
          <div class="coupon-list">
            <el-empty v-if="myCoupons.length === 0" description="暂无优惠券" />
            <div v-else class="coupon-items">
              <div
                v-for="coupon in myCoupons"
                :key="coupon.id"
                class="coupon-item"
              >
                <div class="coupon-left">
                  <div class="coupon-amount">
                    <template v-if="coupon.couponType === 1">
                      <span class="amount">¥{{ coupon.discountAmount }}</span>
                    </template>
                    <template v-else-if="coupon.couponType === 2">
                      <span class="amount">{{ coupon.discountRate * 10 }}折</span>
                    </template>
                  </div>
                  <div class="coupon-condition">
                    满{{ coupon.minAmount }}可用
                  </div>
                </div>
                <div class="coupon-right">
                  <div class="coupon-name">{{ coupon.couponName }}</div>
                  <div class="coupon-time">
                    {{ formatDate(coupon.endTime) }}到期
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { User, Present } from '@element-plus/icons-vue'
import {
  getMemberInfo,
  getPointsBalance,
  getPointsRecords,
  getMyCoupons,
  getMyMemberBenefits,
  getMyBenefitUsage,
  getMyBenefitGrantLogs
} from '@/api/member'
import { useRouter } from 'vue-router'

const router = useRouter()
const memberInfo = ref({})
const avatar = ref('')
const pointsBalance = ref(0)
const pointsRecords = ref([])
const memberBenefits = ref([])
const benefitUsageRecords = ref([])
const pointsPage = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0
})
const usagePage = ref({
  pageNum: 1,
  pageSize: 5,
  total: 0
})
const grantRecords = ref([])
const grantPage = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0
})
const myCoupons = ref([])

const isBirthdayToday = computed(() => {
  if (!memberInfo.value.birthday) return false
  const today = new Date()
  const birthday = new Date(memberInfo.value.birthday)
  return today.getMonth() === birthday.getMonth() && today.getDate() === birthday.getDate()
})

const daysUntilBirthday = computed(() => {
  if (!memberInfo.value.birthday) return -1
  const today = new Date()
  const birthday = new Date(memberInfo.value.birthday)
  const thisYearBirthday = new Date(today.getFullYear(), birthday.getMonth(), birthday.getDate())
  if (thisYearBirthday < today) {
    thisYearBirthday.setFullYear(today.getFullYear() + 1)
  }
  const diffTime = thisYearBirthday.getTime() - today.getTime()
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
})

onMounted(() => {
  loadMemberInfo()
  loadPointsBalance()
  loadPointsRecords()
  loadMyCoupons()
  loadMemberBenefits()
  loadBenefitUsage()
  loadGrantRecords()
})

const loadMemberInfo = async () => {
  try {
    const res = await getMemberInfo()
    memberInfo.value = res.data || {}
    pointsBalance.value = Number(res.data?.points || 0)
  } catch (error) {
    console.error('加载会员信息失败:', error)
  }
}

const loadPointsBalance = async () => {
  try {
    const res = await getPointsBalance()
    pointsBalance.value = Number(res.data || 0)
  } catch (error) {
    console.error('加载积分余额失败:', error)
  }
}

const loadPointsRecords = async () => {
  try {
    const res = await getPointsRecords({
      pageNum: pointsPage.value.pageNum,
      pageSize: pointsPage.value.pageSize
    })
    pointsRecords.value = res.data.records || []
    pointsPage.value.total = res.data.total
  } catch (error) {
    console.error('加载积分记录失败:', error)
  }
}

const loadMyCoupons = async () => {
  try {
    const res = await getMyCoupons(0) // 0-未使用
    myCoupons.value = (res.data || []).slice(0, 3)
  } catch (error) {
    console.error('加载优惠券失败:', error)
  }
}

const loadMemberBenefits = async () => {
  try {
    const res = await getMyMemberBenefits()
    memberBenefits.value = res.data || []
  } catch (error) {
    console.error('加载会员权益失败:', error)
  }
}

const loadBenefitUsage = async () => {
  try {
    const res = await getMyBenefitUsage({
      pageNum: usagePage.value.pageNum,
      pageSize: usagePage.value.pageSize
    })
    benefitUsageRecords.value = res.data?.records || []
    usagePage.value.total = Number(res.data?.total || 0)
  } catch (error) {
    console.error('加载权益使用记录失败:', error)
  }
}

const loadGrantRecords = async () => {
  try {
    const res = await getMyBenefitGrantLogs({
      pageNum: grantPage.value.pageNum,
      pageSize: grantPage.value.pageSize
    })
    grantRecords.value = res.data?.records || []
    grantPage.value.total = Number(res.data?.total || 0)
  } catch (error) {
    console.error('加载权益发放记录失败:', error)
  }
}

const getGrowthPercentage = () => {
  if (!memberInfo.value.nextLevelGrowth) return 100
  const total = memberInfo.value.growthValue + memberInfo.value.nextLevelGrowth
  return Math.floor((memberInfo.value.growthValue / total) * 100)
}

const goCouponCenter = () => {
  router.push('/coupon')
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

const formatUsageEffect = (row) => {
  const amount = Number(row.effectAmount || 0)
  const points = Number(row.effectPoints || 0)
  if (amount !== 0) {
    return `${amount > 0 ? '-' : '+'}¥${Math.abs(amount).toFixed(2)}`
  }
  if (points !== 0) {
    return `${points > 0 ? '+' : '-'}${Math.abs(points)} 积分`
  }
  return '-'
}

const getUsageEffectClass = (row) => {
  const points = Number(row.effectPoints || 0)
  const amount = Number(row.effectAmount || 0)
  if (points < 0 || amount < 0) return 'text-danger'
  return 'text-success'
}

const formatBenefitType = (type) => {
  const map = {
    1: '专属折扣',
    2: '积分倍率',
    3: '免配送费',
    4: '生日礼券',
    5: '专属服务'
  }
  return map[type] || '未知权益'
}
</script>

<style scoped>
.member-center-container {
  padding: 20px;
}

.member-card {
  margin-bottom: 20px;
}

.birthday-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #fff5f5 0%, #fff0e6 100%);
  border: 1px solid #ffd6d6;
}

.birthday-banner {
  display: flex;
  align-items: center;
  gap: 20px;
}

.birthday-banner__icon {
  color: #f56c6c;
  flex-shrink: 0;
}

.birthday-banner__content {
  flex: 1;
}

.birthday-banner__content h3 {
  margin: 0 0 8px 0;
  color: #f56c6c;
  font-size: 18px;
}

.birthday-banner__content p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.benefits-card {
  margin-bottom: 20px;
}

.usage-card {
  margin-bottom: 20px;
}

.member-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.info-text h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
}

.info-text p {
  margin: 5px 0;
  color: #666;
}

.member-stats {
  display: flex;
  gap: 40px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  margin-top: 5px;
  color: #909399;
  font-size: 14px;
}

.text-success {
  color: #67c23a;
  font-weight: bold;
}

.text-danger {
  color: #f56c6c;
  font-weight: bold;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.benefit-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.benefit-item {
  position: relative;
  min-height: 150px;
  padding: 18px;
  border: 1px solid #f0e0ca;
  border-radius: 16px;
  background: linear-gradient(135deg, #fffaf3 0%, #f7ead8 100%);
  overflow: hidden;
}

.benefit-item::after {
  content: '';
  position: absolute;
  right: -28px;
  bottom: -32px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(168, 115, 56, 0.12);
}

.benefit-badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(168, 115, 56, 0.12);
  color: #8a5526;
  font-size: 12px;
  font-weight: 600;
}

.benefit-item h3 {
  margin: 14px 0 8px;
  font-size: 18px;
}

.benefit-item strong {
  color: #c27a2c;
  font-size: 20px;
}

.benefit-item p {
  position: relative;
  z-index: 1;
  margin: 10px 0 0;
  color: #8a6f5a;
  line-height: 1.6;
}

.usage-benefit {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.usage-benefit small {
  color: #909399;
}

.coupon-list {
  min-height: 300px;
}

.coupon-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.coupon-item {
  display: flex;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.coupon-left {
  width: 120px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 15px;
}

.coupon-amount .amount {
  font-size: 24px;
  font-weight: bold;
}

.coupon-condition {
  font-size: 12px;
  margin-top: 5px;
}

.coupon-right {
  flex: 1;
  padding: 15px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.coupon-name {
  font-weight: bold;
  margin-bottom: 5px;
}

.coupon-time {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 1200px) {
  .benefit-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .member-header,
  .member-stats {
    align-items: flex-start;
    flex-direction: column;
  }

  .benefit-grid {
    grid-template-columns: 1fr;
  }
}
</style>
