<template>
  <div class="dashboard-container">
    <div class="page-header">
      <h1 class="page-title">数据统计大屏</h1>
    </div>

    <!-- 核心数据卡片 -->
    <el-row :gutter="24" class="stats-cards">
      <el-col :span="6">
        <div class="stat-card sales cursor-pointer">
          <div class="stat-icon">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">¥{{ salesStats.totalSales || 0 }}</div>
            <div class="stat-label">总销售额</div>
            <div class="stat-trend positive">
              <el-icon><CaretTop /></el-icon>
              今日: ¥{{ salesStats.todaySales || 0 }}
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card orders cursor-pointer">
          <div class="stat-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ salesStats.totalOrders || 0 }}</div>
            <div class="stat-label">总订单数</div>
            <div class="stat-trend neutral">
              今日: {{ salesStats.todayOrders || 0 }}
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card users cursor-pointer">
          <div class="stat-icon">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ userStats.totalUsers || 0 }}</div>
            <div class="stat-label">总用户数</div>
            <div class="stat-trend positive">
              <el-icon><CaretTop /></el-icon>
              +{{ userStats.todayNewUsers || 0 }}
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card active cursor-pointer">
          <div class="stat-icon">
            <el-icon><Medal /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ userStats.activeUsers || 0 }}</div>
            <div class="stat-label">活跃用户</div>
            <div class="stat-trend neutral">本月下单用户</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="24" class="charts-row">
      <!-- 销售趋势图 -->
      <el-col :xs="24" :sm="24" :md="24" :lg="16">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">销售趋势</h3>
            <el-button-group size="small" class="trend-tabs">
              <el-button
                @click="changeTrendDays(7)"
                :type="trendDays === 7 ? 'primary' : ''"
                size="small"
              >
                7天
              </el-button>
              <el-button
                @click="changeTrendDays(15)"
                :type="trendDays === 15 ? 'primary' : ''"
                size="small"
              >
                15天
              </el-button>
              <el-button
                @click="changeTrendDays(30)"
                :type="trendDays === 30 ? 'primary' : ''"
                size="small"
              >
                30天
              </el-button>
            </el-button-group>
          </div>
          <div ref="salesTrendChart" class="chart-body"></div>
        </div>
      </el-col>

      <el-col :xs="24" :sm="24" :md="24" :lg="8">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">商品分类销售概览</h3>
          </div>
          <div class="category-summary-panel">
            <template v-if="categorySummary.length">
              <div class="category-summary-hero">
                <span class="category-summary-hero__label">总销量</span>
                <strong class="category-summary-hero__value">{{ categoryTotalSales }}</strong>
                <span class="category-summary-hero__caption">
                  主力分类：{{ categoryLeader?.name || '暂无数据' }}
                </span>
              </div>

              <div class="category-summary-metrics">
                <div class="category-summary-metric">
                  <span>第一名占比</span>
                  <strong>{{ categoryLeader ? `${categoryLeader.percent}%` : '--' }}</strong>
                </div>
                <div class="category-summary-metric">
                  <span>TOP3 贡献</span>
                  <strong>{{ `${categoryTopThreeShare}%` }}</strong>
                </div>
                <div class="category-summary-metric">
                  <span>覆盖分类</span>
                  <strong>{{ categorySummary.length }}</strong>
                </div>
              </div>

              <div class="category-summary-list">
                <div
                  v-for="item in categoryFocusList"
                  :key="item.name"
                  class="category-summary-item"
                >
                  <div class="category-summary-item__head">
                    <div class="category-summary-item__title">
                      <span class="category-summary-item__rank">#{{ item.rank }}</span>
                      <span>{{ item.name }}</span>
                    </div>
                    <strong>{{ item.value }} 件</strong>
                  </div>
                  <div class="category-summary-item__meta">
                    <span>{{ item.percent }}% 占比</span>
                    <span>{{ item.rank === 1 ? '销量最高' : `第 ${item.rank} 位` }}</span>
                  </div>
                </div>
              </div>

              <p v-if="categoryMoreCount > 0" class="category-summary-footnote">
                其余 {{ categoryMoreCount }} 个分类已省略展示
              </p>
            </template>
            <el-empty v-else description="暂无分类销售数据" :image-size="72" />
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 商品数据表格 -->
    <el-row :gutter="24" class="tables-row">
      <!-- 热销商品 -->
      <el-col :span="12">
        <div class="table-card">
          <div class="table-header">
            <h3 class="table-title">热销商品 TOP 10</h3>
            <el-tag type="success" size="small">实时更新</el-tag>
          </div>
          <el-table :data="topProducts" class="data-table" max-height="350">
            <el-table-column type="index" label="排名" width="60" align="center">
              <template #default="{ $index }">
                <span v-if="$index < 3" class="rank-badge" :class="`rank-${$index + 1}`">
                  {{ $index + 1 }}
                </span>
                <span v-else>{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="productName" label="商品名称" min-width="120" />
            <el-table-column prop="categoryName" label="分类" width="100" />
            <el-table-column prop="salesCount" label="销量" width="80" align="center" />
            <el-table-column prop="stock" label="库存" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.stock < 10 ? 'danger' : 'success'" size="small">
                  {{ row.stock }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <!-- 低库存预警 -->
      <el-col :span="12">
        <div class="table-card warning-card">
          <div class="table-header">
            <h3 class="table-title">低库存预警</h3>
            <el-tag type="warning" size="small">{{ lowStockProducts.length }} 个商品</el-tag>
          </div>
          <el-table :data="lowStockProducts" class="data-table" max-height="350">
            <el-table-column prop="productName" label="商品名称" min-width="120" />
            <el-table-column prop="categoryName" label="分类" width="100" />
            <el-table-column prop="stock" label="库存" width="80" align="center">
              <template #default="{ row }">
                <el-tag type="danger" size="small">{{ row.stock }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="salesCount" label="销量" width="80" align="center" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, nextTick, ref } from 'vue'
import { TrendCharts, Document, User, Medal, CaretTop } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getSalesStatistics,
  getTopSellingProducts,
  getLowStockProducts,
  getUserStatistics,
  getDailySalesTrend,
  getCategorySalesDistribution
} from '@/api/statistics'

const salesStats = ref({})
const userStats = ref({})
const topProducts = ref([])
const lowStockProducts = ref([])
const categoryDistribution = ref([])
const trendDays = ref(7)

const salesTrendChart = ref(null)
let salesChart = null

const categoryTotalSales = computed(() =>
  categoryDistribution.value.reduce((sum, item) => sum + Number(item.value || 0), 0)
)

const categorySummary = computed(() => {
  const total = categoryTotalSales.value || 1
  return categoryDistribution.value.map((item, index) => ({
    ...item,
    rank: index + 1,
    percent: Number(((Number(item.value || 0) / total) * 100).toFixed(1))
  }))
})

const categoryLeader = computed(() => categorySummary.value[0] || null)

const categoryTopThreeShare = computed(() =>
  Number(categorySummary.value.slice(0, 3).reduce((sum, item) => sum + item.percent, 0).toFixed(1))
)

const categoryFocusList = computed(() => categorySummary.value.slice(0, 4))

const categoryMoreCount = computed(() =>
  Math.max(categorySummary.value.length - categoryFocusList.value.length, 0)
)

onMounted(async () => {
  await loadData()
  await nextTick()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  salesChart?.dispose()
})

const loadData = async () => {
  try {
    // 加载销售统计
    const salesRes = await getSalesStatistics()
    salesStats.value = salesRes.data || {}

    // 加载用户统计
    const userRes = await getUserStatistics()
    userStats.value = userRes.data || {}

    // 加载热销商品
    const topRes = await getTopSellingProducts(10)
    topProducts.value = topRes.data || []

    // 加载低库存商品
    const lowStockRes = await getLowStockProducts(10)
    lowStockProducts.value = lowStockRes.data || []

    // 加载分类销售概览
    const categoryRes = await getCategorySalesDistribution()
    categoryDistribution.value = (categoryRes.data || [])
      .filter(item => Number(item.sales || 0) > 0)
      .sort((left, right) => Number(right.sales || 0) - Number(left.sales || 0))
      .map((item, index) => ({
        name: item.categoryName || `未命名分类 ${index + 1}`,
        value: Number(item.sales || 0)
      }))
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

const initCharts = async () => {
  await loadSalesTrendChart()
}

const loadSalesTrendChart = async () => {
  try {
    const res = await getDailySalesTrend(trendDays.value)
    const trendData = res.data || []

    if (salesChart) {
      salesChart.dispose()
    }

    salesChart = echarts.init(salesTrendChart.value)

    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: 'var(--color-border)',
        textStyle: { color: 'var(--color-text)' }
      },
      legend: {
        data: ['销售额', '订单数'],
        textStyle: { color: 'var(--color-text-secondary)' }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: trendData.map(item => item.date.substring(5)),
        axisLine: { lineStyle: { color: 'var(--color-border)' } },
        axisLabel: { color: 'var(--color-text-muted)' }
      },
      yAxis: [
        {
          type: 'value',
          name: '销售额（元）',
          position: 'left',
          axisLine: { show: false },
          axisLabel: { color: 'var(--color-text-muted)' },
          splitLine: { lineStyle: { color: 'var(--color-border-light)' } }
        },
        {
          type: 'value',
          name: '订单数',
          position: 'right',
          axisLine: { show: false },
          axisLabel: { color: 'var(--color-text-muted)' },
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: '销售额',
          type: 'line',
          smooth: true,
          data: trendData.map(item => item.sales),
          itemStyle: { color: 'var(--color-primary)' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(111, 78, 55, 0.3)' },
              { offset: 1, color: 'rgba(111, 78, 55, 0.05)' }
            ])
          }
        },
        {
          name: '订单数',
          type: 'bar',
          yAxisIndex: 1,
          data: trendData.map(item => item.orders),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'var(--color-accent)' },
              { offset: 1, color: 'rgba(210, 105, 30, 0.5)' }
            ]),
            borderRadius: [4, 4, 0, 0]
          }
        }
      ]
    }

    salesChart.setOption(option)
  } catch (error) {
    console.error('加载销售趋势图失败:', error)
  }
}

const changeTrendDays = async (days) => {
  trendDays.value = days
  await loadSalesTrendChart()
}

const handleResize = () => {
  salesChart?.resize()
}
</script>

<style scoped>
/* ============================================
   容器样式 / Container
   ============================================ */
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6);
  min-height: auto;
  padding: 0;
  background: transparent;
}

.page-header {
  margin-bottom: var(--spacing-8);
}

.page-title {
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  color: var(--color-text);
  margin-bottom: var(--spacing-2);
}

.page-subtitle {
  font-size: var(--text-base);
  color: var(--color-text-muted);
  margin: 0;
}

/* ============================================
   统计卡片 / Stats Cards
   ============================================ */
.stats-cards {
  margin-bottom: var(--spacing-8);
}

.stat-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  display: flex;
  align-items: center;
  gap: var(--spacing-5);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(18px);
  transition: all var(--transition-base);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  flex-shrink: 0;
}

.stat-card.sales .stat-icon {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
}

.stat-card.orders .stat-icon {
  background: linear-gradient(135deg, var(--color-accent), #f5576c);
}

.stat-card.users .stat-icon {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
}

.stat-card.active .stat-icon {
  background: linear-gradient(135deg, var(--color-success), #38f9d7);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--color-text);
  line-height: 1;
  margin-bottom: var(--spacing-1);
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-1);
}

.stat-trend {
  font-size: var(--text-xs);
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
}

.stat-trend.positive {
  color: var(--color-success);
}

.stat-trend.neutral {
  color: var(--color-text-muted);
}

/* ============================================
   图表区域 / Charts
   ============================================ */
.charts-row {
  margin-bottom: var(--spacing-8);
}

.chart-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(18px);
  overflow: hidden;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-6) var(--spacing-6) var(--spacing-4);
  border-bottom: 1px solid var(--color-border-light);
}

.chart-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin: 0;
}

.trend-tabs :deep(.el-button) {
  border-color: var(--color-border);
}

.trend-tabs :deep(.el-button:hover) {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.trend-tabs :deep(.el-button--primary) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.chart-body {
  height: 350px;
  padding: var(--spacing-4);
}

.category-summary-panel {
  min-height: 350px;
  padding: var(--spacing-4);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.category-summary-hero {
  padding: var(--spacing-4);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, rgba(111, 78, 55, 0.08), rgba(255, 255, 255, 0.98));
}

.category-summary-hero__label {
  display: block;
  font-size: var(--text-xs);
  font-weight: var(--font-bold);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.category-summary-hero__value {
  display: block;
  margin-top: var(--spacing-2);
  font-size: var(--text-4xl);
  line-height: 1;
  color: var(--color-text);
}

.category-summary-hero__caption {
  display: block;
  margin-top: var(--spacing-3);
  color: var(--color-text-secondary);
  font-weight: var(--font-medium);
}

.category-summary-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.category-summary-metric {
  padding: 12px 14px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.96);
}

.category-summary-metric span {
  display: block;
  margin-bottom: 6px;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.category-summary-metric strong {
  display: block;
  font-size: var(--text-lg);
  color: var(--color-text);
}

.category-summary-list {
  display: grid;
  gap: 10px;
}

.category-summary-item {
  padding: 14px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.98);
}

.category-summary-item__head,
.category-summary-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3);
}

.category-summary-item__head {
  margin-bottom: 8px;
}

.category-summary-item__title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  font-weight: var(--font-semibold);
  color: var(--color-text);
}

.category-summary-item__title span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-summary-item__rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 26px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--color-primary-50);
  color: var(--color-primary);
  font-size: var(--text-xs);
  font-weight: var(--font-bold);
  flex-shrink: 0;
}

.category-summary-item__meta {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.category-summary-footnote {
  margin: 0;
  text-align: right;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

/* ============================================
   表格区域 / Tables
   ============================================ */
.tables-row {
  margin-bottom: var(--spacing-8);
}

.table-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(18px);
  overflow: hidden;
}

.warning-card {
  border-left: 4px solid var(--color-warning);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-5) var(--spacing-6);
  border-bottom: 1px solid var(--color-border-light);
}

.table-title {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin: 0;
}

.data-table {
  border: none;
}

.data-table :deep(.el-table__header-wrapper) {
  background: var(--color-bg-alt);
}

.data-table :deep(th.el-table__cell) {
  background: transparent;
  color: var(--color-text-secondary);
  font-weight: var(--font-semibold);
  border-bottom: 1px solid var(--color-border);
}

.data-table :deep(td.el-table__cell) {
  border-bottom: 1px solid var(--color-border-light);
}

.data-table :deep(.el-table__row:hover td) {
  background: var(--color-primary-50);
}

/* 排名徽章 */
.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: var(--text-xs);
  font-weight: var(--font-bold);
  color: white;
}

.rank-badge.rank-1 {
  background: linear-gradient(135deg, #FFD700, #FFA500);
}

.rank-badge.rank-2 {
  background: linear-gradient(135deg, #C0C0C0, #A0A0A0);
}

.rank-badge.rank-3 {
  background: linear-gradient(135deg, #CD7F32, #A0522D);
}

/* ============================================
   响应式设计 / Responsive
   ============================================ */
@media (max-width: 1200px) {
  .dashboard-container {
    padding: var(--spacing-4);
  }

  .stat-card {
    margin-bottom: var(--spacing-4);
  }

  .charts-row .el-col,
  .tables-row .el-col {
    margin-bottom: var(--spacing-4);
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: var(--text-2xl);
  }

  .stats-cards .el-col {
    margin-bottom: var(--spacing-4);
  }

  .stat-card {
    flex-direction: column;
    text-align: center;
  }

  .stat-icon {
    margin-right: 0;
    margin-bottom: var(--spacing-3);
  }

  .stat-trend {
    justify-content: center;
  }

  .chart-header {
    flex-direction: column;
    gap: var(--spacing-3);
    align-items: flex-start;
  }

  .chart-body {
    height: 250px;
  }

  .category-summary-panel {
    min-height: 0;
  }

  .category-summary-metrics {
    grid-template-columns: 1fr;
  }

  .category-summary-hero__value {
    font-size: var(--text-3xl);
  }
}
</style>
