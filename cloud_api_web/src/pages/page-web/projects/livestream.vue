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
        <div :title="地图切换" class="map-switch" @click="isFlatMap = !isFlatMap"><el-icon><Switch /></el-icon></div>
        <div style="width: 100%; height: 100%; border: 2px solid white;">
          <ThreeDModel v-if="isFlatMap" />
          <TwoDModel v-else />
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
import { message } from 'ant-design-vue'
import { onMounted, watch, ref, reactive, computed } from 'vue'
import { getRoot } from '/@/root'
import { useMyStore } from '/@/store'
import { ELocalStorageKey, ERouterName } from '/@/types/enums'
import { TaskStatus, TaskProgressInfo, TaskProgressStatus, TaskProgressWsStatusMap, MediaStatus, MediaStatusProgressInfo, TaskMediaHighestPriorityProgressInfo } from '/@/types/task'
import { useRouter } from 'vue-router'
import { getDeviceTopo, getUnreadDeviceHms, updateDeviceHms, getPlatformInfo, getAllWorkspaceInfo } from '/@/api/manage'
import CustomTree from '/@/components/substationTree.vue'
import TwoDModel from '/@/components/g-map/mapPanel.vue'
import ThreeDModel from '/@/components/cesium/3DModelPanel.vue'
import tsaPanel from '/@/components/tsaPanel.vue'
// import ttt from '/@/components/GMap.vue'
import deviceState from '/@/components/devices/drone_control/device_state.vue'
// import control from '/@/components/control/loadHandle.vue'
import { EDeviceTypeName } from '/@/types'
const showLive1 = ref<boolean>(false)
const scorllHeight = ref() // 容器自适应滚动高度

const root = getRoot()
const routeName = ref<string>('LiveOthers')
const showLive = ref<boolean>(false)
const showDockLive = ref<boolean>(false)
const router = useRouter()
let workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
const userId = ref(localStorage.getItem(ELocalStorageKey.UserId)!)
const isFlatMap = ref(false) // 是否二维地图

// 无人机视频---------------------------------------------------
const toggleDroneVideo = () => {
  showLive.value = !showLive.value
}
// Function to close the video window
const closeVideo = () => {
  showLive.value = false
}
// 机场视频------------------------------------------------------
const toggleDockVideo = () => {
  showDockLive.value = !showDockLive.value
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
  // 添加树形图数据
  getTreeData()
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
