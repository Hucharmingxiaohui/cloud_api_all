<template>
  <div class="container">
    <!-- <div class="header1">监控中心</div> -->
    <div class="main-box" :style="{ height: scorllHeight + 'px'}">
      <div class="box-left">
        <tsaPanel/>
        <!-- <div class="workspace-node">
          <custom-tree :treeData="treeData" @change="handleNodeChange" />
          <tsaPanel/>
        </div> -->
        <!-- <div class="device-box">
          <tsaPanel/>
        </div> -->
      </div>
      <div class="box-right">
        <div v-if="cloudRendererEnabled" title="地图切换" class="map-switch" @click="isFlatMap = !isFlatMap"><el-icon><Switch /></el-icon></div>
        <div style="width: 100%; height: 100%; border: 2px solid white; position: relative;">
          <TwoDModel v-if="isFlatMap && isMounted" />
          <OutdoorRenderer v-if="cloudRendererEnabled && !isFlatMap && isMounted" />
          <!-- 三维模式下的设备详情弹窗（二维模式由地图组件内部弹窗渲染，避免重复） -->
          <div class="osd-panel fz12" v-if="!isFlatMap && osdVisible && osdVisible.visible">
            <div class="title-bg" style="border-bottom: 1px solid #515151; height: 37px;">
              <div class="thumbnail_1"></div>
              <div class="box_text">{{ osdVisible.gateway_callsign }}</div>
              <span> </span>
            </div>
            <span><a style="color: white; position: absolute; top: 5px; right: 5px;" @click="() => osdVisible.visible = false"><CloseOutlined /></a></span>
            <div class="flex-display" style="border-bottom: 1px solid #515151; padding: 10px; margin-right: 10px;">
              <div class="flex-column flex-align-stretch flex-justify-center device-bg" style="width: 140px; background: #2d2d2d; border: 1px solid #323D56;">
                <a-tooltip :title="osdVisible.model">
                  <div class="flex-column flex-align-center flex-justify-center" style="width: 100%;">
                    <span>
                      <img class="thumbnail_1" referrerpolicy="no-referrer"
                        src="https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG175308f62f1d0541bffda14c08e3c9d8.png" />
                    </span>
                    <span class="text-bg" style="height: 60px; width: 100%;">
                      Dock
                    </span>
                  </div>
                </a-tooltip>
              </div>
              <div class="osd flex-1" style="flex: 1; background-color: rgba(11,23,42, 1)">
                <div style="margin-left: 10px;margin-top: 0; padding: 10px; border: 1px solid #323D56; ">
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="24" :style="deviceInfo.dock.basic_osd?.mode_code === EDockModeCode.Disconnected ? 'color: red; font-weight: 700;': 'color: rgb(25,190,107)'">
                      工作模式：{{ EDockModeCode[deviceInfo.dock.basic_osd?.mode_code] ?? '--' }}</a-col>
                  </a-row>
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="12">
                      <a-tooltip title="网络状态">
                        <span>网络：</span>
                        <span :style="qualityStyle">
                          <span v-if="deviceInfo.dock.basic_osd?.network_state?.type === NetworkStateTypeEnum.FOUR_G"><SignalFilled /></span>
                          <span v-else><GlobalOutlined /></span>
                        </span>
                        <span class="ml10">{{ deviceInfo.dock.basic_osd?.network_state?.rate ?? '--' }} kb/s</span>
                      </a-tooltip>
                    </a-col>
                    <a-col span="12">
                      <a-tooltip title="待上传媒体文件">
                        <span>待上传媒体：</span>
                        <span class="ml10">{{ deviceInfo.dock.link_osd?.media_file_detail?.remain_upload ?? '--' }}</span>
                      </a-tooltip>
                    </a-col>
                  </a-row>
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="12">
                      <a-tooltip title="存储容量">
                        <span>存储：</span>
                        <span class="ml10" v-if="deviceInfo.dock.basic_osd?.storage?.total > 0">
                          <a-progress type="circle" :width="20" :percent="deviceInfo.dock.basic_osd?.storage?.used * 100 / deviceInfo.dock.basic_osd?.storage?.total"
                            :strokeWidth="20" :showInfo="false" :strokeColor="deviceInfo.dock.basic_osd?.storage?.used * 100 / deviceInfo.dock.basic_osd?.storage?.total > 80 ? 'red' : '#00ee8b' "/>
                        </span>
                        <span class="ml10" v-else>--</span>
                      </a-tooltip>
                    </a-col>
                    <a-col span="12">
                      <span>风速：</span>
                      <span class="ml10">{{ (deviceInfo.dock.basic_osd?.wind_speed ?? '--') + ' m/s'}}</span>
                    </a-col>
                  </a-row>
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="12">
                      <span>环境温度：</span>
                      <span class="ml10">{{ deviceInfo.dock.basic_osd?.environment_temperature ?? '--' }} °C</span>
                    </a-col>
                    <a-col span="12">
                      <span>机场温度：</span>
                      <span class="ml10">{{ deviceInfo.dock.basic_osd?.temperature ?? '--' }} °C</span>
                    </a-col>
                  </a-row>
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="12">
                      <span>降雨量：</span>
                      <span class="ml10">{{ RainfallEnum[deviceInfo.dock.basic_osd?.rainfall] ?? '--' }}</span>
                    </a-col>
                    <a-col span="12">
                      <span>机场湿度：</span>
                      <span class="ml10">{{ deviceInfo.dock.basic_osd?.humidity ?? '--' }}</span>
                    </a-col>
                  </a-row>
                  <a-row>
                    <a-col span="12">
                      <span>舱内有无无人机：</span>
                      <span class="ml10">{{ deviceInfo.dock.basic_osd?.drone_in_dock === undefined ? '--' : (deviceInfo.dock.basic_osd?.drone_in_dock ? '是' : '否') }}</span>
                    </a-col>
                  </a-row>
                </div>
                <!-- 无人机 -->
                <div v-if="deviceInfo.device && deviceInfo.device.mode_code !== EModeCode.Disconnected" style="margin-left: 10px; margin-top: 5px; padding: 10px; border: 1px solid #323D56; ">
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="24" :style="deviceInfo.device.mode_code === EModeCode.Disconnected ? 'color: red; font-weight: 700;': 'color: rgb(25,190,107)'">
                      无人机工作模式：{{ EModeCode[deviceInfo.device.mode_code] ?? '--' }}</a-col>
                  </a-row>
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="12">
                      <span>飞行高度：</span>
                      <span class="ml10">{{ deviceInfo.device.height ?? '--' }} m</span>
                    </a-col>
                    <a-col span="12">
                      <span>海拔高度：</span>
                      <span class="ml10">{{ deviceInfo.device.elevation ?? '--' }} m</span>
                    </a-col>
                  </a-row>
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="12">
                      <span>距返航点：</span>
                      <span class="ml10">{{ deviceInfo.device.home_distance ?? '--' }} m</span>
                    </a-col>
                    <a-col span="12">
                      <span>水平速度：</span>
                      <span class="ml10">{{ deviceInfo.device.horizontal_speed ?? '--' }} m/s</span>
                    </a-col>
                  </a-row>
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="12">
                      <span>垂直速度：</span>
                      <span class="ml10">{{ deviceInfo.device.vertical_speed ?? '--' }} m/s</span>
                    </a-col>
                    <a-col span="12">
                      <span>风速：</span>
                      <span class="ml10">{{ deviceInfo.device.wind_speed ?? '--' }} m/s</span>
                    </a-col>
                  </a-row>
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="12">
                      <span>风向（角度）：</span>
                      <span class="ml10">{{ deviceInfo.device.wind_direction ?? '--' }}</span>
                    </a-col>
                    <a-col span="12">
                      <span>电量：</span>
                      <span class="ml10">{{ deviceInfo.device.battery?.capacity_percent ?? '--' }} %</span>
                    </a-col>
                  </a-row>
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="12">
                      <a-tooltip title="低电量返航阈值">
                        <span>返航电量：</span>
                        <span class="ml10">{{ deviceInfo.device.battery?.landing_power ?? '--' }} %</span>
                      </a-tooltip>
                    </a-col>
                    <a-col span="12">
                      <span>剩余飞行时间：</span>
                      <span class="ml10">{{ formatRemainTime(deviceInfo.device.battery?.remain_flight_time) }}</span>
                    </a-col>
                  </a-row>
                  <a-row style="margin-bottom: 5px;">
                    <a-col span="12">
                      <span>GPS 卫星数：</span>
                      <span class="ml10">{{ deviceInfo.device.position_state?.gps_number ?? '--' }}</span>
                    </a-col>
                    <a-col span="12">
                      <span>RTK 卫星数：</span>
                      <span class="ml10">{{ deviceInfo.device.position_state?.rtk_number ?? '--' }}</span>
                    </a-col>
                  </a-row>
                </div>
                <div v-else style="margin-left: 10px; margin-top: 5px; padding: 5px 10px; color: rgba(255,255,255,0.6);">
                  无人机未上报数据（未开机或未起飞）
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!isFlatMap && livestream.visible" class="liveview">
          <div class="liveview__header">
            <div class="liveview__title">监控直播</div>
            <button class="liveview__close" type="button" @click="closeLivestream">×</button>
          </div>
          <div class="liveview__tabs">
            <el-button class="btn" :class="{ active: showLive }" :disabled="!livestream.has_drone" @click="toggleDroneVideo">无人机视频</el-button>
            <el-button class="btn" :class="{ active: showDockLive }" @click="toggleDockVideo">机场视频</el-button>
          </div>
          <div class="liveview__video">
            <LivestreamOthers v-if="showLive && livestream.has_drone" :sn="livestream.dorne_sn" />
            <LivestreamDock v-else-if="showDockLive" :sn="livestream.dock_sn" />
            <div v-else class="liveview__empty">请选择监控画面</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {
  DeviceOsd, DeviceStatus, DockOsd, EGear, EModeCode, GatewayOsd, EDockModeCode,
  NetworkStateQualityEnum, NetworkStateTypeEnum, RainfallEnum, DroneInDockEnum
} from '/@/types/device'
import { onMounted, watch, ref, nextTick, reactive, computed, defineAsyncComponent } from 'vue'
import { getRoot } from '/@/root'
import { useMyStore } from '/@/store'
import { ELocalStorageKey, ERouterName } from '/@/types/enums'
import { TaskStatus, TaskProgressInfo, TaskProgressStatus, TaskProgressWsStatusMap, MediaStatus, MediaStatusProgressInfo, TaskMediaHighestPriorityProgressInfo } from '/@/types/task'
import { useRouter } from 'vue-router'
import { getDeviceTopo, getUnreadDeviceHms, updateDeviceHms, getPlatformInfo, getAllWorkspaceInfo } from '/@/api/manage'
import CustomTree from '/@/components/substationTree.vue'
import { EDeviceTypeName, EBizCode } from '/@/types'
import { useConnectWebSocket } from '/@/hooks/use-connect-websocket'
import { CloseOutlined, SignalFilled, GlobalOutlined } from '@ant-design/icons-vue'
import { isCloudRendererEnabled } from '/@/components/cloudRenderer/cloudRendererConfig'

// 重型组件改为异步加载，减少首屏 bundle 体积
const TwoDModel = defineAsyncComponent(() => import('/@/components/g-map/mapPanel1.vue'))
const tsaPanel = defineAsyncComponent(() => import('/@/components/tsaPanel.vue'))
const OutdoorRenderer = defineAsyncComponent(() => import('/@/components/cloudRenderer/OutdoorRenderer.vue'))
const LivestreamOthers = defineAsyncComponent(() => import('/@/components/livestream-others.vue'))
const LivestreamDock = defineAsyncComponent(() => import('/@/components/livestream-dock.vue'))
const showLive1 = ref<boolean>(false)
const scorllHeight = ref() // 容器自适应滚动高度

const root = getRoot()
const routeName = ref<string>('LiveOthers')
const showLive = ref<boolean>(false)
const showDockLive = ref<boolean>(false)
const router = useRouter()
let workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
const userId = ref(localStorage.getItem(ELocalStorageKey.UserId)!)
const cloudRendererEnabled = isCloudRendererEnabled()
const isFlatMap = ref(!cloudRendererEnabled) // 是否二维地图
const isMounted = ref(false) // 是否已经完成初始化

// 无人机视频---------------------------------------------------
const toggleDroneVideo = () => {
  if (!livestream.value.has_drone) {
    showLive.value = false
    showDockLive.value = true
    return
  }
  showLive.value = true
  showDockLive.value = false
}
// Function to close the video window
const closeVideo = () => {
  showLive.value = false
}
// 机场视频------------------------------------------------------
const toggleDockVideo = () => {
  showDockLive.value = true
  showLive.value = false
}
const closeDockVideo = () => {
  showDockLive.value = false
}
// function closeVideo () {
//   showLive.value = false
// }
function closeVideo1 () {
  showLive1.value = false
}
function selectOperate () {
  showControl.value = true
}
onMounted(() => {
  scorllHeight.value = window.innerHeight - 60
  watch(() => root.$route.name, (data) => {
    showLive.value = data === ERouterName.LIVING
  }, { deep: true })
  isMounted.value = true
})

//= ===========================================添加树形图==========================================================================

const selectedNode = ref(null)

const treeData = ref([
  {
    title: '区域1',
    key: '1',
  }
])

function getTreeData () {
  let workspaces = null
  getAllWorkspaceInfo(userId.value).then(res => {
    // 转换数据格式
    workspaces = res.data
    if (workspaces) {
      clearLeafNodesAndAddData(treeData.value, workspaces)
    }
  })
}
// 添加树形图方法
const clearLeafNodesAndAddData = (treeData, data) => {
  treeData.forEach(node => {
    // 如果有子节点，则递归遍历
    if (node.children && node.children.length > 0) {
      clearLeafNodesAndAddData(node.children, data)
    }

    // 如果是叶子节点，清空现有数据并添加 data 中的数据
    if (!node.children || node.children.length === 0) {
      node.children = data.map(item => ({
        title: item.workspace_name,
        key: item.workspace_id, // 使用 workspace_id 作为 key
        workspace_id: item.workspace_id,
        workspace_desc: item.workspace_desc,
        platform_name: item.platform_name,
        bind_code: item.bind_code,
        isLeaf: true // 显式标记为叶子节点
      }))
    }
  })
}
// 树形图选中方法
const handleNodeChange = (node) => {
  // selectedNode.value = node

  workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!

  // console.log('Node changed in parent:', node) // 确认父组件事件是否触发
}
const showControl = ref(false)
const queryImages = () => {
  const job_id = '2a9e063d-5b35-47ab-ad01-b48b51073e4c'
  // 你的查询逻辑
  // 例如获取任务结果并处理
}
// ===============================================无人机状态控制信息==================================
const str: string = '--'
const store = useMyStore()
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
const osdVisible = computed(() => {
  return store.state.osdVisible
})
// 机场弹窗网络状态着色（与二维地图弹窗一致）
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
// 剩余飞行时间（秒）格式化为 分:秒
const formatRemainTime = (seconds: any) => {
  if (typeof seconds !== 'number' || isNaN(seconds)) return '--'
  return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
}
const livestream = computed(() => store.state.liveStream)

watch(() => [livestream.value.visible, livestream.value.has_drone, livestream.value.dock_sn, livestream.value.dorne_sn], ([visible]) => {
  if (!visible) {
    showLive.value = false
    showDockLive.value = false
    return
  }
  showLive.value = !!livestream.value.has_drone
  showDockLive.value = !livestream.value.has_drone
}, { deep: true })

function closeLivestream () {
  livestream.value.visible = false
  showLive.value = false
  showDockLive.value = false
  store.commit('SET_LIVESTREAM_INFO', livestream)
}

//  设备联通，位置在地图显示
watch(() => store?.state.deviceStatusEvent,
  data => {
    if (data && Object.keys(data.deviceOnline).length !== 0) {
      // deviceTsaUpdateHook.initMarker(data.deviceOnline.domain, data.deviceOnline.device_callsign, data.deviceOnline.sn)
      store.state.deviceStatusEvent.deviceOnline = {} as DeviceStatus
    }
    if (data && Object.keys(data.deviceOffline).length !== 0) {
      // deviceTsaUpdateHook.removeMarker(data.deviceOffline.sn)
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

watch(() => store?.state.deviceState, data => {
  if (data.currentType === EDeviceTypeName.Gateway && data.gatewayInfo[data.currentSn]) {
    // const coordinate = wgs84togcj02(data.gatewayInfo[data.currentSn].longitude, data.gatewayInfo[data.currentSn].latitude)
    // deviceTsaUpdateHook.moveTo(data.currentSn, coordinate[0], coordinate[1])
    if (osdVisible.value.visible && osdVisible.value.gateway_sn !== '') {
      deviceInfo.gateway = data.gatewayInfo[osdVisible.value.gateway_sn]
    }
  }
  if (data.currentType === EDeviceTypeName.Aircraft && data.deviceInfo[data.currentSn]) {
    // const coordinate = wgs84togcj02(data.deviceInfo[data.currentSn].longitude, data.deviceInfo[data.currentSn].latitude)
    // deviceTsaUpdateHook.moveTo(data.currentSn, coordinate[0], coordinate[1])
    if (osdVisible.value.visible && osdVisible.value.sn !== '') {
      deviceInfo.device = data.deviceInfo[osdVisible.value.sn]
    }
  }
  if (data.currentType === EDeviceTypeName.Dock && data.dockInfo[data.currentSn]) {
    // const coordinate = wgs84togcj02(data.dockInfo[data.currentSn].basic_osd?.longitude, data.dockInfo[data.currentSn].basic_osd?.latitude)
    // deviceTsaUpdateHook.initMarker(EDeviceTypeName.Dock, EDeviceTypeName[EDeviceTypeName.Dock], data.currentSn, coordinate[0], coordinate[1])
    if (osdVisible.value.visible && osdVisible.value.is_dock && osdVisible.value.gateway_sn !== '') {
      deviceInfo.dock = data.dockInfo[osdVisible.value.gateway_sn]
      deviceInfo.device = data.deviceInfo[deviceInfo.dock.basic_osd.sub_device?.device_sn ?? osdVisible.value.sn]
    }
  }
}, {
  deep: true
})

// 页面级 WebSocket：直播页三维模式下不挂载二维地图，OSD 数据需要本页自行接收写入 store
let dockOsdEmptyTimer: any = null
let dockOsdEmptyStartTime = 0
const messageHandler = async (payload: any) => {
  if (!payload) return
  switch (payload.biz_code) {
    case EBizCode.GatewayOsd:
      store.commit('SET_GATEWAY_INFO', payload.data)
      break
    case EBizCode.DeviceOsd:
      store.commit('SET_DEVICE_INFO', payload.data)
      break
    case EBizCode.DockOsd: {
      // 机场 OSD 存在周期性缺字段的报文（如环境温度为空），直接写入会整包覆盖 basic_osd 导致弹窗数据闪烁；
      // 与控制台页一致：带环境温度的报文立即写入，缺字段报文延迟 2 秒确认后仍缺失才写入
      const currentData = payload.data
      const hasEnvironmentTemperature = currentData.host?.environment_temperature !== ''
      if (hasEnvironmentTemperature) {
        clearTimeout(dockOsdEmptyTimer)
        dockOsdEmptyStartTime = 0
        store.commit('SET_DOCK_INFO', currentData)
      } else {
        const now = Date.now()
        if (dockOsdEmptyStartTime === 0) {
          dockOsdEmptyStartTime = now
        }
        clearTimeout(dockOsdEmptyTimer)
        dockOsdEmptyTimer = setTimeout(() => {
          if (Date.now() - dockOsdEmptyStartTime >= 2000) {
            store.commit('SET_DOCK_INFO', currentData)
          }
          dockOsdEmptyStartTime = 0
        }, 2000)
      }
      break
    }
  }
}
useConnectWebSocket(messageHandler)
</script>

<style lang="scss" scoped>
.container {
  width: 100vw;
  padding: 10px;
  display: flex;
  flex-direction: column;
}

.main-box {
  display: flex;
  // height: 100vh;
}

.live {
  position: absolute;
  z-index: 100;
  left: 600px;
  top: 200px;
  margin-left: 10px;
  text-align: center;
  width: 600px;
  height: 480px;
  background: #232323;
  border: 1px solid #2da3a5;
}
.docklive{
  position: absolute;
  z-index: 100;
  left:950px;
  top: 200px;
  margin-left: 10px;
  text-align: center;
  width: 600px;
  height: 480px;
  background: #232323;
  border: 1px solid #2da3a5;
}

.box-left {
  // background: rgba(59, 116, 255, 0.15);
  // width: 20%;
  background-color: rgba(17, 43, 88, 0.54);
  width: 430px;
  color: white;
  border-radius: 0;
  height: calc(100vh - 120px);;
  overflow-y: auto;
  overflow-x: hidden;
  // display: flex;
  // flex-direction: column; /* 使子元素按列排列 */
  .workspace-node{
    padding: 20px;
    width: 100%;
    height: 300px;
    border-bottom: 1px solid #1299c3;
    overflow: auto;
  }
  .device-box{
    padding: 10px;
    margin-top: 10px;
    flex: 1; /* 占满剩余空间 */
  }
}

.box-right {
  background-color: rgba(17, 43, 88, 0.54);
  flex: 1;
  margin-left: 10px;
  color: white;
  height: calc(100vh - 120px);
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 15px;
  .operation {
    margin-bottom: 20px;
  }
  .map-switch{
    position: absolute;
    right: 30px;
    top: 20px;
    color: rgb(17, 193, 224);
    font-size: 20px;
    height: 30px;
    width: 30px;
    border-radius: 50%;
    background: #075f8e;
    text-align: center;
    z-index: 2000;
    cursor: pointer;
  }
  .map-container {
    flex-grow: 1;
    height: 100%; // 调整高度使地图占据剩余空间
    border: 1px solid #2da3a5;
    border-radius: 8px;
    overflow: hidden;
  }
}

.liveview {
  position: absolute;
  z-index: 5000;
  top: 30px;
  left: 30px;
  width: min(520px, calc(100% - 60px));
  height: 410px;
  display: flex;
  flex-direction: column;
  color: #fff;
  background: rgba(5, 25, 61, 0.96);
  border: 1px solid rgba(52, 181, 238, 0.9);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.48), inset 0 0 20px rgba(34, 135, 255, 0.22);
}

.liveview__header {
  height: 42px;
  padding: 0 12px 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(45, 163, 165, 0.45);
  background: linear-gradient(90deg, rgba(11, 82, 132, 0.82), rgba(5, 25, 61, 0.4));
}

.liveview__title {
  font-size: 16px;
  font-weight: 600;
}

.liveview__close {
  width: 28px;
  height: 28px;
  padding: 0;
  color: #d9f7ff;
  font-size: 23px;
  line-height: 24px;
  cursor: pointer;
  border: 0;
  background: transparent;
}

.liveview__tabs {
  height: 48px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.liveview__tabs .btn.active {
  color: #fff;
  border-color: #47d6ff;
  background: linear-gradient(to top, #168dc4, #07527c);
}

.liveview__video {
  flex: 1;
  min-height: 0;
  margin: 0 12px 12px;
  overflow: hidden;
  border: 1px solid rgba(45, 163, 165, 0.55);
  background: #050b12;
}

.liveview__empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7898ad;
}

@media (max-width: 900px) {
  .liveview {
    top: 18px;
    left: 18px;
    width: calc(100% - 36px);
    height: 360px;
  }
}
.box-right1 {
  background: rgba(59, 116, 255, 0.15);
  flex: 3;
  margin-left: 10px;
  padding: 10px;
  color: white;
  border-radius: 15px;
  height: calc(100vh - 80px);;
  position: relative;
  overflow-y: auto;
}

.header1 {
  width: 100%;
  height: 60px;
  background: #05204b;
  padding: 16px;
  font-size: 20px;
  font-weight: bold;
  color: aliceblue;
}

.operation {
  display: flex;
  flex-direction: column;
  // padding: 15px;
  height:100vh;
  display: flex;
  justify-content: space-between;
  color: rgba(255, 255, 255, 0.762);
  // background-color: rgba(0, 112, 209, 0.2);
  font-size: 16px;
  // border: 4px solid rgba(0, 112, 209, 1);
  // border-bottom: 4px solid rgba(0, 112, 209, 1);
  // border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
  .item1 {
    display: flex;
    align-items: center;
    padding: 10px;
  }
}

.control-panel {
  // position: relative; /* 使其脱离文档流 */
  // top: 0px; /* 距离上边20px */
  // right: 0px; /* 固定到右边 */
  // border: 1px solid #1299c3;
  // background: #023956;
  // padding: 20px;
  border-radius: 8px;
  color: white;
  height:100%;
  width: 100%; /* 设置宽度 */
  z-index: 10; /* 确保它显示在前面 */
}
.btn {
  border: 2px solid #1299c3;
  background: linear-gradient(to top, #11b4fb, #023956);
  color: rgba(255, 255, 255, 0.762);
}

// 设备详情弹窗（三维模式），样式与二维地图组件内弹窗保持一致
.osd-panel {
  position: absolute;
  margin-left: 10px;
  left: 0;
  top: 10px;
  width: 540px;
  background: url('/@/assets/v4/osd_bg.png') 100% no-repeat;
  background-size: 100% 100%;
  background-color: rgb(5, 13, 99);
  color: #fff;
  border-radius: 2px;
  z-index: 10;
}
.title-bg {
  background: url('/@/assets/v4/osd_title.png') no-repeat !important;
  display: flex;
  justify-content: left;
  align-items: center;
  padding-left: 10px;
  margin-bottom: 10px;
  .thumbnail_1 {
    width: 24px;
    height: 22px;
    margin-right: 15px;
    background: url('/@/assets/v4/plan_icon1.png') 100% no-repeat;
    background-size: 100% 100%;
  }
  .box_text {
    text-shadow: 0px 0px 4px rgba(201, 252, 255, 0.41);
    background-image: linear-gradient(180deg, rgba(255, 255, 255, 1) 0, rgba(144, 201, 255, 1) 100%);
    font-size: 24px;
    font-family: Google Sans-Medium;
    font-weight: 500;
    white-space: nowrap;
    line-height: 30px;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}
.device-bg {
  background: url('/@/assets/v4/device_bg.png') 100% no-repeat !important;
  background-color: rgba(8, 51, 99, 0.9) !important;
}
.text-bg {
  background: url('/@/assets/v4/text_bg.png') 100% no-repeat !important;
  background-size: 100% 100%;
  text-align: center;
  padding-top: 20px;
}
.flex-display {
  display: flex;
}
.osd > div {
  margin-top: 5px;
  padding-left: 5px;
}
.ml10 {
  margin-left: 10px;
}
.fz12 {
  font-size: 12px;
}
.fz14 {
  font-size: 14px;
}
</style>
