<template>
  <section class="transfer-panel" v-loading="busy">
    <div class="transfer-heading"><div><h3>离线接收 · 工地巡检结果 <small>{{ data?.tasks.length || 0 }} 个任务</small></h3><p>所有部署端均可导出和接收。接收的任务可查看、分析，不会生成飞行计划。</p></div><el-button type="primary" :disabled="busy" @click="refresh">刷新并接收</el-button></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <template v-if="data">
      <details class="transfer-instructions"><summary>拷贝目录与使用方法</summary><p>本机接收目录：<code>{{ data.inbox }}</code></p><p>在来源端任务结果中点击“导出离线任务包”，将 ZIP 拷到此目录。拷贝时先使用 .part 后缀，完成后改成 .zip，再点击刷新并接收。</p><p>只处理当前工作空间的接收目录。失败包移入同级 failed 目录，并保存原因；修复后可重新放入 inbox。单次最多接收100个包。</p><p>部署编号：<code>{{ data.deployment }}</code></p></details>
      <div v-if="receipts.length" class="transfer-receipts" aria-live="polite"><div v-for="(receipt, index) in receipts" :key="index" :class="{ failed: receipt.status === 'FAILED' }"><b>{{ receipt.status === 'IMPORTED' ? '已接收' : receipt.status === 'SKIPPED' ? '已跳过' : '接收失败' }}</b> {{ receipt.file }} · {{ receipt.message }}</div></div>
      <div class="transfer-filter"><el-input v-model="keyword" placeholder="查找接收任务名称、来源机巢" clearable /><span>{{ filtered.length }} 个接收任务</span></div>
      <el-table :data="paged" max-height="300" empty-text="暂无离线接收任务；本机飞行任务在下方列表">
        <el-table-column prop="name" label="任务名称" min-width="180" />
        <el-table-column label="来源" min-width="170"><template #default="{ row }"><span>离线导入 · {{ row.dock_sn || '来源机巢未记录' }}</span><div class="source-id">部署 {{ row.origin.deployment.slice(0, 8) }}</div></template></el-table-column>
        <el-table-column label="飞行时间" min-width="165"><template #default="{ row }">{{ time(row.execute_time) }}</template></el-table-column>
        <el-table-column prop="photo_count" label="原图" width="85" />
        <el-table-column label="操作" width="130"><template #default="{ row }"><el-button link type="primary" @click="open(row.job_id)">查看任务结果</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-if="filtered.length > 5" v-model:current-page="page" :page-size="5" :total="filtered.length" layout="prev, pager, next" />
    </template>
  </section>
</template>
<script setup lang="ts">
import { computed, defineExpose, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getConstructionTransfers, scanConstructionTransfers } from '/@/api/construction'
import type { ConstructionTransfers as TransferData } from '/@/api/construction'
const router = useRouter()
const data = ref<TransferData>(); const busy = ref(false); const error = ref(''); const keyword = ref(''); const page = ref(1)
const receipts = ref<NonNullable<TransferData['receipts']>>([])
let alive = true
const filtered = computed(() => (data.value?.tasks || []).filter(row => `${row.name} ${row.dock_sn || ''}`.includes(keyword.value.trim())))
const paged = computed(() => filtered.value.slice((page.value - 1) * 5, page.value * 5))
watch(keyword, () => { page.value = 1 })
function time (value?: number) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '未记录' }
function open (id: string) { router.push({ path: '/task/taskResult', query: { job_id: id } }) }
async function load (scan = false) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try {
    const next = await (scan ? scanConstructionTransfers() : getConstructionTransfers())
    if (!alive) return
    data.value = next; page.value = 1
    if (scan) receipts.value = next.receipts || []
  } catch (cause) { if (alive) error.value = cause instanceof Error ? cause.message : '离线接收失败' } finally { if (alive) busy.value = false }
}
async function refresh () { await load(true) }
defineExpose({ refresh })
onMounted(() => load())
onBeforeUnmount(() => { alive = false })
</script>
<style scoped>
.transfer-panel{margin:12px 0;padding:16px;border:1px solid #244b74;border-radius:8px;background:#0c2445;color:#dcecff}.transfer-heading{display:flex;justify-content:space-between;gap:16px;align-items:center}.transfer-heading h3{margin:0;font-size:16px}.transfer-heading small{margin-left:12px;font-weight:400;color:#83bafa}.transfer-heading p,.transfer-instructions{font-size:12px;color:#a5bed9}.transfer-instructions{margin:10px 0;line-height:1.8}.transfer-instructions summary{cursor:pointer}.transfer-instructions code{word-break:break-all;color:#d2e7ff}.transfer-filter{display:flex;align-items:center;gap:12px;margin:12px 0;font-size:12px}.transfer-filter :deep(.el-input){max-width:320px}.transfer-receipts{max-height:150px;overflow:auto;font-size:12px;line-height:1.8;color:#80d9b0}.transfer-receipts .failed{color:#ffb18e}.source-id{font-size:11px;color:#829db9}.transfer-panel :deep(.el-pagination){margin-top:10px}
</style>
