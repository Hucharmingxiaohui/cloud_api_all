
<template>

<div class="container">
    <!-- <div class="header">航线管理</div> -->
    <div class="operation">
      <el-button class="new_btn iconfont icon-uav" type="primary" style="margin-left: 10px; width: 100px;"
        @click="select(EDeviceTypeName.Aircraft)">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/search.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">无人机</span>
      </el-button>
      <el-button class="new_btn iconfont icon-wurenjijichang" type="primary" style="margin-left: 10px; width: 100px;"
        @click="select(EDeviceTypeName.Dock)">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/search.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">机场</span>
      </el-button>
    </div>

  </div>
  <!-- <a-menu v-model:selectedKeys="current" mode="horizontal" @select="select">
    <a-menu-item :key="EDeviceTypeName.Aircraft" class="ml20">
      Aircraft
    </a-menu-item>
    <a-menu-item :key="EDeviceTypeName.Dock">
      Dock
    </a-menu-item>
  </a-menu> -->
  <div class="content">
      <div class="table-container">
        <el-table
          :data="data.device"
          :tree-props="treeProps"
          row-key="device_sn"
          default-expand-all
          stripe
        >
          <el-table-column  type="selection" width="55" :selectable="selectable" />
          <el-table-column label="序号" align='center' width="60">
              <template #default="scope">
                {{ scope.$index +(paginationProp.current - 1) * paginationProp.pageSize+ 1 }}
              </template>
            </el-table-column>
          <el-table-column label="模型" prop="nickname">
            <template #default="scope">
              <div>
                <el-input v-if="editableData[scope.row.device_sn]" v-model="editableData[scope.row.device_sn].nickname"
                  style="margin: -5px 0" />
                <template v-else>
                  <el-tooltip :content="scope.row.nickname">
                    <div style="display: flex; justify-content: center; align-items: center;">
                      <span v-if="(judgeCurrentType(EDeviceTypeName.Dock) && scope.row.domain !== EDeviceTypeName.Dock)" style="height: 30px; width: 30px;">
                        <span style="border-left: 2px solid rgb(200,200,200);border-bottom: 2px solid rgb(200,200,200);height: 15px; width: 15px;  display: inline-block;"></span>
                      </span>
                      <span>{{ scope.row.nickname }}</span>
                    </div>
                  </el-tooltip>
                </template>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="device_sn" label="SN" />
          <el-table-column prop="device_name" label="名称" />
          <el-table-column label="固件版本" prop="firmware_version">
            <template #default="scope">
              <span v-if="judgeCurrentType(EDeviceTypeName.Dock)">
                <DeviceFirmwareUpgrade :device="scope.row" class="table-flex-col" @device-upgrade="onDeviceUpgrade" />
              </span>
              <span v-else>
                {{ scope.row.firmware_version }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" prop="status">
            <template #default="scope">
              <span v-if="scope.row.status" class="flex-row flex-align-center" style="justify-content: center;">
                <span class="mr5" style="width: 12px; height: 12px; border-radius: 50%; background-color: green;" />
                <span>在线</span>
              </span>
              <span class="flex-row flex-align-center" style="justify-content: center;" v-else>
                <span class="mr5" style="width: 12px; height: 12px; border-radius: 50%; background-color: red;" />
                <span>离线</span>
              </span>
            </template>
          </el-table-column>
          <el-table-column
            prop="workspace_name"
            label="工作空间"
            show-overflow-tooltip
          >
            <template #default="scope">
              <span v-if="current.indexOf(EDeviceTypeName.Dock) !== -1 && scope.row.domain === EDeviceTypeName.Aircraft">
                <!-- 空白显示 -->
              </span>
              <span v-else>{{ scope.row.workspace_name }}</span>
            </template>
          </el-table-column>

          <!-- 添加时间列 -->
          <el-table-column
            prop="bound_time"
            label="添加时间"
            :sortable="true"
          ></el-table-column>

            <!-- 上一次在线时间列 -->
          <el-table-column
            prop="login_time"
            label="上一次在线时间"
            :sortable="true"
          ></el-table-column>

          <el-table-column label="操作" width="200">
            <template #default="scope">
              <div class="editable-row-operations">
                <div v-if="editableData[scope.row.device_sn]">
                  <el-tooltip content="确认修改">
                    <span @click="save(scope.row)" style="color: #28d445;">
                      <CheckOutlined />
                    </span>
                  </el-tooltip>
                  <el-tooltip content="取消修改">
                    <span
                      @click="() => delete editableData[scope.row.device_sn]"
                      style="color: #e70102;"
                    >
                      <CloseOutlined />
                    </span>
                  </el-tooltip>
                </div>
                <div v-else class="flex-align-center flex-row" style="color: #2d8cf0">
                  <el-button size="small" link type="primary" class="wayliedit" v-if="current.indexOf(EDeviceTypeName.Dock) !== -1"
                  @click="showDeviceLogUploadRecord(scope.row)">日志</el-button>
                  <el-button size="small" link type="primary" class="wayliedit" v-if="current.indexOf(EDeviceTypeName.Dock) !== -1"
                  @click="showHms(scope.row)">HMS</el-button>
                  <el-button size="small" link type="primary" class="wayliedit"
                    @click="edit(scope.row)">编辑</el-button>
                  <el-popconfirm    width="220" confirm-button-text="确定"
                    cancel-button-text="取消" icon-color="#626AEF" title="你确定要删除飞行任务吗？"
                    @confirm="unbind">
                    <template #reference>
                      <el-button size="small" link type="primary" class="preview" @click="() => { deleteTip = true, deleteSn = scope.row.device_sn }" >删除</el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <!-- <el-table :data="data.device" stripe  :loading="loading"  row-key="device_sn" :tree-props="treeProps" :row-selection="rowSelection">
          <el-table-column type="selection" width="55"  />
          <el-table-column label="模型" prop="nickname">
            <template #default="scope">
              <div>
                <el-input v-if="editableData[scope.row.device_sn]" v-model="editableData[scope.row.device_sn].nickname"
                  style="margin: -5px 0" />
                <template v-else>
                  <el-tooltip :content="scope.row.nickname">
                    <span>{{ scope.row.nickname }}</span>
                  </el-tooltip>
                </template>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="SN" prop="device_sn">
            <template #default="scope">
              <div>
                <template >
                  <el-tooltip :content="scope.row.device_sn">
                    <span>{{ scope.row.device_sn }}</span>
                  </el-tooltip>
                </template>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="编号">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.id }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="330px">
            <template #default="scope">
              <div class="action-buttons">
                <el-button size="small" link type="primary" class="download"
                  @click="downloadWayline(scope.row.id, scope.row.name)">下载</el-button>
                <el-button size="small" link type="primary" class="preview"
                  @click="openDrag(scope.row.id, scope.row.template_types[0])">预览</el-button>
                <el-button size="small" link type="primary" class="waylipot"
                  @click="openWaylinePoints(scope.row)">航点</el-button>
                <el-button size="small" link type="primary" class="wayliedit"
                  @click="editDrag(scope.row.id, scope.row.name, scope.row.template_types[0])">编辑</el-button>
                <el-popconfirm width="220" confirm-button-text="确定" cancel-button-text="不，谢谢" icon-color="#626AEF"
                  title="航线文件一旦删除就无法恢复,是否继续？" @confirm="deleteWayline(scope.row.id)">
                  <template #reference>
                    <el-button size="small" link type="primary" class="delete">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table> -->
      </div>
      <div class="pagination-container">
      <el-pagination v-model:current-page="paginationProp.current" v-model:page-size="paginationProp.pageSize"
        :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
        layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
        @current-change="handleCurrentChange"></el-pagination>
    </div>
    </div>

    <!-- <div class="pagination-container">
      <el-pagination v-model:current-page="paginationProp.current" v-model:page-size="paginationProp.pageSize"
        :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
        layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
        @current-change="handleCurrentChange"></el-pagination>
    </div> -->
    <!-- 设备升级 -->
    <DeviceFirmwareUpgradeModal title="设备升级" v-model:visible="deviceFirmwareUpgradeModalVisible" :device="selectedDevice"
      @ok="onUpgradeDeviceOk"></DeviceFirmwareUpgradeModal>

    <!-- 设备日志上传记录 -->
    <DeviceLogUploadRecordDrawer v-model:visible="deviceLogUploadRecordVisible" :device="currentDevice">
    </DeviceLogUploadRecordDrawer>

    <!-- hms 信息 -->
    <DeviceHmsDrawer v-model:visible="hmsVisible" :device="currentDevice">
    </DeviceHmsDrawer>
</template>
<script lang="ts" setup>
import { ColumnProps, TableState } from 'ant-design-vue/lib/table/interface'
import { h, onMounted, reactive, ref, UnwrapRef } from 'vue'
import { IPage } from '/@/api/http/type'
import { BindBody, bindDevice, getBindingDevices, unbindDevice, updateDevice } from '/@/api/manage'
import { EDeviceTypeName, ELocalStorageKey } from '/@/types'
import { EditOutlined, CheckOutlined, CloseOutlined, DeleteOutlined, FileSearchOutlined, CloudServerOutlined } from '@ant-design/icons-vue'
import { Device, DeviceFirmwareStatusEnum } from '/@/types/device'
import DeviceFirmwareUpgrade from '/@/components/devices/device-upgrade/DeviceFirmwareUpgrade.vue'
import DeviceFirmwareUpgradeModal from '/@/components/devices/device-upgrade/DeviceFirmwareUpgradeModal.vue'
import { useDeviceFirmwareUpgrade } from '/@/components/devices/device-upgrade/use-device-upgrade'
import { useDeviceUpgradeEvent } from '/@/components/devices/device-upgrade/use-device-upgrade-event'
import { DeviceCmdExecuteInfo, DeviceCmdExecuteStatus } from '/@/types/device-cmd'
import DeviceLogUploadRecordDrawer from '/@/components/devices/device-log/DeviceLogUploadRecordDrawer.vue'
import DeviceHmsDrawer from '/@/components/devices/device-hms/DeviceHmsDrawer.vue'
import { message, notification } from 'ant-design-vue'

//= ========================================================================================
// 控制是否显示复选框列
const showSelectionColumn = ref(true) // 控制复选框列是否显示
const selectable = (record: any) => {
  showSelectionColumn.value = !(judgeCurrentType(EDeviceTypeName.Dock) && record.domain !== EDeviceTypeName.Dock)
  return !(judgeCurrentType(EDeviceTypeName.Dock) && record.domain !== EDeviceTypeName.Dock)
}
//= =========================================================================================
interface DeviceData {
  device: Device[]
}
const treeProps = reactive({
  checkStrictly: true,
})
const loading = ref(true)
const deleteTip = ref<boolean>(false)
const deleteSn = ref<string>()

const rowClassName = (record: any, index: number) => {
  const className = []
  className.push(index % 2 === 0 ? 'even-row' : 'odd-row')
  if ((index & 1) === 0) {
    className.push('table-striped')
  }
  if (record.domain !== EDeviceTypeName.Dock) {
    className.push('child-row')
  }
  // return className.toString().replaceAll(',', ' ')
  return className.join(' ')
}

const expandRows = ref<string[]>([])
const data = reactive<DeviceData>({
  device: []
})

const paginationProp = reactive({
  pageSizeOptions: ['20', '50', '100'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 50,
  current: 1,
  total: 0
})

// 获取分页信息
function getPaginationBody () {
  return {
    page: paginationProp.current,
    page_size: paginationProp.pageSize
  } as IPage
}
function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  refreshData(paginationProp)
  paginationProp.current = 1 // 重置为第一页
}
function handleCurrentChange (val: number) {
  paginationProp.current = val
  refreshData(paginationProp)
}

const rowSelection = {
  onChange: (selectedRowKeys: (string | number)[], selectedRows: []) => {
    console.log(`selectedRowKeys: ${selectedRowKeys}`, 'selectedRows: ', selectedRows)
  },
  onSelect: (record: any, selected: boolean, selectedRows: []) => {
    console.log(record, selected, selectedRows)
  },
  onSelectAll: (selected: boolean, selectedRows: [], changeRows: []) => {
    console.log(selected, selectedRows, changeRows)
  },
  getCheckboxProps: (record: any) => ({
    disabled: judgeCurrentType(EDeviceTypeName.Dock) && record.domain !== EDeviceTypeName.Dock,
    style: judgeCurrentType(EDeviceTypeName.Dock) && record.domain !== EDeviceTypeName.Dock ? 'display: none' : ''
  }),
}
type Pagination = TableState['pagination']

const workspaceId: string = localStorage.getItem(ELocalStorageKey.WorkspaceId) || ''
const editableData: UnwrapRef<Record<string, Device>> = reactive({})
const current = ref([EDeviceTypeName.Aircraft])

function judgeCurrentType (type: EDeviceTypeName): boolean {
  return current.value.indexOf(type) !== -1
}

// 设备升级
const {
  deviceFirmwareUpgradeModalVisible,
  selectedDevice,
  onDeviceUpgrade,
  onUpgradeDeviceOk
} = useDeviceFirmwareUpgrade(workspaceId)

function onDeviceUpgradeWs (payload: DeviceCmdExecuteInfo) {
  updateDevicesByWs(data.device, payload)
}

function updateDevicesByWs (devices: Device[], payload: DeviceCmdExecuteInfo) {
  if (!devices || devices.length <= 0) {
    return
  }
  for (let i = 0; i < devices.length; i++) {
    if (devices[i].device_sn === payload.sn) {
      if (!payload.output) return
      const { status, progress, ext } = payload.output
      if (status === DeviceCmdExecuteStatus.Sent || status === DeviceCmdExecuteStatus.InProgress) { // 升级中
        const rate = ext?.rate ? (ext.rate / 1024).toFixed(2) + 'kb/s' : ''
        devices[i].firmware_status = DeviceFirmwareStatusEnum.DuringUpgrade
        devices[i].firmware_progress = (progress?.percent || 0) + '% ' + rate
      } else { // 终态：成功，失败，超时
        if (status === DeviceCmdExecuteStatus.Failed || status === DeviceCmdExecuteStatus.Timeout) {
          notification.error({
            message: `(${payload.sn}) Upgrade failed`,
            description: `Error Code: ${payload.result}`,
            duration: null
          })
        }
        // 拉取列表
        getDevices(current.value[0], true)
      }
      return
    }
    if (devices[i].children) {
      updateDevicesByWs(devices[i].children || [], payload)
    }
  }
}

useDeviceUpgradeEvent(onDeviceUpgradeWs)

// 获取设备列表信息
function getDevices (domain: number, closeLoading?: boolean) {
  if (!closeLoading) {
    loading.value = true
  }
  getBindingDevices(workspaceId, getPaginationBody(), domain).then(res => {
    if (res.code !== 0) {
      return
    }
    const resData: Device[] = res.data.list
    expandRows.value = []
    resData.forEach((val: any) => {
      if (val.children) {
        val.children = [val.children]
      }
      if (judgeCurrentType(EDeviceTypeName.Dock)) {
        expandRows.value.push(val.device_sn)
      }
    })
    data.device = resData
    // console.log('设备列表信息', resData)
    paginationProp.total = res.data.pagination.total
    paginationProp.current = res.data.pagination.page
    paginationProp.pageSize = res.data.pagination.page_size
    loading.value = false
  })
}

function refreshData (page: Pagination) {
  paginationProp.current = page?.current!
  paginationProp.pageSize = page?.pageSize!
  getDevices(current.value[0])
}

// 编辑
function edit (record: Device) {
  editableData[record.device_sn] = record
}

// 保存
function save (record: Device) {
  delete editableData[record.device_sn]
  updateDevice({ nickname: record.nickname }, workspaceId, record.device_sn)
}

// 删除
function showDeleteTip (sn: any) {
  deleteTip.value = true
}

// 解绑
function unbind () {
  deleteTip.value = false
  unbindDevice(deleteSn.value?.toString()!).then(res => {
    if (res.code !== 0) {
      return
    }
    getDevices(current.value[0])
  })
}

// 选择设备
function select (item: any) {
  getDevices(item)
  current.value = []
  current.value.push(item)
}

const currentDevice = ref({} as Device)
// 设备日志
const deviceLogUploadRecordVisible = ref(false)
function showDeviceLogUploadRecord (dock: Device) {
  deviceLogUploadRecordVisible.value = true
  currentDevice.value = dock
}

// 健康状态
const hmsVisible = ref<boolean>(false)

function showHms (dock: Device) {
  hmsVisible.value = true
  currentDevice.value = dock
}

onMounted(() => {
  console.log('测试', current)
  getDevices(current.value[0])
})
</script>

<style lang="scss" scoped>
.table {
  background-color: (255, 255, 255, 0.762) !important;
  margin: 20px;
  padding: 0px;
  border: none;
  height: 88vh;
}

.table-striped {
  background-color: #167bad;
  color: white;
  width: 100vh;
  padding: 0;
  border: none;
  border-collapse: collapse;
}

.ant-table-tbody tr td,
.ant-table td {
  border: none;
  padding: 0;
  white-space: nowrap;
}

.ant-table-fixed-header>.ant-table-content>.ant-table-scroll>.ant-table-body {
  background-color: transparent;
}

.ant-table-header,
.ant-table-thead tr th {
  background: rgb(0, 57, 154) !important;
  color: white;
  border: 0;
  // border: 1px solid rgb(0, 57, 154);
}

// th.ant-table-selection-column {
//   background-color: rgb(133, 8, 56) !important;
// }

.ant-menu {
  background: #012462;
  border: 1px solid #012462;
  color: white;
}

.ant-menu-title-content {
  color: white;
  font-weight: bold;
}

// .ant-table-header {
//   background-color: rgb(0, 57, 154) !important;
// }

.child-row {
  height: 70px;
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

.even-row {
  // background-color: rgba(0, 114, 245, 0.6);
  padding-top: 10px;
  background-color: rgba(0, 45, 120, 1);
  color: white;
  /* 浅灰色 */
}

.odd-row {
  background-color: rgba(0, 45, 120, 1);
  /* 白色 */
  color: white;
}
.even-row:hover, .odd-row:hover {
  background: transparent !important; /* 修改为你需要的颜色 */
  cursor: pointer; /* 鼠标悬停时显示手型 */
}
.table-striped {
  border-left: 5px solid #ececec;
  /* 示例样式 */
}

.child-row {
  color: white;
  /* 示例样式 */
}

/* 移除深度选择器和 scoped 的限制 */
table tbody tr:hover > td {
  background-color: rgba(0, 114, 245, 0.6) !important;
}

table tbody .ant-table-row-selected > td {
  background-color: rgba(0, 114, 245, 0.6) !important;
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
    width: 200px;
    height: 30px;
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

.custom-select {
  // margin-top: 10px;
  width: 300px;

  :deep(.el-select--large .el-select__wrapper) {
    min-height: 30px;
  }

  :deep(.el-select__wrapper) {
    background-color: #05204B;
  }

  :deep(.custom-select .el-select-dropdown) {
    background-color: transparent !important;
    /* 透明背景 */
    border: 1px solid blue !important;
    /* 边框颜色 */
  }

  :deep(.custom-select .el-select-dropdown__item) {
    background-color: #05204B !important;
    /* 下拉项背景色 */
    color: #fff !important;
    /* 字体颜色 */
  }

  :deep(.el-select.is-disabled .el-select__dropdown) {
    background: #05204B;
    color: white;
  }

  :deep(.el-select__placeholder) {
    color: white;
  }

  :deep(.el-cascader-panel) {
    // box-sizing: border-box;
    // list-style: none;
    // margin: 0;
    // min-height: 100%;
    // padding: 6px 0;
    // position: relative;
    background: #05204B;
    color: white;
  }

  :deep(el-cascader__dropdown.el-popper) {
    background: #05204b !important;
    ;
    color: white;
  }
}
:deep(.el-input__inner){
  color: #fff !important;
}
:deep(.el-input__wrapper){
  // background-color:#10739e !important;
  border: 1px solid #10739e !important;
  border-top: 1px solid #10739e !important;  // 确保上边框
  border-bottom: 1px solid #10739e !important; // 确保下边框
}

::v-deep .el-dialog__title {
  font-size: 16px;
  /* 修改为你想要的大小 */
  // font-weight: bold;
  color: #F1F6FF;
}

::v-deep .el-dialog {
  background-color: #0B2757;
  // background: rgba(59, 116, 255, 0.15);
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
}

.container {
  // height: 100%;
  width: 100vw;
  // padding: 10px;
  display: flex;
  flex-direction: column;
  /* 使子元素垂直排列 */
}

.ellipsis {
  white-space: nowrap;
  /* 防止换行 */
  overflow: hidden;
  /* 隐藏超出部分 */
  text-overflow: ellipsis;
  /* 显示省略号 */
}

.main-box {
  display: flex;
  /* 使用 flexbox 布局 */
  height: calc(100vh - 80px);
  /* 设置容器高度为视口高度 */
}

.box-right {
  // background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  flex: 1;
  /* 右侧占据剩余空间 */
  // width: calc(80% - 10px);
  // margin-left: 10px;
  // background-color: #2196F3; /* 蓝色背景 */
  // padding: 20px;
  /* 内边距 */
  color: white;
  /* 字体颜色 */
  // border-radius: 15px;
  // border: none;
  height: 100%;
}

// .box-right:hover {
//   box-shadow: inset 0px 0px 20px 3px rgba(34, 135, 255, 0.7);
// }
// 头部  标题 面包屑
.header {
  width: 100%;
  height: 60px;
  background: #05204B;
  padding: 16px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color: aliceblue;
}

// 下拉框
.select-operation {
  :deep(.el-select__placeholder) {
    color: rgba(255, 255, 255, 1);
    font-size: 14px;
    font-family: Google Sans-Medium;
    font-weight: 500;
  }

  :deep(.el-select__wrapper) {

    // background: rgba(59, 116, 255, 0.15);
    background-color: #0B2756;
    // box-shadow: inset 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
    box-shadow: 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
    // border: 1px solid #719fff;
    // border-radius: 4px;
    width: 200px;
    height: 30px;
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

// 操作部分
.operation {
  display: flex;
  // justify-items: center; /* 这里可能是错误的，flexbox 中应该使用 justify-content */
  align-items: center;
  /* 这个会确保 label 在垂直方向居中 */
  background-color: rgba(1, 36, 98, 1);
  border-radius: 4px;
  // width: 100%;
  height: 60px;
  margin: 31px 12px 0 12px;

  .label {
    height: 60px;
    display: flex;
    /* 这个可以保留，确保子元素居中 */
    align-items: center;
    /* 垂直居中 */
    justify-content: center;
    /* 水平居中 */
    color: rgba(255, 255, 255, 1);
    font-size: 14px;
    font-family: Google Sans-Medium;
    font-weight: 500;
    margin: 0 10px 0 30px;
  }

  .new_btn {
    background-image: linear-gradient(180deg,
        rgba(70, 145, 217, 1) 0,
        rgba(21, 81, 181, 1) 100%);
    border-radius: 4px;
    width: 108px;
    height: 37px;

    // margin: 12px 0 0 30px;
    .thumbnail_1 {
      width: 12px;
      height: 12px;
      margin: 5px 0 0 12px;
    }

    .btn_text {
      width: 70px;
      height: 18px;
      overflow-wrap: break-word;
      color: rgba(255, 255, 255, 1);
      font-size: 14px;
      font-family: Google Sans-Medium;
      font-weight: 500;
      text-align: left;
      white-space: nowrap;
      line-height: 14px;
      margin: 9px 20px 0 8px;
    }

  }

  .new_btn1 {
    background-image: linear-gradient(180deg,
        rgba(248, 212, 94, 1) 0,
        rgba(227, 157, 6, 1) 100%);
    border-radius: 4px;
    width: 70px;
    height: 37px;

    // margin: 12px 0 0 30px;
    .thumbnail_1 {
      width: 12px;
      height: 12px;
      margin: 5px 0 0 12px;
    }

    .btn_text {
      width: 30px;
      height: 18px;
      overflow-wrap: break-word;
      color: rgba(255, 255, 255, 1);
      font-size: 14px;
      font-family: Google Sans-Medium;
      font-weight: 500;
      text-align: left;
      white-space: nowrap;
      line-height: 14px;
      margin: 9px 20px 0 8px;
    }

  }

}

.content {
  margin: 15px 12px 0 12px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

// 操作下面的几个按钮的样式-------------------------------------------

.download,
.preview,
.temeasure,
.waylipot,
.wayliedit {
  background-color: rgba(51, 122, 255, 0.12);
  border-radius: 4px;
  height: 28px;
  border: 1px solid rgba(0, 64, 147, 1);
  width: 40px;
  margin-left: 7px;
  color: #fff;
}

.delete {
  background-color: rgba(255, 92, 51, 0.19);
  border-radius: 4px;
  height: 28px;
  color: rgba(255, 215, 215, 1);
  border: 1px solid rgba(255, 132, 132, 1);
  width: 40px;
  margin-left: 7px;
}

.action-buttons .el-button {
  color: white;
  /* 使文字颜色为白色 */
}

.action-buttons .delete {
  color: rgba(255, 215, 215, 1);
  /* 使文字颜色为白色 */
}

//----------------------------------------------------------------------
.btn {
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color: rgba(255, 255, 255, 0.762);
}

:deep(.btn1) {
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color: rgba(255, 255, 255, 0.762);
}

::v-deep .home-ant-input.ant-input-affix-wrapper .ant-input {
  background-color: black;
  color: #c5c8cc;
}

.live {
  position: absolute;
  z-index: 1;
  left: 0;
  top: 400px;
  margin-left: 345px;

  text-align: center;
  width: 1000px;
  height: 420px;
  background: #232323;
}

.live1 {
  padding: 10px 0 0 0;
  position: absolute;
  z-index: 1;
  left: 0;
  top: 200px;
  margin-left: 345px;
  width: 940px;
  height: 690px;
  // background: #232323;
  // background: rgba(59, 116, 255, 0.2);
  background-color: #205CA1;

  // background-color: #294A72;
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  .content {
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #06346A;
    padding: 20px;

    .content-left {
      width: 300px;
      // background-color:#1d4292;
      height: 600px;
      border-right: 1px solid #023956;
    }

    .content-right {
      width: 600px;
      background-color: #1d4292;
      height: 600px;
      border: 3px dashed #3667A7;
    }

  }

}

.pagination-container {
  position: absolute;
  bottom: 40px;
  /* 距离底部的距离，可调整 */
  left: 50%;
  /* 距离右边的距离，可调整 */
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  align-items: center;
}

// 输入框
:deep(.el-input) {
  --el-input-border-color: #1d4292;
}

:deep(.el-input__wrapper) {
  background-color: #0B2756;
}

:deep(.el-select__wrapper) {
  background-color: #0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: aliceblue;
}
// 移除树结构展开图标

::v-deep .el-table tr {
  background-color: #0B2756 !important;
  /* opacity: 0.6; */
  color: #F1F6FF;
  font-weight: bold;
}

// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #2264a7;
}

// 表格最后一条白线
:deep .el-table__inner-wrapper::before {
  height: 0;
}

/* // 修改高亮当前行颜色 */
::v-deep .el-table tbody tr:hover>td {
  background: rgba(0, 114, 245, 0.6) !important;
}

::v-deep .el-table td
 {
  border: 2px solid #01123288; /* 设置列的边框颜色和粗细 */
  font-size: 16px;
  font-weight: 500;
}

// 表格样式
::v-deep .el-table{
   .cell {
    text-align: center;
    :deep(.el-icon){
      height: 0 !important;
    }
    }
}
// 去掉树结构展开图标
::v-deep i.el-icon {
  height: 0 !important;
  width: 0 !important;
  // font-size: 10px !important;
}
::v-deep .el-table__expand-icon {
  height: 0 !important;
  width: 0 !important;
}
// // 表头大小
::v-deep .el-table th {
  height: 50px;
  font-size: 16px !important; /* 如果你需要修改表头字体大小，设置一个不同的大小 */
  color: rgba(255, 255, 255, 1);
  background-color: #00399A;
  border-left: 2px #01123288 solid;
  border-bottom: 2px #01123288 solid !important;
}
// :header-cell-style="{ height: '43px', color: 'rgba(255, 255, 255, 1)', fontSize: '16px ', fontWeight: 'bold', backgroundColor: '#00399A', borderLeft: '2px #01123288 solid', borderBottom: '1px #154480 solid' }">

/* // 斑马线颜色 */

::v-deep .el-table--striped .el-table__body tr.el-table__row--striped td {
  background: rgba(0, 45, 120, 1);
}

//分页数据
/* 修改前后箭头未点击时的背景颜色 */
:deep .el-pagination .btn-prev,
:deep .el-pagination .btn-next {
  background-color: #062254 !important;
  color: #fff;
}

/* 修改未点击时的数字方块背景颜色 */
:deep .el-pagination .el-pager li:not(.active):not(.disabled):hover {
  background-color: #2264a7 !important;
}

/* 未点击时的数字方块背景颜色 */
:deep .el-pagination .el-pager li:not(.active):not(.disabled) {
  background-color: #062254 !important;
  color: #fff;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #124AAD !important; //修改默认的背景色
  color: #fff;
}

::v-deep el-pager {
  background-color: #0B2756;
}

/* 移除表格行的底部边框 */
::v-deep .el-table__body-wrapper .el-table__row {
  border-bottom: none !important;
  /* 或者： */
  box-shadow: none !important;
}

/* 如果仍然看到白色线条，可能是阴影效果的原因 */
::v-deep .el-table__body-wrapper .el-table__row td {
  border-bottom: none !important;
}
</style>
