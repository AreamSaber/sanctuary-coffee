<template>
  <div class="category-manage-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>分类管理</span>
          <el-button type="primary" @click="handleAdd(null)">
            <el-icon><Plus /></el-icon>
            添加一级分类
          </el-button>
        </div>
      </template>
      
      <!-- 分类树 -->
      <el-table
        :data="tableData"
        style="width: 100%"
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        v-loading="loading"
      >
        <el-table-column prop="categoryName" label="分类名称" min-width="200" />
        <el-table-column prop="categoryCode" label="分类编码" width="150" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="level" label="层级" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleAdd(row.id)">
              添加子分类
            </el-button>
            <el-button link type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 分类表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryRules"
        label-width="100px"
      >
        <el-form-item label="父分类">
          <el-input
            :value="getParentCategoryName(categoryForm.parentId)"
            disabled
            placeholder="无（一级分类）"
          />
        </el-form-item>
        
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="categoryForm.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="categoryForm.categoryCode" placeholder="请输入分类编码" />
        </el-form-item>
        
        <el-form-item label="排序">
          <el-input-number
            v-model="categoryForm.sortOrder"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="状态">
          <el-radio-group v-model="categoryForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCategoryTree,
  addCategory,
  updateCategory,
  deleteCategory
} from '@/api/product'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('添加分类')
const tableData = ref([])
const allCategories = ref([])
const categoryFormRef = ref()

const categoryForm = reactive({
  id: null,
  parentId: null,
  categoryName: '',
  categoryCode: '',
  sortOrder: 0,
  status: 1
})

const categoryRules = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' }
  ],
  categoryCode: [
    { required: true, message: '请输入分类编码', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCategoryTree()
    tableData.value = res.data
    allCategories.value = flattenTree(res.data)
  } catch (error) {
    console.error('加载分类失败:', error)
  } finally {
    loading.value = false
  }
}

const flattenTree = (tree, result = []) => {
  tree.forEach(node => {
    result.push(node)
    if (node.children && node.children.length > 0) {
      flattenTree(node.children, result)
    }
  })
  return result
}

const getParentCategoryName = (parentId) => {
  if (!parentId || parentId === 0) {
    return '无（一级分类）'
  }
  const parent = allCategories.value.find(item => item.id === parentId)
  return parent ? parent.categoryName : ''
}

const handleAdd = (parentId) => {
  dialogTitle.value = parentId ? '添加子分类' : '添加一级分类'
  resetForm()
  categoryForm.parentId = parentId || 0
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑分类'
  Object.assign(categoryForm, {
    id: row.id,
    parentId: row.parentId,
    categoryName: row.categoryName,
    categoryCode: row.categoryCode,
    sortOrder: row.sortOrder,
    status: row.status
  })
  dialogVisible.value = true
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该分类吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCategory(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSubmit = async () => {
  await categoryFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const data = { ...categoryForm }
        if (data.parentId === 0) {
          delete data.parentId
        }
        
        if (categoryForm.id) {
          await updateCategory(data)
          ElMessage.success('更新成功')
        } else {
          await addCategory(data)
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
  Object.assign(categoryForm, {
    id: null,
    parentId: null,
    categoryName: '',
    categoryCode: '',
    sortOrder: 0,
    status: 1
  })
  categoryFormRef.value?.resetFields()
}
</script>

<style scoped>
.category-manage-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
