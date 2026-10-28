<template>
  <main class="overall-page">
    <header><div><h2>工地总体航线与计划</h2><p>一条航线巡检多个区域，按航点归类照片；转场航点可不绑定区域。</p></div><div class="tools"><el-button :disabled="busy" @click="router.push('/taskManage/construction/plans')">区域计划</el-button><el-button :disabled="busy || editing" @click="refresh">刷新</el-button><el-button :disabled="busy || editing" @click="router.push('/wayline-panel')">航线管理</el-button></div></header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <div class="overall-layout">
      <aside>
        <h3>总体航线</h3><p v-if="!sn">请先在地图选择机巢</p>
        <el-button type="primary" :disabled="!sn || busy || editing || formVisible" @click="openLibrary">从航线库添加</el-button>
        <el-button class="upload-overall" :loading="uploading" :disabled="!sn || busy || editing || formVisible || libraryVisible" @click="fileInput?.click()">上传 KMZ</el-button>
        <input ref="fileInput" type="file" accept=".kmz" hidden aria-label="上传总体航线 KMZ" @change="uploadKmz" />
        <p v-if="uploaded" class="upload-receipt">{{ uploaded.name }} 已保存到航线库。{{ editing ? '请完成工作面航点绑定。' : '尚未完成本次绑定，可继续解析。' }}<el-button v-if="!editing" link type="primary" :disabled="busy || formVisible" @click="inspect(uploaded.id)">重新解析</el-button></p>
        <p v-if="sn && !rows.length">当前机巢尚未配置总体航线</p>
        <button v-for="row in rows" :key="row.overall_id" class="route-card" :class="{ active: selected?.overall_id === row.overall_id }" :disabled="busy || editing || formVisible" @click="choose(row)"><strong>{{ row.name }}</strong><span>{{ row.bindings.length }} 个区域 · v{{ row.version }}</span><span :class="{ error: row.status !== 'VALID' }">{{ row.message }}</span></button>
        <h3>完全覆盖区域</h3><button v-for="area in coveredAreas" :key="area.id" class="area-card" :disabled="busy || formVisible" @click="areaId = area.id">{{ area.name }}</button>
      </aside>
      <section class="main-content">
        <div class="overall-map"><ConstructionAreaMap :areas="mapAreas" :selected-id="areaId || null" :boundary="selectedArea?.boundary || []" :editable="false" :disabled="busy || formVisible" :coverage-locked="editing || formVisible || libraryVisible" :docks="docks" :dock-error="dockError" :docks-loading="docksLoading" :active-dock-sn="sn" :wayline="preview" :selected-waypoints="checked" @coverage-change="coverageChanged" @select="areaId = $event" @select-waypoint="togglePoint" @refresh-docks="refreshDocks" /></div>
        <section v-if="draft" class="bindings-panel">
          <p v-if="draft.source_wayline_id" class="execution-notice">当前为平台补齐拍照标识的执行版，上传原件已保留。保存绑定后，计划将使用此执行版。</p>
          <div class="tools"><h3>{{ draft.name }} · {{ editing ? '编辑航点绑定' : '区域航点绑定' }}</h3><el-button v-if="!editing" :disabled="busy || formVisible" @click="edit">编辑 / 重新校验绑定</el-button></div>
          <template v-if="editing">
            <p>选择区域，勾选列表或点击地图航点后批量绑定。仅支持有唯一照片后缀的到点拍照；一个航点归属一个区域。</p>
            <p v-if="draft.points.length && !draft.points.some(p => eligible(p.index))" class="error" role="alert">当前没有可绑定的航点。请查看下表“绑定条件”：没有识别到后缀不等于没有拍照，可能是拍照动作未设置命名后缀；不同航点使用相同后缀也无法区分照片归属。</p>
            <div class="tools"><el-select v-model="areaId" placeholder="选择完全覆盖的区域"><el-option v-for="area in coveredAreas" :key="area.id" :value="area.id" :label="area.name" /></el-select><el-button :disabled="!areaId || !checked.length || busy" @click="assign">绑定选中航点</el-button><el-button :disabled="!checked.length || busy" @click="unassign">取消选中航点绑定</el-button><el-button :disabled="busy" @click="suggest">按位置预选当前区域航点</el-button></div>
          </template>
          <p v-if="!editing">{{ draft.bindings.map(b => `${b.area_name || areaName(b.area_id)}：航点${b.waypoints.join('、')}`).join('；') }}</p>
          <el-table v-if="editing" :data="draft.points" max-height="260" row-key="index">
            <el-table-column v-if="editing" width="65"><template #default="{ row }"><el-tooltip :content="busy ? '正在处理，请稍候' : bindingIssue(row.index) || '勾选此航点，然后点击绑定选中航点'" placement="right"><span><el-checkbox :model-value="checked.includes(row.index)" :aria-label="'选择航点 ' + row.index" :disabled="busy || !eligible(row.index)" @change="togglePoint(row.index)" /></span></el-tooltip></template></el-table-column>
            <el-table-column prop="index" label="航点" width="75" />
            <el-table-column label="拍照后缀" min-width="160"><template #default="{ row }">{{ row.photo_suffixes.join('、') || '未识别到拍照后缀' }}</template></el-table-column>
            <el-table-column label="绑定条件" min-width="230"><template #default="{ row }"><span :class="{ error: !!bindingIssue(row.index) }">{{ bindingIssue(row.index) || '可绑定' }}</span></template></el-table-column>
            <el-table-column label="绑定巡检区域" min-width="180"><template #default="{ row }">{{ areaName(assignments[row.index]) }}</template></el-table-column>
          </el-table>
          <div v-if="editing" class="tools"><el-button type="primary" :disabled="busy || !Object.values(assignments).some(Boolean)" @click="save">保存绑定</el-button><el-button :disabled="busy" @click="cancelEdit">取消编辑</el-button><span>保存新版本后，旧计划需按新绑定重新创建。</span></div>
        </section>
        <section v-if="selected && !editing" class="plans-panel">
          <div class="tools"><h3>总体巡检计划</h3><el-button type="primary" :disabled="busy || !currentCovered || selected.status !== 'VALID'" @click="formVisible = true">新建总体计划</el-button><el-checkbox v-model="archived" :disabled="busy" @change="loadPlans">显示已归档</el-checkbox></div>
          <p v-if="!currentCovered" class="error">当前机巢未完全覆盖此航线及绑定区域，请检查定位或已保存的范围。</p>
          <p>下发会执行整条总体航线；各区域共用同一次飞行任务和结果。</p>
          <el-table :data="plans" max-height="320" empty-text="暂无总体计划">
            <el-table-column prop="name" label="计划名称" min-width="160" />
            <el-table-column label="执行方式" min-width="170"><template #default="{ row }">{{ row.task_type === 1 ? new Date(row.begin_time).toLocaleString() : '立即任务' }}</template></el-table-column>
            <el-table-column prop="message" label="状态" min-width="210" />
            <el-table-column label="操作" width="240"><template #default="{ row }"><el-button link type="primary" :disabled="busy || !currentCovered || row.status !== 'READY'" @click="dispatch(row)">下发任务</el-button><el-button v-if="['UNKNOWN', 'SUBMITTING'].includes(row.last_dispatch?.status)" link :disabled="busy" @click="reconcile(row)">查询下发</el-button><el-button link :disabled="busy || !!row.archived" @click="archive(row)">归档</el-button></template></el-table-column>
          </el-table>
        </section>
      </section>
    </div>
    <el-dialog v-model="libraryVisible" title="选择总体航线" width="720px" append-to-body :close-on-click-modal="false" :show-close="!busy" :close-on-press-escape="!busy">
      <p>选择航线库已有文件；新文件可关闭此窗口后点击“上传 KMZ”，上传后直接进入工作面绑定。</p>
      <div class="tools"><el-input v-model="keyword" placeholder="搜索航线名称" @keyup.enter="libraryPage = 1; loadLibrary()" /><el-button :disabled="busy" @click="libraryPage = 1; loadLibrary()">查询</el-button></div>
      <p v-if="error" class="error">{{ error }}</p>
      <el-table :data="library" v-loading="busy" max-height="350"><el-table-column prop="name" label="航线名称" /><el-table-column width="150"><template #default="{ row }"><el-button :disabled="busy" @click="inspect(row.id)">选择并解析</el-button></template></el-table-column></el-table>
      <el-pagination v-model:current-page="libraryPage" :page-size="10" :total="libraryTotal" layout="prev, pager, next, total" :disabled="busy" @current-change="loadLibrary" />
    </el-dialog>
    <ConstructionPlanForm v-if="formVisible && selected && preview" :area="formContext" :routes="[preview]" :devices="[{ device_sn: sn, nickname: docks.find(d => d.sn === sn)?.name || sn }]" :overall-id="selected.overall_id" @close="formVisible = false" @busy="busy = $event" @saved="created" />
  </main>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ConstructionAreaMap from './ConstructionAreaMap.vue'
import ConstructionPlanForm from './ConstructionPlanForm.vue'
import { useConstructionDocks } from './useConstructionDocks'
import { areaCoverage, pointsWithinCoverage } from './dockCoverageGeometry'
import { insideArea } from './constructionWaylineGeometry'
import { getWaylineFiles, importSubKmzFile } from '/@/api/wayline'
import { listConstructionAreas, listConstructionOverallRoutes, inspectConstructionOverallRoute, prepareConstructionOverallRoute, saveConstructionOverallRoute, listConstructionOverallPlans, dispatchConstructionPlan, archiveConstructionPlan, refreshConstructionDispatch } from '/@/api/construction'
import type { ConstructionAreaRecord, ConstructionOverallRoute, ConstructionPlan, ConstructionWayline } from '/@/api/construction'
import { uuidv4 } from '/@/utils/uuid'
const router = useRouter()
const { docks, error: dockError, loading: docksLoading, refresh: refreshDocks } = useConstructionDocks()
const sn = ref(''); const radius = ref<number | null>(null); const busy = ref(false); const error = ref('')
const areas = ref<ConstructionAreaRecord[]>([]); const areaId = ref(''); const rows = ref<ConstructionOverallRoute[]>([])
const selected = ref<ConstructionOverallRoute | null>(null); const draft = ref<ConstructionOverallRoute | null>(null); const editing = ref(false)
const assignments = ref<Record<number, string>>({}); const checked = ref<number[]>([])
const plans = ref<ConstructionPlan[]>([]); const archived = ref(false); const formVisible = ref(false)
const libraryVisible = ref(false); const library = ref<Array<{ id: string; name: string }>>([]); const keyword = ref(''); const libraryPage = ref(1); const libraryTotal = ref(0)
const fileInput = ref<HTMLInputElement | null>(null); const uploading = ref(false)
const uploaded = ref<{ id: string; name: string } | null>(null)
let epoch = 0; let plansEpoch = 0; const requests = new Map<string, string>()
const mapAreas = computed(() => areas.value.map(a => ({ ...a, boundary: a.boundary || [] })))
const selectedArea = computed(() => areas.value.find(a => a.id === areaId.value))
const coveredAreas = computed(() => { const dock = docks.value.find(d => d.sn === sn.value); return dock && radius.value !== null ? areas.value.filter(a => a.enabled && !a.archived && areaCoverage(dock, radius.value!, a.boundary || []) === 'full') : [] })
const currentCovered = computed(() => {
  const dock = docks.value.find(d => d.sn === sn.value)
  return !!selected.value && !!dock && radius.value !== null && selected.value.bindings.every(b => coveredAreas.value.some(a => a.id === b.area_id)) && pointsWithinCoverage(dock, radius.value, selected.value.points)
})
const preview = computed<ConstructionWayline | null>(() => draft.value ? { ...draft.value, status: 'VALID', source: 'overall', area_version: draft.value.version || 0, message: '' } : null)
const formContext = computed<ConstructionAreaRecord>(() => ({ id: selected.value?.overall_id || '', name: selected.value?.name || '', version: selected.value?.version || 1, enabled: true, archived: false, category: '', notes: '' }))
function message (e: unknown) { return e instanceof Error ? e.message : '操作失败，请重试' }
function areaName (id?: string) { return id ? areas.value.find(a => a.id === id)?.name || draft.value?.bindings?.find(b => b.area_id === id)?.area_name || '原区域已不可用' : '未绑定（转场 / 其他）' }
function coverageChanged (value: { sn: string; radius: number | null; loading: boolean; error: string }) {
  radius.value = value.loading || value.error ? null : value.radius
  if (sn.value === value.sn) return
  sn.value = value.sn; epoch++; plansEpoch++; busy.value = false; uploaded.value = null; rows.value = []; selected.value = null; draft.value = null; plans.value = []; checked.value = []; assignments.value = {}; areaId.value = ''; loadRoutes()
}
async function refresh () {
  if (busy.value || editing.value || formVisible.value) return
  busy.value = true; error.value = ''
  try { areas.value = await listConstructionAreas(); await refreshDocks() } catch (e) { error.value = message(e) } finally { busy.value = false }
  await loadRoutes()
}
async function loadRoutes () {
  if (!sn.value) return
  const token = ++epoch; busy.value = true; error.value = ''
  try {
    const result = await listConstructionOverallRoutes(sn.value); if (token !== epoch) return; rows.value = result
    const current = result.find(r => r.overall_id === selected.value?.overall_id)
    if (current) await choose(current)
  } catch (e) { if (token === epoch) error.value = message(e) } finally { if (token === epoch) busy.value = false }
}
function setDraft (row: ConstructionOverallRoute) {
  draft.value = row; assignments.value = {}; checked.value = []
  for (const b of row.bindings || []) for (const index of b.waypoints) assignments.value[index] = b.area_id
}
async function choose (row: ConstructionOverallRoute) { selected.value = row; setDraft(row); areaId.value = row.bindings[0]?.area_id || ''; await loadPlans() }
async function loadPlans () {
  const token = ++plansEpoch; const id = selected.value?.overall_id; plans.value = []
  if (!id) return
  try { const result = await listConstructionOverallPlans(id, archived.value); if (token === plansEpoch) plans.value = result } catch (e) { if (token === plansEpoch) error.value = message(e) }
}
async function openLibrary () { libraryVisible.value = true; libraryPage.value = 1; await loadLibrary() }
async function loadLibrary () {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { const w = localStorage.getItem('workspace_id'); if (!w) throw new Error('请先登录工作空间'); const r = await getWaylineFiles(w, { page: libraryPage.value, page_size: 10, name: keyword.value.trim() }); if (r.code !== 0) throw new Error(r.message || '航线库加载失败'); library.value = r.data.list || []; libraryTotal.value = r.data.pagination?.total || 0 } catch (e) { error.value = message(e) } finally { busy.value = false }
}
async function inspect (id: string) {
  if (busy.value) return
  const token = ++epoch
  busy.value = true; error.value = ''
  try {
    await inspectDraft(id, token)
  } catch (e) { if (token === epoch) error.value = message(e) } finally { if (token === epoch) busy.value = false }
}
async function inspectDraft (id: string, token: number) {
  let r = await inspectConstructionOverallRoute(sn.value, id)
  if (token !== epoch) return
  if (r.needs_preparation) r = await prepareConstructionOverallRoute(sn.value, id, r.file_sign)
  if (token !== epoch) return
  const old = rows.value.find(v => v.wayline_id === r.wayline_id)
  const changed = !!old && old.file_sign !== r.file_sign
  selected.value = old || null; setDraft({ ...r, overall_id: old?.overall_id || '', version: old?.version || 0, bindings: changed ? [] : old?.bindings || [] }); editing.value = true; plans.value = []; libraryVisible.value = false
  if (changed) ElMessage.warning('航线文件已变化，旧航点绑定已清空，请重新确认')
}
async function uploadKmz (event: Event) {
  const input = event.target as HTMLInputElement; const file = input.files?.[0]; input.value = ''
  if (!file || !sn.value || busy.value || editing.value || formVisible.value || libraryVisible.value) return
  if (!/\.kmz$/i.test(file.name) || !file.size) { error.value = '请选择非空的 KMZ 航线文件'; return }
  const workspace = localStorage.getItem('workspace_id')
  if (!workspace) { error.value = '请先登录工作空间'; return }
  const token = ++epoch; busy.value = true; uploading.value = true; error.value = ''; uploaded.value = null
  let saved = false
  try {
    const form = new FormData(); form.append('file', file, file.name)
    const response = await importSubKmzFile(workspace, form)
    if (token !== epoch) return
    if (response.code !== 0) throw new Error(response.message || '航线上传失败')
    saved = true
    const id = response.data?.wayline_id
    if (typeof id !== 'string' || !id) throw new Error('上传接口未返回航线标识，请从航线库查找已上传文件')
    uploaded.value = { id, name: response.data.name || file.name }
    await inspectDraft(id, token)
    if (token === epoch) ElMessage.success('上传成功，请绑定工作面与拍照航点')
  } catch (e) {
    if (token === epoch) error.value = saved ? '文件已保存到航线库，但未进入绑定：' + message(e) : '上传未确认成功：' + message(e) + '。如连接中断，请先在航线库确认文件是否已上传。'
  } finally { uploading.value = false; if (token === epoch) busy.value = false }
}
function edit () { if (selected.value) inspect(selected.value.wayline_id) }
function bindingIssue (index: number) {
  const p = draft.value?.points.find(p => p.index === index)
  if (!p) return '航点信息未加载'
  if (!p.photo_suffixes?.length) return '缺少可识别的到点拍照后缀；请检查拍照动作及命名设置'
  const conflicts = draft.value!.points.filter(other => other.index !== index && p.photo_suffixes.some(s => other.photo_suffixes?.includes(s))).map(other => other.index)
  return conflicts.length ? '拍照后缀与航点 ' + conflicts.join('、') + ' 重复，请设置不同后缀' : ''
}
function eligible (index: number) { return !bindingIssue(index) }
function togglePoint (index: number) { if (!editing.value || busy.value || !eligible(index)) return; checked.value = checked.value.includes(index) ? checked.value.filter(i => i !== index) : [...checked.value, index] }
function assign () { if (!coveredAreas.value.some(a => a.id === areaId.value)) return; for (const i of checked.value) assignments.value[i] = areaId.value; checked.value = [] }
function unassign () { for (const i of checked.value) delete assignments.value[i]; checked.value = [] }
function suggest () { if (!selectedArea.value) return; checked.value = (draft.value?.points || []).filter(p => eligible(p.index) && insideArea(p, selectedArea.value!.boundary || [])).map(p => p.index) }
async function cancelEdit () { try { await ElMessageBox.confirm('放弃未保存的航点绑定？', '取消编辑') } catch { return }; editing.value = false; if (selected.value) { setDraft(selected.value); await loadPlans() } else { draft.value = null; checked.value = [] } }
async function save () {
  if (!draft.value || busy.value) return
  busy.value = true; error.value = ''
  try {
    const ids = [...new Set(Object.values(assignments.value).filter(Boolean))]
    const bindings = ids.map(id => { const a = coveredAreas.value.find(a => a.id === id); if (!a) throw new Error('绑定区域已不在完全覆盖范围，请重新选择'); return { area_id: id, area_version: a.version, waypoints: Object.keys(assignments.value).map(Number).filter(i => assignments.value[i] === id) } })
    const row = await saveConstructionOverallRoute({ dock_sn: sn.value, wayline_id: draft.value.wayline_id, file_sign: draft.value.file_sign, expected_version: draft.value.version, bindings })
    rows.value = [row, ...rows.value.filter(r => r.overall_id !== row.overall_id)]; editing.value = false; uploaded.value = null; await choose(row); ElMessage.success('总体航线绑定已保存')
  } catch (e) { error.value = message(e) } finally { busy.value = false }
}
async function created () { formVisible.value = false; ElMessage.success('总体计划已保存'); await loadPlans() }
async function dispatch (plan: ConstructionPlan) {
  if (busy.value || !currentCovered.value) return
  busy.value = true
  try {
    await ElMessageBox.confirm(`下发“${plan.name}”？将执行整条总体航线，巡检 ${(plan.bindings || selected.value?.bindings || []).length} 个区域。`, '下发总体任务', { confirmButtonText: '确认下发', cancelButtonText: '取消' })
    if (!requests.has(plan.plan_id)) requests.set(plan.plan_id, uuidv4())
    const r = await dispatchConstructionPlan(plan.plan_id, requests.get(plan.plan_id)!)
    if (['SUCCEEDED', 'FAILED'].includes(r.status)) requests.delete(plan.plan_id)
    ElMessage.info(r.message); await loadPlans()
  } catch (e) { if (e !== 'cancel' && e !== 'close') error.value = message(e) } finally { busy.value = false }
}
async function reconcile (plan: ConstructionPlan) { busy.value = true; try { const r = await refreshConstructionDispatch(plan.plan_id); if (['SUCCEEDED', 'FAILED'].includes(r.last_dispatch?.status || '')) requests.delete(plan.plan_id); await loadPlans() } catch (e) { error.value = message(e) } finally { busy.value = false } }
async function archive (plan: ConstructionPlan) { busy.value = true; try { await ElMessageBox.confirm('归档总体计划？历史任务和照片会保留。', '归档'); await archiveConstructionPlan(plan.plan_id); await loadPlans() } catch (e) { if (e !== 'cancel' && e !== 'close') error.value = message(e) } finally { busy.value = false } }
onBeforeRouteLeave(async () => { if (busy.value) return false; if (!editing.value && !formVisible.value) return true; try { await ElMessageBox.confirm('离开将放弃未保存内容。', '离开页面'); return true } catch { return false } })
onBeforeUnmount(() => { epoch++; plansEpoch++ })
refresh()
</script>
<style scoped>
.overall-page { height: 100%; overflow: auto; padding: 16px; box-sizing: border-box; background: #061c3e; color: #dce9fb; --el-text-color-primary: #e7f0ff; --el-text-color-regular: #c2d4ed; --el-fill-color-blank: #102b50; --el-bg-color: #102b50; --el-border-color: #294d79; --el-fill-color-light: #17365c; }
header,.tools { display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap; } h2,h3 { margin: 0; color: #e7f0ff; } p { font-size: 13px; color: #a8bdd8; line-height: 1.7; } .error { color: #ffad8e; }
.overall-layout { display: grid; grid-template-columns: 240px minmax(0,1fr); gap: 12px; margin-top: 12px; } aside,.bindings-panel,.plans-panel { background: #0d294f; padding: 14px; border: 1px solid #29476d; border-radius: 5px; } aside { display: flex; flex-direction: column; gap: 12px; }
.main-content { min-width: 0; display: flex; flex-direction: column; gap: 12px; } .overall-map { height: 450px; min-height: 350px; border: 1px solid #29476d; } .route-card,.area-card { text-align: left; padding: 12px; color: #dce9fb; background: #102f55; border: 1px solid #294d79; border-radius: 4px; cursor: pointer; } .route-card span { display: block; margin-top: 8px; font-size: 12px; } .route-card.active { border-color: #39d9ff; } button:disabled { opacity: .5; cursor: default; } .bindings-panel .tools { margin: 10px 0; } .tools .el-input { width: 300px; }
.upload-overall { margin-left: 0; }.upload-receipt { margin: 0; overflow-wrap: anywhere; }
.execution-notice { padding: 10px; border: 1px solid #45749a; background: #163c60; }
@media(max-width: 800px) { .overall-layout { grid-template-columns: 1fr; } aside { max-height: 280px; overflow: auto; } }
</style>
