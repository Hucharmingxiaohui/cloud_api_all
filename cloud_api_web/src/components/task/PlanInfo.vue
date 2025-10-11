<template>
    <div class="container">
        <!-- 左侧任务栏 -->
        <div class="left-box">
            <div style="height: 50px; line-height: 50px;color: aliceblue; border-bottom: 1px solid #4f4f4f; font-weight: 450;">
                <a-row>
                <a-col :span="1"></a-col>
                <a-col :span="22">任务信息</a-col>
                <a-col :span="1"></a-col>
                </a-row>
            </div>
        </div>
        <div class="live" v-show="showMap" v-drag-window>
            <div style="height: 40px; width: 100%" class="drag-title"></div>
            <a style="position: absolute; right: 10px; top: 10px; font-size: 16px; color: white;" @click="closeDrag"><CloseOutlined /></a>
                <ttt ref="wayLineid"></ttt>
            </div>
        <!-- <div class="right-box">
            <div class="map-box">
                <GMap />
            </div>
            <div class="video-box">
                <div class="box1">

                </div>
                <div class="box2">
                </div>
            </div>
        </div> -->

    </div>
</template>
<script setup>
import map from '/@/components/g-map/mapPanel.vue'
// import GMap from '/@/components/GMap.vue'
import { onMounted, ref } from 'vue'

const showMap = ref(false)
onMounted(() => {
  setTimeout(() => {
    showMap.value = true
  }, 5000)
})

</script>
<style scoped>
.container{
    height: 100%;
    width: 100%;
    display: flex;
    align-items: center;
}
.left-box{
    height: 100vh;
    width: 350px;
    margin: 10px;
    background: #141829;
}
.right-box{
    margin: 10px;
    margin-left: 0;
    /* position: absolute; */
    height: 100vh;
    width: calc(100% - 350px);
    /* height: 500px; */
}
.map-box{
    height: 350px;
    width: 100%;
    background: #141829;
}
.video-box{
    margin-top: 10px;
    width: 100%;
    height: fit-content;
    display: flex;
    align-items: center;

}
.box1{
    width: 460px;
    height: 460px;
    background: #141829;
}
.box2{
    width: 460px;
    height: 460px;
    margin-left: 10px;
    background: #141829;
}
.live {
  position: absolute;
  z-index: 1;
  left: 0;
  top: 400px;
  margin-left: 345px;

  text-align: center;
  width: 1000px;
  height: 420px;
  background: #232323;
}
</style>
