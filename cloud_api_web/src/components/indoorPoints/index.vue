<template>
  <div class="container">
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="点位名称:" prop="name">
          <el-input v-model="queryForm.name" placeholder="请输入点位名称" class="custom-input" />
        </el-form-item>
        <el-form-item>
          <el-button class="new_btn" type="primary" :icon="Search" @click="getList">查询</el-button>
          <el-button class="new_btn1" type="primary" :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button class="new_btn" type="primary" :icon="Plus" @click="openAddDialog">新增</el-button>
          <el-button class="new_btn2" type="danger" :icon="Delete" @click="handleBatchDelete">删除</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="content">
      <div class="table-container">
        <el-table :data="tableData" stripe @selection-change="handleSelectionChange">
          <el-table-column type="selection" align="center" width="60" />
          <el-table-column type="index" align="center" label="序号" width="60" />
          <el-table-column label="点位名称" prop="name" align="center" />
          <el-table-column label="X坐标" prop="x" align="center" />
          <el-table-column label="Y坐标" prop="y" align="center" />
          <el-table-column label="Z坐标" prop="z" align="center" />
          <el-table-column label="操作" align="center" width="150" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNo"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 40]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑点位' : '新增点位'" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="点位名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入点位名称" />
        </el-form-item>
        <el-form-item label="X坐标" prop="x">
          <el-input-number v-model="form.x" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Y坐标" prop="y">
          <el-input-number v-model="form.y" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Z坐标" prop="z">
          <el-input-number v-model="form.z" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="nobtn" @click="dialogVisible = false">取消</el-button>
        <el-button class="okbtn" type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  getAllIndoorPoints,
  addIndoorPoint,
  updateIndoorPoint,
  deleteIndoorPoint,
  type IndoorPointRequest
} from '/@/api/indoorPoints'

interface PointItem {
  id: string
  name: string
  x: number
  y: number
  z: number
}

const queryForm = reactive({
  name: ''
})

const form = reactive<IndoorPointRequest>({
  name: '',
  x: 0,
  y: 0,
  z: 0
})

const rules = {
  name: [{ required: true, message: '请输入点位名称', trigger: 'blur' }],
  x: [{ required: true, message: '请输入X坐标', trigger: 'blur' }],
  y: [{ required: true, message: '请输入Y坐标', trigger: 'blur' }],
  z: [{ required: true, message: '请输入Z坐标', trigger: 'blur' }]
}

const pagination = reactive({
  pageSizeOptions: ['10', '20', '40'],
  pageSize: 10,
  pageNo: 1,
  total: 0
})

const tableData = ref<PointItem[]>([])
const allData = ref<PointItem[]>([])
const selectRows = ref<string[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref('')
const formRef = ref<FormInstance>()

onMounted(() => {
  getList()
})

async function getList () {
  try {
    const res = await getAllIndoorPoints()
    if (res.code !== 0) return
    allData.value = res.data || []
    filterData()
  } catch (error) {
    //
  }
}

function filterData () {
  const filtered = allData.value.filter(item => {
    if (queryForm.name && !item.name.includes(queryForm.name)) return false
    return true
  })
  pagination.total = filtered.length
  const start = (pagination.pageNo - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  tableData.value = filtered.slice(start, end)
}

function handleReset () {
  queryForm.name = ''
  pagination.pageNo = 1
  getList()
}

function handleSelectionChange (val: PointItem[]) {
  selectRows.value = val.map(item => item.id)
}

function openAddDialog () {
  isEdit.value = false
  editId.value = ''
  form.name = ''
  form.x = 0
  form.y = 0
  form.z = 0
  dialogVisible.value = true
}

function openEditDialog (row: PointItem) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.x = row.x
  form.y = row.y
  form.z = row.z
  dialogVisible.value = true
}

async function handleSubmit () {
  const valid = await formRef.value?.validate()
  if (!valid) return

  try {
    if (isEdit.value) {
      const res = await updateIndoorPoint(editId.value, { ...form })
      if (res.code !== 0) return
      ElMessage.success('修改成功!')
    } else {
      const res = await addIndoorPoint({ ...form })
      if (res.code !== 0) return
      ElMessage.success('新增成功!')
    }
    dialogVisible.value = false
    await getList()
  } catch (error) {
    //
  }
}

async function handleDelete (row: PointItem) {
  try {
    await ElMessageBox.confirm('确定要删除该点位吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deleteIndoorPoint(row.id)
    if (res.code !== 0) return
    ElMessage.success('删除成功!')
    await getList()
  } catch (error) {
    //
  }
}

async function handleBatchDelete () {
  if (selectRows.value.length === 0) {
    ElMessage.warning('请选择要删除的数据！')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectRows.value.length} 个点位吗?`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    for (const id of selectRows.value) {
      const res = await deleteIndoorPoint(id)
      if (res.code !== 0) return
    }
    ElMessage.success('批量删除成功!')
    selectRows.value = []
    await getList()
  } catch (error) {
    //
  }
}

function handleSizeChange (val: number) {
  pagination.pageSize = val
  pagination.pageNo = 1
  filterData()
}

function handleCurrentChange (val: number) {
  pagination.pageNo = val
  filterData()
}
</script>

<style lang="scss" scoped>
.container {
  width: 100%;
  display: flex;
  flex-direction: column;
}

.operation {
  display: flex;
  align-items: center;
  background-color: rgba(1, 36, 98, 1);
  border-radius: 4px;
  height: 60px;
  padding-top: 15px;
  padding-left: 10px;
  margin: 31px 12px 0 12px;
}

.new_btn {
  background-image: linear-gradient(180deg, rgba(70, 145, 217, 1) 0, rgba(21, 81, 181, 1) 100%);
  border-radius: 4px;
  height: 30px;
}

.new_btn1 {
  background-image: linear-gradient(180deg, rgba(248, 212, 94, 1) 0, rgba(227, 157, 6, 1) 100%);
  border-radius: 4px;
  height: 30px;
}

.new_btn2 {
  background-image: linear-gradient(180deg, rgb(246, 164, 132) 0, rgb(190, 64, 22) 100%);
  border-radius: 4px;
  height: 30px;
}

.content {
  margin: 15px 12px 0 12px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

.table-container {
  flex-grow: 1;
  overflow: hidden;
  height: 65vh;
  overflow-y: auto;
}

.okbtn {
  background-color: rgba(7, 75, 208, 1);
  border: 1px solid rgba(0, 64, 147, 1);
}

.okbtn:hover {
  border: 1px solid rgba(0, 112, 209, 1);
}

.nobtn {
  background-color: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(206, 227, 255, 0.42);
}

.pagination-container {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  align-items: center;
}

:deep(.el-input__wrapper) {
  background-color: #0B2756;
}

:deep(.el-input) {
  --el-input-border-color: #1d4292;
}

:deep(.el-input__inner) {
  color: white;
}

:deep(.el-form-item__label) {
  color: white;
}

:deep(.el-table tr) {
  background-color: #011C4B !important;
  color: #F1F6FF;
  font-weight: bold;
}

:deep(.el-table__fixed-right .el-table__body tr) {
  background-color: #011C4B !important;
}

:deep(.el-table__fixed-right .el-table__body td) {
  background-color: #011C4B !important;
}

:deep(.el-table__fixed-right-patch) {
  background-color: #00399A !important;
}

:deep(.el-table th) {
  height: 50px;
  font-size: 14px !important;
  color: rgba(255, 255, 255, 1);
  background-color: #00399A;
  border-left: 2px #01123288 solid;
  border-bottom: 2px #01123288 solid !important;
}

:deep(.el-table td) {
  border: 2px solid #01123288;
  font-size: 14px;
  font-weight: 500;
}

:deep(.el-table__fixed-right .el-table__body td) {
  border: 2px solid #01123288;
  font-size: 14px;
  font-weight: 500;
}

:deep(.el-table tbody tr:hover > td) {
  background: rgba(0, 114, 245, 0.6) !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(0, 45, 120, 1);
}

:deep(.el-table .cell) {
  text-align: center;
}

:deep(.el-table__inner-wrapper::before) {
  height: 0;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  background-color: #062254 !important;
  color: #fff;
}

:deep(.el-pagination .el-pager li:not(.active):not(.disabled):hover) {
  background-color: #2264a7 !important;
}

:deep(.el-pagination .el-pager li:not(.active):not(.disabled)) {
  background-color: #062254 !important;
  color: #fff;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #124AAD !important;
  color: #fff;
}

:deep(.el-pager li.is-active) {
  color: #11B4FB !important;
}

:deep(.el-dialog) {
  background-color: #0B2757;
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
}

:deep(.el-dialog__title) {
  font-size: 18px;
  color: white;
}

</style>
