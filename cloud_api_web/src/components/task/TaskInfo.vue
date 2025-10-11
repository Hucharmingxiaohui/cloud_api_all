<template>
  <div class="layout">
    <!-- 左边：任务信息和站点日志部分 -->
    <div class="left-panel">
      <!-- 任务信息部分，占据页面的30% -->
      <div class="task-info-section" :class="{ fullHeight: activeTab === '手动接管' }">
        <div class="tabs">
          <button :class="{ active: activeTab === '任务信息' }" @click="activeTab = '任务信息'">任务信息</button>
          <button :class="{ active: activeTab === '手动接管' }" @click="activeTab = '手动接管'">手动接管</button>
        </div>

        <!-- 任务信息页面 -->
        <div v-if="activeTab === '任务信息'" class="task-info">
          <div class="manual-task-list">
            <span>当前执行任务:</span>
            <span style="margin-top:10px"> {{TaskInfo.job_name}}</span>
            <!-- <label for="task-select">执行任务</label>
            <select id="task-select" v-model="selectedTask">
              <option disabled value="">请选择一个任务</option>
              <option v-for="task in tasks" :key="task">{{ task }}</option>
            </select>
            <button class="execute-button" @click="executeTask">执行</button> -->
          </div>
        </div>

        <!-- 手动接管页面 -->
        <div v-if="activeTab === '手动接管'" class="manual-control">
          <p>在此处实现手动接管功能。</p>
        </div>
      </div>

      <!-- 站点日志部分，仅在选择“任务信息”时显示 -->
      <div v-if="activeTab === '任务信息'" class="log-section">
        <h3>站点日志</h3>
        <div class="log-content">
          <div v-for="log in logs" :key="log.time" class="log-item">
            <p class="log-time">{{ log.time }}</p>
            <p class="log-message">{{ log.message }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 右边：地图和视频流部分 -->
    <div class="right-panel">
      <!-- 地图窗口，上半部分 -->
      <div class="map-window">
        <gaodeMap ref="gaodemap" />
      </div>

      <!-- 视频流窗口，调整为左右两个视频窗口 -->
      <div class="video-windows">
        <!-- 左边的视频流窗口 -->
        <div class="video-window left">
          <div class="video-controls">
            <button :class="{ active: videoType === 'flv' }" @click="switchToFlv">FLV</button>
            <button :class="{ active: videoType === 'webrtc' }" @click="switchToWebRTC">WebRTC</button>
          </div>
          <video id="videoElementLeft" class="video-stream" controls></video>
        </div>

        <!-- 右边的视频流窗口 -->
        <div class="video-window right">
          <video id="videoElementRight" class="video-stream" controls></video>
          <div class="video-controls">
            <p style="color: white;">机库视频</p>
            <p v-if="errorRightStream" class="error-message">{{ errorRightStream }}</p>
          </div>
          <p v-if="errorRightStream" class="error-message">{{ errorRightStream }}</p>
        </div>
      </div>
    </div>
    <!-- 右侧的右部分：状态显示 -->
    <div class="state-display-section">
      <show-state /> <!-- 直接在此处使用 showState 组件 -->
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { getLocation, deleteWaylineFile, downloadWaylineFile, getWaylineFiles, importKmzFile, searchWaylineFiles, gethWaylineInfo, editWaylineInfo } from '/@/api/wayline'
import ShowState from './showState.vue' // 引入 showState 组件
import flvjs from 'flv.js' // 引入 flv.js 库
import gaodeMap from '/@/components/g-map/mapPanel.vue' // 引入地图组件
const TaskInfo = reactive({
  job_name: '',
  wayline_id: '',
  workspace_id: '',
})

const activeTab = ref('任务信息') // 默认显示任务信息

// 获取当前任务的航线航点
const gaodemap = ref(null) // 节点
function getCurrentWayline (workspace_id, wayline_id) {
  gethWaylineInfo(workspace_id, wayline_id).then(res => {
    if (res.code !== 0) {
      return
    }
    gaodemap.value.WaylineInfo(res.data)
  })
}
// 手动任务相关数据
const selectedTask = ref('')
const tasks = ref([
  '任务1: 巡逻任务',
  '任务2: 侦察任务',
  '任务3: 货物运输',
  '任务4: 应急救援',
])

// 站点日志数据
const logs = ref([
  { time: '2024-10-17 10:00', message: '任务1已启动' },
  { time: '2024-10-17 10:05', message: '任务1已完成' },
  { time: '2024-10-17 10:10', message: '任务2已启动' },
  { time: '2024-10-17 10:15', message: '任务2已完成' },
])

// 执行任务
const executeTask = () => {
  if (selectedTask.value) {
    alert(`正在执行 ${selectedTask.value}`)
  } else {
    alert('请选择一个任务')
  }
}

// 视频流类型
const videoType = ref('flv') // 默认使用 FLV 流
const errorRightStream = ref('') // 错误提示信息

// 初始化左侧 FLV 视频流
const initFlv = () => {
  const videoElementLeft = document.getElementById('videoElementLeft')
  if (flvjs.isSupported()) {
    const flvPlayer = flvjs.createPlayer({
      type: 'flv',
      url: 'http://172.20.63.157:8900/live/12345.flv',
      isLive: true,
      hasAudio: false,
      hasVideo: true
    })
    flvPlayer.attachMediaElement(videoElementLeft)
    flvPlayer.load()
    flvPlayer.play()
  } else {
    console.log('FLV视频流不被支持')
  }
}

// 初始化右侧视频流
const initRightStream = () => {
  const videoElementRight = document.getElementById('videoElementRight')
  // 此处模拟右侧视频流的加载
  const simulatedStreamURL = 'http://example.com/simulated_video_stream' // 更换为实际的视频流地址
  if (videoElementRight) {
    videoElementRight.src = simulatedStreamURL
  }
}

// 切换到 FLV 视频流
const switchToFlv = () => {
  videoType.value = 'flv'
  initFlv()
}

// 切换到 WebRTC 视频流
const switchToWebRTC = () => {
  videoType.value = 'webrtc'
  // startWebRTC()
}

// 在组件挂载时启动默认的 FLV 视频流
onMounted(() => {
  const data = JSON.parse(localStorage.getItem('TaskInfo'))
  TaskInfo.job_name = data.job_name
  TaskInfo.wayline_id = data.file_id
  TaskInfo.workspace_id = data.workspace_id
  setTimeout(() => {
    getCurrentWayline(TaskInfo.workspace_id, TaskInfo.wayline_id)
  }, 500)

  // initFlv() // 启动左侧 FLV 视频流
  // initRightStream() // 初始化右侧视频流
})
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
}

.left-panel {
  width: 25%;
  display: flex;
  flex-direction: column;
  margin-left: 10px;
  height: calc(98vh - 60px);
}

.task-info-section {
  background-color: #222;
  color: #ffffff;
  padding: 20px;
  height: 25%; /* 默认高度为25% */
  box-sizing: border-box;
  border-radius: 10px;
  transition: height 0.1s; /* 添加平滑过渡效果 */
}

.task-info-section.fullHeight {
  height: calc(98vh - 60px); /* 如果手动接管被选中，设置为全高 */
}

.log-section {
  background-color: #1b1f23;
  color: #fff;
  padding: 20px;
  height: calc(72% - 3px); /* 调整高度，使其与其他部分协调 */
  box-sizing: border-box;
  overflow-y: auto;
  border-radius: 10px;
  margin-top: 3px;
}

.log-section h3 {
  color: #ffffff;
  font-size: 20px;
}

.tabs {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.tabs button {
  padding: 10px;
  border: none;
  background-color: #444;
  color: #fff;
  cursor: pointer;
  flex: 1;
  text-align: center;
  margin: 0 5px;
  font-size: 18px;
}

.tabs button.active {
  background-color: #6200ea;
}

.manual-task-list {
  display: flex;
  color: #ffffff;
  flex-direction: column;
}

.manual-task-list select {
  padding: 10px;
  font-size: 16px;
  margin-bottom: 10px;
  width: 100%;
  background-color: #333;
  color: #ffffff;
}

.manual-task-list option {
  background-color: #222;
  color: #ffffff;
}

.execute-button {
  padding: 10px;
  background-color: #007bff;
  color: white;
  border: none;
  cursor: pointer;
  font-size: 16px;
  border-radius: 5px;
}

.execute-button:hover {
  background-color: #0056b3;
}

.log-content {
  max-height: 100%;
  overflow-y: auto;
}

.log-item {
  margin-bottom: 10px;
}

.log-time {
  font-size: 14px;
  color: #00ffcc;
}

.log-message {
  font-size: 14px;
  color: #00bfff;
}

/* 右侧的地图和视频流部分 */
.right-panel {
  width: 75%;
  display: flex;
  flex-direction: column;
  padding-left: 20px;
  box-sizing: border-box;
  height: 90vh;
}

.map-window {
  background-color: #1b1f23;
  border-radius: 8px;
  padding: 10px;
  width: 100%;
  height: 40%;
  margin-left: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 视频窗口 */
.video-windows {
  display: flex;
  width: 100%;
  height: 60%;
  margin-top: 3px;
  margin-left: 20px;
}

.video-window {
  border-radius: 8px;
  padding: 10px;
  position: relative; /* 使按钮绝对定位 */
}

.video-window.left {
  margin-right: 5px; /* 左窗口与右窗口之间的间距 */
}

.video-stream {
  width: 100%;
  height: 100%;
}

.state-display-section {
  width: 30%;
  border-radius: 8px;
  padding: 10px;
  margin-left: 50px;
  margin-right: 10px;
}

.video-controls {
  margin-top: 10px;
  position: absolute; /* 绝对定位 */
  top: 10px; /* 距离顶部的距离 */
  left: 10px; /* 距离左侧的距离 */
  display: flex; /* 使按钮排列在同一行 */
  z-index: 10; /* 增加层级，确保按钮在视频流上方 */
}

.video-controls button {
  margin-right: 10px; /* 按钮之间的间距 */
  padding: 5px 10px; /* 按钮的内边距 */
  background-color: #007bff; /* 按钮的背景色 */
  color: white; /* 按钮的文字颜色 */
  border: none; /* 去掉边框 */
  border-radius: 5px; /* 圆角 */
  cursor: pointer; /* 鼠标悬停时的指针 */
}

.video-controls button.active {
  background-color: #0056b3; /* 选中时的背景色 */
}

.video-controls p {
  color: white;
  font-size: 20px; /* 可以根据需要调整大小 */
}

.error-message {
  color: red;
  font-size: 14px;
}

/* 手动接管部分的样式 */
.manual-control {

  color: #fff;
  padding: 20px;
  height: calc(98vh - 60px); /* 占满剩余空间 */
  box-sizing: border-box;
  border-radius: 10px;
}
</style>
