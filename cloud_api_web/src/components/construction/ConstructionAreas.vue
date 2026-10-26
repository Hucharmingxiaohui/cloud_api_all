<template>
  <main class="construction-shell">
    <div class="page-heading">
      <div>
        <h2>施工区域</h2>
        <p>围绕机巢划定施工区域，管理巡检边界与历史版本。</p>
      </div>
      <div class="heading-actions">
        <el-checkbox v-model="showArchived" size="small" @change="load">显示已归档</el-checkbox>
        <el-button type="primary" @click="newArea">＋ 新建区域</el-button>
      </div>
    </div>
    <div class="area-layout">
      <aside class="panel area-list">
        <div class="panel-heading"><h3>区域台账</h3><span>{{ areas.length }} 个区域</span></div>
        <el-input v-model="keyword" placeholder="搜索区域名称，按回车查询" clearable aria-label="搜索区域名称" @keyup.enter="load" />
        <el-radio-group v-model="enabledFilter" size="small" class="filter-pills" aria-label="区域状态" @change="load">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="on">启用</el-radio-button>
          <el-radio-button label="off">暂停</el-radio-button>
        </el-radio-group>
        <div v-if="loading" class="list-status">加载中…</div>
        <div v-else-if="!areas.length" class="list-status">暂无区域，请点击“新建区域”在地图上标定。</div>
        <div v-else class="area-cards">
          <button v-for="area in areas" :key="area.id" type="button" class="area-card" :class="{ selected: draft?.id === area.id }" @click="selectArea(area)">
            <div class="area-card-top"><span class="badge" :class="area.enabled ? 'success' : 'muted'">{{ area.enabled ? '启用' : '暂停' }}</span><span v-if="area.archived" class="badge muted">已归档</span><span class="badge info">v{{ area.version }}</span></div>
            <strong class="area-name">{{ area.name }}</strong>
            <p class="area-meta">{{ area.category }} · {{ formatSqm(area.area_sqm) }}</p>
            <div class="area-actions" @click.stop>
              <el-button size="small" link type="primary" @click="editArea(area)">编辑</el-button>
              <el-button size="small" link @click="openHistory(area)">历史</el-button>
              <el-button size="small" link :type="area.archived ? 'success' : 'danger'" @click="toggleArchive(area)">{{ area.archived ? '恢复' : '归档' }}</el-button>
            </div>
          </button>
        </div>
      </aside>
      <section class="panel area-editor">
        <ConstructionAreaMap
          :areas="areas"
          :selected-id="draft?.id ?? null"
          :boundary="draft?.boundary ?? []"
          :editable="editing"
          :disabled="saving"
          :docks="docks"
          :dock-error="dockError"
          :docks-loading="docksLoading"
          :wayline="routePreview"
          @refresh-docks="refreshDocks"
          @drawing="drawing = $event"
          @select="onMapSelect"
          @update:boundary="value => { if (draft) draft.boundary = value }"
        />
        <div class="property-panel">
          <el-radio-group v-if="draft?.id && !editing" v-model="detailTab" size="small" class="detail-tabs"><el-radio-button label="routes">区域航线</el-radio-button><el-radio-button label="detail">区域详情</el-radio-button></el-radio-group>
          <ConstructionAreaWaylines v-if="draft?.id && !editing && detailTab === 'routes'" :key="draft.id" :area-id="draft.id" :version="draft.version" :enabled="draft.enabled && !areas.find(a => a.id === draft?.id)?.archived" :initial-wayline="String(route.query.wayline || '')" @preview="routePreview = $event" @busy="routeBusy = $event" />
          <template v-else>
          <div class="panel-heading"><h3>{{ editing ? (draft?.id ? '编辑区域' : '新建区域') : '区域详情' }}</h3><span v-if="draft?.id" class="badge info">版本 v{{ draft.version }}</span></div>
          <template v-if="draft">
            <el-form label-position="top" :disabled="!editing || saving" @submit.prevent>
              <el-form-item label="区域名称"><el-input v-model="draft.name" :maxlength="100" placeholder="例如：1号风机 · 基础施工区" /></el-form-item>
              <el-form-item label="施工类型"><el-select v-model="draft.category"><el-option v-for="category in categories" :key="category" :value="category" :label="category" /></el-select></el-form-item>
              <el-form-item label="允许创建巡检计划"><el-switch v-model="draft.enabled" /></el-form-item>
              <el-form-item label="备注"><el-input v-model="draft.notes" type="textarea" :rows="2" :maxlength="500" placeholder="选填" /></el-form-item>
            </el-form>
            <div class="boundary-summary">
              <span>边界顶点：{{ draft.boundary.length }} 个</span>
              <span>面积：{{ draft.boundary.length >= 3 ? formatSqm(areaSquareMeters(draft.boundary)) : '—' }}</span>
              <span class="muted">坐标 WGS84</span>
            </div>
            <p v-if="boundaryError" class="field-error" role="alert">{{ boundaryError }}</p>
            <div class="property-actions">
              <el-button v-if="editing" @click="cancelEdit">取消</el-button>
              <el-button v-if="editing" type="primary" :loading="saving" :disabled="drawing" @click="save">保存区域</el-button>
              <el-button v-else type="primary" @click="startEdit">编辑区域</el-button>
            </div>
          </template>
          <div v-else class="property-empty"><span>◇</span><h4>选择一个施工区域</h4><p>从左侧台账选择区域查看详情，或点击“新建区域”在地图上绘制边界。</p><el-button type="primary" plain @click="newArea">新建区域</el-button></div>
          </template>
        </div>
      </section>
    </div>

    <el-drawer v-model="historyVisible" title="版本历史" size="420px">
      <div v-if="historyLoading" class="list-status">加载中…</div>
      <el-table v-else :data="historyRows" size="small">
        <el-table-column label="版本" width="80"><template #default="{ row }">v{{ row.version }}</template></el-table-column>
        <el-table-column label="面积" min-width="110"><template #default="{ row }">{{ formatSqm(row.area_sqm) }}</template></el-table-column>
        <el-table-column label="保存时间" min-width="160"><template #default="{ row }">{{ formatTime(row.gmt_create) }}</template></el-table-column>
      </el-table>
    </el-drawer>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute } from 'vue-router'
import ConstructionAreaWaylines from './ConstructionAreaWaylines.vue'
import ConstructionAreaMap from './ConstructionAreaMap.vue'
import { useConstructionDocks } from './useConstructionDocks'
import { areaSquareMeters, validateBoundary } from './constructionModel'
import {
  archiveConstructionArea, constructionAreaHistory, createConstructionArea, getConstructionArea,
  listConstructionAreas, updateConstructionArea, ConstructionAreaConflictError
} from '/@/api/construction'
import type { ConstructionAreaRecord, ConstructionAreaVersionRecord, ConstructionPoint, ConstructionWayline } from '/@/api/construction'

const route = useRoute()
const detailTab = ref('routes')
const routePreview = ref<ConstructionWayline | null>(null)
const routeBusy = ref(false)

const categories = ['基础施工', '吊装作业', '道路施工', '材料堆场', '其他施工']
const { docks, error: dockError, loading: docksLoading, refresh: refreshDocks } = useConstructionDocks()
const drawing = ref(false)

const areas = ref<ConstructionAreaRecord[]>([])
const keyword = ref('')
const enabledFilter = ref<'all' | 'on' | 'off'>('all')
const showArchived = ref(false)
const loading = ref(false)
const saving = ref(false)
const editingMode = ref(false)
const draft = ref<{ id: string | null; name: string; category: string; enabled: boolean; notes: string; version: number; boundary: ConstructionPoint[] } | null>(null)
let baseline = ''
let selectionSequence = 0

const historyVisible = ref(false)
const historyLoading = ref(false)
const historyRows = ref<ConstructionAreaVersionRecord[]>([])

const editing = computed(() => !!draft.value && editingMode.value)
const dirty = computed(() => drawing.value || (!!draft.value && JSON.stringify({ ...draft.value }) !== baseline))
const boundaryError = computed(() => (draft.value && draft.value.boundary.length ? validateBoundary(draft.value.boundary) : ''))
function formatSqm (value?: number) {
  if (!value || value <= 0) return '—'
  return value >= 10000 ? `${(value / 10000).toFixed(2)} 公顷` : `${Math.round(value)} ㎡`
}
function formatTime (value?: string) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN', { hour12: false })
}
function markBaseline () { baseline = draft.value ? JSON.stringify({ ...draft.value }) : '' }
async function load () {
  loading.value = true
  try {
    areas.value = await listConstructionAreas({
      keyword: keyword.value.trim() || undefined,
      enabled: enabledFilter.value === 'all' ? null : enabledFilter.value === 'on',
      archived: showArchived.value
    })
  } catch (error) { fail(error) } finally { loading.value = false }
}
async function confirmDiscard () {
  if (routeBusy.value) { ElMessage.info('航线操作正在处理，请稍候'); return false }
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('当前修改尚未保存，离开将丢失本次修改。', '放弃未保存的修改？', { confirmButtonText: '放弃修改', cancelButtonText: '继续编辑', type: 'warning' })
    return true
  } catch { return false }
}
function blankDraft () {
  selectionSequence++; routePreview.value = null
  draft.value = { id: null, name: '', category: categories[0], enabled: true, notes: '', version: 0, boundary: [] }
  markBaseline()
  editingMode.value = true
}
async function newArea () {
  if (!await confirmDiscard()) return
  blankDraft()
}
function applyRecord (record: ConstructionAreaRecord) {
  routePreview.value = null
  draft.value = {
    id: record.id,
    name: record.name,
    category: record.category,
    enabled: record.enabled,
    notes: record.notes ?? '',
    version: record.version,
    boundary: record.boundary ?? []
  }
  markBaseline()
}
async function selectArea (area: ConstructionAreaRecord) {
  const sequence = ++selectionSequence
  if (draft.value && draft.value.id === area.id && !editing.value) return
  if (!await confirmDiscard()) return
  try { const record = await getConstructionArea(area.id); if (sequence !== selectionSequence) return; applyRecord(record); editingMode.value = false } catch (error) { if (sequence === selectionSequence) fail(error) }
}
async function editArea (area: ConstructionAreaRecord) {
  const sequence = ++selectionSequence
  if (!await confirmDiscard()) return
  try { const record = await getConstructionArea(area.id); if (sequence !== selectionSequence) return; applyRecord(record); editingMode.value = true } catch (error) { if (sequence === selectionSequence) fail(error) }
}
function onMapSelect (id: string) {
  const area = areas.value.find(item => item.id === id)
  if (area) selectArea(area)
}
function startEdit () { editingMode.value = true }
async function cancelEdit () {
  if (!draft.value) return
  const id = draft.value.id
  if (!await confirmDiscard()) return
  if (!id) { draft.value = null; editingMode.value = false; return }
  try { applyRecord(await getConstructionArea(id)) } catch (error) { fail(error) } finally { editingMode.value = false }
}
async function save () {
  if (!draft.value) return
  const error = validateBoundary(draft.value.boundary)
  if (error) { ElMessage.error(error); return }
  if (!draft.value.name.trim()) { ElMessage.error('请填写区域名称'); return }
  saving.value = true
  try {
    const body = {
      name: draft.value.name.trim(),
      category: draft.value.category,
      enabled: draft.value.enabled,
      notes: draft.value.notes,
      boundary: draft.value.boundary
    }
    const saved = draft.value.id
      ? await updateConstructionArea(draft.value.id, { ...body, expected_version: draft.value.version })
      : await createConstructionArea(body)
    ElMessage.success(`区域已保存（v${saved.version}）`)
    applyRecord(saved)
    editingMode.value = false
    await load()
  } catch (cause) {
    if (cause instanceof ConstructionAreaConflictError) {
      try {
        await ElMessageBox.confirm(`${cause.message}`, '版本冲突', { confirmButtonText: '载入最新区域', cancelButtonText: '暂不处理', type: 'warning' })
        if (draft.value?.id) applyRecord(await getConstructionArea(draft.value.id))
        await load()
      } catch { /* user chose to keep editing stale draft */ }
    } else fail(cause)
  } finally { saving.value = false }
}
async function toggleArchive (area: ConstructionAreaRecord) {
  const next = !area.archived
  try {
    await ElMessageBox.confirm(
      next ? `归档“${area.name}”后默认不再显示，历史版本保留，可随时恢复。` : `恢复“${area.name}”到区域台账？`,
      next ? '归档区域' : '恢复区域',
      { confirmButtonText: next ? '归档' : '恢复', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  try {
    await archiveConstructionArea(area.id, next)
    ElMessage.success(next ? '已归档' : '已恢复')
    if (draft.value?.id === area.id && next) { draft.value = null; editingMode.value = false }
    await load()
  } catch (error) { fail(error) }
}
async function openHistory (area: ConstructionAreaRecord) {
  historyVisible.value = true
  historyLoading.value = true
  try { historyRows.value = await constructionAreaHistory(area.id) } catch (error) { fail(error) } finally { historyLoading.value = false }
}
function fail (error: unknown) { ElMessage.error(error instanceof Error ? error.message : '操作失败，请重试') }

onBeforeRouteLeave(confirmDiscard)
onBeforeRouteUpdate(confirmDiscard)
function beforeUnload (event: BeforeUnloadEvent) {
  if (dirty.value) { event.preventDefault(); event.returnValue = '' }
}
window.addEventListener('beforeunload', beforeUnload)
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload))
watch([editing, detailTab], () => { routePreview.value = null })
load().then(async () => {
  const id = route.query.area
  if (typeof id === 'string') {
    try { applyRecord(await getConstructionArea(id)); detailTab.value = 'routes' } catch (error) { fail(error) }
  }
})
refreshDocks()
</script>

<style scoped>
.construction-shell { height: 100%; min-height: 0; padding: 16px 18px 20px; box-sizing: border-box; display: flex; flex-direction: column; overflow: auto; color: #dce9fb; background: #061c3e; --el-text-color-primary: #e7f0ff; --el-text-color-regular: #c2d4ed; --el-text-color-placeholder: #8199b8; --el-fill-color-blank: #102b50; --el-fill-color-light: #16365e; --el-bg-color: #102b50; --el-border-color: #2b4b73; --el-disabled-bg-color: #0e2546; --el-disabled-text-color: #b0c4df; }
.construction-shell :deep(*) { box-sizing: border-box; }
.page-heading { flex-shrink: 0; padding: 2px 0 4px; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.page-heading h2 { margin: 0; font-size: 18px; color: #f1f6ff; }
.page-heading p { margin: 4px 0 0; font-size: 12px; color: #a8bdd8; }
.heading-actions { display: flex; align-items: center; gap: 12px; }
.area-layout { flex: 1; display: grid; grid-template-columns: 260px minmax(0, 1fr); gap: 14px; min-height: 520px; }
.panel { background: #0b2756; border: 1px solid #23436e; border-radius: 4px; padding: 12px; }
.area-list { display: flex; flex-direction: column; gap: 10px; min-height: 0; overflow: hidden; }
.panel-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.panel-heading h3 { margin: 0; font-size: 14px; color: #f1f6ff; }
.panel-heading span { font-size: 12px; color: #a8bdd8; }
.filter-pills { margin-top: 2px; }
.list-status { padding: 18px 0; text-align: center; font-size: 12px; color: #a8bdd8; }
.area-cards { display: flex; flex-direction: column; gap: 8px; overflow: auto; min-height: 0; }
.area-card { padding: 10px; text-align: left; background: #102c54; border: 1px solid #23436e; border-radius: 4px; color: inherit; cursor: pointer; }
.area-card.selected { border-color: #409eff; box-shadow: 0 0 0 1px #409eff inset; }
.area-card-top { display: flex; gap: 6px; margin-bottom: 6px; }
.area-name { display: block; font-size: 13px; color: #f1f6ff; }
.area-meta { margin: 4px 0 0; font-size: 12px; color: #a8bdd8; }
.area-actions { display: flex; gap: 2px; margin-top: 6px; }
.badge { padding: 1px 8px; border-radius: 10px; font-size: 11px; }
.badge.success { color: #8dff6b; background: #16351f; }
.badge.muted { color: #a8bdd8; background: #1b3357; }
.badge.info { color: #8fcfff; background: #12335e; }
.area-editor { display: grid; grid-template-columns: minmax(0, 1fr) 270px; gap: 0; min-height: 0; padding: 0; overflow: hidden; }
.property-panel { border-left: 1px solid #23436e; padding: 18px 16px; min-height: 0; overflow: auto; }
.detail-tabs { margin-bottom: 18px; }
.property-empty { padding: 50px 6px 10px; text-align: center; color: #9bb2d0; line-height: 1.8; }
.property-empty > span { display: block; font-size: 44px; color: #5b9fd9; }
.property-empty h4 { color: #deebff; font-size: 14px; }
.property-empty p { font-size: 12px; margin: 12px 0 20px; }
.property-panel :deep(.el-select) { width: 100%; }
.boundary-summary { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 8px; font-size: 12px; color: #d4e2f5; }
.boundary-summary .muted { color: #a8bdd8; }
.field-error { margin: 0 0 8px; font-size: 12px; color: #ff9f80; }
.property-actions { display: flex; justify-content: flex-end; gap: 8px; }
@media (max-width: 1200px) {
  .area-layout { grid-template-columns: 220px minmax(0, 1fr); }
  .area-editor { grid-template-columns: minmax(0, 1fr) 230px; }
}
@media (max-width: 960px) {
  .area-editor { grid-template-columns: 1fr; grid-template-rows: minmax(360px, 1fr) auto; }
  .property-panel { border-left: 0; border-top: 1px solid #23436e; max-height: 240px; }
  .property-empty { padding: 4px; }
  .property-empty > span { display: none; }
}
@media (max-width: 640px) {
  .page-heading { align-items: flex-start; flex-direction: column; }
  .area-layout { display: flex; flex-direction: column; flex: none; }
  .area-list { max-height: 240px; flex-shrink: 0; }
  .area-editor { min-height: 580px; }
}
</style>
