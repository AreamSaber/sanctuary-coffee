<template>
  <div class="app-page shopping-cart-page">
    <section class="app-page-header app-page-header--compact">
      <div>
        <h1 class="app-page-header__title">购物车</h1>
      </div>
      <div class="app-page-actions">
        <el-button type="danger" plain @click="handleClearCart">清空购物车</el-button>
      </div>
    </section>

    <section class="page-grid page-grid--content">
      <el-card class="stack-card" shadow="never">
        <div class="stack-card__body">
          <el-empty v-if="cartList.length === 0" description="购物车为空">
            <el-button type="primary" @click="router.push('/shop')">去选购</el-button>
          </el-empty>

          <div v-else class="app-table-shell">
            <el-table :data="cartList">
              <el-table-column label="商品" min-width="320">
                <template #default="{ row }">
                  <div class="product-info">
                    <ProductImage :src="row.mainImage" :name="row.productName" fit="cover" class="product-info__image" />
                    <div class="product-info__copy">
                      <strong>{{ row.productName }}</strong>
                      <span v-if="row.specInfo || row.skuName">{{ row.specInfo || row.skuName }}</span>
                      <span>库存 {{ row.stock }}</span>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="单价" width="120">
                <template #default="{ row }">
                  <span class="price">¥{{ row.price }}</span>
                </template>
              </el-table-column>
              <el-table-column label="数量" width="150">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.quantity"
                    :min="1"
                    :max="row.stock"
                    @change="handleQuantityChange(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="小计" width="120">
                <template #default="{ row }">
                  <span class="price">¥{{ row.subtotal }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button link type="danger" @click="handleRemove(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-card>

      <el-card class="stack-card" shadow="never">
        <div class="stack-card__header">
          <div>
            <h2 class="stack-card__title">结算摘要</h2>
            <p class="compact-note">先进入订单确认页选择地址和填写备注，再提交订单并进入支付。</p>
          </div>
        </div>
        <div class="stack-card__body">
          <div class="metric-list">
            <div class="metric-list__item">
              <span>商品数量</span>
              <strong>{{ cartList.length }}</strong>
            </div>
            <div class="metric-list__item">
              <span>合计金额</span>
              <strong class="price">¥{{ totalAmount }}</strong>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            class="shopping-cart-page__submit"
            :disabled="cartList.length === 0"
            @click="handleCheckout"
          >
            去结算
          </el-button>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProductImage from '@/components/common/ProductImage.vue'
import { clearCart, getCartList, removeFromCart, updateQuantity } from '@/api/cart'

const router = useRouter()
const cartList = ref([])

const totalAmount = computed(() =>
  cartList.value.reduce((sum, item) => sum + parseFloat(item.subtotal || 0), 0).toFixed(2)
)

onMounted(() => {
  loadCartList()
})

const loadCartList = async () => {
  try {
    const res = await getCartList()
    cartList.value = res.data || []
  } catch (error) {
    console.error('加载购物车失败:', error)
  }
}

const handleQuantityChange = async (row) => {
  try {
    await updateQuantity(row.id, row.quantity)
    loadCartList()
  } catch (error) {
    console.error('更新数量失败:', error)
  }
}

const handleRemove = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await removeFromCart(id)
    ElMessage.success('删除成功')
    loadCartList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleClearCart = async () => {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await clearCart()
    ElMessage.success('清空成功')
    loadCartList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空失败:', error)
    }
  }
}

const handleCheckout = () => {
  if (cartList.value.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }

  const cartIds = cartList.value.map((item) => item.id).join(',')
  router.push(`/order/confirm?cartIds=${cartIds}`)
}
</script>

<style scoped>
.product-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
}

.product-info__image {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  overflow: hidden;
  flex-shrink: 0;
}

.product-info__copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.product-info__copy span {
  color: var(--color-text-muted);
}

.price {
  color: #d9485f;
  font-weight: var(--font-bold);
}

.shopping-cart-page__submit {
  width: 100%;
  margin-top: var(--spacing-6);
}
</style>
