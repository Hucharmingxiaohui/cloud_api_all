
<template>
    <div id="cesiumBox" style="width: 100%;height: 100%; position: relative;">
        <div style="position: absolute; right: 0; bottom: 0; z-index: 999; margin-right: 30px; margin-bottom: 40px;">
            <el-slider v-model="zoomScale" :min="1" vertical height="150px" :marks="marks" @input="setCameraFocalLength"/>
        </div>
        <div style="position: absolute; right: 0; bottom: 0; z-index: 999; margin-right: 80px; margin-bottom: 10px;">
            <!-- <el-button @click="changeWideAngle(120)">广角</el-button> -->
            <el-radio-group v-model="cameraradio" size="large" @change="changeWideAngle">
                <el-radio-button label="默认" value="60" />
                <el-radio-button label="广角" value="120" />
            </el-radio-group>
        </div>
    </div>
</template>
<script setup>

import 'cesium/Build/Cesium/Widgets/widgets.css'
import * as Cesium from 'cesium'
import { reactive, onMounted, defineExpose, ref, computed, onBeforeUnmount, watch, defineEmits } from 'vue'
import { removeLogo, load3DTilesModels, computeCameraChangeDis } from './cesiumTools'
import { CreateFrustum } from './CreateFrustum'

const emit = defineEmits()

const viewer = ref(null)

onMounted(() => {
  initCesiumMap()
  startRenderLoop()
  cesium.viewer.camera.changed.addEventListener(() => {
    updateCameraParameters()
  })
})
// -----------------------------------------------场景--------------------------------------------------
const cesium = {
  viewer: null,
}

// 初始化场景
function initCesiumMap () {
  Cesium.Ion.defaultAccessToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiI3ZmJjODE1Yy1kMjU4LTQyZTgtODAyZC1mNzE2MDNhMmQ3YzUiLCJpZCI6MTk5NzQwLCJpYXQiOjE3MDk2Mjg5Mjh9.GuRbyEbm8FknaFOM34kGm9wCbf2XVjp873h_QD-Vs7A'
  cesium.viewer = new Cesium.Viewer('cesiumBox', {
    useDefaultRenderLoop: false,
    selectionIndicator: true,
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
    // imageryProviderViewModels: [], // 不使用默认地图
    geocoder: false, // 不显示地理编码器
    homeButton: false, // 不显示“Home”按钮
    sceneMode: Cesium.SceneMode.SCENE3D, // 直接使用3D模式
    animation: false, // 左下角时间表盘
  })
  cesium.viewer.scene.screenSpaceCameraController.enableZoom = false// 禁用鼠标滚轮

  cesium.viewer._cesiumWidget._creditContainer.style.display = 'none' // 隐藏logo版权
  if (Cesium.FeatureDetection.supportsImageRenderingPixelated()) {
    //  判断是否支持图像渲染像素化处理
    cesium.viewer.resolutionScale = window.devicePixelRatio
  }
  cesium.viewer.scene.postProcessStages.fxaa.enabled = true
  cesium.viewer.scene.debugShowFramesPerSecond = false // 显示帧率
}

// 地图渲染
function startRenderLoop () {
  cesium.viewer.render()
  requestAnimationFrame(startRenderLoop)
}

// 加载模型
function loadModel (tilesetUrl) {
  load3DTilesModels(cesium.viewer, tilesetUrl)
}

// -----------------------------------------------相机操作--------------------------------------------------
// 设置主相机视锥体参数
function setCamera (position, heading, pitch, roll, focalLengthMM = null) {
//   const camera = new Cesium.Camera(cesium.viewer.scene)
//   camera.frustum.fov = Cesium.Math.PI_OVER_THREE
//   camera.frustum.near = 1.0
//   camera.frustum.far = 2000.0
//   camera.setView({
//     destination: Cesium.Cartesian3.fromDegrees(120, 36, 2500)
//   })
//   const primitive = new Cesium.DebugCameraPrimitive({
//     camera: camera,
//     color: Cesium.Color.RED,
//     show: true,
//     updateOnchange: true
//   })
//   cesium.viewer.scene.primitives.add(primitive)
  // 应用到主相机
  cesium.viewer.camera.setView({
    destination: position,
    orientation: {
      heading: Cesium.Math.toRadians(heading),
      pitch: Cesium.Math.toRadians(pitch),
      roll: Cesium.Math.toRadians(roll),
    }

  })

//   cesium.viewer.camera.flyTo({
//     destination: Cesium.Cartesian3.fromDegrees(120, 36, 10000)
//   })
}
function createPoint (p) {
  return cesium.viewer.entities.add({
    position: p,
    point: {
      pixelSize: 10,
      color: new Cesium.Color(1.0, 1.0, 0.0, 1.0),
    },
  })
}

function createFrame (p) {
  // X轴：红色，Y轴：绿色，Z轴：蓝色
  const modelMatrix = Cesium.Transforms.eastNorthUpToFixedFrame(p)
  cesium.viewer.scene.primitives.add(
    new Cesium.DebugModelMatrixPrimitive({
      modelMatrix: modelMatrix,
      length: 30000.0,
      width: 3.0,
    })
  )
}

const zoomScale = ref(1)
let prevZoomScale = 1
const zoomSensitivity = ref(1)
const marks = reactive({
  1: {
    style: {
      color: '#FFFFFF',
    },
    label: '1X',
  },
  100: {
    style: {
      color: '#FFFFFF',
    },
    label: '120X',
  },
})
// 设置相机的变焦系数
function setCameraDis (val) {
  //   cesium.viewer.camera.move(cesium.viewer.camera.direction, val)
  computeCameraChangeDis(cesium.viewer, zoomScale.value, prevZoomScale, zoomSensitivity.value)
  prevZoomScale = zoomScale.value
}

function setCameraFocalLength (val) {
  if (cesium.viewer?.camera.frustum instanceof Cesium.PerspectiveFrustum && val > 0) {
    const sensorHeightMM = 4.8 // CMOS 高度，单位 mm
    const fovRad = 2 * Math.atan(sensorHeightMM / (2 * val)) // 焦距转FOV
    cesium.viewer.camera.frustum.fov = fovRad
    console.log(`焦距已设置为 ${val}mm，对应视角 FOV 为 ${Cesium.Math.toDegrees(fovRad).toFixed(2)}°`)
  }

  updateCameraParameters()
}

const cameraradio = ref(60)
// 设置相机的广角
function changeWideAngle (angle) {
  if (cesium.viewer.camera.frustum instanceof Cesium.PerspectiveFrustum) {
    cesium.viewer.camera.frustum.fov = Cesium.Math.toRadians(angle)
  }
}

// 2025/7/31 xtj 子组件暴露相机参数给父页面
function updateCameraParameters () {
  const camera = cesium.viewer.camera
  const position = camera.position
  const orientation = camera.orientation

  const heading = Cesium.Math.toDegrees(camera.heading)
  const pitch = Cesium.Math.toDegrees(camera.pitch)
  const roll = Cesium.Math.toDegrees(camera.roll)

  const cartographic = Cesium.Cartographic.fromCartesian(position)
  const longitude = Cesium.Math.toDegrees(cartographic.longitude)
  const latitude = Cesium.Math.toDegrees(cartographic.latitude)
  const height = cartographic.height

  // --------- 焦距 focalLength 计算部分 ---------
  const fov = camera.frustum.fov // 弧度
  const sensorHeightMM = 7.0 // 你当前设置的 CMOS 垂直尺寸 mm
  const sensorDiagonalMM = 9.6 // 假设对角线为 8.0mm，可换成 10.0、12.0 等

  const focalLengthMM = sensorHeightMM / (2 * Math.tan(fov / 2)) // 实际焦距
  const equivalentFocalLengthMM = focalLengthMM * (43.3 / sensorDiagonalMM) // 等效焦距

  const cameraParams = {
    heading,
    pitch,
    roll,
    longitude,
    latitude,
    height,
    fov,
    focalLength: focalLengthMM.toFixed(2), // 加入焦距，保留 2 位小数
    equivalentFocalLength: equivalentFocalLengthMM.toFixed(1)
  }

  console.log(cameraParams)
  emit('cameraUpdated', cameraParams)
}
// xtj
// function setFocalLength (val) {
//   if (val > 0 && cesium.viewer?.camera.frustum instanceof Cesium.PerspectiveFrustum) {
//     const sensorHeightMM = 4.8 // CMOS 高度，单位 mm
//     const fovRad = 2 * Math.atan(sensorHeightMM / (2 * val))
//     cesium.viewer.camera.frustum.fov = fovRad
//     console.log(`焦距设置为 focalLength=${val}mm，对应 fov=${Cesium.Math.toDegrees(fovRad).toFixed(2)}°`)
//   }
// }
// -----------------------------------------------相机操作--------------------------------------------------

// 暴露方法给父组件
defineExpose({
  loadModel,
  setCamera,
  // setFocalLength,
  viewer: cesium.viewer,
})

// xtj
// 添加此行避免 ESLint 报错

</script>
<style lang="scss" scoped>

</style>
