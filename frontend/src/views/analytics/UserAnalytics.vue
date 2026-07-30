<template>
  <div class="user-analytics-container">
    <!-- 实时数据卡片 -->
    <el-row :gutter="20" class="realtime-cards">
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic
            title="今日活跃用户"
            :value="realtimeStats.todayActiveUsers || 0"
            suffix="人"
          />
          <div class="stat-footer">
            <span class="stat-label">DAU</span>
            <el-tag type="success" size="small">实时</el-tag>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic
            title="实时在线用户"
            :value="realtimeStats.onlineUsers || 0"
            suffix="人"
          />
          <div class="stat-footer">
            <span class="stat-label">最近5分钟活跃</span>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic
            title="今日页面浏览量"
            :value="realtimeStats.todayPageViews || 0"
            suffix="次"
          />
          <div class="stat-footer">
            <span class="stat-label">PV</span>
            <span class="stat-trend positive">+12.5%</span>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic
            title="今日新增用户"
            :value="realtimeStats.todayNewUsers || 0"
            suffix="人"
          />
          <div class="stat-footer">
            <span class="stat-label">注册用户</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 复购率统计卡片 -->
    <el-card class="repurchase-card">
      <template #header>
        <div class="card-header">
          <span>复购率分析</span>
          <el-button type="primary" link @click="loadRepurchaseRate">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="repurchase-stat">
            <div class="repurchase-stat__value">{{ repurchaseRate.totalUsers || 0 }}</div>
            <div class="repurchase-stat__label">购买用户总数</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="repurchase-stat">
            <div class="repurchase-stat__value">{{ repurchaseRate.repurchaseUsers || 0 }}</div>
            <div class="repurchase-stat__label">复购用户数（≥2次）</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="repurchase-stat">
            <div class="repurchase-stat__value repurchase-stat__value--highlight">
              {{ repurchaseRate.repurchaseRate || 0 }}%
            </div>
            <div class="repurchase-stat__label">复购率</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 时间选择器 -->
    <el-card class="filter-card">
      <div class="filter-bar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :shortcuts="shortcuts"
          @change="loadAnalytics"
        />
        <el-button type="primary" @click="loadAnalytics">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </el-card>
    
    <!-- 转化漏斗 -->
    <el-card class="funnel-card">
      <template #header>
        <span>用户转化漏斗</span>
      </template>
      <div id="conversion-funnel" style="height: 400px"></div>
    </el-card>
    
    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 用户活跃度趋势 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>用户活跃度趋势</span>
          </template>
          <div id="activity-trend" style="height: 350px"></div>
        </el-card>
      </el-col>
      
      <!-- 行为分布 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>用户行为分布</span>
          </template>
          <div id="behavior-distribution" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="chart-row">
      <!-- 设备分布 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>设备分布</span>
          </template>
          <div id="device-distribution" style="height: 300px"></div>
        </el-card>
      </el-col>
      
      <!-- 时段活跃度 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>24小时活跃度分布</span>
          </template>
          <div id="hourly-activity" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 数据表格区域 -->
    <el-row :gutter="20" class="table-row">
      <!-- 页面访问排行 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>页面访问排行TOP10</span>
          </template>
          <el-table :data="pageViewStats" style="width: 100%" max-height="350">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="pageUrl" label="页面" min-width="150">
              <template #default="{ row }">
                <el-tooltip :content="row.pageUrl" placement="top">
                  <span class="page-url">{{ formatPageUrl(row.pageUrl) }}</span>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="views" label="浏览量" width="80" align="right" />
            <el-table-column prop="uniqueVisitors" label="独立访客" width="90" align="right" />
            <el-table-column prop="avgDuration" label="平均时长" width="90" align="right">
              <template #default="{ row }">
                {{ formatDuration(row.avgDuration) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      
      <!-- 商品偏好分析 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>热门商品TOP10</span>
          </template>
          <el-table :data="productPreferences" style="width: 100%" max-height="350">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="productName" label="商品名称" min-width="120">
              <template #default="{ row }">
                <el-tooltip :content="row.productName" placement="top">
                  <span class="product-name">{{ row.productName }}</span>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览" width="70" align="right" />
            <el-table-column prop="addToCartCount" label="加购" width="70" align="right" />
            <el-table-column prop="purchaseCount" label="购买" width="70" align="right" />
            <el-table-column prop="conversionRate" label="转化率" width="80" align="right">
              <template #default="{ row }">
                <el-tag :type="getConversionRateType(row.conversionRate)">
                  {{ row.conversionRate }}%
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 用户活跃度统计 -->
    <el-card class="active-users-card">
      <template #header>
        <span>用户活跃度统计</span>
      </template>
      <el-row :gutter="40">
        <el-col :span="6">
          <div class="active-stat">
            <div class="stat-value">{{ activeUsers.dailyActiveUsers || 0 }}</div>
            <div class="stat-label">日活跃用户(DAU)</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="active-stat">
            <div class="stat-value">{{ activeUsers.weeklyActiveUsers || 0 }}</div>
            <div class="stat-label">周活跃用户(WAU)</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="active-stat">
            <div class="stat-value">{{ activeUsers.monthlyActiveUsers || 0 }}</div>
            <div class="stat-label">月活跃用户(MAU)</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="active-stat">
            <div class="stat-value">{{ activeUsers.newUsers || 0 }}</div>
            <div class="stat-label">时段内新增用户</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getAnalyticsReport, getRealTimeStats, getActivityTrend, getHotProducts, getRepurchaseRate } from '@/api/analytics'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

// 日期范围
const dateRange = ref([
  dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD')
])

// 快捷选项
const shortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近一个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  },
  {
    text: '最近三个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
      return [start, end]
    }
  }
]

// 实时统计数据
const realtimeStats = ref({})

// 分析数据
const analyticsData = ref({})
const activeUsers = ref({})
const pageViewStats = ref([])
const productPreferences = ref([])
const repurchaseRate = ref({})

// 图表实例
let activityTrendChart = null
let behaviorChart = null
let deviceChart = null
let hourlyChart = null
let funnelChart = null

// 定时器
let realtimeTimer = null

// 加载实时统计
const loadRealtimeStats = async () => {
  try {
    const res = await getRealTimeStats()
    realtimeStats.value = res.data
  } catch (error) {
    console.error('加载实时统计失败:', error)
  }
}

// 加载分析报告
const loadAnalytics = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }
  
  try {
    const res = await getAnalyticsReport({
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    })
    
    analyticsData.value = res.data
    activeUsers.value = res.data.activeUsers || {}
    pageViewStats.value = res.data.pageViews || []
    productPreferences.value = res.data.productPreferences || []
    
    // 更新图表
    await nextTick()
    updateCharts()
  } catch (error) {
    ElMessage.error('加载分析报告失败')
  }
}

// 加载活跃度趋势
const loadActivityTrend = async () => {
  try {
    const res = await getActivityTrend({ days: 30 })
    updateActivityTrendChart(res.data)
  } catch (error) {
    console.error('加载活跃度趋势失败:', error)
  }
}

// 加载复购率统计
const loadRepurchaseRate = async () => {
  try {
    const startTime = dateRange.value ? dateRange.value[0] + ' 00:00:00' : undefined
    const endTime = dateRange.value ? dateRange.value[1] + ' 23:59:59' : undefined
    const res = await getRepurchaseRate({ startTime, endTime })
    repurchaseRate.value = res.data || {}
  } catch (error) {
    console.error('加载复购率统计失败:', error)
  }
}

// 初始化图表
const initCharts = () => {
  activityTrendChart = echarts.init(document.getElementById('activity-trend'))
  behaviorChart = echarts.init(document.getElementById('behavior-distribution'))
  deviceChart = echarts.init(document.getElementById('device-distribution'))
  hourlyChart = echarts.init(document.getElementById('hourly-activity'))
  funnelChart = echarts.init(document.getElementById('conversion-funnel'))
}

// 更新图表
const updateCharts = () => {
  updateBehaviorChart()
  updateDeviceChart()
  updateHourlyChart()
  updateFunnelChart()
}

// 更新活跃度趋势图
const updateActivityTrendChart = (data) => {
  if (!activityTrendChart || !data) return
  
  const dates = Object.keys(data).sort()
  const values = dates.map(date => data[date])
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    xAxis: {
      type: 'category',
      data: dates.map(d => dayjs(d).format('MM-DD'))
    },
    yAxis: {
      type: 'value',
      name: '活跃用户数'
    },
    series: [{
      data: values,
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0)' }
        ])
      }
    }]
  }
  
  activityTrendChart.setOption(option)
}

// 更新行为分布图
const updateBehaviorChart = () => {
  if (!behaviorChart || !analyticsData.value.behaviorDistribution) return
  
  const data = Object.entries(analyticsData.value.behaviorDistribution).map(([name, value]) => ({
    name: getBehaviorName(name),
    value
  }))
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '20',
          fontWeight: 'bold'
        }
      },
      data
    }]
  }
  
  behaviorChart.setOption(option)
}

// 更新设备分布图
const updateDeviceChart = () => {
  if (!deviceChart || !analyticsData.value.deviceDistribution) return
  
  const data = Object.entries(analyticsData.value.deviceDistribution).map(([name, value]) => ({
    name: getDeviceName(name),
    value
  }))
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}%'
    },
    series: [{
      type: 'pie',
      radius: '60%',
      data,
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
  
  deviceChart.setOption(option)
}

// 更新时段活跃度图
const updateHourlyChart = () => {
  if (!hourlyChart || !analyticsData.value.hourlyActivity) return
  
  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
  const values = hours.map((_, i) => analyticsData.value.hourlyActivity[i] || 0)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    xAxis: {
      type: 'category',
      data: hours
    },
    yAxis: {
      type: 'value',
      name: '活跃度'
    },
    series: [{
      data: values,
      type: 'bar',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ])
      }
    }]
  }
  
  hourlyChart.setOption(option)
}

// 更新转化漏斗图
const updateFunnelChart = () => {
  if (!funnelChart || !analyticsData.value.conversionFunnel) return
  
  const funnel = analyticsData.value.conversionFunnel
  
  const data = [
    { value: funnel.totalVisitors || 0, name: '访问用户' },
    { value: funnel.viewProduct || 0, name: '浏览商品' },
    { value: funnel.addToCart || 0, name: '加入购物车' },
    { value: funnel.checkout || 0, name: '提交订单' },
    { value: funnel.purchase || 0, name: '支付成功' }
  ]
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 人'
    },
    series: [{
      name: '转化漏斗',
      type: 'funnel',
      left: '10%',
      top: 60,
      bottom: 60,
      width: '80%',
      min: 0,
      max: funnel.totalVisitors || 100,
      minSize: '0%',
      maxSize: '100%',
      sort: 'descending',
      gap: 2,
      label: {
        show: true,
        position: 'inside',
        formatter: '{b}\n{c}人'
      },
      labelLine: {
        length: 10,
        lineStyle: {
          width: 1,
          type: 'solid'
        }
      },
      itemStyle: {
        borderColor: '#fff',
        borderWidth: 1
      },
      emphasis: {
        label: {
          fontSize: 20
        }
      },
      data
    }]
  }
  
  funnelChart.setOption(option)
}

// 辅助函数
const getBehaviorName = (type) => {
  const nameMap = {
    VIEW: '浏览',
    SEARCH: '搜索',
    CLICK: '点击',
    ADD_CART: '加购',
    ORDER: '下单',
    PAY: '支付',
    REVIEW: '评价'
  }
  return nameMap[type] || type
}

const getDeviceName = (type) => {
  const nameMap = {
    PC: '电脑',
    MOBILE: '手机',
    TABLET: '平板'
  }
  return nameMap[type] || type
}

const formatPageUrl = (url) => {
  if (!url) return '-'
  const parts = url.split('/')
  return parts[parts.length - 1] || 'index'
}

const formatDuration = (duration) => {
  if (!duration) return '0s'
  const seconds = Math.round(duration)
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  return `${minutes}m${seconds % 60}s`
}

const getConversionRateType = (rate) => {
  if (rate >= 10) return 'success'
  if (rate >= 5) return 'warning'
  if (rate >= 1) return ''
  return 'danger'
}

// 窗口大小改变时重新渲染图表
const handleResize = () => {
  activityTrendChart?.resize()
  behaviorChart?.resize()
  deviceChart?.resize()
  hourlyChart?.resize()
  funnelChart?.resize()
}

onMounted(() => {
  initCharts()
  loadRealtimeStats()
  loadAnalytics()
  loadActivityTrend()
  loadRepurchaseRate()

  // 定时刷新实时数据（每30秒）
  realtimeTimer = setInterval(loadRealtimeStats, 30000)

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (realtimeTimer) {
    clearInterval(realtimeTimer)
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.user-analytics-container {
  padding: 20px;

  .repurchase-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .repurchase-stat {
      text-align: center;
      padding: 20px 0;

      &__value {
        font-size: 36px;
        font-weight: 600;
        color: #303133;

        &--highlight {
          color: #409eff;
        }
      }

      &__label {
        margin-top: 10px;
        font-size: 14px;
        color: #909399;
      }
    }
  }

  .realtime-cards {
    margin-bottom: 20px;
    
    .stat-card {
      :deep(.el-statistic__head) {
        color: #909399;
        font-size: 14px;
      }
      
      :deep(.el-statistic__content) {
        font-size: 28px;
        font-weight: 600;
      }
      
      .stat-footer {
        margin-top: 10px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .stat-label {
          color: #909399;
          font-size: 12px;
        }
        
        .stat-trend {
          font-size: 12px;
          
          &.positive {
            color: #67c23a;
          }
          
          &.negative {
            color: #f56c6c;
          }
        }
      }
    }
  }
  
  .filter-card {
    margin-bottom: 20px;
    
    .filter-bar {
      display: flex;
      align-items: center;
      gap: 15px;
    }
  }
  
  .funnel-card {
    margin-bottom: 20px;
  }
  
  .chart-row {
    margin-bottom: 20px;
  }
  
  .table-row {
    margin-bottom: 20px;
    
    .page-url,
    .product-name {
      display: inline-block;
      max-width: 150px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
  
  .active-users-card {
    .active-stat {
      text-align: center;
      
      .stat-value {
        font-size: 32px;
        font-weight: 600;
        color: #409eff;
      }
      
      .stat-label {
        margin-top: 10px;
        color: #606266;
        font-size: 14px;
      }
    }
  }
}
</style>
