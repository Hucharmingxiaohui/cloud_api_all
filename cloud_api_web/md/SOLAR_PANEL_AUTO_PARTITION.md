# 光伏巡检 - 新建光伏区域 - 自动分区

本文档记录"光伏巡视区域管理 → 新增 → 自动分区"功能的实现位置、算法演进过程（含两次已知问题的根因与修复），供后续维护或更换 AI 助手时快速恢复上下文。

## 一、功能入口与交互流程

路由入口（`src/router/index.ts`，约 131 行）：

```text
/variableMgt/solarMgt/solarPanelMgt   光伏巡视区域管理
```

完整操作流程：

1. 列表页点击"新增"，切换到 `solarDraw.vue` 绘制页面；
2. 选择正射图（默认选第一张），自动调用 `fetchPanelExtent()` 查询已识别光伏板；
3. 若该图尚未识别光伏板，点击"识别光伏板"（走后端 `detecSolarImgByIdApi`，按板名 upsert）；
4. 输入分区数（默认 9），点击"一键分区"，前端本地计算生成互不重叠的分区四边形；
5. 在下方分区列表中填写每个分区的 `tilt_angle / area_height / panel_height` 参数；
6. 点击"保存"，像素角点随参数提交到后端。

## 二、涉及文件

| 文件 | 作用 |
| --- | --- |
| `src/components/variableMgt/solarMgt/solarPanelMgt/solarDraw.vue` | 自动分区 UI、识别/分区/保存流程、Canvas 绘制 |
| `src/components/variableMgt/solarMgt/solarPanelMgt/solarPartitionGeometry.ts` | 纯几何算法：板数据标准化、分组、分区矩形生成 |
| `src/api/turbine/turbineMgt.ts` | `getDetectedSolarPanelsApi` / `detecSolarImgByIdApi` / `insertSolarPanelApi` 等 |
| 后端 `SolarPanelAreaController` | `POST /api/solarPanelArea/detectAreaGenSolar` 保存分区并转地理坐标 |

关键代码位置（行号以本文档撰写时为准，可能随改动漂移，以函数名为准）：

```text
solarDraw.vue
  autoPartition()                              一键分区入口（校验、确认弹窗、生成区域）
  partitionPanelsIntoNonOverlappingRectangles  调用点（doPartition 内）
  AUTO_PARTITION_PADDING = 10                  外扩像素（画布坐标，乘缩放比换算到原图像素）
  drawDetectedPanelMasks()                     叠加绘制已识别光伏板掩膜及板名标注

solarPartitionGeometry.ts
  normalizeDetectedPanel()                     后端板角点 → 顺时针 PixelPoint[]，过滤退化板
  partitionPanelsIntoNonOverlappingRectangles() 对外主入口
  createPartitionCells()                       递归行列网格切分（核心，每层局部拟合方向）
  applyPartitionPadding()                      局部四边外扩 + SAT 相交检测兜底
  getProjectionFrame()                         由板集最小面积矩形建立投影坐标系（每层递归各自计算）
  partitionPanelsBalanced() / expandRectangle() 旧版算法，仍导出但已不再被调用
```

## 三、算法演进与问题记录

### v1：分组最小外接矩形 + 独立外扩（已废弃，存在重叠问题）

最初实现：先用双中心迭代 `partitionPanelsBalanced` 按板数均衡分组，每组取最小面积旋转矩形 `getGroupRectangle`，再各自用 `expandRectangle` 向四周扩 `AUTO_PARTITION_PADDING`。

问题根因（用户反馈"分出来的区域很多会重叠"）：

1. 各组只是板的集合，外接矩形之间没有任何互斥约束，组 A 的矩形可以压到组 B 的板上；
2. 每个矩形独立向四周扩 10 像素，本来贴边甚至小范围重叠的矩形被进一步放大重叠；
3. 保存链路（前端 `saveDetectionAreas` → 后端 `detectAreaGenSolar`）全程无相交校验，重叠原样入库。

### v2：单轴连续条带（已废弃，单列正常、多列混乱）

第一次修复：沿整体最小面积矩形的**主轴**把所有板按投影值排序，切成连续条带，相邻条带共享一条中间边界，padding 不再互相扩张。

该版本在"单列（每行一块板）"下表现正常，但用户验证"21 块板、7 行 × 3 列"时分区完全错乱。根因：

- 同一行的 3 块板在主轴上的投影值几乎相同；
- 当分区数与整行板数对不齐（如 21 块分 9 区，目标组大小为 3,3,3,3,3,2,2,1,1）时，切割点会落进同一行内部；
- 相邻条带边界的"左右外缘中点"随之塌缩到该行的投影值上，产生**零宽/极窄的退化矩形**，一排板被硬拆到两个区域，绘制结果混乱。

### v3（当前）：递归行列网格切分

即 `createPartitionCells()`，机制上同时保证"不重叠"与"不切断光伏板"：

1. 每一层递归都用**当前板集自己的最小面积矩形**建立旋转投影坐标系（u = 主轴方向，v = 法向）；因此同一正射图内存在不同朝向或错位的多簇阵列时，各簇矩形会各自对齐板排，不会继承全局错误角度；
2. 每层递归比较当前板集在 u、v 两个方向上的**角点跨度**，选跨度更大的方向下刀（kd-tree 式，7 行 × 3 列会先切出整行、再细分）；
3. 按中心投影排序后，按 `targetGroupSizes` 的均衡数量取切分点；若切分点两侧板的投影相同（即落在同一排/列内部），就近向两侧寻找第一个有空隙的位置作为切点，保证一排板永远不会被拆到两个区域；
4. 递归到组数为 1 时，叶子矩形直接取该组板自己的紧致范围（u/v 各自的 min/max），不会出现退化矩形；
5. `applyPartitionPadding()` 在每个分区自己的局部坐标系中四边外扩。若外扩后的任意两个凸四边形通过 SAT（分离轴定理）检测到相交，则所有区域退回不外扩，保证不会因留白产生重叠；
6. 若传入图片边界 `bounds`，对 padding 做二分搜索，取不越界的最大外扩值（阵列贴图边时自动放弃外扩）。

以 21 块（7 行 × 3 列）分 9 区为例，输出为：前 5 行各一个整行分区（各 3 块），第 6、7 行各按 2+1 切成两个分区，板数分布与目标一致且每个矩形干净地框住自己的板。

## 四、验证情况

- `npm run build:test` 构建通过（含 vite-plugin-eslint 对几何文件的检查）；
- `npx vue-tsc --noEmit` 在本仓库当前状态下不可用（TS 4.5 与已安装的新版 `@types/node` 等类型不兼容，且多个无关旧文件本就有语法错误），与本次改动无关；
- 实机验证：单列布局正常；7 行 × 3 列、21 块板场景待用户在页面确认。

## 五、已知行为与可调点

1. **切分方向优先级**：当前按"每层局部跨度更大"自动选择，不保证全局先横切或先竖切。如需固定方向（例如总是先按列竖切），可在 `createPartitionCells()` 的方向选择处调整。
2. **板数均衡**：为对齐整行/整列，允许组大小与平均值有 ±1 块左右的自然差异；调整切点的"就近搜索"逻辑在 `createPartitionCells()` 内。
3. **外扩量**：`solarDraw.vue` 中 `AUTO_PARTITION_PADDING = 10`（画布像素），如需调整分区之间的留白只改这个常量即可；分区之间的公共边永远不外扩。
4. **后端不校验重叠**：`detectAreaGenSolar` 直接保存前端角点，防重叠完全依赖前端本算法；若后续有其他入口写入分区，需要在后端补校验。
5. **旧算法保留**：`partitionPanelsBalanced` / `expandRectangle` 仍导出但不再被页面使用，确认无其他引用后可清理。
