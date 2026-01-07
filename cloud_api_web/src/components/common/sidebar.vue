<template>
  <div class="page">
    <div class="block_1">
      <div class="text-wrapper_1 ">
        <a-row class="ant-row">
          <a-col :span="1"></a-col>
          <el-col :span="7" class="text_1"  style="display: flex; justify-content: center;">
            <!-- 左侧菜单 -->
             <div style="width: 100%;">
                          <el-menu
              :default-active="activeIndex"
              class="el-menu-demo"
              mode="horizontal"
              @select="handleSelect"
            >
              <template v-for="item in menuOptions" :key="item.name">
                <el-menu-item :index="item.name" v-if="!item.children || item.children.length === 0">
                  <router-link :to="item.path">
                    {{ item.label }}
                  </router-link>
                </el-menu-item>
                <el-sub-menu
                  v-else
                  :index="item.name"
                  popper-class="child-menu-title"
                >
                  <template #title>
                    {{ item.label }}
                  </template>
                  <template v-for="child in item.children" :key="child.name">
                    <el-menu-item :index="child.name">
                      <router-link :to="child.path">
                        {{ child.label }}
                      </router-link>
                    </el-menu-item>
                  </template>
                </el-sub-menu>
              </template>
            </el-menu>
             </div>
          </el-col>
          <a-col :span="1"></a-col>
          <a-col :span="6" class="text_2">
            <span>河北华电220kV尹夏储能站</span>
          </a-col>
          <a-col :span="2" ></a-col>
          <a-col :span="7" class="text_1"  style="display: flex; justify-content: end;">
            <!-- 右侧菜单 -->
              <div style="width: 100%; padding-left: 35px;">
                            <el-menu
              class="el-menu-demo"
              mode="horizontal"
              :default-active="activeIndex"
              @select="handleSelect"
            >
              <template v-for="item in menuRightOptions" :key="item.name">
                <el-menu-item :index="item.name" v-if="!item.children || item.children.length===0">
                  <router-link :to="item.path">
                    {{ item.label }}
                  </router-link>
                </el-menu-item>
                <el-sub-menu
                  v-else
                  :index="item.name"
                  popper-class="child-menu-title"
                >
                  <template #title>
                    {{ item.label }}
                  </template>
                  <template v-for="child in item.children" :key="child.name">
                    <el-menu-item :index="child.name" v-if="!child.children || child.children.length===0">
                      <router-link :to="child.path">
                        {{ child.label }}
                      </router-link>
                    </el-menu-item>
                    <el-sub-menu v-else :index="child.name">
                      <template #title>
                        {{ child.label }}
                      </template>

                      <el-menu-item
                        v-for="grandChild in child.children"
                        :key="grandChild.name"
                        :index="grandChild.name"
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
              </div>

            <!-- 退出 -->
           <div style="position: absolute; right: 50px; top: -5px;">
            <el-dropdown>
              <div style="cursor: pointer;">
                <span
                  class="fz20 mt20"
                  style="
                    display: inline-flex;
                    padding: 8px;
                    border: transparent !important;
                  "
                >
                 <!-- <img :src="userIcon" alt=""> -->
                  <el-icon color="white" class="no-outline"><UserFilled /></el-icon>
                </span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="logout">
                    <el-icon><SwitchButton /></el-icon>
                    <span style="margin-left: 8px;">注销</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
import { ERouterName, ELocalStorageKey } from '/@/types'
import { routes } from '/@/router'
import { Avatar, UserFilled } from '@element-plus/icons-vue'
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

// 定义菜单类型（与路由meta对齐）
interface MenuItem {
  index: string
  label: string
  path: string,
  name: string,
  children?: MenuItem[]
  position?: 'left' | 'right'
  cache?: boolean
  newWindow?: boolean
}

// 菜单自动隐藏
const viewportWidth = ref<number>(window.innerWidth)

const handleResize = () => {
  viewportWidth.value = window.innerWidth
}
onMounted(() => {
  // window.addEventListener('resize', handleResize)
  // handleResize() // Initialize viewportWidth
  activeIndex.value = localStorage.getItem(STORAGE_KEY) || '2'

  // const path = findPathByIndex(allMenuItems, activeIndex.value)
  router.push(selected.value)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
// ---------------------------------------- 自动获取路由-----------------------------------------------------------------
// 1. 从路由中收集所有需要显示的菜单（showInMenu: true）
const menuTree = ref<MenuItem[]>([])
function getShowInMenuRoutes (routes) {
  const result = []

  function traverseRoutes (routeList, parentPath = '') {
    if (!Array.isArray(routeList)) return

    for (const route of routeList) {
      // 构建完整路径
      const fullPath = route.path

      // 检查当前路由是否显示在菜单中
      if (route.meta && route.meta.showInMenu === true) {
        // 创建路由副本，保留children用于菜单树结构
        const routeCopy = {
          ...route.meta,
          path: route.path,
          name: route.name,
          label: route.meta.label || '',
          children: [] // 初始化children数组
        }

        // 递归处理子路由，构建子菜单
        if (route.children && route.children.length > 0) {
          route.children.forEach(child => {
            if (child.meta?.showInMenu === true) {
              const childFullPath = child.path
              routeCopy.children.push({
                ...child.meta,
                path: childFullPath,
                name: child.name,
                label: child.meta.label || '',
                children: child.children || []
              })
            }
          })
        }

        result.push(routeCopy)
      } else {
        // 如果当前路由不显示，但可能有需要显示的子路由，继续递归
        if (route.children && route.children.length > 0) {
          traverseRoutes(route.children, fullPath.endsWith('/') ? fullPath : `${fullPath}/`)
        }
      }
    }
  }

  traverseRoutes(routes)

  return result
}

const menuData = getShowInMenuRoutes(routes)
menuTree.value = menuData

// 2. 拆分左右菜单（按position字段）
const menuOptions = computed(() => {
  return menuTree.value.filter(menu => menu.position === 'left')
})
console.log('左侧菜单', menuOptions)
const menuRightOptions = computed(() => {
  return menuTree.value.filter(menu => menu.position === 'right')
})

// 左侧菜单
// 菜单配置数据（添加path字段）

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
        position: relative;
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
