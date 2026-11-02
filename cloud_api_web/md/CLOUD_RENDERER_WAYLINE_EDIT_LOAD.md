# 云渲染三维航线编辑 — load 对接说明（给云渲染服务）

> 日期：2026-07-22  
> 状态：平台前端已完成「下载 KMZ → 本地解析 → 左侧列表展示」；**场景内画航点依赖云渲染实现 `load`**  
> 相关前端：`CloudWaylineEditor.vue` / `cloudRendererClient.ts` / `kmzBrowserParser.ts`

---

## 1. 背景

平台已支持从**航线管理**进入三维编辑：

1. 用户点击某条航线的「三维编辑」
2. 前端下载该航线 KMZ（`GET .../waylines/{id}/url`）
3. **浏览器本地**解压解析 KMZ（不调用后端解析接口）
4. 映射为与新建规划相同的 `waypoints[]`
5. 打开 `/wayline/cloud3d-editor?mode=edit&waylineId=...`
6. 连接云渲染后，通过信令发送 **`wayline-command` / `action: load`**
7. 云渲染应清空当前航线，按 JSON 画出全部航点，并持续回推 `wayline-state`

当前：左侧列表已有点；若云渲染未实现 `load`，中间 3D 场景仍是空的。

---

## 2. 信令通道（已有）

与现有航线规划相同：

- WebSocket 信令（会话建立后）
- 平台发送：

```json
{
  "type": "wayline-command",
  "action": "<action>",
  "...payload fields 展开在同级或见下..."
}
```

前端实现（`cloudRendererClient.ts`）：

```ts
sendWaylineCommand(action, payload) {
  this.sendSignal({ type: 'wayline-command', action, ...payload })
}
```

因此 **`load` 的实际报文形状**是：

```json
{
  "type": "wayline-command",
  "action": "load",
  "routeName": "wayline-route017",
  "waylineId": "602e2a6f-9fae-499d-8e75-e8d3d588289a",
  "selectedIndex": 0,
  "waypoints": [ /* 见 §3 */ ]
}
```

同时会跟一条（兼容）：

```json
{
  "type": "wayline-command",
  "action": "set-route-name",
  "value": "wayline-route017"
}
```

---

## 3. 必须实现：`action = "load"`

### 3.1 语义

| 项 | 要求 |
|---|---|
| 收到 load | **替换**当前整条航线（先 clear，再写入新点） |
| 不要 merge | 不是追加，是全量覆盖 |
| 空数组 | `waypoints: []` 视为清空 |
| 画完后 | 立即推送一次完整 `wayline-state`（见 §4） |
| 选中 | 按 `selectedIndex` 选中；越界则 `-1` 或 `0` |
| 相机 | 建议 fly/fit 到整条航线包围盒，便于用户看到点 |

### 3.2 完整字段

```ts
interface LoadCommand {
  type: 'wayline-command'
  action: 'load'
  routeName: string
  waylineId?: string          // 平台航线 ID，可选，仅元数据
  selectedIndex: number       // 默认选中航点，从 0 起；无选中可为 -1
  waypoints: Waypoint[]
}

type CaptureMode = 'none' | 'visable' | 'ir' | 'visable,ir'

interface Waypoint {
  point_name: string
  longitude: number           // WGS84 经度
  latitude: number            // WGS84 纬度
  height: number              // 绝对高度（米，与规划一致，多为椭球高）
  capture_mode: CaptureMode
  speed: number               // m/s
  headingTrue: number         // 兼容字段：真实偏航，与 camera_params.heading 相同
  camera_params: {
    heading: number           // **真实偏航**（0=北，顺时针）；协议主字段
    pitch: number             // 俯仰，通常 -90~0
    roll: number              // 滚转，通常 0
    focalLength: number       // 35mm 等效焦距（mm）
  }
}
```

### 3.3 样例（真实结构）

```json
{
  "type": "wayline-command",
  "action": "load",
  "routeName": "wayline-route017",
  "waylineId": "602e2a6f-9fae-499d-8e75-e8d3d588289a",
  "selectedIndex": 0,
  "waypoints": [
    {
      "point_name": "WP_001",
      "longitude": 121.365897,
      "latitude": 37.519834,
      "height": 128.5,
      "capture_mode": "visable",
      "speed": 5,
      "headingTrue": 90,
      "camera_params": {
        "heading": 90,
        "pitch": -45,
        "roll": 0,
        "focalLength": 75
      }
    },
    {
      "point_name": "WP_002",
      "longitude": 121.366100,
      "latitude": 37.519900,
      "height": 130.0,
      "capture_mode": "none",
      "speed": 5,
      "headingTrue": 95,
      "camera_params": {
        "heading": 95,
        "pitch": -45,
        "roll": 0,
        "focalLength": 75
      }
    }
  ]
}
```

### 3.4 字段使用建议（场景侧）

| 字段 | 场景用途 |
|---|---|
| `longitude/latitude/height` | 航点位置（与双击加点同一坐标系） |
| `camera_params.heading` | **真实偏航**（0=北，顺时针）；页面唯一可编辑偏航 |
| `camera_params.pitch/roll` | 俯仰/滚转 |
| `camera_params.focalLength` | 视锥 FOV（35mm 等效） |
| `headingTrue` | 兼容字段，数值必须与 `camera_params.heading` 相同 |
| `capture_mode` | 区分过渡点 / 可见光 / 红外 / 双光 |
| `point_name` | 标签 |
| `speed` | 属性面板 |

### 3.5 偏航约定（2026-07 更新：只保留真实偏航）

**协议层不再区分「场景偏航 / 真实偏航」两套角。**

| 字段 | 含义 | 谁用 |
|---|---|---|
| `camera_params.heading` | 真实北向偏航（主字段） | 平台 UI 编辑；KMZ 的 aircraft/gimbal/waypoint 角 |
| `headingTrue` | 同值兼容字段 | 旧代码可读；新代码优先 `camera_params.heading` |

云渲染职责：

1. 收到真实偏航后，**内部**转换成场景/视锥角（平台不做换算）  
2. 回推 `wayline-state` 时：`camera_params.heading` 与 `headingTrue` 都填**同一真实偏航**  
3. 机体 `move-body` 用 georef + 真实偏航解算，避免镜像  

**load / 编辑验收：**

- [ ] 页面只有一个偏航控件，绑定 `camera_params.heading`  
- [ ] 视锥朝向正确（云渲染内部转场景角）  
- [ ] `wayline-state` 中两字段始终相等且为真实北向角  
- [ ] 生成 KMZ：`aircraftHeading = gimbalYaw = waypointHeadingAngle = camera_params.heading`

`capture_mode` 注意拼写是 **`visable`**（历史约定，不是 visible）。

---

## 4. 回推：`wayline-state`（已有协议，load 后必推）

平台监听：

```json
{
  "type": "wayline-state",
  "payload": {
    "routeName": "wayline-route017",
    "selectedIndex": 0,
    "waypoints": [ /* 与 §3 相同结构 */ ]
  }
}
```

要求：

1. **load 成功后立刻推一次**，且 `waypoints.length` 与 load 一致  
2. 用户在场景内改点/加点/删点后继续推  
3. **不要在 load 前后反复推空数组**覆盖前端（前端编辑态会忽略空数组，但场景仍应正确）

前端 `normalizeWaylineState` 校验要点：

- 每个点必须有合法 `camera_params`
- `capture_mode` ∈ `none | visable | ir | visable,ir`
- 数值字段均为 finite number（含 `headingTrue`）

---

## 5. 已有 wayline-command（保持兼容）

load 之外，规划页已在用，请勿破坏：

| action | 含义 | 典型 payload |
|---|---|---|
| `select` | 选中航点 | `{ index }` |
| `remove` | 删除航点 | `{ index }` |
| `update` | 更新航点字段 | `{ index, patch }` |
| `clear` | 清空 | `{}` |
| `set-route-name` | 改名 | `{ value }` |
| **`load`（新增）** | **整线载入** | **见 §3** |
| **`move-body`（新增）** | **机体坐标系平移** | `{ index, forward, right }` 单位米 |

### 5.1 `move-body`（机体前后左右）

平台**不再**用 `camera_params.heading` 本地算经纬度（会镜像）。  
东南西北仍用 `update` 改 lon/lat；机体方向改发：

```json
{
  "type": "wayline-command",
  "action": "move-body",
  "index": 0,
  "forward": 1.0,
  "right": 0.0
}
```

| 字段 | 含义 |
|---|---|
| `index` | 航点下标 |
| `forward` | 沿机头方向位移（米，+前 -后） |
| `right` | 沿机头右侧位移（米，+右 -左） |

云渲染应用 **georef / 真实机头（headingTrue 或内部机体朝向）** 解算新 lon/lat/height，再回推 `wayline-state`。

双击加点等仍走现有 `viewport-double-click` / 内部逻辑。

---

## 6. 时序

```
平台 CloudWaylineEditor 打开 (mode=edit)
  → 本地已有 waypoints（左侧列表）
  → 建立云渲染会话
  → 收到 renderer-ready 或 连接状态变空
  → 发送 wayline-command load + set-route-name
  → 【云渲染】clear → 创建航点实体/折线/视锥 → fit 视角
  → 【云渲染】推 wayline-state（含全部点）
  → 平台用远程 state 同步（此时应用远程非空列表）
  → 用户继续编辑（与新建规划相同）
  → 生成 KMZ：仍走 POST /api/wayline/build-kmz（已有）
```

前端 load 发送有 **1.5s 节流**；若第一次 load 时会话未就绪，连接成功后会再发。

---

## 7. 验收清单（云渲染）

- [ ] 收到 `load` 后场景出现与 `waypoints.length` 相同数量的航点  
- [ ] 航点经纬高与 JSON 一致（抽查 1～2 点）  
- [ ] 折线按顺序连接  
- [ ] `selectedIndex` 高亮正确  
- [ ] `capture_mode=none` 与拍照点样式可区分  
- [ ] load 后 `wayline-state` 非空且字段完整  
- [ ] load 另一条航线时，旧点全部消失（全量替换）  
- [ ] load 后仍可用：选点、改姿态、删点、双击加点、clear  
- [ ] `build-kmz` 仍可用，导出点与场景一致  

---

## 8. 平台侧现状（供对照，云渲染无需改）

| 能力 | 状态 |
|---|---|
| 航线管理「三维编辑」按钮 | 已有 |
| 下载 KMZ | 已有 |
| 浏览器解析 KMZ | 已有（`kmzBrowserParser.ts`） |
| 映射 waypoints | 已有（`cloudWaylineMapper.ts`） |
| 发送 `load` | 已有 |
| 编辑态忽略远程空 state | 已有（防闪空列表） |
| 生成 KMZ + 导入航线库 | 已有（与新建相同） |
| 覆盖原航线 vs 另存 | 当前仍是 **import 新文件**；未做按 id 覆盖 |

---

## 9. 错误与边界

| 情况 | 建议 |
|---|---|
| `waypoints` 缺失 | 当作 `[]` 清空 |
| 单点坐标非法 | 跳过该点并 log；或整单失败并推错误（二选一，建议 skip+log） |
| 点数很多（>500） | 批量创建，避免卡死主线程 |
| 重复 load | 以最后一次为准 |
| 与用户正在拖点冲突 | load 时取消当前拖拽 |

可选错误回推（若已有错误通道则复用）：

```json
{
  "type": "wayline-error",
  "message": "load failed: ..."
}
```

---

## 10. 最小实现伪代码

```js
onWaylineCommand(msg) {
  if (msg.action === 'load') {
    const waypoints = Array.isArray(msg.waypoints) ? msg.waypoints : []
    clearWayline()
    setRouteName(msg.routeName || '')
    for (const [i, wp] of waypoints.entries()) {
      addWaypointEntity(wp, i)
    }
    rebuildPolyline()
    setSelectedIndex(msg.selectedIndex ?? (waypoints.length ? 0 : -1))
    fitToWaypoints(waypoints)
    emitWaylineState() // 必须非空（若有点）
  }
  // ... 其它 action
}
```

---

## 11. 联调方式

1. 平台航线管理 → 任选已生成三维航线 →「三维编辑」  
2. 云渲染日志应看到 `wayline-command` / `load`  
3. 场景出现航点；浏览器控制台原先有  
   `[cloud3d-edit] draft ready { count: N, ... }`  
4. 改一点后点「生成 KMZ」，应导出含修改后的航线  

---

## 12. 文档索引

| 文档 | 内容 |
|---|---|
| 本文 | 编辑 load 协议（云渲染必读） |
| `CLOUD_RENDERER_INTEGRATION.md` | 云渲染整体对接（若已有） |
| `dfe-wayline-buildKmz-api.md` | KMZ 生成报文（平台/云渲染 build-kmz） |
| `CESIUM_3D_WAYLINE_IMPLEMENTATION.md` | 旧 Cesium 实现参考（非本次主路径） |

---

## 13. 联系与约定摘要

**云渲染最少改动一句话：**

> 实现 `wayline-command` + `action:"load"`：用 payload 里的 `waypoints` **全量替换**场景航线，并立刻回推非空 `wayline-state`。

平台不再改后端解析；KMZ 解析在浏览器完成。云渲染只消费与新建规划相同的 JSON。
