<template>
  <section class="eua-result">
    <header class="result-header">
      <div class="header-info">
        <div class="eyebrow">EUA · 飞行结果</div>
        <h2>{{ route.query.taskName || 'EUA任务结果' }}</h2>
        <dl class="task-metadata">
          <div><dt>所属计划</dt><dd>{{ route.query.planName || '—' }}</dd></div>
          <div><dt>任务编码</dt><dd>{{ route.query.businessId || '—' }}</dd></div>
          <div><dt>EUA 任务 ID</dt><dd>{{ route.query.euaTaskId || '—' }}</dd></div>
        </dl>
      </div>
      <div class="header-actions">
        <el-button type="primary" :loading="reportSubmitting" :disabled="reportSubmitting || reportStatus.status === 'GENERATING'" @click="generateReport">{{ reportStatus.status === 'GENERATING' ? '报告生成中' : '生成报告' }}</el-button>
        <el-button :loading="reportDownloading" @click="exportReport">导出报告</el-button>
        <el-button :icon="Refresh" type="primary" :loading="loading" @click="loadResults">刷新结果</el-button>
        <el-button @click="router.push({ name: 'euaTaskRecord' })">返回飞行任务</el-button>
      </div>
    </header>

    <div v-if="reportStatus.status !== 'NOT_GENERATED' || reportError" class="report-status" role="status">
      {{ reportError || reportStatus.message || reportStatusText }}
      <span v-if="reportStatus.status === 'COMPLETED' && !reportError"> · 点击“导出报告”下载 Word，图片更新后可重新生成。</span>
    </div>

    <div class="result-toolbar">
      <span>图片结果 <strong>{{ rows.length }}</strong></span>
      <el-input v-model="keyword" placeholder="搜索图片名称 / 点位" clearable />
      <el-select v-model="type" placeholder="图片类型">
        <el-option label="全部类型" value="all" />
        <el-option label="可见光" value="0" />
        <el-option label="红外" value="1" />
      </el-select>
    </div>

    <div v-loading="loading" element-loading-background="rgba(0, 18, 47, .8)" class="results">
      <div v-if="error" class="empty-state">{{ error }} <el-button link type="primary" @click="loadResults">重新加载</el-button></div>
      <div v-else-if="!loading && !filteredRows.length" class="empty-state">{{ rows.length ? '没有匹配的图片' : '暂无图片结果，任务可能尚未查图或分析完成，请稍后刷新' }}</div>
      <div v-else class="result-grid">
        <article v-for="row in filteredRows" :key="row.id" class="picture-card">
          <div class="card-heading">
            <div><h3 :title="row.pictureName">{{ row.pictureName || '未命名图片' }}</h3><span>{{ row.pointName || '未关联点位' }}</span></div>
            <span class="type-badge" :class="{ infrared: Number(row.pointType) === 1 }">{{ Number(row.pointType) === 1 ? '红外' : '可见光' }}</span>
          </div>
          <div class="image-pair" :class="{ 'single-image': !hasAnalysisImage(row) }">
            <div v-for="kind in visibleImageKinds(row)" :key="kind.key" class="image-cell">
              <div class="image-label">{{ kind.label }}</div>
              <el-image v-if="row[kind.key]" :src="row[kind.key]" :preview-src-list="[row[kind.key]]" fit="contain" :preview-teleported="true" hide-on-click-modal>
                <template #error><div class="image-placeholder">图片加载失败</div></template>
              </el-image>
              <div v-else class="image-placeholder">{{ row[kind.key + 'Message'] || '加载中…' }}</div>
            </div>
          </div>
          <div class="card-result">
            <span class="status" :class="'status-' + row.analysisStatus">{{ statusText(row.analysisStatus) }}</span>
            <strong>{{ row.pointVal || '-' }}</strong><span>{{ row.pointValUnit || '' }}</span>
          </div>
          <details><summary>图片路径</summary><p>FTP路径：{{ row.ftpPath || '未转存（本平台任务不向上级上报）' }}</p><p v-if="hasAnalysisImage(row)">分析图：{{ row.analysisImageUrl || '-' }}</p></details>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { downloadEuaTaskReport, generateEuaTaskReport, getEuaTaskReportStatus, getEuaTaskPictureImage, getEuaTaskPictureList } from '/@/api/cq-eua-plan'
import type { EuaTaskReportStatus } from '/@/api/cq-eua-plan'

const route = useRoute()
const router = useRouter()
const rows = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const keyword = ref('')
const type = ref('all')
const reportStatus = ref<EuaTaskReportStatus>({ status: 'NOT_GENERATED' })
const reportError = ref('')
const reportSubmitting = ref(false)
const reportDownloading = ref(false)
const reportStatusText = computed(() => ({ NOT_GENERATED: '尚未生成报告', GENERATING: '报告正在生成，请稍后导出', COMPLETED: '报告已生成', FAILED: '报告生成失败，请重新生成' })[reportStatus.value.status])
let reportVersion = 0
let reportTimer: ReturnType<typeof setTimeout> | undefined

function reportParams () {
  return { businessId: String(route.query.businessId || ''), euaTaskId: String(route.query.euaTaskId || '') }
}

function reportMessage (error: any) {
  return error?.response?.data?.message || error?.response?.data?.msg || error?.message || '报告请求失败，请稍后重试'
}

function scheduleReportStatus (version: number) {
  clearTimeout(reportTimer)
  if (version === reportVersion && reportStatus.value.status === 'GENERATING') {
    reportTimer = setTimeout(() => { refreshReportStatus(version).catch(() => {}) }, 3000)
  }
}

async function refreshReportStatus (version = reportVersion) {
  try {
    const response = await getEuaTaskReportStatus(reportParams())
    if (version !== reportVersion) return
    if (response.code !== 0 || !response.data) throw new Error(response.message || '查询报告状态失败')
    reportStatus.value = response.data
    reportError.value = ''
    scheduleReportStatus(version)
  } catch (error: any) {
    if (version === reportVersion) reportError.value = reportMessage(error)
    throw error
  }
}

async function generateReport () {
  const version = reportVersion
  reportSubmitting.value = true
  try {
    const response = await generateEuaTaskReport(reportParams())
    if (version !== reportVersion) return
    if (response.code !== 0 || !response.data) throw new Error(response.message || '生成报告失败')
    reportStatus.value = response.data
    reportError.value = ''
    ElMessage.success(response.message || '报告生成任务已提交，请稍后导出')
    scheduleReportStatus(version)
  } catch (error: any) {
    if (version === reportVersion) ElMessage.error(reportMessage(error))
  } finally {
    if (version === reportVersion) reportSubmitting.value = false
  }
}

async function exportReport () {
  const version = reportVersion
  reportDownloading.value = true
  try {
    await refreshReportStatus(version)
    if (version !== reportVersion) return
    if (reportStatus.value.status !== 'COMPLETED') {
      ElMessage.warning(reportStatus.value.status === 'NOT_GENERATED' ? '请先生成报告' : reportStatusText.value)
      return
    }
    const blob = await downloadEuaTaskReport(reportParams())
    if (version !== reportVersion) return
    if (blob.type.includes('json')) {
      const response = JSON.parse(await blob.text())
      throw new Error(response.message || response.msg || '报告尚未就绪，请稍后导出')
    }
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = (reportStatus.value.fileName || `${route.query.taskName || 'EUA巡视报告'}.docx`).replace(/[\\/:*?"<>|]/g, '_')
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
    setTimeout(() => URL.revokeObjectURL(url), 10000)
  } catch (error: any) {
    let message = reportMessage(error)
    if (error?.response?.data instanceof Blob) {
      try {
        const response = JSON.parse(await error.response.data.text())
        message = response.message || response.msg || message
      } catch (_) { /* 非JSON错误响应沿用请求错误。 */ }
    }
    if (version === reportVersion) ElMessage.error(message)
  } finally {
    if (version === reportVersion) reportDownloading.value = false
  }
}
const imageKinds = [{ key: 'originalPreview', label: '原图' }, { key: 'analysisPreview', label: '分析图' }]
function hasAnalysisImage (row: any) {
  return Number(row.pointType) !== 1 || Boolean(row.analysisImageUrl)
}

function visibleImageKinds (row: any) {
  return hasAnalysisImage(row) ? imageKinds : imageKinds.slice(0, 1)
}
let generation = 0
const objectUrls = new Set<string>()
const filteredRows = computed(() => rows.value.filter(row =>
  (type.value === 'all' || String(row.pointType) === type.value) &&
  `${row.pictureName} ${row.pointName}`.toLowerCase().includes(keyword.value.trim().toLowerCase())))

function releaseImages () {
  objectUrls.forEach(url => URL.revokeObjectURL(url))
  objectUrls.clear()
}

function statusText (value: any) {
  return ({ 0: '待分析', 1: '已分析', 2: '分析失败', 3: '测温完成' } as Record<string, string>)[String(value)] || '未知状态'
}

async function loadImage (row: any, analysis: boolean, version: number) {
  const key = analysis ? 'analysisPreview' : 'originalPreview'
  if (analysis && !row.analysisImageUrl) {
    row[key + 'Message'] = Number(row.pointType) === 1 ? '红外测温结果见下方，无独立分析图' : '暂无分析图'
    return
  }
  try {
    const blob = await getEuaTaskPictureImage(row.id, analysis)
    if (version !== generation) return
    if (!blob.type.startsWith('image/')) throw new Error('图片响应无效')
    const url = URL.createObjectURL(blob)
    objectUrls.add(url)
    row[key] = url
  } catch (e) {
    if (version === generation) row[key + 'Message'] = '图片不可用，请检查文件是否存在'
  }
}

async function loadResults () {
  const version = ++generation
  releaseImages()
  rows.value = []
  error.value = ''
  loading.value = true
  const businessId = String(route.query.businessId || '')
  const euaTaskId = String(route.query.euaTaskId || '')
  try {
    if (!businessId && !euaTaskId) throw new Error('缺少任务标识，请返回执行记录重新进入')
    const res: any = await getEuaTaskPictureList({ businessId, business_id: businessId, euaTaskId, eua_task_id: euaTaskId })
    if (version !== generation) return
    if (res.code !== 0) throw new Error(res.msg || res.message || '查询任务结果失败')
    rows.value = (res.data || []).map((item: any) => ({
      ...item,
      pictureName: item.pictureName ?? item.picture_name ?? '',
      pointName: item.pointName ?? item.point_name ?? '',
      pointType: item.pointType ?? item.point_type,
      analysisStatus: item.analysisStatus ?? item.analysis_status,
      analysisImageUrl: item.analysisImageUrl ?? item.analysis_image_url ?? '',
      pointVal: item.pointVal ?? item.point_val ?? '',
      pointValUnit: item.pointValUnit ?? item.point_val_unit ?? '',
      ftpPath: item.ftpPath ?? item.ftp_path ?? '',
      reportMsg: item.reportMsg ?? item.report_msg ?? '',
      originalPreview: '',
      analysisPreview: '',
      originalPreviewMessage: '',
      analysisPreviewMessage: ''
    }))
    // 分批加载，避免任务图片较多时同时占满HTTP连接。
    for (let i = 0; i < rows.value.length; i += 4) {
      if (version !== generation) break
      await Promise.all(rows.value.slice(i, i + 4).flatMap(row => [loadImage(row, false, version), loadImage(row, true, version)]))
    }
  } catch (e: any) {
    if (version === generation) error.value = e.message || '查询任务结果失败'
  } finally {
    if (version === generation) loading.value = false
  }
}

watch(() => [route.query.businessId, route.query.euaTaskId], () => {
  reportVersion++
  clearTimeout(reportTimer)
  reportStatus.value = { status: 'NOT_GENERATED' }
  reportError.value = ''
  reportSubmitting.value = false
  reportDownloading.value = false
  if (route.query.businessId || route.query.euaTaskId) refreshReportStatus().catch(() => {})
  loadResults()
}, { immediate: true })
onBeforeUnmount(() => { generation++; reportVersion++; clearTimeout(reportTimer); releaseImages() })
</script>

<style scoped lang="scss">
.eua-result {
  height: 100%; min-height: 0; box-sizing: border-box; display: flex; flex-direction: column;
  overflow: hidden; padding: 24px; background: #00152f; color: #d9eaff;
  --el-fill-color-blank: #0b2950; --el-bg-color: #0b2950; --el-text-color-regular: #d9eaff;
  --el-text-color-primary: #e3efff; --el-border-color: #29507e; --el-fill-color-light: #133b66;
}
.result-header { display: flex; flex-shrink: 0; justify-content: space-between; gap: 24px; padding: 22px 26px; background: linear-gradient(110deg, #073565, #071f40); border: 1px solid #234e7c; border-top: 2px solid #27adf5; border-radius: 6px; }
.header-info { flex: 1; min-width: 0; }
.eyebrow { color: #63caff; font-size: 12px; letter-spacing: 2px; }
h2 { margin: 10px 0 20px; color: #edf7ff; font-size: 22px; overflow-wrap: anywhere; }
.task-metadata { display: grid; grid-template-columns: minmax(130px, 1fr) minmax(200px, 1.4fr) minmax(180px, 1.2fr); gap: 16px 24px; margin: 0; }
.task-metadata > div { min-width: 0; border-left: 2px solid #275b86; padding-left: 12px; }
.task-metadata dt { font-size: 12px; color: #8eafd1; margin-bottom: 7px; }
.task-metadata dd { margin: 0; font-size: 13px; color: #d9eaff; overflow-wrap: anywhere; }
.header-actions { display: flex; align-items: flex-start; flex-shrink: 0; flex-wrap: wrap; gap: 10px; }
.header-actions .el-button { margin-left: 0; }
.report-status { flex-shrink: 0; padding: 12px 16px; margin-top: 12px; background: #092344; border-left: 3px solid #27adf5; color: #b5d4ee; font-size: 13px; }
.result-toolbar { display: flex; flex-shrink: 0; align-items: center; gap: 14px; margin: 22px 0; }
.result-toolbar > span { margin-right: auto; } .result-toolbar strong { color: #4dc4ff; margin-left: 10px; }
.result-toolbar .el-input { width: 250px; } .result-toolbar .el-select { width: 140px; }
.results { flex: 1; min-height: 0; overflow-y: auto; overflow-x: hidden; padding-right: 10px; scrollbar-gutter: stable; scrollbar-width: thin; scrollbar-color: #356b9b #061d38; }
.results::-webkit-scrollbar { width: 8px; }.results::-webkit-scrollbar-track { background: #061d38; }.results::-webkit-scrollbar-thumb { background: #356b9b; border-radius: 4px; }
.result-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 540px), 1fr)); gap: 20px; align-items: start; }
.picture-card { background: #092344; border: 1px solid #234870; border-radius: 6px; overflow: hidden; }
.card-heading { display: flex; justify-content: space-between; gap: 12px; padding: 16px 20px; }
h3 { font-size: 15px; margin: 0 0 7px; color: #eef6ff; overflow-wrap: anywhere; }.card-heading span { font-size: 12px; color: #8eafd1; }
.card-heading .type-badge { align-self: flex-start; padding: 4px 10px; color: #71d1ff; background: #113d62; border-radius: 4px; white-space: nowrap; }.card-heading .infrared { color: #ffd095; background: #493626; }
.image-pair { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 0 20px 16px; }
.image-pair.single-image { grid-template-columns: 1fr; }
.image-label { font-size: 12px; color: #a2bfe0; margin-bottom: 7px; }.image-cell { min-width: 0; }
.el-image, .image-placeholder { width: 100%; height: 210px; background: #03162d; border-radius: 4px; }.el-image { cursor: zoom-in; }
.image-placeholder { display: flex; align-items: center; justify-content: center; text-align: center; padding: 16px; box-sizing: border-box; color: #6e91b5; font-size: 12px; }
.card-result { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; padding: 14px 20px; border-top: 1px solid #1d3e60; font-size: 13px; }.card-result strong { color: #e2f5ff; font-size: 18px; }.status { color: #e1bc6d; }.status-1, .status-3 { color: #5dd9b0; }.status-2 { color: #ff8b87; }
details { padding: 12px 20px; background: #071d38; font-size: 12px; color: #8eaccd; } summary { cursor: pointer; } details p { overflow-wrap: anywhere; margin: 10px 0; }
.empty-state { text-align: center; padding: 80px 20px; color: #8eafd1; border: 1px dashed #254c73; border-radius: 6px; }
:deep(.el-input__wrapper), :deep(.el-input__inner), :deep(.el-select__wrapper) { background: #0b2950; color: #d9eaff; }
@media (max-width: 1100px) { .result-header { flex-direction: column; }.header-actions { align-self: flex-end; }.task-metadata { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 760px) { .eua-result { padding: 16px 12px; }.result-header { padding: 16px; }.task-metadata { grid-template-columns: 1fr; gap: 10px; }.task-metadata > div { display: flex; gap: 12px; }.task-metadata dt { flex-shrink: 0; min-width: 80px; margin-bottom: 0; }.result-toolbar { flex-wrap: wrap; margin: 14px 0; }.image-pair { gap: 8px; padding: 0 12px 12px; }.el-image, .image-placeholder { height: 170px; } }
</style>
