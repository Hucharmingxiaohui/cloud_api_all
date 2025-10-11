<template>
  <a-drawer
    title="设备日志上传记录"
    placement="right"
    v-model:visible="sVisible"
    @update:visible="onVisibleChange"
    :width="800"
    wrapClassName="drawer-style"
    >
    <!-- 设备日志上传记录 -->
    <div class="device-log-upload-record-wrap">
      <div class="page-action-row">
        <el-button class="new_btn iconfont icon-yunshangchuan_o" type="primary" style="margin-left: 10px; width: 100px;"
        @click="onUploadDeviceLog">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/search.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">上传日志</span>
      </el-button>
      </div>
      <div class="device-log-upload-list">

        <el-table
          :data="deviceUploadLogState.uploadLogList"
          row-key="logs_id"
          default-expand-all
          stripe
        >
          <el-table-column
              prop="create_time"
              label="上传时间"
              show-overflow-tooltip
            />
         <el-table-column
            prop="device_type"
            label="设备类型"
            show-overflow-tooltip
          >
            <template #default="scope">
              <div>
              <div v-if="getDeviceInfo(scope.row).parents && getDeviceInfo(scope.row).parents.length > 0">{{ DEVICE_NAME[getDeviceInfo(scope.row).parents[0].device_model.device_model_key]}}</div>
              <div v-if="getDeviceInfo(scope.row).hosts && getDeviceInfo(scope.row).hosts.length > 0">{{ DEVICE_NAME[getDeviceInfo(scope.row).hosts[0].device_model.device_model_key]}}</div>
            </div>
            </template>
          </el-table-column>

          <el-table-column
            prop="device_sn"
            label="设备SN"
            show-overflow-tooltip
          >
            <template #default="scope">
              <div>
                <div v-if="getDeviceInfo(scope.row).parents && getDeviceInfo(scope.row).parents.length > 0">{{ getDeviceInfo(scope.row).parents[0].sn }}</div>
                <div v-if="getDeviceInfo(scope.row).hosts && getDeviceInfo(scope.row).hosts.length > 0">{{ getDeviceInfo(scope.row).hosts[0].sn }}</div>
            </div>
            </template>
          </el-table-column>
          <el-table-column
            prop="status"
            label="上传状态"
            show-overflow-tooltip
          >
            <template #default="scope">
              <div>
              <div>
                <span class="circle-icon" :style="{backgroundColor: getDeviceLogUploadStatus(scope.row).color}"></span>
                {{ getDeviceLogUploadStatus(scope.row).text }}
              </div>
              <div v-if="scope.row.status === DeviceLogUploadStatusEnum.Uploading">
                <a-progress :percent="getLogProgress(scope.row)" />
              </div>
            </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <div class="editable-row-operations">

                <div class="flex-align-center flex-row" style="color: #2d8cf0">
                  <el-button size="small" link type="primary" class="wayliedit"
                  @click="showDeviceLogDetail(scope.row)">详情</el-button>
                  <el-button size="small" link type="primary" class="wayliedit" v-if="scope.row === DeviceLogUploadStatusEnum.Uploading"
                  @click="onCancelUploadDeviceLog(scope.row)">取消</el-button>
                  <el-button size="small" link type="primary" class="wayliedit" v-else
                  @click="onDeleteUploadDeviceLog(scope.row)">删除</el-button>
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-container">
        <el-pagination v-model:current-page="deviceUploadLogState.paginationProp.current" v-model:page-size="deviceUploadLogState.paginationProp.pageSize"
          :page-sizes="deviceUploadLogState.paginationProp.pageSizeOptions" :total="deviceUploadLogState.paginationProp.total"
          layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handleCurrentChange"></el-pagination>
      </div>
      </div>
    </div>
  </a-drawer>
  <!-- 设备日志上传弹框 -->
  <DeviceLogUploadModal
     v-model:visible="deviceLogUploadModalVisible"
     :device="props.device"
     @upload-log-ok="onUploadLogOk"
  ></DeviceLogUploadModal>

  <!-- 设备日志上传详情弹框 -->
  <DeviceLogDetailModal
     v-model:visible="deviceLogDetailModalVisible"
     :deviceLog="currentDeviceLog"
  ></DeviceLogDetailModal>
</template>

<script lang="ts" setup>
import { watchEffect, reactive, ref, defineProps, defineEmits } from 'vue'
import { ColumnProps, TableState } from 'ant-design-vue/lib/table/interface'
import { IPage } from '/@/api/http/type'
import { Device, DOMAIN, DEVICE_NAME } from '/@/types/device'
import DeviceLogUploadModal from './DeviceLogUploadModal.vue'
import DeviceLogDetailModal from './DeviceLogDetailModal.vue'
import { getDeviceUploadLogList, GetDeviceUploadLogListRsp, cancelDeviceLogUpload, deleteDeviceLogUpload } from '/@/api/device-log'
import { StopOutlined, DeleteOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import { DeviceLogUploadStatusEnum, DeviceLogUploadStatusMap, DeviceLogUploadStatusColor, DeviceLogUploadInfo, DeviceLogUploadWsStatusMap, DeviceLogProgressInfo } from '/@/types/device-log'
import { useDeviceLogUploadProgressEvent } from './use-device-log-upload-progress-event'
import { Modal } from 'ant-design-vue'

const props = defineProps<{
  visible: boolean,
  device: null | Device,
}>()
const emit = defineEmits(['update:visible'])

const sVisible = ref(false)

watchEffect(() => {
  sVisible.value = props.visible
  // 显示弹框时，获取设备日志上传记录信息
  if (props.visible) {
    getDeviceUploadLogInfo()
  }
})

function onVisibleChange (sVisible: boolean) {
  setVisible(sVisible)
}

function setVisible (v: boolean, e?: Event) {
  sVisible.value = v
  emit('update:visible', v, e)
}

// 日志列表
const deviceLogUploadListColumns: ColumnProps[] = [
  { title: '上传时间', dataIndex: 'create_time', width: 100 },
  { title: '设备型号', dataIndex: 'device_type', width: 80, slots: { customRender: 'device_type' } },
  { title: '设备SN', dataIndex: 'device_sn', width: 120, slots: { customRender: 'device_sn' } },
  { title: '上传状态', dataIndex: 'status', width: 120, slots: { customRender: 'status' } },
  { title: '操作', dataIndex: 'actions', width: 80, slots: { customRender: 'action' } },
]

const deviceUploadLogState = reactive({
  uploadLogList: [] as GetDeviceUploadLogListRsp[],
  loading: false,
  paginationProp: {
    pageSizeOptions: ['5', '10', '20'],
    showQuickJumper: true,
    showSizeChanger: true,
    pageSize: 5,
    current: 1,
    total: 0
  }
})

// 获取上传的设备日志
async function getDeviceUploadLogInfo () {
  deviceUploadLogState.loading = true
  try {
    const { code, data } = await getDeviceUploadLogList({
      device_sn: props.device?.device_sn || '',
      page: deviceUploadLogState.paginationProp.current,
      page_size: deviceUploadLogState.paginationProp.pageSize
    })
    if (code === 0) {
      deviceUploadLogState.uploadLogList = data.list
      deviceUploadLogState.paginationProp.total = data.pagination.total
      deviceUploadLogState.paginationProp.current = data.pagination.page
      deviceUploadLogState.paginationProp.pageSize = data.pagination.page_size
    }
    deviceUploadLogState.loading = false
  } catch (error) {
    deviceUploadLogState.loading = false
  }
}
type Pagination = TableState['pagination']

// 获取设备信息
function getDeviceInfo (deviceLogItem: GetDeviceUploadLogListRsp) {
  const { device_topo: deviceTopo } = deviceLogItem
  return deviceTopo
}

// 获取上传状态
function getDeviceLogUploadStatus (deviceLogItem: GetDeviceUploadLogListRsp) {
  const statusObj = {
    color: '',
    text: ''
  }
  const { status } = deviceLogItem
  statusObj.color = DeviceLogUploadStatusColor[status]
  statusObj.text = DeviceLogUploadStatusMap[status]
  return statusObj
}

// 获取上传进度
function getLogProgress (deviceLogItem: GetDeviceUploadLogListRsp) {
  let percent = 0
  const { logs_progress } = deviceLogItem
  if (logs_progress && logs_progress.length > 0) {
    logs_progress.forEach(log => {
      percent += (log.progress || 0)
    })
    percent = percent / logs_progress.length
  }
  return Math.floor(percent)
}

// 设备日志上传进度更新
function onDeviceLogUploadWs (data: DeviceLogUploadInfo) {
  const { sn, output } = data
  if (output) {
    const { files, status, logs_id: logId } = output || {}
    const deviceLogItem = deviceUploadLogState.uploadLogList.find(log => log.logs_id === logId)
    if (!deviceLogItem) return
    if (status) {
      deviceLogItem.status = DeviceLogUploadWsStatusMap[status]
    }
    if (files && files.length > 0) {
      const logsProgress = [] as DeviceLogProgressInfo[]
      files.forEach(file => {
        logsProgress.push({
          ...file,
          status: DeviceLogUploadWsStatusMap[file.status]
        })
      })
      deviceLogItem.logs_progress = logsProgress
    }
  }
}

useDeviceLogUploadProgressEvent(onDeviceLogUploadWs)

// 搜索
async function onDeviceUploadLogTableChange (page: Pagination) {
  deviceUploadLogState.paginationProp.current = page?.current || 1
  deviceUploadLogState.paginationProp.pageSize = page?.pageSize || 20
  await getDeviceUploadLogInfo()
}

async function handleSizeChange (val: number) {
  // paginationProp.pageSize = val
  // refreshData(paginationProp)
  // paginationProp.current = 1 // 重置为第一页

  deviceUploadLogState.paginationProp.current = 1
  deviceUploadLogState.paginationProp.pageSize = val
  await getDeviceUploadLogInfo()
}
async function handleCurrentChange (val: number) {
  // deviceUploadLogState.paginationProp.pageSize = val
  deviceUploadLogState.paginationProp.current = val
  await getDeviceUploadLogInfo()
}

// 查看上传设备日志详情
const deviceLogDetailModalVisible = ref(false)
const currentDeviceLog = ref({} as GetDeviceUploadLogListRsp)

function showDeviceLogDetail (deviceLogItem: GetDeviceUploadLogListRsp) {
  if (!deviceLogItem) return
  currentDeviceLog.value = deviceLogItem
  deviceLogDetailModalVisible.value = true
}

// 取消上传设备日志
async function onCancelUploadDeviceLog (deviceLogItem: GetDeviceUploadLogListRsp) {
  Modal.confirm({
    title: '取消日志上传',
    content: '您确认取消设备日志上传吗？',
    okType: 'danger',
    onOk () {
      cancelDeviceLogUploadOk()
    },
  })
}

async function cancelDeviceLogUploadOk () {
  const { code } = await cancelDeviceLogUpload({
    device_sn: props.device?.device_sn || '',
    module_list: [DOMAIN.DOCK, DOMAIN.DRONE],
    status: 'cancel'
  })
  if (code === 0) {
    await getDeviceUploadLogInfo()
  }
}

// 删除上传的设备日志
function onDeleteUploadDeviceLog (deviceLogItem: GetDeviceUploadLogListRsp) {
  Modal.confirm({
    title: '删除上传日志',
    content: '您确认删除该条已上传设备日志吗？',
    okType: 'danger',
    onOk () {
      deleteUploadDeviceLogOk(deviceLogItem)
    },
  })
}

async function deleteUploadDeviceLogOk (deviceLogItem: GetDeviceUploadLogListRsp) {
  const { code } = await deleteDeviceLogUpload({
    device_sn: props.device?.device_sn || '',
    logs_id: deviceLogItem.logs_id
  })
  if (code === 0) {
    await getDeviceUploadLogInfo()
  }
}

// 上传日志
const deviceLogUploadModalVisible = ref(false)

function onUploadDeviceLog () {
  deviceLogUploadModalVisible.value = true
}

function onUploadLogOk () {
  // 刷新列表
  getDeviceUploadLogInfo()
}
</script>

<style lang="scss" scoped>
.device-log-upload-record-wrap{
  .page-action-row{
    display: flex;
    justify-content: space-between;
    width: 100%;
  }

  .device-log-upload-list{
    padding: 20px 0 10px;
    // background: #00173B;
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

  .row-action{
    color: #2d8cf0;
    & > span{
      margin-right: 10px;
    }
  }
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
// 修改侧边栏

:deep(.ant-progress-text){
  color: #fff !important;
}

:global(.ant-drawer-close){
  color: #fff;
}
:global(.drawer-style .ant-drawer-content) {
  background-color: #0B2756;
}
:global(.drawer-style .ant-drawer-header) {
  background-color: #0B2756;
  border-bottom: 1px solid #253653;
}
:global(.ant-drawer-title) {
  color: white;
}

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
    }
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
:deep(.el-select__wrapper) {
  background-color: #0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: aliceblue;
}
:deep(.el-input__wrapper) {
  background-color: #0B2756;
}
:deep(.el-input__inner){
  color: #fff !important;
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
</style>
