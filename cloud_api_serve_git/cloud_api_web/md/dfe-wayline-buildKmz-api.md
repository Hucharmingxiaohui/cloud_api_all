# dfe-wayline 航线生成接口报文规范（三维云渲染对接）

> 适用场景：三维场景中点选航点 → 组装 JSON → 调用后端生成大疆 WPML 航线文件（KMZ）  
> 目标接口：`POST /buildKmz`（模块 `dfe-wayline`）  
> 字段命名：**驼峰（lowerCamelCase）**  
> Content-Type：`application/json`  
> 成功响应：二进制 KMZ 文件流（`Content-Type: application/vnd.google-earth.kmz`）

---

## 1. 接口总览

| 项目 | 说明 |
|---|---|
| 方法 | `POST` |
| 路径 | `/buildKmz` |
| 请求体 | `UavRouteReq` JSON |
| 成功 | HTTP 200，Body 为 `.kmz` 文件 |
| 失败 | 非 2xx 或服务异常（当前实现校验较弱，错误字段可能导致 KMZ 无法起飞） |
| 本地落盘 | 服务端相对路径 `file/kmz/{routeName}.kmz` |

### 1.1 相关接口（可选）

| 接口 | 用途 | 备注 |
|---|---|---|
| `POST /buildKmz` | 生成 KMZ 并下载 | **三维规划主用** |
| `POST /updateKmz` | 编辑已有 KMZ | 本地路径硬编码，不建议三维服务直接用 |
| `POST /parseKmz?fileUrl=...` | 从 URL 解析 KMZ | 返回 `KmlInfo` |

---

## 2. 三维服务对接原则（必读）

1. **只生成 `templateType = "waypoint"` 航点航线**（三维点选最匹配）。
2. **精准复拍**统一用 `takePhotoType = 2`（`orientedShoot`），不要用普通拍照 `0` 冒充。
3. **高度建议使用绝对椭球高（WGS84）**，与当前后端 WPML `executeHeightMode=WGS84` 一致。
4. **偏航角（heading）范围建议 `[-180, 180]` 或 `[0, 360)`，但 `360` 请改为 `359`**（历史可飞经验）。
5. **机头偏航动作**与**云台偏航**是两回事：
   - 机头：`aircraftHeading` + `aircraftPathMode`
   - 云台：`gimbalYawRotateAngle` / `gimbalPitchRotateAngle`
6. 动作列表 `actions` 按**字段非空**推断类型，顺序是 if-else 优先级，**不要混塞无关字段**。
7. 三维侧只需组装 JSON；KMZ 内部 `template.kml` + `waylines.wpml` 由后端生成。

---

## 3. 推荐固定模板（已验证可飞风格）

以下默认值来自历史 C++ 服务 `route_convert.hpp` 中可飞报文，**三维服务应优先复用**。

### 3.1 航线级固定字段

```json
{
  "routeName": "solar_route_001",
  "templateType": "waypoint",
  "droneType": 100,
  "subDroneType": 1,
  "payloadType": 99,
  "payloadPosition": 0,
  "finishAction": "goHome",
  "exitOnRcLostAction": "goBack",
  "globalHeight": 80,
  "takeOffSecurityHeight": 20,
  "globalRTHHeight": 100,
  "globalTransitionalSpeed": 10,
  "autoFlightSpeed": 5,
  "imageFormat": "visable",
  "gimbalPitchMode": "usePointSetting",
  "waypointHeadingReq": {
    "waypointHeadingMode": "fixed",
    "waypointHeadingAngle": 0
  },
  "waypointTurnReq": {
    "waypointTurnMode": "toPointAndStopWithDiscontinuityCurvature",
    "useStraightLine": 1
  },
  "startActionList": [
    {
      "actionIndex": 0,
      "hoverTime": 1,
      "gimbalYawRotateAngle": -90,
      "gimbalPitchRotateAngle": -90,
      "zoom": 1,
      "useGlobalImageFormat": 1
    }
  ],
  "routePointList": []
}
```

> 注意：历史 C++ 里 `finishAction` 写过 `returnToHome`，后端枚举标准值是 **`goHome`**。三维服务请统一传 `goHome`。

### 3.2 航点固定骨架

```json
{
  "routePointIndex": 0,
  "longitude": 113.900001,
  "latitude": 22.500001,
  "height": 80.0,
  "speed": 5,
  "gimbalPitchAngle": 0,
  "waypointHeadingReq": {
    "waypointHeadingMode": "fixed",
    "waypointHeadingAngle": 90.0
  },
  "waypointTurnReq": {
    "waypointTurnMode": "toPointAndStopWithDiscontinuityCurvature",
    "useStraightLine": 1
  },
  "actions": []
}
```

### 3.3 精准复拍动作固定骨架（核心）

```json
{
  "actionIndex": 0,
  "takePhotoType": 2,
  "useGlobalImageFormat": 0,
  "imageFormat": "visable",
  "orientedPhotoMode": "normalPhoto",
  "focalLength": 56,
  "gimbalYawRotateAngle": 90.0,
  "gimbalPitchRotateAngle": -30.0,
  "imageWidth": 960,
  "imageHeight": 720,
  "orientedCameraApertue": 440,
  "orientedCameraLuminance": 3800,
  "orientedCameraShutterTime": 0.003,
  "orientedCameraISO": 100,
  "AFPos": 159,
  "focusX": 480,
  "focusY": 360,
  "focusRegionWidth": 480,
  "focusRegionHeight": 360,
  "orientedFileSuffix": "P001"
}
```

---

## 4. 请求体完整字段说明（UavRouteReq）

### 4.1 顶层字段

| 字段 | 类型 | 必填 | 说明 | 三维建议 |
|---|---|---|---|---|
| `routeName` | string | 是 | 航线名，同时作为 KMZ 文件名 | 唯一、无路径分隔符 |
| `templateType` | string | 是 | 模板类型 | 固定 `waypoint` |
| `droneType` | int | 是 | 飞行器机型枚举 | 见 §7.1；历史可飞常用 `100`（M4D/M4TD） |
| `subDroneType` | int | 条件 | 子类型；M30/M3/M3D/M4D 需要 | 历史可飞常用 `1` |
| `payloadType` | int | 是 | 负载类型枚举 | 见 §7.2；历史可飞常用 `99` |
| `payloadPosition` | int | 是 | 负载挂载位置 | 通常 `0` |
| `imageFormat` | string | 建议 | 全局镜头格式 | 精准复拍常用 `visable` |
| `finishAction` | string | 是 | 航线结束动作 | `goHome` / `noAction` / `autoLand` / `gotoFirstWaypoint` |
| `exitOnRcLostAction` | string | 建议 | 失控动作 | `goBack` / `landing` / `hover` |
| `globalHeight` | double | 是 | 全局高度（m） | 与点位高度同坐标系 |
| `autoFlightSpeed` | double | 是 | 全局飞行速度（m/s） | 常用 `5` |
| `takeOffSecurityHeight` | double | 是 | 起飞安全高度（m） | 常用 `20` |
| `globalTransitionalSpeed` | double | 是 | 过渡速度（m/s） | 常用 `10` |
| `globalRTHHeight` | double | 是 | 返航高度（m） | 常用 `100` |
| `takeOffRefPoint` | string | 否 | 参考起飞点 `"lat,lon,height"` 或后端约定格式 | 有机场坐标可填 |
| `gimbalPitchMode` | string | 是 | 云台俯仰控制 | 三维建议 `usePointSetting` |
| `waypointHeadingReq` | object | 是 | 全局偏航模式 | 见 §4.2 |
| `waypointTurnReq` | object | 是 | 全局转弯模式 | 见 §4.3 |
| `startActionList` | array | 否 | 航线开始前动作 | 可空数组；历史常放云台归位+变焦 |
| `routePointList` | array | 是 | 航点列表 | 至少一个点；索引从 0 连续 |
| `mappingTypeReq` | object | 否 | 建图模板参数 | **三维点选不用** |

### 4.2 `waypointHeadingReq`（全局或航点级）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `waypointHeadingMode` | string | 是 | 偏航模式，见 §7.4 |
| `waypointHeadingAngle` | double | 条件 | `fixed` / `smoothTransition` 时需要角度 |
| `waypointPoiPoint` | string | 条件 | `towardPOI` 时兴趣点坐标 |

三维推荐：

```json
{ "waypointHeadingMode": "fixed", "waypointHeadingAngle": 90.0 }
```

### 4.3 `waypointTurnReq`（全局或航点级）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `waypointTurnMode` | string | 是 | 转弯模式，见 §7.5 |
| `useStraightLine` | int | 建议 | `0/1`，是否贴合直线 |
| `waypointTurnDampingDist` | double | 条件 | 协调转弯/过点曲线时的阻尼距离 |

三维推荐（到点停，最稳）：

```json
{
  "waypointTurnMode": "toPointAndStopWithDiscontinuityCurvature",
  "useStraightLine": 1
}
```

### 4.4 `routePointList[]`（RoutePointReq）

| 字段 | 类型 | 必填 | 说明 | 三维建议 |
|---|---|---|---|---|
| `routePointIndex` | int | 是 | 航点序号，从 0 递增 | 必须连续 |
| `longitude` | double | 是 | 经度 | WGS84 |
| `latitude` | double | 是 | 纬度 | WGS84 |
| `height` | double | 强烈建议 | 航点高度（m） | 填绝对高；不填则用全局高 |
| `speed` | double | 否 | 航点速度（m/s） | 可固定 5 |
| `gimbalPitchAngle` | double | 否 | 航点云台俯仰（仅 KML 层） | 可 `0`，真实俯仰放动作里 |
| `waypointHeadingReq` | object | 建议 | 航点偏航 | 与观察方向一致 |
| `waypointTurnReq` | object | 建议 | 航点转弯 | 同全局推荐值 |
| `actions` | array | 否 | 到达该点后的动作序列 | 精准复拍放这里 |
| `timeInterval` | double | 否 | 等时间隔拍照（s） | 三维单点拍摄一般不传 |
| `distanceInterval` | double | 否 | 等距间隔拍照（m） | 一般不传 |
| `endIntervalRouteIndex` | int | 否 | 间隔动作结束航点索引 | 一般不传 |

### 4.5 `actions[]` / `startActionList[]`（PointActionReq）

后端**不是**靠 `actionType` 字段，而是按字段非空 + if-else 优先级推断：

| 优先级 | 判定条件 | 动作类型 | WPML func |
|---|---|---|---|
| 1 | `hoverTime != null` | 悬停 | `hover` |
| 2 | `aircraftHeading != null` | 机头偏航 | `rotateYaw` |
| 3 | `takePhotoType == 0` | 普通拍照 | `takePhoto` |
| 4 | `takePhotoType == 2` | **精准复拍** | `orientedShoot` |
| 5 | `gimbalYawRotateAngle` 或 `gimbalPitchRotateAngle` 非空 | 云台旋转 | `gimbalRotate` |
| 6 | `zoom != null` | 变焦 | `zoom` |
| 7 | `takePhotoType == 1` | 全景拍照 | `panoShot` |
| 8 | `startRecord == true` | 开始录像 | `startRecord` |
| 9 | `stopRecord == true` | 停止录像 | `stopRecord` |

#### 4.5.1 通用字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `actionIndex` | int | 动作序号，从 0 递增 |
| `hoverTime` | double | 悬停秒数 |
| `aircraftHeading` | double | 机头目标偏航角 |
| `aircraftPathMode` | string | `clockwise` / `counterClockwise` |
| `takePhotoType` | int | `0` 普通 / `1` 全景 / `2` 精准复拍 |
| `useGlobalImageFormat` | int | `1` 用全局镜头；`0` 用本动作 `imageFormat` |
| `imageFormat` | string | 镜头：`visable`/`wide`/`zoom`/`ir`/`narrow_band` |
| `gimbalYawRotateAngle` | double | 云台偏航角（相对北，绝对角） |
| `gimbalPitchRotateAngle` | double | 云台俯仰角 |
| `zoom` | double | 变焦焦距/倍率（按机型） |
| `startRecord` | bool | 开始录像 |
| `stopRecord` | bool | 停止录像 |

#### 4.5.2 精准复拍专用字段（`takePhotoType=2`）

| 字段 | 类型 | 必填 | 说明 | 可飞建议值 |
|---|---|---|---|---|
| `takePhotoType` | int | 是 | 固定 `2` | `2` |
| `gimbalYawRotateAngle` | double | 是 | 云台偏航 | 三维计算值 |
| `gimbalPitchRotateAngle` | double | 是 | 云台俯仰 | 三维计算值 |
| `focalLength` | number/string | 是 | 焦距 | 按距离/镜头计算 |
| `imageFormat` | string | 强烈建议 | 镜头 | `visable` |
| `useGlobalImageFormat` | int | 强烈建议 | 是否用全局镜头 | `0` |
| `orientedPhotoMode` | string | 建议 | 模式 | `normalPhoto` |
| `orientedFileSuffix` | string | 建议 | 照片后缀/点位编码 | 如 `PANEL_A01` |
| `imageWidth` | int | 建议 | 图像宽 | `960` |
| `imageHeight` | int | 建议 | 图像高 | `720` |
| `orientedCameraApertue` | int | 建议 | 光圈（字段名历史拼写 Apertue） | `440` |
| `orientedCameraLuminance` | int | 建议 | 亮度 | `3800` |
| `orientedCameraShutterTime` | double | 建议 | 快门 | `0.003` |
| `orientedCameraISO` | int | 建议 | ISO | `100` |
| `AFPos` | double | 可选 | 对焦参数 | 后端当前写死 `159` |
| `focusX` / `focusY` | double | 可选 | 对焦点 | 后端当前写死 `480/360` |
| `focusRegionWidth` / `focusRegionHeight` | double | 可选 | 对焦框 | 可传 `480/360` |

**危险提示：**

- 精准复拍动作对象中**不要**带 `hoverTime`，否则会被优先级 1 识别成悬停。
- 若只要精准复拍，不要额外塞 `zoom` 作为同动作字段；变焦请单独做一个 action，或直接用 `focalLength`。

---

## 5. 三维点选 → 报文组装流程

```
三维场景点击点位
  → 得到 lon/lat/height + 观察 heading/pitch + focalLength
  → 生成 1 个 routePoint
  → actions 放入 1 个 orientedShoot（takePhotoType=2）
  → 多点按顺序累加 routePointIndex
  → 套上 §3.1 固定头
  → POST /buildKmz
  → 得到 KMZ 文件
```

### 5.1 单点拍摄（推荐最少可飞结构）

```json
{
  "routeName": "solar_single_001",
  "templateType": "waypoint",
  "droneType": 100,
  "subDroneType": 1,
  "payloadType": 99,
  "payloadPosition": 0,
  "finishAction": "goHome",
  "exitOnRcLostAction": "goBack",
  "globalHeight": 80,
  "takeOffSecurityHeight": 20,
  "globalRTHHeight": 100,
  "globalTransitionalSpeed": 10,
  "autoFlightSpeed": 5,
  "imageFormat": "visable",
  "gimbalPitchMode": "usePointSetting",
  "waypointHeadingReq": {
    "waypointHeadingMode": "fixed",
    "waypointHeadingAngle": 0
  },
  "waypointTurnReq": {
    "waypointTurnMode": "toPointAndStopWithDiscontinuityCurvature",
    "useStraightLine": 1
  },
  "startActionList": [],
  "routePointList": [
    {
      "routePointIndex": 0,
      "longitude": 113.900123,
      "latitude": 22.500456,
      "height": 85.2,
      "speed": 5,
      "gimbalPitchAngle": 0,
      "waypointHeadingReq": {
        "waypointHeadingMode": "fixed",
        "waypointHeadingAngle": 120.0
      },
      "waypointTurnReq": {
        "waypointTurnMode": "toPointAndStopWithDiscontinuityCurvature",
        "useStraightLine": 1
      },
      "actions": [
        {
          "actionIndex": 0,
          "aircraftHeading": 120.0,
          "aircraftPathMode": "counterClockwise"
        },
        {
          "actionIndex": 1,
          "takePhotoType": 2,
          "useGlobalImageFormat": 0,
          "imageFormat": "visable",
          "orientedPhotoMode": "normalPhoto",
          "focalLength": 56,
          "gimbalYawRotateAngle": 120.0,
          "gimbalPitchRotateAngle": -35.0,
          "imageWidth": 960,
          "imageHeight": 720,
          "orientedCameraApertue": 440,
          "orientedCameraLuminance": 3800,
          "orientedCameraShutterTime": 0.003,
          "orientedCameraISO": 100,
          "AFPos": 159,
          "focusX": 480,
          "focusY": 360,
          "focusRegionWidth": 480,
          "focusRegionHeight": 360,
          "orientedFileSuffix": "PANEL_001"
        }
      ]
    }
  ]
}
```

### 5.2 多点巡检（点选 N 个目标）

每个目标一个航点，动作建议：

1. （可选）`aircraftHeading` 对齐观察方向  
2. `takePhotoType=2` 精准复拍  
3. （可选）`hoverTime` 单独动作，不要和拍照混一个对象

```json
{
  "routeName": "solar_multi_001",
  "templateType": "waypoint",
  "droneType": 100,
  "subDroneType": 1,
  "payloadType": 99,
  "payloadPosition": 0,
  "finishAction": "goHome",
  "exitOnRcLostAction": "goBack",
  "globalHeight": 80,
  "takeOffSecurityHeight": 20,
  "globalRTHHeight": 100,
  "globalTransitionalSpeed": 10,
  "autoFlightSpeed": 5,
  "imageFormat": "visable",
  "gimbalPitchMode": "usePointSetting",
  "waypointHeadingReq": {
    "waypointHeadingMode": "fixed",
    "waypointHeadingAngle": 0
  },
  "waypointTurnReq": {
    "waypointTurnMode": "toPointAndStopWithDiscontinuityCurvature",
    "useStraightLine": 1
  },
  "startActionList": [
    {
      "actionIndex": 0,
      "hoverTime": 1,
      "gimbalYawRotateAngle": -90,
      "gimbalPitchRotateAngle": -90,
      "zoom": 1,
      "useGlobalImageFormat": 1
    }
  ],
  "routePointList": [
    {
      "routePointIndex": 0,
      "longitude": 113.901,
      "latitude": 22.501,
      "height": 82,
      "speed": 5,
      "waypointHeadingReq": {
        "waypointHeadingMode": "fixed",
        "waypointHeadingAngle": 30
      },
      "waypointTurnReq": {
        "waypointTurnMode": "toPointAndStopWithDiscontinuityCurvature",
        "useStraightLine": 1
      },
      "actions": [
        {
          "actionIndex": 0,
          "takePhotoType": 2,
          "useGlobalImageFormat": 0,
          "imageFormat": "visable",
          "orientedPhotoMode": "normalPhoto",
          "focalLength": 48,
          "gimbalYawRotateAngle": 30,
          "gimbalPitchRotateAngle": -40,
          "imageWidth": 960,
          "imageHeight": 720,
          "orientedCameraApertue": 440,
          "orientedCameraLuminance": 3800,
          "orientedCameraShutterTime": 0.003,
          "orientedCameraISO": 100,
          "orientedFileSuffix": "A01"
        }
      ]
    },
    {
      "routePointIndex": 1,
      "longitude": 113.902,
      "latitude": 22.502,
      "height": 83,
      "speed": 5,
      "waypointHeadingReq": {
        "waypointHeadingMode": "fixed",
        "waypointHeadingAngle": 45
      },
      "waypointTurnReq": {
        "waypointTurnMode": "toPointAndStopWithDiscontinuityCurvature",
        "useStraightLine": 1
      },
      "actions": [
        {
          "actionIndex": 0,
          "aircraftHeading": 45,
          "aircraftPathMode": "counterClockwise"
        },
        {
          "actionIndex": 1,
          "takePhotoType": 2,
          "useGlobalImageFormat": 0,
          "imageFormat": "visable",
          "orientedPhotoMode": "normalPhoto",
          "focalLength": 60,
          "gimbalYawRotateAngle": 45,
          "gimbalPitchRotateAngle": -25,
          "imageWidth": 960,
          "imageHeight": 720,
          "orientedCameraApertue": 440,
          "orientedCameraLuminance": 3800,
          "orientedCameraShutterTime": 0.003,
          "orientedCameraISO": 100,
          "orientedFileSuffix": "A02"
        }
      ]
    }
  ]
}
```

### 5.3 仅对齐机头 + 悬停（不拍照）

```json
{
  "actions": [
    { "actionIndex": 0, "aircraftHeading": 90, "aircraftPathMode": "counterClockwise" },
    { "actionIndex": 1, "hoverTime": 2 }
  ]
}
```

### 5.4 普通拍照 / 全景 / 变焦 / 录像

```json
// 普通拍照
{ "actionIndex": 0, "takePhotoType": 0, "useGlobalImageFormat": 1 }

// 全景
{ "actionIndex": 0, "takePhotoType": 1, "useGlobalImageFormat": 1 }

// 变焦
{ "actionIndex": 0, "zoom": 10.0 }

// 云台旋转（非精准复拍）
{ "actionIndex": 0, "gimbalYawRotateAngle": 10, "gimbalPitchRotateAngle": -20 }

// 录像
{ "actionIndex": 0, "startRecord": true }
{ "actionIndex": 1, "stopRecord": true }
```

---

## 6. 三维服务最小字段清单（Checklist）

### 6.1 必须

- [ ] `routeName`
- [ ] `templateType = "waypoint"`
- [ ] `droneType` / `subDroneType` / `payloadType` / `payloadPosition`
- [ ] `finishAction` / `exitOnRcLostAction`
- [ ] `globalHeight` / `autoFlightSpeed` / `takeOffSecurityHeight` / `globalTransitionalSpeed` / `globalRTHHeight`
- [ ] `gimbalPitchMode`
- [ ] `waypointHeadingReq` / `waypointTurnReq`
- [ ] `routePointList` 非空
- [ ] 每个点：`routePointIndex` / `longitude` / `latitude` / `height`
- [ ] 精准复拍：`takePhotoType=2` + 云台 yaw/pitch + `focalLength`

### 6.2 强烈建议

- [ ] 航点 `speed=5`
- [ ] 航点级 `waypointHeadingReq.fixed + angle`
- [ ] 航点级 `waypointTurnReq` 到点停
- [ ] `imageFormat="visable"`
- [ ] `orientedFileSuffix` 填点位编码，便于回传对应
- [ ] `actionIndex` 从 0 连续

### 6.3 不要做

- [ ] 不要把 `hoverTime` 和精准复拍塞进同一个 action 对象  
- [ ] 不要传 `actionType` 字符串（后端不认）  
- [ ] 不要依赖 `actionGroup`（C++ 历史字段，Java 不读）  
- [ ] 不要在 `routeName` 里放 `\` `/` 等路径字符  
- [ ] 不要把 heading=360 直接下发（改为 359）

---

## 7. 枚举字典

### 7.1 `droneType`（飞行器）

| 值 | 机型 |
|---|---|
| 89 | M350 RTK |
| 60 | M300 RTK |
| 67 | M30/M30T |
| 77 | M3E/M3T/M3M |
| 91 | M3D/M3TD |
| 100 | M4D/M4TD（历史可飞常用） |

`subDroneType`：对 67/77/91/100 需要；具体 0/1 按子型号（如双光/三光）配置。

### 7.2 `payloadType`（负载）

| 值 | 负载 |
|---|---|
| 42 | H20 |
| 43 | H20T |
| 52 | M30 双光 |
| 53 | M30T 三光 |
| 61 | H20N |
| 66 | Mavic 3E |
| 67 | Mavic 3T |
| 68 | Mavic 3M |
| 80 | Matrice 3D |
| 81 | Matrice 3TD |
| 82 | H30 |
| 83 | H30T |
| 99 | 历史可飞常用值（按现场机型确认） |
| 65534 | PSDK |

> 机型/负载枚举必须与真实飞机一致，否则可能无法执行。

### 7.3 `finishAction`

| 值 | 含义 |
|---|---|
| `goHome` | 完成后返航（推荐巡检） |
| `noAction` | 完成后退出航线，不自动返航 |
| `autoLand` | 完成后原地降落 |
| `gotoFirstWaypoint` | 完成后回首航点 |

### 7.4 `exitOnRcLostAction`

| 值 | 含义 |
|---|---|
| `goBack` | 返航 |
| `landing` | 降落 |
| `hover` | 悬停 |

### 7.5 `waypointHeadingMode`

| 值 | 含义 |
|---|---|
| `followWayline` | 沿航线方向 |
| `manually` | 手动 |
| `fixed` | 锁定偏航角（三维推荐） |
| `smoothTransition` | 平滑过渡到目标角 |
| `towardPOI` | 朝向兴趣点 |

### 7.6 `waypointTurnMode`

| 值 | 含义 |
|---|---|
| `coordinateTurn` | 协调转弯，不过点 |
| `toPointAndStopWithDiscontinuityCurvature` | 直线到点停（推荐） |
| `toPointAndStopWithContinuityCurvature` | 曲线到点停 |
| `toPointAndPassWithContinuityCurvature` | 曲线过点不停 |

### 7.7 `gimbalPitchMode`

| 值 | 含义 |
|---|---|
| `manual` | 手动 |
| `usePointSetting` | 按航点设置（推荐） |

### 7.8 `imageFormat`

| 值 | 含义 |
|---|---|
| `visable` | 可见光（注意拼写是 visable，不是 visible） |
| `wide` | 广角 |
| `zoom` | 变焦 |
| `ir` | 红外 |
| `narrow_band` | 窄带 |

### 7.9 `aircraftPathMode`

| 值 | 含义 |
|---|---|
| `clockwise` | 顺时针转机头 |
| `counterClockwise` | 逆时针转机头 |

### 7.10 `templateType`（完整）

| 值 | 含义 | 三维点选 |
|---|---|---|
| `waypoint` | 航点飞行 | **使用** |
| `mapping2d` | 建图航拍 | 不用 |
| `mapping3d` | 倾斜摄影 | 不用 |
| `mappingStrip` | 航带飞行 | 不用 |

---

## 8. 后端生成 KMZ 的关键行为（便于排查）

1. 生成 ZIP（存储，不压缩），内部：
   - `wpmz/template.kml`
   - `wpmz/waylines.wpml`
2. KML 高度模式：相对起飞点（模板层）  
3. WPML 执行高度模式：当前代码为 **`WGS84`**  
4. 航点动作触发默认：`reachPoint`（到达航点）  
5. 动作组模式：`sequence`（顺序执行）  
6. 首尾航点若配置协调转弯，会被强制改为“到点停”  
7. `orientedShoot` 中部分对焦参数（`AFPos/focusX/focusY`）后端可能写死，三维侧传了也会被覆盖

---

## 9. 三维服务伪代码

```ts
type Vec3 = { lon: number; lat: number; height: number }
type Shot = Vec3 & {
  heading: number // 机头/观察偏航
  pitch: number   // 云台俯仰（通常负值朝下）
  focalLength: number
  pointCode: string
}

function clampHeading(h: number) {
  return h === 360 ? 359 : h
}

function buildOrientedAction(shot: Shot, actionIndex: number) {
  return {
    actionIndex,
    takePhotoType: 2,
    useGlobalImageFormat: 0,
    imageFormat: 'visable',
    orientedPhotoMode: 'normalPhoto',
    focalLength: shot.focalLength,
    gimbalYawRotateAngle: clampHeading(shot.heading),
    gimbalPitchRotateAngle: shot.pitch,
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
    orientedFileSuffix: shot.pointCode
  }
}

function buildBuildKmzBody(routeName: string, shots: Shot[]) {
  return {
    routeName,
    templateType: 'waypoint',
    droneType: 100,
    subDroneType: 1,
    payloadType: 99,
    payloadPosition: 0,
    finishAction: 'goHome',
    exitOnRcLostAction: 'goBack',
    globalHeight: 80,
    takeOffSecurityHeight: 20,
    globalRTHHeight: 100,
    globalTransitionalSpeed: 10,
    autoFlightSpeed: 5,
    imageFormat: 'visable',
    gimbalPitchMode: 'usePointSetting',
    waypointHeadingReq: {
      waypointHeadingMode: 'fixed',
      waypointHeadingAngle: 0
    },
    waypointTurnReq: {
      waypointTurnMode: 'toPointAndStopWithDiscontinuityCurvature',
      useStraightLine: 1
    },
    startActionList: [],
    routePointList: shots.map((s, i) => ({
      routePointIndex: i,
      longitude: s.lon,
      latitude: s.lat,
      height: s.height,
      speed: 5,
      gimbalPitchAngle: 0,
      waypointHeadingReq: {
        waypointHeadingMode: 'fixed',
        waypointHeadingAngle: clampHeading(s.heading)
      },
      waypointTurnReq: {
        waypointTurnMode: 'toPointAndStopWithDiscontinuityCurvature',
        useStraightLine: 1
      },
      actions: [
        {
          actionIndex: 0,
          aircraftHeading: clampHeading(s.heading),
          aircraftPathMode: 'counterClockwise'
        },
        buildOrientedAction(s, 1)
      ]
    }))
  }
}
```

---

## 10. 调用示例

### 10.1 HTTP

```http
POST /buildKmz HTTP/1.1
Host: <wayline-service-host>:<port>
Content-Type: application/json
Accept: application/vnd.google-earth.kmz

{...UavRouteReq JSON...}
```

### 10.2 curl

```bash
curl -X POST "http://127.0.0.1:6789/buildKmz" \
  -H "Content-Type: application/json" \
  -d @route.json \
  --output solar_route_001.kmz
```

---

## 11. 常见失败原因（飞不起来/导入失败）

| 现象 | 可能原因 | 处理 |
|---|---|---|
| KMZ 导入失败 | 机型/负载枚举不匹配 | 改成现场真实 `droneType/payloadType` |
| 动作不执行 | 动作字段混用导致类型误判 | 一个 action 只表达一种动作 |
| 拍不到目标 | yaw/pitch/focalLength 错 | 三维计算后直接透传，单位角度+焦距 |
| 高度异常 | 相对高/绝对高混用 | 与 WPML `WGS84` 保持一致，统一绝对高 |
| 首尾转弯异常 | 用了协调转弯 | 改用到点停 |
| 照片难对应点位 | 没传 `orientedFileSuffix` | 传点位编码 |
| 文件名异常 | `routeName` 含非法字符 | 仅字母数字下划线中文 |

---

## 12. 给三维云渲染的最终约定（摘要）

1. 接口：`POST /buildKmz`  
2. 模板：只发 `waypoint`  
3. 每个三维点 = 1 个 `routePoint`  
4. 每个点至少 1 个精准复拍 action：`takePhotoType=2`  
5. 角度：`gimbalYawRotateAngle=heading`，`gimbalPitchRotateAngle=pitch`  
6. 焦距：`focalLength`  
7. 固定机型参数先用可飞模板（100/1/99/0），再按现场飞机校准  
8. 结束动作巡检用 `goHome`  

---

## 13. 文档维护

| 项 | 值 |
|---|---|
| 后端模块 | `dfe-wayline` |
| 入口类 | `UavRouteController#buildKmz` |
| 请求模型 | `UavRouteReq` / `RoutePointReq` / `PointActionReq` |
| 生成核心 | `RouteFileUtils` |
| 历史可飞参考 | `D:\uav\157beifen\include\route_convert.hpp` |

如机型、镜头、默认高度/速度变更，请同步更新本文 §3 与 §7。
