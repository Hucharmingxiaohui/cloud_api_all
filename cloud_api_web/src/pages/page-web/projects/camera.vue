<template>
  <div class="container" ref="containerRef">
    <div class="header">摄像机管理</div>
    <div class="main-box">
      <div class="box-right">
        <!-- 操作按钮 -->
        <div class="operation">
          <div class="item1">
            <el-button class="btn" @click="syn_video">同步视频服务</el-button>
          </div>
          <div class="item1">
            <el-button class="btn" @click="openInsertDialog">新增</el-button>
          </div>
        </div>

        <div class="tablelw1">
          <el-table :row-key="row => row.id" :data="tableData"
            :header-cell-style="{ color: '#fff', fontSize: '16px', backgroundColor: '#003896', borderLeft: '0.5px #154480 solid', borderBottom: '1px #154480 solid' }"
            :cell-style="{ borderBottom: '0.5px #143275 solid', background: '#002D78', borderLeft: '0.5px #143275 solid', color: '#DCDFE5' }">
            <el-table-column prop="id" label="序号" />
            <el-table-column prop="video_name" label="视频名称" />
            <el-table-column prop="belonging_equipment" label="所属设备" />
            <el-table-column prop="camera_type" label="摄像头类型" />
            <el-table-column prop="code18" label="18位编码" />
            <el-table-column label="RTMP地址">
              <template #default>
                --
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template v-slot="scope">
                <el-button size="mini" class="edit-btn" @click="editVideoInfo(scope.row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="pagination-container">
          <el-pagination v-model:current-page="paginationProp.current" v-model:page-size="paginationProp.pageSize"
            :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
            layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
            @current-change="handleCurrentChange"></el-pagination>
        </div>
      </div>
    </div>
  </div>
  <div class="tanchuang">
    <el-dialog v-model="addDialogVisible" title="新增视频通道" width="20%">
      <el-form :model="addForm" label-width="120px">
        <el-form-item label="选择场站" required>
          <!-- <el-input v-model="addForm.point_name" placeholder="请输入点位名称"> @change="updateTempType"</el-input> -->
          <el-select v-model="addForm.sub_code" placeholder="请选择" size="large" class="select-operation"
            :teleported='false' @change="updateSub">
            <el-option v-for="item in subOption" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择设备" required v-if="deviceVisible">
          <!-- <el-input v-model="addForm.point_name" placeholder="请输入点位名称"> @change="updateTempType"</el-input> -->
          <el-select v-model="addForm.device_sn" placeholder="请选择" size="large" class="select-operation"
            :teleported='false' @change="getDeviceInfo">
            <el-option v-for="item in deviceOption" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="视频名称">
          <!-- <el-input v-model="addForm.point_name" placeholder="请输入点位名称"> @change="updateTempType"</el-input> -->
          <el-input v-model="addForm.video_name" placeholder="请输入点位名称"></el-input>
        </el-form-item>
      </el-form>
      <template v-slot:footer>
        <div class="dialog-footer">
          <el-button @click="addDialogVisible = false" class="nobtn">取 消</el-button>
          <el-button type="primary" @click="submitData" class="okbtn">确 定</el-button>
        </div>
      </template>
    </el-dialog>
    <!-- 编辑弹框 -->
    <el-dialog v-model="editDialogVisible" title="编辑视频信息" width="20%">
      <div class="etbitcode">
        <span>视频18位编码</span>
        <span><el-input v-model="editBitCode" maxlength="18"></el-input></span>
      </div>
      <template #footer>
        <el-button @click="editDialogVisible = false" class="nobtn">取消</el-button>
        <el-button type="primary" @click="saveEdit" class="okbtn">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import { getAllVideo, updateVideo, getAllSub, getAllDeviceBySub, insertvideo } from '/@/api/subAndvideo'
import { message } from 'ant-design-vue'
import { ELocalStorageKey } from '/@/types/enums'

// 维护表格的数据

const tableData = ref([])

const loading = ref(false)

// 分页属性
const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '30', '40'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 10,
  current: 1,
  total: 0
})

const height = ref(0)
const containerRef = ref<HTMLElement | null>(null)

// 页面加载时动态计算滚动区域高度
onMounted(() => {
  if (containerRef.value) {
    height.value = window.innerHeight - containerRef.value.offsetHeight
  }
  getVideoInfo()
})

//= ===================================================================功能实现================================================
/**
 * 查询所有的通道信息
 * @param page
 */
function getVideoInfo () {
  getAllVideo().then(res => {
    if (res.code !== 0) {
      return
    }
    tableData.value = res.data
  })
}

/**
 * 打开编辑弹框
 */
const editDialogVisible = ref(false)
const selectedVideoInfo = ref(null)
const editVideoInfo = (row: any) => {
  selectedVideoInfo.value = row
  editBitCode.value = row.code18
  editDialogVisible.value = true // 打开编辑弹框
}

/**
 * 保存编辑后的数据
 */
const editBitCode = ref('')
const saveEdit = () => {
  if (editBitCode.value.length < 18) {
    message.error('输入的编码不足18位')
  } else {
    selectedVideoInfo.value.code18 = editBitCode.value
    updateVideo(selectedVideoInfo.value).then(res => {
      if (res.code !== 0) {
        return
      }
      message.success('修改成功!')
    })
    editDialogVisible.value = false // 关闭弹框
  }
}

/**
 * 打开新增弹框
 */
const addDialogVisible = ref(false)
function openInsertDialog () {
  addDialogVisible.value = true
  getSubInfo()
}

/**
 * 查询场站信息
 */
const addForm = reactive({
  sub_code: '',
  video_name: '',
  workspace_id: '',
  belonging_equipment: '',
  device_sn: '',
  camera_type: '',
  rtmpAddress: ''
})
const subOption = ref([])
function getSubInfo () {
  getAllSub().then(res => {
    if (res.code !== 0) {
      return
    }
    subOption.value = res.data.map(item => ({
      label: item.sub_name,
      value: item.sub_code,
    }))
    addForm.device_sn = ''
  })
}

/**
 * 根据场站id 查询设备信息
 */
const deviceVisible = ref(false)
const deviceOption = ref([])
function updateSub (val: any) {
  getAllDeviceBySub(val).then(res => {
    if (res.code !== 0) {
      return
    }
    deviceVisible.value = true
    deviceOption.value = res.data.map(item => ({
      label: item.device_name,
      value: item.device_sn,
      value1: item.device_type
    }))
  })
}

/**
 * 设备选项更新事件
 */
function getDeviceInfo (val: any) {
  const data = deviceOption.value.find(item => item.value === val)
  addForm.camera_type = data.value1
  addForm.belonging_equipment = data.label
}

/**
 * 根据场站id 查询设备信息
 */
function submitData () {
  if (addForm.sub_code === '' || addForm.device_sn === '') {
    message.error('必填项存在空值')
    return
  }
  addDialogVisible.value = false
  addForm.workspace_id = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
  insertvideo(addForm).then(res => {
    if (res.code !== 0) {
      return
    }
    message.success('添加成功!')
    getVideoInfo()
  })
}
const data = ref([])

// 刷新数据函数，根据分页属性刷新数据
function refreshData (page: any) {
  loading.value = true
  setTimeout(() => {
    data.value = [
      { id: 1, station_name: '烟台芝罘', equip: '大疆机场', camera_type: '可见光', bit_code: '1234567812345678', RTMP_ip: 'rtmp://ip_address' },
      { id: 2, station_name: '华为研究院', equip: '大疆机场', camera_type: '红外', bit_code: '1234567812345688', RTMP_ip: 'rtmp://another_ip' }
    ] // 模拟数据
    paginationProp.total = 50 // 假设总共有 50 条数据
    loading.value = false
  }, 1000)
}

// 分页事件：页码改变时
function handleCurrentChange (val: number) {
  paginationProp.current = val
  refreshData(paginationProp)
}

// 分页事件：每页显示条数改变时
function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  refreshData(paginationProp)
}

// 同步视频服务操作
const syn_video = async () => {
  loading.value = true
  try {
    console.log('同步视频服务...')
    await new Promise(resolve => setTimeout(resolve, 1000))
    console.log('同步完成')
  } catch (error) {
    console.error('同步失败', error)
  } finally {
    loading.value = false
  }
}

// 初始化数据
onMounted(() => {
  refreshData(paginationProp) // 加载数据
})
</script>

<style lang="scss" scoped>
/* 保持原样，不做任何修改 */
.edit-btn {
  background-color: rgba(51, 122, 255, 0.12) !important;
  height: 28px;
  border: 1px solid rgba(0, 64, 147, 1) !important;
  margin: 7px;
  width: 40px;
  color: white;
  margin-left: 7px;
}

// .edit-btn:hover {
//   background-color: #1a67c6;
//   /* 鼠标悬停时的背景颜色 */
//   color: white;
//   /* 悬停时的字体颜色 */
// }

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

.custom-select {
  width: 300px;

  :deep(.el-select--large .el-select__wrapper) {
    min-height: 30px;
  }

  :deep(.custom-select .el-select-dropdown) {
    background-color: transparent !important;
    border: 1px solid blue !important;
  }

  :deep(.custom-select .el-select-dropdown__item) {
    background-color: #05204B !important;
    color: #fff !important;
  }

  :deep(.el-select.is-disabled .el-select__dropdown) {
    background: #05204B;
    color: white;
  }

  :deep(.el-cascader-panel) {
    background: #05204B;
    color: white;
  }
}

.container {
  width: 100%;
  padding: 10px;
  display: flex;
  flex-direction: column;
}

//分页部分================================================================================
.pagination-container {
  position: absolute;
  bottom: 20px;
  /* 距离底部的距离，可调整 */
  left: 50%;
  /* 距离右边的距离，可调整 */
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  align-items: center;
}

::v-deep .el-select__wrapper.el-tooltip__trigger {
  background-color: #0B2756 !important;
  border: 1px solid #154480 !important;
  box-shadow: none;
}

::v-deep .el-pagination button {
  background-color: #0B2756;
  color: white;
}

::v-deep .el-pager li {
  background-color: #0B2756;
  color: white;
}

//弹窗部分===============================================================================
::v-deep .el-input__wrapper {
  background-color: #0B2756;
  color: white;
  border: 1px solid #154480 !important;
  box-shadow: none;
}

::v-deep .el-popper.is-light {
  background: #0B2756;
  border: 1px solid #154480 !important;
}

::v-deep .el-popper.is-light .el-popper__arrow:before {
  background: #0B2756;
  border: 1px solid #154480 !important;
}

::v-deep .el-select-dropdown__item {
  color: white;
}

::v-deep .el-dialog {
  background-color: #0B2756 !important;
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);

}

::v-deep .el-dialog__title {
  color: white !important;
}

::v-deep .el-form-item__label {
  color: white !important;
}

.etbitcode {
  color: white;
}

::v-deep .el-select__placeholder {
  color: white !important;
}

::v-deep .el-input__inner {
  color: white !important;
}

//=========================================================================================
.main-box {
  display: flex;
  flex-grow: 1;
  /* 允许它根据屏幕大小自适应 */
  height: auto;
}

.box-right {
  background: rgba(59, 116, 255, 0.15);
  flex: 1;
  padding: 20px;
  color: white;
  border-radius: 15px;
  border: none;
  overflow: hidden;
  /* 避免内容溢出 */
}

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

.operation {
  height: fit-content;
  padding: 15px;
  display: flex;
  justify-content: space-between;
  color: rgba(255, 255, 255, 0.762);
  background-color: rgba(0, 112, 209, 0.2);
  font-size: 16px;
  border: 4px solid rgba(0, 112, 209, 1);
  border-bottom: none;
  border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;

  .item1 {
    display: flex;
    justify-items: center;
    align-items: center;
  }
}

.el-select-dropdown__item.is-hovering {
  background-color: #00399A;
}

/* // 修改高亮当前行颜色 */
::v-deep .el-table tbody tr:hover>td {
  background: #00399A !important;
}

/* // 斑马线颜色 */

::v-deep .el-table--striped .el-table__body tr.el-table__row--striped td {
  background: rgba(0, 45, 120, 1) !important;
}

.btn {
  background-image: linear-gradient(180deg,
      rgba(70, 145, 217, 1) 0,
      rgba(21, 81, 181, 1) 100%);
  border-radius: 4px;
  width: 108px;
  border: 1px solid #1299C3;
  color: white;
  height: 37px;
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

/* 页面的表格和分页样式保持原样 */
</style>
