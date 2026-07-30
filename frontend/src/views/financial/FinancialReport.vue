<template>
  <div class="financial-report-container">
    <!-- 时间选择器 -->
    <el-card class="filter-card">
      <div class="filter-bar">
        <el-radio-group v-model="reportType" @change="handleReportTypeChange">
          <el-radio-button label="daily">日报表</el-radio-button>
          <el-radio-button label="monthly">月报表</el-radio-button>
          <el-radio-button label="yearly">年报表</el-radio-button>
          <el-radio-button label="custom">自定义</el-radio-button>
        </el-radio-group>
        
        <template v-if="reportType === 'daily'">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="loadReport"
          />
        </template>
        
        <template v-else-if="reportType === 'monthly'">
          <el-date-picker
            v-model="selectedMonth"
            type="month"
            placeholder="选择月份"
            format="YYYY-MM"
            value-format="YYYY-MM"
            @change="loadReport"
          />
        </template>
        
        <template v-else-if="reportType === 'yearly'">
          <el-date-picker
            v-model="selectedYear"
            type="year"
            placeholder="选择年份"
            format="YYYY"
            value-format="YYYY"
            @change="loadReport"
          />
        </template>
        
        <template v-else-if="reportType === 'custom'">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="loadReport"
          />
        </template>
        
        <el-button type="primary" @click="loadReport">查询</el-button>
        <el-button @click="exportReport">
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>
      </div>
    </el-card>
    
    <!-- 核心指标卡片 -->
    <el-row :gutter="20" class="metrics-row">
      <el-col :span="6">
        <el-card class="metric-card">
          <el-statistic
            title="总收入"
            :value="reportData.totalRevenue || 0"
            prefix="¥"
            :precision="2"
          />
          <div class="metric-trend" :class="{ positive: true }">
            <el-icon><TrendCharts /></el-icon>
            <span>环比增长 15.2%</span>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card">
          <el-statistic
            title="订单总数"
            :value="reportData.totalOrders || 0"
            suffix="单"
          />
          <div class="metric-trend" :class="{ positive: true }">
            <el-icon><TrendCharts /></el-icon>
            <span>环比增长 8.5%</span>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card">
          <el-statistic
            title="平均客单价"
            :value="reportData.averageOrderAmount || 0"
            prefix="¥"
            :precision="2"
          />
          <div class="metric-trend" :class="{ negative: false }">
            <el-icon><TrendCharts /></el-icon>
            <span>环比持平</span>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card">
          <el-statistic
            title="净收入"
            :value="reportData.netRevenue || 0"
            prefix="¥"
            :precision="2"
          />
          <div class="metric-sub">
            <span>退款: ¥{{ reportData.totalRefund || 0 }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 收入趋势图 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>收入趋势</span>
          </template>
          <div id="revenue-chart" style="height: 350px"></div>
        </el-card>
      </el-col>
      
      <!-- 支付方式分布 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>支付方式分布</span>
          </template>
          <div id="payment-chart" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 时段分析和商品排行 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 时段销售分布 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>时段销售分布</span>
          </template>
          <div id="hourly-chart" style="height: 300px"></div>
        </el-card>
      </el-col>
      
      <!-- 商品销售排行 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>商品销售TOP10</span>
          </template>
          <el-table :data="productRankData" style="width: 100%" max-height="300">
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="productName" label="商品名称" />
            <el-table-column prop="salesCount" label="销量" width="80" align="right" />
            <el-table-column prop="salesAmount" label="销售额" width="100" align="right">
              <template #default="{ row }">
                ¥{{ row.salesAmount }}
              </template>
            </el-table-column>
            <el-table-column prop="percentage" label="占比" width="80" align="right">
              <template #default="{ row }">
                {{ row.percentage }}%
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 其他统计信息 -->
    <el-card class="stats-card">
      <template #header>
        <span>其他统计</span>
      </template>
      <el-row :gutter="40">
        <el-col :span="8">
          <div class="stat-item">
            <div class="stat-label">会员消费占比</div>
            <div class="stat-value">
              <el-progress
                :percentage="Number(reportData.memberRevenueRatio) || 0"
                :stroke-width="10"
                :color="customColors"
              />
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-item">
            <div class="stat-label">非促销优惠金额</div>
            <div class="stat-value">¥{{ reportData.couponDeductAmount || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-item">
            <div class="stat-label">促销活动优惠金额</div>
            <div class="stat-value">¥{{ reportData.promotionDeductAmount || 0 }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, TrendCharts } from '@element-plus/icons-vue'
import { getFinancialReport, exportFinancialReport } from '@/api/financial'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

// 报表类型
const reportType = ref('daily')
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const selectedMonth = ref(dayjs().format('YYYY-MM'))
const selectedYear = ref(dayjs().format('YYYY'))
const dateRange = ref([])

// 报表数据
const reportData = ref({})
const productRankData = ref([])

// 图表实例
let revenueChart = null
let paymentChart = null
let hourlyChart = null

// 自定义颜色
const customColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#6f7ad3', percentage: 100 }
]

// 加载报表
const loadReport = async () => {
  try {
    let params = {}
    
    if (reportType.value === 'daily') {
      params.type = 'daily'
      params.date = selectedDate.value
    } else if (reportType.value === 'monthly') {
      const [year, month] = selectedMonth.value.split('-')
      params.type = 'monthly'
      params.year = parseInt(year)
      params.month = parseInt(month)
    } else if (reportType.value === 'yearly') {
      params.type = 'yearly'
      params.year = selectedYear.value
    } else if (reportType.value === 'custom' && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    } else {
      ElMessage.warning('请选择时间范围')
      return
    }
    
    const res = await getFinancialReport(params)
    reportData.value = res.data
    productRankData.value = res.data.productSalesRank || []
    
    // 更新图表
    await nextTick()
    updateCharts()
  } catch (error) {
    ElMessage.error('加载报表失败')
  }
}

// 报表类型改变
const handleReportTypeChange = () => {
  // 重置日期选择
  if (reportType.value === 'custom') {
    dateRange.value = [
      dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
      dayjs().format('YYYY-MM-DD')
    ]
  }
}

// 导出报表
const exportReport = async () => {
  try {
    const reportRange = getReportRange()
    if (!reportRange) {
      ElMessage.warning('请选择时间范围')
      return
    }

    const response = await exportFinancialReport({
      ...reportRange,
      format: 'csv'
    })
    const blob = new Blob([response.data], {
      type: response.data.type || 'text/csv;charset=utf-8'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    const disposition = response.headers?.['content-disposition'] || ''
    const matchedName = disposition.match(/filename="?([^"]+)"?/)
    link.href = url
    link.download = matchedName?.[1] || `financial_report_${reportRange.startDate}_${reportRange.endDate}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

// 初始化图表
const initCharts = () => {
  // 收入趋势图
  const revenueChartDom = document.getElementById('revenue-chart')
  if (revenueChartDom) {
    revenueChart = echarts.init(revenueChartDom)
  }
  
  // 支付方式分布图
  const paymentChartDom = document.getElementById('payment-chart')
  if (paymentChartDom) {
    paymentChart = echarts.init(paymentChartDom)
  }
  
  // 时段分布图
  const hourlyChartDom = document.getElementById('hourly-chart')
  if (hourlyChartDom) {
    hourlyChart = echarts.init(hourlyChartDom)
  }
}

// 更新图表
const updateCharts = () => {
  // 收入趋势图
  if (revenueChart && reportData.value.dailyRevenueTrend) {
    const dates = reportData.value.dailyRevenueTrend.map(item => item.date)
    const revenues = reportData.value.dailyRevenueTrend.map(item => item.revenue)
    
    const option = {
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: dates
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: '¥{value}'
        }
      },
      series: [{
        data: revenues,
        type: 'line',
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0)' }
          ])
        }
      }]
    }
    revenueChart.setOption(option)
  }
  
  // 支付方式分布图
  if (paymentChart && reportData.value.paymentMethodDistribution) {
    const data = Object.entries(reportData.value.paymentMethodDistribution).map(([name, value]) => ({
      name: getPaymentMethodName(name),
      value
    }))
    
    const option = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: ¥{c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left'
      },
      series: [{
        type: 'pie',
        radius: '50%',
        data
      }]
    }
    paymentChart.setOption(option)
  }
  
  // 时段分布图
  if (hourlyChart && reportData.value.hourlyDistribution) {
    const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
    const values = Array.from({ length: 24 }, (_, i) => reportData.value.hourlyDistribution[i] || 0)
    
    const option = {
      tooltip: {
        trigger: 'axis',
        formatter: '{b}: ¥{c}'
      },
      xAxis: {
        type: 'category',
        data: hours
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: '¥{value}'
        }
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
}

// 获取支付方式名称
const getPaymentMethodName = (method) => {
  const nameMap = {
    'WECHAT': '微信支付',
    'ALIPAY': '支付宝',
    'CASH': '现金',
    'CARD': '银行卡',
    'UNKNOWN': '其他'
  }
  return nameMap[method] || method
}

// 窗口大小改变时重新渲染图表
const handleResize = () => {
  revenueChart?.resize()
  paymentChart?.resize()
  hourlyChart?.resize()
}

const getReportRange = () => {
  if (reportType.value === 'daily') {
    return {
      startDate: selectedDate.value,
      endDate: selectedDate.value
    }
  }

  if (reportType.value === 'monthly' && selectedMonth.value) {
    const startDate = `${selectedMonth.value}-01`
    return {
      startDate,
      endDate: dayjs(startDate).endOf('month').format('YYYY-MM-DD')
    }
  }

  if (reportType.value === 'yearly' && selectedYear.value) {
    return {
      startDate: `${selectedYear.value}-01-01`,
      endDate: `${selectedYear.value}-12-31`
    }
  }

  if (reportType.value === 'custom' && dateRange.value.length === 2) {
    return {
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    }
  }

  return null
}

onMounted(() => {
  initCharts()
  loadReport()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  revenueChart?.dispose()
  paymentChart?.dispose()
  hourlyChart?.dispose()
})
</script>

<style lang="scss" scoped>
.financial-report-container {
  padding: 20px;
  
  .filter-card {
    margin-bottom: 20px;
    
    .filter-bar {
      display: flex;
      align-items: center;
      gap: 15px;
    }
  }
  
  .metrics-row {
    margin-bottom: 20px;
    
    .metric-card {
      :deep(.el-statistic__head) {
        color: #909399;
        font-size: 14px;
      }
      
      :deep(.el-statistic__content) {
        font-size: 24px;
        font-weight: 600;
      }
      
      .metric-trend {
        margin-top: 10px;
        display: flex;
        align-items: center;
        gap: 5px;
        font-size: 12px;
        
        &.positive {
          color: #67c23a;
        }
        
        &.negative {
          color: #f56c6c;
        }
      }
      
      .metric-sub {
        margin-top: 10px;
        font-size: 12px;
        color: #909399;
      }
    }
  }
  
  .chart-row {
    margin-bottom: 20px;
  }
  
  .stats-card {
    .stat-item {
      text-align: center;
      
      .stat-label {
        margin-bottom: 10px;
        color: #606266;
        font-size: 14px;
      }
      
      .stat-value {
        font-size: 20px;
        font-weight: 600;
        color: #303133;
      }
    }
  }
}
</style>
