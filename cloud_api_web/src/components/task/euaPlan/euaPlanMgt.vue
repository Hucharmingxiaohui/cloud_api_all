<template>
  <div class="container">
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="计划名称:">
          <el-input v-model="queryForm.planName" placeholder="请输入计划名称" class="custom-input" />
        </el-form-item>
        <el-form-item label="EUA航线名称:">
          <el-input v-model="queryForm.routeName" placeholder="请输入EUA航线名称" class="custom-input" />
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
        <el-table-column prop="routeName" label="EUA航线名称" show-overflow-tooltip />
        <el-table-column prop="routeId" label="EUA航线ID" show-overflow-tooltip />
        <el-table-column prop="pointCount" label="点位数量" width="100" align="center">
          <template #default="scope">
            <span v-if="hasPlanPoints(scope.row)">{{ scope.row.pointCount }}</span>
            <el-tag v-else type="warning">需重新选择点位</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近执行状态" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.lastExecuteStatus === 2" type="warning">执行中</el-tag>
            <el-tag v-else-if="scope.row.lastExecuteStatus === 1" type="success">成功</el-tag>
            <el-tag v-else-if="scope.row.lastExecuteStatus === 0" type="danger">失败</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastTaskName" label="最近任务名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="lastEuaTaskId" label="最近EUA任务ID" show-overflow-tooltip />
        <el-table-column prop="lastExecuteTime" label="最近执行时间" width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" :disabled="!hasPlanPoints(scope.row) || executingIds.includes(scope.row.id)" :loading="executingIds.includes(scope.row.id)" @click="executePlan(scope.row)">立即执行</el-button>
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
      :title="planForm.id ? '编辑EUA飞行计划' : '新建EUA飞行计划'"
      width="820px"
      top="6vh"
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :show-close="!saving"
      :before-close="closePlanDialog"
      class="eua-plan-dialog"
      append-to-body
    >
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="100px" class="plan-form">
        <el-form-item label="计划名称" prop="planName">
          <el-input v-model="planForm.planName" maxlength="50" show-word-limit placeholder="请输入计划名称" />
        </el-form-item>
        <el-form-item label="航线与点位" required>
          <div class="point-picker" v-loading="optionsLoading">
            <el-alert v-if="selectionWarning" :title="selectionWarning" type="warning" :closable="false" show-icon />
            <el-button v-if="selectionWarning" :disabled="optionsLoading || !!optionsError || saving" @click="reselectPoints">重新选择全部点位</el-button>
            <el-alert v-if="optionsError" :title="optionsError" type="error" :closable="false" />
            <el-button v-if="optionsError" @click="loadPointOptions(editingSnapshot)">重试加载点位</el-button>
            <div class="point-picker-heading">
              <div class="point-picker-help">
                <div>仅支持选择同一场站、同一航线下的点位</div>
                <div>勾选航线可全选，清空选择后可切换航线</div>
              </div>
              <span class="point-selection-count">已选 <strong>{{ planForm.pointIds.length }}</strong> 个点位</span>
            </div>
            <el-tree
              ref="selectionTreeRef"
              :data="pointTree"
              node-key="key"
              show-checkbox
              default-expand-all
              :props="{ label: 'label', children: 'children', disabled: 'disabled' }"
              :check-on-click-node="false"
              :expand-on-click-node="false"
              :empty-text="optionsLoading ? '正在加载点位' : '暂无可选点位，请先导入wayline_id，再绑定EUA航线名称'"
              @check="handlePointCheck"
            >
              <template #default="{ data }">
                <div class="point-tree-node" :class="{ 'is-route': !!data.children, 'is-unavailable': data.disabled }" :title="data.label">
                  <template v-if="data.children">
                    <div class="point-tree-title">
                      <span class="point-tree-name">{{ data.routeName }}</span>
                      <span class="point-route-count">{{ data.children.length }} 个点位</span>
                    </div>
                    <div class="point-tree-meta">场站：{{ data.subCode }} · 航线 ID：{{ data.routeId }}</div>
                  </template>
                  <template v-else>
                    <span class="point-tree-name">{{ data.pointName }}</span>
                    <span class="point-tree-code">{{ data.pointCode }}</span>
                  </template>
                </div>
              </template>
            </el-tree>
          </div>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="planForm.enabled" :active-value="1" :inactive-value="0" :disabled="saving" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="planForm.remark" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请输入备注（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="optionsLoading || !!optionsError || !!selectionWarning" @click="submitPlan">保存计划</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElAlert, ElButton, ElMessage, ElMessageBox, ElSwitch, ElTree, FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { addEuaPlan, deleteEuaPlan, executeEuaPlan, getEuaPointOptions, getEuaPlanPage, updateEuaPlan, EuaPointGroup, EuaPlanPayload, EuaPlanRow } from '/@/api/cq-eua-plan'
import { buildPointTree, hasPlanPoints, restoreSelection, selectionError } from './pointSelection'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<EuaPlanRow[]>([])
const pointGroups = ref<EuaPointGroup[]>([])
const optionsLoading = ref(false)
const optionsError = ref('')
const selectionWarning = ref('')
const selectionTreeRef = ref<InstanceType<typeof ElTree>>()
const executingIds = ref<number[]>([])
const editingSnapshot = ref<EuaPlanRow | null>(null)
let optionsRequest = 0
const planDialogVisible = ref(false)
const planFormRef = ref<FormInstance>()

const queryForm = reactive({
  planName: '',
  routeName: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const planForm = reactive<EuaPlanPayload>({
  id: undefined,
  planName: '',
  pointIds: [] as number[],
  remark: '',
  enabled: 1
})

const planRules: FormRules = {
  planName: [{ required: true, whitespace: true, message: '请输入计划名称', trigger: 'blur' }]
}

const pointTree = computed(() => buildPointTree(pointGroups.value, planForm.pointIds).map((group, groupIndex) => ({
  ...group,
  routeName: pointGroups.value[groupIndex].routeName,
  subCode: pointGroups.value[groupIndex].subCode,
  routeId: pointGroups.value[groupIndex].routeId,
  disabled: group.disabled || saving.value || !!selectionWarning.value,
  children: group.children.map((point, pointIndex) => ({
    ...point,
    pointName: pointGroups.value[groupIndex].points[pointIndex].pointName || pointGroups.value[groupIndex].points[pointIndex].pointCode,
    pointCode: pointGroups.value[groupIndex].points[pointIndex].pointCode,
    disabled: point.disabled || saving.value || !!selectionWarning.value
  }))
})))

function closePlanDialog (done: () => void) {
  if (!saving.value) done()
}

async function syncCheckedPoints () {
  await nextTick()
  selectionTreeRef.value?.setCheckedKeys(planForm.pointIds.map((id: number) => `point:${id}`))
}

function handlePointCheck (_node: any, state: { checkedNodes: any[] }) {
  const ids = state.checkedNodes.filter(node => node.pointId !== undefined).map(node => node.pointId)
  const error = ids.length ? selectionError(pointGroups.value, ids) : ''
  if (error) ElMessage.warning(error)
  else planForm.pointIds = ids
  syncCheckedPoints()
}

function reselectPoints () {
  selectionWarning.value = ''
  planForm.pointIds = []
  syncCheckedPoints()
}

async function getPlan () {
  loading.value = true
  try {
    const res = await getEuaPlanPage({
      page: pagination.current,
      pageSize: pagination.pageSize,
      planName: queryForm.planName,
      routeName: queryForm.routeName
    })
    if (res.code === 0) {
      tableData.value = (res.data?.records || []).map(normalizePlanRow)
      pagination.total = res.data?.total || 0
    } else {
      ElMessage.error(res.message || '计划列表加载失败')
    }
  } catch {
    ElMessage.error('计划列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function reset () {
  queryForm.planName = ''
  queryForm.routeName = ''
  pagination.current = 1
  getPlan()
}

function toCreatePlan () {
  resetPlanForm()
  planDialogVisible.value = true
  editingSnapshot.value = null
  loadPointOptions()
}

function editPlan (row: any) {
  resetPlanForm()
  planForm.id = row.id
  planForm.planName = row.planName
  planForm.remark = row.remark
  planForm.enabled = row.enabled ?? 1
  editingSnapshot.value = row
  planDialogVisible.value = true
  loadPointOptions(row)
}

function resetPlanForm () {
  planForm.id = undefined
  planForm.planName = ''
  planForm.pointIds = []
  planForm.remark = ''
  planForm.enabled = 1
  selectionWarning.value = ''
  planFormRef.value?.clearValidate()
}

async function loadPointOptions (row: any = null) {
  const request = ++optionsRequest
  optionsLoading.value = true
  optionsError.value = ''
  pointGroups.value = []
  planForm.pointIds = []
  try {
    const res = await getEuaPointOptions()
    if (request !== optionsRequest) return
    if (res.code !== 0) throw new Error(res.message || '点位加载失败')
    pointGroups.value = res.data || []
    if (row) {
      const restored = restoreSelection(pointGroups.value, row)
      planForm.pointIds = restored.ids
      selectionWarning.value = restored.warning
    }
    await syncCheckedPoints()
  } catch (error) {
    if (request === optionsRequest) optionsError.value = error instanceof Error ? error.message : '点位加载失败，请重试'
  } finally {
    if (request === optionsRequest) optionsLoading.value = false
  }
}

function normalizePlanRow (item: any) {
  return {
    ...item,
    id: item.id,
    planName: item.planName || item.plan_name || item.name || '',
    routeId: item.routeId || item.route_id || item.waylineId || item.wayline_id || '',
    lastExecuteStatus: item.lastExecuteStatus ?? item.last_execute_status,
    lastEuaTaskId: item.lastEuaTaskId || item.last_eua_task_id || '',
    lastTaskName: item.lastTaskName || item.last_task_name || '',
    lastExecuteTime: item.lastExecuteTime || item.last_execute_time || ''
  }
}

async function submitPlan () {
  if (saving.value || optionsLoading.value || optionsError.value || selectionWarning.value) return
  if (!await planFormRef.value?.validate().catch(() => false)) return
  const error = selectionError(pointGroups.value, planForm.pointIds)
  if (error) {
    ElMessage.warning(error)
    return
  }
  saving.value = true
  try {
    const payload = {
      ...(planForm.id ? { id: planForm.id } : {}),
      planName: planForm.planName.trim(),
      pointIds: [...planForm.pointIds],
      remark: planForm.remark,
      enabled: planForm.enabled
    }
    const res = planForm.id ? await updateEuaPlan(payload) : await addEuaPlan(payload)
    if (res.code === 0) {
      ElMessage.success('保存成功')
      planDialogVisible.value = false
      getPlan()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function executePlan (row: any) {
  if (!hasPlanPoints(row) || executingIds.value.includes(row.id)) return
  try {
    await ElMessageBox.confirm(`确定立即执行计划“${row.planName}”吗？`, '提示', { type: 'warning' })
  } catch { return }
  executingIds.value.push(row.id)
  try {
    const res = await executeEuaPlan(row.id)
    if (res.code === 0 && res.data?.success) {
      ElMessage.success('任务已下发EUA平台')
      getPlan()
    } else {
      ElMessage.error(res.data?.responseMsg || res.message || '任务下发失败')
    }
  } catch {
    ElMessage.error('任务下发失败，请稍后重试')
  } finally {
    executingIds.value = executingIds.value.filter(id => id !== row.id)
  }
}

async function removePlan (row: any) {
  try {
    await ElMessageBox.confirm(`确定删除计划“${row.planName}”吗？`, '提示', { type: 'warning' })
  } catch { return }
  try {
    const res = await deleteEuaPlan(row.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      getPlan()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    ElMessage.error('删除失败，请稍后重试')
  }
}

onMounted(() => {
  getPlan()
})
</script>

<style lang="scss" scoped>
.point-picker {
  width: 100%;
  min-width: 0;
  border: 1px solid rgba(113, 159, 255, 0.3);
  border-radius: 8px;
  overflow: hidden;
  background: rgba(3, 17, 42, 0.3);
  .el-tree {
    height: clamp(180px, 32vh, 300px);
    overflow: auto;
    padding: 10px 12px;
    background: transparent;
    color: #e5efff;
    --el-tree-node-hover-bg-color: rgba(70, 143, 242, 0.14);
    :deep(.el-tree-node__content) { height: auto; min-height: 36px; padding-right: 8px; }
    :deep(.el-tree-node__expand-icon) { color: #9dbbe6; }
    :deep(.el-checkbox) { margin-right: 10px; }
  }
}
.point-picker-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(113, 159, 255, 0.2);
  background: rgba(69, 133, 233, 0.08);
}
.point-picker-help { color: #b8cbea; font-size: 12px; line-height: 22px; }
.point-selection-count {
  flex-shrink: 0;
  padding: 0 10px;
  border-radius: 4px;
  background: rgba(64, 158, 255, 0.15);
  color: #bcdfff;
  font-size: 12px;
  line-height: 28px;
  strong { color: #fff; margin: 0 3px; font-size: 15px; }
}
.point-tree-node {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  line-height: 22px;
  &.is-route { display: block; padding: 7px 0; }
  &.is-unavailable { opacity: 0.45; }
}
.point-tree-title { display: flex; align-items: center; gap: 10px; min-width: 0; }
.point-tree-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #e5efff; }
.is-route .point-tree-name { font-weight: 600; color: #fff; }
.point-route-count { flex-shrink: 0; color: #a8cfff; font-size: 12px; }
.point-tree-meta { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #9bb6dc; font-size: 12px; }
.point-tree-code { margin-left: auto; flex-shrink: 0; max-width: 45%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #a8c0e1; font-size: 12px; }
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
  max-width: calc(100vw - 32px);
  border-radius: 10px;
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
  padding: 24px 28px 4px 20px;
  max-height: 72vh;
  overflow-y: auto;
}

:global(.eua-plan-dialog .el-dialog__footer) {
  padding: 16px 28px 22px;
  text-align: right;
  border-top: 1px solid rgba(113, 159, 255, 0.18);
}

:global(.eua-plan-dialog .plan-form .el-form-item) { margin-bottom: 22px; }
:global(.eua-plan-dialog .el-dialog__footer .el-button) { min-width: 88px; }

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
