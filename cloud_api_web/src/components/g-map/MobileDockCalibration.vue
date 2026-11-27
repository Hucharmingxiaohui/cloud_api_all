<template>
  <section class="mobile-calibration">
    <div class="calibration-title"><strong>车载机巢标定</strong><button :disabled="loading || submitting" @click="refresh">刷新状态</button></div>
    <p>{{ error || info?.message || '正在读取机场状态…' }}</p>
    <p v-if="info?.coordinate" class="coordinate">经度 {{ info.coordinate.longitude.toFixed(8) }} · 纬度 {{ info.coordinate.latitude.toFixed(8) }}<br>椭球高 {{ info.coordinate.height.toFixed(3) }} 米 · 来源：机场自收敛坐标</p>
    <p v-if="info?.operation" role="status" :class="{ failed: ['failed', 'unknown'].includes(info.operation.state) }">{{ info.operation.message }}</p>
    <button class="calibrate" :disabled="!info?.ready || !!error || submitting || loading" @click="start">{{ submitting || info?.operation?.state === 'pending' ? '标定处理中…' : '车载机巢标定' }}</button>
  </section>
</template>

<script setup lang="ts">
/* global defineProps */
import { ref, watch, onBeforeUnmount } from 'vue'
import { Modal } from 'ant-design-vue'
import request from '/@/api/http/request'

interface CalibrationStatus {
  ready: boolean; message: string
  coordinate?: { longitude: number; latitude: number; height: number }
  operation?: { state: string; message: string }
}
const props = defineProps<{ sn: string }>()
const info = ref<CalibrationStatus | null>(null)
const error = ref('')
const loading = ref(false)
const submitting = ref(false)
let generation = 0
let timer: ReturnType<typeof setTimeout> | undefined
let confirmation: ReturnType<typeof Modal.confirm> | undefined
function url (sn: string) { return `/api/mobile-docks/${encodeURIComponent(sn)}/calibration` }
function unpack (response: any): CalibrationStatus {
  if (response.data.code !== 0) throw new Error(response.data.message || '标定请求失败')
  return response.data.data
}
function schedule (token: number) {
  clearTimeout(timer)
  if (token === generation) timer = setTimeout(refresh, 3000)
}
async function refresh () {
  const token = generation
  const sn = props.sn
  if (!sn || submitting.value) return
  loading.value = true
  try {
    const data = unpack(await request.get(url(sn), { timeout: 10000 }))
    if (token === generation) { info.value = data; error.value = '' }
  } catch (e: any) {
    if (token === generation) { error.value = e?.message || '机场状态读取失败'; info.value = null }
  } finally {
    if (token === generation) { loading.value = false; schedule(token) }
  }
}
function start () {
  const token = generation
  const sn = props.sn
  confirmation = Modal.confirm({
    title: '车载机巢标定',
    content: '请确认车辆已停稳、飞行器已回舱。将使用机场最新自收敛经纬度和椭球高度进行标定；此操作不会提高自收敛坐标的测量精度。',
    okText: '开始标定',
    cancelText: '取消',
    async onOk () {
      if (token !== generation || submitting.value) return
      submitting.value = true
      clearTimeout(timer)
      try {
        const data = unpack(await request.post(url(sn), {}, { timeout: 15000 }))
        if (token === generation) { info.value = data; error.value = '' }
      } catch (e: any) {
        if (token === generation) error.value = e?.message || '请求未完成，请刷新核对状态，勿重复提交'
      } finally {
        if (token === generation) { submitting.value = false; schedule(token) }
      }
    },
  })
}
watch(() => props.sn, () => {
  generation++; clearTimeout(timer); confirmation?.destroy()
  info.value = null; error.value = ''; submitting.value = false
  refresh()
}, { immediate: true })
onBeforeUnmount(() => { generation++; clearTimeout(timer); confirmation?.destroy() })
</script>

<style scoped>
.mobile-calibration{margin:10px 5px;padding:12px;border:1px solid #31547c;border-radius:6px;background:#102742;color:#cbe3f8;font-size:12px}
.calibration-title{display:flex;justify-content:space-between;align-items:center;gap:12px}.mobile-calibration p{margin:8px 0;line-height:1.7;overflow-wrap:anywhere}.coordinate{color:#9bbbd6}.failed{color:#ffb687}
.mobile-calibration button{padding:5px 10px;border:1px solid #47779c;border-radius:4px;background:#163a5c;color:#d8efff;cursor:pointer}.mobile-calibration button:disabled{opacity:.5;cursor:not-allowed}.mobile-calibration .calibrate{background:#096c9b;border-color:#38a9d0}.mobile-calibration button:focus-visible{outline:2px solid #72d3ff;outline-offset:2px}
</style>
