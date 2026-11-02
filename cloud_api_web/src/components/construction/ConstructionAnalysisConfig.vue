<template>
  <el-dialog :model-value="true" :title="title" width="min(1080px, 96vw)" append-to-body :close-on-click-modal="false" :before-close="close" :close-on-press-escape="!busy" :show-close="!busy" class="construction-config-dialog">
    <div v-loading="busy" class="analysis-config">
      <el-alert title="仅配置检测规则，不执行分析" description="航点独立配置优先于工作面，工作面优先于默认配置。保存只影响之后新建的计划；已有计划和历史任务保留原快照。" type="info" :closable="false" show-icon />
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <div v-if="data" class="config-layout" :class="{ 'with-points': isRoute }">
        <aside v-if="isRoute" class="config-points">
          <h4>{{ data.name }} · 航点</h4>
          <el-input v-model="search" placeholder="查找航点号或工作面" clearable />
          <div class="point-list"><button v-for="point in filteredPoints" :key="point.index" :class="{ active: activeIndex === point.index }" :disabled="busy || !point.area_id" @click="selectPoint(point.index)"><strong>航点 {{ point.index }} · {{ point.area_name || '未绑定工作面' }}</strong><span>{{ point.area_id ? sourceName(point.source) + ' · ' + summary(point.effective) : '请先绑定工作面，再设置检测规则' }}</span></button></div>
          <p>设置不修改KMZ拍摄镜头。未识别到照片后缀时，后续仍需核对照片与航点的对应关系。</p>
        </aside>
        <section v-if="draft && (!isRoute || activePoint)" class="config-editor">
          <h3>{{ isRoute ? '航点 ' + activeIndex + ' · ' + activePoint?.area_name : data.area_name || '当前工作空间默认规则' }}</h3>
          <el-radio-group v-if="target !== 'default'" v-model="mode" :disabled="busy" @change="changeMode"><el-radio-button label="inherit">{{ target === 'area' ? '继承默认配置' : '继承工作面 / 默认配置' }}</el-radio-button><el-radio-button label="custom">独立配置</el-radio-button></el-radio-group>
          <p class="config-source">{{ mode === 'inherit' ? '当前继承：' + sourceName(activeState?.inherited_source || 'default') : '当前使用独立配置' }} · {{ summary(draft) }}</p>
          <el-form label-position="top" :disabled="busy || mode === 'inherit'">
            <section class="config-block"><div class="block-heading"><h4>可见光检测</h4><div><el-button link type="primary" @click="draft.visible_checks = data.catalog.map(c => c.code)">全选</el-button><el-button link @click="draft.visible_checks = []">全不选</el-button></div></div>
              <el-checkbox-group v-model="draft.visible_checks" class="checks"><el-checkbox v-for="item in data.catalog" :key="item.code" :label="item.code">{{ item.name }}</el-checkbox></el-checkbox-group>
              <p>全不选表示不检测可见光照片；仍保留采集原图。</p>
            </section>
            <section class="config-block"><div class="block-heading"><h4>红外温度异常检测</h4><el-switch v-model="draft.infrared.enabled" aria-label="红外温度异常检测开关" /></div>
              <p>与可见光“火灾烟雾”检测独立。关闭后不进行红外温度检测，红外原图仍归档。</p>
              <div v-if="draft.infrared.enabled" class="thermal-fields">
                <el-form-item label="高温阈值（℃）"><el-input-number :model-value="draft.infrared.max_temperature_c ?? undefined" :min="-50" :max="1000" :precision="1" placeholder="请按现场要求填写" @update:model-value="value => { if (draft) draft.infrared.max_temperature_c = value ?? null }" /></el-form-item>
                <el-form-item label="高温区域最小像素数"><el-input-number v-model="draft.infrared.min_hotspot_pixels" :min="1" :max="10000000" :precision="0" /></el-form-item>
              </div>
              <p v-if="draft.infrared.enabled">记录用于后续检测的温度阈值及区域大小条件；当前不计算温度、不判断火情。填写范围不代表相机测温量程。</p>
            </section>
          </el-form>
          <el-alert v-if="!draft.visible_checks.length && !draft.infrared.enabled" title="该配置不启用任何检测，照片仍正常保存。" type="warning" :closable="false" />
          <p v-if="validationError" class="validation-error">{{ validationError }}</p>
        </section>
        <el-empty v-else description="没有可配置的已绑定航点" />
      </div>
    </div>
    <template #footer><span v-if="saved" class="saved-message">{{ saved }}</span><el-button :disabled="busy" @click="reload">重新加载</el-button><el-button :disabled="busy" @click="close">关闭</el-button><el-button type="primary" :disabled="busy || !draft || !!validationError || (isRoute && !activePoint)" @click="save">保存检测配置</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getConstructionAnalysisConfig, saveConstructionAnalysisConfig } from '/@/api/construction'
import type { ConstructionAnalysisConfiguration, ConstructionConfigContext, ConstructionDetectionPolicy } from '/@/api/construction'
/* global defineProps, defineEmits */
const props = defineProps<{ context: ConstructionConfigContext }>()
const emit = defineEmits(['close'])
const data = ref<ConstructionAnalysisConfiguration>(); const draft = ref<ConstructionDetectionPolicy>()
const busy = ref(false); const error = ref(''); const saved = ref(''); const search = ref(''); const activeIndex = ref<number>()
const mode = ref('custom'); let baseline = ''; let alive = true
const isRoute = computed(() => !!(props.context.wayline_id || props.context.overall_id))
const target = computed(() => isRoute.value ? 'waypoint' : props.context.area_id ? 'area' : 'default')
const title = computed(() => ({ waypoint: '航点检测配置', area: '工作面检测配置', default: '默认检测配置' }[target.value]))
const activePoint = computed(() => data.value?.points?.find(p => p.index === activeIndex.value))
const activeState = computed(() => isRoute.value ? activePoint.value : data.value?.area)
const filteredPoints = computed(() => (data.value?.points || []).filter(p => `${p.index} ${p.area_name || ''}`.includes(search.value.trim())))
const dirty = computed(() => JSON.stringify([mode.value, draft.value]) !== baseline)
function clone<T> (v: T): T { return JSON.parse(JSON.stringify(v)) }
function sourceName (source: string) { return ({ default: '默认配置', area: '工作面配置', waypoint: '航点独立配置' } as Record<string, string>)[source] || source }
function summary (p: ConstructionDetectionPolicy) { return `可见光 ${p.visible_checks.length} 项 · 红外${p.infrared.enabled ? '开启' : '关闭'}` }
const validationError = computed(() => {
  if (!draft.value || mode.value === 'inherit') return ''
  const ir = draft.value.infrared
  if (ir.enabled && (ir.max_temperature_c === null || !Number.isFinite(ir.max_temperature_c) || ir.max_temperature_c < -50 || ir.max_temperature_c > 1000)) return '开启红外检测后，请填写有效的高温阈值（-50至1000℃）。'
  if (!Number.isInteger(ir.min_hotspot_pixels) || ir.min_hotspot_pixels < 1 || ir.min_hotspot_pixels > 10000000) return '请填写有效的高温区域最小像素数。'
  return ''
})
function resetDraft () {
  if (!data.value) return
  mode.value = target.value !== 'default' && !activeState.value?.override ? 'inherit' : 'custom'
  draft.value = clone(target.value === 'default' ? data.value.default_policy : activeState.value?.override || activeState.value?.effective || data.value.default_policy)
  baseline = JSON.stringify([mode.value, draft.value]); saved.value = ''
}
function changeMode () {
  if (mode.value === 'inherit' && data.value) {
    // A currently overridden point must preview its parent, not its old independent settings.
    draft.value = clone(activeState.value?.inherited || data.value.default_policy)
  }
}
async function discard () { if (!dirty.value || !data.value) return true; try { await ElMessageBox.confirm('放弃未保存的检测配置？', '确认切换'); return true } catch { return false } }
async function selectPoint (index: number) { if (index === activeIndex.value || !(await discard())) return; activeIndex.value = index; resetDraft() }
async function load () {
  busy.value = true; error.value = ''
  try { const next = await getConstructionAnalysisConfig(props.context); if (!alive) return; data.value = next; if (!next.points?.some(p => p.index === activeIndex.value && p.area_id)) activeIndex.value = next.points?.find(p => p.area_id)?.index; resetDraft() } catch (e) { if (alive) error.value = e instanceof Error ? e.message : '配置加载失败' } finally { if (alive) busy.value = false }
}
async function reload () { if (await discard()) await load() }
async function close () { if (!busy.value && await discard()) emit('close') }
async function save () {
  if (!data.value || !draft.value || validationError.value) return
  busy.value = true; error.value = ''; saved.value = ''
  try {
    const next = await saveConstructionAnalysisConfig(props.context, { target: target.value, expected_revision: data.value.revision, policy: mode.value === 'inherit' ? null : clone(draft.value), waypoint_index: activeIndex.value, file_sign: data.value.file_sign, route_version: data.value.route_version })
    if (!alive) return
    data.value = next; resetDraft(); saved.value = '已保存，之后新建的计划使用此配置。'
  } catch (e) { if (alive) error.value = e instanceof Error ? e.message : '配置保存失败' } finally { if (alive) busy.value = false }
}
onMounted(load)
onBeforeUnmount(() => { alive = false })
</script>
<style scoped>
.analysis-config{color:#24415f}.analysis-config .el-alert{margin-bottom:12px}.config-layout.with-points{display:grid;grid-template-columns:300px minmax(0,1fr);gap:22px}.config-points{min-width:0;border-right:1px solid #dce5ee;padding-right:16px}.config-points h4{margin:8px 0}.point-list{max-height:360px;overflow:auto;margin-top:12px}.point-list button{width:100%;text-align:left;display:grid;gap:6px;padding:12px;margin-bottom:8px;border:1px solid #d5e1ef;border-radius:6px;background:#f4f8fc;color:#284360;cursor:pointer}.point-list button.active{background:#e7f2ff;border-color:#409eff}.point-list button:disabled{opacity:.55;cursor:not-allowed}.point-list span,.config-points p,.config-block p,.config-source{font-size:12px;line-height:1.7;color:#627a93}.config-editor{min-width:0}.config-editor h3{margin:4px 0 14px}.config-block{border:1px solid #dae5ef;border-radius:8px;padding:14px;margin:14px 0}.block-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:10px}.block-heading h4{margin:0}.checks{display:grid;grid-template-columns:repeat(2,minmax(0,1fr))}.checks :deep(.el-checkbox){margin-right:8px}.thermal-fields{display:flex;gap:20px;flex-wrap:wrap}.validation-error{color:#c74632}.saved-message{color:#268457;font-size:12px;margin-right:12px}@media(max-width:760px){.config-layout.with-points{grid-template-columns:1fr}.config-points{border-right:0}.point-list{max-height:160px}.checks{grid-template-columns:1fr}}
</style>
