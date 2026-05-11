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
        <div class="pc-nav-item" @click="backToProjects">
          <svg class="pc-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
          <span>项目中心</span>
        </div>
        <div class="pc-nav-item pc-nav-item--has-children" :class="{ 'pc-nav-item--expanded': expandedNav }" @click="expandedNav = !expandedNav">
          <svg class="pc-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          <span>需求管理</span>
        </div>
        <div class="pc-nav-submenu" v-show="expandedNav">
          <div class="pc-nav-subitem" :class="{ 'pc-nav-subitem--active': activeModule === 'requirements' }" @click="activeModule = 'requirements'">
            <span>需求管理</span>
          </div>
          <div class="pc-nav-subitem" :class="{ 'pc-nav-subitem--active': activeModule === 'testpoints' }" @click="activeModule = 'testpoints'">
            <span>测试点</span>
          </div>
        </div>
        <div class="pc-nav-item pc-nav-item--has-children" :class="{ 'pc-nav-item--expanded': expandedCaseNav }" @click="expandedCaseNav = !expandedCaseNav">
          <svg class="pc-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/><path d="M9 14l2 2 4-4"/></svg>
          <span>用例管理</span>
        </div>
        <div class="pc-nav-submenu" v-show="expandedCaseNav">
          <div class="pc-nav-subitem" :class="{ 'pc-nav-subitem--active': activeModule === 'generate_cases' }" @click="activeModule = 'generate_cases'">
            <span>生成用例</span>
          </div>
          <div class="pc-nav-subitem" :class="{ 'pc-nav-subitem--active': activeModule === 'test_cases' }" @click="activeModule = 'test_cases'">
            <span>测试用例</span>
          </div>
        </div>
        <div class="pc-nav-item" :class="{ 'pc-nav-item--active': activeModule === 'api' }" @click="activeModule = 'api'">
          <svg class="pc-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
          <span>接口测试</span>
        </div>
        <div class="pc-nav-item" :class="{ 'pc-nav-item--active': activeModule === 'web_automation' }" @click="activeModule = 'web_automation'">
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
          <span class="pc-breadcrumb-item pc-breadcrumb-item--active">{{ currentMeta.title }}</span>
        </div>
        <div class="pc-topbar-right">
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="pc-loading">
        <div class="pc-spinner"></div>
        <span>正在加载工作台数据...</span>
      </div>

      <template v-else>
        <!-- ========== 需求管理 ========== -->
        <section v-if="activeModule === 'requirements'" class="pc-content-section">
          <div class="pc-filter-bar">
            <div class="pc-filter-row">
              <div class="pc-filter-group">
                <label class="pc-filter-label">需求名称</label>
                <input v-model.trim="requirementSearch" type="text" class="pc-filter-input pc-filter-input--wide" placeholder="请输入需求名称搜索" />
              </div>
              <div class="pc-filter-group">
                <label class="pc-filter-label">所属项目</label>
                <select v-model="requirementProjectFilter" class="pc-filter-select">
                  <option value="">全部</option>
                  <option v-for="project in projects" :key="project.id" :value="project.id">{{ project.name }}</option>
                </select>
              </div>
              <div class="pc-filter-group">
                <label class="pc-filter-label">状态</label>
                <select v-model="requirementStatusFilter" class="pc-filter-select">
                  <option value="">全部</option>
                  <option value="PENDING_ANALYSIS">待分析</option>
                  <option value="GENERATED">已提取</option>
                  <option value="CONFIRMED">已确认</option>
                </select>
              </div>
              <div class="pc-filter-group">
                <label class="pc-filter-label">测试类型</label>
                <select v-model="requirementTestTypeFilter" class="pc-filter-select">
                  <option value="">全部</option>
                  <option value="FUNCTIONAL">功能测试</option>
                  <option value="INTERFACE">接口测试</option>
                </select>
              </div>
              <div class="pc-filter-actions">
                <button class="pc-btn pc-btn--primary" @click="requirementCurrentPage = 1">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                  查询
                </button>
                <button class="pc-btn" @click="resetRequirementFilters">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
                  重置
                </button>
              </div>
            </div>
          </div>
          <div class="pc-toolbar">
            <div class="pc-toolbar-left">
              <button class="pc-btn pc-btn--primary" @click="showUploadDialog = true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                上传需求文档
              </button>
            </div>
            <div class="pc-toolbar-right">
              <span class="pc-toolbar-info">共 <strong>{{ filteredRequirements.length }}</strong> 条记录</span>
            </div>
          </div>
          <div class="pc-card">
            <div class="pc-card-body">
              <div v-if="!filteredRequirements.length" class="pc-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="color:#aaa"><rect x="2" y="3" width="20" height="18" rx="2"/><path d="M8 7h8M8 11h5"/></svg>
                <h4>暂无需求文档</h4>
                <p>先上传需求文档，再提取测试点。</p>
              </div>
              <table v-else class="pc-table">
                <thead>
                  <tr>
                    <th style="width:60px">序号</th>
                    <th style="min-width:200px">需求名称</th>
                    <th style="min-width:150px">所属项目</th>
                    <th style="width:120px">测试类型</th>
                    <th style="width:100px">版本号</th>
                    <th style="width:120px">状态</th>
                    <th style="width:180px">创建时间</th>
                    <th style="width:180px">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in paginatedRequirements" :key="item.id">
                    <td>{{ (requirementCurrentPage - 1) * requirementPageSize + index + 1 }}</td>
                    <td class="text-left pc-text-cell">{{ truncateText(item.name) }}</td>
                    <td>{{ getProjectName(item.projectId) }}</td>
                    <td>{{ testTypeLabel(item.testType) }}</td>
                    <td>{{ item.version || 'v1.0' }}</td>
                    <td>
                      <span :class="['pc-status', getStatusClass(item.status)]">{{ requirementStatusLabel(item.status) }}</span>
                    </td>
                    <td>{{ formatDate(item.createdAt) }}</td>
                    <td>
                      <div class="pc-action-cell">
                        <button class="pc-btn-text pc-btn-text--primary" type="button" @click="openPromptDialog(item.id)" :disabled="busy">提取测试点</button>
                        <button class="pc-btn-text pc-btn-text--primary" type="button" @click="confirmRequirementTestPoints(item.id)" :disabled="busy || item.status !== 'GENERATED'">确认</button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="pc-pagination" v-if="filteredRequirements.length">
              <span class="pc-pagination-info">共 <strong>{{ filteredRequirements.length }}</strong> 条记录</span>
              <div class="pc-pagination-pages">
                <button class="pc-page-btn" @click="requirementCurrentPage = Math.max(1, requirementCurrentPage - 1)" :disabled="requirementCurrentPage === 1">&lt;</button>
                <button v-for="page in requirementTotalPages" :key="page" class="pc-page-btn" :class="{ 'pc-page-btn--active': requirementCurrentPage === page }" @click="requirementCurrentPage = page">{{ page }}</button>
                <button class="pc-page-btn" @click="requirementCurrentPage = Math.min(requirementTotalPages, requirementCurrentPage + 1)" :disabled="requirementCurrentPage === requirementTotalPages">&gt;</button>
              </div>
              <select class="pc-pagination-select" v-model.number="requirementPageSize" @change="requirementCurrentPage = 1">
                <option :value="10">10条/页</option>
                <option :value="20">20条/页</option>
                <option :value="50">50条/页</option>
              </select>
            </div>
          </div>

          <!-- 上传需求文档对话框 -->
          <div v-if="showUploadDialog" class="pc-dialog-overlay" @click="showUploadDialog = false">
            <div class="pc-dialog" @click.stop>
              <div class="pc-dialog-header">
                <h3>上传需求文档</h3>
                <button class="pc-dialog-close" @click="showUploadDialog = false">×</button>
              </div>
              <div class="pc-dialog-body">
                <form class="pc-dialog-form" @submit.prevent="uploadRequirement">
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">项目 <em>*</em></label>
                    <select v-model="requirementForm.projectId" class="pc-dialog-form-input">
                      <option value="">请选择项目</option>
                      <option v-for="project in projects" :key="project.id" :value="project.id">{{ project.name }}</option>
                    </select>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">需求名称 <em>*</em></label>
                    <input v-model.trim="requirementForm.name" type="text" class="pc-dialog-form-input" placeholder="请输入需求名称" />
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">版本号</label>
                    <input v-model.trim="requirementForm.version" type="text" class="pc-dialog-form-input" placeholder="版本号，如 v1.0" />
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">测试类型 <em>*</em></label>
                    <select v-model="requirementForm.testType" class="pc-dialog-form-input">
                      <option value="">请选择测试类型</option>
                      <option value="FUNCTIONAL">功能测试</option>
                      <option value="INTERFACE">接口测试</option>
                    </select>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">上传文件 <em>*</em></label>
                    <input type="file" class="pc-file-input" @change="handleRequirementFileChange" />
                  </div>
                  <div class="pc-dialog-form-actions">
                    <button class="pc-btn pc-btn--success" type="submit" :disabled="busy">{{ busy ? '处理中...' : '上传需求' }}</button>
                    <button class="pc-btn" type="button" @click="showUploadDialog = false">取消</button>
                  </div>
                </form>
              </div>
            </div>
          </div>

          <!-- 提示语对话框 -->
          <div v-if="showPromptDialog" class="pc-dialog-overlay" @click="showPromptDialog = false">
            <div class="pc-dialog" @click.stop style="max-width: 560px;">
              <div class="pc-dialog-header">
                <h3>提取测试点 - 输入提示语</h3>
                <button class="pc-dialog-close" @click="showPromptDialog = false">×</button>
              </div>
              <div class="pc-dialog-body">
                <div class="pc-dialog-form">
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">提示语 <em>*</em></label>
                    <textarea
                      v-model="promptForm.prompt"
                      class="pc-dialog-form-input"
                      placeholder="请输入提示语，例如：请根据需求文档，从功能、性能、安全性等方面生成详细的测试点"
                      style="height: 140px; resize: vertical;"
                    ></textarea>
                    <p style="margin-top: 6px; font-size: 12px; color: #999;">提示语将与上传的需求文档一起发送给大模型，用于指导生成测试点。</p>
                  </div>
                  <div class="pc-dialog-form-actions">
                    <button class="pc-btn pc-btn--success" type="button" @click="submitPromptDialog" :disabled="busy || !promptForm.prompt.trim()">{{ busy ? '生成中...' : '确认' }}</button>
                    <button class="pc-btn" type="button" @click="showPromptDialog = false">取消</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- ========== 测试点 ========== -->
        <section v-else-if="activeModule === 'testpoints'" class="pc-content-section">
          <div class="pc-filter-bar">
            <div class="pc-filter-row">
              <div class="pc-filter-group">
                <label class="pc-filter-label">测试点名称</label>
                <input v-model.trim="testPointSearch" type="text" class="pc-filter-input pc-filter-input--wide" placeholder="请输入测试点名称搜索" />
              </div>
              <div class="pc-filter-group">
                <label class="pc-filter-label">来源需求</label>
                <select v-model="testPointRequirementFilter" class="pc-filter-select">
                  <option value="">全部</option>
                  <option v-for="req in requirements" :key="req.id" :value="req.id">{{ req.name }}</option>
                </select>
              </div>
              <div class="pc-filter-group">
                <label class="pc-filter-label">风险</label>
                <select v-model="testPointRiskFilter" class="pc-filter-select">
                  <option value="">全部</option>
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                </select>
              </div>
              <div class="pc-filter-group">
                <label class="pc-filter-label">测试类型</label>
                <select v-model="testPointTypeFilter" class="pc-filter-select">
                  <option value="">全部</option>
                  <option value="FUNCTIONAL">功能测试</option>
                  <option value="INTERFACE">接口测试</option>
                </select>
              </div>
              <div class="pc-filter-actions">
                <button class="pc-btn pc-btn--primary" @click="testPointCurrentPage = 1">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                  查询
                </button>
                <button class="pc-btn" @click="resetTestPointFilters">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
                  重置
                </button>
              </div>
            </div>
          </div>
          <div class="pc-toolbar">
            <div class="pc-toolbar-left">
              <button class="pc-btn pc-btn--primary" type="button" @click="addTestPoint">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                新增测试点
              </button>
              <button class="pc-btn pc-btn--sm" type="button" @click="activeModule = 'generate_cases'">进入用例管理</button>
            </div>
            <div class="pc-toolbar-right">
              <span class="pc-toolbar-info">共 <strong>{{ filteredTestPoints.length }}</strong> 条记录</span>
            </div>
          </div>
          <div class="pc-card">
            <div class="pc-card-body">
              <div v-if="!filteredTestPoints.length" class="pc-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="color:#aaa"><rect x="2" y="3" width="20" height="18" rx="2"/><path d="M8 7h8M8 11h5"/></svg>
                <h4>暂无测试点</h4>
                <p>执行"提取测试点"后，这里会展示结构化结果，或点击"新增测试点"手动添加。</p>
              </div>
              <table v-else class="pc-table">
                <thead>
                  <tr><th>测试点</th><th>来源需求</th><th>测试类型</th><th>来源类型</th><th>场景</th><th>风险</th><th style="width:180px">创建时间</th><th>操作</th></tr>
                </thead>
                <tbody>
                  <tr v-for="point in paginatedTestPoints" :key="point.id">
                    <td class="text-left pc-text-cell">{{ truncateText(point.name) }}</td>
                    <td>{{ getRequirementName(point.requirementId) }}</td>
                    <td>{{ testTypeLabel(point.testType) }}</td>
                    <td>{{ pointSourceLabel(point.sourceType) }}</td>
                    <td>{{ point.sceneType || 'Functional' }}</td>
                    <td>{{ point.riskLevel || 'MEDIUM' }}</td>
                    <td>{{ formatDate(point.createdAt) }}</td>
                    <td class="pc-action-cell">
                      <button class="pc-btn-text pc-btn-text--primary" type="button" @click="editTestPoint(point)">编辑</button>
                      <button class="pc-btn-text pc-btn-text--danger" type="button" @click="deleteTestPoint(point)">删除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="pc-pagination" v-if="filteredTestPoints.length">
              <span class="pc-pagination-info">共 <strong>{{ filteredTestPoints.length }}</strong> 条记录</span>
              <div class="pc-pagination-pages">
                <button class="pc-page-btn" @click="testPointCurrentPage = Math.max(1, testPointCurrentPage - 1)" :disabled="testPointCurrentPage === 1">&lt;</button>
                <button v-for="page in testPointTotalPages" :key="page" class="pc-page-btn" :class="{ 'pc-page-btn--active': testPointCurrentPage === page }" @click="testPointCurrentPage = page">{{ page }}</button>
                <button class="pc-page-btn" @click="testPointCurrentPage = Math.min(testPointTotalPages, testPointCurrentPage + 1)" :disabled="testPointCurrentPage === testPointTotalPages">&gt;</button>
              </div>
              <select class="pc-pagination-select" v-model.number="testPointPageSize" @change="testPointCurrentPage = 1">
                <option :value="10">10条/页</option>
                <option :value="20">20条/页</option>
                <option :value="50">50条/页</option>
              </select>
            </div>
          </div>

          <!-- 新增/编辑测试点对话框 -->
          <div v-if="showTestPointDialog" class="pc-dialog-overlay" @click="showTestPointDialog = false">
            <div class="pc-dialog" @click.stop>
              <div class="pc-dialog-header">
                <h3>{{ isEditTestPoint ? '编辑测试点' : '新增测试点' }}</h3>
                <button class="pc-dialog-close" @click="showTestPointDialog = false">×</button>
              </div>
              <div class="pc-dialog-body">
                <form class="pc-dialog-form" @submit.prevent="saveTestPoint">
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">测试点名称 <em>*</em></label>
                    <input v-model.trim="testPointForm.name" type="text" class="pc-dialog-form-input" placeholder="请输入测试点名称" />
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">来源需求</label>
                    <select v-model="testPointForm.requirementId" class="pc-dialog-form-input">
                      <option value="">请选择需求</option>
                      <option v-for="req in requirements" :key="req.id" :value="req.id">{{ req.name }}</option>
                    </select>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">场景</label>
                    <select v-model="testPointForm.sceneType" class="pc-dialog-form-input">
                      <option value="Functional">Functional</option>
                      <option value="Non-Functional">Non-Functional</option>
                    </select>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">风险</label>
                    <select v-model="testPointForm.riskLevel" class="pc-dialog-form-input">
                      <option value="LOW">LOW</option>
                      <option value="MEDIUM">MEDIUM</option>
                      <option value="HIGH">HIGH</option>
                    </select>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">测试类型 <em>*</em></label>
                    <select v-model="testPointForm.testType" class="pc-dialog-form-input">
                      <option value="FUNCTIONAL">功能测试</option>
                      <option value="INTERFACE">接口测试</option>
                    </select>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">描述</label>
                    <div class="pc-rich-text-editor">
                      <div ref="editor" class="pc-rich-text-content" contenteditable="true" v-html="testPointForm.description" @input="updateDescription"></div>
                    </div>
                  </div>
                  <div class="pc-dialog-form-actions">
                    <button class="pc-btn pc-btn--success" type="submit" :disabled="busy">{{ busy ? '保存中...' : '保存' }}</button>
                    <button class="pc-btn" type="button" @click="showTestPointDialog = false">取消</button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </section>

        <!-- ========== 生成用例 ========== -->
        <section v-else-if="activeModule === 'generate_cases'" class="pc-content-section">
          <div class="pc-filter-bar">
            <div class="pc-filter-row">
              <div class="pc-filter-group">
                <label class="pc-filter-label">测试点名称</label>
                <input v-model.trim="generateCaseSearch" type="text" class="pc-filter-input pc-filter-input--wide" placeholder="请输入测试点名称搜索" />
              </div>
              <div class="pc-filter-actions">
                <button class="pc-btn pc-btn--primary" @click="generateCaseCurrentPage = 1">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                  查询
                </button>
                <button class="pc-btn" @click="generateCaseSearch = ''">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
                  重置
                </button>
              </div>
            </div>
          </div>
          <div class="pc-toolbar">
            <div class="pc-toolbar-left">
              <button class="pc-btn pc-btn--primary" type="button" @click="batchGenerateTestCases" :disabled="busy || !selectedPointIds.length">批量生成</button>
            </div>
            <div class="pc-toolbar-right">
              <span class="pc-toolbar-info">已选 <strong>{{ selectedPointIds.length }}</strong> 个，共 <strong>{{ filteredGenerateCases.length }}</strong> 条记录</span>
            </div>
          </div>
          <div class="pc-card">
            <div class="pc-card-body">
              <div v-if="!filteredGenerateCases.length" class="pc-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="color:#aaa"><rect x="2" y="3" width="20" height="18" rx="2"/><path d="M8 7h8M8 11h5"/></svg>
                <h4>暂无测试点</h4>
                <p>请先在需求管理中提取测试点。</p>
              </div>
              <table v-else class="pc-table">
                <thead>
                  <tr>
                    <th style="width:40px"><input type="checkbox" class="pc-checkbox" v-model="selectAllGeneratePoints" @change="toggleSelectAllGeneratePoints" /></th>
                    <th>测试点名称</th>
                    <th style="min-width:150px">所在项目</th>
                    <th style="width:120px">用例类型</th>
                    <th style="width:180px">创建时间</th>
                    <th style="width:120px">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="point in paginatedGenerateCases" :key="point.id">
                    <td><input v-model="selectedPointIds" type="checkbox" :value="point.id" class="pc-checkbox" /></td>
                    <td class="text-left pc-text-cell">{{ truncateText(point.name) }}</td>
                    <td>{{ getProjectName(point.projectId) }}</td>
                    <td>{{ testTypeLabel(point.testType) }}</td>
                    <td>{{ formatDate(point.createdAt) }}</td>
                    <td class="pc-action-cell">
                      <button class="pc-btn-text pc-btn-text--primary" type="button" @click.prevent="generateCasesForPoint(point.id)" :disabled="busy">生成</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="pc-pagination" v-if="filteredGenerateCases.length">
              <span class="pc-pagination-info">共 <strong>{{ filteredGenerateCases.length }}</strong> 条记录</span>
              <div class="pc-pagination-pages">
                <button class="pc-page-btn" @click="generateCaseCurrentPage = Math.max(1, generateCaseCurrentPage - 1)" :disabled="generateCaseCurrentPage === 1">&lt;</button>
                <button v-for="page in generateCaseTotalPages" :key="page" class="pc-page-btn" :class="{ 'pc-page-btn--active': generateCaseCurrentPage === page }" @click="generateCaseCurrentPage = page">{{ page }}</button>
                <button class="pc-page-btn" @click="generateCaseCurrentPage = Math.min(generateCaseTotalPages, generateCaseCurrentPage + 1)" :disabled="generateCaseCurrentPage === generateCaseTotalPages">&gt;</button>
              </div>
              <select class="pc-pagination-select" v-model.number="generateCasePageSize" @change="generateCaseCurrentPage = 1">
                <option :value="10">10条/页</option>
                <option :value="20">20条/页</option>
                <option :value="50">50条/页</option>
              </select>
            </div>
          </div>

          <!-- 生成用例附加提示词对话框 -->
          <div v-if="showCasePromptDialog" class="pc-dialog-overlay" @click="showCasePromptDialog = false">
            <div class="pc-dialog" @click.stop>
              <div class="pc-dialog-header">
                <h3>附加提示词 (可选)</h3>
                <button class="pc-dialog-close" @click="showCasePromptDialog = false">×</button>
              </div>
              <div class="pc-dialog-body">
                <form class="pc-dialog-form" @submit.prevent="submitCasePromptDialog">
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">自定义生成要求</label>
                    <textarea v-model="casePromptForm.prompt" class="pc-dialog-form-input" placeholder="例如：重点测试越权访问、只生成异常用例..." style="height: 120px; resize: vertical;"></textarea>
                  </div>
                  <div class="pc-dialog-form-actions">
                    <button class="pc-btn pc-btn--success" type="submit" :disabled="busy">{{ busy ? '生成中...' : '开始生成' }}</button>
                    <button class="pc-btn" type="button" @click="showCasePromptDialog = false">取消</button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </section>

        <!-- ========== 测试用例 ========== -->
        <section v-else-if="activeModule === 'test_cases'" class="pc-content-section">
          <div class="pc-filter-bar">
            <div class="pc-filter-row">
              <div class="pc-filter-group">
                <label class="pc-filter-label">标题/编号</label>
                <input v-model.trim="caseSearch" type="text" class="pc-filter-input pc-filter-input--wide" placeholder="请输入标题或编号搜索" />
              </div>
              <div class="pc-filter-group">
                <label class="pc-filter-label">状态</label>
                <select v-model="caseStatusFilter" class="pc-filter-select">
                  <option value="">全部</option>
                  <option value="PENDING_REVIEW">待评审</option>
                  <option value="APPROVED">已通过</option>
                  <option value="REJECTED">已拒绝</option>
                </select>
              </div>
              <div class="pc-filter-group">
                <label class="pc-filter-label">测试类型</label>
                <select v-model="caseTypeFilter" class="pc-filter-select">
                  <option value="">全部</option>
                  <option value="FUNCTIONAL">功能测试</option>
                  <option value="INTERFACE">接口测试</option>
                </select>
              </div>
              <div class="pc-filter-group">
                <label class="pc-filter-label">执行方式</label>
                <select v-model="caseExecutionTypeFilter" class="pc-filter-select">
                  <option value="">全部</option>
                  <option value="MANUAL">手工执行</option>
                  <option value="API_AUTOMATION">接口自动化</option>
                  <option value="WEB_AUTOMATION">Web自动化</option>
                </select>
              </div>
              <div class="pc-filter-actions">
                <button class="pc-btn pc-btn--primary" @click="testCaseCurrentPage = 1">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                  查询
                </button>
                <button class="pc-btn" @click="resetCaseFilters">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
                  重置
                </button>
              </div>
            </div>
          </div>
          <div class="pc-toolbar">
            <div class="pc-toolbar-left">
              <button class="pc-btn pc-btn--primary" type="button" @click="addTestCase">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                新增测试用例
              </button>
            </div>
            <div class="pc-toolbar-right">
              <span class="pc-toolbar-info">共 <strong>{{ filteredCases.length }}</strong> 条记录</span>
            </div>
          </div>
          <div class="pc-card">
            <div class="pc-card-body">
              <div v-if="!filteredCases.length" class="pc-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="color:#aaa"><rect x="2" y="3" width="20" height="18" rx="2"/><path d="M8 7h8M8 11h5"/></svg>
                <h4>暂无匹配用例</h4>
                <p>先生成用例，或调整筛选条件。</p>
              </div>
              <table v-else class="pc-table">
                <thead>
                  <tr><th>编号</th><th>标题</th><th>测试点</th><th>测试类型</th><th>执行方式</th><th>状态</th><th style="width:180px">创建时间</th><th>操作</th></tr>
                </thead>
                <tbody>
                  <tr v-for="item in paginatedCases" :key="item.id">
                    <td>{{ item.caseNumber }}</td>
                    <td class="pc-text-cell">{{ truncateText(item.title) }}</td>
                    <td>{{ getTestPointName(item.testPointId) }}</td>
                    <td>{{ testTypeLabel(item.testType) }}</td>
                    <td>{{ executionTypeLabel(item.executionType) }}</td>
                    <td><span :class="['pc-status', getStatusClass(item.status)]">{{ caseStatusLabel(item.status) }}</span></td>
                    <td>{{ formatDate(item.createdAt) }}</td>
                    <td class="pc-action-cell">
                      <button v-if="isInterfaceCase(item)" class="pc-btn-text pc-btn-text--primary" type="button" @click="queueCaseExecution(item)">加入接口执行</button>
                      <button v-else-if="item.testType === 'WEB'" class="pc-btn-text pc-btn-text--primary" type="button" @click="openWebAutomationModule">进入Web执行</button>
                      <button v-else class="pc-btn-text pc-btn-text--primary" type="button" @click="openWebAutomationModule">立即执行</button>
                      <button class="pc-btn-text pc-btn-text--primary" type="button" @click="editTestCase(item)">编辑</button>
                      <button class="pc-btn-text pc-btn-text--danger" type="button" @click="deleteCase(item)" :disabled="busy">删除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="pc-pagination" v-if="filteredCases.length">
              <span class="pc-pagination-info">共 <strong>{{ filteredCases.length }}</strong> 条记录</span>
              <div class="pc-pagination-pages">
                <button class="pc-page-btn" @click="testCaseCurrentPage = Math.max(1, testCaseCurrentPage - 1)" :disabled="testCaseCurrentPage === 1">&lt;</button>
                <button v-for="page in testCaseTotalPages" :key="page" class="pc-page-btn" :class="{ 'pc-page-btn--active': testCaseCurrentPage === page }" @click="testCaseCurrentPage = page">{{ page }}</button>
                <button class="pc-page-btn" @click="testCaseCurrentPage = Math.min(testCaseTotalPages, testCaseCurrentPage + 1)" :disabled="testCaseCurrentPage === testCaseTotalPages">&gt;</button>
              </div>
              <select class="pc-pagination-select" v-model.number="testCasePageSize" @change="testCaseCurrentPage = 1">
                <option :value="10">10条/页</option>
                <option :value="20">20条/页</option>
                <option :value="50">50条/页</option>
              </select>
            </div>
          </div>

          <!-- 新增/编辑测试用例对话框 -->
          <div v-if="showTestCaseDialog" class="pc-dialog-overlay" @click="showTestCaseDialog = false">
            <div class="pc-dialog" @click.stop>
              <div class="pc-dialog-header">
                <h3>{{ isEditTestCase ? '编辑测试用例' : '新增测试用例' }}</h3>
                <button class="pc-dialog-close" @click="showTestCaseDialog = false">×</button>
              </div>
              <div class="pc-dialog-body">
                <form class="pc-dialog-form" @submit.prevent="saveTestCase">
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">测试点 <em>*</em></label>
                    <select v-model="testCaseForm.testPointId" class="pc-dialog-form-input">
                      <option value="">请选择测试点</option>
                      <option v-for="point in testPoints" :key="point.id" :value="point.id">{{ point.name }}</option>
                    </select>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">标题 <em>*</em></label>
                    <input v-model.trim="testCaseForm.title" type="text" class="pc-dialog-form-input" placeholder="请输入测试用例标题" />
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">前置条件</label>
                    <textarea v-model="testCaseForm.precondition" class="pc-dialog-form-input" placeholder="请输入前置条件" style="height: 80px; resize: vertical;"></textarea>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">测试步骤 <em>*</em></label>
                    <textarea v-model="testCaseForm.steps" class="pc-dialog-form-input" placeholder="请输入测试步骤" style="height: 120px; resize: vertical;"></textarea>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">预期结果 <em>*</em></label>
                    <textarea v-model="testCaseForm.expectedResult" class="pc-dialog-form-input" placeholder="请输入预期结果" style="height: 80px; resize: vertical;"></textarea>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">优先级</label>
                    <select v-model="testCaseForm.priority" class="pc-dialog-form-input">
                      <option value="LOW">LOW</option>
                      <option value="MEDIUM">MEDIUM</option>
                      <option value="HIGH">HIGH</option>
                    </select>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">测试类型 <em>*</em></label>
                    <select v-model="testCaseForm.testType" class="pc-dialog-form-input">
                      <option value="FUNCTIONAL">功能测试</option>
                      <option value="INTERFACE">接口测试</option>
                    </select>
                  </div>
                  <div class="pc-dialog-form-group">
                    <label class="pc-dialog-form-label">执行方式 <em>*</em></label>
                    <select v-model="testCaseForm.executionType" class="pc-dialog-form-input">
                      <option value="MANUAL">手工执行</option>
                      <option value="API_AUTOMATION">接口自动化</option>
                      <option value="WEB_AUTOMATION">Web自动化</option>
                    </select>
                  </div>
                  <div class="pc-dialog-form-actions">
                    <button class="pc-btn pc-btn--success" type="submit" :disabled="busy">{{ busy ? '保存中...' : '保存' }}</button>
                    <button class="pc-btn" type="button" @click="showTestCaseDialog = false">取消</button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </section>

        <!-- Web自动化 -->
        <section v-else-if="activeModule === 'web_automation'" class="pc-content-section">
          <div class="pc-filter-bar">
            <div class="pc-filter-row">
              <div class="pc-filter-group">
                <label class="pc-filter-label">标题/编号</label>
                <input v-model.trim="webAutomationSearch" type="text" class="pc-filter-input pc-filter-input--wide" placeholder="请输入标题或编号搜索" />
              </div>
              <div class="pc-filter-group">
                <label class="pc-filter-label">环境</label>
                <select v-model="webAutomationEnvironment" class="pc-filter-select">
                  <option value="PRODUCTION">PRODUCTION</option>
                  <option value="STAGING">STAGING</option>
                  <option value="DEV">DEV</option>
                </select>
              </div>
              <div class="pc-filter-actions">
                <button class="pc-btn pc-btn--primary" @click="webAutomationCurrentPage = 1">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                  查询
                </button>
                <button class="pc-btn" @click="resetWebAutomationFilters">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
                  重置
                </button>
              </div>
            </div>
          </div>
          <div class="pc-toolbar">
            <div class="pc-toolbar-left">
              <button class="pc-btn pc-btn--primary" type="button" @click="batchExecuteWebAutomationCases" :disabled="busy || !selectedWebAutomationCaseIds.length">批量执行</button>
            </div>
            <div class="pc-toolbar-right">
              <span class="pc-toolbar-info">已选 <strong>{{ selectedWebAutomationCaseIds.length }}</strong> 个，共 <strong>{{ filteredWebAutomationCases.length }}</strong> 条记录</span>
            </div>
          </div>
          <div class="pc-card">
            <div class="pc-card-body">
              <div v-if="filteredWebAutomationCases.length" class="pc-table-container">
                <table class="pc-table pc-table--wrap">
                  <thead>
                    <tr><th><input v-model="selectAllWebAutomationCases" type="checkbox" class="pc-checkbox" @change="toggleAllWebAutomationCases" /></th><th>编号</th><th>标题</th><th>状态</th><th style="width:180px">创建时间</th><th>操作</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in paginatedWebAutomationCases" :key="item.id">
                      <td><input v-model="selectedWebAutomationCaseIds" type="checkbox" class="pc-checkbox" :value="item.id" /></td>
                      <td>{{ item.caseNumber }}</td>
                      <td class="pc-text-cell">{{ truncateText(item.title) }}</td>
                      <td>
                        <span :class="['pc-status', executionCaseStatusClass(item)]">{{ executionCaseStatusLabel(item) }}</span>
                      </td>
                      <td>{{ formatDate(item.createdAt) }}</td>
                      <td class="pc-action-cell">
                        <button class="pc-btn-text pc-btn-text--primary" type="button" @click="executeWebAutomationCase(item.id)" :disabled="busy">立即执行</button>
                        <button class="pc-btn-text pc-btn-text--primary" type="button" @click="openPayloadModal(item)">查看Payload</button>
                        <button v-if="getLastTaskForCase(item.id)" class="pc-btn-text pc-btn-text--success" type="button" @click="viewAllureReport(getLastTaskForCase(item.id).id)" :disabled="busy">查看报告</button>
                        <button v-if="getLastTaskForCase(item.id)" class="pc-btn-text pc-btn-text--primary" type="button" @click="retryTask(getLastTaskForCase(item.id).id)" :disabled="busy">重试</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-else class="pc-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="color:#aaa"><rect x="2" y="3" width="20" height="18" rx="2"/><path d="M8 7h8M8 11h5"/></svg>
                <h4>暂无功能测试用例</h4>
                <p>先在用例管理中生成功能测试用例。</p>
              </div>
            </div>
            <div class="pc-pagination" v-if="filteredWebAutomationCases.length">
              <span class="pc-pagination-info">共 <strong>{{ filteredWebAutomationCases.length }}</strong> 条记录</span>
              <div class="pc-pagination-pages">
                <button class="pc-page-btn" @click="webAutomationCurrentPage = Math.max(1, webAutomationCurrentPage - 1)" :disabled="webAutomationCurrentPage === 1">&lt;</button>
                <button v-for="page in webAutomationTotalPages" :key="page" class="pc-page-btn" :class="{ 'pc-page-btn--active': webAutomationCurrentPage === page }" @click="webAutomationCurrentPage = page">{{ page }}</button>
                <button class="pc-page-btn" @click="webAutomationCurrentPage = Math.min(webAutomationTotalPages, webAutomationCurrentPage + 1)" :disabled="webAutomationCurrentPage === webAutomationTotalPages">&gt;</button>
              </div>
              <select class="pc-pagination-select" v-model.number="webAutomationPageSize" @change="webAutomationCurrentPage = 1">
                <option :value="10">10条/页</option>
                <option :value="20">20条/页</option>
                <option :value="50">50条/页</option>
              </select>
            </div>
          </div>
        </section>

        <!-- ========== 接口测试 ========== -->
        <section v-else class="pc-content-section">
          <div class="pc-filter-bar">
            <div class="pc-filter-row">
              <div class="pc-filter-group">
                <label class="pc-filter-label">标题/编号</label>
                <input v-model.trim="apiTestSearch" type="text" class="pc-filter-input pc-filter-input--wide" placeholder="请输入标题或编号搜索" />
              </div>
              <div class="pc-filter-group">
                <label class="pc-filter-label">环境</label>
                <select v-model="executionEnvironment" class="pc-filter-select">
                  <option value="PRODUCTION">PRODUCTION</option>
                  <option value="STAGING">STAGING</option>
                  <option value="DEV">DEV</option>
                </select>
              </div>
              <div class="pc-filter-actions">
                <button class="pc-btn pc-btn--primary" @click="apiTestCurrentPage = 1">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                  查询
                </button>
                <button class="pc-btn" @click="resetApiTestFilters">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
                  重置
                </button>
              </div>
            </div>
          </div>
          <div class="pc-toolbar">
            <div class="pc-toolbar-left">
              <button class="pc-btn pc-btn--primary" type="button" @click="batchExecuteCases" :disabled="busy || !selectedExecutionCaseIds.length">批量执行</button>
            </div>
            <div class="pc-toolbar-right">
              <span class="pc-toolbar-info">已选 <strong>{{ selectedExecutionCaseIds.length }}</strong> 个，共 <strong>{{ filteredApiTestCases.length }}</strong> 条记录</span>
            </div>
          </div>
          <div class="pc-card">
            <div class="pc-card-body">
              <div v-if="filteredApiTestCases.length" class="pc-table-container">
                <table class="pc-table pc-table--wrap">
                  <thead>
                    <tr><th><input v-model="selectAllExecutionCases" type="checkbox" class="pc-checkbox" @change="toggleAllExecutionCases" /></th><th>编号</th><th>标题</th><th>状态</th><th style="width:180px">创建时间</th><th>操作</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in paginatedApiTestCases" :key="item.id">
                      <td><input v-model="selectedExecutionCaseIds" type="checkbox" class="pc-checkbox" :value="item.id" /></td>
                      <td>{{ item.caseNumber }}</td>
                      <td class="pc-text-cell">{{ truncateText(item.title) }}</td>
                      <td>
                        <span :class="['pc-status', executionCaseStatusClass(item)]">{{ executionCaseStatusLabel(item) }}</span>
                      </td>
                      <td>{{ formatDate(item.createdAt) }}</td>
                      <td class="pc-action-cell">
                        <button class="pc-btn-text pc-btn-text--primary" type="button" @click="executeSingleCase(item.id)" :disabled="busy">立即执行</button>
                        <button class="pc-btn-text pc-btn-text--primary" type="button" @click="openPayloadModal(item)">查看Payload</button>
                        <button v-if="getLastTaskForCase(item.id)" class="pc-btn-text pc-btn-text--success" type="button" @click="viewAllureReport(getLastTaskForCase(item.id).id)" :disabled="busy">查看报告</button>
                        <button v-if="getLastTaskForCase(item.id)" class="pc-btn-text pc-btn-text--primary" type="button" @click="retryTask(getLastTaskForCase(item.id).id)" :disabled="busy">重试</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-else class="pc-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="color:#aaa"><rect x="2" y="3" width="20" height="18" rx="2"/><path d="M8 7h8M8 11h5"/></svg>
                <h4>暂无接口测试用例</h4>
                <p>先在用例管理中生成接口测试用例。</p>
              </div>
            </div>
            <div class="pc-pagination" v-if="filteredApiTestCases.length">
              <span class="pc-pagination-info">共 <strong>{{ filteredApiTestCases.length }}</strong> 条记录</span>
              <div class="pc-pagination-pages">
                <button class="pc-page-btn" @click="apiTestCurrentPage = Math.max(1, apiTestCurrentPage - 1)" :disabled="apiTestCurrentPage === 1">&lt;</button>
                <button v-for="page in apiTestTotalPages" :key="page" class="pc-page-btn" :class="{ 'pc-page-btn--active': apiTestCurrentPage === page }" @click="apiTestCurrentPage = page">{{ page }}</button>
                <button class="pc-page-btn" @click="apiTestCurrentPage = Math.min(apiTestTotalPages, apiTestCurrentPage + 1)" :disabled="apiTestCurrentPage === apiTestTotalPages">&gt;</button>
              </div>
              <select class="pc-pagination-select" v-model.number="apiTestPageSize" @change="apiTestCurrentPage = 1">
                <option :value="10">10条/页</option>
                <option :value="20">20条/页</option>
                <option :value="50">50条/页</option>
              </select>
            </div>
          </div>
        </section>
      </template>

      <!-- Toast 消息 -->
      <transition name="toast">
        <div v-if="error" class="pc-toast pc-toast--error" @click="error = ''">{{ error }}</div>
      </transition>
      <transition name="toast">
        <div v-if="info" class="pc-toast pc-toast--success" @click="info = ''">{{ info }}</div>
      </transition>

      <!-- 结果详情模态框 -->
      <div v-if="resultModal" class="pc-dialog-overlay" @click="resultModal = null">
        <div class="pc-dialog" @click.stop>
          <div class="pc-dialog-header">
            <h3>任务 #{{ resultModal.task.id }} 执行结果</h3>
            <button class="pc-dialog-close" @click="resultModal = null">×</button>
          </div>
          <div class="pc-dialog-body">
            <pre class="pc-code-block">{{ buildResultText(resultModal) }}</pre>
            <div v-if="resultModal.result.artifacts" class="pc-dialog-body-section">
              <h4>Web 执行工件</h4>
              <div class="pc-payload-summary">
                <div v-for="step in buildArtifactSteps(resultModal.result.artifacts)" :key="step.index" class="pc-payload-step">
                  <span :class="['pc-status', step.status === 'FAILED' ? 'pc-status--archived' : 'pc-status--active']">{{ step.status }}</span>
                  <span>{{ step.name }}</span>
                </div>
              </div>
              <pre class="pc-code-block">{{ prettyJsonText(resultModal.result.artifacts) }}</pre>
            </div>
            <div v-if="resultModal.result.reportUrl" class="pc-dialog-body-section">
              <h4>Allure 测试报告</h4>
              <a :href="resultModal.result.reportUrl" target="_blank" class="pc-link">查看 Allure 报告</a>
            </div>
          </div>
        </div>
      </div>

      <div v-if="payloadModal" class="pc-dialog-overlay" @click="payloadModal = null">
        <div class="pc-dialog pc-dialog--wide" @click.stop>
          <div class="pc-dialog-header">
            <h3>{{ payloadModal.title }} - automationPayload</h3>
            <button class="pc-dialog-close" @click="payloadModal = null">×</button>
          </div>
          <div class="pc-dialog-body">
            <div class="pc-payload-summary" v-if="buildPayloadSteps(payloadModal.automationPayload).length">
              <div v-for="step in buildPayloadSteps(payloadModal.automationPayload)" :key="step.index" class="pc-payload-step">
                <span class="pc-num-highlight">#{{ step.index }}</span>
                <span>{{ step.action }}</span>
                <span>{{ step.description }}</span>
                <code v-if="step.selector">{{ step.selector }}</code>
              </div>
            </div>
            <pre class="pc-code-block">{{ prettyJsonText(payloadModal.automationPayload) }}</pre>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { api, API_BASE_URL, clearStoredToken, clearStoredUser, getStoredUser } from '../api'

export default {
  name: 'ProjectWorkbenchConsole',
  data() {
    return {
      project: null,
      projects: [],
      requirements: [],
      testPoints: [],
      testCases: [],
      executionTasks: [],
      executionResults: [],
      loading: false,
      busy: false,
      error: '',
      info: '',
      activeModule: localStorage.getItem('workbenchActiveModule') || 'requirements',
      currentUser: getStoredUser() || 'admin',
      requirementForm: { name: '', version: 'v1.0', file: null, projectId: null, testType: '' },
      selectedPointIds: [],
      selectAllGeneratePoints: false,
      caseSearch: '',
      caseStatusFilter: '',
      caseTypeFilter: '',
      caseExecutionTypeFilter: '',
      executionEnvironment: 'PRODUCTION',
      selectedExecutionCaseIds: [],
      selectAllExecutionCases: false,
      // Web自动化
      webAutomationSearch: '',
      webAutomationEnvironment: 'PRODUCTION',
      selectedWebAutomationCaseIds: [],
      selectAllWebAutomationCases: false,
      webAutomationCurrentPage: 1,
      webAutomationPageSize: 10,
      resultModal: null,
      payloadModal: null,
      showUploadDialog: false,
      showPromptDialog: false,
      promptForm: { requirementId: null, prompt: '' },
      showCasePromptDialog: false,
      casePromptForm: { type: 'single', testPointId: null, prompt: '' },
      showTestPointDialog: false,
      isEditTestPoint: false,
      testPointForm: { id: null, name: '', requirementId: '', sceneType: 'Functional', riskLevel: 'MEDIUM', description: '', testType: 'INTERFACE', sourceType: 'MANUAL_CREATED' },
      testPointSearch: '',
      testPointRequirementFilter: '',
      testPointRiskFilter: '',
      testPointTypeFilter: '',
      showTestCaseDialog: false,
      isEditTestCase: false,
      testCaseForm: { id: null, projectId: null, testPointId: null, title: '', precondition: '', steps: '', expectedResult: '', priority: 'MEDIUM', automation: false, status: 'PENDING_REVIEW', testType: 'INTERFACE', executionType: 'WEB_AUTOMATION', automationPayload: '' },
      expandedNav: localStorage.getItem('workbenchExpandedNav') === 'true' || (localStorage.getItem('workbenchActiveModule') === 'requirements' || localStorage.getItem('workbenchActiveModule') === 'testpoints'),
      expandedCaseNav: localStorage.getItem('workbenchExpandedCaseNav') === 'true' || (localStorage.getItem('workbenchActiveModule') === 'generate_cases' || localStorage.getItem('workbenchActiveModule') === 'test_cases'),
      // 需求管理筛选
      requirementSearch: '',
      requirementProjectFilter: '',
      requirementStatusFilter: '',
      requirementTestTypeFilter: '',
      requirementCurrentPage: 1,
      requirementPageSize: 10,
      // 测试点分页
      testPointCurrentPage: 1,
      testPointPageSize: 10,
      // 生成用例
      generateCaseSearch: '',
      generateCaseCurrentPage: 1,
      generateCasePageSize: 10,
      // 测试用例
      testCaseCurrentPage: 1,
      testCasePageSize: 10,
      // 接口测试
      apiTestSearch: '',
      apiTestCurrentPage: 1,
      apiTestPageSize: 10
    }
  },
  computed: {
    currentMeta() {
      return ({
        requirements: { kicker: 'Requirement Management', title: '需求管理', desc: '上传需求文档并提取结构化测试点。' },
        testpoints: { kicker: 'Test Points', title: '测试点', desc: '查看和管理从需求中提取的测试点。' },
        generate_cases: { kicker: 'Generate Cases', title: '生成用例', desc: '基于测试点生成测试用例。' },
        test_cases: { kicker: 'Test Cases', title: '测试用例', desc: '查看和管理测试用例列表。' },
        api: { kicker: 'API Testing', title: '接口测试', desc: '基于测试用例发起执行任务并查看测试结果。' }
      }[this.activeModule] || { kicker: 'Requirement Management', title: '需求管理', desc: '上传需求文档并提取结构化测试点。' })
    },
    // 需求管理 - 倒序 + 筛选
    sortedRequirements() {
      return [...this.requirements].sort((a, b) => new Date(b.createdAt || b.updatedAt) - new Date(a.createdAt || a.updatedAt))
    },
    filteredRequirements() {
      let result = this.sortedRequirements
      if (this.requirementSearch) {
        const q = this.requirementSearch.toLowerCase()
        result = result.filter(r => (r.name || '').toLowerCase().includes(q))
      }
      if (this.requirementProjectFilter) {
        result = result.filter(r => r.projectId === this.requirementProjectFilter)
      }
      if (this.requirementStatusFilter) {
        result = result.filter(r => r.status === this.requirementStatusFilter)
      }
      if (this.requirementTestTypeFilter) {
        result = result.filter(r => this.normalizeTestType(r.testType) === this.requirementTestTypeFilter)
      }
      return result
    },
    paginatedRequirements() {
      const start = (this.requirementCurrentPage - 1) * this.requirementPageSize
      return this.filteredRequirements.slice(start, start + this.requirementPageSize)
    },
    requirementTotalPages() {
      return Math.ceil(this.filteredRequirements.length / this.requirementPageSize) || 1
    },
    // 测试点 - 倒序 + 筛选
    sortedTestPoints() {
      return [...this.testPoints].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    },
    filteredTestPoints() {
      let result = this.sortedTestPoints
      if (this.testPointSearch) {
        const q = this.testPointSearch.toLowerCase()
        result = result.filter(p => (p.name || '').toLowerCase().includes(q))
      }
      if (this.testPointRequirementFilter) {
        result = result.filter(p => p.requirementId === this.testPointRequirementFilter)
      }
      if (this.testPointRiskFilter) {
        result = result.filter(p => (p.riskLevel || 'MEDIUM') === this.testPointRiskFilter)
      }
      if (this.testPointTypeFilter) {
        result = result.filter(p => this.normalizeTestType(p.testType) === this.testPointTypeFilter)
      }
      return result
    },
    paginatedTestPoints() {
      const start = (this.testPointCurrentPage - 1) * this.testPointPageSize
      return this.filteredTestPoints.slice(start, start + this.testPointPageSize)
    },
    testPointTotalPages() {
      return Math.ceil(this.filteredTestPoints.length / this.testPointPageSize) || 1
    },
    // 生成用例 - 倒序 + 筛选
    filteredGenerateCases() {
      let result = this.sortedTestPoints
      if (this.generateCaseSearch) {
        const q = this.generateCaseSearch.toLowerCase()
        result = result.filter(p => (p.name || '').toLowerCase().includes(q))
      }
      return result
    },
    paginatedGenerateCases() {
      const start = (this.generateCaseCurrentPage - 1) * this.generateCasePageSize
      return this.filteredGenerateCases.slice(start, start + this.generateCasePageSize)
    },
    generateCaseTotalPages() {
      return Math.ceil(this.filteredGenerateCases.length / this.generateCasePageSize) || 1
    },
    // 测试用例 - 倒序 + 筛选
    sortedCases() {
      return [...this.testCases].sort((a, b) => new Date(b.createdAt || b.updatedAt) - new Date(a.createdAt || a.updatedAt))
    },
    filteredCases() {
      return this.sortedCases.filter(item => {
        const q = this.caseSearch.toLowerCase()
        const matchesSearch = !q || (item.title || '').toLowerCase().includes(q) || (item.caseNumber || '').toLowerCase().includes(q)
        const matchesStatus = !this.caseStatusFilter || item.status === this.caseStatusFilter
        const matchesType = !this.caseTypeFilter || this.normalizeTestType(item.testType) === this.caseTypeFilter
        const matchesExecutionType = !this.caseExecutionTypeFilter || (item.executionType || (item.requestData ? 'API_AUTOMATION' : 'MANUAL')) === this.caseExecutionTypeFilter
        return matchesSearch && matchesStatus && matchesType && matchesExecutionType
      })
    },
    paginatedCases() {
      const start = (this.testCaseCurrentPage - 1) * this.testCasePageSize
      return this.filteredCases.slice(start, start + this.testCasePageSize)
    },
    testCaseTotalPages() {
      return Math.ceil(this.filteredCases.length / this.testCasePageSize) || 1
    },
    // 接口测试 - 倒序 + 筛选
    filteredApiTestCases() {
      let result = this.sortedCases.filter(item => this.isInterfaceCase(item))
      if (this.apiTestSearch) {
        const q = this.apiTestSearch.toLowerCase()
        result = result.filter(item => (item.title || '').toLowerCase().includes(q) || (item.caseNumber || '').toLowerCase().includes(q))
      }
      return result
    },
    paginatedApiTestCases() {
      const start = (this.apiTestCurrentPage - 1) * this.apiTestPageSize
      return this.filteredApiTestCases.slice(start, start + this.apiTestPageSize)
    },
    apiTestTotalPages() {
      return Math.ceil(this.filteredApiTestCases.length / this.apiTestPageSize) || 1
    },
    // Web自动化 - 倒序 + 筛选
    filteredWebAutomationCases() {
      let result = this.sortedCases.filter(item => this.isWebAutomationCase(item))
      if (this.webAutomationSearch) {
        const q = this.webAutomationSearch.toLowerCase()
        result = result.filter(item => (item.title || '').toLowerCase().includes(q) || (item.caseNumber || '').toLowerCase().includes(q))
      }
      return result
    },
    paginatedWebAutomationCases() {
      const start = (this.webAutomationCurrentPage - 1) * this.webAutomationPageSize
      return this.filteredWebAutomationCases.slice(start, start + this.webAutomationPageSize)
    },
    webAutomationTotalPages() {
      return Math.ceil(this.filteredWebAutomationCases.length / this.webAutomationPageSize) || 1
    },
    sortedTasks() {
      return [...this.executionTasks].sort((a, b) => new Date(b.executedAt || b.createdAt) - new Date(a.executedAt || a.createdAt))
    }
  },
  watch: {
    activeModule(newVal) {
      localStorage.setItem('workbenchActiveModule', newVal)
      if (newVal === 'requirements' || newVal === 'testpoints') {
        this.expandedNav = true
        this.expandedCaseNav = false
      } else if (newVal === 'generate_cases' || newVal === 'test_cases') {
        this.expandedNav = false
        this.expandedCaseNav = true
      } else {
        this.expandedNav = false
        this.expandedCaseNav = false
      }
    },
    expandedNav(newVal) { localStorage.setItem('workbenchExpandedNav', newVal) },
    expandedCaseNav(newVal) { localStorage.setItem('workbenchExpandedCaseNav', newVal) },
    error(v) { if (v) setTimeout(() => { this.error = '' }, 4000) },
    info(v) { if (v) setTimeout(() => { this.info = '' }, 3000) }
  },
  mounted() {
    const module = this.$route.query.module
    if (module) this.activeModule = module
    this.loadProjectData().then(() => {
      const projectId = this.$route.params.id
      localStorage.setItem('lastProjectId', projectId)
    })
  },
  methods: {
    buildQuery(path, params) {
      const search = new URLSearchParams()
      Object.entries(params || {}).forEach(([key, value]) => {
        if (Array.isArray(value)) { value.forEach(item => search.append(key, item)) }
        else if (value !== undefined && value !== null && value !== '') { search.append(key, value) }
      })
      const query = search.toString()
      return query ? `${path}?${query}` : path
    },
    async loadProjectData() {
      this.loading = true
      this.error = ''
      try {
        const [projects, requirements, testPoints, testCases, tasks] = await Promise.all([
          api.get('/api/projects'),
          api.get('/api/requirements'),
          api.get('/api/test-points'),
          api.get('/api/test-cases'),
          api.get('/api/automation/tasks')
        ])
        const results = await Promise.allSettled(tasks.map(task => api.get(`/api/automation/results/${task.id}`)))
        this.projects = projects
        this.requirements = requirements.map(item => ({ ...item, testType: this.normalizeTestType(item.testType) }))
        this.testPoints = testPoints.map(item => ({ ...item, testType: this.normalizeTestType(item.testType), sourceType: item.sourceType || 'MIGRATED' }))
        this.testCases = testCases.map(item => ({
          ...item,
          testType: this.normalizeTestType(item.testType || (item.requestData || item.automationPayload ? 'INTERFACE' : 'FUNCTIONAL')),
          executionType: item.executionType || (item.automationPayload ? 'WEB_AUTOMATION' : (item.requestData ? 'API_AUTOMATION' : 'MANUAL'))
        }))
        this.executionTasks = tasks.map(item => ({ ...item, taskType: item.taskType || 'INTERFACE' }))
        this.executionResults = results.filter(item => item.status === 'fulfilled').map(item => item.value)
        this.selectedExecutionCaseIds = []
        this.selectAllExecutionCases = false
        return Promise.resolve()
      } catch (error) {
        this.error = error.message || '加载项目数据失败。'
        return Promise.reject(error)
      } finally {
        this.loading = false
      }
    },
    handleRequirementFileChange(event) {
      const [file] = event.target.files || []
      this.requirementForm.file = file || null
      if (file && !this.requirementForm.name) this.requirementForm.name = file.name.replace(/\.[^.]+$/, '')
    },
    async uploadRequirement() {
      if (!this.requirementForm.projectId) { this.error = '请先选择项目。'; return }
      if (!this.requirementForm.testType) { this.error = '请选择测试类型。'; return }
      if (!this.requirementForm.file) { this.error = '请先选择需求文件。'; return }
      this.busy = true; this.error = ''; this.info = ''
      try {
        const formData = new FormData()
        formData.append('projectId', this.requirementForm.projectId)
        formData.append('name', this.requirementForm.name || this.requirementForm.file.name)
        formData.append('version', this.requirementForm.version || 'v1.0')
        formData.append('testType', this.requirementForm.testType)
        formData.append('file', this.requirementForm.file)
        await api.upload('/api/requirements/upload', formData)
        this.info = '需求文档上传成功。'
        this.requirementForm = { name: '', version: 'v1.0', file: null, projectId: null, testType: '' }
        this.showUploadDialog = false
        await this.loadProjectData()
      } catch (error) { this.error = error.message || '上传需求失败。' }
      finally { this.busy = false }
    },
    openPromptDialog(requirementId) {
      this.promptForm = { requirementId, prompt: '' }
      this.showPromptDialog = true
    },
    async submitPromptDialog() {
      const { requirementId, prompt } = this.promptForm
      this.showPromptDialog = false
      await this.generateRequirementTestPoints(requirementId, prompt)
    },
    async generateRequirementTestPoints(id, prompt) {
      this.busy = true; this.error = ''; this.info = ''
      try {
        const body = prompt ? { prompt } : undefined
        await api.post(`/api/requirements/${id}/generate-test-points`, body)
        this.info = '测试点提取完成。'
        await this.loadProjectData()
      }
      catch (error) { this.error = error.message || '提取测试点失败。' }
      finally { this.busy = false }
    },
    async confirmRequirementTestPoints(id) {
      this.busy = true; this.error = ''; this.info = ''
      try { await api.post(`/api/requirements/${id}/confirm-test-points`); this.info = '测试点已确认。'; await this.loadProjectData() }
      catch (error) { this.error = error.message || '确认测试点失败。' }
      finally { this.busy = false }
    },
    generateCasesForPoint(testPointId) {
      this.casePromptForm = { type: 'single', testPointId, prompt: '' }
      this.showCasePromptDialog = true
    },
    batchGenerateTestCases() {
      if (!this.selectedPointIds.length) { this.error = '请先选择测试点。'; return }
      this.casePromptForm = { type: 'batch', testPointId: null, prompt: '' }
      this.showCasePromptDialog = true
    },
    async submitCasePromptDialog() {
      this.showCasePromptDialog = false
      if (this.casePromptForm.type === 'single') {
        await this.doGenerateCasesForPoint(this.casePromptForm.testPointId, this.casePromptForm.prompt)
      } else {
        await this.doBatchGenerateTestCases(this.casePromptForm.prompt)
      }
    },
    async doGenerateCasesForPoint(testPointId, prompt) {
      this.busy = true; this.error = ''; this.info = ''
      try { 
        await api.post(this.buildQuery('/api/test-cases/generate', { projectId: this.$route.params.id, testPointId, ...(prompt ? { prompt } : {}) })); 
        this.info = '测试用例生成成功。'; await this.loadProjectData() 
      }
      catch (error) { this.error = error.message || '生成测试用例失败。' }
      finally { this.busy = false }
    },
    async doBatchGenerateTestCases(prompt) {
      this.busy = true; this.error = ''; this.info = ''
      try { 
        await api.post(this.buildQuery('/api/test-cases/generate/batch', { projectId: this.$route.params.id, testPointIds: this.selectedPointIds, ...(prompt ? { prompt } : {}) })); 
        this.info = `已为 ${this.selectedPointIds.length} 个测试点生成测试用例。`; this.selectedPointIds = []; await this.loadProjectData() 
      }
      catch (error) { this.error = error.message || '批量生成用例失败。' }
      finally { this.busy = false }
    },
    toggleSelectAllGeneratePoints() {
      this.selectedPointIds = this.selectAllGeneratePoints ? this.filteredGenerateCases.map(p => p.id) : []
    },
    queueCaseExecution(item) {
      this.activeModule = 'api'
      if (!this.selectedExecutionCaseIds.includes(item.id)) this.selectedExecutionCaseIds.push(item.id)
    },
    async deleteCase(item) {
      if (!window.confirm(`确认删除测试用例"${item.title}"吗？`)) return
      this.busy = true; this.error = ''; this.info = ''
      try { await api.delete(`/api/test-cases/${item.id}`); this.info = '测试用例已删除。'; await this.loadProjectData() }
      catch (error) { this.error = error.message || '删除测试用例失败。' }
      finally { this.busy = false }
    },
    toggleAllExecutionCases() {
      this.selectedExecutionCaseIds = this.selectAllExecutionCases ? this.filteredApiTestCases.map(item => item.id) : []
    },
    resetApiTestFilters() {
      this.apiTestSearch = ''
      this.executionEnvironment = 'PRODUCTION'
      this.apiTestCurrentPage = 1
    },
    async executeSingleCase(testCaseId) {
      this.busy = true; this.error = ''; this.info = ''
      try {
        const task = await api.post('/api/automation/tasks', { projectId: this.$route.params.id, testCaseId, environment: this.executionEnvironment })
        await api.post(`/api/automation/tasks/${task.id}/execute`)
        this.info = '接口测试任务已发起。'; await this.loadProjectData()
      } catch (error) { this.error = error.message || '执行接口测试失败。' }
      finally { this.busy = false }
    },
    async executeSingleWebCase(testCaseId) {
      this.busy = true; this.error = ''; this.info = ''
      try {
        const task = await api.post('/api/automation/tasks', { projectId: this.$route.params.id, testCaseId, environment: this.webAutomationEnvironment })
        await api.post(`/api/automation/tasks/${task.id}/execute`)
        this.info = 'Web 自动化任务已发起。'; await this.loadProjectData()
      } catch (error) { this.error = error.message || '执行 Web 自动化失败。' }
      finally { this.busy = false }
    },
    async batchExecuteCases() {
      if (!this.selectedExecutionCaseIds.length) { this.error = '请先选择要执行的用例。'; return }
      this.busy = true; this.error = ''; this.info = ''
      try {
        const tasks = await api.post('/api/automation/tasks/batch', { projectId: this.$route.params.id, testCaseIds: this.selectedExecutionCaseIds, environment: this.executionEnvironment })
        await api.post('/api/automation/tasks/batch/execute', tasks.map(item => item.id))
        this.info = `已发起 ${this.selectedExecutionCaseIds.length} 条执行任务。`; await this.loadProjectData()
      } catch (error) { this.error = error.message || '批量执行失败。' }
      finally { this.busy = false }
    },
    async batchExecuteWebCases() {
      if (!this.selectedWebAutomationCaseIds.length) { this.error = '请先选择要执行的 Web 用例。'; return }
      this.busy = true; this.error = ''; this.info = ''
      try {
        const tasks = await api.post('/api/automation/tasks/batch', { projectId: this.$route.params.id, testCaseIds: this.selectedWebAutomationCaseIds, environment: this.webAutomationEnvironment })
        await api.post('/api/automation/tasks/batch/execute', tasks.map(item => item.id))
        this.info = `已发起 ${this.selectedWebAutomationCaseIds.length} 条 Web 自动化任务。`
        this.selectedWebAutomationCaseIds = []
        this.selectAllWebAutomationCases = false
        await this.loadProjectData()
      } catch (error) { this.error = error.message || '批量执行 Web 自动化失败。' }
      finally { this.busy = false }
    },
    async retryTask(id) {
      this.busy = true; this.error = ''; this.info = ''
      try { await api.post(`/api/automation/tasks/${id}/retry`); this.info = '任务已重新发起。'; await this.loadProjectData() }
      catch (error) { this.error = error.message || '任务重试失败。' }
      finally { this.busy = false }
    },
    // Web自动化相关方法
    resetWebAutomationFilters() {
      this.webAutomationSearch = ''
      this.webAutomationEnvironment = 'PRODUCTION'
      this.webAutomationCurrentPage = 1
    },
    toggleAllWebAutomationCases() {
      if (this.selectAllWebAutomationCases) {
        this.selectedWebAutomationCaseIds = this.filteredWebAutomationCases.map(item => item.id)
      } else {
        this.selectedWebAutomationCaseIds = []
      }
    },
    executeWebAutomationCase(caseId) {
      this.executeSingleWebCase(caseId)
    },
    batchExecuteWebAutomationCases() {
      if (!this.selectedWebAutomationCaseIds.length) return
      this.batchExecuteWebCases()
    },
    async viewAllureReport(taskId) {
      this.busy = true; this.error = ''; this.info = ''
      try {
        const response = await api.post(`/api/automation/tasks/${taskId}/allure-report`)
        if (response.reportUrl) { window.open(API_BASE_URL + response.reportUrl, '_blank') }
        else if (response.error) { this.error = '生成报告失败：' + response.error }
        else { this.error = '生成报告失败：未返回报告路径' }
      } catch (error) { this.error = error.message || '生成 Allure 报告失败。' }
      finally { this.busy = false }
    },
    async openResultModal(task) {
      const cached = this.getExecutionResult(task.id)
      if (cached) { this.resultModal = { task, result: cached }; return }
      try { const result = await api.get(`/api/automation/results/${task.id}`); this.resultModal = { task, result } }
      catch (error) { this.error = error.message || '获取执行结果失败。' }
    },
    buildResultText(payload) {
      const result = payload.result || {}
      return [`结果: ${this.resultLabel(result.result)}`, `状态码: ${result.responseStatus ?? '-'}`, `请求: ${result.requestMethod || '-'} ${result.requestUrl || ''}`, '', '请求体:', result.requestBody || '暂无', '', '响应体:', result.responseBody || '暂无', '', '断言:', result.assertionResult || '暂无', '', '分析:', result.llmAnalysis || result.errorMessage || '暂无'].join('\n')
    },
    openPayloadModal(item) {
      this.payloadModal = item
    },
    parseJsonSafe(value) {
      if (!value) return null
      if (typeof value === 'object') return value
      try { return JSON.parse(value) } catch (error) { return null }
    },
    prettyJsonText(value) {
      const parsed = this.parseJsonSafe(value)
      return parsed ? JSON.stringify(parsed, null, 2) : (value || '暂无数据')
    },
    buildPayloadSteps(automationPayload) {
      const payload = this.parseJsonSafe(automationPayload)
      const steps = Array.isArray(payload?.steps) ? payload.steps : []
      return steps.map((step, index) => ({
        index: index + 1,
        action: step.action || 'unknown',
        description: step.description || step.target || step.selector || '未填写描述',
        selector: step.selector || ''
      }))
    },
    buildArtifactSteps(artifacts) {
      const payload = this.parseJsonSafe(artifacts)
      const steps = Array.isArray(payload?.steps) ? payload.steps : []
      return steps.map((step, index) => ({
        index: step.index || index + 1,
        name: step.name || step.description || `Web step ${index + 1}`,
        status: step.status || 'UNKNOWN'
      }))
    },
    truncateText(text, maxLength = 40) {
      if (!text) return ''
      return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
    },
    getRequirementName(id) { return this.requirements.find(item => item.id === id)?.name || `需求 #${id}` },
    getTestPointName(id) { return this.testPoints.find(item => item.id === id)?.name || `测试点 #${id}` },
    getCaseTitle(id) { return this.testCases.find(item => item.id === id)?.title || `测试用例 #${id}` },
    getExecutionResult(taskId) { return this.executionResults.find(item => item.taskId === taskId) },
    getProjectName(projectId) { return this.projects.find(item => item.id === projectId)?.name || `项目 #${projectId}` },
    normalizeTestType(testType) { return testType === 'FUNCTIONAL' ? 'FUNCTIONAL' : 'INTERFACE' },
    testTypeLabel(testType) { return { FUNCTIONAL: '功能测试', INTERFACE: '接口测试' }[this.normalizeTestType(testType)] },
    executionTypeLabel(executionType) { return { MANUAL: '手工执行', API_AUTOMATION: '接口自动化', WEB_AUTOMATION: 'Web自动化' }[executionType || 'MANUAL'] || (executionType || '手工执行') },
    pointSourceLabel(sourceType) { return { LLM_GENERATED: 'LLM生成', MANUAL_CREATED: '手工创建', MIGRATED: '历史迁移' }[sourceType || 'MIGRATED'] || (sourceType || '历史迁移') },
    isInterfaceCase(item) { return this.normalizeTestType(item?.testType) === 'INTERFACE' },
    isWebAutomationCase(item) { return this.normalizeTestType(item?.testType) === 'FUNCTIONAL' },
    getLastTaskForCase(testCaseId) {
      return this.executionTasks
        .filter(task => task.testCaseId === testCaseId)
        .sort((a, b) => new Date(b.executedAt || b.createdAt) - new Date(a.executedAt || a.createdAt))[0]
    },
    executionCaseStatusLabel(item) { return this.getLastTaskForCase(item.id) ? '已执行' : '未执行' },
    executionCaseStatusClass(item) { return this.getLastTaskForCase(item.id) ? 'pc-status--active' : 'pc-status--archived' },
    requirementStatusLabel(status) { return { PENDING_ANALYSIS: '待分析', GENERATED: '已提取', CONFIRMED: '已确认' }[status] || status || '未知' },
    caseStatusLabel(status) { return { PENDING_REVIEW: '待评审', APPROVED: '已通过', REJECTED: '已拒绝' }[status] || status || '未知' },
    taskStatusLabel(status) { return { PENDING: '排队中', RUNNING: '执行中', COMPLETED: '已完成', FAILED: '失败' }[status] || status || '未知' },
    resultLabel(status) { return { PASSED: '通过', FAILED: '失败' }[status] || '未返回' },
    openWebAutomationModule() { this.activeModule = 'web_automation'; this.info = '已切换到 Web 自动化执行视图。' },
    getStatusClass(status) {
      return { PENDING_ANALYSIS: 'pc-status--archived', GENERATED: 'pc-status--active', CONFIRMED: 'pc-status--active', PENDING_REVIEW: 'pc-status--archived', APPROVED: 'pc-status--active', REJECTED: 'pc-status--archived' }[status] || 'pc-status--archived'
    },
    formatDate(value) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '未记录' },
    backToProjects() { this.$router.push('/projects') },
    // 筛选重置方法
    resetRequirementFilters() { this.requirementSearch = ''; this.requirementProjectFilter = ''; this.requirementStatusFilter = ''; this.requirementTestTypeFilter = '' },
    resetTestPointFilters() { this.testPointSearch = ''; this.testPointRequirementFilter = ''; this.testPointRiskFilter = ''; this.testPointTypeFilter = '' },
    resetCaseFilters() { this.caseSearch = ''; this.caseStatusFilter = ''; this.caseTypeFilter = ''; this.caseExecutionTypeFilter = '' },
    // 测试点相关
    addTestPoint() {
      this.isEditTestPoint = false
      this.testPointForm = { id: null, name: '', requirementId: '', sceneType: 'Functional', riskLevel: 'MEDIUM', description: '', testType: 'INTERFACE', sourceType: 'MANUAL_CREATED' }
      this.showTestPointDialog = true
    },
    editTestPoint(point) { this.isEditTestPoint = true; this.testPointForm = { sourceType: 'MANUAL_CREATED', ...point, testType: this.normalizeTestType(point.testType) }; this.showTestPointDialog = true },
    updateDescription(event) {
      this.testPointForm.description = event.target.innerHTML;
    },
    async saveTestPoint() {
      if (!this.testPointForm.name) { this.error = '请输入测试点名称。'; return }
      if (!this.testPointForm.testType) { this.error = '请选择测试类型。'; return }
      this.busy = true; this.error = ''; this.info = ''
      try {
        if (this.isEditTestPoint) { await api.put(`/api/test-points/${this.testPointForm.id}`, this.testPointForm); this.info = '测试点编辑成功。' }
        else { await api.post('/api/test-points', { ...this.testPointForm, projectId: this.$route.params.id }); this.info = '测试点新增成功。' }
        this.showTestPointDialog = false; await this.loadProjectData()
      } catch (error) { this.error = error.message || '保存测试点失败。' }
      finally { this.busy = false }
    },
    async deleteTestPoint(point) {
      if (!window.confirm(`确认删除测试点"${point.name}"吗？`)) return
      this.busy = true; this.error = ''; this.info = ''
      try { await api.delete(`/api/test-points/${point.id}`); this.info = '测试点已删除。'; await this.loadProjectData() }
      catch (error) { this.error = error.message || '删除测试点失败。' }
      finally { this.busy = false }
    },
    // 测试用例相关
    addTestCase() {
      this.isEditTestCase = false
      this.testCaseForm = { id: null, projectId: this.$route.params.id, testPointId: null, title: '', precondition: '', steps: '', expectedResult: '', priority: 'MEDIUM', automation: false, status: 'PENDING_REVIEW', testType: 'INTERFACE', executionType: 'WEB_AUTOMATION', automationPayload: '' }
      this.showTestCaseDialog = true
    },
    editTestCase(item) { this.isEditTestCase = true; this.testCaseForm = { executionType: 'WEB_AUTOMATION', automationPayload: '', ...item, testType: this.normalizeTestType(item.testType) }; this.showTestCaseDialog = true },
    async saveTestCase() {
      if (!this.testCaseForm.projectId) { this.error = '请选择项目。'; return }
      if (!this.testCaseForm.testPointId) { this.error = '请选择测试点。'; return }
      if (!this.testCaseForm.title) { this.error = '请输入测试用例标题。'; return }
      if (!this.testCaseForm.steps) { this.error = '请输入测试步骤。'; return }
      if (!this.testCaseForm.expectedResult) { this.error = '请输入预期结果。'; return }
      if (!this.testCaseForm.testType) { this.error = '请选择测试类型。'; return }
      if (!this.testCaseForm.executionType) { this.error = '请选择执行方式。'; return }
      this.busy = true; this.error = ''; this.info = ''
      try {
        if (this.isEditTestCase) { await api.put(`/api/test-cases/${this.testCaseForm.id}`, this.testCaseForm); this.info = '测试用例编辑成功。' }
        else { await api.post('/api/test-cases', this.testCaseForm); this.info = '测试用例新增成功。' }
        this.showTestCaseDialog = false; await this.loadProjectData()
      } catch (error) { this.error = error.message || '保存测试用例失败。' }
      finally { this.busy = false }
    },
    logout() { clearStoredToken(); clearStoredUser(); this.$router.push('/login') }
  }
}
</script>

<style scoped>
/* ============================================================ 
   项目工作台 - 与项目中心保持一致的设计风格
   ============================================================ */
:root { --pc-sky: #0EA5E9; --pc-sky-dark: #0284C7; --pc-sky-light: #BAE6FD; --pc-sky-lighter: #E0F2FE; --pc-sky-bg: #F0F9FF; }
.pc-layout { display: flex; width: 100%; min-height: 100vh; background: #f5f6f8; font-family: 'Microsoft YaHei', 'PingFang SC', -apple-system, sans-serif; }
.pc-sidebar { width: 200px; background: #fff; border-right: 1px solid #e4e7ed; display: flex; flex-direction: column; flex-shrink: 0; box-shadow: 2px 0 6px rgba(0,0,0,0.03); }
.pc-sidebar-header { padding: 16px; border-bottom: 1px solid #e4e7ed; }
.pc-logo { display: flex; align-items: center; gap: 10px; }
.pc-logo-icon { width: 32px; height: 32px; background: linear-gradient(135deg, #0EA5E9, #0369A1); color: #fff; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 16px; }
.pc-logo-text { font-size: 14px; font-weight: 700; color: #333; }
.pc-sidebar-nav { flex: 1; padding: 8px 0; }
.pc-nav-item { display: flex; align-items: center; gap: 10px; padding: 11px 20px; font-size: 13px; color: #555; cursor: pointer; transition: all 0.2s; border-left: 3px solid transparent; }
.pc-nav-item:hover { background: #E0F2FE; color: #0284C7; }
.pc-nav-item--active { background: #E0F2FE; color: #0369A1; font-weight: 600; border-left-color: #0EA5E9; }
.pc-nav-item--has-children { position: relative; }
.pc-nav-item--expanded { background: #F0F9FF; }
.pc-nav-submenu { background: #F8FCFF; }
.pc-nav-subitem { padding: 9px 20px 9px 48px; font-size: 12px; color: #666; cursor: pointer; transition: all 0.2s; }
.pc-nav-subitem:hover { color: #0284C7; background: #E0F2FE; }
.pc-nav-subitem--active { color: #0369A1; font-weight: 600; background: #E0F2FE; }
.pc-nav-icon { width: 18px; height: 18px; flex-shrink: 0; }
.pc-sidebar-footer { padding: 12px 16px; border-top: 1px solid #e4e7ed; display: flex; align-items: center; justify-content: space-between; }
.pc-user-info { display: flex; align-items: center; gap: 8px; }
.pc-avatar { width: 28px; height: 28px; background: linear-gradient(135deg, #0EA5E9, #0369A1); color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; }
.pc-user-name { font-size: 12px; color: #555; }
.pc-btn-logout { background: none; border: none; cursor: pointer; color: #999; padding: 4px; border-radius: 4px; transition: all 0.2s; }
.pc-btn-logout:hover { color: #f44336; background: #fef0f0; }
.pc-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-width: 0; }
.pc-topbar { background: #fff; border-bottom: 1px solid #e4e7ed; padding: 8px 20px; display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; }
.pc-breadcrumb { display: flex; align-items: center; gap: 4px; font-size: 13px; }
.pc-breadcrumb-item { color: #888; cursor: pointer; }
.pc-breadcrumb-item:hover { color: #0EA5E9; }
.pc-breadcrumb-item--active { color: #333; font-weight: 500; cursor: default; }
.pc-breadcrumb-item--active:hover { color: #333; }
.pc-topbar-right { display: flex; align-items: center; gap: 8px; }
.pc-content-section { flex: 1; display: flex; flex-direction: column; overflow: auto; }
.pc-filter-bar { background: #fff; padding: 14px 20px; border-bottom: 1px solid #e4e7ed; flex-shrink: 0; }
.pc-filter-row { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.pc-filter-group { display: flex; align-items: center; gap: 6px; }
.pc-filter-label { font-size: 13px; color: #555; white-space: nowrap; }
.pc-filter-input { height: 32px; padding: 0 10px; font-size: 13px; border: 1px solid #dcdfe6; border-radius: 4px; outline: none; color: #333; background: #fff; min-width: 140px; transition: border-color 0.2s; }
.pc-filter-input:hover { border-color: #0EA5E9; }
.pc-filter-input:focus { border-color: #0EA5E9; box-shadow: 0 0 0 2px rgba(14,165,233,0.12); }
.pc-filter-input::placeholder { color: #bbb; }
.pc-filter-input--wide { min-width: 200px; }
.pc-filter-select { height: 32px; padding: 0 28px 0 10px; font-size: 13px; border: 1px solid #dcdfe6; border-radius: 4px; outline: none; color: #333; background: #fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M0 0l5 6 5-6z' fill='%23888'/%3E%3C/svg%3E") no-repeat right 8px center; appearance: none; cursor: pointer; min-width: 100px; transition: border-color 0.2s; }
.pc-filter-select:hover { border-color: #0EA5E9; }
.pc-filter-select:focus { border-color: #0EA5E9; box-shadow: 0 0 0 2px rgba(14,165,233,0.12); }
.pc-filter-actions { display: flex; align-items: center; gap: 8px; margin-left: auto; }
.pc-btn { display: inline-flex; align-items: center; justify-content: center; gap: 5px; height: 32px; padding: 0 14px; font-size: 13px; border: 1px solid #dcdfe6; border-radius: 4px; cursor: pointer; white-space: nowrap; transition: all 0.2s; outline: none; background: #fff; color: #555; font-family: inherit; }
.pc-btn:hover { color: #0EA5E9; border-color: #BAE6FD; }
.pc-btn:active { transform: scale(0.97); }
.pc-btn svg { width: 14px; height: 14px; flex-shrink: 0; }
.pc-btn--primary { background: #0EA5E9; border-color: #0EA5E9; color: #fff; }
.pc-btn--primary:hover { background: #0284C7; border-color: #0284C7; color: #fff; }
.pc-btn--success { background: #0EA5E9; border-color: #0EA5E9; color: #fff; }
.pc-btn--success:hover { background: #0284C7; border-color: #0284C7; color: #fff; }
.pc-btn--danger { background: #fff; border-color: #fbc4c4; color: #f44336; }
.pc-btn--danger:hover { background: #fef0f0; border-color: #f44336; color: #f44336; }
.pc-btn--sm { height: 28px; padding: 0 10px; font-size: 12px; }
.pc-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.pc-toolbar { background: #fff; padding: 10px 20px; border-bottom: 1px solid #e4e7ed; display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; }
.pc-toolbar-left, .pc-toolbar-right { display: flex; align-items: center; gap: 10px; }
.pc-toolbar-info { font-size: 12px; color: #888; }
.pc-toolbar-info strong { color: #0EA5E9; font-weight: 600; }
.pc-card { flex: 1; overflow: auto; background: #fff; }
.pc-card-body { padding: 0; }
.pc-card-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid #e4e7ed; }
.pc-card-header h3 { margin: 0; font-size: 15px; font-weight: 600; color: #333; }
.pc-card-header-actions { display: flex; align-items: center; gap: 8px; }
.pc-table-container { flex: 1; overflow: auto; background: #fff; }
.pc-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.pc-table thead { position: sticky; top: 0; z-index: 2; }
.pc-table th {
  height: 40px;
  padding: 8px 12px;
  font-weight: 600;
  font-size: 13px;
  color: #555;
  background: #fafbfc;
  border-bottom: 2px solid #BAE6FD;
  text-align: left;
  white-space: nowrap;
}

.pc-table td {
  height: 46px;
  padding: 8px 8px;
  color: #333;
  border-bottom: 1px solid #ebeef5;
  text-align: left;
  white-space: nowrap;
}

.pc-table th {
  padding: 8px 8px;
}

.pc-table th:first-child,
.pc-table td:first-child {
  text-align: center;
  padding-left: 12px;
  padding-right: 12px;
}

.pc-table th:not(:first-child),
.pc-table td:not(:first-child) {
  text-align: left;
  padding-left: 8px;
  padding-right: 8px;
}

.pc-table td.text-left {
  text-align: left;
}
.pc-table td.pc-text-cell { max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pc-table tbody tr { transition: background 0.15s; }
.pc-table tbody tr:hover { background: #F0F9FF; }
.pc-table tbody tr:nth-child(even) { background: #F8FCFF; }
.pc-table tbody tr:nth-child(even):hover { background: #F0F9FF; }
.pc-table--wrap td { white-space: normal; word-break: break-all; }
.pc-table-cell--wrap { white-space: normal !important; word-break: break-all; }
.pc-checkbox { width: 15px; height: 15px; accent-color: #0EA5E9; cursor: pointer; }
.pc-num-highlight { color: #0EA5E9; font-weight: 600; }
.pc-status { display: inline-flex; align-items: center; padding: 2px 10px; font-size: 12px; border-radius: 3px; white-space: nowrap; }
.pc-status--active { color: #0EA5E9; background: #E0F2FE; border: 1px solid #BAE6FD; }
.pc-status--archived { color: #909399; background: #f4f4f5; border: 1px solid #e0e0e0; }
.pc-action-cell { display: flex; align-items: center; justify-content: center; gap: 6px; }
.pc-btn-text { background: none; border: none; cursor: pointer; font-size: 12px; padding: 4px 8px; border-radius: 3px; transition: all 0.2s; font-family: inherit; }
.pc-btn-text--primary { color: #0EA5E9; }
.pc-btn-text--primary:hover { background: #E0F2FE; color: #0369A1; }
.pc-btn-text--danger { color: #f44336; }
.pc-btn-text--danger:hover { background: #fef0f0; color: #d32f2f; }
.pc-btn-text--success { color: #0EA5E9; }
.pc-btn-text--success:hover { background: #E0F2FE; color: #0369A1; }
.pc-btn-text:disabled { opacity: 0.4; cursor: not-allowed; }
.pc-pagination { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 12px 20px; background: #fff; border-top: 1px solid #e4e7ed; flex-shrink: 0; font-size: 13px; }
.pc-pagination-info { color: #888; }
.pc-pagination-info strong { color: #0EA5E9; }
.pc-pagination-pages { display: flex; align-items: center; gap: 4px; }
.pc-page-btn { min-width: 28px; height: 28px; padding: 0 6px; font-size: 12px; border: 1px solid #dcdfe6; border-radius: 3px; background: #fff; color: #555; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; font-family: inherit; }
.pc-page-btn:hover { color: #0EA5E9; border-color: #BAE6FD; }
.pc-page-btn--active { background: #0EA5E9; border-color: #0EA5E9; color: #fff; }
.pc-page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.pc-pagination-select { height: 28px; padding: 0 20px 0 8px; font-size: 12px; border: 1px solid #dcdfe6; border-radius: 3px; background: #fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M0 0l5 6 5-6z' fill='%23888'/%3E%3C/svg%3E") no-repeat right 6px center; appearance: none; cursor: pointer; outline: none; color: #555; font-family: inherit; }
.pc-loading { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 60px 20px; color: #888; font-size: 14px; }
.pc-spinner { width: 24px; height: 24px; border: 3px solid #E0F2FE; border-top-color: #0EA5E9; border-radius: 50%; animation: pc-spin 0.8s linear infinite; }
@keyframes pc-spin { to { transform: rotate(360deg); } }
.pc-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 20px; color: #888; }
.pc-empty h4 { margin: 16px 0 6px; color: #666; font-size: 15px; }
.pc-empty p { margin: 0; font-size: 13px; color: #aaa; }
.pc-toast { position: fixed; top: 20px; left: 50%; transform: translateX(-50%); padding: 10px 28px; border-radius: 6px; font-size: 13px; box-shadow: 0 4px 16px rgba(0,0,0,0.12); z-index: 9999; cursor: pointer; }
.pc-toast--success { background: #E0F2FE; color: #0369A1; border: 1px solid #BAE6FD; }
.pc-toast--error { background: #fef0f0; color: #f44336; border: 1px solid #fbc4c4; }
.toast-enter-active { transition: all 0.3s ease; }
.toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from { opacity: 0; transform: translateX(-50%) translateY(-12px); }
.toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(-12px); }
.pc-dialog-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.pc-dialog { background: #fff; border-radius: 8px; box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12); width: 100%; max-width: 500px; overflow: hidden; animation: pc-dialog-fade-in 0.3s ease; }
.pc-dialog--wide { max-width: 860px; }
@keyframes pc-dialog-fade-in { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
.pc-dialog-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid #e4e7ed; background: #fafbfc; }
.pc-dialog-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: #333; }
.pc-dialog-close { background: none; border: none; font-size: 20px; color: #999; cursor: pointer; padding: 0; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; border-radius: 4px; transition: all 0.2s; }
.pc-dialog-close:hover { color: #666; background: #f0f0f0; }
.pc-dialog-body { padding: 20px; }
.pc-dialog-form { display: flex; flex-direction: column; gap: 16px; }
.pc-dialog-form-group { display: flex; flex-direction: column; gap: 6px; }
.pc-dialog-form-label { font-size: 13px; color: #555; font-weight: 500; }
.pc-dialog-form-label em { color: #f44336; font-style: normal; }
.pc-dialog-form-input { height: 36px; padding: 0 12px; font-size: 13px; border: 1px solid #dcdfe6; border-radius: 4px; outline: none; color: #333; background: #fff; transition: border-color 0.2s; }
.pc-dialog-form-input:hover { border-color: #0EA5E9; }
.pc-dialog-form-input:focus { border-color: #0EA5E9; box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.12); }
.pc-dialog-form-input::placeholder { color: #bbb; }

.pc-rich-text-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.pc-rich-text-content {
  min-height: 120px;
  padding: 12px;
  outline: none;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.4;
}

.pc-rich-text-content:focus {
  border-color: #0EA5E9;
  box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.12);
}
.pc-dialog-form-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; margin-top: 8px; }
.pc-code-block { background: #f5f6f8; border: 1px solid #e4e7ed; border-radius: 4px; padding: 12px; font-size: 12px; font-family: 'Consolas', monospace; white-space: pre-wrap; word-break: break-all; max-height: 400px; overflow: auto; color: #333; }
.pc-payload-summary { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.pc-payload-step { display: flex; align-items: center; gap: 10px; padding: 8px 10px; border: 1px solid #e4e7ed; border-radius: 4px; background: #fafbfc; font-size: 12px; color: #333; }
.pc-payload-step code { padding: 2px 6px; border-radius: 3px; background: #E0F2FE; color: #0369A1; font-family: 'Consolas', monospace; }
.pc-link { color: #0EA5E9; text-decoration: none; }
.pc-link:hover { text-decoration: underline; }
@media (max-width: 1024px) { .pc-sidebar { width: 160px; } .pc-filter-row { flex-wrap: wrap; } }
@media (max-width: 768px) { .pc-layout { flex-direction: column; } .pc-sidebar { width: 100%; flex-direction: row; height: auto; overflow-x: auto; } .pc-sidebar-nav { display: flex; flex-direction: row; } .pc-nav-item { border-left: none; border-bottom: 3px solid transparent; } .pc-nav-item--active { border-left: none; border-bottom-color: #0EA5E9; } .pc-dialog { margin: 0 20px; max-width: none; } }
</style>
