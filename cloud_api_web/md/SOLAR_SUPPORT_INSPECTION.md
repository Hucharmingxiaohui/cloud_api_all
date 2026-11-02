# 光伏组件支架巡检

> 本文记录“新建光伏计划可选组件支架巡检”的当前实现、数据链路、航线几何与已知限制，供后续联调和维护使用。
>
> 相关但独立的功能说明见：[SOLAR_PANEL_AUTO_PARTITION.md](SOLAR_PANEL_AUTO_PARTITION.md)。

## 1. 功能目标

现有光伏计划仅生成面向组件板面的航线。选择“组件 + 支架”后，服务先输出全部组件行，再从最后一排开始倒序输出支架行，使同一架无人机执行一条混合航线。

当前实现范围仅为：

- 新建计划时选择是否同时巡检组件支架；
- 原组件航点保留不变；
- 全部组件行完成后，从最后一排开始倒序追加支架斜视航点；
- 未勾选时保持原航线行为；
- 支架航点与组件航点共用一次任务和 KMZ。

不包含逆变器、汇流箱、箱变等其他设备的预置多视角点检。

## 2. 新建计划使用方式

路径：`任务管理 → 新建光伏计划 → 第二步光伏参数配置`

巡检内容改为两个独立复选项：

```text
光伏组件
组件支架
```

| 选择 | 航线结果 |
| --- | --- |
| 仅组件 | 只生成原组件巡检航点。 |
| 仅支架 | 只生成支架斜视航点，不保留组件航点。 |
| 组件 + 支架 | 先巡检组件第 1 排至最后一排，再巡检支架最后一排至第 1 排。4 排、每排 3 个组件点时，预期为 24 个航点。 |

至少应选择一类巡检对象。勾选“组件支架”后，显示计划级统一参数：

| 参数 | 请求字段 | 默认值 | 含义 |
| --- | --- | --- | --- |
| 支架相对高度 | `support_rel_alt` | 10 m | 支架航点相对组件板面的高度。 |
| 支架云台俯仰 | `support_pitch` | -50° | 支架拍摄的云台俯仰角。 |
| 支架后向余量 | `support_margin` | 1 m | 视线从板后沿下方穿入时的额外后向距离。 |

## 3. 请求与服务调用链

```text
createSolarPlan.vue
  └─ POST /waylinePlan/createWaylinePlan
       inspect_components: true / false
       inspect_support: true / false
       support_rel_alt / support_pitch / support_margin
       ↓
PubWaylineJobPlanDfEntity 巡检对象和支架参数
  └─ RoutePlanServiceImpl.buildSolarPanelWayline()
       ↓
POST http://172.20.63.157:5001/solar
       inspectComponents / inspectSupport
       supportRelAlt / supportPitch / supportMargin
       ↓
uav_server /solar
  └─ 按 uniform / polygon / adaptive 三种模式生成组件与支架航点
```

### 命名规则注意事项

Java 后端配置了全局 `SNAKE_CASE` Jackson 策略，因此前端必须提交：

```json
{
  "inspect_components": true,
  "inspect_support": true,
  "support_rel_alt": 10,
  "support_pitch": -50,
  "support_margin": 1
}
```

不能只提交 `inspectSupport`。实体仍保留 camelCase `@JsonAlias`，用于兼容历史请求；`RoutePlanServiceImpl` 发往 C++ 服务时使用 camelCase。

## 4. 修改文件

### 前端：`cloud_api_web`

| 文件 | 责任 |
| --- | --- |
| `src/components/task/solarflyPlan/createSolarPlan.vue` | 组件/支架独立复选框；支架参数输入；请求转换为 snake_case。 |

关键位置：

- 默认选择组件；支架参数默认 `10/-50/1`；
- `getGroupPlanBody()` 写入组件/支架选择及三个支架参数；
- 预览（`index=0`）和正式创建（`index=1`）均复用同一请求体构造逻辑。

### Java 后端：`cloud_api_serve_git/sample`

| 文件 | 责任 |
| --- | --- |
| `df/electricInspectionDf/model/PubWaylineJobPlanDfEntity.java` | 增加非持久化组件/支架选择及三个支架参数字段，并用 `@JsonAlias` 兼容 camelCase。 |
| `df/uavCommonHandleDf/service/impl/RoutePlanServiceImpl.java` | 调用 `/solar` 前写入组件/支架选择及支架参数；缺省使用组件=true、支架=false、10/-50/1。 |
| `src/test/java/com/dji/sample/df/uavCommonHandleDf/service/impl/RoutePlanServiceImplTest.java` | 验证 snake_case、camelCase、字段缺省及仅支架参数透传。 |

### 航线服务：`D:\cluade project\uav_server`

| 文件 | 责任 |
| --- | --- |
| `include/http_routes.hpp` | `/solar` 解析组件/支架选择及支架参数，传入三种生成路径。 |
| `include/solar.hpp` | 支架斜视航点生成逻辑；自定义四角、自定义多边形、自适应板排三种路径均调用。 |
| `tests/support_route_contract.test.js` | 静态合同检查：三种路径均接收开关、追加支架行，并采用斜视低空参数。运行：`node tests/support_route_contract.test.js`。 |

`uav_server` 仅在服务器 Linux 环境编译运行；修改头文件后需在服务器重新编译并重启进程。

## 5. 支架航线几何

### 5.1 第一版镜像垂直拍的问题

第一版仅将组件航点向板后方镜像，保持组件航高和近垂直视角。实测小倾角组件（例如倾角 10°、组件线相对板面 20m、相机约 -80°）只能拍到组件背面，组件会遮挡后立柱和后斜梁，无法达到支架巡检目的。

### 5.2 当前斜视低空方案

`include/solar.hpp` 中保留以下默认值；请求携带参数时以请求值为准：

```cpp
kDefaultSupportRelAlt = 10.0;  // 支架航点相对板面的高度，单位：m
kDefaultSupportPitch  = -50.0; // 云台俯仰角，单位：度
kDefaultSupportMargin = 1.0;   // 从板后沿下方通过的额外余量，单位：m
```

支架线的设计原则：

1. 相对板面降至默认 10m；
2. 云台使用默认 -50° 斜视，而不是垂直拍板背；
3. 从组件航点回退其原有前向偏移，再按板面朝向向后偏移；
4. 后向距离由 `相对高度 / tan(|俯仰角|) + 板后沿投影 + 后向余量` 计算；
5. 混合巡检时，先完整输出全部组件行，再从最近的最后一排开始，倒序输出全部支架行；每条支架行内部仍逆着对应组件行方向飞行，避免末排到首排的长距离折返和逐排反复升降；
6. 支架航点机头/云台朝向为组件航点 `heading + 180°`，照片后缀为原区域后缀加 `_support`。

该方案的目的不是拍到完整组件背板，而是让视线从板后沿下方穿入，获取后立柱、后斜梁等可见结构。

## 6. 验收与联调

### 6.1 开关验证

仅组件时，`/solar` 日志应含：

```json
"inspectComponents": true,
"inspectSupport": false
```

仅支架时，应含：

```json
"inspectComponents": false,
"inspectSupport": true
```

若该字段完全缺失，说明运行中的 Java 后端未部署本次 `RoutePlanServiceImpl` 修改。

### 6.2 航点数量验证

对于 4 排、每排 3 个航点的区域：

```text
仅组件：12 个航点
仅支架：12 个航点
组件 + 支架：24 个航点
```

服务日志中的 `总航点数` 和最终 payload 的 `totalPoints` 都应一致。

### 6.3 预览/试飞核对

- 仅支架模式不应有组件点；混合模式中支架点高度应低于组件点；
- 混合模式应先完成全部组件行，再按“支架最后一排 → … → 支架第 1 排”完成支架行；不应出现组件/支架交错或从末排飞回首排才开始支架巡检；
- 支架点 pitch 应为 `-50`，不应继续是组件拍摄用的 `-80`；
- 支架点 heading 应与对应组件点相差 180°；
- 画面应能看到后立柱/后斜梁，而不仅是组件背面；
- 支架线位于屋顶排间低空区域，必须先确认女儿墙、水箱、天线等障碍物净空，再实际飞行。

## 7. 当前已知限制

1. 支架线高度、俯仰角和后向余量当前写死，尚未做页面或站点配置；不同排距、倾角、屋顶障碍物环境可能需要调整常量。
2. 无人机可见范围仍受组件遮挡限制；拍不到的基础、连接件等不能标记为已完成无人机检查。
3. 组件支架图片采用 `_support` 后缀。结果分析、缺陷归档和报告需明确区分组件照片与支架照片，不能按“所有照片都属于组件”处理。
4. 当前光伏报告属于缺陷报告而不是全量影像报告：报告只从 `DefectEntity.imagePath` 插入图片，不会直接使用平台媒体预览中的全部照片。支架照片已能在平台预览，不代表必然进入报告。
5. 已发现报告生成存在恢复性问题：后端生成前即写入 `is_reported=1`；若生成中途失败，后续请求返回“报告已生成”，但可能留下很小或不完整的 DOCX。排查后需先使用“重置报告”再重新生成。

## 8. 验证记录

- 前端目标文件 ESLint 通过；
- Java `RoutePlanServiceImplTest` 通过，覆盖 `inspect_support`、`inspectSupport` 与字段缺省；
- `node tests/support_route_contract.test.js` 通过；
- C++ 服务完整编译和实拍验证需在 Linux 服务器环境执行。
