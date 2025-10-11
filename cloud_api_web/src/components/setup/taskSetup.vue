<template>
    <div style="width: 100%;">
        <div class="main-box">
            <div class="item-box">
                <div class="item-title">
                    <span>进入执行行列时间</span>
                    <span class="title-box">重启生效</span>
                </div>
                <div class="item-describe">航线任务提前多长时间进入执行行列(分钟)</div>
            </div>
            <div class="item-content">
                <el-input v-model="formData.waiting_time" />
            </div>
        </div>
        <div class="main-box">
            <div class="item-box">
                <div class="item-title">
                    <span>超时时间</span>
                    <span class="title-box">重启生效</span>
                </div>
                <div class="item-describe">航线任务超过计划多长时间判定位超时（分钟）</div>
            </div>
            <div class="item-content">
                <el-input v-model="formData.timeout_time" />
            </div>
        </div>
        <div  style="display: flex; justify-content: center; margin: 30px 0 20px 0;">
            <el-button class="btn" @click="submit">保存</el-button>
        </div>
    </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
const formData = reactive({
  waiting_time: 5,
  timeout_time: 200,
})
function submit () {
  console.log('提交配置管理')
}

</script>
<style lang="scss" scoped>
.main-box{
    padding: 20px 15px;
    height: fit-content;
    width: 100%;
    display: flex;
    justify-content: space-between; /* 将子元素分布在两侧 */
    align-items: center;        /* 垂直居中对齐 */
    border-bottom: 1px solid rgba(0, 112, 209, 0.566);
    .item-title{
        color: azure;
        font-size: 18px;
        .title-box{
            margin-left: 10px;
            color: #E3B06D;
            border: 1px solid #D2CBC3;
            padding: 3px;
            font-size: 16px;
        }
    }
    .item-describe{
        color: #8CB0D3;
        font-size: 16px;
    }
}
// 输入框
:deep(.el-input) {
    --el-input-border-color: #8CB0D3;
}
:deep(.el-input__wrapper) {
    background-color:transparent;
}
:deep(.el-input__inner) {
    // background-color:#0B2756;
    color: aliceblue;
}
// 按钮
.btn{
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color:rgba(255, 255, 255, 0.762);
}
</style>
