<template>
    <!-- <div style="width: 100%; height: 100%;">
        <div style="width: 100%;height: 80px;"></div>
        <div class="container">
        <div id="cesiumContainer"></div>
        <canvas id="threeContainer"></canvas>
        <div class="show-info">
            <span>{{ infoText.longitude }}</span>
            <button @click="flyToPoint()"></button>
        </div>
        </div>
    </div> -->
    <div class="container">
      <div class="left " :style="{ width: leftWidth + 'px' }">
        <div class="layout-arrow">
          <img :src="arrowIcon" />
        </div>
        <div class="box1">
            <div class="operation">
                <div class="item">选择航线:</div>
                <div class="item">
                  <el-select v-model="selected_wayline" placeholder="选择航线" :teleported='false' class="select-operation" @change="selectWayline">
                    <el-option
                      v-for="item in wayline_list"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>

                  <!-- 手动输入航线名称 -->
                  <el-input v-if="manualInput" v-model="newWayline" placeholder="手动输入航线" @blur="addWayline" />
                  <el-button v-if="!manualInput" @click="toggleManualInput">手动输入航线</el-button>
                </div>
                <div class="item">
                  <el-select v-model="selected_model" placeholder="选择模型" :teleported='false' class="select-operation" @change="selectModel">
                    <el-option
                      v-for="item in model_list"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </div>
                <div style="display: flex; margin-top: 25px;">
                  <div class="control-c" style="margin-right: 20px;">
                    <div class="cross-c">
                        <div style="width: 51px;height: 51px;cursor: pointer;position: relative;border: 1px solid black;" @click="movePoint('4')">
                          <div style="position: absolute;top: 50%;left: 50%; margin-top: -7px; color: aliceblue; transform: rotate(-45deg);" class="iconfont icon-qiehuanqishang">
                          </div>
                        </div>
                        <div style="width: 51px;height: 51px;cursor: pointer;position: relative;border: 1px solid black; " @click="movePoint('1')">
                          <div style="position: absolute;top: 50%;left: 50%; margin-top: -7px; margin-left: -10px; color: aliceblue; transform: rotate(45deg);" class="iconfont icon-qiehuanqishang">
                          </div>
                        </div>
                        <div style="width: 51px;height: 51px;cursor: pointer;position: relative;border: 1px solid black; " @click="movePoint('2')">
                          <div style="position: absolute;top: 50%;left: 50%; margin-top: -14px;color: aliceblue; transform: rotate(-135deg);"  class="iconfont icon-qiehuanqishang">
                          </div>
                        </div>
                        <div style="width: 51px;height: 51px;cursor: pointer;position: relative;border: 1px solid black; " @click="movePoint('3')">
                          <div style="position: absolute;top: 50%;left: 50%;margin-top: -18px;margin-left: -10px; color: aliceblue; transform: rotate(135deg);" class="iconfont icon-qiehuanqishang">
                          </div>
                      </div>
                    </div>
                  </div>
                  <div style="flex-grow: 1;  border-left: 2px solid black; padding-left: 10px;">
                      <div style="display: flex; align-items: center; justify-content: flex-start;color: aliceblue; height: 50px; border-bottom: 2px solid black;">
                        <span style="margin-right: 10px;">高度:</span>
                        <div style=" width: 20px; height: 20px; border-radius: 50%; border: 1px solid blue;color: aliceblue; text-align: center;line-height: 20px; font-size: 16px; cursor: pointer;" @click="movePoint('5')" >↑</div>
                        <div style=" width: 20px; height: 20px; border-radius: 50%; border: 1px solid blue;color: aliceblue; text-align: center;line-height: 20px; font-size: 16px;cursor: pointer;" @click="movePoint('6')">↓</div>
                      </div>
                      <div style="display: flex; align-items: center; justify-content: flex-start;color: aliceblue; height: 50px; border-bottom: 2px solid black;">
                        <span style="margin-right: 10px;"> 选中状态:</span>
                        <span v-if="isDragging">已选中</span>
                        <span v-if="isDragging" @click="cancelDragging" style="cursor: pointer;">✖️</span>
                        <span v-if="!isDragging">未选中</span>
                        <!-- <span v-if="!isDragging">✔️</span> -->
                      </div>

                  </div>
                </div>
                <div class="item"><el-button class="btn" @click="handleConfirm" >确认航点</el-button></div>
                <div class="item"><el-button class="btn" @click="clearDraw" >清空</el-button></div>
                <div class="item"><el-button class="btn" @click="handleplanning" >路径规划</el-button></div>
                <div class="item"><el-button class="btn" @click="handlegenerate" >生成航线</el-button></div>
                <!-- <div class="item"><el-button class="btn">查看航线</el-button></div>
                <div class="item"><el-button class="btn">一键执飞</el-button></div> -->
              </div>
          <el-divider />
        </div>
        <div class="box2">
            <div class="logs-title" style="color: aliceblue;">点位绘制</div>
            <div style="width: 100%; margin-top: 30px;">
              <el-table :data="PointsData" stripe>
                  <!-- <el-table-column type="selection" width="55" /> -->
                  <el-table-column label="序号" align='center' width="40" type="index">
                  </el-table-column>
                  <el-table-column label="经度" show-overflow-tooltip="true">
                    <template #default="scope">
                      <div>{{ scope.row.lat }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column label="纬度" show-overflow-tooltip="true">
                    <template #default="scope">
                      <div>{{ scope.row.lon }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column label="高度" show-overflow-tooltip="true">
                    <template #default="scope">
                      <div>{{ scope.row.height }}</div>
                    </template>
                  </el-table-column>
                </el-table>
            </div>
        </div>
      </div>
      <div class="middle">
        <!-- 右侧占满剩余空间的内容 -->
        <div class="box3">
          <div id="cesiumContainer"></div>
          <canvas id="threeContainer"></canvas>
          <!-- <div class="show-info">
            <span> 经度：{{ infoText.longitude }}</span>
            <span> 纬度：{{ infoText.latitude }}</span>
            <span> 海拔高度:{{ infoText.height}}</span>
          </div> -->
        </div>
        <ContextMenu ref="contextMenu" :options="options" v-model:isVisible="isMenuVisible" />
      </div>
      <div class="right">
        <div class="camera-box" id = "droneCesiumContainer"> <UavCamera ref="childMap" @cameraUpdated="handleCameraUpdated"></UavCamera></div>
        <!-- <div class="item" style="margin: 20px;color:aliceblue;">
          <span>航向角</span>
          <el-slider v-model="value1" :min="0" max="360"  :disabled="!ischangecamera" @input="changeModels"/>
        </div>
        <div class="item" style="margin: 20px;color:aliceblue;">
          <span>偏航角</span>
          <el-slider v-model="value2" :min="-180" max="180" :disabled="!ischangecamera" @input="changeModels" />
        </div>
        <div class="item" style="margin: 20px;color:aliceblue;">
          <span>翻滚角</span>
          <el-slider v-model="value3" :min="-180" max="180" :disabled="!ischangecamera" @input="changeModels" />
        </div> -->
        <!-- 航向角 -->
<div class="item" style="margin: 20px; color: aliceblue;">
  <span>航向角</span>
  <div style="display: flex; align-items: center;">
    <el-button
      icon="el-icon-minus"
      :disabled="!ischangecamera"
      @click="() => { value1 = (value1 - 1 + 360) % 360; changeModels() }"
      circle size="small"
    />
    <el-slider
      v-model="value1"
      :min="0"
      :max="360"
      :disabled="!ischangecamera"
      @input="changeModels"
      style="flex: 1; margin: 0 10px;"
    />
    <el-button
      icon="el-icon-plus"
      :disabled="!ischangecamera"
      @click="() => { value1 = (value1 + 1) % 360; changeModels() }"
      circle size="small"
    />
  </div>
</div>

<!-- 偏航角 -->
<div class="item" style="margin: 20px; color: aliceblue;">
  <span>偏航角</span>
  <div style="display: flex; align-items: center;">
    <el-button
      icon="el-icon-minus"
      :disabled="!ischangecamera"
      @click="() => { value2 = Math.max(-180, value2 - 1); changeModels() }"
      circle size="small"
    />
    <el-slider
      v-model="value2"
      :min="-180"
      :max="180"
      :disabled="!ischangecamera"
      @input="changeModels"
      style="flex: 1; margin: 0 10px;"
    />
    <el-button
      icon="el-icon-plus"
      :disabled="!ischangecamera"
      @click="() => { value2 = Math.min(180, value2 + 1); changeModels() }"
      circle size="small"
    />
  </div>
</div>

<!-- 翻滚角 -->
<div class="item" style="margin: 20px; color: aliceblue;">
  <span>翻滚角</span>
  <div style="display: flex; align-items: center;">
    <el-button
      icon="el-icon-minus"
      :disabled="!ischangecamera"
      @click="() => { value3 = Math.max(-180, value3 - 1); changeModels() }"
      circle size="small"
    />
    <el-slider
      v-model="value3"
      :min="-180"
      :max="180"
      :disabled="!ischangecamera"
      @input="changeModels"
      style="flex: 1; margin: 0 10px;"
    />
    <el-button
      icon="el-icon-plus"
      :disabled="!ischangecamera"
      @click="() => { value3 = Math.min(180, value3 + 1); changeModels() }"
      circle size="small"
    />
  </div>
</div>
<!-- 拍照模式 -->
<!-- 拍照模式（下拉） -->
<div class="item" style="margin: 20px; color: aliceblue;">
  <span>拍照模式：</span>
  <el-select
        v-model="captureMode"
        placeholder="选择拍照模式"
        size="small"
        :teleported="false"
        style="width: 160px; margin-left: 6px;"
      >
        <el-option v-for="opt in captureOptions"
                   :key="opt.value"
                   :label="opt.label"
                   :value="opt.value" />
      </el-select>
</div>

<div class="item" style="margin: 20px; color: aliceblue;">
<span>速度:</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
  <el-input-number v-model="waypointSpeed" :min="1" :max="20" size="small" />
  <span style="margin-left:4px;">m/s</span></div>

<div class="camera-info-container">
  <div class="info-title">云台参数</div>
  <div class="info-grid">
    <div class="info-item">经度: <span>{{ cameraParams.longitude }}</span></div>
    <div class="info-item">纬度: <span>{{ cameraParams.latitude }}</span></div>
    <div class="info-item">高度: <span>{{ cameraParams.height }}</span></div>
    <div class="info-item">航向角: <span>{{ cameraParams.heading }}</span>°</div>
    <div class="info-item">焦距: <span>{{ cameraParams.equivalentFocalLength }}</span></div>

    <div class="info-item">俯仰角<span>{{ cameraParams.pitch }}</span>°</div>
    <div class="info-item">fov: <span>{{ cameraParams.fov }}</span></div>
    <div class="info-item">翻滚角: <span>{{ cameraParams.roll }}</span>°</div>

  </div>
</div>

  <!-- <div>

  <button class="confirm-button" @click="handleConfirm">确认航点</button>
</div>
<div>

  <button class="confirm-button" @click="handlegenerate">生成航线</button>

</div> -->

        <!-- <div class="item" style="margin: 20px;color:aliceblue;">
          <span>roll</span>
          <el-slider v-model="value3" :min="-180" max="180" @input="changeModels"/>
        </div> -->

      </div>
    </div>
</template>

<script setup>
import controlPanel from '../control/ControlDegree.vue'
import 'cesium/Build/Cesium/Widgets/widgets.css'
import AmapMercatorTilingScheme from './AmapMercatorTilingScheme'
import { AMapImageryProvider, BaiduImageryProvider, GeoVisImageryProvider } from '@cesium-china/cesium-map'
import { getLocation, gethWaylineInfo, getWayPointInfo, commitWaylineFile1 } from '/@/api/wayline'
import * as Cesium from 'cesium'
import { createSampleWayline, getEllipsoidHeight } from '/@/components/wayline/createWayline'
import { reactive, onMounted, ref, computed, onBeforeUnmount, watch, nextTick } from 'vue'
import { EDeviceTypeName, ELocalStorageKey, ERouterName } from '/@/types'
import { importModelFile, getModelInfoByName, getAllModels, deleteModel } from '/@/api/model'
import { gcj02towgs84, wgs84togcj02 } from '/@/vendors/coordtransform'
import ContextMenu from './Menu.vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { drawLine, removeLogo, load3DTilesModels, loadGltfModel, removeGltfModel, createPyramis, removePyramis, updateOrientation, drawPoints } from './cesiumTools'
import { CreateFrustum } from './CreateFrustum'
import { message } from 'ant-design-vue'
import UavCamera from './uavCamera.vue'
import labelBg from '/@/assets/v4/label_bg.png'
import waypointBg from '/@/assets/v4/waypoint.png'

// Vue 组件中的方法 xtj 2025/7/31

import { generateWaylineRequest } from './kmzRequest'
import Camera from '/@/pages/page-web/projects/camera.vue'
import { loadVoxel, checkAndInsertAvoidanceBetweenTwoPoints } from '/src/utils/voxelNav'// 航线规划 体素检测

let routeEntity = null // 航线 polyline 实体
const _arrowMat = null
const _tickUnlisten = null // 退出时要取消 onTick

const manualInput = ref(false) // 控制是否显示手动输入框
const newWayline = ref('') // 用于存储手动输入的航线名称
let animationFrameId = null // 存储循环ID
// 切换手动输入模式
function toggleManualInput () {
  manualInput.value = !manualInput.value
}

// 当手动输入框失去焦点时，添加新的航线
function addWayline () {
  if (newWayline.value.trim()) {
    const newWaylineItem = {
      label: newWayline.value,
      value: new Date().getTime().toString() // 使用当前时间戳作为唯一ID
    }
    wayline_list.value.push(newWaylineItem) // 将新的航线添加到列表中
    selected_wayline.value = newWaylineItem.value // 设置为选中的航线
    newWayline.value = '' // 清空输入框
    manualInput.value = false // 隐藏输入框
  } else {
    ElMessage.error('航线名称不能为空')
  }
}

const cameraParams = ref({
  longitude: null,
  latitude: null,
  height: null,
  heading: null,
  pitch: null,
  roll: null,
  fov: null,
  focalLength: null,
  equivalentFocalLength: null

})

// 处理子组件传来的相机参数 xtj
function handleCameraUpdated (params) {
  cameraParams.value = { ...params }
}

const leftWidth = ref(400)
const cesium = {
  viewer: null,
}
const childMap = ref()
const colorData = ref([])
const positionProperty = null
const rotationAngle = 0
const infoText = reactive({
  longitude: null,
  latitude: null,
  height: null
})
const minWGS84 = [121.254, 37.502]
const maxWGS84 = [121.255, 37.503]

// 右侧拍照模式（默认：红外+可见光）
const captureMode = ref('ir,visable') // 'ir_vis' | 'ir' | 'vis' | 'none'
const captureOptions = [
  { value: 'ir,visable', label: '红外 + 可见光' },
  { value: 'ir', label: '仅红外' },
  { value: 'visable', label: '仅可见光' },
  { value: 'none', label: '不拍照' }
]

const waypointSpeed = ref(5)

// =--------------------------------------------------------------------------------------------------------------------------------------------------------------------

// --------------------------------------------------------------------------------------左侧功能栏----------------------------------------------------------------------------------------
const isRightPanelVisible = ref(true)
const arrowIcon = computed(() => {
  return isRightPanelVisible.value
    ? new URL('../../assets/v3/icon/right-arrow.png', import.meta.url).href
    : new URL('../../assets/v3/icon/left-arrow.png', import.meta.url).href
})

// const selected_wayline = ref('')
// const wayline_list = [
//   {
//     label: '视讯楼顶设备巡检司空二1111',
//     value: '3d715aa6-4dfa-4ca3-afbb-395e8ccdd080'
//   }
// ]
const selected_wayline = ref('')
const wayline_list = ref([
  {
    label: '视讯楼顶设备巡检司空二1111',
    value: '3d715aa6-4dfa-4ca3-afbb-395e8ccdd080'
  }
])

// const af = [[37.51887700000004, 121.365815, 104.16000000201166, 0.0], [37.518889304404105, 121.36578556150079, 110.59132765606046, 1.0], [37.51890160877591, 121.36575612305117, 117.02265666425228, 1.0], [37.51893733974753, 121.36575612305117, 120.06777343899012, 1.0], [37.518963924487416, 121.36567839189483, 124.4349324721843, 1.0], [37.518978204919335, 121.36563009908883, 122.37076354306191, 1.0], [37.51899248534073, 121.36558180623322, 120.30659786239266, 1.0], [37.51901906992678, 121.36550407507995, 124.67377631738782, 1.0], [37.51903335030465, 121.36545578215066, 122.6096192151308, 1.0], [37.51904763067201, 121.36540748917174, 120.54546536225826, 1.0], [37.519061911028864, 121.3653591961432, 118.48131475690752, 1.0], [37.5190761913752, 121.36531090306505, 116.4171674028039, 1.0], [37.519054740781556, 121.36526260993726, 111.30789592210203, 1.0], [37.51905671702005, 121.36524375485438, 102.81240156013519, 1.0], [37.51907099735269, 121.36519546160176, 100.74826088361442, 1.0], [37.51907600000004, 121.365187, 99.87000000104308, 0.0], [37.518726000000036, 121.365113, 96.0, 0.0], [37.51874745097181, 121.36516129310856, 101.10924897994846, 1.0], [37.518733170758175, 121.3652095861675, 103.17339658550918, 1.0], [37.518718890534025, 121.3652578791768, 105.23754744231701, 1.0], [37.51870461029937, 121.36530617213647, 107.30170154664665, 1.0], [37.51869033005421, 121.36535446504652, 109.36585890222341, 1.0], [37.51867604979853, 121.36540275790696, 111.4300195062533, 1.0], [37.5186975006401, 121.36545105071777, 116.53928353264928, 1.0], [37.51874237801971, 121.36552878161903, 118.26232067961246, 1.0], [37.51872809769597, 121.36557707435618, 120.32649014610797, 1.0], [37.518772974957386, 121.36565480528454, 122.04954194277525, 1.0], [37.518782121134244, 121.36573253626409, 120.72749652899802, 1.0], [37.51880554770524, 121.36576197462978, 117.34127717278898, 1.0], [37.518843254751935, 121.36574312030491, 111.89088059682399, 1.0], [37.518866681385155, 121.36577255874425, 108.50466439314187, 1.0], [37.51889010803589, 121.36580199723328, 105.1184503166005, 1.0], [37.51887700000004, 121.365815, 104.16000000201166, 0.0]]

function test (af) {
  const temResult = createSampleWayline(af)
  // const temResult = getEllipsoidHeight(121.365718434507, 37.5195188536013, 107.954605)
  console.log(temResult)
  commitWaylineFile1(workspaceId, temResult).then(res => {
    if (res.code !== 0) {
      return
    }
    ElMessage({
      message: '航线保存成功!',
      type: 'success',
      plain: true,
    })
  })
}
// -----------------------------------------------------------------------------------元素加载-------------------------------------------------------------------------
/**
 * 选择事件，查询航线经纬度
 * @param val
 */
// const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
const waypoints = ref()
const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)
function selectWayline (val) {
  gethWaylineInfo(workspaceId, val).then(res => {
    if (res.code !== 0) {
      return
    }
    const pointList = []
    // 坐标系转换 大地=> 高德 将字符串转换为对象，并为每个点添加高度20
    waypoints.value = res.data.map(point => {
    //   const [lng, lat] = JSON.parse(point)
    //   const [longitude, latitude] = wgs84togcj02(lng, lat)
      const [longitude, latitude] = JSON.parse(point)
      const tree = transformToPostion(longitude, latitude, 100)
      obj.points.push(tree)
      const Entity = drawPoints(cesium.viewer, pointOptions, longitude, latitude, 100)
      const node = {
        id: Entity.id,
        lon: longitude.toFixed(6),
        lat: latitude.toFixed(6),
        height: 100.0
      }
      PointsData.value.push(node)
      return { longitude, latitude, height: 100 }
    })
    drawLine(waypoints.value, cesium.viewer)
  })
}

const selected_model = ref('')
const model_list = ref([])
const body = {
  page: 1,
  total: 0,
  page_size: 10
}
const modelInfo = ref()
function GetModels () {
  getAllModels(body).then(res => {
    if (res.code !== 0) {
      return
    }
    modelInfo.value = res.data.list
    model_list.value = res.data.list.map(item => ({
      label: item.model_name,
      value: item.model_id
    }))
  })
}

/**
 * 选择事件，查询模型
 * @param val
 */
function selectModel (val) {
  const modelData = modelInfo.value.find((item) => item.model_id === val)
  const tilesetUrl = `/${modelData.file_path}/${modelData.json_name}`

  // const tilesetUrl = '/model/Scene/Production_5.json'

  // load3DTiles(tilesetUrl)
  cesium.viewer.scene.primitives.removeAll()// 先清空场景
  load3DTilesModels(cesium.viewer, tilesetUrl)
  childMap.value.loadModel(tilesetUrl)
}
// -------------------------------------------------------------------------------------场景初始化-----------------------------------------------------------------------------------------
const voxel = null
onMounted(async () => {
  GetModels()
  init()

  // const cesiumElement = document.getElementById('cesiumContainer')

  // cesiumElement.addEventListener('contextmenu', handleRightClick)

  // voxel = await loadVoxel('model//Vox') // 这里放 header.json/occupancy.bin 的访问路径根
})

onBeforeUnmount(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId) // 停止循环
    animationFrameId = null
  }
  if (cesium.viewer && !cesium.viewer.isDestroyed()) {
    cesium.viewer.destroy()
    cesium.viewer = null
  }
  document.removeEventListener('contextmenu', handleRightClick)
})

function init () {
  initCesium()
  startRenderLoop()
  load3DTiles()
  setupClickEvent()
}
// function removeLogo (viewer) {
//   const creditContainer = viewer.cesiumWidget.creditContainer
//   creditContainer.style.display = 'none'
// }
function initCesium () {
  Cesium.Ion.defaultAccessToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiI3ZmJjODE1Yy1kMjU4LTQyZTgtODAyZC1mNzE2MDNhMmQ3YzUiLCJpZCI6MTk5NzQwLCJpYXQiOjE3MDk2Mjg5Mjh9.GuRbyEbm8FknaFOM34kGm9wCbf2XVjp873h_QD-Vs7A'
  cesium.viewer = new Cesium.Viewer('cesiumContainer', {
    useDefaultRenderLoop: false,
    selectionIndicator: false,
    infoBox: false,
    navigationInstructionsInitiallyVisible: false,
    fullscreenButton: false,
    allowTextureFilterAnisotropic: false,
    // terrainProvider: Cesium.createWorldTerrain(), // 轨迹追踪
    contextOptions: {
      webgl: {
        alpha: false,
        antialias: true,
        preserveDrawingBuffer: true,
        failIfMajorPerformanceCaveat: false,
        depth: true,
        stencil: false,
      },
    },
    targetFrameRate: 60,
    resolutionScale: 0.1,
    orderIndependentTranslucency: true,
    // imageryProviderViewModels: [], // 不使用默认地图
    baseLayerPicker: false,
    automaticallyTrackDataSourceClocks: false,
    dataSources: null,
    clock: null,
    terrainShadows: Cesium.ShadowMode.DISABLED,
    // 禁用底部的控制面板
    navigationHelpButton: false, // 不显示帮助按钮
    sceneModePicker: false, // 不显示场景模式切换按钮（2D/3D/Columbus视角）
    timeline: false, // 不显示时间轴
    imageryProviderViewModels: [], // 不使用默认地图
    geocoder: false, // 不显示地理编码器
    homeButton: false, // 不显示“Home”按钮
    sceneMode: Cesium.SceneMode.SCENE3D, // 直接使用3D模式
    animation: false, // 左下角时间表盘
  })
  const imageryProvider = new Cesium.UrlTemplateImageryProvider({
    url: '/api/map1/{z}/{x}/{y}.png',
    minimumLevel: 0,
    maximumLevel: 18,
    tilingScheme: new AmapMercatorTilingScheme() // 使用自定义 TilingScheme
  })
  const layer = cesium.viewer.imageryLayers.addImageryProvider(imageryProvider)
  // 在线高德地图纠偏 gcj02 => wgs84
  // cesium.viewer.imageryLayers.addImageryProvider(new AMapImageryProvider({
  //   style: 'img',
  //   crs: 'WGS84'
  // }))
  // cesium.viewer._cesiumWidget._creditContainer.style.display = 'none' // 隐藏logo版权
  removeLogo(cesium.viewer)
  if (Cesium.FeatureDetection.supportsImageRenderingPixelated()) {
    //  判断是否支持图像渲染像素化处理
    cesium.viewer.resolutionScale = window.devicePixelRatio
  }
  cesium.viewer.scene.postProcessStages.fxaa.enabled = true
  cesium.viewer.scene.debugShowFramesPerSecond = true // 显示帧率
}
function startRenderLoop () {
  if (!cesium.viewer || cesium.viewer.isDestroyed()) return
  cesium.viewer.render()
  animationFrameId = requestAnimationFrame(startRenderLoop)
}
//  -------------------------------------------------------------------------------------模型加载-----------------------------------------------------------------------------------------
// 加载3DTiles模型
function load3DTiles () {
  // const tilesetUrl = 'http://172.20.63.157:9000/models/dfelanqiuchang/tileset.json'
  const tilesetUrl = '/model/dfelanqiuchang/tileset.json' // '/model/Scene/Production_5.json'
  load3DTilesModels(cesium.viewer, tilesetUrl)
  childMap.value.loadModel(tilesetUrl)
}

// 加载glft模型
function loadGltf (position) {
  const url = '/model/gltf/airplane.glb'
  return loadGltfModel(cesium.viewer, url, position)
}

// 创建视锥体
function createFrustum (lon, lat, height) {
  const origin = Cesium.Cartesian3.fromDegrees(lon, lat, height)

  // 视角定位
  cesium.viewer.camera.flyTo({
    destination: origin,
  })

  // 确定相对于视点的旋转矩阵
  const enu = Cesium.Transforms.eastNorthUpToFixedFrame(origin)
  const rotation = Cesium.Matrix3.getRotation(enu, new Cesium.Matrix3())
  const orientation = Cesium.Quaternion.fromRotationMatrix(rotation)

  // 创建视锥体
  const createFrustum = new CreateFrustum({
    position: origin,
    orientation: orientation,
    fov: 0,
    near: 1,
    far: 100,
    aspectRatio: 600 / 1080,
  }, cesium.viewer)

  // 动态修改视锥体的姿态
  // const head = 0
  // const pitch = 0
  // const roll = 0
  // const rot = orientation
  //   setInterval(() => {
  //     // 绕Z轴旋转-航向
  //     // head += 0.01;
  //     // rot = Cesium.Matrix3.multiply(
  //     //  rotation,
  //     //  Cesium.Matrix3.fromRotationZ(head),
  //     //  new Cesium.Matrix3()
  //     // );

  //     // 绕X轴旋转-俯仰
  //     // pitch += 0.01;
  //     // rot = Cesium.Matrix3.multiply(
  //     //  rotation,
  //     //  Cesium.Matrix3.fromRotationX(pitch),
  //     //  new Cesium.Matrix3()
  //     // );

  //     // 绕Y轴旋转-翻滚
  //     roll += 0.01
  //     rot = Cesium.Matrix3.multiply(
  //       rotation,
  //       Cesium.Matrix3.fromRotationY(roll),
  //       new Cesium.Matrix3()
  //     )

  //   orientation = Cesium.Quaternion.fromRotationMatrix(rot)
  //   createFrustum.update(origin, orientation)
  // }, 200)
}
//  ----------------------------------------------------------------------------------------------------前后端交互  数据转换--------------------------------------------------------------------------------------------------------

/**
 * 经纬度坐标转笛卡尔坐标系
 * @param lon
 * @param lat
 * @param height
 */
function transformToPostion (lon, lat, height) {
  const cesiumPosition = Cesium.Cartesian3.fromDegrees(lon, lat, height)
  // 计算转换后的 Three.js 坐标
  const threejs = [
    cesiumPosition.x, cesiumPosition.y, cesiumPosition.z
  ]
  return threejs
}

/**
 * 笛卡尔坐标系转经纬度坐标
 * @param lon
 * @param lat
 * @param height
 */
function transformToLonLatHeight (cartesian) {
  // 从 Cartesian3 坐标转换回经纬度和高度
  const cartographic = Cesium.Cartographic.fromCartesian(cartesian)

  // 获取经度、纬度和高度
  const longitude = Cesium.Math.toDegrees(cartographic.longitude) // 经度
  const latitude = Cesium.Math.toDegrees(cartographic.latitude) // 纬度
  const height = cartographic.height // 高度
  return { longitude, latitude, height }
}

// 请求数据
const obj = reactive({
  startPoint: '(-29.144,15.129,2.0)',
  flightSpeed: 2,
  expansionCoefficient: 0.5,
  missionExecutionTime: 20,
  returnBatteryLevel: 25,
  enduranceTime: 40,
  powerConsumptionRation: '2:8:1:2',
  chargingTime: 10,
  returnAltitude: 50,
  points: [
  ],
  maintenanceArea: {
    j: [],
    y: []
  }
})
let timer = null // 定时器
const submitStatus = ref(false)
/**
 * http请求访问
 */
// async function submit () {//弃用 2025/8/5
//   try {
//     obj.points = []
//     PointsData.value.forEach(item => {
//       obj.points.push([Number(item.lat), Number(item.lon), Number(item.height)])
//     })
//     console.log('请求后端数据', obj)
//     submitStatus.value = true
//     const res = await axios.post('http://172.20.63.227:9527/startTask', obj)

//     if (res.status !== 200) {
//       console.log('无法访问后端，登录失败!', 'warning')
//       submitStatus.value = false
//       message.error('路径规划失败')
//     } else {
//       console.log('ssssss', res.data)
//       const task_id = res.data.data.task_id
//       // 定时器
//       timer = setInterval(() => {
//         queryPoints(task_id)
//       }, 5000)
//     }
//   } catch (error) {
//     submitStatus.value = false
//     message.error('路径规划失败')
//     // 处理错误
//     console.error('Error:', error)
//   }
// }

// async function queryPoints (task_id) {
//   try {
//     const res = await axios.get(`http://172.20.63.227:9527/getPath/${task_id}`)
//     if (res.status !== 200) {
//       // submitStatus.value = false
//       console.log('无法访问后端，登录失败!', 'warning')
//       clearInterval(timer)
//     } else {
//       const statue = res.data.data.status
//       if (statue && statue === 'completed') {
//         clearInterval(timer)
//         // clearEntities()
//         if (res.data.data.result && res.data.data.result.length > 0) {
//           res.data.data.result.pop()
//           // 转经纬度
//           let waypoints = []
//           // let test = []
//           waypoints = res.data.data.result.map(point => {
//             const [latitude, longitude, height, flag] = point
//             return { latitude, longitude, height }
//           })
//           test(res.data.data.result)

//           drawLine(waypoints, cesium.viewer)
//           // drawPoints(waypoints)
//           submitStatus.value = false
//         }
//       }
//       if (statue && statue === 'failed') {
//         clearInterval(timer)
//         submitStatus.value = false
//         console.log('路径规划失败', res.data)
//         message.error('路径规划失败')
//       }

//       // clearInterval(timer)
//     }
//   } catch (e) {
//     console.log(e)
//     submitStatus.value = false
//     clearInterval(timer)
//     // new proxy.$tips('服务器发生出错误','error').mess_age()
//   }
// }

// ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

const PointsData = ref([])

// 保持地球不动
function disableDefaultScreenSpaceEventHandlers () {
  cesium.viewer.scene.screenSpaceCameraController.enableRotate = false // 禁止旋转
  cesium.viewer.scene.screenSpaceCameraController.enableTranslate = false // 禁止平移
  cesium.viewer.scene.screenSpaceCameraController.enableZoom = false // 禁止缩放
  cesium.viewer.scene.screenSpaceCameraController.enableTilt = false // 禁止倾斜
  cesium.viewer.scene.screenSpaceCameraController.enableLook = false // 禁止观察（自由视角查看）
}

// 允许地球不动
function enableDefaultScreenSpaceEventHandlers () {
  cesium.viewer.scene.screenSpaceCameraController.enableRotate = true
  cesium.viewer.scene.screenSpaceCameraController.enableTranslate = true
  cesium.viewer.scene.screenSpaceCameraController.enableZoom = true
  cesium.viewer.scene.screenSpaceCameraController.enableTilt = true
  cesium.viewer.scene.screenSpaceCameraController.enableLook = true
}

function getCartesianCoordinate (pos) {
  const cartesianCoordinate = cesium.viewer.scene.pickPosition(pos)
  if (!cartesianCoordinate) {
    return null
  }
  const cartographic = Cesium.Cartographic.fromCartesian(cartesianCoordinate)
  const longitude = Cesium.Math.toDegrees(cartographic.longitude)
  const latitude = Cesium.Math.toDegrees(cartographic.latitude)
  const height = cartographic.height
  const position = {
    longitude: Number(longitude.toFixed(14)),
    latitude: Number(latitude.toFixed(14)),
    altitude: Number(height.toFixed(14))
  }
  return position
}
// ============================================================================================绘制/编辑===========================================================================================

// 绘制点
const pointOptions = {
  type: 'text-icon', // 默认 'text-icon'类型(图标+文字)、 'text' 类型(仅文字)
  paddingTo: 10, // 图标距离文字背景
  textPadding: 20, // 文字左右间距
  textBcgWidth: 200, // 文字背景宽度
  textBcgHeight: 50, // 文字背景高度
  iconWidth: 50, // 图标宽度
  iconHeight: 50, // 图标高度
  textBcgUrl: '../../assets/v4/label_bg.png', // 文字背景图片地址
  iconUrl: '/src/assets/v4/waypoint.png', // 图标地址
  textUrl: '', // 字体地址
  text: '航点', // 文字内容
  textColor: '#E3FFFD', // 字体颜色
  textFontSize: 20, // 字体大小
  textFontWeight: 1000, // 字体粗细
}

// 删除所有点和线
function clearEntities () {
  cesium.viewer.entities.removeAll()
}

// 清空实体
function clearDraw () {
  clearEntities()
  PointsData.value = []// 清空点
  if (routeEntity) { // 清空线
    cesium.viewer.entities.remove(routeEntity)
    routeEntity = null
  }
  sessionStorage.setItem('waypointsData', JSON.stringify([]))
}

// 生成随机颜色的方法
function getRandomColor () {
  const randomValue = () => Math.floor(Math.random() * 256)
  let color = new Cesium.Color(randomValue() / 255, randomValue() / 255, randomValue() / 255, 0.5)
  // 检查颜色是否已存在
  while (colorData.value.some(existingColor => colorsAreEqual(existingColor, color))) {
    // 如果已存在，则重新生成颜色
    color = new Cesium.Color(randomValue() / 255, randomValue() / 255, randomValue() / 255, 0.5)
  }
  // 保存新生成的颜色
  colorData.value.push(color)
  return color
}
// 比较两个颜色是否相等
function colorsAreEqual (color1, color2) {
  return color1.red === color2.red && color1.green === color2.green && color1.blue === color2.blue
}

/**
 * 设置航点绘制、编辑部分
 */

// 1. 参数 右键菜单选项
const isMenuVisible = ref(false)
const isAllowDrawing = ref(true)
const ischangecamera = ref(false)
const options = ref([
  { name: '移动', action: prevPage, shortcut: '', shortcutKey: 'alt + arrowup' },
  { name: '删除', action: nextPage, shortcut: '', shortcutKey: 'alt + arrowdown' },
  { name: '编辑', action: changeCameraViwer, shortcut: '', shortcutKey: 'alt + arrowdown' },
])
const contextMenu = ref(null)
let selectedEntity = null // 当前选中的点实体
let previousPosition = null // 记录之前的位置
const isDragging = ref(false) // 新增标志位，表示是否在拖拽
const startDragPosition = null // 记录拖拽起始位置
// 2. cesium 点击事件
function setupClickEvent () {
  const isDrawing = false // 新增标志位，表示是否在绘制
  const handlePoint = new Cesium.ScreenSpaceEventHandler(cesium.viewer.scene.canvas)

  // 单击事件：绘制新点
  handlePoint.setInputAction(function (event) {
    clearTimeout(timer)
    timer = window.setTimeout(function () {
      if (isDragging.value) {
        ElMessage.error('请取消选中')
        return
      }
      // isDragging.value = true
      // 单击获取坐标点
      const coordinate = getCartesianCoordinate(event.position)
      if (coordinate) {
        const Entity = drawPoints(cesium.viewer, pointOptions, coordinate.longitude, coordinate.latitude, coordinate.altitude)
        console.log('sadada', Entity)
        infoText.longitude = coordinate.longitude.toFixed(14)
        infoText.latitude = coordinate.latitude.toFixed(14)
        infoText.height = coordinate.altitude.toFixed(14)
        // 显示在表格中
        const node = {
          id: Entity.id,
          lon: coordinate.longitude.toFixed(14),
          lat: coordinate.latitude.toFixed(14),
          height: coordinate.altitude.toFixed(14)
        }
        PointsData.value.push(node)
      }
    }, 200)
    // handleConfirm()
    // isDragging.value = false
  }, Cesium.ScreenSpaceEventType.LEFT_CLICK)

  // 右键点击事件
  handlePoint.setInputAction(function (event) {
    const pickedObject = cesium.viewer.scene.pick(event.position)
    if (Cesium.defined(pickedObject) && pickedObject.id) {
      selectedEntity = pickedObject.id // 获取选中的点实体
      previousPosition = selectedEntity.position.getValue(Cesium.JulianDate.now()) // 记录当前位置
    } else {
      selectedEntity = null
    }
  }, Cesium.ScreenSpaceEventType.RIGHT_CLICK)
}

// 取消拖动
function cancelDragging () {
  isDragging.value = false
  isAllowDrawing.value = true
  selectedEntity = null
}

// 移动点的方法，根据方向调整点的位置
function movePoint (val) {
  if (!selectedEntity) {
    return
  }
  const currentPosition = selectedEntity.position.getValue(Cesium.JulianDate.now())
  const cartographic = Cesium.Cartographic.fromCartesian(currentPosition)
  let newLongitude = cartographic.longitude
  let newLatitude = cartographic.latitude
  let newHeight = cartographic.height
  switch (val) {
    case '1':
      // 执行 val 为 '1' 时的逻辑
      newLongitude = cartographic.longitude + 0.0000002
      break
    case '2':
      newLongitude = cartographic.longitude - 0.0000002
      break
    case '3':
      newLatitude = cartographic.latitude - 0.0000002
      break
    case '4':
      newLatitude = cartographic.latitude + 0.0000002
      break
    case '5':
      newHeight = cartographic.height + 1
      break
    case '6':
      newHeight = cartographic.height - 1
      break
    default:
      console.log('无效的输入')
      break
  }
  const newCartographic = new Cesium.Cartographic(newLongitude, newLatitude, newHeight)
  // 将新的 Cartographic 坐标转换回笛卡尔坐标
  const newPosition = Cesium.Cartesian3.fromRadians(newCartographic.longitude, newCartographic.latitude, newCartographic.height)
  selectedEntity.position = new Cesium.CallbackProperty(() => newPosition, false)
  const node = PointsData.value.find(item => item.id === selectedEntity.id)
  if (node) {
    node.lon = Cesium.Math.toDegrees(newCartographic.longitude).toFixed(14)
    node.lat = Cesium.Math.toDegrees(newCartographic.latitude).toFixed(14)
    node.height = newCartographic.height.toFixed(14)
  }
}

// 3. 右键点击事件 显示菜单
function handleRightClick (event) {
  event.preventDefault() // 阻止默认的右键菜单
  if (selectedEntity != null) {
    if (!ischangecamera.value) {
      options.value = [
        { name: '移动', action: prevPage, shortcut: '', shortcutKey: 'alt + arrowup' },
        { name: '删除', action: nextPage, shortcut: '', shortcutKey: 'alt + arrowdown' },
        { name: '编辑', action: changeCameraViwer, shortcut: '', shortcutKey: 'alt + arrowdown' },
      ]
      contextMenu.value.showContextMenu(event)
    } else {
      options.value = [
      //  { name: '移动', action: prevPage, shortcut: '', shortcutKey: 'alt + arrowup' },
      //  { name: '删除', action: nextPage, shortcut: '', shortcutKey: 'alt + arrowdown' },
        { name: '取消编辑', action: changeCameraViwer, shortcut: '', shortcutKey: 'alt + arrowdown' },
      ]
      contextMenu.value.showContextMenu(event)
    }
  } else {
    contextMenu.value.hideContextMenu()
  }
}

// watch(isMenuVisible, (newValue, oldValue) => {
//   console.log('当前的值', oldValue, newValue)
//   if (newValue) {
//     isAllowDrawing.value = true
//   } else {
//     isAllowDrawing.value = false
//   }
// })

// 移动点
function prevPage () {
  isDragging.value = true
}

// 删除点
// function nextPage () {
//   cesium.viewer.entities.remove(selectedEntity)
//   const index = PointsData.value.findIndex(item => item.id === selectedEntity.id)
//   if (index !== -1) {
//     // 删除匹配的记录
//     PointsData.value.splice(index, 1) // 1 表示删除1个元素
//   }
//   isAllowDrawing.value = true
// }

function nextPage () {
  if (!selectedEntity) return

  // 1) 先删 Cesium 实体
  cesium.viewer.entities.remove(selectedEntity)

  // 2) 删表格数据
  const idx = PointsData.value.findIndex(item => item.id === selectedEntity.id)
  if (idx !== -1) PointsData.value.splice(idx, 1)

  // 3) 同步删 sessionStorage 里的“已确认航点”
  try {
    const waypointsInStorage = JSON.parse(sessionStorage.getItem('waypointsData')) || []
    const newList = waypointsInStorage.filter(wp => wp.point_name !== selectedEntity.id)
    sessionStorage.setItem('waypointsData', JSON.stringify(newList))
  } catch (e) {
    // 容错：如果解析失败，直接清空
    sessionStorage.setItem('waypointsData', JSON.stringify([]))
  }

  // 4) 重绘航线（按临时数据）
  updateRoutePolylineFromTemp?.()

  // 5) 收尾
  isAllowDrawing.value = true
  selectedEntity = null
}

// 编辑点位的视角
let airplaneEntity = null
let pyramisPrimitive = null
const value1 = ref(90)
const value2 = ref(-90)
const value3 = ref(0)
const value4 = ref(4) // 默认焦距值 4mm
function changeCameraViwer () { // 进入编辑模式
  // changeModels()// 先更新参数
  if (!ischangecamera.value) {
    const currentPosition = selectedEntity.position.getValue(Cesium.JulianDate.now())
    const Entity = loadGltf(currentPosition)
    airplaneEntity = Entity
    ischangecamera.value = true
    pyramisPrimitive = createPyramis(cesium.viewer, pyramisPrimitive, currentPosition, Math.round(value1.value), Math.round(value2.value), Math.round(value3.value), cameraParams.value.fov)
    childMap.value.setCamera(currentPosition, Math.round(value1.value), Math.round(value2.value), Math.round(value3.value))
    // 进入编辑后立即同步一次
    syncParamsFromSelected()
    loadCaptureModeForSelected()
    loadWaypointSpeedForSelected()
  } else {
    removeGltfModel(cesium.viewer, airplaneEntity)
    removePyramis(cesium.viewer, pyramisPrimitive)
    ischangecamera.value = false
    airplaneEntity = null
    pyramisPrimitive = null
  }
}

function syncParamsFromSelected () {
  if (!selectedEntity) return
  const p = selectedEntity.position.getValue(Cesium.JulianDate.now())
  const c = Cesium.Cartographic.fromCartesian(p)
  cameraParams.value.longitude = +Cesium.Math.toDegrees(c.longitude).toFixed(14)
  cameraParams.value.latitude = +Cesium.Math.toDegrees(c.latitude).toFixed(14)
  cameraParams.value.height = +c.height.toFixed(14)

  // 用当前滑条值当作初始姿态
  cameraParams.value.heading = Math.round(value1.value)
  cameraParams.value.pitch = Math.round(value2.value)
  cameraParams.value.roll = Math.round(value3.value)
}

function loadCaptureModeForSelected () {
  if (!selectedEntity) { captureMode.value = 'ir,visible'; return }
  try {
    const arr = JSON.parse(sessionStorage.getItem('waypointsData')) || []
    const wp = arr.find(w => w.point_name === selectedEntity.id)
    captureMode.value = wp?.capture_mode || 'ir,visible'
  } catch { captureMode.value = 'ir,visible' }
}

function loadWaypointSpeedForSelected () {
  if (!selectedEntity) { waypointSpeed.value = 5; return }
  try {
    const arr = JSON.parse(sessionStorage.getItem('waypointsData')) || []
    const wp = arr.find(w => w.point_name === selectedEntity.id)
    waypointSpeed.value = wp?.speed || 5
  } catch { waypointSpeed.value = 5 }
}

function changeModels () {
  if (selectedEntity == null) {
    return
  }
  const currentPosition = selectedEntity.position.getValue(Cesium.JulianDate.now())
  pyramisPrimitive = createPyramis(cesium.viewer, pyramisPrimitive, currentPosition, Math.round(value1.value), Math.round(value2.value), Math.round(value3.value), cameraParams.value.fov)
  updateOrientation(airplaneEntity, currentPosition, 0.2, 0, 0, 0, Math.round(value1.value) - 90, 0, 0)
  childMap.value.setCamera(currentPosition, Math.round(value1.value), Math.round(value2.value), Math.round(value3.value))
}

watch(() => cameraParams.value.fov, (nv) => { // 监听fov缩放视锥体大小
  // 只在“编辑模式 + 有选中点”时更新
  if (!ischangecamera.value || !selectedEntity) return

  // 合法性 & 夹取

  // 下一帧刷新
  requestAnimationFrame(() => changeModels())
})

// 2025/7/31 xtj 获取相机参数

// // 实时更新相机参数
// 创建一个对象来存储上一次相机参数
const lastCameraParams = {
  heading: null,
  pitch: null,
  roll: null,
  longitude: null,
  latitude: null,
  height: null,
  equivalentFocalLength: null,
  fov: null
}

// 用于更新页面上显示的相机参数
function updateCameraParameters () {
  const camera = cesium.viewer.camera
  const position = camera.position
  const orientation = camera.orientation

  // 获取相机的航向角、俯仰角、翻滚角
  const heading = Cesium.Math.toDegrees(camera.heading)
  const pitch = Cesium.Math.toDegrees(camera.pitch)
  const roll = Cesium.Math.toDegrees(camera.roll)

  // 获取相机的经度、纬度和高度
  const cartographic = Cesium.Cartographic.fromCartesian(position)
  const longitude = Cesium.Math.toDegrees(cartographic.longitude)
  const latitude = Cesium.Math.toDegrees(cartographic.latitude)
  const height = cartographic.height

  // 仅当相机参数发生变化时更新页面
  if (
    heading !== lastCameraParams.heading ||
    pitch !== lastCameraParams.pitch ||
    roll !== lastCameraParams.roll ||
    longitude !== lastCameraParams.longitude ||
    latitude !== lastCameraParams.latitude ||
    height !== lastCameraParams.height
  ) {
    // 更新页面显示的参数
    document.getElementById('longitude').textContent = longitude.toFixed(6)
    document.getElementById('latitude').textContent = latitude.toFixed(6)
    document.getElementById('height').textContent = height.toFixed(2)
    document.getElementById('heading').textContent = heading.toFixed(2)
    document.getElementById('pitch').textContent = pitch.toFixed(2)
    document.getElementById('roll').textContent = roll.toFixed(2)

    // 更新最后的相机参数
    lastCameraParams.heading = heading
    lastCameraParams.pitch = pitch
    lastCameraParams.roll = roll
    lastCameraParams.longitude = longitude
    lastCameraParams.latitude = latitude
    lastCameraParams.height = height
  }
}

function handleConfirm () {
  if (!selected_wayline.value) {
    ElMessage.error('请选择航线!')
    return
  }

  if (!selectedEntity || !selectedEntity.id) {
    ElMessage.error('未选中有效航点!')
    return
  }

  const selectedId = selectedEntity.id

  // 获取当前相机参数（从最新场景中读取）
  const newWaypointData = {
    point_name: selectedId, // 用实体 ID 作为唯一标识
    longitude: cameraParams.value.longitude,
    latitude: cameraParams.value.latitude,
    height: cameraParams.value.height,
    capture_mode: captureMode.value, //
    speed: waypointSpeed.value,
    camera_params: {
      heading: cameraParams.value.heading,
      pitch: cameraParams.value.pitch,
      roll: cameraParams.value.roll,
      // focalLength: cameraParams.value.focalLength,
      focalLength: cameraParams.value.equivalentFocalLength
    }
  }

  // 从 sessionStorage 取出当前航线的航点数据
  let waypointsInStorage = JSON.parse(sessionStorage.getItem('waypointsData')) || []
  const storedWayline = sessionStorage.getItem('selectedWayline')

  // 如果航线不一致，清空数据
  if (storedWayline !== selected_wayline.value) {
    sessionStorage.setItem('waypointsData', JSON.stringify([]))
    sessionStorage.setItem('selectedWayline', selected_wayline.value)
    waypointsInStorage = []
  }

  // 查找是否存在同 ID 的航点
  const existingIndex = waypointsInStorage.findIndex(wp => wp.point_name === selectedId)

  if (existingIndex !== -1) {
    // 覆盖原航点数据
    waypointsInStorage[existingIndex] = newWaypointData
    ElMessage.success('相机参数编辑成功！')
  } else {
    // 新增航点数据
    waypointsInStorage.push(newWaypointData)
    ElMessage.success('相机参数已保存到临时数据中！')
  }

  // 更新存储
  sessionStorage.setItem('waypointsData', JSON.stringify(waypointsInStorage))

  updateRoutePolylineFromTemp()// 更新线段

  console.log('当前航线数据：', waypointsInStorage)
}

const currentWaylineName = computed(() => {
  if (manualInput.value) {
    return newWayline.value?.trim()
  }

  const selected = wayline_list.value.find(item => item.value === selected_wayline.value)
  return selected?.label?.trim() || ''
})

async function handlegenerate () {
  try {
    const waypointsData = JSON.parse(sessionStorage.getItem('waypointsData')) || []

    if (!waypointsData.length) {
      ElMessage.error('没有可用的航点数据!')
      return
    }

    const name = currentWaylineName.value
    if (!name) {
      ElMessage.error('航线名称为空！')
      return
    }

    const data = await generateWaylineRequest(waypointsData, name)

    ElMessage.success('航线生成成功！')
    console.log('生成的航线数据:', data)
  } catch (error) {
    console.error('生成航线失败的错误:', error)
    ElMessage.error('生成航线失败')
  }
}

async function handleplanning () { // 规划航线（避障）
  const arr = JSON.parse(sessionStorage.getItem('waypointsData')) || []
  if (arr.length < 2) return ElMessage.error('没有可用的航点')
  if (!voxel) return ElMessage.error('体素未加载成功')

  let i = 0
  while (i < arr.length - 1) {
    const A = arr[i]; const B = arr[i + 1]
    const res = checkAndInsertAvoidanceBetweenTwoPoints({
      header: voxel.header,
      occ: voxel.occ,
      ptA: { longitude: +A.longitude, latitude: +A.latitude, height: +A.height },
      ptB: { longitude: +B.longitude, latitude: +B.latitude, height: +B.height },
      marginVox: 10, // 体素余量，可根据 voxelSize 调
    })
    if (res.collided && res.avoidLLH) {
      arr.splice(i + 1, 0, {
        point_name: `avoid_${Date.now()}`,
        longitude: res.avoidLLH.longitude.toFixed(14),
        latitude: res.avoidLLH.latitude.toFixed(14),
        height: res.avoidLLH.height.toFixed(14),
        camera_params: {}
      })
      ElMessage.info(`第 ${i + 1} 段插入避障点`)
    }
    i += 1
  }

  sessionStorage.setItem('waypointsData', JSON.stringify(arr))
  updateRoutePolylineFromTemp?.()
  ElMessage.success('A* 路径规划完成')
  console.log(sessionStorage)
  // 之后继续原来的 generateWaylineRequest(...) 流程
}

function updateRoutePolylineFromTemp () {
  const arr = JSON.parse(sessionStorage.getItem('waypointsData')) || []
  if (!arr.length) {
    if (routeEntity) { cesium.viewer.entities.remove(routeEntity); routeEntity = null }
    return
  }
  const positions = arr.map(wp =>
    Cesium.Cartesian3.fromDegrees(+wp.longitude, +wp.latitude, +wp.height)
  )
  createOrUpdateFlowArrowRoute(positions)
}

// 注册一个自定义材质：PolylineTrail（沿线流动的箭头贴图）
function registerPolylineTrailMaterial () {
  if (Cesium.Material.PolylineTrailType) return

  Cesium.Material.PolylineTrailType = 'PolylineTrail'
  Cesium.Material.PolylineTrailSource = `
  czm_material czm_getMaterial(czm_materialInput materialInput)
  {
    czm_material material = czm_getDefaultMaterial(materialInput);

    // s: 沿线0~1，t: 线宽方向
    float s = materialInput.st.s;

    // 外部传入的 repeat(密度) 和 time(0~1偏移)
    vec2 st = vec2(fract(s * repeat + time), materialInput.st.t);
    vec4 tex = texture(image, st);

    material.alpha   = tex.a * color.a;
    material.diffuse = tex.rgb * color.rgb;
    return material;
  }`

  Cesium.Material._materialCache.addMaterial(Cesium.Material.PolylineTrailType, {
    fabric: {
      type: Cesium.Material.PolylineTrailType,
      uniforms: {
        image: '',
        color: new Cesium.Color(0.0, 1.0, 1.0, 0.95),
        time: 0.0,
        repeat: 7.0
      },
      source: Cesium.Material.PolylineTrailSource
    },
    translucent: () => true
  })
}

class FlowPolylineTrailMaterialProperty {
  constructor (options = {}) {
    this._definitionChanged = new Cesium.Event()
    this.color = Cesium.defaultValue(options.color, Cesium.Color.CYAN.withAlpha(0.95))
    this.image = options.image // 贴图
    this.repeat = Cesium.defaultValue(options.repeat, 6.0) // 箭头密度(小=箭头大)
    this.speed = Cesium.defaultValue(options.speed, 0.8) // 流速(可负数表示反向)
    this._t0ms = performance.now()
  }

  get isConstant () { return false }
  get definitionChanged () { return this._definitionChanged }
  getType (time) { return 'PolylineTrail' }
  getValue (time, result) {
    if (!Cesium.defined(result)) result = {}
    const seconds = (performance.now() - this._t0ms) / 1000.0
    result.image = this.image
    result.color = this.color
    result.repeat = this.repeat
    result.time = (seconds * this.speed) % 1.0 // 0~1 循环位移
    return result
  }

  equals (other) {
    return other instanceof FlowPolylineTrailMaterialProperty &&
           other.image === this.image &&
           Cesium.Color.equals(other.color, this.color) &&
           other.repeat === this.repeat &&
           other.speed === this.speed
  }
}

function createOrUpdateFlowArrowRoute (positions) {
  registerPolylineTrailMaterial()

  const mat = new FlowPolylineTrailMaterialProperty({
    image: new URL('../../assets/arrow_flow.png', import.meta.url).href, // 你的箭头纹理
    color: Cesium.Color.CYAN.withAlpha(0.95),
    repeat: 5.0, // 箭头疏密(调小=箭头更大)
    speed: -0.9 // 流速(负数=反向)
  })

  if (!routeEntity) {
    routeEntity = cesium.viewer.entities.add({
      name: 'wayline-flow-arrow',
      polyline: {
        positions,
        width: 13, // ↖️ 加粗，明显很多
        material: mat,
        clampToGround: false
      }
    })
  } else {
    routeEntity.polyline.positions = positions
    routeEntity.polyline.material = mat
  }
}

</script>

<style lang="scss" scoped>
// 遥控器
.control-c{
  width: 102px;
  height: 102px;
  border-radius: 50%;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #2f2f30 0%, #1f1f1f 100%);
  border: solid 2px #0a0a0a;
  .cross-c{
    width: 102px;
    height: 102px;
    display: flex;
    flex-wrap: wrap;
    transform: rotate(45deg);
    position: absolute;
    left: 50%;
    top: 50%;
    margin-left: -51px;
    margin-top: -51px;
  }
}
.container{
  display: flex;
  width: 100%;
  padding: 10px;
  height: calc(100vh - 100px)
}
.left {
  // background-color: #f0f0f0; /* 左侧背景颜色 */
  background: #0a2257; /* 左侧背景颜色 */
  background-size: 100% 100%;
  background-repeat: no-repeat;
  transition: width 0.3s ease;
  position: relative; /* 使伪元素相对于父元素定位 */
  display: flex;
  flex-direction: column; /* 垂直排列子元素 */
  .layout-arrow {
    position: absolute;
    top: calc(50% + 10px) ;
    right: 0;
    transform: translateY(-50%);
    z-index: 10;
  }
  .box1{
    flex-grow: 0;
    margin: 10px 20px 10px 10px;
    .demo-tabs :deep(.el-tabs__item) {
      color: #ffffff;
      font-weight: 600;
      width: 185px;
    }
    .demo-tabs :deep(.el-tabs__content) {
      background-color: #0f327f;
    }
    .demo-tabs :deep(.el-tabs__header ){
      // background-color:transparent; /* 选中标签背景颜色 */
      background-color:#0f327f;
    }
    .demo-tabs :deep(.el-tabs__header .el-tabs__item.is-active ){
      background-color: #1299C3; /* 选中标签背景颜色 */
      color: #fff; /* 选中标签文字颜色 */
      border-radius: 4px; /* 选中标签圆角 */
    }
    .operation{
      margin-top: 10px;
      .item{
        padding: 10px;
        color: #ffffff;
        font-weight: 500;
        .select-operation{
          :deep(.el-select__placeholder){
              font-size: 14px;
              font-weight: 500;
              color: #ffffff;
          }
          :deep(.el-select__wrapper) {
            background: rgba(59, 116, 255, 0.15);
            box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
            border: 1px solid #719fff;
            border-radius: 4px;
            width: 100%;
            height: 30px;
            // padding-left: 5px;
          }
        }
        .btn{
            background: rgba(59, 116, 255, 0.15);
            -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
            box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
            border: 1px solid #719fff;
            border-radius: 4px;
            width: 100%;
            height: 30px;
            font-family: PingFangSC, PingFang SC;
            font-weight: 500;
            font-size: 14px;
            color: #ffffff;
            &:hover{
              background: skyblue;
            }

          }
      }
    }
  }

  .box2{
    margin: 10px 20px 10px 10px;
    height: 100%;
    padding: 10px;
    position: relative;
    flex-grow: 1;

    // width: 100%;
    background-color: #0f327fa0;
    &::before{
      content: '';
      position: absolute;
      height: 35px;
      top: 0;
      left: 0;
      border-left: 3px solid #1299C3;

    }
    .logs-title{
      color: #ffffff ;
      font-weight: 600;
      height: 20px;
      // z-index: 1000;
    }
  }
}
.left::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("../../assets/v3/icon/right-indent.png");
  background-size: 100% 100%;
  background-repeat: no-repeat;
  transform: rotate(180deg); /* 旋转180度 */
  pointer-events: none; /* 确保点击事件不干扰 */
  opacity:.15;
}

.middle {
    // flex-grow: 1; /* 右侧占满剩余空间 */
    width: calc(100vw - 800px);
    background-color: #d9d9d9; /* 右侧背景颜色 */
    position: relative;
    // padding: 20px; /* 右侧内边距 */
    .box3 {
      position: absolute;
      height: 100%;
      width: 100%;
    }

    #cesiumContainer {
      position: absolute;
      height: 100%;
      width: 100%;
    }

    #threeContainer {
      position: absolute;
      height: 100%;
      width: 100%;
      margin: 0%;
      z-index: 10;
      pointer-events: none;
    }
    .show-info {
        position: absolute;
        width: 200px;
        height: 100px;
        color: aliceblue;
        font-size: 16px;
        top: 10px; /* 距离顶部的距离 */
        left: 10px; /* 距离左边的距离 */
        background: rgba(36, 126, 31, 0.8); /* 背景透明度 */
        padding: 5px;
        border-radius: 4px;
        z-index: 20; /* 确保在其他元素上方 */
    }
}
.right{
  width: 400px;
  background: #0a2257;
  .camera-box{
    margin: 10px 20px 10px 10px;
    width: 390px;
    height: 292.5px;
    border: 1px solid darkgray;
  }
}
::v-deep .el-table tr {
  background-color: #011C4B !important;
  /* opacity: 0.6; */
  color: #F1F6FF;
  font-weight: bold;
}

// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #2264a7;
}

::v-deep .el-table td {
  border: 2px solid #01123288;
  /* 设置列的边框颜色和粗细 */
  font-size: 16px;
  font-weight: 500;
}

// 表格样式
::v-deep .el-table {
  .cell {
    text-align: center;
  }
}

// // 表头大小
::v-deep .el-table th {
  height: 50px;
  font-size: 16px !important;
  /* 如果你需要修改表头字体大小，设置一个不同的大小 */
  color: rgba(255, 255, 255, 1);
  background-color: #00399A;
  border-left: 2px #01123288 solid;
  border-bottom: 2px #01123288 solid !important;
}

/* // 修改高亮当前行颜色 */
::v-deep .el-table tbody tr:hover>td {
  background: rgba(0, 114, 245, 0.6) !important;
}

/* // 斑马线颜色 */

::v-deep .el-table--striped .el-table__body tr.el-table__row--striped td {
  background: rgba(0, 45, 120, 1);
}

/* 移除表格行的底部边框 */
::v-deep .el-table__body-wrapper .el-table__row {
  border-bottom: none !important;
  /* 或者： */
  box-shadow: none !important;
}

/* 如果仍然看到白色线条，可能是阴影效果的原因 */
::v-deep .el-table__body-wrapper .el-table__row td {
  border-bottom: none !important;
}

// 表格最后一条白线
:deep .el-table__inner-wrapper::before {
  height: 0;
}
//----------------------------------------------xtj
.camera-info-container {
  margin: 12px;
  font-size: 13px;       /* 字体小一点 */
  color: #f0f0f0;
}

.info-title {
  font-weight: 600;
  margin-bottom: 6px;
  font-size: 14px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 两列 */
  gap: 4px 12px;  /* 行间距4px，列间距12px */
}

.info-item span {
  color: #9ecbff;        /* 值用浅蓝色突出 */
}

.full-span {
  grid-column: 1 / span 2; /* 跨两列显示 */
}

.confirm-button {
  background-color: #4CAF50; /* 背景色为绿色 */
  color: white; /* 字体颜色为白色 */
  padding: 12px 30px; /* 增加上下和左右的内边距，按钮更宽 */
  border: none; /* 去掉默认边框 */
  border-radius: 5px; /* 圆角 */
  font-size: 16px; /* 字体大小 */
  cursor: pointer; /* 鼠标悬停时显示为指针 */
  transition: background-color 0.3s; /* 添加过渡效果 */
  display: block; /* 使按钮成为块级元素 */
  margin: 20px auto; /* 上下有空隙，左右自动居中 */
  width: 200px; /* 设置按钮的固定宽度 */
}

.confirm-button:hover {
  background-color: #45a049; /* 鼠标悬停时的背景色 */
}
</style>
