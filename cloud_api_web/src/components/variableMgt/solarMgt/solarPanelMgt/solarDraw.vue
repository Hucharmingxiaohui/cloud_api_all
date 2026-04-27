<template>
  <div class="solar-area-draw-container">
    <!-- 顶部操作栏 -->
    <div class="header">

      <div class="header-title" style="padding-left: 10px; margin-right: 15px; "  >
          <span style="margin-right: 5px;">光伏区域管理</span>
           <span>></span>
          <span style="margin-left: 5px;">新增光伏区</span>
          <el-button class="back-btn" @click="goBack" :icon="Back" size="small">返回</el-button>
      </div>
      <div class="actions">
        <el-button
          class="start-draw-btn"
          type="primary"
          @click="toggleDrawingMode"
          :disabled="!selectedImage"
        >
          {{ isDrawing ? '结束绘制' : '开始绘制' }}
        </el-button>
        <el-button
          class="save-btn"
          type="success"
          @click="saveDetectionAreas"
          :disabled="detectionAreas.length === 0 || !selectedImage"
        >
          保存
        </el-button>

      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧面板 -->
      <div class="left-panel">
        <!-- 图片选择区域 -->
        <div class="image-select-section">
          <h3>选择光伏区正射图</h3>
          <div class="image-select-wrapper">
            <el-select
              v-model="selectedImageId"
              placeholder="请选择光伏区正射图"
              @change="handleImageChange"
              class="image-select"
              :teleported="false"
              filterable
              :disabled="isDrawing"
            >
              <el-option
                v-for="item in orthophotoList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              >
              </el-option>
            </el-select>
            <el-button
              type="primary"
              size="small"
              @click="openImportDialog"
              :disabled="isDrawing"
            >
              导入
            </el-button>
          </div>
        </div>

        <!-- 检测区域列表 -->
        <div class="area-list-section">
          <div class="section-header">
            <h3>检测区域列表 ({{ detectionAreas.length }})</h3>
            <div class="section-header-right">
              <div class="draw-status" :class="{ 'drawing': isDrawing }">
                {{ isDrawing ? '绘制模式中...' : '点击"开始绘制"添加区域' }}
              </div>
              <el-button
                type="danger"
                size="small"
                @click="clearAllAreas"
                :disabled="detectionAreas.length === 0 || isDrawing"
              >
                清空所有
              </el-button>
            </div>
          </div>

          <div class="area-list">
            <div
              v-for="(area) in detectionAreas"
              :key="area.id"
              class="area-item"
              :class="{
                'active': activeAreaId === area.id,
                'hovered': hoveredAreaId === area.id
              }"
              @mouseenter="hoveredAreaId = area.id"
              @mouseleave="hoveredAreaId = null"
              @click="selectArea(area.id)"
            >
              <div class="area-color-indicator" :style="{ backgroundColor: getAreaColor(area.id) }"></div>
              <div class="area-info">
                <div class="area-name">
                  {{ area.name }}
                  <el-tag v-if="activeAreaId === area.id" size="small" type="info">当前选中</el-tag>
                </div>
                <div class="area-coords">
                  <div class="coord-item">
                    <span>左上: ({{ area.coordinates.x1.toFixed(2) }}, {{ area.coordinates.y1.toFixed(2) }})</span>
                  </div>
                  <div class="coord-item">
                    <span>右下: ({{ area.coordinates.x2.toFixed(2) }}, {{ area.coordinates.y2.toFixed(2) }})</span>
                  </div>
                </div>
              </div>
              <div class="area-actions">
                <el-button
                  type="primary"
                  size="small"
                  @click.stop="editAreaName(area)"
                  :disabled="isDrawing"
                >
                  重命名
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click.stop="deleteArea(area.id)"
                  :disabled="isDrawing"
                >
                  删除
                </el-button>
              </div>
            </div>

            <div v-if="detectionAreas.length === 0" class="empty-tip">
              <el-empty description="暂无检测区域" :image-size="100">
                <template #description>
                  <p style="color: #999; margin-top: 10px;">点击"开始绘制"按钮，然后在图片上绘制矩形区域</p>
                </template>
              </el-empty>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧图片绘制区域 -->
      <div class="right-panel">
        <div class="image-container" ref="imageContainer">
          <!-- 图片显示 -->
          <div v-if="selectedImage" class="image-wrapper">
            <!-- 图片加载中 -->
            <div v-if="imageLoading" class="image-loading">
              <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
              <span class="loading-text">图片加载中...</span>
            </div>
            <img
              :src="selectedImage"
              alt="光伏区正射图"
              @load="onImageLoad"
              @error="onImageError"
              ref="imageRef"
              class="solar-image"
              :style="{ cursor: isDrawing ? 'crosshair' : 'default' }"
            />

            <!-- 绘制画布 -->
            <canvas
              ref="canvasRef"
              class="draw-canvas"
              :class="{ 'drawing-mode': isDrawing }"
              @mousedown="handleMouseDown"
              @mousemove="handleMouseMove"
              @mouseup="handleMouseUp"
              @mouseleave="handleMouseLeave"
            />

            <!-- 绘制引导文字 -->
            <div v-if="isDrawing && !isDrawingRect" class="drawing-hint">
              <div class="hint-content">
                <el-icon class="hint-icon"><Position /></el-icon>
                <span>点击并拖动鼠标绘制矩形区域</span>
              </div>
            </div>

            <!-- 当前绘制预览 -->
            <div v-if="isDrawingRect" class="drawing-preview">
              宽: {{ Math.abs(currentRect.width) }}px, 高: {{ Math.abs(currentRect.height) }}px
            </div>
          </div>

          <!-- 无图片提示 -->
          <div v-else class="no-image-tip">
            <el-empty description="请先选择光伏区正射图" :image-size="150">
              <template #image>
                <el-icon :size="150"><Picture /></el-icon>
              </template>
            </el-empty>
          </div>
        </div>

        <!-- 绘制状态提示 -->
        <!-- <div v-if="isDrawing" class="drawing-tip">
          <el-alert
            title="绘制模式已启用"
            type="info"
            :closable="false"
            center
            show-icon
          >
            在图片上点击并拖动鼠标绘制矩形区域。按 ESC 键可取消当前绘制。
          </el-alert>
        </div> -->

      </div>
       <!-- 右侧参数区域 -->
      <div class="param-panel">
        <h3 style="color: #fff; margin: 0 0 15px 0; font-size: 16px; font-weight: 600;">光伏区域参数</h3>
        <el-form :model="paramForm" label-width="120px" label-position="left" style="color: #fff;">
          <el-form-item label="航线高度（米）" required>
            <el-input v-model="paramForm.flight_altitude" type="number" placeholder="请输入航线高度" />
          </el-form-item>
          <el-form-item label="光伏板倾角（度）" required>
            <el-input v-model="paramForm.tilt_angle" type="number" placeholder="请输入光伏板倾角" />
          </el-form-item>
          <el-form-item label="横向航线数" required>
            <el-input v-model="paramForm.horizontal_lines" type="number" placeholder="请输入横向航线数" />
          </el-form-item>
          <el-form-item label="光伏区域海拔" required>
            <el-input v-model="paramForm.area_height" type="number" placeholder="请输入光伏区域海拔" />
          </el-form-item>
          <el-form-item label="光伏架设高度" required>
            <el-input v-model="paramForm.panel_height" type="number" placeholder="请输入光伏架设高度" />
          </el-form-item>
          <el-form-item label="光伏板朝向" required>
            <el-input v-model="paramForm.panel_heading" type="number" placeholder="请输入光伏板朝向" />
          </el-form-item>
          <el-form-item label="区域边距" required>
            <el-input v-model="paramForm.margin" type="number" placeholder="请输入区域边距" />
          </el-form-item>
          <el-form-item label="航线内点数" required>
            <el-input v-model="paramForm.points_per_line" type="number" placeholder="请输入航线内点数" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 导入正射图对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入正射图"
      width="500px"
      :before-close="handleImportClose"
    >
      <el-form :model="importForm" label-width="100px">
        <el-form-item label="图片名称" required>
          <el-input
            v-model="importForm.name"
            placeholder="请输入图片名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="上传图片" required>
          <el-upload
            ref="uploadRef"
            class="upload-demo"
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            accept="image/*"
          >
            <el-button type="primary">选择图片</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 JPG/PNG 格式图片
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmImport" :loading="importing">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 区域重命名对话框 -->
    <el-dialog
      v-model="renameDialogVisible"
      title="重命名检测区域"
      width="400px"
      :before-close="handleRenameClose"
    >
      <el-form :model="renameForm" label-width="80px">
        <el-form-item label="区域名称" required>
          <el-input
            v-model="renameForm.name"
            placeholder="请输入区域名称"
            maxlength="50"
            show-word-limit
            @keyup.enter="confirmRename"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="renameDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmRename">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, onUnmounted, computed, defineEmits, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Position, Back, ArrowRight, Loading } from '@element-plus/icons-vue'
import { getOrthophotoListApi, getOrthophotoByUrlApi, insertSolarPanelApi, importSolarPanelImgApi } from '/@/api/turbine/turbineMgt'

const emit = defineEmits(['back'])

const router = useRouter()

// 光伏区正射图列表
const orthophotoList = ref<any[]>([])
const selectedImageId = ref<string>('')
const selectedImage = ref<string>('')
const currentImageInfo = ref<any>({})
const imageRef = ref<HTMLImageElement>()
const canvasRef = ref<HTMLCanvasElement>()
const imageContainer = ref<HTMLElement>()

// 图片绘制相关
const ctx = ref<CanvasRenderingContext2D | null>(null)
const imageObj = ref<HTMLImageElement | null>(null)
const imgSize = reactive({
  naturalWidth: 0,
  naturalHeight: 0,
  displayWidth: 0,
  displayHeight: 0
})

// 缩放比例计算（图片已铺满画布，无需额外偏移）
const scaleX = 1
const scaleY = 1

// 绘制状态
const isDrawing = ref(false)
const isDrawingRect = ref(false)
const drawingRect = reactive({
  startX: 0,
  startY: 0,
  endX: 0,
  endY: 0
})

// 当前绘制预览
const currentRect = reactive({
  x: 0,
  y: 0,
  width: 0,
  height: 0
})

// 检测区域数据
interface DetectionArea {
  id: number
  name: string
  coordinates: {
    x1: number
    y1: number
    x2: number
    y2: number
  }
  displayRect?: {
    x: number
    y: number
    width: number
    height: number
  }
}

const detectionAreas = ref<DetectionArea[]>([])
const activeAreaId = ref<number | null>(null)
const hoveredAreaId = ref<number | null>(null)
let areaIdCounter = 1

// 对话框控制
const renameDialogVisible = ref(false)
const importDialogVisible = ref(false)
const importing = ref(false)
const saving = ref(false)
const renameForm = reactive({
  areaId: null as number | null,
  name: ''
})
const importForm = reactive({
  name: ''
})
const importFile = ref<File | null>(null)
const uploadRef = ref()

// 图片缩放
const imageScale = ref(1)
const mousePosition = reactive({ x: -1, y: -1 })
const imageLoading = ref(false)

// 航线参数表单
const paramForm = reactive({
  flight_altitude: null as number | null,
  tilt_angle: null as number | null,
  horizontal_lines: null as number | null,
  area_height: null as number | null,
  panel_height: null as number | null,
  panel_heading: null as number | null,
  margin: null as number | null,
  points_per_line: null as number | null
})

// 颜色列表（用于区分不同区域）
const areaColors = [
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
  '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9',
  '#F8C471', '#82E0AA', '#F1948A', '#85C1E9', '#F7DC6F'
]

// 初始化
onMounted(() => {
  loadOrthophotoList()
  // 监听ESC键退出绘制模式
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})

// 加载光伏区正射图列表
async function loadOrthophotoList () {
  try {
    const res = await getOrthophotoListApi({
      pageSize: 10000,
      pageNo: 1
    })
    if (res.code === 0) {
      orthophotoList.value = res.data.list || []
      if (orthophotoList.value.length > 0) {
        selectedImageId.value = orthophotoList.value[0].id
        handleImageChange()
      }
    } else {
      ElMessage.error('加载正射图列表失败')
    }
  } catch (error) {
    ElMessage.error('加载正射图列表失败')
    console.error(error)
  }
}

// 图片选择变化
async function handleImageChange () {
  if (!selectedImageId.value) {
    selectedImage.value = ''
    clearCanvas()
    detectionAreas.value = []
    return
  }

  const selected = orthophotoList.value.find(item => item.id === selectedImageId.value)
  if (!selected) return

  currentImageInfo.value = selected

  imageLoading.value = true

  try {
    // 👇 这里只会执行一次！
    const imageUrl = await getImageUrlFrom(selected.path)
    selectedImage.value = imageUrl
    detectionAreas.value = []
    activeAreaId.value = null
  } catch (error) {
    ElMessage.error('加载图片失败')
    console.error(error)
    imageLoading.value = false
  }
}

// 从API获取图片URL
async function getImageUrlFrom (path: string): Promise<string> {
  try {
    const blobData = await getOrthophotoByUrlApi(path)
    const blob = new Blob([blobData], { type: 'image/png' })
    return URL.createObjectURL(blob)
  } catch (error) {
    console.error('获取图片URL失败:', error)
    throw error
  }
}

// 固定画布尺寸
const CANVAS_WIDTH = 1000
const CANVAS_HEIGHT = 750

// 图片加载完成
function onImageLoad () {
  imageLoading.value = false

  if (!imageRef.value || !canvasRef.value) return
  const canvas = canvasRef.value

  // 记录图片原始尺寸
  imgSize.naturalWidth = imageRef.value.naturalWidth
  imgSize.naturalHeight = imageRef.value.naturalHeight

  // 图片直接铺满容器（通过 CSS object-fit: fill）
  imgSize.displayWidth = CANVAS_WIDTH
  imgSize.displayHeight = CANVAS_HEIGHT

  // 设置画布固定尺寸
  canvas.width = CANVAS_WIDTH
  canvas.height = CANVAS_HEIGHT
  ctx.value = canvas.getContext('2d')

  redrawAllAreas()
}

// 清空画布
function clearCanvas () {
  if (!ctx.value || !canvasRef.value) return
  ctx.value.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
}

// 绘制所有区域
function redrawAllAreas () {
  if (!ctx.value || !canvasRef.value) return

  clearCanvas()

  // 绘制所有区域
  detectionAreas.value.forEach(area => {
    drawArea(area, area.id === activeAreaId.value, area.id === hoveredAreaId.value)
  })

  // 如果有正在绘制的矩形，绘制它
  if (isDrawingRect.value) {
    drawCurrentRect()
  }
}

// 绘制单个区域
function drawArea (area: DetectionArea, isActive = false, isHovered = false) {
  if (!ctx.value || !area.displayRect) return

  const rect = area.displayRect

  // 画布坐标即图片坐标（图片已铺满画布）
  const x = rect.x
  const y = rect.y
  const width = rect.width
  const height = rect.height

  // 设置样式
  ctx.value.strokeStyle = getAreaColor(area.id)
  ctx.value.lineWidth = isActive ? 3 : 2
  ctx.value.setLineDash(isActive ? [] : [5, 5])

  // 绘制矩形
  ctx.value.beginPath()
  ctx.value.rect(x, y, width, height)
  ctx.value.stroke()

  // 如果被选中或悬停，绘制填充
  if (isActive || isHovered) {
    ctx.value.fillStyle = getAreaColor(area.id) + '20' // 添加透明度
    ctx.value.fillRect(x, y, width, height)
  }

  // 绘制区域名称
  ctx.value.fillStyle = getAreaColor(area.id)
  ctx.value.font = '14px Arial'
  ctx.value.textBaseline = 'top'
  ctx.value.fillText(area.name, x + 5, y + 5)

  // 绘制角点
  drawCorners(x, y, width, height, getAreaColor(area.id))
}

// 绘制角点
function drawCorners (x: number, y: number, width: number, height: number, color: string) {
  if (!ctx.value) return

  const cornerSize = 6
  const halfCorner = cornerSize / 2

  ctx.value.fillStyle = color
  ctx.value.setLineDash([])

  // 四个角
  const corners = [
    [x, y], // 左上
    [x + width, y], // 右上
    [x + width, y + height], // 右下
    [x, y + height] // 左下
  ]

  corners.forEach(corner => {
    ctx.value!.fillRect(corner[0] - halfCorner, corner[1] - halfCorner, cornerSize, cornerSize)
  })
}

// 绘制当前正在绘制的矩形
function drawCurrentRect () {
  if (!ctx.value || !isDrawingRect.value) return

  const x = Math.min(drawingRect.startX, drawingRect.endX)
  const y = Math.min(drawingRect.startY, drawingRect.endY)
  const width = Math.abs(drawingRect.endX - drawingRect.startX)
  const height = Math.abs(drawingRect.endY - drawingRect.startY)

  ctx.value.strokeStyle = '#FF6B6B'
  ctx.value.lineWidth = 2
  ctx.value.setLineDash([5, 5])
  ctx.value.fillStyle = '#FF6B6B20'

  ctx.value.beginPath()
  ctx.value.rect(x, y, width, height)
  ctx.value.stroke()
  ctx.value.fill()

  // 更新预览
  currentRect.x = x
  currentRect.y = y
  currentRect.width = width
  currentRect.height = height
}

// 鼠标事件处理
function handleMouseDown (event: MouseEvent) {
  if (!isDrawing.value || !canvasRef.value) return

  const rect = canvasRef.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  isDrawingRect.value = true
  drawingRect.startX = x
  drawingRect.startY = y
  drawingRect.endX = x
  drawingRect.endY = y

  // 清除之前的绘制预览
  clearCanvas()
  redrawAllAreas()
}

function handleMouseMove (event: MouseEvent) {
  if (!canvasRef.value) return

  const rect = canvasRef.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  // 更新鼠标坐标显示（画布坐标即图片坐标）
  mousePosition.x = Math.round(x)
  mousePosition.y = Math.round(y)

  if (isDrawingRect.value) {
    drawingRect.endX = x
    drawingRect.endY = y

    // 清除并重绘
    clearCanvas()
    redrawAllAreas()
    drawCurrentRect()
  }
}

function handleMouseUp (event: MouseEvent) {
  if (!isDrawingRect.value || !canvasRef.value) return

  const rect = canvasRef.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  drawingRect.endX = x
  drawingRect.endY = y

  // 计算矩形尺寸
  const width = Math.abs(drawingRect.endX - drawingRect.startX)
  const height = Math.abs(drawingRect.endY - drawingRect.startY)

  // 最小尺寸检查
  if (width < 10 || height < 10) {
    ElMessage.warning('绘制区域太小，请重新绘制')
    isDrawingRect.value = false
    redrawAllAreas()
    return
  }

  // 创建新的检测区域
  const newArea: DetectionArea = {
    id: areaIdCounter++,
    name: `检测区${detectionAreas.value.length + 1}`,
    coordinates: {
      x1: Math.min(drawingRect.startX, drawingRect.endX),
      y1: Math.min(drawingRect.startY, drawingRect.endY),
      x2: Math.max(drawingRect.startX, drawingRect.endX),
      y2: Math.max(drawingRect.startY, drawingRect.endY)
    },
    displayRect: {
      x: Math.min(drawingRect.startX, drawingRect.endX),
      y: Math.min(drawingRect.startY, drawingRect.endY),
      width: Math.abs(drawingRect.endX - drawingRect.startX),
      height: Math.abs(drawingRect.endY - drawingRect.startY)
    }
  }

  detectionAreas.value.push(newArea)
  activeAreaId.value = newArea.id

  isDrawingRect.value = false

  // 自动结束绘制模式
  isDrawing.value = false

  // 重绘
  redrawAllAreas()

  ElMessage.success(`已添加检测区域: ${newArea.name}`)
}

function handleMouseLeave () {
  mousePosition.x = -1
  mousePosition.y = -1

  if (isDrawingRect.value) {
    isDrawingRect.value = false
    redrawAllAreas()
  }
}

// 键盘事件处理
function handleKeyDown (event: KeyboardEvent) {
  if (event.key === 'Escape') {
    if (isDrawingRect.value) {
      isDrawingRect.value = false
      redrawAllAreas()
    } else if (isDrawing.value) {
      isDrawing.value = false
    }
  }
}

// 切换绘制模式
function toggleDrawingMode () {
  isDrawing.value = !isDrawing.value
  if (isDrawing.value) {
    ElMessage.info('绘制模式已开启，在图片上点击并拖动绘制矩形区域')
  } else {
    ElMessage.info('绘制模式已关闭')
  }
}

// 选择区域
function selectArea (areaId: number) {
  if (isDrawing.value) return // 绘制模式下禁止选择

  activeAreaId.value = areaId
  redrawAllAreas()
}

// 获取区域颜色
function getAreaColor (areaId: number): string {
  const index = (areaId - 1) % areaColors.length
  return areaColors[index]
}

// 删除区域
function deleteArea (areaId: number) {
  if (isDrawing.value) {
    ElMessage.warning('绘制模式下不能删除区域，请先结束绘制')
    return
  }

  const area = detectionAreas.value.find(a => a.id === areaId)
  if (!area) return

  ElMessageBox.confirm(
    `确定要删除检测区域 "${area.name}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    const index = detectionAreas.value.findIndex(a => a.id === areaId)
    if (index !== -1) {
      detectionAreas.value.splice(index, 1)
      if (activeAreaId.value === areaId) {
        activeAreaId.value = detectionAreas.value.length > 0 ? detectionAreas.value[0].id : null
      }
      redrawAllAreas()
      ElMessage.success('删除成功')
    }
  }).catch(() => {
    // 用户取消删除
  })
}

// 编辑区域名称
function editAreaName (area: DetectionArea) {
  if (isDrawing.value) {
    ElMessage.warning('绘制模式下不能编辑区域名称，请先结束绘制')
    return
  }

  renameForm.areaId = area.id
  renameForm.name = area.name
  renameDialogVisible.value = true
}

function handleRenameClose (done: () => void) {
  renameForm.areaId = null
  renameForm.name = ''
  done()
}

function confirmRename () {
  if (!renameForm.name.trim()) {
    ElMessage.warning('请输入区域名称')
    return
  }

  const area = detectionAreas.value.find(a => a.id === renameForm.areaId)
  if (area) {
    area.name = renameForm.name.trim()
    redrawAllAreas()
    ElMessage.success('重命名成功')
  }

  renameDialogVisible.value = false
  renameForm.areaId = null
  renameForm.name = ''
}

// 清空所有区域
function clearAllAreas () {
  if (detectionAreas.value.length === 0) return

  if (isDrawing.value) {
    ElMessage.warning('绘制模式下不能清空区域，请先结束绘制')
    return
  }

  ElMessageBox.confirm(
    '确定要清空所有检测区域吗？此操作不可撤销。',
    '清空确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    detectionAreas.value = []
    activeAreaId.value = null
    clearCanvas()
    ElMessage.success('已清空所有检测区域')
  }).catch(() => {
    // 用户取消
  })
}

// 保存检测区域
async function saveDetectionAreas () {
  if (detectionAreas.value.length === 0) {
    ElMessage.warning('请先绘制检测区域')
    return
  }

  // 校验所有数值参数
  const numericFields = [
    'flight_altitude', 'tilt_angle', 'horizontal_lines',
    'area_height', 'panel_height', 'panel_heading', 'margin', 'points_per_line'
  ]
  for (const field of numericFields) {
    if (paramForm[field] === null || paramForm[field] === undefined || paramForm[field] === '') {
      ElMessage.warning('请填写完整参数')
      return
    }
  }

  saving.value = true

  try {
    // 计算图片拉伸比例
    const scaleX = imgSize.naturalWidth / CANVAS_WIDTH
    const scaleY = imgSize.naturalHeight / CANVAS_HEIGHT

    const saveData = {
      solar_area_name: currentImageInfo.value.name || '',
      detect_areas: detectionAreas.value.map(area => ({
        area_name: area.name,
        corners_pixels: [
          { row: Math.round(area.coordinates.y1 * scaleY), col: Math.round(area.coordinates.x1 * scaleX) },
          { row: Math.round(area.coordinates.y1 * scaleY), col: Math.round(area.coordinates.x2 * scaleX) },
          { row: Math.round(area.coordinates.y2 * scaleY), col: Math.round(area.coordinates.x2 * scaleX) },
          { row: Math.round(area.coordinates.y2 * scaleY), col: Math.round(area.coordinates.x1 * scaleX) }
        ]
      })),
      flight_altitude: Number(paramForm.flight_altitude),
      tilt_angle: Number(paramForm.tilt_angle),
      horizontal_lines: Number(paramForm.horizontal_lines),
      area_height: Number(paramForm.area_height),
      panel_height: Number(paramForm.panel_height),
      panel_heading: Number(paramForm.panel_heading),
      margin: Number(paramForm.margin),
      points_per_line: Number(paramForm.points_per_line)
    }

    const res = await insertSolarPanelApi(saveData)

    if (res.code === 0) {
      ElMessage.success('保存成功')
      // 清空数据
      detectionAreas.value = []
      activeAreaId.value = null
      clearCanvas()
      // 重置表单
      paramForm.flight_altitude = null
      paramForm.tilt_angle = null
      paramForm.horizontal_lines = null
      paramForm.area_height = null
      paramForm.panel_height = null
      paramForm.panel_heading = null
      paramForm.margin = null
      paramForm.points_per_line = null
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// 图片加载失败
function onImageError () {
  imageLoading.value = false
  ElMessage.error('图片加载失败')
}

// 打开导入对话框
function openImportDialog () {
  importForm.name = ''
  importFile.value = null
  importDialogVisible.value = true
}

// 关闭导入对话框
function handleImportClose (done: () => void) {
  importForm.name = ''
  importFile.value = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  done()
}

// 文件选择变化
function handleFileChange (file: any) {
  importFile.value = file.raw
}

// 确认导入
async function confirmImport () {
  if (!importForm.name.trim()) {
    ElMessage.warning('请输入图片名称')
    return
  }

  const isDuplicate = orthophotoList.value.some(item => item.name === importForm.name.trim())
  if (isDuplicate) {
    ElMessage.warning('该图片名称已存在，请更换名称')
    return
  }

  if (!importFile.value) {
    ElMessage.warning('请选择图片')
    return
  }

  importing.value = true

  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    formData.append('name', importForm.name.trim())

    const res = await importSolarPanelImgApi(formData)

    if (res.code === 0) {
      ElMessage.success('导入成功')
      importDialogVisible.value = false
      // 刷新列表
      await loadOrthophotoList()
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error('导入失败，请稍后重试')
  } finally {
    importing.value = false
    importForm.name = ''
    importFile.value = null
    if (uploadRef.value) {
      uploadRef.value.clearFiles()
    }
  }
}

// 设置图片缩放（固定画布后缩放功能已废弃，保留函数避免报错）
function setImageScale (scale: number) {
  imageScale.value = scale
  ElMessage.info('当前画布已固定尺寸，缩放功能暂不可用')
}

// 返回列表
function goBack () {
  emit('back')
}

// 工具函数
function formatFileSize (bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function formatDate (timestamp: number | string): string {
  const date = new Date(timestamp)
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
}

// 坐标格式化显示
const formatCoords = computed(() => (area: DetectionArea) => {
  return `(${area.coordinates.x1.toFixed(0)}, ${area.coordinates.y1.toFixed(0)}) - (${area.coordinates.x2.toFixed(0)}, ${area.coordinates.y2.toFixed(0)})`
})

</script>

<style lang="scss" scoped>
.solar-area-draw-container {
  width: 100%;
  height: 87vh;
  display: flex;
  flex-direction: column;
//   background: linear-gradient(135deg, #0a1a3a 0%, #1a3a7a 100%);
  color: #fff;
  box-sizing: border-box;

  .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background-color: rgba(1, 36, 98, 1);
      border-radius: 4px;
      height: 60px;
      padding:  15px;
      margin: 31px 12px 0 12px;

      .back-btn {
        margin-left: 15px;
        background: rgba(255, 255, 255, 0.1);
        border-color: rgba(255, 255, 255, 0.2);
        color: #46A2FF;

        &:hover {
          background: rgba(255, 255, 255, 0.2);
        }
      }
    .header-title {
      font-size:16px;
      font-weight: 500;
    }

    .actions {
      display: flex;
      gap: 10px;

      .start-draw-btn {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border: none;

        &:hover {
          background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
        }

        &:disabled {
          background: rgba(102, 126, 234, 0.5);
        }
      }

      .save-btn {
        background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
        border: none;

        &:hover {
          background: linear-gradient(135deg, #38a169 0%, #48bb78 100%);
        }

        &:disabled {
          background: rgba(72, 187, 120, 0.5);
        }
      }

    }
  }

  .main-content {
    margin: 10px;
    padding:0 5px;
    display: flex;
    flex: 1;
    gap: 10px;
    overflow: hidden;

    .left-panel {
      width: 450px;
      display: flex;
      flex-direction: column;
      background-color: #06265a;
      padding: 20px;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      height: 750px;

        .image-select-section {
        margin-bottom: 20px;

        h3 {
          color: #fff;
          margin: 0 0 15px 0;
          font-size: 16px;
          font-weight: 600;
        }

        .image-select-wrapper {
          display: flex;
          gap: 10px;
          align-items: center;

          .image-select {
            flex: 1;
          }
        }

        .image-info {
          background: rgba(26, 54, 93, 0.6);
          border-radius: 6px;
          padding: 12px;
          margin-top: 10px;

          .info-item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;
            font-size: 12px;

            &:last-child {
              margin-bottom: 0;
            }

            .label {
              color: #a0aec0;
            }

            .value {
              color: #fff;
              font-weight: 500;
            }
          }
        }
      }

      .area-list-section {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden;

        .section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 15px;

          h3 {
            color: #fff;
            margin: 0;
            font-size: 16px;
            font-weight: 600;
          }

          .section-header-right {
            display: flex;
            align-items: center;
            gap: 10px;
          }

          .draw-status {
            padding: 4px 8px;
            background: rgba(66, 153, 225, 0.2);
            border-radius: 4px;
            font-size: 12px;
            color: #4299e1;

            &.drawing {
              background: rgba(72, 187, 120, 0.2);
              color: #48bb78;
              animation: pulse 2s infinite;
            }
          }
        }

        .area-list {
          overflow-y: auto;
          background: rgba(26, 54, 93, 0.4);
          border-radius: 6px;
          padding: 10px;
          height: 550px !important;

          .area-item {
            display: flex;
            align-items: center;
            padding: 12px;
            margin-bottom: 8px;
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 6px;
            cursor: pointer;
            transition: all 0.3s ease;

            &:last-child {
              margin-bottom: 0;
            }

            &.active {
              background: rgba(66, 153, 225, 0.2);
              border-color: #4299e1;
              transform: translateX(4px);
            }

            &.hovered:not(.active) {
              background: rgba(255, 255, 255, 0.1);
              border-color: rgba(255, 255, 255, 0.3);
            }

            &:hover:not(.active) {
              background: rgba(255, 255, 255, 0.1);
            }

            .area-color-indicator {
              width: 12px;
              height: 12px;
              border-radius: 50%;
              margin-right: 10px;
              flex-shrink: 0;
            }

            .area-info {
              flex: 1;
              min-width: 0;

              .area-name {
                display: flex;
                align-items: center;
                gap: 8px;
                color: #fff;
                font-weight: 500;
                margin-bottom: 4px;
                font-size: 14px;

                .el-tag {
                  height: 20px;
                  line-height: 18px;
                  padding: 0 6px;
                }
              }

              .area-coords {
                font-size: 12px;
                color: #a0aec0;

                .coord-item {
                  margin-bottom: 2px;

                  &:last-child {
                    margin-bottom: 0;
                  }
                }
              }
            }

            .area-actions {
              display: flex;
              gap: 6px;
              flex-shrink: 0;
            }
          }

          .empty-tip {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 100%;
            color: #a0aec0;
            text-align: center;
            padding: 40px 20px;

            :deep(.el-empty__description) {
              p {
                margin-top: 10px;
              }
            }
          }
        }
      }
    }

    .right-panel {
      width: 1000px;
      height: 750px;
      display: flex;
      flex-direction: column;
      background: rgba(13, 26, 64, 0.8);
      overflow: hidden;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      flex-shrink: 0;

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
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .solar-image {
          position: absolute;
          width: 100%;
          height: 100%;
          object-fit: fill;
          pointer-events: none;
        }

        .draw-canvas {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          cursor: crosshair;
          z-index: 10;

          &.drawing-mode {
            cursor: crosshair;
          }
        }

        .drawing-hint {
          position: absolute;
          top: 20px;
          left: 50%;
          transform: translateX(-50%);
          background: rgba(0, 0, 0, 0.7);
          color: white;
          padding: 10px 20px;
          border-radius: 20px;
          display: flex;
          align-items: center;
          gap: 8px;
          z-index: 20;
          animation: fadeIn 0.3s ease;

          .hint-icon {
            font-size: 16px;
          }
        }

        .drawing-preview {
          position: absolute;
          top: 20px;
          right: 20px;
          background: rgba(0, 0, 0, 0.7);
          color: white;
          padding: 8px 12px;
          border-radius: 4px;
          font-size: 12px;
          z-index: 20;
        }

        .no-image-tip {
          display: flex;
          align-items: center;
          justify-content: center;
          height: 100%;
          color: #a0aec0;

          :deep(.el-icon) {
            color: #4a5568;
          }
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
      }

      .drawing-tip {
        padding: 10px 20px;
        background: rgba(66, 153, 225, 0.1);
        border-top: 1px solid rgba(66, 153, 225, 0.2);

        :deep(.el-alert) {
          background: transparent;
          padding: 0;
        }
      }

      .image-toolbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 20px;
        background: rgba(26, 54, 93, 0.8);
        border-top: 1px solid rgba(255, 255, 255, 0.1);

        .toolbar-left {
          .el-button-group {
            .el-button {
              background: rgba(255, 255, 255, 0.1);
              border-color: rgba(255, 255, 255, 0.2);
              color: #fff;

              &:hover {
                background: rgba(255, 255, 255, 0.2);
              }

              &.is-primary {
                background: #4299e1;
                border-color: #4299e1;
              }
            }
          }
        }

        .toolbar-right {
          .coord-display {
            font-size: 12px;
            color: #a0aec0;
            background: rgba(0, 0, 0, 0.3);
            padding: 4px 8px;
            border-radius: 4px;
          }
        }
      }
    }
    .param-panel {
      width: calc(100% - 1450px);
      display: flex;
      flex-direction: column;
      background-color: #06265a;
      padding: 20px;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      height: 750px;
    }
  }
}

.save-confirm {
  .save-areas-list {
    max-height: 300px;
    overflow-y: auto;
    margin-top: 10px;

    .save-area-item {
      background: rgba(245, 245, 245, 0.1);
      border-radius: 6px;
      padding: 12px;
      margin-bottom: 10px;
      border-left: 4px solid transparent;

      &:last-child {
        margin-bottom: 0;
      }

      .save-area-header {
        display: flex;
        align-items: center;
        margin-bottom: 8px;

        .area-index {
          color: #999;
          margin-right: 8px;
          font-weight: bold;
        }

        .area-name {
          flex: 1;
          color: #333;
          font-weight: 500;
        }

        .area-color {
          width: 12px;
          height: 12px;
          border-radius: 50%;
        }
      }

      .save-area-coords {
        font-size: 12px;
        color: #666;
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// 滚动条样式
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;

  &:hover {
    background: rgba(255, 255, 255, 0.4);
  }
}

// 响应式调整
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column !important;

    .left-panel, .right-panel {
      width: 100% !important;
    }

    .left-panel {
      max-height: 300px;
    }
  }
}
</style>
