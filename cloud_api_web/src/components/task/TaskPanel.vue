<template>
  <div class="container">
    <!-- <div class="header1">任务管理</div> -->
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="任务名称">
          <el-input
            v-model="queryForm.name"
            placeholder="请输入任务名称"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item label="计划类型:">
          <el-select
            v-model="queryForm.taskType"
            placeholder="请选择类型"
            :teleported="false"
            class="select-operation"
          >
            <el-option value="0" label="立即执行"></el-option>
            <el-option value="1" label="定时执行"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型:">
          <el-select
            v-model="queryForm.planType"
            placeholder="请选择任务类型"
            :teleported="false"
            class="select-operation"
          >
            <el-option value="0" label="点位航线计划"></el-option>
            <el-option value="3" label="普通航线计划"></el-option>
            <el-option value="4" label="光伏板计划"></el-option>
            <el-option value="1" label="风机计划"></el-option>
            <el-option value="2" label="兴趣点环绕计划"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <!-- 查询按钮 -->
          <el-button
            class="new_btn"
            type="primary"
            :icon="Search"
            @click="getPlans"
            >查询
          </el-button>
          <!-- 重置按钮 -->
          <el-button
            class="new_btn1"
            type="primary"
            style="margin-left: 10px"
            :icon="Refresh"
            @click="reset"
            >重置
          </el-button>
          <el-button
            class="new_btn1 delete-bg"
            type="primary"
            :icon="Delete"
            @click="batchDeleteTask"
            >删除
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="content">
      <div class="table-container">
        <el-table
          :data="plansData.data"
          stripe
          @selection-change="handleSelectionChange"
          :row-key="row => row.job_id"
          :header-cell-style="{ height: '43px', color: 'rgba(255, 255, 255, 1)',fontSize: '16px', fontWeight: 'bold', backgroundColor: '#00399A',  borderLeft: '2px #01123288 solid', borderBottom: '1px #154480 solid' }"
        >
          <el-table-column
            type="selection"
            width="55"
            :selectable="isRowSelectable"
          />
          <el-table-column label="序号" align="center" width="60">
            <template #default="scope">
              {{ scope.$index +(paginationProp.current - 1) * paginationProp.pageSize+ 1 }}
            </template>
          </el-table-column>

          <el-table-column label="任务时间" width="300px;">
            <template #default="scope">
              <div
                class="flex-row"
                style="white-space: pre-wrap; justify-content: center;"
              >
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
          <el-table-column label="状态">
            <template #default="scope">
              <div>
                <div class="flex-display " style="justify-content: center;">
                  <span
                    class="circle-icon"
                    :style="{ backgroundColor: formatTaskStatus(scope.row).color }"
                  ></span>
                  {{ taskStatusLabels[formatTaskStatus(scope.row).text] }}
                  <a-tooltip
                    v-if="!!scope.row.code"
                    placement="bottom"
                    arrow-point-at-center
                  >
                    <template #title>
                      <div>{{ getCodeMessage(scope.row.code) }}</div>
                    </template>
                    <exclamation-circle-outlined
                      class="ml5"
                      :style="{ color: commonColor.WARN, fontSize: '16px' }"
                    />
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
          <el-table-column label="媒体文件上传" width="200">
            <template #default="scope">
              <div>
                <div class="flex-display flex-align-center">
                  <span
                    class="circle-icon"
                    :style="{ backgroundColor: formatMediaTaskStatus(scope.row).color }"
                  ></span>
                  {{ formatMediaTaskStatus(scope.row).text }}
                  {{ formatMediaTaskStatus(scope.row).number }}
                  <!-- <br/> -->
                  <span
                    v-if="showAnalysisStatus(scope.row.job_id)"
                    class="analysis-status"
                    :style="{ color: getAnalysisStatusColor(scope.row.job_id), marginLeft: '8px' }"
                  >
                    {{ getAnalysisStatusText(scope.row.job_id) }}
                  </span>
                </div>
                <div class="pl15">
                  <a-tooltip
                    v-if="formatMediaTaskStatus(scope.row).status === MediaStatus.ToUpload"
                    placement="bottom"
                    arrow-point-at-center
                  >
                    <template #title>
                      <div>立即上传</div>
                    </template>
                    <UploadOutlined
                      class="ml5"
                      :style="{ color: commonColor.BLUE, fontSize: '16px' }"
                      @click="onUploadMediaFileNow(scope.row.job_id)"
                    />
                  </a-tooltip>
                </div>
                <!-- 显示分析状态 -->
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="scope">
              <el-popconfirm
                v-if="scope.row.status === TaskStatus.Wait "
                width="220"
                confirm-button-text="确定"
                cancel-button-text="取消"
                icon-color="#626AEF"
                title="你确定要取消飞行任务吗？"
                @confirm="onDeleteTask(scope.row.job_id)"
              >
                <template #reference>
                  <el-button size="small" link type="primary" class="preview"
                    >取消</el-button
                  >
                </template>
              </el-popconfirm>
              <el-popconfirm
                v-if="scope.row.status === TaskStatus.Success || scope.row.status === TaskStatus.Fail || scope.row.status === TaskStatus.CanCel || (scope.row.status === TaskStatus.Carrying && scope.row.progress=== '')"
                width="220"
                confirm-button-text="确定"
                cancel-button-text="取消"
                icon-color="#626AEF"
                title="你确定要删除飞行任务吗？请确保飞行器未起飞。"
                @confirm="onDeleteOtherTask(scope.row.job_id, scope.row.status)"
              >
                <template #reference>
                  <el-button size="small" link type="primary" class="preview"
                    >删除</el-button
                  >
                </template>
              </el-popconfirm>
              <el-button
                size="small"
                link
                type="primary"
                class="preview"
                @click="toTaskResult(scope.row)"
                >任务结果</el-button
              >
              <el-popconfirm
                v-if="scope.row.status === TaskStatus.Carrying"
                width="220"
                confirm-button-text="确定"
                cancel-button-text="取消"
                icon-color="#626AEF"
                title="你确定要挂起飞行任务吗？"
                @confirm="onSuspendTask(scope.row.job_id)"
              >
                <template #reference>
                  <el-button size="small" link type="primary" class="preview"
                    >挂起</el-button
                  >
                </template>
              </el-popconfirm>
              <el-popconfirm
                v-if="scope.row.status === TaskStatus.Paused"
                width="220"
                confirm-button-text="确定"
                cancel-button-text="取消"
                icon-color="#626AEF"
                title="你确定要继续吗？"
                @confirm="onResumeTask(scope.row.job_id)"
              >
                <template #reference>
                  <el-button size="small" link type="primary" class="preview"
                    >继续</el-button
                  >
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-container">
        <!-- 分页 -->
        <el-pagination
          v-model:current-page="paginationProp.current"
          v-model:page-size="paginationProp.pageSize"
          :page-sizes="paginationProp.pageSizeOptions"
          :total="paginationProp.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        >
        </el-pagination>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import { ElButton, ElDialog, ElUpload, ElMessageBox, ElMessage } from 'element-plus'
import { TableState } from 'ant-design-vue/lib/table/interface'
import { onMounted, watch, provide, reactive, ref, nextTick, onUnmounted } from 'vue'
import { IPage } from '/@/api/http/type'
import { deleteTask, updateTaskStatus, UpdateTaskStatus, getWaylineJobs, Task, uploadMediaFileNow, getTaskResult, poweroffCf, batchDeleteTaskApi, deleteOtherTask } from '/@/api/wayline'
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
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
const router = useRouter()

const selectedData = ref([])
// 存储已处理的任务ID，避免重复分析
const analyzedTasks = ref(new Set())
// 存储任务分析状态
const taskAnalysisStatus = ref(new Map()) // Map<job_id, { status: 'analyzing' | 'completed' | 'none', loading: boolean }>()
// 维护正在分析的任务列表
const analyzingTasks = ref(new Set<string>()) // 存储正在分析的任务ID
let analysisTimer: number | null = null // 定时器引用

const queryForm = reactive({
  name: '', // 任务名称
  taskType: '', // 计划类型 执行方式：0立即1定时
  planType: '' // 任务类型：0点位航线 1风机 2兴趣点环绕 3普通航线 4光伏
})

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

function isRowSelectable (row, index) {
  return (
    row.status === TaskStatus.Success ||
    row.status === TaskStatus.Fail ||
    row.status === TaskStatus.CanCel
  )
}

const store = useMyStore()
const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
const userId = ref(localStorage.getItem(ELocalStorageKey.UserId)!)
const body: IPage = {
  page: 1,
  total: 0,
  page_size: 10
}
const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '40'],
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
  if (taskItem.saved_count && taskItem.saved_count > 0) {
    taskItem.uploaded_count = uploadedCount + taskItem.saved_count
  } else {
    taskItem.uploaded_count = uploadedCount
  }
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
 * 获取任务列表
 */
function getPlans () {
  getWaylineJobs(workspaceId, { ...body, ...queryForm }).then(res => {
    if (res.code !== 0) {
      return
    }
    plansData.data = res.data.list
    paginationProp.total = res.data.pagination.total
    paginationProp.current = res.data.pagination.page
  })
}

// 重置
function reset () {
  queryForm.name = ''
  queryForm.taskType = ''
  queryForm.planType = ''
  getPlans()
}
// ----------------------------------------------------------------调用算法进行结果分析-------------------------------------------------------------------

/**
 * 保存任务图片并分析
 */
async function anaysisTaskResult (row) {
  try {
    const res = await startTaskAnasisyApi({ jobId: row.job_id })

    if (res.data === '603') {
      // 图片部分上传，分析条件不成熟
      taskAnalysisStatus.value.set(row.job_id, { status: 'waiting', loading: true })
    }

    // 开始分析后，将探测任务添加到分析列表
    if (!analyzingTasks.value.has(row.job_id)) {
      analyzingTasks.value.add(row.job_id)
    }
  } catch (error) {
    console.error('开始分析失败:', error)
    // 分析失败，从分析列表中移除
    analyzingTasks.value.delete(row.job_id)
    taskAnalysisStatus.value.set(row.job_id, { status: 'none', loading: false })
  }
}

/**
 * 判断是否已经分析过, 并更新状态  后续这部分要优化，分析状态要写入任务结果表格中
 */
async function checkAnaysisStaus (row) {
  try {
    const res = await isAnalyzedApi(row.job_id)

    if (res.data === 0) {
      // 未分析过，开始分析
      if (!analyzingTasks.value.has(row.job_id)) {
        analyzingTasks.value.add(row.job_id)
      }
      taskAnalysisStatus.value.set(row.job_id, { status: 'analyzing', loading: true })
      // await anaysisTaskResult(row)
    } else if (res.data === 1) {
      // 已经分析完成
      analyzingTasks.value.delete(row.job_id) // 从分析列表中移除
      taskAnalysisStatus.value.set(row.job_id, { status: 'completed', loading: false })
    } else if (res.data === 4) {
      // 分析条件不成熟，上传的图片不完整
      if (!analyzingTasks.value.has(row.job_id)) {
        analyzingTasks.value.add(row.job_id)
      }
      taskAnalysisStatus.value.set(row.job_id, { status: 'waiting', loading: true })
    } else {
      // 正在分析
      if (!analyzingTasks.value.has(row.job_id)) {
        analyzingTasks.value.add(row.job_id)
      }
      taskAnalysisStatus.value.set(row.job_id, { status: 'analyzing', loading: true })
    }
  } catch (error) {
    console.error('检查分析状态失败:', error)
    // 检查失败，从分析列表中移除
    analyzingTasks.value.delete(row.job_id)
    taskAnalysisStatus.value.set(row.job_id, { status: 'none', loading: false })
  }
}

function startAnalysisTimer () {
  // 清除现有定时器
  if (analysisTimer) {
    clearInterval(analysisTimer)
  }

  // 设置新的定时器，每10秒检查一次
  analysisTimer = setInterval(() => {
    if (analyzingTasks.value.size > 0) {
      console.log(`定时检查分析状态，当前有 ${analyzingTasks.value.size} 个任务正在分析`)

      // 遍历正在分析的任务列表，逐个检查状态
      analyzingTasks.value.forEach(jobId => {
        const task = plansData.data.find(item => item.job_id === jobId)
        if (task) {
          checkSingleTaskAnalysisStatus(task)
        } else {
          // 如果任务不在当前数据中，从分析列表中移除
          analyzingTasks.value.delete(jobId)
        }
      })
    }
  }, 10000) // 10秒检查一次
}

/**
 * 检查单个任务的分析状态
 */
async function checkSingleTaskAnalysisStatus (task) {
  try {
    const res = await isAnalyzedApi(task.job_id)

    if (res.data === 1) {
      // 分析完成
      analyzingTasks.value.delete(task.job_id)
      taskAnalysisStatus.value.set(task.job_id, { status: 'completed', loading: false })
      console.log(`任务 ${task.job_id} 分析完成`)
    } else if (res.data === 0) {
      // 未分析，可能是分析失败，重新分析
      console.log(`任务 ${task.job_id} 未分析，重新开始分析`)
      taskAnalysisStatus.value.set(task.job_id, { status: 'analyzing', loading: true })
      await anaysisTaskResult(task)
    }
    // 其他状态（分析中）保持不变
  } catch (error) {
    console.error(`检查任务 ${task.job_id} 分析状态失败:`, error)
    // 检查失败，暂时保留在分析列表中，下次继续检查
  }
}

/**
 * 初始化分析状态检查
 */
function initAnalysisStatusCheck () {
  // 启动定时器
  startAnalysisTimer()

  // 初始检查所有已上传媒体文件的任务
  plansData.data.forEach(row => {
    const statusInfo = formatMediaTaskStatus(row)
    if (statusInfo.text === '已上传' && !analyzedTasks.value.has(row.job_id)) {
      analyzedTasks.value.add(row.job_id)
      checkAnaysisStaus(row)
    }
  })
}

/**
 * 添加深度监听，当媒体文件状态变为"已上传"时自动分析
 */
watch(
  () => plansData.data,
  (newData, oldData) => {
    // 1. 数据更新后，确保定时器运行
    if (!analysisTimer) {
      initAnalysisStatusCheck()
    }

    // 2. 检查新数据中需要分析的任务
    newData.forEach(row => {
      const statusInfo = formatMediaTaskStatus(row)

      // 当状态为"已上传"且未分析过时，执行分析
      if (statusInfo.text === '已上传' && !analyzedTasks.value.has(row.job_id)) {
        analyzedTasks.value.add(row.job_id)

        // 使用nextTick确保DOM更新完成后再执行
        nextTick(() => {
          console.log('上传图片完成，进行分析....')
          checkAnaysisStaus(row)
        })
      }
    })
    // 3. 清理旧数据中不再存在的任务的分析状态
    if (oldData && oldData.length > 0) {
      const newJobIds = new Set(newData.map(item => item.job_id))
      oldData.forEach(oldRow => {
        if (!newJobIds.has(oldRow.job_id)) {
          // 如果任务已从数据中移除，清理相关状态
          analyzingTasks.value.delete(oldRow.job_id)
          analyzedTasks.value.delete(oldRow.job_id)
        }
      })
    }
  },
  {
    deep: true,
    immediate: true
  }
)

// 监听数据变化，重新初始化分析检查
watch(
  () => plansData.data,
  () => {
    // 数据更新后，确保定时器运行
    if (!analysisTimer) {
      initAnalysisStatusCheck()
    }
  },
  { deep: true }
)

/**
 * 获取任务分析状态显示文本
 */
function getAnalysisStatusText (jobId) {
  const statusInfo = taskAnalysisStatus.value.get(jobId)
  if (!statusInfo) return ''

  switch (statusInfo.status) {
    case 'analyzing':
      return '分析中'
    case 'completed':
      return '分析完成'
    case 'waiting':
      return '部分文件上传'
    case 'none':
      return '分析异常'
    default:
      return ''
  }
}

/**
 * 获取任务分析状态颜色
 */
function getAnalysisStatusColor (jobId) {
  const statusInfo = taskAnalysisStatus.value.get(jobId)
  if (!statusInfo) return '#666'

  switch (statusInfo.status) {
    case 'analyzing':
      return commonColor.WARN // 黄色
    case 'completed':
      return commonColor.SUCCESS // 绿色
    default:
      return '#666'
  }
}

/**
 * 是否显示分析状态
 */
function showAnalysisStatus (jobId) {
  const statusInfo = taskAnalysisStatus.value.get(jobId)
  return statusInfo
}

// -------------------------------------------------------------------------------------------------------------------

useTaskWsEvent({
  onTaskProgressWs,
  onTaskMediaProgressWs,
  onoTaskMediaHighestPriorityWS,
})

onMounted(() => {
  getPlans()
  initAnalysisStatusCheck()
})

// 组件卸载时清除定时器
onUnmounted(() => {
  if (analysisTimer) {
    clearInterval(analysisTimer)
    analysisTimer = null
  }
})

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
// 删除准备中的任务
async function onDeleteTask (jobId: string) {
  const { code } = await deleteTask(workspaceId, {
    job_id: jobId
  })
  if (code === 0) {
    ElMessage.success('任务删除成功!')
    getPlans()
  }
  if (code === -1) {
    ElMessage.error('设备不在线!')
  }
}

// 删除失败、成功、取消状态的任务
async function onDeleteOtherTask (jobId: string, status: any) {
  try {
    // 执行状态，先进行挂起操作
    if (status === TaskStatus.Carrying) {
      const { code } = await updateTaskStatus(workspaceId, {
        job_id: jobId,
        status: UpdateTaskStatus.Suspend
      })
    }
    deleteOtherTask(jobId).then(res => {
      if (res.code !== 0) {
        return
      }
      ElMessage.success('任务删除成功!')
      getPlans()
    })
  } catch (error) {

  }
}

// 批量删除失败、成功、取消状态的任务
function handleSelectionChange (val:any) {
  selectedData.value = val
}
async function batchDeleteTask () {
  try {
    if (selectedData.value.length === 0) {
      ElMessage.warning('请选择要删除的数据!')
      return
    }
    const obj = selectedData.value.map(item => item.job_id)
    ElMessageBox.confirm('确定要删除选中的数据吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(async () => {
        const res = await batchDeleteTaskApi(obj)
        if (res.code !== 0) {
          return
        }
        ElMessage.success('删除成功!')
        await getPlans()
      })
  } catch (e) {

  }
}

// 挂起任务
async function onSuspendTask (jobId: string) {
  const { code } = await updateTaskStatus(workspaceId, {
    job_id: jobId,
    status: UpdateTaskStatus.Suspend
  })
  if (code === 0) {
    ElMessage.success('任务挂起成功!')
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
    ElMessage.success('解除挂起成功!')
    getPlans()
  }
}

// 立即上传媒体
async function onUploadMediaFileNow (jobId: string) {
  const { code } = await uploadMediaFileNow(workspaceId, jobId)
  if (code === 0) {
    ElMessage.success('上传图片成功!')
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
.select-operation {
  :deep(.el-select__placeholder) {
    color: rgb(182, 182, 182);
    font-size: 14px;
    font-family: Google Sans-Medium;
    font-weight: 500;
  }

  :deep(.el-select__wrapper) {

    // background: rgba(59, 116, 255, 0.15);
    background-color: #0B2756;
    // box-shadow: inset 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
    // box-shadow: 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
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
  // height: 28px;
  // width: 40px;
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
  padding-top: 15px;
  padding-left: 15px;
  .new_btn {
    background-image: linear-gradient(180deg,
        rgba(70, 145, 217, 1) 0,
        rgba(21, 81, 181, 1) 100%);
    border-radius: 4px;
    height: 30px;

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
  .delete-bg{
        background-image: linear-gradient(180deg,
        rgb(243, 172, 172) 0,
        rgb(213 53 5) 100%) !important;
  }
  .new_btn1 {
    background-image: linear-gradient(180deg,
        rgba(248, 212, 94, 1) 0,
        rgba(227, 157, 6, 1) 100%);
    border-radius: 4px;
    height: 30px;

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

:deep(.el-form-item__label) {
    color: white;
}

::v-deep .el-input__inner {
    color: white;
}
</style>
