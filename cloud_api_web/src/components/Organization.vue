<template>
  <div class="container1">
    <div class="header" style="display:flex; justify-content: space-between;">
      <span>组织管理</span>
      <div>
        <span style="margin:15px" v-if="showMember">{{ workspaceName }}</span>
        <el-button class="btn" @click="changeMember"> {{ member }}</el-button>
      </div>
    </div>
    <div v-if="!showMember">
      <div class="operation">
        <div class="item1">
          <span style="width: 100px;">组织名称:</span>
          <el-input v-model="search" placeholder="请输入组织名称"></el-input>
        </div>
        <div class="item1">
          <el-button class="btn" @click="load">搜索</el-button>
          <el-button class="btn" @click="add">创建组织</el-button>
        </div>
      </div>
      <div class="tabelW">
        <el-table :data="tableData" :row-key="row => row.id" class="tabelW"
          :header-cell-style="{ color: '#fff', fontSize: '16px', backgroundColor: '#003896', borderLeft: '0.5px #154480 solid', borderBottom: '1px #154480 solid' }"
          :cell-style="{ borderBottom: '0.5px #143275 solid', background: '#002D78', borderLeft: '0.5px #143275 solid', color: '#DCDFE5' }">
          <el-table-column prop="id" label="ID" width="100" />
          <el-table-column prop="workspace_name" label="组织名称" />
          <el-table-column prop="workspace_desc" label="组织描述" />
          <el-table-column prop="platform_name" label="平台名称" />
          <el-table-column label="操作" width="150" v-if="userType === '1'">
            <template #default="scope">
              <el-button size="small" type="text" @click="handleEdit(scope.row)">编辑</el-button>
              <el-popconfirm width="220" confirm-button-text="确定" cancel-button-text="不，谢谢" :icon="InfoFilled"
                icon-color="#626AEF" title="你确定要删除吗" @confirm="handleDelete(scope.row.workspace_id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange" :current-page="currentPage"
          :page-sizes="[10, 20, 30, 40]" :page-size="pageSize" :total="total"
          layout="total, sizes, prev, pager, next, jumper"></el-pagination>
      </div>
    </div>
    <div v-else>
      <div class="operation">
        <div class="item1">
          <span style="width: 100px;">成员名称:</span>
          <el-input v-model="search1" placeholder="请输入用户名称"></el-input>
        </div>
        <div class="item1">
          <el-button class="btn" @click="load1(body)">搜索</el-button>
          <el-button v-if="userType === '1'" class="btn" @click="add1">添加成员</el-button>
        </div>
      </div>
      <div class="tabelW">
        <el-table :data="userdata" style="width: 100%;" stripe
          :header-cell-style="{ color: '#fff', fontSize: '16px', backgroundColor: '#003896', borderLeft: '0.5px #154480 solid', borderBottom: '1px #154480 solid' }"
          :cell-style="{ borderBottom: '0.5px #143275 solid', background: '#002D78', borderLeft: '0.5px #143275 solid', color: '#DCDFE5' }">
          <el-table-column prop="usericons" label="" width="50">
            <el-icon>
              <Avatar />
            </el-icon>
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
              <el-button v-if="userType === '1'" size="mini" type="text" @click="handleEdit1(scope.row)">编辑</el-button>
              <el-popconfirm width="220" confirm-button-text="确定" cancel-button-text="不，谢谢" :icon="InfoFilled"
                icon-color="#626AEF" title="你确定要删除吗" @confirm="handleDelete1(scope.row.user_id)">
                <template #reference>
                  <el-button v-if="userType === '1'" size="small" type="text">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <!--                    分页功能-->
        <el-pagination v-model:current-page="currentPage1" v-model:page-size="pageSize1" :page-sizes="[10, 20, 30, 40]"
          layout="total, sizes, prev, pager, next, jumper" :total="total1" @size-change="handleSizeChange2"
          @current-change="handleCurrentChange2" />
      </div>
    </div>
    <!-- 弹窗 -->
    <div>
      <!-- 编辑组织的弹窗 -->
      <el-dialog title="详细信息" v-model="editDialogVisible" width="30%" class="dialog-color">
        <el-form :model="form" label-width="120px">
          <el-form-item label="id">
            <el-input v-model="form.id" readonly="readonly"></el-input>
          </el-form-item>
          <el-form-item label="组织id">
            <el-input v-model="form.workspace_id" readonly="readonly"></el-input>
          </el-form-item>
          <el-form-item label="组织名称">
            <el-input v-model="form.workspace_name"></el-input>
          </el-form-item>
          <el-form-item label="组织描述">
            <el-input v-model="form.workspace_desc"></el-input>
          </el-form-item>
          <el-form-item label="平台名称">
            <el-input v-model="form.platform_name"></el-input>
          </el-form-item>
          <el-form-item label="绑定码">
            <el-input v-model="form.bind_code" readonly="readonly"></el-input>
          </el-form-item>

        </el-form>

        <template #footer>
          <span class="dialog-footer">
            <el-button @click="editDialogVisible = false">取 消</el-button>
            <el-button type="primary" @click="saveEdit()">确 定</el-button>
          </span>
        </template>
      </el-dialog>
    </div>
    <!-- 创建组织的弹窗 -->
    <div>
      <el-dialog title="组织信息" v-model="editDialogVisible1" width="30%" class="dialog-color">
        <el-form :model="form1" label-width="120px">
          <el-form-item label="组织名称" style="color: antiquewhite;">
            <el-input v-model="form1.workspace_name"></el-input>
          </el-form-item>
          <el-form-item label="组织描述">
            <el-input v-model="form1.workspace_desc"></el-input>
          </el-form-item>
          <el-form-item label="平台名称">
            <el-input v-model="form1.platform_name"></el-input>
          </el-form-item>
          <el-form-item label="绑定码(必填项)">
            <el-input v-model="form1.bind_code"></el-input>
          </el-form-item>

        </el-form>

        <template #footer>
          <span class="dialog-footer">
            <el-button @click="editDialogVisible1 = false">取 消</el-button>
            <el-button type="primary" @click="saveEdit1()">确 定</el-button>
          </span>
        </template>
      </el-dialog>
    </div>
    <!-- 加一个表单修改成员信息 -->
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
            <el-button @click="editDialogVisible2 = false">取 消</el-button>
            <el-button type="primary" @click="saveEdit2()">确 定</el-button>
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
            <el-button @click="editDialogVisible3 = false">取 消</el-button>
            <el-button type="primary" @click="saveEdit3()">确 定</el-button>
          </span>
        </template>
      </el-dialog>

    </div>

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

async function fetchData () {
  try {
    const response = await getAllWorkspaceInfo()
    tableData.value = response.data
    total.value = tableData.value.length // 假设接口返回总数
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  fetchData()
  getPlatformInfo().then(res => {
    workspaceName.value = res.data.workspace_name
    console.log('平台信息', res.data)
  })
  getAllUsers(workspaceId, body)
})

// 切换成员数据
function changeMember () {
  showMember.value = !showMember.value
  if (member.value === '成员管理') {
    member.value = '组织管理'
  } else {
    member.value = '成员管理'
  }
}

function handleSizeChange (val: number) {
  pageSize.value = val
  // 重新请求数据
  fetchData()
}

function handleCurrentChange (val: number) {
  currentPage.value = val
  // 重新请求数据
  fetchData()
}

// 编辑工作空间表格！！！
const editDialogVisible = ref(false) // 控制对话框的显示与隐藏
const form = ref<Workspace>({}) // 用于存储编辑表单数据

function handleEdit (row: Workspace) {
  // 打开编辑对话框
  editDialogVisible.value = true
  // 将当前行的数据赋值给编辑表单
  form.value = { ...row }
}

function saveEdit () {
  updateWorksoaceInfo(form.value.workspace_id, form.value).then(res => {
    if (res.code !== 0) {
      message.error(res.message)
    }
  })
  // 关闭编辑对话框
  editDialogVisible.value = false
  // 重新请求数据
  fetchData()
}

function handleDelete (workspace_id: string) {
  deleteWorkspaceInfo(workspace_id).then(res => {
    if (res.code !== 0) {
      message.error(res.message)
      // 重新请求数据
      fetchData()
    } else {
      // 重新请求数据
      fetchData()
    }
  })
}
// 模糊查询
const search = ref('')
function load () {
  const params = { workspaceName: search.value }
  getWorkspaceByName(params).then((res) => {
    if (res.code !== 0) {
      message.error(res.message)
    } else {
      tableData.value = res.data
      total.value = tableData.value.length
    }
  })
  search.value = '' // 清空搜索框的内容
}

// 创建组织
interface Workspace1 {
  workspace_id: string;
  workspace_name: string;
  workspace_desc: string;
  platform_name: string;
  bind_code: string;
}
const editDialogVisible1 = ref(false)
const form1 = ref<Workspace1>({}) // 用于存储新增工作空间的数据

function add () {
  editDialogVisible1.value = true
}
// 创建工作空间
function saveEdit1 () {
  console.log(form1.value.bind_code)

  if (form1.value.bind_code === undefined) {
    message.error('您的绑定码未填写')
    editDialogVisible1.value = false
    return
  }
  createWorkspace(form1.value).then((res) => {
    if (res.code !== 0) {
      message.error(res.message)
      editDialogVisible1.value = false
    } else {
      // 重新请求数据
      editDialogVisible1.value = false
      fetchData()
    }
  })
}

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
.container1 {
  // height: 100%;
  width: 100%;
  padding: 10px;
}

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
}

// 操作部分
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

.btn {
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color: rgba(255, 255, 255, 0.762);
}

:deep(.el-dialog) {
  background-color: #05204B !important;
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
}
</style>
