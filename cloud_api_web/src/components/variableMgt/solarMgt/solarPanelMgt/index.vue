<template>
  <div class="container">
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="光伏板名称:" prop="solar_panel_name">
          <el-input
            v-model="queryForm.solar_panel_name"
            placeholder="请输入光伏板名称"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item label="光伏板ID:" prop="id">
          <el-input
            v-model="queryForm.id"
            placeholder="请输入光伏板ID"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Search"
            @click="getSolarPanelConfig()"
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
          <el-table-column label="光伏板ID" align="center" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.id }}</div>
            </template>
          </el-table-column>
          <el-table-column label="光伏板名称" align="center" width="150">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.solar_panel_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="角1经度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.corner1_lng }}</div>
            </template>
          </el-table-column>
          <el-table-column label="角1纬度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.corner1_lat }}</div>
            </template>
          </el-table-column>
          <el-table-column label="角2经度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.corner2_lng }}</div>
            </template>
          </el-table-column>
          <el-table-column label="角2纬度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.corner2_lat }}</div>
            </template>
          </el-table-column>
          <el-table-column label="角3经度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.corner3_lng }}</div>
            </template>
          </el-table-column>
          <el-table-column label="角3纬度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.corner3_lat }}</div>
            </template>
          </el-table-column>
          <el-table-column label="角4经度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.corner4_lng }}</div>
            </template>
          </el-table-column>
          <el-table-column label="角4纬度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.corner4_lat }}</div>
            </template>
          </el-table-column>
          <el-table-column label="航线高度(米)" align="center" width="110">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.flight_altitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="光伏板倾角(度)" align="center" width="120">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.tilt_angle }}</div>
            </template>
          </el-table-column>
          <el-table-column label="横向航线数" align="center" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.horizontal_routes }}</div>
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
      title="新增光伏板"
      width="1000"
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
            <el-form-item label="光伏板名称" required prop="solarPanelName">
              <el-input
                v-model="insertForm.solarPanelName"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角1经度" prop="corner1Lng" required>
              <el-input
                v-model="insertForm.corner1Lng"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角1纬度" prop="corner1Lat" required>
              <el-input
                v-model="insertForm.corner1Lat"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角2经度" prop="corner2Lng" required>
              <el-input
                v-model="insertForm.corner2Lng"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角2纬度" prop="corner2Lat" required>
              <el-input
                v-model="insertForm.corner2Lat"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="航线高度(米)" prop="flightAltitude" required>
              <el-input
                v-model="insertForm.flightAltitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角3经度" prop="corner3Lng" required>
              <el-input
                v-model="insertForm.corner3Lng"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角3纬度" prop="corner3Lat" required>
              <el-input
                v-model="insertForm.corner3Lat"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角4经度" prop="corner4Lng" required>
              <el-input
                v-model="insertForm.corner4Lng"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角4纬度" prop="corner4Lat" required>
              <el-input
                v-model="insertForm.corner4Lat"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="光伏板倾角(度)" prop="tiltAngle" required>
              <el-input
                v-model="insertForm.tiltAngle"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="横向航线数" prop="horizontalRoutes" required>
              <el-input
                v-model="insertForm.horizontalRoutes"
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
      title="编辑光伏板"
      width="1000"
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
            <el-form-item label="光伏板名称" required prop="solarPanelName">
              <el-input
                v-model="editForm.solarPanelName"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角1经度" prop="corner1Lng" required>
              <el-input
                v-model="editForm.corner1Lng"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角1纬度" prop="corner1Lat" required>
              <el-input
                v-model="editForm.corner1Lat"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角2经度" prop="corner2Lng" required>
              <el-input
                v-model="editForm.corner2Lng"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角2纬度" prop="corner2Lat" required>
              <el-input
                v-model="editForm.corner2Lat"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="航线高度(米)" prop="flightAltitude" required>
              <el-input
                v-model="editForm.flightAltitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角3经度" prop="corner3Lng" required>
              <el-input
                v-model="editForm.corner3Lng"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角3纬度" prop="corner3Lat" required>
              <el-input
                v-model="editForm.corner3Lat"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角4经度" prop="corner4Lng" required>
              <el-input
                v-model="editForm.corner4Lng"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="角4纬度" prop="corner4Lat" required>
              <el-input
                v-model="editForm.corner4Lat"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="光伏板倾角(度)" prop="tiltAngle" required>
              <el-input
                v-model="editForm.tiltAngle"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="横向航线数" prop="horizontalRoutes" required>
              <el-input
                v-model="editForm.horizontalRoutes"
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
import { addSolarPanelConfigApi, getAllSolarPanelApi, updateSolarPanelConfigApi, deleteSolarPanelApi } from '/@/api/turbine/turbineMgt'

const queryForm = reactive({
  solar_panel_name: '',
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
  solarPanelName: '',
  corner1Lng: '',
  corner1Lat: '',
  corner2Lng: '',
  corner2Lat: '',
  corner3Lng: '',
  corner3Lat: '',
  corner4Lng: '',
  corner4Lat: '',
  flightAltitude: '',
  tiltAngle: '',
  horizontalRoutes: ''
})
const editForm = reactive({
  id: '',
  solarPanelName: '',
  corner1Lng: '',
  corner1Lat: '',
  corner2Lng: '',
  corner2Lat: '',
  corner3Lng: '',
  corner3Lat: '',
  corner4Lng: '',
  corner4Lat: '',
  flightAltitude: '',
  tiltAngle: '',
  horizontalRoutes: ''
})
const tableData = ref([])

const insertDialog = ref(false)
const editDialog = ref(false)

const formRules = {
  solarPanelName: [
    { required: true, message: '请输入光伏板名称', trigger: 'blur' }
  ],
  corner1Lng: [
    { required: true, message: '请输入角1经度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  corner1Lat: [
    { required: true, message: '请输入角1纬度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  corner2Lng: [
    { required: true, message: '请输入角2经度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  corner2Lat: [
    { required: true, message: '请输入角2纬度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  corner3Lng: [
    { required: true, message: '请输入角3经度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  corner3Lat: [
    { required: true, message: '请输入角3纬度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  corner4Lng: [
    { required: true, message: '请输入角4经度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  corner4Lat: [
    { required: true, message: '请输入角4纬度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  flightAltitude: [
    { required: true, message: '请输入航线高度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  tiltAngle: [
    { required: true, message: '请输入光伏板倾角', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  horizontalRoutes: [
    { required: true, message: '请输入横向航线数', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ]
}

onMounted(() => {
  getSolarPanelConfig()
})

async function handleInsert () {
  try {
    const valid = await formRef.value.validate()
    if (!valid) {
      ElMessage.warning('请检查表单是否填写!')
      return
    }
    const submitData = {
      solar_panel_name: insertForm.solarPanelName,
      corner1_lng: Number(insertForm.corner1Lng),
      corner1_lat: Number(insertForm.corner1Lat),
      corner2_lng: Number(insertForm.corner2Lng),
      corner2_lat: Number(insertForm.corner2Lat),
      corner3_lng: Number(insertForm.corner3Lng),
      corner3_lat: Number(insertForm.corner3Lat),
      corner4_lng: Number(insertForm.corner4Lng),
      corner4_lat: Number(insertForm.corner4Lat),
      flight_altitude: Number(insertForm.flightAltitude),
      tilt_angle: Number(insertForm.tiltAngle),
      horizontal_routes: Number(insertForm.horizontalRoutes)
    }
    const res = await addSolarPanelConfigApi(submitData)
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
    solarPanelName: '',
    corner1Lng: '',
    corner1Lat: '',
    corner2Lng: '',
    corner2Lat: '',
    corner3Lng: '',
    corner3Lat: '',
    corner4Lng: '',
    corner4Lat: '',
    flightAltitude: '',
    tiltAngle: '',
    horizontalRoutes: ''
  })
  insertDialog.value = true
}

function openEditDialog (row:any) {
  editDialog.value = true
  Object.assign(editForm, {
    id: row.id,
    solarPanelName: row.solar_panel_name,
    corner1Lng: row.corner1_lng,
    corner1Lat: row.corner1_lat,
    corner2Lng: row.corner2_lng,
    corner2Lat: row.corner2_lat,
    corner3Lng: row.corner3_lng,
    corner3Lat: row.corner3_lat,
    corner4Lng: row.corner4_lng,
    corner4Lat: row.corner4_lat,
    flightAltitude: row.flight_altitude,
    tiltAngle: row.tilt_angle,
    horizontalRoutes: row.horizontal_routes
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
      solar_panel_name: editForm.solarPanelName,
      corner1_lng: Number(editForm.corner1Lng),
      corner1_lat: Number(editForm.corner1Lat),
      corner2_lng: Number(editForm.corner2Lng),
      corner2_lat: Number(editForm.corner2Lat),
      corner3_lng: Number(editForm.corner3Lng),
      corner3_lat: Number(editForm.corner3Lat),
      corner4_lng: Number(editForm.corner4Lng),
      corner4_lat: Number(editForm.corner4Lat),
      flight_altitude: Number(editForm.flightAltitude),
      tilt_angle: Number(editForm.tiltAngle),
      horizontal_routes: Number(editForm.horizontalRoutes)
    }
    const res = await updateSolarPanelConfigApi(submitData)
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
    ElMessageBox.confirm('确定要删除该光伏板吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(async () => {
        const res = await deleteSolarPanelApi(row.id)
        if (res.code !== 0) {
          return
        }
        ElMessage.success('删除成功!')
        await handleRest()
      })
  } catch (error) {

  }
}

function getSolarPanelConfig () {
  try {
    getAllSolarPanelApi({ ...paginationProp, ...queryForm }).then(res => {
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
  queryForm.solar_panel_name = ''
  queryForm.id = ''
  getSolarPanelConfig()
}

function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  getSolarPanelConfig()
}

function handleCurrentChange (val: number) {
  paginationProp.pageNo = val
  getSolarPanelConfig()
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
