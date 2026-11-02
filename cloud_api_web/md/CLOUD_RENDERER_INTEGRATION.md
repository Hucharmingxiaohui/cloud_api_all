# 云渲染功能接入说明

本文档记录当前 `cloud_api_web` 项目新增的云渲染功能，以及它与原有三维航线编辑功能的关系。

## 一、项目现状

当前项目的三维航线编辑功能仍然由浏览器直接运行 Cesium，主要页面为：

```text
src/components/cesium/3DMapPanel.vue
src/components/cesium/Solar3DRouteEditor.vue
```

云渲染功能是新增能力，不替换原有 Cesium 三维航线编辑页面。当前云渲染主要用于 Outdoor 3DGS 场景展示，以及后续模型配准场景。

## 二、云渲染架构

```text
Vue 页面
  -> OutdoorRenderer.vue
  -> cloudRendererClient.ts
  -> HTTP 创建云渲染 session
  -> WebSocket 信令
  -> WebRTC 接收云端视频
  -> video 元素播放渲染画面
```

云渲染服务端使用 Puppeteer/Chrome 加载 3DGS 渲染页面，并通过 WebRTC 将画面推送给前端。前端不直接加载 `.ply`、`.splat` 或 Cesium 场景模型。

## 三、相关前端文件

### 3.1 播放器组件

```text
src/components/cloudRenderer/OutdoorRenderer.vue
```

职责：

- 创建视频播放容器；
- 绑定云渲染客户端；
- 接收 WebRTC 视频流；
- 转发鼠标旋转、平移、滚轮缩放和双击拾取；
- 显示连接状态；
- 支持组件卸载时关闭独立的配准 session。

### 3.2 云渲染客户端

```text
src/components/cloudRenderer/cloudRendererClient.ts
```

职责：

- 调用 `POST /api/session` 创建 session；
- 连接 `/signal?role=client&session=...`；
- 收到 `renderer-ready` 后创建 WebRTC offer；
- 处理 answer 和 ICE；
- 将视频轨道挂载到页面的 `<video>`；
- 发送相机交互和配准命令；
- 处理断线重连和 session 关闭；
- 复用 Outdoor 页面之间的共享 session。

### 3.3 配置封装

```text
src/components/cloudRenderer/cloudRendererConfig.ts
src/types/runtime-config.ts
src/api/http/config.ts
public/config.js
```

配置优先级：

```text
默认配置 < public/config.js 部署配置 < localStorage 本地配置
```

本地配置键名：

```text
cloud-renderer-config
```

### 3.4 云渲染管理页面

```text
src/components/variableMgt/cloudRenderingMgt/index.vue
```

当前包含两个标签页：

1. 室外渲染配置；
2. 模型配准。

室外渲染配置支持：

- 配置云渲染服务地址；
- 选择 3DGS 文件；
- 选择配准用 3DGS 和参考 Tileset；
- 配置 TURN；
- 测试服务连接；
- 保存配置并重连；
- 恢复部署配置；
- 读取云渲染服务资源列表。

模型配准支持：

- 创建 `renderer=align` 的独立 session；
- 使用云端 Cesium + Three.js/3DGS 合成画面；
- 发送平移、旋转、整体缩放、分轴缩放命令；
- 调整平移/旋转步长；
- 保存、重置和自动适配配准结果。

## 四、云渲染配置

当前部署配置位于：

```text
public/config.js
```

示例：

```js
cloudRenderer: {
  baseURL: 'http://127.0.0.1:3000',
  renderer: 'outdoor',
  pointCloudFile: '3dgs/7.1.1.ply',
  rendererParams: {
    alignSplatFile: '3dgs/7.1.1.ply',
    alignReferenceTileset: '3dgs/dfelanqiuchang/tileset.json'
  },
  iceServers: [
    {
      urls: 'turn:host:3478?transport=udp',
      username: 'cloudrender',
      credential: 'password'
    }
  ]
}
```

注意：`baseURL` 必须是浏览器能够访问的云渲染服务地址。不能在用户浏览器访问不到云渲染服务器时继续使用服务器本机的 `127.0.0.1`。

## 五、session 流程

### 5.1 Outdoor

前端创建 session：

```http
POST /api/session
Content-Type: application/json
```

请求体包含：

```json
{
  "renderer": "outdoor",
  "pointCloudFile": "3dgs/7.1.1.ply",
  "rendererParams": {}
}
```

### 5.2 Align

模型配准使用独立 session：

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

Align 页面必须同时加载：

- Cesium 参考 3D Tiles；
- Three.js + Gaussian Splatting 待配准模型；
- 合成画布和 WebRTC 推流。

因此 Align 比 Outdoor 需要更多 WebGL、GPU 显存和浏览器资源。

## 六、交互消息

普通场景交互：

```json
{ "type": "input", "action": "orbit", "dx": 10, "dy": -5 }
{ "type": "input", "action": "pan", "dx": 10, "dy": -5 }
{ "type": "input", "action": "zoom", "delta": -120 }
{ "type": "viewport-double-click", "x": 0.5, "y": 0.4 }
```

配准命令：

```json
{ "type": "align-command", "action": "tx+" }
{ "type": "align-command", "action": "rz-" }
{ "type": "align-command", "action": "scale+" }
{ "type": "align-command", "action": "save" }
```

当前前端配准控制按钮位于：

```text
src/components/variableMgt/cloudRenderingMgt/index.vue
```

## 七、与原 Cesium 三维航线编辑的关系

原有航线编辑页面：

```text
src/components/cesium/3DMapPanel.vue
src/components/cesium/Solar3DRouteEditor.vue
```

仍然在用户浏览器中直接创建 Cesium Viewer，并直接加载浏览器端可访问的 3D Tiles/模型资源。它不经过 `OutdoorRenderer.vue`，也不经过云渲染 WebRTC session。

云渲染管理页面是单独的配置和配准入口，不应误认为已经将原有光伏三维航线编辑页面整体切换为云渲染。

## 八、常见问题

### 8.1 Outdoor 正常但 Align 黑屏

Align 需要同时初始化 Cesium 和 Three.js。若服务端日志出现：

```text
Error constructing CesiumWidget
RuntimeError: The browser supports WebGL, but initialization failed.
```

说明 session 创建和 renderer 页面导航已成功，但服务端 Chrome/Cesium 初始化 WebGL 失败。优先检查：

- Linux `DISPLAY` 是否指向 NVIDIA-backed Xorg；
- Xorg GLX 是否使用 NVIDIA 驱动；
- Chrome 是否使用验证过的 Puppeteer Chrome；
- GPU 显存和 WebGL context 是否已被其他 session 占用；
- 是否同时运行了 Outdoor 和 Align；
- 云渲染服务是否需要重启以释放浏览器/GPU 上下文。

这类错误通常不是“启动配准按钮没有发送请求”，因为服务端已经创建了 Align session。

### 8.2 右侧配准按钮被遮挡

模型配准页面的右侧控制区是独立滚动区域。相关高度和溢出样式位于：

```text
src/components/variableMgt/cloudRenderingMgt/index.vue
src/pages/page-web/projects/workspace.vue
```

### 8.3 改了配置但没有生效

- `public/config.js` 修改后刷新前端页面；
- 云渲染管理页面保存配置后会写入 localStorage；
- 如果 localStorage 有旧配置，它会覆盖 `public/config.js`；
- 可在页面点击“恢复部署配置”，或清除 `cloud-renderer-config` 后重试。

## 九、当前边界

- 当前新增云渲染主要服务于 Outdoor 3DGS 显示和 Align 配准页面。
- 原有光伏三维航线编辑仍由浏览器 Cesium 实现。
- 云端 Align 保存的是 3DGS 对应的 georef 配置，不会自动替换原有浏览器 Cesium 航线编辑页面。
- 如果后续要把 `Solar3DRouteEditor.vue` 全部改成云渲染，需要额外设计航点 HUD、航点编辑交互、Cesium/3DGS 坐标同步和编辑结果回传方案，不能仅替换一个渲染组件。
