<template>
  <div class="login-page">
    <!-- ===== 左侧装饰区 ===== -->
    <section class="login-illustration">
      <div class="illustration-content">
        <h2 class="illustration-slogan">智能驱动测试<br/>高效赋能质量</h2>
        <p class="illustration-sub">从需求管理到接口验证，一站式测试工作台</p>
        <div class="illustration-graphic">
          <!-- 装饰性 SVG 插画 -->
          <svg viewBox="0 0 480 360" fill="none" xmlns="http://www.w3.org/2000/svg" class="hero-svg">
            <!-- 底部平台 -->
            <ellipse cx="240" cy="310" rx="180" ry="30" fill="url(#platform-grad)" opacity="0.3"/>
            <!-- 主屏幕 -->
            <rect x="120" y="60" width="240" height="160" rx="8" fill="#fff" stroke="#e0e0e0" stroke-width="1.5"/>
            <rect x="120" y="60" width="240" height="28" rx="8" fill="url(#header-grad)"/>
            <rect x="120" y="84" width="240" height="4" fill="#e8e8e8"/>
            <!-- 窗口内容模拟 -->
            <rect x="135" y="100" width="60" height="8" rx="2" fill="#E0F2FE"/>
            <rect x="135" y="116" width="90" height="6" rx="2" fill="#f0f0f0"/>
            <rect x="135" y="130" width="80" height="6" rx="2" fill="#f0f0f0"/>
            <rect x="135" y="144" width="95" height="6" rx="2" fill="#f0f0f0"/>
            <rect x="135" y="158" width="70" height="6" rx="2" fill="#f0f0f0"/>
            <rect x="280" y="100" width="65" height="50" rx="4" fill="#E0F2FE" stroke="#BAE6FD"/>
            <rect x="280" y="160" width="65" height="8" rx="2" fill="#BAE6FD"/>
            <!-- 浮动元素 - 检查图标 -->
            <g transform="translate(90, 140)">
              <circle cx="18" cy="18" r="18" fill="#0EA5E9" opacity="0.9"/>
              <polyline points="10,18 16,24 26,13" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            </g>
            <!-- 浮动元素 - 图表 -->
            <g transform="translate(340, 90)">
              <rect width="56" height="56" rx="10" fill="#fff" stroke="#e0e0e0"/>
              <rect x="10" y="30" width="8" height="16" rx="2" fill="#BAE6FD"/>
              <rect x="22" y="20" width="8" height="26" rx="2" fill="#7DD3FC"/>
              <rect x="34" y="14" width="8" height="32" rx="2" fill="#0EA5E9"/>
            </g>
            <!-- 浮动元素 - 齿轮 -->
            <g transform="translate(80, 50)">
              <circle cx="16" cy="16" r="14" fill="none" stroke="#7DD3FC" stroke-width="2" stroke-dasharray="4 3"/>
              <circle cx="16" cy="16" r="5" fill="#0EA5E9" opacity="0.6"/>
            </g>
            <!-- 连接线 -->
            <path d="M108 158 L120 158" stroke="#0EA5E9" stroke-width="1.5" stroke-dasharray="4 2" opacity="0.5"/>
            <path d="M360 118 L396 118" stroke="#0EA5E9" stroke-width="1.5" stroke-dasharray="4 2" opacity="0.5"/>
            <!-- 底部装饰点 -->
            <circle cx="160" cy="260" r="4" fill="#0EA5E9" opacity="0.2"/>
            <circle cx="200" cy="275" r="3" fill="#7DD3FC" opacity="0.3"/>
            <circle cx="310" cy="265" r="5" fill="#BAE6FD" opacity="0.3"/>
            <circle cx="280" cy="280" r="3" fill="#0EA5E9" opacity="0.15"/>
            <!-- 渐变定义 -->
            <defs>
              <linearGradient id="platform-grad" x1="60" y1="310" x2="420" y2="310">
                <stop offset="0%" stop-color="#0EA5E9" stop-opacity="0.1"/>
                <stop offset="50%" stop-color="#0EA5E9" stop-opacity="0.3"/>
                <stop offset="100%" stop-color="#0EA5E9" stop-opacity="0.1"/>
              </linearGradient>
              <linearGradient id="header-grad" x1="120" y1="60" x2="360" y2="60">
                <stop offset="0%" stop-color="#0EA5E9"/>
                <stop offset="100%" stop-color="#7DD3FC"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <!-- 底部流程提示 -->
        <div class="illustration-steps">
          <div class="step-item">
            <span class="step-num">01</span>
            <span class="step-text">上传需求</span>
          </div>
          <div class="step-arrow">→</div>
          <div class="step-item">
            <span class="step-num">02</span>
            <span class="step-text">提取测试点</span>
          </div>
          <div class="step-arrow">→</div>
          <div class="step-item">
            <span class="step-num">03</span>
            <span class="step-text">生成用例</span>
          </div>
          <div class="step-arrow">→</div>
          <div class="step-item">
            <span class="step-num">04</span>
            <span class="step-text">执行验证</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 右侧登录表单区 ===== -->
    <section class="login-form-panel">
      <div class="login-card">
        <!-- Logo -->
        <div class="login-brand">
          <div class="brand-icon">
            <span>Q</span>
          </div>
          <h1 class="brand-name">智能测试平台</h1>
          <p class="brand-sub">AI-Native Quality Console</p>
        </div>

        <!-- 表单 -->
        <div class="login-form-wrapper">
          <h2 class="form-title">账号登录</h2>

          <form class="login-form" @submit.prevent="login">
            <div class="form-field">
              <div class="field-input-wrap">
                <svg class="field-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                <input
                  id="username"
                  v-model.trim="form.username"
                  type="text"
                  autocomplete="username"
                  placeholder="请输入用户名"
                />
              </div>
            </div>

            <div class="form-field">
              <div class="field-input-wrap">
                <svg class="field-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
                <input
                  id="password"
                  v-model.trim="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="current-password"
                  placeholder="请输入密码"
                />
                <button type="button" class="toggle-password" @click="showPassword = !showPassword" tabindex="-1">
                  <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                </button>
              </div>
            </div>

            <button class="login-btn" type="submit" :disabled="submitting">
              {{ submitting ? '正在登录...' : '登 录' }}
            </button>
          </form>

          <!-- 演示账号 -->
          <div class="demo-hint">
            <span>用户名：<strong>admin</strong></span>
            <span>密码：<strong>1</strong></span>
          </div>

          <!-- 错误/成功提示 -->
          <p v-if="error" class="msg msg--error">{{ error }}</p>
          <p v-if="info" class="msg msg--success">{{ info }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { api, getStoredToken, setStoredToken, setStoredUser } from '../api'

export default {
  name: 'LoginConsole',
  data() {
    return {
      form: {
        username: 'admin',
        password: '1'
      },
      showPassword: false,
      submitting: false,
      error: '',
      info: ''
    }
  },
  mounted() {
    if (getStoredToken()) {
      this.$router.replace('/projects')
    }
  },
  methods: {
    async login() {
      this.submitting = true
      this.error = ''
      this.info = ''

      try {
        console.log('开始登录...', this.form)
        const response = await api.post('/api/auth/login', this.form, { auth: false })
        console.log('登录响应:', response)
        if (!response.success || !response.authorization) {
          throw new Error(response.message || '登录失败，请检查账号和密码。')
        }

        setStoredToken(response.authorization)
        setStoredUser(response.username || this.form.username)
        this.info = response.message || '登录成功。'
        this.$router.push('/projects')
      } catch (error) {
        console.error('登录失败:', error)
        this.error = error.message || '登录失败，请稍后重试。'
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
/* ============================================================
   登录页 - 参考图布局：左侧插画区 + 右侧登录表单
   ============================================================ */

.login-page {
  display: flex;
  min-height: 100vh;
  font-family: 'Microsoft YaHei', 'PingFang SC', -apple-system, sans-serif;
  background: #fff;
}

/* ===== 左侧插画区 ===== */
.login-illustration {
  flex: 1;
  background: linear-gradient(135deg, #F7FCFF 0%, #e0f2fe 40%, #F0F9FF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.login-illustration::before {
  content: '';
  position: absolute;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.08), transparent 70%);
  top: -100px;
  right: -100px;
}

.login-illustration::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.06), transparent 70%);
  bottom: -80px;
  left: -80px;
}

.illustration-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 520px;
}

.illustration-slogan {
  font-size: 32px;
  font-weight: 700;
  color: #0369A1;
  line-height: 1.4;
  margin: 0 0 12px;
  letter-spacing: 2px;
}

.illustration-sub {
  font-size: 15px;
  color: #38BDF8;
  margin: 0 0 36px;
}

.illustration-graphic {
  margin: 0 auto 36px;
  max-width: 420px;
}

.hero-svg {
  width: 100%;
  height: auto;
  filter: drop-shadow(0 8px 24px rgba(14, 165, 233, 0.1));
  animation: float 6s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

/* 底部流程步骤 */
.illustration-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid #BAE6FD;
  border-radius: 20px;
  transition: all 0.3s;
}

.step-item:hover {
  background: #fff;
  border-color: #0EA5E9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.15);
}

.step-num {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: #0EA5E9;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-text {
  font-size: 12px;
  color: #555;
  font-weight: 500;
}

.step-arrow {
  color: #BAE6FD;
  font-size: 16px;
  font-weight: 300;
}

/* ===== 右侧登录表单区 ===== */
.login-form-panel {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  flex-shrink: 0;
  background: #fff;
}

.login-card {
  width: 100%;
  max-width: 360px;
}

/* Logo 区域 */
.login-brand {
  text-align: center;
  margin-bottom: 40px;
}

.brand-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  background: linear-gradient(135deg, #0EA5E9, #0369A1);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.3);
}

.brand-icon span {
  color: #fff;
  font-size: 26px;
  font-weight: 700;
}

.brand-name {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin: 0 0 6px;
}

.brand-sub {
  font-size: 13px;
  color: #0EA5E9;
  margin: 0;
  letter-spacing: 1px;
}

/* 表单区域 */
.login-form-wrapper {
  width: 100%;
}

.form-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 24px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-field {
  width: 100%;
}

.field-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.field-icon {
  position: absolute;
  left: 14px;
  width: 18px;
  height: 18px;
  color: #bbb;
  pointer-events: none;
  transition: color 0.2s;
}

.field-input-wrap input {
  width: 100%;
  height: 46px;
  padding: 0 44px 0 42px;
  font-size: 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  outline: none;
  color: #333;
  background: #fafafa;
  transition: all 0.25s;
  font-family: inherit;
}

.field-input-wrap input::placeholder {
  color: #bbb;
}

.field-input-wrap input:hover {
  border-color: #a5d6a7;
}

.field-input-wrap input:focus {
  border-color: #0EA5E9;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.1);
}

.field-input-wrap input:focus ~ .field-icon,
.field-input-wrap:focus-within .field-icon {
  color: #0EA5E9;
}

.toggle-password {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  color: #bbb;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.toggle-password:hover {
  color: #0EA5E9;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(135deg, #0EA5E9, #0284C7);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 6px;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  transition: left 0.5s;
}

.login-btn:hover::before {
  left: 100%;
}

.login-btn:hover {
  background: linear-gradient(135deg, #0284C7, #0369A1);
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.35);
  transform: translateY(-1px);
}

.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.3);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 演示账号提示 */
.demo-hint {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 24px;
  font-size: 13px;
  color: #999;
}

.demo-hint strong {
  color: #666;
}

/* 消息提示 */
.msg {
  margin: 16px 0 0;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 13px;
  text-align: center;
}

.msg--error {
  background: #fef0f0;
  color: #f44336;
  border: 1px solid #fde2e2;
}

.msg--success {
  background: #E0F2FE;
  color: #0369A1;
  border: 1px solid #BAE6FD;
}

/* ===== 响应式 ===== */
@media (max-width: 960px) {
  .login-page {
    flex-direction: column;
  }

  .login-illustration {
    min-height: 300px;
    padding: 30px;
  }

  .illustration-slogan {
    font-size: 24px;
  }

  .illustration-graphic {
    max-width: 320px;
    margin-bottom: 20px;
  }

  .illustration-steps {
    flex-wrap: wrap;
    gap: 6px;
  }

  .step-arrow {
    display: none;
  }

  .login-form-panel {
    width: 100%;
    padding: 30px 24px;
  }
}

@media (max-width: 640px) {
  .login-illustration {
    min-height: 200px;
    padding: 24px;
  }

  .illustration-slogan {
    font-size: 20px;
  }

  .illustration-sub {
    font-size: 13px;
    margin-bottom: 20px;
  }

  .illustration-graphic {
    max-width: 260px;
  }

  .login-card {
    max-width: none;
  }

  .brand-name {
    font-size: 20px;
  }

  .login-brand {
    margin-bottom: 28px;
  }
}
</style>
