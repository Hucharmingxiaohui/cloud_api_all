<template>
    <div style="width: 100%; height: 100%;">
        <el-table :data="pointData" stripe height="600">
         <el-table-column type="index" align="center" label="序号" width="60"/>
         <el-table-column label="场站名称" align="center" prop="station_name"></el-table-column>
         <el-table-column label="区域名称" align="center" prop="area_name"></el-table-column>
         <el-table-column label="间隔名称" align="center" prop="bay_name"></el-table-column>
         <el-table-column label="主设备名称" align="center" prop="main_device_name"></el-table-column>
         <el-table-column label="组件名称" align="center" prop="component_name"></el-table-column>
         <el-table-column label="点位名称" align="center" prop="point_name"></el-table-column>
        </el-table>
        <el-pagination
          style="margin-top: 15px;"
          v-model:current-page="body.pageNo"
          v-model:page-size="body.pageSize"
          :page-sizes="body.pageSizeOptions"
          :total="body.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        >
        </el-pagination>
    </div>
</template>
<script setup>
import { reactive, defineModel, ref, computed, onMounted } from 'vue'
import { ElButton, ElDialog, ElForm, ElFormItem, ElMessageBox, ElInput, ElSelect, ElOption, ElUpload, ElMessage } from 'element-plus'
import { getWindTurbinePointsApi } from '/@/api/turbine/turbineMgt'

const id = defineModel('id')
const pointData = ref([])
const body = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
  pageSizeOptions: [10, 20, 40, 100]
})
onMounted(() => {
  getPoints()
})

// 获取站点信息
async function getPoints () {
  try {
    if (!id.value) return
    const res = await getWindTurbinePointsApi({ id: id.value, ...body })
    if (res.code !== 0) {
      ElMessage.error('获取站点信息失败!')
    }
    pointData.value = res.data.list
    body.total = res.data.pagination.total
  } catch (error) {

  }
}

</script>
<style lang="scss" scoped>
:deep(.el-scrollbar__wrap--hidden-default){
    // margin: 5px;
    background-color: #0B2757;
}

</style>
