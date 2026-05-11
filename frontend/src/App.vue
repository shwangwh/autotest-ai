<template>
  <div class="app-shell">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
:root {
  color-scheme: light;
  --bg-base: #eef8ff;
  --bg-strong: #0b3b5a;
  --surface: rgba(255, 255, 255, 0.82);
  --surface-strong: #f8fcff;
  --surface-dark: rgba(3, 105, 161, 0.84);
  --line-soft: rgba(14, 165, 233, 0.14);
  --line-strong: rgba(14, 165, 233, 0.28);
  --text-main: #0f2738;
  --text-soft: #557086;
  --text-inverse: #f7fbff;
  --accent: #0ea5e9;
  --accent-strong: #0369a1;
  --accent-soft: #bae6fd;
  --success: #1e7a52;
  --danger: #bf4d3f;
  --shadow-soft: 0 18px 40px rgba(18, 34, 43, 0.08);
  --shadow-strong: 0 24px 80px rgba(18, 34, 43, 0.18);
  --radius-xl: 28px;
  --radius-lg: 20px;
  --radius-md: 14px;
  --font-display: "STZhongsong", "Palatino Linotype", "Times New Roman", serif;
  --font-body: "Microsoft YaHei UI", "PingFang SC", "Segoe UI", sans-serif;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.app-shell {
  font-family: var(--font-body);
  min-height: 100vh;
  background: transparent;
  color: var(--text-main);
}

body {
  min-width: 320px;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.20), transparent 28%),
    radial-gradient(circle at bottom right, rgba(3, 105, 161, 0.12), transparent 34%),
    linear-gradient(145deg, #f7fcff 0%, #e0f2fe 45%, #f0f9ff 100%);
  color: var(--text-main);
  font-family: var(--font-body);
}

button,
input,
select,
textarea {
  font: inherit;
}

/* 页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 全局动画 */
@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

/* 通用类 */
.animate-slide-in-up {
  animation: slideInUp 0.5s ease-out;
}

.animate-slide-in-right {
  animation: slideInRight 0.5s ease-out;
}

.animate-pulse {
  animation: pulse 1.5s infinite;
}

/* 平滑滚动 */
html {
  scroll-behavior: smooth;
}
</style>
