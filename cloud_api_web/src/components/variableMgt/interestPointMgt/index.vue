<template>
  <div class="container">
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="兴趣点名称:" prop="turbine_name">
          <el-input
            v-model="queryForm.point_name"
            placeholder="请输入兴趣点名称"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item label="兴趣点ID:" prop="id">
          <el-input
            v-model="queryForm.id"
            placeholder="请输入兴趣点ID"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Search"
            @click="getAllInserestPoint()"
          >
            查询
          </el-button>
          <el-button
            class="new_btn1"
            type="primary"
            :icon="Refresh"
            @click="handleRest()"
          >
            重置
          </el-button>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Plus"
            @click="openInsertDialog()"
          >
            新增
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="content">
      <div class="table-container">
        <el-table :data="tableData" stripe>
          <el-table-column type="index"  align="center" label="序号" width="60" />
          <el-table-column label="兴趣点ID" align="center" >
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.id }}</div>
            </template>
          </el-table-column>
          <el-table-column label="兴趣点名称" align="center" >
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.point_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="兴趣点经度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.point_longitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="兴趣点纬度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.point_latitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="兴趣点高度(米)" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.point_altitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="环绕高度(米)" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.orbit_height }}</div>
            </template>
          </el-table-column>
          <el-table-column label="环绕半径" >
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.orbit_radius }}</div>
            </template>
          </el-table-column>
          <el-table-column label="初始方向" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.init_direction }}</div>
            </template>
          </el-table-column>
          <el-table-column label="焦距" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.focal_length }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openEditDialog(scope.row)"
                >编辑</el-button
              >
              <el-button link type="danger" @click="handleDelete(scope.row)"
                >删除</el-button
              >
              <!-- <el-button link type="primary" @click="handleTask(scope.row)"
                >执行任务</el-button
              > -->
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-container">
        <!-- 分页 -->
        <el-pagination v-model:current-page="paginationProp.pageNo" v-model:page-size="paginationProp.pageSize"
          :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
          layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handleCurrentChange">
        </el-pagination>
      </div>
    </div>
    <!-- 新增弹窗 -->
    <el-dialog
      v-model="insertDialog"
      title="新增风机"
      width="1200"
      style="background-color: #0A2D63; color: white"
    >
      <el-form
        :model="insertForm"
        label-width="180px"
        :rules="formRules"
        ref="formRef"
      >
        <el-row :gutter="20">
          <!-- 第一列 -->
          <el-col :span="12">
            <el-form-item label="兴趣点名称" required prop="point_name">
              <el-input
                v-model="insertForm.point_name"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="兴趣点经度" prop="point_longitude" required>
              <el-input
                v-model="insertForm.point_longitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="兴趣点纬度" prop="point_latitude" required>
              <el-input
                v-model="insertForm.point_latitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
                        <el-form-item label="兴趣点高度(米)" prop="point_altitude" required>
              <el-input
                v-model="insertForm.point_altitude"
                maxlength="50"
              ></el-input>
            </el-form-item>

          </el-col>

          <!-- 第二列 -->
          <el-col :span="12">
            <el-form-item label="环绕高度(米)" prop="orbit_height" required>
              <el-input
                v-model="insertForm.orbit_height"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="环绕半径"
              prop="orbit_radius"
              required
            >
              <el-input
                v-model="insertForm.orbit_radius"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="初始方向"
              prop="init_direction"
              required
            >
              <el-input
                v-model="insertForm.init_direction"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="焦距" prop="focal_length" required>
              <el-input
                v-model="insertForm.focal_length"
                maxlength="50"
              ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template v-slot:footer>
        <div class="dialog-footer">
          <el-button @click="insertDialog = false" class="nobtn"
            >取 消</el-button
          >
          <el-button type="primary" @click="handleInsert()" class="okbtn"
            >确 定</el-button
          >
        </div>
      </template>
    </el-dialog>
    <!-- 更新弹窗 -->
    <el-dialog
      v-model="editDialog"
      title="编辑风机"
      width="1200"
      style="background-color: #0A2D63; color: white"
    >
      <el-form
        :model="editForm"
        label-width="180px"
        :rules="formRules"
        ref="editFormRef"
      >
        <el-row :gutter="20">
          <!-- 第一列 -->
          <el-col :span="12">
            <el-form-item label="兴趣点名称" required prop="point_name">
              <el-input
                v-model="editForm.point_name"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="兴趣点经度" prop="point_longitude" required>
              <el-input
                v-model="editForm.point_longitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="兴趣点纬度" prop="point_latitude" required>
              <el-input
                v-model="editForm.point_latitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
                        <el-form-item label="兴趣点高度(米)" prop="point_altitude" required>
              <el-input
                v-model="editForm.point_altitude"
                maxlength="50"
              ></el-input>
            </el-form-item>

          </el-col>

          <!-- 第二列 -->
          <el-col :span="12">
            <el-form-item label="环绕高度(米)" prop="orbit_height" required>
              <el-input
                v-model="editForm.orbit_height"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="环绕半径"
              prop="orbit_radius"
              required
            >
              <el-input
                v-model="editForm.orbit_radius"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="初始方向"
              prop="init_direction"
              required
            >
              <el-input
                v-model="editForm.init_direction"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="焦距" prop="focal_length" required>
              <el-input
                v-model="editForm.focal_length"
                maxlength="50"
              ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template v-slot:footer>
        <div class="dialog-footer">
          <el-button @click="editDialog = false" class="nobtn">取 消</el-button>
          <el-button type="primary" @click="handleEdit()" class="okbtn"
            >确 定</el-button
          >
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElButton, ElDialog, ElForm, ElFormItem, ElMessageBox, ElInput, ElSelect, ElOption, ElUpload, ElMessage } from 'element-plus'
import { addInserestPointApi, executeFlyTaskApi, getAllInserestPointApi, updateInserestPointApi, deleteInserestPointApi } from '/@/api/turbine/turbineMgt'

// 表单
const queryForm = reactive({
  point_name: '',
  id: ''
})
const formRef = ref(null)
const editFormRef = ref(null)

const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '40'],
  pageSize: 10,
  pageNo: 1,
  total: 0
})

const insertForm = reactive({
  point_name: '',
  point_longitude: '',
  point_latitude: '',
  point_altitude: '',
  orbit_height: '',
  orbit_radius: '',
  init_direction: '',
  focal_length: ''

})
const editForm = reactive({
  point_name: '',
  point_longitude: '',
  point_latitude: '',
  point_altitude: '',
  orbit_height: '',
  orbit_radius: '',
  init_direction: '',
  focal_length: ''
})

// 表格
const tableData = ref([])

// 弹窗
const insertDialog = ref(false)
const editDialog = ref(false)

// 表单规则
const formRules = {
  point_name: [
    { required: true, message: '请输入兴趣带你名称', trigger: 'blur' }
  ],
  point_longitude: [
    { required: true, message: '请输入兴趣点经度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  point_latitude: [
    { required: true, message: '请输入兴趣点纬度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  point_altitude: [
    { required: true, message: '请输入兴趣点海拔高度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],

  orbit_height: [
    { required: true, message: '请输入轨道高度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  orbit_radius: [
    { required: true, message: '请输入轨道半径', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  init_direction: [
    { required: true, message: '请输入飞初始角度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  focal_length: [
    { required: true, message: '请输入焦距', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ]
}

onMounted(() => {
  getAllInserestPoint()
})

/**
 * 新增风机设备配置
 */
async function handleInsert () {
  try {
    const valid = await formRef.value.validate()
    //   if (!valid) {
    //     ElMessage.warning('请检查表单是否填写!')
    //   }
    const res = await addInserestPointApi(insertForm)
    if (res.code !== 0) {
    // 异常
      return
    }
    ElMessage.success('新增成功!')
    insertDialog.value = false
    await handleRest()
  } catch (error) {

  }
}

// 打开新增表单
function openInsertDialog () {
  formRef.value?.resetFields()
  Object.assign(insertForm, {
    point_name: '',
    point_longitude: '',
    point_latitude: '',
    point_altitude: '',
    orbit_height: '',
    orbit_radius: '',
    init_direction: '',
    focal_length: ''
  })
  insertDialog.value = true
}

/**
 * 更新风机配置
 */
function openEditDialog (row:any) {
  editDialog.value = true
  Object.assign(editForm, row)
}

async function handleEdit () {
  try {
    const valid = await editFormRef.value.validate()
    //   if (!valid) {
    //     ElMessage.warning('请检查表单是否填写!')
    //   }
    const res = await updateInserestPointApi(editForm)
    if (res.code !== 0) {
    // 异常
      return
    }
    ElMessage.success('更新成功!')
    editDialog.value = false
    await handleRest()
  } catch (error) {

  }
}

/**
 * 删除风机参数
 */
async function handleDelete (row:any) {
  try {
    ElMessageBox.confirm('确定要删除该兴趣点吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(async () => {
        const res = await deleteInserestPointApi(row.id)
        if (res.code !== 0) {
          return
        }
        ElMessage.success('删除成功!')
        await handleRest()
      })
  } catch (error) {

  }
}
// 获取风机信息查询
function getAllInserestPoint () {
  try {
    getAllInserestPointApi({ ...paginationProp, ...queryForm }).then(res => {
      if (res.code !== 0) {
        return
      }
      tableData.value = res.data.list
      paginationProp.total = res.data.pagination.total
    })
  } catch (error) {

  }
}

// 重置查询
function handleRest () {
  queryForm.point_name = ''
  queryForm.id = ''
  getAllInserestPoint()
}

/**
 * 执行飞行任务
 */
async function handleTask (row) {
  try {
    ElMessageBox.confirm('确定要执行飞行任务吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(async () => {
        const res = executeFlyTaskApi({ id: row.id })
        ElMessage.success('飞行任务发布成功!')
      })
      .catch(() => {
      // ElMessage.info('已取消删除')
      })
  } catch (error) {

  }
}

/**
 * 分页查询
 */

function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  getAllInserestPoint()
}
function handleCurrentChange (val: number) {
  paginationProp.pageNo = val
  getAllInserestPoint()
}
</script>

<style lang="scss" scoped>
.container {
    width: 100%;
    display: flex;
    flex-direction: column;
}

.table-container {
    flex-grow: 1;
    overflow: hidden;
    height: 65vh;
    overflow-y: auto;
}

.ellipsis {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 5;
  overflow: hidden;
  text-overflow: ellipsis;
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

    .new_btn {
        background-image: linear-gradient(180deg,
                rgba(70, 145, 217, 1) 0,
                rgba(21, 81, 181, 1) 100%);
        border-radius: 4px;
        height: 30px;
    }

    .new_btn1 {
        background-image: linear-gradient(180deg,
                rgba(248, 212, 94, 1) 0,
                rgba(227, 157, 6, 1) 100%);
        border-radius: 4px;
        height: 30px;
    }
}

.content {
    margin: 15px 12px 0 12px;
    max-height: calc(100vh - 300px);
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

:deep(.el-table tr) {
    background-color: #011C4B !important;
    color: #F1F6FF;
    font-weight: bold;
}

:deep(.el-table__empty-block) {
    background-color: #2264a7;
}

:deep(.el-table td) {
    border: 2px solid #01123288;
    font-size: 16px;
    font-weight: 500;
}

:deep(.el-table) {
    .cell {
        text-align: center;
    }
}

:deep(.el-table th) {
    height: 50px;
    font-size: 16px !important;
    color: rgba(255, 255, 255, 1);
    background-color: #00399A;
    border-left: 2px #01123288 solid;
    border-bottom: 2px #01123288 solid !important;
}

:deep(.el-table tbody tr:hover>td) {
    background: rgba(0, 114, 245, 0.6) !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
    background: rgba(0, 45, 120, 1);
}

:deep(.el-table__body-wrapper .el-table__row) {
    border-bottom: none !important;
    box-shadow: none !important;
}

:deep(.el-table__body-wrapper .el-table__row td) {
    border-bottom: none !important;
}

:deep(.el-table__header-wrapper tr th.el-table-fixed-column--right){
  background-color: #00399A;
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

:deep(.el-pager li.is-active){
  color:#11B4FB !important;
}

:deep(.el-form-item__label) {
    color: white;
}

:deep(.el-input__inner) {
    color: white;
}

:deep(.el-dialog__title) {
    font-size: 18px;
    color: white;
}

:deep(.el-dialog) {
    background-color: #0B2757;
    -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
}

:deep(.el-upload-dragger) {
    background-color: #4874B3;
}
</style>
