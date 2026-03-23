<template>
  <div class="container">
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="点位名称:" prop="pointName">
          <el-input
            v-model="queryForm.pointName"
            placeholder="请输入点位名称"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item label="点位ID:" prop="id">
          <el-input
            v-model="queryForm.id"
            placeholder="请输入点位ID"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Search"
            @click="getPoinntList()"
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
            class="new_btn2"
            type="danger"
            :icon="Delete"
            @click="handleDelete()"
          >
            删除
          </el-button>
          <el-upload
            :before-upload="beforeUpload"
            :show-file-list="false"
            :http-request="uploadFile"
            style="display: inline-block;"
            ref="upload"
          >
            <el-button
              class="new_btn"
              type="primary"
              :icon="Upload"
              style="margin:2px 10px 0px 10px"
            >
             导入点位
            </el-button>
          </el-upload>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Download"
            @click="downloadTemplate()"
          >
            下载模板
          </el-button>

        </el-form-item>
      </el-form>
    </div>
    <div class="content">
      <div class="table-container">
        <el-table :data="tableData" stripe  @selection-change="handleSelectionChange">
          <el-table-column type="selection"  align="center" width="60"></el-table-column>
          <el-table-column
            type="index"
            align="center"
            label="序号"
            width="60"
          />
          <el-table-column label="点位名称" prop="point_name" align="center">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.point_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="变电站名称" prop="sub_name" align="center">
          </el-table-column>
          <el-table-column label="区域名称" prop="area_name" align="center">
          </el-table-column>
          <el-table-column label="间隔名称" prop="bay_name" align="center"></el-table-column>
          <el-table-column label="主设备名称" prop="device_name" align="center"></el-table-column>
          <el-table-column label="部件名称" prop="component_name" align="center"></el-table-column>
          <el-table-column label="关联航线" prop="wayline_id" align="center"></el-table-column>
          <el-table-column label="关联航点号" prop="wayline_point_pos" align="center"></el-table-column>
          <el-table-column label="图片类型" prop="pic_type" align="center">
            <template #default="scope">
              {{ scope.row.pic_type === 0? '可见光':'红外' }}
            </template>
          </el-table-column>

          <!-- <el-table-column label="操作" width="150">
            <template #default="scope">
              <el-button link type="danger" @click="handleDelete(scope.row)"
                >删除</el-button
              >
            </template>
          </el-table-column> -->
        </el-table>
      </div>
      <div class="pagination-container">
        <!-- 分页 -->
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
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { Search, Refresh, Plus, Delete, Upload, Download } from '@element-plus/icons-vue'
import { downloadFile } from '/@/utils/common'
import { ElButton, ElDialog, ElForm, ElFormItem, ElMessageBox, ElInput, ElSelect, ElOption, ElUpload, ElMessage } from 'element-plus'
import { getPointList, deletePointListapi, importPointList, exportPointTemplate } from '/@/api/points'

// 表单
const queryForm = reactive({
  pointName: '',
  id: '',
  picType: '',
  waylineId: ''
})

const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '40'],
  pageSize: 10,
  pageNo: 1,
  total: 0
})

// 导入点位
interface FileItem {
  uid: string;
  name?: string;
  status?: string;
  response?: string;
  url?: string;
}

const fileList = ref<FileItem[]>([])
// 表格
const tableData = ref([])

const selectRows = ref([])

onMounted(() => {
  getPoinntList()
})

// 获取风机信息查询
function getPoinntList () {
  try {
    getPointList({ ...paginationProp, ...queryForm }).then(res => {
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
  queryForm.pointName = ''
  queryForm.id = ''
  getPoinntList()
}

/**
 * 导出点位填报模板
 */
async function downloadTemplate () {
  try {
    const res = await exportPointTemplate()
    if (!res) {
      ElMessage.error('系统异常,下载报告失败!')
      return
    }
    const data = new Blob([res])
    downloadFile(data, '点位模板.xlsx')
  } catch (error) {

  }
}

/**
 * 导入点位
 */

function beforeUpload (file: FileItem) {
  fileList.value = [file]
  return true
}

const uploadFile = async () => {
  fileList.value.forEach(async (file: FileItem) => {
    const fileData = new FormData()
    fileData.append('file', file, file.name)
    await importPointList(fileData).then((res) => {
      ElMessage.success('导入成功!')
    }).finally(() => {
      fileList.value = []
      getPoinntList()
    })
  })
}

/**
 * 删除点位
 */

function handleSelectionChange (val) {
  selectRows.value = val.map(item => item.id)
}

async function handleDelete () {
  try {
    const obj = { ids: selectRows.value }
    if (selectRows.value.length === 0) {
      ElMessage.warning('请选择要删除的数据！')
      return
    }
    ElMessageBox.confirm('确定要删除点位吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(async () => {
        const res = await deletePointListapi(obj)
        if (res.code !== 0) {
          return
        }
        ElMessage.success('删除成功!')
        await getPoinntList()
      })
  } catch (error) {

  }
}

/**
 * 分页查询
 */

function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  getPoinntList()
}
function handleCurrentChange (val: number) {
  paginationProp.pageNo = val
  getPoinntList()
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

    .new_btn2 {
        background-image: linear-gradient(180deg,
                rgb(246, 164, 132) 0,
                rgb(190, 64, 22) 100%);
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
    background-color: #124AAD !important; // 修改默认的背景色
    color: #fff;
}
:deep(.el-pager li.is-active){
  color:#11B4FB !important;
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
</style>
