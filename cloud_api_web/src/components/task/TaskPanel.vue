<template>
  <div class="container">
    <!-- <div class="header1">任务管理</div> -->
    <div class="operation">
      <span class="label">航线名称:</span>
      <el-input placeholder="请输入文件名称" class="custom-select" style="width: 200px;"></el-input>
      <el-button class="new_btn1" type="primary" style="margin-left: 10px" >
        <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/search.png" />
        <span class="btn_text">查询</span>
      </el-button>

    </div>
    <div class="content">
      <div class="table-container">
          <el-table :data="plansData.data" stripe
            :header-cell-style="{ height: '43px', color: 'rgba(255, 255, 255, 1)',fontSize: '16px', fontWeight: 'bold', backgroundColor: '#00399A',  borderLeft: '2px #01123288 solid', borderBottom: '1px #154480 solid' }">
            <!-- <el-table-column  label="序号" align='center' width="60">
                <template #default="scope">
                      {{ scope.$index+1 }}
                </template>
            </el-table-column> -->
            <el-table-column label="序号" align='center' width="60">
              <template #default="scope">
                {{ scope.$index +(paginationProp.current - 1) * paginationProp.pageSize+ 1 }}
              </template>
            </el-table-column>

            <el-table-column label="任务时间" width="300px;">
              <template #default="scope">
                <div class="flex-row" style="white-space: pre-wrap; justify-content: center;">
                  <!-- <div>
                    <div>{{ formatTaskTime(scope.row.begin_time) }}</div>
                    <div>{{ formatTaskTime(scope.row.end_time) }}</div>
                  </div> -->
                  <div class="ml10">
                    <div>{{ formatTaskTime(scope.row.execute_time) }}</div>
                    <div>{{ formatTaskTime(scope.row.completed_time) }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" >
              <template #default="scope">
                <div>
                  <div class="flex-display " style="justify-content: center;">
                    <span class="circle-icon" :style="{ backgroundColor: formatTaskStatus(scope.row).color }"></span>
                    {{ taskStatusLabels[formatTaskStatus(scope.row).text] }}
                    <a-tooltip v-if="!!scope.row.code" placement="bottom" arrow-point-at-center>
                      <template #title>
                        <div>{{ getCodeMessage(scope.row.code) }}</div>
                      </template>
                      <exclamation-circle-outlined class="ml5" :style="{ color: commonColor.WARN, fontSize: '16px' }" />
                    </a-tooltip>
                  </div>
                  <div v-if="scope.row.status === TaskStatus.Carrying">
                    <a-progress :percent="scope.row.progress || 0" />
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="任务名称" show-overflow-tooltip="true">
              <template #default="scope">
                <div class="ellipsis">{{ scope.row.job_name }}</div>
              </template>
            </el-table-column>
            <el-table-column label="任务类型">
              <template #default="scope">
                <div>{{ taskTypeLabels[scope.row.task_type] }}</div>
              </template>
            </el-table-column>
            <el-table-column label="航线名称" show-overflow-tooltip="true">
              <template #default="scope">
                <div class="ellipsis">{{ scope.row.file_name }}</div>
              </template>
            </el-table-column>
            <el-table-column label="机场名称" show-overflow-tooltip="true">
              <template #default="scope">
                <div>{{ scope.row.dock_name }}</div>
              </template>
            </el-table-column>
            <el-table-column label="RTH相对于机场的高度(m)">
              <template #default="scope">
                <div>{{ scope.row.rth_altitude }}</div>
              </template>
            </el-table-column>
            <el-table-column label="失联动作" show-overflow-tooltip="true">
              <template #default="scope">
                <div>{{ outControlAcion[scope.row.out_of_control_action]  }}</div>
              </template>
            </el-table-column>
            <el-table-column label="用户" show-overflow-tooltip="true">
              <template #default="scope">
                <div>{{ scope.row.username }}</div>
              </template>
            </el-table-column>
            <el-table-column label="媒体文件上传" >
              <template #default="scope">
                <div>
                  <div class="flex-display flex-align-center">
                    <span class="circle-icon"
                      :style="{ backgroundColor: formatMediaTaskStatus(scope.row).color }"></span>
                    {{ formatMediaTaskStatus(scope.row).text }}
                  </div>
                  <div class="pl15">
                    {{ formatMediaTaskStatus(scope.row).number }}
                    <a-tooltip v-if="formatMediaTaskStatus(scope.row).status === MediaStatus.ToUpload"
                      placement="bottom" arrow-point-at-center>
                      <template #title>
                        <div>立即上传</div>
                      </template>
                      <UploadOutlined class="ml5" :style="{ color: commonColor.BLUE, fontSize: '16px' }"
                        @click="onUploadMediaFileNow(scope.row.job_id)" />
                    </a-tooltip>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="scope">
                 <el-button size="small" link type="primary" class="preview" @click="anaysisTaskResult(scope.row)">分析</el-button>
                <el-popconfirm v-if="scope.row.status === TaskStatus.Wait " width="220" confirm-button-text="确定"
                  cancel-button-text="取消" icon-color="#626AEF" title="你确定要删除飞行任务吗？"
                  @confirm="onDeleteTask(scope.row.job_id)">
                  <template #reference>
                    <el-button size="small" link type="primary" class="preview">删除</el-button>
                  </template>
                </el-popconfirm>
                <el-popconfirm  v-if="scope.row.status === TaskStatus.Success || scope.row.status === TaskStatus.Fail || scope.row.status === TaskStatus.CanCel"  width="220" confirm-button-text="确定"
                  cancel-button-text="取消" icon-color="#626AEF" title="你确定要删除飞行任务吗？"
                  @confirm="onDeleteOtherTask(scope.row.job_id)">
                  <template #reference>
                    <el-button size="small" link type="primary" class="preview">删除</el-button>
                  </template>
                </el-popconfirm>
                <!-- <el-button size="small" type="text" @click="toTaskVideo(scope.row)">任务直播</el-button> -->
                <!-- <el-button size="small" type="text" @click="toTaskResult(scope.row)"> 任务结果 </el-button> -->
                <el-button size="small" link type="primary" class="preview" @click="toTaskResult(scope.row)">结果</el-button>
                <el-popconfirm v-if="scope.row.status === TaskStatus.Carrying" width="220" confirm-button-text="确定"
                  cancel-button-text="取消" icon-color="#626AEF" title="你确定要挂起飞行任务吗？"
                  @confirm="onSuspendTask(scope.row.job_id)">
                  <template #reference>
                    <el-button size="small" link type="primary" class="preview">挂起</el-button>
                  </template>
                </el-popconfirm>
                <el-popconfirm v-if="scope.row.status === TaskStatus.Paused" width="220" confirm-button-text="确定"
                  cancel-button-text="取消" icon-color="#626AEF" title="你确定要继续吗？"
                  @confirm="onResumeTask(scope.row.job_id)">
                  <template #reference>
                    <el-button size="small" link type="primary" class="preview">继续</el-button>
                  </template>
                </el-popconfirm>
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
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import { TableState } from 'ant-design-vue/lib/table/interface'
import { onMounted, watch, provide, reactive, ref, nextTick } from 'vue'
import { IPage } from '/@/api/http/type'
import { deleteTask, updateTaskStatus, UpdateTaskStatus, getWaylineJobs, Task, uploadMediaFileNow, getTaskResult, poweroffCf, deleteOtherTask } from '/@/api/wayline'
import { useMyStore } from '/@/store'
import { ELocalStorageKey, ERouterName } from '/@/types/enums'
import { useFormatTask } from './use-format-task'
import { TaskStatus, TaskProgressInfo, TaskProgressStatus, TaskProgressWsStatusMap, MediaStatus, MediaStatusProgressInfo, TaskMediaHighestPriorityProgressInfo } from '/@/types/task'
import { useTaskWsEvent } from './use-task-ws-event'
import { getErrorMessage } from '/@/utils/error-code/index'
import { commonColor } from '/@/utils/color'
import { ExclamationCircleOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { getDeviceTopo, getUnreadDeviceHms, updateDeviceHms, getPlatformInfo, isAnalyzedApi, startTaskAnasisyApi, getAllWorkspaceInfo } from '/@/api/manage'
import CustomTree from '/@/components/substationTree.vue'
import { getRoot } from '/@/root'
const router = useRouter()

const showVideo = ref(false)
const isResizing = ref(false)
const videoModal = ref(null)
let startX, startY, startWidth, startHeight

// 存储已处理的任务ID，避免重复分析
const analyzedTasks = ref(new Set())

const taskTypeLabels = {
  0: '立即任务',
  1: '定时任务',
  2: '条件任务'
} as { [key: string]: string }

const taskStatusLabels = {
  'To be performed': '准备中',
  'In progress': '任务执行中',
  'Task completed': '任务已完成',
  'Task canceled': '任务已取消',
  'Task failed': '任务执行失败',
  'Paused ': '任务中止'
}

const outControlAcion = {
  0: '返航',
  1: '悬停',
  2: '降落'
} as { [key: string]: string }
// import CreatePlan from './CreatePlan.vue'
// import CreatePlan from '/@/pages/page-web/projects/task.vue'
const store = useMyStore()
let workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
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
const open = ref<boolean>(false)
const { formatTaskType, formatTaskTime, formatLostAction, formatTaskStatus, formatMediaTaskStatus } = useFormatTask()

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
      } else if ([TaskProgressStatus.Rejected, TaskProgressStatus.Canceled, TaskProgressStatus.Timeout, TaskProgressStatus.Failed].includes(status)) {
        getPlans()
      } else if ([TaskProgressStatus.OK].includes(status)) {
        getPlans()
        checkAnaysisStaus(taskItem)
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

/**
 * @description: 跳转到任务结果页面,传递参数
 * @param {Number} job_id 任务id
 * */
const root = getRoot()
function toTaskResult (val) {
  localStorage.setItem('TaskInfo', JSON.stringify(val))
  router.push({ path: '/task/taskResult' })
}

/**
 * 保存任务图片并分析
 */
async function anaysisTaskResult (row) {
  try {
    const res = await startTaskAnasisyApi({ jobId: row.job_id })
  } catch (error) {

  }
}

/**
 * 判断是否已经分析过啦  后续这部分要优化，分析状态要写如表格列表中
 */
async function checkAnaysisStaus (row) {
  try {
    const res = await isAnalyzedApi(row.job_id)
    if (res.data === true) {
      anaysisTaskResult(row)
    }
  } catch (error) {

  }
}

/**
 * 添加深度监听，当媒体文件状态变为"已上传"时自动分析
 */
watch(
  () => plansData.data,
  (newData, oldData) => {
    newData.forEach(row => {
      const statusInfo = formatMediaTaskStatus(row)

      // 当状态为"已上传"且未分析过时，执行分析
      if (statusInfo.text === '已上传' && !analyzedTasks.value.has(row.job_id)) {
        analyzedTasks.value.add(row.job_id)

        // 使用nextTick确保DOM更新完成后再执行
        nextTick(() => {
          anaysisTaskResult(row)
        })
      }
    })
  },
  {
    deep: true,
    immediate: true // 立即执行一次，处理初始数据
  }
)

function closeVideo () {
  showVideo.value = false
}
function startResize (event) {
  isResizing.value = true
  startX = event.clientX
  startY = event.clientY
  startWidth = videoModal.value.offsetWidth
  startHeight = videoModal.value.offsetHeight

  document.addEventListener('mousemove', resize)
  document.addEventListener('mouseup', stopResize)
}

function resize (event) {
  if (isResizing.value) {
    const newWidth = startWidth + (event.clientX - startX)
    const newHeight = startHeight + (event.clientY - startY)
    videoModal.value.style.width = newWidth + 'px'
    videoModal.value.style.height = newHeight + 'px'
  }
}

function stopResize () {
  isResizing.value = false
  document.removeEventListener('mousemove', resize)
  document.removeEventListener('mouseup', stopResize)
}

useTaskWsEvent({
  onTaskProgressWs,
  onTaskMediaProgressWs,
  onoTaskMediaHighestPriorityWS,
})

onMounted(() => {
  getPlans()
  // 添加树形图数据
  getTreeData()
})

//= ===========================================添加树形图==========================================================================

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
  // selectedNode.value = node

  workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
  getPlans()
  console.log('Node changed in parent:', node) // 确认父组件事件是否触发
}
function getPlans () {
  getWaylineJobs(workspaceId, body).then(res => {
    if (res.code !== 0) {
      return
    }
    plansData.data = res.data.list
    console.log('任务详情', res.data.list)
    paginationProp.total = res.data.pagination.total
    paginationProp.current = res.data.pagination.page
  })
}

// ============================================================分页数据==========================================================
// 分页事件
function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  refreshData(paginationProp)
}
function handleCurrentChange (val: number) {
  paginationProp.current = val
  refreshData(paginationProp)
}

function refreshData (page: Pagination) {
  body.page = page?.current!
  body.page_size = page?.pageSize!
  getPlans()
}
//= ================================================================================================================================
// 删除任务
async function onDeleteTask (jobId: string) {
  const { code } = await deleteTask(workspaceId, {
    job_id: jobId
  })
  if (code === 0) {
    message.success('任务删除成功!')
    getPlans()
  }
  if (code === -1) {
    message.error('设备不在线!')
  }
}

async function onDeleteOtherTask (jobId: string) {
  deleteOtherTask(jobId).then(res => {
    if (res.code !== 0) {
      return
    }
    message.success('任务删除成功!')
    getPlans()
  })
}

// 挂起任务
async function onSuspendTask (jobId: string) {
  const { code } = await updateTaskStatus(workspaceId, {
    job_id: jobId,
    status: UpdateTaskStatus.Suspend
  })
  if (code === 0) {
    message.success('任务挂起成功!')
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

// 跳转到任务直播
function toTaskVideo (val: any) {
  localStorage.setItem('TaskInfo', JSON.stringify(val))
  router.push({ path: '/task/TaskInfo' })
}
</script>

<style lang="scss" scoped>
.container {
  // height: 100%;
  width: 100vw;
  // padding: 10px;
  display: flex;
  flex-direction: column;
  /* 使子元素垂直排列 */
}

.content {
  margin: 15px 12px 0 12px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}
.main-box {
  display: flex;
  /* 使用 flexbox 布局 */
  height: calc(100vh - 80px);
  /* 设置容器高度为视口高度 */
}

.box-left {
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  width: 20%;
  /* 左侧占据 20% 的宽度 */
  // background-color: #4CAF50; /* 绿色背景 */
  padding: 20px;
  /* 内边距 */
  color: white;
  /* 字体颜色 */
  // transition: 0.5s;
  border-radius: 15px;
  border: none;
  height: 100%;
}

.table-container {
  flex-grow: 1;
  overflow: hidden;
  // height: 500px;
  overflow-y: auto;
}

// .box-left:hover {
//   box-shadow: inset 0px 0px 20px 3px rgba(34, 135, 255, 0.7);
// }
.box-right {
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  flex: 1;
  /* 右侧占据剩余空间 */
  width: calc(80% - 10px);
  margin-left: 10px;
  // background-color: #2196F3; /* 蓝色背景 */
  padding: 20px;
  /* 内边距 */
  color: white;
  /* 字体颜色 */
  border-radius: 15px;
  border: none;
  height: 100%;
}

// 头部  标题 面包屑
.header1 {
  width: 100%;
  height: 60px;
  background: #05204B;
  padding: 16px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color: aliceblue;
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
.ellipsis {
  white-space: nowrap;
  /* 防止换行 */
  overflow: hidden;
  /* 隐藏超出部分 */
  text-overflow: ellipsis;
  /* 显示省略号 */
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

.btn {
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color: rgba(255, 255, 255, 0.762);
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
// ::v-deep .el-table td
//  {
//   border: 2px solid #01123288 /* 设置列的边框颜色和粗细 */
// }
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
  background-color: black;
  /* 头部背景颜色 */
  color: white;
  /* 头部文字颜色 */
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

/**
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
}

.video-modal {
  position: relative;
  width: 80%;
  max-width: 800px;
  min-width: 400px;
  min-height: 300px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.video-header {
  display: flex;
  justify-content: space-between;
  padding: 10px;
  background: #f0f0f0;
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
}

.close-button {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 20px;
}

.video-player {
  width: 100%;
  height: auto;
}

.resizer {
  width: 10px;
  height: 10px;
  background: red;
  position: absolute;
  right: 0;
  bottom: 0;
  cursor: se-resize;
}
*/
</style>
