<template>
  <div class="page">
    <div class="block_1">
      <div class="text-wrapper_1 ">
        <a-row class="ant-row">
          <a-col :span="1"></a-col>
          <el-col :span="7" class="text_1">
            <!-- 左侧菜单 -->
            <el-menu
              :default-active="activeIndex"
              class="el-menu-demo"
              mode="horizontal"
              @select="handleSelect"
            >
              <template v-for="item in menuOptions" :key="item.index">
                <el-menu-item :index="item.index" v-if="!item.children">
                  <router-link :to="item.path">
                    {{ item.label }}
                  </router-link>
                </el-menu-item>
                <el-sub-menu
                  v-else
                  :index="item.index"
                  popper-class="child-menu-title"
                >
                  <template #title>
                    {{ item.label }}
                  </template>
                  <template v-for="child in item.children" :key="child.index">
                    <el-menu-item :index="child.index">
                      <router-link :to="child.path">
                        {{ child.label }}
                      </router-link>
                    </el-menu-item>
                  </template>
                </el-sub-menu>
              </template>
            </el-menu>
          </el-col>
          <a-col :span="1"></a-col>
          <a-col :span="6" class="text_2">
            <span>无人机任务规划与数据采集系统</span>
          </a-col>
          <a-col :span="1"></a-col>
          <a-col :span="7" class="text_1">
            <!-- 右侧菜单 -->
            <el-menu
              class="el-menu-demo"
              mode="horizontal"
              :default-active="activeIndex"
              @select="handleSelect"
            >
              <template v-for="item in menuRightOptions" :key="item.index">
                <el-menu-item :index="item.index" v-if="!item.children">
                  <router-link :to="item.path">
                    {{ item.label }}
                  </router-link>
                </el-menu-item>
                <el-sub-menu
                  v-else
                  :index="item.index"
                  popper-class="child-menu-title"
                >
                  <template #title>
                    {{ item.label }}
                  </template>
                  <template v-for="child in item.children" :key="child.index">
                    <el-menu-item :index="child.index" v-if="!child.children">
                      <router-link :to="child.path">
                        {{ child.label }}
                      </router-link>
                    </el-menu-item>
                    <el-sub-menu v-else :index="child.index">
                      <template #title>
                        {{ child.label }}
                      </template>

                      <el-menu-item
                        v-for="grandChild in child.children"
                        :key="grandChild.index"
                        :index="grandChild.index"
                      >
                        <router-link :to="child.path">
                          {{ grandChild.label }}
                        </router-link>
                      </el-menu-item>
                    </el-sub-menu>
                  </template>
                </el-sub-menu>
              </template>
            </el-menu>
          </a-col>
          <a-col :span="1" class="text_1" style="margin-top: 30px;">
            <div>
              <a-dropdown>
                <div class="height-100">
                  <span
                    class="fz20 mt20"
                    style="border: 2px solid white; border-radius: 50%; display: inline-flex;"
                    ><UserOutlined
                  /></span>
                  <!-- <span class="ml10 mr10" style="float: right;">{{ username }}</span> -->
                </div>
                <template #overlay>
                  <a-menu
                    theme="dark"
                    class="flex-column flex-justify-between flex-align-center"
                  >
                    <a-menu-item>
                      <span class="mr10" style="font-size: 16px;"
                        ><ExportOutlined
                      /></span>
                      <span @click="logout">注销</span>
                    </a-menu-item>
                    <!-- <a-menu-item >
                    <span class="mr10" style="font-size: 16px;"><ExportOutlined /></span>
                    <span @click="goHome">返回</span>
                  </a-menu-item> -->
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
          </a-col>
        </a-row>
      </div>
    </div>
    <div class="bottom-bg"></div>
  </div>
</template>

<script lang="ts" setup>
import { createVNode, defineComponent, ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { getRoot } from '/@/root'
import * as icons from '@ant-design/icons-vue'
import { ERouterName, ELocalStorageKey } from '/@/types'
import { UserOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { useRouter, useRoute } from 'vue-router'
const activeIndex = ref('2')
const router = useRouter()
const route = useRoute()
const STORAGE_KEY = 'menu-active-index' // 存储和读取的key
const root = getRoot()
const selected = ref<string>(root.$route.path) // 跳转
const username = ref(localStorage.getItem(ELocalStorageKey.Username)) // 从本地获取登录用户名
interface IOptions {
  key: number
  label: string
  path:
    | string
    | {
        path: string
        query?: any
      }
  icon: string
}

// 菜单自动隐藏
const viewportWidth = ref<number>(window.innerWidth)
const Icon = (props: {icon: string}) => {
  return createVNode((icons as any)[props.icon])
}

const handleResize = () => {
  viewportWidth.value = window.innerWidth
}
onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize() // Initialize viewportWidth
  activeIndex.value = localStorage.getItem(STORAGE_KEY) || '2'

  // const path = findPathByIndex(allMenuItems, activeIndex.value)
  router.push(selected.value)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
// ----------------------------------------

// 左侧菜单
// 菜单配置数据（添加path字段）
const menuOptions = [
  {
    index: '1',
    label: '变量管理',
    children: [
      {
        index: '1-1',
        label: '风机管理',
        path: '/fanMgt'
      },
    ]
  },
  {
    index: '2',
    label: '设备管理',
    path: '/' + ERouterName.DEVICES,

  },
  {
    index: '3',
    label: '视频直播',
    path: '/' + ERouterName.LIVESTREAM,
  },
]

const menuRightOptions = [
  {
    index: '4',
    label: '航线管理',
    path: '/' + ERouterName.NEW_WAYLINE
  },
  {
    index: '5',
    label: '任务管理',
    children: [
      {
        index: '5-1',
        label: '普通计划',
        path: '/' + ERouterName.FLY_WAYLINE_PLAN,
      },
      {
        index: '5-2',
        label: '风机计划',
        path: '/' + ERouterName.FLY_FAN_PLAN,
      },
      {
        index: '5-3',
        label: '飞行任务',
        path: '/' + ERouterName.TASK
      },
    ]
  },
  {
    index: '6',
    label: '系统管理',
    children: [{
      index: '6-1',
      lable: '日志管理',
      path: '/' + ERouterName.LOGS
    },
    {
      index: '6-2',
      lable: '固件管理',
      path: '/' + ERouterName.FIRMWARES
    }
    ]
  },
]

// 合并所有菜单项
const allMenuItems = [...menuOptions, ...menuRightOptions]

// 查找路径
function findPathByIndex (items, targetIndex) {
  for (const item of items) {
    if (item.index === targetIndex) {
      return item.path
    }
    if (item.children) {
      const found = findPathByIndex(item.children, targetIndex)
      if (found) return found
    }
  }
  return null
}

// 更新菜单激活状态
function handleSelect (index) {
  activeIndex.value = index
  localStorage.setItem(STORAGE_KEY, index)
  // const path = findPathByIndex(allMenuItems, index)
  // if (path) {
  //   router.push(path)
  // }
}

// 查找路径对应的索引
function findIndexByPath (items, targetPath) {
  for (const item of items) {
    if (item.path === targetPath) {
      return item.index
    }
    if (item.children) {
      const found = findIndexByPath(item.children, targetPath)
      if (found) return found
    }
  }
  return null
}

// // 监听路由变化，自动更新激活菜单
// watch(() => route.path, (newPath) => {
//   const matchedIndex = findIndexByPath(allMenuItems, newPath)
//   if (matchedIndex) {
//     activeIndex.value = matchedIndex
//     localStorage.setItem(STORAGE_KEY, matchedIndex)
//   }
// }, { immediate: true })

const logout = () => {
  localStorage.clear()
  root.$router.push('/' + ERouterName.PROJECT)
}

function goHome () {
  root.$router.push('/' + ERouterName.MEMBERS)
}
</script>

<style scoped lang="scss">

// 菜单样式
:deep(.el-menu){
   background-color:transparent;
   height: 50px;
}
:deep(.el-menu:hover){
   background-color:transparent;
   height: 50px;
}
// 一级标题
:deep(.el-menu--horizontal>.el-menu-item ){
  height: 40px;
  overflow-wrap: break-word;
  color: rgba(175, 193, 222, 1) ;
  font-size: 22px;
  font-family: YouSheBiaoTiHei-Regular;
  font-weight: normal;
  text-align: left;
  white-space: nowrap;
  line-height: 40px;
  margin: 15px 10px 0 10px;
}
:deep(.el-menu--horizontal>.el-menu-item.is-active){
  color: rgba(175, 193, 222, 1) !important;
  background: url('/@/assets/v4/selected_center.png') 100% no-repeat;
  background-size: 100% 100%
}
:deep(.el-menu--horizontal>.el-menu-item:hover){
  color: rgb(202, 222, 255); /* 选中时文字颜色 */
  background: none;
}

// 二级标题
:deep(.el-menu--horizontal>.el-sub-menu.is-active .el-sub-menu__title){
  border: none;
  background: url('/@/assets/v4/selected_center.png') 100% no-repeat;
  background-size: 100% 100%
}
:deep(.el-menu--horizontal>.el-sub-menu .el-sub-menu__title){
  height: 40px;
  overflow-wrap: break-word;
  color: rgba(175, 193, 222, 1) ;
  font-size: 22px;
  font-family: YouSheBiaoTiHei-Regular;
  font-weight: normal;
  text-align: left;
  white-space: nowrap;
  line-height: 40px;
  margin: 15px 10px 0 10px;

}
:deep(.el-menu--horizontal>.el-sub-menu .el-sub-menu__title:hover){
  color: rgb(202, 222, 255); /* 选中时文字颜色 */
  background: none;

}
:deep(.el-menu--horizontal.el-menu){
  border:none
}

// 二级标题下拉列表 .el-sub-menu__title
:deep(.el-menu--horizontal .el-menu .el-menu-item, .el-menu--horizontal .el-menu) {
  background: linear-gradient(135deg, #1e3a8a 0%, #0c4a6e 100%) !important;
  border: 1px solid rgba(99, 156, 242, 0.3) !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3) !important;
  padding: 5px 0 !important;
}

.selected-item {
  color: rgba(175, 193, 222, 1); /* 选中时文字颜色 */
  background: url('/@/assets/v4/selected_left.png') 100% no-repeat;
  background-size: 100% 100%; /* 让背景图覆盖整个元素 */
  padding: 15px  30px 0px 30px;
  display: inline-block; /* 使得元素变为块级元素，使其宽度和背景图可以自适应 */
  line-height: 1.5; /* 调整行高，使背景图和文字居中 */
  box-sizing: border-box; /* 确保padding不影响宽高 */
}
.selected-item1 {
  color: rgba(175, 193, 222, 1); /* 选中时文字颜色 */
  background: url('/@/assets/v4/selected_right.png') 100% no-repeat;
  background-size: 100% 100%; /* 让背景图覆盖整个元素 */
  padding: 15px  30px 0px 30px;
  display: inline-block; /* 使得元素变为块级元素，使其宽度和背景图可以自适应 */
  line-height: 1.5; /* 调整行高，使背景图和文字居中 */
  box-sizing: border-box; /* 确保padding不影响宽高 */
}

.unselected-item {
  color: rgba(175, 193, 222, 1); /* 未选中时文字颜色 */
  padding: 15px  30px 0px 30px;
  line-height: 1.5; /* 调整行高，使背景图和文字居中 */
  display: inline-block; /* 使得元素变为块级元素，使其宽度和背景图可以自适应 */
  box-sizing: border-box; /* 确保padding不影响宽高 */
}

.page {
  background-color: rgba(0, 23, 59, 1);
  // position: relative;
  width: 100%;
  height: 100vh;
  // overflow: hidden;

  .block_1 {
    width: 100%;
    height:calc(100vh - 50px );
    // height: 100vh;
    // background: url('/@/assets/v4/header_bg.png') 100% no-repeat;
    background-size: 100% 100%; /* 让背景图覆盖整个元素 */
    // background: url(https://lanhu-oss.lanhuapp.com/FigmaDDSSlicePNG76d2c5ab14b018fa8e5fb491f360bc6b.png)
      // 100% no-repeat;
    .text-wrapper_1 {
      width: 100%;
      height: 62px;
      // margin: 0px 0 0 0px;
      .ant-row{
        background: url('/@/assets/v4/header_bg1.png') 100%  no-repeat;
        background-size: 100% 100% ;
        padding: 0 0 10px 0;
      }
      .text_1 {
        // width: 81px;
        height: 29px;
        overflow-wrap: break-word;
        color: rgba(175, 193, 222, 1) ;
        font-size: 22px;
        font-family: YouSheBiaoTiHei-Regular;
        font-weight: normal;
        text-align: left;
        white-space: nowrap;
        line-height: 29px;
        margin-top: 40px;
      }
      .text_2 {
        display: flex;
        justify-content: center;
        align-items: center;
        text-shadow: 2px 5px 3px rgba(6, 20, 75, 0.42);
        background-image: linear-gradient(
          180deg,
          rgba(255, 255, 255, 1) 0,
          rgba(167, 215, 255, 1) 100%
        );
        height: 50px;
        overflow-wrap: break-word;
        color: rgba(175, 193, 222, 1);
        font-size: 30px;
        font-family: YouSheBiaoTiHei-Regular;
        font-weight: normal;
        text-align: center;
        white-space: nowrap;
        margin-top: 20px;
        line-height: 42px;
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
    }
  }
  .bottom-bg{
    z-index: 4000;
    height: 50px;
    background: url('/@/assets/v4/bottom_bg1.png') 100%  no-repeat;
    background-size: 100% 100% ;
  }

}

.fontBold {
  font-weight: 500;
  font-size: 18px;
}
.header-bg{
  // background-color: aqua;
  // padding: 0;
  background: url('/@/assets/icons/header.png')  no-repeat;
  background-size: 100% 100%;
}
.mean-bar{
  width: 81px;
  height: 29px;
  overflow-wrap: break-word;
  color: rgba(175, 193, 222, 1) !important;
  font-size: 22px;
  font-family: YouSheBiaoTiHei-Regular;
  font-weight: normal;
  text-align: left;
  white-space: nowrap;
  line-height: 22px;
  margin-top: 33px;
}
</style>

<style lang="scss">
.child-menu-title{
  border: none !important;
  .el-menu {
    // 修改二级菜单整个背景颜色
    background-color:rgb(2, 51, 112);
    // 二级菜单中的子选项
    .el-menu-item {
      a{
        color: #e0e2e6 !important;
        font-weight: 500;
        font-size: 16px;
        text-align: center;
      }
      background-color: transparent !important;
      width: 100%;
      &.is-active{
        a{
          color: rgb(8, 120, 248) !important;
          text-align: center;
        }

        background-color: #182dd036 !important;
        width: 100%;

      }
      // 被选择的子选项
      &:not(.is-disabled):hover {
        a{
          color: rgb(41, 197, 222) !important;
        }
      }
    }
  }
}
</style>
