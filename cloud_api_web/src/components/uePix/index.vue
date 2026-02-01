<template>
    <div class="page-container">
        <div class="page-title">三维模型页面加载demo</div>
        <div class="model-box">
            <UEPix></UEPix>
        </div>
    </div>

</template>
<script  setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import UEPix from './UEPix.vue'
</script>
<style scoped>
.page-container{
    height: calc(100vh - 200px);
    width: 100%;
    margin: 15px;
}
.page-title{
    width: 100%;
    text-align: center;
    color: white;
    font-size: 25px;
}
.model-box{
    border: 1px solid skyblue;
    height: 80vh;
}
</style>
