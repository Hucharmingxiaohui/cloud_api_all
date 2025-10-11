
import * as Cesium from 'cesium'
import 'cesium/Build/Cesium/Widgets/widgets.css'
import { renderBillboard } from './drawBillboard'

export function initCesium (containerId) {
  Cesium.Ion.defaultAccessToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiI3ZmJjODE1Yy1kMjU4LTQyZTgtODAyZC1mNzE2MDNhMmQ3YzUiLCJpZCI6MTk5NzQwLCJpYXQiOjE3MDk2Mjg5Mjh9.GuRbyEbm8FknaFOM34kGm9wCbf2XVjp873h_QD-Vs7A'

  const viewer = new Cesium.Viewer(containerId, {
    useDefaultRenderLoop: false,
    selectionIndicator: false,
    infoBox: false,
    navigationInstructionsInitiallyVisible: false,
    fullscreenButton: false,
    allowTextureFilterAnisotropic: false,
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
    baseLayerPicker: false,
    automaticallyTrackDataSourceClocks: false,
    dataSources: null,
    clock: null,
    terrainShadows: Cesium.ShadowMode.DISABLED,
    navigationHelpButton: false, // Don't show help button
    sceneModePicker: false, // Don't show scene mode button (2D/3D/Columbus view)
    timeline: false, // Don't show timeline
    imageryProviderViewModels: [], // Don't use default map
    geocoder: false, // Don't show geocoder
    homeButton: false, // Don't show home button
    sceneMode: Cesium.SceneMode.SCENE3D, // Use 3D mode directly
    animation: false, // Hide animation controls
  })

  viewer._cesiumWidget._creditContainer.style.display = 'none' // Hide logo

  if (Cesium.FeatureDetection.supportsImageRenderingPixelated()) {
    viewer.resolutionScale = window.devicePixelRatio
  }

  viewer.scene.postProcessStages.fxaa.enabled = true
  viewer.scene.debugShowFramesPerSecond = true // Show FPS
}

export function removeLogo (viewer) {
  const creditContainer = viewer.cesiumWidget.creditContainer
  creditContainer.style.display = 'none'
}

// 加载3DTiles模型
export function load3DTilesModels (CeramerViewer, tilesetUrl) {
  Cesium.Cesium3DTileset.fromUrl(tilesetUrl).then((tileset) => {
    CeramerViewer.scene.primitives.add(tileset)
    // const cartographic = Cesium.Cartographic.fromCartesian(tileset.boundingSphere.center)
    // const surface = Cesium.Cartesian3.fromRadians(cartographic.longitude, cartographic.latitude, cartographic.height)
    // const offset = Cesium.Cartesian3.fromRadians(cartographic.longitude, cartographic.latitude, 0)
    // const translation = Cesium.Cartesian3.subtract(offset, surface, new Cesium.Cartesian3())
    // tileset.modelMatrix = Cesium.Matrix4.fromTranslation(translation)
    // 设置缩放系数，越小越清晰
    tileset.maximumScreenSpaceError = 1
    // 定位并缩放到模型位置
    CeramerViewer.zoomTo(tileset)
  }).catch((error) => {
    console.error('Error loading tileset:', error)
  })
}

// 加载gltf模型
export function loadGltfModel (viewer, gltfUrl, position) {
  // 加载模型实体
  const airplaneEntity = viewer.entities.add({
    id: 'airplaneEntity',
    position: position,
    model: {
      uri: gltfUrl,
      minimumPixelSize: 32,
      maximumScale: 200,
      scale: 0.07, // 直接在这里设置scale，避免后续赋值问题
    }
  })
  return airplaneEntity
}

export function drawBoundingBox (viewer, airplaneEntity, position) {
  const newpositions = [
    new Cesium.Cartesian3(position.x - 2, position.y, position.z - 2),
    new Cesium.Cartesian3(position.x + 2, position.y, position.z - 2),
    new Cesium.Cartesian3(position.x - 2, position.y, position.z + 2),
    new Cesium.Cartesian3(position.x + 2, position.y, position.z + 2)
  ]
  // 添加矩形框实体
  viewer.entities.add({
    parent: airplaneEntity,
    name: 'Bounding Rectangle',
    polyline: {
      positions: newpositions,
      width: 2,
      material: Cesium.Color.RED,
      loop: true,
      classificationType: Cesium.ClassificationType.TERRAIN
    }
  })
}
// 计算包围盒的8个角点
function computeBoundingBoxCorners (position, dimensions) {
  const halfX = dimensions.x / 2
  const halfY = dimensions.y / 2
  const halfZ = dimensions.z / 2

  return [
    // 底面
    new Cesium.Cartesian3(position.x - halfX, position.y - halfY, position.z - halfZ),
    new Cesium.Cartesian3(position.x + halfX, position.y - halfY, position.z - halfZ),
    new Cesium.Cartesian3(position.x + halfX, position.y + halfY, position.z - halfZ),
    new Cesium.Cartesian3(position.x - halfX, position.y + halfY, position.z - halfZ),
    new Cesium.Cartesian3(position.x - halfX, position.y - halfY, position.z - halfZ), // 闭合

    // 顶面
    new Cesium.Cartesian3(position.x - halfX, position.y - halfY, position.z + halfZ),
    new Cesium.Cartesian3(position.x + halfX, position.y - halfY, position.z + halfZ),
    new Cesium.Cartesian3(position.x + halfX, position.y + halfY, position.z + halfZ),
    new Cesium.Cartesian3(position.x - halfX, position.y + halfY, position.z + halfZ),
    new Cesium.Cartesian3(position.x - halfX, position.y - halfY, position.z + halfZ), // 闭合

    // 侧面连接线
    new Cesium.Cartesian3(position.x - halfX, position.y - halfY, position.z - halfZ),
    new Cesium.Cartesian3(position.x - halfX, position.y - halfY, position.z + halfZ),

    new Cesium.Cartesian3(position.x + halfX, position.y - halfY, position.z - halfZ),
    new Cesium.Cartesian3(position.x + halfX, position.y - halfY, position.z + halfZ),

    new Cesium.Cartesian3(position.x + halfX, position.y + halfY, position.z - halfZ),
    new Cesium.Cartesian3(position.x + halfX, position.y + halfY, position.z + halfZ),

    new Cesium.Cartesian3(position.x - halfX, position.y + halfY, position.z - halfZ),
    new Cesium.Cartesian3(position.x - halfX, position.y + halfY, position.z + halfZ)
  ]
}
/**
 * 更新gltf模型姿态（基于欧拉角）
 * @param {Cesium.model} model - 目标模型实体（由loadGltfModel创建）
 * @param {Cesium.position} position - 笛卡尔坐标
 * @param {number} scale -模型缩放尺寸 1
 * @param {number} offsetX - 平移y 0
 * @param {number} offsetY - 平移x  0
 * @param {number} offsetZ - 平移y 0
 * @param {number} heading - 航向角（弧度）
 * @param {number} pitch - 俯仰角（弧度）
 * @param {number} roll - 滚转角（弧度）
 */

export function updateOrientation (model, position, scale, offsetX, offsetY, offsetZ, heading, pitch, roll) {
  model.scale = new Cesium.ConstantProperty(scale)
  // 坐标偏移计算（ENU坐标系）‌:ml-citation{ref="2,4" data="citationList"}
  const enuOffset = new Cesium.Cartesian3(offsetX, offsetY, offsetZ)
  const fixedFrame = Cesium.Transforms.eastNorthUpToFixedFrame(position)
  const offsetPosition = Cesium.Matrix4.multiplyByPoint(
    fixedFrame,
    enuOffset,
    new Cesium.Cartesian3()
  )
  model.position = new Cesium.ConstantPositionProperty(offsetPosition)

  const headingVal = Cesium.Math.toRadians(heading) // 角度转弧度
  const pitchVal = Cesium.Math.toRadians(pitch)
  const rollVal = Cesium.Math.toRadians(roll)
  const hpr = new Cesium.HeadingPitchRoll(headingVal, pitchVal, rollVal)
  const orientation = Cesium.Transforms.headingPitchRollQuaternion(
    offsetPosition,
    hpr
  )
  model.orientation = orientation
}

/**
 * 移除指定模型实体
 * @param {Cesium.Viewer} viewer - Cesium Viewer实例
 * @param {Cesium.Entity} entity - 需移除的模型实体对象
 */
export function removeGltfModel (viewer, entity) {
  if (viewer && entity) {
    viewer.entities.remove(entity)
  }
}

/**
 * 绘制点
 * @param {Cesium.Viewer} viewer - Cesium Viewer实例
 * @param {[]} options - 点位参数列表
 * @param {Number} longitude - 精度
 * @param {Number} latitude - 纬度
 * @param {Number} height - 高度
 */
export function drawPoints (viewer, options, longitude, latitude, height) {
  const pointEntity = viewer.entities.add({
    position: Cesium.Cartesian3.fromDegrees(longitude, latitude, height),
    point: {
      pixelSize: 10, // 点的大小
      color: Cesium.Color.RED, // 传入的颜色
    },
  })
  // 调用 renderBillboard 函数生成 billboard 图像
  renderBillboard(options, (error, result) => {
    if (error) {
      console.error('Error rendering billboard:', error)
      return
    }
    const billboard = new Cesium.BillboardGraphics({
      image: result.image,
      scale: 1.0,
      width: 80,
      height: 80,
      depthTest: false,
      disableDepthTestDistance: Number.POSITIVE_INFINITY,
      pixelOffset: new Cesium.Cartesian2(0, -45)
    })
    pointEntity.billboard = billboard
  })
  // const billboard = new Cesium.BillboardGraphics({
  //   image: '/src/assets/v4/waypoint.png',
  //   scale: 1.0,
  //   width: 30, // 图标的宽度
  //   height: 30, // 图标的高度
  //   depthTest: false, // 禁用深度测试
  //   disableDepthTestDistance: Number.POSITIVE_INFINITY, // 禁用深度测试
  //   // 调整图标的位置
  //   pixelOffset: {
  //     x: 0,
  //     y: -12
  //   }
  // })
  // pointEntity.billboard = billboard
  // const label = new Cesium.LabelGraphics({
  //   text: '测试点位', // 标签的文本内容
  //   scale: 0.5, // 标签文本的缩放因子
  //   pixelOffset: new Cesium.Cartesian2(0, -30), // 文本相对位置的偏移，向上偏移30像素
  //   horizontalOrigin: Cesium.HorizontalOrigin.CENTER, // 水平对齐方式，文本与锚点居中对齐
  //   verticalOrigin: Cesium.VerticalOrigin.BOTTOM, // 垂直对齐方式，文本与锚点底部对齐
  //   fillColor: Cesium.Color.YELLOW, // 填充颜色
  //   outlineColor: Cesium.Color.BLACK, // 轮廓颜色
  //   outlineWidth: 1.0, // 轮廓线宽度
  //   backgroundColor: Cesium.Color.BLACK.withAlpha(0.7),
  //   style: Cesium.LabelStyle.FILL_AND_OUTLINE // 标签的样式，填充和轮廓
  // })
  // pointEntity.label = label
  return pointEntity
}

/**
 * 绘制线条，发光效果
 * @param {Cesium.Viewer} viewer - Cesium Viewer实例
 * @param {[{longitude:longitude,latitude:latitude,height:height}]} points -点位数组
 */
export function drawLine (points, viewer) {
  const positions = points.map(point =>
    Cesium.Cartesian3.fromDegrees(point.longitude, point.latitude, point.height)
  )
  viewer.entities.add({
    polyline: {
      positions: positions,
      width: 10,
      material: new Cesium.PolylineGlowMaterialProperty({
        color: Cesium.Color.DEEPSKYBLUE,
        glowPower: 0.5, // 更强的发光效果
      }),
    },
  })
  // Cesium.Color.fromRandom({ alpha: 1.0 }) // 随机颜色
}

/**
 * 创建视锥体
 * @param {Cesium.Viewer} viewer - Cesium Viewer实例
 * @param {Cesium.Entity} position - 视锥体的坐标
 * @param {number} heading - 航向角（弧度）
 * @param {number} pitch - 俯仰角（弧度）
 * @param {number} roll - 滚转角（弧度）
 */
export function createPyramis (viwer, oldPrimitives, position, heading, pitch, roll, fov) {
  if (oldPrimitives) {
    removePyramis(viwer, oldPrimitives)
  }
  const camera = new Cesium.Camera(viwer.scene)
  // camera.frustum.fov = Cesium.Math.PI_OVER_THREE
  camera.frustum.near = 0.1
  camera.frustum.far = 30.0
  camera.frustum.fov = fov

  camera.setView({
    destination: position,
    orientation: {
      heading: Cesium.Math.toRadians(heading),
      pitch: Cesium.Math.toRadians(pitch),
      roll: Cesium.Math.toRadians(roll),
    }
  })
  const primitive = new Cesium.DebugCameraPrimitive({
    camera: camera,
    color: Cesium.Color.YELLOW,
    show: true,
    updateOnchange: true
  })
  viwer.scene.primitives.add(primitive)
  return primitive
}

/**
 * 删除指定视锥体
 * @param {Cesium.Viewer} viewer - Cesium Viewer实例
 * @param {Cesium.DebugCameraPrimitive} primitive - 由createPyramis创建的视锥体对象
 */
export function removePyramis (viewer, primitive) {
  if (viewer && primitive) {
    viewer.scene.primitives.remove(primitive)
  }
}

/**
 1. 变焦操作流程‌
‌步骤1‌：通过PTZScale.value获取当前变焦比例（如从UI滑块输入)。
‌步骤2‌：计算与前次变焦比例的差值，乘以灵敏度系数PTZNum.value，得到移动距离。
‌步骤3‌：调用move()函数驱动相机移动，实现变焦效果。
*/
/**
 * cesium 模拟相机变焦
 * @param {Cesium.Viewer} viewer - Cesium Viewer实例
 * @param {Number} zoomScale 变焦倍数
 * @param {Number} prevZoomScale - 前次变焦倍数
 * @param {Number} zoomSensitivity - 变焦灵敏度
 */
// 更新前次比例
export function computeCameraChangeDis (viewer, zoomScale, prevZoomScale, zoomSensitivity) {
  const distance = (zoomScale - prevZoomScale) * zoomSensitivity
  viewer.camera.move(viewer.camera.direction, distance)
}
