<template>
  <div class="container">
    <!-- <div class="header1">主机连接</div> -->
    <div class="operation">
      <!-- 查询按钮 -->
      <el-button class="new_btn iconfont icon-tianjiazhuji" type="primary" style="margin-left: 30px; width: 100px;"
        @click="openAddDialog">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/search.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">添加主机</span>
      </el-button>
      <!-- <el-button class="btn" type="primary" style="margin-left: 10px" @click="openAddDialog">添加主机</el-button> -->
    </div>
    <div class="content">
      <div class="table-container">
        <el-table :data="currentPageData" stripe
          :header-cell-style="{ color: '#fff', fontSize: '16px', backgroundColor: '#003896', borderLeft: '0.5px #154480 solid', borderBottom: '1px #154480 solid' }">

          <!-- 序号列 -->
          <el-table-column label="序号" align='center' width="60">
              <template #default="scope">
                {{ scope.$index +(paginationProp.current - 1) * paginationProp.pageSize+ 1 }}
              </template>
            </el-table-column>
          <!-- <el-table-column label="输入主机类型" prop="type" /> -->
          <el-table-column label="输入主机类型">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.type }}</div>
            </template>
          </el-table-column>
          <!-- <el-table-column label="主机名称" prop="name" /> -->
          <el-table-column label="主机名称">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.name }}</div>
            </template>
          </el-table-column>
          <!-- <el-table-column label="场站" prop="station" /> -->
          <el-table-column label="场站">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.station }}</div>
            </template>
          </el-table-column>
          <!-- <el-table-column label="主机编码" prop="code" /> -->
          <el-table-column label="主机编码">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.code }}</div>
            </template>
          </el-table-column>
          <!-- <el-table-column label="主机IP" prop="ip" /> -->
          <el-table-column label="主机IP">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.ip }}</div>
            </template>
          </el-table-column>
          <!-- 启用/禁用 -->
          <el-table-column label="启用/禁用">
            <template #default="scope">
              <el-switch v-model="scope.row.status" :active-value="'启用'" :inactive-value="'禁用'"
                @change="handleStatusChange(scope.row)" />
            </template>
          </el-table-column>
          <!-- <el-table-column label="连接状态" prop="connection" /> -->
          <el-table-column label="连接状态">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.connection }}</div>
            </template>
          </el-table-column>
          <!-- <el-table-column label="最后一次注册时间" prop="lastRegisterTime" /> -->
          <el-table-column label="最后一次注册时间">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.lastRegisterTime }}</div>
            </template>
          </el-table-column>
          <!-- <el-table-column label="最后一次心跳时间" prop="lastHeartbeatTime" /> -->
          <el-table-column label="最后一次心跳时间">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.lastHeartbeatTime }}</div>
            </template>
          </el-table-column>

          <el-table-column label="操作">
            <template #default="scope">
              <div class="action-buttons">
                <el-button size="small" type="primary" class="editht" @click="editHost(scope.row)">编辑</el-button>
                <el-button size="small" type="primary" class="deleteht" @click="deleteHost(scope.row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
    <!-- <div style="position: relative; min-height: 100%;"> -->
    <div class="pagination-container">
      <el-pagination v-model:current-page="paginationProp.current" v-model:page-size="paginationProp.pageSize"
        :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
        layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
        @current-change="handleCurrentChange"></el-pagination>
    </div>
  </div>

  <!-- 弹窗 -->
  <el-dialog v-model="dialogVisible" :title="isEditMode ? '编辑主机' : '新增'" width="600px" :before-close="handleDialogClose"
    style="background-color: #0A2D63; box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5); color: white">
    <template #header>
      <span style="color: white; font-size: 20px;">{{ isEditMode ? '编辑主机' : '新增' }}</span>
    </template>
    <el-form :model="newHostForm">
      <el-form-item label="主机类型" :label-width="formLabelWidth" required>
        <el-input v-model="newHostForm.type"></el-input>
      </el-form-item>
      <el-form-item label="接入协议" :label-width="formLabelWidth" required>
        <el-input v-model="newHostForm.agree"></el-input>
      </el-form-item>
      <el-form-item label="主机名称" :label-width="formLabelWidth" required>
        <el-input v-model="newHostForm.name"></el-input>
      </el-form-item>
      <!-- <el-form-item label="场站" :label-width="formLabelWidth" required>
                <el-input v-model="newHostForm.station"></el-input>
            </el-form-item> -->
      <el-form-item label="主机编码" :label-width="formLabelWidth" required>
        <el-input v-model="newHostForm.code"></el-input>
      </el-form-item>
      <el-form-item label="主机IP" :label-width="formLabelWidth" required>
        <el-input v-model="newHostForm.ip"></el-input>
      </el-form-item>
      <el-form-item label="主机端口" :label-width="formLabelWidth" required>
        <el-input v-model="newHostForm.port"></el-input>
      </el-form-item>
      <el-form-item label="ftpIP" :label-width="formLabelWidth" required>
        <el-input v-model="newHostForm.ftpip"></el-input>
      </el-form-item>
      <el-form-item label="ftp端口" :label-width="formLabelWidth" required>
        <el-input v-model="newHostForm.ftpport"></el-input>
      </el-form-item>
      <el-form-item label="ftp用户名" :label-width="formLabelWidth" required>
        <el-input v-model="newHostForm.ftpuseid"></el-input>
      </el-form-item>
      <el-form-item label="ftp密码" :label-width="formLabelWidth" required>
        <el-input v-model="newHostForm.ftppasswd"></el-input>
      </el-form-item>

    </el-form>

    <!-- <span slot="footer" class="dialog-footer"> -->
    <template #footer>
      <el-button @click="handleCancel" class="nobtn">取消</el-button>
      <el-button type="primary" @click="saveHost" class="okbtn">提交</el-button>
    </template>
    <!-- </span> -->
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { reactive } from '@vue/reactivity'
import { ElMessageBox, ElMessage } from 'element-plus'

// 弹窗的显示状态
const dialogVisible = ref(false)
const isEditMode = ref(false) // 是否为编辑模式
const currentEditRow = ref(null) // 当前编辑的行
// 新增主机表单数据
const newHostForm = reactive({
  type: '',
  agree: '',
  name: '',
  code: '',
  ip: '',
  port: '',
  ftpip: '',
  ftpport: '',
  ftpuseid: '',
  ftppasswd: '',

})

const formLabelWidth = '100px' // 设置表单标签宽度

// 关闭弹窗
function handleDialogClose () {
  dialogVisible.value = false
}

const plansData = reactive([

  {
    id: 1,
    type: '主机类型A',
    agree: '404',
    name: '主机1',
    code: 'H001',
    ip: '192.168.1.1',
    port: '3306',
    ftpip: '123',
    ftpport: '333',
    ftpuseid: 'qwer',
    ftppasswd: '123',
    status: '启用',
    connection: '已连接',
    lastRegisterTime: '2024-11-20 10:00:00',
    lastHeartbeatTime: '2024-11-20 10:05:00',
  },
  {
    id: 2,
    type: '主机类型B',
    agree: '404',
    name: '主机2',
    code: 'H002',
    ip: '192.168.1.2',
    port: '3306',
    ftpip: '123',
    ftpport: '333',
    ftpuseid: 'qwer',
    ftppasswd: '123',
    status: '禁用',
    connection: '未连接',
    lastRegisterTime: '2024-11-19 08:30:00',
    lastHeartbeatTime: '2024-11-19 08:35:00',
  }

]//   ] as Task[], // 默认数据
)

// 分页相关属性
const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '30'], // 可选的分页大小
  pageSize: 10, // 默认每页显示10条
  current: 1, // 当前页码
  total: computed(() => plansData.length), // 总数据条数，根据 `allPlansData` 动态计算
})

// 计算当前页显示的数据
const currentPageData = computed(() => {
  const start = (paginationProp.current - 1) * paginationProp.pageSize
  const end = start + paginationProp.pageSize
  return plansData.slice(start, end)
})

// 打开新增弹窗
function openAddDialog () {
  isEditMode.value = false
  Object.assign(newHostForm, {
    type: '',
    agree: '',
    name: '',
    code: '',
    ip: '',
    port: '',
    ftpip: '',
    ftpport: '',
    ftpuseid: '',
    ftppasswd: '',
  })
  dialogVisible.value = true
}

// 编辑主机
// function editHost(row: typeof plansData.data[0]) {
function editHost (row) {
  isEditMode.value = true
  Object.assign(newHostForm, row) // 填充表单数据
  currentEditRow.value = row
  dialogVisible.value = true
}

function deleteHost (row) {
  ElMessageBox.confirm(`确定要删除主机 ${row.name} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      const index = plansData.findIndex((item) => item.id === row.id)
      if (index !== -1) plansData.splice(index, 1) // 删除数据
      ElMessage.success('删除成功')
      // 确保当前页数据刷新
      if (paginationProp.current > 1 && currentPageData.value.length === 0) {
        paginationProp.current-- // 如果当前页没有数据了，则自动回退一页
      }
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}

// 保存主机
// function saveHost () {
//   if (isEditMode.value) {
//     Object.assign(currentEditRow.value, newHostForm) // 更新当前行数据
//     ElMessage.success('编辑成功')
//   } else {
//     plansData.push({ ...newHostForm, id: Date.now() }) // 添加新数据
//     ElMessage.success('添加成功')
//   }
//   dialogVisible.value = false
// }
// function handleCancel () {
//   dialogVisible.value = false
// }
function validateForm () {
  const requiredFields = [
    { value: newHostForm.type, label: '主机类型' },
    { value: newHostForm.agree, label: '接入协议' },
    { value: newHostForm.name, label: '主机名称' },
    { value: newHostForm.code, label: '主机编码' },
    { value: newHostForm.ip, label: '主机IP' },
    { value: newHostForm.port, label: '主机端口' },
    { value: newHostForm.ftpip, label: 'ftpIP' },
    { value: newHostForm.ftpport, label: 'ftp端口' },
    { value: newHostForm.ftpuseid, label: 'ftp用户名' },
    { value: newHostForm.ftppasswd, label: 'ftp密码' },
  ]

  for (const field of requiredFields) {
    if (!field.value || field.value.trim() === '') {
      ElMessage.error(`${field.label} 不能为空`)
      return false
    }
  }
  return true
}

// 保存主机
function saveHost () {
  if (!validateForm()) {
    return // 如果验证未通过，直接返回
  }

  if (isEditMode.value) {
    Object.assign(currentEditRow.value, newHostForm) // 更新当前行数据
    ElMessage.success('编辑成功')
  } else {
    plansData.push({ ...newHostForm, id: Date.now() }) // 添加新数据
    ElMessage.success('添加成功')
  }
  dialogVisible.value = false
}
// ============================================================分页数据==========================================================
// 分页事件
function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  //   refreshData(paginationProp)
  paginationProp.current = 1 // 重置为第一页
}
function handleCurrentChange (val: number) {
  paginationProp.current = val
  //   refreshData(paginationProp)
}

// 处理开关状态变更
function handleStatusChange (row) {
  console.log(`主机 ${row.name} 状态变更为：${row.status}`)
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

.main-box {
  display: flex;
  /* 使用 flexbox 布局 */
  height: 100vh;
  /* 设置容器高度为视口高度 */
}

// 头部  标题 面包屑
.header1 {
  width: 100%;
  height: 60px;
  background: #05204B;
  padding: 16px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color: aliceblue;
}

.table-pagination-container {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100vh;
  /* 适配整个页面 */
}

.table-container {
  flex-grow: 1;
  overflow: hidden;
  height: 500px;
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
  height: 90vh;
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

// 操作下面的几个按钮的样式-------------------------------------------
.editht {
  background-color: rgba(51, 122, 255, 0.12);
  border-radius: 4px;
  height: 28px;
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

.action-buttons .el-button {
  color: white;
  /* 使文字颜色为白色 */
}

.action-buttons .deleteht {
  color: rgba(255, 215, 215, 1);

}

//------------------------------------------------------------------
.el-form-item__label.is-required::before {
  color: red;
  /* 设置星号的颜色为红色 */
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

.el-dialog__header {
  //   background-color: #0A2D63; /* 标题部分背景 */
  color: white !important;
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

:deep(.el-tree-node__content) {
  background-color: #123974;
  color: white;
}

:deep(.el-tree-node__content:hover) {
  background-color: #154480;
  /* 鼠标悬停颜色 */
}

:deep(.el-checkbox__label) {
  color: white;
  /* 复选框文字颜色 */
}

:deep(.el-dialog__footer .el-button) {
  background: linear-gradient(to top, #0B2756, #124AAD);
  color: white;
  border: 1px solid #0A418A;
}

:deep(.el-dialog__footer .el-button:hover) {
  background: linear-gradient(to bottom, #124AAD, #0B2756);
}

/* 修改输入框文字颜色为白色 */
:deep(.el-input__inner) {
  color: white;
  /* 输入文字为白色 */
}

/* 设置输入框在聚焦状态时的边框颜色和文字颜色 */
:deep(.el-input__inner:focus) {
  border-color: #11B4FB;
  /* 聚焦时的边框颜色 */
  color: white;
  /* 聚焦时文字颜色 */
}

/* 禁用状态的样式 */
:deep(.el-input__inner[disabled]) {
  background-color: #1a1a1a;
  /* 禁用状态背景色 */
  color: gray;
  /* 禁用状态文字颜色 */
  border-color: #666;
  /* 禁用状态边框颜色 */
}

/* 覆盖 el-form-item 中的 label 文字颜色 */
:deep(.el-form-item__label) {
  color: white !important;
  /* 设置标签文字为白色 */
}

/* 调整表单项之间的间距，保证整体美观 */
:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-select__wrapper) {
  background-color: #0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: aliceblue;
}

.tablelw1 {
  max-height: 600px;
  overflow-y: auto
}

// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #0A2D63;
}

// 表格最后一条白线
:deep .el-table__inner-wrapper::before {
  height: 0;
}

//表格样式-----------------------------------------------------------------------------------------
// 表格 无数据内容背景设置

::v-deep .el-table tr {
  background-color: #0B2756 !important;
  /* opacity: 0.6; */
  color: #F1F6FF;
  font-weight: bold;
}

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
</style>
