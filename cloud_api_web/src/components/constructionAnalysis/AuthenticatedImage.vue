<template>
  <div class="authenticated-image">
    <span v-if="busy">正在读取图片…</span>
    <img v-else-if="url" :src="url" :alt="alt" loading="lazy" @error="decodeError">
    <a-button v-else size="small" @click="load">{{ error || '加载图片' }} · 重试</a-button>
  </div>
</template>

<script setup lang="ts">
import { defineProps, onBeforeUnmount, ref, watch } from 'vue'
import { getOriginal, getResultImage } from '/@/api/construction-analysis'
import { errorText } from './model'

const props = defineProps<{ taskId: string; imageId: string; batchId?: string; alt: string }>()
const url = ref(''); const busy = ref(false); const error = ref(''); let generation = 0
function release () { if (url.value) URL.revokeObjectURL(url.value); url.value = '' }
function decodeError () { release(); error.value = '图片无法解码' }
async function load () {
  const g = ++generation; release(); busy.value = true; error.value = ''
  try {
    const blob = props.batchId ? await getResultImage(props.taskId, props.batchId, props.imageId) : await getOriginal(props.taskId, props.imageId)
    if (g === generation) url.value = URL.createObjectURL(blob)
  } catch (e) { if (g === generation) error.value = '图片读取失败：' + errorText(e) } finally { if (g === generation) busy.value = false }
}
watch(() => [props.taskId, props.imageId, props.batchId], load, { immediate: true })
onBeforeUnmount(() => { generation++; release() })
</script>

<style scoped>
.authenticated-image { display: flex; justify-content: center; align-items: center; min-height: 64px; background: #edf2f7; color: #52677d; overflow: auto; }
img { display: block; max-width: 100%; max-height: 60vh; object-fit: contain; }
</style>
