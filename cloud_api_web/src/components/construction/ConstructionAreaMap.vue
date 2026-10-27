<template>
  <div class="area-map">
    <div class="dock-toolbar">
      <div><b>机巢位置</b><span>{{ docks?.length ? `${docks.length} 个机巢已有定位` : '等待机巢上报定位' }}</span></div>
      <div class="dock-actions"><select v-model="selectedDock" aria-label="选择定位机巢" :disabled="!docks?.length || disabled || coverageLocked || !!mode"><option value="">全部机巢</option><option v-for="dock in docks" :key="dock.sn" :value="dock.sn">{{ dock.name }}</option></select><button :disabled="!docks?.length || !!mode" @click="locateDocks">定位机巢</button><button :disabled="docksLoading" @click="emit('refresh-docks')">{{ docksLoading ? '加载中' : '刷新' }}</button></div>
    </div>
    <p v-if="dockError" class="dock-message">{{ dockError }}</p>
    <div v-if="selectedDock" class="coverage-toolbar">
      <b>{{ selectedDockName }}覆盖范围</b>
      <span v-if="coverageLoading">加载中…</span>
      <template v-else>
        <label>半径 <input v-model.number="radiusDraft" type="number" min="100" max="5000" step="1" aria-label="机巢覆盖半径（米）" :disabled="coverageSaving || disabled || coverageLocked || !!mode" @input="previewCoverage" /> 米</label>
        <button v-for="value in [100, 2000, 5000]" :key="value" :disabled="coverageSaving || disabled || coverageLocked || !!mode" @click="radiusDraft = value; previewCoverage()">{{ value === 100 ? value + 'm' : value / 1000 + 'km' }}</button>
        <button :disabled="coverageSaving || disabled || coverageLocked || !!mode || !validRadius" @click="saveCoverage">{{ coverageSaving ? '保存中…' : '保存范围' }}</button>
        <button :disabled="!coverageOverlay || !!mode" @click="fitCoverage">查看全范围</button>
        <span>{{ savedRadius === null ? '尚未配置' : `已保存 ${savedRadius}m` }}{{ coveragePreview ? ' · 预览未保存' : '' }}（最大5km）</span>
      </template>
      <span v-if="coverageError" role="alert" class="coverage-error">{{ coverageError }}</span>
    </div>
    <div v-if="selectedDock && !coverageLoading && effectiveRadius !== null" class="coverage-areas">
      <b>覆盖区域 {{ coveredAreas.length }} 个</b><span>（当前已加载区域{{ coveragePreview ? ' · 按预览半径' : '' }}）</span>
      <span v-if="!coveredAreas.length">暂无相交巡检区域</span>
      <button v-for="item in coveredAreas" :key="item.area.id" :class="['coverage-area', item.coverage, { selected: item.area.id === selectedId }]" :disabled="disabled || !!mode || editable" @click="selectCoveredArea(item.area.id)">{{ item.area.name }} · {{ item.coverage === 'full' ? '完全覆盖' : '部分覆盖' }}{{ item.area.enabled ? '' : ' · 暂停' }}</button>
    </div>
    <div v-if="editable" class="map-toolbar">
      <button type="button" :class="{ active: mode === 'rect' }" :disabled="disabled || !!mode" @click="begin('rect')">▭ 矩形框选</button>
      <button type="button" :class="{ active: mode === 'poly' }" :disabled="disabled || !!mode" @click="begin('poly')">⬡ 多边形</button>
      <button v-if="mode === 'poly'" type="button" class="primary-draw" :disabled="pendingPath.length < 3" @click="finishPolyDraw">完成绘制</button>
      <button v-if="mode === 'poly'" type="button" :disabled="!pendingPath.length" @click="undoPoint">撤回</button>
      <button v-if="mode" type="button" class="cancel-draw" @click="cancelDrawing">取消绘制</button>
      <button type="button" :disabled="disabled || !!mode || !boundaryDraft.length" @click="clearDraft">清空重绘</button>
      <span class="map-hint">{{ hint }}</span>
    </div>
    <div v-if="mapError" class="map-error" role="alert">地图加载失败：{{ mapError }}</div>
    <div ref="containerRef" class="map-container" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CURRENT_CONFIG } from '/@/api/http/config'
import { gcj02towgs84, wgs84togcj02 } from '/@/vendors/coordtransform'
import type { ConstructionPoint, ConstructionWayline } from '/@/api/construction'
import type { ConstructionDockMarker } from './useConstructionDocks'
import { getDockCoverage, saveDockCoverage } from '/@/api/construction'
import { areaCoverage, coverageBoundary } from './dockCoverageGeometry'
import { areaLabelPosition } from './constructionWaylineGeometry'
// AMap is loaded globally by index.html (/amap/AMap3.js), same as use-g-map.ts.
declare const AMap: any
/* global defineEmits, defineProps */

const props = defineProps<{
  areas: Array<{ id: string; name: string; enabled: boolean; boundary: ConstructionPoint[] }>
  selectedId: string | null
  boundary: ConstructionPoint[]
  editable: boolean
  disabled?: boolean
  docks?: ConstructionDockMarker[]
  dockError?: string
  docksLoading?: boolean
  activeDockSn?: string
  independentCoverage?: boolean
  coverageLocked?: boolean
  selectedWaypoints?: number[]
  wayline?: ConstructionWayline | null
}>()
const emit = defineEmits(['select', 'update:boundary', 'drawing', 'refresh-docks', 'coverage-change', 'select-waypoint'])
const selectedDock = ref('')
const selectedDockName = computed(() => props.docks?.find(d => d.sn === selectedDock.value)?.name || selectedDock.value)
const coverageLoading = ref(false); const coverageSaving = ref(false); const coverageError = ref('')
const radiusDraft = ref(100); const savedRadius = ref<number | null>(null); const coveragePreview = ref(false)
const validRadius = computed(() => Number.isInteger(radiusDraft.value) && radiusDraft.value >= 100 && radiusDraft.value <= 5000)
let coverageEpoch = 0
let coverageOverlay: any = null
const effectiveRadius = computed(() => coveragePreview.value ? (validRadius.value ? radiusDraft.value : null) : savedRadius.value)
const coveredAreas = computed(() => {
  const dock = props.docks?.find(d => d.sn === selectedDock.value)
  if (!dock || effectiveRadius.value === null) return []
  return props.areas.map(area => ({ area, coverage: areaCoverage(dock, effectiveRadius.value!, area.boundary || []) }))
    .filter(item => item.coverage !== null)
    .sort((a, b) => (a.coverage === b.coverage ? a.area.name.localeCompare(b.area.name, 'zh-CN') : a.coverage === 'full' ? -1 : 1))
})
function selectCoveredArea (id: string) {
  if (props.disabled || props.editable || mode.value) return
  emit('select', id)
  const overlay = areaOverlays.get(id)
  if (map && overlay) map.setFitView([overlay], false, [60, 60, 60, 60])
}

function renderCoverage () {
  if (!map) return
  if (coverageOverlay) { map.remove(coverageOverlay); coverageOverlay = null }
  const dock = props.docks?.find(d => d.sn === selectedDock.value)
  const radius = coveragePreview.value ? radiusDraft.value : savedRadius.value
  if (!dock || radius === null || coverageLoading.value) return
  const boundary = coverageBoundary(dock, radius)
  if (!boundary.length) return
  coverageOverlay = new AMap.Polygon({ path: toGcj(boundary), strokeColor: '#41d9eb', strokeWeight: 2, strokeStyle: coveragePreview.value ? 'dashed' : 'solid', fillColor: '#38cbe0', fillOpacity: 0.12, zIndex: 30, bubble: true })
  map.add(coverageOverlay)
}
function fitCoverage () { if (map && coverageOverlay && !mode.value) map.setFitView([coverageOverlay], false, [60, 60, 60, 60]) }
function previewCoverage () { coveragePreview.value = radiusDraft.value !== savedRadius.value; renderCoverage() }
async function loadCoverage () {
  const token = ++coverageEpoch; const sn = selectedDock.value
  savedRadius.value = null; coveragePreview.value = false; coverageError.value = ''; radiusDraft.value = 100; coverageLoading.value = !!sn; renderCoverage()
  if (!sn) return
  try {
    const data = await getDockCoverage(sn)
    if (token !== coverageEpoch) return
    savedRadius.value = data.radius_m; radiusDraft.value = data.radius_m ?? 100
  } catch (error) { if (token === coverageEpoch) coverageError.value = error instanceof Error ? error.message : '覆盖范围加载失败' } finally {
    if (token === coverageEpoch) { coverageLoading.value = false; renderCoverage(); fitCoverage() }
  }
}
async function saveCoverage () {
  if (!validRadius.value || coverageSaving.value || !selectedDock.value) return
  const sn = selectedDock.value; const token = coverageEpoch; const radius = radiusDraft.value
  coverageSaving.value = true; coverageError.value = ''
  try {
    const data = await saveDockCoverage(sn, radius)
    if (token !== coverageEpoch) return
    savedRadius.value = data.radius_m; coveragePreview.value = false; renderCoverage(); fitCoverage(); ElMessage.success('机巢覆盖范围已保存')
  } catch (error) { if (token === coverageEpoch) coverageError.value = error instanceof Error ? error.message : '覆盖范围保存失败' } finally { coverageSaving.value = false }
}
watch(selectedDock, loadCoverage)
watch([selectedDock, savedRadius, coverageLoading, coverageError], () => {
  emit('coverage-change', { sn: selectedDock.value, radius: savedRadius.value, loading: coverageLoading.value, error: coverageError.value })
}, { immediate: true })

const containerRef = ref<HTMLDivElement | null>(null)
const mapError = ref('')
const mode = ref<'rect' | 'poly' | ''>('')
const boundaryDraft = ref<ConstructionPoint[]>([])
const pendingPath = ref<Array<{ lng: number; lat: number }>>([])

const canVertexEdit = ref(false)

const hint = computed(() => {
  if (mode.value === 'poly') return '单击取点，双击或点“完成绘制”结束'
  if (mode.value === 'rect') return '按住鼠标拖出矩形，松开即完成'
  if (boundaryDraft.value.length) return canVertexEdit.value ? '可拖拽顶点微调边界' : '如需调整边界，请清空后重新绘制'
  return '请先绘制区域边界'
})

let map: any = null
let mouseTool: any = null
let draftPolygon: any = null
let draftEditor: any = null
const pointMarkers: any[] = []
const areaOverlays = new Map<string, any>()
const areaLabels: any[] = []
let lastEmitted = ''
const dockMarkers = new Map<string, any>()
let fittedDocks = false
let resizeObserver: ResizeObserver | null = null
let routeOverlays: any[] = []
function renderWayline (fit = true) {
  if (!map) return
  routeOverlays.forEach(o => map.remove(o)); routeOverlays = []
  const points = props.wayline?.points || []
  if (points.length < 2 || props.wayline?.status !== 'VALID') return
  const line = new AMap.Polyline({ path: toGcj(points), strokeColor: '#ffdb65', strokeWeight: 4, showDir: true, zIndex: 150 })
  routeOverlays.push(line)
  points.forEach((p, i) => {
    const content = document.createElement('span'); content.className = 'route-point'; content.textContent = String(i + 1)
    if (props.selectedWaypoints?.includes(i + 1)) { content.style.background = '#ff8759'; content.style.outline = '3px solid white' }
    const marker = new AMap.Marker({ position: wgs84togcj02(p.lng, p.lat), content, offset: new AMap.Pixel(-12, -12), zIndex: 160, title: `航点 ${i + 1} · ${p.height}m` })
    marker.on('click', () => { if (!props.disabled) emit('select-waypoint', i + 1) })
    routeOverlays.push(marker)
  })
  map.add(routeOverlays)
  const activeDock = props.activeDockSn ? dockMarkers.get(props.activeDockSn) : null
  if (fit) map.setFitView([...routeOverlays, ...(draftPolygon ? [draftPolygon] : []), ...(activeDock ? [activeDock] : [])], false, [65, 65, 65, 65])
}
watch(() => props.wayline, () => renderWayline(), { deep: true })
watch(() => props.selectedWaypoints, () => renderWayline(false), { deep: true })

function locateDocks () {
  if (!map || mode.value) return
  const targets = selectedDock.value ? [dockMarkers.get(selectedDock.value)].filter(Boolean) : [...dockMarkers.values()]
  if (!targets.length) return
  if (targets.length === 1) map.setZoomAndCenter(17, targets[0].getPosition())
  else map.setFitView(targets, false, [80, 80, 80, 80])
}
function renderDocks () {
  if (!map) return
  dockMarkers.forEach(marker => map.remove(marker)); dockMarkers.clear()
  for (const dock of props.docks || []) {
    const content = document.createElement('div')
    content.className = 'construction-dock-marker' + (dock.sn === props.activeDockSn ? ' active-dock' : '')
    const icon = document.createElement('span'); icon.className = 'dock-pin'; icon.textContent = 'H'
    const label = document.createElement('span'); label.className = 'dock-label'; label.textContent = dock.name
    content.append(icon, label)
    const marker = new AMap.Marker({ position: wgs84togcj02(dock.lng, dock.lat), content, offset: new AMap.Pixel(-18, -18), zIndex: 180, title: `${dock.name} · ${dock.sn}` })
    marker.on('click', () => { if (mode.value || props.disabled || props.coverageLocked) return; if (selectedDock.value === dock.sn) fitCoverage(); else selectedDock.value = dock.sn })
    map.add(marker); dockMarkers.set(dock.sn, marker)
  }
  if (selectedDock.value && !dockMarkers.has(selectedDock.value) && !props.coverageLocked) selectedDock.value = ''
  renderCoverage()
  if (!fittedDocks && dockMarkers.size && !mode.value && !props.editable && !props.areas.length && !boundaryDraft.value.length) {
    locateDocks(); fittedDocks = true
  }
}

function toGcj (points: ConstructionPoint[]) {
  return points.map(p => wgs84togcj02(p.lng, p.lat))
}
function toWgs (path: ConstructionPoint[]): ConstructionPoint[] {
  return path.map(p => {
    const wgs = gcj02towgs84(p.lng, p.lat)
    return { lng: wgs[0], lat: wgs[1] }
  })
}
function sameBoundary (a: ConstructionPoint[], b: ConstructionPoint[]) {
  return JSON.stringify(a) === JSON.stringify(b)
}

function closeEditor () {
  if (draftEditor) { try { draftEditor.close() } catch { /* already closed */ } draftEditor = null }
}
function closeDraft () {
  closeEditor()
  if (draftPolygon) { map?.remove(draftPolygon); draftPolygon = null }
}
function renderDraft () {
  closeDraft()
  lastEmitted = JSON.stringify(boundaryDraft.value)
  if (!map || !boundaryDraft.value.length) return
  draftPolygon = new AMap.Polygon({
    path: toGcj(boundaryDraft.value),
    strokeColor: '#39d9ff',
    strokeWeight: 2,
    fillColor: '#39d9ff',
    fillOpacity: 0.18,
    zIndex: 60
  })
  map.add(draftPolygon)
  if (props.editable) openEditor()
}
function openEditor () {
  if (!draftPolygon) return
  // 本地定制版 AMap 未内置 PolyEditor；缺失时边界调整走“清空重绘”，不抛错
  if (typeof AMap.PolyEditor !== 'function') return
  closeEditor()
  draftEditor = new AMap.PolyEditor(map, draftPolygon)
  draftEditor.on('adjust', emitPath)
  draftEditor.on('addnode', emitPath)
  draftEditor.on('removenode', emitPath)
  draftEditor.open()
}
function emitPath () {
  if (!draftPolygon) return
  const path = draftPolygon.getPath().map((p: any) => ({ lng: p.lng, lat: p.lat }))
  const wgs = toWgs(path)
  lastEmitted = JSON.stringify(wgs)
  emit('update:boundary', wgs)
}
function renderAreas () {
  if (!map) return
  for (const overlay of areaOverlays.values()) map.remove(overlay)
  areaOverlays.clear()
  areaLabels.forEach(label => map.remove(label)); areaLabels.length = 0
  for (const area of props.areas) {
    if (!area.boundary || area.boundary.length < 3) continue
    const overlay = new AMap.Polygon({
      path: toGcj(area.boundary),
      strokeColor: area.id === props.selectedId ? '#8fcfff' : '#3f6fae',
      strokeWeight: area.id === props.selectedId ? 2.4 : 1.6,
      strokeStyle: area.enabled ? 'solid' : 'dashed',
      fillColor: area.id === props.selectedId ? '#2f7bd9' : '#274f86',
      fillOpacity: area.id === props.selectedId ? 0.22 : 0.1,
      zIndex: 40,
      extData: { id: area.id }
    })
    overlay.on('click', () => { if (!mode.value) emit('select', area.id) })
    areaOverlays.set(area.id, overlay)
    map.add(overlay)
    const position = areaLabelPosition(area.boundary)
    const content = document.createElement('span')
    content.className = 'construction-area-label' + (area.id === props.selectedId ? ' selected' : '')
    content.textContent = area.name; content.title = area.name
    const label = new AMap.Marker({ position: wgs84togcj02(position.lng, position.lat), content, offset: new AMap.Pixel(0, 0), anchor: 'top-left', zIndex: 140, title: area.name })
    label.on('click', () => { if (!mode.value && !props.disabled && !props.editable) emit('select', area.id) })
    areaLabels.push(label); map.add(label)
  }
  if (!mode.value && !boundaryDraft.value.length && areaOverlays.size) {
    try { map.setFitView([...areaOverlays.values()], false, [60, 60, 60, 60]) } catch { /* fit is best-effort */ }
  }
}

// ---- 矩形：MouseTool 拖拽，松开即完成 ----
function onDrawn (overlay: any) {
  if (!overlay || mode.value !== 'rect') return
  let points: ConstructionPoint[] = []
  if (overlay.getBounds) {
    const bounds = overlay.getBounds()
    const sw = bounds.southwest
    const ne = bounds.northeast
    points = toWgs([
      { lng: sw.lng, lat: sw.lat },
      { lng: ne.lng, lat: sw.lat },
      { lng: ne.lng, lat: ne.lat },
      { lng: sw.lng, lat: ne.lat }
    ])
  }
  try { mouseTool?.close(true) } catch { /* not drawing */ }
  try { mouseTool?.off('draw', onDrawnHandler) } catch { /* not bound */ }
  mode.value = ''
  emit('drawing', false)
  boundaryDraft.value = points
  emit('update:boundary', points)
  renderDraft()
}
const onDrawnHandler = (event: any) => onDrawn(event?.value ?? event?.obj ?? event)

// ---- 多边形：自行接管点击取点，双击或按钮结束，不依赖 MouseTool 的双击行为 ----
// 绘制期用圆点标记显示进度（空路径 Polygon / 单点 Polyline 会让本地定制版 AMap 命中检测抛错并中断事件分发）
function onMapClick (event: any) {
  const w = window as any
  w.__polyClicks = (w.__polyClicks || 0) + 1
  w.__lastClickEvent = event ? { keys: Object.keys(event), lnglat: event.lnglat ? { lng: event.lnglat.lng, lat: event.lnglat.lat, type: typeof event.lnglat } : null } : null
  const lnglat = event?.lnglat
  if (!lnglat) return
  const point = { lng: lnglat.lng, lat: lnglat.lat }
  const last = pendingPath.value[pendingPath.value.length - 1]
  // 双击结束前的两次 click 会落在同一点，去重避免出现重复顶点
  if (last && Math.abs(last.lng - point.lng) < 1e-9 && Math.abs(last.lat - point.lat) < 1e-9) return
  try {
    pendingPath.value.push(point)
    // 绘制期用圆点标记显示进度；本地定制版 AMap 的 Polyline 在单点/更新路径时
    // 内部线段缓存为 undefined，命中检测抛错会中断 dblclick 事件分发
    const marker = new AMap.CircleMarker({
      center: [point.lng, point.lat],
      radius: 4,
      strokeColor: '#39d9ff',
      strokeWeight: 1,
      fillColor: '#39d9ff',
      fillOpacity: 0.9,
      bubble: false,
      zIndex: 55
    })
    pointMarkers.push(marker)
    map.add(marker)
  } catch (e) {
    ;(window as any).__polyError = String(e)
  }
  ;(window as any).__polyPending = pendingPath.value.length
}
function onMapDblclick () {
  if (mode.value === 'poly') finishPolyDraw()
}
// 本地定制版 AMap 不派发地图级 dblclick，改在 DOM 捕获阶段监听原生双击
function onContainerDblclick (event: Event) {
  event.preventDefault()
  if (mode.value === 'poly') finishPolyDraw()
}
function undoPoint () {
  if (mode.value !== 'poly' || !pendingPath.value.length) return
  pendingPath.value.pop()
  const marker = pointMarkers.pop()
  if (marker) map?.remove(marker)
}
function finishPolyDraw () {
  if (mode.value !== 'poly') return
  if (pendingPath.value.length < 3) { ElMessage.warning('至少需要三个顶点，请继续取点'); return }
  const points = toWgs([...pendingPath.value])
  stopPolyDraw()
  mode.value = ''
  emit('drawing', false)
  boundaryDraft.value = points
  emit('update:boundary', points)
  renderDraft()
}
function stopPolyDraw () {
  try { map?.off('click', onMapClick) } catch { /* not bound */ }
  try { map?.off('dblclick', onMapDblclick) } catch { /* not bound */ }
  containerRef.value?.removeEventListener('dblclick', onContainerDblclick, true)
  for (const marker of pointMarkers) map?.remove(marker)
  pointMarkers.length = 0
  pendingPath.value = []
}

function begin (next: 'rect' | 'poly') {
  if (!map || props.disabled) return
  if (next === 'rect' && !mouseTool) return
  mode.value = next
  closeEditor()
  emit('drawing', true)
  if (next === 'rect') {
    const style = { strokeColor: '#39d9ff', strokeWeight: 2, fillColor: '#39d9ff', fillOpacity: 0.15 }
    try { mouseTool.off('draw', onDrawnHandler) } catch { /* not bound yet */ }
    mouseTool.on('draw', onDrawnHandler)
    mouseTool.rectangle(style)
  } else {
    pendingPath.value = []
    map.on('click', onMapClick)
    containerRef.value?.addEventListener('dblclick', onContainerDblclick, true)
  }
}
function cancelDrawing () {
  if (mode.value === 'rect') {
    try { mouseTool?.close(true) } catch { /* not drawing */ }
    try { mouseTool?.off('draw', onDrawnHandler) } catch { /* not bound */ }
  } else {
    stopPolyDraw()
  }
  mode.value = ''
  emit('drawing', false)
}
function clearDraft () {
  if (mode.value) return
  boundaryDraft.value = []
  emit('drawing', false)
  emit('update:boundary', [])
  renderDraft()
}

watch(() => props.boundary, value => {
  const next = value || []
  if (sameBoundary(next, boundaryDraft.value) || sameBoundary(next, JSON.parse(lastEmitted || '[]'))) return
  boundaryDraft.value = next.map(p => ({ ...p }))
  renderDraft()
})
watch(() => props.editable, value => {
  if (!map) return
  if (!value && mode.value) cancelDrawing()
  if (value && draftPolygon) openEditor()
  else closeEditor()
})
watch([() => props.areas, () => props.selectedId], renderAreas, { deep: false })
watch(() => props.docks, renderDocks, { deep: true })
watch(() => props.activeDockSn, value => { if (value && !props.independentCoverage) selectedDock.value = value; renderDocks() })

onMounted(() => {
  if (typeof AMap === 'undefined') {
    mapError.value = '高德地图脚本未加载，请检查 /amap/AMap3.js'
    return
  }
  try {
    const tileLayer = new AMap.TileLayer({
      getTileUrl: (x: number, y: number, z: number) => `/api/${z}/${x}/${y}.png`,
      opacity: 1,
      zIndex: 99
    })
    map = new AMap.Map(containerRef.value, {
      center: CURRENT_CONFIG.droneLocation,
      zoom: CURRENT_CONFIG.initMapLevel,
      layers: [tileLayer],
      animateEnable: false,
      // 绘制场景关闭双击缩放，避免双击被地图缩放抢占
      doubleClickZoom: false
    })
    mouseTool = new AMap.MouseTool(map)
    mouseTool.on('draw', onDrawnHandler)
    canVertexEdit.value = typeof AMap.PolyEditor === 'function'
    boundaryDraft.value = (props.boundary || []).map(p => ({ ...p }))
    renderAreas()
    renderDraft()
    renderDocks()
    renderWayline()
    resizeObserver = new ResizeObserver(() => { if (map && typeof map.resize === 'function') map.resize() })
    if (containerRef.value) resizeObserver.observe(containerRef.value)
  } catch (error) {
    mapError.value = error instanceof Error ? error.message : String(error)
  }
})
onBeforeUnmount(() => {
  coverageEpoch++
  if (coverageOverlay) map?.remove(coverageOverlay)
  resizeObserver?.disconnect()
  routeOverlays.forEach(o => map?.remove(o)); routeOverlays = []
  dockMarkers.forEach(marker => map?.remove(marker)); dockMarkers.clear()
  try { mouseTool?.close(true) } catch { /* ignore */ }
  if (mode.value === 'poly') stopPolyDraw()
  closeDraft()
  for (const overlay of areaOverlays.values()) map?.remove(overlay)
  areaOverlays.clear()
  areaLabels.forEach(label => map?.remove(label)); areaLabels.length = 0
  map?.destroy()
  map = null
  mouseTool = null
})
</script>

<style scoped>
.area-map :deep(.construction-area-label) { display: block; width: max-content; max-width: 220px; transform: translate(-50%, -50%); padding: 5px 9px; border: 1px solid #689bd0; border-radius: 4px; background: #082646e8; color: #e7f3ff; font-size: 13px; line-height: 1.5; font-weight: 600; white-space: normal; overflow-wrap: anywhere; text-align: center; cursor: pointer; box-shadow: 0 2px 6px #00152b80; }
.area-map :deep(.construction-area-label.selected) { border-color: #75e6ff; background: #145480ee; color: #fff; }
.coverage-areas { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; padding: 8px 12px; max-height: 110px; overflow: auto; background: #0c2344; color: #a8bdd8; font-size: 12px; }
.coverage-area { border: 1px solid #2d8e8c; background: #103e48; color: #93e7cf; border-radius: 4px; padding: 4px 8px; cursor: pointer; }
.coverage-area.partial { border-color: #a57f35; background: #3c3426; color: #ffda89; }
.coverage-area.selected { outline: 1px solid #dce9fb; }
.coverage-area:disabled { opacity: .55; cursor: default; }
.coverage-toolbar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding: 8px 12px; background: #102b50; color: #c2d4ed; font-size: 12px; }
.coverage-toolbar input { width: 84px; background: #092345; color: #fff; border: 1px solid #36597e; padding: 4px; border-radius: 3px; }
.coverage-toolbar button { background: #173b62; border: 1px solid #36597e; border-radius: 3px; color: #dce9fb; padding: 4px 8px; cursor: pointer; }
.coverage-toolbar button:disabled { opacity: .5; cursor: default; }
.coverage-error { color: #ffad8e; }
.area-map :deep(.active-dock .dock-pin) { background: #ffcf57; color: #092345; box-shadow: 0 0 15px #ffcf57; }
.area-map :deep(.route-point) { display: flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: 50%; background: #ffdb65; color: #092345; font-weight: bold; border: 2px solid #fff; }
.area-map { display: flex; flex-direction: column; min-height: 0; min-width: 0; height: 100%; overflow: hidden; background: #081e3d; }
.dock-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 12px; flex-wrap: wrap; flex-shrink: 0; border-bottom: 1px solid #23436e; color: #e1edff; }
.dock-toolbar b { font-size: 13px; margin-right: 10px; }
.dock-toolbar span { font-size: 12px; color: #94afcf; }
.dock-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.dock-actions select { max-width: 180px; }
.dock-actions button, .dock-actions select { background: #12335e; color: #deebff; border: 1px solid #335685; border-radius: 4px; padding: 5px 9px; font-size: 12px; }
.dock-actions button { cursor: pointer; }
.dock-actions button:disabled, .dock-actions select:disabled { opacity: .5; cursor: default; }
.dock-message { margin: 0; padding: 6px 12px; color: #f3ca8b; font-size: 12px; }
.area-map :deep(.construction-dock-marker) { display: flex; align-items: center; gap: 7px; width: max-content; }
.area-map :deep(.dock-pin) { display: grid; place-items: center; width: 36px; height: 36px; border: 2px solid #c2f5ff; background: #087eab; color: white; border-radius: 50%; box-shadow: 0 0 0 5px #20bce533, 0 3px 12px #00142ecc; font-size: 19px; font-weight: bold; }
.area-map :deep(.dock-label) { padding: 5px 10px; border: 1px solid #45c6e8; border-radius: 4px; color: #e4faff; background: #092a4ded; white-space: nowrap; font-size: 12px; }
.map-toolbar { display: flex; align-items: center; gap: 8px; padding: 8px 10px; background: #0b2756; border: 1px solid #23436e; border-bottom: 0; border-radius: 4px 4px 0 0; flex-wrap: wrap; }
.map-toolbar button { padding: 4px 12px; font-size: 12px; color: #d4e2f5; background: #12335e; border: 1px solid #335685; border-radius: 3px; cursor: pointer; }
.map-toolbar button:hover:not(:disabled) { border-color: #409eff; }
.map-toolbar button.active { color: #fff; background: #2f7bd9; border-color: #409eff; }
.map-toolbar button.primary-draw { color: #fff; background: #2f7bd9; border-color: #409eff; }
.map-toolbar button:disabled { opacity: 0.5; cursor: not-allowed; }
.map-toolbar button.cancel-draw { color: #ffc9bd; border-color: #8a5a52; }
.map-toolbar button.cancel-draw:hover { border-color: #ff9f80; }
.map-hint { font-size: 12px; color: #a8bdd8; }
.map-error { padding: 6px 10px; margin-bottom: 6px; font-size: 12px; color: #ffd0c5; background: #532e35; border: 1px solid #90515d; border-radius: 4px; }
.map-container { flex: 1; min-height: 280px; overflow: hidden; }
</style>
