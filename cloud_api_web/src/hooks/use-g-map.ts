import AMapLoader from '@amap/amap-jsapi-loader'
import { App, reactive } from 'vue'
import { CURRENT_CONFIG as config } from '/@/api/http/config'
import { AMapConfig } from '/@/constants/index'
import { gcj02towgs84, wgs84togcj02 } from '/@/vendors/coordtransform'
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

  const fanData = config.markerList
  async function initMap (container: string, app:App, showOrigin = false) {
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
    if (config.isAllowScaling) {
      state.map = new AMap.Map(container, {
        center: config.droneLocation,
        zoom: config.initMapLevel,
        layers: [customLayer],
        animateEnable: false, // 关闭动画，加快初始加载
      })
    } else {
      state.map = new AMap.Map(container, {
        center: config.droneLocation,
        zoom: config.initMapLevel,
        layers: [customLayer],
        animateEnable: false, // 关闭动画，加快初始加载
        doubleClickZoom: false, // 初始时禁用双击缩放
        dragEnable: true, // 初始时禁用拖拽
        zoomEnable: false, // 初始时禁用缩放
      })
    }

    // droneLocation is already GCJ-02, matching the initial center. Do not convert twice.
    const origin = config.droneLocation
    if (showOrigin && Array.isArray(origin) && origin.length === 2 &&
      origin.every(value => typeof value === 'number' && Number.isFinite(value)) &&
      Math.abs(origin[0]) <= 180 && Math.abs(origin[1]) <= 90) {
      state.map.add(new AMap.Marker({
        position: origin,
        offset: new AMap.Pixel(-16, -16),
        zIndex: 110,
        bubble: true,
        draggable: false,
        title: `地图初始中心（GCJ-02）：${origin[0].toFixed(6)}, ${origin[1].toFixed(6)}`,
        content: `<div style="position:relative;width:32px;height:32px;pointer-events:none;">
          <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32" aria-hidden="true">
            <circle cx="16" cy="16" r="10" fill="#071e36" fill-opacity="0.75" stroke="#ffd166" stroke-width="2"/>
            <path d="M16 1v9M16 22v9M1 16h9M22 16h9" stroke="#ffd166" stroke-width="2"/>
            <circle cx="16" cy="16" r="3" fill="#ffd166"/>
          </svg>
          <span style="position:absolute;left:16px;bottom:38px;transform:translateX(-50%);white-space:nowrap;padding:5px 10px;border:1px solid #ffd166;border-radius:4px;background:rgba(7,30,54,0.94);color:#ffe3a1;font-size:13px;font-weight:600;box-shadow:0 2px 8px #0008;">地图原点</span>
        </div>`
      }))
    }

    const markerIcon = new AMap.Icon({
      image: fanIcon, // 图标图片
      size: new AMap.Size(72, 96), // 图标显示尺寸
      imageSize: new AMap.Size(72, 96), // 图片实际尺寸
      imageOffset: new AMap.Pixel(0, 0), // 图片内部偏移
    })

    // 存储所有标记的数组
    const markers = []
    // 遍历数据创建标记
    fanData.forEach(([lng, lat, name]) => {
      const marker = new AMap.Marker({
        position: wgs84togcj02(lng, lat),
        icon: markerIcon,
        offset: new AMap.Pixel(-36, -96),
        label: {
          content: `<div style="
          background: transparent;
          color: white;
          font-size: 14px;
          font-weight: bold;
          text-shadow: 1px 1px 3px rgba(0,0,0,0.8), -1px -1px 3px rgba(0,0,0,0.8), 1px -1px 3px rgba(0,0,0,0.8), -1px 1px 3px rgba(0,0,0,0.8);
          padding: 2px 8px;
          white-space: nowrap;
        ">${name}</div>`,
          direction: 'bottom',
          offset: new AMap.Pixel(0, 5),
        },
      })
      // 添加到地图
      state.map.add(marker)
      // 存储到数组
      markers.push(marker)
    })
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

  function globalPropertiesConfig (app: App, showOrigin = false) {
    initMap('g-container', app, showOrigin)
  }

  return {
    globalPropertiesConfig,
  }
}
