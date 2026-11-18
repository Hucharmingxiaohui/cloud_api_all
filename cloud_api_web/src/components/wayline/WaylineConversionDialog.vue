<template>
  <el-dialog :model-value="modelValue" title="厂家 JSON 航线转换 · 机场 3 / M4TD" width="760px" append-to-body :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy" @update:model-value="close">
    <p>本地读取并转换，不上传原始文件、不自动入库或下发。仅支持带 points 执行航点的已适配厂家 JSON，最大 20 MB。</p>
    <input ref="fileInput" type="file" accept=".json,application/json" :disabled="busy" aria-label="选择厂家 JSON 航线" @change="choose" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <template v-if="route">
      <el-descriptions :column="2" border class="summary">
        <el-descriptions-item label="航线">{{ route.name }}</el-descriptions-item>
        <el-descriptions-item label="文件">{{ fileName }}</el-descriptions-item>
        <el-descriptions-item label="航点 / 拍摄">{{ route.points.length }} / {{ route.photoCount }}</el-descriptions-item>
        <el-descriptions-item label="源相机枚举">{{ route.cameraTypes.join('、') || '无拍摄动作' }}</el-descriptions-item>
        <el-descriptions-item label="经度范围">{{ ranges.lng }}</el-descriptions-item>
        <el-descriptions-item label="纬度范围">{{ ranges.lat }}</el-descriptions-item>
        <el-descriptions-item label="源高度范围">{{ ranges.height }} m</el-descriptions-item>
        <el-descriptions-item label="源结束动作">{{ route.sourceFinish }}（不自动推断）</el-descriptions-item>
      </el-descriptions>
      <ul class="warnings"><li v-for="warning in route.warnings" :key="warning">{{ warning }}</li></ul>
      <el-form label-width="150px" :disabled="busy">
        <el-form-item label="M4TD 拍摄镜头">
          <el-select v-model="options.lens" placeholder="确认源拍摄用途后选择" @change="invalidate">
            <el-option label="广角可见光（倍率 1）" value="wide" />
            <el-option label="红外（倍率 1）" value="ir" />
            <el-option label="可见光＋红外（双光，倍率 1）" value="wide,ir" />
          </el-select>
          <span v-if="options.lens === 'wide,ir'" class="hint">同一拍摄动作保存广角和红外图像；不生成融合图。摘要拍摄数为动作次数，不是图像张数。</span>
        </el-form-item>
        <el-form-item label="转换后结束动作">
          <el-select v-model="options.finishAction" placeholder="显式选择，不沿用未知枚举" @change="invalidate">
            <el-option label="返航" value="goHome" /><el-option label="悬停 / 不执行结束动作" value="noAction" />
            <el-option label="自动降落（非自动返回机场）" value="autoLand" /><el-option label="返回首航点" value="gotoFirstWaypoint" />
          </el-select>
        </el-form-item>
        <el-form-item label="安全起飞高度 (m)"><el-input-number v-model="options.takeoffHeight" :min="2" :max="1500" @change="invalidate" /><span class="hint">相对起飞点；不是源航点椭球高</span></el-form-item>
        <el-form-item label="全局返航高度 (m)"><el-input-number v-model="options.rthHeight" :min="2" :max="1500" @change="invalidate" /><span class="hint">需根据现场障碍物复核</span></el-form-item>
      </el-form>
      <el-checkbox v-model="options.confirmed" :disabled="busy" @change="clearResult">我确认：源高度按椭球高解释、所选镜头与源用途对应，并接受逐点停稳和显式姿态动作转换；现场安全另行复核。</el-checkbox>
      <p class="hint">模板 EGM96 海拔字段使用本地全球模型逐点换算，执行椭球高不变。源倍率 1 不推断未知焦距；照片使用唯一后缀，对应设备名称见转换报告。</p>
    </template>
    <el-alert v-if="result" title="文件已生成，尚未通过机场或实飞验证。请同时下载转换报告，核对高度与设备拍摄对应关系。" type="success" :closable="false" />
    <template #footer>
      <el-button :disabled="busy" @click="close(false)">关闭</el-button>
      <el-button type="primary" :loading="busy" :disabled="!route || !options.confirmed || !options.lens || !options.finishAction" @click="convert">转换</el-button>
      <el-button :disabled="!result || busy" @click="downloadKmz">下载 KMZ</el-button>
      <el-button :disabled="!result || busy" @click="downloadReport">下载转换报告</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { loadEgm96 } from '../../utils/egm96'
import { buildVendorKmz, MAX_VENDOR_BYTES, packVendorKmz, parseVendorRoute } from './vendorJsonKmz'
import type { ConversionOptions, VendorRoute } from './vendorJsonKmz'
/* global defineProps, defineEmits */
const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits(['update:modelValue'])
const fileInput = ref<HTMLInputElement>()
const route = ref<VendorRoute | null>(null)
const fileName = ref(''); const error = ref(''); const busy = ref(false)
const result = ref<{ bytes: Uint8Array; report: string; name: string } | null>(null)
const options = reactive<ConversionOptions>({ lens: '', finishAction: '', takeoffHeight: 30, rthHeight: 100, confirmed: false })
const ranges = computed(() => {
  const p = route.value?.points || []
  const range = (key: 'lng' | 'lat' | 'height') => p.length ? `${Math.min(...p.map(v => v[key])).toFixed(key === 'height' ? 3 : 7)} ~ ${Math.max(...p.map(v => v[key])).toFixed(key === 'height' ? 3 : 7)}` : ''
  return { lng: range('lng'), lat: range('lat'), height: range('height') }
})
function clearResult () { result.value = null; error.value = '' }
function invalidate () { options.confirmed = false; clearResult() }
function reset () {
  route.value = null; fileName.value = ''; clearResult()
  Object.assign(options, { lens: '', finishAction: '', takeoffHeight: 30, rthHeight: 100, confirmed: false })
  if (fileInput.value) fileInput.value.value = ''
}
watch(() => props.modelValue, value => { if (!value) reset() })
function close (value: boolean) { if (!busy.value) emit('update:modelValue', value) }
async function choose (event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  reset()
  if (!file) return
  busy.value = true
  try {
    if (!/\.json$/i.test(file.name) || file.size > MAX_VENDOR_BYTES) throw new Error('请选择不超过 20 MB 的 JSON 文件')
    route.value = parseVendorRoute(JSON.parse((await file.text()).replace(/^\uFEFF/, '')))
    fileName.value = file.name
  } catch (e) { error.value = e instanceof Error ? e.message : String(e) } finally { busy.value = false; input.value = '' }
}
async function convert () {
  if (!route.value || busy.value) return
  clearResult(); busy.value = true
  try {
    const built = buildVendorKmz(route.value, options, await loadEgm96())
    const bytes = await packVendorKmz(built)
    result.value = { bytes, report: built.report, name: built.name }
  } catch (e) { error.value = e instanceof Error ? e.message : String(e) } finally { busy.value = false }
}
function save (blob: Blob, name: string) {
  const url = URL.createObjectURL(blob); const a = document.createElement('a')
  a.href = url; a.download = name; document.body.appendChild(a); a.click(); a.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
function safeName () { return Array.from(result.value?.name || '航线').map(c => c.charCodeAt(0) < 32 || '\\/:*?"<>|'.includes(c) ? '_' : c).join('').slice(0, 100) }
function downloadKmz () { if (result.value) save(new Blob([result.value.bytes], { type: 'application/vnd.google-earth.kmz' }), `${safeName()}_M4TD.kmz`) }
function downloadReport () { if (result.value) save(new Blob([result.value.report], { type: 'application/json;charset=utf-8' }), `${safeName()}_转换报告.json`) }
</script>

<style scoped>
.summary { margin-top: 16px; }
.warnings { padding-left: 22px; color: #b57716; line-height: 1.7; }
.hint { color: #8795a8; font-size: 12px; margin-left: 8px; }
:deep(.el-checkbox) { height: auto; align-items: flex-start; }
:deep(.el-checkbox__label) { white-space: normal; line-height: 1.6; }
:deep(.el-alert) { margin-top: 12px; }
</style>
