<template>
  <div class="patrol-tasks">
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" />
    <el-button :loading="loading" @click="load">刷新</el-button>
    <el-table :data="records" v-loading="loading">
      <el-table-column prop="taskCode" label="上级计划ID" />
      <el-table-column prop="taskName" label="任务名称" />
      <el-table-column prop="subCode" label="厂站" />
      <el-table-column prop="centerCode" label="上级中心" />
      <el-table-column label="状态"><template #default="{ row }">{{ ['启用', '停用', '已删除'][row.isenable] }}</template></el-table-column>
      <el-table-column label="操作" width="310"><template #default="{ row }">
        <el-button :disabled="busy || row.isenable !== 1" @click="change(row.id, 0)">启用</el-button>
        <el-button :disabled="busy || row.isenable !== 0" @click="change(row.id, 1)">停用</el-button>
        <el-button :disabled="busy || row.isenable === 2" @click="change(row.id, 2)">删除</el-button>
        <el-button @click="showExecutions(row.id)">执行</el-button>
      </template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="prev, pager, next, total" @current-change="load" />
    <el-dialog v-model="visible" title="上级计划执行记录" width="85%" :teleported="false">
      <el-table :data="executions">
        <el-table-column prop="task_code" label="上级计划ID" />
        <el-table-column prop="fixed_start_time" label="原定时间" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="job_id" label="本地jobId" />
        <el-table-column prop="eua_task_id" label="EUA真实任务ID" />
        <el-table-column prop="eua_record_id" label="EUA执行记录ID" />
        <el-table-column prop="message" label="说明" />
      </el-table>
      <el-pagination v-model:current-page="executionPage" :page-size="20" :total="executionTotal" layout="prev, pager, next, total" @current-change="loadExecutions" />
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { patrolPage, patrolState, patrolExecutions } from '/@/api/center-patrol'
const records = ref<any[]>([])
const executions = ref<any[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const busy = ref(false)
const visible = ref(false)
const selected = ref(0)
const executionPage = ref(1)
const executionTotal = ref(0)
const loadError = ref('')
function errorMessage (error: any) {
  return error.response?.data?.msg || error.response?.data?.message || error.message || '请求失败'
}
function data (response: any) {
  if (response.code !== 0 && response.code !== 200) throw new Error(response.message || response.msg || '请求失败')
  return response.data
}
async function load () {
  loading.value = true
  loadError.value = ''
  try { const result = data(await patrolPage(page.value)); records.value = result.records; total.value = result.total } catch (e: any) { loadError.value = `上级巡视任务加载失败：${errorMessage(e)}`; ElMessage.error(loadError.value) } finally { loading.value = false }
}
async function change (id: number, state: number) {
  try { await ElMessageBox.confirm(`${['启用', '停用', '删除'][state]}此上级计划？此操作不会停止正在飞行的任务。`, '确认操作') } catch { return }
  busy.value = true
  try { data(await patrolState(id, state)); await load() } catch (e: any) { ElMessage.error(errorMessage(e)) } finally { busy.value = false }
}
async function loadExecutions () {
  try { const result = data(await patrolExecutions(selected.value, executionPage.value)); executions.value = result.records; executionTotal.value = result.total } catch (e: any) { ElMessage.error(errorMessage(e)) }
}
async function showExecutions (id: number) { selected.value = id; executionPage.value = 1; visible.value = true; await loadExecutions() }
onMounted(load)
</script>
<style scoped>
.patrol-tasks {
  box-sizing: border-box;
  padding: 20px;
  min-height: calc(100vh - 105px);
  color: #e7f0ff;
  background: #0b2756;
  --el-bg-color: #0b2756;
  --el-bg-color-overlay: #0b2756;
  --el-fill-color-blank: #012462;
  --el-fill-color-light: #1252a0;
  --el-text-color-primary: #e7f0ff;
  --el-text-color-regular: #b8cbea;
  --el-border-color: rgba(113, 159, 255, 0.35);
}
.patrol-tasks > .el-alert { margin-bottom: 12px; }
.patrol-tasks > .el-button { margin-bottom: 12px; }
.patrol-tasks :deep(.el-table) {
  --el-table-bg-color: #012462;
  --el-table-tr-bg-color: #012462;
  --el-table-header-bg-color: #0b2756;
  --el-table-row-hover-bg-color: #1252a0;
  --el-table-text-color: #e7f0ff;
  --el-table-header-text-color: #e7f0ff;
  --el-table-border-color: rgba(113, 159, 255, 0.25);
}
.patrol-tasks :deep(.el-pagination) { margin-top: 16px; justify-content: center; }
.patrol-tasks :deep(.el-loading-mask) { background-color: rgba(3, 17, 42, 0.7); }
</style>
