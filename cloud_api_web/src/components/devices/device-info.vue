<template>
    <div class="device-content">
        <div class="left">
            <img src="../../assets/v3/icon/uav-fly.png" alt="描述" class="responsive-image"/>
            <p class="caption">{{ osdVisible.model }}</p>
        </div>
        <div class="right">
            <div class="items">
                <div class="item">
                    <span>HD</span>
                    <span>{{ deviceInfo.gateway?.transmission_signal_quality }}</span>
                </div>
                <div class="item">
                    <span><ThunderboltOutlined class="fz14"/></span>
                    <span>{{ deviceInfo.gateway && deviceInfo.gateway.capacity_percent !== str ? deviceInfo.gateway?.capacity_percent + ' %' : deviceInfo.gateway?.capacity_percent }}</span>
                </div>
                <div class="item">
                    <span><ThunderboltOutlined class="fz14"/></span>
                    <span>{{ deviceInfo.device.battery.capacity_percent !== str ? deviceInfo.device.battery.capacity_percent + ' %' : deviceInfo.device.battery.capacity_percent }}</span>
                </div>
            </div>
            <div class="items">
                <div class="item">
                    <span>Fixed</span>
                    <span class="ml10 circle" :style="deviceInfo.device.position_state.is_fixed === 1 ? 'backgroud: rgb(25,190,107);' : ' background: red;'"></span>
                </div>
                <div class="item">
                    <span>GPS</span>
                    <span class="ml10">{{ deviceInfo.device.position_state.gps_number }}</span>
                </div>
                <div class="item">
                    <span><TrademarkOutlined class="fz14"/></span>
                    <span class="ml10">{{ deviceInfo.device.position_state.rtk_number }}</span>
                </div>
            </div>
            <div class="items">
                <div class="item">
                    <span><ControlOutlined class="fz16" /></span>
                    <span class="ml10">{{ EGear[deviceInfo.device.gear] }}</span>
                </div>
                <div class="item">
                    <span>ASL</span>
                    <span class="ml10">{{ deviceInfo.device.height === str ? str : deviceInfo.device.height.toFixed(2) + ' m'}}</span>
                </div>
                <div class="item">
                    <span>ALT</span>
                    <span class="ml10">{{ deviceInfo.device.elevation === str ? str : deviceInfo.device.elevation.toFixed(2) + ' m' }}</span>
                </div>
                <div class="item">
                    <span>H</span>
                    <span class="ml10">{{ deviceInfo.device.home_distance === str ? str : deviceInfo.device.home_distance.toFixed(2) + ' m' }}</span>
                </div>
            </div>
            <div class="items">
                <div class="item">
                    <span>H.S</span>
                    <span class="ml10">{{ deviceInfo.device.horizontal_speed === str ? str : deviceInfo.device.horizontal_speed.toFixed(2) + ' m/s'}}</span>
                </div>
                <div class="item">
                    <span>V.S</span>
                    <span class="ml10">{{ deviceInfo.device.vertical_speed === str ? str : deviceInfo.device.vertical_speed.toFixed(2) + ' m/s'}}</span>
                </div>
                <div class="item">
                    <span>ALT</span>
                    <span class="ml10">{{ deviceInfo.device.elevation === str ? str : deviceInfo.device.elevation.toFixed(2) + ' m' }}</span>
                </div>
                <div class="item">
                    <span>W.S</span>
                    <span class="ml10">{{ deviceInfo.device.wind_speed === str ? str : (deviceInfo.device.wind_speed / 10).toFixed(2) + ' m/s'}}</span>
                </div>
            </div>
        </div>
    </div>
</template>
<script setup lang="ts">
import { computed, defineComponent, onMounted, reactive, ref, watch } from 'vue'
import {
  DeviceOsd, DeviceStatus, DockOsd, EGear, EModeCode, GatewayOsd, EDockModeCode,
  NetworkStateQualityEnum, NetworkStateTypeEnum, RainfallEnum, DroneInDockEnum
} from '/@/types/device'
import { deviceTsaUpdate } from '/@/hooks/use-g-map-tsa'
import { useMyStore } from '/@/store'
import { gcj02towgs84, wgs84togcj02 } from '/@/vendors/coordtransform'
import { EDeviceTypeName } from '/@/types'
import {
  BorderOutlined, LineOutlined, CloseOutlined, ControlOutlined, TrademarkOutlined, ArrowDownOutlined,
  ThunderboltOutlined, SignalFilled, GlobalOutlined, HistoryOutlined, CloudUploadOutlined, RocketOutlined,
  FieldTimeOutlined, CloudOutlined, CloudFilled, FolderOpenOutlined, RobotFilled, ArrowUpOutlined, CarryOutOutlined
} from '@ant-design/icons-vue'
const deviceTsaUpdateHook = deviceTsaUpdate()
const store = useMyStore()
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
onMounted(() => {
  console.log(deviceInfo.gateway)
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
</script>
<style scoped lang="scss">
.device-content{
  display: flex;
  .left{
        width: 150px; /* 左侧宽度 */
        background-color: #545454; /* 背景颜色 */
        padding: 10px;
        height: 200px;
        display: flex;
        flex-direction: column; /* 垂直排列 */
        align-items: center; /* 水平居中对齐 */
        .responsive-image {
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
        width: 480px; /* 右侧宽度 */
        // background-color: #e0e0e0; /* 背景颜色 */
        // padding: 10px;
        height: 200px;
        .items{
            display: grid;
            grid-template-columns: repeat(4, 1fr); /* 两列布局 */
            gap: 5px;
            padding: 10px;
            .item{
                // margin-left: 0;
                // width: 67px;
                text-align: left;
                overflow: hidden;
                display: flex;
                align-items: center;
                span:nth-child(2){
                  margin-left: 10px;
                }
            }
        }
    }
}

.circle {
  border-radius: 50%;
  width: 10px;
  height: 10px;
}
</style>
