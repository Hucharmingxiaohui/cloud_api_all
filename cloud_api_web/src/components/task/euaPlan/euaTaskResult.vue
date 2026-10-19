<template>
  <section class="eua-result">
    <header class="result-header">
      <div>
        <div class="eyebrow">EUA · 飞行结果</div>
        <h2>{{ route.query.taskName || 'EUA任务结果' }}</h2>
        <p>任务编码：{{ route.query.businessId || '-' }}</p>
        <p>EUA任务ID：{{ route.query.euaTaskId || '-' }}</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" type="primary" :loading="loading" @click="loadResults">刷新结果</el-button>
        <el-button @click="router.push({ name: 'euaTaskRecord' })">返回执行记录</el-button>
      </div>
    </header>

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
          <div class="image-pair">
            <div v-for="kind in imageKinds" :key="kind.key" class="image-cell">
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
          <details><summary>路径与处理信息</summary><p>FTP路径：{{ row.ftpPath || '未转存（本平台任务不向上级上报）' }}</p><p>分析图：{{ row.analysisImageUrl || '-' }}</p><p>{{ row.reportMsg || '-' }}</p></details>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { getEuaTaskPictureImage, getEuaTaskPictureList } from '/@/api/cq-eua-plan'

const route = useRoute()
const router = useRouter()
const rows = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const keyword = ref('')
const type = ref('all')
const imageKinds = [{ key: 'originalPreview', label: '原图' }, { key: 'analysisPreview', label: '分析图' }]
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

watch(() => [route.query.businessId, route.query.euaTaskId], loadResults, { immediate: true })
onBeforeUnmount(() => { generation++; releaseImages() })
</script>

<style scoped lang="scss">
.eua-result {
  min-height: calc(100vh - 105px); padding: 28px 24px; background: #00152f; color: #d9eaff;
  --el-fill-color-blank: #0b2950; --el-bg-color: #0b2950; --el-text-color-regular: #d9eaff;
  --el-text-color-primary: #e3efff; --el-border-color: #29507e; --el-fill-color-light: #133b66;
}
.result-header { display: flex; justify-content: space-between; gap: 20px; padding: 22px 26px; background: linear-gradient(110deg, #073565, #071f40); border: 1px solid #234e7c; border-top: 2px solid #27adf5; border-radius: 6px; }
.eyebrow { color: #63caff; font-size: 12px; letter-spacing: 2px; }
h2 { margin: 10px 0 14px; color: #edf7ff; font-size: 24px; }
.result-header p { margin: 7px 0; font-size: 13px; color: #9bb9db; overflow-wrap: anywhere; }
.header-actions { display: flex; align-items: flex-start; flex-shrink: 0; }
.result-toolbar { display: flex; align-items: center; gap: 14px; margin: 22px 0; }
.result-toolbar > span { margin-right: auto; } .result-toolbar strong { color: #4dc4ff; margin-left: 10px; }
.result-toolbar .el-input { width: 250px; } .result-toolbar .el-select { width: 140px; }
.results { min-height: 260px; } .result-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 540px), 1fr)); gap: 20px; }
.picture-card { background: #092344; border: 1px solid #234870; border-radius: 6px; overflow: hidden; }
.card-heading { display: flex; justify-content: space-between; gap: 12px; padding: 16px 20px; }
h3 { font-size: 15px; margin: 0 0 7px; color: #eef6ff; overflow-wrap: anywhere; }.card-heading span { font-size: 12px; color: #8eafd1; }
.card-heading .type-badge { align-self: flex-start; padding: 4px 10px; color: #71d1ff; background: #113d62; border-radius: 4px; white-space: nowrap; }.card-heading .infrared { color: #ffd095; background: #493626; }
.image-pair { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 0 20px 16px; }
.image-label { font-size: 12px; color: #a2bfe0; margin-bottom: 7px; }.image-cell { min-width: 0; }
.el-image, .image-placeholder { width: 100%; height: 210px; background: #03162d; border-radius: 4px; }.el-image { cursor: zoom-in; }
.image-placeholder { display: flex; align-items: center; justify-content: center; text-align: center; padding: 16px; box-sizing: border-box; color: #6e91b5; font-size: 12px; }
.card-result { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; padding: 14px 20px; border-top: 1px solid #1d3e60; font-size: 13px; }.card-result strong { color: #e2f5ff; font-size: 18px; }.status { color: #e1bc6d; }.status-1, .status-3 { color: #5dd9b0; }.status-2 { color: #ff8b87; }
details { padding: 12px 20px; background: #071d38; font-size: 12px; color: #8eaccd; } summary { cursor: pointer; } details p { overflow-wrap: anywhere; margin: 10px 0; }
.empty-state { text-align: center; padding: 80px 20px; color: #8eafd1; border: 1px dashed #254c73; border-radius: 6px; }
:deep(.el-input__wrapper), :deep(.el-input__inner), :deep(.el-select__wrapper) { background: #0b2950; color: #d9eaff; }
@media (max-width: 760px) { .eua-result { padding: 16px 12px; }.result-header { flex-direction: column; }.result-toolbar { flex-wrap: wrap; }.image-pair { gap: 8px; padding: 0 12px 12px; }.el-image, .image-placeholder { height: 170px; } }
</style>
