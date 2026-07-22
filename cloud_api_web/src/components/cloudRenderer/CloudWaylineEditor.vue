<template>
  <div class="wayline-editor">
    <aside class="editor-panel editor-panel--left">
      <div class="panel-title">
        <div>
          <span class="panel-title__eyebrow">CLOUD WAYLINE</span>
          <h2>三维航线规划</h2>
        </div>
        <el-button link type="primary" @click="router.back()">返回</el-button>
      </div>

      <div class="route-name-block">
        <label>航线名称</label>
        <el-input v-model="routeNameInput" placeholder="请输入航线名称" @change="updateRouteName" />
      </div>

      <div class="section-heading">
        <span>航点列表</span>
        <strong>{{ waylineState.waypoints.length }}</strong>
      </div>
      <div class="waypoint-list">
        <button
          v-for="(point, index) in waylineState.waypoints"
          :key="`${point.point_name}-${index}`"
          type="button"
          class="waypoint-card"
          :class="{ active: waylineState.selectedIndex === index }"
          @click="selectWaypoint(index)"
        >
          <span class="waypoint-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
          <span class="waypoint-card__body">
            <b>{{ point.point_name || `WP_${index + 1}` }}</b>
            <small>{{ formatCoordinate(point.longitude) }}, {{ formatCoordinate(point.latitude) }}</small>
            <small>高度 {{ formatNumber(point.height, 1) }} m · {{ captureModeLabel(point.capture_mode) }}</small>
          </span>
          <el-button link type="danger" @click.stop="removeWaypoint(index)">删除</el-button>
        </button>
        <div v-if="!waylineState.waypoints.length" class="empty-list">
          <span>双击中间三维画面</span>
          <small>在 3DGS 模型表面添加航点</small>
        </div>
      </div>

      <div class="left-actions">
        <el-button :disabled="!waylineState.waypoints.length" @click="clearWaypoints">清空航点</el-button>
        <el-button type="primary" :loading="buildingKmz" :disabled="!waylineState.waypoints.length" @click="buildKmz">
          生成 KMZ 航线
        </el-button>
      </div>
    </aside>

    <main class="renderer-stage">
      <OutdoorRenderer
        :client="waylineClient"
        renderer="wayline"
        close-on-unmount
        :show-clear-path="false"
        video-fit="contain"
        @status-change="handleStatusChange"
      />
      <div class="stage-toolbar">
        <span class="status-dot" :class="{ online: !statusText }"></span>
        <span>{{ statusText || '云渲染航线会话已连接' }}</span>
      </div>
      <div class="stage-hint">左键拖动旋转 · 右键拖动平移 · 滚轮缩放 · 双击模型添加航点</div>
    </main>

    <aside class="editor-panel editor-panel--right">
      <template v-if="selectedWaypoint">
        <div class="panel-title panel-title--compact">
          <div>
            <span class="panel-title__eyebrow">WAYPOINT {{ waylineState.selectedIndex + 1 }}</span>
            <h2>{{ selectedWaypoint.point_name }}</h2>
          </div>
          <span class="capture-badge">{{ captureModeLabel(selectedWaypoint.capture_mode) }}</span>
        </div>

        <div class="form-scroll" :class="{ 'form-scroll--disabled': !!statusText }">
          <section class="edit-section">
            <h3>位置</h3>
            <div class="form-grid">
              <label>经度<el-input-number v-model="selectedWaypoint.longitude" :precision="7" :step="0.000001" controls-position="right" @change="updatePosition" /></label>
              <label>纬度<el-input-number v-model="selectedWaypoint.latitude" :precision="7" :step="0.000001" controls-position="right" @change="updatePosition" /></label>
              <label>绝对高度 (m)<el-input-number v-model="selectedWaypoint.height" :precision="2" :step="0.5" controls-position="right" @change="updatePosition" /></label>
              <label>速度 (m/s)<el-input-number v-model="selectedWaypoint.speed" :min="0.1" :max="30" :step="0.5" controls-position="right" @change="updatePosition" /></label>
            </div>
            <div class="nudge-row">
              <span>微调步长</span>
              <el-input-number v-model="nudgeMeters" :min="0.1" :max="100" :step="0.5" size="small" />
              <span>米</span>
            </div>
            <div class="control-caption">机体方向微调</div>
            <div class="direction-pad">
              <el-button class="north" @click="nudgeBody(nudgeMeters, 0)">前进</el-button>
              <el-button class="west" @click="nudgeBody(0, -nudgeMeters)">左移</el-button>
              <div class="direction-pad__center">机头</div>
              <el-button class="east" @click="nudgeBody(0, nudgeMeters)">右移</el-button>
              <el-button class="south" @click="nudgeBody(-nudgeMeters, 0)">后退</el-button>
            </div>
            <div class="control-caption">地理方向与高度微调</div>
            <div class="geo-buttons">
              <el-button @click="nudgeGeo(0, nudgeMeters)">向北</el-button>
              <el-button @click="nudgeGeo(nudgeMeters, 0)">向东</el-button>
              <el-button @click="nudgeGeo(0, -nudgeMeters)">向南</el-button>
              <el-button @click="nudgeGeo(-nudgeMeters, 0)">向西</el-button>
              <el-button @click="nudgeHeight(nudgeMeters)">升高</el-button>
              <el-button @click="nudgeHeight(-nudgeMeters)">降低</el-button>
            </div>
          </section>

          <section class="edit-section">
            <h3>姿态与拍摄</h3>
            <div class="form-grid">
              <label>偏航角 / 航向角 (°)<el-input-number v-model="selectedWaypoint.camera_params.heading" :min="0" :max="359" :step="1" controls-position="right" @change="updateCamera" /></label>
              <label>俯仰角 (°)<el-input-number v-model="selectedWaypoint.camera_params.pitch" :min="-90" :max="0" :step="1" controls-position="right" @change="updateCamera" /></label>
              <label>滚转角 (°)<el-input-number v-model="selectedWaypoint.camera_params.roll" :min="-180" :max="180" :step="1" controls-position="right" @change="updateCamera" /></label>
              <label>35mm 焦距 (mm)<el-input-number v-model="selectedWaypoint.camera_params.focalLength" :min="1" :max="500" :step="1" controls-position="right" @change="updateCamera" /></label>
            </div>
            <div class="nudge-row attitude-step">
              <span>姿态微调步长</span>
              <el-input-number v-model="angleStep" :min="0.1" :max="30" :step="0.5" :precision="1" size="small" />
              <span>度</span>
            </div>
            <div class="attitude-nudges">
              <div class="attitude-nudge">
                <span>偏航角</span>
                <el-button @click="nudgeCamera('heading', angleStep)">向左 +</el-button>
                <el-button @click="nudgeCamera('heading', -angleStep)">向右 -</el-button>
              </div>
              <div class="attitude-nudge">
                <span>俯仰角</span>
                <el-button @click="nudgeCamera('pitch', angleStep)">上仰 +</el-button>
                <el-button @click="nudgeCamera('pitch', -angleStep)">下俯 -</el-button>
              </div>
            </div>
            <label class="select-label">拍摄镜头
              <el-select v-model="selectedWaypoint.capture_mode" @change="updateCaptureMode">
                <el-option label="不拍照（过渡点）" value="none" />
                <el-option label="可见光" value="visable" />
                <el-option label="红外" value="ir" />
                <el-option label="可见光 + 红外" value="visable,ir" />
              </el-select>
            </label>
          </section>
        </div>
      </template>
      <div v-else class="empty-editor">
        <span class="empty-editor__mark">+</span>
        <h3>尚未选择航点</h3>
        <p>双击三维场景添加航点，或从左侧列表选择已有航点。</p>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import OutdoorRenderer from './OutdoorRenderer.vue'
import { CloudRendererClient } from './cloudRendererClient'
import { getCloudRendererConfig } from './cloudRendererConfig'
import { importSubKmzFile } from '/@/api/wayline'
import { ELocalStorageKey } from '/@/types'

type CaptureMode = 'none' | 'visable' | 'ir' | 'visable,ir'
interface WaypointCamera { heading: number; pitch: number; roll: number; focalLength: number }
interface Waypoint {
  point_name: string
  longitude: number
  latitude: number
  height: number
  capture_mode: CaptureMode
  speed: number
  camera_params: WaypointCamera
}
interface WaylineState { routeName: string; selectedIndex: number; waypoints: Waypoint[] }

const router = useRouter()
const waylineClient = new CloudRendererClient()
const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
const waylineState = reactive<WaylineState>({ routeName: `wayline-${formatDate(new Date())}`, selectedIndex: -1, waypoints: [] })
const routeNameInput = ref(waylineState.routeName)
const statusText = ref('云渲染航线会话连接中...')
const nudgeMeters = ref(1)
const angleStep = ref(1)
const buildingKmz = ref(false)
let kmzController: AbortController | null = null
const selectedWaypoint = computed(() => waylineState.waypoints[waylineState.selectedIndex] || null)

const stopSignalListener = waylineClient.onSignalMessage(message => {
  if (message.type !== 'wayline-state' || !message.payload) return
  const payload = normalizeWaylineState(message.payload)
  if (!payload) {
    ElMessage.warning('云渲染服务返回了无效的航点状态')
    return
  }
  waylineState.routeName = payload.routeName || waylineState.routeName
  waylineState.selectedIndex = Number.isInteger(payload.selectedIndex) ? payload.selectedIndex : -1
  waylineState.waypoints = Array.isArray(payload.waypoints) ? payload.waypoints : []
  routeNameInput.value = waylineState.routeName
})

function handleStatusChange (status: string) { statusText.value = status }
function selectWaypoint (index: number) { waylineClient.sendWaylineCommand('select', { index }) }
function removeWaypoint (index: number) { waylineClient.sendWaylineCommand('remove', { index }) }
function updateRouteName () {
  const value = routeNameInput.value.trim()
  if (!value) return ElMessage.warning('航线名称不能为空')
  waylineClient.sendWaylineCommand('set-route-name', { value })
}
function updateSelected (patch: Record<string, unknown>) {
  if (waylineState.selectedIndex >= 0) waylineClient.sendWaylineCommand('update', { index: waylineState.selectedIndex, patch })
}
function updatePosition () {
  const point = selectedWaypoint.value
  if (point) updateSelected({ longitude: point.longitude, latitude: point.latitude, height: point.height, speed: point.speed })
}
function updateCamera () {
  if (selectedWaypoint.value) updateSelected({ camera_params: { ...selectedWaypoint.value.camera_params } })
}
function updateCaptureMode () {
  if (selectedWaypoint.value) updateSelected({ capture_mode: selectedWaypoint.value.capture_mode })
}
function nudgeGeo (east: number, north: number) {
  const point = selectedWaypoint.value
  if (!point) return
  const latitudeRadians = point.latitude * Math.PI / 180
  point.latitude += north / 111320
  point.longitude += east / Math.max(111320 * Math.cos(latitudeRadians), 1)
  updatePosition()
}
function nudgeHeight (delta: number) {
  if (!selectedWaypoint.value) return
  selectedWaypoint.value.height += delta
  updatePosition()
}
function nudgeBody (forward: number, right: number) {
  const point = selectedWaypoint.value
  if (!point) return
  const heading = point.camera_params.heading * Math.PI / 180
  nudgeGeo(forward * Math.sin(heading) + right * Math.cos(heading), forward * Math.cos(heading) - right * Math.sin(heading))
}
function nudgeCamera (field: 'heading' | 'pitch', delta: number) {
  const point = selectedWaypoint.value
  if (!point) return
  if (field === 'heading') {
    point.camera_params.heading = ((point.camera_params.heading + delta) % 360 + 360) % 360
  } else {
    point.camera_params.pitch = Math.min(0, Math.max(-90, point.camera_params.pitch + delta))
  }
  updateCamera()
}
async function clearWaypoints () {
  try {
    await ElMessageBox.confirm('确定清空当前航线的全部航点吗？', '清空航点', { type: 'warning' })
    waylineClient.sendWaylineCommand('clear')
  } catch (error) {
    // User cancelled the confirmation dialog.
  }
}
async function buildKmz () {
  const error = validateWaypoints(waylineState.waypoints)
  if (error) return ElMessage.error(error)
  const routeName = routeNameInput.value.trim()
  if (!routeName) return ElMessage.error('请输入航线名称')
  buildingKmz.value = true
  kmzController?.abort()
  kmzController = new AbortController()
  try {
    const config = getCloudRendererConfig()
    const response = await fetch(new URL('/api/wayline/build-kmz', config.baseURL).toString(), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/vnd.google-earth.kmz' },
      body: JSON.stringify(buildWaylineRequest(routeName, waylineState.waypoints)),
      signal: kmzController.signal
    })
    if (!response.ok) throw new Error(await response.text() || `服务返回 ${response.status}`)
    const contentType = response.headers.get('content-type') || ''
    if (contentType.includes('application/json')) {
      const payload = await response.json().catch(() => null)
      throw new Error(payload?.message || payload?.error || '云渲染服务未返回 KMZ 文件')
    }
    const blob = await response.blob()
    if (!blob.size) throw new Error('云渲染服务返回了空的 KMZ 文件')
    const fileName = `${sanitizeFileName(routeName)}.kmz`
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = fileName
    anchor.click()
    window.setTimeout(() => URL.revokeObjectURL(url), 1000)

    if (!workspaceId) throw new Error('未获取到工作空间，无法导入航线')
    const fileData = new FormData()
    fileData.append('file', new File([blob], fileName, { type: 'application/vnd.google-earth.kmz' }))
    const importRes = await importSubKmzFile(workspaceId, fileData)
    if (importRes.code !== 0) throw new Error(importRes.message || '航线导入失败')
    ElMessage.success('KMZ 航线已生成、下载并导入成功')
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return
    ElMessage.error(error instanceof Error ? error.message : 'KMZ 航线生成失败')
  } finally {
    buildingKmz.value = false
    kmzController = null
  }
}
function normalizeWaylineState (value: unknown): WaylineState | null {
  if (!value || typeof value !== 'object') return null
  const payload = value as Partial<WaylineState>
  if (!Array.isArray(payload.waypoints)) return null
  const waypoints: Waypoint[] = []
  for (let index = 0; index < payload.waypoints.length; index += 1) {
    const point = payload.waypoints[index] as Partial<Waypoint>
    const camera = point?.camera_params as Partial<WaypointCamera> | undefined
    const captureMode = String(point?.capture_mode || 'visable') as CaptureMode
    if (!point || !camera || !['none', 'visable', 'ir', 'visable,ir'].includes(captureMode)) return null
    const normalized: Waypoint = {
      point_name: String(point.point_name || `WP_${String(index + 1).padStart(3, '0')}`),
      longitude: Number(point.longitude),
      latitude: Number(point.latitude),
      height: Number(point.height),
      capture_mode: captureMode,
      speed: Number(point.speed ?? 5),
      camera_params: {
        heading: Number(camera.heading ?? 0),
        pitch: Number(camera.pitch ?? -45),
        roll: Number(camera.roll ?? 0),
        focalLength: Number(camera.focalLength ?? 75)
      }
    }
    if (![normalized.longitude, normalized.latitude, normalized.height, normalized.speed, normalized.camera_params.heading, normalized.camera_params.pitch, normalized.camera_params.roll, normalized.camera_params.focalLength].every(Number.isFinite)) return null
    waypoints.push(normalized)
  }
  const selectedIndex = Number(payload.selectedIndex)
  return {
    routeName: String(payload.routeName || waylineState.routeName),
    selectedIndex: Number.isInteger(selectedIndex) && selectedIndex >= -1 && selectedIndex < waypoints.length ? selectedIndex : -1,
    waypoints
  }
}
function buildWaylineRequest (routeName: string, points: Waypoint[]) {
  return {
    routeName,
    templateType: 'waypoint',
    droneType: 100,
    subDroneType: 1,
    payloadType: 99,
    payloadPosition: 0,
    finishAction: 'goHome',
    exitOnRcLostAction: 'goBack',
    globalHeight: Number(points[0].height || 80),
    takeOffSecurityHeight: 20,
    globalRTHHeight: 100,
    globalTransitionalSpeed: 10,
    autoFlightSpeed: 5,
    imageFormat: 'visable',
    gimbalPitchMode: 'usePointSetting',
    waypointHeadingReq: { waypointHeadingMode: 'fixed', waypointHeadingAngle: 0 },
    waypointTurnReq: { waypointTurnMode: 'toPointAndStopWithDiscontinuityCurvature', useStraightLine: 1 },
    startActionList: [],
    routePointList: points.map((point, index) => {
      const heading = normalizeHeading(point.camera_params.heading)
      const actions: Record<string, unknown>[] = [{ actionIndex: 0, aircraftHeading: heading, aircraftPathMode: 'counterClockwise' }]
      if (point.capture_mode !== 'none') {
        actions.push({
          actionIndex: 1,
          takePhotoType: 2,
          useGlobalImageFormat: 0,
          imageFormat: point.capture_mode,
          orientedPhotoMode: 'normalPhoto',
          focalLength: Number(point.camera_params.focalLength || 75),
          gimbalYawRotateAngle: heading,
          gimbalPitchRotateAngle: Number(point.camera_params.pitch ?? -45),
          imageWidth: 960,
          imageHeight: 720,
          orientedCameraApertue: 440,
          orientedCameraLuminance: 3800,
          orientedCameraShutterTime: 0.003,
          orientedCameraISO: 100,
          AFPos: 159,
          focusX: 480,
          focusY: 360,
          focusRegionWidth: 480,
          focusRegionHeight: 360,
          orientedFileSuffix: point.point_name || `WP_${index + 1}`
        })
      }
      return {
        routePointIndex: index,
        longitude: Number(point.longitude),
        latitude: Number(point.latitude),
        height: Number(point.height),
        speed: Number(point.speed || 5),
        gimbalPitchAngle: 0,
        waypointHeadingReq: { waypointHeadingMode: 'fixed', waypointHeadingAngle: heading },
        waypointTurnReq: { waypointTurnMode: 'toPointAndStopWithDiscontinuityCurvature', useStraightLine: 1 },
        actions
      }
    })
  }
}
function validateWaypoints (points: Waypoint[]) {
  if (!points.length) return '请至少添加一个航点'
  if (points.length > 2000) return '航点数量不能超过 2000'
  for (let index = 0; index < points.length; index += 1) {
    const point = points[index]
    if (!Number.isFinite(Number(point.longitude)) || point.longitude < -180 || point.longitude > 180) return `航点 ${index + 1} 经度无效`
    if (!Number.isFinite(Number(point.latitude)) || point.latitude < -90 || point.latitude > 90) return `航点 ${index + 1} 纬度无效`
    if (!Number.isFinite(Number(point.height))) return `航点 ${index + 1} 高度无效`
    if (!Number.isFinite(Number(point.speed)) || point.speed <= 0) return `航点 ${index + 1} 速度无效`
    if (point.capture_mode !== 'none' && (!Number.isFinite(Number(point.camera_params.focalLength)) || point.camera_params.focalLength <= 0)) return `航点 ${index + 1} 焦距无效`
  }
  return ''
}
function normalizeHeading (value: number) { return Math.min(((Number(value) % 360) + 360) % 360, 359) }
function captureModeLabel (mode: CaptureMode) { return ({ none: '过渡点', visable: '可见光', ir: '红外', 'visable,ir': '可见光 + 红外' } as Record<CaptureMode, string>)[mode] || '可见光' }
function sanitizeFileName (name: string) { return name.replace(/[\\/:*?"<>|\s_]+/g, '-') || 'wayline' }
function formatCoordinate (value: number) { return Number(value).toFixed(6) }
function formatNumber (value: number, precision: number) { return Number(value).toFixed(precision) }
function formatDate (date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}-${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
}

onBeforeUnmount(() => {
  kmzController?.abort()
  stopSignalListener()
})
</script>

<style scoped lang="scss">
.wayline-editor {
  --panel-bg: rgba(5, 24, 51, .96);
  --line: rgba(58, 165, 213, .42);
  --cyan: #48d6ff;
  width: 100%; height: calc(100vh - 100px); min-height: 620px; padding: 12px; box-sizing: border-box;
  display: grid; grid-template-columns: 330px minmax(420px, 1fr) 360px; gap: 12px; color: #dff6ff;
  background: radial-gradient(circle at 50% 15%, rgba(19, 96, 132, .2), transparent 38%); overflow: hidden;
}
.editor-panel { min-height: 0; display: flex; flex-direction: column; background: var(--panel-bg); border: 1px solid var(--line); box-shadow: inset 0 0 24px rgba(33, 137, 196, .12); }
.editor-panel--left { padding: 18px; } .editor-panel--right { padding: 18px 16px; }
.panel-title { display: flex; align-items: flex-start; justify-content: space-between; padding-bottom: 15px; border-bottom: 1px solid var(--line); }
.panel-title--compact { align-items: center; } .panel-title h2 { margin: 3px 0 0; color: #fff; font-size: 20px; }
.panel-title__eyebrow { color: #59c9eb; font-size: 10px; letter-spacing: 2px; }
.route-name-block { padding: 16px 0; } .route-name-block label, .select-label { display: grid; gap: 7px; color: #91b7ca; font-size: 13px; }
.section-heading { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; color: #aacddd; }
.section-heading strong { min-width: 28px; padding: 2px 8px; text-align: center; color: var(--cyan); background: rgba(72, 214, 255, .1); border: 1px solid rgba(72, 214, 255, .28); }
.waypoint-list { flex: 1; min-height: 0; display: grid; align-content: start; gap: 8px; overflow-y: auto; }
.waypoint-card { width: 100%; padding: 10px; display: flex; align-items: center; gap: 10px; color: #d9eff7; text-align: left; cursor: pointer; border: 1px solid rgba(70, 137, 169, .32); background: rgba(13, 49, 78, .55); }
.waypoint-card:hover, .waypoint-card.active { border-color: var(--cyan); background: linear-gradient(90deg, rgba(21, 111, 151, .56), rgba(13, 49, 78, .76)); }
.waypoint-card__index { width: 32px; height: 32px; display: grid; place-items: center; flex-shrink: 0; color: #071d2c; font-weight: 700; background: #4fd8ff; clip-path: polygon(50% 0, 100% 25%, 100% 75%, 50% 100%, 0 75%, 0 25%); }
.waypoint-card__body { min-width: 0; flex: 1; display: grid; gap: 2px; } .waypoint-card__body b, .waypoint-card__body small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.waypoint-card__body small { color: #83a9bd; font-size: 11px; } .empty-list, .empty-editor { display: grid; place-content: center; gap: 8px; text-align: center; color: #7094a6; }
.empty-list { min-height: 180px; border: 1px dashed rgba(79, 165, 202, .28); } .left-actions { padding-top: 14px; display: grid; grid-template-columns: 1fr 1.35fr; gap: 8px; }
.renderer-stage { position: relative; min-width: 0; min-height: 0; overflow: hidden; border: 1px solid rgba(70, 192, 231, .62); background: #030b12; box-shadow: 0 0 28px rgba(10, 104, 151, .2); }
.stage-toolbar, .stage-hint { position: absolute; z-index: 4; color: #ccefff; background: rgba(3, 17, 29, .8); border: 1px solid rgba(80, 189, 226, .34); backdrop-filter: blur(5px); }
.stage-toolbar { top: 12px; left: 12px; padding: 7px 11px; display: flex; align-items: center; gap: 8px; font-size: 12px; }
.stage-hint { left: 50%; bottom: 14px; transform: translateX(-50%); padding: 7px 13px; font-size: 12px; white-space: nowrap; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: #e99a43; box-shadow: 0 0 8px #e99a43; } .status-dot.online { background: #41e7a1; box-shadow: 0 0 8px #41e7a1; }
.capture-badge { padding: 5px 9px; color: #70ddff; font-size: 11px; border: 1px solid rgba(72, 214, 255, .4); background: rgba(72, 214, 255, .08); }
.form-scroll { flex: 1; min-height: 0; overflow-y: auto; padding-right: 3px; } .edit-section { padding: 15px 0; border-bottom: 1px solid rgba(58, 165, 213, .22); }
.form-scroll--disabled { pointer-events: none; opacity: .56; }
.edit-section h3 { margin: 0 0 13px; color: #fff; font-size: 15px; } .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 10px; }
.form-grid label { min-width: 0; display: grid; gap: 6px; color: #88adbf; font-size: 12px; } .form-grid :deep(.el-input-number) { width: 100%; }
.nudge-row { margin: 15px 0 10px; display: flex; align-items: center; gap: 8px; color: #88adbf; font-size: 12px; } .nudge-row :deep(.el-input-number) { width: 110px; }
.control-caption { margin: 13px 0 8px; color: #79b8d0; font-size: 12px; text-align: center; }
.direction-pad { width: 200px; margin: 0 auto 12px; display: grid; grid-template-columns: repeat(3, 62px); grid-template-rows: repeat(3, 36px); gap: 7px; }
.direction-pad .north { grid-column: 2; grid-row: 1; } .direction-pad .west { grid-column: 1; grid-row: 2; } .direction-pad .center { grid-column: 2; grid-row: 2; }
.direction-pad .east { grid-column: 3; grid-row: 2; } .direction-pad .south { grid-column: 2; grid-row: 3; }
.direction-pad :deep(.el-button) { width: 62px; margin: 0; padding: 6px; }
.direction-pad__center { grid-column: 2; grid-row: 2; display: grid; place-items: center; color: #43d8ff; font-size: 11px; border: 1px solid rgba(67, 216, 255, .35); background: rgba(67, 216, 255, .08); }
.geo-buttons { display: grid; grid-template-columns: repeat(3, 1fr); gap: 7px; }
.geo-buttons :deep(.el-button) { margin: 0; }
.attitude-step { margin-top: 16px; }
.attitude-nudges { display: grid; gap: 9px; }
.attitude-nudge { display: grid; grid-template-columns: 62px 1fr 1fr; gap: 7px; align-items: center; }
.attitude-nudge span { color: #88adbf; font-size: 12px; }
.attitude-nudge :deep(.el-button) { margin: 0; padding: 7px 5px; }
.select-label { margin-top: 14px; } .empty-editor { height: 100%; padding: 25px; }
.empty-editor__mark { width: 64px; height: 64px; margin: 0 auto; display: grid; place-items: center; color: #4bd9ff; font-size: 38px; border: 1px solid rgba(75, 217, 255, .45); border-radius: 50%; }
.empty-editor h3 { margin: 8px 0 0; color: #dff6ff; } .empty-editor p { margin: 0; line-height: 1.7; }
:deep(.el-input__wrapper), :deep(.el-select__wrapper), :deep(.el-input-number) { background: rgba(5, 23, 43, .76); box-shadow: 0 0 0 1px rgba(68, 142, 177, .45) inset; }
:deep(.el-input__inner), :deep(.el-select__selected-item), :deep(.el-input-number__decrease), :deep(.el-input-number__increase) { color: #e3f8ff; }
@media (max-width: 1280px) { .wayline-editor { grid-template-columns: 280px minmax(360px, 1fr) 320px; } }
@media (max-width: 980px) { .wayline-editor { height: auto; min-height: calc(100vh - 100px); grid-template-columns: 1fr; overflow: auto; } .renderer-stage { height: 560px; order: -1; } .waypoint-list { max-height: 360px; } }
</style>
