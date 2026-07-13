<template>
  <div
    class="cloud-renderer"
    @mousedown="handleMouseDown"
    @mousemove="handleMouseMove"
    @mouseup="handleMouseUp"
    @mouseleave="handleMouseUp"
    @contextmenu.prevent
    @wheel.prevent="handleWheel"
    @dblclick="handleDoubleClick"
  >
    <video ref="videoRef" class="cloud-renderer__video" autoplay muted playsinline></video>
    <div v-if="statusText" class="cloud-renderer__status">{{ statusText }}</div>
    <button class="cloud-renderer__clear" type="button" @click.stop="clearPath">清除轨迹</button>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { cloudRendererClient } from './cloudRendererClient'

const videoRef = ref<HTMLVideoElement | null>(null)
const statusText = ref('云渲染连接中...')

let dragging = false
let lastX = 0
let lastY = 0
let dragAction: 'orbit' | 'pan' = 'orbit'
let stopStatusListener: (() => void) | null = null

function handleMouseDown (event: MouseEvent) {
  dragging = true
  lastX = event.clientX
  lastY = event.clientY
  dragAction = event.button === 2 ? 'pan' : 'orbit'
}

function handleMouseMove (event: MouseEvent) {
  if (!dragging) return
  const dx = event.clientX - lastX
  const dy = event.clientY - lastY
  lastX = event.clientX
  lastY = event.clientY
  cloudRendererClient.sendInput(dragAction, { dx, dy })
}

function handleMouseUp () {
  dragging = false
}

function handleWheel (event: WheelEvent) {
  cloudRendererClient.sendInput('zoom', { delta: event.deltaY })
}

function handleDoubleClick (event: MouseEvent) {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  cloudRendererClient.pick((event.clientX - rect.left) / rect.width, (event.clientY - rect.top) / rect.height)
}

function clearPath () {
  cloudRendererClient.clearPath()
}

onMounted(() => {
  stopStatusListener = cloudRendererClient.onStatusChange(status => {
    statusText.value = status
  })
  cloudRendererClient.attachVideo(videoRef.value)
  cloudRendererClient.start().then(() => {
    cloudRendererClient.attachVideo(videoRef.value)
  })
})

onBeforeUnmount(() => {
  stopStatusListener?.()
  stopStatusListener = null
  cloudRendererClient.detachVideo(videoRef.value)
})
</script>

<style scoped>
.cloud-renderer {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #050b12;
  user-select: none;
}

.cloud-renderer__video {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  background: #050b12;
}

.cloud-renderer__status {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  padding: 10px 18px;
  color: #dffcff;
  background: rgba(2, 18, 28, 0.78);
  border: 1px solid rgba(121, 242, 238, 0.6);
  border-radius: 4px;
  font-size: 14px;
}

.cloud-renderer__clear {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 6px 12px;
  color: #dffcff;
  background: rgba(2, 18, 28, 0.72);
  border: 1px solid rgba(121, 242, 238, 0.55);
  border-radius: 4px;
  cursor: pointer;
}

.cloud-renderer__clear:hover {
  background: rgba(11, 68, 82, 0.86);
}
</style>
