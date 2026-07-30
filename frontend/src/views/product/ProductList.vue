<template>
  <div class="product-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>商品管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            添加商品
          </el-button>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="商品名称/编码"
            clearable
            style="width: 200px"
            @clear="handleSearch"
          />
        </el-form-item>
        
        <el-form-item label="分类">
          <el-select
            v-model="searchForm.categoryId"
            placeholder="请选择分类"
            clearable
            style="width: 200px"
            @clear="handleSearch"
          >
            <el-option
              v-for="category in flatCategories"
              :key="category.id"
              :label="category.categoryName"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            clearable
            style="width: 120px"
            @clear="handleSearch"
          >
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 商品列表 -->
      <el-table
        :data="tableData"
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="商品图片" width="100">
          <template #default="{ row }">
            <ProductImage
              :src="row.mainImage"
              :name="row.productName"
              :preview="true"
              fit="cover"
              style="width: 60px; height: 60px"
            />
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="150" />
        <el-table-column prop="productCode" label="商品编码" width="120" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="规格" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.hasSku" type="warning" size="small">
              SKU {{ row.skuList?.length || 0 }}
            </el-tag>
            <el-tag v-else type="info" size="small" effect="plain">单规格</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.isHot === 1" size="small" type="danger">热门</el-tag>
            <el-tag v-if="row.isNew === 1" size="small" type="success">新品</el-tag>
            <el-tag v-if="row.isRecommend === 1" size="small" type="warning">推荐</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="searchForm.pageNum"
        v-model:page-size="searchForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
    
    <!-- 商品表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="min(1080px, 94vw)"
      @close="resetForm"
    >
      <el-form
        ref="productFormRef"
        :model="productForm"
        :rules="productRules"
        label-width="100px"
      >
        <el-form-item label="商品名称" prop="productName">
          <el-input v-model="productForm.productName" placeholder="请输入商品名称" />
        </el-form-item>
        
        <el-form-item label="商品编码" prop="productCode">
          <el-input v-model="productForm.productCode" placeholder="请输入商品编码" />
        </el-form-item>
        
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="productForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="category in flatCategories"
              :key="category.id"
              :label="category.categoryName"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="商品价格" prop="price">
          <el-input-number
            v-model="productForm.price"
            :min="0.01"
            :precision="2"
            :step="0.1"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="原价">
          <el-input-number
            v-model="productForm.originalPrice"
            :min="0"
            :precision="2"
            :step="0.1"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="库存" prop="stock">
          <el-input-number
            v-model="productForm.stock"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="单位">
          <el-input v-model="productForm.unit" placeholder="杯/份" />
        </el-form-item>
        
        <el-form-item label="商品描述">
          <el-input
            v-model="productForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品描述"
          />
        </el-form-item>

        <el-form-item label="商品主图">
          <el-input
            v-model="productForm.mainImage"
            placeholder="请输入图片 URL，留空则显示默认封面"
            clearable
          />
        </el-form-item>

        <el-form-item label="本地上传">
          <div class="product-image-upload">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :before-upload="beforeProductImageUpload"
              :http-request="handleProductImageUpload"
            >
              <el-button :loading="productImageUploading" type="primary" plain>上传本地图片</el-button>
            </el-upload>
            <span class="product-image-upload__tip">支持 JPG/PNG/GIF/WEBP/BMP，单张不超过 10MB</span>
          </div>
        </el-form-item>

        <el-form-item v-if="productForm.mainImage" label="主图预览">
          <ProductImage
            :src="productForm.mainImage"
            :name="productForm.productName"
            :preview="true"
            fit="cover"
            class="product-form-preview"
          />
        </el-form-item>
        
        <el-form-item label="商品标签">
          <el-checkbox v-model="productForm.isHot" :true-label="1" :false-label="0">热门</el-checkbox>
          <el-checkbox v-model="productForm.isNew" :true-label="1" :false-label="0">新品</el-checkbox>
          <el-checkbox v-model="productForm.isRecommend" :true-label="1" :false-label="0">推荐</el-checkbox>
        </el-form-item>
        
        <el-form-item label="商品状态">
          <el-radio-group v-model="productForm.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>

        <section class="sku-editor">
          <div class="sku-editor__header">
            <div>
              <h3>规格与 SKU</h3>
              <p>配置多规格后，商城会要求用户选择 SKU；保存时商品价格和库存会按可用 SKU 自动汇总。</p>
            </div>
            <el-button type="primary" plain @click="addSpecRow">新增规格</el-button>
          </div>

          <el-table :data="productForm.specList" border class="sku-editor__table">
            <el-table-column label="规格名称" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.specName" placeholder="如：杯型" />
              </template>
            </el-table-column>
            <el-table-column label="规格值" min-width="260">
              <template #default="{ row }">
                <el-input v-model="row.specValues" placeholder="用逗号分隔，如：中杯,大杯" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" @click="removeSpecRow($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!productForm.specList.length" description="暂无规格，可直接按商品库存销售" />

          <div class="sku-editor__header sku-editor__header--sub">
            <div>
              <h3>SKU 明细</h3>
              <p>每个 SKU 可独立维护编码、价格、库存、图片和启停状态。</p>
            </div>
            <div class="sku-editor__actions">
              <el-button plain @click="generateSkuRows">按规格生成 SKU</el-button>
              <el-button type="primary" plain @click="addSkuRow">新增 SKU</el-button>
            </div>
          </div>

          <el-table :data="productForm.skuList" border class="sku-editor__table">
            <el-table-column label="SKU编码" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.skuCode" placeholder="唯一编码" />
              </template>
            </el-table-column>
            <el-table-column label="SKU名称" min-width="170">
              <template #default="{ row }">
                <el-input v-model="row.skuName" placeholder="展示名称" />
              </template>
            </el-table-column>
            <el-table-column label="规格快照" min-width="210">
              <template #default="{ row }">
                <el-input v-model="row.specInfo" placeholder="如：杯型=大杯；甜度=半糖" />
              </template>
            </el-table-column>
            <el-table-column label="价格" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.price" :min="0.01" :precision="2" :step="0.1" />
              </template>
            </el-table-column>
            <el-table-column label="库存" width="130">
              <template #default="{ row }">
                <el-input-number v-model="row.stock" :min="0" />
              </template>
            </el-table-column>
            <el-table-column label="图片 URL" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.image" placeholder="留空使用商品主图" clearable />
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.status" :active-value="1" :inactive-value="0" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" @click="removeSkuRow($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!productForm.skuList.length" description="暂无 SKU，商品将按基础价格和库存销售" />
        </section>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProductImage from '@/components/common/ProductImage.vue'
import { uploadImage } from '@/api/file'
import {
  getProductPage,
  addProduct,
  updateProduct,
  deleteProduct,
  onShelf,
  offShelf,
  getCategoryTree
} from '@/api/product'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('添加商品')
const tableData = ref([])
const total = ref(0)
const productFormRef = ref()
const flatCategories = ref([])
const productImageUploading = ref(false)

const searchForm = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: null,
  status: null
})

const productForm = reactive({
  id: null,
  productName: '',
  productCode: '',
  categoryId: null,
  description: '',
  mainImage: '',
  price: null,
  originalPrice: null,
  stock: 0,
  unit: '杯',
  status: 1,
  isHot: 0,
  isNew: 0,
  isRecommend: 0,
  specList: [],
  skuList: []
})

const productRules = {
  productName: [
    { required: true, message: '请输入商品名称', trigger: 'blur' }
  ],
  productCode: [
    { required: true, message: '请输入商品编码', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '请输入库存', trigger: 'blur' }
  ]
}

const normalizeOptionalValue = (value) => {
  const normalized = value?.trim()
  return normalized ? normalized : null
}

const createSpecRow = (data = {}) => ({
  id: data.id || null,
  specName: data.specName || '',
  specValues: data.specValues || ''
})

const createSkuRow = (data = {}) => ({
  id: data.id || null,
  skuCode: data.skuCode || '',
  skuName: data.skuName || '',
  specInfo: data.specInfo || '',
  price: data.price ?? productForm.price ?? 0.01,
  stock: data.stock ?? 0,
  image: data.image || '',
  status: data.status ?? 1
})

const hasSpecContent = (item) => {
  return Boolean(item?.specName?.trim() || item?.specValues?.trim())
}

const hasSkuContent = (item) => {
  return Boolean(item?.skuCode?.trim() || item?.skuName?.trim() || item?.specInfo?.trim())
}

const normalizeSpecList = () => {
  return productForm.specList
    .filter(hasSpecContent)
    .map((item) => ({
      id: item.id || null,
      specName: item.specName.trim(),
      specValues: item.specValues.trim()
    }))
}

const normalizeSkuList = () => {
  return productForm.skuList
    .filter(hasSkuContent)
    .map((item) => ({
      id: item.id || null,
      skuCode: item.skuCode.trim(),
      skuName: item.skuName.trim(),
      specInfo: normalizeOptionalValue(item.specInfo),
      price: item.price,
      stock: item.stock,
      image: normalizeOptionalValue(item.image),
      status: item.status ?? 1
    }))
}

const splitSpecValues = (value) => {
  return String(value || '')
    .split(/[,\n，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const addSpecRow = () => {
  productForm.specList.push(createSpecRow())
}

const removeSpecRow = (index) => {
  productForm.specList.splice(index, 1)
}

const addSkuRow = () => {
  productForm.skuList.push(createSkuRow())
}

const removeSkuRow = (index) => {
  productForm.skuList.splice(index, 1)
}

const buildSkuCode = (index) => {
  const baseCode = normalizeOptionalValue(productForm.productCode)?.replace(/\s+/g, '-').toUpperCase() || 'SKU'
  return `${baseCode}-${String(index + 1).padStart(2, '0')}`
}

const buildSpecCombinations = (specList) => {
  return specList.reduce((combinations, spec) => {
    const values = splitSpecValues(spec.specValues)
    return combinations.flatMap((combination) =>
      values.map((value) => [
        ...combination,
        { name: spec.specName, value }
      ])
    )
  }, [[]])
}

const generateSkuRows = async () => {
  const specs = normalizeSpecList()
  if (!specs.length) {
    ElMessage.warning('请先添加规格定义')
    return
  }

  const combinations = buildSpecCombinations(specs)
  if (!combinations.length) {
    ElMessage.warning('请填写规格值')
    return
  }

  if (productForm.skuList.length) {
    try {
      await ElMessageBox.confirm('按规格生成会覆盖当前 SKU 明细，是否继续？', '提示', {
        confirmButtonText: '继续生成',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch (error) {
      return
    }
  }

  productForm.skuList = combinations.map((combination, index) => {
    const specInfo = combination.map((item) => `${item.name}=${item.value}`).join('；')
    const skuName = [productForm.productName, combination.map((item) => item.value).join('/')].filter(Boolean).join(' ')
    return createSkuRow({
      skuCode: buildSkuCode(index),
      skuName,
      specInfo,
      price: productForm.price ?? 0.01,
      stock: 0,
      image: productForm.mainImage,
      status: 1
    })
  })
}

const validateSkuConfig = () => {
  const specList = normalizeSpecList()
  const skuList = normalizeSkuList()

  for (const spec of productForm.specList.filter(hasSpecContent)) {
    if (!spec.specName?.trim() || !spec.specValues?.trim()) {
      ElMessage.warning('规格名称和规格值都不能为空')
      return false
    }
  }

  if (specList.length > 0 && skuList.length === 0) {
    ElMessage.warning('已配置规格，请至少维护一个 SKU')
    return false
  }

  const skuCodes = new Set()
  for (const sku of productForm.skuList.filter(hasSkuContent)) {
    if (!sku.skuCode?.trim() || !sku.skuName?.trim()) {
      ElMessage.warning('SKU编码和SKU名称不能为空')
      return false
    }
    if (!sku.price || Number(sku.price) <= 0) {
      ElMessage.warning(`SKU ${sku.skuCode || sku.skuName} 的价格必须大于0`)
      return false
    }
    if (sku.stock == null || Number(sku.stock) < 0) {
      ElMessage.warning(`SKU ${sku.skuCode || sku.skuName} 的库存不能小于0`)
      return false
    }
    const normalizedCode = sku.skuCode.trim()
    if (skuCodes.has(normalizedCode)) {
      ElMessage.warning(`SKU编码重复：${normalizedCode}`)
      return false
    }
    skuCodes.add(normalizedCode)
  }

  return true
}

const buildProductPayload = () => {
  const specList = normalizeSpecList()
  const skuList = normalizeSkuList()
  const payload = {
    ...productForm,
    mainImage: normalizeOptionalValue(productForm.mainImage),
    unit: normalizeOptionalValue(productForm.unit) || '杯',
    specList,
    skuList
  }

  if (skuList.length) {
    payload.price = skuList
      .map((item) => Number(item.price))
      .reduce((min, price) => Math.min(min, price), Number(skuList[0].price))
    payload.stock = skuList.reduce((sum, item) => sum + Number(item.stock || 0), 0)
  }

  return payload
}

const beforeProductImageUpload = (file) => {
  if (!file.type?.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return false
  }

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }

  return true
}

const handleProductImageUpload = async (option) => {
  productImageUploading.value = true
  try {
    const res = await uploadImage(option.file, 'product')
    productForm.mainImage = res.data.url
    ElMessage.success('图片上传成功')
    option.onSuccess?.(res.data)
  } catch (error) {
    option.onError?.(error)
  } finally {
    productImageUploading.value = false
  }
}

onMounted(() => {
  loadData()
  loadCategories()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getProductPage(searchForm)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('加载商品列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategoryTree()
    flatCategories.value = flattenTree(res.data)
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const flattenTree = (tree, result = []) => {
  tree.forEach(node => {
    result.push({
      id: node.id,
      categoryName: '  '.repeat(node.level - 1) + node.categoryName
    })
    if (node.children && node.children.length > 0) {
      flattenTree(node.children, result)
    }
  })
  return result
}

const handleSearch = () => {
  searchForm.pageNum = 1
  loadData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    categoryId: null,
    status: null
  })
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '添加商品'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑商品'
  Object.assign(productForm, {
    ...row,
    mainImage: row.mainImage || '',
    specList: (row.specList || []).map((item) => createSpecRow(item)),
    skuList: (row.skuList || []).map((item) => createSkuRow(item))
  })
  dialogVisible.value = true
}

const handleToggleStatus = async (row) => {
  try {
    if (row.status === 1) {
      await offShelf(row.id)
      ElMessage.success('下架成功')
    } else {
      await onShelf(row.id)
      ElMessage.success('上架成功')
    }
    loadData()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteProduct(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSubmit = async () => {
  await productFormRef.value.validate(async (valid) => {
    if (valid) {
      if (!validateSkuConfig()) {
        return
      }

      submitLoading.value = true
      try {
        const payload = buildProductPayload()
        if (productForm.id) {
          await updateProduct(payload)
          ElMessage.success('更新成功')
        } else {
          await addProduct(payload)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error) {
        console.error('保存失败:', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const resetForm = () => {
  Object.assign(productForm, {
    id: null,
    productName: '',
    productCode: '',
    categoryId: null,
    description: '',
    mainImage: '',
    price: null,
    originalPrice: null,
    stock: 0,
    unit: '杯',
    status: 1,
    isHot: 0,
    isNew: 0,
    isRecommend: 0,
    specList: [],
    skuList: []
  })
  productFormRef.value?.resetFields()
}
</script>

<style scoped>
.product-list-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.product-image-upload {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.product-image-upload__tip {
  color: #909399;
  font-size: 12px;
}

.product-form-preview {
  width: 120px;
  height: 120px;
  border-radius: 16px;
  overflow: hidden;
}

.sku-editor {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  margin-top: 18px;
  padding: 18px;
  background: #fafafa;
}

.sku-editor__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

.sku-editor__header--sub {
  margin-top: 22px;
}

.sku-editor__header h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.sku-editor__header p {
  margin: 0;
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
}

.sku-editor__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.sku-editor__table {
  width: 100%;
  margin-bottom: 10px;
}

.price {
  color: #f56c6c;
  font-weight: bold;
}
</style>
