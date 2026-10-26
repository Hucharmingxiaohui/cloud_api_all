<template>
  <section class="area-waylines">
    <div class="route-actions">
      <el-button type="primary" size="small" :disabled="busy || !enabled" @click="create">二维新建</el-button>
      <el-button size="small" :disabled="busy || !enabled" @click="fileInput?.click()">上传 KMZ</el-button>
      <el-button size="small" :disabled="busy || !enabled" @click="openLibrary">关联已有航线</el-button>
      <input ref="fileInput" type="file" accept=".kmz" hidden @change="upload" />
    </div>
    <p class="route-hint">巡检航点及航段限定在当前区域内。保存后同步显示在航线管理。</p>
    <p v-if="busy">处理中…</p>
    <p v-if="error" class="route-error" role="alert">{{ error }}</p>
    <el-button v-if="error" size="small" @click="load">刷新列表</el-button>
    <p v-if="!busy && !error && !rows.length">该区域暂无航线，可新建或上传。</p>
    <article v-for="row in rows" :key="row.wayline_id" :class="['route-card', { selected: selected === row.wayline_id }]">
      <button class="route-select" :disabled="busy" @click="select(row)"><strong>{{ row.name }}</strong><span>{{ row.source === 'manual' ? '二维创建' : row.source === 'existing' ? '航线库关联' : 'KMZ 导入' }} · 区域 v{{ row.area_version }}</span></button>
      <p :class="{ 'route-error': row.status !== 'VALID' }">{{ row.message }}</p>
      <div class="route-actions"><el-button v-if="row.status === 'STALE'" size="small" :disabled="busy || !enabled" @click="revalidate(row)">重新校验</el-button><el-button size="small" :disabled="busy" @click="unlink(row)">解除关联</el-button></div>
    </article>
    <p v-if="selectedHeight" class="route-hint">预览高度：{{ selectedHeight === 'WGS84' ? 'WGS84 椭球高' : '相对起飞点高度' }}。进场、返航路径不包含在区域边界校验中。</p>
    <el-dialog v-model="libraryVisible" title="关联航线管理中的已有航线" width="680px" append-to-body :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy">
      <p>选择已有航线，区域校验通过后建立关联，原航线文件保持不变。</p>
      <el-input v-model="libraryKeyword" placeholder="按航线名称搜索，回车查询" clearable @keyup.enter="searchLibrary" />
      <p v-if="libraryError" role="alert" class="route-error">{{ libraryError }}</p>
      <el-table :data="libraryRows" v-loading="libraryLoading" max-height="360" row-key="id">
        <el-table-column prop="name" label="航线名称" min-width="210" />
        <el-table-column label="操作" width="150"><template #default="{ row }"><el-button size="small" type="primary" :disabled="busy || rows.some(r => r.wayline_id === row.id)" @click="bindExisting(row.id)">{{ rows.some(r => r.wayline_id === row.id) ? '已关联' : '校验并关联' }}</el-button></template></el-table-column>
      </el-table>
      <el-pagination :current-page="libraryPage" :page-size="10" :total="libraryTotal" layout="prev, pager, next, total" :disabled="busy || libraryLoading" @current-change="page => { libraryPage = page; loadLibrary() }" />
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { bindConstructionWayline, getConstructionWayline, listConstructionWaylines, unlinkConstructionWayline, uploadConstructionWayline, validateConstructionWayline } from '/@/api/construction'
import { getWaylineFiles } from '/@/api/wayline'
import type { ConstructionWayline } from '/@/api/construction'
import { uuidv4 } from '/@/utils/uuid'
/* global defineProps, defineEmits */
const props = defineProps<{ areaId: string; version: number; enabled: boolean; initialWayline?: string }>()
const emit = defineEmits(['preview', 'busy'])
const router = useRouter()
const rows = ref<ConstructionWayline[]>([])
const busy = ref(false); const error = ref(''); const selected = ref(''); const selectedHeight = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
let epoch = 0
const libraryVisible = ref(false); const libraryRows = ref<Array<{ id: string; name: string }>>([])
const libraryKeyword = ref(''); const libraryPage = ref(1); const libraryTotal = ref(0)
const libraryLoading = ref(false); const libraryError = ref('')
let libraryEpoch = 0
async function loadLibrary () {
  const token = ++libraryEpoch; const area = props.areaId
  libraryLoading.value = true; libraryError.value = ''; libraryRows.value = []
  try {
    const workspace = localStorage.getItem('workspace_id')
    if (!workspace) throw new Error('请先登录工作空间')
    const response = await getWaylineFiles(workspace, { page: libraryPage.value, page_size: 10, name: libraryKeyword.value.trim() })
    if (token !== libraryEpoch || area !== props.areaId) return
    if (response.code !== 0) throw new Error(response.message || '航线库加载失败')
    libraryRows.value = response.data.list || []; libraryTotal.value = response.data.pagination?.total || 0
  } catch (cause) { if (token === libraryEpoch) libraryError.value = message(cause) } finally { if (token === libraryEpoch) libraryLoading.value = false }
}
function searchLibrary () { libraryPage.value = 1; loadLibrary() }
function openLibrary () { libraryVisible.value = true; searchLibrary() }
async function bindExisting (id: string) {
  const area = props.areaId; const token = ++epoch
  setBusy(true); libraryError.value = ''
  try {
    const row = await bindConstructionWayline(area, props.version, id)
    if (token !== epoch) return
    rows.value = [row, ...rows.value.filter(r => r.wayline_id !== row.wayline_id)]
    selected.value = row.wayline_id; selectedHeight.value = row.height_mode || ''; emit('preview', row)
    libraryVisible.value = false; error.value = ''
  } catch (cause) { if (token === epoch) libraryError.value = message(cause) } finally { if (token === epoch) setBusy(false) }
}
let uploadRetry: { file: File; area: string; request: string } | null = null
function message (cause: unknown) { return cause instanceof Error ? cause.message : '航线操作失败，请重试' }
function setBusy (value: boolean) { busy.value = value; emit('busy', value) }
async function load () {
  libraryVisible.value = false; libraryEpoch++
  const token = ++epoch; const area = props.areaId
  rows.value = []; selected.value = ''; selectedHeight.value = ''; emit('preview', null); error.value = ''; setBusy(true)
  try {
    const data = await listConstructionWaylines(area)
    if (token !== epoch) return
    rows.value = data
  } catch (e) { if (token === epoch) error.value = message(e) } finally { if (token === epoch) setBusy(false) }
  if (token === epoch && props.initialWayline) {
    const row = rows.value.find(r => r.wayline_id === props.initialWayline)
    if (row) await select(row)
  }
}
async function select (row: ConstructionWayline) {
  const token = ++epoch; const area = props.areaId
  selected.value = row.wayline_id; selectedHeight.value = ''; emit('preview', null); error.value = ''; setBusy(true)
  try {
    const result = await getConstructionWayline(area, row.wayline_id)
    if (token !== epoch) return
    Object.assign(row, result)
    if (result.status !== 'VALID') { error.value = result.message; return }
    selectedHeight.value = result.height_mode || ''; emit('preview', result)
  } catch (e) { if (token === epoch) error.value = message(e) } finally { if (token === epoch) setBusy(false) }
}
function create () {
  localStorage.removeItem(`construction_wayline_draft_${props.areaId}`)
  router.push({ path: '/wayline/createWayline', query: { construction_area: props.areaId, area_version: props.version } })
}
async function upload (event: Event) {
  const input = event.target as HTMLInputElement; const file = input.files?.[0]; input.value = ''
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.kmz') || file.size > 20 * 1024 * 1024) { error.value = '请选择不超过 20MB 的 KMZ 文件'; return }
  const area = props.areaId; const token = ++epoch
  if (!uploadRetry || uploadRetry.area !== area || uploadRetry.file.name !== file.name || uploadRetry.file.size !== file.size || uploadRetry.file.lastModified !== file.lastModified) uploadRetry = { file, area, request: uuidv4() }
  setBusy(true); error.value = ''
  try {
    const row = await uploadConstructionWayline(area, props.version, file, uploadRetry.request)
    if (token !== epoch) return
    uploadRetry = null
    rows.value = [row, ...rows.value.filter(r => r.wayline_id !== row.wayline_id)]
    selected.value = row.wayline_id; selectedHeight.value = row.height_mode || ''; emit('preview', row)
  } catch (e) { if (token === epoch) error.value = message(e) } finally { if (token === epoch) setBusy(false) }
}
async function revalidate (row: ConstructionWayline) {
  const token = ++epoch; const area = props.areaId; setBusy(true); error.value = ''; emit('preview', null)
  try {
    const result = await validateConstructionWayline(area, row.wayline_id)
    if (token !== epoch) return
    Object.assign(row, result); selected.value = row.wayline_id; selectedHeight.value = result.height_mode || ''; emit('preview', result)
  } catch (e) { if (token === epoch) error.value = message(e) } finally { if (token === epoch) setBusy(false) }
}
async function unlink (row: ConstructionWayline) {
  const area = props.areaId; const token = epoch
  try { await ElMessageBox.confirm(`解除“${row.name}”与该区域的关联？航线管理中的文件会保留。`, '解除关联') } catch { return }
  if (token !== epoch) return
  setBusy(true)
  try { await unlinkConstructionWayline(area, row.wayline_id); if (token === epoch) await load() } catch (e) { if (token === epoch) error.value = message(e) } finally { if (token === epoch) setBusy(false) }
}
watch(() => [props.areaId, props.version, props.enabled], load, { immediate: true })
onBeforeUnmount(() => { epoch++; libraryEpoch++; emit('busy', false) })
</script>

<style scoped>
.route-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.route-actions :deep(.el-button + .el-button) { margin-left: 0; }
.route-hint, .route-card p { color: #a8bdd8; font-size: 12px; line-height: 1.7; }
.route-error { color: #ffad8e !important; }
.route-card { border: 1px solid #294d79; border-radius: 5px; padding: 10px; margin-top: 12px; }
.route-card.selected { border-color: #39d9ff; background: #14345c; }
.route-select { width: 100%; border: 0; padding: 0; background: transparent; color: #e8f4ff; text-align: left; cursor: pointer; }
.route-select strong { display: block; overflow-wrap: anywhere; }
.route-select span { display: block; margin-top: 7px; color: #a8bdd8; font-size: 12px; }
</style>
