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
          <el-tabs type="border-card" class="demo-tabs" >
            <el-tab-pane label="任务信息">
              <div class="operation">
                <div class="item">手动任务列表:</div>
                <div class="item">
                  <el-select v-model="selected_task" placeholder="选择任务" :teleported='false' class="select-operation">
                    <el-option
                      v-for="item in task_list"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </div>
                <div class="item"><el-button class="btn">起飞检查</el-button></div>
                <div class="item"><el-button class="btn">一键执飞</el-button></div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="手动接管">
              <controlPanel></controlPanel>
            </el-tab-pane>
          </el-tabs>
          <el-divider />
        </div>
        <div class="box2">
            <div class="logs-title" style="color: aliceblue;">站点日志</div>
        </div>
      </div>
      <div class="right">
        <!-- 右侧占满剩余空间的内容 -->
        <div class="box3">
          <div id="cesiumContainer"></div>
          <canvas id="threeContainer"></canvas>
          <div class="show-info">
            <span> 经度：{{ infoText.longitude }}</span>
            <span> 纬度：{{ infoText.latitude }}</span>
            <span> 海拔高度:{{ infoText.height}}</span>
            <!-- <button @click="flyToPoint()"></button> -->
          </div>
        </div>
      </div>
    </div>
</template>

<script setup>
import controlPanel from '../control/ControlDegree.vue'
import 'cesium/Build/Cesium/Widgets/widgets.css'
import { getLocation } from '/@/api/wayline'
import * as Cesium from 'cesium'
import { reactive, onMounted, ref, computed } from 'vue'
const leftWidth = ref(400)
const cesium = {
  viewer: null,
}
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
// --------------------------------------------------------------------------------------左侧功能栏----------------------------------------------------------------------------------------
const isRightPanelVisible = ref(true)
const arrowIcon = computed(() => {
  return isRightPanelVisible.value
    ? new URL('../../assets/v3/icon/right-arrow.png', import.meta.url).href
    : new URL('../../assets/v3/icon/left-arrow.png', import.meta.url).href
})

const selected_task = ref('')
const task_list = [
  {
    label: '测试任务',
    value: 1
  },
  {
    label: '测试任务1',
    value: 2
  }
]
// -------------------------------------------------------------------------------------场景初始化-----------------------------------------------------------------------------------------
onMounted(() => {
  init()
})
function init () {
  initCesium()
  startRenderLoop()
  load3DTiles()
  loadGltfModel()
  setupClickEvent() // 监听鼠标点击时间
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
    imageryProvider: undefined,
    baseLayerPicker: false,
    automaticallyTrackDataSourceClocks: false,
    dataSources: null,
    clock: null,
    terrainShadows: Cesium.ShadowMode.DISABLED,
  })
  if (Cesium.FeatureDetection.supportsImageRenderingPixelated()) {
    //  判断是否支持图像渲染像素化处理
    cesium.viewer.resolutionScale = window.devicePixelRatio
  }
  cesium.viewer.scene.postProcessStages.fxaa.enabled = true
  cesium.viewer.scene.debugShowFramesPerSecond = true // 显示帧率
  // const center = Cesium.Cartesian3.fromDegrees(
  //   (minWGS84[0] + maxWGS84[0]) / 2,
  //   (minWGS84[1] + maxWGS84[1]) / 2 - 1,
  //   200000
  // );
  // cesium.viewer.camera.flyTo({
  //   destination: center,
  //   orientation: {
  //     heading: CesiumMath.toRadians(0),
  //     pitch: CesiumMath.toRadians(-60),
  //     roll: CesiumMath.toRadians(0),
  //   },
  //   duration: 3,
  // });

  // 加载 3D Tiles 模型
  // this.load3DTiles();
  // this.loadGltfModel(121.254713,37.502219,24,0.2);

  const flightData = [
    { longitude: 121.254703, latitude: 37.503140, height: 40 },
    { longitude: 121.254665, latitude: 37.50367, height: 40 },
    { longitude: 121.254633, latitude: 37.504232, height: 40 },
    // 添加更多数据点
  ]
  drawPoints(flightData)
  drawLine(flightData)
// drawPoints(flightData)
// flightData.forEach(({ longitude, latitude, height}) => {
//   setTimeout(() =>{this.flightTracker1({longitude, latitude, height},0,1);},12000)
// })
// 启动轨迹追踪
}
function startRenderLoop () {
  cesium.viewer.render()
  requestAnimationFrame(startRenderLoop)
}
//  -------------------------------------------------------------------------------------模型加载-----------------------------------------------------------------------------------------
// 加载3DTiles模型
function load3DTiles () {
// const tilesetUrl = './SampleData/mars3d-max-shihua-3dtiles-master/tileset.json'; //  3D Tiles 模型路径
  const tilesetUrl = '/model/yonganjie/Production_2.json'
  // 使用 fromUrl 方法加载 3D Tiles
  Cesium.Cesium3DTileset.fromUrl(tilesetUrl).then((tileset) => {
    cesium.viewer.scene.primitives.add(tileset)
    // //  //利用translation对模型的经度、纬度、高度进行修改
    // var cartographic = Cesium.Cartographic.fromCartesian(tileset.boundingSphere.center);
    // var surface = Cesium.Cartesian3.fromRadians(cartographic.longitude, cartographic.latitude, cartographic.height);
    // var offset = Cesium.Cartesian3.fromRadians(cartographic.longitude, cartographic.latitude,40);
    // var translation = Cesium.Cartesian3.subtract(offset, surface, new Cesium.Cartesian3());
    // tileset.modelMatrix = Cesium.Matrix4.fromTranslation(translation);
    // 设置缩放系数，越小越清晰
    tileset.maximumScreenSpaceError = 1
    // this.tileSet(tileset,121.370868,37.520122,0)
    // 定位模型位置
    cesium.viewer.zoomTo(tileset)
  }).catch((error) => {
    console.error('Error loading tileset:', error)
  })
}
// 加载glft模型
function loadGltfModel () {
  // Load the glTF model from Cesium ion.
  const airplaneEntity = cesium.viewer.entities.add({
    id: 'airplaneEntity', // 给模型一个唯一ID
    model: {
      uri: '/model/gltf/airplane.glb',
      minimumPixelSize: 32,
      maximumScale: 200,
    },
    position: Cesium.Cartesian3.fromDegrees(121.254884, 37.503523, 30),
    // orientation: Cesium.Quaternion.fromAxisAngle(Cesium.Cartesian3.UNIT_Z, Cesium.Math.toRadians(rotationAngle)),
  })
  airplaneEntity.model.scale = 0.2
  cesium.viewer.trackedEntity = airplaneEntity
}
//  ----------------------------------------------------------------------------------------------------功能项--------------------------------------------------------------------------------------------------------
// 实时获取
function queryCoordinate () {
  const deviceOsdTopic = 'thing/product/5YSZL4V00325YL'
  const workspace_id = 'e3dea0f5-37f2-4d79-ae58-490af3228069'
  const deviceSn = '1581F5FHD23BT00DRZM6'

  getLocation(deviceOsdTopic, workspace_id, deviceSn).then(res => {
    if (res.code !== 0) {
      return
    }
    console.log('设备数据', res.data)
  })
}
//  坐标拾取
function setupClickEvent () {
  let timer = null // 定义 timer
  // const that = this; // 保存上下文
  const handlePoint = new Cesium.ScreenSpaceEventHandler(cesium.viewer.scene.canvas)
  handlePoint.setInputAction(function (event) {
    clearTimeout(timer)
    timer = window.setTimeout(function () {
    // 单击获取坐标点
      const coordinate = getCartesianCoordinate(event.position)
      if (coordinate) {
        console.log(coordinate)
        // that.infoText = 'sdcfgf'
        infoText.longitude = coordinate.longitude.toFixed(6)
        infoText.latitude = coordinate.latitude.toFixed(6)
        infoText.height = coordinate.altitude.toFixed(2)

        // `坐标: 经度 ${coordinate.longitude.toFixed(6)}, 纬度 ${coordinate.latitude.toFixed(6)}, 高度 ${coordinate.altitude.toFixed(2)}`;
      }
    }, 200)
    flyToPoint()
  }, Cesium.ScreenSpaceEventType.LEFT_CLICK)
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
    longitude: Number(longitude.toFixed(6)),
    latitude: Number(latitude.toFixed(6)),
    altitude: Number(height.toFixed(2))
  }
  return position
}
// 路径追踪
const isFlying = false
function flightTracker1 (targetDataPoint, rotationAngle, speed) {
  const { longitude, latitude, height } = targetDataPoint

  // 设置时间步长和移动时长
  const timeStepInSeconds = 0.2 // 每隔0.2秒移动一次
  const totalSeconds = 2 // 移动到目标点总用时2秒
  const start = Cesium.JulianDate.now()
  const stop = Cesium.JulianDate.addSeconds(start, totalSeconds, new Cesium.JulianDate())

  cesium.viewer.clock.startTime = start.clone()
  cesium.viewer.clock.stopTime = stop.clone()
  cesium.viewer.clock.currentTime = start.clone()
  cesium.viewer.timeline.zoomTo(start, stop)
  cesium.viewer.clock.multiplier = speed
  cesium.viewer.clock.shouldAnimate = true

  // 检查是否已存在模型
  let airplaneEntity = cesium.viewer.entities.getById('airplaneEntity')

  // 如果不存在，则创建新模型并设置初始位置
  if (!airplaneEntity) {
    airplaneEntity = cesium.viewer.entities.add({
      id: 'airplaneEntity',
      model: {
        uri: '/model/gltf/airplane.glb',
        minimumPixelSize: 64,
        maximumScale: 200,
      },
      position: Cesium.Cartesian3.fromDegrees(121.254884, 37.503523, 24),
      orientation: Cesium.Quaternion.fromAxisAngle(Cesium.Cartesian3.UNIT_Z, Cesium.Math.toRadians(rotationAngle)),
      show: true,
    })
  } else {
    // 如果已经在飞行，重置位置和方向
    // if (isFlying) {
    //   airplaneEntity.position = Cesium.Cartesian3.fromDegrees(longitude, latitude, height)
    //   airplaneEntity.orientation = Cesium.Quaternion.fromAxisAngle(Cesium.Cartesian3.UNIT_Z, Cesium.Math.toRadians(rotationAngle))
    // }
  }

  // 模型的平滑位置属性
  const positionProperty = new Cesium.SampledPositionProperty()
  const initialPosition = airplaneEntity.position.getValue(Cesium.JulianDate.now())

  // 确保初始位置有效
  if (!initialPosition) {
    console.error('初始位置未定义。')
    return
  }

  positionProperty.addSample(start, initialPosition)
  const targetPosition = Cesium.Cartesian3.fromDegrees(longitude, latitude, height)

  for (let i = 1; i <= totalSeconds / timeStepInSeconds; i++) {
    const time = Cesium.JulianDate.addSeconds(start, i * timeStepInSeconds, new Cesium.JulianDate())
    const t = i / (totalSeconds / timeStepInSeconds)
    const interpolatedPosition = Cesium.Cartesian3.lerp(initialPosition, targetPosition, t, new Cesium.Cartesian3())

    positionProperty.addSample(time, interpolatedPosition)
  }

  // 更新模型位置和方向
  airplaneEntity.position = positionProperty
  airplaneEntity.orientation = new Cesium.VelocityOrientationProperty(positionProperty)

  // 确保模型在目标位置停留
  positionProperty.addSample(stop, targetPosition)

  // // 添加一个额外的样本，以确保模型在目标点的状态持续有效
  const additionalTime = Cesium.JulianDate.addSeconds(stop, 5, new Cesium.JulianDate()) // 保持10秒在目标位置
  positionProperty.addSample(additionalTime, targetPosition)

  // // 标记飞机正在飞行
  // isFlying = true

  // // 在到达目标后重置状态
  // setTimeout(() => {
  //   isFlying = false // 飞行结束
  // }, (totalSeconds + 10) * 1000) // 等待总时间加上额外的停留时间
}
// 模型移动函数
function moveModel (targetLongitude, targetLatitude, targetHeight, heading) {
  // 检查是否已存在模型
  const entity = cesium.viewer.entities.getById('airplaneEntity')
  const startPosition = entity.position.getValue(Cesium.JulianDate.now())
  const newPosition = Cesium.Cartesian3.fromDegrees(targetLongitude, targetLatitude, targetHeight)
  const startTime = Cesium.JulianDate.now()
  const duration = 2

  function animate () {
    const elapsedTime = Cesium.JulianDate.secondsDifference(Cesium.JulianDate.now(), startTime)
    const t = Math.min(elapsedTime / duration, 1.0) // 归一化时间
    // 线性插值位置
    const currentPosition = Cesium.Cartesian3.lerp(startPosition, newPosition, t, new Cesium.Cartesian3())
    entity.position = currentPosition

    if (t < 1.0) {
      requestAnimationFrame(animate) // 使用全局的requestAnimationFrame
    } else {
      // 到达目标后设置朝向
      const orientation = Cesium.Transforms.headingPitchRollToFixedFrame(newPosition, new Cesium.HeadingPitchRoll(heading, 0, 0))
      entity.modelMatrix = orientation
    }
  }

  requestAnimationFrame(animate) // 开始动画
}
function flyToPoint () {
  setTimeout(() => {
    const longitude = Number(infoText.longitude)
    const latitude = Number(infoText.latitude)
    const height = Number(infoText.height)
    // console.log('ss', infoText.longitude, infoText.latitude, infoText.height)
    const obj = { longitude: Number(infoText.longitude), latitude: Number(infoText.latitude), height: Number(infoText.height) }
    flightTracker1(obj, 0, 1)
    // moveModel(longitude, latitude, height, 0)
  }, 500)
}
// ============================================================================================绘制===========================================================================================

// 绘制线
function drawLine (points) {
  const positions = points.map(point =>
    Cesium.Cartesian3.fromDegrees(point.longitude, point.latitude, point.height)
  )

  cesium.viewer.entities.add({
    polyline: {
      positions: positions,
      width: 3,
      material: getRandomColor(), // 设置线条颜色和透明度
    },
  })
}
function drawPoints (dataArray) {
  dataArray.forEach(({ longitude, latitude, height }) => {
    cesium.viewer.entities.add({
      position: Cesium.Cartesian3.fromDegrees(longitude, latitude, height),
      point: {
        pixelSize: 10, // 点的大小
        color: Cesium.Color.RED, // 传入的颜色
      },
    })
  })
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
</script>

<style lang="scss" scoped>
.container{
  display: flex;
  width: 100%;
  padding: 10px;
  height: calc(100vh - 60px)
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

  .right {
    flex-grow: 1; /* 右侧占满剩余空间 */
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

</style>
