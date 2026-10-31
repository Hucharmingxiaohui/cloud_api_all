<template>
  <main class="analysis-page">
    <header class="analysis-heading">
      <div><small>工地巡检 / 独立目录分析</small><h2>照片分析与巡检报告</h2><p>按飞行任务加载本部署端照片目录，无需填写服务器路径。</p></div>
      <a-button @click="router.push('/task/taskResult')">返回任务结果</a-button>
    </header>
    <section class="analysis-panel task-toolbar">
      <label for="analysis-task">飞行任务 ID</label>
      <a-input id="analysis-task" v-model:value="taskInput" placeholder="输入任务 ID，加载对应照片目录" :disabled="s.busy" @pressEnter="openTask" />
      <a-button type="primary" :loading="s.busy" :disabled="!taskInput.trim() || !workspace" @click="openTask">加载任务目录</a-button>
      <a-button :loading="healthBusy" @click="checkHealth">检查分析服务连接</a-button>
      <span>{{ healthLabel }}</span>
    </section>
    <a-alert v-if="!workspace" message="尚未选择工作空间，请先进入工作空间后使用分析功能。" type="warning" show-icon />
    <a-alert v-if="s.error" :message="s.error" type="error" show-icon />
    <a-alert v-if="notice" :message="notice" type="info" show-icon />
    <a-alert v-if="s.pendingRequestId" type="warning" show-icon :message="'提交结果待确认，照片配置已锁定。请刷新历史批次，或使用原请求标识重试：' + s.pendingRequestId" />
    <template v-if="s.loaded">
      <section class="analysis-panel">
        <div class="section-heading"><div><h3>任务照片 · {{ s.taskId }}</h3><p>{{ s.photos.length }} 张照片 · {{ includedCount }} 张已选择检测 · {{ s.saved ? '当前配置已保存' : '存在未保存修改，请先保存' }}</p></div>
          <a-space><a-button :disabled="locked || (s.saved && !needsConfirmation(s.photos))" :loading="s.busy" @click="save">{{ needsConfirmation(s.photos) ? '确认元数据并保存配置' : '保存照片配置' }}</a-button><a-button type="primary" :loading="s.busy" :disabled="!canStart" @click="start">{{ s.pendingRequestId ? '使用原标识重试提交' : '开始新批次分析' }}</a-button></a-space>
        </div>
        <a-alert message="snapshot-incomplete / 来源缺失的元数据请逐张核对，再点击“确认元数据并保存配置”。可明确确认镜头为“未确认”、区域为空，无需改为猜测值；保存后以服务端 ready 判定是否就绪。未分配区域的照片仅做单图检测，不归入任意区域人数。空检测项或排除的照片不提交。" type="info" show-icon />
        <p v-if="locked" class="helper">批次进行中、状态未知或提交未确认时，配置暂不可修改。状态未知请查询原批次，不要创建新任务。</p>
        <div class="bulk-toolbar">
          <strong>批量设置（已选 {{ selected.length }} 张）</strong>
          <a-select v-model:value="bulkField" :options="bulkFields" aria-label="批量修改字段" :disabled="locked" />
          <a-input v-if="bulkField === 'areaId'" v-model:value="bulkArea" placeholder="区域 ID，留空即清除区域" :disabled="locked" />
          <a-select v-if="bulkField === 'type'" v-model:value="bulkType" :options="TYPES" aria-label="批量镜头类型" :disabled="locked" />
          <a-select v-if="bulkField === 'checks'" v-model:value="bulkChecks" mode="multiple" :options="CHECKS" placeholder="选择检测项目，留空不参与分析" :disabled="locked" />
          <a-select v-if="bulkField === 'excluded'" v-model:value="bulkExcluded" :options="[{ value: 'yes', label: '排除分析' }, { value: 'no', label: '不排除' }]" :disabled="locked" />
          <a-button :disabled="locked || !selected.length" @click="applyMetadata">应用到所选照片</a-button>
        </div>
        <a-table :data-source="s.photos" :columns="photoColumns" row-key="id" :row-selection="rowSelection" :pagination="{ pageSize: 10, showSizeChanger: true }" :scroll="{ x: 1300 }" :loading="s.busy">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'photo'">
              <button class="thumb-button" :aria-label="'预览原图 ' + record.fileName" @click="preview(record.id)"><AuthenticatedImage :task-id="s.taskId" :image-id="record.id" :alt="record.fileName" /></button>
              <div>{{ record.fileName }}</div><small>{{ record.id }}</small>
            </template>
            <a-input v-else-if="column.key === 'areaId'" :value="record.areaId" :disabled="locked" placeholder="未分配区域" @update:value="setArea(record, $event)" />
            <a-select v-else-if="column.key === 'type'" v-model:value="record.type" :disabled="locked" :options="TYPES" @change="controller.edit()" />
            <a-select v-else-if="column.key === 'checks'" v-model:value="record.checks" mode="multiple" :disabled="locked" :options="CHECKS" placeholder="不参与分析" @change="controller.edit()" />
            <a-checkbox v-else-if="column.key === 'excluded'" v-model:checked="record.excluded" :disabled="locked" @change="controller.edit()">排除</a-checkbox>
            <template v-else-if="column.key === 'ready'"><a-tag :color="record.ready ? 'green' : 'orange'">{{ record.ready ? '就绪' : '未就绪' }}</a-tag><p>{{ record.metadataSource || '来源缺失，需手工核对' }}</p></template>
          </template>
        </a-table>
      </section>
      <section class="analysis-panel">
        <div class="section-heading"><div><h3>分析批次与冻结证据</h3><p>每次主动分析建立新批次，历史结果使用当时冻结的区域、镜头与检测项，不随上方修改变化。</p></div><a-button :loading="s.busy" @click="controller.refreshHistory">刷新历史批次 / 确认提交</a-button></div>
        <a-select class="history-select" :value="s.selectedBatchId || undefined" placeholder="选择历史批次" :disabled="s.batchBusy || s.busy || s.reportBusy" @change="selectBatch">
          <a-select-option v-for="batch in s.batches" :key="batch.batchId" :value="batch.batchId">{{ batch.createdAt }} · {{ batch.batchId }} · {{ statusText(batch.status) }}</a-select-option>
        </a-select>
        <a-alert v-if="s.pollError" :message="s.pollError" type="warning" show-icon />
        <a-spin :spinning="s.batchBusy" tip="正在读取批次结果…">
          <template v-if="s.batch">
            <div class="batch-summary"><a-tag color="blue">{{ statusText(s.batch.status) }}</a-tag><span>逐图进度 {{ s.batch.finished }} / {{ s.batch.total }}（结束处理数，不代表成功数）</span><span>远端任务 {{ s.batch.remoteId || '尚未返回' }}</span><span>请求标识 {{ s.batch.requestId }}</span></div>
            <p v-if="s.batch.message">{{ s.batch.message }}</p>
            <p class="helper">批次完成不代表所有检测成功或现场无隐患；区域去重完成以批次状态为准，不以照片进度判断。</p>
            <h4>区域去重人数</h4>
            <a-table :data-source="s.batch.areas || []" :columns="areaColumns" row-key="area_id" :pagination="false" size="small">
              <template #bodyCell="{ column, record }"><template v-if="column.key === 'count'">{{ countText(record) }}</template><template v-else-if="column.key === 'status'">{{ statusText(record.status) }}</template></template>
            </a-table>
            <p class="helper">仅展示算法返回的同区域跨图去重人数。未成功为“—”，确认无人为“0”；不累加照片人数，也不将各区域人数相加称作全工地去重总人数。</p>
            <h4>逐张照片 · 批次冻结配置与检测结果</h4>
            <a-table :data-source="s.batch.photos || []" :columns="resultColumns" row-key="id" :pagination="{ pageSize: 8 }" :scroll="{ x: 1000 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'metadata'"><b>{{ record.id }}</b><p>区域 {{ record.areaId ?? '未分配' }} · {{ typeText(record.type) }}</p><small>{{ record.checks.map(checkText).join('、') || '无检测项目' }}</small></template>
                <template v-else-if="column.key === 'status'">{{ statusText(record.status) }}<p>{{ record.message }}</p></template>
                <template v-else-if="column.key === 'result'">
                  <div v-for="(check, i) in record.result?.checks || []" :key="check.code + '-' + i" class="check-result"><b>{{ checkText(check.code) }}</b> · {{ statusText(check.status) }}
                    <span v-if="check.code === 'person_count'"> · 单图人数 {{ countText(check) }}</span>
                    <span v-if="check.status === 'success' && check.boxes"> · {{ check.boxes.length }} 个框</span>
                    <span v-if="check.label"> · {{ check.label }}（置信度 {{ check.score ?? '未返回' }}）</span><p v-if="check.message">{{ check.message }}</p>
                  </div>
                  <span v-if="!record.result">尚无结构化结果</span>
                  <p v-if="record.result?.image_error">标注图：{{ record.result.image_error }}</p>
                  <details v-if="record.result"><summary>完整结构化结果 / 框坐标与置信度</summary><pre>{{ JSON.stringify(record.result, null, 2) }}</pre></details>
                </template>
                <template v-else-if="column.key === 'preview'"><a-button size="small" @click="preview(record.id, s.batch.batchId, !!record.imageUrl)">原图 / 标注图</a-button></template>
              </template>
            </a-table>
            <div class="report-toolbar"><a-button :loading="s.reportBusy" :disabled="!canReport" @click="controller.report">生成 Word 报告</a-button><a-button type="primary" :loading="s.reportBusy" :disabled="!s.batch.reportReady || s.batchBusy" @click="download">下载 Word 报告</a-button><span>{{ s.batch.reportReady ? '本批次报告已就绪' : '报告尚未生成' }}</span></div>
          </template>
          <a-empty v-else description="尚无批次结果，请选择历史批次或保存配置后手动开始分析。" />
        </a-spin>
      </section>
    </template>
    <a-empty v-else-if="!s.busy" description="输入飞行任务 ID 加载本地照片目录；加载或检查连接不会自动提交分析。" />
    <a-modal v-model:visible="viewerVisible" title="照片证据对照" :width="1200" :footer="null" :destroy-on-close="true">
      <div v-if="viewerVisible" class="comparison">
        <section><h3>原始采集照片</h3><AuthenticatedImage :task-id="s.taskId" :image-id="viewer.id" alt="原始采集照片" /></section>
        <section v-if="viewer.batchId"><h3>本批次算法标注图</h3><AuthenticatedImage v-if="viewer.hasResult" :task-id="s.taskId" :batch-id="viewer.batchId" :image-id="viewer.id" alt="本批次算法标注图" /><p v-else>该照片尚无标注图，结构化结果仍可查看。</p></section>
      </div>
    </a-modal>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ELocalStorageKey } from '/@/types/enums'
import * as api from '/@/api/construction-analysis'
import type { AnalysisHealth, AnalysisPhoto, PhotoType } from '/@/api/construction-analysis'
import AuthenticatedImage from './AuthenticatedImage.vue'
import { createController, initialState } from './controller'
import { CHECKS, TYPES, canAnalyze, canGenerateReport, checkText, countText, errorText, needsConfirmation, requestId, statusText, typeText } from './model'

const route = useRoute(); const router = useRouter()
const workspace = localStorage.getItem(ELocalStorageKey.WorkspaceId)
const s = reactive(initialState())
const controller = createController(s, api, { uuid: () => requestId(globalThis.crypto), schedule: fn => setTimeout(fn, 4000), cancel: timer => clearTimeout(timer) })
const taskInput = ref(''); const selected = ref<string[]>([]); const notice = ref('')
const locked = computed(() => controller.locked())
const includedCount = computed(() => s.photos.filter(p => !p.excluded && p.checks.length).length)
const canStart = computed(() => !!workspace && s.loaded && !s.busy && (!!s.pendingRequestId || (!locked.value && s.saved && canAnalyze(s.photos))))
const canReport = computed(() => !s.batchBusy && !s.reportBusy && canGenerateReport(s.batch))
const bulkField = ref('areaId'); const bulkArea = ref(''); const bulkType = ref<PhotoType>('unknown'); const bulkChecks = ref<string[]>([]); const bulkExcluded = ref('no')
const bulkFields = [{ value: 'areaId', label: '区域归属' }, { value: 'type', label: '镜头类型' }, { value: 'checks', label: '检测项目' }, { value: 'excluded', label: '是否排除' }]
const rowSelection = computed(() => ({ selectedRowKeys: selected.value, onChange: (keys: string[]) => { selected.value = keys }, getCheckboxProps: () => ({ disabled: locked.value }) }))
const cellSlot = { slots: { customRender: 'bodyCell' } }
const photoColumns = [{ title: '照片 / 原图预览', key: 'photo', width: 210 }, { title: '区域 ID（可空）', key: 'areaId', width: 180 }, { title: '镜头类型', key: 'type', width: 130 }, { title: '最终检测项目', key: 'checks', width: 400 }, { title: '排除', key: 'excluded', width: 90 }, { title: '服务端就绪 / 元数据来源', key: 'ready', width: 180 }].map(column => ({ ...column, ...cellSlot }))
const areaColumns = [{ title: '区域 ID', dataIndex: 'area_id' }, { title: '去重状态', key: 'status', ...cellSlot }, { title: '去重人数', key: 'count', ...cellSlot }, { title: '说明', dataIndex: 'message' }]
const resultColumns = [{ title: '冻结元数据与检测项', key: 'metadata', width: 250 }, { title: '照片状态', key: 'status', width: 140 }, { title: '逐项检测结果', key: 'result', width: 470 }, { title: '照片证据', key: 'preview', width: 160 }].map(column => ({ ...column, ...cellSlot }))
function setArea (photo: AnalysisPhoto, value: string) { if (locked.value) return; photo.areaId = value || null; controller.edit() }
function applyMetadata () {
  if (locked.value) return
  for (const p of s.photos.filter(p => selected.value.includes(p.id))) {
    if (bulkField.value === 'areaId') p.areaId = bulkArea.value.trim() || null
    if (bulkField.value === 'type') p.type = bulkType.value
    if (bulkField.value === 'checks') p.checks = [...bulkChecks.value]
    if (bulkField.value === 'excluded') p.excluded = bulkExcluded.value === 'yes'
  }
  controller.edit()
}
async function openTask () {
  if (!workspace || s.busy || !taskInput.value.trim()) return
  const taskId = taskInput.value.trim()
  if (route.query.taskId === taskId) await controller.load(taskId)
  else await router.replace({ query: { ...route.query, taskId } })
}
async function save () { notice.value = ''; await controller.save(); if (s.saved) notice.value = '照片配置已保存，服务端就绪状态已更新。' }
async function start () { notice.value = ''; await controller.start() }
async function selectBatch (id: string) { await controller.select(id) }
const healthState = ref<AnalysisHealth | null>(null); const healthBusy = ref(false); const healthError = ref(''); let healthGeneration = 0
const healthLabel = computed(() => healthError.value || (healthState.value ? `${!healthState.value.configured ? '未配置' : healthState.value.connected ? '连接正常' : '连接失败'}：${healthState.value.message || '连接状态不代表模型全部可用'}` : '尚未检查连接'))
async function checkHealth () {
  if (healthBusy.value || !workspace) return
  const g = ++healthGeneration; healthBusy.value = true; healthError.value = ''
  try { const result = await api.health(); if (g === healthGeneration) healthState.value = result } catch (e) { if (g === healthGeneration) healthError.value = '连接检查失败：' + errorText(e) } finally { if (g === healthGeneration) healthBusy.value = false }
}
const viewerVisible = ref(false); const viewer = reactive({ id: '', batchId: '', hasResult: false })
function preview (id: string, batchId = '', hasResult = false) { Object.assign(viewer, { id, batchId, hasResult }); viewerVisible.value = true }
const downloadUrls = new Set<string>()
async function download () {
  const batchId = s.batch?.batchId; const taskId = s.taskId
  const blob = await controller.download(); if (!blob) return
  const url = URL.createObjectURL(blob); downloadUrls.add(url)
  const anchor = document.createElement('a'); anchor.href = url
  anchor.download = Array.from(`工地巡检报告-${taskId}-${batchId}.docx`, char => char.charCodeAt(0) < 32 || '\\/:*?"<>|'.includes(char) ? '_' : char).join('')
  document.body.appendChild(anchor); anchor.click(); anchor.remove()
  setTimeout(() => { URL.revokeObjectURL(url); downloadUrls.delete(url) }, 1000)
}
watch(() => route.query.taskId, value => {
  taskInput.value = typeof value === 'string' ? value : ''; selected.value = []; notice.value = ''; viewerVisible.value = false
  if (workspace) controller.load(taskInput.value)
}, { immediate: true })
onBeforeUnmount(() => { controller.dispose(); healthGeneration++; downloadUrls.forEach(url => URL.revokeObjectURL(url)); downloadUrls.clear() })
</script>

<style scoped>
.analysis-page { padding: 24px; height: 100%; overflow: auto; background: #f3f6fa; color: #263c52; }
.analysis-heading, .section-heading { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.analysis-heading h2 { margin: 5px 0; color: #16324f; }.analysis-heading small { color: #587c9b; }
.analysis-page p { margin: 8px 0; }.analysis-page > .ant-alert { margin-bottom: 12px; }
.analysis-panel { background: white; padding: 22px; border: 1px solid #dde5ed; border-radius: 8px; margin: 18px 0; }
.task-toolbar, .bulk-toolbar, .report-toolbar, .batch-summary { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.task-toolbar > .ant-input { width: 340px; }.task-toolbar > span { color: #526b83; }
.bulk-toolbar { padding: 16px 0; }.bulk-toolbar > .ant-input { width: 240px; }.bulk-toolbar > .ant-select { min-width: 150px; max-width: 540px; }
.analysis-page :deep(td .ant-select) { width: 100%; }.analysis-page :deep(.ant-table-cell) { vertical-align: top; }
.thumb-button { width: 130px; padding: 0; border: 0; cursor: pointer; margin-bottom: 5px; }.thumb-button :deep(img) { height: 70px; }
.helper, .analysis-page small { color: #667c90; }.helper { font-size: 13px; }.history-select { width: min(100%, 820px); margin: 12px 0; }
.batch-summary { padding: 18px 0 8px; }.batch-summary span { overflow-wrap: anywhere; }.check-result { padding: 5px 0; border-bottom: 1px solid #edf1f5; }
.analysis-page pre { white-space: pre-wrap; word-break: break-all; max-height: 300px; overflow: auto; font-size: 12px; background: #f4f7fa; padding: 10px; }
.report-toolbar { margin-top: 20px; }.comparison { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; }
@media (max-width: 900px) { .analysis-page { padding: 12px; }.section-heading, .analysis-heading { align-items: flex-start; flex-direction: column; } }
</style>
