# 项目上下文与三维航线改动记录

本文档用于在后续更换对话或更换 AI 助手时快速恢复上下文，说明当前项目结构、运行方式、已定位问题，以及围绕“三维航线 / 光伏三维航线编辑”已经做过的前端改动。

## 一、项目基本结构

当前工作目录：

```text
D:\uav\cloud_api_all_solar3d_upload
```

主要包含两个项目：

```text
cloud_api_serve_git  后端 Spring Boot / Maven 多模块项目
cloud_api_web        前端 Vue3 + Vite 项目
```

前端目录：

```text
D:\uav\cloud_api_all_solar3d_upload\cloud_api_web
```

前端启动命令：

```bash
npm run serve
```

前端开发端口：

```text
8089
```

本地访问示例：

```text
http://localhost:8089/
http://172.20.63.88:8089/
```

后端启动主类曾使用：

```text
com.dji.sample.CloudApiSampleApplication
```

后端端口曾显示为：

```text
6789
```

后端曾遇到的问题：

1. Spring Bean 冲突，原因是本地旧的 `target/classes` 编译产物残留，清理 Maven `clean` 后解决。
2. 后端启动后连接 MySQL 超时，连接地址为 `172.20.63.157:9200/cloud_sample`，需要数据库或内网环境可达。

## 二、前端启动相关问题记录

前端一开始执行：

```bash
npm run server
```

会失败，因为脚本名不是 `server`，而是：

```bash
npm run serve
```

若提示：

```text
'vite' 不是内部或外部命令
```

说明依赖未安装，需要执行：

```bash
npm install
```

前端页面空白时，最终确认是缺少两个依赖导致 Vite 无法解析相关模块。已通过安装以下两个包解决：

```bash
npm install @epicgames-ps/lib-pixelstreamingfrontend-ue5.4
npm install @epicgames-ps/lib-pixelstreamingfrontend-ui-ue5.4
```

这两个依赖来自：

```text
src/components/uePix/UEPix.vue
```

以后如果前端空白，优先看 `npm run serve` 终端是否有 `could not be resolved`。

## 三、原有三维航线功能理解

前端原本已有一套 Cesium 三维航线编辑代码，但入口被注释或隐藏。

关键文件：

```text
cloud_api_web/src/components/WaylinePanel.vue
cloud_api_web/src/router/index.ts
cloud_api_web/src/components/cesium/3DMapPanel.vue
cloud_api_web/src/components/cesium/uavCamera.vue
cloud_api_web/src/components/cesium/cesiumTools.js
```

原有三维航线页面：

```text
src/components/cesium/3DMapPanel.vue
```

原有路由：

```text
/wayline/Model
```

该页面主要使用 Cesium 加载三维场景模型，通常是倾斜摄影或 3D Tiles 数据。模型加载逻辑中存在类似 `tileset.json` 的 3D Tiles 加载方式。

关于 3DGS：当前项目不是直接通过 Cesium 原生加载 `.splat` / `.ply` / `.ksplat` 这类 3DGS 文件。若要接入 3DGS，较稳方案是先转成 Cesium 可加载的 3D Tiles；如果不能转换，则需要额外引入 Three.js Gaussian Splatting 渲染层并与 Cesium 相机同步。

## 四、已恢复的原三维航线入口

已在以下文件恢复“三维航线”入口按钮：

```text
cloud_api_web/src/components/WaylinePanel.vue
```

恢复后的入口指向：

```text
/wayline/Model
```

对应页面仍为：

```text
src/components/cesium/3DMapPanel.vue
```

## 五、已修复原三维航线右键菜单

原三维航线页面右键航点无法弹出编辑菜单，是因为右键事件绑定被注释。

已在：

```text
cloud_api_web/src/components/cesium/3DMapPanel.vue
```

恢复了类似逻辑：

```ts
const cesiumElement = document.getElementById('cesiumContainer')
cesiumElement?.addEventListener('contextmenu', handleRightClick)
```

恢复后可右键航点，显示移动、删除、编辑等菜单。

## 六、航线服务侧接口文档

用户在根目录放置了接口说明文档：

```text
D:\uav\cloud_api_all_solar3d_upload\cloud_api_web\md\UE_3D_ROUTE_INTEGRATION.md
```

已阅读该文档。文档描述的是“航线规划服务”和“UE 三维展示/编辑模块”的接口约定。

核心理解：

1. 航线规划服务生成光伏航线后，会产生 `route_draft_id`。
2. 三维预览报文中包含 `route_draft_id`、航线名、区域、航点列表、相机参数等。
3. 三维编辑端可以编辑航点。
4. 编辑完成后，三维编辑端需要把完整航点数组回传给航线规划服务。
5. 回传时只关心 `route_draft_id` 和编辑后的 `waypoints`，不需要前端负责最终航线生成。

编辑后回传目标地址当前为：

```text
http://172.20.63.157:5001/solar/edited
```

回传报文大致结构：

```json
{
  "route_draft_id": "draft_xxx",
  "waypoints": [
    {
      "lon": 121.3659,
      "lat": 37.5199,
      "height": 123,
      "aircraft_heading": 11.94,
      "pitch": -80,
      "visible_focal_length_35mm": 75,
      "ir_focal_length_35mm": 52
    }
  ]
}
```

## 七、新增“光伏三维航线编辑”页面

按用户要求，不直接改原有 `3DMapPanel.vue` 作为光伏专用页面，而是复制了一份新页面。

新增文件：

```text
cloud_api_web/src/components/cesium/Solar3DRouteEditor.vue
```

该页面从：

```text
cloud_api_web/src/components/cesium/3DMapPanel.vue
```

复制而来，并用于光伏三维航线预览、编辑、回传。

新增路由：

```text
/wayline/solar3d-editor
```

路由配置文件：

```text
cloud_api_web/src/router/index.ts
```

已在航线管理页面新增入口按钮：

```text
光伏三维航线编辑
```

入口所在文件：

```text
cloud_api_web/src/components/WaylinePanel.vue
```

## 八、新增光伏三维航线接口封装

新增文件：

```text
cloud_api_web/src/api/solar3d-route.ts
```

主要职责：

1. 保存 / 读取三维预览报文。
2. 从开发临时接口读取三维预览报文。
3. 将编辑后的完整航点列表回传到航线规划服务。

开发环境下，编辑结果前端请求路径是：

```text
/solar-route/solar/edited
```

通过 Vite 代理转发到：

```text
http://172.20.63.157:5001/solar/edited
```

生产环境下，会优先读取：

```text
window.CURRENT_CONFIG?.solarRouteURL
```

如果没有该配置，则默认使用：

```text
http://172.20.63.157:5001
```

若后续需要修改编辑结果回传地址，优先改：

```text
cloud_api_web/vite.config.ts
```

里面 `/solar-route` 的 `target`。

若正式环境需要改地址，可在：

```text
cloud_api_web/public/config.js
```

增加或调整：

```js
solarRouteURL: 'http://目标地址:端口'
```

## 九、Vite 开发代理与临时测试接口

已修改：

```text
cloud_api_web/vite.config.ts
```

新增开发代理：

```text
/solar-route -> http://172.20.63.157:5001
```

用于将编辑后回传请求转发到航线规划服务。

同时新增了一个仅本地开发用的临时 HTTP 接收接口，用于模拟航线服务向前端发送三维预览报文。

临时接收接口：

```text
POST /dev-api/solar3d-preview
```

示例：

```text
POST http://172.20.63.88:8089/dev-api/solar3d-preview
```

返回：

```json
{
  "code": 0,
  "message": "preview received"
}
```

临时读取接口：

```text
GET /dev-api/solar3d-preview
```

返回最后一次收到的预览报文，并带版本号：

```json
{
  "code": 0,
  "data": {},
  "version": 123456789
}
```

注意：该临时接口只存在于 Vite dev server 内存中，重启 `npm run serve` 后数据会丢失，不用于生产。

因为改了 `vite.config.ts`，每次相关配置修改后都需要重启前端服务：

```bash
Ctrl + C
npm run serve
```

## 十、光伏三维页面接收预览报文逻辑

`Solar3DRouteEditor.vue` 当前支持几种接收方式：

1. 页面打开时从浏览器缓存读取。
2. 页面打开时从开发临时接口读取。
3. 页面已打开时，通过轮询开发临时接口，发现新版本后自动渲染。
4. 页面已打开时，也支持调用全局函数传入。
5. 页面已打开时，也支持 `postMessage` 传入。

全局函数方式：

```js
window.receiveSolar3DRoutePreview(payload)
```

消息方式：

```js
window.postMessage({
  type: 'SOLAR_3D_ROUTE_PREVIEW',
  payload
})
```

但目前最常用测试方式是 Postman 调临时 HTTP 接口：

```text
POST http://172.20.63.88:8089/dev-api/solar3d-preview
```

页面打开后，每 1 秒轮询一次临时接口；如果收到新版本，会自动重新绘制航点和航线，不需要手动刷新。

## 十一、光伏三维页面绘制与编辑逻辑

收到预览报文后，`Solar3DRouteEditor.vue` 会：

1. 保存 `route_draft_id`。
2. 将报文里的 `waypoints` 画成 Cesium 航点。
3. 将航点转换为页面内部 `waypointsData` 结构并保存到 `sessionStorage`。
4. 绘制航线 polyline。

内部保存 key：

```text
waypointsData
selectedWayline
solar3dRoutePreview
```

右键航点菜单可进行：

1. 移动
2. 删除
3. 编辑

点击页面里的“生成航线”按钮时，当前光伏页面不会真正生成最终航线，而是按用户要求回传编辑结果到航线规划服务。也就是说按钮名称目前仍叫“生成航线”，但行为实际是“回传编辑后的完整航点数组”。

回传逻辑所在函数：

```text
Solar3DRouteEditor.vue -> handlegenerate()
```

回传前会在浏览器 Console 打印：

```text
准备回传光伏三维航线编辑结果:
```

如果航线服务没有收到，优先看浏览器 F12 Network 是否有：

```text
/solar-route/solar/edited
```

## 十二、相机参数相关修复

用户反馈：右键编辑航点时，角度默认朝下，没有按预览报文里的相机参数初始化。

已在：

```text
cloud_api_web/src/components/cesium/Solar3DRouteEditor.vue
```

增加逻辑：

1. 进入编辑模式前，根据当前选中的航点 ID 从 `sessionStorage.waypointsData` 找对应航点。
2. 读取该航点的相机参数。
3. 将 `heading`、`pitch`、`roll` 同步到编辑器滑条和相机参数显示。

新增的关键函数包括：

```text
getStoredWaypointForSelected()
applyStoredCameraParamsToEditor()
```

用户之后反馈：角度对了，但焦距仍然是默认值。

已进一步修改：

```text
cloud_api_web/src/components/cesium/uavCamera.vue
```

修复点：

1. 父页面已经向子组件 `setCamera()` 传入焦距。
2. 但原 `uavCamera.vue` 的 `setCamera()` 没使用第五个焦距参数。
3. 已在 `uavCamera.vue` 中根据传入焦距换算 FOV，并更新 Cesium 相机视锥体。
4. 设置后立即调用 `updateCameraParameters()` 更新显示。

当前焦距读取优先级大致为：

1. `point.camera.focal_length_35mm`
2. `point.cameras[0].focal_length_35mm`
3. 默认值 `75`

同时也保存：

```text
visible_focal_length_35mm
ir_focal_length_35mm
```

用于编辑结果回传。

如果后续焦距仍不正确，需要重点检查预览报文里焦距字段到底是：

```text
camera.focal_length_35mm
cameras[].focal_length_35mm
visible_focal_length_35mm
ir_focal_length_35mm
```

或者字段名是否与当前代码不一致。

## 十三、目前测试流程

推荐测试光伏三维航线编辑的流程：

1. 启动前端：

```bash
cd D:\uav\cloud_api_all_solar3d_upload\cloud_api_web
npm run serve
```

2. 用 Postman 发送三维预览报文：

```text
POST http://172.20.63.88:8089/dev-api/solar3d-preview
```

3. 打开前端页面：

```text
http://172.20.63.88:8089/#/wayline/solar3d-editor
```

4. 页面应该自动渲染航点和航线。

5. 如果页面已打开，再次用 Postman 发新预览报文，页面应在约 1 秒后自动刷新航点，不需要手动刷新。

6. 右键航点，点击编辑，检查角度、焦距等相机参数。

7. 编辑完成后点击“生成航线”，实际会回传编辑结果。

8. 在浏览器 Network 检查请求：

```text
/solar-route/solar/edited
```

9. 航线服务实际接收地址：

```text
http://172.20.63.157:5001/solar/edited
```

## 十四、构建验证

多次执行过：

```bash
npm run build
```

构建可以通过。

构建过程中存在大量项目原有 warning，主要是：

1. Vue `::v-deep` 或 `:deep` 旧写法 deprecated。
2. Sass `@import` deprecated。
3. 组件命名冲突 warning，例如 `FlightAreaPanel`。

这些 warning 是项目已有问题，不是本次三维航线改动新引入的阻断错误。

## 十五、已改动文件清单

本轮围绕三维航线和光伏三维航线编辑，主要改动/新增了以下文件：

```text
cloud_api_web/src/components/WaylinePanel.vue
cloud_api_web/src/router/index.ts
cloud_api_web/src/components/cesium/3DMapPanel.vue
cloud_api_web/src/components/cesium/Solar3DRouteEditor.vue
cloud_api_web/src/components/cesium/uavCamera.vue
cloud_api_web/src/api/solar3d-route.ts
cloud_api_web/vite.config.ts
```

新增本文档：

```text
PROJECT_CONTEXT_3D_ROUTE.md
```

## 十六、后续建议

1. 若临时接收接口联调稳定，后续应由正式后端提供预览报文接收和前端获取能力，不建议生产环境依赖 Vite 临时接口。
2. 当前“生成航线”按钮在光伏三维页面实际是“回传编辑结果”，后续可考虑改名为“提交编辑结果”或“回传航点”。
3. 若焦距继续不符合预期，应先确认航线服务预览报文中的焦距字段名称和单位。
4. 若需要正式实时推送预览报文，可考虑后端 WebSocket 或 SSE，而不是前端轮询 Vite 临时接口。
5. 若要对接 3DGS，应先确认模型格式，再决定走 3D Tiles 转换方案还是 Cesium + Three.js 双渲染方案。
