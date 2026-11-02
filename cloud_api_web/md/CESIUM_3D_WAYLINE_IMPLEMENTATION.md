# Cesium 三维航线规划 — 现有实现说明（开发参考）

> 范围：前端现有 **Cesium 三维航线**实现（通用路径）  
> 不含：光伏编辑业务细节（`Solar3DRouteEditor` / `/api/solar3d/*` 仅作对照提及）  
> 目标：给后续三维航线规划（含云渲染对接）提供可落地的代码地图与公式说明

---

## 1. 功能定位

在 Cesium 三维场景中：

1. 加载倾斜/三维模型（3D Tiles）
2. 放置/编辑航点（经纬高）
3. 编辑每个航点的 **航向 / 俯仰 / 滚转 / 焦距（FOV）**
4. 用 **视锥体** 预览拍摄覆盖
5. 用子相机视图模拟机载第一人称画面
6. 将航点临时数据组装为 `buildKmz` 报文，生成大疆 KMZ

**主入口路由**

| 路由 | 组件 | 说明 |
|---|---|---|
| `/wayline/Model` | `src/components/cesium/3DMapPanel.vue` | **通用三维航线编辑（本文主线）** |
| `/wayline/solar3d-editor` | `src/components/cesium/Solar3DRouteEditor.vue` | 光伏三维编辑（对照用，不展开业务） |

---

## 2. 关键文件

| 文件 | 职责 |
|---|---|
| `src/components/cesium/3DMapPanel.vue` | 主场景、航点编辑、确认/生成航线 |
| `src/components/cesium/uavCamera.vue` | 子 Viewer：机载视角、焦距↔FOV、参数回传 |
| `src/components/cesium/cesiumTools.js` | `createPyramis` / `removePyramis` 视锥体 |
| `src/components/cesium/CreateFrustum.js` | 旧版 FrustumGeometry 实现（基本闲置） |
| `src/components/cesium/kmzRequest.js` | `generateWaylineRequest` → `POST /buildKmz` |
| `src/router/index.ts` | `/wayline/Model` 路由注册 |

后端对接（KMZ）：

| 文件 | 职责 |
|---|---|
| `dfe-wayline/.../UavRouteController.java` | `POST /buildKmz` |
| `dfe-wayline/.../RouteFileUtils.java` | KML/WPML 生成；`takePhotoType=2` → orientedShoot |
| `cloud_api_web/md/dfe-wayline-buildKmz-api.md` | 报文规范文档 |

---

## 3. 总体架构

```
┌─────────────────────────────────────────────────────────────┐
│  3DMapPanel.vue（主 Viewer）                                  │
│  - 3D Tiles 模型                                              │
│  - 航点 Entity / 折线                                         │
│  - 黄色视锥体 createPyramis(DebugCameraPrimitive)            │
│  - 航向/俯仰/滚转/焦距滑条                                    │
│  - sessionStorage 临时航点                                    │
└───────────────────────────┬─────────────────────────────────┘
                            │ setCamera / cameraUpdated
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  uavCamera.vue（子 Viewer）                                   │
│  - 独立 Cesium.Viewer                                         │
│  - 按航点位置 + H/P/R 设视角                                  │
│  - 按等效焦距设置 PerspectiveFrustum.fov                      │
│  - emit cameraUpdated(heading/pitch/fov/focal...)             │
└───────────────────────────┬─────────────────────────────────┘
                            │ handlegenerate
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  kmzRequest.generateWaylineRequest                            │
│  - 组装 UavRouteReq                                           │
│  - 每点 takePhotoType=2（精准复拍）                            │
│  - POST /buildKmz → 下载 KMZ                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 主场景实现（3DMapPanel）

### 4.1 Viewer

- 使用 `Cesium.Viewer` 三维模式
- 加载 3D Tiles 模型（`load3DTilesModels`）
- 航点用 `Entity` 点/模型表示，可点选

### 4.2 编辑模式入口

函数：`changeCameraViwer()`（约 1097 行）

进入编辑时：

1. 取选中航点 `selectedEntity.position`
2. 加载机模 glTF（`loadGltf`）
3. 创建/刷新视锥体：`createPyramis(...)`
4. 同步子相机：`childMap.setCamera(position, heading, pitch, roll)`
5. `syncParamsFromSelected()` 把经纬高写入 `cameraParams`

退出编辑时：

- 移除机模、视锥体，清理状态

### 4.3 姿态滑条

| 变量 | 含义 | 默认 |
|---|---|---|
| `value1` | heading 航向角（度） | 90 |
| `value2` | pitch 俯仰角（度） | -90 |
| `value3` | roll 滚转角（度） | 0 |
| `value4` | 焦距滑条（mm，旧路径） | 4 |

`changeModels()`（约 1151 行）：滑条变化时同步

1. 重建视锥体
2. 更新机模朝向（视觉上 `heading - 90`，仅显示）
3. 更新子相机 `setCamera`

`watch(cameraParams.fov)`：FOV 变化时下一帧调用 `changeModels()`，缩放视锥体。

### 4.4 确认航点 `handleConfirm`

把当前编辑结果写入 `sessionStorage.waypointsData`：

```js
{
  point_name: selectedEntity.id,
  longitude,
  latitude,
  height,
  capture_mode,   // 如 'ir,visible'
  speed,          // 默认 5
  camera_params: {
    heading,
    pitch,
    roll,
    focalLength   // 实际写入的是 equivalentFocalLength（35mm 等效）
  }
}
```

注意：

- **位置**来自选中航点 Entity 的 cartographic（不是自由飞相机位置）
- **姿态/焦距**来自编辑态 `cameraParams`（子相机回传 + 滑条）
- `focalLength` 字段名存的是 **35mm 等效焦距**

### 4.5 生成航线 `handlegenerate`

1. 读 `sessionStorage.waypointsData`
2. 校验航线名
3. 调 `generateWaylineRequest(waypointsData, waylineName)`
4. 下载返回的 KMZ

---

## 5. 子相机实现（uavCamera.vue）

### 5.1 职责

- 独立 Cesium Viewer，模拟无人机相机画面
- 禁止缩放等交互（避免破坏姿态）
- 根据焦距设置 FOV
- 通过 `cameraUpdated` 把参数回传父组件

### 5.2 `setCamera(position, heading, pitch, roll, focalLengthMM?)`

```js
viewer.camera.setView({
  destination: position,
  orientation: {
    heading: toRadians(heading),
    pitch: toRadians(pitch),
    roll: toRadians(roll),
  }
})

// 若传入等效焦距，则设置 PerspectiveFrustum.fov
if (focalLengthMM && frustum instanceof PerspectiveFrustum) {
  const sensorHeightMM = 7.0
  const sensorDiagonalMM = 9.6
  const actualFocalLength = focalLengthMM / (43.3 / sensorDiagonalMM)
  frustum.fov = 2 * Math.atan(sensorHeightMM / (2 * actualFocalLength))
}
updateCameraParameters()
```

### 5.3 焦距滑条 `setCameraFocalLength(val)`（另一套模型）

```js
const sensorHeightMM = 4.8
const fovRad = 2 * Math.atan(sensorHeightMM / (2 * val))
frustum.fov = fovRad
```

> **重要不一致**：主换算用 7.0/9.6；滑条用 4.8。  
> 新开发应统一为一种传感器模型，避免 FOV 来回跳。

### 5.4 `updateCameraParameters` 回传字段

```js
{
  heading, pitch, roll,          // 度
  longitude, latitude, height,
  fov,                           // 弧度
  focalLength,                   // 实际焦距 mm（由 FOV 反算）
  equivalentFocalLength          // 35mm 等效焦距
}
```

父组件 `handleCameraUpdated` 更新 `cameraParams`，界面显示航向/俯仰/等效焦距/FOV。

---

## 6. 视锥体实现

### 6.1 主用：`createPyramis`（cesiumTools.js）

```js
export function createPyramis(viewer, oldPrimitive, position, heading, pitch, roll, fov) {
  // 1. 移除旧 primitive
  // 2. new Cesium.Camera(scene)
  camera.frustum.near = 0.1
  camera.frustum.far = 30.0
  camera.frustum.fov = fov   // 弧度
  camera.setView({
    destination: position,
    orientation: {
      heading: toRadians(heading),
      pitch: toRadians(pitch),
      roll: toRadians(roll),
    }
  })
  // 3. DebugCameraPrimitive 黄色显示
  return primitive
}
```

| 参数 | 说明 |
|---|---|
| `position` | 航点 Cartesian3 |
| `heading/pitch/roll` | **度**（内部转弧度） |
| `fov` | **弧度**（与 Cesium frustum.fov 一致） |
| `near/far` | 0.1 / 30 米（可视化长度，非真实拍摄距离） |

### 6.2 旧实现：`CreateFrustum.js`

- `PerspectiveFrustum` + `FrustumGeometry` / outline
- 当前通用编辑主路径 **未使用**；保留兼容

---

## 7. 焦距 / 视场角换算（核心公式）

### 7.1 常量（主路径）

| 符号 | 值 | 含义 |
|---|---|---|
| `sensorHeightMM` | 7.0 | 传感器垂直尺寸 mm |
| `sensorDiagonalMM` | 9.6 | 传感器对角线 mm |
| 全画幅对角 | 43.3 | 35mm 画幅对角线 mm |
| crop factor | 43.3 / 9.6 ≈ 4.51 | 裁切系数 |

### 7.2 35mm 等效焦距 → FOV

```text
actualFocal = eq35mm / (43.3 / sensorDiagonalMM)
            = eq35mm * (sensorDiagonalMM / 43.3)

fov(rad) = 2 * atan(sensorHeightMM / (2 * actualFocal))
```

代码位置：

- `uavCamera.vue` → `setCamera`
- `Solar3DRouteEditor.vue` → `equivalentFocalLengthToFov`（同公式）

### 7.3 FOV → 实际焦距 / 等效焦距

```text
actualFocal = sensorHeightMM / (2 * tan(fov / 2))
eq35mm      = actualFocal * (43.3 / sensorDiagonalMM)
```

代码位置：`uavCamera.vue` → `updateCameraParameters`

### 7.4 与 Cesium FOV 的关系

- 使用 `PerspectiveFrustum.fov`（弧度）
- Cesium 中 `fov` 是 **水平/垂直中较大方向** 的视场（与 aspect 有关）
- 当前实现 **未显式设置 aspectRatio**，按默认视口比走
- 预览报文里若有 `hfov/vfov`，通用路径 **未读取**，只认焦距/FOV

### 7.5 下发给 KMZ 的焦距字段含义

`handleConfirm` 存：

```js
camera_params.focalLength = cameraParams.equivalentFocalLength  // 35mm 等效
```

`kmzRequest` 再原样塞进动作：

```js
focalLength: point.camera_params.focalLength
```

因此后端 `orientedShoot.focalLength` 收到的是 **35mm 等效焦距数值**，不是传感器真实焦距。  
后续若要对齐大疆光学焦距，需要单独定义换算/配置表。

---

## 8. 航点临时数据结构

`sessionStorage` key：

| Key | 内容 |
|---|---|
| `waypointsData` | 航点数组 JSON |
| `selectedWayline` | 当前航线标识 |

单点结构：

```json
{
  "point_name": "WP_001",
  "longitude": 121.3658,
  "latitude": 37.5198,
  "height": 120.5,
  "capture_mode": "ir,visible",
  "speed": 5,
  "camera_params": {
    "heading": 90,
    "pitch": -45,
    "roll": 0,
    "focalLength": 75
  }
}
```

`focalLength`：35mm 等效（mm）  
`heading/pitch/roll`：度

---

## 9. KMZ 报文组装（kmzRequest.js）

### 9.1 入口

```js
generateWaylineRequest(waypointsData, selectedWayline)
// POST /buildKmz
// responseType: blob → 下载 .kmz
```

### 9.2 航线固定头（当前代码）

```js
{
  routeName: selectedWayline,
  templateType: 'waypoint',
  takeOffRefPoint: '37.5198...,121.3658...,108.75', // 写死示例坐标
  droneType: 100,
  subDroneType: 1,
  payloadType: 99,
  payloadPosition: 0,
  executeHeightMode: 'WGS84',
  imageFormat: 'visible,ir',
  finishAction: 'noAction',
  exitOnRcLostAction: 'goBack',
  globalHeight: 50,
  autoFlightSpeed: 10,
  waypointHeadingReq: { waypointHeadingMode: 'fixed', waypointHeadingAngle: 0 },
  waypointTurnReq: {
    waypointTurnMode: 'toPointAndStopWithDiscontinuityCurvature',
    useStraightLine: 1
  },
  gimbalPitchMode: 'usePointSetting',
  startActionList: [ /* 云台归位类动作 */ ],
  routePointList: []
}
```

### 9.3 每点映射

| 前端字段 | 报文字段 |
|---|---|
| `longitude/latitude/height` | 同名 |
| `speed` 或默认 5 | `speed` |
| `camera_params.heading` | `waypointHeadingReq.waypointHeadingAngle` |
| `camera_params.pitch` | `gimbalPitchAngle`（航点层） |
| `camera_params.heading + 3.3` | `actions[0].gimbalYawRotateAngle`（补偿） |
| `camera_params.pitch - 1.5` | `actions[0].gimbalPitchRotateAngle`（补偿） |
| `camera_params.focalLength` | `actions[0].focalLength` |
| 固定 | `takePhotoType: 2`（精准复拍） |

### 9.4 精准复拍动作（核心）

```js
{
  actionIndex: 0,
  takePhotoType: 2,
  gimbalYawRotateAngle: heading + 3.3,   // 画面偏左补偿
  gimbalPitchRotateAngle: pitch - 1.5,   // 画面偏上补偿
  gimbalControlMode: 'absolute',
  useGlobalImageFormat: 0,
  imageFormat: 'ir,visable',
  payloadLensIndex: 'ir,visable',
  orientedCameraType: 81,
  accurateFrameValid: 1,
  orientedPhotoMode: 'normalPhoto',
  focalLength: camera_params.focalLength,
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
  focusRegionHeight: 360
}
// 另有 actionIndex:1 aircraftHeading: -156（写死，疑似历史残留）
```

补偿说明：

- `+3.3°` yaw：解决“拍出来偏左”
- `-1.5°` pitch：解决“拍出来偏上”
- 属于经验值，换机型/云台可能要重标定

---

## 10. 交互流程（开发时序）

```
用户进入 /wayline/Model
  → init Cesium + 加载模型
  → 添加/导入航点 Entity

右键/菜单「编辑」
  → changeCameraViwer()
  → createPyramis + uavCamera.setCamera
  → 显示滑条：航向/俯仰/滚转/焦距

拖动滑条
  → changeModels()
  → 刷新视锥 + 子相机
  → cameraUpdated 更新 cameraParams

点击确认
  → handleConfirm
  → sessionStorage.waypointsData 增改该点

点击生成航线
  → handlegenerate
  → generateWaylineRequest
  → POST /buildKmz → 下载 KMZ
```

---

## 11. 角度与坐标系约定

| 量 | 单位 | 约定 |
|---|---|---|
| longitude / latitude | 度 | WGS84 |
| height | 米 | 与模型/场景一致；KMZ 侧 WPML 常用 WGS84 绝对高 |
| heading | 度 | Cesium heading（0=北，顺时针为正，常见 0~360） |
| pitch | 度 | 水平 0，向下为负（拍摄常用 -20 ~ -90） |
| roll | 度 | 通常 0 |
| fov | 弧度 | Cesium frustum |
| focalLength（存储） | mm | **35mm 等效** |

机模显示朝向：`heading - 90`（仅 glTF 视觉，不参与报文）

---

## 12. 已知问题 / 开发注意

1. **两套传感器模型**  
   - 主路径：7.0 / 9.6  
   - 滑条：4.8  
   → 同一焦距不同 FOV，新开发必须统一

2. **`focalLength` 语义混淆**  
   - 存储/下发是 35mm 等效  
   - 变量名像真实焦距  
   → 建议新代码显式命名 `focalLength35mm`

3. **`kmzRequest` 硬编码**  
   - `aircraftHeading: -156`  
   - `takeOffRefPoint` 固定坐标  
   - yaw/pitch 补偿固定  
   → 三维规划应改为可配置

4. **拼写**  
   - 后端/历史：`visable`  
   - 前端混用 `visible` / `visable`  
   → 组报文时与后端可飞模板对齐（见 C++ `route_convert` 用 `visable`）

5. **视锥 far=30m** 仅可视化，不代表真实对焦/覆盖距离

6. **`CreateFrustum` 闲置**，不要和新逻辑混用两套视锥

7. **通用路径进入编辑时**，对历史航点的焦距/姿态回填弱于光伏编辑页；若做规划器，建议参考 `Solar3DRouteEditor.applyStoredCameraParamsToEditor`

8. **无单测**覆盖 FOV/焦距换算，改公式时应用样例值回归

---

## 13. 推荐复用清单（做新三维规划时）

| 能力 | 直接复用 | 建议改造 |
|---|---|---|
| 双 Viewer 架构 | 是 | 保持主场景 + FPV |
| `createPyramis` 视锥 | 是 | far 可配置 |
| 35mm↔FOV 主公式 | 是 | 删除 4.8 分支，统一常量 |
| sessionStorage 航点 | 可参考 | 可改为内存 store |
| `kmzRequest` | 结构可参考 | 去掉硬编码，对齐 `dfe-wayline-buildKmz-api.md` |
| 精准复拍 takePhotoType=2 | 是 | yaw/pitch 补偿外置配置 |

---

## 14. 最小代码索引

| 能力 | 位置 |
|---|---|
| 路由 | `src/router/index.ts` → `/wayline/Model` |
| 进入编辑 | `3DMapPanel.vue` → `changeCameraViwer` |
| 滑条刷新 | `3DMapPanel.vue` → `changeModels` |
| 确认航点 | `3DMapPanel.vue` → `handleConfirm` |
| 生成 KMZ | `3DMapPanel.vue` → `handlegenerate` → `kmzRequest.js` |
| 设相机+焦距 | `uavCamera.vue` → `setCamera` |
| FOV 反算焦距 | `uavCamera.vue` → `updateCameraParameters` |
| 视锥体 | `cesiumTools.js` → `createPyramis` |
| 后端生成 | `dfe-wayline` → `POST /buildKmz` |

---

## 15. 与后端报文文档的关系

- 前端组包细节：本文 §9  
- 后端字段全集 / 可飞模板：`cloud_api_web/md/dfe-wayline-buildKmz-api.md`  
- 历史可飞 C++ 参考：`D:\uav\157beifen\include\route_convert.hpp`

三维规划新服务建议：

1. 场景内产出与本文一致的 `waypointsData`（或等价结构）  
2. 按 `dfe-wayline-buildKmz-api.md` 组装最终 JSON  
3. 焦距、补偿、机型枚举做成配置，不再写死在 `kmzRequest.js`

---

## 16. 修订记录

| 日期 | 说明 |
|---|---|
| 2026-03-24 | 初版：梳理通用 Cesium 三维航线、相机/视锥/焦距与 buildKmz 链路 |
