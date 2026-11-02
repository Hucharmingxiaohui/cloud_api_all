# 光伏附属设备巡检（逆变器、汇流箱、箱变等）

> 功能版本：v1（2026-10-19）
> 关联提交：`0584e77`（功能主体）、`65f0b58`（补齐缺失文件）
> 前置阅读：`SOLAR_SUPPORT_INSPECTION.md`（组件支架巡检，本文档的姊妹篇）

## 1. 功能目标

光伏场站的逆变器、汇流箱、箱变、配电柜、杆塔、气象站等附属设备数量少、分布零散，不适合像组件那样按区域自动生成航线，采用**台账预采集 + 计划合成**模式：

1. **预采集**：在正射图上标定附属设备位置，按设备预设多个视角拍摄航点（位置、高度、机头朝向、云台俯仰、焦距、拍摄动作），形成设备台账
2. **计划合成**：新建光伏计划时勾选附属设备，预置航点自动追加到该机巢的组件/支架航线末尾，**合成一条任务**
3. **可视化**：预览航线上不同设备的航段分色显示，标注设备名与航点名，标出设备位置

设计决策（已与需求方确认）：
- 合成一条任务（非独立任务）
- 计划级统一参数（与支架参数同口径）
- 多机巢时设备按「与各任务组已选区域质心的最小距离」就近分配；无组选区域时回落到第一组
- 3D 手动编辑与服务器合成的冲突：**edited KMZ 文件存在与否**作为「用户是否编辑过」的信号，存在则直接用（不覆盖用户编辑），不存在才由服务器合成

## 2. 使用方式

### 2.1 设备台账采集（光伏管理 → 光伏附属设备管理）

1. 选择正射图 → 新增设备 → 填名称、类型（逆变器/汇流箱/箱变/配电柜/杆塔/气象站/其他）
2. 「地图设置设备位置」：在正射图上点击落点，像素坐标经 GeoTIFF 服务换算为经纬度
3. 「地图添加航点」或手动添加：每航点填绝对椭球高（米）、机头偏航（0=北，顺时针）、云台俯仰、焦距、拍摄动作（拍照/无动作）、拍摄模式（可见光/红外/双光）；可排序、启停
4. 保存；删除正射图时设备与航点级联清理

注意：**地图只采集经纬度，高度必须人工录入**（绝对椭球高，米）。

### 2.2 计划创建（新建光伏计划 → 第二步）

1. 「附属设备」区域勾选要巡检的设备（可按类型筛选），界面显示每台设备的归属机巢与航点数
2. 按分组顺序预览航线（底部「预览航线」按钮，一次预览全部未创建分组）；预览图上：
   - 主航线绿色，仅标起点/终点
   - 各设备航段独立配色（10 色轮换），航点旁标注「设备名 · 航点名」
   - 设备实际位置画菱形标记并以设备色标注名称
3. 创建任务；含附属设备的分组**必须先预览**，否则前端拦截

## 3. 数据与接口

### 3.1 数据库（`cloud_api_serve_git/sql/20261014_solar_accessory.sql`，需手动执行）

- `manage_solar_accessory`：设备（id、orthophoto_id、name、device_type、经纬度、height=绝对椭球高、enabled）
- `manage_solar_accessory_waypoint`：预置航点（device_id、sort_order 唯一、经纬高、heading、pitch、focal_length、action、image_format、enabled）

### 3.2 后端接口（`/api/solarAccessory`）

| 接口 | 说明 |
|---|---|
| `GET /list?orthophoto_id=` | 按正射图查设备（含航点，按 sort_order） |
| `POST /save` | 聚合保存（事务；更新时整体替换航点；校验经纬高/姿态/焦距/枚举） |
| `POST /delete` | 删除设备及其航点 |
| `POST /coordinates` | 像素↔经纬度换算（direction=pixel_to_geo / geo_to_pixel），代理调用 Python GeoTIFF 服务，单点即可 |

内部服务：
- `SolarAccessoryService.listEnabledByIds(ids, orthophotoId)`：计划用——仅启用设备+启用航点，保持勾选顺序，无航点设备剔除
- `SolarAccessoryRouteMerger`（纯函数）：预览航点格式构造（对齐 C++ `buildThreeDRoutePayload` 相机 JSON）、route_2d 点增强（device_id/device_name/waypoint_name + accessory_devices 标记数组）、edited 输入映射

## 4. 航线合成链路（不改 C++）

调用链：`RoutePlanServiceImpl.buildSolarPanelWayline` → C++ `/solar`（http://…:5001/solar）→ 响应加工 → 保存分支读 KMZ 导入。

- **预览（index=0）**：Java 在 C++ 返回的 `three_d_payload.waypoints` 与 `route_2d.area.points` 末尾追加设备航点；`route_2d` 另带 `accessory_devices` 设备位置标记；像素坐标经 `SolarAccessoryCoordinateService` 批量换算（航点+设备位置一次请求；失败仅告警，2D 不叠加、3D 不受影响）
- **创建（index=1）**：
  - `{routeName}edited.kmz` **已存在**（用户在 3D 编辑器保存过；设备点已随预览载荷进入编辑器，编辑结果天然包含）→ 直接使用，不覆盖
  - **不存在** → Java 把「主航线航点+设备航点」映射为 edited 输入格式，POST `/solar/edited`（配置 `solar.route.edited-url`，默认 http://…:5001/solar/edited）→ C++ 写 `solar{planId}edited.kmz` → 现有保存分支优先读取
  - 选了设备但 edited 文件仍缺失 → 创建直接报错，不静默丢设备拍摄

命名链：主航线 `solar{planId}` → 编辑产物 `solar{planId}edited.kmz`（C++ edited 端点按 source_request.planId 命名）。

## 5. 修改文件

### 前端 `cloud_api_web`
- `src/api/solar-accessory.ts`（新增）：台账接口封装
- `src/components/variableMgt/solarMgt/solarAccessoryMgt/`（新增）：`index.vue` 采集页 + `accessoryModel.ts` 纯函数（校验/排序/枚举）
- `src/components/task/solarflyPlan/createSolarPlan.vue`：附属设备勾选区、就近分配、`accessory_device_ids` 按组下发、预览守卫、底部预览按钮
- `src/components/task/solarflyPlan/loadSolarPanel.vue`：航线分段分色绘制、航点/设备标注、设备菱形标记、重绘清屏
- `src/router/index.ts`：菜单「光伏管理 → 光伏附属设备管理」

### Java 后端 `cloud_api_serve_git/sample`
- `solarDf`：`SolarAccessory`/`SolarAccessoryWaypoint` 实体、两个 Mapper、`SolarAccessoryService`、`SolarAccessoryCoordinateService`、`SolarAccessoryRouteMerger`、`SolarAccessoryController`
- `electricInspectionDf`：`PubWaylineJobPlanDfEntity` 新增瞬态字段 `accessoryDeviceIds`（@JsonAlias 兼容 snake/camel）
- `uavCommonHandleDf`：`RoutePlanServiceImpl` 预览追加/创建合成/edited 守卫
- `OrthophotoEntityServiceImpl`：正射图删除级联清理设备与航点

### 不涉及
- C++ 航线服务（uav_server）、Python GeoTIFF 服务：**无改动**，仅调用既有 `/solar`、`/solar/edited`、`/detectArea`、`/geoToPixel`

## 6. 验证记录

- 后端单测 15/15 通过（`SolarAccessoryRouteMergerTest` 4、`SolarAccessoryCoordinateServiceTest` 3、`SolarAccessoryServiceTest` 7、`RoutePlanServiceImplTest` 1）；注意父 pom `skipTests=true`，需 `-DskipTests=false` 才真正执行
- 前端：模型测试脚本 `scripts/test-solar-accessory.cjs` 通过；两个 SFC 编译 + ESLint 通过
- 实机：测试站已走通「采集 → 计划勾选 → 预览 → 创建」（2026-10-19 用户确认可用）；预览分色标注已实机验证

## 7. 当前已知限制

1. **焦距口径**：采集页录入的毫米焦距按 **35mm 等效焦距**传递（与 Pilot 变焦标注口径一致）；红外/可见光同值
2. **拍摄动作**：合成链路中每个设备航点默认都会拍照（编辑链路无逐点动作模型），`action=无动作` 字段暂未在该链路生效
3. **操作时序**：若先做过 3D 编辑、后勾选设备再创建，会使用旧编辑航线（不含设备点）——建议顺序「勾选设备 → 预览 → （如需）编辑 → 创建」
4. **像素对齐前提**：沿用既有假设（显示用正射图与 TIF 像素维度一致），不一致时 2D 标注偏移
5. **高度口径**：设备高度按绝对椭球高录入，与主航线高度口径的一致性依赖外业录入规范，预览时可目视核对
6. 3D 预览（Cesium 编辑器）中设备航点位置可见，但连线仍为单色；分色仅 2D 预览实现

## 8. 部署清单

1. 执行 `sql/20261014_solar_accessory.sql`（幂等，可重复执行）
2. 后端 `sample` 重新打包 + 重启（仅替换文件不重启无效）
3. 前端重新构建部署
4. 依赖：`application.yml` 的 `wayline.detect-area-gen-url`（GeoTIFF 服务）与 `solar.route.edited-url`（默认 http://172.20.63.157:5001/solar/edited）可达；正射图同名 `.tif` 存在
