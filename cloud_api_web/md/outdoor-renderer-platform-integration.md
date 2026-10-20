# 室外无人机云渲染平台接入说明

本文面向室外无人机平台前端 / 前端 AI，说明当前 `outdoor` 云渲染服务的功能、启动方式、HTTP 接口、WebSocket 信令、渲染控制消息与实时数据格式。

## 1. 当前 outdoor 能力

- 服务端使用 Puppeteer 启动 headless Chrome，在 `outdoor-renderer.html` 内渲染 3DGS 场景。
- 前端通过 WebRTC 接收服务端渲染出的视频流，通过 WebSocket 发送交互控制。
- 支持 3DGS + WGS84 georef 坐标链路：无人机 MQTT 经纬度可实时映射到 3DGS 场景坐标。
- 支持 DJI 上云 API MQTT 实时数据：无人机位置、偏航、机巢位置、在舱 / 出舱状态。
- 支持无人机模型显示：桨叶动画常驻播放、荧光描边、实时偏航。
- 支持机巢位置标记：悬浮旋转荧光圆锥 + 状态文字（无人机在舱内 / 无人机已出舱）。
- 支持飞行轨迹显示：青色荧光轨迹线，可清除。
- 支持当前执行计划航线：完整航线、已完成航段、剩余航段、当前航点和数字序号。
- 支持同一个 Node 服务按 session 同时创建 `outdoor` 和 `align` 页面。
- 支持云渲染三维航线规划（独立 `wayline` session）：复用 outdoor 的 3DGS/georef 坐标链路，支持双击加航点、航点列表、位置/姿态/镜头编辑、无人机模型显示、拍摄视锥、机载视角（FPV）预览、过渡点，以及生成大疆 KMZ 航线文件。
- 支持双击 3DGS 表面拾取 WGS84 坐标。
- 支持多客户端共享同一个 outdoor renderer 和像素流，交互状态共用；每个客户端使用独立 WebRTC peer。
- 支持 WebRTC 断线自动恢复：ICE failed 后只重建对应客户端的 RTCPeerConnection，不影响其他客户端。
- 支持会话关闭后回收浏览器，避免 WebGL 上下文泄漏导致刷新后黑屏。

## 2. 服务启动

推荐 outdoor 模式：

```powershell
$env:RENDERER="outdoor"
npm start
```

常用环境变量：

```text
PORT=3000
PUBLIC_ORIGIN=http://127.0.0.1:3000
RENDERER=outdoor

# 仅在请求没有传 renderer 时生效；支持 three/cesium/align/outdoor
# RENDERER=outdoor

STREAM_WIDTH=1920
STREAM_HEIGHT=1080
STREAM_FPS=30
STREAM_BITRATE=8000000

MQTT_ENABLED=1
MQTT_HOST=172.20.63.157
MQTT_PORT=9205
MQTT_PROTOCOL=mqtt
MQTT_USERNAME=df1560
MQTT_PASSWORD=dfe2k_1500
MQTT_TOPIC=thing/product/+/osd
MQTT_EVENTS_TOPIC=thing/product/+/events
MQTT_DEBUG_LOGS=0

CLOUD_API_ROUTE_ENABLED=1
CLOUD_API_BASE=http://172.20.63.157:6789
CLOUD_API_USERNAME=adminPC
CLOUD_API_PASSWORD=adminPC
CLOUD_API_ROUTE_POLL_MS=5000

MAX_PLANNED_WAYPOINTS=2000
MAX_KMZ_BYTES=25000000

SESSION_IDLE_TIMEOUT_MS=5000
```

推荐模型参数：

```text
3DGS 文件：3dgs/7.1.1.ply
参考倾斜摄影：3dgs/dfelanqiuchang/tileset.json
georef 文件：models/3dgs/7.1.1.georef.json
```

## 3. 前端接入总流程

```text
1. POST /api/session 创建或复用相同配置的渲染会话
2. 打开 WebSocket /signal?role=client&session=<id>
3. 等服务端 renderer-ready
4. 前端创建 RTCPeerConnection，发送 offer
5. 通过信令交换 answer / ice
6. 前端 ontrack 接收视频流并绑定到 video
7. 前端通过信令发送 input / drone-control / drone-set 等控制消息
8. 页面关闭时 POST /api/session/:id/close；共享会话仍有其他客户端时不会销毁 renderer
```

## 4. HTTP 接口

### 4.1 创建会话

```http
POST /api/session
Content-Type: application/json
```

`renderer` 为可选字段。传入后按当前 session 选择页面；不传时回退到服务启动时的 `RENDERER` 环境变量。

支持的模式：

```text
three   -> /renderer.html
cesium  -> /cesium-renderer.html
align   -> /align-renderer.html
outdoor -> /outdoor-renderer.html
wayline -> /wayline-renderer.html

### 4.6 云渲染三维航线规划（wayline）

在室外 3DGS 实景场景中点选拍摄目标、编辑拍摄姿态、预览机载视角并生成大疆 KMZ 航线文件。复用 outdoor 的 3DGS/georef 坐标链路，独立 session，不影响 outdoor 和 align。

#### 4.6.1 创建 session

```json
{
  "renderer": "wayline",
  "pointCloudFile": "3dgs/7.1.1.ply",
  "rendererParams": {
    "alignSplatFile": "3dgs/7.1.1.ply",
    "alignReferenceTileset": "3dgs/dfelanqiuchang/tileset.json"
  }
}
```

wayline session 每次独立创建，不共享（避免多操作者互相修改同一份航线）。信令、WebRTC 视频流与 outdoor 一致；一个 renderer 支持多客户端观看，answer/ICE 按 clientId 路由。

#### 4.6.2 页面能力

- 双击 3DGS 画面在模型表面添加 WGS84 航点。
- 航点编号标签、航线折线、选中高亮。
- 航点列表（名称、经纬度、高度），点选切换当前航点；点击名称可就地编辑航点名称。
- 航点删除、清空、选择。
- 位置微调：高度、地理方向（东南西北）、机体方向（前后左右，随航向旋转）。
- 姿态与拍摄：航向、俯仰、滚转、35mm 等效焦距、速度、拍摄镜头。
- 拍摄镜头支持：不拍照(过渡点)、可见光、红外、可见光+红外。
- 选中航点显示无人机模型，机头朝当前航向。
- 拍照航点显示半透明拍摄视锥，并在右上角提供机载第一视角（FPV）画中画预览。
- 每个航点支持最多 10 个拍照动作（`photo_actions`：镜头/焦距/俯仰/云台偏航逐动作独立，见 4.6.4）；三维场景为每个动作画一个视锥（颜色按动作顺序区分），FPV 画中画跟随选中的动作（默认第一个，`select-action` 切换）。
- 过渡点（不拍照）不显示视锥/FPV，但显示无人机与朝向。
- 规划状态通过 `wayline-state` 推送。
- 通过 `/api/wayline/build-kmz` 代理调用后端 `/buildKmz` 生成并下载 KMZ。

#### 4.6.3 跟随标记模式

wayline 还支持“跟随标记模式”，用于先控制无人机在三维场景中移动，再按当前无人机位置和拍摄姿态落航点。该模式与原有双击编辑模式共存，可以随时切换，不新建独立航线，也不改变 KMZ 导出接口。

##### 用户流程

1. 在编辑模式下双击 3DGS 表面，添加第一个航点并自动选中，作为跟随无人机的起始位置。
2. 点击“切换跟随模式”。主视角切换到无人机后上方的第三人称追踪视角，右上角继续显示无人机 FPV。
3. 平台转发控制指令，驱动跟随无人机前后左右、升降和偏航；航向/俯仰调节用于修改当前无人机的拍摄姿态。
4. 到达目标位置并调整视角后，点击“打点”。renderer 将当前无人机的 WGS84 椭球高位置、真实北向航向、俯仰、滚转和焦距保存为新航点。
5. 继续飞行并重复打点；点击“切换编辑模式”后，可继续使用原有航点列表、双击加点和航点参数编辑。

两种模式共享以下状态：

```text
waypoints / selectedIndex / wayline-state / KMZ 导出
```

##### 客户端发送给 renderer 的消息

进入跟随模式：

```json
{
  "type": "follow-mode",
  "follow": true
}
```

退出跟随模式：

```json
{
  "type": "follow-mode",
  "follow": false
}
```

保存当前跟随无人机为新航点：

```json
{
  "type": "follow-drop"
}
```

跟随模式下，平台可以继续复用现有无人机控制消息：

```json
{
  "type": "drone-control",
  "action": "forward"
}
```

`action` 支持：

```text
forward     沿无人机机头方向前进
back        沿机头方向后退
left        向左平移
right       向右平移
up          上升
down        下降
yaw-left    左转
yaw-right   右转
```

航点面板在跟随模式下不再更新已选航点，而是发送实时跟随调整消息：

```json
{
  "type": "follow-adjust",
  "yawDeg": 5,
  "pitchDeg": -5,
  "heightM": 1,
  "eastM": 0,
  "northM": 0,
  "forwardM": 0,
  "rightM": 0
}
```

上述字段均为可选字段，一次消息只传需要调整的量即可：

| 字段 | 含义 |
|---|---|
| `yawDeg` | 当前无人机机头偏航增量，单位为度 |
| `pitchDeg` | 当前拍摄视角俯仰增量，单位为度 |
| `heightM` | 沿 WGS84 椭球高方向升降，单位为米 |
| `eastM` / `northM` | 按真实地理方向平移，单位为米 |
| `forwardM` / `rightM` | 按当前机头方向和机头右侧平移，单位为米 |

##### renderer 回传状态

`wayline-state` 继续回传完整航点列表，并新增：

```json
{
  "type": "wayline-state",
  "payload": {
    "followMode": true,
    "selectedIndex": 1,
    "waypoints": []
  }
}
```

平台应使用 `payload.followMode` 同步“切换跟随模式”按钮状态。跟随模式下新建航点后，renderer 会更新 `selectedIndex` 并立即回传完整 `wayline-state`。

##### 室外无人机平台接入要求

室外无人机平台需要完成以下接入：

1. wayline 页面增加“切换跟随模式”和“打点”入口，或由平台自己的工具栏触发相同消息。
2. 将平台的前后左右、升降、偏航控制映射为 `drone-control` 消息并转发到当前 wayline session。
3. 将航向/俯仰微调映射为 `follow-adjust`，不要在平台本地直接修改已选航点。
4. 监听 `wayline-state.payload.followMode`、`selectedIndex` 和 `waypoints`，同步平台航点列表和按钮状态。
5. 保留原有 `wayline-command` 编辑模式，跟随模式关闭后继续使用原有航点编辑逻辑。
6. “打点”完成后仍使用现有 `/api/wayline/build-kmz` 生成航线文件，无需新增 KMZ 接口。

跟随模式的控制消息不改变 outdoor 实时遥测消息；它只作用于独立的 `wayline` 规划 session。实际无人机飞控控制仍由平台原有飞控链路负责，跟随模式中的无人机是三维规划预览实体，不会直接控制真机。

#### 4.6.4 航点数据结构

```json
{
  "point_name": "WP_001",
  "longitude": 121.3658,
  "latitude": 37.5198,
  "height": 120.5,
  "capture_mode": "visable",
  "speed": 5,
  "camera_params": {
    "heading": 90,
    "pitch": -45,
    "roll": 0,
    "focalLength": 75
  },
  "photo_actions": [
    { "imageFormat": "visable", "focalLength": 30, "pitch": -45 },
    { "imageFormat": "visable,ir", "focalLength": 75, "pitch": -45, "yaw": 90 }
  ]
}
```

| 字段 | 含义 |
|---|---|
| `point_name` | 航点名称，同时用作 KMZ 导出的照片文件名后缀（`orientedFileSuffix`）；保存时自动净化，规则见 4.6.5 |
| `longitude` / `latitude` | WGS84 经纬度（度） |
| `height` | WGS84 绝对椭球高（米） |
| `capture_mode` | 拍摄镜头（单动作模式）：`none` 不拍照 / `visable` 可见光 / `ir` 红外 / `visable,ir` 都拍 |
| `photo_actions` | 可选，多拍照动作数组，每项 `{ imageFormat, focalLength, pitch, yaw? }`（镜头组合 / 35mm 等效焦距 / 俯仰角 -90~0 / 可选云台偏航——真实北向 0-360，缺省跟随航点机头），最多 10 个。**存在时按它生成拍照动作并覆盖 `capture_mode`**：空数组 = 过渡点；缺省 = 回退 `capture_mode` 单动作模式（完全向后兼容）。三维场景每个动作画一个视锥（颜色按动作顺序），FPV 预览跟随第一个动作 |
| `speed` | 航点速度（m/s） |
| `camera_params.heading` | 对外统一使用的真实偏航角（度，0=北，顺时针为正）；renderer 内部另存场景偏航，仅用于渲染 |
| `camera_params.pitch` | 俯仰角（度，水平 0，向下为负，范围 -90~0） |
| `camera_params.roll` | 滚转角（度） |
| `camera_params.focalLength` | 35mm 等效焦距（毫米） |

#### 4.6.5 客户端 -> renderer：wayline-command

添加航点由双击画面触发（见 4.6.6）；其余通过 `wayline-command`：

```json
{ "type": "wayline-command", "action": "select", "index": 0 }
{ "type": "wayline-command", "action": "select-action", "index": 0, "actionIndex": 1 }
{ "type": "wayline-command", "action": "remove", "index": 0 }
{ "type": "wayline-command", "action": "clear" }
{ "type": "wayline-command", "action": "set-route-name", "value": "wayline_route_001" }
```

更新航点（顶层字段与相机参数可分别 patch，均为浅合并，不覆盖未传字段）：

```json
{
  "type": "wayline-command",
  "action": "update",
  "index": 0,
  "patch": {
    "point_name": "WP_001",
    "longitude": 121.3658,
    "latitude": 37.5198,
    "height": 120.5,
    "speed": 5,
    "capture_mode": "visable",
    "camera_params": {
      "heading": 90,
      "pitch": -45,
      "roll": 0,
      "focalLength": 75
    }
  }
}
```

| action | 说明 |
|---|---|
| `select` | 选中航点（index 从 0 开始），显示无人机/视锥/FPV；同时把动作预览重置为第一个 |
| `select-action` | 选中当前航点的第 N 个拍照动作（actionIndex 从 0 开始），FPV 画中画与视锥高亮跟随该动作 |
| `update` | 更新航点，`patch` 支持名称（point_name）、经纬度、高度、速度、capture_mode、camera_params 和拍照动作（photo_actions，见 4.6.4） |
| `move-body` | 按机体方向（前后左右，米）移动航点，renderer 用 georef 解算 |
| `remove` | 删除指定航点 |
| `clear` | 清空全部航点 |
| `set-route-name` | 设置航线名称 |

航点名称（`point_name`）编辑：

```json
{ "type": "wayline-command", "action": "update", "index": 0, "patch": { "point_name": "变电所北侧_01" } }
```

- 名称同时用作 KMZ 导出的照片文件名后缀（`orientedFileSuffix`），renderer 保存前自动净化：去首尾空格、最长 64 字符、`\/ : * ? " < > |` 与空白字符替换为 `_`。
- 兼容性：输入侧兼容驼峰别名 `pointName`（`update` 的 patch 和 `load` 的航点均可）；协议输出统一为下划线 `point_name`。
- 净化后为空字符串时忽略本次名称修改（保留原名），不报错。
- 名称不要求唯一；修改成功后照常回推 `wayline-state`，平台以回推数据刷新列表，不要本地维护副本。
- 三维场景中的航点标签始终显示序号数字，名称只在列表和 KMZ 中生效。

平台加载整条航线（整体替换当前航点）：

```json
{
  "type": "wayline-command",
  "action": "load",
  "routeName": "wayline_route_001",
  "selectedIndex": 0,
  "waypoints": [
    {
      "point_name": "变电所北侧_01",
      "longitude": 121.3658,
      "latitude": 37.5198,
      "height": 120.5,
      "capture_mode": "visable",
      "speed": 5,
      "headingTrue": 90,
      "camera_params": { "heading": 90, "pitch": -45, "roll": 0, "focalLength": 75 }
    }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `routeName` | 可选，航线名称 |
| `selectedIndex` | 可选，加载后选中的航点（从 0），缺省为 0 |
| `waypoints[].point_name` | 航点名称，缺省时 renderer 自动分配 `WP_001` 风格默认名 |
| `waypoints[].headingTrue` / `camera_params.heading` | 真实北向偏航（度），renderer 内部自行转场景角 |

> **重要（加载时名称必读）**：`load` 是整体替换，平台加载的方案优先于本服务自动恢复的持久化方案（见 4.6.10）。报文中每个航点都应携带 `point_name`（平台从 KMZ 回读航线时，把拍照动作里的 `orientedFileSuffix` 映射回来）；未带名称时 renderer 会按水平位置自动回填作兜底。全链路说明与自查方法见 4.6.11。

位置微调、姿态微调：

- 高度：`update` 的 `patch.height`。
- 地理方向（东南西北，米）：换算成经纬度增量后 `update` 的 `patch.longitude/latitude`。
- **机体方向（前后左右，米）：必须用 `move-body`，不要平台本地算**（见下方重要说明）。
- 航向/俯仰：`update` 的 `patch.camera_params.heading` / `patch.camera_params.pitch`。

> **重要（机体前后左右移动必读）**：机体方向（前=机头、右=机头右侧）**不能**在平台本地用 `camera_params.heading`（场景角）套 sin/cos 换算经纬度。3DGS 场景与真实地理是**镜像**关系（换算斜率 -1），平台没有 georef，本地算在部分偏航角会出现「左右反向/镜像」。必须发 `move-body` 交给 renderer 解算：
>
> ```json
> {
>   "type": "wayline-command",
>   "action": "move-body",
>   "index": 0,
>   "forward": 1,
>   "right": 0
> }
> ```
>
> | 字段 | 含义 |
> |---|---|
> | `index` | 航点序号（从 0） |
> | `forward` | 沿机头方向移动的米数（负值向后） |
> | `right` | 沿机头右侧移动的米数（负值向左） |
>
> renderer 用与 KMZ 导出相同的 georef 换算解算真实方位并更新航点，任何偏航角都正确。移动后照常回推 `wayline-state`。地理方向（东南西北）不受此影响，仍可用 `update` 改经纬度。

#### 4.6.6 renderer -> 客户端：wayline-state

任何航点变化后推送完整规划状态，平台据此渲染航点列表、回填编辑面板：

```json
{
  "type": "wayline-state",
  "payload": {
    "routeName": "wayline_route_001",
    "selectedIndex": 0,
    "selectedActionIndex": 0,
    "waypoints": [
      {
        "point_name": "WP_001",
        "longitude": 121.3658,
        "latitude": 37.5198,
        "height": 120.5,
        "capture_mode": "visable",
        "speed": 5,
        "headingTrue": 10.6,
        "camera_params": { "heading": 359, "pitch": -45, "roll": 0, "focalLength": 75 }
      }
    ]
  }
}
```

注意：`selectedIndex` 从 0 开始；场景中的航点编号标签从 1 开始显示。`selectedActionIndex` 是当前 FPV 预览的拍照动作序号（从 0，仅对该航点有 `photo_actions` 时有意义），由 `select-action` 切换、`select` 切航点时重置为 0。

> **重要（偏航角必读）**：平台协议和页面只使用一个偏航：真实北向偏航。`headingTrue` 作为兼容字段继续回推，但与 `camera_params.heading` 表示同一个真实偏航值；renderer 内部可将它转换为私有场景角，平台不需要也不应该读取或计算场景角。
>
> | 字段 | 含义 | 用途 |
> |---|---|---|
> | `camera_params.heading` | **真实北向偏航**：0=正北，顺时针为正 | 页面显示、编辑、下发飞机、生成 KMZ |
> | `headingTrue` | `camera_params.heading` 的兼容回显字段，数值相同 | 兼容旧平台读取 |
>
> 3DGS 场景经 georef 转换后，与真实地理北向存在固定旋转（例如内部场景 359° 实际对应真实 10°）。该内部角度不通过平台协议暴露，平台无需处理这个转换。
>
> 平台正确用法：
>
> ```js
> // 拼 buildKmz 报文时，机头和云台偏航使用统一的真实偏航
> const heading = point.camera_params.heading;
> aircraftHeading = heading;           // 机头对齐
> gimbalYawRotateAngle = heading;      // 云台偏航
>
> // 面板统一展示和编辑这个真实偏航；不要再增加“场景偏航”控件。
> ```
>
> renderer 会随每次 `wayline-state` 同时回推 `camera_params.heading` 和兼容字段 `headingTrue`，两者数值一致，无需平台自行换算。

#### 4.6.7 视角与拾取

复用 outdoor 输入消息：

```text
input/orbit  旋转相机
input/pan    平移相机
input/zoom   缩放
```

双击画面添加航点：

```json
{ "type": "viewport-double-click", "x": 0.5, "y": 0.4 }
```

`x`/`y` 为归一化视口坐标（0~1）。renderer 在 3DGS 表面拾取命中点，反算为 WGS84 并新增航点。

#### 4.6.8 生成 KMZ

```http
POST /api/wayline/build-kmz
Content-Type: application/json
Accept: application/vnd.google-earth.kmz
```

请求体为 `dfe-wayline` 的 `buildKmz` 报文（`templateType=waypoint`）。服务端将其转发到：

```text
WAYLINE_API_BASE + WAYLINE_BUILD_KMZ_PATH
```

代理已实现：Cloud API 登录鉴权并转发 `x-auth-token`、401 自动刷新重试、请求超时、请求体大小限制（仅该路由放宽）、航点数量限制、返回体大小限制、KMZ 下载响应头。成功返回 `.kmz` 二进制流。

默认环境变量：

```text
WAYLINE_API_ENABLED=1
WAYLINE_API_BASE=http://172.20.63.157:6789
WAYLINE_BUILD_KMZ_PATH=/buildKmz
WAYLINE_API_TIMEOUT_MS=60000
MAX_WAYLINE_REQUEST_BYTES=5000000
```

报文组装要点：

- 每个航点 = 一个 `routePoint`，`routePointIndex` 从 0 连续。
- 高度使用 WGS84 绝对椭球高。
- 拍照航点追加 `takePhotoType=2` 精准复拍动作，`imageFormat` 取自 `capture_mode`（历史拼写 `visable`）。
- 多拍照动作：航点带 `photo_actions` 时每项生成一个拍照动作，`actionIndex` 从 1 起连续顺延，`orientedFileSuffix` 均为该航点 `point_name`；`gimbalYawRotateAngle` 取该动作的 `yaw`（未配置取航点机头偏航）；缺省回退 `capture_mode` 单动作模式。
- 拍照动作的 `actions[].orientedFileSuffix` 填航点的 `point_name`（照片文件名后缀）。这是航点名称进出 KMZ 的唯一载体，映射缺失会导致名称在保存/回读往返中丢失，详见 4.6.11。
- 过渡点（`capture_mode=none`）不追加拍照动作，仅保留机头对齐；其名称因此不会写入 KMZ。
- **偏航使用 `headingTrue`（真实北向），不是 `camera_params.heading`（场景角）**，`aircraftHeading` 与 `gimbalYawRotateAngle` 均取 `headingTrue`。详见 4.6.5 的偏航角说明。
- `heading` 规范化到 `[0,360)`，其中 360 改为 359。
- 机型默认 `droneType=100 / subDroneType=1 / payloadType=99`，`finishAction=goHome`。

实际部署前应按现场飞机和负载校准机型/负载枚举、相机参数及 yaw/pitch 补偿。

#### 4.6.9 平台接入建议

1. 创建 `renderer=wayline` session，接入 WebRTC 视频与信令。
2. 提供三维画面双击加点、航点列表、航点编辑面板（位置/姿态/镜头）。
3. 监听 `wayline-state` 刷新列表和面板；操作通过 `wayline-command` 下发。
4. 需要生成航线时，用当前 `wayline-state` 的航点组装 `buildKmz` 报文，POST 到 `/api/wayline/build-kmz` 下载 KMZ。
   偏航务必用 `headingTrue`（真实北向），不要用 `camera_params.heading`（场景角），否则实拍会整体偏角。
5. 校验航点：经纬度范围、高度有限、速度正数、拍照点焦距正数、航点数上限。
6. 面板展示偏航时，"真实偏航（下发飞机）"显示 `headingTrue`；如需展示可拖动的场景朝向，另行显示 `camera_params.heading`。

#### 4.6.10 航线方案持久化（本服务内置）

wayline 会话的航点（含编辑过的 `point_name`）由服务自动持久化到 `data/wayline-plan.json`（可用环境变量 `WAYLINE_PLAN_FILE` 改路径）：

- renderer 每次航点变化（加点/改名/移动/删除/清空）后 800ms 自动 `POST /api/wayline/plan`（防抖合并）。
- **会话不从该文件自动恢复场景**：wayline 会话一律从空场景开始，是否加载航线完全由平台 `load` 命令决定——平台点"新建航线"不发 load，编辑器就是干净的空场景，不会出现上次编辑的航线。
- 该文件的用途是**名称来源**：平台 load 未带 `point_name` 时，renderer 按水平位置（≤0.2 米）从文件中的航点自动回填名称（见 4.6.11 环节 6）；也可人工查看、排查名称数据。
- 客户端"清空航点"会把空方案持久化，相当于清除存储。

接口（一般无需平台调用，renderer 全自动）：

```http
GET /api/wayline/plan
-> { "ok": true, "exists": true, "plan": { "version": 1, "updatedAt": "...", "routeName": "wayline_route", "waypoints": [ ... ] } }

POST /api/wayline/plan
{ "routeName": "wayline_route_001", "waypoints": [ { "point_name": "变电所北侧_01", "longitude": 121.3, "latitude": 37.5, "height": 120.5 } ] }
```

- `waypoints` 使用与 `wayline-state` 相同的公开形态：`camera_params.heading` / `headingTrue` 都是真实北向偏航，保存与恢复会正确往返换算。
- 服务端校验：航点数不超过 `MAX_PLANNED_WAYPOINTS`，经纬高必须有效，`point_name` 按相同规则净化。
- 该文件是单机运行时状态（已加入 `.gitignore`）。平台若上线自己的航线库，以平台 + `load`（带 `point_name`）为持久化来源即可，两者互不冲突。

#### 4.6.11 航点名称全链路（编辑 → 生成 → 入库 → 回读）

背景：KMZ/WPML 没有独立的"航点名称"字段。名称只存在于两处——规划协议的 `point_name`，以及 KMZ 拍照动作里的 `orientedFileSuffix`（照片文件名后缀）。名称进出 KMZ 必须搭 `orientedFileSuffix`，因此整条链路上任何一环丢弃它，名称就会在往返中丢失。

| 环节 | 动作 | 责任方 |
|---|---|---|
| 1. 编辑 | 平台编辑器改名后发 `wayline-command update`，`patch: { point_name }`（兼容驼峰 `pointName`） | 平台 |
| 2. 短期存储 | renderer 自动持久化到 `data/wayline-plan.json`（见 4.6.10） | 本服务（自动） |
| 3. 生成 KMZ | 组装 `buildKmz` 报文时，把拍照航点的 `point_name` 填进 `actions[].orientedFileSuffix` | 报文组装方（平台直连后端或本服务测试页） |
| 4. 入库/覆盖 | KMZ 保存进平台航线库 | 平台 |
| 5. 回读/加载 | 平台解析 KMZ 后下发 `load`，把 `orientedFileSuffix` 映射回 `point_name` | 平台 |
| 6. 兜底 | load 未带名称（或仍为 `WP_0XX`）时，renderer 按水平位置 ≤0.2 米从当前内存航线（或会话启动时缓存的持久化方案）自动回填名称 | 本服务（自动） |

各环节自查方法（均以本服务日志/产物为准）：

- 环节 1：改名时看服务日志，应出现 `Signal client -> renderer: wayline-command:update`；没有则平台未发命令。
- 环节 3：解压生成的 KMZ（改后缀 `.zip`），在 `wpmz/waylines.wpml` 中搜索自定义名称或 `orientedFileSuffix`；搜不到说明组装报文时未填。
- 环节 5：重开三维编辑时看服务日志 `[wayline] load <名称>: ...`；`load undefined` 说明报文未带名称（此时走环节 6 兜底）。

已知限制：

- 过渡点（`capture_mode=none`）没有拍照动作，名称不会写入 KMZ；其名称跨 KMZ 往返只能靠环节 2 + 环节 6 兜底。
- 环节 6 的坐标回填以航点位置未变为前提；平台覆盖航线时若挪动了航点，对不上的点恢复默认名。
- 跨设备部署（编辑与打开不在同一台渲染服务器）时环节 2/6 失效，环节 5 是唯一可靠通道。

配准页面需要待配准 3DGS 和参考倾斜摄影两个模型：

```json
{
  "renderer": "align",
  "pointCloudFile": "3dgs/7.1.1.ply",
  "rendererParams": {
    "alignSplatFile": "3dgs/7.1.1.ply",
    "alignReferenceTileset": "3dgs/dfelanqiuchang/tileset.json"
  }
}
```

outdoor 页面可以用同样的 3DGS 和参考模型参数读取对应 georef，但只渲染 3DGS：

```json
{
  "renderer": "outdoor",
  "pointCloudFile": "3dgs/7.1.1.ply",
  "rendererParams": {
    "alignSplatFile": "3dgs/7.1.1.ply",
    "alignReferenceTileset": "3dgs/dfelanqiuchang/tileset.json"
  }
}
```

响应中包含实际生效模式和是否复用：

```json
{
  "id": "session-id",
  "renderer": "align",
  "pointCloudFile": "3dgs/7.1.1.ply",
  "shared": false
}
```

只有相同配置的 `outdoor` session 会共享一个 Puppeteer renderer；`align`、`cesium`、`three` 每次创建独立页面，因此 `outdoor` 和 `align` 可以同时运行。

请求示例：

```json
{
  "pointCloudFile": "3dgs/7.1.1.ply",
  "rendererParams": {
    "alignSplatFile": "3dgs/7.1.1.ply",
    "alignReferenceTileset": "3dgs/dfelanqiuchang/tileset.json"
  }
}
```

响应示例：

```json
{
  "id": "session-uuid",
  "pointCloudFile": "3dgs/7.1.1.ply"
}
```

说明：

- `pointCloudFile` 是会话主点云文件。
- `rendererParams.alignSplatFile` 是 outdoor/align 实际使用的 3DGS 文件。
- `rendererParams.alignReferenceTileset` 是 georef 对齐时的参考倾斜摄影。

### 4.2 关闭会话

```http
POST /api/session/:id/close
```

或：

```http
DELETE /api/session/:id
```

关闭后服务端会关闭 renderer page；若无其他会话，会回收整个浏览器释放 WebGL/GPU 资源。

### 4.3 健康检查

```http
GET /health
```

响应示例：

```json
{
  "ok": true,
  "sessions": 1
}
```

### 4.4 资源列表

```http
GET /api/point-clouds
GET /api/reference-tilesets
```

用于前端下拉选择 3DGS 文件和参考倾斜摄影 tileset。

### 4.5 georef 读取 / 保存

配准结果按待配准 3DGS 文件名独立保存。

读取：

```http
GET /api/align/georef?splat=3dgs%2F7.1.1.ply
```

不存在时：

```json
{
  "ok": true,
  "exists": false,
  "georef": null
}
```

存在时返回 `transform`、`modelMatrix`、`inverseModelMatrix`、参考模型、原始中心和半径等信息。

保存：

```http
POST /api/align/georef?splat=3dgs%2F7.1.1.ply
Content-Type: application/json
```

```json
{
  "transform": {
    "translation": [0, 0, 0],
    "rotation": [0, 0, 0],
    "scale": 1,
    "scaleAxis": [1, 1, 1]
  },
  "modelMatrix": [16个矩阵数值],
  "inverseModelMatrix": [16个矩阵数值],
  "referenceTileset": "3dgs/dfelanqiuchang/tileset.json",
  "splat": "3dgs/7.1.1.ply",
  "rawCenter": [0, 0, 0],
  "rawRadius": 10
}
```

通常平台不需要自行构造保存 payload；向 align renderer 发送 `align-command/save` 后，renderer 会用当前状态调用该接口。平台可再次 GET 验证是否保存成功。

```http
GET /api/align/georef?splat=3dgs%2F7.1.1.ply
POST /api/align/georef
```

outdoor 渲染端会读取 georef 中的 `modelMatrix` / `inverseModelMatrix` 做坐标转换。前端平台通常只需使用已有 georef，不需要自己保存。

## 5. WebSocket 信令接口

连接地址：

```text
ws://<host>/signal?role=client&session=<sessionId>
```

服务端内部 renderer 使用：

```text
ws://<host>/signal?role=renderer&session=<sessionId>
```

平台前端只需要使用 `role=client`。

## 6. WebRTC 信令消息

### 6.1 server -> client

#### renderer-ready

renderer 页面加载完成后发送：

```json
{ "type": "renderer-ready" }
```

收到后，平台前端创建 `RTCPeerConnection` 并发送 offer。

#### answer

```json
{
  "type": "answer",
  "sdp": "..."
}
```

#### ice

```json
{
  "type": "ice",
  "candidate": { }
}
```

#### telemetry

渲染端回传当前无人机位置/姿态，用于 UI 面板展示：

```json
{
  "type": "telemetry",
  "x": 0,
  "y": 0,
  "z": 0,
  "yaw": 0,
  "wgs84": {
    "longitude": 121.3659,
    "latitude": 37.5198,
    "height": 103.0
  }
}
```

#### pick-result

双击 3DGS 表面拾取结果：

```json
{
  "type": "pick-result",
  "ok": true,
  "source": "outdoor-3dgs",
  "x": 0.5,
  "y": 0.4,
  "wgs84": {
    "longitude": 121.3659,
    "latitude": 37.5198,
    "height": 103.0
  }
}
```

### 6.2 client -> server/renderer

#### offer

```json
{
  "type": "offer",
  "sdp": "..."
}
```

#### ice

```json
{
  "type": "ice",
  "candidate": { }
}
```

#### input

鼠标/触控交互控制相机：

```json
{
  "type": "input",
  "action": "orbit",
  "dx": 12,
  "dy": -5
}
```

支持：

```text
action=orbit  左键拖拽旋转
action=pan    右键拖拽平移
action=zoom   滚轮缩放，带 delta
```

#### viewport-double-click

双击拾取 3DGS 表面 WGS84：

```json
{
  "type": "viewport-double-click",
  "x": 0.52,
  "y": 0.38
}
```

`x/y` 是归一化视口坐标，范围 0~1。

#### drone-control

手动控制无人机 / 清除轨迹：

```json
{
  "type": "drone-control",
  "action": "forward"
}
```

支持动作：

```text
forward
back
left
right
up
clear-path   清除飞行轨迹
```

可选参数：

```json
{
  "type": "drone-control",
  "action": "forward",
  "step": 0.2,
  "yawStep": 5
}
```

#### drone-set

手动设置无人机位置，支持 WGS84 或场景坐标。

WGS84 示例：

```json
{
  "type": "drone-set",
  "longitude": 121.365988,
  "latitude": 37.519823,
  "height": 133.9,
  "yaw": 12.5
}
```

### 6.3 align-command 配准控制

配准页面通过统一消息控制待配准 3DGS：

```json
{
  "type": "align-command",
  "action": "tx+",
  "amount": 1
}
```

- `action`：操作名称。
- `amount`：可选倍率，默认 `1`。
- `value`：设置步长时使用。

平移：

```text
tx+ / tx-   X 轴平移
ty+ / ty-   Y 轴平移
tz+ / tz-   Z 轴平移
```

旋转，单位为度：

```text
rx+ / rx-   X 轴旋转（俯仰）
ry+ / ry-   Y 轴旋转（横滚）
rz+ / rz-   Z 轴旋转（偏航）
```

整体缩放：

```text
scale+      按当前比例步长放大
scale-      按当前比例步长缩小
scale10     放大 10 倍
scale01     缩小 10 倍
```

分轴缩放：

```text
sx+ / sx-   X 轴缩放
sy+ / sy-   Y 轴缩放
sz+ / sz-   Z 轴缩放
scaleAxisReset  将分轴缩放恢复为 [1,1,1]
```

分轴缩放用于补偿 3DGS 相对参考模型的方向性拉伸。它作用于 3DGS 局部轴，并会写入最终 `modelMatrix`，不是只影响预览。

显示控制：

```text
toggle-splat-view  在真实 3DGS 纹理和代理点云之间切换
opacity+           3DGS 透明度 +0.1，最高 1.0
opacity-           3DGS 透明度 -0.1，最低 0.1
point+             代理点大小 +1，最高 20
point-             代理点大小 -1，最低 1
```

辅助操作：

```text
translate10   平移步长乘 10
translate01   平移步长除 10，最低 0.001
fit           以参考模型中心和半径自动初始化位置、比例
reset         重置后重新执行自动初始化
save          保存当前 georef
```

直接设置步长：

```json
{
  "type": "align-command",
  "action": "step-translate",
  "value": 0.1
}
```

```json
{
  "type": "align-command",
  "action": "step-rotate",
  "value": 0.2
}
```

每次控制后 renderer 会返回 `telemetry`：

```json
{
  "type": "telemetry",
  "position": { "x": 1.2, "y": -0.5, "z": 3.0 },
  "yaw": 12,
  "align": {
    "action": "tx+",
    "transform": {
      "translation": [1.2, -0.5, 3.0],
      "rotation": [0, 0, 12],
      "scale": 7.12,
      "scaleAxis": [1, 1, 1]
    },
    "step": {
      "translate": 0.1,
      "rotate": 0.2,
      "scale": 0.05,
      "scaleAxis": 0.01,
      "pointSize": 3
    }
  }
}
```

当前 `save` 没有单独的 WebSocket 保存成功消息；需要严格确认时，在发送 `save` 后调用 `GET /api/align/georef?splat=...` 检查 `exists` 和保存内容。

### 6.4 配准视角控制与拾取

align 与 outdoor 使用相同的输入消息：

```text
input/orbit  旋转 Cesium 参考相机
input/pan    平移参考相机焦点
input/zoom   改变相机距离
```

双击消息：

```json
{
  "type": "viewport-double-click",
  "x": 0.52,
  "y": 0.38
}
```

align 会对参考 3D Tiles 执行 Cesium `pickPosition`，返回：

```json
{
  "type": "pick-result",
  "ok": true,
  "source": "align-reference-tileset",
  "wgs84": {
    "longitude": 121.3659,
    "latitude": 37.5198,
    "height": 108.7
  },
  "ecef": { "x": 0, "y": 0, "z": 0 }
}
```

场景坐标示例：

```json
{
  "type": "drone-set",
  "x": 0,
  "y": 1,
  "z": 0,
  "yaw": 0
}
```

## 7. 服务端推送给 renderer 的实时消息

这部分平台前端通常不需要直接发送；由服务端 MQTT 模块自动推给 renderer。

### 7.1 uav-realtime

无人机实时位置：

```json
{
  "type": "uav-realtime",
  "payload": {
    "source": "drone",
    "deviceSn": "xxx",
    "longitude": 121.36598831540283,
    "latitude": 37.519822719334385,
    "height": 133.92486572265625,
    "yaw": 12.5,
    "timestamp": 1783323394798,
    "receivedAt": 1783323395000
  }
}
```

说明：

- 飞行中优先使用无人机 OSD。
- 无人机 OSD 超过 6 秒未更新时，回落到机巢 OSD 位置。
- `yaw` 飞行时取 `attitude_head`，在舱时取 `heading`。

### 7.2 dock-state

机巢标记位置与状态：

```json
{
  "type": "dock-state",
  "payload": {
    "longitude": 121.365865,
    "latitude": 37.5198972,
    "height": 103.04,
    "inDock": true,
    "deviceSn": "8UUXN3U00A046E"
  }
}
```

renderer 根据 `inDock` 显示：

```text
true  -> 无人机在舱内（青色）
false -> 无人机已出舱（琥珀色）
```

### 7.3 planned-route

当前执行任务的计划航线。该消息由服务端自动查询 Cloud API、解析航点后推送，平台前端一般不需要主动发送。

```json
{
  "type": "planned-route",
  "payload": {
    "jobId": "2e738cac-356a-4d04-996a-6a6620f37f3a",
    "jobName": "光伏巡检任务",
    "fileId": "d6fa6a18-4dc6-41e3-9b49-9701e1cc4da4",
    "fileName": "solar-plan",
    "dockSn": "8UUXN3U00A046E",
    "currentWaypointIndex": 2,
    "waypoints": [
      { "longitude": 121.3659351, "latitude": 37.5198912, "height": 134.0 },
      { "longitude": 121.3659121, "latitude": 37.5198421, "height": 136.0 }
    ],
    "updatedAt": 1784092800000
  }
}
```

说明：

- `currentWaypointIndex` 使用从 0 开始的索引；未知时为 `null`。
- renderer 上的数字航点标签使用从 1 开始的序号。
- `payload=null` 表示当前无执行中任务，renderer 会清除计划航线。
- 新 renderer 连接后会立即补发当前计划航线。

## 8. MQTT OSD 区分逻辑

DJI 上云 API 的 `thing/product/+/osd` 同时包含机巢 OSD 和无人机 OSD，且两者都带经纬度。服务端按字段区分：

```text
机巢 OSD：drone_in_dock / drone_charge_state / cover_state
无人机 OSD：attitude_head / horizontal_speed / elevation
```

位置选用：

```text
无人机 OSD 6 秒内有更新 -> 用真实飞行位置
否则 -> 用机巢固定位置
```

### 8.1 航点进度事件

服务端还订阅：

```text
thing/product/+/events
```

支持 DJI 事件：

```text
flighttask_progress
in_flight_wayline_progress
```

服务端提取任务 ID 和当前航点索引，更新 `planned-route.currentWaypointIndex`。若近期没有有效进度事件，则根据 MQTT 无人机 WGS84 位置寻找最近航点，作为显示进度的回退方案。

## 9. Cloud API 计划航线获取

### 9.1 查询流程

```text
登录 /manage/api/v1/login
  -> 获取 access_token 和 workspace_id
  -> 每 5 秒查询 /wayline/api/v1/workspaces/{workspaceId}/jobs
  -> 选择 status=2 且 dock_sn 与当前机巢匹配的任务
  -> 读取 file_id
  -> 获取并解析航点
  -> planned-route 推送给所有共享 renderer
```

请求鉴权头：

```text
x-auth-token: <access_token>
```

### 9.2 航点解析回退

优先调用：

```http
GET /wayline/api/v1/workspaces/{workspaceId}/waylines/{fileId}/xyzPoint
```

若后端 KMZ 解析器报错（例如 `For input string: "0.0"`），服务端自动调用：

```http
GET /wayline/api/v1/workspaces/{workspaceId}/waylines/{fileId}/url
```

下载 KMZ 后使用 `adm-zip` 读取 `wpmz/waylines.wpml`，优先使用 `ellipsoidHeight` 作为 WGS84 椭球高。KMZ 下载大小、ZIP 条目数和航点数均有限制，避免异常文件占满内存。

## 10. WebRTC 前端实现要点

建议 ICE 配置：

```js
const ICE_CONFIG = {
  iceServers: [
    {
      urls: 'turn:172.20.13.53:3478?transport=udp',
      username: 'cloudrender',
      credential: 'CloudRender@123456'
    }
  ],
  iceTransportPolicy: 'all'
};
```

说明：

- `iceTransportPolicy: 'all'` 会优先尝试局域网直连，TURN 作为兜底。
- 不建议强制 `relay`，否则所有 1080p 视频都走 TURN，容易频繁 ICE disconnected / failed。

断线恢复建议：

- `disconnected`：先等一段时间（当前 10 秒），避免瞬时抖动就重建。
- `failed`：关闭旧 RTCPeerConnection，创建新 peer，重新发送 offer。
- renderer 收到新 offer 时也会关闭旧 peer，创建新的 peer，保证两端 ICE 状态干净。

## 11. 可视化元素说明

### 11.1 无人机

- 模型：`/models/airplane.glb`
- 自动缩放到约 0.4 场景单位。
- 支持 GLB 内置动画，当前会播放所有 animation clips。
- 使用青色荧光描边，便于在 3DGS 场景中识别。

### 11.2 机巢标记

- 机巢位置来自 dock OSD 的 WGS84。
- 显示悬浮旋转四棱锥，尖头朝下。
- 标记始终显示在最前（depthTest=false，renderOrder=997~999），用于信标识别。
- 状态文字：`无人机在舱内` / `无人机已出舱`。

### 11.3 飞行轨迹

- 轨迹点来自无人机实时位置和手动控制位置。
- 使用动态 BufferGeometry 绘制青色核心线和外层辉光线。
- 自动去除近距离重复点，最多保留 1500 点。
- `drone-control: clear-path` 可清除轨迹。

### 11.4 计划航线

- 蓝色：完整计划航线底线。
- 绿色：已经完成的航段和航点。
- 黄色：尚未完成的航段和航点。
- 橙色闪烁球：当前目标航点。
- 每个航点上方显示从 1 开始的数字序号，标签始终面向镜头。
- 青色实时飞行轨迹与计划航线同时保留，便于对比计划与实际路径。

## 12. 前端最小伪代码

```js
const session = await fetch('/api/session', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    renderer: 'outdoor',
    pointCloudFile: '3dgs/7.1.1.ply',
    rendererParams: {
      alignSplatFile: '3dgs/7.1.1.ply',
      alignReferenceTileset: '3dgs/dfelanqiuchang/tileset.json'
    }
  })
}).then(r => r.json());

const ws = new WebSocket(`ws://${location.host}/signal?role=client&session=${session.id}`);
const pc = new RTCPeerConnection(ICE_CONFIG);

pc.addTransceiver('video', { direction: 'recvonly' });
pc.ontrack = event => {
  video.srcObject = event.streams[0];
};
pc.onicecandidate = event => {
  ws.send(JSON.stringify({ type: 'ice', candidate: event.candidate }));
};

ws.onmessage = async event => {
  const msg = JSON.parse(event.data);

  if (msg.type === 'renderer-ready') {
    const offer = await pc.createOffer({ offerToReceiveVideo: true, offerToReceiveAudio: false });
    await pc.setLocalDescription(offer);
    ws.send(JSON.stringify({ type: 'offer', sdp: offer.sdp }));
  }

  if (msg.type === 'answer') {
    await pc.setRemoteDescription({ type: 'answer', sdp: msg.sdp });
  }

  if (msg.type === 'ice' && msg.candidate) {
    await pc.addIceCandidate(msg.candidate);
  }
};

function sendOrbit(dx, dy) {
  ws.send(JSON.stringify({ type: 'input', action: 'orbit', dx, dy }));
}

function clearPath() {
  ws.send(JSON.stringify({ type: 'drone-control', action: 'clear-path' }));
}
```

如果平台希望使用服务启动时的默认模式，省略 `renderer` 即可：

```json
{
  "pointCloudFile": "3dgs/7.1.1.ply"
}
```

## 13. 部署、Git 与模型文件

项目独立 Git 仓库：

```text
https://github.com/Cecil-x/cloud-rendering-service-outdoor
```

仓库为室外版本独立维护，不再直接覆盖原室内仓库。大型模型资产已通过 `.gitignore` 排除，包括 PLY、PCD、3DGS、B3DM、生成的 GLB、KMZ 和压缩包；小型运行模型 `public/models/airplane.glb` 保留在仓库中。

服务器更新：

```bash
cd /home/Cloudrendering-outside
git pull
npm install
./start-linux-outdoor-gpu.sh
```

新增或更新 npm 依赖后必须执行 `npm install`。例如计划航线 KMZ 回退解析依赖：

```text
adm-zip@0.6.0
```

首次部署需单独复制以下被 Git 忽略的模型资产：

```text
models/3dgs/7.1.1.ply
models/3dgs/dfelanqiuchang/
models/solarqingxie_1/
```

`git pull` 不会删除这些未跟踪且被忽略的模型文件；不要在模型服务器上执行 `git clean -fdx`。

## 14. 注意事项

- outdoor 必须使用已配准的 3DGS 与 georef，否则无人机/机巢位置会偏。
- 不要选择不存在或非 3DGS 的文件（如错误选择 `GlobalMap.pcd` 会导致 3DGS 加载失败、画面黑）。
- 相同 outdoor 配置的多个客户端共享一个 renderer；不同模型或 renderer 参数会创建独立会话。
- `RENDERER` 是进程级默认/回退模式，不再限制所有 session 必须使用同一种 renderer。
- 页面按需创建 session；只有创建并加载 `align` session 后才会持续占用 Cesium + Three.js/3DGS 资源，关闭 session 后释放对应页面资源。

## 15. 平台配准页面实现建议

### 15.1 渲染结构

align 不是单独使用 Cesium 渲染 3DGS，而是双引擎叠加：

```text
Cesium Viewer
  -> 渲染带真实地理坐标的参考倾斜摄影 3D Tiles
  -> 提供 WGS84 / ECEF / ENU 参考系与相机

Three.js + @mkkellogg/gaussian-splats-3d
  -> 渲染待配准的真实 3DGS 纹理
  -> 承载平移、旋转、整体缩放和分轴缩放

最终 composite canvas
  -> 合成 Cesium 参考层、Three.js/3DGS 层和 HUD
  -> 通过 WebRTC 推给平台页面
```

真实纹理模式是默认模式。代理点云仅用于 3DGS 加载失败时的回退或辅助诊断；纹理模式下会隐藏 Cesium PointPrimitive 代理，减少 CPU 开销。

### 15.2 推荐操作流程

```text
1. 平台选择待配准 3DGS。
2. 平台选择带地理坐标的参考 3D Tiles。
3. POST /api/session，renderer=align，并同时传两个模型参数。
4. 建立 WebSocket 和 WebRTC，等待参考模型与真实 3DGS 加载。
5. 使用 orbit/pan/zoom 调整观察视角。
6. 先用 fit、较大平移步长和整体缩放完成粗配准。
7. 切换半透明纹理，使用较小平移/旋转步长完成精配准。
8. 必要时使用 sx/sy/sz 修正方向性拉伸。
9. 发送 align-command/save。
10. GET /api/align/georef?splat=... 验证保存结果。
11. 关闭 align session，outdoor session 下次加载时使用新 georef。
```

### 15.3 配准状态恢复

align 启动后会自动读取当前 3DGS 对应的 georef：

- 文件存在：恢复 `translation`、`rotation`、`scale` 和 `scaleAxis`。
- 老 georef 没有 `scaleAxis`：按 `[1,1,1]` 兼容。
- 文件不存在：按参考模型包围球与 3DGS 半径执行默认居中和自动比例初始化。

配准结果文件通常为：

```text
models/3dgs/7.1.1.ply
  -> models/3dgs/7.1.1.georef.json
```

### 15.4 会话和资源生命周期

- 服务启动本身不会预加载 align 模型。
- 只有平台创建 `renderer=align` 的 session 后，才会启动对应 Puppeteer 页面并加载 Cesium、参考 3D Tiles 和 3DGS。
- align session 存在期间会持续占用 Chrome 内存、WebGL 上下文、GPU 显存和渲染算力，即使暂时没有发送控制命令。
- align 不共享 session，避免多个操作者互相修改同一个临时变换。
- outdoor 和 align 可以同时存在，但相当于同时渲染两份 3DGS，其中 align 还额外加载 Cesium 3D Tiles，GPU 压力会明显增加。
- 平台离开配准页时应关闭 WebSocket，并调用 `POST /api/session/{id}/close`；强制管理清理可调用 `DELETE /api/session/{id}`。
- session 关闭期间若 Puppeteer 页面仍在异步创建，服务端会取消并关闭该页面，防止遗留孤儿 WebGL 页面。

### 15.5 当前限制

- align 的真实 3DGS 与参考 3D Tiles 使用画布合成，不共享同一个深度缓冲；主要依靠透明度和纹理特征进行视觉对齐。
- 分轴缩放沿 3DGS 局部轴工作。如果误差方向与局部轴不一致，可能出现轴向耦合，需要配合旋转后再调整。
- `reset` 不是简单恢复单位矩阵，而是清空变换后再次执行自动居中与自动比例。
- 保存 georef 会直接覆盖该 3DGS 对应的 georef JSON，平台应在 UI 上增加保存确认。
- outdoor 已打开时不会自动热重载刚保存的 georef；建议保存后关闭并重新创建 outdoor session。
- 如果频繁黑屏恢复，优先检查 WebRTC ICE 链路和 TURN 服务；不要强制 relay。
- 机巢标记和轨迹是信标叠加层，默认始终可见，不被 3DGS 遮挡。
