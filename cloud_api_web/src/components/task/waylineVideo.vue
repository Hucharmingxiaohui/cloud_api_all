<template>
    <div class="container">
        <div class="main-box">
            <!-- 左侧区域，分为两部分：视频流和地图 -->
            <div class="box-left">
                <div class="nav">
                    <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/nav-icon.png" />
                    <span class="text_9">{{ current_sub }}</span>
                </div>
                <!-- 小标题部分 -->
                <div class="title">
                    <div class="text-box"> 机场监控</div>
                    <!-- 机场监控 -->
                </div>

                <!-- 视频流部分 -->
                <div class="video-section">
                    <!-- {{ osdVisible}} -->
                    <LivestreamDock :sn="osdVisible.sn"  :deviceInfo="deviceInfo"></LivestreamDock>
                     <!-- <div style="display: flex; align-items: center; justify-content: center; color: #fff; font-size: 30px; margin-top: 100px"> 暂不支持</div> -->
                </div>

                <!-- 地图部分 -->
                <div class="title">
                    <div class="text-box"> 航线监控</div>
                    <!-- 机场监控 -->
                </div>

                <div class="map-section">
                    <waylinePanel />
                </div>
            </div>

            <!-- 中间内容区域 -->
            <div class="box-middle">
                <!-- 切换标签部分，放在左侧 -->
                 <div class="nav-middle">
                     <div class="nav-title">
                        无人机直播
                     </div>
                     <div>
                        <div class="tab-section">
                            <div class="tab" :class="{ active: activeTab === 'load' }" @click="switchTab('load')">
                                负载直播
                            </div>
                            <div class="tab" :class="{ active: activeTab === 'flight' }" @click="switchTab('flight')">
                                飞行相机
                            </div>
                        </div>
                     </div>
                 </div>

                <!-- 视频流部分 -->
                <div class="uav-video">
                    <!-- <video class="video-player" controls autoplay>
                        <source v-bind:src="videoStreamUrl" type="video/mp4" />
                        你的浏览器不支持 video 标签。
                    </video> -->
                    <LivestreamOthers :sn="osdVisible.sn"  :deviceInfo="deviceInfo"></LivestreamOthers>
                </div>

                <!-- 任务名称部分 -->
                <div class="task-name" style="display: flex; flex-direction: row;">
                    <div style="width: 30%; margin:15px; display: flex;flex-direction: column;">
                        <span style="color: aliceblue; font-size: 14px; font-weight: 500;">任务名称</span>
                        <span style="color:aliceblue; font-size: 14px;"> {{ taskInfo.job_name ? taskInfo.job_name : '---' }}</span>
                    </div>
                    <div style="width: 30%; margin:15px; display: flex;flex-direction: column;">
                        <span style="color: aliceblue; font-size: 14px; font-weight: 500;">执行时间</span>
                        <span style="color:aliceblue; font-size: 14px;">{{ taskInfo.execute_time  ? taskInfo.execute_time : '---' }}</span>
                    </div>
                    <div style="width: 30%; margin:15px; display: flex;flex-direction: column;">
                        <span style="color: aliceblue; font-size: 14px; font-weight: 500;">航线名称</span>
                        <span style="color:aliceblue; font-size: 14px;">{{ taskInfo.file_name  ?  taskInfo.file_name : '---' }}</span>
                    </div>
                </div>
            </div>

            <!-- 右侧内容区域 -->
            <div class="box-right">
                <div class="nav">
                    <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/nav-icon.png" />
                    <span class="text_9">设备信息</span>
                </div>
                <!-- 切换标签区域：设备状态、设备运维（放在机舱上方） -->
                <div class="tab-box">
                    <div class="tab" :class="{ active: activeRightTab === 'status' }" @click="switchRightTab('status')">
                        设备状态
                    </div>
                    <div class="tab" :class="{ active: activeRightTab === 'maintenance' }"
                        @click="switchRightTab('maintenance')">
                        设备运维
                    </div>
                </div>

                <!-- 上部分：机舱 -->
                <div class="upper-part" v-if="activeRightTab === 'status'">
                    <deviceState :deviceInfo="deviceInfo" />
                    <droneControlPanel :sn="osdVisible.gateway_sn" :deviceInfo="deviceInfo" :payloads="osdVisible.payloads" />

                </div>

                <!-- 设备运维部分：切换到设备运维时显示6个矩形框 -->

                <div class="maintenance-section" v-if="activeRightTab === 'maintenance'">
                    <!-- <div class="info-item2">
                        <p>机场搜星</p>
                        <p class="data">{{ dataFromBackend.airportSeate }} 个</p>
                    </div>
                    <div class="info-item2">
                        <p>标定状态</p>
                        <p class="data">{{ dataFromBackend.calibStat }}</p>
                    </div>
                    <div class="info-item2">
                        <p>机场存储</p>
                        <p class="data">{{ dataFromBackend.airportStor }} G</p>
                    </div>
                    <div class="info-item2">
                        <p>飞行器存储</p>
                        <p class="data">{{ dataFromBackend.aircraftStor }} G</p>
                    </div>
                    <div class="info-item2">
                        <p>舱内温度</p>
                        <p class="data">{{ dataFromBackend.cabinTemp }} ℃</p>
                    </div>
                    <div class="info-item2">
                        <p>空调状态</p>
                        <p class="data">{{ dataFromBackend.airConStat }}</p>
                    </div> -->
                </div>

                <!-- 标题“参数” -->
                <div v-if="activeRightTab === 'maintenance'">
                    <droneSetting :sn="osdVisible.gateway_sn" :deviceInfo="deviceInfo" />
                    <DockControlPanel :sn="osdVisible.gateway_sn"  :deviceInfo="deviceInfo" @close-control-panel="onCloseControlPanel">
                    </DockControlPanel>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch, onMounted } from 'vue'
import waylinePanel from '/@/components/g-map/showLineAtPlan.vue'
import LivestreamDock from '/@/components/livestream-dock-debug.vue'
import LivestreamOthers from '/@/components/livestream-others-debug.vue'
import { useMyStore } from '/@/store'
import droneControlPanel from '/@/components/devices/drone_control/drone-control.vue'
import deviceState from '/@/components/devices/drone_control/device_state.vue'
import droneSetting from '/@/components/devices/drone_control/dock-setting.vue'
import DockControlPanel from '/@/components/devices/drone_control/dock-control.vue'
import {
  DeviceOsd, DeviceStatus, DockOsd, EGear, EModeCode, GatewayOsd, EDockModeCode,
  NetworkStateQualityEnum, NetworkStateTypeEnum, RainfallEnum, DroneInDockEnum
} from '/@/types/device'
import { useRoute } from 'vue-router'
import { EDeviceTypeName } from '/@/types'
const str: string = '--'
const store = useMyStore()
const route = useRoute()
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
  return store?.state.osdVisible
})
const taskInfo = reactive({
  file_id: '',
  job_name: '',
  execute_time: '',
  file_name: ''

})

const current_sub = ref('') // 当前变电站
onMounted(() => {
  const data = JSON.parse(localStorage.getItem('osdInfo'))
  const data1 = JSON.parse(localStorage.getItem('currentTask'))
  if (data1) {
    const waylineId = store.state.waylineInfo
    waylineId.id = data1.file_id
    store.commit('SET_SELECT_WAYLINE_INFO', waylineId)
    taskInfo.execute_time = data1.execute_time
    taskInfo.job_name = data1.job_name
    taskInfo.file_id = data1.file_id
    taskInfo.file_name = data1.file_name
  }
  store.commit('SET_OSD_VISIBLE_INFO', data)
  //   console.log('sasas', osdVisible.value)
  current_sub.value = history.state.name
//   console.log('传参数据', history.state.name)
})
// 左侧视频流切换标签
const activeTab = ref('load') // 默认选中负载直播标签
const videoStreams = {
  load: 'path-to-load-video-stream.mp4', // 负载直播视频流的路径
  flight: 'path-to-flight-video-stream.mp4' // 飞行飞机视频流的路径
}

watch(() => store?.state.deviceState, data => {
  if (data.currentType === EDeviceTypeName.Gateway && data.gatewayInfo[data.currentSn]) {
    // const coordinate = wgs84togcj02(data.gatewayInfo[data.currentSn].longitude, data.gatewayInfo[data.currentSn].latitude)
    // deviceTsaUpdateHook.moveTo(data.currentSn, coordinate[0], coordinate[1])
    if (osdVisible.value.gateway_sn !== '') {
      deviceInfo.gateway = data.gatewayInfo[osdVisible.value.gateway_sn]
    }
  }

  if (data.currentType === EDeviceTypeName.Aircraft && data.deviceInfo[data.currentSn]) {
    // const coordinate = wgs84togcj02(data.deviceInfo[data.currentSn].longitude, data.deviceInfo[data.currentSn].latitude)
    // deviceTsaUpdateHook.moveTo(data.currentSn, coordinate[0], coordinate[1])
    if (osdVisible.value.sn !== '') {
      deviceInfo.device = data.deviceInfo[osdVisible.value.sn]
    }
  }
  if (data.currentType === EDeviceTypeName.Dock && data.dockInfo[data.currentSn]) {
    // const coordinate = wgs84togcj02(data.dockInfo[data.currentSn].basic_osd?.longitude, data.dockInfo[data.currentSn].basic_osd?.latitude)
    // deviceTsaUpdateHook.initMarker(EDeviceTypeName.Dock, EDeviceTypeName[EDeviceTypeName.Dock], data.currentSn, coordinate[0], coordinate[1])

    // Check if basic_osd and sub_device exist before accessing them
    const dock = data.dockInfo[osdVisible.value.gateway_sn]
    if (dock && dock.basic_osd && dock.basic_osd.sub_device) {
      if (osdVisible.value.is_dock && osdVisible.value.gateway_sn !== '') {
        deviceInfo.dock = dock
        deviceInfo.device = data.deviceInfo[dock.basic_osd.sub_device.device_sn ?? osdVisible.value.sn]
      }
    } else {
      // Handle the case where sub_device is not available, or log an error if needed
      console.warn('Dock or sub_device not found for gateway_sn:', osdVisible.value.gateway_sn)
    }
  }
}, {
  deep: true
})

// 右侧标签切换
const activeRightTab = ref('status') // 默认选中设备状态标签

function switchTab (tab) {
  activeTab.value = tab
}

function switchRightTab (tab) {
  activeRightTab.value = tab
}
// 接入地图==============================

</script>

<style lang="scss" scoped>
.container {
    width: 100%;
    padding: 10px;
    display: flex;
    flex-direction: column;
}

.main-box {
    display: flex;
    height: 100vh;
}

.box-left {
    background: rgba(59, 116, 255, 0.15);
    width: 20%;
    // padding: 10px;
    // border-radius: 15px;
    height: calc(100vh - 120px);
    display: flex;
    flex-direction: column;
    overflow-y: hidden;
    // justify-content: space-between;
}
.nav{
    height: 30px;
    background: url('/@/assets/v4/livestream-nav.png') 100% no-repeat;
    background-size: 100% 100%;
    width: 100%;
    z-index: 3000;
    display: flex;
    align-items: center;
    padding-bottom: 5px;
    // justify-content: center;
    .thumbnail_1{
        // margin-top: 10px;
        // width: 10px;
    }
    // margin: -20px 0 0 1px;
    .text_9 {

        // width: 40px;
        // height: 15px;
        // overflow-wrap: break-word;
        // color: rgba(255, 255, 255, 1);
        // font-size: 24px;
        // letter-spacing: 1.047272801399231px;
        // font-family: Google Sans-Medium;
        // font-weight: 500;
        // text-align: left;
        // white-space: nowrap;
        // line-height: 12px;
        // margin: 7px 0 5px 13px;
        background-image: linear-gradient(
            180deg,
            rgba(255, 255, 255, 1) 0,
            rgba(192, 228, 255, 1) 100%
        );
        width: 200px;
        height: 25px;
        overflow-wrap: break-word;
        color: #fff;
        font-size: 24px;
        font-family: YouSheBiaoTiHei-Regular;
        font-weight: normal;
        text-align: left;
        white-space: nowrap;
        line-height: 24px;
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        margin: 7px 0 5px 7px;
    }
}
.nav-middle{
    background: url('/@/assets/v4/debug_icon2.png') 100% no-repeat;
    background-size: 100% 100%;
    width: 100%;
    height: 40px;
    display: flex;
    justify-content: space-between; /* 两侧元素之间的空间平均分配 */
    align-items: center;
    .nav-title{
        margin-left: 40px;
        margin-bottom: 3px;
        color: #fff;
        font-size: 24px;
        font-weight: bold;
        white-space: nowrap;
        overflow-wrap: break-word;

        background-image: linear-gradient(
            180deg,
            rgba(255, 255, 255, 1) 0,
            rgba(192, 228, 255, 1) 100%
        );
        width: 200px;
        height: 25px;
        font-family: YouSheBiaoTiHei-Regular;
        font-weight: normal;
        text-align: left;
        line-height: 24px;
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;

    }
    // overflow-wrap: break-word;
    // color:#fff ;
    // font-size: 20px;
    // font-family: YouSheBiaoTiHei-Regular;
    // font-weight: normal;
    // text-align: left;
    // white-space: nowrap;
    // line-height: 20px;
    // -webkit-background-clip: text;
    // -webkit-text-fill-color: transparent;
}
.uav-video{
    margin: 10px;
    height: calc(100% - 210px);
}
.video-section {
    // height: 45%;
    // width: 100%;
    // margin: 10px;
    height: 38%;
    // width:ca;
    margin: 5px 10px 0 10px;
    border: 1px solid #51658A;
}
.title{
    height: 30px;
    background: url('../../assets/v4/debug_icon.png')
    100% no-repeat;
    background-size: 100% 100%;
    // width: 100%;
    margin: 15px 10px 10px 10px;
    .text-box {
        background-image: linear-gradient(
            180deg,
            rgba(255, 255, 255, 1) 0,
            rgba(192, 228, 255, 1) 100%
        );
        width: 88px;
        height: 25px;
        overflow-wrap: break-word;
        color: #fff;
        font-size: 20px;
        font-family: YouSheBiaoTiHei-Regular;
        font-weight: normal;
        text-align: left;
        white-space: nowrap;
        line-height: 20px;
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        margin: 1px 0 0 43px;
    }
    // border-bottom:1px solid #ccccccd3;
}
// .title {
//     font-size: 18px;
//     font-weight: bold;
//     margin-bottom: 1px;
//     color: #fff;
//     text-shadow: none;
//     /* 去掉文本阴影 */
//     box-shadow: none;
//     /* 去掉元素周围的阴影 */
// }

.video-player {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.map-section {
    height: 38%;
    // width: 100%;
    // margin-bottom: 8%;
    margin: 5px 10px 0 10px;
    padding: 2px;
    border: 1px solid #51658A;
}

.map {
    width: 100%;
    height: 100%;
    border-radius: 10px;
}

.box-middle {
    background: rgba(59, 116, 255, 0.15);
    // flex: 1;
    width:50%;
    margin-left: 10px;
    // padding: 10px;
    // border-radius: 15px;
    height: calc(100vh - 120px);
    display: flex;
    flex-direction: column;
}

.tab-section {
    display: flex;
    flex-direction: row;
    height: 100%;
    // align-items: flex-start;
    // margin-bottom: 0px;
}
.tab-box{
    position: relative;
    top: -25px;
    display: flex;                  /* 设置父容器为 flex 布局 */
    justify-content: flex-end;
    gap: 10px;                      /* 子元素之间的间距，可以根据需求调整 */
    .tab {
        padding: 5px 8px;
        width: 75px;
        margin-top: 10px;
        background-color: rgba(6,31,74, 0.5);
        border-radius: 5px;
        cursor: pointer;
        color: #fff;
        font-weight: bold;
        margin-right: 10px;
        border: 1px solid rgb(9, 66, 165);
        height: 30px;
        line-height: 20px;
        font-size: 14px;
        white-space: nowrap;
        overflow-wrap: break-word;
    }
}
.tab {
    padding: 5px 8px;
    margin-top: 10px;
    background-color: rgba(6,31,74, 0.5);
    border-radius: 5px;
    cursor: pointer;
    color: #fff;
    font-weight: bold;
    margin-right: 10px;
    border: 1px solid rgb(9, 66, 165);
    height: 30px;
    line-height: 20px;
    font-size: 14px;
    white-space: nowrap;
    overflow-wrap: break-word;
}

.tab.active {
    background-image: linear-gradient(180deg,
            rgba(70, 145, 217, 1) 0,
            rgba(21, 81, 181, 1) 100%);
    border: 1px solid #719fff;
}

.video-section2 {
    height: 800px;
    width: 100%;
}

.video-player {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.task-name {
    margin: 0 10px 10px 10px;
    height: 140px;
    width: calc(100% - 20px);
    // display: flex;
    // justify-content: center;
    // align-items: center;
    border: 1px solid #0056b3;
}

.task-name h3 {
    font-size: 18px;
    font-weight: bold;
    color: #fff;
}

.box-right {
    flex: 1;
    background: rgba(59, 116, 255, 0.15);
    // width: 23%;
    // margin-left: 10px;
    // width: 100%;
    margin-left: 10px;
    padding: 10px 0;
    // border-radius: 15px;
    height: calc(100vh - 120px);
    display: flex;
    flex-direction: column;
    overflow-y: auto;
}

.tab-section {
    display: flex;
    flex-direction: row;
    align-items: flex-start;
    margin-bottom: 10px;
}

.upper-part {
    height: fit-content;
    margin-bottom: 10px;
    background-color: rgba(0, 0, 0, 0.15);
    /* 背景色为半透明黑色 */
    // padding: 15px;
    /* 设置内边距，使内容与边框之间有一定间距 */
    // border-radius: 10px;
    /* 设置圆角 */
    margin-bottom: 15px;
    /* 每个信息项之间的间距 */

}

.lower-part {
    height: 45%;
    margin-bottom: 5%;
    background-color: rgba(0, 0, 0, 0.15);
    /* 背景色为半透明黑色 */
    padding: 15px;
    /* 设置内边距，使内容与边框之间有一定间距 */
    // border-radius: 10px;
    /* 设置圆角 */
    // margin-bottom: 15px;
    /* 每个信息项之间的间距 */
}

.section-title {
    font-size: 18px;
    font-weight: bold;
    color: #fff;
    margin-bottom: 10px;
}

.content {
    background: rgba(255, 255, 255, 0.1);
    padding: 10px;
    border-radius: 8px;
    height: 40vh;
}

.row {
    display: flex;
    justify-content: space-between;
    height: 100%;
}

.column {
    width: 30%;
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
}

.image {
    width: 100%;
    height: auto;
    border-radius: 8px;
}

.image-caption {
    text-align: center;
    color: #fff;
    font-weight: bold;
    margin-top: 10px;
}

.info-item {
    margin-bottom: 2px;
}

.info-item p {
    color: #719fff;
    font-weight: bold;
    margin-bottom: 2px;
    margin-left: 8px;
}

.data {
    font-size: 14px;
    color: #ffff !important;
    font-weight: normal;
    margin-top: 2px;
}

/* 每个信息项的外框样式 */
.info-item {
    background-color: rgba(1, 36, 98, 1);
    /* 背景色为半透明黑色 */
    padding: 15px;
    /* 设置内边距，使内容与边框之间有一定间距 */
    // border-radius: 10px;
    /* 设置圆角 */
    margin-bottom: 15px;
    /* 每个信息项之间的间距 */
    display: flex;
    /* 启用 flexbox */
    flex-direction: column;
    /* 垂直排列内容 */
    justify-content: center;
    /* 垂直居中内容 */
    align-items: center;
    /* 水平居中内容 */
    text-align: center;
    /* 确保文字居中 */
}

/* 为每个列中的项目设置适当的间距 */
.column {
    width: 32%;
    /* 每列占据大约1/3的宽度 */
    display: flex;
    flex-direction: column;
}

/* 每个信息项（标题 + 数据）之间的间距 */
.info-item {
    margin-bottom: 2px;
    /* 设置每个信息项之间的间距 */
}

//第二个页面===============================================================================================================
.maintenance-section {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    /* 设置2列，每列占1份宽度 */
    gap: 20px;
    /* 设置每个矩形框之间的间距 */
}

.info-item2 {
    background-color: rgba(0, 0, 0, 0.15);
    /* 背景色为半透明黑色 */
    padding: 10px;
    // border-radius: 10px;
    margin-bottom: 0px !important;
    text-align: center;
    color: white;
}

.info-item2 p {
    color: #719fff;
    font-weight: bold;
    margin-bottom: 2px;
    margin-left: 8px;
}

/* 标题样式 */
.section-title2 {
    margin-top: 20px;
    font-size: 18px;
    font-weight: bold;
    color: white;
    margin-bottom: 10px;
    /* 给标题和下面的内容间距 */
}

/* 标题样式 */
.section-title {
    margin-top: 20px;
    font-size: 18px;
    font-weight: bold;
    color: white;
    margin-bottom: 2px;
    /* 给标题和下面的内容间距 */
}

/* 控制区域：飞行器夜航灯 */
.parameter-controls {
    display: flex;
    /* 在水平方向上分配空间 */
    align-items: center;
    /* 垂直居中对齐 */
    width: 100%;
    /* 确保宽度填满父容器 */
}

/* 左半部分：飞行器夜航灯和开关 */
.left-side {
    flex: 1;
    /* 左侧部分占据一半的宽度 */
    display: flex;
    // width: 130px;
    color: white;
    justify-content: flex-start;
}

.left-side p {
    margin-right: 2px;
    /* 为标签和开关之间留点间距 */
}

/* 右半部分：限高和输入框 */
.right-side {
    flex: 1;
    /* 右侧部分占据另一半的宽度 */
    display: flex;
    color: white;
    // width: 50px;
    justify-content: flex-start;
    // margin-right: 0px;
}

.right-side p {
    margin-right: 2px;
    /* 为标签和输入框之间留点间距 */
}

/* 每个参数项 */
.parameter-item {
    display: flex;
    align-items: center;
    /* 垂直居中对齐 */
    justify-content: flex-start;
    /* 水平左对齐 */
    padding: 5px;
    /* 给每个元素一点内边距 */
}

.parameter-item2 {
    display: flex;
    align-items: center;

    /* 垂直居中对齐 */
    justify-content: flex-start;
    /* 水平左对齐 */
    padding: 5px;
    /* 给每个元素一点内边距 */
    // margin-left: 20px;
}

/* 控件文本的样式 */
.parameter-item2 p {
    margin-bottom: 0px;

}

/* 开关样式 */
.switch {
    position: relative;
    display: inline-block;
    width: 34px;
    height: 20px;
    margin-left: 5px;
}

.switch input {
    opacity: 0;
    width: 0;
    height: 0;
}

.slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #ccc;
    transition: 0.4s;
    border-radius: 50px;
}

.slider:before {
    position: absolute;
    content: "";
    height: 12px;
    width: 12px;
    border-radius: 50%;
    left: 4px;
    bottom: 4px;
    background-color: white;
    transition: 0.4s;
}

input:checked+.slider {
    background-color: #2061d5;
    /* 开关启用时背景颜色 */
}

input:checked+.slider:before {
    transform: translateX(14px);
    /* 开关启用时滑块移动 */
}

/* 输入框样式 */
.input-box {
    padding: 5px 10px;
    margin-left: 2px;
    /* 给输入框和文本之间加点间距 */
    border: 1px solid #012b78;
    border-radius: 5px;
    background-color: rgba(0, 0, 0, 0.15);
    color: rgb(255, 255, 255);
    // width: 100px;
    width: 150px;
    /* 设置输入框的宽度 */
    text-align: center;
    box-sizing: border-box;
    /* 包括内边距在内的总宽度 */
}

/* 容器使用 Flexbox 布局，使标题和开关按钮处于同一行 */
.remote-debugging-header {
    display: flex;
    justify-content: space-between;
    /* 左右分配空间 */
    align-items: center;
    /* 垂直居中对齐 */
}

//远程调试部分============================================================================
.last-box {
    padding: 20px 15px;
    height: fit-content;
    width: 100%;
    display: flex;
    justify-content: space-between;
    /* 将子元素分布在两侧 */
    align-items: center;
    /* 垂直居中对齐 */
    // border-bottom: 1px solid rgba(0, 112, 209, 0.566);
}

.item-title {
    color: azure;
    font-size: 18px;
}

.title-box {
    margin-left: 10px;
    background-color: rgba(0, 64, 147, 1);
    color: #ffffff;
    // border: 1px solid #D2CBC3;
    padding: 10px;
    font-size: 16px;
}

.title-box:hover {
    background-color: #0056b3;
}

.item-describe {
    color: #8CB0D3;
    font-size: 16px;
}

.info-item3 {
    background-color: rgba(0, 0, 0, 0.15);
    padding: 10px;
    // border-radius: 10px;
    margin-bottom: 0px !important;
    // text-align: center;
    // color: white;
}

//===============================================================================================================
.operation {
    height: 100%;
}

.info-list {
    list-style-type: none;
    padding: 0;
    color: #fff;
}

.info-list li {
    margin: 5px 0;
    font-weight: bold;
}

.operation {
    height: 100%;
}
</style>
