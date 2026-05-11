<template>
  <div class="ms-layout">
    <!-- 顶部导航栏 -->
    <header class="ms-header">
      <a class="ms-header__logo" href="#">
        <span class="ms-header__logo-icon">MS</span>
        MeterSphere
      </a>
      <nav class="ms-header__nav">
        <a
          v-for="item in navItems"
          :key="item.key"
          class="ms-header__nav-item"
          :class="{ 'is-active': activeNav === item.key }"
          href="#"
          @click.prevent="activeNav = item.key"
        >
          {{ item.label }}
        </a>
      </nav>
      <div class="ms-header__actions">
        <span class="ms-header__action-icon" title="通知">🔔</span>
        <span class="ms-header__action-icon" title="帮助">❓</span>
        <span class="ms-header__action-icon" title="设置">⚙️</span>
      </div>
    </header>

    <div class="ms-body">
      <!-- 侧边栏 -->
      <aside class="ms-sidebar">
        <a
          v-for="menu in sidebarMenus"
          :key="menu.key"
          class="ms-sidebar__item"
          :class="{ 'is-active': activeMenu === menu.key }"
          href="#"
          @click.prevent="activeMenu = menu.key"
        >
          <span class="ms-sidebar__icon">{{ menu.icon }}</span>
          {{ menu.label }}
        </a>
      </aside>

      <!-- 主内容区 -->
      <main class="ms-main">
        <!-- 面包屑 -->
        <div class="ms-breadcrumb">
          <a class="ms-breadcrumb__item" href="#">功能用例</a>
          <span class="ms-breadcrumb__separator">/</span>
          <span class="ms-breadcrumb__item is-current">用例列表</span>
        </div>

        <div class="ms-content-card">
          <div style="display: flex; gap: 0; min-height: 500px;">
            <!-- 模块树面板 -->
            <div class="ms-tree-panel">
              <div class="ms-tree-panel__search">
                <div class="ms-input" style="width: 100%;">
                  <input
                    v-model="moduleSearch"
                    type="text"
                    class="ms-input__inner"
                    placeholder="请输入模块名称"
                  >
                  <span class="ms-input__suffix">🔍</span>
                </div>
              </div>
              <div class="ms-tree-panel__header">
                <span>📂 全部用例 ({{ totalCases }})</span>
                <MsButton type="icon" title="新建模块" style="width:24px;height:24px;" @click="handleAddModule">
                  ✚
                </MsButton>
              </div>
              <div
                v-for="mod in filteredModules"
                :key="mod.id"
                class="ms-tree-item"
                :class="{ 'is-active': activeModule === mod.id }"
                @click="activeModule = mod.id"
              >
                <span>{{ mod.name }}</span>
                <span class="ms-tree-item__count">{{ mod.count }}</span>
              </div>
              <hr class="ms-sidebar__divider">
              <div class="ms-tree-item" style="color: var(--ms-text-3);" @click="showRecycleBin = !showRecycleBin">
                <span>🗑️ 回收站</span>
                <span class="ms-tree-item__count">{{ recycleBinCount }}</span>
              </div>
            </div>

            <!-- 右侧列表区 -->
            <div style="flex: 1; padding: 0 0 0 16px; border-left: 1px solid var(--ms-border);">
              <!-- 工具栏 -->
              <div class="ms-toolbar">
                <div class="ms-toolbar__left">
                  <MsButton type="primary" @click="handleCreate">新建</MsButton>
                  <MsButton type="outline" @click="handleImport">导入</MsButton>
                </div>
                <div class="ms-toolbar__right">
                  <div class="ms-input" style="width: 220px;">
                    <input
                      v-model="searchQuery"
                      type="text"
                      class="ms-input__inner"
                      placeholder="通过 ID/名称/标签搜索"
                    >
                    <span class="ms-input__suffix">🔍</span>
                  </div>
                  <MsButton type="icon" title="视图切换">☰</MsButton>
                </div>
              </div>

              <!-- 表格 -->
              <table class="ms-table">
                <thead>
                  <tr>
                    <th style="width: 40px;">
                      <input
                        type="checkbox"
                        class="ms-table__checkbox"
                        :class="{ 'ms-table__checkbox--indeterminate': isIndeterminate }"
                        :checked="isAllSelected"
                        @change="toggleSelectAll"
                      >
                    </th>
                    <th>ID ⇅</th>
                    <th>用例名称</th>
                    <th>评审结果 ▾</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="item in filteredCases"
                    :key="item.id"
                    :class="{ 'is-selected': selectedIds.includes(item.id) }"
                  >
                    <td>
                      <input
                        type="checkbox"
                        class="ms-table__checkbox"
                        :checked="selectedIds.includes(item.id)"
                        @change="toggleSelect(item.id)"
                      >
                    </td>
                    <td>{{ item.id }}</td>
                    <td>
                      <MsButton type="link" @click="handleViewCase(item)">{{ item.name }}</MsButton>
                    </td>
                    <td>
                      <span :class="['ms-tag', tagClass(item.reviewStatus)]">
                        <span :class="['icon-dot', dotClass(item.reviewStatus)]"></span>
                        {{ item.reviewStatus }}
                      </span>
                    </td>
                    <td>
                      <div class="ms-table__actions">
                        <MsButton type="text" size="mini" @click="handleEdit(item)">编辑</MsButton>
                        <span class="ms-divider--vertical"></span>
                        <MsButton type="text" size="mini" @click="handleCopy(item)">复制</MsButton>
                        <span class="ms-divider--vertical"></span>
                        <MsButton type="text-secondary" size="mini">更多</MsButton>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>

              <!-- 分页 -->
              <div class="ms-pagination">
                <span style="color: var(--ms-text-3); font-size: 13px;">共 {{ totalCases }} 条</span>
                <button class="ms-pagination__btn" :disabled="currentPage <= 1" @click="currentPage--">‹</button>
                <button
                  v-for="p in totalPages"
                  :key="p"
                  class="ms-pagination__btn"
                  :class="{ 'is-active': currentPage === p }"
                  @click="currentPage = p"
                >
                  {{ p }}
                </button>
                <button class="ms-pagination__btn" :disabled="currentPage >= totalPages" @click="currentPage++">›</button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- 确认对话框 -->
    <Teleport to="body">
      <div v-if="showConfirmModal" class="ms-modal-mask" @click.self="showConfirmModal = false">
        <div class="ms-modal">
          <div class="ms-modal__header">
            <div class="ms-modal__title">
              <span style="color: var(--ms-warning); font-size: 18px;">⚠️</span>
              离开此页面？
            </div>
            <button class="ms-modal__close" @click="showConfirmModal = false">✕</button>
          </div>
          <div class="ms-modal__body">
            系统不会保存您所做的更改
          </div>
          <div class="ms-modal__footer">
            <MsButton type="secondary" @click="showConfirmModal = false">留下</MsButton>
            <MsButton type="primary" @click="handleConfirmLeave">离开</MsButton>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import MsButton from './MsButton.vue'

// === 响应式数据 ===
const activeNav = ref('cases')
const activeMenu = ref('testcases')
const activeModule = ref('login')
const moduleSearch = ref('')
const searchQuery = ref('')
const showRecycleBin = ref(false)
const showConfirmModal = ref(false)
const selectedIds = ref([100009])
const currentPage = ref(1)

// 导航项
const navItems = [
  { key: 'workbench', label: '工作台' },
  { key: 'cases', label: '用例' },
  { key: 'bugs', label: '缺陷' },
  { key: 'api', label: '接口测试' },
]

// 侧栏菜单
const sidebarMenus = [
  { key: 'workbench', label: '工作台', icon: '📊' },
  { key: 'project', label: '项目管理', icon: '📁' },
  { key: 'plan', label: '测试计划', icon: '📋' },
  { key: 'testcases', label: '测试用例', icon: '📝' },
  { key: 'api', label: '接口测试', icon: '🔌' },
  { key: 'bugs', label: '缺陷管理', icon: '🐛' },
]

// 模块列表
const modules = ref([
  { id: 'unplanned', name: '未规划用例', count: 0 },
  { id: 'login', name: '登录', count: 4 },
  { id: 'content', name: '▸ 内容', count: 12 },
  { id: 'publish', name: '信息发布', count: 3 },
  { id: 'org', name: '机构管理', count: 1 },
])

const recycleBinCount = ref(1)

const filteredModules = computed(() => {
  if (!moduleSearch.value) return modules.value
  return modules.value.filter(m =>
    m.name.includes(moduleSearch.value)
  )
})

// 用例数据
const testCases = ref([
  { id: 100009, name: '登录-验证码登录', reviewStatus: '未评审' },
  { id: 100008, name: '登录-密码登录验证', reviewStatus: '未评审' },
  { id: 100007, name: '内容 — 数据展示', reviewStatus: '未评审' },
  { id: 100006, name: '内容 — 列表筛选', reviewStatus: '未评审' },
  { id: 100005, name: '内容 — 用例详情', reviewStatus: '已通过' },
])

const totalCases = computed(() => testCases.value.length)
const totalPages = computed(() => Math.ceil(totalCases.value / 10))

const filteredCases = computed(() => {
  if (!searchQuery.value) return testCases.value
  return testCases.value.filter(c =>
    c.name.includes(searchQuery.value) || String(c.id).includes(searchQuery.value)
  )
})

const isAllSelected = computed(() =>
  filteredCases.value.length > 0 &&
  filteredCases.value.every(c => selectedIds.value.includes(c.id))
)

const isIndeterminate = computed(() =>
  selectedIds.value.length > 0 && !isAllSelected.value
)

// === 按钮事件 ===
function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = filteredCases.value.map(c => c.id)
  }
}

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

function handleCreate() {
  console.log('新建用例')
}

function handleImport() {
  console.log('导入用例')
}

function handleEdit(item) {
  console.log('编辑:', item.name)
}

function handleCopy(item) {
  console.log('复制:', item.name)
}

function handleViewCase(item) {
  console.log('查看:', item.name)
}

function handleAddModule() {
  console.log('新建模块')
}

function handleConfirmLeave() {
  showConfirmModal.value = false
}

// Tag / Dot 类名映射
function tagClass(status) {
  const map = { '已通过': 'ms-tag--success', '未通过': 'ms-tag--danger', '未评审': 'ms-tag--default' }
  return map[status] || 'ms-tag--default'
}

function dotClass(status) {
  const map = { '已通过': 'icon-dot--success', '未通过': 'icon-dot--danger', '未评审': 'icon-dot--default' }
  return map[status] || 'icon-dot--default'
}
</script>

<style scoped>
.ms-body {
  display: flex;
  flex: 1;
}

.icon-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
  margin-right: 4px;
}
.icon-dot--success { background: var(--ms-success); }
.icon-dot--danger  { background: var(--ms-danger); }
.icon-dot--default { background: var(--ms-text-3); }
</style>
