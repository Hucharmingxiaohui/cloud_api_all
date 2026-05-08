<template>
  <div class="demo-app">
    <router-view />
    <Loading :isLoading="loadState" />
    <!-- <div class="map-wrapper">
      <GMap/>
    </div> -->
  </div>
</template>

<script lang="ts">
import { computed, defineComponent, onErrorCaptured } from 'vue'
import { useMyStore } from './store'
import GMap from '/@/components/GMap.vue'
import map from '/@/components/g-map/mapPanel.vue'
// 默认语言为 en-US，如果你需要设置其他语言，推荐在入口文件全局设置 locale
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { isLoading } from './router/index'
dayjs.locale('zh-cn')

export default defineComponent({
  name: 'App',
  components: { GMap, map },

  setup () {
    const loadState = computed(() => isLoading.value)
    const store = useMyStore()
    const locale = zhCN

    // 错误边界：捕获子组件错误，防止向上传播导致页面空白
    onErrorCaptured((err, instance, info) => {
      console.error('App Error Captured:', err)
      console.error('Component:', instance)
      console.error('Info:', info)
      // 返回 false 阻止错误继续向上传播
      return false
    })

    return { locale, loadState }
  }
})
</script>
<style lang="scss" scoped>
.demo-app {
  width: 100%;
  height: 100%;

  .map-wrapper {
    height: 100%;
    width: 100%;
  }
}
</style>

<style lang="scss">
#demo-app {
  width: 100%;
  height: 100%
}
</style>
