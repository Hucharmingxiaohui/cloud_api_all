<template>
  <div v-if="picking" class="tower-tools" @click.stop @dblclick.stop>
    <button @click="emit('cancel-picking')">取消标定</button>
    <span>请在地图空白处单击标定杆塔位置</span>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'
import type { ConstructionTower } from '/@/api/construction'
import { gcj02towgs84, wgs84togcj02 } from '/@/vendors/coordtransform'
declare const AMap: any
/* global defineProps, defineEmits */
const props = defineProps<{ map: any; towers: ConstructionTower[]; selectedId?: string | null; picking?: boolean; disabled?: boolean }>()
const emit = defineEmits(['select', 'pick', 'cancel-picking'])
const overlays: any[] = []
function remove () { if (props.map && overlays.length) props.map.remove(overlays); overlays.length = 0 }
function render () {
  remove()
  if (!props.map) return
  for (const tower of props.towers) {
    const selected = tower.id === props.selectedId
    const content = document.createElement('span')
    content.className = 'construction-tower-marker'
    content.textContent = tower.tower_no
    content.style.background = selected ? '#ff9f43' : '#2d6cdf'
    content.style.borderColor = selected ? '#ffd9a0' : '#8fc4ff'
    const marker = new AMap.Marker({ position: wgs84togcj02(tower.lng, tower.lat), content, offset: new AMap.Pixel(-14, -14), zIndex: selected ? 175 : 150, title: tower.tower_no })
    marker.on('click', () => { if (!props.disabled && !props.picking) emit('select', tower.id) })
    overlays.push(marker)
  }
  props.map.add(overlays)
}
function click (event: any) {
  if (!props.picking || props.disabled || !event?.lnglat) return
  const position = gcj02towgs84(event.lnglat.lng, event.lnglat.lat)
  emit('pick', { lng: position[0], lat: position[1] })
}
watch(() => props.picking, value => {
  if (!props.map) return
  if (value) props.map.on('click', click)
  else props.map.off('click', click)
})
watch(() => [props.towers, props.selectedId], () => render(), { deep: true })
watch(() => props.map, () => render(), { immediate: true })
onBeforeUnmount(() => { props.map?.off('click', click); remove() })
</script>

<style scoped>
.tower-tools { position:absolute; bottom:12px; left:12px; right:12px; z-index:240; display:flex; flex-wrap:wrap; align-items:center; gap:8px; padding:10px; background:#102846f5; color:#e9e1d5; border:1px solid #be8a50; border-radius:5px; font-size:12px; }
.tower-tools button { padding:5px 9px; background:#3d352d; color:#ffe2bc; border:1px solid #b1804a; border-radius:4px; cursor:pointer; }
</style>
