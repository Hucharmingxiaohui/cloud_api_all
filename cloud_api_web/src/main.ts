import { createApp } from 'vue'
import App from './App.vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/antd.css'
import router from './router'
import { antComponents } from './antd'
import { CommonComponents } from './use-common-components'
import 'virtual:svg-icons-register'
import store, { storeKey } from './store'
import { createInstance } from '/@/root'
import { useDirectives } from './directives'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// Element Plus 语言包
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'dayjs/locale/zh-cn'
import '/@/styles/index.scss'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import Loading from '/@/components/common/loading.vue'
// iconfont
import '/@/assets/iconfonts/iconfont.css'

// font
import '/@/assets/font/fonts.css'

const app = createInstance(App)
app.use(Antd)
app.component('Loading', Loading)
app.mount('#app')
// 统一注册Icon图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.use(store, storeKey)
app.use(router)
app.use(CommonComponents)
app.use(antComponents)
app.use(useDirectives)
app.use(ElementPlus, { locale: zhCn, })
app.mount('#demo-app')
