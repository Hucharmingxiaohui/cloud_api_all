<template>
  <div class="load-solar-panel">
    <div class="image-container">
      <!-- 图片加载中 -->
      <div v-if="imageLoading" class="image-loading">
        <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
        <span class="loading-text">图片加载中...</span>
      </div>

      <div v-if="selectedImage" class="image-wrapper">
        <img
          :src="selectedImage"
          alt="光伏区正射图"
          @load="onImageLoad"
          @error="onImageError"
          ref="imageRef"
          class="solar-image"
        />
        <canvas
          ref="canvasRef"
          class="draw-canvas"
        />
      </div>

      <!-- 无图片提示 -->
      <div v-else class="no-image-tip">
        <el-empty description="请选择光伏板区域以加载正射图" :image-size="150">
          <template #image>
            <el-icon :size="150"><Picture /></el-icon>
          </template>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch, defineProps } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, Loading } from '@element-plus/icons-vue'
import { getOrthophotoByUrlApi } from '/@/api/turbine/turbineMgt'

interface Point {
  x: number
  y: number
}

interface DetectArea {
  solar_panel_area_name: string
  corner1_col: number
  corner1_row: number
  corner2_col: number
  corner2_row: number
  corner3_col: number
  corner3_row: number
  corner4_col: number
  corner4_row: number
}

const props = defineProps<{
  imagePath: string
  detectAreas: DetectArea | null
}>()

const selectedImage = ref<string>('')
const imageRef = ref<HTMLImageElement>()
const canvasRef = ref<HTMLCanvasElement>()
const ctx = ref<CanvasRenderingContext2D | null>(null)
const imageLoading = ref(false)

const CANVAS_WIDTH = 1000
const CANVAS_HEIGHT = 750

// 监听 props 变化
watch(() => [props.imagePath, props.detectAreas], async ([newPath, newArea]) => {
  console.log('props.imagePath=', props.imagePath)
  console.log('props.detectAreas=', props.detectAreas)
  if (newPath && newArea && typeof newArea === 'object' && 'corner1_col' in newArea) {
    await loadImage(newPath as string)
  } else {
    selectedImage.value = ''
    clearCanvas()
  }
}, { deep: true })

// 加载图片
async function loadImage (path: string) {
  if (!path) return
  imageLoading.value = true
  try {
    const blobData = await getOrthophotoByUrlApi(path)
    const blob = new Blob([blobData], { type: 'image/png' })
    selectedImage.value = URL.createObjectURL(blob)
  } catch (error) {
    console.error('加载图片失败:', error)
    ElMessage.error('加载正射图失败')
    imageLoading.value = false
  }
}

// 图片加载完成
function onImageLoad () {
  imageLoading.value = false
  if (!imageRef.value || !canvasRef.value) return

  const canvas = canvasRef.value
  canvas.width = CANVAS_WIDTH
  canvas.height = CANVAS_HEIGHT
  ctx.value = canvas.getContext('2d')

  drawSingleArea()
}

function onImageError () {
  imageLoading.value = false
  ElMessage.error('图片加载失败')
}

// 清空画布
function clearCanvas () {
  if (!ctx.value || !canvasRef.value) return
  ctx.value.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
}

// 绘制单个区域
function drawSingleArea () {
  if (!ctx.value || !imageRef.value || !props.detectAreas) return

  clearCanvas()

  const naturalWidth = imageRef.value.naturalWidth || CANVAS_WIDTH
  const naturalHeight = imageRef.value.naturalHeight || CANVAS_HEIGHT

  const scaleX = CANVAS_WIDTH / naturalWidth
  const scaleY = CANVAS_HEIGHT / naturalHeight

  const area = props.detectAreas

  const points: Point[] = [
    { x: Math.round(area.corner1_col * scaleX), y: Math.round(area.corner1_row * scaleY) },
    { x: Math.round(area.corner2_col * scaleX), y: Math.round(area.corner2_row * scaleY) },
    { x: Math.round(area.corner3_col * scaleX), y: Math.round(area.corner3_row * scaleY) },
    { x: Math.round(area.corner4_col * scaleX), y: Math.round(area.corner4_row * scaleY) }
  ]

  drawArea(points, area.solar_panel_area_name)
}

// 绘制区域
function drawArea (points: Point[], name: string) {
  if (!ctx.value || points.length !== 4) return

  const color = '#FF6B6B'

  // 绘制四边形边框
  ctx.value.strokeStyle = color
  ctx.value.lineWidth = 2
  ctx.value.setLineDash([5, 5])

  ctx.value.beginPath()
  ctx.value.moveTo(points[0].x, points[0].y)
  for (let i = 1; i < points.length; i++) {
    ctx.value.lineTo(points[i].x, points[i].y)
  }
  ctx.value.closePath()
  ctx.value.stroke()

  // 绘制半透明填充
  ctx.value.fillStyle = color + '20'
  ctx.value.fill()

  // 绘制区域名称
  ctx.value.fillStyle = color
  ctx.value.font = 'bold 14px Arial'
  ctx.value.textBaseline = 'top'
  ctx.value.fillText(name, points[0].x + 5, points[0].y + 5)

  // 绘制角点
  points.forEach(p => drawPoint(p.x, p.y, color))
}

// 绘制顶点标记
function drawPoint (x: number, y: number, color: string) {
  if (!ctx.value) return
  const size = 6
  const half = size / 2
  ctx.value.fillStyle = color
  ctx.value.setLineDash([])
  ctx.value.fillRect(x - half, y - half, size, size)
}
</script>

<style lang="scss" scoped>
.load-solar-panel {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;

  .image-container {
    width: 1000px;
    height: 750px;
    position: relative;
    overflow: hidden;
    background: #1a2d4a;

    .image-wrapper {
      position: relative;
      width: 100%;
      height: 100%;
    }

    .solar-image {
      position: absolute;
      width: 100%;
      height: 100%;
      object-fit: fill;
    }

    .draw-canvas {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      z-index: 10;
    }

    .image-loading {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: rgba(26, 45, 74, 0.9);
      z-index: 15;

      .loading-icon {
        color: #4299e1;
        animation: rotate 1s linear infinite;
        margin-bottom: 10px;
      }

      .loading-text {
        color: #a0aec0;
        font-size: 14px;
      }
    }

    .no-image-tip {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      color: #a0aec0;
    }
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
