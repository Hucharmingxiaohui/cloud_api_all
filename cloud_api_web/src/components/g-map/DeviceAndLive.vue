<template>
    <div style="width: 100%;height: 100%;">
      <div  class="osd-panel fz12"  v-if="osdVisible && osdVisible.visible " >
        <div class=" title-bg" style="border-bottom: 1px solid #515151; height: 37px;">
          <div class="thumbnail_1"></div>
          <div class="box_text">{{ osdVisible.gateway_callsign }}</div>
          <span> </span>
        </div>
        <span><a style="color: white; position: absolute; top: 5px; right: 5px;" @click="() => osdVisible.visible = false"><CloseOutlined /></a></span>
          <!-- 机场 -->
        <div class ="flex-display" style="border-bottom: 1px solid #515151;  padding: 10px; margin-right: 10px;">
          <div class="flex-column flex-align-stretch flex-justify-center device-bg" style="width: 140px; background: #2d2d2d; border: 1px solid #323D56;">
            <a-tooltip :title="osdVisible.model">
              <div class="flex-column  flex-align-center flex-justify-center " style="width: 100%;">
                <span>
                  <!-- <RobotFilled style="font-size: 48px;"/> -->
                  <img class="thumbnail_1" referrerpolicy="no-referrer"
                  src="https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG175308f62f1d0541bffda14c08e3c9d8.png" />
                </span>
                <span class="text-bg" style="height: 60px; width: 100%;">
                  Dock
                </span>
              </div>
            </a-tooltip>
          </div>
          <div class="osd flex-1" style="flex: 1 ; background-color: rgba(11,23,42, 1); ">
            <div style="margin-left: 10px;margin-top: 0; padding: 10px; border: 1px solid #323D56; ">
              <a-row style="margin-bottom: 5px;">
                <a-col span="16" :style="deviceInfo.dock.basic_osd?.mode_code === EDockModeCode.Disconnected ? 'color: red; font-weight: 700;': 'color: rgb(25,190,107)'">
                  {{ EDockModeCode[deviceInfo.dock.basic_osd?.mode_code] }}</a-col>
              </a-row>
              <a-row style="margin-bottom: 5px;">
                <a-col span="12">
                  <a-tooltip title="网络状态">
                    <span :style="qualityStyle">
                      <span v-if="deviceInfo.dock.basic_osd?.network_state?.type === NetworkStateTypeEnum.FOUR_G"><SignalFilled /></span>
                      <span v-else><GlobalOutlined /></span>
                    </span>
                    <span class="ml10" >{{ deviceInfo.dock.basic_osd?.network_state?.rate }} kb/s</span>
                  </a-tooltip>
                </a-col>
                <!-- <a-col span="6">
                  <a-tooltip title="The total number of times the dock has performed missions.">
                    <span><CarryOutOutlined /></span>
                    <span class="ml10" >{{ deviceInfo.dock.work_osd?.job_number }} </span>
                  </a-tooltip>
                </a-col> -->
                <a-col span="12">
                  <a-tooltip title="待上传媒体文件">
                    <span><CloudUploadOutlined class="fz14"/></span>
                    <span class="ml10">{{ deviceInfo.dock.link_osd?.media_file_detail?.remain_upload }}</span>
                  </a-tooltip>
                </a-col>
              </a-row>
              <a-row style="margin-bottom: 5px;">
                <a-col span="12">
                  <a-tooltip>
                    <template #title>
                      <p>total: {{ deviceInfo.dock.basic_osd?.storage?.total }}</p>
                      <p>used: {{ deviceInfo.dock.basic_osd?.storage?.used  }}</p>
                    </template>
                    <span><FolderOpenOutlined /></span>
                    <span class="ml10" v-if="deviceInfo.dock.basic_osd?.storage?.total > 0">
                      <a-progress type="circle" :width="20" :percent="deviceInfo.dock.basic_osd?.storage?.used * 100/ deviceInfo.dock.basic_osd?.storage?.total"
                        :strokeWidth="20" :showInfo="false" :strokeColor="deviceInfo.dock.basic_osd?.storage?.used * 100 / deviceInfo.dock.basic_osd?.storage?.total > 80 ? 'red' : '#00ee8b' "/>
                    </span>
                  </a-tooltip>
                </a-col>
                <a-col span="12">
                  <a-tooltip title="风速">
                    <span>W.S</span>
                    <span class="ml10">{{ (deviceInfo.dock.basic_osd?.wind_speed ?? str) + ' m/s'}}</span>
                  </a-tooltip>
                </a-col>
              </a-row>
              <a-row style="margin-bottom: 5px;">
                <a-col span="12">
                  <a-tooltip title="环境温度">
                    <span>°C</span>
                    <span class="ml10">{{ deviceInfo.dock.basic_osd?.environment_temperature }}</span>
                  </a-tooltip>
                </a-col>
                <a-col span="12">
                  <a-tooltip title="机场温度">
                    <span>°C</span>
                    <span class="ml10">{{ deviceInfo.dock.basic_osd?.temperature }}</span>
                  </a-tooltip>
                </a-col>
              </a-row>
              <a-row style="margin-bottom: 5px;">
                <a-col span="12">
                  <a-tooltip title="降雨量">
                    <span>🌧</span>
                    <span class="ml10">{{ RainfallEnum[deviceInfo.dock.basic_osd?.rainfall] }}</span>
                  </a-tooltip>
                </a-col>
                <a-col span="12">
                  <a-tooltip title="机场湿度">
                    <span>💦</span>
                    <span class="ml10">{{ deviceInfo.dock.basic_osd?.humidity }}</span>
                  </a-tooltip>
                </a-col>
              </a-row>
              <a-row>
                <a-col span="12">
                  <a-tooltip title="舱内是否有机场">
                    <span><RocketOutlined /></span>
                    <span class="ml10">{{ deviceInfo.dock.basic_osd?.drone_in_dock? "是":"否" }}</span>
                  </a-tooltip>
                </a-col>
              </a-row>
            </div>

              <!-- 机场控制面板 -->
              <DockControlPanel v-if="dockControlPanelVisible" :sn="osdVisible.gateway_sn"  :deviceInfo="deviceInfo" @close-control-panel="onCloseControlPanel">
              </DockControlPanel>
          </div>
        </div>
        <!--  飞机-->
        <div class ="flex-display" style="padding: 10px; margin-right: 10px;">
          <div class="flex-column flex-align-stretch flex-justify-center device-bg" style="width: 140px; background: #2d2d2d; border: 1px solid #323D56;">
            <a-tooltip :title="osdVisible.model">
              <div style="width: 100%;" class="flex-column flex-align-center flex-justify-center">
                <span>
                  <!-- <a-image :src="M30" :preview="false"/> -->
                  <img class="thumbnail_1" referrerpolicy="no-referrer"
                  src="https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG25f9ea5d482babaf860bd065484ce952.png" />
                </span>
                <span class="text-bg" style="height: 60px; width: 100%;">M30</span>
              </div>
            </a-tooltip>
          </div>
          <div class="osd flex-1" style="flex: 1 ; background-color: rgba(11,23,42, 1);">
            <div style="margin-left: 10px;margin-top: 0; padding: 10px; border: 1px solid #323D56; ">
              <a-row style="margin-bottom: 5px;">
                <a-col span="16" :style="!deviceInfo.device || deviceInfo.device?.mode_code === EModeCode.Disconnected ? 'color: red; font-weight: 700;': 'color: rgb(25,190,107)'">
                  {{ !deviceInfo.device ? EModeCode[EModeCode.Disconnected] : EModeCode[deviceInfo.device?.mode_code] }}</a-col>
              </a-row >
              <a-row style="margin-bottom: 5px;">
                <a-col span="6">
                  <a-tooltip title="Upward Quality">
                    <span><SignalFilled /><ArrowUpOutlined style="font-size: 9px; vertical-align: top;" /></span>
                    <span class="ml10">{{ deviceInfo.dock.link_osd?.sdr?.up_quality }}</span>
                  </a-tooltip>
                </a-col>
                <a-col span="6">
                  <a-tooltip title="Downward Quality">
                    <span><SignalFilled /><ArrowDownOutlined style="font-size: 9px; vertical-align: top;" /></span>
                    <span class="ml10">{{ deviceInfo.dock.link_osd?.sdr?.down_quality }}</span>
                  </a-tooltip>
                </a-col>
                <a-col span="6">
                  <a-tooltip title="Drone Battery Level">
                    <span><ThunderboltOutlined class="fz14"/></span>
                    <span class="ml10">{{ deviceInfo.device && deviceInfo.device.battery.capacity_percent !== str ? deviceInfo.device?.battery.capacity_percent + ' %' : str }}</span>
                  </a-tooltip>
                </a-col>
                <a-col span="6">
                  <a-tooltip>
                    <template #title>
                      <p>total: {{ deviceInfo.device?.storage?.total }}</p>
                      <p>used: {{ deviceInfo.device?.storage?.used  }}</p>
                    </template>
                    <span><FolderOpenOutlined /></span>
                    <span class="ml10" v-if="deviceInfo.device?.storage?.total > 0">
                      <a-progress type="circle" :width="20" :percent="deviceInfo.device?.storage?.used * 100/ deviceInfo.device?.storage?.total"
                        :strokeWidth="20" :showInfo="false" :strokeColor="deviceInfo.device?.storage?.used * 100 / deviceInfo.device?.storage?.total > 80 ? 'red' : '#00ee8b' "/>
                    </span>
                  </a-tooltip>
                </a-col>
              </a-row>
              <a-row style="margin-bottom: 5px;">
                <a-tooltip title="RTK Fixed">
                  <a-col span="6" class="flex-row flex-align-center flex-justify-start">
                    <span>Fixed</span>
                    <span class="ml10 circle" :style="deviceInfo.device?.position_state.is_fixed === 1 ? 'backgroud: rgb(25,190,107);' : ' background: red;'"></span>
                  </a-col>
                </a-tooltip>
                <a-col span="6">
                  <a-tooltip title="GPS">
                    <span>GPS</span>
                    <span class="ml10">{{ deviceInfo.device ? deviceInfo.device.position_state.gps_number : str }}</span>
                  </a-tooltip>
                </a-col>
                <a-col span="6">
                  <a-tooltip title="RTK">
                    <span><TrademarkOutlined class="fz14"/></span>
                    <span class="ml10">{{ deviceInfo.device ? deviceInfo.device.position_state.rtk_number : str }}</span>
                  </a-tooltip>
                </a-col>
              </a-row>
              <a-row style="margin-bottom: 5px;">
                <a-col span="6">
                  <a-tooltip title="Flight Mode">
                    <span><ControlOutlined class="fz16" /></span>
                    <span class="ml10">{{ deviceInfo.device ? EGear[deviceInfo.device?.gear] : str }}</span>
                  </a-tooltip>
                </a-col>
                <a-col span="6">
                  <a-tooltip title="Altitude above sea level">
                    <span>ASL</span>
                    <span class="ml10">{{ !deviceInfo.device || deviceInfo.device.height === str ? str : deviceInfo.device?.height.toFixed(2) + ' m'}}</span>
                  </a-tooltip>
                </a-col>
                <a-col span="6">
                  <a-tooltip title="Altitude above takeoff level">
                    <span>ALT</span>
                    <span class="ml10">{{ !deviceInfo.device || deviceInfo.device.elevation === str ? str : deviceInfo.device?.elevation.toFixed(2) + ' m' }}</span>
                  </a-tooltip>
                </a-col>
                <a-col span="6">
                  <a-tooltip title="Distance to Home Point">
                    <span style="border: 1px solid; border-radius: 50%; width: 18px; height: 18px; line-height: 15px; text-align: center;  display: block; float: left;" >H</span>
                    <span class="ml10">{{ !deviceInfo.device || deviceInfo.device.home_distance === str ? str : deviceInfo.device?.home_distance.toFixed(2) + ' m' }}</span>
                  </a-tooltip>
                </a-col>
              </a-row>
              <a-row style="margin-bottom: 5px;">
                <a-col span="6">
                  <a-tooltip title="Horizontal Speed">
                    <span>H.S</span>
                    <span class="ml10">{{ !deviceInfo.device || deviceInfo.device?.horizontal_speed === str ? str : deviceInfo.device?.horizontal_speed.toFixed(2) + ' m/s'}}</span>
                  </a-tooltip>
                </a-col>
                <a-col span="6">
                  <a-tooltip title="Vertical Speed">
                    <span>V.S</span>
                    <span class="ml10">{{ !deviceInfo.device || deviceInfo.device.vertical_speed === str ? str : deviceInfo.device?.vertical_speed.toFixed(2) + ' m/s'}}</span>
                  </a-tooltip>
                </a-col>
                <a-col span="6">
                  <a-tooltip title="Wind Speed">
                    <span>W.S</span>
                    <span class="ml10">{{ !deviceInfo.device || deviceInfo.device.wind_speed === str ? str : (deviceInfo.device?.wind_speed / 10).toFixed(2) + ' m/s'}}</span>
                  </a-tooltip>
                </a-col>
              </a-row>
            </div>

          </div>
        </div>
        <div class="battery-slide" v-if="deviceInfo.device && deviceInfo.device.battery.remain_flight_time !== 0" style="border: 1px solid red">
          <div style="background: #535759;" class="width-100"></div>
          <div class="capacity-percent" :style="{ width: deviceInfo.device.battery.capacity_percent + '%'}"></div>
          <div class="return-home" :style="{ width: deviceInfo.device.battery.return_home_power + '%'}"></div>
          <div class="landing" :style="{ width: deviceInfo.device.battery.landing_power + '%'}"></div>
          <div class="white-point" :style="{ left: deviceInfo.device.battery.landing_power + '%'}"></div>
          <div class="battery" :style="{ left: deviceInfo.device.battery.capacity_percent + '%' }">
            {{ Math.floor(deviceInfo.device.battery.remain_flight_time / 60) }}:
            {{ 10 > (deviceInfo.device.battery.remain_flight_time % 60) ? '0' : ''}}{{deviceInfo.device.battery.remain_flight_time % 60 }}
          </div>
        </div>
      </div>
    </div>
</template>
<script lang="ts">
import {
  DeviceOsd, DeviceStatus, DockOsd, EGear, EModeCode, GatewayOsd, EDockModeCode,
  NetworkStateQualityEnum, NetworkStateTypeEnum, RainfallEnum, DroneInDockEnum
} from '/@/types/device'
import { computed, defineComponent, onMounted, reactive, ref, watch, defineExpose, onUnmounted } from 'vue'
import { useMyStore } from '/@/store'

const osdVisible = computed(() => {
  return store.state.osdVisible
})

const store = useMyStore()
const str = '--'
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
</script>
