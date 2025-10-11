<template>
    <div class="container">
      <!-- <div class="header1">飞行计划</div> -->
      <div class="operation">
        <!-- 场站/线路选择框 -->
        <span class="label">场站/线路：</span>
        <el-select placeholder="请输入" class="select-operation" :teleported='false'
          style="width: 200px;">
          <el-option label="场站/线路1" value="场站/线路1"></el-option>
          <el-option label="场站/线路2" value="场站/线路2"></el-option>
          <el-option label="场站/线路3" value="场站/线路3"></el-option>
        </el-select>
        <div style="text-align: center;">
                <!-- 自定义按钮，点击触发文件选择 -->
            <el-button @click="triggerFileInput" class="new_btn iconfont icon-chaxunhangxian" type="primary" style="margin-left: 30px; width: 100px;">
              <span style="margin-left: 5px;  font-size: 14px;">
               导入模型
              </span>
            </el-button>
            <!-- 隐藏的文件输入框 -->
            <input type="file" ref="fileInput" webkitdirectory multiple class="file-input" @change="handleFileChange" style="display: none;" />
        </div>
      </div>
      <div class="content">
      <div class="table-container">
        <el-table :data="tableData" stripe>

          <el-table-column label="序号" align='center' width="60">
            <template #default="scope">
              {{ scope.$index + (paginationProp.current - 1) * paginationProp.pageSize + 1 }}
            </template>
          </el-table-column>
          <el-table-column property="model_name" label="模型名称" show-overflow-tooltip="true" />
          <el-table-column property="sub_name" label="变电站" show-overflow-tooltip="true" />
          <el-table-column label="更新时间" prop="update_time">
            <template #default="scope">
              {{ new Date(scope.row.update_time).toLocaleString() }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="330px">
            <template #default="scope">
              <div class="action-buttons">
                <!-- @confirm="deleteWayline(scope.row.id)" -->
                <!-- <el-button size="small" type="text" @click="downloadWayline(scope.row.id, scope.row.name)">查看</el-button> -->
                <el-popconfirm width="220" confirm-button-text="确定" cancel-button-text="不，谢谢" icon-color="#626AEF"
                  title="模型文件一旦删除就无法恢复,是否继续？" @confirm="deleteModelById(scope.row.id)">
                  <template #reference>
                    <el-button size="small" link type="primary" class="delete" >删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <!-- <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange" :current-page="currentPage"
        :page-sizes="[10, 20, 30, 40]" :page-size="pageSize" :total="total"
        layout="total, sizes, prev, pager, next, jumper"></el-pagination> -->
    </div>
    <div class="pagination-container">
      <!-- 分页 -->
      <el-pagination v-model:current-page="paginationProp.current" v-model:page-size="paginationProp.pageSize"
        :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
        layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
        @current-change="handleCurrentChange"></el-pagination>
    </div>
    </div>
</template>
<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TableState } from 'ant-design-vue/lib/table/interface'
import { IPage } from '/@/api/http/type'
import { Task, getFlyPlan, DistributeFlyPlan, deleteFlyPlan } from '/@/api/wayline'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { importModelFile, getModelInfoByName, getAllModels, deleteModel } from '/@/api/model'
import { ELocalStorageKey, ERouterName } from '/@/types'
import { string } from 'mathjs'
const fileInput = ref(null)
interface FileItem {
  uid: string;
  name?: string;
  status?: string;
  response?: string;
  url?: string;
}
interface FileInfo {
  file: FileItem;
  fileList: FileItem[];
}
const fileList = ref<FileItem[]>([])
const loading = ref(false)
onMounted(() => {
  getAllModelsInfo()
})

// ====================================================================文件上传======================================================================
let FileNumber = 0
let uploadNumber = 0
let modelJsonName = ''

async function modelUploadCheck (modelName:string) {
  try {
    const res = await getModelInfoByName(modelName) // 使用 await 来等待异步请求结果
    if (res.code !== 0) {
      return true // 如果返回的 code 不为 0，认为模型名称重复
    }
    if (res.data.length === 0) {
      return false // 如果没有数据，返回 false
    }
    return true // 否则返回 true，表示模型名称重复
  } catch (error) {
    console.error('Error checking model:', error)
    return true // 如果发生错误，默认返回 true
  }
}

/**
 * @description 文件选择事件处理
 * @param event
 */
const handleFileChange = async (event) => {
  // 1. 获取上传的文件列表
  fileList.value = Array.from(event.target.files)
  loading.value = true

  // 2. 统计上传数量，最后一项上传，存放数据库
  FileNumber = fileList.value.length
  uploadNumber = 0
  // 3. 判断是否符合上传格式
  const pathParts = fileList.value[0].webkitRelativePath.split('/')
  const jsonParts = pathParts.filter(part => part.endsWith('.json'))
  if (jsonParts.length === 1) {
    modelJsonName = jsonParts[0]
  } else {
    // 如果不存在或不唯一
    message.warn('上传格式不符合要求!')
    return
  }
  const tree = handleFloder(fileList.value)
  const keys = Object.keys(tree)
  const isExist = await modelUploadCheck(keys[0])
  if (isExist) {
    message.warn('模型名称重复')
    return
  }

  for (const i in tree) {
    if (!(tree[i] instanceof File)) {
      await traverseTree(tree[i], i)
    } else {
      if (tree[i] === tree[tree.length - 1]) {
        await uploadFile(tree[i], i)
      } else {
        await uploadFile(tree[i], i)
      }
    }
  }
}

// 使用按钮包裹input 触发文件选择框
const triggerFileInput = () => {
  fileInput.value.click() // 模拟点击隐藏的文件输入框
}
// 处理目录结构
function handleFloder (fileList) {
  const tree = {}
  fileList.forEach(item => {
    const pathParts = item.webkitRelativePath.split('/')
    let current = tree
    pathParts.forEach((part, pIndex) => {
      if (!current[part]) {
        current[part] = {}
      }
      if (current[item.name]) {
        current[item.name] = item
      }
      current = current[part]
    })
  })
  return tree
}

// 递归遍历树节点
async function traverseTree (node, currentPath = '') {
  if (node instanceof File) {
    await uploadFile(node, currentPath)
  } else {
    for (const i in node) {
      const newPath = `${currentPath}/${i}`
      traverseTree(node[i], newPath)
    }
  }
}

// 上传模型
const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
const uploadFile = async (file, file_path) => {
  const fileData = new FormData()
  fileData.append('file', file, file.name)
  fileData.append('file_path', file_path)
  fileData.append('json_name', modelJsonName)
  uploadNumber = uploadNumber + 1
  if (uploadNumber === FileNumber) {
    fileData.append('isLastFile', 'true')
    await importModelFile(workspaceId, fileData).then((res) => {
      if (res.code === 0) {
        // message.success(`${file.name} 文件上传成功`)
      }
    }).finally(() => {
      nextTick(() => {
        getAllModelsInfo()
      })
      message.success('文件上传成功!')
    })
  } else {
    fileData.append('isLastFile', 'false')
    await importModelFile(workspaceId, fileData).then((res) => {
      if (res.code === 0) {
        // message.success(`${file.name} 文件上传成功`)
      }
    })
  }
}

// =========================================================================增删改查========================================================

const tableData = ref([]) // 点位数据
const body: IPage = {
  page: 1,
  total: 0,
  page_size: 10
}
const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '30', '40'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 10,
  current: 1,
  total: 0
})
type Pagination = TableState['pagination']

/**
 * 分页事件
 * @param val
 */
function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  refreshData(paginationProp)
}

function handleCurrentChange (val: number) {
  paginationProp.current = val
  refreshData(paginationProp)
}

function refreshData (page: Pagination) {
  body.page = page?.current!
  body.page_size = page?.pageSize!
  getAllModelsInfo()
}

/**
 * 请求点位台账
 */
function getAllModelsInfo () {
  getAllModels(body).then(res => {
    if (res.code !== 0) {
      return
    }
    tableData.value = res.data.list
    paginationProp.total = res.data.pagination.total
    paginationProp.current = res.data.pagination.page
    console.log('请求的数据', res.data)
    // message.success('导入成功!')
  })
}

function deleteModelById (id:number) {
  deleteModel(workspaceId, id).then(res => {
    if (res.code !== 0) {
      message.error('删除失败!')
      return
    }
    message.success('删除成功!')
    getAllModelsInfo()
  })
}

</script>

<style lang="scss" scoped>
  .container {
    // height: 100%;
    width: 100vw;
    // padding: 10px;
    display: flex;
    flex-direction: column;
    /* 使子元素垂直排列 */
  }

  .ellipsis {
    white-space: nowrap;
    /* 防止换行 */
    overflow: hidden;
    /* 隐藏超出部分 */
    text-overflow: ellipsis;
    /* 显示省略号 */
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
      // box-shadow: 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
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
    // width: 100%;
    height: 60px;
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
      height: 37px;

      // margin: 12px 0 0 30px;
      .thumbnail_1 {
        width: 12px;
        height: 12px;
        margin: 5px 0 0 12px;
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

    .new_btn1 {
      background-image: linear-gradient(180deg,
          rgba(248, 212, 94, 1) 0,
          rgba(227, 157, 6, 1) 100%);
      border-radius: 4px;
      width: 70px;
      height: 37px;

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

  /* 公共按钮样式 */
  .custom-btn {
    background-color: rgba(51, 122, 255, 0.12);
    height: 28px;
    border: 1px solid rgba(0, 64, 147, 1);
    margin: 7px;
  }

  /* 立即执行按钮 */
  .custom-execute-btn {
    @extend .custom-btn;
    width: 76px;
    margin-left: 7px;
  }

  /* 复制按钮 */
  .custom-copy-btn {
    @extend .custom-btn;
    width: 40px;
    margin-left: 7px;
  }

  /* 编辑按钮 */
  .custom-edit-btn {
    @extend .custom-btn;
    width: 40px;
    margin-left: 7px;
  }

  .custom-delete-btn {
    background-color: rgba(255, 92, 51, 0.19);
    border-radius: 4px;
    height: 28px;
    color: rgba(255, 215, 215, 1);
    border: 1px solid rgba(255, 132, 132, 1);
    width: 40px;
    margin: 7px;
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

  .custom-execute-btn,
  .custom-copy-btn,
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

  :deep(.el-dialog__body) {
    background-color: #0A2D63;
    /* 内容区域背景 */
    color: white;
  }

  :deep(.el-input__wrapper),
  :deep(.el-input) {
    background-color: #123974;
    /* 输入框背景 */
    color: white;
    /* 输入框文字颜色 */
  }

  :deep(.el-input) {
    --el-input-border-color: #1d4292;
  }

  :deep(.el-input__wrapper) {
    background-color: #0B2756;
    box-shadow: 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
  }

  :deep(.el-select__wrapper) {
    background-color: #0B2756;
    box-shadow: 0 0 0 1px #163474 inset;
    color: aliceblue;
  }

  .btn {
    border: 2px solid #1299C3;
    background: linear-gradient(to top, #11B4FB, #023956);
    color: rgba(255, 255, 255, 0.762);
  }

  .btn1 {
    border: 2px solid #ad2d0d;
    background: linear-gradient(to top, #ff1e00, #f32906);
    color: rgba(255, 255, 255, 0.762);
  }

  // 表格 无数据内容背景设置
  :deep(.el-table__empty-block) {
    background-color: #0A2D63;
  }

  // 表格最后一条白线
  :deep .el-table__inner-wrapper::before {
    height: 0;
  }

  .header {
    width: 100%;
    height: 60px;
    background: #fff;
    padding: 16px;
    font-size: 20px;
    font-weight: bold;
    text-align: start;
    color: #000;
  }

  .table-container {
    flex-grow: 1;
    overflow: hidden;
    // height: 500px;
    overflow-y: auto;
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

  /* // 修改高亮当前行颜色 */
  ::v-deep .el-table tbody tr:hover>td {
    background: rgba(0, 114, 245, 0.6) !important;
  }

  /* // 斑马线颜色 */

  ::v-deep .el-table--striped .el-table__body tr.el-table__row--striped td {
    background: rgba(0, 45, 120, 1);
  }

  ::v-deep .el-table td
   {
    border: 2px solid #01123288; /* 设置列的边框颜色和粗细 */
    font-size: 16px;
    font-weight: 500;
  }

  // 表格样式
  ::v-deep .el-table{
     .cell {
      // text-align: center;
      display: flex;
      justify-content: center;
      }
  }
  // // 表头大小
  ::v-deep .el-table th {
    height: 50px;
    font-size: 16px !important; /* 如果你需要修改表头字体大小，设置一个不同的大小 */
    color: rgba(255, 255, 255, 1);
    background-color: #00399A;
    border-left: 2px #01123288 solid;
    border-bottom: 2px #01123288 solid !important;
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
  </style>
