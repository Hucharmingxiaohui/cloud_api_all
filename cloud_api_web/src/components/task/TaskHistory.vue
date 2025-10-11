<template>
  <div class="container">
    <div class="header1">
      <div>历史任务</div>
        <div>
            <el-breadcrumb :separator-icon="icon" >
                <el-breadcrumb-item :to="{ path: '/task' }" class="breadcrumb-item">任务管理</el-breadcrumb-item>
                <el-breadcrumb-item class="breadcrumb-item">历史任务</el-breadcrumb-item>
             </el-breadcrumb>
        </div>
    </div>
    <div class="operation" >
      <div class="item1">
          <span style="width: auto; margin-right: 10px">任务名称:</span>
          <el-input style="width: 150px;" placeholder="请输入任务名称" ></el-input>
          <span style="width: auto; margin:0 10px">时间范围:</span>
          <el-date-picker v-model="selected_time" type="datetimerange" unlink-panels range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYYMMDDHHmmss" ></el-date-picker>
          <!-- <el-input style="width: 150px;" placeholder="请输入任务时间" ></el-input> -->

          <!-- <el-time-picker v-model="selected_time" is-range  type="datetimerange" range-separator="-"  start-placeholder="开始时间" end-placeholder="结束时间"  value-format="YYYYMMDDHHmmss"  /> -->
          <!-- <el-input  placeholder="请输入历史" ></el-input> -->
      </div>
      <div class="item1">
        <el-button class="btn" @click="queryTask">查询任务</el-button>
          <!-- <router-link to="/task/TaskPanel">
            <el-button class="btn" type="primary" style="margin-left: 10px" >任务管理</el-button>
          </router-link> -->
      </div>
    </div>
    <div class="tablelw" >
      <el-table :data="filteredPlansData" :row-key="row => row.id"
        :header-cell-style="{color: '#fff', fontSize: '16px', backgroundColor: '#003896', borderLeft: '0.5px #154480 solid', borderBottom: '1px #154480 solid' }"
        :cell-style="{borderBottom: '0.5px #143275 solid',background: '#002D78', borderLeft: '0.5px #143275 solid' ,color:'#DCDFE5'}">
        <el-table-column label="任务时间">
          <template #default="scope">
            <div class="flex-row" style="white-space: pre-wrap">
              <div>
                <div>{{ formatTaskTime(scope.row.begin_time) }}</div>
                <div>{{ formatTaskTime(scope.row.end_time) }}</div>
              </div>
              <div class="ml10">
                <div>{{ formatTaskTime(scope.row.execute_time) }}</div>
                <div>{{ formatTaskTime(scope.row.completed_time) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="scope">
            <div>
              <div class="flex-display flex-align-center">
                <span class="circle-icon" :style="{backgroundColor: formatTaskStatus(scope.row).color}"></span>
                {{ formatTaskStatus(scope.row).text }}
                <a-tooltip v-if="!!scope.row.code" placement="bottom" arrow-point-at-center >
                  <template #title>
                  <div>{{ getCodeMessage(scope.row.code) }}</div>
                  </template>
                  <exclamation-circle-outlined class="ml5" :style="{color: commonColor.WARN, fontSize: '16px' }"/>
                </a-tooltip>
              </div>
              <div v-if="scope.row.status === TaskStatus.Carrying">
                <a-progress :percent="scope.row.progress || 0" />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="任务名称">
          <template #default="scope">
            <div>{{ scope.row.job_name }}</div>
          </template>
        </el-table-column>
        <el-table-column label="任务类型">
          <template #default="scope">
            <div>{{ scope.row.task_type }}</div>
          </template>
        </el-table-column>
        <el-table-column label="航线名称">
          <template #default="scope">
            <div>{{ scope.row.file_name }}</div>
          </template>
        </el-table-column>
        <el-table-column label="机场名称">
          <template #default="scope">
            <div>{{ scope.row.dock_name }}</div>
          </template>
        </el-table-column>
        <el-table-column label="RTH相对于机场的高度(m)">
          <template #default="scope">
            <div>{{ scope.row.rth_altitude }}</div>
          </template>
        </el-table-column>
        <el-table-column label="失联动作">
          <template #default="scope">
            <div>{{ scope.row.out_of_control_action }}</div>
          </template>
        </el-table-column>
        <el-table-column label="用户">
          <template #default="scope">
            <div>{{ scope.row.username }}</div>
          </template>
        </el-table-column>
        <el-table-column label="媒体文件上传">
          <template #default="scope">
            <div>
              <div class="flex-display flex-align-center">
                <span class="circle-icon" :style="{backgroundColor: formatMediaTaskStatus(scope.row).color}"></span>
                {{ formatMediaTaskStatus(scope.row).text }}
              </div>
              <div class="pl15">
                {{ formatMediaTaskStatus(scope.row).number }}
                <a-tooltip v-if="formatMediaTaskStatus(scope.row).status === MediaStatus.ToUpload" placement="bottom" arrow-point-at-center >
                  <template #title>
                  <div>Upload now</div>
                  </template>
                  <UploadOutlined class="ml5" :style="{color: commonColor.BLUE, fontSize: '16px' }"  @click="onUploadMediaFileNow(scope.row.job_id)"/>
                </a-tooltip>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <!-- <el-popconfirm v-if="scope.row.status === TaskStatus.Wait" width="220" confirm-button-text="确定" cancel-button-text="取消"
                icon-color="#626AEF" title="你确定要查看任务详情吗？" @confirm="() => goToTaskImages(scope.row.job_id)">
                <template #reference>
                    <el-button size="small" type="text">详情</el-button>
                </template>
            </el-popconfirm> -->

            <!-- <el-popconfirm v-if="scope.row.status === TaskStatus.Carrying" width="220" confirm-button-text="确定" cancel-button-text="取消"
              icon-color="#626AEF" title="你确定要挂起飞行任务吗？" @confirm="onSuspendTask(scope.row.job_id)">
              <template #reference>
                <el-button size="small" type="text">挂起</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm v-if="scope.row.status === TaskStatus.Paused" width="220" confirm-button-text="确定" cancel-button-text="取消"
              icon-color="#626AEF" title="你确定要继续吗？" @confirm="onResumeTask(scope.row.job_id)">
              <template #reference>
                <el-button size="small" type="text">继续</el-button>
              </template>
            </el-popconfirm> -->
            <el-button size="small" type="text" @click="toTaskResults(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div style="margin-top: 15px">
          <!-- 分页 -->
          <el-pagination
              v-model:current-page="paginationProp.current"
              v-model:page-size="paginationProp.pageSize"
              :page-sizes="paginationProp.pageSizeOptions"
              :total="paginationProp.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
         ></el-pagination>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from '@vue/reactivity'
import { message } from 'ant-design-vue'
import { TableState } from 'ant-design-vue/lib/table/interface'
import { onMounted, computed } from 'vue'
import { IPage } from '/@/api/http/type'
import { deleteTask, updateTaskStatus, UpdateTaskStatus, getWaylineJobs, Task, uploadMediaFileNow, getTaskResult } from '/@/api/wayline'
import { useMyStore } from '/@/store'
import { ELocalStorageKey, ERouterName } from '/@/types/enums'
import { useFormatTask } from './use-format-task'
import { TaskStatus, TaskProgressInfo, TaskProgressStatus, TaskProgressWsStatusMap, MediaStatus, MediaStatusProgressInfo, TaskMediaHighestPriorityProgressInfo } from '/@/types/task'
import { useTaskWsEvent } from './use-task-ws-event'
import { getErrorMessage } from '/@/utils/error-code/index'
import { commonColor } from '/@/utils/color'
import { ExclamationCircleOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
const router = useRouter()
const store = useMyStore()
const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!

const body: IPage = {
  page: 1,
  total: 0,
  page_size: 10
}
const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '50'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 10,
  current: 1,
  total: 0
})

type Pagination = TableState['pagination']
const customFooter = null
const plansData = reactive({
  data: [] as Task[]
})
const open = ref<boolean>(false)
const { formatTaskType, formatTaskTime, formatLostAction, formatTaskStatus, formatMediaTaskStatus } = useFormatTask()
// 分页事件
function handleSizeChange (val:number) {
  paginationProp.pageSize = val
  refreshData(paginationProp)
}
function handleCurrentChange (val:number) {
  paginationProp.current = val
  refreshData(paginationProp)
}
function refreshData (page: Pagination) {
  body.page = page?.current!
  body.page_size = page?.pageSize!
  getPlans()
}
// 设备任务执行进度更新
function onTaskProgressWs (data: TaskProgressInfo) {
  const { bid, output } = data
  if (output) {
    const { status, progress } = output || {}
    const taskItem = plansData.data.find(task => task.job_id === bid)
    if (!taskItem) return
    if (status) {
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
// 跳出新建任务弹窗
function createTask () {
  open.value = true
  console.log(open.value)
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

function getCodeMessage (code: number) {
  return getErrorMessage(code) + `（code: ${code}）`
}

useTaskWsEvent({
  onTaskProgressWs,
  onTaskMediaProgressWs,
  onoTaskMediaHighestPriorityWS,
})

onMounted(() => {
  getPlans()
})
// 获取数据信息
function goToTaskImages (jobId: string) {
  router.push({
    path: '/task/taskImages',
    query: { job_id: jobId }
  })
}

function getPlans () {
  getWaylineJobs(workspaceId, body).then(res => {
    if (res.code !== 0) {
      return
    }
    plansData.data = res.data.list
    paginationProp.total = res.data.pagination.total
    paginationProp.current = res.data.pagination.page
  })
}

// 任务查询结果
const selected_time = ref('')
function queryTask () {
  console.log('选择时间', selected_time.value)
}

// 过滤数据，只有当任务具有任务执行结束的时间时，显示
const filteredPlansData = computed(() => {
  return plansData.data.filter(item => item.execute_time)
  //  return plansData.data.filter(item => item.completed_time);
})

// 获取任务执行结果
// const job_id = '2a9e063d-5b35-47ab-ad01-b48b51073e4c'
const job_id = '2a9e063d-5b35-47ab-ad01-b48b51073e4c'
// taskData.job_i
function toTaskResults (taskData:any) {
  // const start_time = taskData.execute_time
  const start_time = '2024-06-28 14:23:11'
  const end_time = '2024-06-28 14:23:25'
  // getTaskResult(taskData.workspace_id, job_id, new Date(start_time).getTime(), new Date(end_time).getTime()).then(res => {
  //   if (res.code !== 0) {
  //     return
  //   }
  getTaskResult(taskData.workspace_id, job_id).then(res => {
    if (res.code !== 0) {
      return
    }

    // const dateStr = "2024-08-21 10:30:42";
    // // console.log('ssd', res.data)
    // store.commit('SET_IMGS', data)
    const data = res.data
    // if (data.length === 0) {

    // }
    // ElMessage({
    //   message: '暂无数据.',
    //   type: 'warning',
    // })
    // return
    localStorage.setItem('TaskResult', JSON.stringify(data))
    // router.push({ path: '/task/taskImages', query: { data: JSON.stringify(data) } })
    router.push({ path: '/task/taskImages' })
  })
}

// 删除任务
async function onDeleteTask (jobId: string) {
  const { code } = await deleteTask(workspaceId, {
    job_id: jobId
  })
  if (code === 0) {
    message.success('Deleted successfully')
    getPlans()
  }
}

// 挂起任务
async function onSuspendTask (jobId: string) {
  const { code } = await updateTaskStatus(workspaceId, {
    job_id: jobId,
    status: UpdateTaskStatus.Suspend
  })
  if (code === 0) {
    message.success('Suspended successfully')
    getPlans()
  }
}

// 解除挂起任务
async function onResumeTask (jobId: string) {
  const { code } = await updateTaskStatus(workspaceId, {
    job_id: jobId,
    status: UpdateTaskStatus.Resume
  })
  if (code === 0) {
    message.success('Resumed successfully')
    getPlans()
  }
}

// 立即上传媒体
async function onUploadMediaFileNow (jobId: string) {
  const { code } = await uploadMediaFileNow(workspaceId, jobId)
  if (code === 0) {
    message.success('Upload Media File successfully')
    getPlans()
  }
}
</script>

<style lang="scss" scoped>
.container {
  // height: 100%;
  width: 100vw;
  padding: 10px;
  display: flex;
  flex-direction: column; /* 使子元素垂直排列 */
}
// 头部  标题 面包屑
// .header1 {
//   width: 100%;
//   height: 60px;
//   background: #05204B;
//   padding: 16px;
//   font-size: 20px;
//   font-weight: bold;
//   text-align: start;
//   color:aliceblue;
// }
.header1 {
  width: 100%;
  height: 60px;
  background: #05204B;
  padding: 16px 26px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color:aliceblue;
  display: flex;
  justify-content: space-between;
}
// 操作部分
.operation{
 height: fit-content;
 padding: 15px;
 display: flex;
 justify-content: space-between;
 color: rgba(255, 255, 255, 0.762);
 background-color: rgba(0, 112, 209, 0.2);
 font-size: 16px;
 border: 4px solid rgba(0, 112, 209, 1);
 border-bottom: none;
 border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
 .item1{
  display: flex;
  justify-items: center;
  align-items: center;
 }
}
// 输入框
:deep(.el-input) {
    --el-input-border-color: #1d4292;
}
:deep(.el-input__wrapper) {
    background-color:#0B2756;
  }
:deep(.el-select__wrapper){
  background-color:#0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: aliceblue;
}
// 时间选择器
:deep(.el-date-editor.el-input__wrapper){
  box-shadow: 0 0 0 1px #1d4292;
}
:deep(.el-picker-panel){
  background: #012b78;
  color: #ffffff;
  border: 1px solid #719fff;
}
// 按钮
.btn{
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color:rgba(255, 255, 255, 0.762);
}
// 表格 无数据内容背景设置
:deep(.el-table__empty-block){
   background-color: #0A2D63;
}
// 表格最后一条白线
:deep .el-table__inner-wrapper::before{
    height: 0;
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
.header {
  width: 100%;
  height: 60px;
  background: #fff;
  padding: 16px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color: #000;
}
/* 使用 ::v-deep 确保深层选择器应用 */
::v-deep .custom-drawer .ant-drawer-header {
  background-color: black; /* 头部背景颜色 */
  color: white; /* 头部文字颜色 */
}
.tablelw{
// margin: 0;
  // padding: 16px;
  max-height: 600px;
  overflow-y: auto
}
// 面包屑
.breadcrumb-item :deep(.el-breadcrumb__inner) {
    color:aliceblue ;
    font-size: 20px;
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
::v-deep el-pager{
  background-color:#0B2756;
}
</style>
