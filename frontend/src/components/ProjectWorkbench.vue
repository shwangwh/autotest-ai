<template>
  <div class="workbench-page animate-slide-in-up">
    <header class="page-header">
      <div>
        <p class="eyebrow">Project Workbench</p>
        <h1>{{ project?.name || '项目工作台' }}</h1>
        <p class="subtext">管理项目的测试用例、执行任务和自动化测试。</p>
      </div>
      <div class="header-actions">

        <button class="ghost-btn" type="button" @click="logout">退出登录</button>
      </div>
    </header>

    <div class="workbench-content">
      <section class="test-cases-panel">
        <div class="panel-head">
          <div class="panel-head-left">
            <h2>测试用例管理</h2>
            <p v-if="testCases.length">共 {{ testCases.length }} 个测试用例</p>
          </div>
          <div class="panel-head-right">
            <div class="filter-bar">
              <input v-model="testCaseSearch" type="text" placeholder="搜索测试用例" class="search-input" />
              <select v-model="testCaseStatus" class="filter-select">
                <option value="">全部状态</option>
                <option value="PENDING_REVIEW">待审核</option>
                <option value="APPROVED">已批准</option>
                <option value="REJECTED">已拒绝</option>
              </select>
              <button class="ghost-btn small" type="button" @click="clearTestCaseFilters">清除筛选</button>
            </div>
          </div>
        </div>

        <div v-if="loading" class="empty-state">正在加载测试用例数据...</div>
        <div v-else-if="!filteredTestCases.length" class="empty-state">
          {{ testCaseSearch || testCaseStatus ? '没有符合条件的测试用例' : '当前还没有测试用例，先生成一些吧。' }}
        </div>
        <div v-else class="table-container">
          <div class="table-header-actions">
            <label class="checkbox-label">
              <input type="checkbox" v-model="selectAllTestCases" @change="toggleSelectAllTestCases" />
              全选
            </label>
            <div class="batch-actions">
              <button 
                class="primary-btn small" 
                type="button" 
                @click="batchGenerateTestCases" 
                :disabled="selectedTestPoints.length === 0"
              >
                批量生成用例
              </button>
              <button 
                class="primary-btn small" 
                type="button" 
                @click="batchExecuteTestCases" 
                :disabled="selectedTestCases.length === 0"
              >
                批量执行
              </button>
            </div>
          </div>
          <table class="test-case-table">
            <thead>
              <tr>
                <th class="checkbox-column">
                  <input type="checkbox" v-model="selectAllTestCases" @change="toggleSelectAllTestCases" />
                </th>
                <th>用例编号</th>
                <th>标题</th>
                <th>测试点</th>
                <th>优先级</th>
                <th>状态</th>
                <th>执行状态</th>
                <th>创建人</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="testCase in filteredTestCases" :key="testCase.id" class="test-case-row">
                <td class="checkbox-column">
                  <input type="checkbox" v-model="selectedTestCases" :value="testCase.id" />
                </td>
                <td>{{ testCase.caseNumber }}</td>
                <td>{{ truncateText(testCase.title) }}</td>
                <td>{{ getTestPointName(testCase.testPointId) }}</td>
                <td>
                  <span :class="['priority-chip', testCase.priority.toLowerCase()]">{{ testCase.priority }}</span>
                </td>
                <td>
                  <span :class="['status-chip', testCase.status.toLowerCase()]">{{ testCaseStatusLabel(testCase.status) }}</span>
                </td>
                <td>
                  <span :class="['execution-status-chip', getTestCaseExecutionStatus(testCase.id).toLowerCase()]">{{ getTestCaseExecutionStatus(testCase.id) }}</span>
                </td>
                <td>{{ testCase.creator }}</td>
                <td>
                  <div class="row-actions">
                    <button class="text-btn small" type="button" @click="executeTestCase(testCase)">执行</button>
                    <button class="danger-btn small" type="button" @click="deleteTestCase(testCase)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="test-points-panel">
        <div class="panel-head">
          <div class="panel-head-left">
            <h2>测试点管理</h2>
            <p v-if="testPoints.length">共 {{ testPoints.length }} 个测试点</p>
          </div>
          <div class="panel-head-right">
            <button class="primary-btn small" type="button" @click="openAddTestPointModal">
              新增测试点
            </button>
          </div>
        </div>

        <div v-if="loading" class="empty-state">正在加载测试点数据...</div>
        <div v-else-if="!testPoints.length" class="empty-state">
          当前还没有测试点，需要先添加需求。
        </div>
        <div v-else class="table-container">
          <div class="table-header-actions">
            <label class="checkbox-label">
              <input type="checkbox" v-model="selectAllTestPoints" @change="toggleSelectAllTestPoints" />
              全选
            </label>
          </div>
          <table class="test-point-table">
            <thead>
              <tr>
                <th class="checkbox-column">
                  <input type="checkbox" v-model="selectAllTestPoints" @change="toggleSelectAllTestPoints" />
                </th>
                <th>测试点名称</th>
                <th>需求</th>
                <th>用例数</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="testPoint in testPoints" :key="testPoint.id" class="test-point-row">
                <td class="checkbox-column">
                  <input type="checkbox" v-model="selectedTestPoints" :value="testPoint.id" />
                </td>
                <td>{{ testPoint.name }}</td>
                <td>{{ getRequirementName(testPoint.requirementId) }}</td>
                <td>{{ getTestCaseCountByTestPoint(testPoint.id) }}</td>
                <td>
                  <div class="row-actions">
                    <button class="text-btn small" type="button" @click="generateTestCases(testPoint.id)">生成用例</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="execution-panel">
        <div class="panel-head">
          <div class="panel-head-left">
            <h2>执行任务管理</h2>
            <p v-if="executionTasks.length">共 {{ executionTasks.length }} 个执行任务</p>
          </div>
        </div>

        <div v-if="loading" class="empty-state">正在加载执行任务数据...</div>
        <div v-else-if="!executionTasks.length" class="empty-state">
          当前还没有执行任务，先执行一些测试用例吧。
        </div>
        <div v-else class="table-container">
          <table class="execution-table">
            <thead>
              <tr>
                <th>任务ID</th>
                <th>测试用例</th>
                <th>环境</th>
                <th>状态</th>
                <th>执行时间</th>
                <th>结果</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="task in executionTasks" :key="task.id" class="execution-row">
                <td>{{ task.id }}</td>
                <td>{{ getTestCaseTitle(task.testCaseId) }}</td>
                <td>{{ task.environment }}</td>
                <td>
                  <span :class="['status-chip', task.status.toLowerCase()]">{{ task.status }}</span>
                </td>
                <td>{{ formatDate(task.executedAt) }}</td>
                <td>
                  <span v-if="getExecutionResult(task.id)" :class="['result-chip', getExecutionResult(task.id).result.toLowerCase()]">
                    {{ getExecutionResult(task.id).result }}
                  </span>
                  <span v-else>-</span>
                </td>
                <td>
                  <div class="row-actions">
                    <button v-if="task.status !== 'RUNNING'" class="text-btn small" type="button" @click="retryTask(task.id)">重试</button>
                    <button class="text-btn small" type="button" @click="viewResult(task.id)">查看结果</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <div v-if="error" class="message error">{{ error }}</div>
    <div v-if="info" class="message info">{{ info }}</div>
  </div>

  <!-- 测试点查看模态框 -->
  <div v-if="showTestPointModal" class="modal-overlay" @click="closeTestPointModal">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>测试点详情</h3>
        <button class="close-btn" @click="closeTestPointModal">×</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label>名称</label>
          <input v-model="testPointForm.name" type="text" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>所属需求</label>
          <input :value="getRequirementName(testPointForm.requirementId)" type="text" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>场景类型</label>
          <input v-model="testPointForm.sceneType" type="text" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>风险等级</label>
          <input v-model="testPointForm.riskLevel" type="text" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>自动化建议</label>
          <input :value="testPointForm.automationSuggested ? '建议自动化' : '人工执行'" type="text" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>描述</label>
          <div class="rich-text-display" v-html="testPointForm.description || '暂无描述'"></div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="ghost-btn" @click="closeTestPointModal">关闭</button>
      </div>
    </div>
  </div>

  <!-- 测试点编辑模态框 -->
  <div v-if="showTestPointEditModal" class="modal-overlay" @click="closeTestPointEditModal">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>编辑测试点</h3>
        <button class="close-btn" @click="closeTestPointEditModal">×</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label>名称 <span class="required">*</span></label>
          <input v-model="testPointForm.name" type="text" class="form-control" />
        </div>
        <div class="form-group">
          <label>场景类型</label>
          <select v-model="testPointForm.sceneType" class="form-control">
            <option value="Functional">功能测试</option>
            <option value="Performance">性能测试</option>
            <option value="Security">安全测试</option>
            <option value="Compatibility">兼容性测试</option>
          </select>
        </div>
        <div class="form-group">
          <label>风险等级</label>
          <select v-model="testPointForm.riskLevel" class="form-control">
            <option value="HIGH">高</option>
            <option value="MEDIUM">中</option>
            <option value="LOW">低</option>
          </select>
        </div>
        <div class="form-group">
          <label>自动化建议</label>
          <select v-model="testPointForm.automationSuggested" class="form-control">
            <option value="true">建议自动化</option>
            <option value="false">人工执行</option>
          </select>
        </div>
        <div class="form-group">
          <label>描述</label>
          <div class="rich-text-editor">
            <div ref="editEditor" class="rich-text-content" contenteditable="true" v-html="testPointForm.description" @input="updateTestPointEditDescription"></div>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="ghost-btn" @click="closeTestPointEditModal">取消</button>
        <button class="primary-btn" @click="saveTestPoint" :disabled="savingTestPoint">
          {{ savingTestPoint ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>
  </div>

  <!-- 新增测试点模态框 -->
  <div v-if="showAddTestPointModal" class="modal-overlay" @click="closeAddTestPointModal">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>新增测试点</h3>
        <button class="close-btn" @click="closeAddTestPointModal">×</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label>测试点名称 <span class="required">*</span></label>
          <input v-model="testPointForm.name" type="text" class="form-control" />
        </div>
        <div class="form-group">
          <label>所属需求</label>
          <select v-model="testPointForm.requirementId" class="form-control">
            <option value="">请选择需求</option>
            <option v-for="requirement in requirements" :key="requirement.id" :value="requirement.id">
              {{ requirement.name }}
            </option>
          </select>
        </div>
        <div class="form-group">
          <label>场景类型</label>
          <select v-model="testPointForm.sceneType" class="form-control">
            <option value="Functional">功能测试</option>
            <option value="Performance">性能测试</option>
            <option value="Security">安全测试</option>
            <option value="Compatibility">兼容性测试</option>
          </select>
        </div>
        <div class="form-group">
          <label>风险等级</label>
          <select v-model="testPointForm.riskLevel" class="form-control">
            <option value="HIGH">高</option>
            <option value="MEDIUM">中</option>
            <option value="LOW">低</option>
          </select>
        </div>
        <div class="form-group">
          <label>自动化建议</label>
          <select v-model="testPointForm.automationSuggested" class="form-control">
            <option value="true">建议自动化</option>
            <option value="false">人工执行</option>
          </select>
        </div>
        <div class="form-group">
          <label>描述</label>
          <textarea v-model="testPointForm.description" rows="4" class="form-control"></textarea>
        </div>
      </div>
      <div class="modal-footer">
        <button class="ghost-btn" @click="closeAddTestPointModal">取消</button>
        <button class="primary-btn" @click="addTestPoint" :disabled="savingTestPoint">
          {{ savingTestPoint ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>
  </div>

  <!-- 测试用例查看模态框 -->
  <div v-if="showTestCaseModal" class="modal-overlay" @click="closeTestCaseModal">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>测试用例详情</h3>
        <button class="close-btn" @click="closeTestCaseModal">×</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label>编号</label>
          <input v-model="testCaseForm.caseNumber" type="text" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>标题 <span class="required">*</span></label>
          <input v-model="testCaseForm.title" type="text" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>所属测试点</label>
          <input :value="getTestPointName(testCaseForm.testPointId)" type="text" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>优先级</label>
          <input v-model="testCaseForm.priority" type="text" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>自动化</label>
          <input :value="testCaseForm.automation ? '是' : '否'" type="text" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>状态</label>
          <input :value="testCaseStatusLabel(testCaseForm.status)" type="text" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>前置条件</label>
          <textarea v-model="testCaseForm.precondition" rows="3" disabled class="form-control"></textarea>
        </div>
        <div class="form-group">
          <label>测试步骤</label>
          <textarea v-model="testCaseForm.steps" rows="5" disabled class="form-control"></textarea>
        </div>
        <div class="form-group">
          <label>预期结果</label>
          <textarea v-model="testCaseForm.expectedResult" rows="3" disabled class="form-control"></textarea>
        </div>
      </div>
      <div class="modal-footer">
        <button class="ghost-btn" @click="closeTestCaseModal">关闭</button>
      </div>
    </div>
  </div>

  <!-- 测试用例编辑模态框 -->
  <div v-if="showTestCaseEditModal" class="modal-overlay" @click="closeTestCaseEditModal">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>编辑测试用例</h3>
        <button class="close-btn" @click="closeTestCaseEditModal">×</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label>标题 <span class="required">*</span></label>
          <input v-model="testCaseForm.title" type="text" class="form-control" />
        </div>
        <div class="form-group">
          <label>优先级</label>
          <select v-model="testCaseForm.priority" class="form-control">
            <option value="HIGH">高</option>
            <option value="MEDIUM">中</option>
            <option value="LOW">低</option>
          </select>
        </div>
        <div class="form-group">
          <label>自动化</label>
          <select v-model="testCaseForm.automation" class="form-control">
            <option value="true">是</option>
            <option value="false">否</option>
          </select>
        </div>
        <div class="form-group">
          <label>状态</label>
          <select v-model="testCaseForm.status" class="form-control">
            <option value="PENDING_REVIEW">待审核</option>
            <option value="APPROVED">已批准</option>
            <option value="REJECTED">已拒绝</option>
          </select>
        </div>
        <div class="form-group">
          <label>前置条件</label>
          <textarea v-model="testCaseForm.precondition" rows="3" class="form-control"></textarea>
        </div>
        <div class="form-group">
          <label>测试步骤</label>
          <textarea v-model="testCaseForm.steps" rows="5" class="form-control"></textarea>
        </div>
        <div class="form-group">
          <label>预期结果</label>
          <textarea v-model="testCaseForm.expectedResult" rows="3" class="form-control"></textarea>
        </div>
      </div>
      <div class="modal-footer">
        <button class="ghost-btn" @click="closeTestCaseEditModal">取消</button>
        <button class="primary-btn" @click="saveTestCase" :disabled="savingTestCase">
          {{ savingTestCase ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>
  <!-- 生成用例附加提示词模态框 -->
  <div v-if="showCasePromptDialog" class="modal-overlay" @click="showCasePromptDialog = false">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>附加提示词 (可选)</h3>
        <button class="close-btn" @click="showCasePromptDialog = false">×</button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="submitCasePromptDialog">
          <div class="form-group">
            <label>自定义生成要求</label>
            <textarea v-model="casePromptForm.prompt" class="form-control" placeholder="例如：重点测试越权访问、只生成异常用例..." style="height: 120px; resize: vertical;"></textarea>
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button class="ghost-btn" @click="showCasePromptDialog = false">取消</button>
        <button class="primary-btn" @click="submitCasePromptDialog" :disabled="loading">
          {{ loading ? '生成中...' : '开始生成' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { api, clearStoredToken, clearStoredUser } from '../api'

export default {
  name: 'ProjectWorkbench',
  data() {
    return {
      project: null,
      testCases: [],
      testPoints: [],
      requirements: [],
      executionTasks: [],
      executionResults: [],
      loading: false,
      error: '',
      info: '',
      testCaseSearch: '',
      testCaseStatus: '',
      selectedTestCases: [],
      selectAllTestCases: false,
      selectedTestPoints: [],
      selectAllTestPoints: false,
      // 测试点相关
      showTestPointModal: false,
      showTestPointEditModal: false,
      showAddTestPointModal: false,
      savingTestPoint: false,
      testPointForm: {
        id: null,
        name: '',
        requirementId: null,
        sceneType: 'Functional',
        riskLevel: 'MEDIUM',
        automationSuggested: false,
        description: ''
      },
      // 测试用例相关
      showTestCaseModal: false,
      showTestCaseEditModal: false,
      savingTestCase: false,
      showCasePromptDialog: false,
      casePromptForm: { type: 'single', testPointId: null, prompt: '' },
      testCaseForm: {
        id: null,
        caseNumber: '',
        title: '',
        testPointId: null,
        priority: 'MEDIUM',
        automation: false,
        status: 'PENDING_REVIEW',
        precondition: '',
        steps: '',
        expectedResult: ''
      }
    }
  },
  computed: {
    filteredTestCases() {
      let filtered = [...this.testCases]
      
      if (this.testCaseSearch) {
        const query = this.testCaseSearch.toLowerCase()
        filtered = filtered.filter(testCase => 
          testCase.title.toLowerCase().includes(query) ||
          testCase.caseNumber.toLowerCase().includes(query)
        )
      }
      
      if (this.testCaseStatus) {
        filtered = filtered.filter(testCase => testCase.status === this.testCaseStatus)
      }
      
      return filtered
    }
  },
  mounted() {
    this.loadProjectData()
  },
  methods: {
    async loadProjectData() {
      this.loading = true
      this.error = ''
      try {
        const projectId = this.$route.params.id
        const [projectData, testCasesData, testPointsData, requirementsData, tasksData, resultsData] = await Promise.all([
          api.get(`/api/projects/${projectId}`),
          api.get(`/api/test-cases/project/${projectId}`),
          api.get(`/api/test-points/project/${projectId}`),
          api.get(`/api/requirements/project/${projectId}`),
          api.get(`/api/automation/tasks/project/${projectId}`),
          api.get(`/api/automation/results/project/${projectId}`)
        ])
        
        this.project = projectData
        this.testCases = testCasesData
        this.testPoints = testPointsData
        this.requirements = requirementsData
        this.executionTasks = tasksData
        this.executionResults = resultsData
      } catch (error) {
        this.error = error.message || '加载项目数据失败。'
      } finally {
        this.loading = false
      }
    },
    generateTestCases(testPointId) {
      this.casePromptForm = { type: 'single', testPointId, prompt: '' }
      this.showCasePromptDialog = true
    },
    batchGenerateTestCases() {
      if (!this.selectedTestPoints.length) return
      this.casePromptForm = { type: 'batch', testPointId: null, prompt: '' }
      this.showCasePromptDialog = true
    },
    async submitCasePromptDialog() {
      this.showCasePromptDialog = false
      if (this.casePromptForm.type === 'single') {
        await this.doGenerateTestCases(this.casePromptForm.testPointId, this.casePromptForm.prompt)
      } else {
        await this.doBatchGenerateTestCases(this.casePromptForm.prompt)
      }
    },
    async doGenerateTestCases(testPointId, prompt) {
      this.loading = true
      this.error = ''
      this.info = ''
      try {
        const queryParams = new URLSearchParams()
        queryParams.append('projectId', this.$route.params.id)
        queryParams.append('testPointId', testPointId)
        if (prompt) queryParams.append('prompt', prompt)
        
        await api.post(`/api/test-cases/generate?${queryParams.toString()}`)
        this.info = '测试用例生成成功。'
        await this.loadProjectData()
      } catch (error) {
        this.error = error.message || '生成测试用例失败。'
      } finally {
        this.loading = false
      }
    },
    async doBatchGenerateTestCases(prompt) {
      this.loading = true
      this.error = ''
      this.info = ''
      try {
        const queryParams = new URLSearchParams()
        queryParams.append('projectId', this.$route.params.id)
        this.selectedTestPoints.forEach(id => queryParams.append('testPointIds', id))
        if (prompt) queryParams.append('prompt', prompt)

        await api.post(`/api/test-cases/generate/batch?${queryParams.toString()}`)
        this.info = `已为 ${this.selectedTestPoints.length} 个测试点生成测试用例。`
        await this.loadProjectData()
      } catch (error) {
        this.error = error.message || '批量生成测试用例失败。'
      } finally {
        this.loading = false
      }
    },
    async executeTestCase(testCase) {
      this.loading = true
      this.error = ''
      this.info = ''
      try {
        const task = await api.post('/api/automation/tasks', {
          projectId: this.$route.params.id,
          testCaseId: testCase.id,
          environment: 'PRODUCTION'
        })
        await api.post(`/api/automation/tasks/${task.id}/execute`)
        this.info = '测试用例执行开始。'
        await this.loadProjectData()
      } catch (error) {
        this.error = error.message || '执行测试用例失败。'
      } finally {
        this.loading = false
      }
    },
    async batchExecuteTestCases() {
      if (!this.selectedTestCases.length) return
      
      this.loading = true
      this.error = ''
      this.info = ''
      try {
        const tasks = await api.post('/api/automation/tasks/batch', {
          projectId: this.$route.params.id,
          testCaseIds: this.selectedTestCases,
          environment: 'PRODUCTION'
        })
        
        const taskIds = tasks.map(task => task.id)
        await api.post('/api/automation/tasks/batch/execute', taskIds)
        
        this.info = `已开始执行 ${this.selectedTestCases.length} 个测试用例。`
        await this.loadProjectData()
      } catch (error) {
        this.error = error.message || '批量执行测试用例失败。'
      } finally {
        this.loading = false
      }
    },
    async retryTask(taskId) {
      this.loading = true
      this.error = ''
      this.info = ''
      try {
        await api.post(`/api/automation/tasks/${taskId}/retry`)
        this.info = '任务重试开始。'
        await this.loadProjectData()
      } catch (error) {
        this.error = error.message || '任务重试失败。'
      } finally {
        this.loading = false
      }
    },
    async viewResult(taskId) {
      this.loading = true
      this.error = ''
      try {
        const response = await api.post(`/api/automation/tasks/${taskId}/allure-report`)
        if (response.reportUrl) {
          window.open(response.reportUrl, '_blank')
        } else {
          this.error = '生成报告失败：未返回报告路径'
        }
      } catch (error) {
        this.error = error.message || '查看结果失败。'
      } finally {
        this.loading = false
      }
    },
    // 测试点相关方法
    viewTestPoint(testPoint) {
      this.testPointForm = { ...testPoint }
      this.showTestPointModal = true
    },
    closeTestPointModal() {
      this.showTestPointModal = false
      this.resetTestPointForm()
    },
    editTestPoint(testPoint) {
      this.testPointForm = { ...testPoint }
      this.showTestPointEditModal = true
    },
    closeTestPointEditModal() {
      this.showTestPointEditModal = false
      this.resetTestPointForm()
    },
    resetTestPointForm() {
      this.testPointForm = {
        id: null,
        name: '',
        requirementId: null,
        sceneType: 'Functional',
        riskLevel: 'MEDIUM',
        automationSuggested: false,
        description: ''
      }
    },
    openAddTestPointModal() {
      this.resetTestPointForm();
      this.showAddTestPointModal = true;
    },
    closeAddTestPointModal() {
      this.showAddTestPointModal = false;
      this.resetTestPointForm();
    },
    async addTestPoint() {
      if (!this.testPointForm.name) {
        this.error = '测试点名称不能为空';
        return;
      }

      this.savingTestPoint = true;
      this.error = '';
      this.info = '';

      try {
        await api.post('/api/test-points', {
          ...this.testPointForm,
          projectId: this.$route.params.id
        });
        this.info = '测试点添加成功';
        this.closeAddTestPointModal();
        await this.loadProjectData();
      } catch (error) {
        this.error = error.message || '添加测试点失败';
      } finally {
        this.savingTestPoint = false;
      }
    },
    updateTestPointEditDescription(event) {
      this.testPointForm.description = event.target.innerHTML;
    },
    async saveTestPoint() {
      if (!this.testPointForm.name) {
        this.error = '测试点名称不能为空'
        return
      }

      this.savingTestPoint = true
      this.error = ''
      this.info = ''

      try {
        await api.put(`/api/test-points/${this.testPointForm.id}`, this.testPointForm)
        this.info = '测试点保存成功'
        this.closeTestPointEditModal()
        await this.loadProjectData()
      } catch (error) {
        this.error = error.message || '保存测试点失败'
      } finally {
        this.savingTestPoint = false
      }
    },
    // 测试用例相关方法
    viewTestCase(testCase) {
      this.testCaseForm = { ...testCase }
      this.showTestCaseModal = true
    },
    closeTestCaseModal() {
      this.showTestCaseModal = false
      this.resetTestCaseForm()
    },
    editTestCase(testCase) {
      this.testCaseForm = { ...testCase }
      this.showTestCaseEditModal = true
    },
    closeTestCaseEditModal() {
      this.showTestCaseEditModal = false
      this.resetTestCaseForm()
    },
    resetTestCaseForm() {
      this.testCaseForm = {
        id: null,
        caseNumber: '',
        title: '',
        testPointId: null,
        priority: 'MEDIUM',
        automation: false,
        status: 'PENDING_REVIEW',
        precondition: '',
        steps: '',
        expectedResult: ''
      }
    },
    async saveTestCase() {
      if (!this.testCaseForm.title) {
        this.error = '测试用例标题不能为空'
        return
      }

      this.savingTestCase = true
      this.error = ''
      this.info = ''

      try {
        await api.put(`/api/test-cases/${this.testCaseForm.id}`, this.testCaseForm)
        this.info = '测试用例保存成功'
        this.closeTestCaseEditModal()
        await this.loadProjectData()
      } catch (error) {
        this.error = error.message || '保存测试用例失败'
      } finally {
        this.savingTestCase = false
      }
    },
    // 辅助方法
    getTestPointName(testPointId) {
      const testPoint = this.testPoints.find(tp => tp.id === testPointId)
      return testPoint ? testPoint.name : `测试点 ${testPointId}`
    },
    getRequirementName(requirementId) {
      if (!requirementId) return '无';
      const requirement = this.requirements.find(r => r.id === requirementId);
      return requirement ? this.truncateText(requirement.name) : `需求 ${requirementId}`;
    },
    getTestCaseCountByTestPoint(testPointId) {
      return this.testCases.filter(tc => tc.testPointId === testPointId).length
    },
    getTestCaseTitle(testCaseId) {
      const testCase = this.testCases.find(tc => tc.id === testCaseId)
      return testCase ? this.truncateText(testCase.title) : `测试用例 ${testCaseId}`
    },
    getExecutionResult(taskId) {
      return this.executionResults.find(r => r.taskId === taskId)
    },
    getTestCaseExecutionStatus(testCaseId) {
      const tasks = this.executionTasks.filter(task => task.testCaseId === testCaseId)
      if (!tasks.length) return '未执行'
      
      const latestTask = tasks.sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt))[0]
      const result = this.getExecutionResult(latestTask.id)
      
      if (!result) return '未执行'
      if (result.result === 'PASSED') return '通过'
      if (result.result === 'FAILED') return '不通过'
      return '未执行'
    },
    formatDate(dateString) {
      if (!dateString) return '-'
      return new Date(dateString).toLocaleString('zh-CN')
    },
    testCaseStatusLabel(status) {
      const statusMap = {
        PENDING_REVIEW: '待审核',
        APPROVED: '已批准',
        REJECTED: '已拒绝'
      }
      return statusMap[status] || status
    },
    clearTestCaseFilters() {
      this.testCaseSearch = ''
      this.testCaseStatus = ''
    },
    toggleSelectAllTestCases() {
      if (this.selectAllTestCases) {
        this.selectedTestCases = this.filteredTestCases.map(tc => tc.id)
      } else {
        this.selectedTestCases = []
      }
    },
    toggleSelectAllTestPoints() {
      if (this.selectAllTestPoints) {
        this.selectedTestPoints = this.testPoints.map(tp => tp.id)
      } else {
        this.selectedTestPoints = []
      }
    },
    async deleteTestCase(testCase) {
      if (!confirm('确定要删除这个测试用例吗？')) return
      
      this.loading = true
      this.error = ''
      this.info = ''
      try {
        await api.delete(`/api/test-cases/${testCase.id}`)
        this.info = '测试用例删除成功。'
        await this.loadProjectData()
      } catch (error) {
        this.error = error.message || '删除测试用例失败。'
      } finally {
        this.loading = false
      }
    },
    logout() {
      clearStoredToken()
      clearStoredUser()
      this.$router.push('/login')
    },
    // 辅助方法
    truncateText(text, maxLength = 50) {
      if (!text) return ''
      return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
    },
    getTestPointName(testPointId) {
      const testPoint = this.testPoints.find(tp => tp.id === testPointId)
      return testPoint ? this.truncateText(testPoint.name) : `测试点 ${testPointId}`
    },
    getRequirementName(requirementId) {
      if (!requirementId) return '无';
      const requirement = this.requirements.find(r => r.id === requirementId);
      return requirement ? this.truncateText(requirement.name) : `需求 ${requirementId}`;
    },
    getTestCaseCountByTestPoint(testPointId) {
      return this.testCases.filter(tc => tc.testPointId === testPointId).length
    },
    getTestCaseTitle(testCaseId) {
      const testCase = this.testCases.find(tc => tc.id === testCaseId)
      return testCase ? this.truncateText(testCase.title) : `测试用例 ${testCaseId}`
    },
    getExecutionResult(taskId) {
      return this.executionResults.find(r => r.taskId === taskId)
    },
    getTestCaseExecutionStatus(testCaseId) {
      const tasks = this.executionTasks.filter(task => task.testCaseId === testCaseId)
      if (!tasks.length) return '未执行'
      
      const latestTask = tasks.sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt))[0]
      const result = this.getExecutionResult(latestTask.id)
      
      if (!result) return '未执行'
      if (result.result === 'PASSED') return '通过'
      if (result.result === 'FAILED') return '不通过'
      return '未执行'
    },
    formatDate(dateString) {
      if (!dateString) return '-'
      return new Date(dateString).toLocaleString('zh-CN')
    },
    testCaseStatusLabel(status) {
      const statusMap = {
        PENDING_REVIEW: '待审核',
        APPROVED: '已批准',
        REJECTED: '已拒绝'
      }
      return statusMap[status] || status
    },
    clearTestCaseFilters() {
      this.testCaseSearch = ''
      this.testCaseStatus = ''
    },
    toggleSelectAllTestCases() {
      if (this.selectAllTestCases) {
        this.selectedTestCases = this.filteredTestCases.map(tc => tc.id)
      } else {
        this.selectedTestCases = []
      }
    },
    toggleSelectAllTestPoints() {
      if (this.selectAllTestPoints) {
        this.selectedTestPoints = this.testPoints.map(tp => tp.id)
      } else {
        this.selectedTestPoints = []
      }
    }
  }
}
</script>

<style scoped>
.workbench-page {
  min-height: 100vh;
  padding: 32px;
  background: #f5f7fb;
  color: #152033;
  font-family: 'Microsoft YaHei', 'Segoe UI', sans-serif;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 24px;
  box-shadow: 0 16px 36px rgba(21, 32, 51, 0.12);
  width: 600px;
  max-width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  margin: 40px auto;
}

.modal-header {
  padding: 24px 28px;
  border-bottom: 1px solid #edf1f7;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #152033;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #5f6f8b;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #f4f7fc;
  color: #152033;
}

.modal-body {
  padding: 24px 28px;
}

.modal-footer {
  padding: 20px 28px 24px;
  border-top: 1px solid #edf1f7;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #152033;
}

.required {
  color: #e63946;
}

.form-control {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #d6dce8;
  border-radius: 14px;
  font-size: 14px;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-control:focus {
  outline: none;
  border-color: #0284C7;
  box-shadow: 0 0 0 4px rgba(26, 115, 232, 0.12);
}

.form-control:disabled {
  background: #f7f9fd;
  color: #5f6f8b;
  cursor: not-allowed;
}

.form-control textarea {
  resize: vertical;
  min-height: 80px;
}

.rich-text-display {
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f7f9fd;
  min-height: 80px;
  font-size: 14px;
  line-height: 1.5;
  color: #5f6f8b;
}

.rich-text-editor {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s ease;
}

.rich-text-editor:focus-within {
  border-color: #0284C7;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.1);
}

.rich-text-content {
  min-height: 120px;
  padding: 12px 14px;
  outline: none;
  font-size: 14px;
  line-height: 1.5;
  color: #152033;
}

.rich-text-content b,
.rich-text-content strong {
  font-weight: 700;
}

.rich-text-content ul,
.rich-text-content ol {
  padding-left: 20px;
  margin: 8px 0;
}

.text-btn.small {
  padding: 8px 12px;
  font-size: 13px;
}

.ghost-btn.small {
  padding: 8px 12px;
  font-size: 13px;
}

.page-header,
.test-cases-panel,
.test-points-panel,
.execution-panel {
  background: white;
  border-radius: 24px;
  box-shadow: 0 16px 36px rgba(21, 32, 51, 0.08);
  transition: all 0.3s ease;
}

.page-header {
  padding: 28px 32px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  margin-bottom: 20px;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #0284C7;
}

.page-header h1,
.panel-head h2 {
  margin: 0;
}

.subtext,
.panel-head p {
  margin: 8px 0 0;
  color: #5f6f8b;
}

.header-actions,
.row-actions,
.batch-actions {
  display: flex;
  gap: 10px;
}

.batch-actions {
  align-items: center;
}

.workbench-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.test-cases-panel,
.test-points-panel,
.execution-panel {
  padding: 24px 28px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.panel-head-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  border: 1px solid #d6dce8;
  border-radius: 14px;
  padding: 10px 14px;
  font-size: 14px;
  min-width: 200px;
  transition: all 0.2s ease;
}

.search-input:focus {
  outline: none;
  border-color: #0284C7;
  box-shadow: 0 0 0 4px rgba(26, 115, 232, 0.12);
}

.filter-select {
  border: 1px solid #d6dce8;
  border-radius: 14px;
  padding: 10px 14px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-select:focus {
  outline: none;
  border-color: #0284C7;
  box-shadow: 0 0 0 4px rgba(26, 115, 232, 0.12);
}

.table-container {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #edf1f7;
}

.table-header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f7f9fd;
  border-bottom: 1px solid #edf1f7;
  flex-wrap: wrap;
  gap: 12px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  cursor: pointer;
  width: 16px;
  height: 16px;
}

.test-case-table,
.test-point-table,
.execution-table {
  width: 100%;
  border-collapse: collapse;
}

.test-case-table th,
.test-case-table td,
.test-point-table th,
.test-point-table td,
.execution-table th,
.execution-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #edf1f7;
  text-align: left;
  vertical-align: top;
  transition: all 0.2s ease;
}

.test-case-table th,
.test-point-table th,
.execution-table th {
  font-size: 13px;
  color: #5f6f8b;
  background: #fafbfc;
  font-weight: 600;
  position: sticky;
  top: 0;
  z-index: 10;
}

.checkbox-column {
  width: 40px;
  text-align: center;
}

.test-case-row:hover,
.test-point-row:hover,
.execution-row:hover {
  background: #f7f9fd;
}

.primary-btn,
.ghost-btn,
.text-btn,
.danger-btn {
  border-radius: 14px;
  border: none;
  padding: 12px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.primary-btn {
  color: white;
  background: linear-gradient(135deg, #0284C7, #0EA5E9);
}

.primary-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(26, 115, 232, 0.3);
}

.primary-btn:disabled,
.ghost-btn:disabled,
.danger-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.ghost-btn {
  background: #E0F2FE;
  color: #075985;
}

.ghost-btn:hover {
  background: #BAE6FD;
  transform: translateY(-1px);
}

.ghost-btn.small {
  padding: 8px 12px;
  font-size: 13px;
}

.text-btn {
  background: #E0F2FE;
  color: #0369A1;
}

.text-btn:hover {
  background: #BAE6FD;
  transform: translateY(-1px);
}

.text-btn.small {
  padding: 8px 12px;
  font-size: 13px;
}

.danger-btn {
  background: #fdecec;
  color: #b3261e;
}

.danger-btn:hover {
  background: #fcd7d7;
  transform: translateY(-1px);
}

.danger-btn.small {
  padding: 8px 12px;
  font-size: 13px;
}

.message {
  margin-top: 20px;
  padding: 12px 14px;
  border-radius: 14px;
  font-size: 14px;
  animation: slideIn 0.3s ease;
  max-width: 600px;
}

.message.error {
  background: #fdecec;
  color: #b3261e;
}

.message.info {
  background: #E0F2FE;
  color: #0369A1;
}

.status-chip,
.priority-chip,
.result-chip,
.execution-status-chip {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  transition: all 0.2s ease;
}

.status-chip.pending_review {
  background: #fff3cd;
  color: #856404;
}

.status-chip.approved {
  background: #e7f5ec;
  color: #127245;
}

.status-chip.rejected {
  background: #f8d7da;
  color: #721c24;
}

.status-chip.pending {
  background: #e2e3e5;
  color: #383d41;
}

.status-chip.running {
  background: #d1ecf1;
  color: #0c5460;
}

.status-chip.completed {
  background: #e7f5ec;
  color: #127245;
}

.status-chip.failed {
  background: #f8d7da;
  color: #721c24;
}

.priority-chip.high {
  background: #f8d7da;
  color: #721c24;
}

.priority-chip.medium {
  background: #fff3cd;
  color: #856404;
}

.priority-chip.low {
  background: #e2e3e5;
  color: #383d41;
}

.result-chip.passed,
.execution-status-chip.通过 {
  background: #e7f5ec;
  color: #127245;
}

.result-chip.failed,
.execution-status-chip.不通过 {
  background: #fdecec;
  color: #b3261e;
}

.execution-status-chip.未执行 {
  background: #f8f9fa;
  color: #6c757d;
}

.empty-state {
  padding: 28px;
  border-radius: 18px;
  background: #f7f9fd;
  color: #5f6f8b;
  text-align: center;
  animation: fadeIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@media (max-width: 1200px) {
  .panel-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .filter-bar {
    width: 100%;
  }
  
  .search-input {
    flex: 1;
  }
}

@media (max-width: 768px) {
  .workbench-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 20px 24px;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
    margin-top: 12px;
  }

  .test-cases-panel,
  .test-points-panel,
  .execution-panel {
    overflow-x: auto;
    padding: 20px 16px;
  }
  
  .table-header-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input,
  .filter-select {
    width: 100%;
  }
  
  .row-actions,
  .batch-actions {
    flex-direction: column;
    gap: 6px;
  }
  
  .primary-btn,
  .ghost-btn,
  .text-btn,
  .danger-btn {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .workbench-page {
    padding: 12px;
  }

  .page-header {
    padding: 16px 20px;
  }

  .test-cases-panel,
  .test-points-panel,
  .execution-panel {
    padding: 16px 12px;
  }

  .test-case-table th,
  .test-case-table td,
  .test-point-table th,
  .test-point-table td,
  .execution-table th,
  .execution-table td {
    padding: 10px 8px;
    font-size: 13px;
  }

  .checkbox-column {
    width: 30px;
  }
}
</style>