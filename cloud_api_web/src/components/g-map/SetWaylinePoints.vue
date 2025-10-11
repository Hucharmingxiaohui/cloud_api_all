<template>
    <div class="g-map-wrapper">
      <!-- 地图区域 -->
      <div id="g-container" :style="{ width: '100%', height: '100%' }" />
      <!-- 绘制面板 -->
      <div
        class="g-action-panel"
        :style="{ right: drawVisible ? '366px' : '16px' }"
      >
        <div :class="state.currentType === 'pin' ? 'g-action-item selection' : 'g-action-item'" @click="draw('pin', true)">
          <a><a-image :src="pin" :preview="false" /></a>
        </div>
        <div v-if="mouseMode" class="g-action-item" @click="draw('off', false)">
          <a style="color: red;"><CloseOutlined /></a>
        </div>
      </div>
    </div>
  </template>

<script lang="ts">
import { computed, defineComponent, onMounted, reactive, ref, watch, defineExpose } from 'vue'
import {
  generateLineContent,
  generatePointContent,
  generatePolyContent
} from '/@/utils/map-layer-utils'
import { postElementsReq } from '/@/api/layer'
import { MapDoodleType, MapElementEnum } from '/@/constants/map'
import { useGMapManage } from '/@/hooks/use-g-map'
import { useGMapCover } from '/@/hooks/use-g-map-cover'
import { useMouseTool } from '/@/hooks/use-mouse-tool'
import { getApp, getRoot } from '/@/root'
import { useMyStore } from '/@/store'
import { GeojsonCoordinate } from '/@/types/map'
import { MapDoodleEnum } from '/@/types/map-enum'
import { PostElementsBody } from '/@/types/mapLayer'
import { uuidv4 } from '/@/utils/uuid'
import { gcj02towgs84, wgs84togcj02 } from '/@/vendors/coordtransform'
import { deviceTsaUpdate } from '/@/hooks/use-g-map-tsa'
import {
  DeviceOsd, DeviceStatus, DockOsd, EGear, EModeCode, GatewayOsd, EDockModeCode,
  NetworkStateQualityEnum, NetworkStateTypeEnum, RainfallEnum, DroneInDockEnum
} from '/@/types/device'
import pin from '/@/assets/icons/pin-2d8cf0.svg'
import M30 from '/@/assets/icons/m30.png'
import {
  BorderOutlined, LineOutlined, CloseOutlined, ControlOutlined, TrademarkOutlined, ArrowDownOutlined,
  ThunderboltOutlined, SignalFilled, GlobalOutlined, HistoryOutlined, CloudUploadOutlined, RocketOutlined,
  FieldTimeOutlined, CloudOutlined, CloudFilled, FolderOpenOutlined, RobotFilled, ArrowUpOutlined, CarryOutOutlined
} from '@ant-design/icons-vue'
import { EDeviceTypeName } from '/@/types'
import DockControlPanel from '/@/components/g-map/DockControlPanel.vue'
import { useDockControl } from '/@/components/g-map/use-dock-control'
import DroneControlPanel from '/@/components/g-map/DroneControlPanel.vue'
import { useConnectMqtt } from '/@/components/g-map/use-connect-mqtt'
import FlightAreaActionIcon from '/@/components/flight-area/FlightAreaActionIcon.vue'
import { EFlightAreaType } from '/@/types/flight-area'
import { useFlightArea } from '/@/components/flight-area/use-flight-area'
import { useFlightAreaDroneLocationEvent } from '/@/components/flight-area/use-flight-area-drone-location-event'
import IconPoint from '/@/assets/icons/poi-marker-default.png'
export default defineComponent({
  components: {
    BorderOutlined,
    LineOutlined,
    CloseOutlined,
    ControlOutlined,
    TrademarkOutlined,
    ThunderboltOutlined,
    SignalFilled,
    GlobalOutlined,
    HistoryOutlined,
    CloudUploadOutlined,
    FieldTimeOutlined,
    CloudOutlined,
    CloudFilled,
    FolderOpenOutlined,
    RobotFilled,
    ArrowUpOutlined,
    ArrowDownOutlined,
    DockControlPanel,
    DroneControlPanel,
    CarryOutOutlined,
    RocketOutlined,
    FlightAreaActionIcon,
  },
  name: 'GMap',
  props: {},
  setup () {
    const useMouseToolHook = useMouseTool()
    const useGMapManageHook = useGMapManage()
    const deviceTsaUpdateHook = deviceTsaUpdate()
    const root = getRoot()

    const mouseMode = ref(false)
    const store = useMyStore()
    let useGMapCoverHook = useGMapCover(store)
    const state = reactive({
      currentType: '',
      coverIndex: 0,
      isFlightArea: false,
    })
    const str: string = '--'
    const deviceInfo = reactive({
      gateway: {
        capacity_percent: str,
        transmission_signal_quality: str,
      } as GatewayOsd,
      dock: {

      } as DockOsd,
      device: {
        gear: -1,
        mode_code: EModeCode.Disconnected,
        height: str,
        home_distance: str,
        horizontal_speed: str,
        vertical_speed: str,
        wind_speed: str,
        wind_direction: str,
        elevation: str,
        position_state: {
          gps_number: str,
          is_fixed: 0,
          rtk_number: str
        },
        battery: {
          capacity_percent: str,
          landing_power: str,
          remain_flight_time: 0,
          return_home_power: str,
        },
        latitude: 0,
        longitude: 0,
      } as DeviceOsd
    })
    const shareId = computed(() => {
      return store.state.layerBaseInfo.share
    })
    const defaultId = computed(() => {
      return store.state.layerBaseInfo.default
    })
    const drawVisible = computed(() => {
      return store.state.drawVisible
    })
    // const drawVisible = null
    const livestreamOthersVisible = computed(() => {
      return store.state.livestreamOthersVisible
    })
    const livestreamAgoraVisible = computed(() => {
      return store.state.livestreamAgoraVisible
    })
    const osdVisible = computed(() => {
      return store.state.osdVisible
    })
    const qualityStyle = computed(() => {
      if (deviceInfo.dock.basic_osd?.network_state?.type === NetworkStateTypeEnum.ETHERNET ||
          (deviceInfo.dock.basic_osd?.network_state?.quality || 0) > NetworkStateQualityEnum.FAIR) {
        return 'color: #00ee8b'
      }
      if ((deviceInfo.dock.basic_osd?.network_state?.quality || 0) === NetworkStateQualityEnum.FAIR) {
        return 'color: yellow'
      }
      return 'color: red'
    })
    watch(() => store.state.deviceStatusEvent,
      data => {
        if (Object.keys(data.deviceOnline).length !== 0) {
          deviceTsaUpdateHook.initMarker(data.deviceOnline.domain, data.deviceOnline.device_callsign, data.deviceOnline.sn)
          store.state.deviceStatusEvent.deviceOnline = {} as DeviceStatus
        }
        if (Object.keys(data.deviceOffline).length !== 0) {
          deviceTsaUpdateHook.removeMarker(data.deviceOffline.sn)
          if ((data.deviceOffline.sn === osdVisible.value.sn) || (osdVisible.value.is_dock && data.deviceOffline.sn === osdVisible.value.gateway_sn)) {
            osdVisible.value.visible = false
            store.commit('SET_OSD_VISIBLE_INFO', osdVisible)
          }
          store.state.deviceStatusEvent.deviceOffline = {}
        }
      },
      {
        deep: true
      }
    )
    watch(() => store.state.deviceState, data => {
      if (data.currentType === EDeviceTypeName.Gateway && data.gatewayInfo[data.currentSn]) {
        const coordinate = wgs84togcj02(data.gatewayInfo[data.currentSn].longitude, data.gatewayInfo[data.currentSn].latitude)
        deviceTsaUpdateHook.moveTo(data.currentSn, coordinate[0], coordinate[1])
        if (osdVisible.value.visible && osdVisible.value.gateway_sn !== '') {
          deviceInfo.gateway = data.gatewayInfo[osdVisible.value.gateway_sn]
        }
      }
      if (data.currentType === EDeviceTypeName.Aircraft && data.deviceInfo[data.currentSn]) {
        const coordinate = wgs84togcj02(data.deviceInfo[data.currentSn].longitude, data.deviceInfo[data.currentSn].latitude)
        deviceTsaUpdateHook.moveTo(data.currentSn, coordinate[0], coordinate[1])
        if (osdVisible.value.visible && osdVisible.value.sn !== '') {
          deviceInfo.device = data.deviceInfo[osdVisible.value.sn]
          // console.log(deviceInfo.device)
        }
      }
      if (data.currentType === EDeviceTypeName.Dock && data.dockInfo[data.currentSn]) {
        const coordinate = wgs84togcj02(data.dockInfo[data.currentSn].basic_osd?.longitude, data.dockInfo[data.currentSn].basic_osd?.latitude)
        deviceTsaUpdateHook.initMarker(EDeviceTypeName.Dock, EDeviceTypeName[EDeviceTypeName.Dock], data.currentSn, coordinate[0], coordinate[1])
        if (osdVisible.value.visible && osdVisible.value.is_dock && osdVisible.value.gateway_sn !== '') {
          deviceInfo.dock = data.dockInfo[osdVisible.value.gateway_sn]
          deviceInfo.device = data.deviceInfo[deviceInfo.dock.basic_osd.sub_device?.device_sn ?? osdVisible.value.sn]
        }
      }
    }, {
      deep: true
    })

    watch(
      () => store.state.wsEvent,
      newData => {
        const useGMapCoverHook = useGMapCover()
        const event = newData
        let exist = false
        if (Object.keys(event.mapElementCreat).length !== 0) {
          // console.log(event.mapElementCreat)
          const ele = event.mapElementCreat
          store.state.Layers.forEach(layer => {
            layer.elements.forEach(e => {
              if (e.id === ele.id) {
                exist = true
                // console.log('true')
                // console.log('dfg', ele.resource.content.geometry.coordinates)
              }
            })
          })

          if (exist === false) {
            setLayers({
              id: ele.id,
              name: ele.name,
              resource: ele.resource
            })

            updateCoordinates('wgs84-gcj02', ele)
            const data = { id: ele.id, name: ele.name }
            if (MapElementEnum.PIN === ele.resource?.type) {
              useGMapCoverHook.init2DPin(
                ele.name,
                ele.resource.content.geometry.coordinates,
                ele.resource.content.properties.color,
                data
              )
            } else if (MapElementEnum.LINE === ele.resource?.type) {
              useGMapCoverHook.initPolyline(
                ele.name,
                ele.resource.content.geometry.coordinates,
                ele.resource.content.properties.color,
                data)
            } else if (MapElementEnum.POLY === ele.resource?.type) {
              useGMapCoverHook.initPolygon(
                ele.name,
                ele.resource.content.geometry.coordinates,
                ele.resource.content.properties.color,
                data)
            }
          }
        }
        if (Object.keys(event.mapElementUpdate).length !== 0) {
          console.log(event.mapElementUpdate)
          // console.log('该功能还未实现，请开发商自己增加')
          store.state.wsEvent.mapElementUpdate = {}
        }
        if (Object.keys(event.mapElementDelete).length !== 0) {
          console.log(event.mapElementDelete)
          // console.log('该功能还未实现，请开发商自己增加')
          store.state.wsEvent.mapElementDelete = {}
        }
        // store.commit('SET_POINTS', pointArray)
      },
      {
        deep: true
      }
    )
    // ----------------------------------------------------------------------------------绘制 删除航线-------------------------------------------------------------------------------
    const markers = [] // 用于存储所有标注的引用
    let polylines = [] // 用于存储折线的引用
    let points = [] // 存放所有的航点
    function showWayline (lineArr: string[]) {
      console.log('ceshi')
      const AMap = root.$aMap
      if (!AMap) {
        console.error('AMap is not defined')
        return
      }
      // 将字符串坐标转换为数字数组并转换坐标系
      const convertedLineArr = lineArr.map(coordStr => {
        // const [lng, lat] = JSON.parse(coordStr)
        const [lng, lat] = [coordStr[0], coordStr[1]]
        return [lng, lat]
      })

      // 检查转换后的坐标是否有效
      if (convertedLineArr.some(coord => coord.length !== 2 || coord.some(val => isNaN(val)))) {
        console.error('Invalid coordinates after transformation')
        return
      }
      const polyline = new AMap.Polyline({
        path: convertedLineArr, // 设置线覆盖物路径
        isOutline: true,
        outlineColor: '#ff0000',
        strokeColor: '#3366FF',
        strokeOpacity: 1.0,
        strokeWeight: 3,
        strokeStyle: 'solid'
      })
      root.$map.add(polyline)
      polylines.push(polyline)
      // 设置地图视野以包含所有点
      const bounds = new AMap.Bounds(
        new AMap.LngLat(Math.min(...convertedLineArr.map(coord => coord[0])), Math.min(...convertedLineArr.map(coord => coord[1]))),
        new AMap.LngLat(Math.max(...convertedLineArr.map(coord => coord[0])), Math.max(...convertedLineArr.map(coord => coord[1])))
      )
      root.$map.setBounds(bounds)
      // const iconUrl = IconPoint // 替换为你的图标路径
      // convertedLineArr.forEach((position) => {
      //   const marker = new AMap.Marker({
      //     position: position,
      //     icon: new AMap.Icon({
      //       image: iconUrl,
      //       size: new AMap.Size(50, 50), // 图标大小
      //       imageSize: new AMap.Size(20, 20) // 图标大小
      //     }),
      //     offset: new AMap.Pixel(0, 0) // 偏移量
      //   })
      //   root.$map.add(marker)
      //   markers.push(marker) // 将标注添加到数组中
      // })
      // 使地图适应所有标注
      // root.$map.setFitView([polyline, ...markers])
    }
    let edit = true // 判断用户是在编辑航线还是新建航线
    function drawWayline () {
      const layerData = store.state.Points
      // 绘制航线
      if (points.length < 1 && layerData[0].elements.length < 1) {
        return
      } else if (edit) {
        // 仓库数据监听函数watch  在浏览器刷新时，第一次监听不到。添加immediate: true 监听函数会报错，暂时在此处添加。只有编辑航线运行此段代码
        layerData.forEach(item => {
          if (item.elements) {
            item.elements.forEach(ele => {
              const data = { id: ele.id, name: ele.name }
              const existPoint = store.state.coverMap[ele.id]
              if (!existPoint) { // 该点不存在于高德地图上，绘制它
                updateCoordinates('wgs84-gcj02', ele)
                useGMapCoverHook.init2DPin(
                  ele.name,
                  ele.resource.content.geometry.coordinates,
                  ele.resource.content.properties.color,
                  data
                )
                const point = {
                  id: ele.id,
                  cord: ele.resource.content.geometry.coordinates
                }
                const exists = points.some(existingPoint => existingPoint.id === ele.id)
                if (!exists) {
                  points.push(point)
                }
              }
            })
          }
        })
      }
      const pointArray = points.map(item => item.cord)
      showWayline(pointArray)
      edit = true
    }
    //  航点位置交换时，绘制航线

    function drawLinebyDrag (val:any) {
      // 删除航线
      if (polylines) {
        polylines.forEach(marker => {
          root.$map.remove(marker)
        })
        polylines = []
      }
      // 创建一个映射 id 到 layerData 的索引
      const idToIndexMap = new Map(val[0].elements.map((item, index) => [item.id, index]))

      // 根据 id 顺序重新排序 points
      const sortedData2 = points.slice().sort((a, b) => {
        return idToIndexMap.get(a.id) - idToIndexMap.get(b.id)
      })

      const pointArray = sortedData2.map(item => item.cord)
      // console.log('修改前', points)
      // console.log('修改后', sortedData2)
      showWayline(pointArray)
    }
    // 删除点位,重新绘制航线
    function removeAllWayline (key: any) {
      // 清空原来的折线
      if (polylines) {
        polylines.forEach(marker => {
          root.$map.remove(marker)
        })
        polylines = []
      }
      // console.log(key)
      const filteredData = points.filter(item => item.id !== key)
      points = filteredData
      const pointArray = filteredData.map(item => item.cord)
      if (pointArray.length > 1) {
        showWayline(pointArray)
        // console.log('绘制')
      }

      // markers.forEach(marker => {
      //   root.$map.remove(marker)
      // })
      // markers.length = 0 // 清空标注数组
    }
    // ------------------------------------------------------------------------------------------结束-------------------------------------------------------------------------------
    function draw (type: MapDoodleType, bool: boolean, flightAreaType?: EFlightAreaType) {
      state.currentType = type
      mouseMode.value = bool
      state.isFlightArea = !!flightAreaType
      useMouseToolHook.mouseTool(type, getDrawCallback, flightAreaType)
    }

    // dock 控制面板
    const {
      dockControlPanelVisible,
      setDockControlPanelVisible,
      onCloseControlPanel,
    } = useDockControl()

    // 连接或断开drc
    useConnectMqtt()

    onMounted(() => {
      const app = getApp()
      useGMapManageHook.globalPropertiesConfig(app)
      // console.log('初始化')
    })

    const { getDrawFlightAreaCallback, onFlightAreaDroneLocationWs } = useFlightArea()
    useFlightAreaDroneLocationEvent(onFlightAreaDroneLocationWs)

    function selectFlightAreaAction ({ type, isCircle }: { type: EFlightAreaType, isCircle: boolean }) {
      draw(isCircle ? MapDoodleEnum.CIRCLE : MapDoodleEnum.POLYGON, true, type)
    }

    function getDrawCallback ({ obj }: { obj : any }) {
      if (state.isFlightArea) {
        getDrawFlightAreaCallback(obj)
        return
      }
      switch (state.currentType) {
        case MapDoodleEnum.PIN:
          postPinPositionResource(obj)
          break
        case MapDoodleEnum.POLYLINE:
          postPolylineResource(obj)
          break
        case MapDoodleEnum.POLYGON:
          postPolygonResource(obj)
          break
        default:
          break
      }
    }
    async function postPinPositionResource (obj) {
      const req = getPinPositionResource(obj)
      setLayers(req)
      const coordinates = req.resource.content.geometry.coordinates
      updateCoordinates('gcj02-wgs84', req);
      (req.resource.content.geometry.coordinates as GeojsonCoordinate).push((coordinates as GeojsonCoordinate)[2])
      // const result = await postElementsReq(shareId.value, req)
      // obj.setExtData({ id: req.id, name: req.name })
      store.state.coverMap[req.id] = [obj]
      // 更新地图上的航点名称
      // console.log('调用更新航线名称函数')
      // console.log(req.name)
      useGMapCoverHook.updatePinElementTitle(req.id, req.name)
      const Layers = store.state.Points
      localStorage.setItem('wayline', JSON.stringify(Layers[0]))
    }
    async function postPolylineResource (obj) {
      const req = getPolylineResource(obj)
      setLayers(req)
      updateCoordinates('gcj02-wgs84', req)
      const result = await postElementsReq(shareId.value, req)
      obj.setExtData({ id: req.id, name: req.name })
      store.state.coverMap[req.id] = [obj]
    }
    async function postPolygonResource (obj) {
      const req = getPoygonResource(obj)
      setLayers(req)
      updateCoordinates('gcj02-wgs84', req)
      const result = await postElementsReq(shareId.value, req)
      obj.setExtData({ id: req.id, name: req.name })
      store.state.coverMap[req.id] = [obj]
    }

    function getPinPositionResource (obj) {
      const position = obj.getPosition()
      const resource = generatePointContent(position)
      const name = obj._originOpts.title
      const id = uuidv4()
      return {
        id,
        name,
        resource
      }
    }
    function getPolylineResource (obj) {
      const path = obj.getPath()
      const resource = generateLineContent(path)
      const { name, id } = getBaseInfo(obj._opts)
      return {
        id,
        name,
        resource
      }
    }
    function getPoygonResource (obj) {
      const path = obj.getPath()
      const resource = generatePolyContent(path)
      const { name, id } = getBaseInfo(obj._opts)
      return {
        id,
        name,
        resource
      }
    }
    function getBaseInfo (obj) {
      const name = obj.title
      const id = uuidv4()
      return { name, id }
    }

    function setLayers (resource: PostElementsBody) {
      // 添加航点数据
      const points1 = store.state.Points
      const exists1 = points1[0].elements.some(item => item.id === resource.id) // 避免重复添加
      if (!exists1) {
        // 添加航点数据
        const Placemark = {
          useGlobalHeight: 1,
          useGlobalHeadingParam: 1,
          gimbalPitchAngle: -37,
          useGlobalSpeed: 1,
          useGlobalTurnParam: 1,
          actionGroup: {}
        }
        resource.Placemark = Placemark
        resource.name = '航点' + (points1[0].elements.length + 1)
        points1[0].elements.push(resource)
        store.commit('SET_POINTS', points1)
        const point = {
          id: resource.id,
          cord: resource.resource.content.geometry.coordinates
        }
        points.push(point)
        // 绘制航线
        edit = false
        drawWayline()
      }
    }

    watch(() => store.state.Points, newData => {
      // 添加标志  重新绘制航线------------------
      const pointData = newData
      pointData.forEach(item => {
        if (item.elements) {
          item.elements.forEach(ele => {
            const data = { id: ele.id, name: ele.name }
            const existPoint = store.state.coverMap[ele.id]
            // 转成高德坐标系
            const coordStr = ele.resource.content.geometry.coordinates
            const [lng, lat] = wgs84togcj02(coordStr[0], coordStr[1])
            if (!existPoint) { // 该点不存在于高德地图上，绘制它
              // const [lng1, lat1] = wgs84togcj02(coordStr[0], coordStr[1])
              // updateCoordinates('wgs84-gcj02', ele)
              const AMap = root.$aMap
              if (AMap) {
                useGMapCoverHook = useGMapCover()
                useGMapCoverHook.init2DPin(
                  ele.name,
                  [lng, lat],
                  ele.resource.content.properties.color,
                  data
                )
                useGMapCoverHook.updatePinElementTitle(ele.id, ele.name)
              }
              // console.log('后', ele.resource.content.geometry.coordinates)
              // updateCoordinates('gcj02-wgs84', ele) // 再把坐标系改正回大地坐标系
            }
            const point = {
              id: ele.id,
              cord: [lng, lat]
            }
            const exists = points.some(existingPoint => existingPoint.id === ele.id)
            if (!exists) {
              points.push(point)
            }
          })
        }
        // console.log('航线', points)
      })
    }, { deep: true })
    function closeLivestreamOthers () {
      store.commit('SET_LIVESTREAM_OTHERS_VISIBLE', false)
    }
    function closeLivestreamAgora () {
      store.commit('SET_LIVESTREAM_AGORA_VISIBLE', false)
    }
    function updateCoordinates (transformType: string, element: any) {
      const geoType = element.resource?.content.geometry.type
      const type = element.resource?.type as number
      if (element.resource) {
        if (MapElementEnum.PIN === type) {
          const coordinates = element.resource?.content.geometry
            .coordinates as GeojsonCoordinate
          if (transformType === 'wgs84-gcj02') {
            const transResult = wgs84togcj02(
              coordinates[0],
              coordinates[1]
            ) as GeojsonCoordinate
            element.resource.content.geometry.coordinates = transResult
          } else if (transformType === 'gcj02-wgs84') {
            const transResult = gcj02towgs84(
              coordinates[0],
              coordinates[1]
            ) as GeojsonCoordinate
            element.resource.content.geometry.coordinates = transResult
          }
        } else if (MapElementEnum.LINE === type) {
          const coordinates = element.resource?.content.geometry
            .coordinates as GeojsonCoordinate[]
          if (transformType === 'wgs84-gcj02') {
            coordinates.forEach((coordinate, i, arr) => {
              arr[i] = wgs84togcj02(
                coordinate[0],
                coordinate[1]
              ) as GeojsonCoordinate
            })
          } else if (transformType === 'gcj02-wgs84') {
            coordinates.forEach((coordinate, i, arr) => {
              arr[i] = gcj02towgs84(
                coordinate[0],
                coordinate[1]
              ) as GeojsonCoordinate
            })
          }
          element.resource.content.geometry.coordinates = coordinates
        } else if (MapElementEnum.POLY === type) {
          const coordinates = element.resource?.content.geometry
            .coordinates[0] as GeojsonCoordinate[]
          if (transformType === 'wgs84-gcj02') {
            coordinates.forEach((coordinate, i, arr) => {
              arr[i] = wgs84togcj02(
                coordinate[0],
                coordinate[1]
              ) as GeojsonCoordinate
            })
          } else if (transformType === 'gcj02-wgs84') {
            coordinates.forEach((coordinate, i, arr) => {
              arr[i] = gcj02towgs84(
                coordinate[0],
                coordinate[1]
              ) as GeojsonCoordinate
            })
          }
          element.resource.content.geometry.coordinates = [coordinates]
        }
      }
    }
    // defineExpose({ removeAllWayline })
    return {
      draw,
      mouseMode,
      drawVisible,
      livestreamOthersVisible,
      livestreamAgoraVisible,
      osdVisible,
      pin,
      removeAllWayline,
      drawWayline,
      showWayline,
      drawLinebyDrag,
      state,
      M30,
      deviceInfo,
      EGear,
      EModeCode,
      str,
      EDockModeCode,
      dockControlPanelVisible,
      setDockControlPanelVisible,
      onCloseControlPanel,
      NetworkStateTypeEnum,
      NetworkStateQualityEnum,
      RainfallEnum,
      DroneInDockEnum,
      qualityStyle,
      selectFlightAreaAction,
    }
  }
})
</script>

  <style lang="scss" scoped>

  .g-map-wrapper {
    height: 100%;
    width: 100%;

    .g-action-panel {
      position: absolute;
      top: 80px;
      right: 16px;
      .g-action-item {
        width: 28px;
        height: 28px;
        background: white;
        color: $primary;
        border-radius: 2px;
        line-height: 28px;
        text-align: center;
        margin-bottom: 2px;
      }
      .g-action-item:hover {
        border: 1px solid $primary;
        border-radius: 2px;
      }
    }
    .selection {
      border: 1px solid $primary;
      border-radius: 2px;
    }

    // antd button 光晕
    &:deep(.ant-btn){
      &::after {
        display: none;
      }
    }
  }
  .osd-panel {
    position: absolute;
    margin-left: 10px;
    left: 0;
    top: 10px;
    width: 480px;
    background: #000;
    color: #fff;
    border-radius: 2px;
    opacity: 0.8;
  }
  .osd > div:not(.dock-control-panel) {
    margin-top: 5px;
    padding-left: 5px;
  }

  .circle {
    border-radius: 50%;
    width: 10px;
    height: 10px;
  }

  .battery-slide {
    .capacity-percent {
      background: #00ee8b;
    }
    .return-home {
      background: #ff9f0a;
    }
    .landing {
      background: #f5222d;
    }
    .white-point {
      width: 4px;
      height: 4px;
      border-radius: 50%;
      background: white;
      bottom: -0.5px;
    }
    .battery {
      background: #141414;
      color: #00ee8b;
      margin-top: -10px;
      height: 20px;
      width: auto;
      border-left: 1px solid #00ee8b;
      padding: 0 5px;
    }
  }
  .battery-slide > div {
    position: absolute;
    min-height: 2px;
    border-radius: 2px;
  }

  .liveview {
    position: absolute;
    color: #fff;
    z-index: 1;
    left: 0;
    margin-left: 10px;
    top: 10px;
    text-align: center;
    width: 800px;
    height: 720px;
    background: #232323;
  }
  </style>
