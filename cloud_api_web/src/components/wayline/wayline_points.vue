<template>
  <div class="container">
    <!-- <div class="operation">
    </div> -->
    <div class="operation">
      <span class="label">当前航线名称：{{ currentRouteName }}</span>
      <el-button class="new_btn" type="primary" >
        <span class="btn_text">导入关联表</span>
      </el-button>
    </div>
    <div class="content1">
      <div class="table-container">
          <el-table  :data="paginatedData" stripe>
            <el-table-column label="航点序号" width="100">
              <template #default="scope">
                {{ scope.row.id}}
              </template>
            </el-table-column>
            <el-table-column label="航点序列号">
              <template #default="scope">
                {{ scope.row.photo_sequence}}
              </template>
            </el-table-column>
            <el-table-column label="是否拍照">
              <template #default="scope">
                   {{ scope.row.photo_sequence && scope.row.photo_sequence.trim() !== '' ? '是' : '' }}
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
                  <el-button size="small" type="primary" class="custom-execute-btn" @click="handleEditSuffix(scope.row)">编辑后缀</el-button>
                  <el-button size="small" type="primary" class="custom-execute-btn" @click="handleAssociatePoint(scope.row)">关联点位</el-button>
                  <div v-if="scope.row.pub_wayline_point_df_entity">
                    <el-button size="small" type="primary" class="custom-execute-btn" @click="handleTempConfig(scope.row)">测温配置</el-button>
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      <div class="pagination-container">
        <!-- 分页 -->
        <!-- <el-pagination v-model:current-page="paginationProp.current" v-model:page-size="paginationProp.pageSize"
          :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
          layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handleCurrentChange"></el-pagination> -->
          <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
                    :current-page="currentPage" :page-sizes="[5, 10, 20, 40]" :page-size="pageSize" :total="totalItems" layout="total, sizes, prev, pager, next, jumper">
             </el-pagination>
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
        <!-- <el-tree
          style="max-width: 600px"
          :data="treeData"
          :props="defaultProps"
          @node-click="handleNodeClick"
        /> -->
        <el-tree
          ref="selectTree"
          style="max-width: 600px"
          class="organization_configuration"
          :props="props"
          :load="loadNode"
          @check="handleTreeNodeClick"
          :highlight-current="true"
          node-key="key"
          lazy
          :check-on-click-node="true"
          :show-checkbox="false"
          :check-strictly="true"
        />
        <div style="display: flex;align-items: center;justify-content: center; margin-top: 20px;">
          <el-button @click="showPointsConfig = false" class="btn">取消</el-button>
          <el-button @click="submitPoint">确定</el-button>
        </div>
      </div>

    </div>
  </div>

</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { CloseOutlined } from '@ant-design/icons-vue'
import { getWaypoints, getLocation, deleteWaylineFile, downloadWaylineFile, getWaylineFiles, importKmzFile, searchWaylineFiles, gethWaylineInfo, editWaylineInfo } from '/@/api/wayline'
import { getPointsBySub, updatePoints, PointDetailData, getAreaBySub, getBayByAreaId, getDeviceByBayId, getComponentByDeviceId, getPointsByComponentId } from '/@/api/points'
import { useMyStore } from '/@/store'
import { ELocalStorageKey } from '/@/types/enums'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type Node from 'element-plus/es/components/tree/src/model/node'
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

// 页面加载时动态计算滚动区域高度
onMounted(() => {
  setTimeout(() => {
    getWaylineInfo()
  }, 500)
})

/**
 * @description: 查询航线信息
 * @param {string} wayineId 航线id
 * @param {string} workspaceId 工作空间id
 * */
// const waylineId = computed(() => {
//   return store?.state?.waylineData?.id ?? null // 如果 store 或 waylineData 为空，返回 null
// })
const waylineId = ref(JSON.parse(localStorage.getItem('waylineId')))
const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
const waylineInfo = ref([])
const currentRouteName = ref('')
function getWaylineInfo () {
  const targetValue = waylineId.value

  // 判断 Proxy 的目标对象是否为空
  if (targetValue && Object.keys(targetValue).length === 0) {
    router.push({ path: '/wayline-panel' })
    return
  }
  currentRouteName.value = waylineId.value.name
  getWaypoints(workspaceId, waylineId.value.id).then(res => {
    if (res.code !== 0) {
      return
    }
    waylineInfo.value = res.data
    console.log('sasa', waylineInfo.value)
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
  // getPoints()
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

interface Tree {
  name: string
  key: string
  leaf?: boolean
}

const props = {
  label: 'name',
  children: 'zones',
  isLeaf: 'leaf',
}

const loadNode = async (node: Node, resolve: (data: Tree[]) => void) => {
  if (node.level === 0) {
    try {
      // 获取异步数据
      const areaData = await getAreas()
      resolve(areaData)
    } catch (error) {
      console.error('获取数据失败:', error)
      resolve([]) // 获取失败时返回空数组
    }
    return
  }
  if (node.level > 4) return resolve([])
  if (node.level === 1) {
    // 请求数据
    try {
      // 获取异步数据
      const bayData = await getBayInfo(node.data.key)
      resolve(bayData)
    } catch (error) {
      console.error('获取数据失败:', error)
      resolve([]) // 获取失败时返回空数组
    }
  }
  if (node.level === 2) {
    // 请求数据
    try {
      // 获取异步数据
      const deviceData = await getDeviceInfo(node.data.key)
      resolve(deviceData)
    } catch (error) {
      console.error('获取数据失败:', error)
      resolve([]) // 获取失败时返回空数组
    }
  }
  if (node.level === 3) {
    // 请求数据
    try {
      // 获取异步数据
      const componentData = await getComponentInfo(node.data.key)
      resolve(componentData)
    } catch (error) {
      console.error('获取数据失败:', error)
      resolve([]) // 获取失败时返回空数组
    }
  }
  if (node.level === 4) {
    // 请求数据
    try {
      // 获取异步数据
      const pointData = await getPointsInfo(node.data.key)
      resolve(pointData)
    } catch (error) {
      console.error('获取数据失败:', error)
      resolve([]) // 获取失败时返回空数组
    }
  }
}

// 树ref
const selectTree = ref()
const selectedPointId = ref()
// 树节点选中事件
const handleTreeNodeClick = (data, checkObj) => {
  // 共两个参数，依次为：传递给 data 属性的数组中该节点所对应的对象、树目前的选中状态对象
  // 树目前的选中状态对象，包含 checkedNodes、checkedKeys、halfCheckedNodes、halfCheckedKeys 四个属性

  if (checkObj.checkedKeys.length !== 0) {
    if (checkObj.checkedKeys.length === 2) {
      // 如果选择超过一个节点，则只保留最后一个节点
      // 单选实现
      selectTree.value.setCheckedKeys([data.key])
      if (data.isLeaf) {
        selectedPointId.value = data.key
      }
    }
  }
}
// const handleCheckChange = (
//   data: Tree,
//   checked: boolean,
//   indeterminate: boolean
// ) => {
//   console.log(data, checked, indeterminate)
// }

interface TreeNode {
  label: string;
  key: string;
  children?: TreeNode[];
}

/**
 * @description: 查询台账信息
 * @param {string} sub_code 场站编码
 * */
const treeData = ref([])
const pointsData = ref([])
const sub_code = 'faf3362c-3c90-2fce-0f88-b059716cb160'
function getAreas () {
  return new Promise((resolve, reject) => {
    getAreaBySub(sub_code).then(res => {
      if (res.code !== 0) {
        reject(new Error('请求失败')) // 使用 Error 对象来抛出错误
        return
      }
      const output = res.data.map(item => ({
        name: item.area_name,
        key: item.area_id
      }))
      resolve(output) // 返回获取到的数据
    }).catch(error => {
      reject(error) // 捕获并传递任何错误
    })
  })
}

function getBayInfo (area_id:string) {
  return new Promise((resolve, reject) => {
    getBayByAreaId(area_id).then(res => {
      if (res.code !== 0) {
        reject(new Error('请求失败')) // 使用 Error 对象来抛出错误
        return
      }
      const output = res.data.map(item => ({
        name: item.bay_name,
        key: item.bay_id
      }))
      resolve(output) // 返回获取到的数据
    }).catch(error => {
      reject(error) // 捕获并传递任何错误
    })
  })
}

function getDeviceInfo (bay_id:string) {
  return new Promise((resolve, reject) => {
    getDeviceByBayId(bay_id).then(res => {
      if (res.code !== 0) {
        reject(new Error('请求失败')) // 使用 Error 对象来抛出错误
        return
      }
      const output = res.data.map(item => ({
        name: item.device_name,
        key: item.device_id
      }))
      resolve(output) // 返回获取到的数据
    }).catch(error => {
      reject(error) // 捕获并传递任何错误
    })
  })
}

function getComponentInfo (device_id:string) {
  return new Promise((resolve, reject) => {
    getComponentByDeviceId(device_id).then(res => {
      if (res.code !== 0) {
        reject(new Error('请求失败')) // 使用 Error 对象来抛出错误
        return
      }
      const output = res.data.map(item => ({
        name: item.component_name,
        key: item.component_id
      }))
      resolve(output) // 返回获取到的数据
    }).catch(error => {
      reject(error) // 捕获并传递任何错误
    })
  })
}

function getPointsInfo (component_id:string) {
  return new Promise((resolve, reject) => {
    getPointsByComponentId(component_id).then(res => {
      if (res.code !== 0) {
        reject(new Error('请求失败')) // 使用 Error 对象来抛出错误
        return
      }
      const output = res.data.map(item => ({
        name: item.point_name,
        key: item.point_code,
        isLeaf: true
      }))
      pointsData.value = res.data
      resolve(output) // 返回获取到的数据
    }).catch(error => {
      reject(error) // 捕获并传递任何错误
    })
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
    const index = pointsData.value.findIndex(item => item.point_code === selectedPointId.value)
    if (index !== -1) {
      pointsData.value[index].wayline_id = waylineId.value.id
      pointsData.value[index].waypoint_sequence = waypoint_sequence.split('_')[0]
      pointsData.value[index].waypoint_name = wayline_name
      // 将更新后的记录赋值给record
      const pointData = pointsData.value[index]
      console.log('关联的点位', pointData)
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
// ===========================================================前端分页功能实现==================================================
const currentPage = ref(1)
const pageSize = ref(5)
const totalItems = computed(() => waylineInfo.value.length)
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return waylineInfo.value.slice(start, end)
})
const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
}
const handleSizeChange = (newSize) => {
  pageSize.value = newSize
}
// ==============================================================================================
// // 分页事件：页码改变时
// function handleCurrentChange (val: number) {
//   paginationProp.current = val
//   refreshData(paginationProp)
// }

// // 分页事件：每页显示条数改变时
// function handleSizeChange (val: number) {
//   paginationProp.pageSize = val
//   refreshData(paginationProp)
// }

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
::v-deep .el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content {
  background-color: rgba(14, 63, 83, 0.764) !important;
  color: #409eff !important;
  font-weight: bold !important;
}

::v-deep .el-tree-node__content {
  background-color: rgba(0,23,59,0.75) !important;
  color: #fff !important;
  font-weight: bold !important;
}
::v-deep .el-tree{
  height: 400px;
  background-color: rgba(0,23,59,0.75);
  overflow-y: auto;
}
/* 公共按钮样式 */
.custom-btn {
  background-color: rgba(51, 122, 255, 0.12);
  height: 28px;
  border: 1px solid rgba(0, 64, 147, 1);
  margin: 7px;
}

/* 立即执行按钮 */
.custom-execute-btn {
  @extend .custom-btn;
  width: 76px;
  margin-left: 7px;
}
.table-container {
  flex-grow: 1;
  overflow: hidden;
  // height: 500px;
  overflow-y: auto;
}
// .box-left:hover {
//   box-shadow: inset 0px 0px 20px 3px rgba(34, 135, 255, 0.7);
// }

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
  height:550px;

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
    height: 500px;
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
// 操作部分
.operation {
  display: flex;
  // justify-items: center; /* 这里可能是错误的，flexbox 中应该使用 justify-content */
  align-items: center;
  /* 这个会确保 label 在垂直方向居中 */
  background-color: rgba(1, 36, 98, 1);
  border-radius: 4px;
  // width: 100%;
  height: 60px;
  margin: 31px 12px 0 12px;

  .label {
    height: 60px;
    display: flex;
    /* 这个可以保留，确保子元素居中 */
    align-items: center;
    /* 垂直居中 */
    justify-content: center;
    /* 水平居中 */
    color: rgba(255, 255, 255, 1);
    font-size: 14px;
    font-family: Google Sans-Medium;
    font-weight: 500;
    margin: 0 10px 0 30px;
  }

  .new_btn {
    background-image: linear-gradient(180deg,
        rgba(70, 145, 217, 1) 0,
        rgba(21, 81, 181, 1) 100%);
    border-radius: 4px;
    width: 108px;
    height: 37px;

    // margin: 12px 0 0 30px;
    .thumbnail_1 {
      width: 12px;
      height: 12px;
      margin: 5px 0 0 12px;
    }

    .btn_text {
      width: 56px;
      height: 18px;
      overflow-wrap: break-word;
      color: rgba(255, 255, 255, 1);
      font-size: 14px;
      font-family: Google Sans-Medium;
      font-weight: 500;
      text-align: left;
      white-space: nowrap;
      line-height: 14px;
      margin: 9px 20px 0 8px;
    }

  }

  .new_btn1 {
    background-image: linear-gradient(180deg,
        rgba(248, 212, 94, 1) 0,
        rgba(227, 157, 6, 1) 100%);
    border-radius: 4px;
    width: 70px;
    height: 37px;

    // margin: 12px 0 0 30px;
    .thumbnail_1 {
      width: 12px;
      height: 12px;
      margin: 5px 0 0 12px;
    }

    .btn_text {
      width: 30px;
      height: 18px;
      overflow-wrap: break-word;
      color: rgba(255, 255, 255, 1);
      font-size: 14px;
      font-family: Google Sans-Medium;
      font-weight: 500;
      text-align: left;
      white-space: nowrap;
      line-height: 14px;
      margin: 9px 20px 0 8px;
    }

  }

}

.content1 {
  margin: 15px 12px 0 12px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

.pagination-container {
  position: absolute;
  bottom: 40px;
  /* 距离底部的距离，可调整 */
  left: 50%;
  /* 距离右边的距离，可调整 */
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  align-items: center;
}
::v-deep .el-table tr {
  background-color: #011C4B !important;
  /* opacity: 0.6; */
  color: #F1F6FF;
  font-weight: bold;
}

// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #2264a7;
}

/* // 修改高亮当前行颜色 */
::v-deep .el-table tbody tr:hover>td {
  background: rgba(0, 114, 245, 0.6) !important;
}

/* // 斑马线颜色 */

::v-deep .el-table--striped .el-table__body tr.el-table__row--striped td {
  background: rgba(0, 45, 120, 1);
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

/* 移除表格行的底部边框 */
::v-deep .el-table__body-wrapper .el-table__row {
  border-bottom: none !important;
  /* 或者： */
  box-shadow: none !important;
}

/* 如果仍然看到白色线条，可能是阴影效果的原因 */
::v-deep .el-table__body-wrapper .el-table__row td {
  border-bottom: none !important;
}
// 表格最后一条白线
:deep .el-table__inner-wrapper::before {
  height: 0;
}
::v-deep .el-table td
 {
  border: 2px solid #01123288; /* 设置列的边框颜色和粗细 */
  font-size: 16px;
  font-weight: 500;
}

// 表格样式
::v-deep .el-table{
   .cell {
    // text-align: center;
    display: flex;
    justify-content: center;
    }
}
// // 表头大小
::v-deep .el-table th {
  height: 50px;
  font-size: 16px !important; /* 如果你需要修改表头字体大小，设置一个不同的大小 */
  color: rgba(255, 255, 255, 1);
  background-color: #00399A;
  border-left: 2px #01123288 solid;
  border-bottom: 2px #01123288 solid !important;
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
