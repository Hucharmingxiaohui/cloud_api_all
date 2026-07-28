<template>
  <div class="cloud-management">
    <div class="page-title">云渲染管理</div>

    <el-tabs v-model="activeTab" class="cloud-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="室外渲染配置" name="outdoor">
        <div class="settings-panel outdoor-panel">
          <div class="panel-heading">
            <div>
              <h3>Outdoor 渲染配置</h3>
              <p>配置云渲染服务、3DGS 文件和 WebRTC 连接参数。</p>
            </div>
            <el-tag :type="connectionTagType">{{ connectionStatus }}</el-tag>
          </div>

          <el-form label-position="top" class="settings-form">
            <el-form-item label="启用室外云渲染">
              <el-switch v-model="form.enabled" active-text="已启用" inactive-text="未启用" />
            </el-form-item>
            <el-form-item label="云渲染服务地址">
              <el-input v-model="form.baseURL" placeholder="http://127.0.0.1:3000" />
            </el-form-item>
            <el-form-item label="Outdoor 点云文件">
              <div class="resource-row">
                <el-select v-model="form.pointCloudFile" filterable allow-create default-first-option placeholder="选择 3DGS 文件">
                  <el-option v-for="file in pointClouds" :key="file" :label="file" :value="file" />
                </el-select>
                <el-button :loading="loadingResources" :disabled="!form.enabled" @click="loadResources">读取文件</el-button>
              </div>
            </el-form-item>
            <el-form-item label="配准使用的 3DGS 文件">
              <el-select v-model="form.rendererParams.alignSplatFile" filterable allow-create placeholder="选择待配准 3DGS">
                <el-option v-for="file in pointClouds" :key="file" :label="file" :value="file" />
              </el-select>
            </el-form-item>
            <el-form-item label="参考基准模型">
              <el-select v-model="form.rendererParams.alignReferenceTileset" filterable allow-create placeholder="选择参考 Tileset">
                <el-option v-for="file in referenceTilesets" :key="file" :label="file" :value="file" />
              </el-select>
            </el-form-item>
            <el-form-item label="TURN 服务地址">
              <el-input v-model="turnServer.urls" placeholder="turn:host:3478?transport=udp" />
            </el-form-item>
            <el-form-item label="TURN 用户名">
              <el-input v-model="turnServer.username" />
            </el-form-item>
            <el-form-item label="TURN 密码">
              <el-input v-model="turnServer.credential" type="password" show-password />
            </el-form-item>
          </el-form>

          <div class="actions">
            <el-button type="primary" :loading="testing" :disabled="!form.enabled" @click="testConnection">测试连接</el-button>
            <el-button type="success" :loading="saving" @click="saveOutdoor">保存并重连</el-button>
            <el-button @click="resetConfig">恢复部署配置</el-button>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="模型配准" name="align">
        <div class="align-layout">
          <div class="align-view">
            <OutdoorRenderer
              v-if="alignStarted"
              :client="alignRendererClient"
              renderer="align"
              close-on-unmount
              @status-change="handleAlignStatus"
            />
            <div v-else class="align-empty">
              <span>选择模型后点击“启动配准”</span>
            </div>
            <div class="align-hint">鼠标左键旋转，右键平移，滚轮缩放，双击拾取参考坐标</div>
          </div>
          <div class="align-panel settings-panel">
            <h3>模型配准</h3>
            <p>选择待配准 3DGS 和参考基准模型后启动配准会话。</p>
            <el-form label-position="top" class="settings-form">
              <el-form-item label="待配准 3DGS">
                <el-select v-model="form.rendererParams.alignSplatFile" filterable allow-create>
                  <el-option v-for="file in pointClouds" :key="file" :label="file" :value="file" />
                </el-select>
              </el-form-item>
              <el-form-item label="参考基准模型">
                <el-select v-model="form.rendererParams.alignReferenceTileset" filterable allow-create>
                  <el-option v-for="file in referenceTilesets" :key="file" :label="file" :value="file" />
                </el-select>
              </el-form-item>
            </el-form>
            <div class="control-groups">
              <div class="control-group">
                <span>平移</span>
                <div class="button-grid">
                  <el-button v-for="action in translateActions" :key="action" @click="sendAlign(action)">{{ action }}</el-button>
                </div>
              </div>
              <div class="control-group">
                <span>旋转</span>
                <div class="button-grid">
                  <el-button v-for="action in rotateActions" :key="action" @click="sendAlign(action)">{{ action }}</el-button>
                </div>
              </div>
              <div class="control-group">
                <span>缩放与显示</span>
                <div class="button-grid">
                  <el-button v-for="action in displayActions" :key="action" @click="sendAlign(action)">{{ action }}</el-button>
                </div>
              </div>
              <div class="control-group">
                <span>分轴缩放</span>
                <div class="button-grid">
                  <el-button v-for="action in axisScaleActions" :key="action" @click="sendAlign(action)">{{ action }}</el-button>
                </div>
              </div>
              <div class="control-group">
                <span>步长控制</span>
                <div class="button-grid">
                  <el-button @click="sendAlign('translate10')">平移步长 x10</el-button>
                  <el-button @click="sendAlign('translate01')">平移步长 /10</el-button>
                </div>
                <div class="step-row">
                  <el-input-number v-model="translateStep" :min="0.001" :step="0.01" />
                  <el-button @click="sendAlign('step-translate', { value: translateStep })">设置平移步长</el-button>
                </div>
                <div class="step-row">
                  <el-input-number v-model="rotateStep" :min="0.01" :step="0.1" />
                  <el-button @click="sendAlign('step-rotate', { value: rotateStep })">设置旋转步长</el-button>
                </div>
              </div>
            </div>
            <div class="actions">
              <el-button type="primary" :loading="alignStarting" @click="startAlign">{{ alignStarted ? '重新启动配准' : '启动配准' }}</el-button>
              <el-button type="success" @click="sendAlign('save')">保存配准</el-button>
              <el-button @click="sendAlign('reset')">重置</el-button>
              <el-button @click="sendAlign('fit')">自动适配</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import OutdoorRenderer from '/@/components/cloudRenderer/OutdoorRenderer.vue'
import { CloudRendererClient, cloudRendererClient } from '/@/components/cloudRenderer/cloudRendererClient'
import { fetchRendererResources, getCloudRendererConfig, resetCloudRendererConfig, saveCloudRendererConfig } from '/@/components/cloudRenderer/cloudRendererConfig'
import type { CloudRendererConfig } from '/@/types/runtime-config'
import { useRoute, useRouter } from 'vue-router'

const activeTab = ref('outdoor')
const route = useRoute()
const router = useRouter()
const alignRendererClient = new CloudRendererClient()
const alignStarted = ref(false)
const alignStarting = ref(false)
let alignStartTimer: number | null = null
const connectionStatus = ref('未连接')
const loadingResources = ref(false)
const testing = ref(false)
const saving = ref(false)
const pointClouds = ref<string[]>([])
const referenceTilesets = ref<string[]>([])
const form = reactive<CloudRendererConfig>(getCloudRendererConfig())
const turnServer = reactive({
  urls: form.iceServers[0]?.urls || '',
  username: form.iceServers[0]?.username || '',
  credential: form.iceServers[0]?.credential || ''
})

const translateActions = ['tx+', 'tx-', 'ty+', 'ty-', 'tz+', 'tz-']
const rotateActions = ['rx+', 'rx-', 'ry+', 'ry-', 'rz+', 'rz-']
const displayActions = ['scale+', 'scale-', 'scale10', 'scale01', 'scaleAxisReset', 'toggle-splat-view', 'opacity+', 'opacity-', 'point+', 'point-']
const axisScaleActions = ['sx+', 'sx-', 'sy+', 'sy-', 'sz+', 'sz-']
const translateStep = ref(0.1)
const rotateStep = ref(0.2)
const connectionTagType = computed(() => connectionStatus.value.includes('失败') ? 'danger' : connectionStatus.value.includes('连接') ? 'warning' : 'success')

function routeTabName (routeName: unknown) {
  return routeName === 'cloudRenderingAlign' ? 'align' : 'outdoor'
}

activeTab.value = routeTabName(route.name)

watch(() => route.name, name => {
  activeTab.value = routeTabName(name)
})

function handleTabChange (tab: string) {
  router.push(tab === 'align' ? '/variableMgt/cloudRenderingMgt/align' : '/variableMgt/cloudRenderingMgt/outdoor')
}

function buildConfig (renderer: 'outdoor' | 'align' = 'outdoor'): CloudRendererConfig {
  return {
    ...form,
    renderer,
    rendererParams: { ...form.rendererParams },
    iceServers: turnServer.urls ? [{ ...turnServer }] : []
  }
}

async function loadResources () {
  loadingResources.value = true
  try {
    const [clouds, tilesets] = await Promise.all([
      fetchRendererResources(form.baseURL, '/api/point-clouds'),
      fetchRendererResources(form.baseURL, '/api/reference-tilesets')
    ])
    pointClouds.value = clouds
    referenceTilesets.value = tilesets
    ElMessage.success('云渲染文件读取成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '云渲染文件读取失败')
  } finally {
    loadingResources.value = false
  }
}

async function testConnection () {
  testing.value = true
  try {
    const response = await fetch(new URL('/health', form.baseURL).toString())
    if (!response.ok) throw new Error(`服务返回 ${response.status}`)
    connectionStatus.value = '服务正常'
    ElMessage.success('云渲染服务连接正常')
  } catch (error) {
    connectionStatus.value = '连接失败'
    ElMessage.error(error instanceof Error ? error.message : '云渲染服务连接失败')
  } finally {
    testing.value = false
  }
}

async function saveOutdoor () {
  saving.value = true
  try {
    const config = buildConfig('outdoor')
    saveCloudRendererConfig(config)
    if (!config.enabled) {
      cloudRendererClient.close()
      connectionStatus.value = '未启用'
      ElMessage.success('室外云渲染已关闭')
      return
    }
    await cloudRendererClient.restart('outdoor')
    connectionStatus.value = '已重连'
    ElMessage.success('配置已保存并重连云渲染')
  } catch (error) {
    connectionStatus.value = '连接失败'
    ElMessage.error(error instanceof Error ? error.message : '保存或重连失败')
  } finally {
    saving.value = false
  }
}

async function resetConfig () {
  Object.assign(form, resetCloudRendererConfig())
  const server = form.iceServers[0]
  turnServer.urls = server?.urls || ''
  turnServer.username = server?.username || ''
  turnServer.credential = server?.credential || ''
  try {
    if (!form.enabled) {
      cloudRendererClient.close()
      connectionStatus.value = '未启用'
      ElMessage.success('已恢复部署配置，室外云渲染未启用')
      return
    }
    await cloudRendererClient.restart('outdoor')
    ElMessage.success('已恢复部署配置并重连')
  } catch (error) {
    ElMessage.error('已恢复配置，但云渲染重连失败')
  }
}

async function startAlign () {
  if (!form.rendererParams.alignSplatFile || !form.rendererParams.alignReferenceTileset) {
    ElMessage.warning('请先选择待配准 3DGS 和参考基准模型')
    return
  }
  alignStarting.value = true
  saveCloudRendererConfig(buildConfig('align'))
  if (alignStarted.value) {
    alignStarted.value = false
    await nextTick()
  }
  alignStarted.value = true
  connectionStatus.value = '配准启动中'
  clearAlignStartTimer()
  alignStartTimer = window.setTimeout(() => {
    if (alignStarting.value) {
      alignStarting.value = false
      connectionStatus.value = '配准画面启动超时'
      ElMessage.error('配准会话已创建，但渲染画面未就绪，请检查云渲染服务 WebGL/GPU 状态')
    }
  }, 20000)
}

function handleAlignStatus (status: string) {
  if (!status) {
    clearAlignStartTimer()
    alignStarting.value = false
    connectionStatus.value = '配准画面已连接'
    ElMessage.success('配准画面已连接')
    return
  }
  connectionStatus.value = status
  if (status.includes('失败') || status.includes('异常')) {
    clearAlignStartTimer()
    alignStarting.value = false
  }
}

function clearAlignStartTimer () {
  if (alignStartTimer) {
    window.clearTimeout(alignStartTimer)
    alignStartTimer = null
  }
}

function sendAlign (action: string, payload: Record<string, any> = {}) {
  alignRendererClient.sendAlignCommand(action, payload)
}

onMounted(loadResources)
onBeforeUnmount(clearAlignStartTimer)
</script>

<style lang="scss" scoped>
.cloud-management {
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  padding: 20px;
  color: #dff6ff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.page-title { margin-bottom: 18px; color: #fff; font-size: 22px; font-weight: 600; }
.cloud-tabs { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.settings-panel { padding: 20px; border: 1px solid rgba(36, 153, 221, .55); background: rgba(7, 31, 70, .55); }
.panel-heading { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
h3 { margin: 0 0 8px; color: #fff; }
p { margin: 0; color: #8eb6d1; }
.settings-form { max-width: 900px; }
.outdoor-panel { width: min(100%, 980px); margin: 0 auto; box-sizing: border-box; }
.outdoor-panel .settings-form { max-width: none; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 28px; }
.outdoor-panel .settings-form .el-form-item:nth-child(1),
.outdoor-panel .settings-form .el-form-item:nth-child(2),
.outdoor-panel .settings-form .el-form-item:nth-child(3),
.outdoor-panel .settings-form .el-form-item:nth-child(4) { grid-column: span 2; }
.outdoor-panel .settings-form .el-form-item { min-width: 0; }
.resource-row { display: flex; gap: 10px; width: 100%; }
.resource-row .el-select { flex: 1; }
.actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 22px; }
.align-layout { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(340px, .8fr); gap: 16px; height: 100%; min-height: 0; overflow: hidden; }
.align-view { position: relative; min-height: 0; height: 100%; border: 1px solid #2d93c4; background: #050b12; }
.align-empty { display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; color: #779bb1; background: radial-gradient(circle at center, rgba(15, 69, 91, .25), #050b12 65%); }
.align-hint { position: absolute; left: 12px; bottom: 12px; padding: 8px 12px; color: #c8f2ff; background: rgba(2, 18, 28, .82); }
.align-panel { min-height: 0; height: 100%; overflow-y: auto; overflow-x: hidden; }
.control-groups { display: grid; gap: 18px; margin-top: 20px; }
.control-group > span { display: block; margin-bottom: 8px; color: #8eb6d1; }
.button-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.step-row { display: flex; gap: 8px; margin-top: 8px; }
:deep(.el-form-item__label) { color: #c8e8f5; }
:deep(.el-input__wrapper), :deep(.el-select__wrapper) { background: rgba(4, 20, 47, .7); box-shadow: 0 0 0 1px rgba(88, 170, 210, .55) inset; }
:deep(.el-input__inner), :deep(.el-select__selected-item) { color: #e7f8ff; }
:deep(.el-tabs__header) { flex-shrink: 0; }
:deep(.el-tabs__content) { flex: 1; min-height: 0; overflow: hidden; }
:deep(.el-tab-pane) { height: 100%; min-height: 0; overflow: hidden; }
@media (max-width: 1000px) { .align-layout { grid-template-columns: 1fr; } .align-view { min-height: 480px; } }
@media (max-width: 720px) {
  .outdoor-panel { padding: 14px; }
  .outdoor-panel .settings-form { display: block; }
}
</style>
