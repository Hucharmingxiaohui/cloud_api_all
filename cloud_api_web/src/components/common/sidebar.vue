<template>
  <div class="page">
    <div class="block_1">
      <div class="text-wrapper_1 ">
        <a-row class="ant-row">
          <a-col :span="1" ></a-col>
          <a-col :span="7" class="text_1" style="display: flex; align-content: center; justify-content: center;">
            <a-space size="large">
              <router-link
                v-for="item in visibleOptions"
                :key="item.key"
                :to="item.path"
                :class="{
                  'menu-item': true,
                  // selected: selectedRoute(item),
                }"
              >
              <span @click="selectedRoute(item)" :class="getItemClass(item)" style="color: rgba(175, 193, 222, 1);">{{ item.label }}</span>
              </router-link>
            </a-space>
          </a-col>
          <a-col :span="1" ></a-col>
          <a-col :span="6" class="text_2">
            <span >无人机任务规划与数据采集系统</span>
          </a-col>
          <a-col :span="1" ></a-col>
          <a-col :span="7" class="text_1" style="display: flex; align-content: center; justify-content: center;">
            <a-space size="large">
              <router-link
                v-for="item in visibleOptions1.slice(0,1)"
                :key="item.key"
                :to="item.path"
                :class="{
                  'menu-item': true,
                }"
              >
                <span @click="selectedRoute(item)" :class="getItemClass1(item)" style="color: rgba(175, 193, 222, 1);">{{ item.label }}</span>
              </router-link>
              <a-dropdown v-if="visibleOptions1.some(item => item.label.includes('任务管理'))">
                <span :class="getItemClass3()"> 任务管理 </span>
                <template #overlay>
                  <a-menu theme="dark" class="flex-column flex-justify-between flex-align-center" >
                    <a-menu-item
                      v-for="item in options4"
                      :key="item.key"
                    >
                      <router-link :to="item.path">
                          <span @click="selectedRoute(item)" :style="selected === item.path ? 'color: #2d8cf0;' : 'color: white'" style="padding: 10px 0; display: block;">{{ item.label }}</span>
                      </router-link>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-dropdown v-if="visibleOptions1.some(item => item.label.includes('系统管理'))">
                <span :class="getItemClass2()"> 系统管理 </span>
                <template #overlay>
                  <a-menu theme="dark" class="flex-column flex-justify-between flex-align-center" >
                    <a-menu-item
                      v-for="item in options3"
                      :key="item.key"
                    >
                      <router-link :to="item.path">
                          <span @click="selectedRoute(item)" :style="selected === item.path ? 'color: #2d8cf0;' : 'color: white'" style="padding: 10px 0; display: block;">{{ item.label }}</span>
                      </router-link>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </a-col>
          <a-col :span="1" class="text_1"  style="margin-top: 30px;">
          <div>
            <a-dropdown>
              <div class="height-100">
                <span class="fz20 mt20" style="border: 2px solid white; border-radius: 50%; display: inline-flex;"><UserOutlined /></span>
                <!-- <span class="ml10 mr10" style="float: right;">{{ username }}</span> -->
              </div>
              <template #overlay>
                <a-menu theme="dark" class="flex-column flex-justify-between flex-align-center">
                  <a-menu-item >
                    <span class="mr10" style="font-size: 16px;"><ExportOutlined /></span>
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
<!-- <div class="header-bg" style="height: 60px;">
  <a-row>
    <a-col :span="2"></a-col>
    <a-col :span="6" style="display: flex; justify-content: center; align-items: center; height: 60px">
      <a-space class="fz16 height-100" size="large">
        <router-link
          v-for="item in visibleOptions"
          :key="item.key"
          :to="item.path"
          :class="{
            'menu-item': true,
            // selected: selectedRoute(item),
          }"
        >
        <span @click="selectedRoute(item)"  class="mean-bar"  :style="selected === item.path ? 'color: #2d8cf0;' : 'color: white'">{{ item.label }}</span>
        </router-link>
      </a-space>
    </a-col>
    <a-col :span="8"  style="display: flex; justify-content: center; align-items: center; font-size: 25px; font-weight: bold; height: 60px;">
      <span style="white-space: nowrap; overflow: hidden;">无人机管控平台</span>
    </a-col>
    <a-col :span="6"  style="display: flex; justify-content: center; align-items: center; height: 60px;">
      <a-space class="fz16 height-100" size="large">
        <router-link
          v-for="item in visibleOptions1.slice(0,2)"
          :key="item.key"
          :to="item.path"
          :class="{
            'menu-item': true,
            // selected: selectedRoute(item),
          }"
        >
          <span @click="selectedRoute(item)" class="mean-bar"  :style="selected === item.path ? 'color: #2d8cf0;' : 'color: white'">{{ item.label }}</span>
        </router-link>
        <a-dropdown class="mean-bar" v-if="visibleOptions1.some(item => item.label.includes('系统管理'))">
          <span :style="options3.some(item => item.path.includes(selected)) ? 'color: #2d8cf0;' : 'color: white'"> 系统管理 </span>
          <template #overlay>
            <a-menu theme="dark" class="flex-column flex-justify-between flex-align-center" >
              <a-menu-item
                v-for="item in options3"
                :key="item.key"
              >
                <router-link :to="item.path">
                    <span @click="selectedRoute(item)"  class="mean-bar" :style="selected === item.path ? 'color: #2d8cf0;' : 'color: white'">{{ item.label }}</span>
                </router-link>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-space>
    </a-col>
    <a-col :span="2"  style="display: flex; justify-content: center; align-items: center;">
      <div>
        <a-dropdown>
          <div class="height-100">
            <span class="fz20 mt20" style="border: 2px solid white; border-radius: 50%; display: inline-flex;"><UserOutlined /></span>
            <span class="ml10 mr10" style="float: right;">{{ username }}</span>
          </div>
          <template #overlay>
            <a-menu theme="dark" class="flex-column flex-justify-between flex-align-center">
              <a-menu-item >
                <span class="mr10" style="font-size: 16px;"><ExportOutlined /></span>
                <span @click="logout">注销</span>
              </a-menu-item>
              <a-menu-item >
                <span class="mr10" style="font-size: 16px;"><ExportOutlined /></span>
                <span @click="goHome">返回</span>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </a-col>
  </a-row>
</div> -->
</template>

<script lang="ts" setup>
import { createVNode, defineComponent, ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { getRoot } from '/@/root'
import * as icons from '@ant-design/icons-vue'
import { ERouterName, ELocalStorageKey } from '/@/types'
import { UserOutlined, ExportOutlined } from '@ant-design/icons-vue'

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
const visibleOptions = computed(() => {
  const maxItems = Math.floor(viewportWidth.value / 4 / 120) // Adjust 150 based on your menu item width
  return options1.slice(0, maxItems)
})
const visibleOptions1 = computed(() => {
  const maxItems = Math.floor(viewportWidth.value / 4 / 120) // Adjust 150 based on your menu item width
  return options2.slice(0, maxItems)
})
const Icon = (props: {icon: string}) => {
  return createVNode((icons as any)[props.icon])
}

const handleResize = () => {
  viewportWidth.value = window.innerWidth
}
onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize() // Initialize viewportWidth
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
// ----------------------------------------
const root = getRoot()
const selected = ref<string>(root.$route.path) // 跳转
const username = ref(localStorage.getItem(ELocalStorageKey.Username)) // 从本地获取登录用户名
const options1 = [
  // { key: 0, label: '组织管理', path: '/' + ERouterName.Organization, icon: 'ApartmentOutlined' },
  { key: 0, label: '风机管理', path: '/fanMgt', icon: 'ApartmentOutlined' },
  // { key: 0, label: '场站管理', path: '/station', icon: 'ApartmentOutlined' },
  { key: 1, label: '设备管理', path: '/' + ERouterName.DEVICES, icon: 'UserOutlined' },
  { key: 2, label: '视频直播', path: '/' + ERouterName.LIVESTREAM, icon: 'VideoCameraOutlined' },
  // { key: 3, label: '地图标注', path: '/' + ERouterName.LAYER, icon: 'EnvironmentOutlined' },
]
const options2 = [
  { key: 5, label: '航线管理', path: '/' + ERouterName.NEW_WAYLINE, icon: 'NodeIndexOutlined' },
  { key: 6, label: '任务管理', path: '/' + ERouterName.FLY_PLAN, icon: 'CalendarOutlined' },
  { key: 7, label: '系统管理', path: '/' + ERouterName.FLIGHT_AREA, icon: 'GroupOutlined' },
  // { key: 7, label: '飞行区域', path: '/' + ERouterName.FLIGHT_AREA, icon: 'GroupOutlined' },
]
const options3 = [
  // { key: 8, label: '飞行区域', path: '/' + ERouterName.FLIGHT_AREA, icon: 'GroupOutlined' },
  // { key: 9, label: '视频管理', path: '/' + ERouterName.Camera_Management, icon: 'GroupOutlined' },
  { key: 10, label: '系统配置', path: '/' + ERouterName.Setup_Management, icon: 'GroupOutlined' },
  // { key: 11, label: '媒体文件', path: '/' + ERouterName.MEDIA, icon: 'PictureOutlined' },
  { key: 12, label: '台账管理', path: '/' + ERouterName.Points_Management, icon: 'PictureOutlined' },
  { key: 13, label: '主机连接', path: '/' + ERouterName.HOSTCONNECT, icon: 'CalendarOutlined' },
  { key: 14, label: '日志管理', path: '/' + ERouterName.LOGS, icon: 'CalendarOutlined' },
  { key: 15, label: '成员管理', path: '/member', icon: 'CalendarOutlined' },
  { key: 16, label: '固件管理', path: '/' + ERouterName.FIRMWARES, icon: 'GroupOutlined' },
  { key: 17, label: '模型管理', path: '/modelManage', icon: 'CalendarOutlined' },

]

const options4 = [
  // { key: 8, label: '飞行区域', path: '/' + ERouterName.FLIGHT_AREA, icon: 'GroupOutlined' },
  { key: 17, label: '飞行计划', path: '/' + ERouterName.FLY_PLAN, icon: 'GroupOutlined' },
  { key: 18, label: '飞行任务', path: '/' + ERouterName.TASK, icon: 'GroupOutlined' },
]

// watch(() => root.$route.path, data => {
//   console.log('跳转', data)
// })

function selectedRoute (item: IOptions) {
  const path = typeof item.path === 'string' ? item.path : item.path.path
  selected.value = path
  // console.log('xxxx', selected.value)
  return root.$route.path?.indexOf(path) === 0
}

const getItemClass = (item: any) => {
  // console.log('asssss', item.path, selected.value)
  return {
    'selected-item': selected.value === item.path || (item.path === '/livestream' && selected.value === '/remoteDebug'), // 选中时添加 selected-item 类
    'unselected-item': selected.value !== item.path, // 未选中时添加 unselected-item 类
  }
}
const getItemClass1 = (item: any) => {
  return {
    'selected-item1': selected.value === item.path, // 选中时添加 selected-item 类
    'unselected-item': selected.value !== item.path, // 未选中时添加 unselected-item 类
  }
}
const getItemClass2 = () => {
  return {
    'selected-item1': options3.some(option => option.path.includes(selected.value)),
    'unselected-item': !options3.some(option => option.path.includes(selected.value))
  }
}

const getItemClass3 = () => {
  return {
    'selected-item1': options4.some(option => option.path.includes(selected.value)) || selected.value.includes('task'),
    'unselected-item': !options4.some(option => option.path.includes(selected.value))
  }
}
// const getItemStyle1 = () => {
//   return options3.some(item => item.path.includes(selected.value))
//     ? {
//         color: 'rgba(175, 193, 222, 1)', // 选中时文字颜色
//         transform: 'skew(-20deg)', // 平行四边形效果
//         padding: '10px 20px',
//         // border: 'none', // 移除默认边框
//         border: '2px solid #0B274F',
//         background: 'linear-gradient(to right, #0B2954 0%, #315F96 35%, #ffffff 50%,#315F96 65%, #0B2954 100%), linear-gradient(to bottom, #0B2954 0%, #1E477A 95%, #ffffff 98%, #062249 100%)', // 顶部渐变
//         position: 'relative',
//         display: 'inline-block',
//         backgroundSize: '100% 100%', // 修正: 使用驼峰命名
//         backgroundBlendMode: 'overlay', // 修正: 使用驼峰命名
//         boxSizing: 'border-box'
//       }
//     : {
//         color: 'rgba(175, 193, 222, 1) ' // 未选中时文字颜色
//       }
// }
const logout = () => {
  localStorage.clear()
  root.$router.push('/' + ERouterName.PROJECT)
}

function goHome () {
  root.$router.push('/' + ERouterName.MEMBERS)
}

</script>

<style scoped lang="scss">

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

@import '/@/styles/index.scss';

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
