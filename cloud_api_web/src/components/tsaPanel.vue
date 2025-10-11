<template>
  <div class="content">
    <div class="nav">
      <img class="thumbnail_1" referrerpolicy="no-referrer" src="../assets/v4/nav-icon.png" />
      <span class="text_9">{{ current_sub }}</span>
    </div>
    <div class="nav-select">
      <el-select v-model="current_sub" placeholder="变电站" class="select-operation" :teleported='false'
          style="width: 50px;" @change="changeSub">
          <el-option v-for="item in subOption" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>
    <div class="title">
      设备管理
    </div>
    <div class="content" v-if="onlineDocks.data.length === 0">
      <a-empty :image="noData" :image-style="{ height: '60px' }" />
    </div>
    <div class="content" v-else>
      <div class="device" v-for="dock in onlineDocks.data" :key="dock.sn">
        <div class="dock" >
        <!-- <div class="dock"  v-if="currentDeviceForSub.includes(dock.sn)"> -->
          <div class="group_4 flex-col justify-end">
            <div class="group_30 flex-row justify-between">
              <img
                class="image_6"
                referrerpolicy="no-referrer"
                src="https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG1cda726884908d30359e37844a217e60.png"
              />
              <div class="group_31 flex-col justify-between">
                <div class="box_35 flex-row justify-between">
                  <div class="group_5 flex-col"></div>
                  <span class="text_11" style="margin-left: 10px;">机场</span>
                </div>
                <span class="text_13" style="margin-top: 15px !important;">{{dock.gateway.callsign}}</span>
              </div>
              <div class="group_32 flex-col justify-between">
                <div class="group_6 flex-row" @click="toRemoteDebug($event, dock, true, dockInfo[dock.gateway.sn] && dockInfo[dock.gateway.sn].basic_osd?.mode_code !== EDockModeCode.Disconnected)">
                  <div class="section_2 flex-col"></div>
                  <span class="text_12">控制台</span>
                </div>
              </div>
            </div>
            <div class="group_33 flex-row">
              <div class="group_7 flex-col">
                <div class="group_8 flex-row">
                  <div class="group_9 flex-col">
                    <div class="block_3 flex-col"></div>
                  </div>
                  <!-- {{ (deviceInfo[dock.sn]?.mode_code !== EModeCode.Disconnected) ? '已连接' : '未连接' }} -->
                  <!-- <span class="text_15" v-if="deviceInfo[dock.sn]?.mode_code == 0"> 已连接</span> -->
                  <span class="text_15" v-if="deviceInfo[dock.sn]?.mode_code === EModeCode.Disconnected || !deviceInfo[dock.sn]?.mode_cod"> 未连接</span>
                  <span class="text_15" v-else >已连接</span>
                </div>
              </div>
              <div style="width: 230px; ">
                <div style="width: 170px; margin-left: 60px;" >
                  <div style="display: flex; align-items: center; height: 25px;">
                    <span style="width: 50px;">状态:</span>
                    <span  style="overflow: hidden;width: 120px;" :style="dockInfo[dock.gateway.sn] && dockInfo[dock.gateway.sn].basic_osd?.mode_code !== EDockModeCode.Disconnected ? 'color: #00ee8b' :  'color: red;'">
                      {{ dockInfo[dock.gateway.sn] ? EDockModeCode[dockInfo[dock.gateway.sn].basic_osd?.mode_code] : EDockModeCode[EDockModeCode.Disconnected] }}</span>
                  </div>
                  <div style="display: flex; align-items: center; height: 25px;" >
                    <div v-if="hmsInfo[dock.gateway.sn]" class="flex-align-center flex-row">
                          <div  style="width: 18px; height: 16px; text-align: center; border: 1px solid orange; line-height: 16px;" >
                            <div :class="hmsInfo[dock.gateway.sn][0].level === EHmsLevel.CAUTION ? 'caution-blink' :
                            hmsInfo[dock.gateway.sn][0].level === EHmsLevel.WARN ? 'warn-blink' : 'notice-blink'">
                              <span :style="hmsInfo[dock.gateway.sn].length > 99 ? 'font-size: 11px' : 'font-size: 12px'">{{ hmsInfo[dock.gateway.sn].length }}</span>
                              <span class="fz10">{{ hmsInfo[dock.gateway.sn].length > 99 ? '+' : ''}}</span>
                            </div>
                          </div>
                        <a-popover trigger="click" placement="bottom" background-color="#43C575" v-model:visible="hmsVisible[dock.gateway.sn]"
                          @visibleChange="readHms(hmsVisible[dock.gateway.sn], dock.gateway.sn)"
                          :overlayStyle="{width: '200px', height: '300px'}">
                          <div :class="hmsInfo[dock.gateway.sn][0].level === EHmsLevel.CAUTION ? 'caution' :
                            hmsInfo[dock.gateway.sn][0].level === EHmsLevel.WARN ? 'warn' : 'notice'" style="margin-left: 10px; width: 150px; height: 16px; line-height: 16px;">
                            <span class="word-loop">{{ hmsInfo[dock.gateway.sn][0].message_zh }}</span>
                          </div>
                          <template #content>
                            <a-collapse style="background: black; height: 300px; overflow-y: auto;" :bordered="false" expand-icon-position="right" :accordion="true">
                              <a-collapse-panel v-for="hms in hmsInfo[dock.gateway.sn]" :key="hms.hms_id" :showArrow="false"
                                style=" margin: 0 auto 3px auto; border: 0; width: 140px; border-radius: 3px"
                                :class="hms.level === EHmsLevel.CAUTION ? 'caution' : hms.level === EHmsLevel.WARN ? 'warn' : 'notice'"
                                >
                                <template #header="{ isActive }">
                                  <div class="flex-row flex-align-center" style="width: 130px;">
                                    <div style="width: 110px;">
                                      <span class="word-loop" :class="hms.level === EHmsLevel.CAUTION ? 'caution' : hms.level === EHmsLevel.WARN ? 'warn' : 'notice'">{{ hms.message_zh }}</span>
                                    </div>
                                    <div style="width: 20px; height: 15px; font-size: 10px; z-index: 2 " class="flex-row flex-align-center flex-justify-center"
                                      :class="hms.level === EHmsLevel.CAUTION ? 'caution' : hms.level === EHmsLevel.WARN ? 'warn' : 'notice'"
                                    >
                                      <DoubleRightOutlined :rotate="isActive ? 90 : 0" />
                                    </div>
                                  </div>
                                </template>

                                <a-tooltip :title="hms.create_time">
                                  <div style="color: white;" class="text-hidden">{{ hms.create_time }}</div>
                                </a-tooltip>
                              </a-collapse-panel>
                            </a-collapse>
                          </template>
                        </a-popover>
                      </div>
                      <div v-else class="width-100" style="height: 90%; background: transparent"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="MytaskId.length !== 0" style="display: flex; flex-direction: column; height: 100px; margin-top: 15px; background-color: rgba(0, 52, 152, 0.16);border: 1px solid rgba(0, 64, 147, 1);">
                <div style="display: flex; flex-direction: row;">
                    <div style="width: 140px; height: 60px;line-height: 60px;border: 1px solid rgba(0, 64, 147, 1);" >
                      <div style="width: 120px; height: 40px; margin:10px 0 10px 10px; line-height: 40px; text-align: center; background-color: #43C575;"> {{ Mystatus }}</div>
                    </div>
                    <div style="width: calc(100% - 140px ); height: 60px; display:flex;flex-direction: column;border: 1px solid rgba(0, 64, 147, 1); ">
                      <span style="padding: 5px 10px 5px 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">{{ MytaskId }}</span>
                      <span style="padding: 0 10px 5px 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">执行时间:{{ execute_time }}</span>
                    </div>
                </div>
                <div style="height: 40px; width: 100%;display:flex;align-items: center;justify-content: center;">
                    <el-button style="width: 100px; height: 30px; border: 1px solid rgba(0, 64, 147, 1);  background-color: rgba(0, 52, 152, 0.16);" @click="toTask()">查看任务</el-button>
                    <el-button  style="width: 100px; height: 30px;border: 1px solid rgba(0, 64, 147, 1);  background-color: rgba(0, 52, 152, 0.16);"  @click="showWayline()">查看航线</el-button>
                </div>
            </div>
        </div>
        <div class="drone" >
        <!-- <div class="drone" v-if="currentDeviceForSub.includes(dock.sn)"></div> -->
          <div class="group_12 flex-col justify-end">
            <div class="box_38 flex-row">
              <img
                class="image_3"
                referrerpolicy="no-referrer"
                src="https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG25f9ea5d482babaf860bd065484ce952.png"
              />
              <div class="group_35 flex-col">
                <div class="box_39 flex-row justify-between">
                  <img
                    class="label_2"
                    referrerpolicy="no-referrer"
                    src="https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG4e8b5e45195758a311d29bb503f6b630.png"
                  />
                  <span class="text_22">{{ dock.callsign ?? 'No Drone' }}</span>
                </div>
                <div style="width: 170px; margin-top: 10px;" >
                  <div style="display: flex; align-items: center; height: 25px;">
                    <span style="width: 50px;">状态:</span>
                    <span  style="overflow: hidden;width: 120px;" :style="deviceInfo[dock.sn] && deviceInfo[dock.sn].mode_code !== EModeCode.Disconnected ? 'color: #00ee8b' :  'color: red;'">
                      {{ deviceInfo[dock.sn] ? EModeCode[deviceInfo[dock.sn].mode_code] : EModeCode[EModeCode.Disconnected] }}</span>
                  </div>
                  <div style="display: flex; align-items: center; height: 25px;" >
                    <div v-if="hmsInfo[dock.sn]" class="flex-align-center flex-row">
                        <div  style="width: 18px; height: 16px; text-align: center; border: 1px solid orange; line-height: 16px;">
                          <div :class="hmsInfo[dock.sn][0].level === EHmsLevel.CAUTION ? 'caution-blink' :
                          hmsInfo[dock.sn][0].level === EHmsLevel.WARN ? 'warn-blink' : 'notice-blink'">
                            <span :style="hmsInfo[dock.sn].length > 99 ? 'font-size: 11px' : 'font-size: 12px'">{{ hmsInfo[dock.sn].length }}</span>
                            <span class="fz10">{{ hmsInfo[dock.sn].length > 99 ? '+' : ''}}</span>
                          </div>
                        </div>
                        <a-popover trigger="click" placement="bottom" background-color="#43C575" v-model:visible="hmsVisible[dock.sn]" @visibleChange="readHms(hmsVisible[dock.sn], dock.sn)"
                          :overlayStyle="{width: '200px', height: '300px'}">
                          <div :class="hmsInfo[dock.sn][0].level === EHmsLevel.CAUTION ? 'caution' :
                            hmsInfo[dock.sn][0].level === EHmsLevel.WARN ? 'warn' : 'notice'" style="margin-left: 10px; width: 150px; height: 16px; line-height: 16px;background: transparent !important;;">
                            <span class="word-loop">{{ hmsInfo[dock.sn][0].message_zh }}</span>
                          </div>
                          <template #content>
                            <a-collapse style="background: transparent; height: 300px; overflow-y: auto;" :bordered="false" expand-icon-position="right" :accordion="true">
                              <a-collapse-panel v-for="hms in hmsInfo[dock.sn]" :key="hms.hms_id" :showArrow="false"
                                style=" margin: 0 auto 3px auto; border: 0; width: 140px; border-radius: 3px"
                                :class="hms.level === EHmsLevel.CAUTION ? 'caution' : hms.level === EHmsLevel.WARN ? 'warn' : 'notice'"
                                >
                                <template #header="{ isActive }">
                                  <div class="flex-row flex-align-center" style="width: 130px;">
                                    <div style="width: 110px;">
                                      <span class="word-loop" :class="hms.level === EHmsLevel.CAUTION ? 'caution' : hms.level === EHmsLevel.WARN ? 'warn' : 'notice'">{{ hms.message_zh }}</span>
                                    </div>
                                    <div style="width: 20px; height: 15px; font-size: 10px; z-index: 2 " class="flex-row flex-align-center flex-justify-center"
                                      :class="hms.level === EHmsLevel.CAUTION ? 'caution' : hms.level === EHmsLevel.WARN ? 'warn' : 'notice'"
                                    >
                                      <DoubleRightOutlined :rotate="isActive ? 90 : 0" />
                                    </div>
                                  </div>
                                </template>

                                <a-tooltip :title="hms.create_time">
                                  <div style="color: white;" class="text-hidden">{{ hms.create_time }}</div>
                                </a-tooltip>
                              </a-collapse-panel>
                            </a-collapse>
                          </template>
                        </a-popover>
                      </div>
                      <div v-else class="width-100" style="height: 90%; background: transparent"></div>
                  </div>
                </div>
              </div>
              <img
                class="label_3"
                referrerpolicy="no-referrer"
                src="https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG1e4255ec5a9d78aaf5f4cbb350bad7a2.png"
              />
            </div>
            <div class="box_42 flex-row">
              <div class="box_11 flex-col">
                <div class="block_6 flex-col"  @click="switchVisible($event, dock, true, dockInfo[dock.gateway.sn] && dockInfo[dock.gateway.sn].basic_osd?.mode_code !== EDockModeCode.Disconnected)">
                  <span class="text_27">设备详情</span>
                  <img
                    class="image_4"
                    referrerpolicy="no-referrer"
                    src="https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG02bdd71a41b72089a809b3a17f718437.png"
                  />
                </div>
              </div>
              <div class="box_12 flex-row" style="border: 1px solid rgba(0, 64, 147, 1);">
                <div class="box_13 flex-col"></div>
                <div class="text-wrapper_8 flex-col" @click="openLiveStream(dock)">
                  <span class="text_28">监控直播</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>

</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref, watch, WritableComputedRef } from 'vue'
import { EDeviceTypeName, ELocalStorageKey } from '/@/types'
import noData from '/@/assets/icons/no-data.png'
import rc from '/@/assets/icons/rc.png'
import { OnlineDevice, EModeCode, OSDVisible, EDockModeCode, DeviceOsd } from '/@/types/device'
import { useMyStore } from '/@/store'
import { getDeviceTopo, getUnreadDeviceHms, updateDeviceHms, getPlatformInfo, getAllWorkspaceInfo } from '/@/api/manage'
import { RocketOutlined, EyeInvisibleOutlined, EyeOutlined, RobotOutlined, DoubleRightOutlined } from '@ant-design/icons-vue'
import { EHmsLevel } from '/@/types/enums'
import { useRouter } from 'vue-router'

// ------------任务 ---------------------
import { message } from 'ant-design-vue'
import { TableState } from 'ant-design-vue/lib/table/interface'
import { IPage } from '/@/api/http/type'
import { deleteTask, updateTaskStatus, UpdateTaskStatus, getWaylineJobs, Task, uploadMediaFileNow, getTaskResult, poweroffCf } from '/@/api/wayline'
import { TaskStatus, TaskProgressInfo, TaskProgressStatus, TaskProgressWsStatusMap, MediaStatus, MediaStatusProgressInfo, TaskMediaHighestPriorityProgressInfo } from '/@/types/task'
import { useTaskWsEvent } from '/@/components/task/use-task-ws-event'
import { getAllSub } from '/@/api/points'
import { getDeviceBySub } from '/@/api/subAndvideo'

// ---------------------------------
const router = useRouter()
const store = useMyStore()
const username = ref(localStorage.getItem(ELocalStorageKey.Username))
const workspaceId = ref(localStorage.getItem(ELocalStorageKey.WorkspaceId)!)
const osdVisible = computed(() => store.state.osdVisible)
const hmsVisible = new Map<string, boolean>()
const scorllHeight = ref()

const onlineDevices = reactive({
  data: [] as OnlineDevice[]
})

const onlineDocks = reactive({
  data: [] as OnlineDevice[]
})

const deviceInfo = computed(() => store?.state.deviceState.deviceInfo)
const dockInfo = computed(() => store?.state.deviceState.dockInfo)
const hmsInfo = computed({
  get: () => store?.state.hmsInfo,
  set: (val) => {
    return val
  }
})

// 任务信息

// import CreatePlan from './CreatePlan.vue'
// import CreatePlan from '/@/pages/page-web/projects/task.vue'
const userId = ref(localStorage.getItem(ELocalStorageKey.UserId)!)
const body: IPage = {
  page: 1,
  total: 0,
  page_size: 5
}
const paginationProp = reactive({
  pageSizeOptions: ['5', '10', '15'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 5,
  current: 1,
  total: 0
})
type Pagination = TableState['pagination']
const customFooter = null
const plansData = reactive({
  data: [] as Task[]
})

const Mystatus = ref('')
const MytaskId = ref('')
const execute_time = ref('')
const current_wayline = ref('')
const task_zh = [
  {
    value: 'sent',
    label: '任务已下发'
  },
  {
    value: 'in_progress',
    label: '任务执行中'
  },
  {
    value: 'paused',
    label: '任务暂停'
  },
  {
    value: 'rejected',
    label: '任务被拒绝'
  },
  {
    value: 'canceled',
    label: '任务被终止'
  },
  {
    value: 'failed',
    label: '任务失败'
  },
  {
    value: 'ok',
    label: '上传成功'
  }
]
const current_task = ref('')
// 设备任务执行进度更新
function onTaskProgressWs (data: TaskProgressInfo) {
  const { bid, output } = data
  if (output) {
    const { status, progress } = output || {}
    const taskItem = plansData.data.find(task => task.job_id === bid)
    if (!taskItem) return
    if (status) {
      const selectedStatus = task_zh.find(item => item.value === status)
      Mystatus.value = selectedStatus.label
      MytaskId.value = taskItem.job_id
      execute_time.value = taskItem.execute_time
      current_wayline.value = taskItem.file_id
      current_task.value = taskItem
      taskItem.status = TaskProgressWsStatusMap[status]
      // 执行中，更新进度
      if (status === TaskProgressStatus.Sent || status === TaskProgressStatus.inProgress) {
        taskItem.progress = progress?.percent || 0
      } else if ([TaskProgressStatus.Rejected, TaskProgressStatus.Canceled, TaskProgressStatus.Timeout, TaskProgressStatus.Failed, TaskProgressStatus.OK].includes(status)) {
        getPlans()
      }
    }
  }
}
// 媒体上传进度更新
function onTaskMediaProgressWs (data: MediaStatusProgressInfo) {
  const { media_count: mediaCount, uploaded_count: uploadedCount, job_id: jobId } = data
  if (isNaN(mediaCount) || isNaN(uploadedCount) || !jobId) {
    return
  }
  const taskItem = plansData.data.find(task => task.job_id === jobId)
  if (!taskItem) return
  if (mediaCount === uploadedCount) {
    taskItem.uploading = false
  } else {
    taskItem.uploading = true
  }
  taskItem.media_count = mediaCount
  taskItem.uploaded_count = uploadedCount
}

function onoTaskMediaHighestPriorityWS (data: TaskMediaHighestPriorityProgressInfo) {
  const { pre_job_id: preJobId, job_id: jobId } = data
  const preTaskItem = plansData.data.find(task => task.job_id === preJobId)
  const taskItem = plansData.data.find(task => task.job_id === jobId)
  if (preTaskItem) {
    preTaskItem.uploading = false
  }
  if (taskItem) {
    taskItem.uploading = true
  }
}
useTaskWsEvent({
  onTaskProgressWs,
  onTaskMediaProgressWs,
  onoTaskMediaHighestPriorityWS,
})

function getPlans () {
  getWaylineJobs(workspaceId.value, body).then(res => {
    if (res.code !== 0) {
      return
    }
    plansData.data = res.data.list
    // console.log('sss', plansData.data)
    paginationProp.total = res.data.pagination.total
    paginationProp.current = res.data.pagination.page
  })
}

// ------------------------------------------------
onMounted(() => {
  getSubInfo()
  getOnlineTopo()
  setTimeout(() => {
    watch(() => store?.state.deviceStatusEvent,
      data => {
        getOnlineTopo()
        if (data.deviceOnline.sn) {
          getUnreadHms(data.deviceOnline.sn)
        }
      },
      {
        deep: true
      }
    )
    getOnlineDeviceHms()
  }, 3000)
  const element = document.getElementsByClassName('scrollbar').item(0) as HTMLDivElement
  const parent = element?.parentNode as HTMLDivElement
  scorllHeight.value = parent?.clientHeight - parent?.firstElementChild?.clientHeight
  getPlans()

  // showWayline()
})

function getOnlineTopo () {
  getDeviceTopo(workspaceId.value).then((res) => {
    if (res.code !== 0) {
      return
    }
    onlineDevices.data = []
    onlineDocks.data = []
    res.data.forEach((gateway: any) => {
      const child = gateway.children
      const device: OnlineDevice = {
        model: child?.device_name,
        callsign: child?.nickname,
        sn: child?.device_sn,
        mode: EModeCode.Disconnected,
        gateway: {
          model: gateway?.device_name,
          callsign: gateway?.nickname,
          sn: gateway?.device_sn,
          domain: gateway?.domain
        },
        payload: []
      }
      child?.payloads_list.forEach((payload: any) => {
        device.payload.push({
          index: payload.index,
          model: payload.model,
          payload_name: payload.payload_name,
          payload_sn: payload.payload_sn,
          control_source: payload.control_source,
          payload_index: payload.payload_index
        })
      })
      if (EDeviceTypeName.Dock === gateway.domain) {
        hmsVisible.set(device.sn, false)
        hmsVisible.set(device.gateway.sn, false)
        onlineDocks.data.push(device)
      }
      if (gateway.status && EDeviceTypeName.Gateway === gateway.domain) {
        onlineDevices.data.push(device)
      }
    })
    console.log('在线机场', onlineDocks)
  })
}

// 获取所有的场站信息
const current_sub = ref('')
const subOption = ref([])
function getSubInfo () {
  getAllSub().then(res => {
    if (res.code !== 0) {
      return
    }
    subOption.value = res.data.map(item => ({
      value: item.sub_code,
      label: item.sub_name
    }))
    current_sub.value = subOption.value[0].label
    // console.log('打印的数据',subOption.value[0].value )
    getCurrentDeviceBySub(subOption.value[0].value)
  })
}

// 切换场站
function changeSub (val:any) {
  const selectedOption = subOption.value.find(item => item.value === val)
  current_sub.value = selectedOption.label
  getCurrentDeviceBySub(val)
}

const currentDeviceForSub = ref([])
// 根据场站展示对应的设备
function getCurrentDeviceBySub (sub_code:string) {
  getDeviceBySub(sub_code).then(res => {
    if (res.code !== 0) {
      return
    }

    currentDeviceForSub.value = res.data.map(device => device.device_sn)
    // console.log('输出的数据', currentDeviceForSub.value)
  })
}

// 跳转到远程调试界面
function toRemoteDebug (e: any, device: OnlineDevice, isDock: boolean, isClick: boolean) {
  // if (!isClick) {
  //   e.target.style.cursor = 'not-allowed'
  //   return
  // }
  if (device.sn === osdVisible.value.sn) {
    osdVisible.value.visible = false
  } else {
    osdVisible.value.sn = device.sn
    osdVisible.value.callsign = device.callsign
    osdVisible.value.model = device.model
    osdVisible.value.visible = false
    osdVisible.value.gateway_sn = device.gateway.sn
    osdVisible.value.is_dock = isDock
    osdVisible.value.gateway_callsign = device.gateway.callsign
    osdVisible.value.payloads = device.payload
  }
  store.commit('SET_OSD_VISIBLE_INFO', osdVisible)
  localStorage.setItem('osdInfo', JSON.stringify(osdVisible.value))

  // 设置航线信息
  localStorage.setItem('currentTask', JSON.stringify(current_task.value))
  // const waylineId = store.state.waylineInfo
  // waylineId.id = current_wayline.value
  // store.commit('SET_SELECT_WAYLINE_INFO', waylineId)
  router.push({ path: '/remoteDebug', state: { name: current_sub.value } })
}

// 地图显示航线
function showWayline () {
  // current_wayline.value = '011cfcd3-07bb-4919-8dc4-15028cee4cff'
  const waylineId = store.state.waylineInfo
  if (waylineId.id === current_wayline.value) {
    waylineId.update_time = Math.floor(Date.now() / 1000)
  }
  waylineId.id = current_wayline.value
  store.commit('SET_SELECT_WAYLINE_INFO', waylineId)
}

function toTask () {
  router.push({ path: '/task' })
}

// 弹出设备弹窗
function switchVisible (e: any, device: OnlineDevice, isDock: boolean, isClick: boolean) {
  if (!isClick) {
    e.target.style.cursor = 'not-allowed'
    return
  }
  if (device.sn === osdVisible.value.sn) {
    osdVisible.value.visible = !osdVisible.value.visible
  } else {
    osdVisible.value.sn = device.sn
    osdVisible.value.callsign = device.callsign
    osdVisible.value.model = device.model
    osdVisible.value.visible = true
    osdVisible.value.gateway_sn = device.gateway.sn
    osdVisible.value.is_dock = isDock
    osdVisible.value.gateway_callsign = device.gateway.callsign
    osdVisible.value.payloads = device.payload
  }
  store.commit('SET_OSD_VISIBLE_INFO', osdVisible)
}

// 弹出视频弹窗
const liveStream = computed(() => store.state.liveStream)
function openLiveStream (device: OnlineDevice) {
  // if (device.sn === osdVisible.value.sn) {
  //   osdVisible.value.visible = !osdVisible.value.visible
  // }
  console.log('在线设备', device)
  liveStream.value.visible = !liveStream.value.visible
  liveStream.value.dock_sn = device.gateway.sn
  liveStream.value.dorne_sn = device.sn
  store.commit('SET_LIVESTREAM_INFO', liveStream)
}

function getUnreadHms (sn: string) {
  getUnreadDeviceHms(workspaceId.value, sn).then(res => {
    if (res.data.length !== 0) {
      hmsInfo.value[sn] = res.data
    }
  })
  console.info(hmsInfo.value)
}

function getOnlineDeviceHms () {
  const snList = Object.keys(dockInfo.value)
  if (snList.length === 0) {
    return
  }
  snList.forEach(sn => {
    getUnreadHms(sn)
  })
  const deviceSnList = Object.keys(deviceInfo.value)
  if (deviceSnList.length === 0) {
    return
  }
  deviceSnList.forEach(sn => {
    getUnreadHms(sn)
  })
}

function readHms (visiable: boolean, sn: string) {
  if (!visiable) {
    updateDeviceHms(workspaceId.value, sn).then(res => {
      if (res.code === 0) {
        delete hmsInfo.value[sn]
      }
    })
  }
}

function openLivestreamOthers () {
  store.commit('SET_LIVESTREAM_OTHERS_VISIBLE', true)
}

function openLivestreamAgora () {
  store.commit('SET_LIVESTREAM_AGORA_VISIBLE', true)
}

</script>

<style lang="scss" scoped>
.nav{
  height: 27px;
  background: url('/@/assets/v4/livestream-nav.png') 100% no-repeat;
  background-size: 100% 100%;
  width: 400px;
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
      width: 40px;
      height: 15px;
      overflow-wrap: break-word;
      color: rgba(255, 255, 255, 1);
      font-size: 24px;
      letter-spacing: 1.047272801399231px;
      font-family: Google Sans-Medium;
      font-weight: 500;
      text-align: left;
      white-space: nowrap;
      line-height: 12px;
      margin: 7px 0 5px 13px;
    }
}
.nav-select {
    margin-left: 270px;
    margin-top: -10px;
}

// 下拉框
.select-operation {
  :deep(.el-select__placeholder) {
    font-size: 14px;
    font-weight: 500;
    color: #A8ABB2;
  }

  :deep(.el-select__wrapper) {

    // background: rgba(59, 116, 255, 0.15);
    background-color: #0B2756;
    // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    // border: 1px solid #719fff;
    // border-radius: 4px;
    width: 100px;
    height: 25px;
  }

  /**修改下拉图标颜色 */
  :deep(.el-select__caret) {
    color: #ffffff;
  }

  :deep(.el-select-dropdown) {
    background: #012b78;
    box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    border: 1px solid #719fff;
  }

  :deep(.el-select-dropdown__item) {
    font-size: 14px;
    font-weight: 500;
    color: #ffffff;
  }

  :deep(.el-select-dropdown__item.is-hovering) {

    background-color: skyblue;
  }
}
.title{
  width: 400px;
  height: 29px;
  overflow-wrap: break-word;
  color: #ffffff;
  font-size: 20px;
  // font-family: YouSheBiaoTiHei-Regular;
  font-weight: normal;
  text-align: left;
  white-space: nowrap;
  line-height: 20px;
  margin: 10px 0 15px 0;
  padding-left: 30px;
  background: url('/@/assets/v4/live-title.png') 100% no-repeat;
  border-bottom: 1px solid #455777;
}
.dock{
  background-color: rgba(0, 20, 59, 1);
  border-radius: 8px;
  position: relative;
  width: 394px;
  height: fit-content;
  border: 1px solid rgba(2, 29, 81, 1);
  margin: 12px 0 0 11px;
  .group_4 {
    .group_30 {
      width: 377px;
      height: 77px;
      margin: 5px 0 0 6px;
      .image_6 {
        width: 121px;
        height: 77px;
      }
      .group_31 {
        width: 140px;
        height: 50px;
        margin: 13px 0 0 23px;
        .box_35 {
          width: 40px;
          height: 18px;
          .group_5 {
            background-color: rgba(90, 186, 255, 1);
            width: 4px;
            height: 100%;
            // margin-top: 4px;
          }
          .text_11 {
            width: 28px;
            height: 18px;
            overflow-wrap: break-word;
            color: rgba(255, 255, 255, 1);
            font-size: 14px;
            font-family: Google Sans-Medium;
            font-weight: 500;
            text-align: center;
            white-space: nowrap;
            line-height: 14px;
          }
        }
        .text_13 {
          width: 140px;
          height: 18px;
          overflow-wrap: break-word;
          color: rgba(175, 232, 255, 1);
          font-size: 14px;
          font-family: Google Sans-Medium;
          font-weight: 500;
          text-align: center;
          white-space: nowrap;
          line-height: 14px;
          margin-top: 20px;
        }
      }
      .group_32 {
        width: 70px;
        height: 53px;
        margin: 10px 0 0 23px;
        .group_6 {
          background-color: rgba(0, 218, 71, 0.11);
          border-radius: 2px;
          width: 70px;
          height: 18px;
          border: 0.800000011920929px solid rgba(0, 218, 71, 0.78);
          cursor: pointer;
          .section_2 {
            background: url('/@/assets/v4/icon1.png') 100% no-repeat;
            // box-shadow: 0px 0px 4px 0px rgba(0, 255, 68, 0.5);
            // background-color: rgba(175, 255, 199, 1);
            width: 25px;
            height: 9px;
            margin-top: 4px;
            // margin: 4px 0 0 8px;
          }
          .text_12 {
            width: 30px;
            height: 13px;
            overflow-wrap: break-word;
            color: rgba(15, 216, 80, 1);
            font-size: 10px;
            font-family: Google Sans-Medium;
            font-weight: 500;
            text-align: left;
            white-space: nowrap;
            line-height: 10px;
            margin: 2px 15px 0 8px;
          }
        }
        .block_2 {
          box-shadow: 0px 0px 4px 0px rgba(6, 151, 255, 0.5);
          background-color: rgba(0, 87, 218, 0.11);
          border-radius: 2px;
          width: 70px;
          height: 18px;
          border: 0.800000011920929px solid rgba(0, 120, 218, 0.78);
          margin-top: 17px;
          .box_4 {
            background-color: rgba(175, 232, 255, 1);
            width: 9px;
            height: 10px;
            margin: 4px 0 0 9px;
          }
          .text_14 {
            width: 40px;
            height: 13px;
            overflow-wrap: break-word;
            color: rgba(15, 162, 216, 1);
            font-size: 10px;
            font-family: Google Sans-Medium;
            font-weight: 500;
            text-align: left;
            white-space: nowrap;
            line-height: 10px;
            margin: 2px 5px 0 7px;
          }
        }
      }
    }
    .group_33 {
      width: 372px;
      height: 52px;
      margin: 29px 0 0 11px;
      .group_7 {
        height: 49px;
        background: url(https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG528b587e81cbbe89e17e49f7b957e924.png)
          100% no-repeat;
        background-size: 100% 100%;
        margin-top: 1px;
        width: 112px;
        .group_8 {
          width: 113px;
          height: 38px;
          background: url('/@/assets/v4/btn_bg.png') 100% no-repeat;
          background-size: 100% 100%;
          margin-top: 11px;
          display: flex;
          align-content: center;
          .group_9 {
            height: 30px;
            background: url('/@/assets/v4/duihao.png') 100% no-repeat;
            background-size: 70% 70%;
            width: 30px;
            margin: 3px 0 0 10px;
            .block_3 {
              width: 18px;
              height: 18px;
              background: url(https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNGcf59ce6a251b66ebce0de586896e880a.png)
                100% no-repeat;
              background-size: 100% 100%;
              margin: 6px 0 0 6px;
            }
          }
          .text_15 {
            width: 42px;
            height: 18px;
            overflow-wrap: break-word;
            color: rgba(122, 250, 251, 1);
            font-size: 14px;
            font-family: Google Sans-Medium;
            font-weight: 500;
            text-align: center;
            white-space: nowrap;
            line-height: 14px;
            margin: 11px 18px 0 13px;
          }
        }
      }
      .box_36 {
        width: 14px;
        height: 41px;
        margin: 5px 0 0 71px;
        .box_17 {
          background-color: rgba(177, 199, 223, 1);
          width: 13px;
          height: 13px;
        }
        .text-wrapper_10 {
          box-shadow: 0px 0px 4px 0px rgba(255, 183, 121, 1);
          background-color: rgba(255, 146, 51, 0.11);
          border-radius: 2px;
          height: 20px;
          border: 0.800000011920929px solid rgba(255, 146, 51, 0.78);
          margin-top: 14px;
          width: 20px;
          .text_29 {
            width: 5px;
            height: 15px;
            overflow-wrap: break-word;
            color: rgba(255, 146, 51, 1);
            font-size: 12px;
            font-family: Google Sans-Medium;
            font-weight: 500;
            text-align: left;
            white-space: nowrap;
            line-height: 12px;
            margin-left: 4px;
          }
        }
      }
      .box_37 {
        width: 167px;
        height: 52px;
        margin-left: 8px;
        .text-wrapper_17 {
          width: 110px;
          height: 24px;
          .text_30 {
            width: 43px;
            height: 24px;
            overflow-wrap: break-word;
            color: rgba(177, 199, 223, 1);
            font-size: 14px;
            letter-spacing: 1px;
            font-family: Inter-Regular;
            font-weight: normal;
            text-align: right;
            white-space: nowrap;
            line-height: 24px;
          }
          .text_31 {
            width: 57px;
            height: 24px;
            overflow-wrap: break-word;
            color: rgba(177, 199, 223, 1);
            font-size: 14px;
            letter-spacing: 1px;
            font-family: Inter-Regular;
            font-weight: normal;
            text-align: right;
            white-space: nowrap;
            line-height: 24px;
          }
        }
        .text_23 {
          width: 120px;
          height: 24px;
          color: rgba(255, 146, 51, 1);
          font-size: 12px;
          letter-spacing: 1px;
          font-family: Inter-Regular;
          font-weight: normal;
          text-align: right;
          line-height: 24px;
          margin-top: 4px;
          white-space: nowrap;    /* 禁止换行 */
          overflow: hidden;      /* 超出部分隐藏 */
          text-overflow: ellipsis; /* 用省略号表示超出部分 */
        }
      }
    }
    .group_34 {
      width: 394px;
      height: 40px;
      margin-top: 14px;
      .box_7 {
        background-color: rgba(0, 52, 152, 0.16);
        width: 197px;
        height: 40px;
        border: 1px solid rgba(0, 64, 147, 1);
        .group_10 {
          box-shadow: 0px 0px 15px 0px rgba(107, 226, 190, 0.5);
          background-color: rgba(107, 226, 190, 1);
          width: 17px;
          height: 17px;
          margin: 10px 0 0 57px;
        }
        .text-wrapper_6 {
          box-shadow: 0px 0px 15px 0px rgba(107, 226, 190, 0.8);
          height: 17px;
          width: 59px;
          margin: 10px 56px 0 0;
          .text_20 {
            width: 59px;
            height: 17px;
            overflow-wrap: break-word;
            color: rgba(107, 226, 190, 1);
            font-size: 14px;
            letter-spacing: 0.75px;
            font-family: Inter-Bold;
            font-weight: 700;
            text-align: left;
            white-space: nowrap;
            line-height: 14px;
          }
        }
      }
      .box_8 {
        width: 198px;
        height: 40px;
        background: url(https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG4430fdf70fff8ba299f3576abd94b58a.png)
          100% no-repeat;
        background-size: 100% 100%;
        margin-left: -1px;
        .group_11 {
          box-shadow: 0px 0px 15px 0px rgba(107, 194, 226, 0.8);
          background-color: rgba(90, 186, 255, 1);
          width: 22px;
          height: 19px;
          margin: 9px 0 0 51px;
        }
        .text-wrapper_7 {
          box-shadow: 0px 0px 15px 0px rgba(107, 194, 226, 0.8);
          height: 17px;
          width: 59px;
          margin: 10px 59px 0 7px;
          .text_21 {
            width: 59px;
            height: 17px;
            overflow-wrap: break-word;
            color: rgba(104, 192, 255, 1);
            font-size: 14px;
            letter-spacing: 0.75px;
            font-family: Inter-Bold;
            font-weight: 700;
            text-align: left;
            white-space: nowrap;
            line-height: 14px;
          }
        }
      }
    }
  }
}
.drone{
  background-color: rgba(0, 20, 59, 1);
  border-radius: 8px;
  position: relative;
  width: 394px;
  height: 175px;
  border: 1px solid rgba(2, 29, 81, 1);
  margin: 24px 0 0 11px;
  .group_12 {
      .box_38 {
        width: 368px;
        height: 108px;
        margin: 15px 0 0 12px;
        .image_3 {
          width: 166px;
          height: 98px;
          margin-top: 10px;
        }
        .group_35 {
          width: 156px;
          height: 93px;
          margin-left: 16px;
          .box_39 {
            width: 111px;
            height: 24px;
            margin-left: 22px;
            .label_2 {
              width: 24px;
              height: 24px;
            }
            .text_22 {
              width: 83px;
              height: 24px;
              overflow-wrap: break-word;
              color: rgba(255, 255, 255, 1);
              font-size: 14px;
              letter-spacing: 0.10000000149011612px;
              font-family: Inter-Medium;
              font-weight: 500;
              text-align: left;
              white-space: nowrap;
              line-height: 24px;
            }
          }
          .box_40 {
            width: 100px;
            height: 24px;
            margin-top: 17px;
            .image-text_2 {
              width: 65px;
              height: 24px;
              .group_18 {
                background-color: rgba(177, 199, 223, 1);
                width: 13px;
                height: 13px;
                margin-top: 5px;
              }
              .text-group_3 {
                width: 43px;
                height: 24px;
                overflow-wrap: break-word;
                color: rgba(177, 199, 223, 1);
                font-size: 14px;
                letter-spacing: 1px;
                font-family: Inter-Regular;
                font-weight: normal;
                text-align: right;
                white-space: nowrap;
                line-height: 24px;
              }
            }
            .text_32 {
              width: 29px;
              height: 24px;
              overflow-wrap: break-word;
              color: rgba(15, 216, 80, 1);
              font-size: 14px;
              letter-spacing: 1px;
              font-family: Inter-Regular;
              font-weight: normal;
              text-align: right;
              white-space: nowrap;
              line-height: 24px;
            }
          }
          .box_41 {
            width: 156px;
            height: 24px;
            margin-top: 4px;
            .text-wrapper_12 {
              box-shadow: 0px 0px 4px 0px rgba(255, 183, 121, 1);
              background-color: rgba(255, 146, 51, 0.11);
              border-radius: 2px;
              height: 14px;
              border: 0.800000011920929px solid rgba(255, 146, 51, 0.78);
              margin-top: 4px;
              width: 14px;
              .text_33 {
                width: 5px;
                height: 15px;
                overflow-wrap: break-word;
                color: rgba(255, 146, 51, 1);
                font-size: 12px;
                font-family: Google Sans-Medium;
                font-weight: 500;
                text-align: left;
                white-space: nowrap;
                line-height: 12px;
                margin-left: 4px;
              }
            }
            .text_34 {
              width: 131px;
              height: 24px;
              overflow-wrap: break-word;
              color: rgba(255, 146, 51, 1);
              font-size: 12px;
              letter-spacing: 1px;
              font-family: Inter-Regular;
              font-weight: normal;
              text-align: right;
              line-height: 24px;
            }
          }
        }
        .label_3 {
          width: 29px;
          height: 24px;
          margin-left: 1px;
        }
      }
      .box_42 {
        width: 394px;
        height: 40px;
        margin-top: 12px;
        .box_11 {
          background-color: rgba(0, 52, 152, 0.16);
          height: 40px;
          border: 1px solid rgba(0, 64, 147, 1);
          width: 197px;
          .block_6 {
            box-shadow: 0px 0px 15px 0px rgba(107, 226, 190, 0.8);
            height: 17px;
            width: 59px;
            position: relative;
            margin: 12px 0 0 83px;
            cursor: pointer;
            .text_27 {
              width: 59px;
              height: 17px;
              overflow-wrap: break-word;
              color: rgba(107, 226, 190, 1);
              font-size: 14px;
              letter-spacing: 0.75px;
              font-family: Inter-Bold;
              font-weight: 700;
              text-align: left;
              white-space: nowrap;
              line-height: 14px;
            }
            .image_4 {
              position: absolute;
              left: -26px;
              top: -1px;
              width: 33px;
              height: 18px;
            }
          }
        }
        .box_12 {
          width: 198px;
          height: 40px;
          background: url(https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG4430fdf70fff8ba299f3576abd94b58a.png)
            100% no-repeat;
          background-size: 100% 100%;
          margin-left: -1px;
          .box_13 {
            box-shadow: 0px 0px 15px 0px rgba(107, 194, 226, 0.8);
            background-color: rgba(90, 186, 255, 1);
            width: 15px;
            height: 18px;
            margin: 11px 0 0 55px;
          }
          .text-wrapper_8 {
            box-shadow: 0px 0px 15px 0px rgba(107, 194, 226, 0.8);
            height: 17px;
            width: 59px;
            margin: 12px 59px 0 10px;
            cursor: pointer;
            .text_28 {
              width: 59px;
              height: 17px;
              overflow-wrap: break-word;
              color: rgba(104, 192, 255, 1);
              font-size: 14px;
              letter-spacing: 0.75px;
              font-family: Inter-Bold;
              font-weight: 700;
              text-align: left;
              white-space: nowrap;
              line-height: 14px;
            }
          }
        }
      }
    }

}
.btn-box{
    .group_34 {
        width: 394px;
        height: 40px;
        // margin-top: 14px;
        .box_7 {
          background-color: rgba(0, 52, 152, 0.16);
          width: 197px;
          height: 40px;
          border: 1px solid rgba(0, 64, 147, 1);
          .group_10 {
            box-shadow: 0px 0px 15px 0px rgba(107, 226, 190, 0.5);
            background-color: rgba(107, 226, 190, 1);
            width: 17px;
            height: 17px;
            margin: 10px 0 0 57px;
          }
          .text-wrapper_6 {
            box-shadow: 0px 0px 15px 0px rgba(107, 226, 190, 0.8);
            height: 17px;
            width: 59px;
            margin: 10px 56px 0 0;
            .text_20 {
              width: 59px;
              height: 17px;
              overflow-wrap: break-word;
              color: rgba(107, 226, 190, 1);
              font-size: 14px;
              letter-spacing: 0.75px;
              font-family: Inter-Bold;
              font-weight: 700;
              text-align: left;
              white-space: nowrap;
              line-height: 14px;
            }
          }
        }
        .box_8 {
          width: 198px;
          height: 40px;
          background: url(https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG4430fdf70fff8ba299f3576abd94b58a.png)
            100% no-repeat;
          background-size: 100% 100%;
          border: 1px solid rgba(0, 64, 147, 1);
          margin-left: -1px;
          .group_11 {
            box-shadow: 0px 0px 15px 0px rgba(107, 194, 226, 0.8);
            background-color: rgba(90, 186, 255, 1);
            width: 22px;
            height: 19px;
            margin: 9px 0 0 51px;
          }
          .text-wrapper_7 {
            box-shadow: 0px 0px 15px 0px rgba(107, 194, 226, 0.8);
            height: 17px;
            width: 59px;
            margin: 10px 59px 0 7px;
            .text_21 {
              width: 59px;
              height: 17px;
              overflow-wrap: break-word;
              color: rgba(104, 192, 255, 1);
              font-size: 14px;
              letter-spacing: 0.75px;
              font-family: Inter-Bold;
              font-weight: 700;
              text-align: left;
              white-space: nowrap;
              line-height: 14px;
            }
          }
        }
      }
  }

.project-tsa-wrapper {
  height: 100%;
  .scrollbar {
    overflow: auto;
  }
  ::-webkit-scrollbar {
    display: none;
  }
}
.ant-collapse > .ant-collapse-item > .ant-collapse-header {
  color: white;
  border: 0;
  padding-left: 14px;
}

.text-hidden {
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap;
  -o-text-overflow: ellipsis;
}
.font-bold {
  font-weight: 700;
}

.battery-slide {
  width: 100%;
  .capacity-percent {
    background: #00ee8b;
  }
  .return-home {
    background: #ff9f0a;
  }
  .landing {
    background: #f5222d;
  }
  .battery {
    background: white;
    border-radius: 1px;
    width: 8px;
    height: 4px;
    margin-top: -3px;
  }
}
.battery-slide > div {
  position: relative;
  margin-top: -2px;
  min-height: 2px;
  border-radius: 2px;
  white-space: nowrap;
}
.disable {
  cursor: not-allowed;
}

.notice-blink {
  color: $success;
  animation: blink 500ms infinite;
}
.caution-blink {
  color: orange;
  animation: blink 500ms infinite;
}
.warn-blink {
  color: red;
  animation: blink 500ms infinite;
}
.notice {
  // background: $success;
  background: transparent !important;;
  color: $success;
  overflow: hidden;
  cursor: pointer;
}
.caution {
  // background: orange;
  background: transparent !important;;
  color: orange;
  cursor: pointer;
  overflow: hidden;
}
.warn {
  // background: red;
  background: transparent !important;;
  color: red;
  cursor: pointer;
  overflow: hidden;
}
.word-loop {
  white-space: nowrap;
  display: inline-block;
  animation: 10s loop linear infinite normal;
}
@keyframes blink {
  from {
    opacity: 1;
  }
  50% {
    opacity: 0.35;
  }
  to {
    opacity: 1;
  }
}
@keyframes loop {
  0% {
    transform: translateX(20px);
    -webkit-transform: translateX(20px);
  }
  100% {
    transform: translateX(-100%);
    -webkit-transform: translateX(-100%);
  }
}

</style>
