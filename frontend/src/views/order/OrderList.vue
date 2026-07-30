<template>
  <div class="app-page order-list-page">
    <section class="app-page-header">
      <div>
        <h1 class="app-page-header__title">我的订单</h1>
      </div>
      <div class="app-page-actions">
        <el-button @click="router.push('/financial/invoice')">发票中心</el-button>
      </div>
    </section>

    <el-card class="app-panel-card order-list-card">
      <template #header>
        <div class="card-header">
          <strong>订单状态</strong>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="order-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待支付" name="1" />
        <el-tab-pane label="待发货" name="2" />
        <el-tab-pane label="待收货" name="3" />
        <el-tab-pane label="已完成" name="4" />
        <el-tab-pane label="退款中" name="6" />
        <el-tab-pane label="已退款" name="7" />
      </el-tabs>

      <div v-loading="loading" class="order-list">
        <el-empty v-if="orderList.length === 0" description="暂无订单" />

        <template v-else>
          <article v-for="order in orderList" :key="order.id" class="order-item">
            <header class="order-header">
              <div class="order-header__main">
                <strong class="order-no">订单号：{{ order.orderNo }}</strong>
                <span class="order-time">{{ order.createTime }}</span>
              </div>
              <el-tag :type="getStatusType(order.orderStatus)">
                {{ order.orderStatusText }}
              </el-tag>
            </header>

            <div class="order-body">
              <div class="order-goods">
                <div v-for="item in order.items" :key="item.id" class="goods-item">
                  <ProductImage :src="item.productImage" :name="item.productName" fit="cover" class="goods-image" />
                  <div class="goods-info">
                    <div class="goods-name">{{ item.productName }}</div>
                    <div v-if="item.specInfo" class="goods-spec">{{ item.specInfo }}</div>
                    <div class="goods-price-row">
                      <span class="goods-price">¥{{ item.price }}</span>
                      <span class="quantity">x{{ item.quantity }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="order-amount">
                <span class="order-amount__label">实付金额</span>
                <strong class="order-amount__value">¥{{ order.payAmount }}</strong>
              </div>

              <div class="order-actions">
                <el-button
                  v-if="order.orderStatus === 1"
                  type="primary"
                  size="small"
                  @click="handlePay(order)"
                >
                  去支付
                </el-button>
                <el-button v-if="order.orderStatus === 1" size="small" @click="handleCancel(order.id)">
                  取消订单
                </el-button>
                <el-button
                  v-if="order.orderStatus === 3"
                  type="primary"
                  size="small"
                  @click="handleConfirm(order.id)"
                >
                  确认收货
                </el-button>
                <el-button v-if="order.orderStatus === 3" size="small" @click="handleViewTracking(order.id)">
                  查看物流
                </el-button>
                <el-button
                  v-if="canApplyInvoice(order)"
                  size="small"
                  type="warning"
                  @click="openInvoiceDialog(order)"
                >
                  申请发票
                </el-button>
                <el-button
                  v-if="canApplyRefund(order)"
                  size="small"
                  type="danger"
                  plain
                  @click="handleApplyAfterSale(order)"
                >
                  申请售后
                </el-button>
                <el-button
                  v-if="[6, 7].includes(order.orderStatus)"
                  size="small"
                  type="warning"
                  plain
                  @click="handleViewAfterSale(order)"
                >
                  售后进度
                </el-button>
                <el-button v-if="order.orderStatus === 4" size="small" @click="handleReview(order)">
                  评价
                </el-button>
                <el-button
                  v-if="order.orderStatus === 4 || order.orderStatus === 5"
                  size="small"
                  @click="handleDelete(order.id)"
                >
                  删除
                </el-button>
                <el-button size="small" @click="handleViewDetail(order.id)">查看详情</el-button>
              </div>
            </div>
          </article>

          <div class="app-pagination">
            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next"
              @current-change="loadData"
            />
          </div>
        </template>
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="min(840px, 92vw)">
      <div v-if="currentOrder" class="order-detail">
        <el-descriptions :column="2" border class="order-detail__meta">
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(currentOrder.orderStatus)">
              {{ currentOrder.orderStatusText }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ currentOrder.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ currentOrder.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">
            {{ currentOrder.receiverAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="订单备注" :span="2">
            {{ currentOrder.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>商品明细</el-divider>

        <div class="app-table-shell">
          <el-table :data="currentOrder.items" border>
            <el-table-column prop="productName" label="商品名称" />
            <el-table-column prop="specInfo" label="规格" width="140" />
            <el-table-column prop="price" label="单价" width="120">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="90" />
            <el-table-column prop="totalAmount" label="小计" width="120">
              <template #default="{ row }">¥{{ row.totalAmount }}</template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="invoiceDialogVisible" title="申请发票" width="min(600px, 92vw)" @closed="resetInvoiceForm">
      <el-form ref="invoiceFormRef" :model="invoiceForm" :rules="invoiceRules" label-width="100px">
        <el-form-item label="订单号">
          <el-input :model-value="invoiceTarget?.orderNo || ''" disabled />
        </el-form-item>
        <el-form-item label="发票类型" prop="invoiceType">
          <el-select v-model="invoiceForm.invoiceType" style="width: 100%">
            <el-option label="普通发票" :value="1" />
            <el-option label="增值税发票" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="抬头类型" prop="type">
          <el-radio-group v-model="invoiceForm.type">
            <el-radio label="PERSONAL">个人</el-radio>
            <el-radio label="COMPANY">企业</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发票抬头" prop="title">
          <el-input v-model="invoiceForm.title" />
        </el-form-item>
        <el-form-item v-if="invoiceForm.type === 'COMPANY'" label="税号" prop="taxNumber">
          <el-input v-model="invoiceForm.taxNumber" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="invoiceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitInvoice">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProductImage from '@/components/common/ProductImage.vue'
import { applyInvoice } from '@/api/financial'
import { cancelOrder, confirmReceipt, deleteOrder, getOrderDetail, getOrderPage } from '@/api/order'
import { checkCanReview } from '@/api/review'

const router = useRouter()

const loading = ref(false)
const activeTab = ref('all')
const orderList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailVisible = ref(false)
const currentOrder = ref(null)

const invoiceDialogVisible = ref(false)
const invoiceTarget = ref(null)
const invoiceFormRef = ref()
const invoiceForm = reactive({
  invoiceType: 1,
  type: 'PERSONAL',
  title: '',
  taxNumber: ''
})

const invoiceRules = {
  invoiceType: [{ required: true, message: '请选择发票类型', trigger: 'change' }],
  type: [{ required: true, message: '请选择抬头类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入发票抬头', trigger: 'blur' }],
  taxNumber: [
    {
      validator: (rule, value, callback) => {
        if (invoiceForm.type === 'COMPANY' && !value) {
          callback(new Error('请输入税号'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }

    if (activeTab.value !== 'all') {
      params.orderStatus = Number(activeTab.value)
    }

    const res = await getOrderPage(params)
    orderList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('订单列表加载失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = async () => {
  pageNum.value = 1
  await loadData()
}

const handlePay = (order) => {
  router.push(`/payment?orderId=${order.id}`)
}

const handleCancel = async (orderId) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单')
    await cancelOrder(orderId, value)
    ElMessage.success('订单已取消')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消订单失败')
    }
  }
}

const handleConfirm = async (orderId) => {
  try {
    await ElMessageBox.confirm('确认已收到商品吗？', '提示', { type: 'warning' })
    await confirmReceipt(orderId)
    ElMessage.success('确认收货成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('确认收货失败')
    }
  }
}

const handleDelete = async (orderId) => {
  try {
    await ElMessageBox.confirm('确认删除这笔订单吗？', '提示', { type: 'warning' })
    await deleteOrder(orderId)
    ElMessage.success('订单已删除')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除订单失败')
    }
  }
}

const handleApplyAfterSale = (order) => {
  router.push({
    path: '/order/after-sale/apply',
    query: { orderId: order.id }
  })
}

const handleReview = (order) => {
  if (!order.items?.length) {
    ElMessage.warning('订单商品信息加载失败')
    return
  }
  handleReviewableItem(order)
}

const handleReviewableItem = async (order) => {
  try {
    for (const item of order.items) {
      const res = await checkCanReview({
        orderId: order.id,
        productId: item.productId
      })

      if (res.data) {
        router.push(`/review/add?orderId=${order.id}&productId=${item.productId}&itemId=${item.id}`)
        return
      }
    }

    ElMessage.info('该订单暂无可评价商品')
  } catch (error) {
    ElMessage.error('暂时无法发起评价')
  }
}

const handleViewTracking = (orderId) => {
  router.push(`/delivery/tracking?orderId=${orderId}`)
}

const handleViewAfterSale = (order) => {
  router.push({
    path: '/order/after-sales',
    query: { orderNo: order.orderNo }
  })
}

const handleViewDetail = async (orderId) => {
  try {
    const res = await getOrderDetail(orderId)
    currentOrder.value = res.data
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('订单详情加载失败')
  }
}

const canApplyInvoice = (order) => {
  return order.payStatus === 1 && ![6, 7].includes(order.orderStatus)
}

const canApplyRefund = (order) => {
  return order.payStatus === 1 && [2, 3, 4].includes(order.orderStatus)
}

const openInvoiceDialog = (order) => {
  invoiceTarget.value = order
  invoiceDialogVisible.value = true
}

const submitInvoice = async () => {
  await invoiceFormRef.value.validate(async (valid) => {
    if (!valid || !invoiceTarget.value) return

    try {
      await applyInvoice(invoiceTarget.value.id, {
        invoiceType: invoiceForm.invoiceType,
        type: invoiceForm.type,
        title: invoiceForm.title,
        taxNumber: invoiceForm.type === 'COMPANY' ? invoiceForm.taxNumber : null
      })
      ElMessage.success('发票申请已提交')
      invoiceDialogVisible.value = false
      router.push('/financial/invoice')
    } catch (error) {
      ElMessage.error(error.message || '发票申请失败')
    }
  })
}

const resetInvoiceForm = () => {
  invoiceTarget.value = null
  Object.assign(invoiceForm, {
    invoiceType: 1,
    type: 'PERSONAL',
    title: '',
    taxNumber: ''
  })
  invoiceFormRef.value?.clearValidate()
}

const getStatusType = (status) => {
  const typeMap = {
    1: 'warning',
    2: 'primary',
    3: 'primary',
    4: 'success',
    5: 'info',
    6: 'warning',
    7: 'success'
  }
  return typeMap[status] || 'info'
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-4);
}

.card-header__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-header__copy strong {
  font-size: var(--text-lg);
}

.card-header__copy span {
  color: var(--color-text-muted);
}

.order-tabs :deep(.el-tabs__nav-wrap) {
  overflow-x: auto;
}

.order-list {
  min-height: 320px;
}

.order-item {
  border: 1px solid rgba(111, 78, 55, 0.1);
  border-radius: 24px;
  margin-bottom: var(--spacing-4);
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #fffaf4 100%);
}

.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-4);
  padding: var(--spacing-4) var(--spacing-5);
  border-bottom: 1px solid rgba(111, 78, 55, 0.08);
  background: rgba(111, 78, 55, 0.04);
}

.order-header__main {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--spacing-3);
}

.order-no {
  font-size: var(--text-base);
}

.order-time {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.order-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 140px 220px;
  gap: var(--spacing-5);
  padding: var(--spacing-5);
  align-items: start;
}

.goods-item {
  display: flex;
  gap: var(--spacing-4);
  padding: var(--spacing-3) 0;
}

.goods-item + .goods-item {
  border-top: 1px solid rgba(111, 78, 55, 0.06);
}

.goods-image {
  width: 84px;
  height: 84px;
  border-radius: 18px;
  overflow: hidden;
  flex-shrink: 0;
}

.goods-info {
  min-width: 0;
}

.goods-name {
  font-weight: var(--font-bold);
  color: var(--color-text);
}

.goods-spec {
  margin-top: 6px;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.goods-price-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-top: 10px;
}

.goods-price {
  color: #d9485f;
  font-weight: var(--font-bold);
}

.quantity {
  color: var(--color-text-muted);
}

.order-amount {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  min-height: 100%;
  padding-left: var(--spacing-4);
  border-left: 1px solid rgba(111, 78, 55, 0.08);
}

.order-amount__label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.order-amount__value {
  font-size: var(--text-2xl);
  color: #d9485f;
}

.order-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-content: flex-start;
  gap: 10px;
  padding-left: var(--spacing-4);
  border-left: 1px solid rgba(111, 78, 55, 0.08);
}

.order-detail {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

@media (max-width: 1024px) {
  .order-body {
    grid-template-columns: 1fr;
  }

  .order-amount,
  .order-actions {
    padding-left: 0;
    border-left: none;
    border-top: 1px solid rgba(111, 78, 55, 0.08);
    padding-top: var(--spacing-4);
  }

  .order-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .order-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .goods-item {
    gap: var(--spacing-3);
  }

  .goods-image {
    width: 72px;
    height: 72px;
  }
}
</style>
