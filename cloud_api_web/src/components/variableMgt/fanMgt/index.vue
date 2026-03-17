<template>
  <div class="container">
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="风机名称:" prop="turbine_name">
          <el-input
            v-model="queryForm.turbine_name"
            placeholder="请输入风机名称"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item label="风机ID:" prop="id">
          <el-input
            v-model="queryForm.id"
            placeholder="请输入风机ID"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Search"
            @click="getWindTurbineConfig()"
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
          <el-table-column label="风机ID" align="center" width="150">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.id }}</div>
            </template>
          </el-table-column>
          <el-table-column label="风机名称" align="center" width="150">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.turbine_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="机场经度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.airport_longitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="机场纬度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.airport_latitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="机场海拔(米)" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.airport_altitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="最高点经度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.peak_longitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="最高点纬度" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.peak_latitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="最高点海拔(米)" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.peak_altitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="叶片中心高度(米)" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.blade_center_height }}</div>
            </template>
          </el-table-column>
          <el-table-column label="叶片长度(米)" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.blade_length }}</div>
            </template>
          </el-table-column>
          <el-table-column label="无人机距离(停机)" align="center">
            <template #default="scope">
              <div class="ellipsis">
                {{ scope.row.uav_blade_distance_stop  }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="无人机距离(不停机)" align="center">
            <template #default="scope">
              <div class="ellipsis">
                {{ scope.row.uav_blade_distance_working  }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="风机底部高度(米)" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.blade_bottom_height }}</div>
            </template>
          </el-table-column>
          <el-table-column label="单个扇叶的点数" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.blade_points }}</div>
            </template>
          </el-table-column>
          <el-table-column label="塔筒的点数" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.tower_points }}</div>
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
              <el-button link type="primary" @click="createPoints(scope.row)"
                >生成点位</el-button
              >
              <el-button link type="primary" @click="showPoints(scope.row)"
                >查看点位</el-button
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
          <el-col :span="12">
            <el-form-item label="风机名称" required prop="turbine_name">
              <el-input
                v-model="insertForm.turbine_name"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="机场经度" prop="airport_longitude" required>
              <el-input
                v-model="insertForm.airport_longitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="机场纬度" prop="airport_latitude" required>
              <el-input
                v-model="insertForm.airport_latitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="机场海拔高度(米)"
              prop="airport_altitude"
              required
            >
              <el-input
                v-model="insertForm.airport_altitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="飞行最高点经度" prop="peak_longitude" required>
              <el-input
                v-model="insertForm.peak_longitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="风机底部的高度" prop="peak_longitude" required>
              <el-input
                v-model="insertForm.blade_bottom_height"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="单个扇叶的点数" prop="peak_longitude" required>
              <el-input
                v-model="insertForm.blade_points"
                maxlength="50"
              ></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="飞行最高点纬度" prop="peak_latitude" required>
              <el-input
                v-model="insertForm.peak_latitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="飞行最高点海拔高度(米)"
              prop="peak_altitude"
              required
            >
              <el-input
                v-model="insertForm.peak_altitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="叶片旋转中心高度(米)"
              prop="blade_center_height"
              required
            >
              <el-input
                v-model="insertForm.blade_center_height"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="单个叶片长度(米)" prop="blade_length" required>
              <el-input
                v-model="insertForm.blade_length"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="塔筒的点数" prop="tower_points" required>
              <el-input
                v-model="insertForm.tower_points"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="无人机距离(停机)"
              prop="uav_blade_distance_stop"
              required
            >
              <el-input
                v-model="insertForm.uav_blade_distance_stop"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="无人机距离(不停机)"
              prop="uav_blade_distance_working"
              required
            >
              <el-input
                v-model="insertForm.uav_blade_distance_working"
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
          <el-col :span="12">
            <el-form-item label="风机名称" required prop="turbine_name">
              <el-input
                v-model="editForm.turbine_name"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="机场经度" prop="airport_longitude" required>
              <el-input
                v-model="editForm.airport_longitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="机场纬度" prop="airport_latitude" required>
              <el-input
                v-model="editForm.airport_latitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="机场海拔高度(米)"
              prop="airport_altitude"
              required
            >
              <el-input
                v-model="editForm.airport_altitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="飞行最高点经度" prop="peak_longitude" required>
              <el-input
                v-model="editForm.peak_longitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="风机底部的高度" prop="peak_longitude" required>
              <el-input
                v-model="editForm.blade_bottom_height"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="单个扇叶的点数" prop="peak_longitude" required>
              <el-input
                v-model="editForm.blade_points"
                maxlength="50"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="飞行最高点纬度" prop="peak_latitude" required>
              <el-input
                v-model="editForm.peak_latitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="飞行最高点海拔高度(米)"
              prop="peak_altitude"
              required
            >
              <el-input
                v-model="editForm.peak_altitude"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="叶片旋转中心高度(米)"
              prop="blade_center_height"
              required
            >
              <el-input
                v-model="editForm.blade_center_height"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="单个叶片长度(米)" prop="blade_length" required>
              <el-input
                v-model="editForm.blade_length"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="塔筒的点数" prop="tower_points" required>
              <el-input
                v-model="editForm.tower_points"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="无人机距离(停机)"
              prop="uav_blade_distance_stop"
              required
            >
              <el-input
                v-model="editForm.uav_blade_distance_stop"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="无人机距离(不停机)"
              prop="uav_blade_distance_working"
              required
            >
              <el-input
                v-model="editForm.uav_blade_distance_working"
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

    <el-dialog v-model="pointsDialog" title="点位详情">
      <div>
        <PointsDetail :id="currentId"></PointsDetail>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElButton, ElDialog, ElForm, ElFormItem, ElMessageBox, ElInput, ElSelect, ElOption, ElUpload, ElMessage } from 'element-plus'
import { addWindTurbineConfigApi, executeFlyTaskApi, createWindTurbinePointsApi, getAllWindTurbineApi, updateWindTurbineConfigApi, deleteWindTurbineApi } from '/@/api/turbine/turbineMgt'
import PointsDetail from './pointsDetail.vue'
const currentId = ref('')
const queryForm = reactive({
  turbine_name: '',
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
  turbine_name: '',
  airport_longitude: '',
  airport_latitude: '',
  airport_altitude: '',
  peak_longitude: '',
  peak_latitude: '',
  peak_altitude: '',
  blade_center_height: '',
  blade_length: '',
  uav_blade_distance_stop: '',
  uav_blade_distance_working: '',
  blade_bottom_height: '',
  blade_points: '',
  tower_points: ''

})
const editForm = reactive({
  turbine_name: '',
  airport_longitude: '',
  airport_latitude: '',
  airport_altitude: '',
  peak_longitude: '',
  peak_latitude: '',
  peak_altitude: '',
  blade_center_height: '',
  blade_length: '',
  uav_blade_distance_stop: '',
  uav_blade_distance_working: '',
  blade_bottom_height: '',
  blade_points: '',
  tower_points: ''
})
const tableData = ref([])

const insertDialog = ref(false)
const editDialog = ref(false)
const pointsDialog = ref(false)

const formRules = {
  turbine_name: [
    { required: true, message: '请输入风机名称', trigger: 'blur' }
  ],
  airport_longitude: [
    { required: true, message: '请输入机场经度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  airport_latitude: [
    { required: true, message: '请输入机场纬度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  airport_altitude: [
    { required: true, message: '请输入机场海拔高度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  peak_longitude: [
    { required: true, message: '请输入飞行最高点经度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  peak_latitude: [
    { required: true, message: '请输入飞行最高点纬度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  peak_altitude: [
    { required: true, message: '请输入飞行最高点海拔高度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  blade_center_height: [
    { required: true, message: '请输入叶片旋转中心高度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  blade_length: [
    { required: true, message: '请输入单个叶片长度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  uav_blade_distance_stop: [
    { required: true, message: '请输入无人机距离（停机）', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  uav_blade_distance_working: [
    { required: true, message: '请输入无人机距离（不停机）', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  blade_bottom_height: [
    { required: true, message: '请输入风机底部高度(米)', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  blade_points: [
    { required: true, message: '请输入单个扇叶的点数', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  tower_points: [
    { required: true, message: '请输入塔筒的点数', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ]
}

onMounted(() => {
  getWindTurbineConfig()
})

async function handleInsert () {
  try {
    const valid = await formRef.value.validate()
    if (!valid) {
      ElMessage.warning('请检查表单是否填写!')
      return
    }
    const res = await addWindTurbineConfigApi(insertForm)
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
    turbine_name: '',
    airport_longitude: '',
    airport_latitude: '',
    airport_altitude: '',
    peak_longitude: '',
    peak_latitude: '',
    peak_altitude: '',
    blade_center_height: '',
    blade_length: '',
    uav_blade_distance_stop: '',
    uav_blade_distance_working: '',
  })
  insertDialog.value = true
}

function openEditDialog (row:any) {
  editDialog.value = true
  Object.assign(editForm, row)
}

async function handleEdit () {
  try {
    const valid = await editFormRef.value.validate()
    if (!valid) {
      ElMessage.warning('请检查表单是否填写!')
      return
    }
    const res = await updateWindTurbineConfigApi(editForm)
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
    ElMessageBox.confirm('确定要删除该兴趣点吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(async () => {
        const res = await deleteWindTurbineApi(row.id)
        if (res.code !== 0) {
          return
        }
        ElMessage.success('删除成功!')
        await handleRest()
      })
  } catch (error) {

  }
}

function getWindTurbineConfig () {
  try {
    getAllWindTurbineApi({ ...paginationProp, ...queryForm }).then(res => {
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
  queryForm.turbine_name = ''
  queryForm.id = ''
  getWindTurbineConfig()
}

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
      })
  } catch (error) {

  }
}

async function createPoints (row) {
  try {
    const res = await createWindTurbinePointsApi(row.id)
    if (res.code !== 0) {
      ElMessage.error('点位生成失败!')
      return
    }
    ElMessage.success('点位生成成功!')
  } catch (error) {

  }
}

function showPoints (row) {
  currentId.value = row.id
  pointsDialog.value = true
}

function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  getWindTurbineConfig()
}
function handleCurrentChange (val: number) {
  paginationProp.pageNo = val
  getWindTurbineConfig()
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

    .label {
        height: 60px;
        display: flex;
        align-items: center;
        justify-content: center;
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
        height: 30px;
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
        height: 30px;
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

:deep(el-pager) {
    background-color: #0B2756;
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
