
<template>
  <div class="container">
    <div class="operation">
      <div class="flex-row">
        <a-select style="width: 150px" v-model:value="param.firmware_status" class="select-operation"
          @select="getAllFirmwares(pageParam)">
          <a-select-option v-for="(key, value) in FirmwareStatusEnum" :key="key" :value="value">
            {{ key }}
          </a-select-option>
        </a-select>
        <a-select style="width: 150px" v-model:value="param.device_name" class="select-operation"
          @select="getAllFirmwares(pageParam)">
          <a-select-option v-for="item in deviceNameList" :key="item.label" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
        <a-input-search :enter-button="true" v-model:value="param.product_version" class="custom-select"
          placeholder="请输入版本" style="width: 250px" @search="getAllFirmwares(pageParam)" />
        <el-button class="btn iconfont icon-shangchuan" type="primary" @click="sVisible = true">
          <span style="margin-left: 5px; font-size: 14px;">点击上传</span>
        </el-button>
        <el-dialog title="导入固件文件" v-model="sVisible" width="30%" center>
          <a-form :rules="rules" ref="formRef" :model="uploadParam" :label-col="{ span: 6 }">
            <a-form-item name="status" label="可用的" required>
              <a-switch v-model:checked="uploadParam.status" />
            </a-form-item>
            <a-form-item name="device_name" label="设备名称" required>
              <a-select
                style="width: 220px"
                mode="multiple"
                placeholder="can choose multiple"
                v-model:value="uploadParam.device_name">
                <a-select-option
                  v-for="k in DeviceNameEnum"
                  :key="k"
                  :value="k"
                >
                  {{ k }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item name="release_note" label="版本说明" required>
              <a-textarea v-model:value="uploadParam.release_note" showCount :maxlength="300" />
            </a-form-item>
            <a-form-item label="文件" required>
              <a-upload
                :multiple="false"
                :before-upload="beforeUpload"
                :show-upload-list="true"
                :file-list="fileList"
                :remove="removeFile"
              >
                <a-button type="primary">
                  <UploadOutlined />
                  导入固件文件
                </a-button>
              </a-upload>
            </a-form-item>
          </a-form>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="onCancel" class="nobtn">取 消</el-button>
              <el-button type="primary" @click="uploadFile">确 定</el-button>
            </span>
          </template>
        </el-dialog>
      </div>
    </div>
    <div class="content">
      <div class="table-container">
        <el-table :data="data.firmware" stripe row-key="firmware_id">
          <el-table-column label="序号" type="index" width="60" />
          <el-table-column label="模型">
            <template #default="scope">
              <div v-for="text in scope.row.device_name" :key="text">
                {{ text }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="file_name" label="文件名称" show-overflow-tooltip />
          <el-table-column prop="product_version" label="固件版本" show-overflow-tooltip />
          <el-table-column prop="username" label="创建人" show-overflow-tooltip />
          <el-table-column label="发布日期" :sortable="true" prop="released_time" show-overflow-tooltip></el-table-column>
          <el-table-column label="版本说明" prop="release_note" show-overflow-tooltip></el-table-column>
          <el-table-column label="状态">
            <template #default="scope">
              <div>
                <DeviceFirmwareStatus :firmware="scope.row" />
              </div>
            </template>
          </el-table-column>
        </el-table>

      </div>
      <div class="pagination-container">
        <!-- 分页 -->
        <el-pagination v-model:current-page="paginationProp.current" v-model:page-size="paginationProp.pageSize"
          :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
          layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handleCurrentChange">
        </el-pagination>
      </div>

    </div>
  </div>

  <!-- <div class="ml20 mt20 mr20 flex-row flex-align-center flex-justify-between">
  </div>
  <div class="table flex-display flex-column">
    <a-table :columns="columns" :data-source="data.firmware" :pagination="paginationProp" @change="refreshData"
      row-key="firmware_id" :rowClassName="(record, index) => ((index % 2) === 0 ? 'table-striped' : null)"
      :scroll="{ x: '100%', y: 600 }">
      <template #device_name="{ record }">
        <div v-for="text in record.device_name" :key="text">
          {{ text }}
        </div>
      </template>
      <template #file_size="{ record }">
        <div>{{ bytesToSize(record.file_size) }}</div>
      </template>
      <template #firmware_status="{ record }">
        <DeviceFirmwareStatus :firmware="record" />
      </template>
      <template v-for="col in ['file_name', 'release_note']" #[col]="{ text }" :key="col">
        <a-tooltip :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
      </template>

    </a-table>
  </div> -->
</template>
<script lang="ts" setup>
import { message, notification, PaginationProps } from 'ant-design-vue'
import { TableState } from 'ant-design-vue/lib/table/interface'
import { onMounted, reactive, Ref, ref, UnwrapRef } from 'vue'
import { IPage } from '/@/api/http/type'
import { getFirmwares, importFirmareFile } from '/@/api/manage'
import DeviceFirmwareStatus from '/@/components/devices/DeviceFirmwareStatus.vue'
import { ELocalStorageKey } from '/@/types'
import { UploadOutlined } from '@ant-design/icons-vue'
import { Firmware, FirmwareQueryParam, FirmwareStatusEnum, DeviceNameEnum, FirmwareUploadParam } from '/@/types/device-firmware'
import { commonColor } from '/@/utils/color'
import { bytesToSize } from '/@/utils/bytes'
import moment from 'moment'

interface FirmwareData {
  firmware: Firmware[]
}
const columns = [
  { title: '模型', dataIndex: 'device_name', width: 120, ellipsis: true, className: 'titleStyle', slots: { customRender: 'device_name' } },
  { title: '文件名称', dataIndex: 'file_name', width: 220, ellipsis: true, className: 'titleStyle', slots: { customRender: 'file_name' } },
  { title: '固件版本', dataIndex: 'product_version', width: 180, className: 'titleStyle' },
  { title: '文件大小', dataIndex: 'file_size', width: 150, className: 'titleStyle', slots: { customRender: 'file_size' } },
  { title: '创建人', dataIndex: 'username', width: 100, className: 'titleStyle' },
  { title: '发布日期', dataIndex: 'released_time', width: 160, sorter: (a: Firmware, b: Firmware) => a.released_time.localeCompare(b.released_time), className: 'titleStyle' },
  { title: '版本说明', dataIndex: 'release_note', width: 300, ellipsis: true, className: 'titleStyle', slots: { customRender: 'release_note' } },
  { title: '状态', dataIndex: 'firmware_status', width: 100, className: 'titleStyle', slots: { customRender: 'firmware_status' } },
]

const data = reactive<FirmwareData>({
  firmware: []
})

const paginationProp = reactive({
  pageSizeOptions: ['20', '50', '100'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 50,
  current: 1,
  total: 0
})

const deviceNameList = ref<any[]>([{ label: '所有', value: '' }])

type Pagination = TableState['pagination']

const pageParam: IPage = {
  page: 1,
  total: 0,
  page_size: 50
}
const workspaceId: string = localStorage.getItem(ELocalStorageKey.WorkspaceId)!

const param = reactive<FirmwareQueryParam>({
  product_version: '',
  device_name: '',
  firmware_status: FirmwareStatusEnum.NONE
})

onMounted(() => {
  getAllFirmwares(pageParam)
  for (const key in DeviceNameEnum) {
    const value = DeviceNameEnum[key]
    deviceNameList.value.push({ label: value, value: value })
  }
})

// function refreshData (page: Pagination) {
//   pageParam.page = page?.current!
//   pageParam.page_size = page?.pageSize!
//   getAllFirmwares(pageParam)
// }
// 分页事件
function handleSizeChange (val: number) {
  pageParam.page_size = val
  getAllFirmwares(pageParam)
}
function handleCurrentChange (val: number) {
  pageParam.page = val
  getAllFirmwares(pageParam)
}

function getAllFirmwares (page: IPage) {
  getFirmwares(workspaceId, page, param).then(res => {
    const firmwareList: Firmware[] = res.data.list
    data.firmware = firmwareList
    paginationProp.total = res.data.pagination.total
    paginationProp.current = res.data.pagination.page
  })
}

const sVisible = ref(false)
const uploadParam = reactive<FirmwareUploadParam>({
  device_name: [],
  release_note: '',
  status: true
})

const rules = {
  status: [{ required: true }],
  release_note: [{ required: true, message: 'Please input release note.' }],
  device_name: [{ required: true, message: 'Please select which models this firmware belongs to.' }]
}
interface FileItem {
  uid: string;
  name?: string;
  status?: string;
  response?: string;
  url?: string;
}

interface FileInfo {
  file: FileItem;
  fileList: FileItem[];
}
const fileList = ref<FileItem[]>([])

function beforeUpload (file: FileItem) {
  if (!file.name || !file.name?.endsWith('.zip')) {
    message.error('Format error. Please select zip file.')
    return false
  }
  fileList.value = [file]
  return false
}

const formRef = ref()
function removeFile (file: FileItem) {
  fileList.value = []
}
function onCancel () {
  formRef.value.resetFields()
  fileList.value = []
  sVisible.value = false
}

const uploadFile = async () => {
  if (fileList.value.length === 0) {
    message.error('Please select at least one file.')
  }
  let uploading: string
  formRef.value.validate().then(async () => {
    const file: FileItem = fileList.value[0]
    const fileData = new FormData()
    fileData.append('file', file as any, file.name)
    Object.keys(uploadParam).forEach((key) => {
      const val = uploadParam[key as keyof FirmwareUploadParam]
      if (val instanceof Array) {
        val.forEach((value) => {
          fileData.append(key, value)
        })
      } else {
        fileData.append(key, val.toString())
      }
    })
    const timestamp = new Date().getTime()
    uploading = (file.name ?? 'uploding') + timestamp
    notification.open({
      key: uploading,
      message: `Uploading  ${moment().format()}`,
      description: `[${file.name}] is uploading... `,
      duration: null
    })
    importFirmareFile(workspaceId, fileData).then((res) => {
      if (res.code === 0) {
        notification.success({
          message: `Uploaded  ${moment().format()}`,
          description: `[${file.name}] file uploaded successfully. Duration: ${moment.duration(new Date().getTime() - timestamp).asSeconds()}`,
          duration: null
        })
        getAllFirmwares(pageParam)
      } else {
        notification.error({
          message: `Failed to upload [${file.name}]. Check and try again.`,
          description: `Error message: ${res.message} ${moment().format()}`,
          style: { color: commonColor.FAIL },
          duration: null,
        })
      }
    }).finally(() => {
      notification.close(uploading)
    })
    fileList.value = []
    formRef.value.resetFields()
    sVisible.value = false
  })
}

</script>
<style lang="scss" scoped>
::v-deep .ant-form-item-label > label{
  color: white !important;
}
:deep(.el-dialog__title){
  color: white !important;
}
// ::v-deep .ant-modal-header{
//   background: #00399A;
// }
:deep(.el-dialog) {
  background-color: #0B2756 !important;
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

  .flex-row {
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
      width: 56px;
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

::v-deep .ant-input {
  color: white;
}

.content {
  margin: 15px 12px 0 12px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

.table-container {
  flex-grow: 1;
  overflow: hidden;
  // height: 500px;
  overflow-y: auto;
}

.ant-select-single:not(.ant-select-customize-input) .ant-select-selector {
  padding: 5px;
  margin-right: 5px;
}

// =========================================
.btn {
  background-image: linear-gradient(180deg,
      rgba(70, 145, 217, 1) 0,
      rgba(21, 81, 181, 1) 100%);
  border-radius: 4px;
}

:deep(.ant-input) {
  background-color: #0B2756;
  border: 1px solid #1d4292;
}

:deep(.ant-select:not(.ant-select-customize-input) .ant-select-selector) {
  background-color: #0B2756;
  border: 1px solid #1d4292;
  color: white;
}

::v-deep .el-table tr {
  background-color: #011C4B !important;
  /* opacity: 0.6; */
  color: #F1F6FF;
  font-weight: bold;
}

// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #2264a7;
}

/* // 修改高亮当前行颜色 */
::v-deep .el-table tbody tr:hover>td {
  background: rgba(0, 114, 245, 0.6) !important;
}

/* // 斑马线颜色 */

::v-deep .el-table--striped .el-table__body tr.el-table__row--striped td {
  background: rgba(0, 45, 120, 1);
}

::v-deep .el-table td {
  border: 2px solid #01123288
    /* 设置列的边框颜色和粗细 */
}

//分页数据
/* 修改前后箭头未点击时的背景颜色 */
:deep .el-pagination .btn-prev,
:deep .el-pagination .btn-next {
  background-color: #062254 !important;
  color: #fff;
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

::v-deep .ant-select-dropdown.ant-select-dropdown-placement-bottomLeft {
  background-color: #00399A;
  color: white;
}

::v-deep .ant-select-item-option-selected:not(.ant-select-item-option-disabled) {
  background-color: #00399A;
  color: white;
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

// 表格最后一条白线
:deep .el-table__inner-wrapper::before {
  height: 0;
}

::v-deep .el-table td {
  border: 2px solid #01123288;
  /* 设置列的边框颜色和粗细 */
  font-size: 16px;
  font-weight: 500;
}

// 表格样式
::v-deep .el-table {
  .cell {
    text-align: center;
  }
}

// // 表头大小
::v-deep .el-table th {
  height: 50px;
  font-size: 16px !important;
  /* 如果你需要修改表头字体大小，设置一个不同的大小 */
  color: rgba(255, 255, 255, 1);
  background-color: #00399A;
  border-left: 2px #01123288 solid;
  border-bottom: 2px #01123288 solid !important;
}
</style>
