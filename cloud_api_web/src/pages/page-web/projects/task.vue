<template>
      <!-- <div >
          <router-link :to="{ name: ERouterName.CREATE_PLAN}">
          </router-link>

      </div> -->
      <router-view/>
      <!-- <div v-if="!taskRoute">
        <router-view/>
      </div> -->
</template>

<script lang="ts" setup>
import { PlusOutlined, MinusOutlined } from '@ant-design/icons-vue'
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ERouterName } from '/@/types/enums'
// import createPlan from '/@/components/task/CreatePlan.vue'

const route = useRoute()

const taskRoute = computed(() => {
  return route.name === ERouterName.TASK
})
</script>

<style lang="scss" scoped>
.route-icon {
color: #fff;
font-size: 16px;
}
// .container {
//   height: 100%;
//   width: 100%;
//   padding: 10px;
// }
// 头部  标题 面包屑
.header1 {
  width: 100%;
  height: 60px;
  background: #05204B;
  padding: 16px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color:aliceblue;
}
// 操作部分
.operation{
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
 .item1{
  display: flex;
  justify-items: center;
  align-items: center;
 }
}
.main-content {
    // padding: 15px;
    color: rgba(255, 255, 255, 0.762);
    background-color: rgba(0, 112, 209, 0.2);
    font-size: 16px;
    // border: 4px solid rgba(0, 112, 209, 1);
    border-bottom: none;
    // border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
}
.createPlan{
    width: 350px;
    overflow-y: auto;
    height: 600px;
}
.uranus-scrollbar {
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-color: #c5c8cc transparent;
}
</style>
