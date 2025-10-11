<template>
    <div class="container">
      <!-- <div class="header">媒体文件</div> -->
      <div class="main-box">
        <div class="box-left">
            <div class="title">
                系统配置
            </div>
            <div class="nav">
                <ul>
                    <li class="nav-item ">
                        <router-link
                            v-for="item in NavList"
                            :key="item.id"
                            :to="item.url"
                            :class="{'nav-item': true, 'isclick': item.isActive}"
                            @click="clickNav(item)"
                            >
                            <div :class="{'isclick': item.isActive}">
                                <!-- <img class="nav-icon" :src="item.icon" width="20px" height="20px" alt="sy" /> -->
                                {{ item.title }}
                            </div>
                        </router-link>
                    </li>
                </ul>
                <div class="nav-img-box" style="margin-top: 108px;">
                    <!-- <img src="@/assets/装饰图.png" alt="zs" /> -->
                </div>
            </div>
        </div>
        <div class="box-right">
          <div class="operation">
               <router-view />
            <div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
const NavList = reactive([
  {
    title: '系统信息',
    url: '/platformInfo',
    isActive: false
    // icon: require('@/assets/首页-选中.png')
  },
  {
    title: '接口配置',
    url: '/apiConfig',
    isActive: false
    // icon: require('@/assets/客户列表.png')
  },
  {
    title: '任务配置',
    url: '/taskConfig',
    isActive: false
    // icon: require('@/assets/客户列表.png')
  }
])

onMounted(() => {
  // 加载页面，默认选中第一项
  NavList[0].isActive = true
})
function clickNav (item) {
// 重置 NavList 中的 isActive
  NavList.forEach(item => {
    item.isActive = false
  })

  // 设置当前点击项为选中状态
  item.isActive = true
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
  padding: 20px;
  display: flex;
  flex-direction: column; /* 使子元素垂直排列 */
}

.main-box{
  display: flex; /* 使用 flexbox 布局 */
  height: 100vh; /* 设置容器高度为视口高度 */
}
.box-left{
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  width: 20%; /* 左侧占据 20% 的宽度 */
  // background-color: #4CAF50; /* 绿色背景 */
  padding: 20px; /* 内边距 */
  color: white; /* 字体颜色 */
  // transition: 0.5s;
  border-radius: 15px;
  border: none;
  height: calc(100vh - 100px);
}
// .box-left:hover {
//   box-shadow: inset 0px 0px 20px 3px rgba(34, 135, 255, 0.7);
// }
.box-right{
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  flex: 1; /* 右侧占据剩余空间 */
  width: calc(80% - 10px);
  margin-left: 10px;
  // background-color: #2196F3; /* 蓝色背景 */
  padding: 20px; /* 内边距 */
  color: white; /* 字体颜色 */
  border-radius: 15px;
  border: none;
  height: calc(100vh - 100px);
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
  color:aliceblue;
  // border-bottom: 4px solid rgba(0, 112, 209, 1);
  // border-image: linear-gradient(120deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
}
.operation{
 height: fit-content;
 width: 70%;
 padding: 15px;
 display: flex;
 justify-content: space-between;
 color: rgba(255, 255, 255, 0.762);
 background-color: rgba(0, 112, 209, 0.2);
 font-size: 16px;
 border: 4px solid rgba(0, 112, 209, 1);
//  border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
 border-bottom: none;
 border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
 margin: 0 auto;
}

.title{
 font-size: 20px;
 padding: 10px;
 border-bottom: 1px solid #11B4FB;
}
li {
    list-style-type: none;
}
a {
    text-decoration: none;
    color: #fff;
}
.nav {
    width: 100%;
    height: 100%;
    // background: #FFFFFF;
    box-shadow: 3px 0px 6px 0px rgba(82, 82, 82, 0.14);
}
.nav ul {
    margin-top: 32px;
    padding: 0;
}
.nav-item {
    width: 100%;
    height: 46px;
    font-family: PingFangSC-Medium;
    font-size: 16px;
    // color: #39475A;
    letter-spacing: 0;
    line-height: 46px;
    font-weight: 500;
}
.nav-item div {
    padding-left: 17px;
    height: 46px;
    line-height: 46px;
}
.nav-item div img {
    width: 20px;
    height: 20px;
}
.nav-icon {
    vertical-align: middle;
    margin-right: 7px;
    padding-bottom: 6px;
}
.isclick {
    background: #26c4cc50;
    // width: 100%;
    a {
        // color: #20b31b;
    }
}
.nav-img-box img {
    width: 200px;
    height: 416px;
}
// 按钮
.btn{
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color:rgba(255, 255, 255, 0.762);
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
::v-deep el-pager{
  background-color:#0B2756;
}
// 输入框
:deep(.el-input) {
    --el-input-border-color: #1d4292;
}
:deep(.el-input__wrapper) {
    background-color:#0B2756;
  }
:deep(.el-select__wrapper){
  background-color:#0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: aliceblue;
}
// 表格
// 表格 无数据内容背景设置
:deep(.el-table__empty-block){
   background-color: #0A2D63;
}
.custom-table {
  --header-bg-color: #003896;
  --header-text-color: #fff;
  --row-bg-color: #002D78;
  --row-text-color: white;
}

.custom-table::v-deep .ant-table-thead > tr > th {
  background-color: var(--header-bg-color) !important;
  color: var(--header-text-color) !important;
  font-weight: bold !important;
}

.custom-table::v-deep .ant-table-tbody > tr > td {
  background-color: var(--row-bg-color) !important;
  color: var(--row-text-color) !important;
}
// 表格最后一条白线
:deep .el-table__inner-wrapper::before{
    height: 0;
}

::v-deep .home-ant-input.ant-input-affix-wrapper .ant-input {
  background-color: black;
  color: #c5c8cc;
}
.tablelw{
  max-height: 600px;
  overflow-y: auto
}
</style>
