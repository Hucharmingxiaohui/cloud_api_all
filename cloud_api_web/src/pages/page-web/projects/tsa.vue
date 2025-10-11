<template>
  <div class="container">
    <!-- <div class="header">设备管理</div> -->
    <div class="header" style="display:flex; justify-content: space-between;">
      <span>设备管理</span>
      <div>
        <!-- <span style="margin:15px" >{{ workspaceName }}</span> -->
        <el-button class="btn" @click="devicebind">设备绑定</el-button>
      </div>
    </div>
    <!-- // 展示状态码  -->
   <div>
        <el-dialog v-model="showSelectDialog" title="设备绑定"  center width="40%" top="10%"  class="selectDialog" :close-on-click-modal="false">
            <!-- 在标题下方添加一条横线 -->
          <div style=" margin: 10px; display: grid; grid-template-columns: repeat(2, 1fr); gap: 4px;">
            <div style="margin: 10px; font-size: 14px; width: auto;display: flex;flex-direction: column;">
              <span style="color:rgb(148, 154, 159)">MQTT网关地址:</span>
              <span style="color:aliceblue">http://12345664444444444444</span>
            </div>
            <div style="margin: 10px; font-size: 14px; width: auto;display: flex;flex-direction: column;">
              <span style="color:rgb(148, 154, 159)">MQTT账号:</span>
              <span style="color:aliceblue">root</span>
            </div>
            <div style="margin: 10px; font-size: 14px; width: auto;display: flex;flex-direction: column;">
              <span style="color:rgb(148, 154, 159)">MQTT密码:</span>
              <span style="color:aliceblue">1234566</span>
            </div>
            <div style="margin: 10px; font-size: 14px; width: auto;display: flex;flex-direction: column;">
              <span style="color:rgb(148, 154, 159)">组织id:</span>
              <span style="color:aliceblue">2548</span>
            </div>
            <div style="margin: 10px; font-size: 14px; width: auto;display: flex;flex-direction: column;">
              <span style="color:rgb(148, 154, 159)">设备绑定码:</span>
              <span style="color:aliceblue">qtt</span>
            </div>
          </div>
          <div style="margin: 10px 20px; font-size: 14px; width: auto;color:aliceblue">
              <span>说明:用户需要将上述信息通过遥控器输入至机场内完成注册，若直接注册，则遥控器完成操作后需要刷新列表才可显示。</span>
            </div>
        </el-dialog>
    </div>

   <div class="main-box">
    <div class="box-left">
      <custom-tree :treeData="treeData" @change="handleNodeChange" />
    </div>
    <div class="box-right">
      <div class="operation">
        <a-tabs v-model:activeKey="activeKey" class="custom-tabs" tabBarStyle="background: none; border-bottom: none;">
          <a-tab-pane key="EDeviceTypeName.Aircraft" tab="无人机">
              <!-- 表格 -->
            <div class="tablelw" >
              <el-table :data="onlineDevices.data" :row-key="row => row.id" class="tabelW"
                :header-cell-style="{color: '#fff', fontSize: '16px', backgroundColor: '#003896', borderLeft: '0.5px #154480 solid', borderBottom: '1px #154480 solid' }"
                :cell-style="{borderBottom: '0.5px #143275 solid',background: '#002D78', borderLeft: '0.5px #143275 solid' ,color:'#DCDFE5'}">
                <el-table-column prop="sn" label="无人机编号" />
                <el-table-column label="无人机型号">
                  <template #default="scope" class="font-bold" :style="deviceInfo[scope.row.sn] && deviceInfo[scope.row.sn].mode_code !== EModeCode.Disconnected ? 'color: #00ee8b' :  'color: red;'">
                    {{ scope.row.model ? `${scope.row.model} - ${scope.row.callsign}` : 'No Drone'}}
                  </template>
                </el-table-column>
                <el-table-column label="状态">
                  <template #default="scope">
                  {{ deviceInfo[scope.row.sn] ? EModeCode[deviceInfo[scope.row.sn].mode_code] : EModeCode[EModeCode.Disconnected] }}
                  </template>
                </el-table-column>
                <el-table-column label="设备">
                  <template #default="scope">
                    {{ scope.row.gateway.model }} - {{ scope.row.gateway.callsign }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="150" >
                  <template #default="scope">
                    <el-button size="mini" type="text" @click="switchVisible($event, scope.row, false, deviceInfo[scope.row.sn] && deviceInfo[scope.row.sn].mode_code !== EModeCode.Disconnected)">详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
              <!-- <div v-if="onlineDevices.data.length === 0" style="height: 150px; color: white;">
                <a-empty :image="noData" :image-style="{ height: '60px' }" />
              </div>
              <div v-else class="fz12" style="color: white;">
                <div v-for="device in onlineDevices.data" :key="device.sn" style="background: #3c3c3c; height: 90px; width: 250px; margin-bottom: 10px;">
                  <div class="battery-slide" v-if="deviceInfo[device.sn]">
                    <div style="background: #535759; width: 100%;"></div>
                    <div class="capacity-percent" :style="{ width: deviceInfo[device.sn].battery.capacity_percent + '%'}"></div>
                    <div class="return-home" :style="{ width: deviceInfo[device.sn].battery.return_home_power + '%'}"></div>
                    <div class="landing" :style="{ width: deviceInfo[device.sn].battery.landing_power + '%'}"></div>
                    <div class="battery" :style="{ left: deviceInfo[device.sn].battery.capacity_percent + '%' }"></div>
                  </div>
                  <div style="border-bottom: 1px solid #515151; border-radius: 2px; height: 50px; width: 100%;" class="flex-row flex-justify-between flex-align-center">
                    <div style="float: left; padding: 5px 5px 8px 8px; width: 88%">
                      <div style="width: 100%; height: 100%;">
                        <a-tooltip>
                          <template #title>{{ device.model ? `${device.model} - ${device.callsign}` : 'No Drone'}}</template>
                          <span class="text-hidden" style="max-width: 200px; display: block; height: 20px;">{{ device.model ? `${device.model} - ${device.callsign}` : 'No Drone'}}</span>
                        </a-tooltip>
                      </div>
                      <div class="mt5" style="background: #595959;">
                        <span class="ml5 mr5"><RocketOutlined /></span>
                        <span class="font-bold" :style="deviceInfo[device.sn] && deviceInfo[device.sn].mode_code !== EModeCode.Disconnected ? 'color: #00ee8b' :  'color: red;'">
                          {{ deviceInfo[device.sn] ? EModeCode[deviceInfo[device.sn].mode_code] : EModeCode[EModeCode.Disconnected] }}
                        </span>
                      </div>
                    </div>
                    <div style="float: right; background: #595959; height: 50px; width: 40px;" class="flex-row flex-justify-center flex-align-center">
                      <div class="fz16" @click="switchVisible($event, device, false, deviceInfo[device.sn] && deviceInfo[device.sn].mode_code !== EModeCode.Disconnected)">
                        <a v-if="osdVisible.sn === device.sn && osdVisible.visible"><EyeOutlined /></a>
                        <a v-else><EyeInvisibleOutlined /></a>
                      </div>
                    </div>
                  </div>
                  <div class="flex-row flex-justify-center flex-align-center" style="height: 40px;">
                    <div class="flex-row" style="height: 20px; background: #595959; width: 94%;" >
                      <span class="mr5"><a-image style="margin-left: 2px; margin-top: -2px; height: 20px; width: 20px;" :src="rc" /> </span>
                      <a-tooltip>
                        <template #title>{{ device.gateway.model }} - {{ device.gateway.callsign }}</template>
                        <div class="text-hidden" style="max-width: 200px;">{{ device.gateway.model }} - {{ device.gateway.callsign }}</div>
                      </a-tooltip>
                    </div>
                  </div>
                </div>
              </div> -->
          </a-tab-pane>
          <a-tab-pane key="EDeviceTypeName.Dock" tab="机库" force-render>
            <div v-if="onlineDocks.data.length === 0" style="height: 150px; color: white;">
              <a-empty :image="noData" :image-style="{ height: '60px' }" />
          </div>
          <div v-else class="fz12" style="color: white;">
            <div v-for="dock in onlineDocks.data" :key="dock.sn" style="background: #3c3c3c; height: 90px; width: 250px; margin-bottom: 10px;">
              <div style="border-radius: 2px; height: 100%; width: 100%;" class="flex-row flex-justify-between flex-align-center">
                <div style="float: left; padding: 0px 5px 8px 8px; width: 88%">
                  <div style="width: 80%; height: 30px; line-height: 30px; font-size: 16px;">
                    <a-tooltip :title="`${dock.gateway.callsign} - ${dock.callsign ?? 'No Drone'}`">
                      <div class="text-hidden" style="max-width: 200px;">{{ dock.gateway.callsign }} - {{ dock.callsign ?? 'No Drone' }}</div>
                    </a-tooltip>
                  </div>
                  <div class="mt5 flex-align-center flex-row flex-justify-between" style="background: #595959;">
                    <div class="flex-align-center flex-row">
                      <span class="ml5 mr5"><RobotOutlined /></span>
                      <div class="font-bold text-hidden" style="max-width: 80px;" :style="dockInfo[dock.gateway.sn] && dockInfo[dock.gateway.sn].basic_osd?.mode_code !== EDockModeCode.Disconnected ? 'color: #00ee8b' :  'color: red;'">
                        {{ dockInfo[dock.gateway.sn] ? EDockModeCode[dockInfo[dock.gateway.sn].basic_osd?.mode_code] : EDockModeCode[EDockModeCode.Disconnected] }}
                      </div>
                    </div>
                    <div class="mr5 flex-align-center flex-row" style="width: 85px; margin-right: 0; height: 18px;">
                      <div v-if="hmsInfo[dock.gateway.sn]" class="flex-align-center flex-row">
                          <div :class="hmsInfo[dock.gateway.sn][0].level === EHmsLevel.CAUTION ? 'caution-blink' :
                            hmsInfo[dock.gateway.sn][0].level === EHmsLevel.WARN ? 'warn-blink' : 'notice-blink'" style="width: 18px; height: 16px; text-align: center;">
                            <span :style="hmsInfo[dock.gateway.sn].length > 99 ? 'font-size: 11px' : 'font-size: 12px'">{{ hmsInfo[dock.gateway.sn].length }}</span>
                            <span class="fz10">{{ hmsInfo[dock.gateway.sn].length > 99 ? '+' : ''}}</span>
                          </div>
                        <a-popover trigger="click" placement="bottom" color="black" v-model:visible="hmsVisible[dock.gateway.sn]"
                          @visibleChange="readHms(hmsVisible[dock.gateway.sn], dock.gateway.sn)"
                          :overlayStyle="{width: '200px', height: '300px'}">
                          <div :class="hmsInfo[dock.gateway.sn][0].level === EHmsLevel.CAUTION ? 'caution' :
                            hmsInfo[dock.gateway.sn][0].level === EHmsLevel.WARN ? 'warn' : 'notice'" style="margin-left: 3px; width: 62px; height: 16px;">
                            <span class="word-loop">{{ hmsInfo[dock.gateway.sn][0].message_en }}</span>
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
                                      <span class="word-loop">{{ hms.message_en }}</span>
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
                      <div v-else class="width-100" style="height: 90%; background: rgba(0, 0, 0, 0.35)"></div>
                    </div>
                  </div>
                  <div class="mt5 flex-align-center flex-row flex-justify-between" style="background: #595959;">
                    <div class="flex-row">
                      <span class="ml5 mr5"><RocketOutlined /></span>
                      <div class="font-bold text-hidden" style="max-width: 80px" :style="deviceInfo[dock.sn] && deviceInfo[dock.sn].mode_code !== EModeCode.Disconnected ? 'color: #00ee8b' :  'color: red;'">
                        {{ deviceInfo[dock.sn] ? EModeCode[deviceInfo[dock.sn].mode_code] : EModeCode[EModeCode.Disconnected] }}
                      </div>
                    </div>
                    <div class="mr5 flex-align-center flex-row" style="width: 85px; margin-right: 0; height: 18px;">
                      <div v-if="hmsInfo[dock.sn]" class="flex-align-center flex-row">
                        <div :class="hmsInfo[dock.sn][0].level === EHmsLevel.CAUTION ? 'caution-blink' :
                          hmsInfo[dock.sn][0].level === EHmsLevel.WARN ? 'warn-blink' : 'notice-blink'" style="width: 18px; height: 16px; text-align: center;">
                          <span :style="hmsInfo[dock.sn].length > 99 ? 'font-size: 11px' : 'font-size: 12px'">{{ hmsInfo[dock.sn].length }}</span>
                          <span class="fz10">{{ hmsInfo[dock.sn].length > 99 ? '+' : ''}}</span>
                        </div>
                        <a-popover trigger="click" placement="bottom" color="black" v-model:visible="hmsVisible[dock.sn]" @visibleChange="readHms(hmsVisible[dock.sn], dock.sn)"
                          :overlayStyle="{width: '200px', height: '300px'}">
                          <div :class="hmsInfo[dock.sn][0].level === EHmsLevel.CAUTION ? 'caution' :
                            hmsInfo[dock.sn][0].level === EHmsLevel.WARN ? 'warn' : 'notice'" style="margin-left: 3px; width: 62px; height: 16px;">
                            <span class="word-loop">{{ hmsInfo[dock.sn][0].message_en }}</span>
                          </div>
                          <template #content>
                            <a-collapse style="background: black; height: 300px; overflow-y: auto;" :bordered="false" expand-icon-position="right" :accordion="true">
                              <a-collapse-panel v-for="hms in hmsInfo[dock.sn]" :key="hms.hms_id" :showArrow="false"
                                style=" margin: 0 auto 3px auto; border: 0; width: 140px; border-radius: 3px"
                                :class="hms.level === EHmsLevel.CAUTION ? 'caution' : hms.level === EHmsLevel.WARN ? 'warn' : 'notice'"
                                >
                                <template #header="{ isActive }">
                                  <div class="flex-row flex-align-center" style="width: 130px;">
                                    <div style="width: 110px;">
                                      <span class="word-loop">{{ hms.message_en }}</span>
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
                      <div v-else class="width-100" style="height: 90%; background: rgba(0, 0, 0, 0.35)"></div>
                    </div>
                  </div>
                </div>
                <div style="float: right; background: #595959; height: 100%; width: 40px;" class="flex-row flex-justify-center flex-align-center">
                  <div class="fz16" @click="switchVisible($event, dock, true, dockInfo[dock.gateway.sn] && dockInfo[dock.gateway.sn].basic_osd?.mode_code !== EDockModeCode.Disconnected)">
                    <a v-if="osdVisible.gateway_sn === dock.gateway.sn && osdVisible.visible"><EyeOutlined /></a>
                    <a v-else><EyeInvisibleOutlined /></a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </a-tab-pane>

      </a-tabs>
        <div >
            <!-- <GMap></GMap> -->
            <div class="device-info"  v-if="devicew_visible" v-drag-window>
              <div>
                <div class="drag-title">设备信息</div>
                <!-- <div class="drag-title" style="height: 40px; width: 100% ;line-height: 40px; color: aliceblue">无人机视频</div>  ../../assets/v3/icon/uav-fly.png-->
                <a style="position: absolute; right: 10px; top: 10px; font-size: 16px; color: white;" @click="closeDevice()"><CloseOutlined /></a>
              </div>
              <!-- <Devicebox></Devicebox> -->

            </div>
        </div>
      </div>
    </div>
   </div>

    <!-- <div style="height: 100%;">
        <GMap></GMap>
    </div> -->

  </div>
  <div class="plan-panel-wrapper" >
  </div>
</template>

<script lang="ts" setup>
import GMap from '/@/components/GMap.vue'
import { computed, onMounted, reactive, ref, watch, WritableComputedRef } from 'vue'
import { EDeviceTypeName, ELocalStorageKey } from '/@/types'
import noData from '/@/assets/icons/no-data.png'
import rc from '/@/assets/icons/rc.png'
import { OnlineDevice, EModeCode, OSDVisible, EDockModeCode, DeviceOsd } from '/@/types/device'
import { useMyStore } from '/@/store'
import { getDeviceTopo, getUnreadDeviceHms, updateDeviceHms, getPlatformInfo, getAllWorkspaceInfo } from '/@/api/manage'
import { RocketOutlined, EyeInvisibleOutlined, EyeOutlined, RobotOutlined, DoubleRightOutlined, CloseOutlined } from '@ant-design/icons-vue'
import { EHmsLevel } from '/@/types/enums'
// import Devicebox from '/@/components/devices/device-info.vue'
import { TrustedServers } from 'cesium'
import CustomTree from '/@/components/substationTree.vue'

const activeKey = ref('EDeviceTypeName.Aircraft')
const workspaceName = ref('')

const store = useMyStore()
const username = ref(localStorage.getItem(ELocalStorageKey.Username))
const userId = ref(localStorage.getItem(ELocalStorageKey.UserId)!)
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

const deviceInfo = computed(() => store.state.deviceState.deviceInfo)
const dockInfo = computed(() => store.state.deviceState.dockInfo)
const hmsInfo = computed({
  get: () => store.state.hmsInfo,
  set: (val) => {
    return val
  }
})

onMounted(() => {
  getOnlineTopo()
  setTimeout(() => {
    watch(() => store.state.deviceStatusEvent,
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

  // 获取平台信息
  getPlatformInfo().then(res => {
    workspaceName.value = res.data.workspace_name
  })
  // 添加树形图数据
  getTreeData()

  const element = document.getElementsByClassName('scrollbar').item(0) as HTMLDivElement
  const parent = element?.parentNode as HTMLDivElement
  scorllHeight.value = parent?.clientHeight - parent?.firstElementChild?.clientHeight
})

// ---------------------------------------------------------切换工作空间-------------------------------------

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
  selectedNode.value = node
  console.log('Node changed in parent:', node) // 确认父组件事件是否触发
}
//= =========================================================================切换工作空间   已废弃================================
const showSelectDialog = ref(false)
const workspaceOption = ref([])
// // 切换工作空间
function changeWorkspace () {
  showSelectDialog.value = true
  getAllWorkspaceInfo(userId.value).then(res => {
    // 转换数据格式
    const workspaces = res.data
    // workspaceOption.value = workspaces.map(workspace => ({
    //   value: workspace.workspace_id,
    //   label: workspace.workspace_name
    // }))
    // console.log('asfas', workspaces)
  })
}
const selectedWorkspace = ref('')
function updateWorkspaceOption (val) {
  // 根据选中的值找到对应的 label  //更新空间名称
  const selectedOption = workspaceOption.value.find(item => item.value === val)
  workspaceName.value = selectedOption.label
}
function SubmitSelect () {
  showSelectDialog.value = false
  getOnlineTopo()
}
//= =============================================================================设备绑定=====================================================
function devicebind () {
  showSelectDialog.value = true
}
// ----------------------------------------------------------------------------------------------------------------
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
  })
}

const devicew_visible = ref(false) // 是否展示设备详细信息
const device_model = ref('') // 点击记录设备型号
const device_sn = ref('') // 点击记录设备编号
const ssss = ref({})
function switchVisible (e: any, device: OnlineDevice, isDock: boolean, isClick: boolean) {
  if (!isClick) {
    e.target.style.cursor = 'not-allowed'
    return
  }
  if (device.sn === osdVisible.value.sn) {
    osdVisible.value.visible = !osdVisible.value.visible
    devicew_visible.value = !devicew_visible.value
  } else {
    osdVisible.value.sn = device.sn
    osdVisible.value.callsign = device.callsign
    osdVisible.value.model = device.model
    osdVisible.value.visible = true
    osdVisible.value.gateway_sn = device.gateway.sn
    osdVisible.value.is_dock = isDock
    osdVisible.value.gateway_callsign = device.gateway.callsign
    osdVisible.value.payloads = device.payload

    devicew_visible.value = true

    console.log('sadfs0', osdVisible.value.model)
  }
  store.commit('SET_OSD_VISIBLE_INFO', osdVisible)

  // 查找到当前记录
  device_model.value = device.model
  device_sn.value = device.sn
  ssss.value = device.gateway
}
// 关闭设备详细信息界面
function closeDevice () {
  devicew_visible.value = false
  osdVisible.value.visible = false
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

.custom-select{
  // margin-top: 10px;
  width: 300px;
  :deep(.el-select--large .el-select__wrapper){
    min-height: 30px;
  }
  :deep(.el-select__wrapper){
    background-color:#05204B;
  }
  :deep(.custom-select .el-select-dropdown) {
    background-color: transparent !important; /* 透明背景 */
    border: 1px solid blue !important; /* 边框颜色 */
  }

  :deep(.custom-select .el-select-dropdown__item) {
    background-color: #05204B !important; /* 下拉项背景色 */
    color: #fff !important; /* 字体颜色 */
  }

  :deep(.el-select.is-disabled .el-select__dropdown){
    background:#05204B;
    color: white;
  }
  :deep(.el-select__placeholder){
    color: white;
  }
}

:deep(.el-dialog__title) {
  font-size: 18px; /* 修改为你想要的大小 */
  // font-weight: bold;
  color: #F1F6FF;
  // border-bottom: 1px solid #686868;
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid #b5b2b2e6;
}
::v-deep .el-dialog {
  background-color: #0B2757;
  // background: rgba(59, 116, 255, 0.15);
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
}

.device-info{
  position: absolute;
  z-index: 100;
  left: 300px;
  top: 100px;
  margin-left: 0px;

  text-align: center;
  width: 630px;
  height: 250px;
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

.container {
  // height: 100%;
  width: 100%;
  padding: 10px;
  display: flex;
  flex-direction: column; /* 垂直排列 */

}

.main-box{
  display: flex; /* 使用 flexbox 布局 */
  height: 100vh; /* 设置容器高度为视口高度 */
}
.box-left{
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  width: 20%; /* 左侧占据 20% 的宽度 */
  // background-color: #4CAF50; /* 绿色背景 */
  padding: 20px; /* 内边距 */
  color: white; /* 字体颜色 */
  border-radius: 15px;
  border: none;
  height: 85vh;
}
.box-right{
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  flex: 1; /* 右侧占据剩余空间 */
  width: calc(80% - 10px);
  margin-left: 10px;
  // background-color: #2196F3; /* 蓝色背景 */
  padding: 20px; /* 内边距 */
  color: white; /* 字体颜色 */
  border-radius: 15px;
  border: none;
  height: 85vh;
}
// 头部  标题 面包屑
.header {
  width: 100%;
  height: 60px;
  background: #05204B;
  padding: 16px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color:aliceblue;
}
.tablelw{
  width: 100%;
}

.btn{
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color:rgba(255, 255, 255, 0.762);
}
.operation{
 height: fit-content;
 padding: 15px;
//  display: flex;
//  justify-content: space-between;
 color: rgba(255, 255, 255, 0.762);
 background-color: rgba(0, 112, 209, 0.2);
 font-size: 16px;
 border: 4px solid rgba(0, 112, 209, 1);
 border-bottom: none;
 border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
//  .item1{
//   display: flex;
//   justify-items: center;
//   align-items: center;
//  }
}

.plan-panel-wrapper {
  width: 100%;
  padding: 16px;
  .plan-table {
    background: #fff;
    margin-top: 10px;
  }
  .action-area {

    &::v-deep {
      .ant-btn {
        margin-right: 10px;
        margin-bottom: 10px;
      }
    }
  }

  .circle-icon {
    display: inline-block;
    width: 12px;
    height: 12px;
    margin-right: 3px;
    border-radius: 50%;
    vertical-align: middle;
    flex-shrink: 0;
  }
}
// 设置标签容器样式，居中对齐
:deep(.custom-tabs .ant-tabs-nav-wrap)  {
  display: flex;
  justify-content: left;
}
// 将标签样式改为按钮样式，设置背景色、文字颜色、圆角、内边距和鼠标指针。
:deep( .custom-tabs .ant-tabs-tab) {
  border: 2px solid #1299C3;
  background-color: #055278;
  color:rgba(255, 255, 255, 0.762);
  border-radius: 4px;
  margin: 0 4px;
  padding: 8px 16px;
  transition: background-color 0.3s;
  cursor: pointer;
}
// 设置选中标签的背景色和文字颜色。
:deep(.custom-tabs .ant-tabs-tab-active) {
  background: linear-gradient(to top, #11B4FB, #023956);
  color: white;
}
// 设置标签悬停时的样式
:deep(.custom-tabs .ant-tabs-tab:hover) {
  background-color: #40a9ff;
  color: white;
}
// 隐藏默认的下划线
:deep(.custom-tabs .ant-tabs-ink-bar) {
  display: none !important;
}
// 设置内容容器样式
:deep(.custom-tabs .ant-tabs-content-holder) {
  padding-top: 16px;
}
// 表格
// 表格 无数据内容背景设置
:deep(.el-table__empty-block){
   background-color: #0A2D63;
}
// 表格最后一条白线
:deep .el-table__inner-wrapper::before{
    height: 0;
}
.project-tsa-wrapper > :first-child {
  height: 50px;
  line-height: 50px;
  align-items: center;
  border-bottom: 1px solid #4f4f4f;
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
  background: $success;
  animation: blink 500ms infinite;
}
.caution-blink {
  background: orange;
  animation: blink 500ms infinite;
}
.warn-blink {
  background: red;
  animation: blink 500ms infinite;
}
.notice {
  background: $success;
  overflow: hidden;
  cursor: pointer;
}
.caution {
  background: orange;
  cursor: pointer;
  overflow: hidden;
}
.warn {
  background: red;
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
