<template>
  <div class="app-page payment-page">
    <section class="app-page-header app-page-header--compact">
      <div>
        <h1 class="app-page-header__title">订单支付</h1>
      </div>
      <div class="app-page-actions">
        <el-tag v-if="orderNo" effect="plain">订单号：{{ orderNo }}</el-tag>
        <el-button @click="router.push('/order')">返回订单</el-button>
      </div>
    </section>

    <section v-if="!hasOrderId" class="page-grid page-grid--content">
      <el-card class="stack-card" shadow="never">
        <div class="stack-card__body">
          <el-empty description="还没有待支付订单">
            <div class="payment-page__empty-actions">
              <el-button type="primary" @click="router.push('/order')">去订单中心</el-button>
              <el-button @click="router.push('/shop')">去商品中心</el-button>
            </div>
          </el-empty>
        </div>
      </el-card>
    </section>

    <section v-else class="page-grid page-grid--content">
      <el-card class="stack-card" shadow="never" v-loading="loading">
        <div class="stack-card__body payment-page__body">
          <div v-if="settlement.items?.length" class="payment-section">
            <h3 class="stack-card__title">商品清单</h3>
            <div
              v-for="item in settlement.items"
              :key="`${item.productId}-${item.skuId || 'default'}`"
              class="payment-item"
            >
              <ProductImage :src="item.productImage" :name="item.productName" fit="cover" class="payment-item__image" />
              <div class="payment-item__copy">
                <strong>{{ item.productName }}</strong>
                <span v-if="item.specInfo">{{ item.specInfo }}</span>
                <span>¥{{ item.price }} x {{ item.quantity }}</span>
              </div>
              <strong class="payment-item__total">¥{{ item.totalAmount }}</strong>
            </div>
          </div>

          <div class="payment-section">
            <div class="payment-section__head">
              <h3 class="stack-card__title">优惠设置</h3>
              <p class="compact-note">只保留本单可用的优惠券和积分额度。</p>
            </div>

            <div class="payment-option">
              <el-checkbox v-model="useCoupon" :disabled="availableCoupons.length === 0">使用优惠券</el-checkbox>
              <el-select
                v-if="useCoupon"
                v-model="selectedCoupon"
                placeholder="选择优惠券"
                class="payment-option__control"
              >
                <el-option
                  v-for="coupon in availableCoupons"
                  :key="coupon.id"
                  :label="formatCouponLabel(coupon)"
                  :value="coupon.id"
                />
              </el-select>
            </div>

            <div class="payment-option">
              <el-checkbox v-model="usePoints" :disabled="settlement.availablePoints === 0">
                使用积分（可用 {{ settlement.availablePoints || 0 }}）
              </el-checkbox>
              <el-input-number
                v-if="usePoints"
                v-model="pointsToUse"
                :min="0"
                :max="settlement.availablePoints || 0"
                :step="100"
              />
            </div>

            <div v-if="settlement.memberBenefits?.length" class="member-benefit-strip">
              <div class="member-benefit-strip__head">
                <span>当前会员权益</span>
                <strong v-if="pointRewardMultiplier > 1">积分 {{ pointRewardMultiplier }} 倍</strong>
              </div>
              <div class="member-benefit-strip__items">
                <el-tag
                  v-for="benefit in settlement.memberBenefits"
                  :key="benefit.id"
                  type="warning"
                  effect="plain"
                >
                  {{ benefit.benefitName }} · {{ benefit.valueText }}
                </el-tag>
              </div>
            </div>
          </div>

          <div class="payment-section">
            <div class="payment-section__head">
              <h3 class="stack-card__title">支付方式</h3>
              <p class="compact-note">当前为毕设演示环境，采用模拟支付验证订单状态、优惠核销、库存扣减和配送单生成流程。</p>
            </div>

            <el-alert
              title="模拟支付说明"
              type="info"
              show-icon
              :closable="false"
              description="本页面不会跳转真实支付宝或微信收银台；创建支付单后点击确认模拟支付，系统会按真实业务流程推进订单状态。"
              class="payment-demo-alert"
            />

            <el-radio-group v-model="payType" class="pay-types" :disabled="Boolean(paymentNo)">
              <el-radio :label="1" border>支付宝（模拟）</el-radio>
              <el-radio :label="2" border>微信支付（模拟）</el-radio>
              <el-radio :label="3" border>余额/线下确认（模拟）</el-radio>
            </el-radio-group>
          </div>

          <div v-if="paymentNo" class="payment-section">
            <div class="payment-section__head">
              <h3 class="stack-card__title">支付进度</h3>
              <p class="compact-note">{{ paymentProgressTip }}</p>
            </div>

            <el-descriptions :column="1" border>
              <el-descriptions-item label="支付单号">{{ paymentNo }}</el-descriptions-item>
              <el-descriptions-item label="当前状态">
                {{ isZeroAmount ? '已自动完成' : '待确认支付' }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="payment-pending-actions">
              <el-button
                v-if="!isZeroAmount"
                type="primary"
                :loading="confirming"
                @click="handleConfirmPayment"
              >
                确认模拟支付
              </el-button>
              <el-button @click="router.push('/order')">返回订单列表</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stack-card" shadow="never">
        <div class="stack-card__header">
          <div>
            <h2 class="stack-card__title">支付摘要</h2>
            <p class="compact-note">会员折扣、优惠券和积分会统一折算到应付金额。</p>
          </div>
        </div>
        <div class="stack-card__body">
          <div class="metric-list">
            <div class="metric-list__item">
              <span>商品金额</span>
              <strong>¥{{ formatAmount(settlement.totalAmount) }}</strong>
            </div>
            <div class="metric-list__item">
              <span>运费</span>
              <strong>¥{{ formatAmount(settlement.freightAmount) }}</strong>
            </div>
            <div v-if="promotionDiscount > 0" class="metric-list__item payment-summary__discount">
              <span>促销活动优惠</span>
              <strong>-¥{{ promotionDiscount.toFixed(2) }}</strong>
            </div>
            <div v-if="freightBenefitDiscount > 0" class="metric-list__item payment-summary__discount">
              <span>会员免配送费</span>
              <strong>-¥{{ freightBenefitDiscount.toFixed(2) }}</strong>
            </div>
            <div v-if="couponDiscount > 0" class="metric-list__item payment-summary__discount">
              <span>优惠券抵扣</span>
              <strong>-¥{{ couponDiscount.toFixed(2) }}</strong>
            </div>
            <div v-if="pointsDiscount > 0" class="metric-list__item payment-summary__discount">
              <span>积分抵扣</span>
              <strong>-¥{{ pointsDiscount.toFixed(2) }}</strong>
            </div>
            <div v-if="memberDiscount > 0" class="metric-list__item payment-summary__discount">
              <span>{{ memberDiscountLabel }}</span>
              <strong>-¥{{ memberDiscount.toFixed(2) }}</strong>
            </div>
            <div class="metric-list__item">
              <span>应付金额</span>
              <strong class="payment-summary__total">¥{{ finalAmount }}</strong>
            </div>
            <div v-if="estimatedRewardPoints > 0" class="metric-list__item payment-summary__reward">
              <span>预计支付奖励积分</span>
              <strong>+{{ estimatedRewardPoints }}</strong>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            class="payment-summary__submit"
            :loading="paying"
            :disabled="Boolean(paymentNo)"
            @click="handlePay"
          >
            {{ paymentNo ? '模拟支付单已创建' : '创建模拟支付单' }}
          </el-button>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ProductImage from '@/components/common/ProductImage.vue'
import { getMyCoupons } from '@/api/member'
import { confirmPayment, createPayment, getOrderSettlement } from '@/api/payment'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const paying = ref(false)
const confirming = ref(false)
const orderId = ref(null)
const orderNo = ref('')
const paymentNo = ref('')
const payType = ref(1)

const createDefaultSettlement = () => ({
  totalAmount: 0,
  freightAmount: 0,
  promotionDiscount: 0,
  freightDiscount: 0,
  couponDiscount: 0,
  pointsDiscount: 0,
  memberDiscount: 0,
  memberDiscountRate: 1,
  pointRewardMultiplier: 1,
  estimatedRewardPoints: 0,
  payAmount: 0,
  availablePoints: 0,
  pointsRate: 100,
  memberBenefits: [],
  items: []
})

const settlement = ref(createDefaultSettlement())
const useCoupon = ref(false)
const selectedCoupon = ref(null)
const availableCoupons = ref([])
const usePoints = ref(false)
const pointsToUse = ref(0)
const hasOrderId = computed(() => Number.isFinite(orderId.value) && orderId.value > 0)
const promotionDiscount = computed(() => Number(settlement.value.promotionDiscount || 0))
const freightBenefitDiscount = computed(() => Number(settlement.value.freightDiscount || 0))
const memberDiscountRate = computed(() => Number(settlement.value.memberDiscountRate || 1))
const memberDiscountLabel = computed(() => {
  const rate = memberDiscountRate.value
  if (!Number.isFinite(rate) || rate <= 0 || rate >= 1) return '会员折扣'
  return `会员折扣（${(rate * 10).toFixed(1).replace(/\.0$/, '')}折）`
})
const pointRewardMultiplier = computed(() => Number(settlement.value.pointRewardMultiplier || 1))
const amountBeforeUserDiscounts = computed(() => {
  const totalAmount = Number(settlement.value.totalAmount || 0)
  const freightAmount = Number(settlement.value.freightAmount || 0)
  return Math.max(totalAmount + freightAmount - promotionDiscount.value - freightBenefitDiscount.value, 0)
})
const remainingFreightAmount = computed(() => {
  const freightAmount = Number(settlement.value.freightAmount || 0)
  return Math.max(freightAmount - freightBenefitDiscount.value, 0)
})

const couponDiscount = computed(() => {
  if (!useCoupon.value || !selectedCoupon.value) return 0
  const coupon = availableCoupons.value.find((item) => item.id === selectedCoupon.value)
  if (!coupon) return 0

  const baseAmount = amountBeforeUserDiscounts.value
  let discount = 0
  if (Number(coupon.couponType) === 3) {
    discount = remainingFreightAmount.value
  } else if (coupon.discountAmount) {
    discount = Number(coupon.discountAmount)
  } else if (coupon.discountRate) {
    discount = Math.max(baseAmount * (1 - Number(coupon.discountRate)), 0)
  }
  return Math.min(Math.max(discount, 0), baseAmount)
})

const amountAfterCoupon = computed(() => Math.max(amountBeforeUserDiscounts.value - couponDiscount.value, 0))

const pointsDiscount = computed(() => {
  if (!usePoints.value || !pointsToUse.value) return 0
  const rate = Number(settlement.value.pointsRate || 100)
  const discount = Number(pointsToUse.value) / rate
  return Math.min(Math.max(discount, 0), amountAfterCoupon.value)
})

const amountAfterPoints = computed(() => Math.max(amountAfterCoupon.value - pointsDiscount.value, 0))

const memberDiscount = computed(() => {
  const rate = memberDiscountRate.value
  if (!Number.isFinite(rate) || rate <= 0 || rate >= 1) return 0
  return amountAfterPoints.value * (1 - rate)
})

const finalAmount = computed(() => {
  return Math.max(amountAfterPoints.value - memberDiscount.value, 0).toFixed(2)
})

const estimatedRewardPoints = computed(() => Math.floor(Number(finalAmount.value) * pointRewardMultiplier.value))

const isZeroAmount = computed(() => Number(finalAmount.value) === 0)
const paymentProgressTip = computed(() => (
  isZeroAmount.value
    ? '应付金额为 0，后端已自动完成模拟支付。'
    : '当前为模拟支付流程，请点击确认模拟支付；确认后订单会进入配送流程。'
))

const initializePage = async (rawOrderId) => {
  resetPageState()

  const parsedOrderId = Number(rawOrderId)
  if (!Number.isFinite(parsedOrderId) || parsedOrderId <= 0) {
    return
  }

  orderId.value = parsedOrderId
  await loadSettlement()
  if (hasOrderId.value) {
    await loadCoupons()
  }
}

const resetPageState = () => {
  orderId.value = null
  orderNo.value = ''
  paymentNo.value = ''
  payType.value = 1
  settlement.value = createDefaultSettlement()
  useCoupon.value = false
  selectedCoupon.value = null
  availableCoupons.value = []
  usePoints.value = false
  pointsToUse.value = 0
}

const loadSettlement = async () => {
  loading.value = true
  try {
    const res = await getOrderSettlement(orderId.value)
    settlement.value = res.data
    orderNo.value = res.data.orderNo
  } catch (error) {
    ElMessage.error(error.message || '结算信息加载失败')
    orderId.value = null
  } finally {
    loading.value = false
  }
}

const loadCoupons = async () => {
  try {
    const res = await getMyCoupons(0)
    availableCoupons.value = (res.data || []).filter((coupon) => {
      const minAmount = Number(coupon.minAmount || 0)
      if (amountBeforeUserDiscounts.value < minAmount) {
        return false
      }
      return Number(coupon.couponType) !== 3 || remainingFreightAmount.value > 0
    })
    if (!availableCoupons.value.some((coupon) => coupon.id === selectedCoupon.value)) {
      selectedCoupon.value = null
      useCoupon.value = false
    }
  } catch (error) {
    availableCoupons.value = []
  }
}

watch(
  () => route.query.orderId,
  async (value) => {
    await initializePage(value)
  },
  { immediate: true }
)

const handlePay = async () => {
  paying.value = true
  try {
    const res = await createPayment({
      orderId: orderId.value,
      payType: payType.value,
      couponId: useCoupon.value ? selectedCoupon.value : null,
      usePoints: usePoints.value ? pointsToUse.value : 0
    })

    paymentNo.value = res.data

    if (isZeroAmount.value) {
      ElMessage.success('应付金额为 0，模拟支付已自动完成')
      setTimeout(() => router.push('/order'), 1000)
      return
    }

    ElMessage.success('模拟支付单创建成功，请点击确认模拟支付')
  } catch (error) {
    ElMessage.error(error.message || '支付单创建失败')
  } finally {
    paying.value = false
  }
}

const handleConfirmPayment = async () => {
  if (!paymentNo.value) {
    return
  }

  confirming.value = true
  try {
    await confirmPayment(paymentNo.value)
    ElMessage.success('模拟支付已确认，订单已进入配送流程')
    router.push('/order')
  } catch (error) {
    ElMessage.error(error.message || '确认支付失败')
  } finally {
    confirming.value = false
  }
}

const formatCouponLabel = (coupon) => {
  if (Number(coupon.couponType) === 3) {
    return `${coupon.couponName || '优惠券'} - 免配送费`
  }
  if (coupon.discountAmount) {
    return `${coupon.couponName || '优惠券'} - 减 ${coupon.discountAmount}`
  }
  if (coupon.discountRate) {
    return `${coupon.couponName || '优惠券'} - 折扣 ${coupon.discountRate}`
  }
  return coupon.couponName || `优惠券 ${coupon.id}`
}

const formatAmount = (value) => Number(value || 0).toFixed(2)
</script>

<style scoped>
.payment-page__body {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6);
}

.payment-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.payment-section__head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.payment-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  padding: var(--spacing-4);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.48);
}

.payment-item__image {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  overflow: hidden;
  flex-shrink: 0;
}

.payment-item__copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
}

.payment-item__copy span {
  color: var(--color-text-muted);
}

.payment-item__total {
  color: #d9485f;
}

.payment-option {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--spacing-3);
}

.payment-option__control {
  width: min(320px, 100%);
}

.member-benefit-strip {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: var(--spacing-4);
  border: 1px solid rgba(194, 122, 44, 0.18);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(255, 247, 236, 0.86) 0%, rgba(246, 228, 200, 0.72) 100%);
}

.member-benefit-strip__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3);
  color: #8a5526;
  font-weight: 700;
}

.member-benefit-strip__items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.payment-demo-alert {
  border-radius: 16px;
}

.pay-types {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-3);
}

.payment-pending-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-3);
}

.payment-summary__discount {
  color: #2f8f63;
}

.payment-summary__reward {
  color: #c27a2c;
}

.payment-summary__total {
  color: #d9485f;
  font-size: var(--text-xl);
}

.payment-summary__submit {
  width: 100%;
  margin-top: var(--spacing-6);
}

.payment-page__empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--spacing-3);
}
</style>
