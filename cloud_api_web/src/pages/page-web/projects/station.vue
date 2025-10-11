<template>
  <div class="container">
    <div class="operation">
      <span class="label">成员名称:</span>
      <el-input v-model="search1" placeholder="请输入场站名称" class="custom-select" style="width: 200px;"></el-input>
      <el-button class="new_btn iconfont icon-chaxunhangxian" type="primary" style="margin-left: 30px; width: 70px;"
        @click="load1(body)">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../../assets/v4/search.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">查询</span>
      </el-button>
      <el-button class="new_btn iconfont icon-tianjiazhandian" type="primary" style="margin-left: 10px" @click="add1">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer"
                    src="https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG8c71ebd00fa5c3751c60f6beabe74d4f.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">添加场站</span>
      </el-button>
    </div>
    <div class="content">
      <div class="table-container">
        <el-table :data="StationOption" stripe>
          <el-table-column label="序号" align='center' width="60">
            <template #default="scope">
              {{ scope.$index + (currentPage1 - 1) * pageSize1 + 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="sub_name" label="场站名"></el-table-column>
          <el-table-column prop="user_type" label="场站地址">
            <template #default="scope">
              <span>{{ scope.row.addr }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="create_time" label="创建时间"></el-table-column>
          <el-table-column label="编辑" width="150">
            <template #default="scope">
              <el-button size="mini" type="text" @click="handleEdit1(scope.row)">编辑</el-button>
              <el-popconfirm width="220" confirm-button-text="确定" cancel-button-text="不，谢谢" :icon="InfoFilled"
                icon-color="#626AEF" title="你确定要删除吗" @confirm="handleDelete1(scope.row.id)">
                <template #reference>
                  <el-button size="small" type="text">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <!--                    分页功能-->
        <div class="pagination-container">
          <!-- 分页 -->
          <el-pagination v-model:current-page="currentPage1" v-model:page-size="pageSize1" :page-sizes="[10, 20, 30, 40]"
            :total="total1" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange2"
            @current-change="handleCurrentChange2">
          </el-pagination>
        </div>
      </div>
    </div>
  </div>
  <div>
    <el-dialog title="场站信息" v-model="editDialogVisible2" width="30%" class="dialog-color">
      <el-form :model="form2" label-width="120px">
        <el-form-item label="变电站名称" required>
          <el-input v-model="form2.sub_name"></el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible2 = false" class="nobtn">取 消</el-button>
          <el-button type="primary" @click="saveEdit2()" class="okbtn">确 定</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
  <!-- 添加一个成员的弹窗 -->
  <div>
    <el-dialog title="场站信息" v-model="editDialogVisible3" width="30%" class="dialog-color">
      <el-form :model="form3" label-width="120px" required>
        <el-form-item label="变电站名称">
          <el-input v-model="form3.sub_name"></el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible3 = false" class="nobtn">取 消</el-button>
          <el-button type="primary" @click="saveEdit3()" class="okbtn">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, UnwrapRef } from 'vue'
import {
  getAllWorkspaceInfo, updateWorksoaceInfo, deleteWorkspaceInfo,
  getWorkspaceByName, createWorkspace, getPlatformInfo, getAllUsersInfo,
  updateUserInfo, deleteUser, addUser, getOneUser, addSub, deleteSub, updateSub
} from '/@/api/manage'
import { message, PaginationProps } from 'ant-design-vue'
import { ELocalStorageKey, ERouterName } from '/@/types'
import { localeContextKey, ElMessageBox, ElMessage } from 'element-plus'
import { IPage } from '/@/api/http/type'
import { insertTEMPConfig, getAllSub, PointData, getAllPoints } from '/@/api/points'

// 定义接口类型
interface Workspace {
  id: number;
  workspace_id: string;
  workspace_name: string;
  workspace_desc: string;
  platform_name: string;
  bind_code: string;
}

const tableData = ref<Workspace[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showMember = ref(false)
const member = ref('成员管理')

onMounted(() => {
  getSubInfo()
})

/**
 * @description: 查询所有的场站
 * @param {string} waylineInfo 航线信息
 * */
const StationOption = ref([])
function getSubInfo () {
  getAllSub().then(res => {
    if (res.code !== 0) {
      return
    }
    StationOption.value = res.data
    // console.log('场站信息', res.data)
    // StationOption.value = res.data.map(item => ({
    //   value: item.sub_code,
    //   label: item.sub_name
    // }))
  })
}
const currentPage1 = ref(1)
const pageSize1 = ref(10)
const total1 = ref(0)
const body: IPage = {
  page: 1,
  total: 0,
  page_size: 10
}

// 定义添加场站成员信息表单
const form2 = reactive({
  id: '',
  sub_name: '',
  workspace_id: '',
})

const editDialogVisible2 = ref(false)
// 打开修改表单弹窗
function handleEdit1 (row: any) {
  // 打开编辑对话框
  editDialogVisible2.value = true
  // 将当前行的数据赋值给编辑表单
  // form2.value = { ...row }
  form2.id = row.id
  form2.sub_name = row.sub_name
  form2.workspace_id = row.workspace_id
}

// 编辑数据
function saveEdit2 () {
  if (!validateForm1()) {
    return // 如果验证未通过，直接返回
  }
  updateSub(form2).then(res => {
    if (res.code !== 0) {
      message.error(res.message)
    } else {
      // 关闭编辑对话框
      editDialogVisible2.value = false

      // 重新请求数据
      getSubInfo()
    }
  })
}
// 删除场站数据
function handleDelete1 (sub_id: string) {
  // deleteSub
  deleteSub(sub_id).then(res => {
    if (res.code !== 0) {
      message.error(res.message)
    } else {
      // 关闭编辑对话框
      // editDialogVisible2.value = false
      getSubInfo()
      // 重新请求数据
    }
  })
}

// 定义添加场站成员信息表单
const form3 = reactive({
  sub_name: '',
  workspace_id: '',
})

function validateForm () {
  const requiredFields = [
    { value: form3.sub_name, label: '变电站名称' },
  ]

  for (const field of requiredFields) {
    if (!field.value || field.value.trim() === '') {
      ElMessage.error(`${field.label} 不能为空`)
      return false
    }
  }
  return true
}

function validateForm1 () {
  const requiredFields = [
    { value: form2.sub_name, label: '变电站名称' },
  ]

  for (const field of requiredFields) {
    if (!field.value || field.value.trim() === '') {
      ElMessage.error(`${field.label} 不能为空`)
      return false
    }
  }
  return true
}
const editDialogVisible3 = ref(false)
// 添加用户成员信息
function add1 () {
  editDialogVisible3.value = true
}
function saveEdit3 () {
  if (!validateForm()) {
    return // 如果验证未通过，直接返回
  }
  form3.workspace_id = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
  addSub(form3).then(res => {
    if (res.code !== 0) {
      message.error(res.message)
    } else {
      // 关闭编辑对话框
      editDialogVisible3.value = false

      // 重新请求数据
      getSubInfo()
    }
  })
}
// 搜索用户
const search1 = ref('')
function load1 (page: IPage) {
  // getOneUser(workspaceId, search1.value, page).then(res => {
  //   const userList: Member[] = res.data.list
  //   userdata.value = userList
  //   total1.value = res.data.pagination.total
  //   currentPage1.value = res.data.pagination.page
  // })
}
function handleSizeChange2 (val: number) {
  pageSize1.value = val
  const body: IPage = {
    page: currentPage1.value,
    total: 0,
    page_size: pageSize1.value
  }

  load1(body)
}
function handleCurrentChange2 (val: number) {
  currentPage1.value = val
  const body: IPage = {
    page: currentPage1.value,
    total: 0,
    page_size: pageSize1.value
  }
  load1(body)
}
// 获取本地存储的值
const userType = localStorage.getItem(ELocalStorageKey.User_Type)

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

// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #0A2D63;
}

// 表格最后一条白线
:deep .el-table__inner-wrapper::before {
  height: 0;
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

::v-deep .el-dialog {
  background-color: #0B2757;
  // background: rgba(59, 116, 255, 0.15);
  width: 25%;
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
}

:deep(.el-dialog__title) {
  color: #DCDFE5
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

:deep(.el-form-item__label) {
  color: #DCDFE5
}

// .numbers {
//   width: 50%;
//   // border-right: 2px solid #02a6b5;
//   // border: 1px solid rgba(217, 224, 217, 0.884);
// }

// .numbers h2 {
//   font-family: "楷体"; /* 设置为楷体字体 */
//   font-size: 35px; /* 设置字号为 30 像素 */
//   color:aliceblue;
//   width: 100%;
//   margin: 20px;
//   justify-content: center;
//   align-items: center;
// }
// .numbers h3 {
//   font-family: "楷体"; /* 设置为楷体字体 */
//   font-size: 25px; /* 设置字号为 30 像素 */
//   color:aliceblue;
//   width: 100%;
//   margin: 20px;
//   margin-top: 30px;
// }
:deep .el-table__empty-block {
  background-color: #003896;
  // color: antiquewhite;
}

:deep .el-table__inner-wrapper::before {
  height: 0;
}

:deep .el-input {
  --el-input-border-color: #163474;
}

:deep .el-input__wrapper {
  background-color: #0B2756;
}

:deep .el-input__inner {
  color: white;
}

:deep .el-select__wrapper {
  background-color: #0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: aliceblue;
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

.tabelW {
  margin-bottom: 20px;
  // padding-right: 10px;
  // width: 98%;
}
</style>
