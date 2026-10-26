<template>
  <div class="param-grid">
    <el-form-item label="相对起飞点航高（m）"><el-input-number :model-value="modelValue.altitude" :min="10" :max="120" controls-position="right" @update:model-value="update('altitude', $event)" /></el-form-item>
    <el-form-item label="飞行速度（m/s）"><el-input-number :model-value="modelValue.speed" :min="1" :max="15" controls-position="right" @update:model-value="update('speed', $event)" /></el-form-item>
    <el-form-item label="云台俯仰（°）"><el-input-number :model-value="modelValue.pitch" :min="-90" :max="0" controls-position="right" @update:model-value="update('pitch', $event)" /></el-form-item>
    <el-form-item label="拍照间隔（s）"><el-input-number :model-value="modelValue.interval" :min="2" :max="30" controls-position="right" @update:model-value="update('interval', $event)" /></el-form-item>
    <el-form-item label="拍摄模式" class="full"><el-radio-group :model-value="modelValue.lens" @update:model-value="update('lens', $event)"><el-radio-button label="dual">双光</el-radio-button><el-radio-button label="visible">可见光</el-radio-button><el-radio-button label="infrared">红外</el-radio-button></el-radio-group></el-form-item>
  </div>
</template>
<script setup lang="ts">
/* global defineEmits, defineProps */
import type { ConstructionParams } from '../../types/construction'
const props: { modelValue: ConstructionParams } = defineProps<{ modelValue: ConstructionParams }>()
const emit = defineEmits(['update:modelValue'])
function update (key: keyof ConstructionParams, value: any) { emit('update:modelValue', { ...props.modelValue, [key]: value }) }
</script>
<style scoped>
.param-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 12px}.full{grid-column:1/-1}.param-grid :deep(.el-input-number){width:100%}
</style>
