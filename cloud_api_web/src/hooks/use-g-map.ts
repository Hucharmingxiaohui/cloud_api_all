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
  // 广西风电坐标
  // const fanData = [[109.010635, 22.70582, '6号风机'],
  //   [109.026554821, 22.701189467, '7号风机'],
  //   [109.036217569, 22.694190623, '8号风机'],
  //   [109.027966891, 22.694944173, '9号风机'],
  //   [109.032494791, 22.689203567, '10号风机'],
  // ]
  // 机场坐标 109.026590883032, 22.702118868888
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
      center: [114.517241728, 37.947927613],
      zoom: 15,
      layers: [customLayer],
      animateEnable: false, // 关闭动画，加快初始加载
      doubleClickZoom: false, // 初始时禁用双击缩放
      dragEnable: true, // 初始时禁用拖拽
      zoomEnable: false, // 初始时禁用缩放
    })

    // const markerIcon = new AMap.Icon({
    //   image: fanIcon, // 图标图片
    //   size: new AMap.Size(72, 96), // 图标显示尺寸
    //   imageSize: new AMap.Size(96, 128), // 图片实际尺寸
    //   imageOffset: new AMap.Pixel(0, 0), // 图片内部偏移
    // })

    // // 存储所有标记的数组
    // const markers = []
    // // 遍历数据创建标记
    // fanData.forEach(([lng, lat, name]) => {
    //   const marker = new AMap.Marker({
    //     position: [lng, lat],
    //     icon: markerIcon,
    //     offset: new AMap.Pixel(0, -20),
    //     label: {
    //       content: `<div style="
    //       background: transparent;
    //       color: white;
    //       font-size: 14px;
    //       font-weight: bold;
    //       text-shadow: 1px 1px 3px rgba(0,0,0,0.8), -1px -1px 3px rgba(0,0,0,0.8), 1px -1px 3px rgba(0,0,0,0.8), -1px 1px 3px rgba(0,0,0,0.8);
    //       padding: 2px 8px;
    //       white-space: nowrap;
    //     ">${name}</div>`,
    //       direction: 'bottom',
    //       offset: new AMap.Pixel(0, 5),
    //     },
    //   })
    //   // 添加到地图
    //   state.map.add(marker)
    //   // 存储到数组
    //   markers.push(marker)
    // })

    // const marker = new AMap.Marker({
    //   position: [109.02655, 22.701153], // 位置
    //   icon: markerIcon,
    //   offset: new AMap.Pixel(0, -20),
    //   label: {
    //     content: '<div style="' +
    //     'background: transparent;' +
    //     'color: white;' +
    //     'font-size: 16px;' +
    //     'font-weight: bold;' +
    //     'text-shadow: 1px 1px 3px rgba(0,0,0,0.8), -1px -1px 3px rgba(0,0,0,0.8), 1px -1px 3px rgba(0,0,0,0.8), -1px 1px 3px rgba(0,0,0,0.8);' +
    //     'padding: 2px 8px;' +
    //     'white-space: nowrap;' +
    //   '">6号风机</div>',
    //     direction: 'bottom',
    //     offset: new AMap.Pixel(0, 5), // 向上偏移
    //   }
    // })
    // state.map.add(marker) // 添加到地图

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
