<template>
  <div class="container">
    <div class="left">
        <div class="box-title">
           无人机控制
        </div>
        <!-- 遥控器 -->
        <div class="UAV-control">
            <div class="flex-center"  style="margin-top: 5px;">
                <img src="../../../assets/v3/icon/rightBottom2.png"  style="width: 100px; height: 70px;">
                <img src="../../../assets/v3/icon/rightBottom3.png"  style="width: 100px; height: 70px;"/>
            </div>
            <div class="flex-center">
                <div class="sector">
                    <div class="box s1">
                    <div
                        @mousedown="onMouseDown(KeyCode.KEY_A)"
                        @onmouseup="onMouseUp"
                        class="arrow"
                        id="id-10"
                    >
                        <i class="iconfont icon-qiehuanqishang"></i>
                    </div>
                    </div>
                    <div class="box s2">
                    <div
                        @mousedown="onMouseDown(KeyCode.KEY_W)"
                        @onmouseup="onMouseUp"
                        class="arrow"
                        id="id-11"
                    >
                        <i class="iconfont icon-qiehuanqishang"></i>
                    </div>
                    </div>
                    <div class="box s3">
                    <div
                        @mousedown="onMouseDown(KeyCode.KEY_D)"
                        @onmouseup="onMouseUp"
                        class="arrow"
                        id="id-12"
                    >
                        <i class="iconfont icon-qiehuanqishang"></i>
                    </div>
                    </div>
                    <div class="box s4">
                    <div
                        @mousedown="onMouseDown(KeyCode.KEY_S)"
                        @onmouseup="onMouseUp"
                        class="arrow"
                        id="id-13"
                    >
                        <i class="iconfont icon-qiehuanqishang"></i>
                    </div>
                    </div>
                    <div class="center" id="id-18" @click="changeDirection(13)">
                    <div
                        @click="handleEmergencyStop"
                        class="arrow-reset"
                        id="id-14"
                    >
                        <span>急停</span>
                    </div>

                    </div>
                </div>
            </div>
            <div class="flex-center">
                <el-button class ='box-btn' style="top:-140px;left: -88px;" @mousedown="onMouseDown(KeyCode.KEY_Q)" @onmouseup="onMouseUp">
                    <span class="iconfont icon-icon-test" ></span>
                </el-button>
            </div>
            <div class="flex-center">
                <el-button class ='box-btn' style="top:-170px;left: 88px;" @mousedown="onMouseDown(KeyCode.KEY_E)" @onmouseup="onMouseUp">
                    <span class="iconfont icon-icon-test1" ></span>
                </el-button>
            </div>
            <div class="flex-center">
                <el-button class ='box-btn' style="top:-160px;left: -88px;" @mousedown="onMouseDown(KeyCode.ARROW_UP)" @onmouseup="onMouseUp">
                    <span class="iconfont icon-xiangshang4" ></span>
                </el-button>
            </div>
            <div class="flex-center">
                <el-button class ='box-btn' style="top:-190px;left: 88px;" @mousedown="onMouseDown(KeyCode.ARROW_DOWN)" @onmouseup="onMouseUp">
                    <span class="iconfont icon-xiangxia4" ></span>
                </el-button>
            </div>
        </div>
        <svg width="0" height="0">
            <clipPath id="sector">
                <path
                d="M 72.5,72.5 L 0,130 A 60.25,60.25 0 0,1 9.7425,9.7425 z"
                ></path>
            </clipPath>
        </svg>
        <!-- 控制按钮 -->
        <div class="btn-list">
            <el-button class="btn" @click="onClickFightControl" >{{ flightController ? '释放控制权' : '申请控制权'}}</el-button>
            <DroneControlPopover
                :visible="takeoffToPointPopoverData.visible"
                :loading="takeoffToPointPopoverData.loading"
                @confirm="($event) => onTakeoffToPointConfirm(true)"
                @cancel="($event) =>onTakeoffToPointConfirm(false)"
            >
                <template #formContent>
                <div class="form-content">
                    <div>
                    <span class="form-label">纬度:</span>
                    <a-input-number v-model:value="takeoffToPointPopoverData.latitude" />
                    </div>
                    <div>
                    <span class="form-label">经度:</span>
                    <a-input-number v-model:value="takeoffToPointPopoverData.longitude" />
                    </div>
                    <div>
                    <span class="form-label">高度(m):</span>
                    <a-input-number v-model:value="takeoffToPointPopoverData.height" />
                    </div>
                    <div>
                    <span class="form-label">安全飞行高度(m):</span>
                    <a-input-number v-model:value="takeoffToPointPopoverData.securityTakeoffHeight" />
                    </div>
                    <div>
                    <span class="form-label">返程高度(m):</span>
                    <a-input-number v-model:value="takeoffToPointPopoverData.rthAltitude" />
                    </div>
                    <div>
                    <span class="form-label">丢失动作:</span>
                    <a-select v-model:value="takeoffToPointPopoverData.rcLostAction" style="width: 120px"
                        :options="LostControlActionInCommandFLightOptions"></a-select>
                    </div>
                    <div>
                    <span class="form-label">航线丢失动作:</span>
                    <a-select v-model:value="takeoffToPointPopoverData.exitWaylineWhenRcLost" style="width: 120px"
                        :options="WaylineLostControlActionInCommandFlightOptions"></a-select>
                    </div>
                    <div>
                    <span class="form-label">返程模式:</span>
                    <a-select v-model:value="takeoffToPointPopoverData.rthMode" style="width: 120px"
                        :options="RthModeInCommandFlightOptions"></a-select>
                    </div>
                    <div>
                    <span class="form-label">指挥模式丢失操作:</span>
                    <a-select v-model:value="takeoffToPointPopoverData.commanderModeLostAction" style="width: 120px"
                        :options="CommanderModeLostActionInCommandFlightOptions"></a-select>
                    </div>
                    <div>
                    <span class="form-label">指挥飞行模式:</span>
                    <a-select v-model:value="takeoffToPointPopoverData.commanderFlightMode" style="width: 120px"
                        :options="CommanderFlightModeInCommandFlightOptions"></a-select>
                    </div>
                    <div>
                    <span class="form-label">指挥飞行高度(m):</span>
                    <a-input-number v-model:value="takeoffToPointPopoverData.commanderFlightHeight" />
                    </div>
                </div>
                </template>
                <el-button class="btn" @click="onShowTakeoffToPointPopover">起飞</el-button>
                <!-- <div>
                <Button size="small" ghost @click="openLivestreamAgora" >
                    <span>Agora Live</span>
                </Button>
                <Button size="small" ghost @click="openLivestreamOthers" >
                    <span>RTMP/GB28181 Live</span>
                </Button>
                </div> -->
            </DroneControlPopover>
        </div>
        <div class="btn-list">
            <DroneControlPopover
                :visible="flyToPointPopoverData.visible"
                :loading="flyToPointPopoverData.loading"
                @confirm="($event) => onFlyToConfirm(true)"
                @cancel="($event) =>onFlyToConfirm(false)"
            >
                <template #formContent>
                <div class="form-content">
                    <div>
                    <span class="form-label">纬度:</span>
                    <a-input-number v-model:value="flyToPointPopoverData.latitude" />
                    </div>
                    <div>
                    <span class="form-label">经度:</span>
                    <a-input-number v-model:value="flyToPointPopoverData.longitude" />
                    </div>
                    <div>
                    <span class="form-label">高度(m):</span>
                    <a-input-number v-model:value="flyToPointPopoverData.height" />
                    </div>
                </div>
                </template>
                <el-button class="btn" @click="onShowFlyToPopover">指令飞行</el-button>
            </DroneControlPopover>
            <el-button class="btn" @click="onStopFlyToPoint">取消飞行</el-button>
        </div>
        <div class="btn-list">
            <div v-for="(cmdItem) in cmdList" :key="cmdItem.cmdKey" >
                 <el-button class="btn" :loading="cmdItem.loading" size="small"  @click="sendControlCmd(cmdItem, 0)">{{ cmdItem.operateText === 'Return Home' ? '返航':'取消返航'}}</el-button>
            </div>
        </div>
        <div class="btn-list">
          <el-button class="btn" @click="opensettingVisible" >属性配置</el-button>
        </div>
        <div class="kv">
    <span class="label">电池电量</span>
    <span class="value">
      <template v-if="batteryPct !== null">{{ batteryPct }} %</template>
      <template v-else>--</template>
    </span>
  </div>
        <!-- 弹窗 -->
    </div>
    <div class="right">
        <div class="box-title">
           负载控制
        </div>
        <!-- 遥控器 -->
        <div class="UAV-control">
            <div class="flex-center"  style="margin-top: 5px;">
                <img src="../../../assets/v3/icon/rightBottom2.png"  style="width: 100px; height: 70px;">
                <img src="../../../assets/v3/icon/rightBottom3.png"  style="width: 100px; height: 70px;"/>
            </div>
            <div class="flex-center">
                <div class="sector">
                    <div class="box s1">
                    <div
                        @mousedown="onMouseDown(KeyCode.KEY_A)"
                        @onmouseup="onMouseUp"
                        class="arrow"
                        id="id-10"
                    >
                        <i class="iconfont icon-qiehuanqishang"></i>
                    </div>
                    </div>
                    <div class="box s2">
                    <div
                        @mousedown="onMouseDown(KeyCode.KEY_W)"
                        @onmouseup="onMouseUp"
                        class="arrow"
                        id="id-11"
                    >
                        <i class="iconfont icon-qiehuanqishang"></i>
                    </div>
                    </div>
                    <div class="box s3">
                    <div
                        @mousedown="onMouseDown(KeyCode.KEY_D)"
                        @onmouseup="onMouseUp"
                        class="arrow"
                        id="id-12"
                    >
                        <i class="iconfont icon-qiehuanqishang"></i>
                    </div>
                    </div>
                    <div class="box s4">
                    <div
                        @mousedown="onMouseDown(KeyCode.KEY_S)"
                        @onmouseup="onMouseUp"
                        class="arrow"
                        id="id-13"
                    >
                        <i class="iconfont icon-qiehuanqishang"></i>
                    </div>
                    </div>
                    <div class="center" id="id-18" @click="changeDirection(13)">
                    <div
                        class="arrow-reset"
                        id="id-14"
                    >
                        <span></span>
                    </div>

                    </div>
                </div>
            </div>
        </div>
        <!-- 负载控制按钮 -->
        <div class="btn-list">
            <Select v-model:value="payloadSelectInfo.value" style="width: 110px; marginRight: 5px" :options="payloadSelectInfo.options" @change="handlePayloadChange"/>
            <el-button class="btn" @click="onAuthPayload">负载控制</el-button>
        </div>
        <div class="btn-list">
            <DroneControlPopover
                :visible="gimbalResetPopoverData.visible"
                :loading="gimbalResetPopoverData.loading"
                @confirm="($event) => onGimbalResetConfirm(true)"
                @cancel="($event) =>onGimbalResetConfirm(false)"
                >
                <template #formContent>
                    <div class="form-content">
                    <div>
                        <span class="form-label">reset mode:</span>
                        <a-select
                        v-model:value="gimbalResetPopoverData.resetMode"
                        style="width: 180px"
                        :options="GimbalResetModeOptions"
                        ></a-select>
                    </div>
                    </div>
                </template>
                <el-button class="btn" @click="onShowGimbalResetPopover">云台重置</el-button>
            </DroneControlPopover>
            <el-button class="btn" @click="onSwitchCameraMode">模式切换</el-button>
        </div>
        <div class="btn-list">
            <el-button class="btn" @click="onStartCameraRecording">开始录像</el-button>
            <el-button class="btn" @click="onStopCameraRecording">停止录像</el-button>
        </div>
        <div class="btn-list">
            <el-button class="btn" @click="onTakeCameraPhoto">拍照</el-button>
            <DroneControlPopover
            :visible="zoomFactorPopoverData.visible"
            :loading="zoomFactorPopoverData.loading"
            @confirm="($event) => onZoomFactorConfirm(true)"
            @cancel="($event) =>onZoomFactorConfirm(false)"
            >
            <template #formContent>
                <div class="form-content">
                    <div>
                    <span class="form-label">相机类型:</span>
                    <a-select v-model:value="zoomFactorPopoverData.cameraType" style="width: 120px"
                        :options="ZoomCameraTypeOptions"></a-select>
                    </div>
                    <div>
                    <span class="form-label">变焦倍数:</span>
                    <a-input-number v-model:value="zoomFactorPopoverData.zoomFactor" :min="2" :max="200" />
                    </div>
                </div>
            </template>
            <el-button class="btn" @click="($event) => onShowZoomFactorPopover()">变焦</el-button>
            </DroneControlPopover>
        </div>
        <div class="btn-list">
            <DroneControlPopover
            :visible="cameraAimPopoverData.visible"
            :loading="cameraAimPopoverData.loading"
            @confirm="($event) => onCameraAimConfirm(true)"
            @cancel="($event) =>onCameraAimConfirm(false)"
          >
            <template #formContent>
              <div class="form-content">
                <div>
                  <span class="form-label">相机类型:</span>
                  <a-select v-model:value="cameraAimPopoverData.cameraType" style="width: 120px"
                    :options="CameraTypeOptions"></a-select>
                </div>
                <div>
                  <span class="form-label">锁定:</span>
                  <a-switch v-model:checked="cameraAimPopoverData.locked" />
                </div>
                <div>
                  <span class="form-label">x:</span>
                  <a-input-number v-model:value="cameraAimPopoverData.x" :min="0" :max="1"/>
                </div>
                <div>
                  <span class="form-label">y:</span>
                  <a-input-number v-model:value="cameraAimPopoverData.y" :min="0" :max="1"/>
                </div>
              </div>
            </template>
            <el-button class="btn" @click="($event) => onShowCameraAimPopover()">AIM</el-button>
          </DroneControlPopover>

          <!-- LookAt：按经纬高指向目标（可锁定） -->
<DroneControlPopover
  :visible="lookAtPopoverData.visible"
  :loading="lookAtPopoverData.loading"
  @confirm="() => onCameraLookAtConfirm(true)"
  @cancel="() => onCameraLookAtConfirm(false)"
>
  <template #formContent>
    <div class="form-content">
      <div>
        <span class="form-label">锁定:</span>
        <a-switch v-model:checked="lookAtPopoverData.locked" />
      </div>
      <div>
        <span class="form-label">纬度:</span>
        <a-input-number v-model:value="lookAtPopoverData.latitude" />
      </div>
      <div>
        <span class="form-label">经度:</span>
        <a-input-number v-model:value="lookAtPopoverData.longitude" />
      </div>
      <div>
        <span class="form-label">高度(m):</span>
        <a-input-number v-model:value="lookAtPopoverData.height" :min="1" />
      </div>
    </div>

  </template>
  <el-button class="btn" @click="onShowLookAtPopover">LookAt</el-button>
</DroneControlPopover>

        </div>
    </div>
  </div>
<!-- 海上目标追踪任务：放在“无人机控制/负载控制”下面 -->
<div class="mission-panel">
  <div class="box-title">-</div>

  <DroneControlPopover
    :visible="missionParams.visible"
    :loading="false"
    @confirm="() => onMissionParamConfirm(true)"
    @cancel="() => onMissionParamConfirm(false)"
  >
    <template #formContent>
      <div class="form-content">
        <div><span class="form-label">起飞纬度:</span><a-input-number v-model:value="missionParams.lat" /></div>
        <div><span class="form-label">起飞经度:</span><a-input-number v-model:value="missionParams.lng" /></div>
        <div><span class="form-label">机巢悬停高度:</span><a-input-number v-model:value="missionParams.homeHoverAlt" /></div>
        <div><span class="form-label">安全起飞高度:</span><a-input-number v-model:value="missionParams.securityTakeoffAlt" /></div>
        <div><span class="form-label">指导飞行高度:</span><a-input-number v-model:value="missionParams.flightAlt" /></div>
        <div><span class="form-label">作业高度(m):</span><a-input-number v-model:value="missionParams.alt" :min="10" /></div>
        <div><span class="form-label">抵近速度(m/s):</span><a-input-number v-model:value="missionParams.approachSpeed" :min="1" :max="MAX_SPEED" /></div>
        <div><span class="form-label">盘旋半径(m):</span><a-input-number v-model:value="missionParams.orbitRadius" :min="20" /></div>
        <div><span class="form-label">盘旋点数:</span><a-input-number v-model:value="missionParams.orbitPoints" :min="6" :max="60" /></div>
        <div><span class="form-label">相机类型:</span>
          <a-select v-model:value="missionParams.cameraType" style="width: 150px" :options="CameraTypeOptions"></a-select>
        </div>
        <div><span class="form-label">自动对焦/锁定:</span><a-switch v-model:checked="missionParams.lockAim" /></div>
      </div>
    </template>

  </DroneControlPopover>

  <div class="btn-list mission-btns">
    <el-button class="btn" @click="openMissionParam">参数设置</el-button>
    <el-button class="btn" :loading="missionBusy" @click="onOneKeyTakeoff">一键起飞</el-button>
    <el-button class="btn" :loading="missionBusy" @click="onApproachObserve">-观察</el-button>
    <el-button class="btn" :loading="missionBusy" @click="onApproachOrbit">-盘旋</el-button>
    <el-button class="btn" :loading="missionBusy" type="danger" @click="onEndMission">结束任务</el-button>
  </div>

  <div class="mission-status">
    <span>当前状态：{{ missionStateText }}</span>
  </div>
</div>

    <!-- 信息提示 -->
  <div class="message-box">
    <DroneControlInfoPanel :message="drcInfo"></DroneControlInfoPanel>
  </div>
  <!-- 设备属性设置 -->
  <div v-if="settingVisible" v-drag-window class="device-info">
      <div>
          <div class="drag-title"> 属性设置</div>
          <!-- <div class="drag-title" style="height: 40px; width: 100% ;line-height: 40px; color: aliceblue">无人机视频</div>  ../../assets/v3/icon/uav-fly.png-->
          <a style="position: absolute; right: 10px; top: 10px; font-size: 16px; color: white;" @click="closeDevice()"><CloseOutlined /></a>
      </div>
      <div>
          <droneSetting :sn="props.sn" :deviceInfo="props.deviceInfo" />

      </div>

  </div>

</template>
<script setup lang="ts">
import { defineProps, reactive, ref, watch, computed, onMounted, watchEffect } from 'vue'
import { Select, message, Button } from 'ant-design-vue'
import { PayloadInfo, DeviceInfoType, ControlSource, DeviceOsdCamera, DrcStateEnum } from '/@/types/device'
import { useMyStore } from '/@/store'
import { postDrcEnter, postDrcExit } from '/@/api/drc'
import { useMqtt, DeviceTopicInfo } from '/@/components/g-map/use-mqtt'
import { DownOutlined, UpOutlined, LeftOutlined, RightOutlined, PauseCircleOutlined, UndoOutlined, RedoOutlined, ArrowUpOutlined, ArrowDownOutlined, CloseOutlined } from '@ant-design/icons-vue'
import { useManualControl, KeyCode } from '/@/components/g-map/use-manual-control'
import { usePayloadControl } from '/@/components/g-map/use-payload-control'
import { CameraMode, CameraType, CameraTypeOptions, ZoomCameraTypeOptions, CameraListItem } from '/@/types/live-stream'
import { useDroneControlWsEvent } from '/@/components/g-map/use-drone-control-ws-event'
import { useDroneControlMqttEvent } from '/@/components/g-map/use-drone-control-mqtt-event'
import {
  postFlightAuth, LostControlActionInCommandFLight, WaylineLostControlActionInCommandFlight, ERthMode,
  ECommanderModeLostAction, ECommanderFlightMode
} from '/@/api/drone-control/drone'
import { useDroneControl } from '/@/components/g-map/use-drone-control'
import {
  GimbalResetMode, GimbalResetModeOptions, LostControlActionInCommandFLightOptions, WaylineLostControlActionInCommandFlightOptions,
  RthModeInCommandFlightOptions, CommanderModeLostActionInCommandFlightOptions, CommanderFlightModeInCommandFlightOptions
} from '/@/types/drone-control'
import DroneControlPopover from '/@/components/g-map/DroneControlPopover.vue'
import DroneControlInfoPanel from '/@/components/g-map/DroneControlInfoPanel.vue'
import { noDebugCmdList as baseCmdList, DeviceCmdItem, DeviceCmd } from '/@/types/device-cmd'
import { useDockControl } from '/@/components/g-map/use-dock-control'
import droneSetting from './dock-setting.vue'

const props = defineProps<{
  sn: string,
  deviceInfo: DeviceInfoType,
  payloads: null | PayloadInfo[]
}>()

// 电量：优先飞机 -> 兜底用 Dock 读数 -> 无则 null
const batteryPct = computed(() => {
  const uav = props.deviceInfo?.device?.battery?.capacity_percent
  if (typeof uav === 'number') return uav
  const dock = props.deviceInfo?.dock?.basic_osd?.drone_charge_state?.capacity_percent
  return typeof dock === 'number' ? dock : null
})

const store = useMyStore()
const clientId = computed(() => {
  return store.state.clientId
})

const initCmdList = baseCmdList.map(cmdItem => Object.assign({}, cmdItem))
const cmdList = ref(initCmdList)

const {
  sendDockControlCmd
} = useDockControl()

async function sendControlCmd (cmdItem: DeviceCmdItem, index: number) {
  cmdItem.loading = true
  const result = await sendDockControlCmd({
    sn: props.sn,
    cmd: cmdItem.cmdKey,
    action: cmdItem.action
  }, false)
  if (result) {
    message.success('Return home successful')
    if (flightController.value) {
      exitFlightCOntrol()
    }
  } else {
    message.error('Failed to return home')
  }
  cmdItem.loading = false
}

const { flyToPoint, stopFlyToPoint, takeoffToPoint } = useDroneControl()
const MAX_SPEED = 14

const flyToPointPopoverData = reactive({
  visible: false,
  loading: false,
  latitude: null as null | number,
  longitude: null as null | number,
  height: null as null | number,
  maxSpeed: MAX_SPEED,
})

function onShowFlyToPopover () {
  flyToPointPopoverData.visible = !flyToPointPopoverData.visible
  flyToPointPopoverData.loading = false
  flyToPointPopoverData.latitude = null
  flyToPointPopoverData.longitude = null
  flyToPointPopoverData.height = null
}

async function onFlyToConfirm (confirm: boolean) {
  if (confirm) {
    if (!flyToPointPopoverData.height || !flyToPointPopoverData.latitude || !flyToPointPopoverData.longitude) {
      message.error('Input error')
      return
    }
    try {
      await flyToPoint(props.sn, {
        max_speed: flyToPointPopoverData.maxSpeed,
        points: [
          {
            latitude: flyToPointPopoverData.latitude,
            longitude: flyToPointPopoverData.longitude,
            height: flyToPointPopoverData.height
          }
        ]
      })
    } catch (error) {
    }
  }
  flyToPointPopoverData.visible = false
}

async function onStopFlyToPoint () {
  await stopFlyToPoint(props.sn)
}

const takeoffToPointPopoverData = reactive({
  visible: false,
  loading: false,
  latitude: null as null | number,
  longitude: null as null | number,
  height: null as null | number,
  securityTakeoffHeight: null as null | number,
  maxSpeed: MAX_SPEED,
  rthAltitude: null as null | number,
  rcLostAction: LostControlActionInCommandFLight.RETURN_HOME,
  exitWaylineWhenRcLost: WaylineLostControlActionInCommandFlight.EXEC_LOST_ACTION,
  rthMode: ERthMode.SETTING,
  commanderModeLostAction: ECommanderModeLostAction.CONTINUE,
  commanderFlightMode: ECommanderFlightMode.SETTING,
  commanderFlightHeight: null as null | number,
})

function onShowTakeoffToPointPopover () {
  takeoffToPointPopoverData.visible = !takeoffToPointPopoverData.visible
  takeoffToPointPopoverData.loading = false
  takeoffToPointPopoverData.latitude = null
  takeoffToPointPopoverData.longitude = null
  takeoffToPointPopoverData.securityTakeoffHeight = null
  takeoffToPointPopoverData.rthAltitude = null
  takeoffToPointPopoverData.rcLostAction = LostControlActionInCommandFLight.RETURN_HOME
  takeoffToPointPopoverData.exitWaylineWhenRcLost = WaylineLostControlActionInCommandFlight.EXEC_LOST_ACTION
  takeoffToPointPopoverData.rthMode = ERthMode.SETTING
  takeoffToPointPopoverData.commanderModeLostAction = ECommanderModeLostAction.CONTINUE
  takeoffToPointPopoverData.commanderFlightMode = ECommanderFlightMode.SETTING
  takeoffToPointPopoverData.commanderFlightHeight = null
}

async function onTakeoffToPointConfirm (confirm: boolean) {
  if (confirm) {
    if (!takeoffToPointPopoverData.height ||
        !takeoffToPointPopoverData.latitude ||
        !takeoffToPointPopoverData.longitude ||
        !takeoffToPointPopoverData.securityTakeoffHeight ||
        !takeoffToPointPopoverData.rthAltitude ||
        !takeoffToPointPopoverData.commanderFlightHeight) {
      message.error('Input error')
      return
    }
    try {
      await takeoffToPoint(props.sn, {
        target_latitude: takeoffToPointPopoverData.latitude,
        target_longitude: takeoffToPointPopoverData.longitude,
        target_height: takeoffToPointPopoverData.height,
        security_takeoff_height: takeoffToPointPopoverData.securityTakeoffHeight,
        rth_altitude: takeoffToPointPopoverData.rthAltitude,
        max_speed: takeoffToPointPopoverData.maxSpeed,
        rc_lost_action: takeoffToPointPopoverData.rcLostAction,
        exit_wayline_when_rc_lost: takeoffToPointPopoverData.exitWaylineWhenRcLost,
        rth_mode: takeoffToPointPopoverData.rthMode,
        commander_mode_lost_action: takeoffToPointPopoverData.commanderModeLostAction,
        commander_flight_mode: takeoffToPointPopoverData.commanderFlightMode,
        commander_flight_height: takeoffToPointPopoverData.commanderFlightHeight,
      })
    } catch (error) {
    }
  }
  takeoffToPointPopoverData.visible = false
}

const deviceTopicInfo: DeviceTopicInfo = reactive({
  sn: props.sn,
  pubTopic: '',
  subTopic: ''
})

useMqtt(deviceTopicInfo)

// 飞行控制
// const drcState = computed(() => {
//   return store.state.deviceState?.dockInfo[props.sn]?.link_osd?.drc_state === DrcStateEnum.CONNECTED
// })
const flightController = ref(false)

async function onClickFightControl () {
  if (flightController.value) {
    exitFlightCOntrol()
    return
  }
  enterFlightControl()
}

// 进入飞行控制
async function enterFlightControl () {
  try {
    const { code, data } = await postDrcEnter({
      client_id: clientId.value,
      dock_sn: props.sn,
    })
    if (code === 0) {
      flightController.value = true
      if (data.sub && data.sub.length > 0) {
        deviceTopicInfo.subTopic = data.sub[0]
      }
      if (data.pub && data.pub.length > 0) {
        deviceTopicInfo.pubTopic = data.pub[0]
      }
      // 获取飞行控制权
      if (droneControlSource.value !== ControlSource.A) {
        await postFlightAuth(props.sn)
      }
      message.success('Get flight control successfully')
    }
  } catch (error: any) {
  }
}

// 退出飞行控制
async function exitFlightCOntrol () {
  try {
    const { code } = await postDrcExit({
      client_id: clientId.value,
      dock_sn: props.sn,
    })
    if (code === 0) {
      flightController.value = false
      deviceTopicInfo.subTopic = ''
      deviceTopicInfo.pubTopic = ''
      message.success('Exit flight control')
    }
  } catch (error: any) {
  }
}

// drc mqtt message
const { drcInfo, errorInfo } = useDroneControlMqttEvent(props.sn)

const {
  handleKeyup,
  handleEmergencyStop,
  resetControlState,
} = useManualControl(deviceTopicInfo, flightController)

function onMouseDown (type: KeyCode) {
  handleKeyup(type)
}

function onMouseUp () {
  resetControlState()
}

// 负载控制
const payloadSelectInfo = {
  value: null as any,
  controlSource: undefined as undefined | ControlSource,
  options: [] as any,
  payloadIndex: '' as string,
  camera: undefined as undefined | DeviceOsdCamera // 当前负载osd信息
}

const handlePayloadChange = (value: string) => {
  const payload = props.payloads?.find(item => item.payload_sn === value)
  if (payload) {
    payloadSelectInfo.payloadIndex = payload.payload_index || ''
    payloadSelectInfo.controlSource = payload.control_source
    payloadSelectInfo.camera = undefined
  }
}

// function getCurrentCamera (cameraList: CameraListItem[], cameraIndex?: string):CameraListItem | null {
//   let camera = null
//   cameraList.forEach(item => {
//     if (item.camera_index === cameraIndex) {
//       camera = item
//     }
//   })
//   return camera
// }

// const currentCamera = computed(() => {
//   return getCurrentCamera(props.deviceInfo.dock.basic_osd.live_capacity?.device_list[0]?.camera_list as CameraListItem[], camera_index)
// })
// 更新负载信息
watch(() => props.payloads, (payloads) => {
  if (payloads && payloads.length > 0) {
    payloadSelectInfo.value = payloads[0].payload_sn
    payloadSelectInfo.controlSource = payloads[0].control_source || ControlSource.B
    payloadSelectInfo.payloadIndex = payloads[0].payload_index || ''
    payloadSelectInfo.options = payloads.map(item => ({ label: item.payload_name, value: item.payload_sn }))
    payloadSelectInfo.camera = undefined
  } else {
    payloadSelectInfo.value = null
    payloadSelectInfo.controlSource = undefined
    payloadSelectInfo.options = []
    payloadSelectInfo.payloadIndex = ''
    payloadSelectInfo.camera = undefined
  }
}, {
  immediate: true,
  deep: true
})
watch(() => props.deviceInfo.device, (droneOsd) => {
  if (droneOsd && droneOsd.cameras) {
    payloadSelectInfo.camera = droneOsd.cameras.find(item => item.payload_index === payloadSelectInfo.payloadIndex)
  } else {
    payloadSelectInfo.camera = undefined
  }
}, {
  immediate: true,
  deep: true
})

// ws 消息通知
const { droneControlSource, payloadControlSource } = useDroneControlWsEvent(props.sn, payloadSelectInfo.value)
watch(() => payloadControlSource, (controlSource) => {
  payloadSelectInfo.controlSource = controlSource.value
}, {
  immediate: true,
  deep: true
})
const {
  checkPayloadAuth,
  authPayload,
  resetGimbal,
  switchCameraMode,
  takeCameraPhoto,
  startCameraRecording,
  stopCameraRecording,
  changeCameraFocalLength,
  cameraAim,
  cameraLookAt, // NEW
} = usePayloadControl()

async function onAuthPayload () {
  const result = await authPayload(props.sn, payloadSelectInfo.payloadIndex)
  if (result) {
    payloadControlSource.value = ControlSource.A
  }
}

const gimbalResetPopoverData = reactive({
  visible: false,
  loading: false,
  resetMode: null as null | GimbalResetMode,
})

function onShowGimbalResetPopover () {
  gimbalResetPopoverData.visible = !gimbalResetPopoverData.visible
  gimbalResetPopoverData.loading = false
  gimbalResetPopoverData.resetMode = null
}

async function onGimbalResetConfirm (confirm: boolean) {
  if (confirm) {
    if (gimbalResetPopoverData.resetMode === null) {
      message.error('Please select reset mode')
      return
    }
    gimbalResetPopoverData.loading = true
    try {
      await resetGimbal(props.sn, {
        payload_index: payloadSelectInfo.payloadIndex,
        reset_mode: gimbalResetPopoverData.resetMode
      })
    } catch (err) {
    }
  }
  gimbalResetPopoverData.visible = false
}

async function onSwitchCameraMode () {
  if (!checkPayloadAuth(payloadSelectInfo.controlSource)) {
    return
  }
  const currentCameraMode = payloadSelectInfo.camera?.camera_mode
  await switchCameraMode(props.sn, {
    payload_index: payloadSelectInfo.payloadIndex,
    camera_mode: currentCameraMode === CameraMode.Photo ? CameraMode.Video : CameraMode.Photo
  })
}

async function onTakeCameraPhoto () {
  if (!checkPayloadAuth(payloadSelectInfo.controlSource)) {
    return
  }
  await takeCameraPhoto(props.sn, payloadSelectInfo.payloadIndex)
}

async function onStartCameraRecording () {
  if (!checkPayloadAuth(payloadSelectInfo.controlSource)) {
    return
  }
  await startCameraRecording(props.sn, payloadSelectInfo.payloadIndex)
}

async function onStopCameraRecording () {
  if (!checkPayloadAuth(payloadSelectInfo.controlSource)) {
    return
  }
  await stopCameraRecording(props.sn, payloadSelectInfo.payloadIndex)
}

const zoomFactorPopoverData = reactive({
  visible: false,
  loading: false,
  cameraType: null as null | CameraType,
  zoomFactor: null as null | number,
})

function onShowZoomFactorPopover () {
  zoomFactorPopoverData.visible = !zoomFactorPopoverData.visible
  zoomFactorPopoverData.loading = false
  zoomFactorPopoverData.cameraType = null
  zoomFactorPopoverData.zoomFactor = null
}

async function onZoomFactorConfirm (confirm: boolean) {
  if (confirm) {
    if (!zoomFactorPopoverData.zoomFactor || zoomFactorPopoverData.cameraType === null) {
      message.error('Please input Zoom Factor')
      return
    }
    zoomFactorPopoverData.loading = true
    try {
      await changeCameraFocalLength(props.sn, {
        payload_index: payloadSelectInfo.payloadIndex,
        camera_type: zoomFactorPopoverData.cameraType,
        zoom_factor: zoomFactorPopoverData.zoomFactor
      })
    } catch (err) {
    }
  }
  zoomFactorPopoverData.visible = false
}

const cameraAimPopoverData = reactive({
  visible: false,
  loading: false,
  cameraType: null as null | CameraType,
  locked: false,
  x: null as null | number,
  y: null as null | number,
})

function onShowCameraAimPopover () {
  cameraAimPopoverData.visible = !cameraAimPopoverData.visible
  cameraAimPopoverData.loading = false
  cameraAimPopoverData.cameraType = null
  cameraAimPopoverData.locked = false
  cameraAimPopoverData.x = null
  cameraAimPopoverData.y = null
}

// LookAt 弹窗状态
const lookAtPopoverData = reactive({
  visible: false,
  loading: false,
  locked: true as boolean,
  latitude: null as null | number,
  longitude: null as null | number,
  height: null as null | number,
})

function onShowLookAtPopover () {
  lookAtPopoverData.visible = true
  lookAtPopoverData.loading = false
  // 默认复用上次/置空
  // lookAtPopoverData.locked 保持上次即可
  // 其他清空
  // lookAtPopoverData.latitude = null
  // lookAtPopoverData.longitude = null
  // lookAtPopoverData.height = null
}

function openLivestreamOthers () {
  store.commit('SET_LIVESTREAM_OTHERS_VISIBLE', true)
}

function openLivestreamAgora () {
  store.commit('SET_LIVESTREAM_AGORA_VISIBLE', true)
}

async function onCameraAimConfirm (confirm: boolean) {
  if (confirm) {
    if (cameraAimPopoverData.cameraType === null || cameraAimPopoverData.x === null || cameraAimPopoverData.y === null) {
      message.error('Input error')
      return
    }
    try {
      await cameraAim(props.sn, {
        payload_index: payloadSelectInfo.payloadIndex,
        camera_type: cameraAimPopoverData.cameraType,
        locked: cameraAimPopoverData.locked,
        x: cameraAimPopoverData.x,
        y: cameraAimPopoverData.y,
      })
    } catch (error) {
    }
  }
  cameraAimPopoverData.visible = false
}

async function onCameraLookAtConfirm (confirm: boolean) {
  message.info('[LookAt] confirm fired')
  console.debug('[LookAt] confirm =', confirm)
  lookAtPopoverData.visible = false
  if (!confirm) return

  // 1) 断言函数已注入
  if (typeof cameraLookAt !== 'function') {
    console.error('[LookAt] cameraLookAt 未注入（usePayloadControl 没有 return 这个函数？）')
    message.error('内部错误：cameraLookAt 未注入')
    return
  }

  // 2) 权限与索引检查（带日志）
  console.debug('[LookAt] controlSource =', payloadSelectInfo.controlSource, ' payloadIndex=', payloadSelectInfo.payloadIndex)
  if (!checkPayloadAuth(payloadSelectInfo.controlSource)) {
    message.error('请先获取“负载控制”权限')
    return
  }
  if (!payloadSelectInfo.payloadIndex) {
    message.error('未选中负载或 payloadIndex 为空')
    return
  }

  // 3) 参数校验：允许 0 度经纬
  const { latitude, longitude, height, locked } = lookAtPopoverData
  if (
    latitude == null || longitude == null || height == null ||
    !Number.isFinite(latitude) || !Number.isFinite(longitude) || !Number.isFinite(height)
  ) {
    message.error('请输入完整的经纬度与高度')
    console.warn('[LookAt] bad params:', { latitude, longitude, height })
    return
  }

  try {
    lookAtPopoverData.loading = true
    console.debug('[LookAt] request payload =', {
      payload_index: payloadSelectInfo.payloadIndex,
      locked,
      latitude,
      longitude,
      height
    })
    await cameraLookAt(props.sn, {
      payload_index: payloadSelectInfo.payloadIndex,
      locked,
      latitude,
      longitude,
      height,
    })
    console.debug('[LookAt] request done')
  } catch (e) {
    console.error('[LookAt] request error:', e)
    message.error('LookAt 下发失败')
  } finally {
    lookAtPopoverData.loading = false
  }
}

watch(() => errorInfo, (errorInfo) => {
  if (errorInfo.value) {
    message.error(errorInfo.value)
    console.error(errorInfo.value)
    errorInfo.value = ''
  }
}, {
  immediate: true,
  deep: true
})

// 打开属性配置信息
const settingVisible = ref(false)
function opensettingVisible () {
  settingVisible.value = !settingVisible.value
}
function closeDevice () {
  settingVisible.value = false
}

// === 海上目标追踪任务 - 轻量状态机 ===------------------------------------------------------------------------------------------------------------
type MissionState = 'idle' | 'takingOff' | 'approaching' | 'orbiting' | 'hovering';
const missionState = ref<MissionState>('idle')
const missionBusy = ref(false)

const missionParams = reactive({
  visible: false,
  lat: null as null | number,
  lng: null as null | number,
  homeHoverAlt: 115 as null | number,
  securityTakeoffAlt: 20 as null | number,
  flightAlt: 20 as null | number,
  alt: 80 as number, // 作业高度（可改）
  approachSpeed: 8 as number, // 抵近速度
  orbitRadius: 60 as number, // 盘旋半径
  orbitPoints: 16 as number, // 轨迹离散点数（越多越圆）
  cameraType: null as null | CameraType,
  lockAim: true as boolean, // 抵近/盘旋阶段自动AIM
})

const missionStateText = computed(() => {
  switch (missionState.value) {
    case 'idle': return '待命'
    case 'hovering': return '起飞盘旋'
    case 'takingOff': return '起飞中'
    case 'approaching': return '抵近观察'
    case 'orbiting': return '抵近盘旋'
    default: return '未知'
  }
})

function openMissionParam () {
  missionParams.visible = true
}
function onMissionParamConfirm (confirm: boolean) {
  missionParams.visible = false
  if (!confirm) return
  // 校验
  if (
    missionParams.lat == null || missionParams.lng == null || missionParams.alt == null ||
    missionParams.approachSpeed == null || missionParams.orbitRadius == null || missionParams.orbitPoints == null
  ) {
    message.error('参数不完整')
    return
  }
  message.success('参数已更新')
}

// 1)取机巢坐标的小工具：优先OSD，其次任务参数兜底
function getHomeFromDevice (): { lat: number; lng: number } | null {
  // 尝试从设备信息拿机巢经纬度（按你给的结构推断，可按实际字段名改）
  const lat = (props.deviceInfo as any)?.dock?.basic_osd?.latitude
  const lng = (props.deviceInfo as any)?.dock?.basic_osd?.longitude
  console.log('lat:' + lat)
  console.log('lng:' + lng)
  if (typeof lat === 'number' && typeof lng === 'number') return { lat, lng }

  // 兜底：在「参数设置」里也让用户填过 homeLat/homeLng，则可用：
  if (missionParams?.lat != null && missionParams?.lng != null) {
    return { lat: missionParams.lat, lng: missionParams.lng }
  }

  // 再兜底：如果你有后端接口能取home（例如 /api/device/{sn}/home），可以在这里同步取一次
  // const { data } = await axios.get(`/api/device/${props.sn}/home`);
  // return data ? { lat: data.lat, lng: data.lng } : null;

  return null
}

// —— 一键起飞：复用 takeoffToPoint，飞到目标上空（或就近点位）
// 2) 一键起飞 → 机巢正上方悬停
async function onOneKeyTakeoff () {
  if (missionBusy.value) return

  const home = getHomeFromDevice()
  if (!home) {
    message.error('找不到机巢坐标：请确认设备OSD或在参数里填写 homeLat/homeLng')
    return
  }
  if (missionParams.homeHoverAlt == null) {
    message.error('请设置机巢悬停高度（homeHoverAlt）')
    return
  }
  if (missionParams.securityTakeoffAlt == null) {
    message.error('请设置安全起飞高度（securityTakeoffAlt）')
    return
  }
  if (missionParams.flightAlt == null) {
    message.error('请设置指挥飞行高度高度（flightAlt）')
    return
  }

  missionBusy.value = true
  missionState.value = 'takingOff'
  try {
    await takeoffToPoint(props.sn, {
      target_latitude: home.lat,
      target_longitude: home.lng,
      target_height: missionParams.homeHoverAlt, // 悬停高度
      security_takeoff_height: missionParams.securityTakeoffAlt, // Math.max(20, Math.min(50, missionParams.homeHoverAlt - 10)),
      rth_altitude: Math.max(30, missionParams.securityTakeoffAlt),
      max_speed: Math.min(MAX_SPEED, 8), // 统一用下划线；速度取保守值
      rc_lost_action: LostControlActionInCommandFLight.RETURN_HOME,
      exit_wayline_when_rc_lost: WaylineLostControlActionInCommandFlight.EXEC_LOST_ACTION,
      rth_mode: ERthMode.SETTING,
      commander_mode_lost_action: ECommanderModeLostAction.CONTINUE,
      commander_flight_mode: ECommanderFlightMode.SETTING,
      commander_flight_height: missionParams.flightAlt,
    })
    message.success('已起飞并前往机巢上空悬停')
    missionState.value = 'hovering'
  } catch (e) {
    message.error('起飞失败')
    missionState.value = 'idle'
  } finally {
    missionBusy.value = false
  }
}

// —— 抵近观察：飞到目标点上空并自动AIM
async function onApproachObserve () {
  if (missionBusy.value) return
  if (missionParams.lat == null || missionParams.lng == null || missionParams.alt == null) {
    message.error('请先设置目标与作业高度')
    return
  }
  missionBusy.value = true
  missionState.value = 'approaching'
  try {
    await flyToPoint(props.sn, {
      max_speed: Math.min(MAX_SPEED, missionParams.approachSpeed),
      points: [{ latitude: missionParams.lat, longitude: missionParams.lng, height: missionParams.alt }],
    })

    // 自动对准目标（基于画面中心对准），不依赖经纬度→云台角换算，避免坐标/姿态换算复杂度
    if (missionParams.lockAim && missionParams.cameraType !== null) {
      await cameraAim(props.sn, {
        payload_index: payloadSelectInfo.payloadIndex,
        camera_type: missionParams.cameraType,
        locked: true,
        x: 0.5, // 画面中心
        y: 0.5,
      })
    }
    message.success('已抵近目标进行观察')
  } catch (e) {
    message.error('抵近失败')
    missionState.value = 'idle'
  } finally {
    missionBusy.value = false
  }
}

// —— 抵近盘旋：在目标点生成一圈离散点，复用 flyToPoint 执行简易圆轨迹
function haversineOffsetLatLng (lat: number, lng: number, dn: number, de: number) {
  // 小范围近似：根据北向/东向米值偏移到经纬度（简单够用）
  const R = 6378137
  const dLat = dn / R
  const dLng = de / (R * Math.cos((Math.PI * lat) / 180))
  return {
    lat: lat + (dLat * 180) / Math.PI,
    lng: lng + (dLng * 180) / Math.PI,
  }
}

async function onApproachOrbit () {
  if (missionBusy.value) return
  if (
    missionParams.lat == null || missionParams.lng == null || missionParams.alt == null ||
    missionParams.orbitRadius == null || missionParams.orbitPoints == null
  ) {
    message.error('请先设置目标与盘旋参数')
    return
  }
  missionBusy.value = true
  missionState.value = 'orbiting'
  try {
    // 生成圆周离散点（顺时针）
    const pts = []
    for (let i = 0; i < missionParams.orbitPoints; i++) {
      const theta = (2 * Math.PI * i) / missionParams.orbitPoints
      const de = missionParams.orbitRadius * Math.cos(theta) // 东向
      const dn = missionParams.orbitRadius * Math.sin(theta) // 北向
      const p = haversineOffsetLatLng(missionParams.lat!, missionParams.lng!, dn, de)
      pts.push({ latitude: p.lat, longitude: p.lng, height: missionParams.alt })
    }
    // 把最后一个点收拢到第一个点，闭环更圆滑
    pts.push(pts[0])

    await flyToPoint(props.sn, {
      max_speed: Math.min(MAX_SPEED, Math.max(4, missionParams.approachSpeed - 2)), // 盘旋减速更稳
      points: pts,
    })

    if (missionParams.lockAim && missionParams.cameraType !== null) {
      await cameraAim(props.sn, {
        payload_index: payloadSelectInfo.payloadIndex,
        camera_type: missionParams.cameraType,
        locked: true,
        x: 0.5,
        y: 0.5,
      })
    }
    message.success('盘旋轨迹已下发')
  } catch (e) {
    message.error('盘旋指令失败')
    missionState.value = 'idle'
  } finally {
    missionBusy.value = false
  }
}

// —— 结束任务：停止指令飞行并解除AIM锁定（可按需加返航）
async function onEndMission () {
  if (missionBusy.value) return
  missionBusy.value = true
  try {
    await stopFlyToPoint(props.sn)
    if (missionParams.cameraType !== null) {
      // 解除锁定但不强制改云台角
      await cameraAim(props.sn, {
        payload_index: payloadSelectInfo.payloadIndex,
        camera_type: missionParams.cameraType,
        locked: false,
        x: 0.5,
        y: 0.5,
      })
    }
    missionState.value = 'idle'
    message.success('任务已结束')
  } catch (e) {
    message.error('结束任务失败')
  } finally {
    missionBusy.value = false
  }
}

// // 2) 从你现有的 cmdList 里找到“返航”这项（和按钮一致）
// const rthItem = computed(() =>
//   cmdList.value.find(i => i?.operateText === 'Return Home' || i?.cmdKey === 'RETURN_HOME')
// )

// // 3) 实时监测：当电量==88% 时，直接调用按钮用的函数
// let fired = false
// watchEffect(() => {
//   if (batteryPct.value === 94 && !fired && rthItem.value && !rthItem.value.loading) {
//     fired = true
//     console.log('[AUTO-RTH] 电量 = 90%，触发返航')
//     // 与按钮完全相同的调用：sendControlCmd(cmdItem, 0)
//     // @ts-ignore
//     sendControlCmd(rthItem.value, 0)
//   }
//   // 离开 88% 时解除闩锁，便于你再次测试
//   if (batteryPct.value !== 88) fired = false
// })

// ========== 找到按钮里“返航”那一项（跟按钮同一命令）==========
const rthItem = computed(() =>
  cmdList.value.find(i => i?.operateText === 'Return Home' || i?.cmdKey === 'RETURN_HOME')
)

// ========== 只要成功一次就停止 ==========
const autoRthDone = ref(false) // 成功返航后，永久不再触发（可按需重置）
let lastAttempt = 0
const FAIL_COOLDOWN_MS = 30_000 // 失败后冷却 30s 再试（防止刷指令）

// 一个小工具：稳健判断 sendDockControlCmd 返回是否“成功”
function isOk (res: any) {
  return res === true ||
         res?.code === 0 ||
         res?.success === true ||
         res?.data?.result === 0 ||
         res?.data?.code === 0
}

// ========== 触发：电量==88 且未成功过 ==========
watchEffect(async () => {
  if (autoRthDone.value) return
  if (batteryPct.value !== 93) return
  if (!rthItem.value || rthItem.value.loading) return

  const now = Date.now()
  if (now - lastAttempt < FAIL_COOLDOWN_MS) return
  lastAttempt = now

  // 复用按钮同款命令；为了拿到“成功/失败”，这里直接调底层 sendDockControlCmd
  try {
    const payload = { sn: props.sn, cmd: rthItem.value.cmdKey, action: rthItem.value.action }
    const res = await sendDockControlCmd(payload, false)
    // console.log('[AUTO-RTH] raw result:', res)

    if (isOk(res)) {
      autoRthDone.value = true // ✅ 成功后锁死，不再触发
      message.success('已自动下发返航')
    } else {
      message.error(res?.msg || res?.message || '自动返航失败，稍后重试')
    }
  } catch (e: any) {
    message.error(e?.message || '自动返航异常，稍后重试')
  }
})

</script>

<style lang="scss" scoped>
:deep(drone-control-popconfirm .ant-input-number-input ){
  color: #303030 !important;
}
.container {
  display: flex;         /* 使用 Flexbox 布局 */
  width: 100%;          /* 设定宽度为 100% */
  background: #102B66;
  .left, .right {
    margin: 5px;
    background:#0E2557;
    width: 50%;           /* 每一侧占 50% 的宽度 */
    // padding: 10px;        /* 添加一些内边距 */
    // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    // border: 1px solid #719fff;
   .box-title{
     color: #f0f0f0;
     font-size: 16px;
     text-align: center;
   }
   .UAV-control{
    height: 180px;
    overflow: hidden;
    .flex-center{
        display: flex;
        justify-content: center;
        align-items: center;
    }
    .box-btn{
        width: 30px;
        height: 30px;
        // background-color: antiquewhite;
        position: relative;
        cursor: pointer;
        background: #012b78;
    }
   }
   .btn-list{
      display: grid;
      grid-template-columns: repeat(2, 1fr); /* 两列布局 */
      gap: 10px;
      margin-bottom: 8px;
   }
  }
}
.left {
//   background-color: #f0f0f0; /* 左侧背景色 */
  border-right: none;
}

.right {
//   background-color: #d0d0d0; /* 右侧背景色 */
}
// 按钮
.btn{
    background: rgba(59, 116, 255, 0.15);
    -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    border: 1px solid #719fff;
    border-radius: 4px;
    width:  100px;
    height: 36px;
    font-family: PingFangSC, PingFang SC;
    font-weight: 500;
    font-size: 14px;
    margin-left: 0 !important;
    color: #ffffff;
    &:hover{
    background: skyblue;
    }

}
// 控制遥控器按钮
.sector {
  box-sizing: content-box;
  position: relative;
  left:0;
  top: -47px;
  right: 30px;
  bottom: 6px;
  width: 145px;
  height: 145px;
  margin: 0 auto;
  border: 1px solid #0a1e41;
  border-radius: 50%;
  background: #0a1e41;
  overflow: hidden;
  transform: rotate(-5deg);
}

.sector .box {
  position: absolute;
  width: 145px;
  height: 145px;
  border-radius: 0%;
  border: none;
  box-shadow: none;
  clip-path: url(#sector);
}

.sector .box {
  transition: 0.5s;
  cursor: pointer;
  background: #012b78;
}
.sector .center {
  transition: 0.5s;
  cursor: pointer;
  background: #005cb4;
}
.sector .box:hover,
.sector .center:hover {
  background: skyblue;
}

.sector .s1 {
  transform: rotate(0deg);
}

.sector .s2 {
  transform: rotate(90deg);
}

.sector .s3 {
  transform: rotate(180deg);
}

.sector .s4 {
  transform: rotate(270deg);
}
.sector .center {
  width: 68px;
  height: 68px;
  position: absolute;
  left: 36px;
  top: 36px;
  box-sizing: content-box;
  border: 2px solid #0a1e41;
  border-radius: 50%;
}

.arrow {
  float: none;
  transform: rotate(-90.5deg);
  width: 100%;
  height: 100%;

  margin: 0;
  padding: 0;
  padding-top: 11px;
  //padding-left: 2px;
  box-sizing: border-box;
  border: none;
  color: white;
  text-align: center;
  font-size: 18px;
}
.arrow-reset{
  // float: none;
  transform: rotate(5deg);
  // transform: rotate(5deg);
  width: 100%;
  height: 100%;
  // margin: 0;
  // padding: 0;
  // padding-top: 11px;
  //padding-left: 2px;
  box-sizing: border-box;
  border: none;
  color: white;
  // text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: PingFangSC;
  font-weight: 500;
  font-size: 14px;
  color: #ffffff;
  font-style: normal;
  letter-spacing: 2px;
  // writing-mode: vertical-rl;
}
.message-box{
    -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    border: 1px solid #719fff;
    background: transparent !important;
}

.device-info{
  position: absolute;
  z-index: 100;
  left: 50px;
  top: 100px;
  padding: 5px;

  text-align: center;
  width: 450px;
  height: fit-content;
  background: #303030;
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  border: 1px solid #686868;
  border-radius: 4px;

  .drag-title{
    border-bottom: 1px solid #626262;
    height: 50px;
    width: 100%;
    line-height: 40px ;
    color: aliceblue;
  }
}

.mission-panel{
  margin: 8px 5px 0;
  background:#0E2557;
  padding: 10px;
  border: 1px solid #719fff44;
  .box-title{
    color:#f0f0f0;
    font-size:16px;
    text-align:left;
    margin-bottom:8px;
  }
  .mission-btns{
    grid-template-columns: repeat(4, 1fr);
  }
  .mission-status{
    margin-top: 6px;
    color: #cfe3ff;
    font-size: 12px;
    opacity: .9;
  }
}

</style>
