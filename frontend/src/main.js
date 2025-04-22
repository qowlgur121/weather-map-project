import { createApp } from 'vue'
import { createPinia } from 'pinia' // Pinia 임포트
import App from './App.vue'

// Vue 앱 인스턴스 생성
const app = createApp(App)

// Pinia 플러그인 등록
app.use(createPinia())

// 앱 마운트
app.mount('#app')