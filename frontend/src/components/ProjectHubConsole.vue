<template>
  <div class="pc-layout">
    <!-- ===== 左侧导航栏 ===== -->
    <aside class="pc-sidebar">
      <div class="pc-sidebar-header">
        <div class="pc-logo">
          <span class="pc-logo-icon">Q</span>
          <span class="pc-logo-text">智能测试平台</span>
        </div>
      </div>
      <nav class="pc-sidebar-nav">
        <div class="pc-nav-item pc-nav-item--active">
          <svg class="pc-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
          <span>项目中心</span>
        </div>
        <div class="pc-nav-item" @click="navigateToWorkbench('requirements')">
          <svg class="pc-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          <span>需求管理</span>
        </div>
        <div class="pc-nav-item" @click="navigateToWorkbench('generate_cases')">
          <svg class="pc-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/><path d="M9 14l2 2 4-4"/></svg>
          <span>用例管理</span>
        </div>
        <div class="pc-nav-item" @click="navigateToWorkbench('api')">
          <svg class="pc-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
          <span>接口测试</span>
        </div>
        <div class="pc-nav-item" @click="navigateToWorkbench('web_automation')">
          <svg class="pc-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="14" rx="2"/><path d="M8 20h8"/><path d="M12 18v2"/></svg>
          <span>Web自动化</span>
        </div>
      </nav>
      <div class="pc-sidebar-footer">
        <div class="pc-user-info">
          <span class="pc-avatar">{{ currentUser.charAt(0).toUpperCase() }}</span>
          <span class="pc-user-name">{{ currentUser }}</span>
        </div>
        <button class="pc-btn-logout" @click="logout" title="退出登录">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        </button>
      </div>
    </aside>

    <!-- ===== 主内容区 ===== -->
    <main class="pc-main">
      <!-- 面包屑顶栏 -->
      <div class="pc-topbar">
        <div class="pc-breadcrumb">
          <span class="pc-breadcrumb-item pc-breadcrumb-item--active">项目中心</span>
        </div>
        <div class="pc-topbar-right">
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="pc-stats">
        <div class="pc-stat-card">
          <span class="pc-stat-label">项目总数</span>
          <strong class="pc-stat-value">{{ totalProjects }}</strong>
        </div>
        <div class="pc-stat-card">
          <span class="pc-stat-label">需求总数</span>
          <strong class="pc-stat-value">{{ totalRequirements }}</strong>
        </div>
        <div class="pc-stat-card">
          <span class="pc-stat-label">用例总数</span>
          <strong class="pc-stat-value">{{ totalCases }}</strong>
        </div>
        <div class="pc-stat-card">
          <span class="pc-stat-label">执行任务</span>
          <strong class="pc-stat-value">{{ totalTasks }}</strong>
        </div>
      </div>

      <!-- 筛选区域 -->
      <div class="pc-filter-bar">
        <div class="pc-filter-row">
          <div class="pc-filter-group">
            <label class="pc-filter-label">项目名称</label>
            <input
              v-model.trim="search"
              type="text"
              class="pc-filter-input pc-filter-input--wide"
              placeholder="请输入项目名称搜索"
            />
          </div>
          <div class="pc-filter-group">
            <label class="pc-filter-label">负责人</label>
            <input
              v-model.trim="ownerSearch"
              type="text"
              class="pc-filter-input"
              placeholder="请输入负责人"
            />
          </div>
          <div class="pc-filter-group">
            <label class="pc-filter-label">状态</label>
            <select v-model="statusFilter" class="pc-filter-select">
              <option value="">全部</option>
              <option value="ACTIVE">活跃</option>
              <option value="ARCHIVED">归档</option>
            </select>
          </div>
          <div class="pc-filter-actions">
            <button class="pc-btn pc-btn--primary" @click="loadProjects">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
              查询
            </button>
            <button class="pc-btn" @click="resetFilters">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
              重置
            </button>
          </div>
        </div>
      </div>

      <!-- 操作工具栏 -->
      <div class="pc-toolbar">
        <div class="pc-toolbar-left">
          <button class="pc-btn pc-btn--primary" @click="showCreateDialog = true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            新建项目
          </button>
          <button
            class="pc-btn pc-btn--danger"
            :disabled="!selectedProjects.length"
            @click="batchDeleteProjects"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
            批量删除
          </button>
        </div>
        <div class="pc-toolbar-right">
          <span class="pc-toolbar-info">
            已选 <strong>{{ selectedProjects.length }}</strong> 个项目，共
            <strong>{{ filteredProjects.length }}</strong> 个
          </span>
        </div>
      </div>

      <!-- 新建项目对话框 -->
      <div v-if="showCreateDialog" class="pc-dialog-overlay" @click="showCreateDialog = false">
        <div class="pc-dialog" @click.stop>
          <div class="pc-dialog-header">
            <h3>新建项目</h3>
            <button class="pc-dialog-close" @click="showCreateDialog = false">×</button>
          </div>
          <div class="pc-dialog-body">
            <form class="pc-dialog-form" @submit.prevent="createProject">
              <div class="pc-dialog-form-group">
                <label class="pc-dialog-form-label">项目名称 <em>*</em></label>
                <input v-model.trim="form.name" type="text" class="pc-dialog-form-input" placeholder="请输入项目名称" />
              </div>
              <div class="pc-dialog-form-group">
                <label class="pc-dialog-form-label">负责人</label>
                <input v-model.trim="form.owner" type="text" class="pc-dialog-form-input" placeholder="请输入负责人" />
              </div>
              <div class="pc-dialog-form-group">
                <label class="pc-dialog-form-label">项目描述</label>
                <input v-model.trim="form.description" type="text" class="pc-dialog-form-input" placeholder="请输入项目描述" />
              </div>
              <div class="pc-dialog-form-actions">
                <button class="pc-btn pc-btn--success" type="submit" :disabled="saving">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="20 6 9 17 4 12"/></svg>
                  {{ saving ? '创建中...' : '确认' }}
                </button>
                <button class="pc-btn" type="button" @click="showCreateDialog = false">取消</button>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- 编辑项目对话框 -->
      <div v-if="showEditDialog" class="pc-dialog-overlay" @click="showEditDialog = false">
        <div class="pc-dialog" @click.stop>
          <div class="pc-dialog-header">
            <h3>编辑项目</h3>
            <button class="pc-dialog-close" @click="showEditDialog = false">×</button>
          </div>
          <div class="pc-dialog-body">
            <form class="pc-dialog-form" @submit.prevent="updateProject">
              <div class="pc-dialog-form-group">
                <label class="pc-dialog-form-label">项目名称 <em>*</em></label>
                <input v-model.trim="editForm.name" type="text" class="pc-dialog-form-input" placeholder="请输入项目名称" />
              </div>
              <div class="pc-dialog-form-group">
                <label class="pc-dialog-form-label">负责人</label>
                <input v-model.trim="editForm.owner" type="text" class="pc-dialog-form-input" placeholder="请输入负责人" />
              </div>
              <div class="pc-dialog-form-group">
                <label class="pc-dialog-form-label">项目描述</label>
                <input v-model.trim="editForm.description" type="text" class="pc-dialog-form-input" placeholder="请输入项目描述" />
              </div>
              <div class="pc-dialog-form-group">
                <label class="pc-dialog-form-label">状态</label>
                <select v-model="editForm.status" class="pc-dialog-form-input">
                  <option value="ACTIVE">活跃</option>
                  <option value="ARCHIVED">归档</option>
                </select>
              </div>
              <div class="pc-dialog-form-actions">
                <button class="pc-btn pc-btn--success" type="submit" :disabled="saving">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="20 6 9 17 4 12"/></svg>
                  {{ saving ? '保存中...' : '保存' }}
                </button>
                <button class="pc-btn" type="button" @click="showEditDialog = false">取消</button>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- 编辑项目对话框 -->
      <div v-if="showEditDialog" class="pc-dialog-overlay" @click="showEditDialog = false">
        <div class="pc-dialog" @click.stop>
          <div class="pc-dialog-header">
            <h3>编辑项目</h3>
            <button class="pc-dialog-close" @click="showEditDialog = false">×</button>
          </div>
          <div class="pc-dialog-body">
            <form class="pc-dialog-form" @submit.prevent="updateProject">
              <div class="pc-dialog-form-group">
                <label class="pc-dialog-form-label">项目名称 <em>*</em></label>
                <input v-model.trim="editForm.name" type="text" class="pc-dialog-form-input" placeholder="请输入项目名称" />
              </div>
              <div class="pc-dialog-form-group">
                <label class="pc-dialog-form-label">负责人</label>
                <input v-model.trim="editForm.owner" type="text" class="pc-dialog-form-input" placeholder="请输入负责人" />
              </div>
              <div class="pc-dialog-form-group">
                <label class="pc-dialog-form-label">项目描述</label>
                <input v-model.trim="editForm.description" type="text" class="pc-dialog-form-input" placeholder="请输入项目描述" />
              </div>
              <div class="pc-dialog-form-group">
                <label class="pc-dialog-form-label">状态</label>
                <select v-model="editForm.status" class="pc-dialog-form-input">
                  <option value="ACTIVE">活跃</option>
                  <option value="ARCHIVED">归档</option>
                </select>
              </div>
              <div class="pc-dialog-form-actions">
                <button class="pc-btn pc-btn--success" type="submit" :disabled="saving">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="20 6 9 17 4 12"/></svg>
                  {{ saving ? '保存中...' : '保存' }}
                </button>
                <button class="pc-btn" type="button" @click="showEditDialog = false">取消</button>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- 数据表格 -->
      <div class="pc-table-container">
        <div v-if="loading" class="pc-loading">
          <div class="pc-spinner"></div>
          <span>正在加载项目数据...</span>
        </div>
        <div v-else-if="!filteredProjects.length" class="pc-empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="color:#aaa"><rect x="2" y="3" width="20" height="18" rx="2"/><path d="M8 7h8M8 11h5"/></svg>
          <h3>{{ hasFilters ? '没有符合条件的项目' : '暂无项目' }}</h3>
          <p>{{ hasFilters ? '请调整筛选条件后重试' : '点击"新建项目"创建第一个项目' }}</p>
        </div>
        <table v-else class="pc-table">
          <thead>
            <tr>
              <th style="width:40px">
                <input type="checkbox" class="pc-checkbox" v-model="selectAll" @change="toggleSelectAll" />
              </th>
              <th style="width:60px">序号</th>
              <th style="min-width:200px">项目名称</th>
              <th style="width:100px">负责人</th>
              <th style="min-width:200px">项目描述</th>
              <th style="width:70px">需求数</th>
              <th style="width:70px">用例数</th>
              <th style="width:70px">任务数</th>
              <th style="width:150px">创建时间</th>
              <th style="width:80px">状态</th>
              <th style="width:180px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(project, index) in filteredProjects"
              :key="project.id"
              :class="{ 'row-selected': selectedProjects.includes(project.id) }"
            >
              <td>
                <input
                  type="checkbox"
                  class="pc-checkbox"
                  :value="project.id"
                  v-model="selectedProjects"
                />
              </td>
              <td>{{ index + 1 }}</td>
              <td class="text-left pc-text-cell">
                {{ truncateText(project.name) }}
              </td>
              <td>{{ project.owner || 'admin' }}</td>
              <td class="text-left pc-desc-cell">{{ truncateText(project.description || '暂无描述') }}</td>
              <td><span class="pc-num-highlight">{{ project.requirementCount || 0 }}</span></td>
              <td><span class="pc-num-highlight">{{ project.caseCount || 0 }}</span></td>
              <td><span class="pc-num-highlight">{{ project.taskCount || 0 }}</span></td>
              <td>{{ formatDateTime(project.createdAt) }}</td>
              <td>
                <span :class="['pc-status', statusClass(project.status)]">
                  {{ statusLabel(project.status) }}
                </span>
              </td>
              <td>
                <div class="pc-action-cell">
                  <button class="pc-btn-text pc-btn-text--primary" @click="editProject(project)" :disabled="saving">
                    编辑
                  </button>
                  <button class="pc-btn-text pc-btn-text--danger" @click="deleteProject(project)" :disabled="saving">
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="pc-pagination" v-if="filteredProjects.length">
        <span class="pc-pagination-info">
          共 <strong>{{ filteredProjects.length }}</strong> 个项目
        </span>
        <div class="pc-pagination-pages">
          <button class="pc-page-btn" disabled>&lt;</button>
          <button class="pc-page-btn pc-page-btn--active">1</button>
          <button class="pc-page-btn" disabled>&gt;</button>
        </div>
        <select class="pc-pagination-select">
          <option>20条/页</option>
          <option>50条/页</option>
        </select>
      </div>
    </main>

    <!-- Toast 消息 -->
    <transition name="toast">
      <div v-if="error" class="pc-toast pc-toast--error" @click="error = ''">{{ error }}</div>
    </transition>
    <transition name="toast">
      <div v-if="info" class="pc-toast pc-toast--success" @click="info = ''">{{ info }}</div>
    </transition>
  </div>
</template>

<script>
import { api, clearStoredToken, clearStoredUser, getStoredUser } from '../api'

export default {
  name: 'ProjectHubConsole',
  data() {
    return {
      projects: [],
      loading: false,
      saving: false,
      search: '',
      ownerSearch: '',
      statusFilter: '',
      selectedProjects: [],
      selectAll: false,
      showCreateDialog: false,
      showEditDialog: false,
      error: '',
      info: '',
      currentUser: getStoredUser() || 'admin',
      lastProjectId: localStorage.getItem('lastProjectId'),
      form: {
        name: '',
        owner: getStoredUser() || 'admin',
        description: ''
      },
      editForm: {
        id: null,
        name: '',
        owner: '',
        description: '',
        status: 'ACTIVE'
      }
    }
  },
  computed: {
    filteredProjects() {
      let result = [...this.projects]
      // 按创建时间倒序排列（越新越靠前）
      result.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
      if (this.search) {
        const q = this.search.toLowerCase()
        result = result.filter(p => (p.name || '').toLowerCase().includes(q))
      }
      if (this.ownerSearch) {
        const q = this.ownerSearch.toLowerCase()
        result = result.filter(p => (p.owner || '').toLowerCase().includes(q))
      }
      if (this.statusFilter) {
        result = result.filter(p => p.status === this.statusFilter)
      }
      return result
    },
    hasFilters() {
      return !!(this.search || this.ownerSearch || this.statusFilter)
    },
    totalProjects() {
      return this.projects.length
    },
    totalRequirements() {
      return this.projects.reduce((sum, project) => sum + (project.requirementCount || 0), 0)
    },
    totalCases() {
      return this.projects.reduce((sum, project) => sum + (project.caseCount || 0), 0)
    },
    totalTasks() {
      return this.projects.reduce((sum, project) => sum + (project.taskCount || 0), 0)
    }
  },
  mounted() {
    this.loadProjects()
  },
  methods: {
    async loadProjects() {
      this.loading = true
      this.error = ''
      try {
        console.log('开始加载项目数据...')
        const response = await api.get('/api/projects')
        console.log('项目数据加载成功:', response)
        this.projects = response
      } catch (error) {
        console.error('加载项目失败:', error)
        this.error = error.message || '加载项目失败。'
      } finally {
        this.loading = false
      }
    },
    async createProject() {
      if (!this.form.name) {
        this.error = '请先填写项目名称。'
        return
      }
      this.saving = true
      this.error = ''
      this.info = ''
      try {
        console.log('开始创建项目...', this.form)
        await api.post('/api/projects', {
          name: this.form.name,
          owner: this.form.owner || 'admin',
          description: this.form.description,
          status: 'ACTIVE',
          reviewStatus: 'UNREVIEWED'
        })
        console.log('项目创建成功')
        this.info = '项目创建成功。'
        this.form.name = ''
        this.form.description = ''
        this.showCreateDialog = false
        await this.loadProjects()
      } catch (error) {
        console.error('创建项目失败:', error)
        this.error = error.message || '创建项目失败。'
      } finally {
        this.saving = false
      }
    },
    async deleteProject(project) {
      if (!window.confirm(`确认删除项目"${project.name}"吗？此操作不可恢复。`)) return
      this.saving = true
      this.error = ''
      this.info = ''
      try {
        await api.delete(`/api/projects/${project.id}`)
        this.info = '项目已删除。'
        this.selectedProjects = this.selectedProjects.filter(id => id !== project.id)
        await this.loadProjects()
      } catch (error) {
        this.error = error.message || '删除项目失败。'
      } finally {
        this.saving = false
      }
    },
    async batchDeleteProjects() {
      if (!this.selectedProjects.length) return
      if (!window.confirm(`确认删除选中的 ${this.selectedProjects.length} 个项目吗？`)) return
      this.saving = true
      this.error = ''
      this.info = ''
      try {
        for (const id of this.selectedProjects) {
          await api.delete(`/api/projects/${id}`)
        }
        this.info = `已删除 ${this.selectedProjects.length} 个项目。`
        this.selectedProjects = []
        this.selectAll = false
        await this.loadProjects()
      } catch (error) {
        this.error = error.message || '批量删除失败。'
      } finally {
        this.saving = false
      }
    },
    editProject(project) {
      this.editForm = {
        id: project.id,
        name: project.name || '',
        owner: project.owner || '',
        description: project.description || '',
        status: project.status || 'ACTIVE'
      }
      this.showEditDialog = true
    },
    async updateProject() {
      if (!this.editForm.name) {
        this.error = '请先填写项目名称。'
        return
      }
      this.saving = true
      this.error = ''
      this.info = ''
      try {
        await api.put(`/api/projects/${this.editForm.id}`, {
          name: this.editForm.name,
          owner: this.editForm.owner,
          description: this.editForm.description,
          status: this.editForm.status
        })
        this.info = '项目更新成功。'
        this.showEditDialog = false
        await this.loadProjects()
      } catch (error) {
        this.error = error.message || '更新项目失败。'
      } finally {
        this.saving = false
      }
    },
    toggleSelectAll() {
      this.selectedProjects = this.selectAll
        ? this.filteredProjects.map(p => p.id)
        : []
    },
    resetFilters() {
      this.search = ''
      this.ownerSearch = ''
      this.statusFilter = ''
    },

    statusLabel(status) {
      return { ACTIVE: '活跃', ARCHIVED: '归档' }[status] || '活跃'
    },
    statusClass(status) {
      return {
        ACTIVE: 'pc-status--active',
        ARCHIVED: 'pc-status--archived'
      }[status] || 'pc-status--active'
    },
    formatDateTime(dateTime) {
      if (!dateTime) return '-'
      const d = new Date(dateTime)
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    },
    truncateText(text, maxLength = 40) {
      if (!text) return ''
      return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
    },
    navigateToWorkbench(module) {
      // 导航到工作台页面，使用第一个项目的ID作为默认值
      // 由于我们修改了工作台页面，使其加载所有项目的数据，所以项目ID实际上不再重要
      this.$router.push(`/project/1?module=${module}`)
    },
    logout() {
      clearStoredToken()
      clearStoredUser()
      this.$router.push('/login')
    }
  },
  watch: {
    error(v) { if (v) setTimeout(() => { this.error = '' }, 4000) },
    info(v) { if (v) setTimeout(() => { this.info = '' }, 3000) }
  }
}
</script>

<style scoped>
/* ============================================================
   项目中心 - 参考图布局：左侧导航 + 面包屑 + 筛选 + 工具栏 + 表格 + 分页
   ============================================================ */

/* === 变量 === */
:root {
  --pc-sky: #0EA5E9;
  --pc-sky-dark: #0284C7;
  --pc-sky-light: #BAE6FD;
  --pc-sky-lighter: #E0F2FE;
  --pc-sky-bg: #F0F9FF;
}

/* === 整体布局 === */
.pc-layout {
  display: flex;
  width: 100%;
  min-height: 100vh;
  background: #f5f6f8;
  font-family: 'Microsoft YaHei', 'PingFang SC', -apple-system, sans-serif;
}

/* === 左侧导航栏 === */
.pc-sidebar {
  width: 200px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: 2px 0 6px rgba(0,0,0,0.03);
}

.pc-sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.pc-logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pc-logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #0EA5E9, #0369A1);
  color: #fff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
}

.pc-logo-text {
  font-size: 14px;
  font-weight: 700;
  color: #333;
}

.pc-sidebar-nav {
  flex: 1;
  padding: 8px 0;
}

.pc-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 20px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.pc-nav-item:hover {
  background: #E0F2FE;
  color: #0284C7;
}

.pc-nav-item--active {
  background: #E0F2FE;
  color: #0369A1;
  font-weight: 600;
  border-left-color: #0EA5E9;
}

.pc-nav-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.pc-sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pc-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pc-avatar {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #0EA5E9, #0369A1);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.pc-user-name {
  font-size: 12px;
  color: #555;
}

.pc-btn-logout {
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.pc-btn-logout:hover {
  color: #f44336;
  background: #fef0f0;
}

/* === 主内容区 === */
.pc-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* === 面包屑顶栏 === */
.pc-topbar {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 8px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

/* === 统计卡片 === */
.pc-stats {
  display: flex;
  gap: 14px;
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.pc-stat-card {
  flex: 1;
  padding: 14px;
  background: #F0F9FF;
  border-radius: 8px;
  border: 1px solid #BAE6FD;
  text-align: center;
}

.pc-stat-label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.pc-stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #0EA5E9;
}

.pc-breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.pc-breadcrumb-item { color: #888; cursor: pointer; }
.pc-breadcrumb-item:hover { color: #0EA5E9; }
.pc-breadcrumb-item--active { color: #333; font-weight: 500; cursor: default; }
.pc-breadcrumb-item--active:hover { color: #333; }
.pc-breadcrumb-sep { color: #bbb; font-size: 12px; }

.pc-topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* === 筛选区域 === */
.pc-filter-bar {
  background: #fff;
  padding: 14px 20px;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.pc-filter-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.pc-filter-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pc-filter-label {
  font-size: 13px;
  color: #555;
  white-space: nowrap;
}

.pc-filter-label em {
  color: #f44336;
  font-style: normal;
}

.pc-filter-input {
  height: 32px;
  padding: 0 10px;
  font-size: 13px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
  color: #333;
  background: #fff;
  min-width: 140px;
  transition: border-color 0.2s;
}

.pc-filter-input:hover { border-color: #0EA5E9; }
.pc-filter-input:focus { border-color: #0EA5E9; box-shadow: 0 0 0 2px rgba(14,165,233,0.12); }
.pc-filter-input::placeholder { color: #bbb; }
.pc-filter-input--wide { min-width: 200px; }

.pc-filter-select {
  height: 32px;
  padding: 0 28px 0 10px;
  font-size: 13px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
  color: #333;
  background: #fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M0 0l5 6 5-6z' fill='%23888'/%3E%3C/svg%3E") no-repeat right 8px center;
  appearance: none;
  cursor: pointer;
  min-width: 100px;
  transition: border-color 0.2s;
}

.pc-filter-select:hover { border-color: #0EA5E9; }
.pc-filter-select:focus { border-color: #0EA5E9; box-shadow: 0 0 0 2px rgba(14,165,233,0.12); }

.pc-filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

/* === 按钮 === */
.pc-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 32px;
  padding: 0 14px;
  font-size: 13px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  outline: none;
  background: #fff;
  color: #555;
  font-family: inherit;
}

.pc-btn:hover { color: #0EA5E9; border-color: #BAE6FD; }
.pc-btn:active { transform: scale(0.97); }

.pc-btn svg { width: 14px; height: 14px; flex-shrink: 0; }

.pc-btn--primary {
  background: #0EA5E9;
  border-color: #0EA5E9;
  color: #fff;
}

.pc-btn--primary:hover { background: #0284C7; border-color: #0284C7; color: #fff; }

.pc-btn--success {
  background: #0EA5E9;
  border-color: #0EA5E9;
  color: #fff;
}

.pc-btn--success:hover { background: #0284C7; border-color: #0284C7; color: #fff; }

.pc-btn--danger {
  background: #fff;
  border-color: #fbc4c4;
  color: #f44336;
}

.pc-btn--danger:hover { background: #fef0f0; border-color: #f44336; color: #f44336; }

.pc-btn--sm {
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
}

.pc-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* === 工具栏 === */
.pc-toolbar {
  background: #fff;
  padding: 10px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.pc-toolbar-left,
.pc-toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pc-toolbar-info {
  font-size: 12px;
  color: #888;
}

.pc-toolbar-info strong {
  color: #0EA5E9;
  font-weight: 600;
}

/* === 新建项目面板 === */
.pc-create-panel {
  background: #F0F9FF;
  border-bottom: 1px solid #BAE6FD;
  padding: 14px 20px;
  flex-shrink: 0;
}

.pc-create-form {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

/* === 表格 === */
.pc-table-container {
  flex: 1;
  overflow: auto;
  background: #fff;
}

.pc-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.pc-table thead {
  position: sticky;
  top: 0;
  z-index: 2;
}

.pc-table th {
  height: 40px;
  padding: 8px 12px;
  font-weight: 600;
  font-size: 13px;
  color: #555;
  background: #fafbfc;
  border-bottom: 2px solid #BAE6FD;
  text-align: center;
  white-space: nowrap;
}

.pc-table td {
  height: 46px;
  padding: 8px 12px;
  color: #333;
  border-bottom: 1px solid #ebeef5;
  text-align: center;
  white-space: nowrap;
}

.pc-table td.text-left { text-align: left; }

.pc-table tbody tr { transition: background 0.15s; }
.pc-table tbody tr:hover { background: #F0F9FF; }
.pc-table tbody tr:nth-child(even) { background: #F8FCFF; }
.pc-table tbody tr:nth-child(even):hover { background: #F0F9FF; }
.pc-table tbody tr.row-selected { background: #E0F2FE !important; }

.pc-checkbox {
  width: 15px;
  height: 15px;
  accent-color: #0EA5E9;
  cursor: pointer;
}

.pc-project-link {
  color: #0EA5E9;
  cursor: pointer;
  text-decoration: none;
  font-weight: 500;
}

.pc-project-link:hover {
  color: #0369A1;
  text-decoration: underline;
}

.pc-desc-cell {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #888 !important;
  font-size: 12px;
}

.pc-text-cell {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pc-num-highlight {
  color: #0EA5E9;
  font-weight: 600;
}

/* === 状态标签 === */
.pc-status {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  font-size: 12px;
  border-radius: 3px;
  white-space: nowrap;
}

.pc-status--active {
  color: #0EA5E9;
  background: #E0F2FE;
  border: 1px solid #BAE6FD;
}

.pc-status--archived {
  color: #909399;
  background: #f4f4f5;
  border: 1px solid #e0e0e0;
}

/* === 操作按钮 === */
.pc-action-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.pc-btn-text {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 3px;
  transition: all 0.2s;
  font-family: inherit;
}

.pc-btn-text--primary { color: #0EA5E9; }
.pc-btn-text--primary:hover { background: #E0F2FE; color: #0369A1; }
.pc-btn-text--danger { color: #f44336; }
.pc-btn-text--danger:hover { background: #fef0f0; color: #d32f2f; }
.pc-btn-text:disabled { opacity: 0.4; cursor: not-allowed; }

/* === 分页 === */
.pc-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  flex-shrink: 0;
  font-size: 13px;
}

.pc-pagination-info { color: #888; }
.pc-pagination-info strong { color: #0EA5E9; }

.pc-pagination-pages {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pc-page-btn {
  min-width: 28px;
  height: 28px;
  padding: 0 6px;
  font-size: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  background: #fff;
  color: #555;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  font-family: inherit;
}

.pc-page-btn:hover { color: #0EA5E9; border-color: #BAE6FD; }
.pc-page-btn--active { background: #0EA5E9; border-color: #0EA5E9; color: #fff; }
.pc-page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.pc-pagination-select {
  height: 28px;
  padding: 0 20px 0 8px;
  font-size: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  background: #fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M0 0l5 6 5-6z' fill='%23888'/%3E%3C/svg%3E") no-repeat right 6px center;
  appearance: none;
  cursor: pointer;
  outline: none;
  color: #555;
  font-family: inherit;
}

/* === 空状态 & 加载 === */
.pc-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 20px;
  color: #888;
  font-size: 14px;
}

.pc-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #E0F2FE;
  border-top-color: #0EA5E9;
  border-radius: 50%;
  animation: pc-spin 0.8s linear infinite;
}

@keyframes pc-spin {
  to { transform: rotate(360deg); }
}

.pc-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #888;
}

.pc-empty h3 { margin: 16px 0 6px; color: #666; font-size: 15px; }
.pc-empty p { margin: 0; font-size: 13px; color: #aaa; }

/* === Toast 提示 === */
.pc-toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 28px;
  border-radius: 6px;
  font-size: 13px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  z-index: 9999;
  cursor: pointer;
}

.pc-toast--success {
  background: #E0F2FE;
  color: #0369A1;
  border: 1px solid #BAE6FD;
}

.pc-toast--error {
  background: #fef0f0;
  color: #f44336;
  border: 1px solid #fbc4c4;
}

/* === 动画 === */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
  opacity: 0;
}

.slide-enter-to,
.slide-leave-from {
  max-height: 100px;
  opacity: 1;
}

.toast-enter-active { transition: all 0.3s ease; }
.toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from { opacity: 0; transform: translateX(-50%) translateY(-12px); }
.toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(-12px); }

/* === 对话框样式 === */
.pc-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.pc-dialog {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  width: 100%;
  max-width: 500px;
  overflow: hidden;
  animation: pc-dialog-fade-in 0.3s ease;
}

@keyframes pc-dialog-fade-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.pc-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafbfc;
}

.pc-dialog-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.pc-dialog-close {
  background: none;
  border: none;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.pc-dialog-close:hover {
  color: #666;
  background: #f0f0f0;
}

.pc-dialog-body {
  padding: 20px;
}

.pc-dialog-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pc-dialog-form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pc-dialog-form-label {
  font-size: 13px;
  color: #555;
  font-weight: 500;
}

.pc-dialog-form-label em {
  color: #f44336;
  font-style: normal;
}

.pc-dialog-form-input {
  height: 36px;
  padding: 0 12px;
  font-size: 13px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
  color: #333;
  background: #fff;
  transition: border-color 0.2s;
}

.pc-dialog-form-input:hover {
  border-color: #0EA5E9;
}

.pc-dialog-form-input:focus {
  border-color: #0EA5E9;
  box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.12);
}

.pc-dialog-form-input::placeholder {
  color: #bbb;
}

.pc-dialog-form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

/* === 响应式 === */
@media (max-width: 1024px) {
  .pc-sidebar { width: 160px; }
  .pc-filter-row { flex-wrap: wrap; }
}

@media (max-width: 768px) {
  .pc-layout { flex-direction: column; }
  .pc-sidebar {
    width: 100%;
    flex-direction: row;
    height: auto;
    overflow-x: auto;
  }
  .pc-sidebar-nav { display: flex; flex-direction: row; }
  .pc-nav-item { border-left: none; border-bottom: 3px solid transparent; }
  .pc-nav-item--active { border-left: none; border-bottom-color: #0EA5E9; }
  .pc-dialog {
    margin: 0 20px;
    max-width: none;
  }
}
</style>
