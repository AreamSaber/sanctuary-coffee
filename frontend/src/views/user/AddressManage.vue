<template>
  <div class="app-page address-manage-page">
    <section class="app-page-header app-page-header--compact">
      <div>
        <h1 class="app-page-header__title">地址管理</h1>
      </div>
      <div class="app-page-actions">
        <el-button @click="router.push('/user')">返回个人中心</el-button>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加地址
        </el-button>
      </div>
    </section>

    <el-card class="stack-card" shadow="never">
      <div class="stack-card__body">
        <el-empty v-if="addressList.length === 0" description="暂无收货地址">
          <template #image>
            <el-icon :size="56"><Location /></el-icon>
          </template>
          <el-button type="primary" @click="handleAdd">添加新地址</el-button>
        </el-empty>

        <div v-else class="address-grid">
          <article
            v-for="item in addressList"
            :key="item.id"
            class="address-card"
            :class="{ 'address-card--default': item.isDefault === 1 }"
          >
            <div class="address-card__head">
              <div>
                <strong>{{ item.receiverName }}</strong>
                <span>{{ item.receiverPhone }}</span>
              </div>
              <el-tag v-if="item.isDefault === 1" type="danger" effect="dark">默认</el-tag>
            </div>

            <p class="address-card__body">
              {{ item.province }} {{ item.city }} {{ item.district }} {{ item.detailAddress }}
            </p>

            <div class="address-card__actions">
              <el-button type="primary" plain @click="handleEdit(item)">编辑</el-button>
              <el-button type="danger" plain @click="handleDelete(item.id)">删除</el-button>
              <el-button v-if="item.isDefault !== 1" plain @click="handleSetDefault(item.id)">
                设为默认
              </el-button>
            </div>
          </article>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="min(560px, 92vw)">
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="100px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>

        <el-form-item label="联系电话" prop="receiverPhone">
          <el-input v-model="addressForm.receiverPhone" placeholder="请输入联系电话" />
        </el-form-item>

        <el-form-item label="省份" prop="province">
          <el-input v-model="addressForm.province" placeholder="请输入省份" />
        </el-form-item>

        <el-form-item label="城市" prop="city">
          <el-input v-model="addressForm.city" placeholder="请输入城市" />
        </el-form-item>

        <el-form-item label="区/县" prop="district">
          <el-input v-model="addressForm.district" placeholder="请输入区/县" />
        </el-form-item>

        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="addressForm.detailAddress"
            type="textarea"
            :rows="3"
            placeholder="请输入详细地址"
          />
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch v-model="isDefault" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Plus } from '@element-plus/icons-vue'
import { addAddress, deleteAddress, getAddressList, setDefaultAddress, updateAddress } from '@/api/user'

const router = useRouter()
const addressList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加地址')
const loading = ref(false)
const isDefault = ref(false)
const addressFormRef = ref()

const addressForm = reactive({
  id: null,
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: ''
})

const addressRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区/县', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

onMounted(() => {
  loadAddressList()
})

const loadAddressList = async () => {
  try {
    const res = await getAddressList()
    addressList.value = res.data || []
  } catch (error) {
    console.error('获取地址列表失败:', error)
  }
}

const handleAdd = () => {
  dialogTitle.value = '添加地址'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (item) => {
  dialogTitle.value = '编辑地址'
  Object.assign(addressForm, item)
  isDefault.value = item.isDefault === 1
  dialogVisible.value = true
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该地址吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteAddress(id)
    ElMessage.success('删除成功')
    loadAddressList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSetDefault = async (id) => {
  try {
    await setDefaultAddress(id)
    ElMessage.success('设置成功')
    loadAddressList()
  } catch (error) {
    console.error('设置默认地址失败:', error)
  }
}

const handleSubmit = async () => {
  await addressFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    loading.value = true
    try {
      const payload = {
        ...addressForm,
        isDefault: isDefault.value ? 1 : 0
      }

      if (addressForm.id) {
        await updateAddress(payload)
        ElMessage.success('更新成功')
      } else {
        await addAddress(payload)
        ElMessage.success('添加成功')
      }

      dialogVisible.value = false
      loadAddressList()
    } catch (error) {
      console.error('保存地址失败:', error)
    } finally {
      loading.value = false
    }
  })
}

const resetForm = () => {
  Object.assign(addressForm, {
    id: null,
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: ''
  })
  isDefault.value = false
  addressFormRef.value?.clearValidate()
}
</script>

<style scoped>
.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-4);
}

.address-card {
  padding: var(--spacing-5);
  border-radius: 24px;
  border: 1px solid rgba(107, 101, 91, 0.08);
  background: rgba(255, 255, 255, 0.5);
}

.address-card--default {
  border-color: rgba(199, 98, 79, 0.28);
  box-shadow: inset 0 0 0 1px rgba(199, 98, 79, 0.08);
}

.address-card__head {
  display: flex;
  justify-content: space-between;
  gap: var(--spacing-4);
  align-items: flex-start;
}

.address-card__head div {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.address-card__body {
  margin: var(--spacing-4) 0;
  color: var(--color-text-secondary);
}

.address-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-3);
}
</style>
