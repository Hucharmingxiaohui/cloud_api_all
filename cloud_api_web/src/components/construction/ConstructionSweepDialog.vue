<template>
  <el-dialog :model-value="true" title="自动生成 S 型面状航线" width="560px" append-to-body :close-on-click-modal="false" @close="emit('close')">
    <el-form label-width="150px" class="sweep-form">
      <el-form-item label="航线方向"><el-input-number v-model="settings.direction" :min="0" :max="179" :precision="0" /><span>°，0为南北向，90为东西向</span></el-form-item>
      <el-form-item label="扫描行间距"><el-input-number v-model="settings.rowSpacing" :min="1" :max="1000" /> 米</el-form-item>
      <el-form-item label="沿线拍照点间距"><el-input-number v-model="settings.pointSpacing" :min="1" :max="1000" /> 米</el-form-item>
      <el-form-item label="相对起飞点高度"><el-input-number v-model="settings.height" :min="1" :max="1500" /> 米</el-form-item>
      <el-form-item label="飞行速度"><el-input-number v-model="settings.speed" :min="1" :max="15" /> 米/秒</el-form-item>
      <el-form-item label="云台俯仰角"><el-input-number v-model="settings.pitch" :min="-90" :max="0" /> °</el-form-item>
      <el-form-item label="拍照镜头"><el-select v-model="settings.lens"><el-option label="可见光（广角）" value="wide" /><el-option label="红外" value="ir" /><el-option label="可见光 + 红外" value="wide,ir" /></el-select></el-form-item>
    </el-form>
    <p class="sweep-note">机头沿航线方向，到点停稳拍照；−90°为垂直向下。镜头需与实际设备匹配。间距为规划参数，不代表已校验照片覆盖率；本功能不进行避障或地形跟随。</p>
    <p v-if="error" role="alert" class="sweep-error">{{ error }}</p>
    <p v-if="result" class="sweep-summary">{{ result.rows }} 条扫描线 · {{ result.points.length }} 个航点（{{ result.points.filter(p => p.capture).length }} 个拍照点）· 约 {{ Math.round(result.distance) }} 米</p>
    <template #footer><el-button @click="emit('close')">取消</el-button><el-button @click="calculate">计算航线</el-button><el-button type="primary" :disabled="!result" @click="emit('apply', { result, settings: { ...settings } })">应用到编辑器</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { ConstructionPoint } from '/@/api/construction'
import { generateSweep } from './constructionSweep'
/* global defineProps, defineEmits */
const props = defineProps<{ boundary: ConstructionPoint[] }>()
const emit = defineEmits(['close', 'apply'])
const settings = reactive({ direction: 0, rowSpacing: 20, pointSpacing: 20, height: 100, speed: 5, pitch: -90, lens: 'wide' })
const result = ref<ReturnType<typeof generateSweep> | null>(null); const error = ref('')
watch(settings, () => { result.value = null; error.value = '' })
function calculate () {
  result.value = null; error.value = ''
  try {
    if (!Number.isFinite(settings.height) || settings.height < 1 || settings.height > 1500 || !Number.isFinite(settings.speed) || settings.speed < 1 || settings.speed > 15 || !Number.isFinite(settings.pitch) || settings.pitch < -90 || settings.pitch > 0) throw Error('请填写有效的高度、速度和云台俯仰角')
    result.value = generateSweep(props.boundary, settings)
  } catch (e) { error.value = e instanceof Error ? e.message : '生成失败，请调整参数' }
}
</script>
<style scoped>
.sweep-form :deep(.el-form-item__content){gap:8px}.sweep-form span,.sweep-note{font-size:12px;line-height:1.8;color:#63788e}.sweep-error{color:#c74632}.sweep-summary{padding:12px;background:#eaf4ff;color:#215784;border-radius:6px}
</style>
