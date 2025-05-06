<template>
  <div class="statistics-container">
    <el-container>
      <el-header>
        <div class="header-content">
          <h2>统计分析</h2>
          <el-button @click="$router.push('/home')">返回首页</el-button>
        </div>
      </el-header>
      <el-main>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card class="summary-card">
              <template #header>
                <div class="card-header">
                  <span>收支概览</span>
                </div>
              </template>
              <div class="summary-item">
                <span class="label">总收入：</span>
                <span class="value income">{{ formatCurrency(statistics.totalIncome || 0) }} 元</span>
              </div>
              <div class="summary-item">
                <span class="label">总支出：</span>
                <span class="value expense">{{ formatCurrency(statistics.totalExpense || 0) }} 元</span>
              </div>
              <div class="summary-item">
                <span class="label">结余：</span>
                <span class="value" :class="statistics.balance >= 0 ? 'income' : 'expense'">
                  {{ formatCurrency(statistics.balance || 0) }} 元
                </span>
              </div>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card>
              <div ref="balanceChartContainer" style="height: 300px;"></div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>收入分类</span>
                </div>
              </template>
              <div ref="incomeChartContainer" style="height: 300px;"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>支出分类</span>
                </div>
              </template>
              <div ref="expenseChartContainer" style="height: 300px;"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import api from '../api'
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

export default {
  name: 'StatisticsView',
  setup() {
    const statistics = ref({
      totalIncome: 0,
      totalExpense: 0,
      balance: 0,
      incomeByCategory: [],
      expenseByCategory: []
    })

    const balanceChartContainer = ref(null)
    const incomeChartContainer = ref(null)
    const expenseChartContainer = ref(null)

    let balanceChart = null
    let incomeChart = null
    let expenseChart = null

    // Format currency with 2 decimal places
    const formatCurrency = (value) => {
      return parseFloat(value || 0).toFixed(2)
    }

    // Load statistics data
    const loadStatistics = async () => {
      try {
        const response = await api.getStatistics()
        statistics.value = response.data
        renderCharts()
      } catch (error) {
        console.error('Error loading statistics:', error)
        ElMessage.error('加载统计数据失败')
      }
    }

    // Render charts
    const renderCharts = () => {
      renderBalanceChart()
      renderPieChart(incomeChart, statistics.value.incomeByCategory, '收入分布')
      renderPieChart(expenseChart, statistics.value.expenseByCategory, '支出分布')
    }

    // Render balance chart
    const renderBalanceChart = () => {
      if (!balanceChart) return

      const option = {
        title: {
          text: '收支概览',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: '{b}: {c} 元'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: ['总收入', '总支出', '结余']
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '金额',
            type: 'bar',
            data: [
              {
                value: statistics.value.totalIncome || 0,
                itemStyle: { color: '#67C23A' }
              },
              {
                value: statistics.value.totalExpense || 0,
                itemStyle: { color: '#F56C6C' }
              },
              {
                value: statistics.value.balance || 0,
                itemStyle: {
                  color: statistics.value.balance >= 0 ? '#67C23A' : '#F56C6C'
                }
              }
            ],
            label: {
              show: true,
              position: 'top',
              formatter: '{c} 元'
            }
          }
        ]
      }

      balanceChart.setOption(option)
    }

    // Render pie chart
    const renderPieChart = (chart, data, title) => {
      if (!chart || !data || data.length === 0) return

      const chartData = data.map(item => {
        return {
          name: item.category || '未分类',
          value: item.amount || 0
        }
      })

      const option = {
        title: {
          text: title,
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} 元 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
        },
        series: [
          {
            name: title,
            type: 'pie',
            radius: '60%',
            center: ['50%', '50%'],
            data: chartData,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            },
            label: {
              formatter: '{b}: {c} 元 ({d}%)'
            }
          }
        ]
      }

      chart.setOption(option)
    }

    // Initialize charts
    const initCharts = () => {
      // Initialize balance chart
      if (balanceChartContainer.value) {
        balanceChart = echarts.init(balanceChartContainer.value)
      }

      // Initialize income chart
      if (incomeChartContainer.value) {
        incomeChart = echarts.init(incomeChartContainer.value)
      }

      // Initialize expense chart
      if (expenseChartContainer.value) {
        expenseChart = echarts.init(expenseChartContainer.value)
      }
    }

    // Handle window resize
    const handleResize = () => {
      balanceChart?.resize()
      incomeChart?.resize()
      expenseChart?.resize()
    }

    onMounted(() => {
      // Load statistics data
      loadStatistics()

      // Initialize charts after DOM is ready
      setTimeout(() => {
        initCharts()
        renderCharts()
      }, 100)

      // Add resize event listener
      window.addEventListener('resize', handleResize)
    })

    onUnmounted(() => {
      // Remove resize event listener
      window.removeEventListener('resize', handleResize)

      // Dispose charts
      balanceChart?.dispose()
      incomeChart?.dispose()
      expenseChart?.dispose()
    })

    return {
      statistics,
      formatCurrency,
      balanceChartContainer,
      incomeChartContainer,
      expenseChartContainer
    }
  }
}
</script>

<style scoped>
.statistics-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.el-header {
  background-color: #f5f7fa;
  color: #333;
  line-height: 60px;
  border-bottom: 1px solid #e6e6e6;
}

.el-main {
  padding: 20px;
  background-color: #f5f7fa;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-card {
  height: 300px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  font-size: 16px;
}

.label {
  font-weight: bold;
}

.value {
  font-size: 18px;
}

.income {
  color: #67C23A;
}

.expense {
  color: #F56C6C;
}
</style>