<template>
  <div class="container">
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="正射图名称:" prop="solar_area_name">
          <el-input
            v-model="queryForm.solar_area_name"
            placeholder="请输入正射图名称"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item label="正射图ID:" prop="id">
          <el-input
            v-model="queryForm.id"
            placeholder="请输入正射图ID"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Search"
            @click="getSoloarImgList()"
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
            :icon="Plus"
            type="primary"
            @click="openImportDialog()"
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
          <el-table-column label="正射图ID" align="center" prop="idid">
            <template #default="scope" >
              <div class="ellipsis">{{ scope.row.id }}</div>
            </template>
          </el-table-column>
          <el-table-column label="正射图名称" align="center">
            <template #default="scope">
              {{ scope.row.name }}
            </template>
          </el-table-column>
          <el-table-column label="光伏板数量" align="center">
            <template #default="scope">
              {{ scope.row.solar_panel_total }}
            </template>
          </el-table-column>
          <el-table-column label="光伏板组件数量" align="center">
            <template #default="scope">
              {{ scope.row.component_total }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300">
            <template #default="scope">
              <el-button link type="primary" @click="openEditDialog(scope.row)" v-if="scope.row.component_list && scope.row.component_list.length>0">重新检测</el-button>
              <el-button link type="primary" @click="openEditDialog(scope.row)" v-else>检测</el-button>
              <el-button link type="primary" @click="openDetailDialog(scope.row)" v-if="scope.row.component_list && scope.row.component_list.length>0">详情</el-button>
              <el-button link type="primary" @click="createPoints(scope.row)">生成点位</el-button>
              <el-button link type="primary" @click="showPoints(scope.row)">查看点位</el-button>
              <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
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

    <SolarComponentDetail
      v-model="detailDialogVisible"
      :row-data="currentRowData"
      @close="detailDialogVisible = false"
    />

    <el-dialog v-model="pointsDialogVisible" title="点位详情">
      <div>
        <PointsDetail :id="currentPointOrthophotoId"></PointsDetail>
      </div>
    </el-dialog>

    <!-- 导入正射图弹窗 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入正射图"
      width="500px"
      style="background-color: #0A2D63; color: white"
    >
      <el-form :model="importForm" label-width="100px">
        <el-form-item label="图片名称" required>
          <el-input
            v-model="importForm.name"
            placeholder="请输入图片名称"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="上传图片" required>
          <el-upload
            ref="uploadRef"
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            accept="image/*"
          >
            <el-button type="primary">选择图片</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 JPG/PNG 格式图片
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmImport" :loading="importing">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElButton, ElInput, ElMessage, ElMessageBox } from 'element-plus'
import { getOrthophotoListApi, deleteSolarImgByIdApi, detecSolarImgByIdApi, importSolarPanelImgApi, createSolarPanelPointsApi } from '/@/api/turbine/turbineMgt'
import SolarComponentDetail from './SolarComponentDetail.vue'
import PointsDetail from '../solarPanelMgt/pointsDetail.vue'

const queryForm = reactive({
  solar_area_name: '',
  id: ''
})
const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '40'],
  pageSize: 10,
  pageNo: 1,
  total: 0
})

const tableData = ref([])

const detailDialogVisible = ref(false)
const currentRowData = ref<any>({})
const pointsDialogVisible = ref(false)
const currentPointOrthophotoId = ref('')

async function openEditDialog (row: any) {
  try {
    const res = await detecSolarImgByIdApi({
      orthophoto_id: row.id,
      solar_area_name: row.name
    })
    if (res.code !== 0) {
      return
    }
    ElMessage.success('检测成功!')
    getSoloarImgList()
  } catch (error) {

  }
}

function openDetailDialog (row: any) {
  currentRowData.value = JSON.parse(JSON.stringify(row))
  detailDialogVisible.value = true
}

async function createPoints (row: any) {
  if (!row.id) {
    ElMessage.warning('当前正射图ID为空，无法生成点位')
    return
  }
  try {
    const res = await createSolarPanelPointsApi(row.id)
    if (res.code !== 0) {
      ElMessage.error('生成点位失败')
      return
    }
    ElMessage.success('生成点位成功')
  } catch (error) {
    ElMessage.error('生成点位失败')
  }
}

function showPoints (row: any) {
  if (!row.id) {
    ElMessage.warning('当前正射图ID为空，无法查看点位')
    return
  }
  currentPointOrthophotoId.value = row.id
  pointsDialogVisible.value = true
}

async function handleDelete (row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除正射图 "${row.name}" 吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await deleteSolarImgByIdApi(row.id)
    if (res.code !== 0) {
      return
    }
    ElMessage.success('删除成功')
    getSoloarImgList()
  } catch (error) {
    // 用户取消
  }
}

// ---------- 导入正射图 ----------
const importDialogVisible = ref(false)
const importing = ref(false)
const importForm = reactive({ name: '' })
const importFile = ref<File | null>(null)
const uploadRef = ref()

function openImportDialog () {
  importForm.name = ''
  importFile.value = null
  importDialogVisible.value = true
}

function handleFileChange (file: any) {
  importFile.value = file.raw
}

// 新增/导入
async function confirmImport () {
  if (!importForm.name.trim()) {
    ElMessage.warning('请输入图片名称')
    return
  }
  if (!importFile.value) {
    ElMessage.warning('请选择图片')
    return
  }

  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    formData.append('name', importForm.name.trim())

    const res = await importSolarPanelImgApi(formData)
    if (res.code === 0) {
      ElMessage.success('导入成功')
      importDialogVisible.value = false
      getSoloarImgList()
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error('导入失败，请稍后重试')
  } finally {
    importing.value = false
    importForm.name = ''
    importFile.value = null
    if (uploadRef.value) {
      uploadRef.value.clearFiles()
    }
  }
}
// ---------- 导入结束 ----------

onMounted(() => {
  getSoloarImgList()
})

function getSoloarImgList () {
  try {
    getOrthophotoListApi({ ...paginationProp, ...queryForm }).then(res => {
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
  queryForm.solar_area_name = ''
  queryForm.id = ''
  getSoloarImgList()
}

function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  getSoloarImgList()
}

function handleCurrentChange (val: number) {
  paginationProp.pageNo = val
  getSoloarImgList()
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

:deep(.el-table__header-wrapper tr th) {
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

:deep(.el-input__inner) {
  color: white;
}
:deep(.el-form-item__label) {
  color: white;
}

:deep(.el-dialog__title) {
  font-size: 18px;
  color: white;
}
:deep(.el-select__wrapper) {
    background-color: #0B2756;
    box-shadow: 0 0 0 1px #163474 inset;
    color: aliceblue;
}

</style>
