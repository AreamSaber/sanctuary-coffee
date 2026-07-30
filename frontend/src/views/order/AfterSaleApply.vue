<template>
  <div class="app-page after-sale-apply-page">
    <section class="app-page-header">
      <div>
        <p class="section-eyebrow">After-Sales Apply</p>
        <h1 class="app-page-header__title">申请售后</h1>
        <p class="section-caption">选择售后类型、填写原因说明和凭证图片，提交后可在“我的售后”查看处理进度。</p>
      </div>
      <div class="app-page-actions">
        <el-button @click="router.push('/order')">返回订单</el-button>
      </div>
    </section>

    <el-alert
      title="模拟退款说明"
      type="info"
      show-icon
      :closable="false"
      description="当前为毕设演示环境，退款类售后提交后由后台审核；审核通过后执行模拟原路退款，不调用真实第三方支付接口。"
      class="apply-alert"
    />

    <el-card class="app-panel-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="card-header__copy">
            <strong>售后申请信息</strong>
            <span>订单商品、实付金额和售后说明会同步进入后台售后管理。</span>
          </div>
          <el-tag v-if="orderDetail" type="info">订单号：{{ orderDetail.orderNo }}</el-tag>
        </div>
      </template>

      <div v-loading="loading">
        <template v-if="orderDetail">
          <el-descriptions :column="2" border class="order-summary">
            <el-descriptions-item label="订单状态">{{ orderDetail.orderStatusText || formatOrderStatusText(orderDetail.orderStatus) }}</el-descriptions-item>
            <el-descriptions-item label="实付金额">¥{{ formatAmount(orderDetail.payAmount) }}</el-descriptions-item>
            <el-descriptions-item label="收货人">{{ orderDetail.receiverName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ orderDetail.receiverPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">{{ orderDetail.receiverAddress || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider>商品明细</el-divider>
          <div class="app-table-shell goods-table">
            <el-table :data="orderDetail.items || []" border>
              <el-table-column label="商品" min-width="220">
                <template #default="{ row }">
                  <div class="goods-cell">
                    <ProductImage :src="row.productImage" :name="row.productName" fit="cover" class="goods-image" />
                    <div>
                      <div class="goods-name">{{ row.productName }}</div>
                      <div v-if="row.specInfo" class="goods-spec">{{ row.specInfo }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="price" label="单价" width="120">
                <template #default="{ row }">¥{{ formatAmount(row.price) }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="90" />
              <el-table-column prop="totalAmount" label="小计" width="120">
                <template #default="{ row }">¥{{ formatAmount(row.totalAmount) }}</template>
              </el-table-column>
            </el-table>
          </div>

          <el-divider>申请内容</el-divider>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="after-sale-form">
            <el-form-item label="售后类型" prop="type">
              <el-radio-group v-model="form.type">
                <el-radio-button :label="1">仅退款</el-radio-button>
                <el-radio-button :label="2">配送问题</el-radio-button>
                <el-radio-button :label="3">退货退款</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="退款金额" prop="refundAmount" v-if="isRefundRelated">
              <el-input-number
                v-model="form.refundAmount"
                :min="0.01"
                :max="maxRefundAmount"
                :precision="2"
                :step="1"
                controls-position="right"
              />
              <span class="form-tip">最多可退 ¥{{ formatAmount(maxRefundAmount) }}</span>
            </el-form-item>
            <el-form-item label="售后原因" prop="reason">
              <el-select v-model="form.reason" filterable allow-create placeholder="请选择或输入原因">
                <el-option v-for="item in reasonOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="详细说明" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="请补充问题描述，例如商品状态、配送异常、希望如何处理等"
              />
            </el-form-item>
            <el-form-item label="凭证图片">
              <div class="image-input-list">
                <el-input
                  v-for="(image, index) in imageInputs"
                  :key="index"
                  v-model="imageInputs[index]"
                  placeholder="填写凭证图片 URL，例如 /uploads/after-sale.jpg"
                  clearable
                >
                  <template #append>
                    <el-button @click="removeImageInput(index)">删除</el-button>
                  </template>
                </el-input>
                <el-button type="primary" plain @click="addImageInput">添加图片地址</el-button>
              </div>
              <p class="form-tip">当前项目未接入真实图片上传组件，先支持填写图片 URL 作为演示凭证。</p>
            </el-form-item>
          </el-form>
        </template>
        <el-empty v-else description="订单信息加载失败" />
      </div>

      <template #footer>
        <el-button @click="router.push('/order')">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!orderDetail" @click="handleSubmit">提交售后申请</el-button>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ProductImage from '@/components/common/ProductImage.vue'
import { getOrderDetail } from '@/api/order'
import { applyAfterSale } from '@/api/afterSale'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const orderDetail = ref(null)
const imageInputs = ref([''])

const form = reactive({
  type: 1,
  reason: '',
  description: '',
  refundAmount: 0
})

const reasonOptions = computed(() => {
  if (Number(form.type) === 2) {
    return ['配送超时', '配送员无法联系', '商品洒漏或破损', '配送地址处理异常']
  }
  if (Number(form.type) === 3) {
    return ['商品质量问题', '商品与描述不符', '收到错误商品', '需要退货退款']
  }
  return ['口味不符预期', '不想要了', '重复下单', '其他退款原因']
})

const isRefundRelated = computed(() => [1, 3].includes(Number(form.type)))
const maxRefundAmount = computed(() => Number(orderDetail.value?.payAmount || 0))

const rules = {
  type: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
  reason: [{ required: true, message: '请输入售后原因', trigger: 'blur' }],
  description: [{ max: 500, message: '售后说明不能超过500个字符', trigger: 'blur' }],
  refundAmount: [
    {
      validator: (rule, value, callback) => {
        if (!isRefundRelated.value) {
          callback()
          return
        }
        if (!value || Number(value) <= 0) {
          callback(new Error('请输入退款金额'))
          return
        }
        if (Number(value) > maxRefundAmount.value) {
          callback(new Error('退款金额不能超过订单实付金额'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

onMounted(() => {
  loadOrderDetail()
})

watch(() => form.type, () => {
  form.reason = ''
  if (!isRefundRelated.value) {
    form.refundAmount = 0
  } else if (orderDetail.value) {
    form.refundAmount = maxRefundAmount.value
  }
})

const loadOrderDetail = async () => {
  const orderId = Number(route.query.orderId)
  if (!Number.isFinite(orderId) || orderId <= 0) {
    ElMessage.error('缺少订单ID')
    router.push('/order')
    return
  }

  loading.value = true
  try {
    const res = await getOrderDetail(orderId)
    orderDetail.value = res.data
    form.refundAmount = maxRefundAmount.value
  } catch (error) {
    ElMessage.error(error.message || '订单详情加载失败')
  } finally {
    loading.value = false
  }
}

const addImageInput = () => {
  imageInputs.value.push('')
}

const removeImageInput = (index) => {
  imageInputs.value.splice(index, 1)
  if (imageInputs.value.length === 0) {
    imageInputs.value.push('')
  }
}

const handleSubmit = async () => {
  if (!orderDetail.value) {
    return
  }

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const images = imageInputs.value.map(item => item.trim()).filter(Boolean)
      await applyAfterSale({
        orderId: orderDetail.value.id,
        type: form.type,
        reason: form.reason.trim(),
        description: form.description?.trim() || form.reason.trim(),
        refundAmount: isRefundRelated.value ? form.refundAmount : 0,
        images: JSON.stringify(images)
      })
      ElMessage.success('售后申请已提交')
      router.push({ path: '/order/after-sales', query: { orderNo: orderDetail.value.orderNo } })
    } catch (error) {
      ElMessage.error(error.message || '售后申请提交失败')
    } finally {
      submitting.value = false
    }
  })
}

const formatAmount = (amount) => {
  if (amount === null || amount === undefined || amount === '') {
    return '0.00'
  }
  const numeric = Number(amount)
  return Number.isFinite(numeric) ? numeric.toFixed(2) : amount
}

const formatOrderStatusText = (status) => {
  const map = {
    1: '待付款',
    2: '待发货',
    3: '待收货',
    4: '已完成',
    5: '已取消',
    6: '退款中',
    7: '已退款'
  }
  return map[status] || '未知'
}
</script>

<style scoped>
.apply-alert {
  margin-bottom: var(--spacing-4);
  border-radius: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-4);
}

.card-header__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-header__copy span,
.form-tip,
.goods-spec {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.order-summary,
.goods-table {
  margin-bottom: var(--spacing-4);
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.goods-image {
  width: 58px;
  height: 58px;
  border-radius: 16px;
  overflow: hidden;
  flex-shrink: 0;
}

.goods-name {
  font-weight: var(--font-bold);
  color: var(--color-text);
}

.after-sale-form {
  max-width: 820px;
}

.after-sale-form :deep(.el-select),
.after-sale-form :deep(.el-textarea),
.after-sale-form :deep(.el-input-number) {
  width: 100%;
}

.image-input-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
  width: 100%;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
