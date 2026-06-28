import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import '@/access.ts'
import VueCropper from 'vue-cropper';
import 'vue-cropper/dist/index.css'
import ECharts from 'vue-echarts'
import 'echarts'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)
app.use(VueCropper)
app.component('v-chart', ECharts)

app.mount('#app')
