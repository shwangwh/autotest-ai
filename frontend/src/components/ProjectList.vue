<template>
  <div class="project-hub">
    <!-- 左侧导航菜单 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h2>PROJECT HUB</h2>
      </div>
      <nav class="sidebar-nav">
        <ul>
          <li class="nav-item active">
            <span class="nav-label">重点项目调度</span>
          </li>
          <li class="nav-item">
            <span class="nav-label">政府投资项目调度</span>
          </li>
          <li class="nav-item">
            <span class="nav-label">专项项目调度</span>
          </li>
          <li class="nav-item">
            <span class="nav-label">项目调度报表</span>
          </li>
        </ul>
      </nav>
    </aside>

    <!-- 右侧主内容区 -->
    <main class="main-content">
      <!-- 顶部筛选栏 -->
      <div class="filter-section">
        <div class="filter-row">
          <div class="filter-item">
            <label>项目名称</label>
            <input v-model="filters.projectName" type="text" placeholder="请输入项目名称" />
          </div>
          <div class="filter-item">
            <label>总投资（万元）</label>
            <div class="range-inputs">
              <input v-model="filters.minInvestment" type="text" placeholder="请输入" />
              <span>-</span>
              <input v-model="filters.maxInvestment" type="text" placeholder="请输入" />
            </div>
          </div>
          <div class="filter-item">
            <label>项目代码</label>
            <input v-model="filters.projectCode" type="text" placeholder="请输入项目代码" />
          </div>
          <div class="filter-item">
            <label>期号</label>
            <select v-model="filters.period">
              <option value="2026W16">2026W16</option>
              <option value="2026W15">2026W15</option>
              <option value="2026W14">2026W14</option>
            </select>
          </div>
          <div class="filter-actions">
            <button class="primary-btn" @click="searchProjects">查询</button>
            <button class="ghost-btn" @click="resetFilters">重置</button>
          </div>
          <div class="filter-expand">
            <button class="ghost-btn">▼ 展开</button>
          </div>
        </div>

        <!-- 操作按钮区 -->
        <div class="action-buttons">
          <button class="ghost-btn">调度数据导入</button>
          <button class="ghost-btn">项目日历</button>
          <button class="ghost-btn">修改</button>
          <button class="ghost-btn">报表导出</button>
        </div>
      </div>

      <!-- 项目表格 -->
      <div class="table-section">
        <div v-if="loading" class="loading-state">正在加载项目数据...</div>
        <div v-else-if="!filteredProjects.length" class="empty-state">
          {{ hasFilters ? '没有符合条件的项目' : '当前还没有项目' }}
        </div>
        <div v-else class="table-container">
          <table class="project-table">
            <thead>
              <tr>
                <th class="checkbox-column">
                  <input type="checkbox" v-model="selectAll" @change="toggleSelectAll" />
                </th>
                <th>序号</th>
                <th>报送情况</th>
                <th>预警类型</th>
                <th>审核单位</th>
                <th>项目名称</th>
                <th>期号</th>
                <th>计划任务名称</th>
                <th>总投资（万元）</th>
                <th>本月完成投资（万元）</th>
                <th>累计完成投资（万元）</th>
              </tr>
            </thead>
            <tbody>
              <tr 
                v-for="(project, index) in filteredProjects" 
                :key="project.id" 
                :class="['project-row', project.highlight ? 'highlighted' : '']"
              >
                <td class="checkbox-column">
                  <input type="checkbox" v-model="selectedProjects" :value="project.id" />
                </td>
                <td>
                  <div class="row-info">
                    <span class="expand-icon" v-if="project.children && project.children.length">▼</span>
                    <span v-else></span>
                    {{ index + 1 }}
                    <span v-if="project.subIndex" class="sub-index">[{{ project.subIndex }}]</span>
                  </div>
                </td>
                <td>
                  <span :class="['status-badge', project.reportStatus]">
                    {{ reportStatusLabel(project.reportStatus) }}
                  </span>
                </td>
                <td>
                  <span :class="['alert-badge', project.alertType]">
                    {{ alertTypeLabel(project.alertType) }}
                  </span>
                </td>
                <td>{{ project.auditUnit }}</td>
                <td>
                  <div class="project-name">
                    <span v-if="project.important" class="important-icon">●</span>
                    {{ project.name }}
                  </div>
                </td>
                <td>{{ project.period }}</td>
                <td>{{ project.taskName }}</td>
                <td>{{ project.totalInvestment }}</td>
                <td>{{ project.monthlyInvestment }}</td>
                <td>{{ project.cumulativeInvestment }}</td>
              </tr>
              <!-- 子项目行 -->
              <tr 
                v-for="(child, childIndex) in flattenedChildren" 
                :key="child.id" 
                class="child-row"
              >
                <td class="checkbox-column">
                  <input type="checkbox" v-model="selectedProjects" :value="child.id" />
                </td>
                <td>
                  <div class="row-info">
                    <span class="child-indent"></span>
                    <span class="sub-index">{{ child.subIndex }}</span>
                  </div>
                </td>
                <td>
                  <span :class="['status-badge', child.reportStatus]">
                    {{ reportStatusLabel(child.reportStatus) }}
                  </span>
                </td>
                <td>
                  <span :class="['alert-badge', child.alertType]">
                    {{ alertTypeLabel(child.alertType) }}
                  </span>
                </td>
                <td>{{ child.auditUnit }}</td>
                <td>
                  <div class="project-name child-project">
                    <span v-if="child.important" class="important-icon">●</span>
                    {{ child.name }}
                  </div>
                </td>
                <td>{{ child.period }}</td>
                <td>{{ child.taskName }}</td>
                <td>{{ child.totalInvestment }}</td>
                <td>{{ child.monthlyInvestment }}</td>
                <td>{{ child.cumulativeInvestment }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { api, clearStoredToken, clearStoredUser, getStoredUser } from '../api'

export default {
  name: 'ProjectList',
  data() {
    return {
      projects: [],
      loading: false,
      filters: {
        projectName: '',
        minInvestment: '',
        maxInvestment: '',
        projectCode: '',
        period: '2026W16'
      },
      selectedProjects: [],
      selectAll: false
    }
  },
  mounted() {
    this.loadProjects()
  },
  computed: {
    filteredProjects() {
      let filtered = [...this.projects]
      
      // 应用筛选条件
      if (this.filters.projectName) {
        const query = this.filters.projectName.toLowerCase()
        filtered = filtered.filter(project => 
          project.name.toLowerCase().includes(query)
        )
      }
      
      if (this.filters.projectCode) {
        filtered = filtered.filter(project => 
          project.code && project.code.includes(this.filters.projectCode)
        )
      }
      
      if (this.filters.period) {
        filtered = filtered.filter(project => project.period === this.filters.period)
      }
      
      return filtered
    },
    hasFilters() {
      return Object.values(this.filters).some(value => value)
    },
    flattenedChildren() {
      const children = []
      this.filteredProjects.forEach(project => {
        if (project.children && project.children.length) {
          project.children.forEach((child, index) => {
            child.subIndex = index + 1
            children.push(child)
          })
        }
      })
      return children
    }
  },
  methods: {
    async loadProjects() {
      this.loading = true
      try {
        // 模拟数据，实际项目中应从API获取
        this.projects = [
          {
            id: 1,
            name: '测试调度流程',
            period: '2026W16',
            taskName: 'jiaxr调度测试',
            totalInvestment: 123,
            monthlyInvestment: 0,
            cumulativeInvestment: 11,
            reportStatus: 'pending',
            alertType: 'none',
            auditUnit: '省重点处',
            important: false,
            highlight: false
          },
          {
            id: 2,
            name: '2026年总体计划',
            period: '2026W16',
            taskName: '第一轮子调度项目',
            totalInvestment: 60599961,
            monthlyInvestment: 30,
            cumulativeInvestment: 121551,
            reportStatus: 'approved',
            alertType: 'none',
            auditUnit: '',
            important: true,
            highlight: false,
            children: [
              {
                id: 21,
                name: '下半年-新建20414128',
                period: '2026W16',
                taskName: '第一轮子调度项目',
                totalInvestment: 10099995,
                monthlyInvestment: 10,
                cumulativeInvestment: 20264,
                reportStatus: 'approved',
                alertType: 'none',
                auditUnit: '省重点处',
                important: false,
                highlight: false
              },
              {
                id: 22,
                name: '上半年-新建20414128',
                period: '2026W16',
                taskName: '第一轮子调度项目',
                totalInvestment: 10099992,
                monthlyInvestment: 0,
                cumulativeInvestment: 20251,
                reportStatus: 'approved',
                alertType: 'none',
                auditUnit: '省重点处',
                important: false,
                highlight: true
              }
            ]
          },
          {
            id: 3,
            name: '下半年-新建20414107',
            period: '2026W16',
            taskName: '1051第一轮报表...',
            totalInvestment: 10099995,
            monthlyInvestment: 10,
            cumulativeInvestment: 20264,
            reportStatus: 'approved',
            alertType: 'none',
            auditUnit: '',
            important: false,
            highlight: false
          }
        ]
      } catch (error) {
        console.error('加载项目失败:', error)
      } finally {
        this.loading = false
      }
    },
    searchProjects() {
      // 执行搜索逻辑
      console.log('搜索项目:', this.filters)
      // 这里可以调用API进行搜索
    },
    resetFilters() {
      this.filters = {
        projectName: '',
        minInvestment: '',
        maxInvestment: '',
        projectCode: '',
        period: '2026W16'
      }
    },
    toggleSelectAll() {
      if (this.selectAll) {
        const allIds = [...this.filteredProjects.map(p => p.id)]
        this.filteredProjects.forEach(project => {
          if (project.children) {
            project.children.forEach(child => {
              allIds.push(child.id)
            })
          }
        })
        this.selectedProjects = allIds
      } else {
        this.selectedProjects = []
      }
    },
    reportStatusLabel(status) {
      const map = {
        pending: '待审核',
        approved: '已审核'
      }
      return map[status] || status || '未知'
    },
    alertTypeLabel(type) {
      const map = {
        none: '无预警',
        warning: '预警'
      }
      return map[type] || type || '未知'
    }
  }
}
</script>

<style scoped>
.project-hub {
  display: flex;
  min-height: 100vh;
  font-family: 'Microsoft YaHei', 'Segoe UI', sans-serif;
  background: #f5f5f5;
}

/* 左侧导航菜单 */
.sidebar {
  width: 200px;
  background: #075985;
  color: white;
  padding: 20px 0;
  box-shadow: 2px 0 5px rgba(0,0,0,0.1);
}

.sidebar-header {
  padding: 0 20px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.sidebar-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.sidebar-nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav-item {
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 4px solid transparent;
}

.nav-item:hover {
  background: rgba(255,255,255,0.1);
}

.nav-item.active {
  background: rgba(255,255,255,0.15);
  border-left-color: #0EA5E9;
  font-weight: 700;
}

.nav-label {
  font-size: 14px;
}

/* 右侧主内容区 */
.main-content {
  flex: 1;
  padding: 20px;
  overflow-x: auto;
}

/* 顶部筛选栏 */
.filter-section {
  background: white;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-item label {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
}

.filter-item input,
.filter-item select {
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 150px;
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.range-inputs input {
  min-width: 100px;
}

.filter-actions {
  display: flex;
  gap: 10px;
}

.filter-expand {
  margin-left: auto;
}

.action-buttons {
  display: flex;
  gap: 10px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

/* 表格区域 */
.table-section {
  background: white;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.loading-state,
.empty-state {
  padding: 40px;
  text-align: center;
  color: #666;
}

.table-container {
  overflow-x: auto;
}

.project-table {
  width: 100%;
  border-collapse: collapse;
}

.project-table th {
  background: #075985;
  color: white;
  padding: 12px 10px;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  position: sticky;
  top: 0;
  z-index: 10;
}

.project-table td {
    padding: 10px;
    border-bottom: 1px solid #eee;
    font-size: 14px;
    text-align: left;
  }

.checkbox-column {
  width: 40px;
  text-align: center;
}

.project-row:hover {
  background: #f8f9fa;
}

.project-row.highlighted {
  background: #fff3cd;
}

.child-row {
  background: #f8f9fa;
}

.row-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expand-icon {
  font-size: 12px;
  cursor: pointer;
  color: #666;
}

.sub-index {
  font-size: 12px;
  color: #666;
}

.child-indent {
  width: 20px;
  display: inline-block;
}

.child-project {
  padding-left: 20px;
}

.project-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.important-icon {
  color: #28a745;
  font-size: 12px;
}

.status-badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.pending {
  background: #fff3cd;
  color: #856404;
}

.status-badge.approved {
  background: #d4edda;
  color: #155724;
}

.alert-badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.alert-badge.none {
  background: #d4edda;
  color: #155724;
}

.alert-badge.warning {
  background: #fff3cd;
  color: #856404;
}

/* 按钮样式 */
.primary-btn {
  background: #075985;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.primary-btn:hover {
  background: #0369A1;
}

.ghost-btn {
  background: #f8f9fa;
  color: #333;
  border: 1px solid #ddd;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.ghost-btn:hover {
  background: #e9ecef;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  
  .filter-item {
    flex-direction: column;
    align-items: stretch;
    gap: 4px;
  }
  
  .filter-item input,
  .filter-item select {
    width: 100%;
  }
  
  .range-inputs {
    justify-content: space-between;
  }
  
  .range-inputs input {
    flex: 1;
  }
  
  .filter-actions {
    justify-content: center;
  }
  
  .filter-expand {
    margin-left: 0;
    text-align: center;
  }
}

@media (max-width: 768px) {
  .project-hub {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
    padding: 10px 0;
  }
  
  .sidebar-nav ul {
    display: flex;
    overflow-x: auto;
  }
  
  .nav-item {
    white-space: nowrap;
    border-left: none;
    border-bottom: 4px solid transparent;
  }
  
  .nav-item.active {
    border-left: none;
    border-bottom-color: #0EA5E9;
  }
  
  .main-content {
    padding: 10px;
  }
  
  .table-section {
    overflow-x: auto;
  }
  
  .project-table {
    min-width: 800px;
  }
}
</style>
