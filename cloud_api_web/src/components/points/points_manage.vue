<template>
  <div class="container">
    <!-- <div class="header">点位台账管理</div> -->
    <div class="operation">
      <!-- 变电站选择框 -->
      <div style="margin-left: 10px;">
        <el-select v-model="selectedSubstation" placeholder="选择变电站名称" class="select-operation" :teleported='false'
          style="width: 200px;">
          <el-option v-for="item in subOption" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>

      <!-- 设备类型选择框 -->
      <el-select v-model="selectedEquipType" placeholder="选择设备类型" class="select-operation" :teleported='false'
        style="width: 200px;">
        <el-option label="所有" value="所有"></el-option>
        <el-option label="风机" value="风机"></el-option>
        <el-option label="水箱" value="水箱"></el-option>
        <el-option label="篮球板" value="篮球板"></el-option>
      </el-select>

      <!-- 识别类型选择框 -->
      <el-select v-model="selectedIdentType" placeholder="选择识别类型" class="select-operation" :teleported='false'
        style="width: 200px;">
        <el-option label="识别类型1" value="识别类型1"></el-option>
        <el-option label="识别类型2" value="识别类型2"></el-option>
        <el-option label="识别类型3" value="识别类型3"></el-option>
      </el-select>

      <!-- 搜索框：点位名称 -->
      <el-input v-model="searchPoint" placeholder="搜索点位名称" class="custom-input"
        style="width: 200px; height:30px"></el-input>

      <!-- 查询按钮 -->
      <el-button class="new_btn iconfont icon-chaxunhangxian" type="primary" style="margin-left: 30px; width: 70px;"
        @click="queryPoint">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/search.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">查询</span>
      </el-button>
      <!-- 重置按钮 -->
      <el-button class="new_btn1 iconfont icon-zhongzhi-" type="primary" style="margin-left: 10px" @click="resetPoint">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/refresh.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">重置</span>
      </el-button>

      <!-- 导出按钮 -->
      <!-- <el-button class="btn" type="primary" style="margin-left: 10px" @click="exportPoint">导出</el-button> -->
      <!-- <el-button class="new_btn" type="primary" style="margin-left: 10px; width: 70px;" @click="exportPoint">
        <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/search.png" />
        <span class="btn_text" style="width: 30px;">导出</span>
      </el-button> -->
      <el-button class="new_btn2 iconfont icon-daoru" type="primary" style="margin-left: 10px; width: 150px;"
        @click="impPointLedger = true">
        <!-- <img class="thumbnail_2" referrerpolicy="no-referrer" src="../../assets/v4/import.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">导入点位台账</span>
      </el-button>
      <el-button class="new_btn2 iconfont icon-daochu" type="primary" style="margin-left: 10px; width: 150px;"
        @click="expModelFile">
        <!-- <img class="thumbnail_2" referrerpolicy="no-referrer" src="../../assets/v4/export.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">导出建模文件</span>
      </el-button>
    </div>
    8
    <div class="item1">
      <!-- <el-button class="btn" @click="impPointLedger = true">导入点位台账</el-button> -->
      <!-- 导入弹窗 -->
      <!-- 导入点位台账的弹窗 -->
      <el-dialog v-model="impPointLedger" title="导入点位台账" width="20%">
        <div style="display: flex; flex-direction: row; align-items: center; margin: 15px 0">
          <div style="margin-right: 10px; color: #fff">选择场站:</div>
          <div>
            <el-select v-model="current_sub" placeholder="选择识别类型" class="select-operation" :teleported='false'
              style="width: 200px;">
              <el-option v-for="item in subOption" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
        </div>
        <div style="color: #fff;">
          选择点位台账:
        </div>
        <el-upload action="" :before-upload="beforeUploadExcel" accept=".xlsx, .xls" drag :on-change="handleFileChange">
          <i class="el-icon-upload"></i>
          <div class="el-upload__text" style="color: #fff;">将文件拖到此处，或<em>点击上传</em></div>
          <template v-slot:tip>
            <div style="color: #A3A5A8;">只支持Excel文件（.xlsx, .xls）</div>
          </template>
        </el-upload>
        <!-- 文件名称显示 -->
        <div v-if="selectedFile" style="margin-top: 10px; color: #606266;">
          <span style="color: white;">已选择文件: {{ selectedFile.name }}</span>
        </div>
        <template v-slot:footer>
          <span class="dialog-footer">
            <el-button @click="impPointLedger = false" class="nobtn">取 消</el-button>
            <el-button type="primary" @click="confirmImport" :disabled="!selectedFile" class="okbtn">确定</el-button>
          </span>
        </template>
      </el-dialog>
      <!-- <el-button class="btn" type="primary" style="margin-left: 10px" @click="expModelFile">导出建模文件:disabled="!selectedFile"</el-button> -->
      <div style="padding-left: 10px;" v-if="importVisible">
        <el-button class="btn1" @click="openWaylineDialog">导入航线 </el-button>
      </div>
    </div>

    <div class="content">
      <div class="table-container">
        <el-table :data="tableData" stripe>
          <el-table-column type="selection" width="55" />
          <el-table-column label="序号" align='center' width="60">
            <template #default="scope">
              {{ scope.$index + (paginationProp.current - 1) * paginationProp.pageSize + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="点位名称">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.point_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="区域/间隔">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.bay_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="设备名称">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.device_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="设备类型">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.device_type }}</div>
            </template>
          </el-table-column>
          <el-table-column label="所属部位">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.component_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="识别类型">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.point_analyse_type }}</div>
            </template>
          </el-table-column>
          <el-table-column label="关联航线航点">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.waypoint_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="scope">
              <div class="action-buttons">
                <el-button size="small" type="warning" class="edit-btn" @click="editRow(scope.row)">编辑</el-button>
                <el-popconfirm width="220" confirm-button-text="确定" cancel-button-text="不，谢谢" icon-color="#626AEF"
                  title="点位记录一旦删除就无法恢复,是否继续？" @confirm="deleteWayline(scope.row.id)">
                  <template #reference>
                    <el-button size="small" type="danger" class="delete-btn">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
    <!-- </div> -->
    <div class="pagination-container">
      <!-- 分页 -->
      <div style="margin-top: 15px">
        <el-pagination v-model:current-page="paginationProp.current" v-model:page-size="paginationProp.pageSize"
          :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
          layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handleCurrentChange"></el-pagination>
        <!-- <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
                :current-page="currentPage" :page-sizes="[5, 10, 20, 40]" :page-size="pageSize" :total="totalItems" layout="total, sizes, prev, pager, next, jumper">
          </el-pagination> -->
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑点位" width="20%" style="background-color: #0A2D63; color: white">
      <el-form :model="editForm" label-width="120px">
        <template #header>
          <span style="color: white; font-size: 20px;">编辑点位</span>
        </template>
        <el-form-item label="点位名称" required>
          <el-input v-model="editForm.point_name" placeholder="请输入点位名称"></el-input>
        </el-form-item>
        <el-form-item label="区域名称">
          <el-input v-model="editForm.area_name" placeholder="请输入区域名称"></el-input>
        </el-form-item>
        <el-form-item label="间隔名称">
          <el-input v-model="editForm.bay_name" placeholder="请输入间隔名称"></el-input>
        </el-form-item>
        <el-form-item label="设备名称">
          <el-input v-model="editForm.device_name" placeholder="请输入设备名称"></el-input>
        </el-form-item>
        <el-form-item label="所属部位">
          <el-input v-model="editForm.component_name" placeholder="请输入所属部位"></el-input>
        </el-form-item>
        <el-form-item label="设备类型">
          <el-input v-model="editForm.device_type" placeholder="请输入设备类型"></el-input>
        </el-form-item>
        <el-form-item label="识别类型">
          <el-input v-model="editForm.point_analyse_type" placeholder="请输入设备名称"></el-input>
        </el-form-item>
      </el-form>
      <template v-slot:footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false" class="nobtn">取 消</el-button>
          <el-button type="primary" @click="saveEdit" class="okbtn">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { TableState } from 'ant-design-vue/lib/table/interface'
import { IPage } from '/@/api/http/type'
import { ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElUpload, ElMessage } from 'element-plus'
import CustomTree from '/@/components/substationTree.vue'
import { insertPointsByXlsx, PointData, getAllPoints, updatePoints, deletePoint, getAllSub } from '/@/api/points'
import * as XLSX from 'xlsx'
import { message } from 'ant-design-vue'

// 状态变量
const loading = ref(false)

const selectedSubstation = ref<string | null>(null)
const selectedEquipType = ref<string | null>(null)
const selectedIdentType = ref<string | null>(null)
const searchPoint = ref<string>('')

const editDialogVisible = ref(false) // 控制编辑对话框的显示
const editForm = reactive({
  id: '',
  area_name: '',
  bay_name: '',
  device_name: '',
  device_type: '',
  component_name: '',
  point_name: '',
  point_analyse_type: '',
})

const body: IPage = {
  page: 1,
  total: 0,
  page_size: 10
}
const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '30', '40'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 10,
  current: 1,
  total: 0
})
type Pagination = TableState['pagination']

const waylinesData = reactive({
  // data: generateRandomData()
  data: [] // 初始数据为空
})
paginationProp.total = waylinesData.data.length // 确保总条数与数据长度一致

const tableData = ref<PointData[]>([]) // 点位数据
onMounted(() => {
  queryAllPoints()
  getSubInfo()
})

/**
 * 分页事件
 * @param val
 */
function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  refreshData(paginationProp)
}

function handleCurrentChange (val: number) {
  paginationProp.current = val
  refreshData(paginationProp)
}

function refreshData (page: Pagination) {
  body.page = page?.current!
  body.page_size = page?.pageSize!
  queryAllPoints()
}

/**
 * // 查询所有的场站
 */
const subOption = ref([])
const current_sub = ref('') // 当前选中的场站
function getSubInfo () {
  getAllSub().then(res => {
    if (res.code !== 0) {
      return
    }
    subOption.value = res.data.map(item => ({
      value: item.sub_code,
      label: item.sub_name
    }))
  })
}
// --------------------------------------------------导入excel数据--------------------------------------------------

const impPointLedger = ref(false)
const selectedFile = ref<File | null>(null)
const beforeUploadExcel = (file: File) => {
  const isExcel = file.type === 'application/vnd.ms-excel' || file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  if (!isExcel) {
    ElMessage.error('只能上传Excel文件！')
    return false
  }
  selectedFile.value = file // 存储选择的文件
  return false // 阻止默认的上传行为
}

const handleFileChange = (file: File) => {
  selectedFile.value = file // 更新选中的文件
}

const confirmImport = () => {
  // console.log('当前选中的场站', current_sub)
  if (!current_sub.value) {
    ElMessage.error('请先选择变电站！')
    return
  }
  if (selectedFile.value) {
    // 关闭弹窗
    impPointLedger.value = false
    // 读取文件信息
    readExcelData(selectedFile.value)
    selectedFile.value = null
  } else {
    ElMessage.error('请先选择文件！')
  }
}

/**
 * @description: 读取 Excel 文件内容，插入数据库
 * @param file 文件对象
 * */
// const tableData = ref<PointData[]>([]) // 用于存储解析的表格数据
const readExcelData = (file) => {
  const reader = new FileReader()

  reader.onload = (e) => {
    const data = e.target.result
    const workbook = XLSX.read(new Uint8Array(data), { type: 'array' })

    // 获取第一个工作表的内容
    const sheetName = workbook.SheetNames[0]
    const worksheet = workbook.Sheets[sheetName]

    // 将工作表数据转换为 JSON 格式（假设第一行为表头）
    const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 })

    // 假设第一行是表头（列名）
    const headers = jsonData[0] // 表头

    // 定义目标键名数组
    const Table_data = [
      'area_name', 'area_id', 'bay_name', 'bay_id', 'device_name', 'device_id', 'device_type',
      'component_name', 'component_type_code', 'phase', 'point_describe', 'point_name', 'component_id',
      'point_analyse_type', 'waypoint_name'
    ]

    // 过滤掉表头的第一行，并将数据和表头进行映射
    const mappedData = jsonData.slice(1).map(row => {
      // 创建一个新的对象并映射数据
      const obj = {}
      Table_data.forEach((key, index) => {
        obj[key] = row[index] || '' // 将每一行数据映射到目标键名上
      })

      // 判断整行是否为空（即所有值都为空）
      if (Object.values(obj).every(value => value === '')) {
        return null // 返回 null 表示这行是空的，不添加到结果中
      }

      return obj // 如果整行不为空，返回对象
    }).filter(row => row !== null)
    // console.log('sssssss', mappedData)
    // tableData.value = mappedData

    // 请求后端，添加台账
    insertExcelData(sub_code, mappedData)
  }

  // 使用 readAsArrayBuffer 代替 readAsBinaryString
  reader.readAsArrayBuffer(file)
}

/**
 * @description: 台账导入数据库
 * @param sub_code 变电站编码
 * @param tableData 台账数据
 * */
const sub_code = current_sub.value
function insertExcelData (sub_code: string, tableData: Array<PointData>) {
  insertPointsByXlsx(sub_code, tableData).then(res => {
    if (res.code !== 0) {
      return
    }
    const data = res
    message.success('导入成功!')
    queryAllPoints()
  })
}
// =======================================================================================导出excel======================================================================================
/*
  导出点位台账
  param 台账数据
*/
function exportPoint () {

}

/**
 * 请求点位台账
 */
function queryAllPoints () {
  getAllPoints(body).then(res => {
    if (res.code !== 0) {
      return
    }
    tableData.value = res.data.list
    paginationProp.total = res.data.pagination.total
    paginationProp.current = res.data.pagination.page
    // console.log('请求的数据', res.data)
    // message.success('导入成功!')
  })
}

const editRow = (row: any) => {
  const { id, area_name, bay_name, device_name, device_type, component_name, point_name, point_analyse_type } = row
  Object.assign(editForm, {
    id,
    area_name,
    bay_name,
    device_name,
    device_type,
    component_name,
    point_name,
    point_analyse_type
  })
  // editForm.id = row.id
  // editForm.area_name = row.area_name
  // editForm.bay_name = row.bay_name
  // editForm.device_name = row.device_name
  // editForm.device_type = row.device_type
  // editForm.point_name = row.point_name
  // editForm.point_analyse_type = row.point_analyse_type
  editDialogVisible.value = true // 显示弹窗
}

/**
 * @description: 提交更新数据
 * */
const saveEdit = () => {
  // 保存编辑后的数据
  const index = waylinesData.data.findIndex(item => item.id === editForm.value.id)
  if (index !== -1) {
    waylinesData.data[index] = { ...editForm.value }
  }
  editDialogVisible.value = false // 关闭弹窗
}

/**
 * @description: 编辑操作，更新台账
 * @param pointData 台账数据
 * */
function updatePoint (pointData: any) {
  updatePoints(pointData).then(res => {
    if (res.code !== 0) {
      return
    }
    message.success('关联成功')
  })
}
// ===========================================================前端分页功能实现==================================================
// const currentPage = ref(1)
// const pageSize = ref(5)
// const totalItems = computed(() => tableData.value.length)
// const paginatedData = computed(() => {
//   const start = (currentPage.value - 1) * pageSize.value
//   const end = start + pageSize.value
//   return tableData.value.slice(start, end)
// })
// const handleCurrentChange = (newPage) => {
//   currentPage.value = newPage
// }
// const handleSizeChange = (newSize) => {
//   pageSize.value = newSize
// }
// ============================================================分页数据==========================================================
// ================================================================================================================================================================================
// 删除点位
/**
 * @description:删除单条台账
 * @param id 台账id
 * */
const deleteWayline = (id: string) => {
  deletePoint(id).then(res => {
    if (res.code !== 0) {
      return
    }
    queryAllPoints()
    message.success('删除成功')
  })
}

// 重置筛选条件
const resetPoint = () => {
  selectedSubstation.value = null
  selectedEquipType.value = null
  selectedIdentType.value = null
  searchPoint.value = ''
  paginationProp.current = 1
  paginationProp.pageSize = 10
}
</script>

<style lang="scss" scoped>
.wayline-panel {
  background: #3c3c3c;
  margin-left: auto;
  margin-right: auto;
  margin-top: 10px;
  height: 90px;
  width: 95%;
  font-size: 13px;
  border-radius: 2px;
  cursor: pointer;

  .title {
    display: flex;
    flex-direction: row;
    align-items: center;
    height: 30px;
    font-weight: bold;
    margin: 0px 10px 0 10px;
  }
}

.uranus-scrollbar {
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-color: #c5c8cc transparent;
}

::v-deep .home-ant-input.ant-input-affix-wrapper .ant-input {
  background-color: black;
  color: #c5c8cc;
}

// 下拉框
.select-operation {
  margin-right: 10px;

  :deep(.el-select__placeholder) {
    font-size: 14px;
    font-weight: 500;
    color: #A8ABB2;
  }

  :deep(.el-select__wrapper) {

    // background: rgba(59, 116, 255, 0.15);
    background-color: #0B2756;
    // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    // border: 1px solid #719fff;
    // border-radius: 4px;
    width: 200px;
    height: 30px;
  }

  /**修改下拉图标颜色 */
  :deep(.el-select__caret) {
    color: #ffffff;
  }

  :deep(.el-select-dropdown) {
    background: #012b78;
    // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    border: 1px solid #719fff;
  }

  :deep(.el-select-dropdown__item) {
    font-size: 14px;
    font-weight: 500;
    color: #ffffff;
  }

  :deep(.el-select-dropdown__item.is-hovering) {

    background-color: skyblue;
  }
}

// 上传按钮
::v-deep .el-upload-dragger {
  background-color: #4874B3;
}

::v-deep .el-dialog__title {
  font-size: 18px;
  /* 修改为你想要的大小 */
  // font-weight: bold;
  color: white;
}

::v-deep .el-dialog {
  background-color: #0B2757;
  // background: rgba(59, 116, 255, 0.15);
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
}

.container {
  // height: 100%;
  width: 100%;
  // min-width: 1500px;
  // padding: 10px;
  display: flex;
  flex-direction: column;
  /* 垂直排列 */

}

.table-container {
  flex-grow: 1;
  overflow: hidden;
  height: 70vh;
  overflow-y: auto;
}

.ellipsis {
  white-space: nowrap;
  /* 防止换行 */
  overflow: hidden;
  /* 隐藏超出部分 */
  text-overflow: ellipsis;
  /* 显示省略号 */
}

// 下拉框
.select-operation {
  :deep(.el-select__placeholder) {
    color: rgba(255, 255, 255, 1);
    font-size: 14px;
    font-family: Google Sans-Medium;
    font-weight: 500;
  }

  :deep(.el-select__wrapper) {

    // background: rgba(59, 116, 255, 0.15);
    background-color: #0B2756;
    // box-shadow: inset 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
    box-shadow: 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
    // border: 1px solid #719fff;
    // border-radius: 4px;
    width: 200px;
    height: 30px;
  }

  /**修改下拉图标颜色 */
  :deep(.el-select__caret) {
    color: #ffffff;
  }

  :deep(.el-select-dropdown) {
    background: #012b78;
    box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    border: 1px solid #719fff;
  }

  :deep(.el-select-dropdown__item) {
    font-size: 14px;
    font-weight: 500;
    color: #ffffff;
  }

  :deep(.el-select-dropdown__item.is-hovering) {

    background-color: skyblue;
  }
}

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

    .thumbnail_2 {
      width: 20px;
      height: 20px;
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

  .new_btn2 {
    background-image: linear-gradient(180deg,
        rgba(70, 145, 217, 1) 0,
        rgba(21, 81, 181, 1) 100%);
    border-radius: 4px;
    padding: 0 5px;

    .thumbnail_1 {
      width: 12px;
      height: 12px;
      margin: 5px 0 0 12px;
    }

    .thumbnail_2 {
      width: 20px;
      height: 20px;
    }

  }

  .new_btn1 {
    background-image: linear-gradient(180deg,
        rgba(248, 212, 94, 1) 0,
        rgba(227, 157, 6, 1) 100%);
    border-radius: 4px;
    width: 70px;
    height: 37px;
    margin-left: 0px;

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

.content {
  margin: 15px 12px 0 12px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

.item1 {
  display: flex;
  justify-items: center;
  align-items: center;
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

::v-deep .home-ant-input.ant-input-affix-wrapper .ant-input {
  background-color: black;
  color: #c5c8cc;
}

.live {
  position: absolute;
  z-index: 1;
  left: 0;
  top: 400px;
  margin-left: 345px;

  text-align: center;
  width: 1000px;
  height: 420px;
  background: #232323;
}

/* 公共按钮样式 */
.custom-btn {
  background-color: rgba(51, 122, 255, 0.12);
  height: 28px;
  border: 1px solid rgba(0, 64, 147, 1);
  margin: 7px;
}

/* 编辑按钮 */
.edit-btn {
  @extend .custom-btn;
  width: 40px;
  margin-left: 7px;
}

.delete-btn {
  background-color: rgba(255, 92, 51, 0.19);
  border-radius: 4px;
  height: 28px;
  color: rgba(255, 215, 215, 1);
  border: 1px solid rgba(255, 132, 132, 1);
  width: 40px;
  margin: 7px;
}

.okbtn {
  background-color: rgba(7, 75, 208, 1);
  // height: 40px;
  border: 1px solid rgba(0, 64, 147, 1);
}

.okbtn:hover {
  border: 1px solid rgba(0, 112, 209, 1);
}

.nobtn {
  background-color: rgba(255, 255, 255, 0.05);
  // height: 40px;
  border: 1px solid rgba(206, 227, 255, 0.42);
}

.action-buttons {
  display: flex;
  /* 使用 flex 布局 */
  gap: 8px;
  /* 按钮之间的间距 */
  justify-content: flex-start;
  /* 如果需要调整对齐方式，可以改为 center 或 space-between */
  align-items: center;
  /* 垂直方向对齐 */
}

.custom-edit-btn,
.custom-delete-btn {
  margin: 0;
  /* 清除之前的外边距 */
  width: auto;
  /* 根据内容自适应宽度 */
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

// 输入框
:deep(.el-input) {
  --el-input-border-color: #1d4292;
}

:deep(.el-input__wrapper) {
  background-color: #0B2756;
  // box-shadow: 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
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

::v-deep .el-table td {
  border: 2px solid #01123288;
  /* 设置列的边框颜色和粗细 */
  font-size: 16px;
  font-weight: 500;
}

// 表格样式
::v-deep .el-table {
  .cell {
    text-align: center;
  }
}

// // 表头大小
::v-deep .el-table th {
  height: 50px;
  font-size: 16px !important;
  /* 如果你需要修改表头字体大小，设置一个不同的大小 */
  color: rgba(255, 255, 255, 1);
  background-color: #00399A;
  border-left: 2px #01123288 solid;
  border-bottom: 2px #01123288 solid !important;
}

/* // 修改高亮当前行颜色 */
::v-deep .el-table tbody tr:hover>td {
  background: rgba(0, 114, 245, 0.6) !important;
}

/* // 斑马线颜色 */

::v-deep .el-table--striped .el-table__body tr.el-table__row--striped td {
  background: rgba(0, 45, 120, 1);
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

//弹窗====================================================================
/* 修改标题的样式 */
.substation-title {
  font-size: 16px;
  /* 修改标题的字体大小 */
  // font-weight: bold;
  /* 加粗字体 */
  color: white;
  /* 设置标题的颜色 */
  // margin-bottom: 0px;
  /* 设置标题和下拉框的间距 */
  display: block;
  /* 让标题独占一行 */
}

/* 修改下拉框的样式 */
.substation-select {
  background-color: #154480;
  margin-bottom: 20px;

  font-size: 14px;
  /* 设置字体大小 */
  color: #333;
  /* 设置字体颜色 */
}

/* 下拉框的选项样式 */
.substation-select .el-select-dropdown {
  border-radius: 4px;
  // border: 1px solid #ccc;
}

/* 增加下拉框 hover 的效果 */
.substation-select .el-select-dropdown__item:hover {
  background-color: #154480;
  color: #409EFF;
  /* Hover时的字体颜色 */
}

::v-deep .el-form-item__label {
  color: white;
}

::v-deep .el-input__inner {
  color: white;
}</style>
