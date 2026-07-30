<template>
  <div class="coupon-center-container">
    <el-card>
      <template #header>
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="领券中心" name="available" />
          <el-tab-pane label="我的优惠券" name="my" />
        </el-tabs>
      </template>
      
      <!-- 领券中心 -->
      <div v-if="activeTab === 'available'" v-loading="loading">
        <div class="coupon-grid">
          <div
            v-for="coupon in availableCoupons"
            :key="coupon.id"
            class="coupon-card"
          >
            <div class="coupon-content">
              <div class="coupon-main">
                <div class="coupon-value">
                  <template v-if="coupon.couponType === 1">
                    <span class="value">¥{{ coupon.discountAmount }}</span>
                  </template>
                  <template v-else-if="coupon.couponType === 2">
                    <span class="value">{{ coupon.discountRate * 10 }}折</span>
                  </template>
                  <template v-else>
                    <span class="value">免邮</span>
                  </template>
                </div>
                <div class="coupon-info">
                  <h3>{{ coupon.couponName }}</h3>
                  <p class="condition">满{{ coupon.minAmount }}元可用</p>
                  <p class="time">
                    {{ formatDate(coupon.startTime) }} - {{ formatDate(coupon.endTime) }}
                  </p>
                  <p class="remain">剩余: {{ coupon.remainCount }}</p>
                </div>
              </div>
              <div class="coupon-action">
                <el-button
                  v-if="!coupon.received"
                  type="primary"
                  @click="handleReceive(coupon.id)"
                >
                  立即领取
                </el-button>
                <el-tag v-else type="info">已领取</el-tag>
              </div>
            </div>
          </div>
        </div>
        
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadAvailableCoupons"
          style="margin-top: 20px; justify-content: center"
        />
      </div>
      
      <!-- 我的优惠券 -->
      <div v-else>
        <el-radio-group v-model="couponStatus" @change="loadMyCoupons" style="margin-bottom: 20px">
          <el-radio-button :value="0">未使用</el-radio-button>
          <el-radio-button :value="1">已使用</el-radio-button>
          <el-radio-button :value="2">已过期</el-radio-button>
        </el-radio-group>
        
        <div class="coupon-grid">
          <div
            v-for="coupon in myCoupons"
            :key="coupon.id"
            class="coupon-card"
            :class="{ 'coupon-disabled': couponStatus !== 0 }"
          >
            <div class="coupon-content">
              <div class="coupon-main">
                <div class="coupon-value">
                  <template v-if="coupon.couponType === 1">
                    <span class="value">¥{{ coupon.discountAmount }}</span>
                  </template>
                  <template v-else-if="coupon.couponType === 2">
                    <span class="value">{{ coupon.discountRate * 10 }}折</span>
                  </template>
                  <template v-else>
                    <span class="value">免邮</span>
                  </template>
                </div>
                <div class="coupon-info">
                  <h3>{{ coupon.couponName }}</h3>
                  <p class="condition">满{{ coupon.minAmount }}元可用</p>
                  <p class="time">
                    {{ formatDate(coupon.expireTime || coupon.endTime) }}到期
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <el-empty v-if="myCoupons.length === 0" description="暂无优惠券" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAvailableCoupons, receiveCoupon, getMyCoupons } from '@/api/member'

const loading = ref(false)
const activeTab = ref('available')
const availableCoupons = ref([])
const myCoupons = ref([])
const couponStatus = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

onMounted(() => {
  loadAvailableCoupons()
})

const loadAvailableCoupons = async () => {
  loading.value = true
  try {
    const res = await getAvailableCoupons({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    availableCoupons.value = res.data.records || []
    total.value = res.data.total
  } catch (error) {
    console.error('加载优惠券失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMyCoupons = async () => {
  try {
    const res = await getMyCoupons(couponStatus.value)
    myCoupons.value = res.data || []
  } catch (error) {
    console.error('加载我的优惠券失败:', error)
  }
}

const handleTabChange = (tab) => {
  if (tab === 'my') {
    loadMyCoupons()
  }
}

const handleReceive = async (couponId) => {
  try {
    await receiveCoupon(couponId)
    ElMessage.success('领取成功')
    loadAvailableCoupons()
  } catch (error) {
    console.error('领取失败:', error)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}
</script>

<style scoped>
.coupon-center-container {
  padding: 20px;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
  min-height: 400px;
}

.coupon-card {
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
}

.coupon-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.coupon-card.coupon-disabled {
  opacity: 0.6;
  filter: grayscale(1);
}

.coupon-content {
  padding: 20px;
}

.coupon-main {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.coupon-value {
  width: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 8px;
  padding: 10px;
}

.coupon-value .value {
  font-size: 28px;
  font-weight: bold;
}

.coupon-info {
  flex: 1;
}

.coupon-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
}

.coupon-info p {
  margin: 4px 0;
  font-size: 13px;
  color: #666;
}

.coupon-info .condition {
  color: #f56c6c;
  font-weight: bold;
}

.coupon-action {
  text-align: center;
}
</style>
