<template>
  <section class="parking-panel">
    <div class="parking-heading"><b>车载驻车点</b><select :value="selectedId" :disabled="busy || picking || locked" aria-label="选择驻车点" @change="choose(($event.target as HTMLSelectElement).value)"><option value="">请选择驻车点</option><option v-for="p in points" :key="p.id" :value="p.id">{{ p.name }}</option></select><button :disabled="busy || locked || !!draft" @click="create">＋ 标定驻车点</button><button :disabled="busy || !!draft" @click="load">刷新</button></div>
    <p v-if="error" class="parking-error" role="alert">{{ error }}</p>
    <div v-if="draft" class="parking-form">
      <label>名称 <input v-model="draft.name" maxlength="80" :disabled="busy" placeholder="例如：北侧施工道路驻车点" /></label>
      <label>半径 <input v-model.number="draft.radius_m" type="number" min="100" max="5000" step="1" :disabled="busy" /> 米</label>
      <button v-for="r in [100, 2000, 5000]" :key="r" :disabled="busy" @click="draft.radius_m = r">{{ r === 100 ? '100m' : r / 1000 + 'km' }}</button>
      <span>{{ draft.lng === null ? '尚未选点' : `WGS84：${draft.lng.toFixed(6)}, ${draft.lat!.toFixed(6)}` }}</span>
      <button :disabled="busy || locked" @click="startPicking">{{ picking ? '请在地图上单击选点…' : '重新地图选点' }}</button>
      <button :disabled="busy || !validDraft" @click="save">{{ busy ? '保存中…' : '保存驻车点' }}</button><button :disabled="busy" @click="cancel">取消</button>
    </div>
    <div v-else-if="active" class="parking-details">
      <strong>{{ active.name }}</strong><span>覆盖半径 {{ active.radius_m }}m</span><span class="parking-distance">{{ distanceText }}</span>
      <button :disabled="locked" @click="fit">定位驻车点</button><button :disabled="busy || locked" @click="edit">编辑</button><button :disabled="busy || locked" @click="remove">删除</button>
    </div>
    <div v-if="center && validRadius" class="parking-areas"><b>{{ draft ? '预览覆盖' : '驻车点覆盖' }}工作面 {{ covered.length }} 个</b><span>（当前已加载工作面）</span><span v-if="!covered.length">暂无相交工作面</span><button v-for="item in covered" :key="item.area.id" :class="item.coverage" :disabled="locked || picking" @click="emit('select-area', item.area.id)">{{ item.area.name }} · {{ item.coverage === 'full' ? '完全覆盖' : '部分覆盖' }}</button></div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { listParkingPoints, createParkingPoint, updateParkingPoint, deleteParkingPoint } from '/@/api/construction'
import type { ConstructionParkingPoint, ConstructionPoint } from '/@/api/construction'
import type { ConstructionDockMarker } from './useConstructionDocks'
import { gcj02towgs84, wgs84togcj02 } from '/@/vendors/coordtransform'
import { areaCoverage, coverageBoundary, groundDistance } from './dockCoverageGeometry'
declare const AMap: any
/* global defineProps, defineEmits, defineExpose */
const props = defineProps<{ map: any; areas: Array<{ id: string; name: string; boundary: ConstructionPoint[] }>; docks: ConstructionDockMarker[]; dockSn: string; locked: boolean }>()
const emit = defineEmits(['select-area', 'picking', 'editing', 'selected'])
const points = ref<ConstructionParkingPoint[]>([]); const selectedId = ref(''); const busy = ref(false); const error = ref(''); const picking = ref(false)
type Draft = { id?: string; version?: number; name: string; lng: number | null; lat: number | null; radius_m: number }
const draft = ref<Draft | null>(null)
const active = computed(() => points.value.find(p => p.id === selectedId.value))
const center = computed(() => draft.value ? (draft.value.lng !== null && draft.value.lat !== null ? { lng: draft.value.lng, lat: draft.value.lat } : null) : active.value || null)
const radius = computed(() => draft.value?.radius_m ?? active.value?.radius_m ?? 0)
const validRadius = computed(() => Number.isInteger(radius.value) && radius.value >= 100 && radius.value <= 5000)
const validDraft = computed(() => !!draft.value?.name.trim() && !!center.value && validRadius.value)
const dock = computed(() => props.docks.find(d => d.sn === props.dockSn) || (!props.dockSn && props.docks.length === 1 ? props.docks[0] : null))
const distanceText = computed(() => {
  if (!dock.value || !center.value) return props.docks.length ? '请选择当前机巢以显示连线和距离' : '等待机巢上报定位后显示距离'
  const metres = groundDistance(dock.value, center.value)
  return `${dock.value.name} → 驻车点：${metres >= 1000 ? (metres / 1000).toFixed(2) + ' km' : Math.round(metres) + ' m'}（直线距离）`
})
const covered = computed(() => !center.value || !validRadius.value ? [] : props.areas.map(area => ({ area, coverage: areaCoverage(center.value!, radius.value, area.boundary || []) })).filter(p => p.coverage).sort((a, b) => a.coverage === b.coverage ? a.area.name.localeCompare(b.area.name, 'zh-CN') : a.coverage === 'full' ? -1 : 1))
let overlays: any[] = []; let focusOverlays: any[] = []; let alive = true; let epoch = 0
// The bundled map renderer may recreate custom marker DOM; delegate to its stable container.
function markerEvent (event: Event) {
  const target = (event.target as HTMLElement)?.closest?.('[data-parking-id]') as HTMLElement | null
  if (!target?.dataset.parkingId) return
  if (event instanceof KeyboardEvent && event.key !== 'Enter' && event.key !== ' ') return
  event.preventDefault(); event.stopPropagation(); choose(target.dataset.parkingId)
}
function failure (e: unknown) { error.value = e instanceof Error ? e.message : '驻车点操作失败' }
function stopPicking () { const wasPicking = picking.value; picking.value = false; props.map.off('click', pick); props.map.setDefaultCursor?.(''); if (wasPicking) emit('picking', false) }
function startPicking () { if (props.locked || busy.value) return; stopPicking(); picking.value = true; props.map.on('click', pick); props.map.setDefaultCursor?.('crosshair'); emit('picking', true) }
function pick (event: any) {
  if (!picking.value || !draft.value || !event.lnglat || props.locked) return
  const p = gcj02towgs84(event.lnglat.lng, event.lnglat.lat)
  draft.value.lng = p[0]; draft.value.lat = p[1]; stopPicking()
}
function cancel () { stopPicking(); draft.value = null; error.value = '' }
function create () { draft.value = { name: '', lng: null, lat: null, radius_m: 2000 }; error.value = ''; startPicking() }
function edit () { if (active.value) { draft.value = { ...active.value }; error.value = '' } }
async function choose (id: string) {
  if (props.locked || busy.value || picking.value) return
  if (draft.value) { try { await ElMessageBox.confirm('放弃未保存的驻车点修改？', '切换驻车点') } catch { return } }
  cancel(); selectedId.value = id; render(); fit(); if (id) emit('selected')
}
async function load () {
  const token = ++epoch; busy.value = true; error.value = ''
  try { const rows = await listParkingPoints(); if (!alive || token !== epoch) return; points.value = rows; if (!rows.some(p => p.id === selectedId.value)) selectedId.value = '' } catch (e) { if (alive && token === epoch) failure(e) } finally { if (alive && token === epoch) busy.value = false }
}
async function save () {
  if (!draft.value || !validDraft.value || busy.value) return
  stopPicking(); const value = { ...draft.value, name: draft.value.name.trim(), lng: draft.value.lng!, lat: draft.value.lat! }; busy.value = true; error.value = ''
  try { const saved = value.id ? await updateParkingPoint(value.id, value) : await createParkingPoint(value); if (!alive) return; points.value = [...points.value.filter(p => p.id !== saved.id), saved]; selectedId.value = saved.id; draft.value = null; render(); fit() } catch (e) { if (alive) failure(e) } finally { if (alive) busy.value = false }
}
async function remove () {
  const p = active.value; if (!p || busy.value) return
  try { await ElMessageBox.confirm(`删除驻车点“${p.name}”？`, '删除驻车点', { type: 'warning' }) } catch { return }
  busy.value = true; error.value = ''
  try { await deleteParkingPoint(p.id, p.version); if (!alive) return; points.value = points.value.filter(row => row.id !== p.id); selectedId.value = '' } catch (e) { if (alive) failure(e) } finally { if (alive) busy.value = false }
}
function marker (point: ConstructionPoint, name: string, selected: boolean, id?: string) {
  const content = document.createElement('div'); content.className = 'parking-marker' + (selected ? ' selected' : '')
  if (id) {
    content.setAttribute('role', 'button'); content.tabIndex = 0; content.setAttribute('aria-label', `选择驻车点 ${name}`)
    content.dataset.parkingId = id
  }
  const pin = document.createElement('span'); pin.className = 'parking-pin'; pin.textContent = 'P'
  const label = document.createElement('span'); label.className = 'parking-label'; label.textContent = name
  content.append(pin, label)
  const item = new AMap.Marker({ position: wgs84togcj02(point.lng, point.lat), content, offset: new AMap.Pixel(-16, -16), zIndex: selected ? 210 : 190, title: name })
  overlays.push(item); if (selected) focusOverlays.push(item)
}
function render () {
  overlays.forEach(o => props.map.remove(o)); overlays = []; focusOverlays = []
  points.value.forEach(p => { if (draft.value?.id !== p.id) marker(p, p.name, !draft.value && p.id === selectedId.value, p.id) })
  if (center.value) {
    if (draft.value) marker(center.value, draft.value.name || '待保存驻车点', true)
    if (validRadius.value) { const circle = new AMap.Polygon({ path: coverageBoundary(center.value, radius.value).map(p => wgs84togcj02(p.lng, p.lat)), strokeColor: '#bd98ff', strokeWeight: 2, strokeStyle: draft.value ? 'dashed' : 'solid', fillColor: '#ad85ff', fillOpacity: 0.12, zIndex: 35, bubble: true }); overlays.push(circle); focusOverlays.push(circle) }
    if (dock.value) {
      const line = new AMap.Polyline({ path: [dock.value, center.value].map(p => wgs84togcj02(p.lng, p.lat)), strokeColor: '#f7bd70', strokeWeight: 3, strokeStyle: 'dashed', zIndex: 170 }); overlays.push(line); focusOverlays.push(line)
      const content = document.createElement('span'); content.className = 'parking-distance-label'; content.textContent = distanceText.value
      overlays.push(new AMap.Marker({ position: wgs84togcj02((dock.value.lng + center.value.lng) / 2, (dock.value.lat + center.value.lat) / 2), content, offset: new AMap.Pixel(0, -42), zIndex: 205 }))
    }
  }
  overlays.forEach(o => props.map.add(o))
}
function fit () { if (focusOverlays.length) props.map.setFitView(focusOverlays, false, [70, 70, 70, 70]) }
watch([points, selectedId, draft, () => props.docks, () => props.dockSn], render, { deep: true })
watch(() => props.locked, value => { if (value) stopPicking() })
watch(() => !!draft.value, value => emit('editing', value))
defineExpose({ cancelPicking: stopPicking })
onMounted(() => { props.map.getContainer().addEventListener('click', markerEvent, true); props.map.getContainer().addEventListener('keydown', markerEvent, true); load() })
onBeforeUnmount(() => { alive = false; epoch++; stopPicking(); props.map.getContainer().removeEventListener('click', markerEvent, true); props.map.getContainer().removeEventListener('keydown', markerEvent, true); overlays.forEach(o => props.map.remove(o)) })
</script>

<style scoped>
.parking-panel{flex-shrink:0;background:#172d50;border-top:1px solid #4d507a;padding:10px 12px;color:#dbe9ff;font-size:12px;max-height:260px;overflow:auto}.parking-heading,.parking-form,.parking-details,.parking-areas{display:flex;align-items:center;flex-wrap:wrap;gap:8px}.parking-heading b{color:#d2b5ff}.parking-panel button,.parking-panel select,.parking-panel input{background:#143252;color:#e5edff;border:1px solid #526a99;border-radius:4px;padding:6px 9px}.parking-panel button{cursor:pointer}.parking-panel button:disabled{opacity:.5;cursor:not-allowed}.parking-panel input[type=number]{width:85px}.parking-panel select{max-width:220px}.parking-form,.parking-details,.parking-areas{margin-top:10px}.parking-distance{color:#ffd594}.parking-error{color:#ffb5a4;margin:6px 0}.parking-areas .full{border-color:#73bba6;color:#b9f3dd}.parking-areas .partial{color:#ffe0aa;border-color:#ac874c}
</style>
