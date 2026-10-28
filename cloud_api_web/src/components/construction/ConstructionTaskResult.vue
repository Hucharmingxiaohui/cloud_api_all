<template>
  <main class="construction-shell ctr-shell" v-loading="busy">
    <header class="result-heading">
      <div class="heading-title"><span class="eyebrow">工地巡检 / 任务结果</span><h2>{{ detail?.name || '工地巡检任务结果' }}</h2><p>任务编号 <span>{{ detail?.job_id || jobId }}</span></p></div>
      <div class="heading-actions"><el-button :disabled="!detail?.media_complete || busy" @click="exportTask">导出离线任务包</el-button><el-button @click="load">刷新结果</el-button><el-button @click="back">返回飞行任务</el-button></div>
    </header>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="error-banner" />
    <template v-if="detail">
      <el-alert v-if="exportPath" :title="'离线任务包已生成：' + exportPath" description="将此 ZIP 拷到另一台平台显示的接收目录，复制完成后点击刷新并接收。两端均支持此功能。" type="success" :closable="false" show-icon />
      <el-alert v-if="detail.transfer_source" :title="'离线导入 · 来源部署 ' + detail.transfer_source.deployment + ' · 原任务 ' + detail.transfer_source.job_id" type="info" :closable="false" />
      <el-alert v-if="detail.analysis_stale" title="照片归属已变更，已有分析结果属于旧版本，请重新分析。" type="warning" :closable="false" show-icon />
      <section class="stat-cards" aria-label="任务概览">
        <div class="stat-card"><span>照片接收</span><strong>{{ detail.received_count }}<em>/ {{ detail.expected_count ?? '未知' }} 张</em></strong><small :class="detail.media_complete ? 'ok' : 'warn'">{{ detail.media_complete ? '● 媒体已收齐' : '● 完整性待确认' }}</small></div>
        <div class="stat-card"><span>本地原图</span><strong>{{ archivedCount }}<em>张已归档</em></strong><div class="archive-track"><i :style="{ width: archivePercent + '%' }"></i></div><small>{{ detail.photos.length - archivedCount }} 张待归档 · 本部署端保存</small></div>
        <div class="stat-card"><span>巡检范围</span><strong>{{ areaGroups.filter(g => !g.warn).length }}<em>个照片分区</em></strong><small>{{ overall ? overallWaypointCount + ' 个绑定航点 · 总体航线任务' : '单区域航线任务' }}</small></div>
        <div class="stat-card analysis-stat"><span>图像分析</span><strong class="status-value">{{ statusLabel }}</strong><small>{{ detail.analysis.batch_id ? '已关联本次飞行任务' : '回站连接主机后手动提交' }}</small></div>
      </section>

      <div class="result-workspace">
        <aside class="region-sidebar">
          <div class="sidebar-heading"><h3>巡检区域</h3><span>{{ areaGroups.length }} 个分组</span></div>
          <button class="region-choice" :class="{ selected: selectedArea === '' }" @click="selectedArea = ''"><span class="region-icon">▦</span><span class="region-copy"><b>全部照片</b><small>本次飞行采集</small></span><strong>{{ detail.photos.length }}</strong></button>
          <nav class="region-list" aria-label="按区域筛选">
            <button v-for="(group, i) in areaGroups" :key="group.key" class="region-choice" :class="{ selected: selectedArea === group.key, warning: group.warn }" @click="selectedArea = group.key">
              <span class="region-icon">{{ group.warn ? '!' : String(i + 1).padStart(2, '0') }}</span><span class="region-copy"><b>{{ group.name }}</b><small>{{ group.key === '__excluded' ? '不参与本次分析' : group.warn ? '待核对照片归属' : group.waypoints.length ? group.waypoints.length + ' 个绑定航点' : '本次任务照片' }}</small></span><strong>{{ group.photos.length }}</strong>
            </button>
          </nav>
          <div class="sidebar-note"><b>本次任务快照</b><p>照片按执行时的区域与航点归类，后续修改航线不会改变本次结果。</p><span v-if="overall">{{ overall.name || '总体航线' }}</span></div>
        </aside>

        <section class="result-main">
          <el-tabs v-model="tab" class="ctr-tabs">
            <el-tab-pane label="本地照片" name="photos">
              <div class="gallery-toolbar">
                <div><h3>{{ activeAreaName }}</h3><p>共 {{ filteredPhotos.length }} 张照片 · 点击已归档照片查看原图</p></div>
                <div class="gallery-actions"><el-input v-model="keyword" clearable placeholder="搜索照片名称 / 航点" aria-label="搜索照片" /><el-select v-model="archiveFilter" aria-label="归档状态"><el-option label="全部状态" value="all" /><el-option label="已归档" value="archived" /><el-option label="待归档" value="pending" /></el-select><el-button :disabled="busy || !detail.photos.length" @click="archive">保存 / 补齐本地原图</el-button></div>
              </div>
              <div v-if="selectedArea === '__unmatched'" class="inline-notice">这些照片尚未关联到工作面，请核对拍照命名与航点绑定；不能直接用于工作面人数汇总。</div>
              <div v-if="overall" class="association-toolbar"><span>{{ unresolvedCount }} 张照片待确认工作面；已排除 {{ detail.photos.filter(p => p.excluded).length }} 张</span><el-button :disabled="busy || associationLocked" @click="openAssociations">确认照片归属</el-button><small v-if="associationLocked">分析批次进行中，归属暂不可修改</small></div>
              <div class="gallery-content">
                <section v-for="group in visibleGroups" :key="group.key" class="photo-group" :class="{ warn: group.warn }">
                  <div class="group-heading"><h3>{{ group.name }} <small>{{ group.photos.length }} 张</small></h3><span v-if="group.waypoints.length">航点 {{ group.waypoints.join(' · ') }}</span><span v-else-if="group.warn">{{ group.name }}</span></div>
                  <div class="photo-grid">
                    <button v-for="photo in group.photos" :key="photo.file_id" class="photo-card" :class="{ archived: photo.archived }" :disabled="!photo.archived" @click="preview(photo)">
                      <div class="photo-thumb"><img v-if="thumbs[photo.file_id]" :src="thumbs[photo.file_id]" :alt="photo.name" loading="lazy" @error="thumbs[photo.file_id] = ''"><div v-else class="thumb-placeholder"><span>▧</span>{{ !photo.archived ? '原图待归档' : thumbs[photo.file_id] === '' ? '预览不可用 · 点击重试' : '正在读取本地照片' }}</div><span class="photo-state" :class="{ saved: photo.archived }">{{ photo.archived ? '已归档' : '待归档' }}</span><span v-if="photo.waypoint_index" class="waypoint-chip">航点 {{ photo.waypoint_index }}</span><div v-if="photo.archived" class="photo-hover">查看原图 ↗</div></div>
                      <div class="photo-caption"><b :title="photo.name">{{ photo.name }}</b><small>{{ photo.excluded ? '已排除分析' : photo.area_name || areaName(photo.area_id) }}{{ photo.association === 'MANUAL' ? ' · 人工确认' : '' }}</small></div>
                    </button>
                  </div>
                </section>
                <el-empty v-if="!filteredPhotos.length" :description="keyword || archiveFilter !== 'all' ? '没有符合筛选条件的照片' : '当前区域暂无接收照片'" :image-size="90"><el-button v-if="keyword || archiveFilter !== 'all'" @click="keyword = ''; archiveFilter = 'all'">清除筛选</el-button></el-empty>
              </div>
              <div class="gallery-footer"><span>原始照片保留在本部署端</span><el-pagination v-model:current-page="page" :page-size="24" :total="filteredPhotos.length" layout="total, prev, pager, next" :pager-count="5" hide-on-single-page /></div>
            </el-tab-pane>

            <el-tab-pane label="分析与结果" name="analysis">
              <div class="analysis-workspace">
                <section class="analysis-console">
                  <div class="section-title"><div><span class="eyebrow">站内图像分析</span><h3>回站后，一次提交本次任务</h3></div><span class="badge" :class="analysisComplete ? 'success' : 'info'">{{ statusLabel }}</span></div>
                  <div class="analysis-steps"><div :class="{ done: detail.media_complete }"><b>01</b><span>照片收齐<small>{{ detail.media_complete ? '已完成' : '等待采集完成' }}</small></span></div><div :class="{ done: connected }"><b>02</b><span>连接主机<small>{{ connected === null ? '未检查' : connected ? '连接正常' : '连接失败' }}</small></span></div><div :class="{ done: !!detail.analysis.batch_id }"><b>03</b><span>手动分析<small>{{ detail.analysis.batch_id ? '已有分析批次' : '待提交' }}</small></span></div><div :class="{ done: analysisComplete }"><b>04</b><span>结果回传<small>{{ analysisComplete ? '分析完成' : '等待分析结果' }}</small></span></div></div>
                  <el-alert v-if="!detail.analysis_configured" title="分析服务尚未配置，当前可查看及归档本地照片。" type="info" :closable="false" show-icon />
                  <p v-if="detail.analysis.message" class="inline-notice">{{ detail.analysis.message }}</p>
                  <div class="analysis-rows"><div><span>分析主机</span><b>{{ connected === null ? '未检查' : connected ? '可连接' : '不可连接' }}</b></div><div><span>当前批次</span><b>{{ detail.analysis.batch_id || '尚未提交' }}</b></div></div>
                  <div class="analysis-actions"><el-button :disabled="busy || !detail.analysis_configured" @click="check">检查分析主机</el-button><el-button type="primary" :disabled="!canSubmit" @click="submit">{{ detail.analysis.batch_id ? '重新分析' : '提交分析' }}</el-button><el-button :disabled="busy || !detail.analysis.batch_id || !detail.analysis_configured" @click="refresh">查询进度 / 取回结果</el-button></div>
                  <p class="helper">{{ submitHint }}。提交覆盖本次任务全部待分析照片，不受左侧区域筛选影响。</p>
                </section>
                <section class="analysis-results"><div class="section-title"><h3>{{ activeAreaName }} · 人数统计</h3><span>同一工作面跨角度去重</span></div>
                  <div v-if="areaCounts.length" class="people-grid"><article v-for="item in areaCounts" :key="item.area_id"><span>{{ areaName(item.area_id) }}</span><strong>{{ item.status === 'success' && Number.isInteger(item.count) && item.count >= 0 ? item.count : '—' }}<small>人</small></strong><p>{{ countStatus(item.status) }}</p><small v-if="item.message">{{ item.message }}</small></article></div>
                  <el-empty v-else :description="detail.analysis.result ? '当前结果尚未包含该区域的去重人数' : '尚无已回传的分析结果；未分析不代表无隐患'" :image-size="80" />
                  <p class="helper">仅展示算法返回的区域去重人数，不将多张照片人数相加。未成功统计显示“—”，确认无人显示“0”。</p>
                  <details v-if="detail.analysis.result" class="raw-result"><summary>查看服务返回的原始结果</summary><pre>{{ JSON.stringify(detail.analysis.result, null, 2) }}</pre></details>
                </section>
              </div>
            </el-tab-pane>

            <el-tab-pane label="巡检报告" name="report">
              <section class="report-panel"><div class="section-title"><div><span class="eyebrow">一次任务 · 一份报告</span><h3>工地巡检报告</h3></div><span class="badge warning">正式报告待接入</span></div><div class="report-sheet"><div class="report-cover"><span>施工现场巡检</span><h2>{{ detail.name }}</h2><p>任务编号 {{ detail.job_id }}</p><div class="report-stamp">资料概览</div></div><div class="report-body"><h3>巡检资料汇总</h3><div class="report-summary"><span>接收照片 <b>{{ detail.received_count }} 张</b></span><span>本地归档 <b>{{ archivedCount }} 张</b></span><span>分析状态 <b>{{ statusLabel }}</b></span></div><h3>区域章节</h3><div v-for="(group, index) in areaGroups" :key="group.key" class="report-chapter"><span>{{ String(index + 1).padStart(2, '0') }}</span><b>{{ group.name }}</b><small>{{ group.photos.length }} 张照片</small><el-button link type="primary" @click="selectedArea = group.key; tab = 'photos'">查看资料 →</el-button></div><div class="report-notice">当前展示已采集资料，不构成最终巡检结论。正式报告将按区域汇总检测结果、去重人数与异常证据图；待分析服务及报告接口接入后开放生成与下载。</div></div></div></section>
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>
    </template>
    <el-empty v-else-if="!busy" description="任务结果加载失败"><el-button @click="load">重新加载</el-button></el-empty>

    <el-dialog v-model="associationVisible" title="确认照片所属工作面" width="min(1000px, 94vw)" append-to-body :close-on-click-modal="false" :show-close="!busy" :close-on-press-escape="!busy">
      <div class="association-form"><p>仅在本次任务的工作面中确认归属。不能确定的照片可暂不处理；排除分析必须填写原因，不会删除原图。</p>
        <el-checkbox v-model="onlyUnresolved" :disabled="busy">只显示待确认照片</el-checkbox>
        <el-table v-if="associationVisible" :data="associationPhotos" row-key="file_id" max-height="340" @selection-change="associationSelection = $event.map(p => p.file_id)">
          <el-table-column type="selection" width="45" :selectable="() => !busy" /><el-table-column prop="name" label="照片" min-width="220" /><el-table-column label="当前归属" min-width="120"><template #default="{ row }">{{ row.excluded ? '已排除' : row.area_name || '待确认' }}</template></el-table-column><el-table-column label="原因" min-width="180"><template #default="{ row }">{{ row.association_message || '按航点标识关联' }}</template></el-table-column><el-table-column width="85"><template #default="{ row }"><el-button link :disabled="!row.archived" @click="preview(row)">看原图</el-button></template></el-table-column>
        </el-table>
        <p>已选 {{ associationSelection.length }} 张（每批最多500张）</p><el-radio-group v-model="associationMode" :disabled="busy"><el-radio label="area">确认工作面</el-radio><el-radio label="exclude">排除分析</el-radio></el-radio-group>
        <el-select v-if="associationMode === 'area'" v-model="associationArea" placeholder="选择本次任务的工作面" :disabled="busy"><el-option v-for="area in overall?.bindings || []" :key="area.area_id" :label="area.area_name || area.area_id" :value="area.area_id" /></el-select>
        <el-input v-else v-model="exclusionReason" placeholder="排除原因，例如转场测试照片" maxlength="200" show-word-limit :disabled="busy" />
        <p v-if="error" class="viewer-error-text">{{ error }}</p>
      </div><template #footer><el-button :disabled="busy" @click="associationVisible = false">取消</el-button><el-button type="primary" :loading="busy" :disabled="!associationSelection.length || associationSelection.length > 500 || (associationMode === 'area' ? !associationArea : !exclusionReason.trim())" @click="saveAssociations">保存确认</el-button></template>
    </el-dialog>

    <el-dialog v-model="photoVisible" title="巡检照片" width="min(1240px, 94vw)" top="5vh" class="construction-photo-dialog" append-to-body @closed="zoom = 1">
      <div v-if="selectedPhoto" class="viewer-layout"><div class="viewer-stage" v-loading="previewBusy"><div class="viewer-canvas"><img v-if="photoUrl" :src="photoUrl" :alt="selectedPhoto.name" :style="{ width: (zoom * 100) + '%', maxHeight: zoom === 1 ? '58vh' : 'none' }" @error="previewError = '图片无法解码，可下载原图使用专用软件查看'"><p v-if="previewError" class="viewer-error">{{ previewError }} <el-button @click="preview(selectedPhoto)">重试</el-button></p></div><div class="viewer-controls"><el-button :disabled="previewIndex <= 0 || previewBusy" @click="movePreview(-1)">← 上一张</el-button><span>{{ previewIndex + 1 }} / {{ previewPhotos.length }}</span><el-button :disabled="previewIndex >= previewPhotos.length - 1 || previewBusy" @click="movePreview(1)">下一张 →</el-button><el-button @click="zoom = zoom === 1 ? 2 : 1">{{ zoom === 1 ? '放大 2×' : '适应窗口' }}</el-button></div></div><aside class="viewer-info"><span>原始采集照片</span><h3>{{ selectedPhoto.name }}</h3><dl><dt>巡检区域</dt><dd>{{ selectedPhoto.area_name || areaName(selectedPhoto.area_id) }}</dd><dt>拍摄航点</dt><dd>{{ selectedPhoto.waypoint_index ? '航点 ' + selectedPhoto.waypoint_index : '未关联' }}</dd><dt>保存状态</dt><dd>已归档至本部署端</dd></dl><p v-if="selectedPhoto.association_message">{{ selectedPhoto.association_message }}</p><el-button type="primary" :disabled="!photoUrl || previewBusy" @click="downloadPhoto">下载原图</el-button><small>当前为采集原图，非算法标注结果图。</small></aside></div>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, defineProps, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ERouterName } from '/@/types'
import { uuidv4 } from '/@/utils/uuid'
import { getConstructionResult, archiveConstructionPhotos, checkConstructionHost, submitConstructionAnalysis, refreshConstructionAnalysis, getConstructionPhoto, assignConstructionPhotos, exportConstructionTask } from '/@/api/construction'
import type { ConstructionResult } from '/@/api/construction'

type Photo = ConstructionResult['photos'][number]
interface PhotoGroup { key: string; name: string; waypoints: number[]; photos: Photo[]; warn?: boolean }
const props = defineProps<{ jobId: string }>()
const router = useRouter(); const tab = ref('photos'); const busy = ref(false); const error = ref('')
const detail = ref<ConstructionResult | null>(null); const connected = ref<boolean | null>(null)
const exportPath = ref('')
const selectedArea = ref(''); const keyword = ref(''); const archiveFilter = ref('all'); const page = ref(1)
const overall = computed(() => detail.value?.overall_route?.overall_id ? detail.value.overall_route : null)
const areaGroups = computed<PhotoGroup[]>(() => {
  const d = detail.value; if (!d) return []
  if (!overall.value) return [{ key: 'all', name: d.overall_route?.bindings?.[0]?.area_name || '巡检照片', waypoints: [], photos: d.photos }]
  const groups: PhotoGroup[] = (overall.value.bindings || []).map(b => ({ key: b.area_id, name: b.area_name || b.area_id, waypoints: b.waypoints || [], photos: d.photos.filter(p => !p.excluded && p.area_id === b.area_id) }))
  const keys = new Set(groups.map(g => g.key))
  const rest = d.photos.filter(p => !p.excluded && (!p.area_id || !keys.has(p.area_id)))
  if (rest.length) groups.push({ key: '__unmatched', name: '未匹配 / 未绑定', waypoints: [], photos: rest, warn: true })
  const excluded = d.photos.filter(p => p.excluded)
  if (excluded.length) groups.push({ key: '__excluded', name: '已排除分析', waypoints: [], photos: excluded, warn: true })
  return groups
})
const activeAreaName = computed(() => areaGroups.value.find(g => g.key === selectedArea.value)?.name || '全部区域')
const filteredPhotos = computed(() => areaGroups.value.filter(g => !selectedArea.value || g.key === selectedArea.value).flatMap(g => g.photos).filter(p => {
  const term = keyword.value.trim().toLowerCase()
  return (!term || (p.name + ' 航点 ' + (p.waypoint_index || '')).toLowerCase().includes(term)) && (archiveFilter.value === 'all' || (archiveFilter.value === 'archived' ? p.archived : !p.archived))
}))
const pagePhotos = computed(() => filteredPhotos.value.slice((page.value - 1) * 24, page.value * 24))
const visibleGroups = computed(() => {
  if (!selectedArea.value) return pagePhotos.value.length ? [{ key: '__all', name: '采集照片', waypoints: [], photos: pagePhotos.value, warn: false }] : []
  const ids = new Set(pagePhotos.value.map(p => p.file_id))
  return areaGroups.value.map(g => ({ ...g, photos: g.photos.filter(p => ids.has(p.file_id)) })).filter(g => g.photos.length)
})
const overallWaypointCount = computed(() => (overall.value?.bindings || []).reduce((sum, b) => sum + (b.waypoints || []).length, 0))
const archivedCount = computed(() => (detail.value?.photos || []).filter(p => p.archived).length)
const archivePercent = computed(() => detail.value?.photos.length ? archivedCount.value / detail.value.photos.length * 100 : 0)
const labels: Record<string, string> = { SUBMITTING: '提交结果待确认', UNKNOWN: '提交结果待确认', QUEUED: '排队中', RUNNING: '分析中', SUCCEEDED: '分析完成', FAILED: '分析失败' }
const statusLabel = computed(() => detail.value?.analysis_stale ? '归属变更待重分析' : labels[detail.value?.analysis.status || ''] || '待手动分析')
const analysisComplete = computed(() => !detail.value?.analysis_stale && detail.value?.analysis.status === 'SUCCEEDED')
const unresolvedCount = computed(() => detail.value?.unresolved_count ?? (overall.value ? detail.value?.photos.filter(p => !p.area_id && !p.excluded).length || 0 : 0))
const associationLocked = computed(() => !!detail.value?.analysis.batch_id && !['SUCCEEDED', 'FAILED'].includes(detail.value.analysis.status || ''))
const canSubmit = computed(() => !busy.value && unresolvedCount.value === 0 && detail.value?.photos.some(p => !p.excluded) && connected.value === true && detail.value?.analysis_configured && detail.value?.media_complete && !associationLocked.value)
const submitHint = computed(() => unresolvedCount.value ? '请先在本地照片页确认所有待定照片归属，或填写排除原因' : !detail.value?.analysis_configured ? '请先配置站内分析服务' : !detail.value.media_complete ? '等待本次飞行完成并收齐照片' : associationLocked.value ? '请查询已有批次，避免重复下发' : !detail.value.photos.some(p => !p.excluded) ? '所有照片均已排除，暂无可分析照片' : connected.value !== true ? '接入站内网络后，请先检查主机连接' : '已具备提交条件，重新分析将保留原批次记录')
interface AreaCount { area_id: string; status: string; count: number | null; message?: string }
const areaCounts = computed<AreaCount[]>(() => {
  const values = detail.value?.analysis.result?.areas
  if (detail.value?.analysis_stale) return []
  if (!Array.isArray(values)) return []
  return values.filter((v): v is AreaCount => !!v && typeof v.area_id === 'string' && typeof v.status === 'string').filter(v => !selectedArea.value || selectedArea.value === 'all' || v.area_id === selectedArea.value)
})
function areaName (id?: string) { return areaGroups.value.find(g => g.key === id)?.name || id || (overall.value ? '未匹配 / 未绑定' : '巡检照片') }
function countStatus (status: string) { return ({ success: '区域去重人数', unavailable: '模型不可用', not_applicable: '不适用', unassessable: '无法判断', failed: '统计失败', not_requested: '未要求统计' } as Record<string, string>)[status] || '统计待确认' }

const thumbs = ref<Record<string, string>>({}); let disposed = false; let epoch = 0; let thumbEpoch = 0; let requestId = uuidv4()
const photoVisible = ref(false); const selectedPhoto = ref<Photo | null>(null); const photoUrl = ref(''); const previewBusy = ref(false); const previewError = ref(''); const zoom = ref(1); let previewEpoch = 0
const previewPhotos = computed(() => filteredPhotos.value.filter(p => p.archived))
const previewIndex = computed(() => previewPhotos.value.findIndex(p => p.file_id === selectedPhoto.value?.file_id))
function back () { router.push('/' + ERouterName.TASK) }
async function run (action: (token: number, job: string) => Promise<void>) {
  if (busy.value) return
  const token = epoch; const job = props.jobId
  busy.value = true; error.value = ''
  try { await action(token, job) } catch (cause: any) { if (token === epoch && !disposed) { error.value = cause?.response?.data?.message || cause?.message || '请求失败，请检查本地服务和主机连接'; connected.value = null } } finally { if (token === epoch && !disposed) busy.value = false }
}
function current (token: number) { return token === epoch && !disposed }
const associationVisible = ref(false); const associationSelection = ref<string[]>([]); const onlyUnresolved = ref(true)
const associationMode = ref('area'); const associationArea = ref(''); const exclusionReason = ref('')
const associationPhotos = computed(() => (detail.value?.photos || []).filter(p => !onlyUnresolved.value || (!p.area_id && !p.excluded)))
function openAssociations () { associationSelection.value = []; onlyUnresolved.value = unresolvedCount.value > 0; associationArea.value = ''; associationMode.value = 'area'; exclusionReason.value = ''; associationVisible.value = true }
async function saveAssociations () {
  if (!detail.value || associationLocked.value || !associationSelection.value.length) return
  const body = { file_ids: [...associationSelection.value], expected_revision: detail.value.association_revision || 0, area_id: associationMode.value === 'area' ? associationArea.value : undefined, excluded: associationMode.value === 'exclude', reason: exclusionReason.value.trim() }
  await run(async (token, job) => { const data = await assignConstructionPhotos(job, body); if (current(token)) { detail.value = data; associationVisible.value = false; associationSelection.value = [] } })
}
async function load () { await run(async (token, job) => { const data = await getConstructionResult(job); if (current(token)) detail.value = data }) }
async function exportTask () { await run(async (token, job) => { const data = await exportConstructionTask(job); if (current(token)) exportPath.value = data.path }) }
async function archive () { await run(async (token, job) => { const data = await archiveConstructionPhotos(job); if (current(token)) detail.value = data }) }
async function loadThumbs () {
  const token = ++thumbEpoch; const job = props.jobId
  const photos = [...pagePhotos.value].filter(p => p.archived)
  // Keep only the visible page in memory: the media endpoint currently returns original files.
  const keep = new Set(photos.map(p => p.file_id))
  for (const [id, url] of Object.entries(thumbs.value)) { if (!keep.has(id)) { if (url) URL.revokeObjectURL(url); delete thumbs.value[id] } }
  let index = 0
  await Promise.all([0, 1, 2].map(async () => {
    while (index < photos.length) {
      if (token !== thumbEpoch || disposed) return
      const photo = photos[index++]
      if (thumbs.value[photo.file_id] !== undefined) continue
      try { const blob = await getConstructionPhoto(job, photo.file_id); if (token !== thumbEpoch || disposed) return; thumbs.value[photo.file_id] = URL.createObjectURL(blob) } catch { if (token !== thumbEpoch || disposed) return; thumbs.value[photo.file_id] = '' }
    }
  }))
}
async function check () { await run(async (token, job) => { const data = await checkConstructionHost(job); if (current(token)) connected.value = data.connected }) }
async function submit () {
  if (!canSubmit.value) return
  const job = props.jobId
  try { await ElMessageBox.confirm('将提交本次飞行任务的照片进行分析，包含全部巡检区域。是否继续？', '手动提交分析', { confirmButtonText: '提交分析', cancelButtonText: '取消' }) } catch { return }
  if (job !== props.jobId || !canSubmit.value) return
  await run(async (token, id) => { const batch = await submitConstructionAnalysis(id, requestId); if (current(token) && detail.value) { detail.value.analysis = batch; requestId = uuidv4() } })
}
async function refresh () { await run(async (token, job) => { const batch = await refreshConstructionAnalysis(job); if (current(token) && detail.value) detail.value.analysis = batch }) }
async function preview (photo: Photo) {
  if (!photo.archived) return
  const token = ++previewEpoch
  selectedPhoto.value = photo; photoVisible.value = true; previewBusy.value = true; previewError.value = ''; zoom.value = 1
  if (photoUrl.value) URL.revokeObjectURL(photoUrl.value)
  photoUrl.value = ''
  try { const blob = await getConstructionPhoto(props.jobId, photo.file_id); if (disposed || token !== previewEpoch) return; photoUrl.value = URL.createObjectURL(blob) } catch { if (token === previewEpoch && !disposed) previewError.value = '原图加载失败，请重试' } finally { if (token === previewEpoch && !disposed) previewBusy.value = false }
}
function movePreview (offset: number) { const photo = previewPhotos.value[previewIndex.value + offset]; if (photo) preview(photo) }
function downloadPhoto () { if (!photoUrl.value || !selectedPhoto.value) return; const link = document.createElement('a'); link.href = photoUrl.value; link.download = selectedPhoto.value.name; link.click() }
function release () { ++thumbEpoch; ++previewEpoch; for (const url of Object.values(thumbs.value)) if (url) URL.revokeObjectURL(url); thumbs.value = {}; if (photoUrl.value) URL.revokeObjectURL(photoUrl.value); photoUrl.value = '' }
watch([selectedArea, keyword, archiveFilter], () => { page.value = 1 })
watch(pagePhotos, () => { loadThumbs() })
watch(filteredPhotos, photos => { page.value = Math.min(page.value, Math.max(1, Math.ceil(photos.length / 24))) })
watch(() => props.jobId, () => { ++epoch; release(); detail.value = null; connected.value = null; busy.value = false; photoVisible.value = false; selectedArea.value = ''; keyword.value = ''; archiveFilter.value = 'all'; page.value = 1; tab.value = 'photos'; requestId = uuidv4(); load() }, { immediate: true })
onBeforeUnmount(() => { disposed = true; ++epoch; release() })
</script>

<style scoped src="./construction.css"></style>
<style scoped src="./construction-result.css"></style>
