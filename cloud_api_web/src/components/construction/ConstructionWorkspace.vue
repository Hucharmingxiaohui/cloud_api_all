<template>
  <main class="construction-shell">
    <div class="demo-banner" role="note"><span><b>前端演示</b> 示例机巢、坐标与任务；修改仅保存到当前浏览器。未连接真实设备，不会下发飞行指令。</span><div><el-button size="small" link @click="exportDemo">导出演示数据</el-button><el-button size="small" link @click="resetDemo">恢复示例</el-button></div></div>
    <div v-if="storageError" class="storage-error" role="alert">{{ storageError }}</div>
    <nav class="construction-nav" aria-label="工地巡检模块"><el-tabs v-if="standalone" :key="page" :model-value="page" :before-leave="canSelectTab" @tab-click="navigateTab"><el-tab-pane v-for="tab in tabs" :key="tab.key" :name="tab.key"><template #label>{{ tab.label }}<small>{{ tab.count }}</small></template></el-tab-pane></el-tabs><el-radio-group v-model="deployment" size="small" aria-label="演示部署方式"><el-radio-button label="mobile">车载部署</el-radio-button><el-radio-button label="fixed">固定部署</el-radio-button></el-radio-group></nav>

    <section v-if="page === 'areas'" class="area-page">
      <div class="page-heading"><div><h2>施工区域</h2><p>定义巡检边界，让每一次巡视都有迹可循。</p></div><el-button type="primary" @click="newArea">＋ 新建区域</el-button></div>
      <div class="area-layout">
        <aside class="panel area-list">
          <div class="panel-heading"><h3>区域台账</h3><span>{{ data.areas.length }} 个区域</span></div>
          <el-input v-model="areaSearch" placeholder="搜索区域名称" clearable aria-label="搜索区域名称" />
          <el-radio-group v-model="areaFilter" size="small" class="filter-pills" aria-label="区域状态"><el-radio-button v-for="item in areaFilters" :key="item.key" :label="item.key">{{ item.label }}</el-radio-button></el-radio-group>
          <div class="area-cards"><button v-for="area in filteredAreas" :key="area.id" class="area-card" :class="{ selected: areaDraft?.id === area.id }" @click="selectArea(area)"><div><span class="area-symbol">⬡</span><span class="badge" :class="area.enabled ? 'success' : 'muted'">{{ area.enabled ? '启用' : '暂停' }}</span></div><h4>{{ area.name }}</h4><p>{{ area.category }} <span>v{{ area.version }}</span></p><footer><span>{{ formatArea(area.boundary) }}</span><span>{{ area.params.lens === 'dual' ? '双光采集' : lensLabel(area.params.lens) }}</span></footer></button><div v-if="!filteredAreas.length" class="empty-state compact">没有匹配的区域<br><small>试试其他关键词或新建区域</small></div></div>
          <div class="list-footnote">示例范围仅用于交互预览<br>实际作业前需导入现场地图与坐标</div>
        </aside>
        <ConstructionMap :areas="data.areas" :selected-id="areaDraft?.id" :boundary="areaDraft?.boundary" :editable="areaEditing" @select="selectAreaById" @update:boundary="updateBoundary" @drawing="drawing = $event" />
        <aside v-if="areaDraft" class="panel area-properties">
          <div class="panel-heading"><h3>{{ areaEditing ? '编辑区域' : '区域详情' }}</h3><span class="badge" :class="areaEditing ? 'warning' : 'muted'">{{ areaEditing ? '编辑中' : `版本 ${areaDraft.version}` }}</span></div>
          <el-form label-position="top" :disabled="!areaEditing" @submit.prevent>
            <el-form-item label="区域名称"><el-input v-model="areaDraft.name" maxlength="40" placeholder="例如：03号风机基础施工区" /></el-form-item>
            <el-form-item label="施工类型"><el-select v-model="areaDraft.category"><el-option v-for="category in categories" :key="category" :value="category" :label="category" /></el-select></el-form-item>
            <div class="boundary-summary"><div><small>边界面积</small><strong>{{ formatArea(areaDraft.boundary) }}</strong></div><div><small>边界顶点</small><strong>{{ areaDraft.boundary.length }} <em>个</em></strong></div></div>
            <el-form-item label="允许创建巡检计划"><el-switch v-model="areaDraft.enabled" /><span class="switch-caption">{{ areaDraft.enabled ? '启用' : '暂停' }}</span></el-form-item>
            <h4 class="section-label">默认采集参数</h4><ConstructionParams v-model="areaDraft.params" />
            <el-form-item label="区域备注"><el-input v-model="areaDraft.notes" type="textarea" :rows="2" maxlength="300" placeholder="记录施工重点和巡检关注事项" /></el-form-item>
          </el-form>
          <p class="helper">航高仅为演示参数，未进行地形、净空或飞行条件校验。</p>
          <div class="property-actions" v-if="areaEditing"><el-button @click="cancelArea">取消</el-button><el-button type="primary" :disabled="drawing" @click="saveAreaDraft">保存区域</el-button></div>
          <div class="property-actions" v-else><el-button @click="areaEditing = true">编辑区域</el-button><el-button type="primary" :disabled="!areaDraft.enabled" @click="createPlanForArea(areaDraft.id)">创建计划</el-button></div>
        </aside>
      </div>
    </section>

    <section v-else-if="page === 'plans'" class="plan-page">
      <div class="page-heading"><div><h2>{{ planEditing ? (data.plans.some(p => p.id === planDraft.id) ? '编辑巡检计划' : '新建巡检计划') : '巡检计划' }}</h2><p>施工区域、执行机巢和采集参数，集中配置。</p></div><el-button v-if="!planEditing" type="primary" @click="newPlan()">＋ 新建计划</el-button><el-button v-else @click="closePlan">返回计划列表</el-button></div>
      <template v-if="planEditing">
        <div class="plan-steps"><span><b>01</b> 选择区域与机巢</span><i>—</i><span><b>02</b> 配置采集参数</span><i>—</i><span><b>03</b> 保存并预览流程</span></div>
        <div class="plan-layout"><div class="panel plan-form"><el-form label-position="top" @submit.prevent>
          <el-form-item label="计划名称"><el-input v-model="planDraft.name" maxlength="60" placeholder="输入巡检计划名称" /></el-form-item>
          <el-form-item label="施工区域"><el-select :model-value="planDraft.areaId" placeholder="选择已启用的施工区域" @update:model-value="changePlanArea"><el-option v-for="area in data.areas" :key="area.id" :value="area.id" :label="`${area.name} · v${area.version}${area.enabled ? '' : '（暂停）'}`" :disabled="!area.enabled" /></el-select></el-form-item>
          <el-form-item label="执行机巢（示例）"><el-select v-model="planDraft.dockId"><el-option v-for="dock in demoDocks" :key="dock.id" :value="dock.id" :label="dock.name" /></el-select></el-form-item>
          <div class="info-note">{{ selectedDock?.mode === 'mobile' ? '车载部署：真实执行前需确认最新部署位置。' : '固定部署：使用现场局域网和本地存储。' }}设备状态尚未接入。</div>
          <h4 class="section-label">航线与采集设置</h4>
          <el-form-item label="航线来源"><el-radio-group v-model="planDraft.source"><el-radio-button label="generated">区域自动规划</el-radio-button><el-radio-button label="imported">已有 KMZ</el-radio-button></el-radio-group></el-form-item>
          <div v-if="planDraft.source === 'imported'" class="file-choice"><label>选择 KMZ 文件<input type="file" accept=".kmz" @change="chooseKmz" /></label><p>{{ planDraft.fileName || '尚未选择文件' }}</p><small>仅暂存文件名，不上传、不解析航点。</small></div>
          <p v-else class="helper">自动规划服务待接入。右侧展示区域边界，不生成可执行航线。</p>
          <ConstructionParams v-model="planDraft.params" />
          <el-form-item label="执行方式"><el-radio-group v-model="planDraft.schedule"><el-radio label="manual">手动执行</el-radio><el-radio label="scheduled">定时执行</el-radio></el-radio-group></el-form-item>
          <el-form-item v-if="planDraft.schedule === 'scheduled'" label="计划时间"><el-date-picker v-model="planDraft.scheduledAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择未来时间" /></el-form-item>
          <p class="helper">定时配置仅保存为演示数据，不会启动调度。</p>
          <div class="property-actions"><el-button @click="closePlan">取消</el-button><el-button type="primary" @click="savePlanDraft">保存演示计划</el-button></div>
        </el-form></div>
        <div class="plan-preview"><ConstructionMap :areas="planArea ? [planArea] : []" :selected-id="planArea?.id" /><div class="panel preview-summary"><div><span class="eyebrow">AREA PREVIEW</span><h3>{{ planArea?.name || '请先选择施工区域' }}</h3><p>{{ planArea ? formatArea(planArea.boundary) : '—' }} · WGS84 · {{ selectedDock?.name }}</p></div><span class="badge warning">规划服务未接入</span><div class="preview-metrics"><div><small>预设航高</small><strong>{{ planDraft.params.altitude }} <em>m</em></strong></div><div><small>预设速度</small><strong>{{ planDraft.params.speed }} <em>m/s</em></strong></div><div><small>航程 / 耗时</small><strong>待计算</strong></div></div></div></div></div>
      </template>
      <div v-else class="panel table-panel">
        <div class="table-tools"><el-form inline @submit.prevent><el-form-item label="计划 / 区域"><el-input v-model="planSearch" placeholder="搜索计划或区域名称" clearable /></el-form-item><el-form-item><el-button @click="planSearch = ''">重置</el-button></el-form-item></el-form><span>{{ filteredPlans.length }} 个演示计划</span></div>
        <el-table :data="pagedPlans" stripe empty-text="暂无计划，请先创建施工区域和巡检计划">
          <el-table-column label="巡检计划" min-width="220"><template #default="{ row }"><strong>{{ row.name }}</strong><p class="table-secondary">{{ dateLabel(row.createdAt) }}</p></template></el-table-column>
          <el-table-column label="施工区域" min-width="210"><template #default="{ row }">{{ areaName(row.areaId) }}<p class="table-secondary">区域版本 v{{ row.areaVersion }} <span class="warning-text" v-if="isPlanStale(row)">· 请更新配置</span></p></template></el-table-column>
          <el-table-column label="执行机巢" min-width="165"><template #default="{ row }">{{ dockName(row.dockId) }}</template></el-table-column>
          <el-table-column label="配置" min-width="150"><template #default="{ row }">{{ row.params.altitude }}m · {{ lensLabel(row.params.lens) }}<p class="table-secondary">{{ row.schedule === 'scheduled' ? '定时（未启动）' : '手动执行' }}</p></template></el-table-column>
          <el-table-column label="操作" width="240" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="editPlan(row)">编辑</el-button><el-button link type="primary" :disabled="isPlanStale(row)" @click="previewPlan(row)">预览任务流程</el-button><el-tooltip content="演示模式未接入真实任务下发"><span><el-button link disabled>下发</el-button></span></el-tooltip></template></el-table-column>
        </el-table>
        <div class="pagination-container"><el-pagination v-model:current-page="planPage" v-model:page-size="planPageSize" :page-sizes="[10, 20, 40]" :total="filteredPlans.length" layout="total, sizes, prev, pager, next" background /></div>
      </div>
    </section>

    <section v-else class="tasks-page">
      <div class="page-heading"><div><h2>{{ selectedTask ? '工地任务结果预览' : '选择结果展示样例' }}</h2><p>{{ selectedTask ? '任务快照、照片与结果在同一处查看。' : '真实工地任务统一在飞行任务列表查看。此处仅选择演示记录。' }}</p></div><el-button v-if="!standalone" @click="router.push('/' + ERouterName.TASK)">返回飞行任务</el-button><el-button v-if="selectedTask" @click="navigate('plans')">返回工地计划</el-button><el-button v-else @click="navigate('plans')">从计划预览任务</el-button></div>
      <template v-if="selectedTask">
        <div class="panel task-overview"><div><span class="badge warning">{{ selectedTask.status === 'sample' ? '结果展示样例' : '流程预览 · 未下发' }}</span><h3>{{ selectedTask.name }}</h3><p>{{ selectedTask.area.name }} · v{{ selectedTask.area.version }} · {{ selectedTask.dock.name }}</p></div><div class="task-meta"><span>记录时间<strong>{{ dateLabel(selectedTask.createdAt) }}</strong></span><span>拍摄配置<strong>{{ selectedTask.params.altitude }}m · {{ lensLabel(selectedTask.params.lens) }}</strong></span></div></div>
        <div class="task-pipeline"><div><b>01</b><span>飞行任务<small>未连接设备 / 未执行</small></span></div><div><b>02</b><span>本地照片<small>{{ taskPhotos.length ? `${taskPhotos.length}张本机临时预览` : '未接入媒体接收服务' }}</small></span></div><div><b>03</b><span>智能分析<small>待接入算法服务</small></span></div><div><b>04</b><span>巡检报告<small>待接入报告服务</small></span></div></div>
        <el-tabs v-model="detailTab" class="detail-tabs"><el-tab-pane v-for="tab in detailTabs" :key="tab.key" :name="tab.key" :label="tab.label" /></el-tabs>
        <div v-if="detailTab === 'photos'" class="panel photo-panel"><div class="table-tools"><div class="filter-pills"><button v-for="item in lensFilters" :key="item.key" :class="{ active: photoFilter === item.key }" @click="photoFilter = item.key">{{ item.label }}</button></div><div class="photo-import"><el-select v-model="importLens" aria-label="导入照片镜头类型"><el-option label="标记为可见光" value="visible" /><el-option label="标记为红外" value="infrared" /></el-select><label class="upload-button">＋ 选择本机照片<input type="file" accept="image/jpeg,image/png,image/webp" multiple @change="importPhotos" /></label></div></div><p class="helper">仅在当前会话预览，刷新或离开模块后需重新选择；不代表照片已上传到本地服务器。最多20张，每张不超过20MB。</p><div class="photo-grid"><button v-for="photo in filteredPhotos" :key="photo.id" class="photo-card" @click="activePhoto = photo"><div class="photo-image"><img :src="photo.url" :alt="photo.name" /><span>{{ photo.sample ? '示意图 · 非实拍' : '本机临时预览' }}</span><i>{{ lensLabel(photo.lens) }}</i></div><div><strong>{{ photo.name }}</strong><small>{{ photo.sample ? '仅演示照片卡片与预览交互' : '未分析 · 未上传' }}</small></div></button></div><div v-if="!filteredPhotos.length" class="empty-state"><span>▧</span><h3>还没有照片</h3><p>选择本机照片体验查看流程；真实照片回传将在后端接入后启用。</p></div></div>
        <div v-else-if="detailTab === 'analysis'" class="panel analysis-panel"><div class="panel-heading"><h3>回站手动分析与复核</h3><span class="badge warning">算法未接入</span></div><p>回站连接网线后检查分析主机，再手动提交本任务照片。结果仍关联原飞行任务。</p><div class="property-actions"><el-button disabled>检查分析主机</el-button><el-button disabled>提交分析</el-button></div><p class="helper">此处为演示结果；真实任务请从飞行任务列表进入。</p><div class="analysis-metrics"><div><small>单图人员数量</small><strong>—</strong></div><div><small>区域去重人数</small><strong>—</strong></div><div><small>已确认隐患</small><strong>—</strong></div></div><p class="helper">未分析不等于无异常；区域人数不能直接累加单图人数。</p><template v-if="selectedTask.status === 'sample'"><div class="info-note">以下是一条隐患交互样例，不来自照片识别。复核选择仅在当前会话有效。</div><div class="issue-example"><div class="issue-icon">!</div><div><h4>未佩戴安全帽 <span class="badge warning">示例</span></h4><p>展示类别、证据图片及人工复核流程，识别置信度暂不展示。</p><el-radio-group v-model="reviewState"><el-radio-button label="pending">待确认</el-radio-button><el-radio-button label="confirmed">确认示例</el-radio-button><el-radio-button label="dismissed">标记为误报</el-radio-button></el-radio-group></div></div></template><div v-else class="empty-state"><h3>等待分析服务接入</h3><p>真实检测框、人员统计和隐患将与本次执行记录关联。</p></div></div>
        <div v-else-if="detailTab === 'report'" class="panel report-panel"><div class="report-paper"><div class="eyebrow">REPORT STRUCTURE · PREVIEW</div><h3>工地无人机巡检报告</h3><p>{{ selectedTask.area.name }}</p><hr /><div v-for="section in ['一、任务与施工区域概况', '二、采集情况与媒体完整性', '三、人员统计口径与结果', '四、隐患明细及人工复核', '五、处置记录与巡检结论']" :key="section">{{ section }}<span class="paper-line" /></div><p class="warning-text">模板结构预览，不是正式巡检报告。</p></div><div><h3>报告生成待接入</h3><p>报告将保留区域版本、媒体完整性和人工复核信息。</p><el-button disabled>生成正式报告</el-button><el-button @click="exportTask">导出演示任务摘要</el-button></div></div>
        <div v-else class="snapshot-layout"><ConstructionMap :areas="[selectedTask.area]" :selected-id="selectedTask.area.id" /><div class="panel"><div class="panel-heading"><h3>执行快照（演示）</h3></div><dl class="snapshot-list"><dt>区域版本</dt><dd>v{{ selectedTask.area.version }}</dd><dt>区域面积</dt><dd>{{ formatArea(selectedTask.area.boundary) }}</dd><dt>演示机巢</dt><dd>{{ selectedTask.dock.name }}</dd><dt>云台俯仰</dt><dd>{{ selectedTask.params.pitch }}°</dd><dt>拍照间隔</dt><dd>{{ selectedTask.params.interval }}秒</dd></dl><p class="helper">后续编辑区域不会覆盖这条记录的区域快照。</p></div></div>
      </template>
      <template v-else>
        <div class="panel"><p>演示记录不会写入飞行任务列表。请选择结果样例，或从工地计划预览流程。</p><el-select placeholder="选择演示结果" @change="openTask"><el-option v-for="task in data.tasks" :key="task.id" :value="task.id" :label="task.name" /></el-select></div>
      </template>
    </section>
    <el-dialog v-model="photoDialogVisible" title="照片预览" width="min(1000px, 92vw)" append-to-body class="construction-photo-dialog"><template v-if="activePhoto"><img :src="activePhoto.url" :alt="activePhoto.name" style="display:block;max-width:100%;max-height:70vh;margin:auto" /><p>{{ activePhoto.name }} · {{ activePhoto.sample ? '示意图，非现场实拍' : '本机文件临时预览，未上传、未分析' }}</p></template></el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ERouterName } from '/@/types'
import type { ConstructionArea, ConstructionPlan, ConstructionPoint } from '../../types/construction'
import ConstructionMap from './ConstructionMap.vue'
import ConstructionParams from './ConstructionParams.vue'
import { areaSquareMeters, clone, defaultParams, demoDocks, newId } from './constructionModel'
import { useConstructionDemo } from './useConstructionDemo'

const route = useRoute(); const router = useRouter()
const { data, storageError, saveArea, savePlan, previewTask, reset, exportRaw } = useConstructionDemo()
const deployment = ref('mobile')
const standalone = computed(() => route.path.startsWith('/construction-preview'))
const page = computed(() => String(route.meta.constructionPage || route.params.page || (route.query.demo_task ? 'tasks' : 'areas')))
const tabs = computed(() => [{ key: 'areas', label: '施工区域', count: data.value.areas.length }, { key: 'plans', label: '巡检计划', count: data.value.plans.length }, { key: 'tasks', label: '任务与结果', count: data.value.tasks.length }])
const categories = ['基础施工', '吊装作业', '道路施工', '材料堆场', '其他施工']
const areaFilters = [{ key: 'all', label: '全部' }, { key: 'enabled', label: '启用' }, { key: 'paused', label: '暂停' }]
const areaSearch = ref(''); const areaFilter = ref('all'); const planSearch = ref('')
const areaDraft = ref<ConstructionArea | null>(data.value.areas[0] ? clone(data.value.areas[0]) : null)
const areaBaseline = ref(JSON.stringify(areaDraft.value)); const areaEditing = ref(false); const drawing = ref(false)
function blankPlan (): ConstructionPlan { return { id: newId('plan'), name: '', areaId: '', areaVersion: 0, dockId: deployment.value === 'mobile' ? demoDocks[0].id : demoDocks[1].id, params: clone(defaultParams), source: 'generated', fileName: '', schedule: 'manual', scheduledAt: '', createdAt: new Date().toISOString() } }
const planDraft = ref<ConstructionPlan>(blankPlan()); const planEditing = ref(false); const planBaseline = ref('')
const dirty = computed(() => (areaEditing.value && (drawing.value || JSON.stringify(areaDraft.value) !== areaBaseline.value)) || (planEditing.value && JSON.stringify(planDraft.value) !== planBaseline.value))
const filteredAreas = computed(() => data.value.areas.filter(a => a.name.includes(areaSearch.value.trim()) && (areaFilter.value === 'all' || (areaFilter.value === 'enabled' ? a.enabled : !a.enabled))))
const filteredPlans = computed(() => data.value.plans.filter(p => `${p.name}${areaName(p.areaId)}`.includes(planSearch.value.trim())))
const planPage = ref(1); const planPageSize = ref(10)
const pagedPlans = computed(() => filteredPlans.value.slice((planPage.value - 1) * planPageSize.value, planPage.value * planPageSize.value))
watch([planSearch, planPageSize], () => { planPage.value = 1 }, { flush: 'sync' })
watch(() => filteredPlans.value.length, total => { planPage.value = Math.min(planPage.value, Math.max(1, Math.ceil(total / planPageSize.value))) }, { flush: 'sync' })
const planArea = computed(() => data.value.areas.find(a => a.id === planDraft.value.areaId))
const selectedDock = computed(() => demoDocks.find(d => d.id === planDraft.value.dockId))
const selectedTask = computed(() => data.value.tasks.find(t => t.id === (route.query.demo_task || route.query.task)))
function formatArea (points: ConstructionPoint[]) { return `${(areaSquareMeters(points) / 10000).toFixed(2)} 公顷` }
function dateLabel (value: string) { return Number.isFinite(Date.parse(value)) ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—' }
function lensLabel (value: string) { return ({ dual: '可见光 + 红外', visible: '可见光', infrared: '红外' } as Record<string, string>)[value] || value }
function areaName (id: string) { return data.value.areas.find(a => a.id === id)?.name || '区域已不存在' }
function dockName (id: string) { return demoDocks.find(d => d.id === id)?.name || '未选择' }
function isPlanStale (plan: ConstructionPlan) { const area = data.value.areas.find(a => a.id === plan.areaId); return !area || !area.enabled || area.version !== plan.areaVersion }
function fail (error: unknown) { ElMessage.error(error instanceof Error ? error.message : '操作失败，请重试') }
async function confirmDiscard () {
  if (!dirty.value) return true
  try { await ElMessageBox.confirm('当前修改尚未保存，离开将丢失本次修改。', '放弃未保存的修改？', { confirmButtonText: '放弃修改', cancelButtonText: '继续编辑', type: 'warning' }); return true } catch { return false }
}
onBeforeRouteLeave(confirmDiscard)
onBeforeRouteUpdate(confirmDiscard)
function beforeUnload (event: BeforeUnloadEvent) { if (dirty.value) { event.preventDefault(); event.returnValue = '' } }
window.addEventListener('beforeunload', beforeUnload)
function navigate (tab: string, query: Record<string, string> = {}) {
  if (standalone.value) return router.push({ path: '/construction-preview/' + tab, query })
  if (tab === 'tasks') return router.push({ path: '/task/taskResult', query: { demo_task: query.task || 'select' } })
  return router.push({ path: tab === 'areas' ? '/variableMgt/constructionAreas' : '/taskManage/construction/plans', query })
}
function canSelectTab (tab: string | number) { return String(tab) === page.value }
function navigateTab (tab: { paneName: string | number }) { if (String(tab.paneName) !== page.value) return navigate(String(tab.paneName)) }
async function selectArea (area: ConstructionArea) { if (!await confirmDiscard()) return; areaEditing.value = false; drawing.value = false; areaDraft.value = clone(area); areaBaseline.value = JSON.stringify(areaDraft.value) }
function selectAreaById (id: string) { const area = data.value.areas.find(a => a.id === id); if (area) selectArea(area) }
async function newArea () {
  if (!await confirmDiscard()) return
  areaDraft.value = { id: newId('area'), name: '', category: categories[0], enabled: true, notes: '', version: 0, boundary: [], params: clone(defaultParams), updatedAt: new Date().toISOString() }
  areaBaseline.value = JSON.stringify(areaDraft.value); areaEditing.value = true; drawing.value = false
}
function updateBoundary (boundary: ConstructionPoint[]) { if (areaEditing.value && areaDraft.value) areaDraft.value.boundary = boundary }
function saveAreaDraft () { if (!areaDraft.value) return; try { areaDraft.value = clone(saveArea(areaDraft.value)); areaBaseline.value = JSON.stringify(areaDraft.value); areaEditing.value = false; ElMessage.success('区域已保存到浏览器演示数据') } catch (error) { fail(error) } }
async function cancelArea () { if (!await confirmDiscard()) return; const area = data.value.areas.find(a => a.id === areaDraft.value?.id) || data.value.areas[0]; areaDraft.value = area ? clone(area) : null; areaBaseline.value = JSON.stringify(areaDraft.value); areaEditing.value = false; drawing.value = false }
function changePlanArea (id: string) { const area = data.value.areas.find(a => a.id === id); planDraft.value.areaId = id; planDraft.value.areaVersion = area?.version || 0; if (area) { planDraft.value.params = clone(area.params); if (!planDraft.value.name) planDraft.value.name = `${area.name}巡检` } }
async function newPlan (areaId?: string) { if (!await confirmDiscard()) return; planDraft.value = blankPlan(); const area = data.value.areas.find(a => a.id === areaId && a.enabled) || data.value.areas.find(a => a.enabled); if (area) changePlanArea(area.id); planBaseline.value = JSON.stringify(planDraft.value); planEditing.value = true }
function createPlanForArea (id: string) { navigate('plans', { area: id, create: '1' }) }
function editPlan (plan: ConstructionPlan) { planDraft.value = clone(plan); planBaseline.value = JSON.stringify(planDraft.value); planEditing.value = true; if (isPlanStale(plan)) ElMessage.warning('区域已更新或暂停，请重新选择区域确认参数') }
async function closePlan () { if (!await confirmDiscard()) return; planEditing.value = false }
function savePlanDraft () { try { savePlan(planDraft.value); planEditing.value = false; ElMessage.success('演示计划已保存，未创建真实飞行任务') } catch (error) { fail(error) } }
function chooseKmz (event: Event) { const input = event.target as HTMLInputElement; const file = input.files?.[0]; if (file) { if (/\.kmz$/i.test(file.name)) planDraft.value.fileName = file.name; else ElMessage.error('请选择KMZ文件') } input.value = '' }
function previewPlan (plan: ConstructionPlan) { try { const task = previewTask(plan); openTask(task.id) } catch (error) { fail(error) } }
function openTask (id: string) { navigate('tasks', { task: id }) }
const detailTabs = [{ key: 'photos', label: '本地照片' }, { key: 'analysis', label: '分析与复核' }, { key: 'report', label: '巡检报告' }, { key: 'overview', label: '区域与参数' }]
const detailTab = ref('photos'); const reviewState = ref('pending')
watch(() => route.fullPath, () => {
  areaEditing.value = false; drawing.value = false; planEditing.value = false; detailTab.value = 'photos'; reviewState.value = 'pending'
  const area = data.value.areas.find(a => a.id === areaDraft.value?.id) || data.value.areas[0]
  areaDraft.value = area ? clone(area) : null; areaBaseline.value = JSON.stringify(areaDraft.value)
  if (page.value === 'plans' && route.query.create === '1') newPlan(typeof route.query.area === 'string' ? route.query.area : undefined)
}, { immediate: true })
interface Photo { id: string; name: string; url: string; lens: string; sample: boolean }
const photos = ref<Record<string, Photo[]>>({}); const activePhoto = ref<Photo | null>(null); const photoFilter = ref('all'); const importLens = ref('visible')
const photoDialogVisible = computed({ get: () => !!activePhoto.value, set: (value: boolean) => { if (!value) activePhoto.value = null } })
const lensFilters = [{ key: 'all', label: '全部照片' }, { key: 'visible', label: '可见光' }, { key: 'infrared', label: '红外' }]
function sampleImage (infrared: boolean, index: number) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="640" height="400" viewBox="0 0 640 400"><rect width="640" height="400" fill="${infrared ? '#241e48' : '#263d43'}"/><path d="M-40 310L270 160L690 220" fill="none" stroke="${infrared ? '#53447e' : '#637b7d'}" stroke-width="55"/><rect x="140" y="110" width="180" height="105" rx="8" fill="${infrared ? '#a64972' : '#86938b'}" transform="rotate(-12 230 160)"/><circle cx="455" cy="140" r="64" fill="none" stroke="${infrared ? '#de8264' : '#b2b9a0'}" stroke-width="20"/><path d="M455 140L455 80M455 140L400 175M455 140L510 175" stroke="#c7cabb" stroke-width="8"/><rect x="20" y="20" width="220" height="32" rx="4" fill="#0c1b28"/><text x="32" y="42" fill="#b5d9db" font-size="15" font-family="sans-serif">${infrared ? 'INFRARED' : 'VISIBLE'} · DEMO ${index}</text><text x="320" y="350" fill="#c8dbdc" text-anchor="middle" font-size="19" font-family="sans-serif">SCHEMATIC · NOT A FIELD PHOTO</text></svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}
const samples: Photo[] = [1, 2, 3].map((n) => ({ id: `sample-${n}`, name: `施工区域示意 ${String(n).padStart(2, '0')}`, url: sampleImage(n === 3, n), lens: n === 3 ? 'infrared' : 'visible', sample: true }))
const taskPhotos = computed(() => selectedTask.value ? photos.value[selectedTask.value.id] || [] : [])
const filteredPhotos = computed(() => [...taskPhotos.value, ...(selectedTask.value?.status === 'sample' ? samples : [])].filter(p => photoFilter.value === 'all' || p.lens === photoFilter.value))
function importPhotos (event: Event) {
  const input = event.target as HTMLInputElement
  const task = selectedTask.value
  if (!task) return
  const existing = photos.value[task.id] || []
  const files = Array.from(input.files || [])
  let rejected = 0
  for (const file of files) {
    if (existing.length >= 20 || file.size > 20 * 1024 * 1024 || !['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) { rejected++; continue }
    existing.push({ id: newId('photo'), name: file.name, url: URL.createObjectURL(file), lens: importLens.value, sample: false })
  }
  photos.value[task.id] = existing
  input.value = ''
  if (rejected) ElMessage.warning(`${rejected}个文件因类型、大小或数量限制未加入预览`)
}
function revokePhotos () { Object.values(photos.value).flat().forEach(p => URL.revokeObjectURL(p.url)); photos.value = {}; activePhoto.value = null }
function download (content: string, name: string) { const url = URL.createObjectURL(new Blob([content], { type: 'application/json;charset=utf-8' })); const anchor = document.createElement('a'); anchor.href = url; anchor.download = name; anchor.click(); setTimeout(() => URL.revokeObjectURL(url), 1000) }
function exportDemo () { try { download(exportRaw(), '工地巡检-浏览器演示数据.json') } catch (error) { fail(error) } }
function exportTask () { if (selectedTask.value) download(JSON.stringify({ notice: '仅为前端演示摘要，不是实际执行记录或正式报告', task: selectedTask.value }, null, 2), '工地巡检-演示任务摘要.json') }
async function resetDemo () {
  try { await ElMessageBox.confirm('将清除本模块的浏览器演示区域、计划、任务及临时照片预览。真实平台数据不受影响。', '恢复示例数据', { confirmButtonText: '恢复示例', cancelButtonText: '取消', type: 'warning' }) } catch { return }
  try { reset(); areaEditing.value = false; planEditing.value = false; drawing.value = false; areaDraft.value = clone(data.value.areas[0]); areaBaseline.value = JSON.stringify(areaDraft.value); revokePhotos(); navigate('areas'); ElMessage.success('已恢复示例数据') } catch (error) { fail(error) }
}
onBeforeUnmount(() => { window.removeEventListener('beforeunload', beforeUnload); revokePhotos() })
</script>

<style scoped src="./construction.css"></style>
