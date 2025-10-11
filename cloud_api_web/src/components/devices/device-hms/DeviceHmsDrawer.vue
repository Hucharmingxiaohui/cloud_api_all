<template>
  <a-drawer
    title="Hms详情"
    placement="right"
    v-model:visible="sVisible"
    @update:visible="onVisibleChange"
    :destroyOnClose="true"
     wrapClassName="drawer-style"
    :width="800">
    <div class="flex-row flex-align-center" style="margin-bottom: 20px;">
      <div style="width: 240px;">
        <a-range-picker
          v-model:value="time"
          format="YYYY-MM-DD"
          :placeholder="['开始时间', '结束时间']"
          @change="onTimeChange"/>
      </div>
      <div class="ml5">
        <a-select
          style="width: 150px"
          v-model:value="param.level"
          @select="onLevelSelect">
          <a-select-option
            v-for="item in levels"
            :key="item.label"
            :value="item.value"
          >
            {{ item.label }}
          </a-select-option>
        </a-select>
      </div>
      <div class="ml5">
        <a-select
          v-model:value="param.domain"
          :disabled="!param.children_sn || !param.device_sn"
          style="width: 150px"
          @select="onDeviceTypeSelect">
          <a-select-option
            v-for="item in deviceTypes"
            :key="item.label"
            :value="item.value"
          >
            {{ item.label }}
          </a-select-option>
        </a-select>
      </div>
      <div class="ml5">
        <!-- <a-input-search
          v-model:value="param.message"
          placeholder="请输入查询内容"
          style="width: 200px"
          @search="getHms"/> -->

        <a-input v-model:value="param.message"  placeholder="请输入查询内容"  style="width: 200px">
          <template #suffix >
              <a @search="getHms">  <SearchOutlined style="color: rgba(255, 255, 255, 1)" /></a>
          </template>
        </a-input>
      </div>
    </div>
    <div>
      <el-table
          :data="hmsData.data"
          row-key="hms_id"
          default-expand-all
          show-overflow-tooltip
          stripe
        >
          <el-table-column
            prop="device_type"
            label="警告时间|结束时间"
            show-overflow-tooltip
          >
            <template #default="scope">
              <div>{{ scope.row.create_time }}</div>
              <div :style="scope.row.update_time ? '' : scope.row.level === EHmsLevel.CAUTION ? 'color: orange;' :
                scope.row.level === EHmsLevel.WARN ? 'color: red;' : 'color: #28d445;'">{{ scope.row.update_time ?? '正在发生...' }}</div>
            </template>
          </el-table-column>
          <el-table-column
            prop="level"
            label="层级"
            show-overflow-tooltip
          >
            <template #default="scope">
                <div :class="scope.row.level === EHmsLevel.CAUTION ? 'caution' : scope.row.level === EHmsLevel.WARN ? 'warn' : 'notice'" style="width: 10px; height: 10px; border-radius: 50%;"></div>
                <div style="margin-left: 3px;">{{ EHmsLevel_zn[scope.row.level] }}</div>
            </template>
          </el-table-column>
          <el-table-column
            prop="domain"
            label="设备"
            show-overflow-tooltip
          >
            <template #default="scope">
              <div >{{ EDeviceTypeName[scope.row.domain] }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="key" label="错误代码"  show-overflow-tooltip />
          <el-table-column prop="message_zh" label="信息" show-overflow-tooltip />
      </el-table>
      <div class="pagination-container">
        <el-pagination v-model:current-page="hmsPaginationProp.current" v-model:page-size="hmsPaginationProp.pageSize"
          :page-sizes="hmsPaginationProp.pageSizeOptions" :total="hmsPaginationProp.total"
          layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handleCurrentChange"></el-pagination>
      </div>
      <!-- <a-table :columns="hmsColumns"  :scroll="{ x: '100%', y: 600 }" :data-source="hmsData.data" :pagination="hmsPaginationProp" @change="refreshHmsData" row-key="hms_id"
        :rowClassName="rowClassName" :loading="loading">
        <template #time="{ record }">
          <div class="flex-row flex-align-center">
            <div>{{ record.create_time }}</div>
            <div :style="record.update_time ? '' : record.level === EHmsLevel.CAUTION ? 'color: orange;' :
              record.level === EHmsLevel.WARN ? 'color: red;' : 'color: #28d445;'">{{ record.update_time ?? '正在发生...' }}</div>
          </div>

        </template>
        <template #level="{ text }">
          <div style="display: flex; align-items: center;justify-content: center;">
            <div :class="text === EHmsLevel.CAUTION ? 'caution' : text === EHmsLevel.WARN ? 'warn' : 'notice'" style="width: 10px; height: 10px; border-radius: 50%;"></div>
            <div style="margin-left: 3px;">{{ EHmsLevel[text] }} {{ text }}</div>
          </div>
        </template>
        <template v-for="col in ['code', 'message']" #[col]="{ text }" :key="col">
          <a-tooltip :title="text">
              <div >{{ text }}</div>
          </a-tooltip>
        </template>
        <template #domain="{text}">
          <a-tooltip :title="EDeviceTypeName[text]">
              <div >{{ EDeviceTypeName[text] }}</div>
          </a-tooltip>
        </template>
      </a-table> -->
    </div>
  </a-drawer>
</template>

<!-- 暂时只抽取该组件 -->
<script lang="ts" setup>
import { watchEffect, reactive, ref, defineProps, defineEmits, watch } from 'vue'
import { getDeviceHms, HmsQueryBody } from '/@/api/manage'
import moment, { Moment } from 'moment'
import { ColumnProps, TableState } from 'ant-design-vue/lib/table/interface'
import { Device, DeviceHms } from '/@/types/device'
import { IPage } from '/@/api/http/type'
import { EllipsisOutlined, RocketOutlined, CameraFilled, UserOutlined, SelectOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { EDeviceTypeName, EHmsLevel, ELocalStorageKey } from '/@/types'

const props = defineProps<{
  visible: boolean,
  device: null | Device,
}>()
const emit = defineEmits(['update:visible', 'ok', 'cancel'])

const workspaceId: string = localStorage.getItem(ELocalStorageKey.WorkspaceId) || ''
// 健康状态
const sVisible = ref(false)

const EHmsLevel_zn = ['通知', '注意', '警告']
watch(props, () => {
  sVisible.value = props.visible
  // 显示弹框时，获取设备hms信息
  if (props.visible) {
    showHms()
  }
})

function onVisibleChange (sVisible: boolean) {
  setVisible(sVisible)
}

function setVisible (v: boolean, e?: Event) {
  sVisible.value = v
  emit('update:visible', v, e)
}

const loading = ref(false)

// const hmsColumns: ColumnProps[] = [
//   { title: 'Alarm Begin | End Time', dataIndex: 'create_time', width: '25%', className: 'titleStyle', slots: { customRender: 'time' } },
//   { title: 'Level', dataIndex: 'level', width: '120px', className: 'titleStyle', slots: { customRender: 'level' } },
//   { title: 'Device', dataIndex: 'domain', width: '12%', className: 'titleStyle', slots: { customRender: 'domain' } },
//   { title: 'Error Code', dataIndex: 'key', width: '20%', className: 'titleStyle', ellipsis: true, slots: { customRender: 'code' } },
//   { title: 'Hms Message', dataIndex: 'message_en', className: 'titleStyle', ellipsis: true, slots: { customRender: 'message' } },
//   { title: 'Hms Message', dataIndex: 'message_zh', className: 'titleStyle', ellipsis: true, slots: { customRender: 'message' } },
// ]

interface DeviceHmsData {
  data: DeviceHms[]
}

const hmsData = reactive<DeviceHmsData>({
  data: []
})

type Pagination = TableState['pagination']

const hmsPaginationProp = reactive({
  pageSizeOptions: ['10', '20', '40'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 10,
  current: 1,
  total: 0
})

// 获取分页信息
function getPaginationBody () {
  return {
    page: hmsPaginationProp.current,
    page_size: hmsPaginationProp.pageSize
  } as IPage
}

function showHms () {
  const dock = props.device
  if (!dock) return
  if (dock.domain === EDeviceTypeName.Dock) {
    getDeviceHmsBySn(dock.device_sn, dock.children?.[0].device_sn ?? '')
  }
  if (dock.domain === EDeviceTypeName.Aircraft) {
    param.domain = EDeviceTypeName.Aircraft
    getDeviceHmsBySn('', dock.device_sn)
  }
}

function refreshHmsData (page: Pagination) {
  hmsPaginationProp.current = page?.current!
  hmsPaginationProp.pageSize = page?.pageSize!
  getHms()
}

// 分页
function handleSizeChange (val: number) {
  // paginationProp.pageSize = val
  // refreshData(paginationProp)
  // paginationProp.current = 1 // 重置为第一页

  hmsPaginationProp.current = 1
  hmsPaginationProp.pageSize = val
  getHms()
}
function handleCurrentChange (val: number) {
  // deviceUploadLogState.paginationProp.pageSize = val
  hmsPaginationProp.current = val
  getHms()
}

const param = reactive<HmsQueryBody>({
  sns: [],
  device_sn: '',
  children_sn: '',
  language: 'en',
  begin_time: new Date(new Date().setDate(new Date().getDate() - 7)).setHours(0, 0, 0, 0),
  end_time: new Date().setHours(23, 59, 59, 999),
  domain: -1,
  level: '',
  message: ''
})

const levels = [
  {
    label: '所有',
    value: ''
  }, {
    label: '通知',
    value: EHmsLevel.NOTICE
  }, {
    label: '注意',
    value: EHmsLevel.CAUTION
  }, {
    label: '警告',
    value: EHmsLevel.WARN
  }
]

const deviceTypes = [
  {
    label: '所有',
    value: -1
  }, {
    label: '飞行器',
    value: EDeviceTypeName.Aircraft
  }, {
    label: '机场',
    value: EDeviceTypeName.Dock
  }
]

const rowClassName = (record: any, index: number) => {
  const className = []
  if ((index & 1) === 0) {
    className.push('table-striped')
  }
  if (record.domain !== EDeviceTypeName.Dock) {
    className.push('child-row')
  }
  return className.toString().replaceAll(',', ' ')
}

const time = ref([moment(param.begin_time), moment(param.end_time)])

function getHms () {
  loading.value = true
  getDeviceHms(param, workspaceId, getPaginationBody())
    .then(res => {
      hmsPaginationProp.total = res.data.pagination.total
      hmsPaginationProp.current = res.data.pagination.page
      hmsData.data = res.data.list
      console.log('HMS信息', hmsData.data)
      hmsData.data.forEach(hms => {
        hms.domain = hms.sn === param.children_sn ? EDeviceTypeName.Aircraft : EDeviceTypeName.Dock
      })
      loading.value = false
    }).catch(_err => {
      loading.value = false
    })
}

function getDeviceHmsBySn (sn: string, childSn: string) {
  param.device_sn = sn
  param.children_sn = childSn
  param.sns = [param.device_sn, param.children_sn]
  getHms()
}

function onTimeChange (newTime: [Moment, Moment]) {
  param.begin_time = newTime[0].valueOf()
  param.end_time = newTime[1].valueOf()
  getHms()
}

function onDeviceTypeSelect (val: number) {
  param.sns = [param.device_sn, param.children_sn]
  if (val === EDeviceTypeName.Dock) {
    param.sns = [param.device_sn, '']
  }
  if (val === EDeviceTypeName.Aircraft) {
    param.sns = ['', param.children_sn]
  }
  getHms()
}

function onLevelSelect (val: number) {
  param.level = val
  getHms()
}
</script>
<style lang="scss" scoped>
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
:deep(.ant-input){
  background-color:rgba(0, 0, 0, 0.5);
  color:#fff;
  border:1px solid #719fff;
}
:deep(.ant-select:not(.ant-select-customize-input) .ant-select-selector ) {
  background-color:rgba(0, 0, 0, 0.5);
  color:#fff;
  border:1px solid #719fff;
}
:deep(.ant-input-affix-wrapper ){
  background-color:rgba(0, 0, 0, 0.5);
  color:#fff;
  border:1px solid #719fff;
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
