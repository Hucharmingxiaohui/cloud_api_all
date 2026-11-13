const CURRENT_CONFIG = {
  // license
  baseURL: 'http://172.20.63.157:6789/',
  kmzURL: 'http://172.20.63.157:6789/',
  websocketURL: 'ws://172.20.63.157:6789/api/v1/ws',
  // baseURL: 'http://127.0.0.1:6789/',
  // kmzURL: 'http://127.0.0.1:6789/',
  // websocketURL: 'ws://127.0.0.1:6789/api/v1/ws',
  rtmpURL: 'rtmp://172.20.63.157:2035/live/', // 视频请求推流地址
  rtcURL: 'http://172.20.63.157:2085/rtc/v1/whip/?app=live&stream=', // 视频请求推流地址
  flvURL: 'http://172.20.63.157:9080/live/',  // 视频流地址
  UEPixURL: 'ws://172.20.63.106:901',
  cloudRenderer: {
    enabled: true,
    baseURL: 'http://127.0.0.1:3000',
    renderer: 'outdoor',
    pointCloudFile: '3dgs/7.1.1.ply',
    rendererParams: {
      alignSplatFile: '3dgs/7.1.1.ply',
      alignReferenceTileset: '3dgs/dfelanqiuchang/tileset.json'
    },
    iceServers: [
      {
        urls: 'turn:172.20.13.53:3478?transport=udp',
        username: 'cloudrender',
        credential: 'CloudRender@123456'
      }
    ]
  },

  // 风机坐标 用于地图初始显示风机marker,不需要时置空(高德地图坐标)
  // 由于遥控器获取的是WGS84坐标，而不是gcj02坐标。此处暂时放置WGS84坐标，代码中进行WGS84=>gcj02转换
  markerList: [],

  // 机场坐标,用于初始定位地图中心(高德 GCJ-02 坐标，经度在前)
  // 现场机巢 OSD WGS84: [103.54643128618687, 24.75737802924538]，已按平台转换函数转换
  droneLocation: [103.5482592735524, 24.754279202270375],

  // 系统名称
  sysName: '东方电子无人机管控平台',
  // 工地巡检菜单模式：true 隐藏风机/光伏管理、点位管理、EUA设备监控及风机/光伏/EUA计划
  // 仅控制菜单展示，不删除历史数据；修改后刷新浏览器生效
  constructionOnly: false,
  // 地图初始级别
  initMapLevel: 18,
  // 地图是否允许放缩
  isAllowScaling: true,

  // 此处不用动
  amapKey: '617568068ecaef890bd0949fb2431c54',
  rtspPort: '8554',
  agoraAPPID: 'd976e18cfac145459627088a800fc301',
  agoraToken: '007eJxTYNi19Ouhaymc2S/XT32XvLXG/q+uXad12RXZ6CVGRhcPc/IoMKRYmpulGlokpyUmG5qYmphamhmZG1hYJFoYGKQlGxsYrnYUSmsIZGSoiaxjYIRCEJ+VITs7Py+VgQEAn/4fRA==',
  agoraChannel: 'kkone',
  appId: '143742',
  appKey: 'a3c1804b348e340ff0b133e3466350f',
  appLicense: 'wlKjGJuazaLbacm0Yf+HDAPhBredlzGoWJeChjaXXeWsAeI3WlYcutkRlFbvJIPfR1bNCohPTQlaXFp39QRCDomq6OYQXrigBUeEH+drJztq9xB0CZHwNTok/o1TtOI+z61uvkgkePBEb6yaRJVM7rOLk8T7vTACw4bJ4fZrSzA=',
}

window.CURRENT_CONFIG = CURRENT_CONFIG;
