<template>
  <el-dialog
    :model-value="modelValue"
    width="900"
    :close-on-click-modal="false"
    @close="handleClose"
    style="background-color: #0A2D63; color: white"
  >
    <template #title>
      <span style="color: white;font-size: 18px;">光伏组件详情</span>
    </template>
    <div class="detail-container">
      <!-- 左侧树结构 -->
      <div class="tree-panel">
        <el-tree
          :data="treeData"
          :props="defaultProps"
          highlight-current
          node-key="id"
          @node-click="handleNodeClick"
          ref="treeRef"
        />
      </div>
      <!-- 右侧图片绘制区域 -->
      <div class="image-panel">
        <div class="image-wrapper" ref="imageWrapper">
          <img
            v-if="imageUrl"
            :src="imageUrl"
            alt="正射图"
            ref="imageRef"
            class="solar-image"
            @load="onImageLoad"
          />
          <canvas
            ref="canvasRef"
            class="draw-canvas"
            width="540"
            height="540"
          />
          <div v-if="!imageUrl" class="no-image-tip">
            <el-empty description="暂无图片" :image-size="100" />
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, watch, nextTick, computed, reactive, defineProps, defineEmits } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrthophotoByUrlApi, getSolarPanelByIdApi, getSolarPanelPostionByIdApi, getComponentListByIdApi } from '/@/api/turbine/turbineMgt'

const props = defineProps<{
  modelValue: boolean
  rowData: any
}>()

const emit = defineEmits(['update:modelValue', 'close'])

const treeRef = ref()
const imageRef = ref<HTMLImageElement>()
const canvasRef = ref<HTMLCanvasElement>()
const imageWrapper = ref<HTMLElement>()
const imageUrl = ref('')
const ctx = ref<CanvasRenderingContext2D | null>(null)
const imgNaturalSize = reactive({ width: 0, height: 0 })

const CANVAS_SIZE = 540

const defaultProps = {
  children: 'children',
  label: 'label'
}

// 将 component_list 转换为树形数据
const treeData = computed(() => {
  const list = props.rowData?.component_list || []
  return list.map((item: any) => ({
    id: item.solarPanelId,
    label: item.solarPanelName || '未命名阵列',
    type: 'panel',
    children: (item.children || []).map((child: any) => ({
      id: child.componentId,
      label: child.componentName || '未命名组件',
      type: 'component'
    }))
  }))
})

watch(() => props.modelValue, async (val) => {
  if (val) {
    // TODO: 测试完成后恢复为：await loadImage(props.rowData?.path)
    // await loadTestImage()
    await loadImage(props.rowData?.path)
  }
})

function handleClose () {
  emit('update:modelValue', false)
  emit('close')
  // 清空画布和图片
  clearCanvas()
  if (imageUrl.value) {
    URL.revokeObjectURL(imageUrl.value)
    imageUrl.value = ''
  }
}

// 临时测试完成后恢复为 loadImage，删除 loadTestImage
async function loadImage (path: string) {
  try {
    if (imageUrl.value) {
      URL.revokeObjectURL(imageUrl.value)
    }
    const blobData = await getOrthophotoByUrlApi(path)
    const blob = new Blob([blobData], { type: 'image/png' })
    imageUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    ElMessage.error('加载图片失败')
    console.error(error)
  }
}

// async function loadTestImage () {
//   try {
//     if (imageUrl.value) {
//       URL.revokeObjectURL(imageUrl.value)
//     }
//     imageUrl.value = new URL('./test.jpg', import.meta.url).href
//   } catch (error) {
//     ElMessage.error('加载测试图片失败')
//     console.error(error)
//   }
// }

function onImageLoad () {
  if (imageRef.value) {
    imgNaturalSize.width = imageRef.value.naturalWidth
    imgNaturalSize.height = imageRef.value.naturalHeight
  }
  nextTick(() => {
    initCanvas()
  })
}

function initCanvas () {
  if (!canvasRef.value) return
  ctx.value = canvasRef.value.getContext('2d')
  clearCanvas()
}

function clearCanvas () {
  if (!ctx.value || !canvasRef.value) return
  ctx.value.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
}

// 绘制四边形角点
function drawPolygon (points: Array<{ row?: number; col?: number; x?: number; y?: number }>) {
  if (!ctx.value || points.length < 4) return

  clearCanvas()

  // 解析坐标（兼容 row/col 和 x/y 两种格式）
  const rawPoints = points.map((p: any) => ({
    x: p.col ?? p.x ?? 0,
    y: p.row ?? p.y ?? 0
  }))

  // 将原始图片像素坐标缩放到 540x540 画布
  let displayPoints = rawPoints
  if (imgNaturalSize.width > 0 && imgNaturalSize.height > 0) {
    const scaleX = CANVAS_SIZE / imgNaturalSize.width
    const scaleY = CANVAS_SIZE / imgNaturalSize.height
    displayPoints = rawPoints.map(p => ({
      x: p.x * scaleX,
      y: p.y * scaleY
    }))
  }

  ctx.value.strokeStyle = '#FF6B6B'
  ctx.value.lineWidth = 2
  ctx.value.fillStyle = 'rgba(255, 107, 107, 0.2)'

  ctx.value.beginPath()
  ctx.value.moveTo(displayPoints[0].x, displayPoints[0].y)
  for (let i = 1; i < displayPoints.length; i++) {
    ctx.value.lineTo(displayPoints[i].x, displayPoints[i].y)
  }
  ctx.value.closePath()
  ctx.value.fill()
  ctx.value.stroke()

  // 绘制角点标记
  // displayPoints.forEach((p, idx) => {
  //   ctx.value!.fillStyle = '#FF6B6B'
  //   ctx.value!.beginPath()
  //   ctx.value!.arc(p.x, p.y, 4, 0, Math.PI * 2)
  //   ctx.value!.fill()

  //   // 绘制序号
  //   ctx.value!.fillStyle = '#fff'
  //   ctx.value!.font = '12px Arial'
  //   ctx.value!.fillText(String(idx + 1), p.x + 6, p.y - 6)
  // })
}

function extractCorners (item: any): Array<{ col: number; row: number }> {
  const hasCorner =
    item.corner1_col !== undefined && item.corner1_row !== undefined &&
    item.corner2_col !== undefined && item.corner2_row !== undefined &&
    item.corner3_col !== undefined && item.corner3_row !== undefined &&
    item.corner4_col !== undefined && item.corner4_row !== undefined

  if (!hasCorner) return []

  return [
    { col: item.corner1_col, row: item.corner1_row },
    { col: item.corner2_col, row: item.corner2_row },
    { col: item.corner3_col, row: item.corner3_row },
    { col: item.corner4_col, row: item.corner4_row }
  ]
}

async function handleNodeClick (data: any) {
  try {
    let res
    if (data.type === 'panel') {
      res = await getSolarPanelPostionByIdApi(data.id)
    } else {
      res = await getComponentListByIdApi(data.id)
    }

    if (res.code !== 0) {
      ElMessage.error('获取数据失败')
      return
    }

    let corners: Array<{ col: number; row: number }> = []

    if (data.type === 'panel') {
      // 一级节点返回对象
      corners = extractCorners(res.data)
    } else {
      // 二级节点返回列表，需匹配当前点击的 component id
      const list = Array.isArray(res.data) ? res.data : [res.data]
      const target = list.find((item: any) => item.id === data.id)
      console.log('sssss', target)
      if (target) {
        corners = extractCorners(target)
      }
    }

    if (corners.length >= 4) {
      drawPolygon(corners)
    } else {
      clearCanvas()
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取数据失败')
  }
}
</script>

<style lang="scss" scoped>

.detail-container {
  display: flex;
  gap: 20px;
  height: 540px;
}

.tree-panel {
  width: 280px;
  background: rgba(6, 38, 90, 0.6);
  border-radius: 4px;
  padding: 15px;
  overflow-y: auto;
}

.image-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(6, 38, 90, 0.6);
  border-radius: 4px;
}

.image-wrapper {
  position: relative;
  width: 540px;
  height: 540px;
  background: #1a2d4a;
  overflow: hidden;
}

.solar-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.draw-canvas {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 10;
}

.no-image-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

:deep(.el-dialog__title) {
  font-size: 20px;
  color: white !important;
}

:deep(.el-dialog) {
  background-color: #0B2757;
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
}

:deep(.el-tree) {
  background: transparent;
  color: #fff;
  .el-text{
    color: #e1e1e1 !important;
  }
  .el-tree-node.is-current {
    > .el-tree-node__content {
      background-color: rgba(0, 114, 245, 0.5) !important;

      .el-tree-node__label {
        color: rgb(15, 131, 248) !important;
      }
    }
  }
}

:deep(.el-tree-node__content:hover) {
  background-color: rgba(0, 114, 245, 0.3);

}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: rgba(0, 114, 245, 0.5);
}

:deep(.el-tree-node:focus > .el-tree-node__content) {
  background-color: rgba(0, 114, 245, 0.5);
}

:deep(.el-empty__description) {
  color: #a0aec0;
}
</style>
