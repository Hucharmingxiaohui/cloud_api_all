<template>
  <div class="container">
    <!-- <div class="header1">日志管理</div> -->
    <div class="operation">
      <span class="label">一级模块</span>
      <el-select v-model="selectedStation" placeholder="请输入" class="select-operation" :teleported='false'
        style="width: 200px;">
        <el-option label="一级模块1" value="一级模块1"></el-option>
        <el-option label="一级模块2" value="一级模块2"></el-option>
      </el-select>
      <span class="label">二级模块</span>
      <el-select v-model="selectedScheme" placeholder="请输入" class="select-operation" :teleported='false'
        style="width: 200px;">
        <el-option label="二级模块1" value="二级模块1"></el-option>
        <el-option label="二级模块2" value="二级模块2"></el-option>
      </el-select>
      <span class="label">查询参数：</span>
      <el-input placeholder="查询参数" class="custom-select" style="width: 200px;"></el-input>

      <!-- 查询按钮 -->
      <el-button class="new_btn iconfont icon-chaxunhangxian" type="primary" style="margin-left: 30px; width: 70px;"
        @click="queryPoint">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/search.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">查询</span>
      </el-button>
      <!-- 重置按钮 -->
      <el-button class="new_btn1 iconfont icon-zhongzhi-" type="primary" style="margin-left: 10px" @click="resetPoint">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/refresh.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">重置</span>
      </el-button>

      <span class="label">日志等级</span>
      <el-select v-model="selectedLog" placeholder="请输入" class="select-operation" :teleported='false'
        style="width: 200px;">
        <el-option label="日志等级1" value="日志等级1"></el-option>
        <el-option label="日志等级2" value="日志等级2"></el-option>
      </el-select>
    </div>
    <div class="content">
      <div class="table-container">
        <el-table :data="currentPageData" stripe>

          <!-- 序号列 -->
          <el-table-column label="序号" type="index" width="100" />

          <el-table-column label="一级模块" prop="一级模块" />
          <el-table-column label="二级模块" prop="二级模块" />
          <el-table-column label="日志等级" prop="日志等级" />
          <el-table-column label="时间" prop="时间" />
          <el-table-column label="任务时间" prop="任务时间" />
          <el-table-column label="日志内容" prop="日志内容" />
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
</template>

<script setup lang="ts">
import { reactive, ref, computed } from '@vue/reactivity'

const selectedStation = ref<string | null>(null)
const selectedScheme = ref<string | null>(null)
const selectedLog = ref<string | null>(null)
const searchQuery = ref('') // 查询参数

const plansData = reactive({
  data: [
    {
      id: 1,
      一级模块: '一级模块1',
      二级模块: '二级模块1',
      日志等级: '日志等级1',
      时间: '2024-01-01 10:00:00',
      任务时间: '2024-01-01 11:00:00',
      日志内容: '这是第一条日志内容'
    },
    {
      id: 2,
      一级模块: '一级模块2',
      二级模块: '二级模块2',
      日志等级: '日志等级2',
      时间: '2024-01-02 10:00:00',
      任务时间: '2024-01-02 11:00:00',
      日志内容: '这是第二条日志内容'
    },
    {
      id: 1,
      一级模块: '一级模块1',
      二级模块: '二级模块1',
      日志等级: '日志等级1',
      时间: '2024-01-01 10:00:00',
      任务时间: '2024-01-01 11:00:00',
      日志内容: '这是第一条日志内容'
    },
    {
      id: 2,
      一级模块: '一级模块2',
      二级模块: '二级模块2',
      日志等级: '日志等级2',
      时间: '2024-01-02 10:00:00',
      任务时间: '2024-01-02 11:00:00',
      日志内容: '这是第二条日志内容'
    }
  ]
})

// 分页相关属性
const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '30'], // 可选的分页大小
  pageSize: 10, // 默认每页显示10条
  current: 1, // 当前页码
  total: computed(() => plansData.data.length), // 总数据条数，根据 `allPlansData` 动态计算
})

// 计算当前页显示的数据
const currentPageData = computed(() => {
  const start = (paginationProp.current - 1) * paginationProp.pageSize
  const end = start + paginationProp.pageSize
  return plansData.data.slice(start, end)
})

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

// 获取唯一的一级模块、二级模块和日志等级
const uniqueStations = computed(() => {
  return Array.from(new Set(plansData.data.map(item => item.一级模块)))
})

const uniqueSchemes = computed(() => {
  return Array.from(new Set(plansData.data.map(item => item.二级模块)))
})

const uniqueLogs = computed(() => {
  return Array.from(new Set(plansData.data.map(item => item.日志等级)))
})
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

//调整分页部分=================================================================================
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

//=========================================================================================================
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

// .pagination-container {
//   position: absolute;
//   bottom: 20px;
//   /* 距离底部的距离，可调整 */
//   right: 20px;
//   /* 距离右边的距离，可调整 */
// }

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
::v-deep .el-table tr {
  background-color: #0B2756 !important;
  /* opacity: 0.6; */
  color: #F1F6FF;
  font-weight: bold;
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

//----------------------------------------------------------------------------------------------------
//分页数据

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
</style>
