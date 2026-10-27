<template>
  <main class="construction-shell" v-loading="busy">
    <div class="page-heading"><div><h2>工地巡检任务结果</h2><p>{{ detail?.name || jobId }}</p></div><div><el-button @click="load">刷新本地结果</el-button><el-button @click="back">返回飞行任务</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <template v-if="detail">
      <div class="panel"><p>任务 ID：{{ detail.job_id }}</p><p>照片接收：{{ detail.received_count }} / {{ detail.expected_count ?? '总数未知' }} 张 · {{ detail.media_complete ? '已收齐' : '飞行未完成或完整性待确认' }}</p><p>本地归档：{{ detail.photos.filter(p => p.archived).length }} 张</p></div>
      <el-tabs v-model="tab"><el-tab-pane label="本地照片" name="photos" /><el-tab-pane label="分析与结果" name="analysis" /><el-tab-pane label="巡检报告" name="report" /></el-tabs>
      <div v-if="tab === 'photos'" class="panel">
        <div class="table-tools"><span>原图保留在本部署端，分析批次继续关联本条飞行任务。</span><el-button :disabled="busy || !detail.photos.length" @click="archive">保存 / 补齐本地原图</el-button></div>
        <div v-if="detail.overall_route?.overall_id" class="table-tools"><span>按本次执行的区域绑定快照归类</span><el-select v-model="photoArea"><el-option value="" :label="'全部照片（' + detail.photos.length + '）'" /><el-option v-for="area in detail.overall_route.bindings" :key="area.area_id" :value="area.area_id" :label="area.area_name + '（' + detail.photos.filter(p => p.area_id === area.area_id).length + '）'" /><el-option value="__unmatched" :label="'未匹配 / 未绑定（' + detail.photos.filter(p => !p.area_id).length + '）'" /></el-select></div>
        <el-table :data="visiblePhotos" empty-text="当前分类暂无照片"><el-table-column prop="name" label="照片名称" /><el-table-column v-if="detail.overall_route?.overall_id" label="巡检区域 / 航点"><template #default="{ row }">{{ row.area_name || row.association_message || '未匹配' }}{{ row.waypoint_index ? ' · 航点' + row.waypoint_index : '' }}</template></el-table-column><el-table-column label="本地状态"><template #default="{ row }">{{ row.archived ? '已归档' : '待归档' }}</template></el-table-column><el-table-column label="操作"><template #default="{ row }"><el-button link :disabled="!row.archived || busy" @click="preview(row)">查看原图</el-button></template></el-table-column></el-table>
      </div>
      <div v-else-if="tab === 'analysis'" class="panel">
        <h3>回站手动分析</h3><p>回到站内连接网线后，先检查分析主机，再手动提交。连接主机和刷新结果不会启动新的分析。</p>
        <p>主机连接：{{ connected === null ? '未检查' : connected ? '可连接' : '不可连接' }} · 分析状态：{{ statusLabel }}</p>
        <el-alert v-if="!detail.analysis_configured" title="工地算法接口尚未配置；照片归档可独立使用。" type="info" :closable="false" />
        <p v-if="detail.analysis.message">{{ detail.analysis.message }}</p>
        <div class="property-actions"><el-button :disabled="busy || !detail.analysis_configured" @click="check">检查分析主机</el-button><el-button type="primary" :disabled="!canSubmit" @click="submit">{{ detail.analysis.batch_id ? '重新分析' : '提交分析' }}</el-button><el-button :disabled="busy || !detail.analysis.batch_id || !detail.analysis_configured" @click="refresh">查询分析进度 / 取回结果</el-button></div>
        <p v-if="detail.analysis.batch_id">分析批次：{{ detail.analysis.batch_id }}</p>
        <p class="helper">提交结果待确认时，请查询原批次，避免重复下发。重新分析会保留原批次记录。</p>
        <div v-if="detail.analysis.result"><h3>已保存的分析结果</h3><p>人员、隐患和证据图片的专用展示将在算法字段约定后接入。</p><pre class="analysis-json">{{ JSON.stringify(detail.analysis.result, null, 2) }}</pre></div>
        <el-empty v-else description="尚无已回传的分析结果；未分析不代表无隐患" />
      </div>
      <div v-else class="panel"><h3>工地巡检报告</h3><p>报告模板待接入，将关联当前任务及分析批次。</p><el-button disabled>生成正式报告</el-button></div>
    </template>
    <el-dialog v-model="photoVisible" :title="photoName" width="min(1000px, 92vw)" append-to-body><img v-if="photoUrl" :src="photoUrl" :alt="photoName" style="max-width:100%;max-height:70vh" /></el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, defineProps, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ERouterName } from '/@/types'
import { uuidv4 } from '/@/utils/uuid'
import { getConstructionResult, archiveConstructionPhotos, checkConstructionHost, submitConstructionAnalysis, refreshConstructionAnalysis, getConstructionPhoto } from '/@/api/construction'
import type { ConstructionResult } from '/@/api/construction'

const props = defineProps<{ jobId: string }>()
const router = useRouter(); const tab = ref('photos'); const busy = ref(false); const error = ref('')
const detail = ref<ConstructionResult | null>(null); const connected = ref<boolean | null>(null)
const photoArea = ref('')
const visiblePhotos = computed(() => (detail.value?.photos || []).filter(p => !photoArea.value || (photoArea.value === '__unmatched' ? !p.area_id : p.area_id === photoArea.value)))
const photoUrl = ref(''); const photoName = ref(''); const photoVisible = ref(false)
let requestId = uuidv4(); let disposed = false
const labels: Record<string, string> = { SUBMITTING: '提交结果待确认', UNKNOWN: '提交结果待确认', QUEUED: '排队中', RUNNING: '分析中', SUCCEEDED: '分析完成', FAILED: '分析失败' }
const statusLabel = computed(() => labels[detail.value?.analysis.status || ''] || '待手动分析')
const canSubmit = computed(() => !busy.value && connected.value === true && detail.value?.analysis_configured && detail.value?.media_complete && (!detail.value.analysis.batch_id || ['SUCCEEDED', 'FAILED'].includes(detail.value.analysis.status || '')))
function back () { router.push('/' + ERouterName.TASK) }
async function run (action: () => Promise<void>) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await action() } catch (cause: any) { error.value = cause?.response?.data?.message || cause?.message || '请求失败，请检查本地服务和主机连接'; connected.value = null } finally { busy.value = false }
}
async function load () { await run(async () => { detail.value = await getConstructionResult(props.jobId) }) }
async function archive () { await run(async () => { detail.value = await archiveConstructionPhotos(props.jobId) }) }
async function check () { await run(async () => { connected.value = (await checkConstructionHost(props.jobId)).connected }) }
async function submit () {
  if (!canSubmit.value) return
  try { await ElMessageBox.confirm('将向站内分析主机提交本任务照片进行分析。是否继续？', '手动提交分析', { confirmButtonText: '提交分析', cancelButtonText: '取消' }) } catch { return }
  await run(async () => {
    const batch = await submitConstructionAnalysis(props.jobId, requestId)
    if (detail.value) detail.value.analysis = batch
    requestId = uuidv4()
  })
}
async function refresh () { await run(async () => { const batch = await refreshConstructionAnalysis(props.jobId); if (detail.value) detail.value.analysis = batch }) }
async function preview (photo: { file_id: string; name: string }) {
  await run(async () => {
    const blob = await getConstructionPhoto(props.jobId, photo.file_id)
    if (disposed) return
    if (photoUrl.value) URL.revokeObjectURL(photoUrl.value)
    photoUrl.value = URL.createObjectURL(blob); photoName.value = photo.name; photoVisible.value = true
  })
}
watch(() => props.jobId, load, { immediate: true })
onBeforeUnmount(() => { disposed = true; if (photoUrl.value) URL.revokeObjectURL(photoUrl.value) })
</script>

<style scoped src="./construction.css"></style>
<style scoped>
.panel { margin-bottom: 16px; padding: 20px; }
.analysis-json { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
