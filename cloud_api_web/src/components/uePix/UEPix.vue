<template>
  <div class="child-container">
    <div class="player-container" ref="playerContainer">
    </div>
    <div class="send-info">
      <el-form label-position="top" class="message-form">
        <el-form-item label="输入指令:">
          <el-input v-model="sendData.sendInfo" type="textarea" :rows="4"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="sendMessage">发送</el-button>
        </el-form-item>
        <el-form-item label="接收信息:">
          <el-input v-model="sendData.responseInfo" type="textarea" :rows="4"></el-input>
        </el-form-item>
      </el-form>
    </div>
  </div>

</template>
<script setup>
import { CURRENT_CONFIG } from '/@/api/http/config'
import { ref, onMounted, onBeforeUnmount, reactive } from 'vue'
import { ElButton, ElDialog, ElForm, ElFormItem, ElMessageBox, ElInput, ElSelect, ElOption, ElUpload, ElMessage } from 'element-plus'
import { Config, PixelStreaming } from '@epicgames-ps/lib-pixelstreamingfrontend-ue5.4'
import { Application, PixelStreamingApplicationStyle } from '@epicgames-ps/lib-pixelstreamingfrontend-ui-ue5.4'

// 响应式数据
const playerContainer = ref(null)
const stream = ref(null)
const application = ref(null)

const sendData = reactive({
  sendInfo: '',
  responseInfo: ''
})

// 处理响应事件
const handleResponse = (res) => {
  console.log(res)
  sendData.responseInfo = res
}

// 发送消息
const sendMessage = () => {
  // console.log(stream.value)
  if (stream.value) {
    stream.value.emitUIInteraction(sendData.responseInfo)
  }
}

onMounted(() => {
  const PixelStreamingApplicationStyles = new PixelStreamingApplicationStyle()
  PixelStreamingApplicationStyles.applyStyleSheet()

  // Example of how to set the logger level
  // Logger.SetLoggerVerbosity(10);

  // Create a config object
  const config = new Config({
    initialSettings: {
      AutoPlayVideo: true,
      AutoConnect: true,
      StartVideoMuted: true,
      ss: CURRENT_CONFIG.UEPixURL,
      WaitForStreamer: true,
      GamepadInput: false,
      XRControllerInput: false,
      HoveringMouse: true,
      HideUI: true,
      // 增强清晰度(2K推流)
      // 限制最大分辨率
      MaxFPS: 30,
      MinFPS: 15,
      MaxBitrate: 5000000, // 5Mbps
      MinBitrate: 2000000, // 2Mbps
      MatchViewportRes: true,
    },
  })

  // Create a Native DOM delegate instance that implements the Delegate interface class
  const pixelStream = new PixelStreaming(config)
  stream.value = pixelStream

  const appInstance = new Application({
    stream: pixelStream,
    onColorModeChanged: (isLightMode) => PixelStreamingApplicationStyles.setColorMode(isLightMode)
  })

  application.value = appInstance
  if (playerContainer.value) {
    playerContainer.value.appendChild(appInstance.rootElement)
  }
  pixelStream.addResponseEventListener('handle_responses', handleResponse)

  // document.body.appendChild(appInstance.rootElement);
  // pixelStream.addResponseEventListener("handle_responses", handleResponse);
})

onBeforeUnmount(() => {
  // 清理逻辑
  // if (application.value && application.value.rootElement) {
  //   document.body.removeChild(application.value.rootElement);
  // }
  // if (stream.value) {
  //   stream.value.removeResponseEventListener("handle_responses", handleResponse);
  // }
  if (application.value && application.value.rootElement && playerContainer.value) {
    // 从组件容器中移除，而不是从 document.body
    playerContainer.value.removeChild(application.value.rootElement)
  }
  if (stream.value) {
    stream.value.removeResponseEventListener('handle_responses', handleResponse)
  }
})

// 暴露方法到模板
// defineExpose({
//   sendMessage
// })
</script>

<style scoped>
.child-container{
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
}
.player-container {
    width: 80%;
    height: 100%;
    min-height: -webkit-fill-available;
    font-family: 'Montserrat';
    margin: 0;
}
.send-info{
  width: 20%;
  height: 100%;
  border: 1px solid rgba(121, 242, 238, 0.726);

}

.message-form {
  margin: 20px;
  color: rgb(255, 255, 255);
  font-size: 20px;
  font-weight: 500;
}

:deep(.el-form-item__label){
  color: rgb(233, 236, 231) !important;
}
</style>
