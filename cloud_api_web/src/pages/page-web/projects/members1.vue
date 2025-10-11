<template>
  <div class="container">
    <div class="operation">
      <span class="label">成员名称:</span>
      <el-input v-model="search1" placeholder="请输入用户名称" class="custom-select" style="width: 200px;"></el-input>
      <el-button class="new_btn iconfont icon-chaxunhangxian" type="primary" style="margin-left: 30px; width: 70px;"
        @click="load1(body)">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../../assets/v4/search.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">查询</span>
      </el-button>
      <el-button v-if="userType === '1'" class="new_btn iconfont icon-tianjiachengyuan" type="primary"
        style="margin-left: 10px" @click="add1">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer"
                    src="https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG8c71ebd00fa5c3751c60f6beabe74d4f.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">添加成员</span>
      </el-button>
    </div>
    <div class="content">
      <div class="table-container">
        <el-table :data="userdata" stripe
          :header-cell-style="{ height: '43px', color: 'rgba(255, 255, 255, 1)', fontSize: '14px', fontWeight: '500', backgroundColor: '#00399A', borderLeft: '0.5px #154480 solid', borderBottom: '1px #154480 solid' }">
          <el-table-column prop="usericons" label="" width="50">
            <el-icon>
              <Avatar />
            </el-icon>
          </el-table-column>
          <el-table-column label="序号" align='center' width="60">
            <template #default="scope">
              {{ scope.$index +(currentPage1 - 1) * pageSize1+ 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名"></el-table-column>
          <el-table-column prop="user_type" label="用户类型">
            <template #default="scope">
              <span v-if="scope.row.user_type === 'Web'">管理员</span>
              <span v-else-if="scope.row.user_type === 'Pilot'">成员</span>
            </template>
          </el-table-column>
          <el-table-column prop="create_time" label="创建时间"></el-table-column>
          <el-table-column label="管理成员" width="150">
            <template #default="scope">
              <el-button class="editht" v-if="userType === '1'" size="mini" type="text" @click="handleEdit1(scope.row)">编辑</el-button>
              <el-popconfirm width="220" confirm-button-text="确定" cancel-button-text="不，谢谢" :icon="InfoFilled"
                icon-color="#626AEF" title="你确定要删除吗" @confirm="handleDelete1(scope.row.user_id)">
                <template #reference>
                  <el-button class="deleteht" v-if="userType === '1'" size="small" type="text">删除</el-button>
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
    <el-dialog title="成员信息" v-model="editDialogVisible2" width="30%" class="dialog-color">
      <el-form :model="form2" label-width="120px">
        <el-form-item label="用户ID">
          <el-input v-model="form2.user_id"></el-input>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form2.username"></el-input>
        </el-form-item>
        <el-form-item label="组织名称">
          <el-input v-model="form2.workspace_name"></el-input>
        </el-form-item>
        <el-form-item label="用户类型">
          <el-radio-group v-model="form2.user_type">
            <el-radio :label="'Web'">管理员</el-radio>
            <el-radio :label="'Pilot'">成员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="MQTT用户名">
          <el-input v-model="form2.mqtt_username"></el-input>
        </el-form-item>
        <el-form-item label="MQTT密码">
          <el-input v-model="form2.mqtt_password"></el-input>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-input v-model="form2.create_time"></el-input>
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
  updateUserInfo, deleteUser, addUser, getOneUser
} from '/@/api/manage'
import { message, PaginationProps } from 'ant-design-vue'
import { ELocalStorageKey, ERouterName } from '/@/types'
import { localeContextKey } from 'element-plus'
import { IPage } from '/@/api/http/type'

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
  getPlatformInfo().then(res => {
    workspaceName.value = res.data.workspace_name
    console.log('平台信息', res.data)
  })
  getAllUsers(workspaceId, body)
})

// 成员管理模块！！！
// 第1步获取组织名称
const workspaceName = ref('')
// 第2步默认加载该工作空间下所有成员的信息
// 定义数据
export interface Member {
  user_id: string
  username: string
  user_type: string
  workspace_name: string
  create_time: string
  mqtt_username: string
  mqtt_password: string
}

const userdata = ref<Member[]>([])
const currentPage1 = ref(1)
const pageSize1 = ref(10)
const total1 = ref(0)
const workspaceId: string = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
const body: IPage = {
  page: 1,
  total: 0,
  page_size: 10
}
// 加载用户信息
function getAllUsers (workspaceId: string, page: IPage) {
  getAllUsersInfo(workspaceId, page).then(res => {
    const userList: Member[] = res.data.list
    userdata.value = userList
    total1.value = res.data.pagination.total
    currentPage1.value = res.data.pagination.page
  })
}

const form2 = ref<Member>({}) // 用于存储需要修改的用户信息
const editDialogVisible2 = ref(false)
// 打开修改表单弹窗
function handleEdit1 (row: Member) {
  // 打开编辑对话框
  editDialogVisible2.value = true
  // 将当前行的数据赋值给编辑表单
  form2.value = { ...row }
}

// 保存数据
function saveEdit2 () {
  updateUserInfo(workspaceId, form2.value.user_id, form2.value).then(res => {
    if (res.code !== 0) {
      message.error(res.message)
    } else {
      // 关闭编辑对话框
      editDialogVisible2.value = false
      getAllUsers(workspaceId, body)
      // 重新请求数据
    }
  })
}
// 删除用户信息
function handleDelete1 (user_id: string) {
  deleteUser(workspaceId, user_id).then(res => {
    if (res.code !== 0) {
      message.error(res.message)
    } else {
      // 关闭编辑对话框
      // editDialogVisible2.value = false
      getAllUsers(workspaceId, body)
      // 重新请求数据
    }
  })
}

// 定义添加用户成员信息表单
interface Member1 {
  username: string
  password: string
  workspace_id: string
  user_type: string
  mqtt_username: string
  mqtt_password: string
}
const form3 = ref<Member1>({})
const editDialogVisible3 = ref(false)
// 添加用户成员信息
function add1 () {
  editDialogVisible3.value = true
}
function saveEdit3 () {
  addUser(workspaceId, form3.value).then(res => {
    if (res.code !== 0) {
      message.error(res.message)
    } else {
      // 关闭编辑对话框
      editDialogVisible3.value = false
      getAllUsers(workspaceId, body)
      // 重新请求数据
    }
  })
}
// 搜索用户
const search1 = ref('')
function load1 (page: IPage) {
  getOneUser(workspaceId, search1.value, page).then(res => {
    const userList: Member[] = res.data.list
    userdata.value = userList
    total1.value = res.data.pagination.total
    currentPage1.value = res.data.pagination.page
  })
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
}

.editht {
  background-color: rgba(51, 122, 255, 0.12);
  border-radius: 4px;
  height: 28px;
  color: white;
  border: 1px solid rgba(0, 64, 147, 1);
  width: 40px;
  margin-left: 7px;
}

.deleteht {
  background-color: rgba(255, 92, 51, 0.19);
  border-radius: 4px;
  height: 28px;
  color: rgba(255, 215, 215, 1);
  border: 1px solid rgba(255, 132, 132, 1);
  width: 40px;
  margin-left: 7px;
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

:deep(.el-dialog) {
  background-color: #0B2756 !important;
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
}

:deep(.el-input__inner) {
  color: white;
}

:deep(.el-dialog__title) {
  color: #DCDFE5
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
}</style>
