<template>
  <div v-if="enabled && points.length > 1" class="elevation-review">
    <button type="button" @click="open = !open">{{ open ? '收起高程复核' : '航线高程 / 剖面复核' }}</button>
    <section v-if="open" aria-label="航线高程复核">
      <header><b>航线高程复核 · {{ mode === 'WGS84' ? 'WGS84 椭球高' : mode === 'relativeToStartPoint' ? '相对起飞点' : '高度模式未知' }}</b><button @click="refresh">重新读取</button></header>
      <label v-if="mode === 'relativeToStartPoint'">原航线起飞点椭球高（m）<input v-model="base" type="number" step="0.1" placeholder="必须核实起飞基准" /></label>
      <div class="thresholds"><label>低净空提示（m）<input v-model.number="low" type="number" min="0" max="300" /></label><label>高差上限提示（m）<input v-model.number="high" type="number" min="1" max="1500" /></label></div>
      <p v-if="!thresholdsValid" class="warn">请填写有效阈值：0 ≤ 低净空 &lt; 高差上限 ≤ 1500 米；当前阈值不能用于复核。</p>
      <p v-if="loading">正在读取现场高程…</p><p v-if="error || calculation.error" role="alert" class="warn">{{ error || calculation.error }}</p>
      <template v-if="profile">
        <p class="summary" :class="{ warn: dangerCount || lowCount || highCount || unknownCount }">疑似穿地 {{ dangerCount }} · 低净空 {{ lowCount }} · 达到上限 {{ highCount }} · 未知 {{ unknownCount }}（采样点数）</p>
        <p>长度 {{ (profile.total / 1000).toFixed(2) }} km · 采样间距不大于 {{ profile.spacing.toFixed(1) }} m · {{ unknownCount ? '含未知区间，无法完成全部复核' : '已计算采样点，不代表航线安全认证' }}</p>
        <svg viewBox="0 0 800 190" role="img" aria-label="航线与地表的椭球高剖面">
          <text x="8" y="14">{{ range.max.toFixed(0) }} m</text><text x="8" y="185">{{ range.min.toFixed(0) }} m</text>
          <path :d="terrainPath" fill="none" stroke="#68d6a4" stroke-width="2" />
          <path :d="flightPath" fill="none" stroke="#72caff" stroke-width="2" />
          <circle v-for="(s, i) in profile.samples.filter(s => state(s) !== 'normal' && s.flight !== null)" :key="i" :cx="x(s.distance)" :cy="y(s.flight || 0)" r="2.2" :fill="state(s) === 'unknown' ? '#c4ccd5' : '#ff7a62'" />
          <line v-if="selected" :x1="x(selected.distance)" :x2="x(selected.distance)" y1="20" y2="170" stroke="#fff" stroke-dasharray="4 4" />
        </svg>
        <div>绿色：地表椭球高 · 蓝色：航线椭球高 · 红色：提示 · 灰色：未知</div>
        <input v-model.number="index" class="sample-slider" aria-label="选择剖面采样点" type="range" min="0" :max="profile.samples.length - 1" />
        <p v-if="selected">航段 {{ selected.segment }} → {{ selected.segment + 1 }} · 累计 {{ selected.distance.toFixed(0) }} m · 地表 {{ fmt(selected.terrain) }} · 航线 {{ fmt(selected.flight) }} · 高差 {{ fmt(selected.clearance) }}<button v-if="map" @click="locate">地图定位</button></p>
      </template>
      <p class="note">采用约 30 米分辨率地表模型，可能含树木、建筑；不能识别电线等细小障碍。高差是航线减地表的估算值，阈值仅供提示，不代替真高、空域及现场障碍复核。未知区间不插值补齐；不修改航线、不硬拦截。</p>
    </section>
  </div>
</template>
<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { CURRENT_CONFIG } from '/@/api/http/config'
import { loadElevation } from '/@/utils/terrain-elevation'
import { buildElevationProfile } from '/@/utils/elevation-profile'
import type { ElevationGrid, HeightPoint, ProfileSample } from '/@/utils/elevation-profile'
import { wgs84togcj02 } from '/@/vendors/coordtransform'
/* global defineProps */
declare const AMap: any
const props = defineProps<{ points: HeightPoint[]; mode?: string; map?: any }>()
const enabled = CURRENT_CONFIG.terrainElevation?.enabled === true
const open = ref(false); const loading = ref(false); const error = ref(''); const base = ref(''); const low = ref(30); const high = ref(300); const index = ref(0)
const grid = ref<ElevationGrid | null>(null); let epoch = 0; let marker: any; let markerMap: any
function clearMarker () { if (marker && markerMap) markerMap.remove(marker); marker = null; markerMap = null }
async function refresh () {
  const token = ++epoch; loading.value = true; error.value = ''; grid.value = null
  try { const value = await loadElevation(); if (token === epoch) grid.value = value } catch (e) { if (token === epoch) error.value = e instanceof Error ? e.message : String(e) } finally { if (token === epoch) loading.value = false }
}
watch(open, value => { if (value && !grid.value) refresh(); if (!value) clearMarker() })
watch(() => props.points, () => { index.value = 0; clearMarker(); base.value = '' }, { deep: true })
watch(() => props.mode, () => { base.value = ''; clearMarker() })
const calculation = computed(() => {
  if (!grid.value) return { result: null, error: '' }
  try { return { result: buildElevationProfile(props.points, props.mode, base.value, grid.value), error: '' } } catch (e) { return { result: null, error: e instanceof Error ? e.message : String(e) } }
})
const profile = computed(() => calculation.value.result)
const thresholdsValid = computed(() => typeof low.value === 'number' && typeof high.value === 'number' && Number.isFinite(low.value) && Number.isFinite(high.value) && low.value >= 0 && high.value > low.value && high.value <= 1500)
function state (s: ProfileSample) {
  if (s.clearance === null || !thresholdsValid.value) return 'unknown'
  if (s.clearance <= 0) return 'danger'
  if (s.clearance < low.value) return 'low'
  if (s.clearance >= high.value) return 'high'
  return 'normal'
}
const count = (type: string) => computed(() => profile.value?.samples.filter(s => state(s) === type).length || 0)
const dangerCount = count('danger'); const lowCount = count('low'); const highCount = count('high'); const unknownCount = count('unknown')
const selected = computed(() => profile.value?.samples[index.value])
const range = computed(() => {
  const values = profile.value?.samples.flatMap(s => [s.flight, s.terrain]).filter((v): v is number => v !== null) || []
  return { min: values.length ? Math.min(...values) - 15 : 0, max: values.length ? Math.max(...values) + 15 : 100 }
})
function x (distance: number) { return 65 + distance / Math.max(profile.value?.total || 0, 1) * 725 }
function y (height: number) { return 170 - (height - range.value.min) / (range.value.max - range.value.min) * 150 }
function path (field: 'flight' | 'terrain') {
  let pen = false
  return profile.value?.samples.map(s => { const value = s[field]; if (value === null) { pen = false; return '' } const command = pen ? 'L' : 'M'; pen = true; return `${command}${x(s.distance)},${y(value)}` }).join(' ') || ''
}
const terrainPath = computed(() => path('terrain')); const flightPath = computed(() => path('flight'))
function fmt (n: number | null) { return n === null ? '未知' : `${n.toFixed(1)} m` }
function locate () {
  clearMarker(); if (!props.map || !selected.value) return
  const s = selected.value; const position = wgs84togcj02(s.lng, s.lat)
  markerMap = props.map; marker = new AMap.Marker({ position, zIndex: 250, title: `高程复核：高差 ${fmt(s.clearance)}` }); markerMap.add(marker); markerMap.setCenter(position)
}
watch(index, clearMarker)
onBeforeUnmount(() => { epoch++; clearMarker() })
</script>
<style scoped>
.elevation-review{position:relative;z-index:180;padding:7px 12px;background:#0b2944;color:#e2f4ff;font-size:12px}.elevation-review button{border:1px solid #4ba8c8;border-radius:4px;background:#123c59;color:#dff8ff;padding:5px 10px;cursor:pointer}.elevation-review section{max-height:55vh;overflow:auto;overflow-x:hidden;padding:10px 2px}.elevation-review header,.thresholds{display:flex;gap:16px;align-items:center;flex-wrap:wrap;margin-bottom:8px}.elevation-review input[type=number]{width:120px;background:#082139;color:#fff;border:1px solid #578298;padding:4px;margin:0 6px}.elevation-review svg{width:100%;max-height:230px;background:#06192b}.elevation-review svg text{fill:#c6e4f8;font-size:12px}.sample-slider{width:100%;box-sizing:border-box;margin:0}.warn{color:#ffb17c}.note{color:#a8c8db;line-height:1.6}.elevation-review p{margin:8px 0}
</style>
