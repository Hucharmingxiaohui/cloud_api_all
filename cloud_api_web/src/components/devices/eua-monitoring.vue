<template>
  <section class="eua-monitoring">
    <header class="page-heading">
      <div><span class="eyebrow">EUA · 共享只读数据</span><h1>EUA设备监控</h1><p>独立于 DJI 设备管理 · 最新快照与历史告警</p></div>
      <el-button type="primary" :icon="Refresh" :loading="state.loading || statusLoading" @click="refresh">刷新</el-button>
    </header>

    <el-alert v-if="status?.enabled === false" title="EUA接收未开启，仍可查看历史数据" type="warning" :closable="false" show-icon />
    <el-alert v-if="statusError" :title="statusError" type="warning" :closable="false" show-icon />
    <div class="summary-grid">
      <article class="summary-card"><span>{{ activeTab === 'hms' ? '查询范围内告警记录' : '匹配快照' }}</span><strong>{{ state.loading || state.error || !state.loaded ? '—' : state.total }}</strong><small>按本标签已提交条件统计，不代表在线或当前故障数量</small></article>
      <article class="summary-card"><span>EUA接收配置</span><strong class="status-text">{{ status ? (status.enabled ? '已开启' : '未开启') : '未知' }}</strong><small>仅表示接收配置，不代表 MQTT 连接或设备在线状态</small></article>
      <article class="summary-card"><span>服务端时间</span><strong class="server-time">{{ formatField(timeField, status?.server_time) }}</strong><small>手动刷新更新 · 无自动轮询</small></article>
    </div>

    <div class="data-panel">
      <el-tabs v-model="activeTab" @tab-change="switchTab">
        <el-tab-pane label="无人机状态" name="drone" />
        <el-tab-pane label="机巢状态" name="dock" />
        <el-tab-pane label="告警记录" name="hms" />
      </el-tabs>
      <p class="data-note">OSD为最新快照，非飞行轨迹；告警为历史接收记录，未提供解除状态。无人机与机巢源时间可能不同，空值显示 —。</p>
      <el-form :model="state.filters" label-position="top" class="filter-grid" @submit.prevent="submit">
        <el-form-item label="单位编码（精确）"><el-input v-model="state.filters.unitCode" clearable maxlength="256" placeholder="输入单位编码" /></el-form-item>
        <el-form-item label="机巢 SN（精确）"><el-input v-model="state.filters.dockSn" clearable maxlength="256" placeholder="输入机巢 SN" /></el-form-item>
        <el-form-item label="无人机 SN（精确）"><el-input v-model="state.filters.droneSn" clearable maxlength="256" placeholder="输入无人机 SN" /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="state.filters.keyword" clearable maxlength="256" :placeholder="activeTab === 'hms' ? '编号 / 告警代码 / 告警内容' : '编号 / 任务 ID'" /></el-form-item>
        <el-form-item :label="activeTab === 'hms' ? '接收时间范围（服务端本地时间）' : '快照更新时间范围（服务端本地时间）'" class="date-filter">
          <el-date-picker v-model="state.filters.range" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" start-placeholder="开始时间" end-placeholder="结束时间" :default-time="defaultTimes" />
        </el-form-item>
        <template v-if="activeTab === 'hms'">
          <el-form-item label="告警级别"><el-select v-model="state.filters.level" clearable placeholder="全部级别"><el-option v-for="(label, index) in ['通知', '提醒', '警告']" :key="index" :label="label" :value="index" /></el-select></el-form-item>
          <el-form-item label="告警来源"><el-select v-model="state.filters.gatewayType" clearable placeholder="全部来源"><el-option label="机巢" value="1" /><el-option label="无人机" value="2" /></el-select></el-form-item>
          <el-form-item label="模块"><el-select v-model="state.filters.module" clearable placeholder="全部模块"><el-option v-for="(label, index) in ['飞行任务', '设备管理', '媒体', 'HMS']" :key="index" :label="label" :value="index" /></el-select></el-form-item>
        </template>
        <div class="filter-actions"><el-button type="primary" native-type="submit" :icon="Search">查询</el-button><el-button :icon="Refresh" @click="reset">重置</el-button></div>
      </el-form>
      <el-alert v-if="validationError" :title="validationError" type="warning" :closable="false" show-icon />
      <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" show-icon />
      <div class="table-scroll">
        <el-table :key="activeTab" v-loading="state.loading" :data="state.records" :row-key="rowKey" stripe :max-height="520" :empty-text="state.error ? '查询失败，请刷新重试' : '暂无符合条件的EUA记录'">
          <el-table-column v-for="field in columns" :key="field.key" :label="field.label" :min-width="field.key === 'message' ? 280 : field.kind === 'time' ? 210 : 150" show-overflow-tooltip>
            <template #default="scope">
              <el-tag v-if="field.key === 'level'" :type="levelType(scope.row.level)" effect="dark">{{ formatField(field, scope.row[field.key]) }}</el-tag>
              <span v-else>{{ formatField(field, scope.row[field.key]) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right"><template #default="scope"><el-button link type="primary" @click="openDetail(scope.row)">详情</el-button></template></el-table-column>
        </el-table>
      </div>
      <div class="pagination"><el-pagination :current-page="state.current" :page-size="state.size" :page-sizes="[10, 20, 50, 100]" :total="state.total" :disabled="state.loading" layout="total, sizes, prev, pager, next" @current-change="changePage" @size-change="changeSize" /></div>
    </div>

    <el-drawer v-model="detailVisible" :title="detailTitle" size="min(620px, 94vw)" append-to-body custom-class="eua-monitoring-drawer">
      <p class="drawer-note">只读字段 · 源模式保留代码，不推断在线、解除或上报状态</p>
      <dl class="detail-grid"><template v-for="field in details" :key="field.key"><dt>{{ field.label }}</dt><dd>{{ formatField(field, selected?.[field.key]) }}</dd></template></dl>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getEuaMonitoringStatus, getEuaMonitoringPage, getEuaHmsPage } from '/@/api/eua-monitoring'
import type { EuaReceptionStatus } from '/@/api/eua-monitoring'
import { buildQuery, createTabState, detailFields, emptyFilters, formatField, invalidateTab, loadTab, requestError, tableFields } from './eua-monitoring-model'
import type { EuaRow, Field, TabKey } from './eua-monitoring-model'

const activeTab = ref<TabKey>('drone')
const states = reactive({ drone: createTabState(), dock: createTabState(), hms: createTabState() })
const state = computed(() => states[activeTab.value])
const columns = computed(() => tableFields(activeTab.value))
const status = ref<EuaReceptionStatus | null>(null)
const statusLoading = ref(false)
const statusError = ref('')
const validationError = ref('')
const timeField: Field = { key: 'update_time', label: '服务端时间', kind: 'time' }
const defaultTimes = [new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)]
const detailVisible = ref(false)
const selected = ref<EuaRow | null>(null)
const detailTab = ref<TabKey>('drone')
const details = computed(() => detailFields(detailTab.value))
const detailTitle = computed(() => ({ drone: '无人机快照详情', dock: '机巢快照详情', hms: '告警记录详情' }[detailTab.value]))
let alive = true
let statusGeneration = 0

async function loadStatus () {
  const generation = ++statusGeneration
  statusLoading.value = true
  statusError.value = ''
  try {
    const response = await getEuaMonitoringStatus()
    if (!alive || generation !== statusGeneration) return
    if (typeof response?.enabled !== 'boolean') throw new Error('Invalid status')
    status.value = response
  } catch (error) {
    if (alive && generation === statusGeneration) {
      status.value = null
      statusError.value = requestError(error, 'EUA接收配置读取') + '，历史查询仍可使用'
    }
  } finally {
    if (alive && generation === statusGeneration) statusLoading.value = false
  }
}

function queryPage (tab: TabKey = activeTab.value) {
  const target = states[tab]
  try {
    const query = buildQuery(target.applied, target.current, target.size, tab)
    return loadTab(target, () => tab === 'hms' ? getEuaHmsPage(query) : getEuaMonitoringPage(query))
  } catch (error) {
    validationError.value = (error as Error).message
  }
}

function submit () {
  const target = state.value
  try {
    buildQuery(target.filters, 1, target.size, activeTab.value)
    target.applied = { ...target.filters, range: [...(target.filters.range || [])] }
    target.current = 1
    validationError.value = ''
    queryPage()
  } catch (error) {
    validationError.value = (error as Error).message
  }
}

function reset () {
  state.value.filters = emptyFilters()
  submit()
}

function switchTab () {
  validationError.value = ''
  detailVisible.value = false
  // Invalidating other tabs prevents late responses from a departed view being applied.
  for (const tab of ['drone', 'dock', 'hms'] as TabKey[]) if (tab !== activeTab.value) invalidateTab(states[tab])
  queryPage()
}

function refresh () { loadStatus(); queryPage() }
function changePage (page: number) { state.value.current = page; queryPage() }
function changeSize (size: number) { state.value.size = size; state.value.current = 1; queryPage() }
function openDetail (row: EuaRow) { selected.value = row; detailTab.value = activeTab.value; detailVisible.value = true }
function rowKey (row: EuaRow) { return String(row.id) }
function levelType (level: number | null) { return level === 2 ? 'danger' : level === 1 ? 'warning' : 'info' }

onMounted(refresh)
onBeforeUnmount(() => {
  alive = false
  statusGeneration++
  for (const tab of ['drone', 'dock', 'hms'] as TabKey[]) invalidateTab(states[tab])
})
</script>

<style scoped lang="scss">
.eua-monitoring {
  --el-bg-color: #10243c; --el-bg-color-overlay: #17304b; --el-fill-color-blank: #10243c;
  --el-fill-color-light: #183550; --el-fill-color-lighter: #132d48; --el-fill-color: #17304b;
  --el-text-color-primary: #e0edff; --el-text-color-regular: #b9cce4; --el-text-color-secondary: #8ea9c7;
  --el-border-color: #2c4d6e; --el-border-color-light: #284663; --el-border-color-lighter: #203d59;
  --el-color-primary: #409eff; --el-mask-color: rgba(5, 19, 36, .75);
  height: 100%; min-height: 0; overflow: auto; padding: 24px 28px 32px; box-sizing: border-box;
  color: #dbeaff; background: radial-gradient(ellipse at 90% 0, #173a62 0, transparent 45%), #09192d;
}
.page-heading { display: flex; justify-content: space-between; align-items: center; gap: 20px; margin-bottom: 20px;
  h1 { color: #edf6ff; font-size: 25px; margin: 6px 0; letter-spacing: 1px; }
  p { color: #92afce; margin: 0; font-size: 13px; }
}
.eyebrow { color: #65b9ff; font-size: 11px; letter-spacing: 2px; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin: 18px 0; }
.summary-card { display: flex; flex-direction: column; gap: 8px; padding: 20px 22px; border: 1px solid #264b72; border-radius: 12px; background: linear-gradient(135deg, #163454, #10243c); box-shadow: 0 8px 24px #030e1e40;
  span { color: #9cb7d4; font-size: 13px; } strong { color: #79c7ff; font-size: 30px; font-weight: 600; }
  .status-text { font-size: 23px; } .server-time { font-size: 17px; min-height: 36px; display: flex; align-items: center; overflow-wrap: anywhere; }
  small { color: #8da8c5; font-size: 12px; }
}
.data-panel { padding: 12px 22px 18px; background: #10243ce8; border: 1px solid #244562; border-radius: 12px; box-shadow: 0 12px 32px #0003; }
.data-note { color: #91adcc; font-size: 12px; line-height: 1.8; margin: 0 0 18px; }
.filter-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0 16px;
  .date-filter { grid-column: span 2; } :deep(.el-select) { width: 100%; }
  :deep(.el-date-editor) { width: 100%; box-sizing: border-box; }
  :deep(.el-form-item__label) { color: #a7c2df; font-size: 12px; padding-bottom: 5px; }
}
.filter-actions { display: flex; align-items: flex-end; padding-bottom: 18px; }
.table-scroll { overflow: auto; margin-top: 8px; }
.pagination { display: flex; justify-content: flex-end; overflow-x: auto; padding-top: 20px; }
:deep(.el-tabs__item) { color: #9dbad9; font-size: 15px; } :deep(.el-tabs__item.is-active) { color: #69b8ff; }
:deep(.el-tabs__nav-wrap::after) { background: #284563; }
:deep(.el-table) { --el-table-header-bg-color: #193b5c; --el-table-tr-bg-color: #10243c; --el-table-row-hover-bg-color: #1d4266; --el-table-border-color: #23415e; }
:deep(.el-alert) { margin-bottom: 12px; } :deep(.el-alert--warning.is-light) { background: #3a3020; color: #efc47e; }
:deep(.el-alert--error.is-light) { background: #412832; color: #ffadb5; }
@media (max-width: 1100px) { .filter-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .summary-grid { gap: 10px; } .summary-card { padding: 16px; } }
@media (max-width: 700px) { .eua-monitoring { padding: 16px 12px; } .summary-grid { grid-template-columns: 1fr; } .filter-grid { grid-template-columns: 1fr; .date-filter { grid-column: span 1; } } .data-panel { padding: 10px; } .pagination { justify-content: flex-start; } }
</style>

<style lang="scss">
.eua-monitoring-drawer {
  --el-bg-color: #10243c; --el-text-color-primary: #deebfc; --el-text-color-regular: #b9cce4;
  background: #10243c; color: #deebfc;
  .el-drawer__header { color: #deebfc; margin-bottom: 12px; border-bottom: 1px solid #294561; padding-bottom: 18px; }
  .drawer-note { color: #8eaccb; font-size: 12px; line-height: 1.8; }
  .detail-grid { display: grid; grid-template-columns: 160px minmax(0, 1fr); margin-top: 20px;
    dt, dd { padding: 12px 8px; border-bottom: 1px solid #213e5a; margin: 0; font-size: 13px; overflow-wrap: anywhere; white-space: pre-wrap; }
    dt { color: #94b2d0; } dd { color: #e0edff; }
  }
  @media (max-width: 600px) { .detail-grid { grid-template-columns: 120px minmax(0, 1fr); } }
}
</style>
