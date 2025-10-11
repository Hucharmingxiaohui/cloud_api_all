<template>
  <a-modal
    title="日志上传详情"
    v-model:visible="sVisible"
    width="900px"
    :footer="null"
    @update:visible="onVisibleChange">
    <div class="device-log-detail-wrap">
      <div class="device-log-list">
        <div class="log-list-item">
          <a-button type="primary" class="download-btn" :disabled="!airportTableLogState.logList?.file_id || !airportTableLogState.logList?.object_key"  size="small" @click="onDownloadLog(airportTableLogState.logList.file_id)">
             下载机场日志
          </a-button>
          <el-table
            :data="airportTableLogState.logList?.list"
            row-key="boot_index"
            stripe
          >
            <el-table-column  type="selection" width="55" />
            <el-table-column
              label="机场日志"
              prop="time"
            >
              <template #default="{ row }">
                <div>{{ getLogTime(row) }}</div>
              </template>
            </el-table-column>
            <el-table-column
              label="文件大小"
              prop="size"
            >
              <template #default="{ row }">
                <div>{{ getLogSize(row.size) }}</div>
              </template>
            </el-table-column>
          </el-table>
          <!-- <a-table  :columns="airportLogColumns"
                    :scroll="{ x: '100%', y: 600 }"
                    :data-source="airportTableLogState.logList?.list"
                    rowKey="boot_index"
                    :pagination = "false"
                    >
            <template #log_time="{record}">
              <div>{{getLogTime(record)}}</div>
            </template>
            <template #size="{record}">
              <div>{{getLogSize(record.size)}}</div>
            </template>
          </a-table> -->
        </div>
        <div class="log-list-item">
          <a-button type="primary"  class="download-btn" :disabled="!droneTableLogState.logList?.file_id || !droneTableLogState.logList?.object_key" size="small" @click="onDownloadLog(droneTableLogState.logList.file_id)">
             下载飞行器日志
          </a-button>
          <el-table
            :data="droneTableLogState.logList?.list"
            row-key="boot_index"
            stripe
          >
            <el-table-column  type="selection" width="55" />
            <el-table-column
              label="飞行器日志"
              prop="time"
            >
              <template #default="{ row }">
                <div>{{ getLogTime(row) }}</div>
              </template>
            </el-table-column>
            <el-table-column
              label="文件大小"
              prop="size"
            >
              <template #default="{ row }">
                <div>{{ getLogSize(row.size) }}</div>
              </template>
            </el-table-column>
          </el-table>
          <!-- <a-table  :columns="droneLogColumns"
                    :scroll="{ x: '100%', y: 600 }"
                    :data-source="droneTableLogState.logList?.list"
                    rowKey="boot_index"
                    :pagination = "false"
          >
            <template #log_time="{record}">
              <div>{{getLogTime(record)}}</div>
            </template>
            <template #size="{record}">
              <div>{{getLogSize(record.size)}}</div>
            </template>
          </a-table> -->
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { watchEffect, reactive, ref, defineProps, defineEmits } from 'vue'
import { ColumnProps, TableState } from 'ant-design-vue/lib/table/interface'
import { IPage } from '/@/api/http/type'
import { DOMAIN } from '/@/types/device'
import { DeviceLogFileInfo, GetDeviceUploadLogListRsp, getUploadDeviceLogUrl } from '/@/api/device-log'
import { useDeviceLogUploadDetail } from './use-device-log-upload-detail'
import { download } from '/@/utils/download'

const props = defineProps<{
  visible: boolean,
  deviceLog: null | GetDeviceUploadLogListRsp,
}>()
const emit = defineEmits(['update:visible'])

const sVisible = ref(false)

watchEffect(() => {
  sVisible.value = props.visible
  if (props.visible) {
    classifyDeviceLog()
  }
})

function onVisibleChange (sVisible: boolean) {
  setVisible(sVisible)
}

function setVisible (v: boolean, e?: Event) {
  sVisible.value = v
  emit('update:visible', v, e)
}

// 表格
const airportLogColumns: ColumnProps[] = [
  { title: '机场日志', dataIndex: 'time', width: '70%', slots: { customRender: 'log_time' } },
  { title: '文件大小', dataIndex: 'size', width: '30%', slots: { customRender: 'size' } },
]

const droneLogColumns: ColumnProps[] = [
  { title: '飞行器日志', dataIndex: 'time', width: '70%', slots: { customRender: 'log_time' } },
  { title: '文件大小', dataIndex: 'size', width: '30%', slots: { customRender: 'size' } },
]

const airportTableLogState = reactive({
  logList: {} as DeviceLogFileInfo,
})

const droneTableLogState = reactive({
  logList: {} as DeviceLogFileInfo,
})

function classifyDeviceLog () {
  if (!props.deviceLog) return
  const { device_logs } = props.deviceLog
  const { files } = device_logs || {}
  if (files && files.length > 0) {
    files.forEach(file => {
      if (file.module === DOMAIN.DOCK) {
        airportTableLogState.logList = file
      } else if (file.module === DOMAIN.DRONE) {
        droneTableLogState.logList = file
        console.log('日志详情数据', droneTableLogState.logList)
      }
    })
  }
}

const { getLogTime, getLogSize } = useDeviceLogUploadDetail()

async function onDownloadLog (fileId: string) {
  const { data } = await getUploadDeviceLogUrl({
    file_id: fileId,
    logs_id: props.deviceLog?.logs_id || ''
  })
  if (data) {
    // download(data)
    console.log('文件路径', data)
  // download('https:/github.com/dji-sdk/Mobile-SDK-Android-V5/archive/refs/heads/dev-sdk-main.zip')
  }
}

</script>

<style lang="scss" scoped>
.device-log-detail-wrap{

  .device-log-list{
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    .log-list-item{
      width: 420px;

      .download-btn{
        margin-bottom: 10px;
      }
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
