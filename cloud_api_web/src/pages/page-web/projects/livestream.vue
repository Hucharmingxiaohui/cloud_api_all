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
        <div style="width: 100%; height: 100%; border: 2px solid white;">
          <TwoDModel v-if="isFlatMap && isMounted" />
          <OutdoorRenderer v-if="cloudRendererEnabled && !isFlatMap && isMounted" />
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
import { EDeviceTypeName } from '/@/types'
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
</style>
