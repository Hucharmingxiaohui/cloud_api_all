<template>
  <Teleport v-if="ledgerTarget" :to="ledgerTarget">
    <div v-show="active" class="lines-shell">
      <header>
        <el-radio-group :model-value="ledgerTab" size="small" :disabled="editing || saving || routeBusy" @change="switchLedger"><el-radio-button label="lines">线路</el-radio-button><el-radio-button label="towers">杆塔</el-radio-button></el-radio-group>
        <h3>{{ ledgerTab === 'lines' ? `线路台账 · ${lines.length} 条` : `杆塔台账 · ${towers.length} 座` }}</h3>
        <div v-if="ledgerTab === 'lines'"><label><input v-model="archived" type="checkbox" :disabled="editing || saving" @change="load" /> 显示已归档</label><el-button :disabled="saving" type="primary" @click="newLine">＋ 新建线路</el-button></div>
        <div v-else><el-button :disabled="towerSaving" type="primary" @click="newTower">＋ 新建杆塔</el-button></div>
      </header>
      <aside v-if="ledgerTab === 'lines'">
        <el-input v-model="keyword" placeholder="搜索线路名称，按回车查询" clearable :disabled="editing || saving" @keyup.enter="load" />
        <el-radio-group v-model="enabled" :disabled="editing || saving" size="small" @change="load"><el-radio-button label="all">全部</el-radio-button><el-radio-button label="on">启用</el-radio-button><el-radio-button label="off">暂停</el-radio-button></el-radio-group>
        <p v-if="loadError" role="alert">{{ loadError }} <el-button :disabled="editing || saving" @click="load">重试</el-button></p>
        <p v-if="loading">加载中…</p>
        <p v-else-if="!lines.length">标定线路后，可上传或关联巡检航线</p>
        <article v-for="line in lines" :key="line.line_id" :class="{ selected: draft?.line_id === line.line_id }">
          <button class="line-select" :disabled="editing || saving" @click="select(line)"><strong>{{ line.name }}</strong><span>{{ line.type }} · {{ formatLength(line.length_m) }} · v{{ line.version }}</span><span>{{ line.enabled ? '启用' : '暂停' }}{{ line.archived ? ' · 已归档' : '' }}</span></button>
          <div><el-button link :disabled="editing || saving" @click="select(line, true)">编辑</el-button><el-button link :disabled="editing || saving" @click="history(line)">历史</el-button><el-button link :disabled="editing || saving" @click="archive(line)">{{ line.archived ? '恢复' : '归档' }}</el-button></div>
        </article>
      </aside>
      <aside v-else>
        <p v-if="towersError" role="alert">{{ towersError }} <el-button :disabled="towerSaving" @click="loadTowers">重试</el-button></p>
        <p v-if="towersLoading">加载中…</p>
        <p v-else-if="!towers.length">尚未标定杆塔，点击“新建杆塔”在地图上选点标定。</p>
        <article v-for="tower in towers" :key="tower.id" :class="{ selected: towerDraft?.id === tower.id }">
          <button class="line-select" :disabled="towerSaving || towerPicking" @click="editTower(tower)"><strong>{{ tower.tower_no }}</strong><span>经度 {{ tower.lng.toFixed(6) }} · 纬度 {{ tower.lat.toFixed(6) }}</span></button>
          <div><el-button link :disabled="towerSaving || towerPicking" @click="editTower(tower)">编辑</el-button><el-button link type="danger" :disabled="towerSaving || towerPicking" @click="removeTower(tower)">删除</el-button></div>
        </article>
      </aside>
    </div>
  </Teleport>
  <Teleport v-if="propertiesTarget" :to="propertiesTarget">
      <section v-show="active" class="line-properties">
        <template v-if="ledgerTab === 'towers'">
          <h3>{{ towerDraft?.id ? '编辑杆塔' : '新建杆塔' }}</h3>
          <p>在地图上单击标定杆塔位置；杆塔编号在同工作空间内唯一。</p>
          <template v-if="towerDraft">
            <el-form label-position="top" :disabled="towerSaving" @submit.prevent>
              <el-form-item label="杆塔编号"><el-input v-model="towerDraft.tower_no" :maxlength="32" placeholder="例如：A1" /></el-form-item>
            </el-form>
            <p v-if="Number.isFinite(towerDraft.lng) && Number.isFinite(towerDraft.lat)">位置：经度 {{ towerDraft.lng.toFixed(6) }} · 纬度 {{ towerDraft.lat.toFixed(6) }}</p>
            <p v-else>尚未标定位置，请点击“标定位置”在地图上选点。</p>
            <p v-if="towerError" role="alert">{{ towerError }}</p>
            <div><el-button v-if="towerPicking" type="primary" :disabled="!Number.isFinite(towerDraft.lng) || !Number.isFinite(towerDraft.lat)" @click="confirmTowerPicking">确定位置</el-button><el-button v-else :disabled="towerSaving" @click="beginTowerPicking">标定位置</el-button><el-button v-if="towerDraft.id" type="danger" :disabled="towerSaving" @click="removeTower(towerDraft)">删除</el-button><el-button type="primary" :loading="towerSaving" :disabled="towerPicking" @click="saveTower">保存杆塔</el-button></div>
          </template>
          <p v-else>选择左侧杆塔查看或编辑，或点击“新建杆塔”标定。</p>
        </template>
        <template v-else-if="draft">
          <h3>{{ historical ? '历史版本（只读）' : editing ? '编辑线路' : '线路详情' }} · v{{ draft.version }}</h3>
          <p>线路折点用于标定位置；巡检航线独立关联，不限制在多边形内。</p>
          <template v-if="!editing && !historical && draft.line_id">
            <el-button type="primary" :disabled="routeBusy || draft.archived" @click="analysisVisible = true">线路检测配置</el-button>
            <el-button :disabled="routeBusy" @click="router.push({ path: '/taskManage/construction', query: { area: 'line:' + draft.line_id } })">巡检计划</el-button>
            <el-radio-group v-model="inspectionTab" :disabled="routeBusy"><el-radio-button label="routes">巡检航线</el-radio-button><el-radio-button label="details">线路详情</el-radio-button></el-radio-group>
            <ConstructionLineWaylines v-if="inspectionTab === 'routes'" :area-id="'line:' + draft.line_id" :version="draft.version" :enabled="draft.enabled && !draft.archived" :initial-wayline="initialWayline" @preview="emit('preview', $event)" @busy="routeBusy = $event" />
          </template>
          <div v-show="editing || historical || inspectionTab === 'details'">
          <el-form label-position="top" :disabled="!editing || saving" @submit.prevent>
            <el-form-item label="线路名称"><el-input v-model="draft.name" :maxlength="100" /></el-form-item>
            <el-form-item label="线路类型"><el-select v-model="draft.type" filterable allow-create default-first-option><el-option v-for="type in types" :key="type" :value="type" :label="type" /></el-select></el-form-item>
            <el-form-item label="颜色"><el-select v-model="draft.color"><el-option v-for="color in colors" :key="color" :value="color" :label="color"><span :style="{ color }">● {{ color }}</span></el-option></el-select></el-form-item>
            <el-form-item label="启用"><el-switch v-model="draft.enabled" /></el-form-item>
            <el-form-item label="备注"><el-input v-model="draft.notes" type="textarea" :maxlength="500" /></el-form-item>
            <el-form-item v-if="editing" label="线路来源">
              <el-radio-group v-model="lineMode" :disabled="!editing || saving" @change="onLineModeChange"><el-radio-button label="draw">手绘折线</el-radio-button><el-radio-button label="tower">杆塔链</el-radio-button></el-radio-group>
            </el-form-item>
          </el-form>
          <template v-if="lineMode === 'tower'">
            <p>可跨编号前缀选择杆塔，按下方连接顺序生成折线；新勾选的杆塔追加到末尾。</p>
            <div class="tower-chain-list">
              <label v-for="tower in towers" :key="tower.id" class="tower-chain-item"><input type="checkbox" :checked="towerChain.includes(tower.id)" :disabled="!editing || saving" @change="toggleChainTower(tower.id)" /> {{ tower.tower_no }}</label>
            </div>
            <ol class="tower-chain-order">
              <li v-for="(id, index) in towerChain" :key="id">
                <span>{{ towers.find(tower => tower.id === id)?.tower_no || draft.tower_nos?.[index] || id }}</span>
                <el-button size="small" :disabled="!editing || saving || index === 0" @click="moveChainTower(index, -1)">上移</el-button>
                <el-button size="small" :disabled="!editing || saving || index === towerChain.length - 1" @click="moveChainTower(index, 1)">下移</el-button>
              </li>
            </ol>
            <p v-if="!towers.length">杆塔列表为空，请先在“杆塔”台账中标定杆塔。</p>
          </template>
          <template v-else>
            <p>折点 {{ draft.points.length }} 个 · WGS84</p>
            <p>{{ editing ? '预估长度' : '服务端长度' }}：{{ formatLength(editing ? lineLengthMeters(draft.points) : draft.length_m) }}</p>
          </template>
          <p>创建：{{ formatTime(draft.gmt_create) }}<br />修改：{{ formatTime(draft.gmt_modified) }}</p>
          <p v-if="saveError" role="alert">{{ saveError }}</p>
          <div v-if="editing"><el-button :disabled="saving" @click="cancel">取消</el-button><el-button type="primary" :loading="saving" :disabled="drawing || (lineMode === 'tower' && towerChain.length < 2)" @click="save">保存线路</el-button></div>
          <el-button v-else :disabled="saving" @click="historical ? select(draft) : startEdit()">{{ historical ? '返回当前版本' : '编辑线路' }}</el-button></div>
        </template>
        <p v-else>选择线路查看详情，或新建线路在地图上连续点击标定。标定线路后，可上传或关联巡检航线。</p>
      </section>
  </Teleport>
    <ConstructionAnalysisConfig v-if="analysisVisible && draft" :context="{ area_id: 'line:' + draft.line_id }" @close="analysisVisible = false" />
    <el-drawer v-model="historyVisible" title="线路版本历史（只读）" size="460px">
      <p v-if="historyLoading">加载中…</p><p v-if="historyError" role="alert">{{ historyError }}</p>
      <article v-for="row in historyRows" :key="row.version"><h4>v{{ row.version }} · {{ row.name }}</h4><p>{{ row.type }} · {{ row.enabled ? '启用' : '暂停' }}{{ row.archived ? ' · 已归档' : '' }} · {{ formatLength(row.length_m) }}</p><p>{{ formatTime(row.gmt_create) }}</p><el-button @click="viewHistory(row)">定位查看</el-button></article>
    </el-drawer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ConstructionLine, ConstructionPoint, ConstructionTower } from '/@/api/construction'
import { archiveConstructionLine, constructionLineHistory, createConstructionLine, getConstructionLine, listConstructionLines, updateConstructionLine, ConstructionAreaConflictError, listConstructionTowers, createConstructionTower, updateConstructionTower, deleteConstructionTower } from '/@/api/construction'
import { useRouter, useRoute } from 'vue-router'
import ConstructionLineWaylines from './ConstructionLineWaylines.vue'
import ConstructionAnalysisConfig from './ConstructionAnalysisConfig.vue'
import { lineLengthMeters, validateLine } from './constructionLineModel'
/* global defineProps, defineEmits, defineExpose */
defineProps<{ active: boolean; ledgerTarget: HTMLElement | null; propertiesTarget: HTMLElement | null }>()
const emit = defineEmits(['map-state', 'preview'])
const router = useRouter(); const route = useRoute()
const initialWayline = typeof route.query.wayline === 'string' ? route.query.wayline : ''
const routeBusy = ref(false); const analysisVisible = ref(false)
const inspectionTab = ref('routes')
const types = ['输电线路', '道路', '管线', '其他']
const colors = ['#FF9F43', '#F05A68', '#AC83FF', '#55D6A0', '#F2CA52']
const lines = ref<ConstructionLine[]>([]); const mapLines = ref<ConstructionLine[]>([])
const keyword = ref(''); const enabled = ref('all'); const archived = ref(false)
const loading = ref(false); const loadError = ref(''); const saveError = ref('')
const draft = ref<ConstructionLine | null>(null); const editing = ref(false); const saving = ref(false); const drawing = ref(false)
const historical = ref(false)
const ledgerTab = ref<'lines' | 'towers'>('lines')
const towers = ref<ConstructionTower[]>([]); const towersLoading = ref(false); const towersError = ref('')
const towerDraft = ref<{ id: string; tower_no: string; lng: number; lat: number } | null>(null)
const towerPicking = ref(false); const towerSaving = ref(false); const towerError = ref('')
const lineMode = ref<'draw' | 'tower'>('draw')
const towerChain = ref<string[]>([])
let baseline = ''; let sequence = 0; let disposed = false
const dirty = computed(() => drawing.value || (editing.value && (JSON.stringify(draft.value) !== baseline || JSON.stringify(towerChain.value) !== JSON.stringify(draft.value?.tower_ids || []))))
const historyVisible = ref(false); const historyLoading = ref(false); const historyError = ref(''); const historyRows = ref<ConstructionLine[]>([])
const message = (error: unknown) => error instanceof Error ? error.message : '线路操作失败'
function formatLength (value: number) { return value >= 1000 ? `${(value / 1000).toFixed(2)} km` : `${value.toFixed(1)} m` }
function formatTime (value?: string) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—' }
function apply (line: ConstructionLine) { emit('preview', null); analysisVisible.value = false; draft.value = JSON.parse(JSON.stringify(line)); draft.value!.notes = line.notes || ''; lineMode.value = (line.tower_ids && line.tower_ids.length) ? 'tower' : 'draw'; towerChain.value = line.tower_ids ? [...line.tower_ids] : []; baseline = JSON.stringify(draft.value); historical.value = false; saveError.value = '' }
async function confirmDiscard () {
  if (saving.value || routeBusy.value) return false
  if (!dirty.value) return true
  try { await ElMessageBox.confirm('当前线路修改尚未保存，离开将丢失本次修改。', '放弃未保存的修改？', { confirmButtonText: '放弃修改', cancelButtonText: '继续编辑', type: 'warning' }); return true } catch { return false }
}
const mapState = computed(() => ({
  lines: mapLines.value,
  selectedLineId: draft.value?.line_id || null,
  linePoints: editing.value && lineMode.value === 'tower' ? towerChain.value.flatMap(id => { const tower = towers.value.find(t => t.id === id); return tower ? [{ lng: tower.lng, lat: tower.lat }] : [] }) : draft.value?.points || [],
  lineEditable: editing.value && lineMode.value === 'draw',
  lineColor: draft.value?.color,
  lineName: draft.value?.name,
  towers: towers.value,
  selectedTowerId: towerDraft.value?.id || null,
  towerPicking: towerPicking.value,
  disabled: saving.value || routeBusy.value
}))
watch(mapState, value => emit('map-state', value), { immediate: true, deep: true })
function setDrawing (value: boolean) { drawing.value = value }
function deactivate () {
  emit('preview', null); analysisVisible.value = false
  const discard = dirty.value
  sequence++; editing.value = false; drawing.value = false; historyVisible.value = false
  if (discard && draft.value) {
    const current = mapLines.value.find(line => line.line_id === draft.value?.line_id)
    if (current) apply(current)
    else draft.value = null
  }
}
defineExpose({ confirmDiscard, mapState, onSelectLine, updatePoints, setDrawing, deactivate, onSelectTower, onPickTower, cancelTowerPicking })
async function load () {
  loading.value = true; loadError.value = ''
  try {
    const [ledger, allLines] = await Promise.all([listConstructionLines({ keyword: keyword.value.trim() || undefined, enabled: enabled.value === 'all' ? null : enabled.value === 'on', archived: archived.value }), listConstructionLines()])
    if (disposed) return
    lines.value = ledger; mapLines.value = allLines.filter(line => !line.archived)
  } catch (error) { loadError.value = message(error) } finally { loading.value = false }
}
async function loadTowers () {
  towersLoading.value = true; towersError.value = ''
  try { towers.value = await listConstructionTowers() } catch (error) { towersError.value = message(error) } finally { towersLoading.value = false }
}
function switchLedger (tab: 'lines' | 'towers') {
  if (tab === ledgerTab.value || saving.value || routeBusy.value) return
  ledgerTab.value = tab
  emit('preview', null); analysisVisible.value = false
  if (tab === 'towers') loadTowers()
}
function newTower () { towerDraft.value = { id: '', tower_no: '', lng: NaN, lat: NaN }; towerError.value = '' }
function editTower (tower: ConstructionTower) { towerDraft.value = { id: tower.id, tower_no: tower.tower_no, lng: tower.lng, lat: tower.lat }; towerError.value = '' }
function onSelectTower (id: string) { if (towerSaving.value || towerPicking.value) return; const tower = towers.value.find(item => item.id === id); if (tower) editTower(tower) }
function onPickTower (point: ConstructionPoint) { if (towerDraft.value) { towerDraft.value.lng = point.lng; towerDraft.value.lat = point.lat; towerError.value = '' } }
function beginTowerPicking () { if (towerDraft.value && !towerSaving.value) { towerError.value = ''; towerPicking.value = true } }
function confirmTowerPicking () {
  if (!towerDraft.value || !Number.isFinite(towerDraft.value.lng) || !Number.isFinite(towerDraft.value.lat)) return
  towerPicking.value = false
}
function cancelTowerPicking () { towerPicking.value = false }
function onLineModeChange () {
  if (lineMode.value === 'tower' && !towers.value.length) loadTowers()
  if (lineMode.value === 'draw') towerChain.value = []
}
async function saveTower () {
  if (!towerDraft.value || towerSaving.value || towerPicking.value) return
  const d = towerDraft.value
  if (!d.tower_no.trim()) { towerError.value = '请填写杆塔编号'; return }
  if (!Number.isFinite(d.lng) || !Number.isFinite(d.lat)) { towerError.value = '请在地图上标定杆塔位置'; return }
  towerSaving.value = true; towerError.value = ''
  try {
    const body = { tower_no: d.tower_no.trim(), lng: d.lng, lat: d.lat }
    if (d.id) await updateConstructionTower(d.id, body)
    else await createConstructionTower(body)
    towerDraft.value = null; await loadTowers()
    ElMessage.success(d.id ? '杆塔已更新' : '杆塔已保存')
  } catch (error) { towerError.value = message(error) } finally { towerSaving.value = false }
}
async function removeTower (tower: { id: string; tower_no: string }) {
  if (towerSaving.value || towerPicking.value) return
  try { await ElMessageBox.confirm(`删除杆塔“${tower.tower_no}”？被线路引用的杆塔无法删除。`, '删除杆塔', { type: 'warning' }) } catch { return }
  towerSaving.value = true; towerError.value = ''
  try { await deleteConstructionTower(tower.id); if (towerDraft.value?.id === tower.id) towerDraft.value = null; await loadTowers(); ElMessage.success('杆塔已删除') } catch (error) { towerError.value = message(error) } finally { towerSaving.value = false }
}
function toggleChainTower (id: string) {
  if (!editing.value || saving.value) return
  const index = towerChain.value.indexOf(id)
  if (index >= 0) towerChain.value.splice(index, 1)
  else towerChain.value.push(id)
}
function moveChainTower (index: number, direction: number) {
  const next = index + direction
  if (!editing.value || saving.value || index < 0 || next < 0 || next >= towerChain.value.length) return
  const ids = [...towerChain.value]
  ;[ids[index], ids[next]] = [ids[next], ids[index]]
  towerChain.value = ids
}
async function newLine () {
  if (!await confirmDiscard()) return
  sequence++; lineMode.value = 'draw'; towerChain.value = []
  apply({ line_id: '', version: 0, name: '', type: '其他', color: colors[0], enabled: true, notes: '', archived: false, points: [], length_m: 0 }); editing.value = true
}
async function select (line: ConstructionLine, edit = false) {
  if (saving.value || !await confirmDiscard()) return
  const token = ++sequence
  try { const record = await getConstructionLine(line.line_id); if (disposed || token !== sequence) return; apply(record); editing.value = edit } catch (error) { ElMessage.error(message(error)) }
}
async function onSelectLine (id: string) { if (editing.value || saving.value || routeBusy.value) return; await select(mapLines.value.find(item => item.line_id === id) || { line_id: id } as ConstructionLine) }
function updatePoints (points: ConstructionPoint[]) { if (draft.value && editing.value && !saving.value) draft.value.points = points }
function startEdit () { if (draft.value && !saving.value && !routeBusy.value) { baseline = JSON.stringify(draft.value); editing.value = true; emit('preview', null) } }
async function cancel () {
  if (!await confirmDiscard()) return
  const id = draft.value?.line_id; editing.value = false; drawing.value = false
  if (!id) { draft.value = null; return }
  try { apply(await getConstructionLine(id)) } catch (error) { ElMessage.error(message(error)); draft.value = null }
}
async function save () {
  if (!draft.value || !editing.value || saving.value || drawing.value) return
  const value = draft.value
  const error = lineMode.value === 'tower' ? (towerChain.value.length < 2 ? '请选择至少两个杆塔组成杆塔链' : '') : validateLine(value.points)
  if (error || !value.name.trim() || !value.type.trim() || value.type.trim().length > 32) { saveError.value = error || '名称必填；类型为1至32字'; return }
  saving.value = true; saveError.value = ''
  try {
    const common = { name: value.name.trim(), type: value.type.trim(), color: value.color, enabled: value.enabled, notes: value.notes }
    const body = lineMode.value === 'tower' ? { ...common, tower_ids: towerChain.value } : { ...common, points: value.points }
    const saved = value.line_id ? await updateConstructionLine(value.line_id, { ...body, expected_version: value.version }) : await createConstructionLine(body)
    apply(saved); editing.value = false; ElMessage.success(`线路已保存（v${saved.version}）`); await load()
  } catch (cause) {
    saveError.value = message(cause)
    if (cause instanceof ConstructionAreaConflictError) {
      try { await ElMessageBox.confirm(cause.message, '版本冲突', { confirmButtonText: '载入最新线路', cancelButtonText: '保留草稿', type: 'warning' }); if (draft.value?.line_id) { apply(await getConstructionLine(draft.value.line_id)); editing.value = false }; await load() } catch { /* Keep the unsaved draft on cancellation. */ }
    }
  } finally { saving.value = false }
}
async function archive (line: ConstructionLine) {
  if (editing.value || saving.value || routeBusy.value) return
  try { await ElMessageBox.confirm(`${line.archived ? '恢复' : '归档'}“${line.name}”？历史版本将保留。`, '线路归档', { type: 'warning' }) } catch { return }
  saving.value = true
  try {
    const saved = await archiveConstructionLine(line.line_id, !line.archived, line.version)
    if (draft.value?.line_id === line.line_id) {
      if (saved.archived === archived.value) apply(saved)
      else {
        sequence++; draft.value = null; baseline = ''; historical.value = false
        analysisVisible.value = false; historyVisible.value = false; saveError.value = ''
        emit('preview', null)
      }
    }
    mapLines.value = mapLines.value.filter(item => item.line_id !== saved.line_id)
    lines.value = lines.value.filter(item => item.line_id !== saved.line_id)
    await load()
    ElMessage.success(saved.archived ? '线路已归档，可在已归档列表中恢复' : '线路已恢复')
  } catch (error) { ElMessage.error(message(error)) } finally { saving.value = false }
}
async function history (line: ConstructionLine) {
  if (editing.value || saving.value || routeBusy.value) return
  historyRows.value = []; historyError.value = ''; historyLoading.value = true; historyVisible.value = true
  try { historyRows.value = await constructionLineHistory(line.line_id) } catch (error) { historyError.value = message(error) } finally { historyLoading.value = false }
}
function viewHistory (line: ConstructionLine) { apply(line); historical.value = true; editing.value = false; historyVisible.value = false }
function beforeUnload (event: BeforeUnloadEvent) { if (dirty.value) { event.preventDefault(); event.returnValue = '' } }
onMounted(() => { window.addEventListener('beforeunload', beforeUnload); load() })
onBeforeUnmount(() => { disposed = true; sequence++; window.removeEventListener('beforeunload', beforeUnload) })
</script>

<style scoped>
.lines-shell { display:flex; flex-direction:column; min-height:0; height:100%; }
header { display:flex; flex-direction:column; align-items:flex-start; gap:12px; margin-bottom:12px; }
header>div { display:flex; align-items:center; gap:12px; }
aside,.line-properties { padding:12px; background:#0b2756; overflow:auto; }
aside { display:flex; flex-direction:column; gap:10px; }
article { padding:10px; background:#102c54; border:1px solid #36547a; border-radius:4px; }
article.selected { border-color:#ff9f43; }
.line-select { display:flex; flex-direction:column; gap:6px; width:100%; text-align:left; border:0; color:inherit; background:transparent; cursor:pointer; }
.line-select span,p { font-size:12px; color:#b6c9e0; line-height:1.7; overflow-wrap:anywhere; }
[role=alert] { color:#ffb599; }
.line-properties :deep(.el-select) { width:100%; }
</style>
