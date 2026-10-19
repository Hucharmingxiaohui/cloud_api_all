<template>
  <div class="accessory-page">
    <header><strong>光伏附属设备管理</strong><span>正射图采集 · 设备与预置拍摄航点</span>
      <el-button :disabled="!photoId || busy" @click="newDevice">新增设备</el-button>
      <el-button type="primary" :disabled="!draft || busy" :loading="saving" @click="save">保存当前设备</el-button>
    </header>
    <div class="workspace">
      <aside class="device-panel">
        <h3>选择正射图</h3>
        <el-select :model-value="photoId" filterable placeholder="请选择正射图" :disabled="busy" @change="switchPhoto">
          <el-option v-for="photo in photos" :key="photo.id" :label="photo.name" :value="photo.id" />
        </el-select>
        <h3>附属设备</h3>
        <el-select v-model="filter" clearable placeholder="全部设备类型"><el-option v-for="type in deviceTypes" :key="type.value" :label="type.label" :value="type.value" /></el-select>
        <div class="device-list">
          <button v-for="device in filteredDevices" :key="device.id" class="device-row" :class="{ active: draft?.id === device.id }" :disabled="busy" @click="selectDevice(device)">
            <b>{{ device.name }}</b><small>{{ typeName(device.device_type) }} · {{ device.waypoints.length }}个航点 · {{ device.enabled ? '启用' : '停用' }}</small>
          </button>
          <p v-if="!filteredDevices.length" class="hint">暂无设备，请点击“新增设备”开始采集。</p>
        </div>
      </aside>
      <main v-loading="loading || locating" class="map-panel">
        <div class="map-toolbar">
          <el-button size="small" :disabled="!imageUrl" @click="zoom = Math.min(5, zoom + 0.25)">放大</el-button>
          <el-button size="small" :disabled="!imageUrl" @click="zoom = Math.max(0.5, zoom - 0.25)">缩小</el-button>
          <el-button size="small" @click="zoom = 1">适应</el-button>
          <span>{{ mode === 'device' ? '点击正射图设置设备位置' : mode === 'waypoint' ? '点击正射图添加拍摄航点' : '点击设备或航点编辑属性' }}</span>
          <el-button v-if="mode" size="small" @click="mode = ''">取消落点</el-button>
        </div>
        <div class="map-scroll">
          <svg v-if="imageUrl" ref="mapSvg" :viewBox="`0 0 ${imageSize.width} ${imageSize.height}`" :style="{ width: `${zoom * 100}%` }" class="ortho-map" @click="mapClick">
            <image :href="imageUrl" :width="imageSize.width" :height="imageSize.height" />
            <polygon v-for="(region, index) in regions" :key="index" :points="region" class="region" />
            <polyline v-if="waypointPixels.length" :points="waypointLine" class="route-line" />
            <g v-for="(device, index) in mapDevices" :key="device.id || 'draft'" @click.stop="selectMapDevice(device)">
              <template v-if="devicePixels[index]">
                <circle :cx="devicePixels[index].col" :cy="devicePixels[index].row" :r="markerSize" class="device-marker" :class="{ disabled: !device.enabled }" />
                <text :x="devicePixels[index].col + markerSize * 1.3" :y="devicePixels[index].row" :font-size="markerSize * 1.6">{{ device.name || '新设备' }}</text>
              </template>
            </g>
            <g v-for="(pixel, index) in waypointPixels" :key="index" @click.stop="pointIndex = index">
              <template v-if="pixel">
              <line v-if="selectedDevicePixel" :x1="selectedDevicePixel.col" :y1="selectedDevicePixel.row" :x2="pixel.col" :y2="pixel.row" class="sight-line" />
              <circle :cx="pixel.col" :cy="pixel.row" :r="markerSize" class="waypoint-marker" :class="{ selected: pointIndex === index }" />
              <line :x1="pixel.col" :y1="pixel.row" :x2="pixel.col + Math.sin((draft?.waypoints[index].heading || 0) * Math.PI / 180) * markerSize * 3" :y2="pixel.row - Math.cos((draft?.waypoints[index].heading || 0) * Math.PI / 180) * markerSize * 3" class="heading-line" />
              <text :x="pixel.col" :y="pixel.row + markerSize * 0.5" text-anchor="middle" :font-size="markerSize * 1.5">{{ index + 1 }}</text>
              </template>
            </g>
          </svg>
          <div v-else class="empty-map">请选择正射图</div>
        </div>
      </main>
      <aside class="property-panel">
        <template v-if="draft">
          <h3>设备信息 <small v-if="dirty">（未保存）</small></h3>
          <el-form label-position="top" size="small" :disabled="busy">
            <el-form-item label="设备名称"><el-input v-model="draft.name" maxlength="100" /></el-form-item>
            <el-form-item label="设备类型"><el-select v-model="draft.device_type"><el-option v-for="type in deviceTypes" :key="type.value" :label="type.label" :value="type.value" /></el-select></el-form-item>
            <el-form-item label="设备经度"><el-input-number v-model="draft.longitude" :min="-180" :max="180" :precision="7" :controls="false" @change="projectMarkers" /></el-form-item>
            <el-form-item label="设备纬度"><el-input-number v-model="draft.latitude" :min="-90" :max="90" :precision="7" :controls="false" @change="projectMarkers" /></el-form-item>
            <el-form-item label="设备绝对椭球高（m）"><el-input-number v-model="draft.height" :min="-1000" :max="10000" :precision="2" :controls="false" /></el-form-item>
            <el-form-item label="启用"><el-switch v-model="draft.enabled" /></el-form-item>
          </el-form>
          <el-button size="small" :disabled="busy || !imageUrl" @click="mode = 'device'">地图设置设备位置</el-button>
          <el-button size="small" type="danger" :disabled="busy" @click="removeDevice">删除设备</el-button>
          <h3>预置航点（{{ draft.waypoints.length }}）</h3>
          <el-button size="small" :disabled="busy || !imageUrl" @click="mode = 'waypoint'">地图添加航点</el-button>
          <el-button size="small" :disabled="busy" @click="addWaypoint()">手动添加</el-button>
          <div class="waypoint-list"><div v-for="(point, index) in draft.waypoints" :key="index" class="point-row" :class="{ active: pointIndex === index }" @click="pointIndex = index">
            <span>{{ index + 1 }}. {{ point.name }}</span><button :disabled="index === 0" @click.stop="movePoint(index, -1)">↑</button><button :disabled="index === draft.waypoints.length - 1" @click.stop="movePoint(index, 1)">↓</button><button @click.stop="removePoint(index)">×</button>
          </div></div>
          <el-form v-if="activePoint" label-position="top" size="small" :disabled="busy">
            <el-form-item label="航点名称"><el-input v-model="activePoint.name" maxlength="100" /></el-form-item>
            <el-form-item label="经度"><el-input-number v-model="activePoint.longitude" :min="-180" :max="180" :precision="7" :controls="false" @change="projectMarkers" /></el-form-item>
            <el-form-item label="纬度"><el-input-number v-model="activePoint.latitude" :min="-90" :max="90" :precision="7" :controls="false" @change="projectMarkers" /></el-form-item>
            <el-form-item label="绝对椭球高（m）"><el-input-number v-model="activePoint.height" :min="-1000" :max="10000" :precision="2" :controls="false" /></el-form-item>
            <el-form-item label="机头偏航（°，0为北）"><el-input-number v-model="activePoint.heading" :min="-180" :max="360" :controls="false" /></el-form-item>
            <el-form-item label="云台俯仰（°）"><el-input-number v-model="activePoint.pitch" :min="-90" :max="30" :controls="false" /></el-form-item>
            <el-form-item label="焦距（mm，非变焦倍数）"><el-input-number v-model="activePoint.focal_length" :min="0.1" :max="2000" :controls="false" /></el-form-item>
            <el-form-item label="拍摄动作"><el-select v-model="activePoint.action"><el-option label="拍照" value="take_photo" /><el-option label="无动作" value="none" /></el-select></el-form-item>
            <el-form-item label="拍摄模式"><el-select v-model="activePoint.image_format"><el-option label="可见光" value="visible" /><el-option label="红外" value="ir" /><el-option label="双光" value="both" /></el-select></el-form-item>
            <el-form-item label="航点启用"><el-switch v-model="activePoint.enabled" /></el-form-item>
          </el-form>
          <p class="hint">地图只采集经纬度，不提供高度；请录入已确认的绝对椭球高。此处连线为设备拍摄顺序示意，不是已验证安全的飞行航线。</p>
        </template>
        <p v-else class="hint">选择已有设备，或新增设备并在图上落点。</p>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrthophotoListApi, getOrthophotoByUrlApi, getAllSolarPanelApi } from '/@/api/turbine/turbineMgt'
import { convertAccessoryCoordinates, deleteSolarAccessory, listSolarAccessories, saveSolarAccessory } from '/@/api/solar-accessory'
import type { SolarAccessory } from '/@/api/solar-accessory'
import { deviceTypes, moveWaypoint, reorderWaypoints, validateAccessory } from './accessoryModel'

type Pixel = { row: number; col: number }
const photos = ref<any[]>([])
const photoId = ref('')
const devices = ref<SolarAccessory[]>([])
const draft = ref<SolarAccessory | null>(null)
const snapshot = ref('null')
const filter = ref('')
const pointIndex = ref(-1)
const imageUrl = ref('')
const imageSize = ref({ width: 1000, height: 700 })
const regions = ref<string[]>([])
const zoom = ref(1)
const mapSvg = ref<SVGSVGElement>()
const mode = ref('')
const loading = ref(false)
const locating = ref(false)
const saving = ref(false)
const busy = computed(() => loading.value || locating.value || saving.value)
const dirty = computed(() => JSON.stringify(draft.value) !== snapshot.value)
const activePoint = computed(() => draft.value?.waypoints[pointIndex.value])
const filteredDevices = computed(() => devices.value.filter(device => !filter.value || device.device_type === filter.value))
const mapDevices = computed(() => {
  const result = devices.value.filter(device => device.id !== draft.value?.id)
  return draft.value ? [...result, draft.value] : result
})
const devicePixels = ref<(Pixel | null)[]>([])
const waypointPixels = ref<(Pixel | null)[]>([])
const selectedDevicePixel = computed(() => draft.value ? devicePixels.value[mapDevices.value.length - 1] : null)
const waypointLine = computed(() => waypointPixels.value.filter((point): point is Pixel => point !== null).map(point => `${point.col},${point.row}`).join(' '))
const markerSize = computed(() => Math.max(imageSize.value.width, imageSize.value.height) / 110)
let generation = 0
let projection = 0
let disposed = false

function typeName (value: string) { return deviceTypes.find(type => type.value === value)?.label || value }
function errorMessage (error: any) { ElMessage.error(error?.message || '操作失败') }
async function discard () {
  if (!dirty.value) return true
  try { await ElMessageBox.confirm('当前设备修改尚未保存，是否放弃？', '未保存修改', { type: 'warning' }); return true } catch { return false }
}
function setDraft (device: SolarAccessory | null) {
  draft.value = device ? JSON.parse(JSON.stringify(device)) : null
  snapshot.value = JSON.stringify(draft.value)
  pointIndex.value = -1
  mode.value = ''
}
async function selectDevice (device: SolarAccessory) {
  if (busy.value || (device.id && device.id === draft.value?.id)) return
  if (!await discard()) return
  setDraft(device)
  await projectMarkers()
}
function selectMapDevice (device: SolarAccessory) {
  if (device === draft.value) { pointIndex.value = -1; return }
  selectDevice(device)
}
async function newDevice () {
  if (!await discard()) return
  setDraft({ orthophoto_id: photoId.value, name: '', device_type: 'inverter', longitude: null, latitude: null, height: null, enabled: true, waypoints: [] })
  mode.value = 'device'
  await projectMarkers()
}
async function switchPhoto (id: string) {
  if (id === photoId.value || !await discard()) return
  const token = ++generation
  ++projection
  photoId.value = id
  setDraft(null)
  devices.value = []; regions.value = []; devicePixels.value = []; waypointPixels.value = []
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  imageUrl.value = ''; zoom.value = 1
  loading.value = true
  try {
    const photo = photos.value.find(item => item.id === id)
    const [blobData, loadedDevices, areaResponse]: any = await Promise.all([
      getOrthophotoByUrlApi(photo.path), listSolarAccessories(id),
      getAllSolarPanelApi({ orthophoto_id: id, pageSize: 10000, pageNo: 1, solar_panel_area_name: '', id: '' })
    ])
    if (token !== generation || disposed) return
    if (areaResponse.code !== 0) throw new Error(areaResponse.message || '巡视区域加载失败')
    const url = URL.createObjectURL(new Blob([blobData], { type: 'image/png' }))
    const image = new Image()
    try {
      await new Promise<void>((resolve, reject) => { image.onload = () => resolve(); image.onerror = () => reject(new Error('正射图无法显示')); image.src = url })
    } catch (error) { URL.revokeObjectURL(url); throw error }
    if (token !== generation || disposed) { URL.revokeObjectURL(url); return }
    imageSize.value = { width: image.naturalWidth, height: image.naturalHeight }
    imageUrl.value = url
    devices.value = loadedDevices
    // 兼容两种序列化命名：本地新包 camelCase / 服务器旧包 snake_case
    const pick = (obj: any, camel: string, snake: string) => {
      const v = obj[camel] !== undefined ? obj[camel] : obj[snake]
      return v === undefined || v === null ? NaN : Number(v)
    }
    regions.value = (areaResponse.data?.list || []).map((area: any) => [1, 2, 3, 4].map(index => {
      const col = pick(area, `corner${index}Col`, `corner${index}_col`)
      const row = pick(area, `corner${index}Row`, `corner${index}_row`)
      return Number.isFinite(col) && Number.isFinite(row) ? `${col},${row}` : ''
    }).filter(Boolean)).filter((points: string[]) => points.length === 4).map((points: string[]) => points.join(' '))
    await projectMarkers()
  } catch (error) { if (token === generation && !disposed) errorMessage(error) } finally { if (token === generation) loading.value = false }
}
async function projectMarkers () {
  const token = ++projection
  const photo = photoId.value
  const all: any[] = [...mapDevices.value, ...(draft.value?.waypoints || [])]
  const valid = all.map((point, index) => ({ point, index })).filter(({ point }) => typeof point.longitude === 'number' && typeof point.latitude === 'number')
  devicePixels.value = []; waypointPixels.value = []
  if (!photo || !valid.length) return
  try {
    const response = await convertAccessoryCoordinates(photo, 'geo_to_pixel', valid.map(({ point }) => ({ lon: point.longitude, lat: point.latitude })))
    if (token !== projection || photo !== photoId.value || disposed) return
    const pixels: (Pixel | null)[] = all.map(() => null)
    valid.forEach(({ index }, i) => { pixels[index] = response.points[i] })
    devicePixels.value = pixels.slice(0, mapDevices.value.length)
    // Null entries retain the waypoint index when a manually entered coordinate is incomplete.
    waypointPixels.value = pixels.slice(mapDevices.value.length)
  } catch (error) { if (token === projection && !disposed) errorMessage(error) }
}
async function mapClick (event: MouseEvent) {
  if (!mode.value || !draft.value || busy.value || !mapSvg.value) return
  const ctm = mapSvg.value.getScreenCTM()
  if (!ctm) return
  const point = new DOMPoint(event.clientX, event.clientY).matrixTransform(ctm.inverse())
  const col = Math.floor(point.x); const row = Math.floor(point.y)
  if (col < 0 || row < 0 || col >= imageSize.value.width || row >= imageSize.value.height) return
  const photo = photoId.value; const current = draft.value; const action = mode.value
  locating.value = true
  try {
    const response = await convertAccessoryCoordinates(photo, 'pixel_to_geo', [{ col, row }])
    if (photo !== photoId.value || current !== draft.value || disposed) return
    const geo = response.points[0]
    if (action === 'device') { current.longitude = geo.lon; current.latitude = geo.lat; mode.value = '' } else addWaypoint(geo)
    await projectMarkers()
  } catch (error) { if (!disposed) errorMessage(error) } finally { locating.value = false }
}
function addWaypoint (geo?: { lon: number; lat: number }) {
  if (!draft.value) return
  if (draft.value.waypoints.length >= 200) { ElMessage.warning('单台设备最多200个航点'); return }
  draft.value.waypoints.push({ name: `航点${draft.value.waypoints.length + 1}`, sort_order: draft.value.waypoints.length + 1, longitude: geo?.lon ?? null, latitude: geo?.lat ?? null, height: null, heading: 0, pitch: -30, focal_length: 24, action: 'take_photo', image_format: 'visible', enabled: true })
  pointIndex.value = draft.value.waypoints.length - 1
  projectMarkers()
}
function movePoint (index: number, direction: number) {
  if (!draft.value || busy.value) return
  moveWaypoint(draft.value.waypoints, index, direction)
  pointIndex.value = index + direction
  projectMarkers()
}
function removePoint (index: number) {
  if (!draft.value || busy.value) return
  draft.value.waypoints.splice(index, 1); reorderWaypoints(draft.value.waypoints)
  pointIndex.value = Math.min(index, draft.value.waypoints.length - 1)
  projectMarkers()
}
async function save () {
  if (!draft.value || busy.value) return
  const message = validateAccessory(draft.value)
  if (message) { ElMessage.warning(message); return }
  saving.value = true
  try {
    reorderWaypoints(draft.value.waypoints)
    const saved = await saveSolarAccessory(JSON.parse(JSON.stringify(draft.value)))
    devices.value = [...devices.value.filter(device => device.id !== saved.id), saved]
    setDraft(saved); await projectMarkers(); ElMessage.success('设备及航点已保存')
  } catch (error) { errorMessage(error) } finally { saving.value = false }
}
async function removeDevice () {
  if (!draft.value || busy.value) return
  try { await ElMessageBox.confirm('删除设备及其全部预置航点？', '确认删除', { type: 'warning' }) } catch { return }
  saving.value = true
  try {
    const id = draft.value.id
    if (id) await deleteSolarAccessory(id, photoId.value)
    devices.value = devices.value.filter(device => device.id !== id)
    setDraft(null); await projectMarkers(); ElMessage.success('已删除')
  } catch (error) { errorMessage(error) } finally { saving.value = false }
}
function beforeUnload (event: BeforeUnloadEvent) { if (dirty.value) { event.preventDefault(); event.returnValue = '' } }
onBeforeRouteLeave(async () => !busy.value && await discard())
onMounted(async () => {
  window.addEventListener('beforeunload', beforeUnload)
  try {
    const response = await getOrthophotoListApi({ pageSize: 10000, pageNo: 1 })
    if (response.code !== 0) throw new Error(response.message || '正射图列表加载失败')
    if (disposed) return
    photos.value = response.data?.list || []
    if (photos.value.length) await switchPhoto(photos.value[0].id)
  } catch (error) { if (!disposed) errorMessage(error) }
})
onUnmounted(() => { disposed = true; ++generation; ++projection; window.removeEventListener('beforeunload', beforeUnload); if (imageUrl.value) URL.revokeObjectURL(imageUrl.value) })
</script>

<style scoped>
.accessory-page { height: 100%; min-height: 650px; display: flex; flex-direction: column; background: #111e30; color: #e1e8f0; }
header { display: flex; align-items: center; gap: 16px; padding: 14px 20px; border-bottom: 1px solid #33445b; }
header span { flex: 1; color: #96a9bf; }
.workspace { display: flex; flex: 1; min-height: 0; }
aside { padding: 16px; overflow-y: auto; flex-shrink: 0; }
.device-panel { width: 250px; border-right: 1px solid #33445b; }
.property-panel { width: 310px; border-left: 1px solid #33445b; }
h3 { font-size: 15px; color: #d9e7f7; margin: 16px 0 12px; }
.hint, small { color: #91a6bf; font-size: 12px; line-height: 1.7; }
.device-row { display: flex; flex-direction: column; text-align: left; width: 100%; border: 1px solid #34455f; background: #182a40; color: #e1e8f0; padding: 10px; margin: 8px 0; cursor: pointer; }
.active { border-color: #40a9ff !important; background: #1e3b5d !important; }
.map-panel { display: flex; flex-direction: column; flex: 1; min-width: 0; background: #0b1421; }
.map-toolbar { padding: 10px; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; font-size: 12px; }
.map-scroll { flex: 1; overflow: auto; position: relative; }
.ortho-map { display: block; height: auto; cursor: crosshair; }
.empty-map { padding: 100px 20px; text-align: center; color: #91a6bf; }
.region { fill: #25c29b22; stroke: #25c29b; stroke-width: 2; vector-effect: non-scaling-stroke; pointer-events: none; }
.device-marker { fill: #ffad32; stroke: #fff; stroke-width: 2; vector-effect: non-scaling-stroke; cursor: pointer; }
.device-marker.disabled { fill: #7e8793; }
.waypoint-marker { fill: #187bc1; stroke: #fff; stroke-width: 1; vector-effect: non-scaling-stroke; cursor: pointer; }
.waypoint-marker.selected { fill: #e35749; }
text { fill: white; stroke: #102035; stroke-width: 0.6; paint-order: stroke; pointer-events: none; }
.route-line, .heading-line { fill: none; stroke: #53beff; stroke-width: 2; vector-effect: non-scaling-stroke; pointer-events: none; }
.sight-line { stroke: #ffffff66; stroke-width: 1; stroke-dasharray: 4 4; vector-effect: non-scaling-stroke; pointer-events: none; }
.point-row { display: flex; gap: 5px; align-items: center; padding: 6px; border: 1px solid #34455f; margin: 5px 0; cursor: pointer; }
.point-row span { flex: 1; }
.point-row button { background: transparent; color: #c2d5eb; border: 1px solid #52667f; cursor: pointer; }
.point-row button:disabled { opacity: 0.3; }
.waypoint-list { max-height: 210px; overflow: auto; margin: 12px 0; }
:deep(.el-input-number), :deep(.el-select) { width: 100%; }
:deep(.el-form-item__label) { color: #b7c8dc; }
</style>
