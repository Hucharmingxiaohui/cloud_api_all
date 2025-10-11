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
                <el-button class="btn">指令飞行</el-button>
                <el-button class="btn">取消飞行</el-button>
            </div>
            <div class="btn-list">
                <el-button class="btn">返航</el-button>
                <el-button class="btn">取消返航</el-button>
            </div>

            <!-- 弹窗 -->
        </div>
        <div class="right">
            <div class="box-title">
            负载控制
            </div>
            <!-- 遥控器 -->
            <el-row
            :gutter="10"
            style="display: flex; align-items: center;"
            >
            <el-col :span="20" >
                <div class="UAV-control">
                    <div class="flex-center"  style="margin-top: 5px;">
                        <img src="../../../assets/v3/icon/rightBottom2.png"  style="width: 100px; height: 70px;">
                        <img src="../../../assets/v3/icon/rightBottom3.png"  style="width: 100px; height: 70px;"/>
                    </div>
                    <div class="flex-center">
                        <div class="sector">
                            <div class="box s1">
                            <div
                                @mousedown="changeDirection(1)"
                                @mouseup="changeDirection(100)"
                                class="arrow"
                                id="id-10"
                            >
                                <i class="iconfont icon-qiehuanqishang"></i>
                            </div>
                            </div>
                            <div class="box s2">
                            <div
                                @mousedown="changeDirection(2)"
                                @mouseup="changeDirection(200)"
                                class="arrow"
                                id="id-11"
                            >
                                <i class="iconfont icon-qiehuanqishang"></i>
                            </div>
                            </div>
                            <div class="box s3">
                            <div
                                @mousedown="changeDirection(3)"
                                @mouseup="changeDirection(300)"
                                class="arrow"
                                id="id-12"
                            >
                                <i class="iconfont icon-qiehuanqishang"></i>
                            </div>
                            </div>
                            <div class="box s4">
                            <div
                                @mousedown="changeDirection(4)"
                                @mouseup="changeDirection(500)"
                                class="arrow"
                                id="id-13"
                            >
                                <i class="iconfont icon-qiehuanqishang"></i>
                            </div>
                            </div>
                            <div class="center" id="id-18" @click="changeDirection(13)">
                            <div
                                @mousedown="changeDirection(5)"
                                @mouseup="changeDirection(500)"
                                class="arrow-reset"
                                id="id-14"
                            >
                                <span>重置</span>
                            </div>

                            </div>
                        </div>
                    </div>
                </div>
            </el-col>
            <el-col :span="4" >
                <div class="button-container">
                    <button
                        class="button-circle"
                        :class="{ 'button-disabled': operation_data.PTZ_sensitivity >= 50 }"
                        @mousedown="changeDirection(6)"
                        @mouseup="changeDirection(1000)"
                        :disabled="operation_data.PTZ_sensitivity >= 50"
                    >
                        <i class="iconfont icon-plus"></i>
                    </button>
                    <span class="text-show">灵敏度</span>
                    <button
                        class="button-circle"
                        :class="{ 'button-disabled': operation_data.PTZ_sensitivity <= 1 }"
                        @mousedown="changeDirection(7)"
                        @mouseup="changeDirection(900)"
                        :disabled="operation_data.PTZ_sensitivity <= 1"
                    >
                        <i class="iconfont icon-minus"></i>
                    </button>
                    <span class="number-show">{{operation_data.PTZ_sensitivity}}</span>
                </div>

            </el-col>
            </el-row>

            <div>
                <!-- <div class="setup-box" style="margin-top: 20px">
                    <div class="flex-center">
                        <span style="width: 50px; display: inline-block">灵敏度:</span>
                        <span class="number-show">{{operation_data.PTZ_sensitivity}}</span>
                    </div>
                    <div>
                        <button
                            class="button-circle"
                            :class="{ 'button-disabled': operation_data.PTZ_sensitivity >= 50 }"
                            @mousedown="changeDirection(6)"
                            @mouseup="changeDirection(1000)"
                            :disabled="operation_data.PTZ_sensitivity >= 50"
                        >
                            <i class="iconfont icon-plus"></i>
                        </button>
                        <button
                            class="button-circle"
                            :class="{ 'button-disabled': operation_data.PTZ_sensitivity <= 1 }"
                            @mousedown="changeDirection(7)"
                            @mouseup="changeDirection(900)"
                            :disabled="operation_data.PTZ_sensitivity <= 1"
                        >
                            <i class="iconfont icon-minus"></i>
                        </button>
                    </div>
                </div> -->

                <div class="setup-box" style="margin-top: 20px">
                    <div class="flex-center">
                        <span style="width: 50px; display: inline-block">变 焦:</span>
                        <span class="number-show">{{operation_data.zoom_factor}}</span>
                    </div>
                    <div>
                        <button
                            class="button-circle"
                            :class="{ 'button-disabled': operation_data.zoom_factor >= 200 }"
                            @mousedown="changeDirection(8)"
                            @mouseup="changeDirection(1000)"
                            :disabled="operation_data.zoom_factor >= 200"
                        >
                            <i class="iconfont icon-plus"></i>
                        </button>
                        <button
                            class="button-circle"
                            :class="{ 'button-disabled': operation_data.zoom_factor <= 2 }"
                            @mousedown="changeDirection(9)"
                            @mouseup="changeDirection(900)"
                            :disabled="operation_data.zoom_factor <= 2"
                        >
                            <i class="iconfont icon-minus"></i>
                        </button>
                    </div>
                </div>
            </div>
            <!-- 负载控制按钮 -->
            <div class="btn-list" style="margin-top: 20px">
                <span>
                    <el-button class="btn" @click="getControlAuth">申请权限</el-button>
                </span>
                <span>
                    <el-button class="btn" @click="releaseControlAuth">释放权限</el-button>
                </span>
                <span>
                    <el-button class="btn" v-if="!operation_data.recording_state" @click="Recording">开始录像</el-button>
                    <el-button class="btn" v-else @click="Recording">停止录像</el-button>
                </span>
                <span>
                    <el-select v-model="operation_data.camera_type_selected" placeholder="类型切换" :teleported='false' class="select-operation" @change="switchCameraType" >
                    <el-option
                        v-for="item in operation_data.camera_type"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"

                    />
                    </el-select>
                </span>
                <span >
                    <el-button class="btn" style="position: relative;"  @click="openSetup">
                    设置
                    <i class="iconfont icon-shezhi" style="margin-left: 10px; position: absolute; right: 15px;top: 10px;"></i>
                    </el-button>
                </span>
            </div>
            <div v-if="operation_data.show_setup">
                <div class="setup-box" style="margin-top: 20px">
                    <span>相机曝光值:</span>
                    <span class="number-show">{{operation_data.exposureSet}}</span>
                </div>
                <div style="padding:0 10px; margin-bottom: 15px;">
                    <el-slider v-model="operation_data.exposureSet" :show-tooltip="false" :min="1" :max="31" @mouseup="changeExposureSet"/>
                </div>
                <div class="btn-list">
                    <span>
                        <el-select v-model="operation_data.camera_mode_selected" placeholder="拍照模式切换" :teleported='false' class="select-operation" @change="switchCameraMode" >
                            <el-option
                            v-for="item in operation_data.camera_mode"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"

                            />
                        </el-select>
                    </span>
                    <span>
                        <el-select v-model="operation_data.reset_loaction_selected" placeholder="云台重置选项" :teleported='false' class="select-operation">
                            <el-option
                            v-for="item in operation_data.reset_loaction"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                            />
                        </el-select>
                    </span>
                    <span>
                        <el-select v-model="operation_data.exposure_mode_selected" placeholder="相机曝光模式" :teleported='false' class="select-operation" @change="switchExposureMode">
                            <el-option
                            v-for="item in operation_data.exposure_mode"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                            />
                        </el-select>
                    </span>
                </div>

            </div>

        </div>
    </div>
</template>
<script setup>
import { defineProps, reactive, ref, watch, computed, onMounted, watchEffect } from 'vue'

import { requestControlAuto, deleteControlAuto, changeVideoType, changeCameraMode, cameraRecording, cameraPhotoTake, PTZControl, PTZReset, FocalLengthSet, exposureValueSet, exposureModeSet } from '/@/api/pilot-control/pilot-control'
import { ElMessage } from 'element-plus'

const props = defineProps({
  sn: {
    type: String,
    required: true
  },
  deviceInfo: {
    type: Object,
    required: true
  },
  payloads: {
    type: Array,
    default: () => null
  }
})
const operation_data = reactive({
  authTips: '当前用户暂无操作权限!',
  camera_type: [ // 相机类型
    {
      label: '红外',
      value: 'ir'
    },
    {
      label: '广角',
      value: 'wide'
    },
    {
      label: '变焦',
      value: 'zoom'
    },

  ],
  camera_type_selected: '', // 被选中的类型值
  PTZ_sensitivity: 2.0, // 云台灵敏度
  zoom_factor: 10, // 变焦倍数
  islocked: true, // 相机和飞行器是否锁定
  show_setup: false,
  exposureSet: 1, // 相机曝光值
  reset_loaction: [
    {
      label: '回中',
      value: '0'
    },
    {
      label: '向下',
      value: '1'
    },
    {
      label: '偏航回中',
      value: '2'
    },
    {
      label: '俯仰向下',
      value: '3'
    },

  ],
  reset_loaction_selected: '',
  camera_mode: [ // 相机类型
    {
      label: '拍照',
      value: '0'
    },
    {
      label: '录像',
      value: '1'
    },
    {
      label: '智能低光',
      value: '2'
    },
    {
      label: '全景拍照',
      value: '3'
    },

  ],
  camera_mode_selected: '',
  exposure_mode: [
    {
      label: '自动',
      value: '1'
    },
    {
      label: '快门优先曝光',
      value: '2'
    },
    {
      label: '光圈优先曝光',
      value: '3'
    },
    {
      label: '手动曝光',
      value: '4'
    },
  ],
  exposure_mode_selected: '',
  hasAuto: false, // 是否拥有操作权限
  recording_state: false, // 开始录像
  takePhoto_state: false, // 开始拍照

})
// ---------------------------------------------------------权限操作-------------------------------------------------------------------------------------------
const gatewaySn = '5YSZL4V00325YL' // 遥控器编号
const deviceSn = '1581F5FHD23BT00DRZM6' // 飞行器权限
const autuValue = { // 权限body
  user_id: 'dongfangpilot',
  user_callsign: 'df1500',
  control_keys: [
    'flight'
  ]
}
// 计时器
// const count = ref(0)
const intervalId = null

onMounted(() => {

})
// // 请求控制权，循环请求
// async function getControlAuth () {
//   count.value = 0

//   const startTimer = () => {
//     intervalId = setInterval(async () => {
//       try {
//         const authData = await getAuth()
//         console.log(authData.code)
//         if (authData === null || authData.code !== 0) {
//           // 处理获取权限失败的情况
//           ElMessage({
//             message: authData.message,
//             type: 'error',
//           })
//           clearInterval(intervalId) // 清除定时器
//           operation_data.hasAuto = false
//           operation_data.authTips = '当前用户暂无操作权限!'
//           return
//         }
//         // 如果获取权限成功
//         if (authData.data.output.status === 'in_progress') {
//           count.value++
//           if (count.value > 20) {
//             ElMessage({
//               message: 'pilot请求响应超时!',
//               type: 'error',
//             })
//             clearInterval(intervalId) // 清除定时器
//             operation_data.authTips = '当前用户暂无操作权限!'
//             return
//           }
//           operation_data.hasAuto = false
//           operation_data.authTips = '操作权限中，等待通过!'
//         }
//         if (authData.data.output.status === 'ok') {
//           operation_data.hasAuto = true
//           operation_data.authTips = '操作权限申请通过,请继续!'
//         }
//       } catch (error) {
//         // console.error('Error fetching auth data:', error)
//         clearInterval(intervalId) // 清除定时器
//         operation_data.hasAuto = false
//       }
//     }, 1000) // 每秒调用一次
//   }

//   startTimer()
// }
// // 循环主体方法
// async function getAuth () {
//   try {
//     const mothod = 'cloud_control_auth_request' // 请求云端控制权限
//     const res = await requestControlAuto(gatewaySn, deviceSn, mothod, autuValue)
//     return res
//   } catch (error) {
//     console.error('Error in requestControlAuto:', error)
//     return null
//   }
// }

function getControlAuth () {
  const mothod = 'cloud_control_auth_request' // 请求云端控制权限
  requestControlAuto(gatewaySn, deviceSn, mothod, autuValue).then(res => {
    if (res.code !== 0) {
      return
    }

    operation_data.hasAuto = true // 释放获得权限
    operation_data.authTips = '拥有权限!'
  })
}
// 释放控制权
function releaseControlAuth () {
  clearInterval(intervalId) // 停止定时器
  const release_mothod = 'cloud_control_release' // 释放云端控制权限
  const body = { control_keys: ['flight'] }
  deleteControlAuto(gatewaySn, deviceSn, release_mothod, body).then(res => {
    console.log(res.code)
    // if (res.code !== 0) {
    //   return
    // }

    operation_data.hasAuto = false // 释放获得权限
    operation_data.authTips = '当前用户暂无操作权限!'
  })
}

//= ==================================================================================负载操作============================================================================
// 切换镜头类型
function switchCameraType () {
  if (!operation_data.hasAuto) {
    ElMessage({
      message: '暂无操作权限,请申请!',
      type: 'warning',
    })
    return
  }
  const method = 'live_lens_change'
  if (operation_data.camera_type_selected === '') {
    ElMessage({
      message: '请选择要切换的类型',
      type: 'warning',
    })
    return
  } else if (operation_data.camera_type_selected === 'ir') {
    ElMessage({
      message: '当前机型并不支持红外',
      type: 'warning',
    })
    return
  }
  const body = {
    video_id: `${deviceSn}/77-0-0/${operation_data.camera_type_selected}-0`,
    video_type: operation_data.camera_type_selected
  }

  changeVideoType(gatewaySn, deviceSn, method, body).then(res => {
    if (res.code !== 0) {
      return
    }
    ElMessage({
      message: '切换成功',
      type: 'success',
    })
  })
}

// 切换相机模式
function switchCameraMode () {
  if (!operation_data.hasAuto) {
    ElMessage({
      message: '暂无操作权限,请申请!',
      type: 'warning',
    })
    return
  }
  const method = 'camera_mode_switch'
  // console.log('dfefggg', operation_data.camera_mode_selected)
  if (operation_data.camera_mode_selected === '') {
    ElMessage({
      message: '请选择要切换的类型',
      type: 'warning',
    })
    return
  }
  const body = {
    camera_mode: operation_data.camera_mode_selected,
    payload_index: '77-0-0'
  }
  changeCameraMode(gatewaySn, deviceSn, method, body).then(res => {
    if (res.code !== 0) {
      return
    }
    ElMessage({
      message: '相机模式切换成功',
      type: 'success',
    })
    // console.log('相机模式切换成功')
  })
}
// // 切换曝光模式
function switchExposureMode () {
  if (!operation_data.hasAuto) {
    ElMessage({
      message: '暂无操作权限,请申请!',
      type: 'warning',
    })
    return
  }
  const method = 'camera_exposure_mode_set'
  const body = {
    camera_type: 'zoom',
    exposure_mode: operation_data.exposure_mode_selected,
    payload_index: '39-0-7'
  }
  exposureModeSet(gatewaySn, deviceSn, method, body).then(res => {
    if (res.code !== 0) {
      return
    }
    ElMessage({
      message: '切换成功',
      type: 'success',
    })
  })
}

// 开始/停止录像
function Recording () {
  if (!operation_data.hasAuto) {
    ElMessage({
      message: '暂无操作权限,请申请!',
      type: 'warning',
    })
    return
  }
  if (operation_data.camera_mode_selected !== '1') { // 判断是否切换到录像模式
    // operation_data.camera_mode_selected = '1'
    // switchCameraMode()
    ElMessage({
      message: '请切换为录像模式!',
      type: 'warning',
    })
    return
  }
  let method = null
  if (!operation_data.recording_state) {
    method = 'camera_recording_start'
  } else {
    method = 'camera_recording_stop'
  }

  const body = {
    payload_index: '77-0-0'
  }
  console.log('sdf', body)
  cameraRecording(gatewaySn, deviceSn, method, body).then(res => {
    if (res.code !== 0) {
      return
    }
    operation_data.recording_state = !operation_data.recording_state
  })
}

// 开始/停止拍照
function takePhoto () {
  if (!operation_data.hasAuto) {
    ElMessage({
      message: '暂无操作权限,请申请!',
      type: 'warning',
    })
    return
  }
  console.log('sdsdfs', operation_data.camera_mode_selected)
  if (operation_data.camera_mode_selected !== '0' && operation_data.camera_mode_selected === '3') { // 判断是否切换到拍照模式
    // operation_data.camera_mode_selected = '0' // 默认为拍照模式
    // switchCameraMode()
    ElMessage({
      message: '请切换为拍照模式!',
      type: 'warning',
    })
    return
  }
  let method = null
  if (!operation_data.recording_state) {
    method = 'camera_photo_take'
  } else {
    method = 'camera_photo_stop'
  }

  const body = {
    payload_index: '77-0-0'
  }
  cameraPhotoTake(gatewaySn, deviceSn, method, body).then(res => {
    if (res.code !== 0) {
      return
    }
    if (operation_data.camera_mode_selected === '3') { // 全景拍照有开始拍照和停止拍照
      operation_data.takePhoto_state = !operation_data.takePhoto_state
    }
  })
}

// 云台操作 移动
function changeDirection (value) {
  if (!operation_data.hasAuto) {
    ElMessage({
      message: '暂无操作权限,请申请!',
      type: 'warning',
    })
    return
  }
  let method = 'camera_screen_drag' // 云台操作
  const body = {
    locked: operation_data.islocked,
    payload_index: '77-0-0',
    pitch_speed: 0.0,
    yaw_speed: 0.0
  }
  const body1 = { // 云台重置
    payload_index: '77-0-0',
    reset_mode: operation_data.reset_loaction_selected === '' ? 0 : operation_data.reset_loaction_selected
  }
  const body2 = {
    camera_type: 'zoom',
    payload_index: '77-0-0',
    zoom_factor: 2
  }
  switch (value) {
    case 1: // 左 operation_data.PTZ_sensitivity
      body.yaw_speed = operation_data.PTZ_sensitivity * -1
      body.pitch_speed = 0
      PTZControl(gatewaySn, deviceSn, method, body).then(res => {
        if (res.code !== 0) {
          ElMessage({
            message: '操作失败!',
            type: 'error',
          })
        }
      })
      break
    case 2: // 上 operation_data.PTZ_sensitivity
      body.pitch_speed = operation_data.PTZ_sensitivity
      body.yaw_speed = 0
      console.log(body)
      PTZControl(gatewaySn, deviceSn, method, body).then(res => {
        if (res.code !== 0) {
          ElMessage({
            message: '操作失败!',
            type: 'error',
          })
        }
      })
      break
    case 3: // 右 operation_data.PTZ_sensitivity
      body.yaw_speed = operation_data.PTZ_sensitivity
      body.pitch_speed = 0
      PTZControl(gatewaySn, deviceSn, method, body).then(res => {
        if (res.code !== 0) {
          ElMessage({
            message: '操作失败!',
            type: 'error',
          })
        }
      })
      break
    case 4: // 下
      body.pitch_speed = operation_data.PTZ_sensitivity * -1
      body.yaw_speed = 0
      PTZControl(gatewaySn, deviceSn, method, body).then(res => {
        if (res.code !== 0) {
          ElMessage({
            message: '操作失败!',
            type: 'error',
          })
        }
      })
      break
    case 5: // 云台重置
      method = 'gimbal_reset'
      PTZReset(gatewaySn, deviceSn, method, body1).then(res => {
        if (res.code !== 0) {
          ElMessage({
            message: '操作失败!',
            type: 'error',
          })
        }
      })
      break
    case 6: // 灵敏度调整  加号
      operation_data.PTZ_sensitivity += 1
      break
    case 7: // 灵敏度调整  减号
      operation_data.PTZ_sensitivity -= 1
      break
    case 8: // 变焦  加
      if (operation_data.camera_type_selected !== 'zoom') {
        ElMessage({
          message: '请切换到变焦模式!',
          type: 'warning',
        })
        return
      }
      method = 'camera_focal_length_set'
      operation_data.zoom_factor += 1
      body2.zoom_factor = operation_data.zoom_factor
      FocalLengthSet(gatewaySn, deviceSn, method, body2).then(res => {
        if (res.code !== 0) {
          ElMessage({
            message: '操作失败!',
            type: 'error',
          })
        }
      })
      break
    case 9: // 变焦 减
      if (operation_data.camera_type_selected !== 'zoom') {
        ElMessage({
          message: '请切换到变焦模式!',
          type: 'warning',
        })
        return
      }
      method = 'camera_focal_length_set'
      operation_data.zoom_factor -= 1
      body2.zoom_factor = operation_data.zoom_factor
      FocalLengthSet(gatewaySn, deviceSn, method, body2).then(res => {
        if (res.code !== 0) {
          ElMessage({
            message: '操作失败!',
            type: 'error',
          })
        }
      })
      break
  }
}

// 开启/关闭设置界面
function openSetup () {
  operation_data.show_setup = !operation_data.show_setup
}
// 曝光值设置
function changeExposureSet () {
  if (!operation_data.hasAuto) {
    ElMessage({
      message: '暂无操作权限,请申请!',
      type: 'warning',
    })
    return
  }
  const method = 'camera_exposure_set'
  const body = {
    camera_type: 'zoom',
    payload_index: '39-0-7',
    exposure_value: operation_data.exposureSet
  }
  exposureValueSet(gatewaySn, deviceSn, method, body).then(res => {
    if (res.code !== 0) {
      return
    }
    console.log('exposureValue setup sucess')
  })
}
</script>

<style lang="scss" scoped>
.container {
  display: flex;         /* 使用 Flexbox 布局 */
  width: 100%;          /* 设定宽度为 100% */
  .left, .right {
    width: 50%;           /* 每一侧占 50% 的宽度 */
    padding: 10px;        /* 添加一些内边距 */
    -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    border: 1px solid #719fff;
    overflow: auto;
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
    width:  110px;
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
.setup-box{
  padding: 0 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-family: PingFangSC;
  font-weight: 500;
  font-size: 14px;
  color: #ffffff;
  font-style: normal;
}
.number-show{
  margin-left: 5px;
  height: 25px;
  width: 40px;
  // background-color:rgba(59, 116, 255, 0.15);
  background: linear-gradient(
    to bottom,
    rgba(59, 116, 255, 0.6) 0%,
    rgba(59, 116, 255, 0) 50%,
    rgba(59, 116, 255, 0.6) 100%
  );
  border-radius: 4px;
  border: 1px solid #719fff;
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  color: #ffffff;
  font-weight: 500;
  font-size: 14px;
  display: inline-block; /* 或者 display: block; */
//   align-items: center;
//   justify-content: center;
  text-align: center;
//   display: flex;
//   align-items: center;
//   justify-content: center;
}
.button-circle {
  background: rgba(59, 116, 255, 0.15);
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  color: #ffffff;
  border: none;
  width: 30px;
  height: 30px;
  margin: 0 5px;
  transition: 0.5s;
  border-radius: 50%;
  cursor: pointer;
}
.button-circle:hover {
  background: skyblue;
}
// 下拉框
.select-operation{
    :deep(.el-select__placeholder){
      font-size: 14px;
      font-weight: 500;
      color: #ffffff;
    }
    :deep(.el-select__wrapper) {
      background: rgba(59, 116, 255, 0.15);
      box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
      border: 1px solid #719fff;
      border-radius: 4px;
      width: 110px;
      height: 36px;
      padding-left: 10px;
      text-align: center;
    }
    /**修改下拉图标颜色 */
    :deep(.el-select__caret){
      color: #ffffff;
    }

    :deep(.el-select-dropdown){
      background: #012b78;
      box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
      border: 1px solid #719fff;

    }
    :deep(.el-select-dropdown__item){
      font-size: 14px;
      font-weight: 500;
      color: #ffffff;
    }
    :deep(.el-select-dropdown__item.is-hovering){
      background-color: skyblue;
    }
}
.button-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
//   position: relative;
}
.text-show {
  font-family: PingFangSC;
  font-weight: 500;
  font-size: 14px;
  color: #ffffff;
  font-style: normal;
  letter-spacing: 2px;
  writing-mode: vertical-rl;
  // line-height: 20px; /* 设置合适的行高，确保每行文本的高度一致 */
  height:50px; /* 设置一个固定的高度，确保文本区域高度一致 */
  display: flex;
  justify-content: center;
  text-align: center;
}
</style>
