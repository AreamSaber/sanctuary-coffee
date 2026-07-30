<template>
  <div class="app-page order-confirm-page">
    <section class="app-page-header app-page-header--compact">
      <div>
        <h1 class="app-page-header__title">确认订单</h1>
      </div>
      <div class="app-page-actions">
        <el-button @click="router.push('/cart')">返回购物车</el-button>
        <el-button @click="router.push('/shop')">继续选购</el-button>
      </div>
    </section>

    <section class="page-grid page-grid--content">
      <el-card class="stack-card" shadow="never" v-loading="loading">
        <div class="stack-card__body order-confirm-page__main">
          <div class="confirm-section">
            <div class="confirm-section__head">
              <h3 class="stack-card__title">收货地址</h3>
              <el-button link type="primary" @click="router.push('/user/address')">管理地址</el-button>
            </div>

            <el-empty v-if="addressList.length === 0" description="暂无收货地址">
              <el-button type="primary" @click="router.push('/user/address')">去添加地址</el-button>
            </el-empty>

            <el-radio-group v-else v-model="selectedAddressId" class="address-list">
              <el-radio
                v-for="address in addressList"
                :key="address.id"
                :label="address.id"
                border
                class="address-option"
              >
                <div class="address-option__body">
                  <div class="address-option__head">
                    <strong>{{ address.receiverName }}</strong>
                    <span>{{ address.receiverPhone }}</span>
                    <el-tag v-if="address.isDefault === 1" type="danger" effect="plain">默认</el-tag>
                  </div>
                  <p>{{ formatAddress(address) }}</p>
                </div>
              </el-radio>
            </el-radio-group>
          </div>

          <div class="confirm-section">
            <div class="confirm-section__head">
              <h3 class="stack-card__title">商品清单</h3>
              <p class="compact-note">本页只提交已选购物车商品，确认后会生成待支付订单。</p>
            </div>

            <el-empty v-if="selectedItems.length === 0" description="暂无可结算商品">
              <el-button type="primary" @click="router.push('/cart')">返回购物车</el-button>
            </el-empty>

            <div v-else class="confirm-items">
              <div v-for="item in selectedItems" :key="item.id" class="confirm-item">
                <ProductImage :src="item.mainImage" :name="item.productName" fit="cover" class="confirm-item__image" />
                <div class="confirm-item__copy">
                  <strong>{{ item.productName }}</strong>
                  <span v-if="item.specInfo || item.skuName">{{ item.specInfo || item.skuName }}</span>
                  <span>¥{{ formatAmount(item.price) }} x {{ item.quantity }}</span>
                </div>
                <strong class="confirm-item__total">¥{{ formatAmount(item.subtotal) }}</strong>
              </div>
            </div>
          </div>

          <div class="confirm-section">
            <div class="confirm-section__head">
              <h3 class="stack-card__title">配送方式</h3>
              <p class="compact-note">选择配送方式后，订单会记录对应运费。</p>
            </div>

            <el-empty v-if="deliveryMethods.length === 0" description="暂无可用配送方式" />
            <el-radio-group v-else v-model="selectedDeliveryMethodId" class="delivery-method-list">
              <el-radio
                v-for="method in deliveryMethods"
                :key="method.id"
                :label="method.id"
                border
                class="delivery-method-option"
              >
                <div class="delivery-method-option__body">
                  <div class="delivery-method-option__head">
                    <strong>{{ method.methodName }}</strong>
                    <el-tag type="success" effect="plain">{{ formatDeliveryFreight(method) }}</el-tag>
                  </div>
                  <p>{{ method.description || '暂无配送说明' }}</p>
                  <span v-if="Number(method.freeThreshold || 0) > 0">
                    满 ¥{{ formatAmount(method.freeThreshold) }} 免基础运费
                  </span>
                </div>
              </el-radio>
            </el-radio-group>
          </div>

          <div class="confirm-section">
            <div class="confirm-section__head">
              <h3 class="stack-card__title">订单备注</h3>
              <p class="compact-note">可填写口味偏好、配送提醒等信息。</p>
            </div>
            <el-input
              v-model="remark"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="例如：少糖、请电话联系后配送"
            />
          </div>
        </div>
      </el-card>

      <el-card class="stack-card" shadow="never">
        <div class="stack-card__header">
          <div>
            <h2 class="stack-card__title">订单摘要</h2>
            <p class="compact-note">提交后进入支付页继续选择优惠券、积分和支付方式。</p>
          </div>
        </div>
        <div class="stack-card__body">
          <div class="metric-list">
            <div class="metric-list__item">
              <span>商品种类</span>
              <strong>{{ selectedItems.length }}</strong>
            </div>
            <div class="metric-list__item">
              <span>商品数量</span>
              <strong>{{ totalQuantity }}</strong>
            </div>
            <div class="metric-list__item">
              <span>商品金额</span>
              <strong class="order-confirm-page__total">¥{{ totalAmount }}</strong>
            </div>
            <div class="metric-list__item">
              <span>配送方式</span>
              <strong>{{ selectedDeliveryMethod?.methodName || '未选择' }}</strong>
            </div>
            <div class="metric-list__item">
              <span>运费</span>
              <strong>¥{{ freightAmount }}</strong>
            </div>
            <div class="metric-list__item">
              <span>订单应付预估</span>
              <strong class="order-confirm-page__total">¥{{ payablePreview }}</strong>
            </div>
          </div>

          <el-alert
            class="order-confirm-page__tip"
            type="info"
            :closable="false"
            show-icon
            title="优惠券、积分、会员折扣和促销优惠会在支付页统一展示。"
          />

          <el-button
            type="primary"
            size="large"
            class="order-confirm-page__submit"
            :loading="submitting"
            :disabled="!canSubmit"
            @click="handleSubmitOrder"
          >
            提交订单
          </el-button>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ProductImage from '@/components/common/ProductImage.vue'
import { getCartList } from '@/api/cart'
import { createOrder } from '@/api/order'
import { getAddressList } from '@/api/user'
import { getDeliveryMethods } from '@/api/delivery'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const cartList = ref([])
const addressList = ref([])
const deliveryMethods = ref([])
const selectedAddressId = ref(null)
const selectedDeliveryMethodId = ref(null)
const selectedCartIds = ref([])
const remark = ref('')

const selectedItems = computed(() => {
  const selectedIdSet = new Set(selectedCartIds.value)
  return cartList.value.filter((item) => selectedIdSet.has(Number(item.id)))
})

const totalQuantity = computed(() => selectedItems.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0))
const totalAmountNumber = computed(() => selectedItems.value.reduce((sum, item) => sum + Number(item.subtotal || 0), 0))
const totalAmount = computed(() => totalAmountNumber.value.toFixed(2))
const selectedDeliveryMethod = computed(() => deliveryMethods.value.find((item) => item.id === selectedDeliveryMethodId.value) || null)
const freightAmountNumber = computed(() => calculateFreight(selectedDeliveryMethod.value, totalAmountNumber.value))
const freightAmount = computed(() => freightAmountNumber.value.toFixed(2))
const payablePreview = computed(() => (totalAmountNumber.value + freightAmountNumber.value).toFixed(2))
const canSubmit = computed(() => selectedItems.value.length > 0 && selectedAddressId.value && selectedDeliveryMethodId.value)

onMounted(() => {
  initializePage()
})

const initializePage = async () => {
  selectedCartIds.value = parseCartIds(route.query.cartIds)
  if (selectedCartIds.value.length === 0) {
    ElMessage.warning('请选择需要结算的商品')
    router.push('/cart')
    return
  }

  loading.value = true
  try {
    await Promise.all([loadCartList(), loadAddressList(), loadDeliveryMethods()])
  } finally {
    loading.value = false
  }
}

const parseCartIds = (rawValue) => {
  if (!rawValue) return []
  return String(rawValue)
    .split(',')
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value) && value > 0)
}

const loadCartList = async () => {
  try {
    const res = await getCartList()
    cartList.value = res.data || []
    const existingIds = new Set(cartList.value.map((item) => Number(item.id)))
    selectedCartIds.value = selectedCartIds.value.filter((id) => existingIds.has(id))
    if (selectedCartIds.value.length === 0) {
      ElMessage.warning('选中的购物车商品已不存在，请重新选择')
      router.push('/cart')
    }
  } catch (error) {
    ElMessage.error(error.message || '购物车信息加载失败')
  }
}

const loadAddressList = async () => {
  try {
    const res = await getAddressList()
    addressList.value = res.data || []
    const defaultAddress = addressList.value.find((item) => item.isDefault === 1)
    selectedAddressId.value = defaultAddress?.id || addressList.value[0]?.id || null
  } catch (error) {
    ElMessage.error(error.message || '收货地址加载失败')
  }
}

const loadDeliveryMethods = async () => {
  try {
    const res = await getDeliveryMethods()
    deliveryMethods.value = res.data || []
    selectedDeliveryMethodId.value = deliveryMethods.value[0]?.id || null
  } catch (error) {
    deliveryMethods.value = []
    ElMessage.error(error.message || '配送方式加载失败')
  }
}

const handleSubmitOrder = async () => {
  if (!canSubmit.value) {
    ElMessage.warning('请确认商品和收货地址')
    return
  }

  submitting.value = true
  try {
    const res = await createOrder({
      addressId: selectedAddressId.value,
      cartIds: selectedItems.value.map((item) => item.id),
      deliveryMethodId: selectedDeliveryMethodId.value,
      remark: remark.value
    })
    ElMessage.success('订单创建成功，正在前往支付页面')
    router.push(`/payment?orderId=${res.data}`)
  } catch (error) {
    ElMessage.error(error.message || '订单创建失败')
  } finally {
    submitting.value = false
  }
}

const formatAddress = (address) => `${address.province || ''}${address.city || ''}${address.district || ''}${address.detailAddress || ''}`
const formatAmount = (value) => Number(value || 0).toFixed(2)

const calculateFreight = (method, amount) => {
  if (!method) return 0
  const freight = Number(method.freight || 0)
  const freeThreshold = Number(method.freeThreshold || 0)
  if (freeThreshold > 0 && amount >= freeThreshold) {
    return 0
  }
  return Math.max(freight, 0)
}

const formatDeliveryFreight = (method) => {
  const freight = calculateFreight(method, totalAmountNumber.value)
  if (freight === 0) return '免运费'
  return `¥${freight.toFixed(2)}`
}
</script>

<style scoped>
.order-confirm-page__main {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6);
}

.confirm-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.confirm-section__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-4);
}

.address-list,
.delivery-method-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.address-option,
.delivery-method-option {
  width: 100%;
  height: auto;
  margin-right: 0;
  padding: var(--spacing-4);
}

.address-option__body,
.delivery-method-option__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  white-space: normal;
}

.address-option__head,
.delivery-method-option__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--spacing-3);
}

.address-option__body p,
.delivery-method-option__body p {
  margin: 0;
  color: var(--color-text-secondary);
}

.delivery-method-option__body span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.confirm-items {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.confirm-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  padding: var(--spacing-4);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.48);
}

.confirm-item__image {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  overflow: hidden;
  flex-shrink: 0;
}

.confirm-item__copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.confirm-item__copy span {
  color: var(--color-text-muted);
}

.confirm-item__total,
.order-confirm-page__total {
  color: #d9485f;
}

.order-confirm-page__tip {
  margin-top: var(--spacing-5);
}

.order-confirm-page__submit {
  width: 100%;
  margin-top: var(--spacing-6);
}
</style>
