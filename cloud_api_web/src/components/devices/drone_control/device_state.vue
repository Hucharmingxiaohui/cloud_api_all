<template>
    <div v-if="true">

       <!-- 机场信息 -->
       <div class="content_title">机场状态: {{ EDockModeCode[deviceInfo.dock.basic_osd?.mode_code] }}</div>
      <div  class="device-content" style="margin-bottom: 20px;">
        <!-- <div class="items">
            <span :style="deviceInfo.dock.basic_osd?.mode_code === EDockModeCode.Disconnected ? 'color: red; font-weight: 700;': 'color: rgb(25,190,107)'"> 2222{{ EDockModeCode[deviceInfo.dock.basic_osd?.mode_code] }}</span>
          </div> -->
        <div class="left">
          <div class="left-box">
            <p class="caption">机场</p>
            <img src="../../../assets/v4/dock.png" alt="描述" class="responsive-image"/>
            <p class="caption">{{ osdVisible.model }}</p>
          </div>
        </div>
        <div class="right">
          <div class="items">
              <div class="item" >
                <span >{{ deviceInfo.dock.basic_osd?.network_state?.rate }} kb/s</span>
                <span style="color: #717EE2;">
                  网络速率
                </span>
              </div>
              <div class="item" >
                <span>{{ deviceInfo.dock.link_osd?.media_file_detail?.remain_upload ?? 0 }}</span>
                <span style="color: #717EE2;">媒体文件待上传数量</span>
              </div>
          </div>
          <div class="items">
              <div class="item" >
                <span >{{ (deviceInfo.dock.basic_osd?.wind_speed ?? str) + ' m/s'}}</span>
                <span style="color: #717EE2;">风速</span>
              </div>
              <div class="item">
                <span>{{ RainfallEnum[deviceInfo.dock.basic_osd?.rainfall]??'无' }}</span>
                <span style="color: #717EE2;">降雨量</span>
              </div>
          </div>
          <div class="items">
              <div class="item">
                <span> {{ deviceInfo.dock.basic_osd?.environment_temperature }}°C</span>
                <span style="color: #717EE2;">环境温度</span>
              </div>
              <div class="item">
                <span >{{ deviceInfo.dock.basic_osd?.temperature }} °C</span>
                <span style="color: #717EE2;">舱内温度</span>
              </div>
          </div>
          <div class="items">
              <div class="item">
                <span>{{ deviceInfo.dock.basic_osd?.humidity??'无' }}</span>
                <span style="color: #717EE2;">舱内湿度</span>
              </div>
              <div class="item">
                <span class="ml10">{{ deviceInfo.dock.basic_osd?.drone_in_dock? '是':'否' }}</span>
                <span style="color: #717EE2;">飞行器是否在舱</span>
              </div>
          </div>
        </div>
      </div>
      <div class="content_title">无人机状态: {{   !deviceInfo.device ? EModeCode[EModeCode.Disconnected] : EModeCode[deviceInfo.device?.mode_code] }}</div>
      <!-- 机场无人机信息 -->
      <div  class="device-content">
        <div class="left">
          <div class="left-box">
            <p class="caption">无人机</p>
            <img src="../../../assets/v3/icon/uav-fly.png" alt="描述" class="responsive-image"/>
            <p class="caption">{{ osdVisible.model }}</p>
          </div>
        </div>
        <div class="right">
          <div class="items">
              <div class="item">
                <span >{{ deviceInfo.device && deviceInfo.device.battery.capacity_percent !== str ? deviceInfo.device?.battery.capacity_percent + ' %' : str }}</span>
                <span style="color: #717EE2;">无人机剩余电量</span>
              </div>
              <div class="item">
                <span >{{ deviceInfo.device ? deviceInfo.device.position_state.rtk_number : str }}</span>
                <span style="color: #717EE2;">RTK 搜星数量</span>
              </div>
          </div>
          <div class="items">
            <div class="item">
                <span> {{ !deviceInfo.device || deviceInfo.device.height === str ? str : deviceInfo.device?.height.toFixed(2) + ' m'}}</span>
                <span style="color: #717EE2;">ASL</span>
            </div>
            <div class="item">
                <span >{{ !deviceInfo.device || deviceInfo.device.elevation === str ? str : deviceInfo.device?.elevation.toFixed(2) + ' m' }}</span>
                <span style="color: #717EE2;">ALT</span>
              </div>
          </div>
          <div class="items">
              <div class="item">
                <span class="ml10">{{ !deviceInfo.device || deviceInfo.device.wind_speed === str ? str : (deviceInfo.device?.wind_speed / 10).toFixed(2) + ' m/s'}}</span>
                <span style="color: #717EE2;">风速</span>
              </div>
              <div class="item">
                <span class="ml10">{{ !deviceInfo.device || deviceInfo.device.home_distance === str ? str : deviceInfo.device?.home_distance.toFixed(2) + ' m' }}</span>
                <span style="color: #717EE2;">离起飞点的距离</span>
              </div>
          </div>
          <div class="items">
              <div class="item">
                <span class="ml10">{{ !deviceInfo.device || deviceInfo.device?.horizontal_speed === str ? str : deviceInfo.device?.horizontal_speed.toFixed(2) + ' m/s'}}</span>
                <span style="color: #717EE2;">水平速度</span>
              </div>
              <div class="item">
                <span class="ml10">{{ !deviceInfo.device || deviceInfo.device.vertical_speed === str ? str : deviceInfo.device?.vertical_speed.toFixed(2) + ' m/s'}}</span>
                <span style="color: #717EE2;">垂直速度</span>
              </div>
          </div>
        </div>
      </div>
      <!-- <DockControlPanel v-if="true" :sn="osdVisible.gateway_sn"  :deviceInfo="deviceInfo" @close-control-panel="onCloseControlPanel">
      </DockControlPanel> -->
      <!-- 加载控制模块 -->
    </div>
</template>
<script setup lang="ts">
import { defineProps, defineEmits, ref, watch, computed } from 'vue'
import { DeviceInfoType, EDockModeCode, EGear, EModeCode, NetworkStateTypeEnum, RainfallEnum, NetworkStateQualityEnum } from '/@/types/device'
import { useMyStore } from '/@/store'
import {
  BorderOutlined, LineOutlined, CloseOutlined, ControlOutlined, TrademarkOutlined, ArrowDownOutlined,
  ThunderboltOutlined, SignalFilled, GlobalOutlined, HistoryOutlined, CloudUploadOutlined, RocketOutlined,
  FieldTimeOutlined, CloudOutlined, CloudFilled, FolderOpenOutlined, RobotFilled, ArrowUpOutlined, CarryOutOutlined, UpOutlined, DownOutlined
} from '@ant-design/icons-vue'
// import DockControlPanel from '/@/components/g-map/DockControlPanel.vue'
import { useDockControl } from '/@/components/g-map/use-dock-control'
import { useConnectMqtt } from '/@/components/g-map/use-connect-mqtt'
import dockControlPanel from './dock-control.vue'
import droneControlPanel from './drone-control.vue'
import pilotControlPanel from './pilot-control.vue'
import controlPanel from '/@/components/control/ControlDegree.vue'
import UAVControlPanel from '/@/components/control/ControlUAV.vue'
const props = defineProps<{
  deviceInfo: DeviceInfoType,
}>()

const store = useMyStore()
const osdVisible = computed(() => {
  return store.state.osdVisible
})
const str: string = '--'
const qualityStyle = computed(() => {
  if (props.deviceInfo.dock.basic_osd?.network_state?.type === NetworkStateTypeEnum.ETHERNET ||
        (props.deviceInfo.dock.basic_osd?.network_state?.quality || 0) > NetworkStateQualityEnum.FAIR) {
    return 'color: #00ee8b'
  }
  if ((props.deviceInfo.dock.basic_osd?.network_state?.quality || 0) === NetworkStateQualityEnum.FAIR) {
    return 'color: yellow'
  }
  return 'color: red'
})
const elTabCurrentName = ref('UAVRemoteControl')
const collapsed = ref(false)
function collapseStatePanel () {
  collapsed.value = !collapsed.value
}
// 接收到值
const deviceInfo = ref(props.deviceInfo)
// dock 控制面板
const {
  dockControlPanelVisible,
  setDockControlPanelVisible,
  onCloseControlPanel,
} = useDockControl()

// 连接或断开drc
useConnectMqtt()
</script >

<style lang="scss" scoped>
.content_title{
  background:rgba(59, 116, 255, 0.15);
  width: 100%;
  height: 35px;
  margin: 5px 0;
  padding-left: 10px;
  line-height: 35px;
  color: #ffffff;
}
.device-content{
  display: flex;
  width: 100%;
  height: fit-content;
  min-height: 200px;
  background:rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // border: 1px solid #719fff;
  .left{
        width: 120px; /* 左侧宽度 */
        height: 280px;
        padding: 5px;
        color: #ffffff;
        .left-box{
          background-color: rgba(0, 0, 0, 0.15);
          display: flex;
          flex-direction: column; /* 垂直排列 */
          align-items: center; /* 水平居中对齐 */
          justify-content: center;
          height: 100%;
        }
        // border-right: 1px solid #719fff;
        .responsive-image {
            margin: 0 3px 0 3px;
            width: 100%; /* 自适应宽度 */
            height: auto; /* 保持纵横比 */
            aspect-ratio: 1; /* 宽高比为1:1，保持正方形 */
            object-fit: cover; /* 裁剪内容以填充盒子 */
        }
        .caption {
        margin-top: 8px; /* 与图片之间的间距 */
        text-align: center; /* 文本居中对齐 */
        }

    }
    .right{
        width: calc(100% - 120px); /* 右侧宽度 */
        // background-color: #e0e0e0; /* 背景颜色 */
        // padding: 10px;
        // padding-bottom: 20px;
        height: fit-content;
        .items{
            display: grid;
            grid-template-columns: repeat(2, 1fr); /* 两列布局 */
            gap: 10px;
            padding: 5px;
            .item{
                // margin-left: 0;
                // width: 67px;
                height: 60px;
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: center;
                width: 100%;
                overflow: hidden;
                display: flex;
                // border: 1px solid #719fff;
                background-color: rgba(0, 0, 0, 0.15);
                // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
                // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
                color: #ffffff;
                span:nth-child(2){
                  margin-left: 10px;
                }
            }
        }
    }
}
.dock-title{
  display: flex;
  justify-content: space-between; /* 子元素分布在两侧 */
  align-items: center; /* 垂直居中对齐 */
  padding: 5px;
  // background: linear-gradient(
  //   to bottom,
  //   rgba(59, 116, 255, 0.6) 0%,
  //   rgba(59, 116, 255, 0) 50%,
  //   rgba(59, 116, 255, 0.6) 100%
  // );
  // border: 1px solid #719fff;
  border-bottom: none;
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  color: #ffffff;
}
.circle {
  border-radius: 50%;
  width: 10px;
  height: 10px;
}

.tool-box{
  margin-bottom: 15px;
    background: linear-gradient(
      270deg,
      rgba(27, 83, 181, 0) 0%,
      #2061d5 100%
    );
    border-radius: 100px 0px 0px 100px;
    img {
      width: 30px;
      height: 30px;
      filter: brightness(1.8);
    }
    .tool-name {
      position: relative;
      top: 2px;
      left: 10px;
      font-family: YouSheBiaoTiHei;
      font-size: 17px;
      color: #ffffff;
      line-height: 21px;
      text-align: left;
      font-style: normal;

      letter-spacing: 1px;

    }
}
//tab样式
.demo-tabs :deep(.el-tabs__item) {
  color: #ffffff;
  font-weight: 600;
}
.demo-tabs :deep(.el-tabs__header .el-tabs__item.is-active ){
  background-color: #0f327f; /* 选中标签背景颜色 */
  color: #fff; /* 选中标签文字颜色 */
  border-radius: 4px; /* 选中标签圆角 */
}
</style>
