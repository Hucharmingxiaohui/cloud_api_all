import AMapLoader from '@amap/amap-jsapi-loader'
import { App, reactive } from 'vue'
import { AMapConfig } from '/@/constants/index'
export function useGMapManage () {
  const state = reactive({
    aMap: null, // Map类
    map: null, // 地图对象
    mouseTool: null,
  })
  // async function initMap (container: string, app:App) {
  //   state.aMap = window.AMap
  // }
  // async function initMap (container: string, app:App) {
  //   // 清理现有的地图实例和相关资源
  //   if (state.map) {
  //     state.map.destroy() // 销毁旧的地图实例
  //     state.map = null // 清空旧的地图对象
  //   }

  //   // 清除其他与地图相关的状态
  //   if (state.mouseTool) {
  //     state.mouseTool.close() // 关闭旧的 MouseTool 工具
  //     state.mouseTool = null // 清空鼠标工具对象
  //   }
  //   // 初始化
  //   state.aMap = AMap
  //   const customLayer = new AMap.TileLayer({
  //     getTileUrl: function (x, y, z) {
  //       return `/api/map1/${z}/${x}/${y}.png`
  //     },
  //     opacity: 1,
  //     zIndex: 99,
  //   })
  //   state.map = new AMap.Map(container, {
  //     center: [121.3574, 37.5419],
  //     zoom: 16,
  //     layers: [customLayer]
  //   })

  // const marker = new AMap.Marker({
  //   position: [121.3574, 37.5419], //位置
  // })
  // state.map.add(marker) //添加到地图

  //   state.mouseTool = new AMap.MouseTool(state.map)

  //   // 挂在到全局
  //   app.config.globalProperties.$aMap = state.aMap
  //   app.config.globalProperties.$map = state.map
  //   app.config.globalProperties.$mouseTool = state.mouseTool
  // }
  async function initMap (container: string, app: App) {
    AMapLoader.load({
      ...AMapConfig
    }).then((AMap) => {
      state.aMap = AMap
      state.map = new AMap.Map(container, {
        center: [121.3574, 37.5419],
        zoom: 20
      })
      state.mouseTool = new AMap.MouseTool(state.map)

      // const marker = new AMap.Marker({
      //   position: [121.3588, 37.5179]
      // })
      // state.map.add(marker)

      // 挂在到全局
      app.config.globalProperties.$aMap = state.aMap
      app.config.globalProperties.$map = state.map
      app.config.globalProperties.$mouseTool = state.mouseTool
    }).catch(e => {
      console.log(e)
    })
  }

  function globalPropertiesConfig (app: App) {
    initMap('g-container', app)
  }

  return {
    globalPropertiesConfig,
  }
}
