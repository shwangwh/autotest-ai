<template>
  <div class="login-page">
    <section class="hero-panel">
      <div class="logo-section">
        <div class="logo-text">因为热爱 快速成长</div>
      </div>
      <img src="../assets/image.png" alt="MeterSphere" class="hero-image">
      <div class="hero-slogan">开源持续测试平台</div>
    </section>

    <section class="form-panel">
      <div class="form-shell">
        <div class="form-header">
          <div class="form-logo">MS</div>
          <h2>MeterSphere</h2>
          <p>开源持续测试工具</p>
        </div>

        <form class="login-form" @submit.prevent="login">
          <div class="form-group">
            <label for="username">账号</label>
            <input v-model.trim="form.username" type="text" id="username" placeholder="请输入账号" autocomplete="username" />
          </div>

          <div class="form-group">
            <label for="password">密码</label>
            <input v-model.trim="form.password" type="password" id="password" placeholder="请输入密码" autocomplete="current-password" />
          </div>

          <button class="login-btn" type="submit" :disabled="submitting">
            {{ submitting ? '登录中...' : '登录' }}
          </button>

          <div class="demo-info">
            用户名: <strong>{{ form.username }}</strong> 密码: <strong>{{ form.password }}</strong>
          </div>

          <p v-if="error" class="message error">{{ error }}</p>
          <p v-if="info" class="message info">{{ info }}</p>
        </form>
      </div>
    </section>
  </div>
</template>

<script>
import { api, getStoredToken, setStoredToken, setStoredUser } from '../api'

export default {
  name: 'Login',
  data() {
    return {
      form: {
        username: 'admin',
        password: '1'
      },
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
        const response = await api.post('/api/auth/login', this.form, { auth: false })
        if (!response.success || !response.authorization) {
          throw new Error(response.message || '登录失败，请检查账号和密码。')
        }

        setStoredToken(response.authorization)
        setStoredUser(response.username || this.form.username)
        this.info = response.message || '登录成功。'
        this.$router.push('/projects')
      } catch (error) {
        this.error = error.message || '登录失败，请稍后重试。'
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  font-family: 'Microsoft YaHei', 'Segoe UI', sans-serif;
  overflow: hidden;
}

.hero-panel {
  background: linear-gradient(135deg, #0284C7 0%, #38BDF8 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  overflow: hidden;
}

.hero-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.logo-section {
  text-align: center;
  z-index: 1;
}

.logo-text {
  font-size: 24px;
  font-weight: bold;
  color: white;
  margin-bottom: 20px;
}

.hero-image {
  width: 100%;
  max-width: 400px;
  margin: 40px 0;
  z-index: 1;
}

.hero-slogan {
  font-size: 20px;
  color: white;
  text-align: center;
  z-index: 1;
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: white;
}

.form-shell {
  width: 100%;
  max-width: 400px;
}

.form-header {
  text-align: center;
  margin-bottom: 40px;
}

.form-logo {
  width: 60px;
  height: 60px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #0284C7 0%, #38BDF8 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: bold;
}

.form-header h2 {
  margin: 8px 0 10px;
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.form-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 16px;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #0284C7;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.1);
}

.login-btn {
  width: 100%;
  border: none;
  border-radius: 8px;
  padding: 14px 20px;
  font-size: 16px;
  font-weight: bold;
  color: white;
  background: linear-gradient(135deg, #0284C7 0%, #38BDF8 100%);
  cursor: pointer;
  transition: all 0.2s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.demo-info {
  margin-top: 20px;
  padding: 12px;
  background: #f7fafc;
  border-radius: 8px;
  font-size: 14px;
  color: #666;
  text-align: center;
}

.demo-info strong {
  color: #0284C7;
}

.message {
  margin: 14px 0 0;
  padding: 12px 14px;
  border-radius: 8px;
  font-size: 14px;
}

.message.error {
  background: #fdecec;
  color: #b3261e;
}

.message.info {
  background: #f0f7ff;
  color: #0284C7;
}

@media (max-width: 768px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    padding: 30px 20px;
  }

  .form-panel {
    padding: 30px 20px;
  }

  .hero-image {
    max-width: 250px;
    margin: 20px 0;
  }
}
</style>
