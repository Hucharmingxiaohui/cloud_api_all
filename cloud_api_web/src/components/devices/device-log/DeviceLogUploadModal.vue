<template>
  <a-modal
    title="设备日志上传"
    v-model:visible="sVisible"
    width="900px"
    :footer="null"
    @update:visible="onVisibleChange">
    <div class="device-log-upload-wrap">
      <div class="page-action-row">
        <el-button class="new_btn iconfont icon-yunshangchuan_o" :disabled="deviceLogUploadBtnDisabled" type="primary" style="margin-left: 10px; width: 100px;"
        @click="uploadDeviceLog">
          <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/search.png" /> -->
          <span style="margin-left: 5px; font-size: 14px;">上传日志</span>
        </el-button>
      </div>
      <div class="device-log-list">
        <div class="log-list-item">
          <el-table
            :data="airportTableLogState.logList?.list"
            :v-loading="airportTableLogState.tableLoading"
            row-key="boot_index"
            @selection-change = "handleairportchange"
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
                    :loading="airportTableLogState.tableLoading"
                    :row-selection="airportTableLogState.rowSelection"
                    rowKey="boot_index"
                    :pagination = "false">
            <template #log_time="{record}">
              <div>{{getLogTime(record)}}</div>
            </template>
            <template #size="{record}">
              <div>{{getLogSize(record.size)}}</div>
            </template>
          </a-table> -->
        </div>
        <div class="log-list-item">
          <el-table
            :data="droneTableLogState.logList?.list"
            :v-loading="droneTableLogState.tableLoading"
            row-key="boot_index"
            @selection-change = "handledronechange"
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
                    :loading="droneTableLogState.tableLoading"
                    :row-selection="droneTableLogState.rowSelection"
                    rowKey="boot_index"
                    :pagination = "false">
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
import { watchEffect, reactive, ref, computed, defineProps, defineEmits } from 'vue'
import { ColumnProps, TableState } from 'ant-design-vue/lib/table/interface'
import { IPage } from '/@/api/http/type'
import { Device, DOMAIN } from '/@/types/device'
import { getDeviceLogList, postDeviceUpgrade, DeviceLogFileInfo, UploadDeviceLogBody, DeviceLogItem } from '/@/api/device-log'
import { message } from 'ant-design-vue'
import { useDeviceLogUploadDetail } from './use-device-log-upload-detail'

const props = defineProps<{
  visible: boolean,
  device: null | Device,
}>()
const emit = defineEmits(['update:visible', 'upload-log-ok'])

const sVisible = ref(false)

watchEffect(() => {
  sVisible.value = props.visible
  // 显示弹框时，获取设备日志信息
  if (props.visible) {
    getDeviceLogInfo()
  }
})

function onVisibleChange (sVisible: boolean) {
  setVisible(sVisible)
  if (!sVisible) {
    resetTableLogState()
  }
}

function setVisible (v: boolean, e?: Event) {
  sVisible.value = v
  emit('update:visible', v, e)
}

// 表格
// const airportLogColumns: ColumnProps[] = [
//   { title: '机场日志', dataIndex: 'time', width: 100, slots: { customRender: 'log_time' } },
//   { title: '文件大小', dataIndex: 'size', width: 25, slots: { customRender: 'size' } },
// ]

// const droneLogColumns: ColumnProps[] = [
//   { title: '飞行器日志', dataIndex: 'time', width: 100, slots: { customRender: 'log_time' } },
//   { title: '文件大小', dataIndex: 'size', width: 25, slots: { customRender: 'size' } },
// ]

const airportTableLogState = reactive({
  logList: {} as DeviceLogFileInfo,
  tableLoading: false,
  selectRow: [] as any[],
  // rowSelection: {
  //   columnWidth: 15,
  //   selectedRowKeys: [] as number[],
  //   onChange: (selectedRowKeys:number[], selectedRows: []) => {
  //     airportTableLogState.rowSelection.selectedRowKeys = selectedRowKeys
  //     airportTableLogState.selectRow = selectedRows
  //     console.log(`selectedRowKeys: ${selectedRowKeys}`, 'selectedRows: ', selectedRows)
  //   },
  // }
})
function handledronechange (val:any[]) {
  // console.log('选中的数据', val)
  droneTableLogState.selectRow = val
}

function handleairportchange (val:any[]) {
  airportTableLogState.selectRow = val
}

function resetTableLogState () {
  airportTableLogState.logList = {} as DeviceLogFileInfo
  airportTableLogState.selectRow = []
  airportTableLogState.tableLoading = false
}

const droneTableLogState = reactive({
  logList: {} as DeviceLogFileInfo,
  tableLoading: false,
  selectRow: [] as any[],
  // rowSelection: {
  //   columnWidth: 15,
  //   selectedRowKeys: [] as number[],
  //   onChange: (selectedRowKeys: number[], selectedRows: []) => {
  //     droneTableLogState.rowSelection.selectedRowKeys = selectedRowKeys
  //     droneTableLogState.selectRow = selectedRows
  //     console.log(`selectedRowKeys: ${selectedRowKeys}`, 'selectedRows: ', selectedRows)
  //   },
  // }
})

const deviceLogUploadBtnDisabled = computed(() => {
  return (airportTableLogState.selectRow && airportTableLogState.selectRow.length <= 0) &&
  (droneTableLogState.selectRow && droneTableLogState.selectRow.length <= 0)
})
// const tt = [
//   {
//     device_sn: '1581F6Q8D23CM00A3Z3K',
//     list: [
//       {
//         boot_index: 431,
//         end_time: 1733472091,
//         start_time: 1733471985,
//         size: 2036161996
//       },
//       {
//         boot_index: 432,
//         end_time: 1733472091,
//         start_time: 1733471985,
//         size: 2036161996
//       },
//     ],
//     object_key: 'wayline/37df4a6e-c13d-44b6-b9b0-e430fc9beb3f.tar',
//     file_id: '2fb96942-862d-4555-a5f1-44481fa3acb2',
//     module: '0'
//   }
// ]
// 获取设备内日志
async function getDeviceLogInfo () {
  airportTableLogState.tableLoading = true
  droneTableLogState.tableLoading = true
  // const files = tt
  // files.forEach(file => {
  //   if (file.module === DOMAIN.DOCK) {
  //     airportTableLogState.logList = file
  //   } else if (file.module === DOMAIN.DRONE) {
  //     droneTableLogState.logList = file
  //   }
  // })
  try {
    const { code, data } = await getDeviceLogList({
      device_sn: props.device?.device_sn || '',
      domain: [DOMAIN.DOCK, DOMAIN.DRONE]
    })
    if (code === 0) {
      const { files } = data
      if (files && files.length > 0) {
        files.forEach(file => {
          if (file.module === DOMAIN.DOCK) {
            airportTableLogState.logList = file
          } else if (file.module === DOMAIN.DRONE) {
            droneTableLogState.logList = file
          }
        })
      }
    }
  } catch (err) {
  }
  airportTableLogState.tableLoading = false
  droneTableLogState.tableLoading = false
}

// 日志上传
async function uploadDeviceLog () {
  const body = {
    device_sn: props.device?.device_sn || '',
    files: [] as any
  } as UploadDeviceLogBody
  if (airportTableLogState.selectRow && airportTableLogState.selectRow.length > 0) {
    body.files.push({
      list: airportTableLogState.selectRow,
      device_sn: airportTableLogState.logList.device_sn,
      module: airportTableLogState.logList.module
    })
  }
  if (droneTableLogState.selectRow && droneTableLogState.selectRow.length > 0) {
    body.files.push({
      list: droneTableLogState.selectRow,
      device_sn: droneTableLogState.logList.device_sn,
      module: droneTableLogState.logList.module
    })
  }
  console.log('请求的数据', body)
  const { code } = await postDeviceUpgrade(body)
  if (code === 0) {
    message.success('日志上传任务执行成功')
    emit('upload-log-ok')
    setVisible(false)
  }
}

const { getLogTime, getLogSize } = useDeviceLogUploadDetail()

</script>

<style lang="scss" scoped>
.device-log-upload-wrap{

  .device-log-list{
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    .log-list-item{
      width: 420px;
    }
  }
}
:global(.ant-modal-header) {
  background-color: #0B2756;
  border-bottom: 1px solid #253653;
}
:global(.ant-modal-title) {
  color: white;
}
:global(.ant-modal-close-x) {
  color: white;
}
:global(.ant-modal-body) {
  background-color: #0B2756;
}
// a-table样式
// :global(.ant-table-fixed-header>.ant-table-content>.ant-table-scroll>.ant-table-body) {
//   background-color: transparent;
// }
// :global(.ant-table-header,
// .ant-table-thead tr th) {
//   background: rgb(0, 57, 154) !important;
//   color: white;
//   border: 0;
//   // border: 1px solid rgb(0, 57, 154);
// }

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
