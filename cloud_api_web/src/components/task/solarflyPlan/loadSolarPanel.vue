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
import { ref, watch, defineProps, onMounted } from 'vue'
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

interface WaylinePoint {
  col: number
  row: number
  lon: number
  lat: number
  height: number
  heading: number
  pitch: number
}

const props = defineProps<{
  imagePath: string
  detectAreas: DetectArea[],
  waylineInfo: WaylinePoint[]
}>()

const selectedImage = ref<string>('')
const imageRef = ref<HTMLImageElement>()
const canvasRef = ref<HTMLCanvasElement>()
const ctx = ref<CanvasRenderingContext2D | null>(null)
const imageLoading = ref(false)

const CANVAS_WIDTH = 1000
const CANVAS_HEIGHT = 750

// 监听 props 变化
watch(() => [props.imagePath, props.detectAreas, props.waylineInfo], async ([newPath, newAreas, newWayline]) => {
  if (newPath && newAreas && Array.isArray(newAreas) && newAreas.length > 0) {
    await loadImage(newPath as string)
    // loadTestImage()
  } else {
    selectedImage.value = ''
    clearCanvas()
  }

  // waylineInfo 变化时重新绘制
  if (newWayline && Array.isArray(newWayline) && newWayline.length > 0 && imageRef.value?.complete) {
    drawWayline()
  }
}, { deep: true, immediate: true })

// onMounted(() => {
//   loadTestImage()
//   drawWayline()
// })

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

async function loadTestImage () {
  try {
    selectedImage.value = new URL('./test.jpg', import.meta.url).href
  } catch (error) {
    ElMessage.error('加载测试图片失败')
    console.error(error)
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

  drawAreas()
  drawWayline()
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

// 区域颜色池 - 荧光高亮色
const AREA_COLORS = ['#39FF14', '#FF073A', '#00F0FF', '#FFEA00', '#FF00FF', '#FF8C00']

// 绘制所有区域
function drawAreas () {
  if (!ctx.value || !imageRef.value || !props.detectAreas || props.detectAreas.length === 0) return

  clearCanvas()

  const naturalWidth = imageRef.value.naturalWidth || CANVAS_WIDTH
  const naturalHeight = imageRef.value.naturalHeight || CANVAS_HEIGHT

  const scaleX = CANVAS_WIDTH / naturalWidth
  const scaleY = CANVAS_HEIGHT / naturalHeight

  props.detectAreas.forEach((area, index) => {
    const color = AREA_COLORS[index % AREA_COLORS.length]

    const points: Point[] = [
      { x: Math.round(area.corner1_col * scaleX), y: Math.round(area.corner1_row * scaleY) },
      { x: Math.round(area.corner2_col * scaleX), y: Math.round(area.corner2_row * scaleY) },
      { x: Math.round(area.corner3_col * scaleX), y: Math.round(area.corner3_row * scaleY) },
      { x: Math.round(area.corner4_col * scaleX), y: Math.round(area.corner4_row * scaleY) }
    ]

    drawArea(points, area.solar_panel_area_name, color)
  })
}

// 绘制区域
function drawArea (points: Point[], name: string, color: string = '#39FF14') {
  if (!ctx.value || points.length !== 4) return

  const ctx2d = ctx.value

  // 绘制发光边框（先画较宽低透明度产生光晕）
  ctx2d.shadowColor = color
  ctx2d.shadowBlur = 12
  ctx2d.strokeStyle = color
  ctx2d.lineWidth = 4
  ctx2d.setLineDash([6, 4])
  ctx2d.lineJoin = 'round'

  ctx2d.beginPath()
  ctx2d.moveTo(points[0].x, points[0].y)
  for (let i = 1; i < points.length; i++) {
    ctx2d.lineTo(points[i].x, points[i].y)
  }
  ctx2d.closePath()
  ctx2d.stroke()

  // 清除阴影后再画实线边框增强清晰度
  ctx2d.shadowBlur = 0
  ctx2d.lineWidth = 2
  ctx2d.strokeStyle = '#FFFFFF'
  ctx2d.setLineDash([])
  ctx2d.stroke()

  // 绘制荧光填充
  ctx2d.fillStyle = hexToRgba(color, 0.25)
  ctx2d.fill()

  // 绘制区域名称（带描边加大号）
  const labelX = points[0].x + 8
  const labelY = points[0].y + 8
  ctx2d.font = 'bold 18px Arial'
  ctx2d.textBaseline = 'top'

  // 文字描边，增强可读性
  ctx2d.lineWidth = 4
  ctx2d.strokeStyle = '#000000'
  ctx2d.lineJoin = 'round'
  ctx2d.strokeText(name, labelX, labelY)

  ctx2d.fillStyle = color
  ctx2d.shadowColor = color
  ctx2d.shadowBlur = 8
  ctx2d.fillText(name, labelX, labelY)
  ctx2d.shadowBlur = 0

  // 绘制角点（菱形）
  points.forEach(p => drawPoint(p.x, p.y, color))
}

function hexToRgba (hex: string, alpha: number) {
  const cleaned = hex.replace('#', '')
  const bigint = parseInt(cleaned, 16)
  const r = (bigint >> 16) & 255
  const g = (bigint >> 8) & 255
  const b = bigint & 255
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

// 绘制顶点标记
function drawPoint (x: number, y: number, color: string) {
  if (!ctx.value) return
  const size = 8
  const half = size / 2
  const ctx2d = ctx.value
  ctx2d.shadowColor = color
  ctx2d.shadowBlur = 10
  ctx2d.fillStyle = color
  ctx2d.setLineDash([])
  ctx2d.beginPath()
  ctx2d.moveTo(x, y - half)
  ctx2d.lineTo(x + half, y)
  ctx2d.lineTo(x, y + half)
  ctx2d.lineTo(x - half, y)
  ctx2d.closePath()
  ctx2d.fill()
  ctx2d.shadowBlur = 0

  // 白色中心点增强对比
  ctx2d.fillStyle = '#FFFFFF'
  ctx2d.beginPath()
  ctx2d.arc(x, y, 2, 0, Math.PI * 2)
  ctx2d.fill()
}

// 绘制航线
function drawWayline () {
  if (!ctx.value || !imageRef.value || !props.waylineInfo || props.waylineInfo.length === 0) return

  const naturalWidth = imageRef.value.naturalWidth || CANVAS_WIDTH
  const naturalHeight = imageRef.value.naturalHeight || CANVAS_HEIGHT

  const scaleX = CANVAS_WIDTH / naturalWidth
  const scaleY = CANVAS_HEIGHT / naturalHeight

  const waylineColor = '#4ADE80'
  const textColor = '#FFFFFF'

  // 连线
  ctx.value.strokeStyle = waylineColor
  ctx.value.lineWidth = 2
  ctx.value.setLineDash([])
  ctx.value.beginPath()

  props.waylineInfo.forEach((point: WaylinePoint, index: number) => {
    const x = Math.round(point.col * scaleX)
    const y = Math.round(point.row * scaleY)

    if (index === 0) {
      ctx.value!.moveTo(x, y)
    } else {
      ctx.value!.lineTo(x, y)
    }
  })
  ctx.value.stroke()

  // 航点标记和名称
  props.waylineInfo.forEach((point: WaylinePoint, index: number) => {
    const x = Math.round(point.col * scaleX)
    const y = Math.round(point.row * scaleY)

    // 绘制航点圆圈
    const radius = 5
    ctx.value!.beginPath()
    ctx.value!.arc(x, y, radius, 0, Math.PI * 2)
    ctx.value!.fillStyle = waylineColor
    ctx.value!.fill()
    ctx.value!.strokeStyle = '#FFFFFF'
    ctx.value!.lineWidth = 1.5
    ctx.value!.stroke()

    // 绘制航点名称
    const name = `航点${index + 1}`
    ctx.value!.fillStyle = textColor
    ctx.value!.font = 'bold 12px Arial'
    ctx.value!.textBaseline = 'bottom'
    ctx.value!.fillText(name, x + 8, y - 5)
  })
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
