<template>
 <div id="cesiumContainer" style="width: 100%;height: 100%; position: relative;">
 <!-- <stateAndLive/> -->
  <div>
    <div class="liveview" v-if="livestream.visible" >
        <div class="title1">
          <div class="thumbnail_1"></div>
          <div class="text_title">监控直播</div>
        </div>
        <a style="position: absolute; right: 10px; top: 10px; font-size: 16px; color: white;" @click="closeLivestream"><CloseOutlined /></a>
        <div style="display: flex; align-items: center;margin: 20px 0 0 10px">
          <el-button class="btn" @click="toggleDroneVideo">无人机视频</el-button>
          <el-button class="btn" @click="toggleDockVideo">机场视频</el-button>
        </div>
        <div  v-if="showDockLive" style="padding: 10px 20px 10px 10px; height: 600px;" class="video_style">
          <LivestreamDock :sn = "livestream.dock_sn"></LivestreamDock>
        </div>
        <div v-if="showLive" style="padding: 10px 20px 10px 10px;  height: 600px;" class="video_style">
          <LivestreamOthers :sn = "livestream.dorne_sn"></LivestreamOthers>
        </div>
    </div>
  </div>
  </div>
</template>

<script setup>
import controlPanel from '../control/ControlDegree.vue'
import 'cesium/Build/Cesium/Widgets/widgets.css'
import { getLocation, getWayPointInfo } from '/@/api/wayline'
import * as Cesium from 'cesium'
import { reactive, onMounted, ref, computed, onBeforeUnmount, watch } from 'vue'
import { EDeviceTypeName, ELocalStorageKey, ERouterName } from '/@/types'
import { importModelFile, getModelInfoByName, getAllModels, deleteModel } from '/@/api/model'
import { gcj02towgs84, wgs84togcj02 } from '/@/vendors/coordtransform'
import { AMapImageryProvider, BaiduImageryProvider, GeoVisImageryProvider } from '@cesium-china/cesium-map'
import * as THREE from 'three'
import * as math from 'mathjs'
import ContextMenu from './Menu.vue'
import axios from 'axios'
import { renderBillboard } from './drawBillboard'
import { drawLine, drawPoints, drawBoundingBox, removeLogo, load3DTilesModels, loadGltfModel, removeGltfModel, createPyramis, removePyramis } from './cesiumTools'
import { CreateFrustum } from './CreateFrustum'
import { message } from 'ant-design-vue'
import UavCamera from './uavCamera.vue'
import { useMyStore } from '/@/store'
import stateAndLive from '/@/components/g-map/DeviceAndLive.vue'
import {
  BorderOutlined, LineOutlined, CloseOutlined, ControlOutlined, TrademarkOutlined, ArrowDownOutlined,
  ThunderboltOutlined, SignalFilled, GlobalOutlined, HistoryOutlined, CloudUploadOutlined, RocketOutlined,
  FieldTimeOutlined, CloudOutlined, CloudFilled, FolderOpenOutlined, RobotFilled, ArrowUpOutlined, CarryOutOutlined
} from '@ant-design/icons-vue'
const cesium = {
  viewer: null,
}
const store = useMyStore()
let airplaneEntity = null
onMounted(() => {
  const store = useMyStore()
  // livestream.value.visible = true
  // store.commit('SET_LIVESTREAM_OTHERS_VISIBLE', livestream)
  init()
})
const value1 = ref(0)
const value2 = ref(0)
const value3 = ref(0)
const heading = ref(0)
const pitch = ref(0)
const roll = ref(0)
// ------------------------------------------------------视频直播信息-----------------------------
const liveStream = ref('dock')
const livestream = computed(() => {
  return store.state.liveStream
})
const showLive = ref(true)
const showDockLive = ref(false)
const toggleDroneVideo = () => {
  showDockLive.value = false
  showLive.value = !showLive.value
}
// Function to close the video window
const closeVideo = () => {
  showLive.value = false
}
// 机场视频------------------------------------------------------
const toggleDockVideo = () => {
  showLive.value = false
  showDockLive.value = !showDockLive.value
}
const closeDockVideo = () => {
  showDockLive.value = false
}

function closeLivestream () {
  livestream.value.visible = false
  store.commit('SET_LIVESTREAM_OTHERS_VISIBLE', livestream)
}
// -----------------------------------------------------三维无人机实景显示-------------------
function init () {
  initCesium()
  startRenderLoop()
  load3DTiles()

  const pos = { longitude: 121.365617, latitude: 37.518652, height: 116.27 }
  const position = WGS84toCartesian(pos)
  airplaneEntity = loadGltf(position)
  setTimeout(() => {
    // drawBoundingBox(cesium.viewer, airplaneEntity, position)
  }, 800)

  const newPosition = { longitude: 121.365617, latitude: 37.518652, height: 116.27 }
}

function test () {
  // const newPosition = Cesium.Cartesian3.fromDegrees(121.366218, 37.519314, 112.98)
  // const offsetPosition = Cesium.Cartesian3.fromElements(
  //   newPosition.x + value1.value,
  //   newPosition.y + value2.value,
  //   newPosition.z + value3.value // 假设newPosition是Cartesian3类型，z坐标增加10米
  // )
  // cesium.viewer.camera.flyTo({
  //   destination: offsetPosition,
  //   orientation: {
  //     heading: heading.value,
  //     pitch: pitch.value,
  //     roll: roll.value
  //   }
  // })
  const newPosition = Cesium.Cartesian3.fromDegrees(121.366218, 37.519314, 112.98)
  const currentPosition = cesium.viewer.camera.position // 当前相机位置（Cartesian3）
  const currentHeading = cesium.viewer.camera.heading // 当前朝向（弧度）
  const currentPitch = cesium.viewer.camera.pitch // 当前俯仰角（弧度）
  const currentRoll = cesium.viewer.camera.roll // 当前翻滚角（弧度）
  console.log('目标位置', newPosition)
  console.log('相机位置', currentPosition)
  console.log('heading', currentHeading)
  console.log('currentPitch', currentPitch)
  console.log('currentRoll', currentRoll)
}

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
  cesium.viewer.render()
  requestAnimationFrame(startRenderLoop)
}

function load3DTiles () {
  // const tilesetUrl = 'http://172.20.63.157:9000/models/dfelanqiuchang/tileset.json'
  const tilesetUrl = '/model/dfelanqiuchang/tileset.json'
  load3DTilesModels(cesium.viewer, tilesetUrl)
}
// 加载glft模型
function loadGltf (position) {
  const url = '/model/gltf/airplane.glb'
  return loadGltfModel(cesium.viewer, url, position)
}

// WGS84转笛卡尔
function WGS84toCartesian (pos) {
  // { longitude: 121.365068, latitude: 37.518005, height: 98.86 }
  const { longitude, latitude, height } = pos
  const cartesian = Cesium.Cartesian3.fromDegrees(longitude, latitude, height)
  return cartesian
}

// -------------------------------------------------------------实时信息---------

const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)
// const waylineId = '32a91acc-489d-4ee1-829a-2e56e61c4027'

// function tt () {
//   cesium.viewer.entities.removeAll()

//   // 坐标系转换 大地 => 高德 将字符串转换为对象，并为每个点添加高度20
// }
// -----------------------------------------------------监听实时位置------------------------------------------
/**
 * @description: 监听航线选择
 * @param waylineInfo store更新选中航线参数
 * */
const waylineInfo = ref(null)
watch(
  () => store.state.waylineInfo,
  newData => {
    // cesium.viewer.entities.removeAll()
    drawWayline(newData.id)
    // removeAllWayline()
    // getWatlineInfo(newData.id)
    // showWayline(waylineInfo.value)
  },
  {
    deep: true
  }
)

/**
 * @description:  监听设备信息
 */
watch(() => store?.state.deviceState, data => {
  // 1. 检查设备数据是否存在
  const deviceId = data.currentSn // 或使用 data.currentSn（需确认是否一致）
  const currentDevice = data?.deviceInfo?.[deviceId]

  if (!currentDevice) {
    console.warn('设备数据未加载')
    return
  }

  // 2. 安全访问属性（可选链 ?.）
  console.log('当前的姿态信息',
    currentDevice?.attitude_head,
    currentDevice?.attitude_pitch,
    currentDevice?.attitude_roll
  )
  const newPosition = {
    longitude: currentDevice?.longitude, // ✅ 正确访问属性
    latitude: currentDevice?.latitude,
    height: currentDevice?.height
  }
  console.log('当前的姿态信息',
    newPosition
  )
  updateOrientation(airplaneEntity, 0.2, newPosition, currentDevice?.attitude_head - 90, currentDevice?.attitude_pitch, currentDevice?.attitude_roll)
}, { deep: true })

const options = {
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
// 绘制任务航线
function drawWayline (waylineId) {
  getWayPointInfo(workspaceId, waylineId).then(res => {
    if (res.code !== 0) {
      return
    }
    let tempWayPoint = []
    console.log(res.data)
    tempWayPoint = res.data.map((point, index) => {
      const [longitude, latitude, height] = point[0].split(',').map(Number)
      options.text = '航点' + (index + 1)
      drawPoints(cesium.viewer, options, longitude, latitude, height)
      return { longitude, latitude, height }
    })
    drawLine(tempWayPoint, cesium.viewer)
  })
}

function updateCamera (newPosition) {
  const offsetPosition = Cesium.Cartesian3.fromElements(
    newPosition.x - 15,
    newPosition.y + 28,
    newPosition.z - 23 // 假设newPosition是Cartesian3类型，z坐标增加10米
  )
  cesium.viewer.camera.flyTo({
    destination: offsetPosition,
    orientation: {
      heading: 0.1,
      pitch: -0.2,
      roll: 6.3
    }
  })
}

function updateOrientation (model, scale, newGeoPosition, heading, pitch, roll) {
  if (!model) {
    console.error('模型实体不可用 ')
    return
  }
  // 设置模型的缩放比例
  model.scale = new Cesium.ConstantProperty(scale)

  // 将新的地理坐标转换为笛卡尔坐标
  const newPosition = Cesium.Cartesian3.fromDegrees(
    newGeoPosition.longitude,
    newGeoPosition.latitude,
    newGeoPosition.height
  )
  // 获取模型的当前位置
  const initialPosition = model.position.getValue(Cesium.JulianDate.now())

  // 计算移动的方向向量
  const direction = Cesium.Cartesian3.subtract(newPosition, initialPosition, new Cesium.Cartesian3())
  const distance = Cesium.Cartesian3.magnitude(direction)

  // 如果距离非常小，直接更新位置和姿态
  if (distance < 1.0) {
    model.position = new Cesium.ConstantPositionProperty(newPosition)
    updateModelOrientation(model, newPosition, heading, pitch, roll)
    updateCamera(newPosition)
    return
  }

  // 设置移动的持续时间（单位：秒）
  const duration = 1 // 1 秒

  // 记录开始时间
  const startTime = Cesium.JulianDate.now()

  // 定义更新函数
  const updateFunction = () => {
    const currentTime = Cesium.JulianDate.now()
    const elapsedTime = Cesium.JulianDate.secondsDifference(currentTime, startTime)

    if (elapsedTime >= duration) {
      // 移动完成，直接设置最终位置和姿态
      model.position = new Cesium.ConstantPositionProperty(newPosition)
      updateModelOrientation(model, newPosition, heading, pitch, roll)
      updateCamera(newPosition)
      return
    }

    // 计算当前的位置
    const t = elapsedTime / duration
    const currentPosition = Cesium.Cartesian3.lerp(
      initialPosition,
      newPosition,
      t,
      new Cesium.Cartesian3()
    )

    // 更新模型的位置
    model.position = new Cesium.ConstantPositionProperty(currentPosition)

    // 更新相机位置（使用当前插值位置）
    updateCamera(currentPosition)

    // 继续更新
    requestAnimationFrame(updateFunction)
  }

  // 开始更新
  updateFunction()
}

// 辅助函数：更新模型的姿态
function updateModelOrientation (model, position, heading, pitch, roll, duration = 0.8) {
  // 将角度转换为弧度
  const headingVal = Cesium.Math.toRadians(heading)
  const pitchVal = Cesium.Math.toRadians(pitch)
  const rollVal = Cesium.Math.toRadians(roll)

  // 计算目标姿态
  const targetHpr = new Cesium.HeadingPitchRoll(headingVal, pitchVal, rollVal)
  const targetOrientation = Cesium.Transforms.headingPitchRollQuaternion(position, targetHpr)

  // 获取当前姿态
  let currentOrientation
  if (model.orientation && model.orientation.getValue) {
    currentOrientation = model.orientation.getValue(Cesium.JulianDate.now())
  } else {
    // 如果 orientation 未初始化，设置为默认值
    currentOrientation = Cesium.Quaternion.IDENTITY // 默认无旋转
    model.orientation = new Cesium.ConstantProperty(currentOrientation)
  }

  // 记录开始时间
  const startTime = Cesium.JulianDate.now()

  // 定义更新函数
  const updateFunction = () => {
    const currentTime = Cesium.JulianDate.now()
    const elapsedTime = Cesium.JulianDate.secondsDifference(currentTime, startTime)

    if (elapsedTime >= duration) {
      // 时间到，直接设置最终姿态
      model.orientation = new Cesium.ConstantProperty(targetOrientation)
      return
    }

    // 计算插值比例
    const t = elapsedTime / duration

    // 使用球面线性插值（SLERP）计算当前姿态
    const currentQuaternion = Cesium.Quaternion.slerp(
      currentOrientation,
      targetOrientation,
      t,
      new Cesium.Quaternion()
    )

    // 更新模型的姿态
    model.orientation = new Cesium.ConstantProperty(currentQuaternion)

    // 继续更新
    requestAnimationFrame(updateFunction)
  }

  // 开始更新
  updateFunction()
}
</script>

<style lang="scss" scoped>
// 视频直播
.liveview {
  position: absolute;
  color: #fff;
  z-index: 1;
  left: 0;
  margin-left: 10px;
  top: 10px;
  text-align: center;
  width: 800px;
  height: 700px;
  // background: #232323;
  background:#0A204B;
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  .title1{
    width: 480px;
    height: 37px;
    background: url('/@/assets/v4/dialog_live.png') 100% no-repeat;
    background-size: 100% 100%;

    display: flex;          /* 启用 flexbox */
    justify-content: left;  /* 水平左对齐 */
    align-items: center;    /* 垂直居中对齐 */
    padding-left: 10px;     /* 根据需要添加左内边距 */

    .thumbnail_1{
      width: 14px;
      height: 12px;
      margin-right: 15px;
      background: url('/@/assets/v4/dialog_title_icon.png') 100% no-repeat;
      background-size: 100% 100%;
    }
    .text_title{
      text-shadow: 0px 0px 4px rgba(201, 252, 255, 0.41);
      background-image: linear-gradient(
        180deg,
        rgba(255, 255, 255, 1) 0,
        rgba(144, 201, 255, 1) 100%
      );
      font-size: 14px;
      font-family: Google Sans-Medium;
      font-weight: 500;
      text-align: justified;
      white-space: nowrap;
      line-height: 14px;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;

    }
  }
  .btn {
    border: 2px solid #1299c3;
    background: linear-gradient(to top, #11b4fb, #023956);
    color: rgba(255, 255, 255, 0.762);
  }
}
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
width: 800px;
padding: 10px;
height: 800px
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
  height: 400px;
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

</style>
