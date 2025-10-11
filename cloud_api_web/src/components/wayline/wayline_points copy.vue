<template>
  <div class="container" ref="containerRef">
    <div class="header">航线结果</div>
    <div class="main-box">
      <div class="box-right">
        <!-- 操作按钮 -->
        <div class="operation">
          <div class="item1">
            <span>当前航线名称：{{ currentRouteName }}</span>
          </div>
          <div class="item2">
            <el-button class="btn" @click="syn_video">导入关联表</el-button>
          </div>
        </div>
        <div class="tablelw1">
          <el-table :row-key="row => row.id" :data="waylineInfo"
            :header-cell-style="{ color: '#fff', fontSize: '16px', backgroundColor: '#003896', borderLeft: '0.5px #154480 solid', borderBottom: '1px #154480 solid' }"
            :cell-style="{ borderBottom: '0.5px #143275 solid', background: '#002D78', borderLeft: '0.5px #143275 solid', color: '#DCDFE5' }">
            <el-table-column label="航点序号">
              <template #default="scope">
                {{ scope.row.id}}
              </template>
            </el-table-column>
            <el-table-column label="航点序列号">
              <template #default="scope">
                {{ scope.row.photo_sequence}}
              </template>
            </el-table-column>
            <el-table-column label="航点序列号">
              <template #default="scope">
                   {{ scope.row.photo_sequence && scope.row.photo_sequence.trim() !== '' ? '拍照' : '' }}
              </template>
            </el-table-column>
            <!-- <el-table-column prop="photo_preview" label="精准复拍图片预览" /> -->
            <el-table-column prop="photo_suffix" label="图片文件后缀" />
            <el-table-column label="点位名称">
              <template #default="scope">
                   {{ scope.row.pub_wayline_point_df_entity ? scope.row.pub_wayline_point_df_entity.point_name : ''  }}
              </template>
            </el-table-column>
            <!-- <el-table-column prop="temp_reference" label="测温配置基准图" /> -->
            <el-table-column label="操作">
              <template #default="scope" >
                <div v-if="scope.row.photo_sequence && scope.row.photo_sequence.trim() !== ''">
                  <el-button size="small" link type="primary" @click="handleEditSuffix(scope.row)">编辑后缀</el-button>
                  <el-button size="small" link type="primary" @click="handleAssociatePoint(scope.row)">关联点位</el-button>
                  <div v-if="scope.row.pub_wayline_point_df_entity">
                    <el-button size="small" link type="primary" @click="handleTempConfig(scope.row)">测温配置</el-button>
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div style="margin-top: 15px">
          <!-- 分页 -->
          <el-pagination v-model:current-page="paginationProp.current" v-model:page-size="paginationProp.pageSize"
            :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
            layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
            @current-change="handleCurrentChange"></el-pagination>
        </div>
      </div>
        <!-- 测温弹窗 -->
        <div class="TEMPPanel" v-show="showTempConfig" v-drag-window>
          <div style="height: 40px; width: 100%; border-bottom: 1px solid #fff;" class="drag-title">红外测温配置</div>
          <a style="position: absolute; right: 10px; top: 10px; font-size: 16px; color: white;" @click="closeTempConfig">
            <CloseOutlined />
          </a>
          <div class="content">
            <div class="content-left">
               <input type="file" ref="fileInput" @change="uploadImgFile"/>
               <el-select  v-model="tempType" placeholder="请选择" size="large" class="select-operation" :teleported='false' @change="updateTempType">
                    <el-option v-for="item in tempTypeOption" :key="item.value" :label="item.label" :value="item.value"
                    />
                </el-select>
                <el-button @click="saveTEMPConfig">保存</el-button>
            </div>
            <div class="content-right">
              <canvas    ref="canvas"  @mousedown="startDrawing" @mousemove="draw" @mouseup="stopDrawing"  style="height: 100%;width: 100%"></canvas>
            </div>
          </div>
        </div>
         <!-- 测温弹窗 -->
        <!-- 台账弹窗 -->
        <div class="PointsPanel" v-show="showPointsConfig" v-drag-window>
          <div style="height: 40px; width: 100%; border-bottom: 1px solid #fff;" class="drag-title">点位关联</div>
          <a style="position: absolute; right: 10px; top: 10px; font-size: 16px; color: white;" @click="closePointsConfig">
            <CloseOutlined />
          </a>
          <div class="content">
            <el-tree
              style="max-width: 600px"
              :data="treeData"
              :props="defaultProps"
              @node-click="handleNodeClick"
            />
            <div style="display: flex;align-items: center;justify-content: center; margin-top: 20px;">
              <el-button @click="showPointsConfig = false" class="btn">取消</el-button>
              <el-button @click="submitPoint">确定</el-button>
            </div>
          </div>

        </div>
         <!-- 台账弹窗 -->
    </div>
  </div>

</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { CloseOutlined } from '@ant-design/icons-vue'
import { getWaypoints, getLocation, deleteWaylineFile, downloadWaylineFile, getWaylineFiles, importKmzFile, searchWaylineFiles, gethWaylineInfo, editWaylineInfo } from '/@/api/wayline'
import { getPointsBySub, updatePoints, PointDetailData } from '/@/api/points'
import { useMyStore } from '/@/store'
import { ELocalStorageKey } from '/@/types/enums'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
const store = useMyStore()
const router = useRouter()
const loading = ref(false)

// 分页属性
const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '30', '40'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 10,
  current: 1,
  total: 5
})
const height = ref(0)
const containerRef = ref<HTMLElement | null>(null)

// 页面加载时动态计算滚动区域高度
onMounted(() => {
  if (containerRef.value) {
    height.value = window.innerHeight - containerRef.value.offsetHeight
  }
  getWaylineInfo()
})

/**
 * @description: 查询航线信息
 * @param {string} wayineId 航线id
 * @param {string} workspaceId 工作空间id
 * */
const waylineId = computed(() => {
  return store?.state?.waylineData?.id ?? null // 如果 store 或 waylineData 为空，返回 null
})
const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
const waylineInfo = ref([])
function getWaylineInfo () {
  const targetValue = waylineId.value

  // 判断 Proxy 的目标对象是否为空
  if (targetValue && Object.keys(targetValue).length === 0) {
    router.push({ path: '/wayline-panel' })
    return
  }

  getWaypoints(workspaceId, waylineId.value).then(res => {
    if (res.code !== 0) {
      return
    }
    waylineInfo.value = res.data
  })
}

/**
 * @description: 解析航线信息
 * @param {string} waylineInfo 航线信息
 * */
const waypointData = ref([])
const waypointNum = ref(0)
function analyzeWayline () {
  if (waylineInfo.value) {
    waypointNum.value = waylineInfo.value?.folder.placeMarks.length
    const points = waylineInfo.value?.folder.placeMarks
  }
}

/**
 * @description: 刷新数据信息
 * @param {string} waylineId 航线id
 * */
function refresh () {
  getWaylineInfo()
  // waylineInfo.value[selectedPointData.value.id].waypoint_name = '航点' + selectedPointData.value.id
  // console.log('更新后的数据', waylineInfo.value)
}
// -------------------------------------------------------------------------台账点位树形图-------------------------------------------------------------------------------------------------
const showPointsConfig = ref(false)
const selectedPointData = ref(null)
function handleAssociatePoint (val:any) {
  showPointsConfig.value = true
  selectedPointData.value = val
  getPoints()
}
function closePointsConfig () {
  showPointsConfig.value = false
}

/**
 * @description: 提交关联的节点
 * */
function submitPoint () {
  showPointsConfig.value = false
  const waypoint_name = '航点' + selectedPointData.value.id
  WaypointToPoint(selectedPointData.value.photo_sequence, waypoint_name)
}

interface TreeNode {
  label: string;
  key: string;
  children?: TreeNode[];
}

/**
 * @description: 叶子节点点击事件
 * */
const selectedPointId = ref('')
const handleNodeClick = (data: TreeNode) => {
  // 点击五级节点时输出该节点的ID
  // console.log(data.key) // 打印点击的节点的key (即id)
  selectedPointId.value = data.key
}

/**
 * @description: 数据转化,后端数据转树形数据
 * @param {array} rawData 后端数据
 * */
const transformData = (rawData: any[]): TreeNode[] => {
  // 使用Map来存储已处理的一级节点（area_name），避免重复
  const areaMap = new Map<string, TreeNode>()

  rawData.forEach(item => {
    // 检查该一级节点（area_name）是否已存在
    let areaNode = areaMap.get(item.area_name)
    // 如果该一级节点不存在，创建并加入
    if (!areaNode) {
      areaNode = {
        label: item.area_name,
        key: item.area_id,
        children: []
      }
      areaMap.set(item.area_name, areaNode)
    }

    // 处理第二级节点（bay_name）
    let bayNode = areaNode.children!.find(child => child.label === item.bay_name)
    if (!bayNode) {
      bayNode = {
        label: item.bay_name,
        key: item.bay_id,
        children: []
      }
      areaNode.children!.push(bayNode)
    }

    // 处理第三级节点（device_name）
    let deviceNode = bayNode.children!.find(child => child.label === item.device_name)
    if (!deviceNode) {
      deviceNode = {
        label: item.device_name,
        key: item.device_id,
        children: []
      }
      bayNode.children!.push(deviceNode)
    }

    // 处理第四级节点（component_name）
    let componentNode = deviceNode.children!.find(child => child.label === item.component_name)
    if (!componentNode) {
      componentNode = {
        label: item.component_name,
        key: item.component_id,
        children: []
      }
      deviceNode.children!.push(componentNode)
    }

    // 叶子节点（point_name）直接添加，并存储point_analyse_type
    const pointNode = {
      label: item.point_name,
      key: item.id, // 使用point_analyse_type作为唯一key
      // point_analyse_type: item.point_analyse_type // 保存分析类型
    }

    componentNode.children!.push(pointNode)
  })

  // 将Map中的所有一级节点返回
  return Array.from(areaMap.values())
}

/**
 * @description: 查询台账信息
 * @param {string} sub_code 场站编码
 * */
const treeData = ref([])
const pointsData = ref([])
const sub_code = 'faf3362c-3c90-2fce-0f88-b059716cb160'
function getPoints () {
  getPointsBySub(sub_code).then(res => {
    if (res.code !== 0) {
      return
    }
    // console.log(res.data)
    pointsData.value = res.data
    treeData.value = transformData(res.data)
  })
}

/**
 * @description: 根据航线id获取场站编码
 * @param {string} sub_code 场站编码
 * */
// function getPoints () {
//   getPointsBySub(sub_code).then(res => {
//     if (res.code !== 0) {
//       return
//     }
//     treeData.value = transformData(res.data)
//   })
// }
/**
 * @description: 航点信息和台账关联(编辑台账)
 * @param {PointDetailData} pointData 台账数据
 * */
// const pointData
function WaypointToPoint (waypoint_sequence:string, wayline_name:string) {
  nextTick(() => {
    // 整合点位数据
    const index = pointsData.value.findIndex(item => item.id === selectedPointId.value)
    if (index !== -1) {
      pointsData.value[index].wayline_id = waylineId.value
      pointsData.value[index].waypoint_sequence = waypoint_sequence
      pointsData.value[index].waypoint_name = wayline_name
      // 将更新后的记录赋值给record
      const pointData = pointsData.value[index]

      updatePoints(pointData).then(res => {
        if (res.code !== 0) {
          return
        }
        message.success('关联成功')
        refresh()
      })
    }
  })
}

/**
 * @description: 添加测温配置
 * @param {PointDetailData} pointData 台账数据
 * */
function insertTEMPConfig () {
  nextTick(() => {
    if (selectedWaypoint.value.pub_wayline_point_df_entity) {
      // 将更新后的记录赋值给record
      const pointData = selectedWaypoint.value.pub_wayline_point_df_entity
      pointData.tem_type = tempType.value
      pointData.tem_conf = JSON.stringify(points)
      updatePoints(pointData).then(res => {
        if (res.code !== 0) {
          return
        }
        console.log('插入测温配置成功!')
      })
    }
  })
}

interface TreeNode {
  label: string;
  key: string;
  children?: TreeNode[];
  id?: number; // 叶子节点特有的属性
}

// ------------------------------------------------------------------------红外测温------------------------------------------------------------------------------------------------
/**
 * @description: 打开测温弹窗
 * @param {string} waylineInfo 航线信息
 * */
const context = ref<CanvasRenderingContext2D | null>(null)
const canvas = ref<HTMLCanvasElement | null>(null)
const send_image = ref()
const image = ref<HTMLImageElement | null>(null) // 使用 ref 来处理响应式 Image
const img_size = ref({ width: '', height: '', canvas_width: '', canvas_height: '' })
const showTempConfig = ref(false)
const selectedWaypoint = ref(null)
function handleTempConfig (val:any) {
  showTempConfig.value = true
  selectedWaypoint.value = val
  context.value?.clearRect(0, 0, canvas.value?.width || 0, canvas.value?.height || 0)
}

/**
 * @description: 关闭测温弹窗
 * */
function closeTempConfig () {
  showTempConfig.value = false
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
  showTempConfig.value = false
  insertTEMPConfig()
}

/**
 * @description: 调用上传图片
 * */
const uploadImgFile = (event: any) => {
  const file = event.target.files[0]
  if (file) {
    drawImage(file) // 调用绘制图片的方法
  } else {
    console.error('请选择文件')
  }
}

/**
 * @description: 图片绘制方法实现
 * */
// 计算并保存缩放比例
let scaleX = 1
let scaleY = 1
const drawImage = (file: any) => {
  const reader = new FileReader()
  const img = new Image()
  context.value = canvas.value?.getContext('2d')
  reader.onload = () => {
    if (canvas.value && context.value) {
      img.onload = () => {
        send_image.value = reader.result as string // 保存图片的 base64 数据
        image.value = img
        const height = 600
        const width = 600
        scaleY = img.height / height
        scaleX = img.width / width
        canvas.value.width = width
        canvas.value.height = height
        context.value.clearRect(0, 0, canvas.value.width, canvas.value.height) // 清空 Canvas
        context.value.drawImage(img, 0, 0, width, height)
        img_size.value = {
          width: img.width.toString(),
          height: img.height.toString(),
          canvas_width: width.toString(),
          canvas_height: height.toString(),
        }
      }
      img.src = reader.result as string
    }
  }
  // 将文件读取为 base64 编码
  reader.readAsDataURL(file)
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
    points.firstPoint_x = Math.round(rectX * scaleX)
    points.firstPoint_x = Math.round(rectY * scaleY)
    points.secondPoint_x = Math.round(points.firstPoint_x + rectWidth * scaleX)
    points.secondPoint_x = Math.round(points.firstPoint_x + rectHeight * scaleY)

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
  points.firstPoint_x = Math.round(pointX * scaleX)
  points.firstPoint_x = Math.round(pointY * scaleY)
}
// --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
// 刷新数据函数，根据分页属性刷新数据
function refreshData (page: any) {
  loading.value = true
  setTimeout(() => {
    paginationProp.total = 50 // 假设总共有 50 条数据
    loading.value = false
  }, 1000)
}

// 分页事件：页码改变时
function handleCurrentChange (val: number) {
  paginationProp.current = val
  refreshData(paginationProp)
}

// 分页事件：每页显示条数改变时
function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  refreshData(paginationProp)
}

</script>

<style lang="scss" scoped>
.container {
  // height: 100%;
  width: 100%;
  padding: 10px;
  display: flex;
  flex-direction: column;
  /* 垂直排列 */

}

.main-box {
  display: flex;
  /* 使用 flexbox 布局 */
  height: 100vh;
  /* 设置容器高度为视口高度 */
}

// .box-left:hover {
//   box-shadow: inset 0px 0px 20px 3px rgba(34, 135, 255, 0.7);
// }
.box-right {
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  flex: 1;
  /* 右侧占据剩余空间 */
  width: calc(80% - 10px);
  margin-left: 10px;
  // background-color: #2196F3; /* 蓝色背景 */
  padding: 20px;
  /* 内边距 */
  color: white;
  /* 字体颜色 */
  border-radius: 15px;
  border: none;
  height: 85vh;
}
.TEMPPanel{
  padding: 10px 0 0 0;
  position: absolute;
  z-index: 1;
  left: 0;
  top: 200px;
  margin-left: 345px;
  width: 940px;
  height:690px;
  // background: #232323;
  // background: rgba(59, 116, 255, 0.2);
  background-color:#205CA1;
  color: #fff;
  .content{
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #06346A;
    padding:20px ;
    .content-left{
      width: 300px;
      // background-color:#1d4292;
      height: 600px;
      border-right: 1px solid #023956;
    }
    .content-right{
      width: 600px;
      background-color:#1d4292;
      height: 600px;
      border: 3px dashed #3667A7;
    }

  }
}
.PointsPanel{
  padding: 10px 0 0 0;
  position: absolute;
  z-index: 1;
  left: 0;
  top: 200px;
  margin-left: 345px;
  width: 500px;
  height:600px;
  // background: #232323;
  // background: rgba(59, 116, 255, 0.2);
  background-color:#205CA1;
  color: #fff;
  .content{
    // display: flex;
    // align-items: center;
    // justify-content: center;
    background-color: #06346A;
    padding:20px ;
    height: 100%;
  }
}
// 头部  标题 面包屑
.header {
  width: 100%;
  height: 60px;
  background: #05204B;
  padding: 16px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color: aliceblue;
}

// 操作部分
.operation {
  height: fit-content;
  padding: 15px;
  display: flex;
  justify-content: flex-end;
  color: rgba(255, 255, 255, 0.762);
  background-color: rgba(0, 112, 209, 0.2);
  font-size: 16px;
  border: 4px solid rgba(0, 112, 209, 1);
  border-bottom: none;
  border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;

  .item1 {
   flex: 1;
  }
  .item2 {
    display: flex;
    justify-items: center;
    align-items: center;
  }
}

.btn {
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color: rgba(255, 255, 255, 0.762);
}

:deep(.btn1) {
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color: rgba(255, 255, 255, 0.762);
}

// 输入框
:deep(.el-input) {
  --el-input-border-color: #1d4292;
}

:deep(.el-input__wrapper) {
  background-color: #0B2756;
}

:deep(.el-select__wrapper) {
  background-color: #0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: aliceblue;
}

.tablelw1 {
  // margin: 0;
  // padding: 16px;
  max-height: 600px;
  overflow-y: auto
}

// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #0A2D63;
}

// 表格最后一条白线
:deep .el-table__inner-wrapper::before {
  height: 0;
}

//分页数据
/* 修改前后箭头未点击时的背景颜色 */
:deep .el-pagination .btn-prev,
:deep .el-pagination .btn-next {
  background-color: #062254 !important;
  color: #fff;
}

/* 修改未点击时的数字方块背景颜色 */
:deep .el-pagination .el-pager li:not(.active):not(.disabled):hover {
  background-color: #2264a7 !important;
}

/* 未点击时的数字方块背景颜色 */
:deep .el-pagination .el-pager li:not(.active):not(.disabled) {
  background-color: #062254 !important;
  color: #fff;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #124AAD !important; //修改默认的背景色
  color: #fff;
}

::v-deep el-pager {
  background-color: #0B2756;
}
</style>
