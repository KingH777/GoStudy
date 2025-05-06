<template>
  <div class="home-container">
    <el-container>
      <el-header>
        <div class="header-content">
          <h2>家庭理财系统</h2>
          <el-dropdown @command="handleCommand">
            <el-button>
              菜单 <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="statistics">统计分析</el-dropdown-item>
                <el-dropdown-item command="exportCsv">导出数据</el-dropdown-item>
                <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
                <el-dropdown-item command="clearData" divided>清空数据</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main>
        <el-card>
          <div class="balance-display">
            <h3>当前余额: {{ formatCurrency(statistics.balance || 0) }} 元</h3>
          </div>

          <el-table
              :data="records"
              style="width: 100%"
              height="400"
              border
              stripe
              @row-contextmenu="handleRowContextMenu"
          >
            <el-table-column prop="id" label="序号" width="70" />
            <el-table-column prop="income" label="收入" width="120">
              <template #default="scope">
                {{ formatCurrency(scope.row.income) }}
              </template>
            </el-table-column>
            <el-table-column prop="expense" label="支出" width="120">
              <template #default="scope">
                {{ formatCurrency(scope.row.expense) }}
              </template>
            </el-table-column>
            <el-table-column prop="category" label="类别" width="120" />
            <el-table-column prop="recordDate" label="日期" width="120" />
            <el-table-column prop="notes" label="备注" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="editRecord(scope.row)">编辑</el-button>
                <el-button size="small" type="danger" @click="confirmDelete(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="input-card">
          <el-form :model="form" label-width="80px" class="input-form">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-form-item label="收入">
                  <el-input-number
                      v-model="form.income"
                      :min="0"
                      :precision="2"
                      @change="handleIncomeChange"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="收入类别">
                  <el-select
                      v-model="form.incomeCategory"
                      placeholder="选择类别"
                      :disabled="form.income <= 0"
                  >
                    <el-option
                        v-for="item in incomeCategories"
                        :key="item"
                        :label="item"
                        :value="item"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="支出">
                  <el-input-number
                      v-model="form.expense"
                      :min="0"
                      :precision="2"
                      @change="handleExpenseChange"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="支出类别">
                  <el-select
                      v-model="form.expenseCategory"
                      placeholder="选择类别"
                      :disabled="form.expense <= 0"
                  >
                    <el-option
                        v-for="item in expenseCategories"
                        :key="item"
                        :label="item"
                        :value="item"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="6">
                <el-form-item label="日期">
                  <el-date-picker
                      v-model="form.recordDate"
                      type="date"
                      placeholder="选择日期"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="备注">
                  <el-input v-model="form.notes" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-button type="primary" @click="addRecord" :loading="loading" style="margin-top: 32px">
                  添加记录
                </el-button>
              </el-col>
            </el-row>
          </el-form>
        </el-card>
      </el-main>
    </el-container>

    <!-- Edit Record Dialog -->
    <el-dialog v-model="dialogVisible" title="编辑记录" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="收入">
          <el-input-number v-model="editForm.income" :min="0" :precision="2" @change="handleEditIncomeChange" />
        </el-form-item>
        <el-form-item label="收入类别">
          <el-select v-model="editForm.category" placeholder="选择类别" :disabled="editForm.income <= 0">
            <el-option v-for="item in incomeCategories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="支出">
          <el-input-number v-model="editForm.expense" :min="0" :precision="2" @change="handleEditExpenseChange" />
        </el-form-item>
        <el-form-item label="支出类别">
          <el-select v-model="editForm.category" placeholder="选择类别" :disabled="editForm.expense <= 0">
            <el-option v-for="item in expenseCategories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="editForm.recordDate" type="date" placeholder="选择日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.notes" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateRecord" :loading="loading">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Change Password Dialog -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="changePassword" :loading="loading">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import api from '../api'
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'

export default {
  name: 'HomeView',
  components: {ArrowDown},
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const records = ref([])
    const statistics = ref({balance: 0, totalIncome: 0, totalExpense: 0})
    const dialogVisible = ref(false)
    const passwordDialogVisible = ref(false)

    const incomeCategories = ['工资', '红包', '生活费', '其他']
    const expenseCategories = ['三餐', '水电', '衣物', '出行', '其他']

    // Form for adding new records
    const form = reactive({
      income: 0,
      expense: 0,
      incomeCategory: '其他',
      expenseCategory: '其他',
      recordDate: new Date().toISOString().split('T')[0],
      notes: ''
    })

    // Form for editing records
    const editForm = reactive({
      id: null,
      income: 0,
      expense: 0,
      category: '',
      recordDate: '',
      notes: ''
    })

    // Form for changing password
    const passwordForm = reactive({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })

    // Load data from the server
    const loadData = async () => {
      try {
        const [recordsRes, statsRes] = await Promise.all([
          api.getAllRecords(),
          api.getStatistics()
        ])
        records.value = recordsRes.data
        statistics.value = statsRes.data
      } catch (error) {
        console.error('Error loading data:', error)
        ElMessage.error('加载数据失败')
      }
    }

    // Format currency with 2 decimal places
    const formatCurrency = (value) => {
      return parseFloat(value || 0).toFixed(2)
    }

    // Handle income input change
    const handleIncomeChange = (value) => {
      if (value > 0) {
        form.expense = 0
      }
    }

    // Handle expense input change
    const handleExpenseChange = (value) => {
      if (value > 0) {
        form.income = 0
      }
    }

    // Handle edit form income change
    const handleEditIncomeChange = (value) => {
      if (value > 0) {
        editForm.expense = 0
      }
    }

    // Handle edit form expense change
    const handleEditExpenseChange = (value) => {
      if (value > 0) {
        editForm.income = 0
      }
    }

    // Add a new record
    const addRecord = async () => {
      if (form.income <= 0 && form.expense <= 0) {
        ElMessage.warning('收入和支出不能同时为0')
        return
      }

      const newRecord = {
        income: form.income,
        expense: form.expense,
        category: form.income > 0 ? form.incomeCategory : form.expenseCategory,
        recordDate: form.recordDate,
        notes: form.notes
      }

      loading.value = true
      try {
        await api.createRecord(newRecord)
        ElMessage.success('添加记录成功')

        // Reset form
        form.income = 0
        form.expense = 0
        form.notes = ''

        // Reload data
        await loadData()
      } catch (error) {
        console.error('Error adding record:', error)
        ElMessage.error('添加记录失败')
      } finally {
        loading.value = false
      }
    }

    // Edit a record
    const editRecord = (row) => {
      Object.assign(editForm, {
        id: row.id,
        income: row.income || 0,
        expense: row.expense || 0,
        category: row.category,
        recordDate: row.recordDate,
        notes: row.notes
      })
      dialogVisible.value = true
    }

// Update a record
    const updateRecord = async () => {
      if (editForm.income <= 0 && editForm.expense <= 0) {
        ElMessage.warning('收入和支出不能同时为0')
        return
      }

      loading.value = true
      try {
        await api.updateRecord(editForm.id, editForm)
        ElMessage.success('更新记录成功')
        dialogVisible.value = false
        await loadData()
      } catch (error) {
        console.error('Error updating record:', error)
        ElMessage.error('更新记录失败')
      } finally {
        loading.value = false
      }
    }

// Confirm delete record
    const confirmDelete = (row) => {
      ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await api.deleteRecord(row.id)
          ElMessage.success('删除成功')
          await loadData()
        } catch (error) {
          console.error('Error deleting record:', error)
          ElMessage.error('删除失败')
        }
      }).catch(() => {
      })
    }

// Change password
    const changePassword = async () => {
      if (!passwordForm.oldPassword || !passwordForm.newPassword) {
        ElMessage.warning('请填写原密码和新密码')
        return
      }

      if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        ElMessage.warning('两次输入的新密码不一致')
        return
      }

      loading.value = true
      try {
        const response = await api.changePassword({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
        if (response.status === 200) {
          ElMessage.success('密码修改成功')
          passwordDialogVisible.value = false
          // Reset form
          passwordForm.oldPassword = ''
          passwordForm.newPassword = ''
          passwordForm.confirmPassword = ''
        }
      } catch (error) {
        console.error('Error changing password:', error)
        ElMessage.error('密码修改失败，请检查原密码是否正确')
      } finally {
        loading.value = false
      }
    }

// Clear all data
    const clearAllData = () => {
      ElMessageBox.confirm('确定要清空所有数据吗？此操作不可恢复！', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await api.clearAllData()
          ElMessage.success('数据已清空')
          await loadData()
        } catch (error) {
          console.error('Error clearing data:', error)
          ElMessage.error('清空数据失败')
        }
      }).catch(() => {
      })
    }

// Export data to CSV
    const exportCsv = () => {
      if (records.value.length === 0) {
        ElMessage.warning('没有数据可导出')
        return
      }

      // Create CSV content
      const headers = ['序号', '收入', '支出', '类别', '日期', '备注']
      const csvContent = [
        headers.join(','),
        ...records.value.map(record =>
            [
              record.id,
              record.income || 0,
              record.expense || 0,
              record.category,
              record.recordDate,
              `"${record.notes || ''}"`
            ].join(',')
        )
      ].join('\n')

      // Create download link
      const blob = new Blob([csvContent], {type: 'text/csv;charset=utf-8;'})
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.setAttribute('href', url)
      link.setAttribute('download', `财务记录_${new Date().toISOString().split('T')[0]}.csv`)
      link.style.display = 'none'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    }

// Handle row context menu (right click)
    const handleRowContextMenu = (row, column, event) => {
      // Prevent default browser context menu
      event.preventDefault()
    }

// Handle dropdown menu commands
    const handleCommand = (command) => {
      switch (command) {
        case 'statistics':
          router.push('/statistics')
          break
        case 'exportCsv':
          exportCsv()
          break
        case 'changePassword':
          passwordDialogVisible.value = true
          break
        case 'clearData':
          clearAllData()
          break
        case 'logout':
          localStorage.removeItem('isAuthenticated')
          router.push('/')
          ElMessage.success('已退出登录')
          break
      }
    }

// Load data when component is mounted
    onMounted(() => {
      loadData()
    })

    return {
      records,
      statistics,
      form,
      editForm,
      passwordForm,
      loading,
      dialogVisible,
      passwordDialogVisible,
      incomeCategories,
      expenseCategories,
      formatCurrency,
      addRecord,
      editRecord,
      updateRecord,
      confirmDelete,
      handleIncomeChange,
      handleExpenseChange,
      handleEditIncomeChange,
      handleEditExpenseChange,
      handleCommand,
      handleRowContextMenu,
      changePassword
    }
  }
}
</script>