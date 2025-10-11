<template>
  <div class="project-app-wrapper">
    <div class="left">
      <div class="main-content uranus-scrollbar dark" >
        <mapLayer />
      </div>
    </div>
    <div class="right">
      <div class="map-wrapper">
        <GMap  />
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { ref, onMounted, nextTick } from 'vue'
import mapLayer from '/@/components/layerPanel.vue'

</script>
<style lang="scss" scoped>
@import '/@/styles/index.scss';

.project-app-wrapper {
  display: flex;
  transition: width 0.2s ease;
  height: 100%;
  width: 100%;

  .left {
    display: flex;
    width: 335px;
    flex: 0 0 335px;
    background-color: #232323;

    .main-content {
      flex: 1;
      color: $text-white-basic;
      width: 200px;
    }
  }

  .right {
    flex-grow: 1;
    position: relative;
    .map-wrapper{
      width: 100%;
      height: 100%;
    }

    .Organization-wrapper,
    .media-wrapper,
    .task-wrapper {
      position: absolute;
      top: 0;
      bottom: 0;
      left: 0;
      right: 0;
      z-index: 100;
      background: #f6f8fa;
    }
  }
}
</style>
