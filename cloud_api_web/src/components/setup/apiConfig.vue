<template>
    <div style="width: 100%;">
        <div class="main-box">
            <div class="item-box">
                <div class="item-title">
                    <span>后端IP</span>
                    <span class="title-box">重启生效</span>
                </div>
                <div class="item-describe">本机跟上级系统通信的网段的IP</div>
            </div>
            <div class="item-content">
                <el-input v-model="formData.localhost" />
            </div>
        </div>
        <div class="main-box">
            <div class="item-box">
                <div class="item-title">
                    <span>场站18位编码</span>
                    <span class="title-box">重启生效</span>
                </div>
                <div class="item-describe">由上级系统分配的场站编码，唯一</div>
            </div>
            <div class="item-content">
                <el-input v-model="formData.station_code" />
            </div>
        </div>
        <div class="main-box">
            <div class="item-box">
                <div class="item-title">
                    <span>上级IP</span>
                    <span class="title-box">重启生效</span>
                </div>
                <div class="item-describe">上级系统的IP地址</div>
            </div>
            <div class="item-content">
                <el-input v-model="formData.localhost" />
            </div>
        </div>
        <div class="main-box">
            <div class="item-box">
                <div class="item-title">
                    <span>上级端口</span>
                    <span class="title-box">重启生效</span>
                </div>
                <div class="item-describe">上级系统的端口</div>
            </div>
            <div class="item-content">
                <el-input v-model="formData.SuperiorIP" />
            </div>
        </div>
        <div class="main-box">
            <div class="item-box">
                <div class="item-title">
                    <span>上级18位编码</span>
                    <span class="title-box">重启生效</span>
                </div>
                <div class="item-describe">上级系统的唯一编码</div>
            </div>
            <div class="item-content">
                <el-input v-model="formData.SuperiorPort" />
            </div>
        </div>
        <div class="main-box">
            <div class="item-box">
                <div class="item-title">
                    <span>上级加密口令</span>
                    <span class="title-box">重启生效</span>
                </div>
                <div class="item-describe">根据主站方分配的口令</div>
            </div>
            <div class="item-content">
                <el-input v-model="formData.Superior_Token" />
            </div>
        </div>
        <div  style="display: flex; justify-content: center; margin: 30px 0 20px 0;">
            <el-button class="btn" @click="submit">保存</el-button>
        </div>
    </div>

</template>
<script setup>
import { reactive, ref } from 'vue'
import { CURRENT_CONFIG as config } from '/@/api/http/config'

const parsedUrl = new URL(config.baseURL)
const formData = reactive({
  localhost: parsedUrl.hostname,
  station_code: '',
  SuperiorIP: '',
  SuperiorPort: '',
  Superior_code: '',
  Superior_Token: ''
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
