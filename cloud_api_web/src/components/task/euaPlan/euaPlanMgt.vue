<template>
  <div class="container">
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="计划名称:">
          <el-input v-model="queryForm.planName" placeholder="请输入计划名称" class="custom-input" />
        </el-form-item>
        <el-form-item label="间隔名称:">
          <el-input v-model="queryForm.bayName" placeholder="请输入间隔名称" class="custom-input" />
        </el-form-item>
        <el-form-item>
          <el-button class="new_btn" type="primary" :icon="Search" @click="getPlan">查询</el-button>
          <el-button class="new_btn1" type="primary" :icon="Refresh" @click="reset">重置</el-button>
          <el-button class="new_btn" type="primary" :icon="Plus" @click="toCreatePlan">新建计划</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content">
      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="planName" label="计划名称" show-overflow-tooltip />
        <el-table-column prop="bayName" label="间隔名称" show-overflow-tooltip />
        <el-table-column prop="bayId" label="间隔ID" show-overflow-tooltip />
        <el-table-column prop="routeId" label="EUA航线ID" show-overflow-tooltip />
        <el-table-column label="最近执行状态" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.lastExecuteStatus === 2" type="warning">执行中</el-tag>
            <el-tag v-else-if="scope.row.lastExecuteStatus === 1" type="success">成功</el-tag>
            <el-tag v-else-if="scope.row.lastExecuteStatus === 0" type="danger">失败</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastEuaTaskId" label="最近EUA任务ID" show-overflow-tooltip />
        <el-table-column prop="lastExecuteTime" label="最近执行时间" width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="executePlan(scope.row)">立即执行</el-button>
            <el-button link type="primary" @click="editPlan(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="removePlan(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        @size-change="getPlan"
        @current-change="getPlan"
      />
    </div>

    <el-dialog
      v-model="planDialogVisible"
      :title="planForm.id ? '编辑EUA平台计划' : '新建EUA平台计划'"
      width="720px"
      :close-on-click-modal="false"
      class="eua-plan-dialog"
      append-to-body
    >
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="120px" class="plan-form">
        <el-form-item label="计划名称" prop="planName">
          <el-input v-model="planForm.planName" maxlength="50" show-word-limit placeholder="请输入计划名称" />
        </el-form-item>
        <el-form-item label="间隔" prop="bayId">
          <el-select v-model="planForm.bayId" filterable placeholder="请选择间隔" :teleported="false" class="select-operation">
            <el-option
              v-for="item in bayOptions"
              :key="item.bayId"
              :label="formatBayLabel(item)"
              :value="item.bayId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="planForm.remark" type="textarea" :rows="4" maxlength="200" show-word-limit placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="saving" @click="submitPlan">保存</el-button>
        <el-button @click="planDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { addEuaPlan, deleteEuaPlan, executeEuaPlan, getEuaBayOptions, getEuaPlanPage, updateEuaPlan } from '/@/api/cq-eua-plan'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const bayOptions = ref<any[]>([])
const planDialogVisible = ref(false)
const planFormRef = ref<FormInstance>()

const queryForm = reactive({
  planName: '',
  bayName: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const planForm = reactive<any>({
  id: undefined,
  planName: '',
  bayId: '',
  remark: ''
})

const planRules: FormRules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  bayId: [{ required: true, message: '请选择间隔', trigger: 'change' }]
}

async function getPlan () {
  loading.value = true
  try {
    const res = await getEuaPlanPage({
      page: pagination.current,
      pageSize: pagination.pageSize,
      planName: queryForm.planName,
      bayName: queryForm.bayName
    })
    if (res.code === 0) {
      const data = res.data || {}
      tableData.value = (data.records || []).map(normalizePlanRow)
      pagination.total = data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function reset () {
  queryForm.planName = ''
  queryForm.bayName = ''
  pagination.current = 1
  getPlan()
}

function toCreatePlan () {
  resetPlanForm()
  planDialogVisible.value = true
  loadBayOptions()
}

function editPlan (row: any) {
  planForm.id = row.id
  planForm.planName = row.planName
  planForm.bayId = row.bayId
  planForm.remark = row.remark
  planDialogVisible.value = true
  loadBayOptions()
}

function resetPlanForm () {
  planForm.id = undefined
  planForm.planName = ''
  planForm.bayId = ''
  planForm.remark = ''
  planFormRef.value?.clearValidate()
}

async function loadBayOptions () {
  const res = await getEuaBayOptions()
  if (res.code === 0) {
    bayOptions.value = (res.data || []).map(normalizeBayOption).filter((item: any) => item.bayId)
  }
}

function normalizeBayOption (item: any) {
  const bayId = item.bayId || item.bay_id || item.id || ''
  const bayName = item.bayName || item.bay_name || item.name || bayId
  const routeId = item.routeId || item.route_id || item.waylineId || item.wayline_id || ''
  return { ...item, bayId, bayName, routeId }
}

function normalizePlanRow (item: any) {
  return {
    ...item,
    id: item.id,
    planName: item.planName || item.plan_name || item.name || '',
    bayId: item.bayId || item.bay_id || '',
    bayName: item.bayName || item.bay_name || '',
    routeId: item.routeId || item.route_id || item.waylineId || item.wayline_id || '',
    lastExecuteStatus: item.lastExecuteStatus ?? item.last_execute_status,
    lastEuaTaskId: item.lastEuaTaskId || item.last_eua_task_id || '',
    lastExecuteTime: item.lastExecuteTime || item.last_execute_time || ''
  }
}

function formatBayLabel (item: any) {
  const name = item.bayName || item.bayId
  return item.routeId ? `${name}（${item.routeId}）` : name
}

async function submitPlan () {
  await planFormRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      id: planForm.id,
      planName: planForm.planName,
      plan_name: planForm.planName,
      name: planForm.planName,
      bayId: planForm.bayId,
      bay_id: planForm.bayId,
      remark: planForm.remark
    }
    const res = planForm.id ? await updateEuaPlan(payload) : await addEuaPlan(payload)
    if (res.code === 0) {
      ElMessage.success('保存成功')
      planDialogVisible.value = false
      getPlan()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

async function executePlan (row: any) {
  await ElMessageBox.confirm(`确定立即执行计划“${row.planName}”吗？`, '提示', { type: 'warning' })
  const res = await executeEuaPlan(row.id)
  if (res.code === 0 && res.data?.success) {
    ElMessage.success('任务已下发EUA平台')
    getPlan()
  } else {
    ElMessage.error(res.data?.responseMsg || res.message || '任务下发失败')
  }
}

async function removePlan (row: any) {
  await ElMessageBox.confirm(`确定删除计划“${row.planName}”吗？`, '提示', { type: 'warning' })
  const res = await deleteEuaPlan(row.id)
  if (res.code === 0) {
    ElMessage.success('删除成功')
    getPlan()
  }
}

onMounted(() => {
  getPlan()
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
  box-shadow: inset 0 0 0 1px rgba(113, 159, 255, 0.45);
}

:deep(.el-pagination .el-input__inner),
:deep(.el-pagination .el-select__selected-item),
:deep(.el-pagination .el-select__caret) {
  color: rgba(255, 255, 255, 0.86);
}

:global(.eua-plan-dialog) {
  background-color: #0B2757;
  box-shadow: inset 0 0 15px 1px rgba(34, 135, 255, 0.5);
  border: 1px solid rgba(113, 159, 255, 0.45);
}

:global(.eua-plan-dialog .el-dialog__header) {
  margin-right: 0;
  border-bottom: 1px solid rgba(113, 159, 255, 0.25);
}

:global(.eua-plan-dialog .el-dialog__title) {
  color: rgba(255, 255, 255, 0.95);
  font-size: 18px;
}

:global(.eua-plan-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: rgba(255, 255, 255, 0.75);
}

:global(.eua-plan-dialog .el-dialog__body) {
  padding: 28px 36px 8px 20px;
}

:global(.eua-plan-dialog .el-dialog__footer) {
  padding: 8px 36px 26px;
  text-align: center;
}

:global(.eua-plan-dialog .plan-form .el-form-item__label) {
  color: rgba(255, 255, 255, 0.88);
}

:global(.eua-plan-dialog .plan-form .el-input__wrapper),
:global(.eua-plan-dialog .plan-form .el-textarea__inner) {
  background-color: #0B2756;
  box-shadow: inset 0 0 0 1px rgba(113, 159, 255, 0.45);
}

:global(.eua-plan-dialog .plan-form .el-input__inner),
:global(.eua-plan-dialog .plan-form .el-textarea__inner) {
  color: #fff;
}

:global(.eua-plan-dialog .plan-form .el-input__count),
:global(.eua-plan-dialog .plan-form .el-input__count-inner),
:global(.eua-plan-dialog .plan-form .el-textarea .el-input__count) {
  background: transparent !important;
  color: rgba(255, 255, 255, 0.58);
}

:global(.eua-plan-dialog .el-button--default) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(113, 159, 255, 0.45);
  color: rgba(255, 255, 255, 0.82);
}

:global(.eua-plan-dialog .el-button--primary) {
  background-image: linear-gradient(180deg, rgba(70, 145, 217, 1) 0, rgba(21, 81, 181, 1) 100%);
  border: none;
}

:global(.el-overlay) {
  background-color: rgba(0, 10, 32, 0.72);
}

.select-operation {
  width: 100%;

  :deep(.el-select__placeholder) {
    color: #A8ABB2;
  }

  :deep(.el-select__wrapper) {
    background-color: #0B2756;
    box-shadow: inset 0 0 0 1px rgba(113, 159, 255, 0.45);
  }

  :deep(.el-select__caret) {
    color: #ffffff;
  }

  :deep(.el-select-dropdown) {
    background: #012b78;
    box-shadow: inset 0 0 15px 1px rgba(34, 135, 255, 0.5);
    border: 1px solid #719fff;
  }

  :deep(.el-select-dropdown__item) {
    color: #ffffff;
  }

  :deep(.el-select-dropdown__item.is-hovering),
  :deep(.el-select-dropdown__item.is-selected) {
    background-color: rgba(34, 135, 255, 0.7);
  }
}
</style>
