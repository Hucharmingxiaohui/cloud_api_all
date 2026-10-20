<template>
  <div class="container">
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="任务名称:">
          <el-input v-model="queryForm.taskName" placeholder="请输入任务名称" class="custom-input" />
        </el-form-item>
        <el-form-item label="EUA任务ID:">
          <el-input v-model="queryForm.euaTaskId" placeholder="请输入EUA任务ID" class="custom-input" />
        </el-form-item>
        <el-form-item>
          <el-button class="new_btn" type="primary" :icon="Search" @click="searchRecords">查询</el-button>
          <el-button class="new_btn1" type="primary" :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content">
      <el-table :data="taskRecordData" v-loading="recordLoading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="taskName" label="任务名称" min-width="230" show-overflow-tooltip />
        <el-table-column prop="planName" label="所属计划名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="businessId" label="本地任务编码" width="230" show-overflow-tooltip />
        <el-table-column prop="euaTaskId" label="EUA任务ID" width="180" show-overflow-tooltip />
        <el-table-column prop="bayId" label="间隔ID" width="170" show-overflow-tooltip />
        <el-table-column prop="routeId" label="航线ID" width="170" show-overflow-tooltip />
        <el-table-column label="下发状态" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.success === 1" type="success">成功</el-tag>
            <el-tag v-else-if="scope.row.success === 0" type="danger">失败</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="responseMsg" label="响应信息" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openTaskResult(scope.row)">任务结果</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="recordPagination.current"
        v-model:page-size="recordPagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="recordPagination.total"
        @size-change="getTaskRecords"
        @current-change="getTaskRecords"
      />
    </div>

  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getEuaTaskRecordPage } from '/@/api/cq-eua-plan'

const recordLoading = ref(false)
const router = useRouter()
const taskRecordData = ref<any[]>([])

const queryForm = reactive({
  taskName: '',
  euaTaskId: ''
})

const recordPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

async function getTaskRecords () {
  recordLoading.value = true
  try {
    const res = await getEuaTaskRecordPage({
      page: recordPagination.current,
      pageSize: recordPagination.pageSize,
      taskName: queryForm.taskName,
      euaTaskId: queryForm.euaTaskId
    })
    if (res.code === 0) {
      const data = res.data || {}
      taskRecordData.value = (data.records || []).map(normalizeTaskRecordRow)
      recordPagination.total = data.total || 0
    }
  } finally {
    recordLoading.value = false
  }
}

function searchRecords () {
  recordPagination.current = 1
  getTaskRecords()
}

function reset () {
  queryForm.taskName = ''
  queryForm.euaTaskId = ''
  recordPagination.current = 1
  getTaskRecords()
}

function openTaskResult (row: any) {
  const businessId = row.businessId || row.business_id || row.taskCode || row.task_code || ''
  const euaTaskId = row.euaTaskId || row.eua_task_id || row.taskId || row.task_id || ''
  router.push({ name: 'euaTaskResult', query: { businessId, euaTaskId, taskName: row.taskName, planName: row.planName } })
}

function normalizeTaskRecordRow (item: any) {
  return {
    ...item,
    businessId: item.businessId || item.business_id || item.taskCode || item.task_code || '',
    taskCode: item.taskCode || item.task_code || item.businessId || item.business_id || '',
    taskName: item.taskName || item.task_name || '',
    planName: item.planName || item.plan_name || '',
    bayId: item.bayId || item.bay_id || '',
    routeId: item.routeId || item.route_id || '',
    euaTaskId: item.euaTaskId || item.eua_task_id || item.taskId || item.task_id || '',
    taskId: item.taskId || item.task_id || item.euaTaskId || item.eua_task_id || '',
    responseMsg: item.responseMsg || item.response_msg || '',
    success: item.success,
    createTime: item.createTime || item.create_time || ''
  }
}

onMounted(() => {
  getTaskRecords()
})
</script>

<style lang="scss" scoped>
.container {
  width: 100%;
  min-height: calc(100vh - 105px);
  display: flex;
  flex-direction: column;
}
.operation {
  display: flex;
  align-items: center;
  background-color: rgba(1, 36, 98, 1);
  border-radius: 4px;
  height: 60px;
  padding-top: 15px;
  padding-left: 10px;
  margin: 31px 12px 0 12px;

  :deep(.el-form-item__label) {
    color: rgba(255, 255, 255, 0.85);
  }

  .new_btn {
    background-image: linear-gradient(180deg, rgba(70, 145, 217, 1) 0, rgba(21, 81, 181, 1) 100%);
    border-radius: 4px;
    height: 30px;
    border: none;
  }

  .new_btn1 {
    background-image: linear-gradient(180deg, rgba(248, 212, 94, 1) 0, rgba(227, 157, 6, 1) 100%);
    border-radius: 4px;
    height: 30px;
    border: none;
    margin-left: 10px;
  }
}
.custom-input {
  width: 220px;

  :deep(.el-input__wrapper) {
    background-color: #0B2756;
    box-shadow: inset 0 0 0 1px rgba(113, 159, 255, 0.45);
  }

  :deep(.el-input__inner) {
    color: #fff;
  }
}
.content {
  margin: 15px 12px 0 12px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.pagination-wrapper {
  margin: auto 12px 14px 12px;
  display: flex;
  justify-content: center;
}

:deep(.el-table) {
  background-color: transparent;
  color: rgba(255, 255, 255, 0.86);
}

:deep(.el-table th.el-table__cell) {
  background-color: #0B2756;
  color: #E7F0FF;
  border-color: rgba(113, 159, 255, 0.35);
}

:deep(.el-table tr),
:deep(.el-table td.el-table__cell) {
  background-color: rgba(1, 36, 98, 0.82);
  color: rgba(255, 255, 255, 0.82);
  border-color: rgba(113, 159, 255, 0.25);
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) {
  background-color: rgba(18, 82, 160, 0.85);
}

:deep(.el-table__empty-block) {
  background-color: rgba(1, 36, 98, 0.82);
}

:deep(.el-table__empty-text) {
  color: rgba(255, 255, 255, 0.6);
}

:deep(.el-pagination) {
  color: rgba(255, 255, 255, 0.75);
}

:deep(.el-pagination .el-pagination__total),
:deep(.el-pagination .el-pagination__jump),
:deep(.el-pagination .el-pagination__goto),
:deep(.el-pagination .el-pagination__classifier) {
  color: rgba(255, 255, 255, 0.72);
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next),
:deep(.el-pagination .el-pager li) {
  background-color: #0B2756;
  color: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(113, 159, 255, 0.35);
}

:deep(.el-pagination .el-pager li.is-active) {
  background-image: linear-gradient(180deg, rgba(70, 145, 217, 1) 0, rgba(21, 81, 181, 1) 100%);
  color: #fff;
  border-color: rgba(113, 159, 255, 0.65);
}

:deep(.el-pagination .el-input__wrapper),
:deep(.el-pagination .el-select__wrapper) {
  background-color: #0B2756;
  box-shadow: inset 0 0 0 1px rgba(113, 159, 255, 0.35);
}

:deep(.el-pagination .el-input__inner) {
  color: #fff;
}

:deep(.el-dialog) {
  background-color: #06265a;
}

:deep(.el-dialog__title),
:deep(.el-dialog__body),
:deep(.el-dialog__footer) {
  color: rgba(255, 255, 255, 0.86);
}
</style>
