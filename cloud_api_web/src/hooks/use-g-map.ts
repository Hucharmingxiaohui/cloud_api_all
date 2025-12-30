import AMapLoader from '@amap/amap-jsapi-loader'
import { App, reactive } from 'vue'
import { AMapConfig } from '/@/constants/index'
// import '/@/amap/AMap3.js'
import fanIcon from '/@/assets/v4/fan.gif'
declare const AMap: any

export function useGMapManage () {
  const state = reactive({
    aMap: null, // Map类
    map: null, // 地图对象
    mouseTool: null,
  })
  // async function initMap (container: string, app:App) {
  //   state.aMap = window.AMap
  // }
  async function initMap (container: string, app:App) {
    // 清理现有的地图实例和相关资源
    if (state.map) {
      state.map.destroy() // 销毁旧的地图实例
      state.map = null // 清空旧的地图对象
    }

    // 清除其他与地图相关的状态
    if (state.mouseTool) {
      state.mouseTool.close() // 关闭旧的 MouseTool 工具
      state.mouseTool = null // 清空鼠标工具对象
    }
    // 初始化
    state.aMap = AMap
    const customLayer = new AMap.TileLayer({
      getTileUrl: function (x, y, z) {
        return `/api/${z}/${x}/${y}.png`
      },
      opacity: 1,
      zIndex: 99,
    })
    state.map = new AMap.Map(container, {
      center: [109.026590883032, 22.702118868888],
      zoom: 14,
      layers: [customLayer],
      animateEnable: false, // 关闭动画，加快初始加载
      doubleClickZoom: false, // 初始时禁用双击缩放
      dragEnable: true, // 初始时禁用拖拽
      zoomEnable: false, // 初始时禁用缩放
    })

    const marker = new AMap.Marker({
      position: [109.02655, 22.701153], // 位置
      icon: fanIcon,
      offset: [0, -20],
    })
    state.map.add(marker) // 添加到地图

    state.mouseTool = new AMap.MouseTool(state.map)

    // 挂在到全局
    app.config.globalProperties.$aMap = state.aMap
    app.config.globalProperties.$map = state.map
    app.config.globalProperties.$mouseTool = state.mouseTool
  }
  // async function initMap (container: string, app: App) {
  //   AMapLoader.load({
  //     ...AMapConfig
  //   }).then((AMap) => {
  //     state.aMap = AMap
  //     state.map = new AMap.Map(container, {
  //       center: [121.3574, 37.5419],
  //       zoom: 20
  //     })
  //     state.mouseTool = new AMap.MouseTool(state.map)

  //     const marker = new AMap.Marker({
  //       position: [121.3588, 37.5179],
  //       icon: fanIcon,
  //       offset: [0, -20],
  //     })
  //     state.map.add(marker)

  //     // 挂在到全局
  //     app.config.globalProperties.$aMap = state.aMap
  //     app.config.globalProperties.$map = state.map
  //     app.config.globalProperties.$mouseTool = state.mouseTool
  //   }).catch(e => {
  //     console.log(e)
  //   })
  // }

  function globalPropertiesConfig (app: App) {
    initMap('g-container', app)
  }

  return {
    globalPropertiesConfig,
  }
}
