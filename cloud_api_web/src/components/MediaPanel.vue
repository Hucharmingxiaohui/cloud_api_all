<template>
  <div class="container">
    <div class="header">媒体文件</div>
    <div class="main-box">
      <div class="box-left">
        <custom-tree :treeData="treeData" @change="handleNodeChange" />
      </div>
      <div class="box-right">
        <div class="operation">
          <div>
            <span>图片名称:</span>
            <el-input v-model="searchValue" placeholder="请输入文件名称"
              style="width: 200px; background-color: black; margin-left: 10px"></el-input>
            <span style="margin-left: 10px;">任务名称:</span>
            <el-input v-model="searchValue" placeholder="请输入任务名称"
              style="width: 200px; background-color: black; margin-left: 10px"></el-input>
          </div>
          <div>
            <a-button type="primary" class="btn" @click="searchData()">搜索</a-button>
          </div>
        </div>
        <a-spin :spinning="loading" :delay="1000" tip="downloading" size="large">
          <div class="tablelw">
            <el-table :data="mediaData.data" :row-key="row => row.id" class="tabelW"
              :header-cell-style="{ color: '#fff', fontSize: '16px', backgroundColor: '#003896', borderLeft: '0.5px #154480 solid', borderBottom: '1px #154480 solid' }"
              :cell-style="{ borderBottom: '0.5px #143275 solid', background: '#002D78', borderLeft: '0.5px #143275 solid', color: '#DCDFE5' }">
              <el-table-column label="文件名称">
                <template #default="scope">
                  <a-tooltip :title="scope.row.file_name">
                    <a @click="showImage(scope.row)">{{ scope.row.file_name }}</a>
                  </a-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="文件路径">
                <template #default="scope">
                  <a-tooltip :title="scope.row.file_path">
                    <span>{{ scope.row.file_path }}</span>
                  </a-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="飞行器">
                <template #default="scope">
                  <a-tooltip :title="scope.row.drone">
                    <span>{{ scope.row.drone }}</span>
                  </a-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="搭载设备">
                <template #default="scope">
                  <a-tooltip :title="scope.row.payload">
                    <span>{{ scope.row.payload }}</span>
                    <!-- <span>{{ scope.row }}</span> -->
                  </a-tooltip>
                </template>
              </el-table-column>

              <el-table-column label="是否原图">
                <template #default="scope">
                  {{ scope.row.is_original }}
                </template>
              </el-table-column>
              <el-table-column label="创建时间">
                <template #default="scope">
                  {{ new Date(scope.row.create_time).toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column label="操作">
                <template #default="scope">
                  <div class="flex-row">
                    <div style="margin-right: 20px;">
                      <el-tooltip content="下载">
                        <span @click="downloadMedia(scope.row)" style="cursor: pointer; color: #409EFF;">
                          <el-icon>
                            <Download />
                          </el-icon>
                        </span>
                      </el-tooltip>
                    </div>
                    <div>
                      <!-- <el-popconfirm width="220" confirm-button-text="确定" cancel-button-text="不，谢谢"
                      icon-color="#626AEF" title="航线文件一旦删除就无法恢复,是否继续？" @confirm="deleteMedia(scope.row)">
                      <template #reference>
                        <el-button size="small" type="text">删除</el-button>
                      </template>
                    </el-popconfirm> -->

                      <el-tooltip content="删除">
                        <span @click="deleteMedia(scope.row)" style="cursor: pointer; color: #409EFF;">
                          <el-icon>
                            <Delete />
                          </el-icon>
                        </span>
                      </el-tooltip>
                    </div>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <!-- 显示图片预览弹窗 -->
            <a-modal v-model:visible="open" width="450px" :closable="true" :maskClosable="false" centered
              :footer="customFooter">
              <a style="width: 100%;height: 100%;"><a-image :src="objectUrl" /></a>
              <template #title>
                <div class="flex-row flex-justify-center">
                  <span>图片预览</span>
                </div>
              </template>
            </a-modal>
          </div>
          <div style="margin-top: 15px">
            <!-- 分页 -->
            <el-pagination v-model:current-page="paginationProp.current" v-model:page-size="paginationProp.pageSize"
              :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
              layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
              @current-change="handleCurrentChange"></el-pagination>
          </div>
        </a-spin>
      </div>
    </div>

    <!-- 弹窗 -->
    <div>
      <el-dialog title="成员信息" v-model="editDialogVisible3" width="30%" class="dialog-color">
        <el-form :model="form3" label-width="120px">
          <el-form-item label="用户名">
            <el-input v-model="form3.username"></el-input>
          </el-form-item>
          <el-form-item label="用户类型">
            <el-radio-group v-model="form3.user_type">
              <el-radio :label="'Web'">管理员</el-radio>
              <el-radio :label="'Pilot'">成员</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="MQTT用户名">
            <el-input v-model="form3.mqtt_username"></el-input>
          </el-form-item>
          <el-form-item label="MQTT密码">
            <el-input v-model="form3.mqtt_password"></el-input>
          </el-form-item>
        </el-form>

        <template #footer>
          <span class="dialog-footer">
            <el-button @click="editDialogVisible3 = false">取 消</el-button>
            <el-button type="primary" @click="saveEdit3()">确 定</el-button>
          </span>
        </template>
      </el-dialog>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref } from '@vue/reactivity'
import { TableState } from 'ant-design-vue/lib/table/interface'
import { onMounted, reactive } from 'vue'
import { IPage } from '../api/http/type'
import { ELocalStorageKey } from '../types/enums'
import { downloadFile } from '../utils/common'
import { downloadMediaFile, getMediaFiles, getOneImage, deleteOneImage } from '/@/api/media'
import { DownloadOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message, Pagination } from 'ant-design-vue'
import { load } from '@amap/amap-jsapi-loader'
import { getDeviceTopo, getUnreadDeviceHms, updateDeviceHms, getPlatformInfo, getAllWorkspaceInfo } from '/@/api/manage'
import CustomTree from '/@/components/substationTree.vue'

const userId = ref(localStorage.getItem(ELocalStorageKey.UserId)!)
let workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
const loading = ref(false)
const searchValue = ref('') // 搜索内容
const columns = [
  {
    title: '文件名称',
    dataIndex: 'file_name',
    ellipsis: true,
    slots: { customRender: 'name' }
  },
  {
    title: '文件路径',
    dataIndex: 'file_path',
    ellipsis: true,
    slots: { customRender: 'path' }
  },
  // {
  //   title: 'FileSize',
  //   dataIndex: 'size',
  // },
  {
    title: '飞行器',
    dataIndex: 'drone'
  },
  {
    title: '搭载设备',
    dataIndex: 'payload'
  },
  {
    title: '原始的',
    dataIndex: 'is_original',
    slots: { customRender: 'original' }
  },
  {
    title: '创建时间',
    dataIndex: 'create_time'
  },
  {
    title: '修改',
    slots: { customRender: 'action' }
  },
]
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

interface MediaFile {
  fingerprint: string,
  drone: string,
  payload: string,
  is_original: string,
  file_name: string,
  file_path: string,
  create_time: string,
  file_id: string,
}

const mediaData = reactive({
  data: [] as MediaFile[]
})

const customFooter = null
onMounted(() => {
  getFiles()
  // 添加树形图数据
  getTreeData()
})
// ---------------------------------------------------------切换工作空间-------------------------------------

const selectedNode = ref(null)

const treeData = ref([
  {
    title: '区域1',
    key: '1',
  }
])

function getTreeData () {
  let workspaces = null
  getAllWorkspaceInfo(userId.value).then(res => {
    // 转换数据格式
    workspaces = res.data
    if (workspaces) {
      clearLeafNodesAndAddData(treeData.value, workspaces)
    }
  })
}
// 添加树形图方法
const clearLeafNodesAndAddData = (treeData, data) => {
  treeData.forEach(node => {
    // 如果有子节点，则递归遍历
    if (node.children && node.children.length > 0) {
      clearLeafNodesAndAddData(node.children, data)
    }

    // 如果是叶子节点，清空现有数据并添加 data 中的数据
    if (!node.children || node.children.length === 0) {
      node.children = data.map(item => ({
        title: item.workspace_name,
        key: item.workspace_id, // 使用 workspace_id 作为 key
        workspace_id: item.workspace_id,
        workspace_desc: item.workspace_desc,
        platform_name: item.platform_name,
        bind_code: item.bind_code,
        isLeaf: true // 显式标记为叶子节点
      }))
    }
  })
  console.log('树形', treeData)
}
// 树形图选中方法
const handleNodeChange = (node) => {
  // selectedNode.value = node

  workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
  getFiles()
  console.log('Node changed in parent:', node) // 确认父组件事件是否触发
}
//= =============================================================================================

function getFiles () {
  getMediaFiles(workspaceId, body).then(res => {
    mediaData.data = res.data.list
    paginationProp.total = res.data.pagination.total
    paginationProp.current = res.data.pagination.page
    // console.info(mediaData.data[0])
  })
}

function refreshData (page: Pagination) {
  body.page = page?.current!
  body.page_size = page?.pageSize!
  getFiles()
}
// 分页事件
function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  refreshData(paginationProp)
}
function handleCurrentChange (val: number) {
  paginationProp.current = val
  refreshData(paginationProp)
}

// function refreshData1 () {
//   getFiles()
// }
function downloadMedia (media: MediaFile) {
  loading.value = true
  downloadMediaFile(workspaceId, media.file_id).then(res => {
    if (!res) {
      return
    }
    const data = new Blob([res])
    downloadFile(data, media.file_name)
  }).finally(() => {
    loading.value = false
  })
}

// 搜索图片
function searchData () {
  // console.log('ceshi', searchValue.value)
  getOneImage(workspaceId, searchValue.value, body).then(res => {
    mediaData.data = res.data.list
    paginationProp.total = res.data.pagination.total
    paginationProp.current = res.data.pagination.page
    // console.info(mediaData.data[0])
  })
}
// 删除图片

function deleteMedia (media: MediaFile) {
  // console.log(media.file_id)
  deleteOneImage(workspaceId, media.file_id).then(res => {
    if (!res) {
      return
    }
    getFiles()
    console.info('删除成功!')
  })
}
// 预览图片

const objectUrl = ref('')
const open = ref(false) // 弹窗
function showImage (media: MediaFile) {
  downloadMediaFile(workspaceId, media.file_id).then(res => {
    if (!res) {
      return
    }
    open.value = true
    const data = new Blob([res])
    objectUrl.value = URL.createObjectURL(data)
    // console.log('ceshi222', objectUrl.value)
    // console.log(open.value)
  })
}

</script>

<style lang="scss" scoped>
.media-panel-wrapper {
  width: 100%;
  padding: 16px;

  .media-table {
    background: #fff;
    margin-top: 10px;
  }

  .action-area {
    color: $primary;
    cursor: pointer;
  }
}

.container {
  // height: 100%;
  width: 100%;
  padding: 10px;
  display: flex;
  flex-direction: column;
  /* 使子元素垂直排列 */
}

.main-box {
  display: flex;
  /* 使用 flexbox 布局 */
  height: 100vh;
  /* 设置容器高度为视口高度 */
}

.box-left {
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  width: 20%;
  /* 左侧占据 20% 的宽度 */
  // background-color: #4CAF50; /* 绿色背景 */
  padding: 20px;
  /* 内边距 */
  color: white;
  /* 字体颜色 */
  // transition: 0.5s;
  border-radius: 15px;
  border: none;
  height: 85vh;
}

// .box-left:hover {
//   box-shadow: inset 0px 0px 20px 3px rgba(34, 135, 255, 0.7);
// }
.box-right {
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  flex: 1;
  /* 右侧占据剩余空间 */
  width: calc(80% - 10px);
  margin-left: 10px;
  // background-color: #2196F3; /* 蓝色背景 */
  padding: 20px;
  /* 内边距 */
  color: white;
  /* 字体颜色 */
  border-radius: 15px;
  border: none;
  height: 85vh;
}

// .box-right:hover {
//   box-shadow: inset 0px 0px 20px 3px rgba(34, 135, 255, 0.7);
// }
// 头部  标题 面包屑
.header {
  width: 100%;
  height: 60px;
  background: #05204B;
  padding: 16px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color: aliceblue;
  // border-bottom: 4px solid rgba(0, 112, 209, 1);
  // border-image: linear-gradient(120deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
}

.operation {
  height: fit-content;
  padding: 15px;
  display: flex;
  justify-content: space-between;
  color: rgba(255, 255, 255, 0.762);
  background-color: rgba(0, 112, 209, 0.2);
  font-size: 16px;
  //  border: 4px solid rgba(0, 112, 209, 1);
  //  border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
  border-bottom: none;

  //  border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
  .item1 {
    display: flex;
    justify-items: center;
    align-items: center;
  }
}

// 按钮
.btn {
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color: rgba(255, 255, 255, 0.762);
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

// 输入框
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

// 表格
// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #0A2D63;
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

.custom-table {
  --header-bg-color: #003896;
  --header-text-color: #fff;
  --row-bg-color: #002D78;
  --row-text-color: white;
}

.custom-table::v-deep .ant-table-thead>tr>th {
  background-color: var(--header-bg-color) !important;
  color: var(--header-text-color) !important;
  font-weight: bold !important;
}

.custom-table::v-deep .ant-table-tbody>tr>td {
  background-color: var(--row-bg-color) !important;
  color: var(--row-text-color) !important;
}

// 表格最后一条白线
:deep .el-table__inner-wrapper::before {
  height: 0;
}

::v-deep .home-ant-input.ant-input-affix-wrapper .ant-input {
  background-color: black;
  color: #c5c8cc;
}

.tablelw {
  max-height: 600px;
  overflow-y: auto
}
</style>
