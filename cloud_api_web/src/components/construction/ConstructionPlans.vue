<template>
  <main class="construction-plans">
    <header><div><h2>工地巡检计划</h2><p>按巡检区域管理计划，查看航线与机巢位置。</p></div><div class="actions"><el-button @click="router.push('/task/TaskPanel')">飞行任务</el-button><el-button :disabled="busy" @click="refresh">刷新</el-button></div></header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <div class="plans-layout">
      <aside class="region-panel">
        <h3>巡检区域 <small>{{ filteredAreas.length }} 个</small></h3>
        <el-input v-model="areaKeyword" placeholder="搜索巡检区域" clearable />
        <el-checkbox v-model="showArchivedAreas" @change="refresh">显示已归档区域</el-checkbox>
        <p v-if="loading">加载中…</p><p v-else-if="!filteredAreas.length">暂无匹配区域</p>
        <div class="region-scroll"><button v-for="area in filteredAreas" :key="area.id" class="plan-area-card" :class="{ selected: selectedId === area.id }" :disabled="busy || formVisible" @click="selectArea(area.id)"><strong>{{ area.name }}</strong><span>{{ area.category }} · v{{ area.version }}</span><span>{{ area.archived ? '已归档' : area.enabled ? '启用' : '暂停' }}</span></button></div>
        <el-button @click="manageArea">施工区域管理</el-button>
      </aside>
      <section class="plans-content">
        <div class="plan-map"><ConstructionAreaMap :areas="mapAreas" :selected-id="selectedId || null" :boundary="selectedArea?.boundary || []" :editable="false" :disabled="busy || formVisible" :docks="docks" :dock-error="dockError" :docks-loading="docksLoading" :active-dock-sn="activeDock" :wayline="preview" @select="selectArea" @refresh-docks="refreshDocks" /></div>
        <section class="plan-list">
          <div class="list-heading"><div><h3>{{ selectedArea ? selectedArea.name + ' · 巡检计划' : '请选择巡检区域' }}</h3><p v-if="activeDock">执行机巢：{{ deviceName(activeDock) }}{{ docks.some(d => d.sn === activeDock) ? '' : '（暂无定位）' }}</p></div><el-button type="primary" :disabled="!canCreate || busy || plansLoading" @click="openCreate">＋ 新建计划</el-button></div>
          <div class="actions plan-filters"><el-input v-model="planKeyword" placeholder="搜索计划名称" clearable @input="page = 1" /><el-checkbox v-model="showArchivedPlans" :disabled="busy" @change="loadPlans">显示已归档计划</el-checkbox><el-button size="small" :disabled="!selectedArea" @click="manageArea">管理区域航线</el-button></div>
          <p v-if="plansError" class="error" role="alert">{{ plansError }}</p>
          <el-table :data="pagedPlans" v-loading="plansLoading" height="100%" highlight-current-row row-key="plan_id" :current-row-key="selectedPlanId" @row-click="selectPlan">
            <el-table-column prop="name" label="计划名称" min-width="155" />
            <el-table-column label="巡检航线" min-width="145"><template #default="{ row }">{{ routeName(row.file_id) }}</template></el-table-column>
            <el-table-column label="执行机巢" min-width="130"><template #default="{ row }">{{ deviceName(row.dock_sn) }}</template></el-table-column>
            <el-table-column label="执行方式" min-width="160"><template #default="{ row }">{{ row.task_type === 1 ? '定时 · ' + formatTime(row.begin_time) : '立即任务' }}</template></el-table-column>
            <el-table-column label="状态" min-width="210"><template #default="{ row }"><span :class="{ error: row.status !== 'READY' }">{{ row.message }}</span><small v-if="row.last_dispatch">最近下发：{{ dispatchLabel(row.last_dispatch.status) }}</small></template></el-table-column>
            <el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><el-button link type="primary" :disabled="busy || row.status !== 'READY'" @click.stop="dispatch(row)">下发任务</el-button><el-button v-if="['SUBMITTING', 'UNKNOWN'].includes(row.last_dispatch?.status)" link :disabled="busy" @click.stop="reconcile(row)">查询下发</el-button><el-button link :disabled="busy || !!row.archived" @click.stop="archive(row)">归档</el-button></template></el-table-column>
            <template #empty>{{ selectedArea ? '当前区域暂无匹配计划，可点击新建计划' : '请从左侧或地图选择区域' }}</template>
          </el-table>
          <el-pagination v-model:current-page="page" :page-size="10" :total="filteredPlans.length" layout="total, prev, pager, next" />
        </section>
      </section>
    </div>
    <ConstructionPlanForm v-if="formVisible && selectedArea" :area="selectedArea" :routes="routes" :devices="devices" @close="closeForm" @saved="created" @busy="busy = $event" @preview="previewRoute" @dock="activeDock = $event" />
  </main>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ConstructionAreaMap from './ConstructionAreaMap.vue'
import ConstructionPlanForm from './ConstructionPlanForm.vue'
import { useConstructionDocks } from './useConstructionDocks'
import { archiveConstructionPlan, dispatchConstructionPlan, getConstructionWayline, listConstructionAreas, listConstructionPlans, listConstructionWaylines, refreshConstructionDispatch } from '/@/api/construction'
import type { ConstructionAreaRecord, ConstructionPlan, ConstructionWayline } from '/@/api/construction'
import { uuidv4 } from '/@/utils/uuid'
const router = useRouter(); const route = useRoute()
const { docks, devices, error: dockError, loading: docksLoading, refresh: refreshDocks } = useConstructionDocks()
const areas = ref<ConstructionAreaRecord[]>([]); const selectedId = ref(typeof route.query.area === 'string' ? route.query.area : '')
const loading = ref(false); const busy = ref(false); const error = ref(''); const plansError = ref(''); const plansLoading = ref(false)
const areaKeyword = ref(''); const planKeyword = ref(''); const showArchivedAreas = ref(false); const showArchivedPlans = ref(false); const page = ref(1)
const plans = ref<ConstructionPlan[]>([]); const routes = ref<ConstructionWayline[]>([]); const preview = ref<ConstructionWayline | null>(null); const activeDock = ref(''); const formVisible = ref(false)
let epoch = 0; let areaEpoch = 0; let previewEpoch = 0
const dispatchRequests = new Map<string, string>()
const selectedPlanId = ref('')
const selectedArea = computed(() => areas.value.find(a => a.id === selectedId.value))
const filteredAreas = computed(() => areas.value.filter(a => a.name.includes(areaKeyword.value.trim())))
const mapAreas = computed(() => filteredAreas.value.map(a => ({ ...a, boundary: a.boundary || [] })))
const canCreate = computed(() => selectedArea.value?.enabled && !selectedArea.value.archived)
const filteredPlans = computed(() => plans.value.filter(p => p.name.includes(planKeyword.value.trim())))
const pagedPlans = computed(() => filteredPlans.value.slice((page.value - 1) * 10, page.value * 10))
function deviceName (sn: string) { const d = devices.value.find(d => d.device_sn === sn); return d?.nickname || d?.device_name || sn }
function routeName (id: string) { return routes.value.find(r => r.wayline_id === id)?.name || id }
function formatTime (value: number) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
function dispatchLabel (value: string) { return ({ SUCCEEDED: '已提交', UNKNOWN: '待确认', SUBMITTING: '提交中', FAILED: '未完成' } as Record<string, string>)[value] || value }
function message (e: unknown) { return e instanceof Error ? e.message : '操作失败，请重试' }
function clearPreview () { previewEpoch++; preview.value = null; activeDock.value = '' }
async function refresh () {
  if (busy.value || formVisible.value) return
  const token = ++areaEpoch; loading.value = true; error.value = ''
  try {
    const data = await listConstructionAreas({ archived: showArchivedAreas.value })
    if (token !== areaEpoch) return
    areas.value = data
    if (!data.some(a => a.id === selectedId.value)) { selectedPlanId.value = ''; selectedId.value = data[0]?.id || '' }
    await loadPlans()
  } catch (e) { if (token === areaEpoch) error.value = message(e) } finally { if (token === areaEpoch) loading.value = false }
  refreshDocks()
}
async function selectArea (id: string) {
  if (busy.value || formVisible.value || id === selectedId.value) return
  selectedPlanId.value = ''; selectedId.value = id; planKeyword.value = ''; await router.replace({ query: { ...route.query, area: id } }); await loadPlans()
}
async function loadPlans () {
  const token = ++epoch; const area = selectedId.value
  plans.value = []; routes.value = []; page.value = 1; clearPreview(); plansError.value = ''
  if (!area) { plansLoading.value = false; return }
  plansLoading.value = true
  const results = await Promise.allSettled([listConstructionPlans(area, showArchivedPlans.value), listConstructionWaylines(area)])
  if (token !== epoch) return
  if (results[0].status === 'fulfilled') plans.value = results[0].value
  else plansError.value = message(results[0].reason)
  if (results[1].status === 'fulfilled') routes.value = results[1].value
  else plansError.value += (plansError.value ? '；' : '') + message(results[1].reason)
  plansLoading.value = false
  const selected = plans.value.find(p => p.plan_id === selectedPlanId.value)
  if (selected) { activeDock.value = selected.dock_sn; await previewRoute(selected.file_id) } else selectedPlanId.value = ''
}
async function previewRoute (id: string) {
  const token = ++previewEpoch; const area = selectedId.value; preview.value = null
  try { const data = await getConstructionWayline(area, id); if (token !== previewEpoch) return; if (data.status !== 'VALID') throw new Error(data.message); preview.value = data } catch (e) { if (token === previewEpoch) ElMessage.warning(message(e)) }
}
function selectPlan (plan: ConstructionPlan) { if (busy.value || formVisible.value) return; selectedPlanId.value = plan.plan_id; activeDock.value = plan.dock_sn; previewRoute(plan.file_id) }
function manageArea () { router.push({ path: '/variableMgt/constructionAreas', query: selectedId.value ? { area: selectedId.value } : {} }) }
function openCreate () { if (!routes.value.some(r => r.status === 'VALID')) { ElMessage.warning('本区域没有有效航线，请先管理区域航线'); return } if (!devices.value.length) { ElMessage.warning('尚未加载到可选机巢，请刷新机巢列表'); return } clearPreview(); formVisible.value = true }
function closeForm () { formVisible.value = false; clearPreview() }
async function created (plan: ConstructionPlan) { closeForm(); ElMessage.success('计划已保存，可在列表下发'); await loadPlans(); selectPlan(plan) }
async function dispatch (plan: ConstructionPlan) {
  if (busy.value) return
  busy.value = true
  try { await ElMessageBox.confirm(`下发“${plan.name}”？执行机巢：${deviceName(plan.dock_sn)}${plan.task_type === 1 ? '，定时：' + formatTime(plan.begin_time) : '，立即执行'}。`, '下发任务', { confirmButtonText: '确认下发', cancelButtonText: '取消' }) } catch { busy.value = false; return }
  if (!dispatchRequests.has(plan.plan_id)) dispatchRequests.set(plan.plan_id, uuidv4())
  try {
    const result = await dispatchConstructionPlan(plan.plan_id, dispatchRequests.get(plan.plan_id)!)
    if (result.status === 'FAILED') dispatchRequests.delete(plan.plan_id)
    if (result.status === 'SUCCEEDED') { dispatchRequests.delete(plan.plan_id); ElMessage.success(result.message) } else ElMessage.warning(result.message || '下发结果待确认，请查看飞行任务列表')
    await loadPlans()
  } catch (e) { ElMessage.error(message(e)) } finally { busy.value = false }
}
async function archive (plan: ConstructionPlan) {
  if (busy.value) return
  busy.value = true
  try { await ElMessageBox.confirm('归档后不能再次下发此计划；历史飞行任务与结果保留。已提交的任务请先在飞行任务列表核查或取消。', '归档计划') } catch { busy.value = false; return }
  try { await archiveConstructionPlan(plan.plan_id); await loadPlans() } catch (e) { ElMessage.error(message(e)) } finally { busy.value = false }
}
async function reconcile (plan: ConstructionPlan) {
  busy.value = true
  try { const result = await refreshConstructionDispatch(plan.plan_id); if (['FAILED', 'SUCCEEDED'].includes(result.last_dispatch?.status || '')) dispatchRequests.delete(plan.plan_id); ElMessage.info(result.message); await loadPlans() } catch (e) { ElMessage.error(message(e)) } finally { busy.value = false }
}
onBeforeRouteLeave(async () => { if (busy.value) return false; if (!formVisible.value) return true; try { await ElMessageBox.confirm('离开将放弃未保存的计划。', '离开页面'); return true } catch { return false } })
onBeforeUnmount(() => { epoch++; areaEpoch++; previewEpoch++ })
refresh()
</script>
<style scoped>
.construction-plans { height: 100%; min-height: 0; box-sizing: border-box; padding: 16px 18px; display: flex; flex-direction: column; gap: 12px; background: #061c3e; color: #dce9fb; overflow: auto; --el-text-color-primary: #e7f0ff; --el-text-color-regular: #c2d4ed; --el-fill-color-blank: #102b50; --el-fill-color-light: #17365c; --el-bg-color: #102b50; --el-border-color: #294d79; }
header,.list-heading,.actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
h2,h3,p { margin: 0; } h2 { font-size: 20px; color: #e7f0ff; } h3 { font-size: 15px; color: #dce9fb; } header p,.list-heading p { margin-top: 6px; color: #9cb4d2; font-size: 12px; }
.plans-layout { display: grid; grid-template-columns: 245px minmax(0,1fr); gap: 12px; flex: 1; min-height: 640px; }
.region-panel,.plan-list,.plan-map { border: 1px solid #29476d; border-radius: 5px; background: #0d294f; overflow: hidden; }
.region-panel { padding: 14px; display: flex; flex-direction: column; gap: 12px; min-height: 0; }
.region-scroll { flex: 1; overflow: auto; } small { display: block; color: #91aac8; font-size: 12px; margin-top: 5px; }
.plan-area-card { width: 100%; padding: 13px; margin-bottom: 9px; text-align: left; background: #102f55; color: #dce9fb; border: 1px solid #294d79; border-radius: 5px; cursor: pointer; }
.plan-area-card span { display: block; margin-top: 8px; font-size: 12px; color: #9eb6d6; }.plan-area-card.selected { border-color: #39d9ff; background: #17406a; }
.plans-content { display: grid; grid-template-rows: minmax(300px, 1.2fr) minmax(300px, 1fr); gap: 12px; min-width: 0; min-height: 0; }
.plan-list { display: flex; flex-direction: column; padding: 14px; gap: 12px; min-height: 0; }.plan-list :deep(.el-table) { flex: 1; min-height: 100px; }
.plan-list :deep(.el-table) { --el-table-current-row-bg-color: #17406a; --el-table-row-hover-bg-color: #17365c; }
.plan-filters { justify-content: flex-start; }.plan-filters .el-input { max-width: 240px; }.error { color: #ffad8e; }.actions { flex-wrap: wrap; }
@media(max-width: 1050px) { .plans-layout { grid-template-columns: 200px minmax(0,1fr); } }
@media(max-width: 680px) { .plans-layout { display: flex; flex-direction: column; }.region-panel { max-height: 240px; flex: none; }.plans-content { min-height: 720px; } header { align-items: flex-start; } }
</style>
