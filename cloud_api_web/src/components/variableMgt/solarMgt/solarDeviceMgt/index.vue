<template>
  <div class="container">
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="设备名称:" prop="device_name">
          <el-input
            v-model="queryForm.device_name"
            placeholder="请输入设备名称"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item label="设备ID:" prop="id">
          <el-input
            v-model="queryForm.id"
            placeholder="请输入设备ID"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Search"
            @click="getInspectionDeviceConfig()"
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
          <el-table-column
            type="index"
            align="center"
            label="序号"
            width="60"
          />
          <el-table-column label="设备ID" align="center" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.id }}</div>
            </template>
          </el-table-column>
          <el-table-column label="设备名称" align="center" width="150">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.device_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="目标点经度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.target_longitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="目标点纬度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.target_latitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="目标点高度(米)" align="center" width="130">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.target_altitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="无人机高度(米)" align="center" width="130">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.drone_altitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="距离设备距离(米)" align="center" width="140">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.drone_distance }}</div>
            </template>
          </el-table-column>
          <el-table-column label="偏航角(度)" align="center" width="110">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.drone_yaw }}</div>
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
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="paginationProp.pageNo"
          v-model:page-size="paginationProp.pageSize"
          :page-sizes="paginationProp.pageSizeOptions"
          :total="paginationProp.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        >
        </el-pagination>
      </div>
    </div>
    <el-dialog
      v-model="insertDialog"
      title="新增巡视设备"
      width="800"
      style="background-color: #0A2D63; color: white"
    >
      <el-form
        :model="insertForm"
        label-width="160px"
        :rules="formRules"
        ref="formRef"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备名称" required prop="deviceName">
              <el-input
                v-model="insertForm.deviceName"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="目标点经度" prop="targetLongitude" required>
              <el-input
                v-model="insertForm.targetLongitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="目标点纬度" prop="targetLatitude" required>
              <el-input
                v-model="insertForm.targetLatitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="目标点高度(米)" prop="targetAltitude" required>
              <el-input
                v-model="insertForm.targetAltitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="无人机高度(米)" prop="droneAltitude" required>
              <el-input
                v-model="insertForm.droneAltitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="距离设备距离(米)" prop="droneDistance" required>
              <el-input
                v-model="insertForm.droneDistance"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="偏航角(度)" prop="droneYaw" required>
              <el-input
                v-model="insertForm.droneYaw"
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
    <el-dialog
      v-model="editDialog"
      title="编辑巡视设备"
      width="800"
      style="background-color: #0A2D63; color: white"
    >
      <el-form
        :model="editForm"
        label-width="160px"
        :rules="formRules"
        ref="editFormRef"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备名称" required prop="deviceName">
              <el-input
                v-model="editForm.deviceName"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="目标点经度" prop="targetLongitude" required>
              <el-input
                v-model="editForm.targetLongitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="目标点纬度" prop="targetLatitude" required>
              <el-input
                v-model="editForm.targetLatitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="目标点高度(米)" prop="targetAltitude" required>
              <el-input
                v-model="editForm.targetAltitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="无人机高度(米)" prop="droneAltitude" required>
              <el-input
                v-model="editForm.droneAltitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="距离设备距离(米)" prop="droneDistance" required>
              <el-input
                v-model="editForm.droneDistance"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="偏航角(度)" prop="droneYaw" required>
              <el-input
                v-model="editForm.droneYaw"
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
import { reactive, ref, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElButton, ElDialog, ElForm, ElFormItem, ElMessageBox, ElInput, ElMessage } from 'element-plus'
import { addInspectionDeviceConfigApi, getAllInspectionDeviceApi, updateInspectionDeviceConfigApi, deleteInspectionDeviceApi } from '/@/api/turbine/turbineMgt'

const queryForm = reactive({
  device_name: '',
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
  deviceName: '',
  targetLongitude: '',
  targetLatitude: '',
  targetAltitude: '',
  droneAltitude: '',
  droneDistance: '',
  droneYaw: ''
})
const editForm = reactive({
  id: '',
  deviceName: '',
  targetLongitude: '',
  targetLatitude: '',
  targetAltitude: '',
  droneAltitude: '',
  droneDistance: '',
  droneYaw: ''
})
const tableData = ref([])

const insertDialog = ref(false)
const editDialog = ref(false)

const formRules = {
  deviceName: [
    { required: true, message: '请输入设备名称', trigger: 'blur' }
  ],
  targetLongitude: [
    { required: true, message: '请输入目标点经度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  targetLatitude: [
    { required: true, message: '请输入目标点纬度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  targetAltitude: [
    { required: true, message: '请输入目标点高度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  droneAltitude: [
    { required: true, message: '请输入无人机高度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  droneDistance: [
    { required: true, message: '请输入距离设备距离', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  droneYaw: [
    { required: true, message: '请输入偏航角', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ]
}

onMounted(() => {
  getInspectionDeviceConfig()
})

async function handleInsert () {
  try {
    const valid = await formRef.value.validate()
    if (!valid) {
      ElMessage.warning('请检查表单是否填写!')
      return
    }
    const submitData = {
      device_name: insertForm.deviceName,
      target_longitude: Number(insertForm.targetLongitude),
      target_latitude: Number(insertForm.targetLatitude),
      target_altitude: Number(insertForm.targetAltitude),
      drone_altitude: Number(insertForm.droneAltitude),
      drone_distance: Number(insertForm.droneDistance),
      drone_yaw: Number(insertForm.droneYaw)
    }
    const res = await addInspectionDeviceConfigApi(submitData)
    if (res.code !== 0) {
      return
    }
    ElMessage.success('新增成功!')
    insertDialog.value = false
    await handleRest()
  } catch (error) {

  }
}

function openInsertDialog () {
  formRef.value?.resetFields()
  Object.assign(insertForm, {
    deviceName: '',
    targetLongitude: '',
    targetLatitude: '',
    targetAltitude: '',
    droneAltitude: '',
    droneDistance: '',
    droneYaw: ''
  })
  insertDialog.value = true
}

function openEditDialog (row:any) {
  editDialog.value = true
  Object.assign(editForm, {
    id: row.id,
    deviceName: row.device_name,
    targetLongitude: row.target_longitude,
    targetLatitude: row.target_latitude,
    targetAltitude: row.target_altitude,
    droneAltitude: row.drone_altitude,
    droneDistance: row.drone_distance,
    droneYaw: row.drone_yaw
  })
}

async function handleEdit () {
  try {
    const valid = await editFormRef.value.validate()
    if (!valid) {
      ElMessage.warning('请检查表单是否填写!')
      return
    }
    const submitData = {
      id: editForm.id,
      device_name: editForm.deviceName,
      target_longitude: Number(editForm.targetLongitude),
      target_latitude: Number(editForm.targetLatitude),
      target_altitude: Number(editForm.targetAltitude),
      drone_altitude: Number(editForm.droneAltitude),
      drone_distance: Number(editForm.droneDistance),
      drone_yaw: Number(editForm.droneYaw)
    }
    const res = await updateInspectionDeviceConfigApi(submitData)
    if (res.code !== 0) {
      return
    }
    ElMessage.success('更新成功!')
    editDialog.value = false
    await handleRest()
  } catch (error) {

  }
}

async function handleDelete (row:any) {
  try {
    ElMessageBox.confirm('确定要删除该巡视设备吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(async () => {
        const res = await deleteInspectionDeviceApi(row.id)
        if (res.code !== 0) {
          return
        }
        ElMessage.success('删除成功!')
        await handleRest()
      })
  } catch (error) {

  }
}

function getInspectionDeviceConfig () {
  try {
    getAllInspectionDeviceApi({ ...paginationProp, ...queryForm }).then(res => {
      if (res.code !== 0) {
        return
      }
      tableData.value = res.data.list
      paginationProp.total = res.data.pagination.total
    })
  } catch (error) {

  }
}

function handleRest () {
  queryForm.device_name = ''
  queryForm.id = ''
  getInspectionDeviceConfig()
}

function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  getInspectionDeviceConfig()
}

function handleCurrentChange (val: number) {
  paginationProp.pageNo = val
  getInspectionDeviceConfig()
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

.pagination-container {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  align-items: center;
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

:deep(.el-select__wrapper) {
    background-color: #0B2756;
    box-shadow: 0 0 0 1px #163474 inset;
    color: aliceblue;
}

:deep(.el-input) {
  --el-input-border-color: #1d4292;
}

:deep(.el-input__wrapper) {
  background-color: #0B2756;
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

:deep(.el-table__header-wrapper tr th.el-table-fixed-column--right) {
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

:deep(.el-pager li.is-active) {
  color: #11B4FB !important;
}

:deep(.el-dialog__title) {
  font-size: 18px;
  color: white;
}

:deep(.el-dialog) {
  background-color: #0B2757;
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
}

:deep(.el-form-item__label) {
  color: white;
}

:deep(.el-input__inner) {
  color: white;
}
</style>
