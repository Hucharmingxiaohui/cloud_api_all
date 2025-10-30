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
            style="width: 70px;"
            :icon="Search"
            @click="getWindTurbineConfig()"
          >
            <span style="margin-left: 5px; font-size: 14px;">查询</span>
          </el-button>
          <el-button
            class="new_btn"
            type="primary"
            style="width: 70px;"
            :icon="Refresh"
            @click="handleRest()"
          >
            <span style="margin-left: 5px; font-size: 14px;">重置</span>
          </el-button>
          <el-button
            class="new_btn"
            type="primary"
            style="width: 70px;"
            :icon="Plus"
            @click="openInsertDialog()"
          >
            <span style="margin-left: 5px; font-size: 14px;">新增</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="content">
      <div class="table-container">
        <el-table :data="tableData" stripe>
          <el-table-column type="index"  align="center" label="序号" width="60" />
          <el-table-column label="风机名称" style="max-width: 100px;">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.turbine_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="机场经度" width="150">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.airport_longitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="机场纬度" width="150">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.airport_latitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="机场海拔(米)" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.airport_altitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="正对航向角" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.approach_yaw }}</div>
            </template>
          </el-table-column>
          <el-table-column label="最高点经度" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.peak_longitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="最高点纬度" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.peak_latitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="最高点海拔(米)" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.peak_altitude }}</div>
            </template>
          </el-table-column>
          <el-table-column label="叶片中心高度(米)" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.blade_center_height }}</div>
            </template>
          </el-table-column>
          <el-table-column label="停机叶片夹角" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.blade_stop_angle }}</div>
            </template>
          </el-table-column>
          <el-table-column label="叶片长度(米)" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.blade_length }}</div>
            </template>
          </el-table-column>
          <el-table-column label="无人机距离(米)" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.uav_blade_distance }}</div>
            </template>
          </el-table-column>
          <el-table-column label="风机底部高度(米)" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.blade_bottom_height }}</div>
            </template>
          </el-table-column>
          <el-table-column label="单个扇叶的点数" width="100">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.blade_points }}</div>
            </template>
          </el-table-column>
          <el-table-column label="塔筒的点数" width="100">
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
              <el-button link type="primary" @click="handleTask(scope.row)"
                >执行任务</el-button
              >
            </template>
          </el-table-column>
        </el-table>
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
            <el-form-item label="正对航向角" prop="approach_yaw" required>
              <el-input
                v-model="insertForm.approach_yaw"
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

          <!-- 第二列 -->
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
            <el-form-item
              label="停机时叶片1夹角"
              prop="blade_stop_angle"
              required
            >
              <el-input
                v-model="insertForm.blade_stop_angle"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="单个叶片长度(米)" prop="blade_length" required>
              <el-input
                v-model="insertForm.blade_length"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="无人机距离页面距离(米)"
              prop="uav_blade_distance"
              required
            >
              <el-input
                v-model="insertForm.uav_blade_distance"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="塔筒的点数" prop="uav_blade_distance" required>
              <el-input
                v-model="insertForm.tower_points"
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
            <el-form-item label="正对航向角" prop="approach_yaw" required>
              <el-input
                v-model="editForm.approach_yaw"
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
          <!-- 第二列 -->
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
            <el-form-item
              label="停机时叶片1夹角"
              prop="blade_stop_angle"
              required
            >
              <el-input
                v-model="editForm.blade_stop_angle"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="单个叶片长度(米)" prop="blade_length" required>
              <el-input
                v-model="editForm.blade_length"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item
              label="无人机距离页面距离(米)"
              prop="uav_blade_distance"
              required
            >
              <el-input
                v-model="editForm.uav_blade_distance"
                maxlength="50"
              ></el-input>
            </el-form-item>
            <el-form-item label="塔筒的点数" prop="uav_blade_distance" required>
              <el-input
                v-model="editForm.tower_points"
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
// import { TableState } from 'ant-design-vue/lib/table/interface'
// import { IPage } from '/@/api/http/type'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElButton, ElDialog, ElForm, ElFormItem, ElMessageBox, ElInput, ElSelect, ElOption, ElUpload, ElMessage } from 'element-plus'
import { addWindTurbineConfigApi, executeFlyTaskApi, getAllWindTurbineApi, updateWindTurbineConfigApi, deleteWindTurbineApi } from '/@/api/turbine/turbineMgt'
// import * as XLSX from 'xlsx'
// import { message } from 'ant-design-vue'

// 表单
const queryForm = reactive({
  turbine_name: '',
  id: ''
})
const formRef = ref(null)
const editFormRef = ref(null)

const paginationProp = reactive({
  pageSize: 10,
  pageNo: 1,
  total: 0
})

const insertForm = reactive({
  turbine_name: '',
  airport_longitude: '',
  airport_latitude: '',
  airport_altitude: '',
  approach_yaw: '',
  peak_longitude: '',
  peak_latitude: '',
  peak_altitude: '',
  blade_center_height: '',
  blade_stop_angle: '',
  blade_length: '',
  uav_blade_distance: '',
  blade_bottom_height: '',
  blade_points: '',
  tower_points: ''

})
const editForm = reactive({
  turbine_name: '',
  airport_longitude: '',
  airport_latitude: '',
  airport_altitude: '',
  approach_yaw: '',
  peak_longitude: '',
  peak_latitude: '',
  peak_altitude: '',
  blade_center_height: '',
  blade_stop_angle: '',
  blade_length: '',
  uav_blade_distance: '',
  blade_bottom_height: '',
  blade_points: '',
  tower_points: ''
})

// 表格
const tableData = ref([])

// 弹窗
const insertDialog = ref(false)
const editDialog = ref(false)

// 表单规则
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
  approach_yaw: [
    { required: true, message: '请输入正对航向角', trigger: 'blur' },
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
  blade_stop_angle: [
    { required: true, message: '请输入停机时叶片1夹角', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  blade_length: [
    { required: true, message: '请输入单个叶片长度', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  uav_blade_distance: [
    { required: true, message: '请输入无人机距离页面距离', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  blade_bottom_height: [
    { required: true, message: '请输入无人机距离页面距离', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  blade_points: [
    { required: true, message: '请输入无人机距离页面距离', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ],
  tower_points: [
    { required: true, message: '请输入无人机距离页面距离', trigger: 'blur' },
    { type: 'number', message: '必须为数字值', trigger: 'blur', transform: value => Number(value) }
  ]
}

onMounted(() => {
  getWindTurbineConfig()
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
    const res = await addWindTurbineConfigApi(insertForm)
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
    turbine_name: '',
    airport_longitude: '',
    airport_latitude: '',
    airport_altitude: '',
    approach_yaw: '',
    peak_longitude: '',
    peak_latitude: '',
    peak_altitude: '',
    blade_center_height: '',
    blade_stop_angle: '',
    blade_length: '',
    uav_blade_distance: ''
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
    const res = await updateWindTurbineConfigApi(editForm)
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
    const res = await deleteWindTurbineApi(row.id)
    if (res.code !== 0) {
      return
    }
    console.log('ssssss1')
    ElMessage.success('删除成功!')
    await handleRest()
  } catch (error) {

  }
}
// 获取风机信息查询
function getWindTurbineConfig () {
  try {
    getAllWindTurbineApi({ ...paginationProp, ...queryForm }).then(res => {
      if (res.code !== 0) {
        return
      }
      tableData.value = res.data
    })
  } catch (error) {

  }
}

// 重置查询
function handleRest () {
  queryForm.turbine_name = ''
  queryForm.id = ''
  getWindTurbineConfig()
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
    height: 65vh;
    overflow-y: auto;
}

.ellipsis {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 5; /* 限制显示行数 */
  overflow: hidden;
  text-overflow: ellipsis;
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
    height: 60px;
    padding-top: 15px;
    padding-left: 10px;
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
        height: 30px;

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

// 固定列表头
:deep(.el-table__header-wrapper tr th.el-table-fixed-column--right){
  background-color: #00399A;
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

:deep(.el-form-item__label) {
    color: white;
}

::v-deep .el-input__inner {
    color: white;
}
 .el-input__inner {
    color: white;
}
</style>
