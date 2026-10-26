<template>
  <div class="construction-map">
    <div class="map-toolbar">
      <div class="map-layer-switch"><button :class="{ active: !tilesEnabled }" @click="tilesEnabled = false">坐标示意</button><button :class="{ active: tilesEnabled }" @click="tilesEnabled = true">本地瓦片</button></div>
      <span>{{ tilesEnabled ? '本地 /api 瓦片 · 需资源服务' : 'WGS84 坐标编辑 · 非实景底图' }}</span>
    </div>
    <svg ref="surface" viewBox="0 0 1000 640" preserveAspectRatio="xMidYMid meet" role="img" aria-label="施工区域交互地图" :class="{ drawing: drawMode }" @click="mapClick" @pointermove="dragMove" @pointerup="dragEnd" @pointercancel="dragEnd">
      <defs><pattern :id="gridId" width="50" height="50" patternUnits="userSpaceOnUse"><path d="M 50 0 L 0 0 0 50" fill="none" stroke="#244550" stroke-width="0.7" /></pattern></defs>
      <rect width="1000" height="640" fill="#10262d" />
      <rect width="1000" height="640" :fill="`url(#${gridId})`" />
      <g v-if="tilesEnabled"><image v-for="tile in tiles" :key="tile.url" :href="tile.url" :x="tile.x" :y="tile.y" :width="tile.size + 0.3" :height="tile.size + 0.3" @error="tileError = true" /></g>
      <g v-for="area in areas" :key="area.id" class="area-shape" @click.stop="!drawMode && $emit('select', area.id)">
        <polygon :points="pointsString(area.id === selectedId && boundary ? boundary : area.boundary)" :fill="area.enabled ? '#38bda52b' : '#7f92951c'" :stroke="area.id === selectedId ? '#72f0c9' : area.enabled ? '#3e9c92' : '#6a8086'" :stroke-width="area.id === selectedId ? 3 : 1.5" :stroke-dasharray="area.enabled ? undefined : '7 5'" :style="{ pointerEvents: drawMode ? 'none' : 'auto' }" />
        <text :x="labelPosition(area).x" :y="labelPosition(area).y" text-anchor="middle" fill="#e6fff6" font-size="15" font-weight="600" paint-order="stroke" stroke="#10262d" stroke-width="5" style="pointer-events:none">{{ area.name }}</text>
      </g>
      <polygon v-if="boundary && !areas.some(a => a.id === selectedId)" :points="pointsString(boundary)" fill="#38bda52b" stroke="#72f0c9" stroke-width="3" />
      <g v-for="dock in demoDocks" :key="dock.id" :transform="`translate(${project(dock.position).x},${project(dock.position).y})`">
        <circle r="20" fill="#122d42" stroke="#79baff" stroke-width="2" /><path d="M-8 5V-5L0-10L8-5V5Z M-3 5V0H3V5" fill="none" stroke="#b6dcff" stroke-width="2" />
        <text y="37" fill="#b6dcff" font-size="12" text-anchor="middle" paint-order="stroke" stroke="#10262d" stroke-width="4">{{ dock.mode === 'mobile' ? '车载机巢（演示）' : '固定机巢（演示）' }}</text>
      </g>
      <polyline v-if="draft.length" :points="pointsString(draft)" fill="#6ee7bf18" stroke="#ffc66e" stroke-width="2" stroke-dasharray="7 5" />
      <circle v-for="(point, i) in draft" :key="`draft-${i}`" :cx="project(point).x" :cy="project(point).y" r="5" fill="#ffc66e" />
      <g v-if="editable && !drawMode && boundary">
        <circle v-for="(point, i) in boundary" :key="i" :cx="project(point).x" :cy="project(point).y" r="7" fill="#f4fffb" stroke="#248d78" stroke-width="3" style="cursor:move" @pointerdown.stop.prevent="dragStart($event, i)" @click.stop />
      </g>
      <g transform="translate(950,90)"><path d="M0-22L-7 0L0-5L7 0Z" fill="#cbe5e6" /><text y="18" text-anchor="middle" fill="#9fb8bd" font-size="12">N</text></g>
    </svg>
    <div v-if="editable" class="draw-tools">
      <button :class="{ active: drawMode === 'rectangle' }" @click="begin('rectangle')">▭ 矩形区域</button>
      <button :class="{ active: drawMode === 'polygon' }" @click="begin('polygon')">⬡ 多边形</button>
      <template v-if="drawMode"><button @click="draft.pop()" :disabled="!draft.length">撤回</button><button v-if="drawMode === 'polygon'" @click="finish" :disabled="draft.length < 3">完成绘制</button><button @click="cancelDraw">取消</button></template>
    </div>
    <div class="map-controls"><button aria-label="放大地图" @click="zoom(0.75)">＋</button><button aria-label="缩小地图" @click="zoom(1.3333)">－</button><button aria-label="查看全部区域" @click="fit">⌖</button><button aria-label="向西移动" @click="centerX -= span / 4">←</button><button aria-label="向东移动" @click="centerX += span / 4">→</button><button aria-label="向北移动" @click="centerY -= span / 4">↑</button><button aria-label="向南移动" @click="centerY += span / 4">↓</button></div>
    <div class="map-caption"><span>{{ hint }}</span><span v-if="tilesEnabled && tileError" class="tile-warning">部分本地瓦片不可用，可切回坐标示意</span><span v-else>{{ scaleMeters }} m / 100 px · 示例坐标</span></div>
    <div v-if="error" class="map-error" role="alert">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
/* global defineEmits, defineProps */
import { computed, onMounted, ref, watch } from 'vue'
import type { ConstructionArea, ConstructionPoint } from '../../types/construction'
import { demoDocks, validateBoundary } from './constructionModel'
interface MapProps { areas: ConstructionArea[]; selectedId?: string; boundary?: ConstructionPoint[]; editable?: boolean }
const props: MapProps = defineProps<MapProps>()
const emit = defineEmits(['select', 'update:boundary', 'drawing'])
const surface = ref<SVGSVGElement>()
const gridId = `construction-grid-${Math.random().toString(36).slice(2)}`
const tilesEnabled = ref(false)
const tileError = ref(false)
const centerX = ref(0)
const centerY = ref(0)
const span = ref(0.00004)
const drawMode = ref('')
const draft = ref<ConstructionPoint[]>([])
const error = ref('')
let dragIndex = -1
let dragOriginal: ConstructionPoint[] = []
function world (p: ConstructionPoint) {
  return { x: (p.lng + 180) / 360, y: (1 - Math.log(Math.tan(Math.PI / 4 + p.lat * Math.PI / 360)) / Math.PI) / 2 }
}
function geographic (x: number, y: number): ConstructionPoint {
  return { lng: Number((x * 360 - 180).toFixed(7)), lat: Number((Math.atan(Math.sinh(Math.PI * (1 - 2 * y))) * 180 / Math.PI).toFixed(7)) }
}
function project (p: ConstructionPoint) { const w = world(p); return { x: (w.x - centerX.value) / span.value * 1000 + 500, y: (w.y - centerY.value) / span.value * 1000 + 320 } }
function pointsString (points: ConstructionPoint[]) { return points.map(p => { const q = project(p); return `${q.x},${q.y}` }).join(' ') }
function labelPosition (area: ConstructionArea) {
  const points = area.id === props.selectedId && props.boundary?.length ? props.boundary : area.boundary
  return project({ lng: points.reduce((sum, p) => sum + p.lng, 0) / points.length, lat: points.reduce((sum, p) => sum + p.lat, 0) / points.length })
}
function fit () {
  const p = [...props.areas.flatMap(a => a.boundary), ...(props.boundary || []), ...demoDocks.map(d => d.position)].map(world)
  const minX = Math.min(...p.map(p => p.x)); const maxX = Math.max(...p.map(p => p.x))
  const minY = Math.min(...p.map(p => p.y)); const maxY = Math.max(...p.map(p => p.y))
  centerX.value = (minX + maxX) / 2; centerY.value = (minY + maxY) / 2
  span.value = Math.max((maxX - minX) * 1.5, (maxY - minY) * 2.5, 0.00002)
}
function zoom (factor: number) { span.value = Math.max(0.000002, Math.min(0.005, span.value * factor)) }
function eventPoint (event: MouseEvent | PointerEvent) {
  const svg = surface.value
  const matrix = svg?.getScreenCTM()
  if (!svg || !matrix) return null
  const point = svg.createSVGPoint(); point.x = event.clientX; point.y = event.clientY
  const p = point.matrixTransform(matrix.inverse())
  if (p.x < 0 || p.y < 0 || p.x > 1000 || p.y > 640) return null
  return geographic(centerX.value + (p.x - 500) * span.value / 1000, centerY.value + (p.y - 320) * span.value / 1000)
}
function begin (mode: string) { drawMode.value = mode; draft.value = []; error.value = ''; emit('drawing', true) }
function cancelDraw () { drawMode.value = ''; draft.value = []; emit('drawing', false) }
function finish () {
  error.value = validateBoundary(draft.value)
  if (error.value) return
  emit('update:boundary', [...draft.value]); cancelDraw()
}
function mapClick (event: MouseEvent) {
  if (!drawMode.value) return
  const p = eventPoint(event)
  if (!p) return
  draft.value.push(p)
  if (drawMode.value === 'rectangle' && draft.value.length === 2) {
    const [a, b] = draft.value
    draft.value = [a, { lng: b.lng, lat: a.lat }, b, { lng: a.lng, lat: b.lat }]
    finish()
    if (error.value) draft.value = []
  }
}
function dragStart (event: PointerEvent, index: number) { dragIndex = index; dragOriginal = (props.boundary || []).map(p => ({ ...p })); surface.value?.setPointerCapture(event.pointerId) }
function dragMove (event: PointerEvent) {
  if (dragIndex < 0) return
  const p = eventPoint(event)
  if (!p) return
  const points = (props.boundary || []).map(p => ({ ...p })); points[dragIndex] = p
  emit('update:boundary', points)
}
function dragEnd (event: PointerEvent) {
  if (dragIndex < 0) return
  error.value = validateBoundary(props.boundary || [])
  if (error.value) emit('update:boundary', dragOriginal)
  dragIndex = -1
  if (surface.value?.hasPointerCapture(event.pointerId)) surface.value.releasePointerCapture(event.pointerId)
}
const hint = computed(() => drawMode.value === 'rectangle' ? '依次点击矩形的两个对角点' : drawMode.value ? `点击添加边界顶点，完成后点击「完成绘制」· ${draft.value.length}个点` : props.editable ? '选择绘制工具重画边界；拖动白色顶点调整区域' : '点击区域查看详情 · 本图不表示飞行可达性')
const scaleMeters = computed(() => Math.round(span.value * 40075016.7 * Math.cos(geographic(centerX.value, centerY.value).lat * Math.PI / 180) / 10))
const tiles = computed(() => {
  if (!tilesEnabled.value) return []
  const z = Math.max(8, Math.min(18, Math.floor(Math.log2(1000 / (256 * span.value)))))
  const n = 2 ** z; const size = 1000 / (n * span.value)
  const left = centerX.value - span.value / 2; const top = centerY.value - span.value * 0.32
  const items: { url: string; x: number; y: number; size: number }[] = []
  for (let x = Math.floor(left * n); x <= Math.floor((left + span.value) * n); x++) {
    for (let y = Math.floor(top * n); y <= Math.floor((top + span.value * 0.64) * n); y++) items.push({ url: `/api/${z}/${x}/${y}.png`, x: (x / n - left) * 1000 / span.value, y: (y / n - top) * 1000 / span.value, size })
  }
  return items.slice(0, 80)
})
watch(tilesEnabled, () => { tileError.value = false })
watch(() => props.editable, (editable) => { if (!editable) { cancelDraw(); dragIndex = -1 } })
watch(() => props.selectedId, () => { cancelDraw(); error.value = ''; fit() })
onMounted(fit)
</script>

<style scoped>
.construction-map{position:relative;min-height:420px;height:100%;overflow:hidden;background:#10262d;border:1px solid #29434b;border-radius:12px;color:#a9c2c8}.construction-map svg{width:100%;height:100%;min-height:420px;display:block;touch-action:none}.drawing{cursor:crosshair}.map-toolbar{position:absolute;top:18px;left:18px;right:18px;display:flex;align-items:center;gap:12px;font-size:11px;z-index:2}.map-layer-switch{display:flex;padding:3px;border:1px solid #35505a;border-radius:7px;background:#0a1c25e8}.construction-map button{background:#16313b;color:#c4d7da;border:1px solid #36545e;border-radius:5px;padding:7px 10px;cursor:pointer;font-size:12px}.construction-map button:hover,.construction-map button.active{background:#24594f;color:#baffdf;border-color:#4cb69d}.construction-map button:disabled{opacity:.4;cursor:default}.map-layer-switch button{border:0;background:transparent}.draw-tools{position:absolute;top:64px;left:18px;display:flex;gap:6px;flex-wrap:wrap;right:65px}.map-controls{position:absolute;right:14px;bottom:55px;display:flex;flex-direction:column;gap:5px}.map-controls button{font-size:17px;width:34px;padding:3px}.map-caption{position:absolute;bottom:0;left:0;right:0;padding:11px 16px;background:#091b26e8;display:flex;justify-content:space-between;gap:12px;font-size:11px;pointer-events:none}.area-shape{cursor:pointer}.map-error{position:absolute;bottom:55px;left:18px;padding:8px 12px;border:1px solid #a66245;background:#432b20;color:#ffd7b2;border-radius:5px;font-size:12px}.tile-warning{color:#ffc777}@media(max-width:900px){.map-toolbar>span{display:none}.map-caption{flex-wrap:wrap}}
</style>
