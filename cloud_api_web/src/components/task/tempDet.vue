<template>
  <div class="TEMPPanel">
    <div class="content1">
      <div class="content-left">
        <el-select
          v-model="tempType"
          placeholder="请选择"
          size="large"
          class="select-operation"
          :teleported="false"
          @change="updateTempType"
        >
          <el-option
            v-for="item in tempTypeOption"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <div class="button-wrapper">
          <!-- 新增字段和输入框 -->
          <div class="temp-fields">
            <el-form label-position="top" label-width="100px">
              <el-row style="margin-top: 15px;">
                <!-- 最高温度 -->
                <el-col :span="24">
                  <el-form-item
                    label="最高温度°C"
                    style="color: #fff;"
                    v-if="tempType == 2"
                  >
                    <el-input
                      v-model="tt.max_tem"
                      placeholder="最高温度"
                      class="info-input1"
                    ></el-input>
                  </el-form-item>
                  <el-form-item label="温度°C" style="color: #fff;" v-else>
                    <el-input
                      v-model="tt.point_tem"
                      placeholder="温度"
                      class="info-input1"
                    >
                    </el-input>
                  </el-form-item>
                </el-col>
                <!-- 最高温度坐标 -->
                <!-- <el-col :span="24">
                    <el-form-item label="最高温度坐标" :label-style="{ color: 'white' }" v-if="tempType == 2">
                      <el-input v-model="tt" placeholder="最高温度坐标"></el-input>
                    </el-form-item>
                    <el-form-item label="温度坐标" :label-style="{ color: 'white' }" v-else>
                      <el-input v-model="tt" placeholder="温度坐标"></el-input>
                    </el-form-item>
                  </el-col> -->
                <!-- 最低温度 -->
                <el-col :span="24">
                  <el-form-item
                    label="最低温度°C"
                    :label-style="{ color: 'white' }"
                    v-if="tempType == 2"
                  >
                    <el-input
                      v-model="tt.min_tem"
                      placeholder="最低温度"
                      class="info-input1"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item
                    label="平均温度°C"
                    :label-style="{ color: 'white' }"
                    v-if="tempType == 2"
                  >
                    <el-input
                      v-model="tt.average_tem"
                      placeholder="最低温度"
                      class="info-input1"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <!-- 最低温度坐标 -->
                <!-- <el-col :span="24">
                    <el-form-item label="最低温度坐标" :label-style="{ color: 'white' }"  v-if="tempType ==2">
                      <el-input v-model="tt" placeholder="最低温度坐标"></el-input>
                    </el-form-item>
                  </el-col> -->
              </el-row>
            </el-form>
          </div>
          <!-- 保存按钮 -->
          <!-- <el-button class="btn1" @click="saveTEMPConfig">保存</el-button> -->
        </div>
      </div>
      <div class="content-right">
        <canvas
          ref="canvas"
          @mousedown="startDrawing"
          @mousemove="draw"
          @mouseup="stopDrawing"
          style="height: 100%;width: 100%"
        ></canvas>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { reactive, ref, defineProps, watch, computed, onMounted, inject } from 'vue'
import { insertTEMPConfig, insertTEMPConfig1 } from '/@/api/points'
// ==========================================================================红外测温=====================================================================================
const tt = ref('')
const context = ref<CanvasRenderingContext2D | null>(null)
const canvas = ref<HTMLCanvasElement | null>(null)
const send_image = ref()
const image = ref<HTMLImageElement | null>(null) // 使用 ref 来处理响应式 Image
const img_size = ref({ width: '', height: '', canvas_width: '', canvas_height: '' })
const showTempConfig = ref(false)

const props = defineProps({
  selectedImage: {
    type: Object,
    required: true,
    default: null
  },
  workspaceId: {
    type: String,
    required: true,
    default: null
  }
})

onMounted(() => {
  handleTempConfig()
})

// 监听 props.selectedImage
watch(() => props.selectedImage,
  (newVal, oldVal) => {
    handleTempConfig()
  },
  { deep: true } // deep 选项应该在对象中
)

/**
 * @description: 打开测温弹窗
 * @param {string} waylineInfo 航线信息
 * */
function handleTempConfig () {
  tt.value = ''
  showTempConfig.value = true
  context.value?.clearRect(0, 0, canvas.value?.width || 0, canvas.value?.height || 0)
  setTimeout(() => {
    drawImage(props.selectedImage.original_image_url)
  }, 1000)
}

const tempType = ref(1)
const tempTypeOption = [
  {
    label: '点测温',
    value: 1
  },
  {
    label: '区域测温',
    value: 2
  }
]

/**
 * @description: 更新测温类型
 * @param {Number} selectedValue 选中类型
 * */
function updateTempType (selectedValue) {
  isDrawing = false
  context.value.clearRect(0, 0, canvas.value.width, canvas.value.height)
  context.value?.drawImage(image.value, 0, 0, canvas.value.width, canvas.value.height) // 重绘图片
}

/**
 * @description: 提交测温配置
 * @param {number} tempType 选中类型
 * @param {number} firstPoint_x  第一个点坐标
 * @param {number} firstPoint_y  第一个点坐标
 * @param {number} secondPoint_x  第一个点坐标
 * @param {number} secondPoint_y  第一个点坐标
 * */
const points = reactive({
  firstPoint_x: 0,
  firstPoint_y: 0,
  secondPoint_x: 0,
  secondPoint_y: 0,
})
function saveTEMPConfig () {
  if (tempType.value === 2) {
    getTEMP(props.selectedImage.file_id, points.firstPoint_x, points.firstPoint_y, points.secondPoint_x, points.secondPoint_y)
  } else {
    getTEMP1(props.selectedImage.file_id, points.firstPoint_x, points.firstPoint_y)
  }
}

/**
 * @description: 图片绘制方法实现
 * */
// 计算并保存缩放比例
let scaleX = 1
let scaleY = 1
let scaleX1 = 1
let scaleY1 = 1
// 加载并绘制图片
const drawImage = (imageUrl: string) => {
  context.value = canvas.value?.getContext('2d')
  const img = new Image()
  img.onload = () => {
    send_image.value = imageUrl // 保存图片的 base64 数据或 URL
    image.value = img
    const height = 600 // 固定高度
    const width = 600 // 固定宽度
    scaleY = img.height / height
    scaleX = img.width / width
    scaleX1 = 512 / img.height
    scaleY1 = 640 / img.width
    if (canvas.value && context.value) {
      canvas.value.width = width
      canvas.value.height = height
      context.value.clearRect(0, 0, canvas.value.width, canvas.value.height) // 清空 Canvas
      context.value.drawImage(img, 0, 0, width, height) // 绘制图片
      img_size.value = {
        width: img.width.toString(),
        height: img.height.toString(),
        canvas_width: width.toString(),
        canvas_height: height.toString(),
      }
    }
  }
  img.src = imageUrl // 设置图片来源
}
/**
 * @description: 鼠标触发绘制
 * */
let isDrawing = false
let startX = 0
let startY = 0
let dianx = 0
let diany = 0
// 开始画框的函数，鼠标点击时触发，
const startDrawing = (event) => {
  if (tempType.value === 2) { // 只在区域测温模式下启用绘制
    isDrawing = true
    startX = event.offsetX
    startY = event.offsetY
    dianx = event.offsetX
    diany = event.offsetY
  } else if (tempType.value === 1) { // 在点测温模式下启用点击绘制
    // 点测温，记录点击的位置
    drawPoint(event)
  }
}

/**
 * @description: 绘制函数，绘制点和框
 * @param 鼠标对象 MouseEvent
 * */
const aa = ref('')
const result = ref()
const draw = (event: MouseEvent) => {
  if (tempType.value === 2 && isDrawing) { // 只有在区域测温时才执行绘制
    const currentX = event.offsetX
    const currentY = event.offsetY
    const width = currentX - startX
    const height = currentY - startY

    // 清空之前的绘制内容
    context.value?.clearRect(0, 0, canvas.value?.width || 0, canvas.value?.height || 0)

    // 重新绘制图片
    if (image.value) {
      context.value?.drawImage(image.value, 0, 0, canvas.value.width, canvas.value.height)
    }

    // 计算矩形的起始位置和宽高，确保坐标顺序正确
    const rectX = width < 0 ? currentX : startX
    const rectY = height < 0 ? currentY : startY
    const rectWidth = Math.abs(width)
    const rectHeight = Math.abs(height)

    // 绘制新的矩形
    context.value!.strokeStyle = 'red' // 设置边框颜色为红色
    context.value?.beginPath()
    context.value?.rect(rectX, rectY, rectWidth, rectHeight)
    context.value?.stroke()
    points.firstPoint_x = Math.round(rectX * scaleX * scaleX1)
    points.firstPoint_y = Math.round(rectY * scaleY * scaleY1)
    points.secondPoint_x = Math.round(points.firstPoint_x + rectWidth * scaleX * scaleX1)
    points.secondPoint_y = Math.round(points.firstPoint_y + rectHeight * scaleY * scaleY1)
    // points.firstPoint_x = Math.round(rectX)
    // points.firstPoint_y = Math.round(rectY)
    // points.secondPoint_x = Math.round(points.firstPoint_x + rectWidth)
    // points.secondPoint_y = Math.round(points.firstPoint_y + rectHeight)

    // insertTEMPConfig(file_path.value, left_top_x.value, left_top_y.value, right_bottom_x.value, right_bottom_y.value).then(res => {
    //   if (!res) {
    //     return
    //   }
    //   result.value = res.data
    // })
  }
}

/**
 * @description: 框测温，停止绘制框的函数，鼠标松开时触发
 * */

const stopDrawing = () => {
  if (isDrawing) {
    isDrawing = false
    console.log('Rectangular drawing stopped')

    getTEMP(props.selectedImage.file_id, points.firstPoint_x, points.firstPoint_y, points.secondPoint_x, points.secondPoint_y)
    //   if (tempType.value === 2) {
    //   getTEMP(props.selectedImage.file_id, points.firstPoint_x, points.firstPoint_y, points.secondPoint_x, points.secondPoint_y)
    // } else {
    //   getTEMP1(props.selectedImage.file_id, points.firstPoint_x, points.firstPoint_y)
    // }
  }
}

/**
 * @description: 绘制测温点
 * @param 鼠标对象 event
 * */
function drawPoint (event) {
  const pointX = event.offsetX
  const pointY = event.offsetY

  // 清空画布并重新绘制图片
  context.value?.clearRect(0, 0, canvas.value?.width || 0, canvas.value?.height || 0)
  if (image.value) {
    context.value?.drawImage(image.value, 0, 0, canvas.value.width, canvas.value.height)
  }

  // 确保 context.value 是有效的 2d 上下文对象, 绘制一个圆点，作为测温点
  if (context.value) {
    const radius = 5
    context.value.beginPath()
    context.value.arc(pointX, pointY, radius, 0, Math.PI * 2)
    context.value.fillStyle = 'red'
    context.value.fill()
  }
  points.firstPoint_x = Math.round(pointX * scaleX * scaleX1)
  points.firstPoint_y = Math.round(pointY * scaleY * scaleY1)
  // points.firstPoint_x = Math.round(pointX)
  // points.firstPoint_y = Math.round(pointY)
  getTEMP1(props.selectedImage.file_id, points.firstPoint_x, points.firstPoint_y)
}

/**
 * @description: 获取测温结果
 * @param {string} file_id 文件名字
 * @param {number} firstPoint_x  第一个点坐标
 * @param {number} firstPoint_y  第一个点坐标
 * @param {number} secondPoint_x  第二个点坐标
 * @param {number} secondPoint_y  第二个点坐标
 * @param {string} props.workspaceId  工作空间id
 * */

function getTEMP (file_id: string, firstPoint_x: number, firstPoint_y: number, secondPoint_x: number, secondPoint_y: number): Promise<string> {
  const obj = {
    left_top_x: firstPoint_x, // 可以是数字或字符串，具体根据实际需要调整
    left_top_y: firstPoint_y,
    right_bottom_x: secondPoint_x,
    right_bottom_y: secondPoint_y
  }
  return insertTEMPConfig(props.workspaceId, file_id, obj)
    .then(res => {
      // res.data.average_tem = parseFloat(res.data.average_tem.toFixed(1))
      tt.value = res.data
      // console.log('1111111111111111', tt.value)
      return res.data // 假设 res.data 是你想要的字符串
    })
}

function getTEMP1 (file_id: string, firstPoint_x: number, firstPoint_y: number) {
  const obj = {
    point_x: firstPoint_x,
    point_y: firstPoint_y,
  }
  return insertTEMPConfig1(props.workspaceId, file_id, obj)
    .then(res => {
      tt.value = res.data
      return res.data // 假设 res.data 是你想要的字符串
    })
}

/**
 * @description: 获取选中记录的最高温度
 * */
function getHighestTemp (val: any) {
  // console.log('测温结果', val)
  if (!val) {
    return ''
  }
  const Temp = '(最低温度:' + val.min_tem + ',最高温度:' + val.max_tem + ')'
  return Temp // 返回最高温度（H）
}
</script>

<style lang="scss" scoped>
.TEMPPanel {
  width: 940px;
  height: 640px;
  z-index: 3000;
  background-color: #205CA1;
  color: #fff;

  .content1 {
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #06346A;
    padding: 20px;

    .content-left {
      width: 300px;
      // background-color:#1d4292;
      height: 600px;
      border-right: 1px solid #023956;
    }

    .content-right {
      width: 600px;
      background-color: #1d4292;
      height: 600px;
      border: 3px dashed #3667A7;
    }

  }
}
</style>
