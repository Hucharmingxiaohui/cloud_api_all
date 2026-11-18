<template>
  <div v-if="editable" class="line-tools" @click.stop @dblclick.stop>
    <button :disabled="disabled || drawing" @click="begin">绘制折线</button>
    <button v-if="drawing" :disabled="pending.length < 2" @click="finish">完成线路</button>
    <button v-if="drawing" :disabled="!pending.length" @click="undo">撤回点</button>
    <button v-if="drawing" @click="cancel">取消绘制</button>
    <button :disabled="disabled || drawing || !points.length" @click="emit('update:points', [])">清空重绘</button>
    <span>{{ drawing ? `已取 ${pending.length} 点，完成后才能保存` : '拖动节点微调；增加节点请清空重绘' }}</span>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { ConstructionLine, ConstructionPoint } from '/@/api/construction'
import { gcj02towgs84, wgs84togcj02 } from '/@/vendors/coordtransform'
import { validateLine } from './constructionLineModel'
declare const AMap: any
/* global defineProps, defineEmits */
const props = defineProps<{ map: any; lines: ConstructionLine[]; selectedId?: string | null; points: ConstructionPoint[]; editable?: boolean; disabled?: boolean; color?: string; name?: string }>()
const emit = defineEmits(['select', 'update:points', 'drawing'])
const drawing = ref(false)
const pending = ref<ConstructionPoint[]>([])
const overlays: any[] = []
const temporary: any[] = []
let previousDoubleClickZoom = true
const gcj = (points: ConstructionPoint[]) => points.map(p => wgs84togcj02(p.lng, p.lat))
function remove (items: any[]) { if (props.map && items.length) props.map.remove(items); items.length = 0 }
function label (text: string, point: ConstructionPoint, color: string) {
  const content = document.createElement('span')
  content.className = 'construction-line-label'; content.textContent = text; content.style.borderColor = color
  return new AMap.Marker({ position: wgs84togcj02(point.lng, point.lat), content, offset: new AMap.Pixel(0, -12), zIndex: 145 })
}
function render (fit = false) {
  remove(overlays)
  if (!props.map || drawing.value) return
  let selectedOverlay: any = null
  const records = props.lines.filter(line => !line.archived && line.line_id !== props.selectedId)
  if (props.points.length) records.push({ line_id: props.selectedId || '', points: props.points, name: props.name || '新线路', color: props.color || '#FF9F43', enabled: true, archived: false } as ConstructionLine)
  for (const line of records) {
    if (line.points.length < 2) continue
    const selected = line.line_id === (props.selectedId || '')
    const overlay = new AMap.Polyline({ path: gcj(line.points), strokeColor: line.color, strokeWeight: selected ? 6 : 3, strokeStyle: line.enabled ? 'solid' : 'dashed', zIndex: selected ? 130 : 100, bubble: false })
    const select = () => { if (!props.disabled && !props.editable && !drawing.value) emit('select', line.line_id) }
    overlay.on('click', select); overlays.push(overlay)
    const name = label(line.name, line.points[Math.floor((line.points.length - 1) / 2)], line.color)
    name.on('click', select); overlays.push(name)
    if (selected) {
      selectedOverlay = overlay
      line.points.forEach((point, index) => {
        const content = document.createElement('span'); content.className = 'construction-line-node'
        content.textContent = index === 0 ? '起' : index === line.points.length - 1 ? '终' : '·'
        content.style.background = line.color
        const marker = new AMap.Marker({ position: wgs84togcj02(point.lng, point.lat), content, draggable: !!props.editable && !props.disabled, zIndex: 155, offset: new AMap.Pixel(-10, -10) })
        marker.on('dragend', () => {
          if (!props.editable || props.disabled || drawing.value) return
          const position = marker.getPosition(); const value = gcj02towgs84(position.lng, position.lat)
          const next = props.points.map(p => ({ ...p })); next[index] = { lng: value[0], lat: value[1] }
          const error = validateLine(next)
          if (error) { ElMessage.error(error); render(); return }
          emit('update:points', next)
        })
        overlays.push(marker)
      })
    }
  }
  props.map.add(overlays)
  if (fit && selectedOverlay) props.map.setFitView([selectedOverlay], false, [70, 70, 70, 70])
}
function renderPending () {
  remove(temporary)
  if (!props.map) return
  if (pending.value.length >= 2) temporary.push(new AMap.Polyline({ path: pending.value.map(p => [p.lng, p.lat]), strokeColor: props.color || '#FF9F43', strokeWeight: 4, zIndex: 160, bubble: true }))
  if (pending.value.length === 1) temporary.push(new AMap.Marker({ position: [pending.value[0].lng, pending.value[0].lat], zIndex: 160 }))
  if (temporary.length) props.map.add(temporary)
}
function click (event: any) {
  if (!drawing.value || props.disabled || !event?.lnglat) return
  const p = { lng: event.lnglat.lng, lat: event.lnglat.lat }; const last = pending.value[pending.value.length - 1]
  if (last && Math.abs(last.lng - p.lng) < 1e-9 && Math.abs(last.lat - p.lat) < 1e-9) return
  if (pending.value.length >= 200) { ElMessage.warning('线路最多支持200个折点'); return }
  pending.value.push(p); renderPending()
}
function begin () {
  if (!props.map || props.disabled || drawing.value || !props.editable) return
  previousDoubleClickZoom = props.map.getStatus?.().doubleClickZoom ?? true
  props.map.setStatus?.({ doubleClickZoom: false })
  drawing.value = true; pending.value = []; remove(overlays)
  props.map.on('click', click); emit('drawing', true)
}
function stop () {
  props.map?.off('click', click); remove(temporary)
  if (drawing.value) props.map?.setStatus?.({ doubleClickZoom: previousDoubleClickZoom })
  drawing.value = false; pending.value = []; emit('drawing', false)
}
function cancel () { stop(); render() }
function undo () { pending.value.pop(); renderPending() }
function finish () {
  const points = pending.value.map(p => { const value = gcj02towgs84(p.lng, p.lat); return { lng: value[0], lat: value[1] } })
  const error = validateLine(points)
  if (error) { ElMessage.error(error); return }
  stop(); emit('update:points', points)
}
watch(() => props.selectedId, () => render(true))
watch(() => [props.lines, props.points, props.editable, props.disabled, props.color, props.name], (_value, previous) => { if (!props.editable && drawing.value) stop(); render(!props.editable && !!previous && !previous[2] && props.points !== previous[1]) }, { deep: true })
watch(() => props.map, () => render(true), { immediate: true })
onBeforeUnmount(() => { stop(); remove(overlays) })
</script>

<style scoped>
.line-tools { position:absolute; bottom:12px; left:12px; right:12px; z-index:240; display:flex; flex-wrap:wrap; align-items:center; gap:8px; padding:10px; background:#102846f5; color:#e9e1d5; border:1px solid #be8a50; border-radius:5px; font-size:12px; }
.line-tools button { padding:5px 9px; background:#3d352d; color:#ffe2bc; border:1px solid #b1804a; border-radius:4px; cursor:pointer; }
.line-tools button:disabled { opacity:.5; cursor:default; }
</style>
